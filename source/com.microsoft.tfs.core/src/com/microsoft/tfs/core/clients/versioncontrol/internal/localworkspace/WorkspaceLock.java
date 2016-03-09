// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.internal.localworkspace;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.microsoft.tfs.core.clients.versioncontrol.exceptions.VersionControlException;
import com.microsoft.tfs.core.clients.versioncontrol.localworkspace.LocalMetadataTableLock;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Workspace;
import com.microsoft.tfs.util.Check;

/**
 * A lock on an entire local {@link Workspace}'s disk data.
 *
 * @threadsafety thread-safe
 */
public class WorkspaceLock {
    private final static Log log = LogFactory.getLog(WorkspaceLock.class);

    private static ThreadLocal<WorkspaceLock> current = new ThreadLocal<WorkspaceLock>();

    private final WorkspaceLock previousWorkspaceLock;
    private final Workspace workspace;
    private final Thread creationThread;
    private final boolean previousStronglyRootSetting;

    private volatile LocalMetadataTableLock lock;
    private volatile ReentrantReadWriteLock rwLock;
    private volatile BaselineFolderCollection baselineFolders;

    /**
     * Constructs a {@link WorkspaceLock} for a workspace. This constructor
     * immediately acquires the {@link Workspace}'s internal lock, then the
     * system-wide metadata table mutexes; callers must call {@link #close()} to
     * release them.
     *
     * @param workspace
     *        the workspace to lock (must not be <code>null</code>)
     */
    public WorkspaceLock(final Workspace workspace) {
        this(workspace, true);
    }

    public WorkspaceLock(final Workspace workspace, final boolean requestYield) {
        Check.notNull(workspace, "workspace"); //$NON-NLS-1$

        this.workspace = workspace;
        this.creationThread = Thread.currentThread();

        boolean success = false;
        try {
            this.lock = new LocalMetadataTableLock(workspace.getLocalMetadataDirectory(), requestYield);

            previousWorkspaceLock = current.get();
            current.set(this);

            previousStronglyRootSetting = workspace.getOfflineCacheData().isStronglyRootMetadataTables();

            /*
             * For the duration of the workspace lock, it's expected that we'll
             * be opening/closing the tables a few times. Ensure the garbage
             * collector does not collect them during this interval.
             */
            workspace.getOfflineCacheData().setStronglyRootMetadataTables(true);

            success = true;
        } finally {
            if (!success) {
                // Unlocks everything
                close();
            }
        }
    }

    public void close() {
        Check.isTrue(
            creationThread == Thread.currentThread(),
            "A different thread is disposing a workspace lock than the one that created it."); //$NON-NLS-1$

        current.set(previousWorkspaceLock);

        /*
         * Try very hard to close/release all our locks. We'll throw the first
         * error at the end if there was one.
         */
        Throwable throwable = null;

        try {
            // Unlock metadata tables first.
            if (lock != null) {
                lock.close();
                lock = null;
            }
        } catch (final Throwable t) {
            if (throwable == null) {
                throwable = t;
            }
            log.error("Error unlocking metadata tables", t); //$NON-NLS-1$
        }

        try {
            /*
             * Restore the previous value of the 'strongly root' setting for the
             * workspace's local metadata table cache. If set back to false,
             * cached instances of the local metadata tables will be only
             */
            workspace.getOfflineCacheData().setStronglyRootMetadataTables(previousStronglyRootSetting);
        } catch (final Throwable t) {
            if (throwable == null) {
                throwable = t;
            }
            log.error("Error resetting strongly root metadata tables", t); //$NON-NLS-1$
        }

        if (throwable instanceof RuntimeException) {
            throw (RuntimeException) throwable;
        } else if (throwable != null) {
            throw new RuntimeException(
                MessageFormat.format("Error closing {0} for workspace {1}", getClass().getName(), this.workspace), //$NON-NLS-1$
                throwable);
        }
    }

    // REGION Yield and TransactionLock

    public boolean isYieldRequested() {
        return lock.isYieldRequested();
    }

    /**
     * If another thread in the system is contending for the WorkspaceLock, then
     * this method waits for any transactions running under the protection of
     * this WorkspaceLock to complete, then yields the WorkspaceLock and
     * re-acquires it.
     *
     *
     */
    public void yield() {
        if (lock.isYieldRequested()) {
            // Prevent any new transactions from starting under the protection
            // of this WorkspaceLock.
            getTransactionLock().writeLock().lock();

            try {
                // Drain any threads which may be using the
                // BaselineFolderCollection (and prevent any new ones from
                // starting).
                int writeLockToken = 0;

                if (null != getBaselineFolders()) {
                    writeLockToken = getBaselineFolders().lockForWrite();
                }

                try {
                    // Yield the lock on the workspace and re-acquire it.
                    lock.yield();

                    // Now that the lock has been reacquired, update the
                    // BaselineFolderCollection since the WP table may have
                    // changed in the interim.
                    if (null != getBaselineFolders()) {
                        // This transaction will be allowed to start even though
                        // we have a write lock on the TransactionLock.
                        final LocalWorkspaceTransaction transaction = new LocalWorkspaceTransaction(workspace, this);
                        try {
                            transaction.execute(new WorkspacePropertiesTransaction() {
                                @Override
                                public void invoke(final LocalWorkspaceProperties wp) {
                                    getBaselineFolders().updateFrom(wp.getBaselineFolders());
                                }
                            });
                        } finally {
                            try {
                                transaction.close();
                            } catch (final IOException e) {
                                throw new VersionControlException(e);
                            }
                        }
                    }
                } finally {
                    if (0 != writeLockToken) {
                        getBaselineFolders().unlockForWrite(writeLockToken);
                    }
                }
            } finally {
                // Permit transactions that are waiting to run to begin
                // execution.
                getTransactionLock().writeLock().unlock();
            }
        }
    }

    /**
     * LocalWorkspaceTransaction objects which are running under the protection
     * of a WorkspaceLock that they do not themselves own must call
     * StartTransaction before beginning to execute. This allows the Yield
     * method to wait for them to complete (by calling EndTransaction) before
     * yielding the lock to a contending thread.
     *
     * Transactions which created their own workspace lock (most transactions)
     * to execute do not need to call Start/EndTransaction because they never
     * yield. The extra bookkeeping is unnecessary.
     */
    public void startTransaction() {
        // If *this thread* has the write lock, then permit the transaction to
        // start. We are not asking "is a write lock held by anyone".
        if (!getTransactionLock().isWriteLockedByCurrentThread()) {
            getTransactionLock().readLock().lock();
        }
    }

    /**
     * LocalWorkspaceTransaction objects which are running under the protection
     * of a WorkspaceLock that they do not themselves own must call
     * EndTransaction when they finish execution.
     *
     * Transactions which created their own workspace lock (most transactions)
     * to execute do not need to call Start/EndTransaction because they never
     * yield. The extra bookkeeping is unnecessary.
     */
    public void endTransaction() {
        if (!getTransactionLock().isWriteLockedByCurrentThread()) {
            getTransactionLock().readLock().unlock();
        }
    }

    /**
     * Used to drain and hold pending transactions during a yield of the
     * workspace lock.
     */
    private ReentrantReadWriteLock getTransactionLock() {
        if (null == rwLock) {
            synchronized (lock) {
                if (null == rwLock) {
                    rwLock = new ReentrantReadWriteLock();
                }
            }
        }

        return rwLock;
    }

    public static WorkspaceLock getCurrent() {
        return current.get();
    }

    public BaselineFolderCollection getBaselineFolders() {
        return baselineFolders;
    }

    public void setBaselineFolders(final BaselineFolderCollection baselineFolders) {
        this.baselineFolders = baselineFolders;
    }

    public Workspace getWorkspace() {
        return workspace;
    }
}
