// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.localworkspace;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicBoolean;

import com.microsoft.tfs.core.clients.versioncontrol.WorkspaceLocation;
import com.microsoft.tfs.core.clients.versioncontrol.Workstation;
import com.microsoft.tfs.core.clients.versioncontrol.exceptions.VersionControlException;
import com.microsoft.tfs.core.clients.versioncontrol.internal.localworkspace.AllTablesTransaction;
import com.microsoft.tfs.core.clients.versioncontrol.internal.localworkspace.LocalPendingChangesTable;
import com.microsoft.tfs.core.clients.versioncontrol.internal.localworkspace.LocalWorkspaceProperties;
import com.microsoft.tfs.core.clients.versioncontrol.internal.localworkspace.LocalWorkspaceTransaction;
import com.microsoft.tfs.core.clients.versioncontrol.internal.localworkspace.WorkspaceLocalItem;
import com.microsoft.tfs.core.clients.versioncontrol.internal.localworkspace.WorkspacePropertiesLocalVersionTransaction;
import com.microsoft.tfs.core.clients.versioncontrol.internal.localworkspace.WorkspaceVersionTable;
import com.microsoft.tfs.core.clients.versioncontrol.path.LocalPath;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.WorkingFolder;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Workspace;
import com.microsoft.tfs.core.clients.versioncontrol.sparsetree.EnumParentsOptions;
import com.microsoft.tfs.core.clients.versioncontrol.sparsetree.EnumSubTreeOptions;
import com.microsoft.tfs.core.clients.versioncontrol.sparsetree.SparseTree;
import com.microsoft.tfs.core.exceptions.internal.CoreCancelException;
import com.microsoft.tfs.core.util.notifications.Notification;
import com.microsoft.tfs.util.tasks.CanceledException;

public class WorkspaceWatcher {
    private final Workspace workspace;

    private boolean isAsynchronous;

    private final Object lock = new Object();
    private final SparseTree<PathWatcher> pathWatchers;
    private PathWatcherReport report;
    private final Set<String> skippedItems;

    private static final boolean ENABLE_PARTIAL_SCANS;

    /**
     * A property to define whether partial scans should be allowed. Default is
     * true.
     */
    private static final String ENABLE_PARTIAL_SCANS_PROPERTY_NAME =
        "com.microsoft.tfs.core.clients.versioncontrol.localworkspace.enablepartialscans"; //$NON-NLS-1$

    static {
        final String propValue = System.getProperty(ENABLE_PARTIAL_SCANS_PROPERTY_NAME);
        ENABLE_PARTIAL_SCANS = propValue != null && propValue.equalsIgnoreCase("false") ? false : true; //$NON-NLS-1$
    }

    public WorkspaceWatcher(final Workspace workspace) {
        this.workspace = workspace;

        this.report = new PathWatcherReport(true);
        this.pathWatchers =
            new SparseTree<PathWatcher>(LocalPath.TFS_PREFERRED_LOCAL_PATH_SEPARATOR, String.CASE_INSENSITIVE_ORDER);
        this.skippedItems = new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);
    }

    /**
     * True if the WorkspaceWatcher should, in the background, scan the
     * workspace for changes in response to local disk events noticed by the
     * PathWatchers. The scan of the workspace may detect new pending changes
     * and alert subscribers to the PendingChangesChanged and
     * PendingChangeCandidatesChanged events on VersionControlServer.
     *
     * The background scans are rate-limited by the QueuedActionLimiter.
     */
    public boolean isAsynchronous() {
        return isAsynchronous;
    }

    public void setAsynchronous(final boolean value) {
        isAsynchronous = value;

        if (WorkspaceLocation.LOCAL != workspace.getLocation()) {
            return;
        }

        if (value) {
            final LocalWorkspaceTransaction transaction = new LocalWorkspaceTransaction(workspace);
            try {
                transaction.execute(new WorkspacePropertiesLocalVersionTransaction() {
                    @Override
                    public void invoke(final LocalWorkspaceProperties wp, final WorkspaceVersionTable lv) {
                        updatePathWatchers(wp, lv);
                    }
                });
            } finally {
                try {
                    transaction.close();
                } catch (final Exception e) {
                    throw new VersionControlException(e);
                }
            }

            synchronized (lock) {
                for (final PathWatcher pathWatcher : pathWatchers.EnumSubTreeReferencedObjects(
                    null,
                    EnumSubTreeOptions.NONE,
                    Integer.MAX_VALUE)) {
                    if (!pathWatcher.isWatching()) {
                        pathWatcher.startWatching();
                    }
                }
            }
        } else {
            // This is a policy action to help limit the number of outstanding
            // change notification I/O requests. It is not necessary for
            // correctness.

            synchronized (lock) {
                for (final PathWatcher pathWatcher : pathWatchers.EnumSubTreeReferencedObjects(
                    null,
                    EnumSubTreeOptions.NONE,
                    Integer.MAX_VALUE)) {
                    if (pathWatcher.isWatching()) {
                        pathWatcher.stopWatching();
                    }
                }
            }
        }
    }

    /**
     * When the location of the workspace (local or server) changes, the
     * Workspace object should inform the WorkspaceWatcher by calling this
     * method. The WorkspaceWatcher will respond by either spinning up
     * PathWatchers or spinning them down, as appropriate.
     */
    public void locationChanged() {
        if (WorkspaceLocation.LOCAL == workspace.getLocation()) {
            // This is now a local workspace. Invalidate the workspace.
            // PathWatchers will be set up the first time we do a scan.

            synchronized (lock) {
                report = new PathWatcherReport(true /* initiallyInvalidated */);
            }
        } else {
            // This is now a server workspace. Stop watching on all PathWatchers
            // and clear the list.
            synchronized (lock) {
                for (final PathWatcher pathWatcher : pathWatchers.EnumSubTreeReferencedObjects(
                    null,
                    EnumSubTreeOptions.NONE,
                    Integer.MAX_VALUE)) {
                    if (pathWatcher.isWatching()) {
                        pathWatcher.stopWatching();
                    }
                }

                pathWatchers.clear();
            }
        }
    }

    /**
     * Stops all watchers. The watchers may be started again automatically by
     * the next call to {@link #poll()} or another method. This method is mainly
     * to support an application switching away from a workspace (perhaps to use
     * another), so that the old workspace's watchers are no longer hooked up.
     */
    public void stopWatching() {
        if (WorkspaceLocation.LOCAL != workspace.getLocation()) {
            return;
        }

        synchronized (lock) {
            for (final PathWatcher pathWatcher : pathWatchers.EnumSubTreeReferencedObjects(
                null,
                EnumSubTreeOptions.NONE,
                Integer.MAX_VALUE)) {
                if (pathWatcher.isWatching()) {
                    pathWatcher.stopWatching();
                }
            }

            pathWatchers.clear();
        }
    }

    /**
     * If a PathWatcher is created but the path to watch on disk does not exist,
     * then the PathWatcher will not start asynchronously watching for changes
     * on disk. (It will still be invalidated, and our polling architecture will
     * work fine.)
     *
     * Call EnsureWatching to try and start each PathWatcher again. This is
     * called every time we perform baseline maintenance on the local workspace
     * properties table, for example.
     */
    public void ensureWatching() {
        if (WorkspaceLocation.LOCAL != workspace.getLocation()) {
            return;
        }

        if (isAsynchronous) {
            synchronized (lock) {
                for (final PathWatcher pathWatcher : pathWatchers.EnumSubTreeReferencedObjects(
                    null,
                    EnumSubTreeOptions.NONE,
                    Integer.MAX_VALUE)) {
                    if (!pathWatcher.isWatching()) {
                        pathWatcher.startWatching();
                    }
                }
            }
        }
    }

    /**
     * Call this to inform the WorkspaceWatcher that the working folders for the
     * Workspace have changed. The WorkspaceWatcher will ensure that it is
     * watching the new working folders, in addition to whatever it was already
     * watching.
     */
    public void workingFoldersChanged(final WorkingFolder[] workingFolders) {
        if (WorkspaceLocation.LOCAL != workspace.getLocation()) {
            return;
        }

        // This is different from a call to UpdatePathWatchers. We are in this
        // method only adding to our set of watchers if a path is not being
        // watched currently. From this entry point, we don't have access to the
        // local version table. We would need it in order to get the roots of
        // the local version table.

        synchronized (lock) {
            for (final String workspaceRoot : WorkingFolder.getWorkspaceRoots(workingFolders)) {
                if (pathWatchers.EnumParents(workspaceRoot, EnumParentsOptions.NONE).iterator().hasNext()) {
                    // This item already has a parent in the tree.
                    continue;
                }

                // Before we add this item, ensure that any children of this
                // item that are already present are removed.
                pathWatchers.remove(workspaceRoot, true /* removeChildren */);

                final PathWatcher toAdd = newPathWatcher(workspaceRoot);

                if (isAsynchronous) {
                    toAdd.startWatching();
                }

                pathWatchers.add(workspaceRoot, toAdd);
            }

            report.fullyInvalidate();
        }
    }

    /**
     * Allows callers to explicitly add a path to the changed paths list for
     * partial scans.
     */
    public void markPathChanged(final String path) {
        if (WorkspaceLocation.LOCAL != workspace.getLocation()) {
            return;
        }

        synchronized (lock) {
            if (path == null || path.length() == 0) {
                report.fullyInvalidate();
            } else {
                report.addChangedPath(path);
            }
        }
    }

    public void forceFullScan() throws IOException {
        final LocalWorkspaceTransaction transaction = new LocalWorkspaceTransaction(workspace);

        try {
            transaction.execute(new AllTablesTransaction() {
                @Override
                public void invoke(
                    final LocalWorkspaceProperties wp,
                    final WorkspaceVersionTable lv,
                    final LocalPendingChangesTable pc) {
                    report.fullyInvalidate();
                    scan(wp, lv, pc);
                }
            });
        } finally {
            transaction.close();
        }
    }

    /**
     * Performs a scan of the workspace if necessary.
     *
     *
     * @param wp
     *        Workspace properties table
     * @param lv
     *        Local version table
     * @param pc
     *        Pending changes table
     */
    public void scan(
        final LocalWorkspaceProperties wp,
        final WorkspaceVersionTable lv,
        final LocalPendingChangesTable pc) {
        updatePathWatchers(wp, lv);

        PathWatcherReport report;

        synchronized (lock) {
            report = poll();
            report.unionWith(this.report);
            this.report = new PathWatcherReport(false /* initiallyInvalidated */);
        }

        // There's no possibility of another thread reading the now-empty report
        // and rushing past without a scan, since we're currently running a
        // local workspace transaction.

        if (!report.isNothingChanged()) {
            LocalWorkspaceScanner scanner;

            synchronized (skippedItems) {
                scanner = new LocalWorkspaceScanner(wp, lv, pc, skippedItems);
            }

            try {
                if (report.getFullyInvalidated() || !ENABLE_PARTIAL_SCANS) {
                    scanner.fullScan();
                } else {
                    scanner.partialScan(report.getChangedPaths());
                }
            } catch (final CoreCancelException e) {
                throw new CanceledException();
            }
        }
    }

    /**
     * Returns true if a scan is necessary on this workspace.
     */
    public boolean isScanNecessary() {
        if (WorkspaceLocation.LOCAL != workspace.getLocation()) {
            return false;
        }

        synchronized (lock) {
            report.unionWith(poll());

            return !report.isNothingChanged();
        }
    }

    public void addSkippedItem(final String localItem) {
        if (WorkspaceLocation.LOCAL != workspace.getLocation()) {
            return;
        }

        synchronized (skippedItems) {
            skippedItems.add(localItem);
        }
    }

    public void pathChanged(final PathWatcher sender) {
        if (isScanNecessary()) {
            /*
             * (This comment is from VS)
             *
             * The Team Explorer Everywhere team asked that we raise a
             * cross-process notification when the scanner runs asynchronously
             * and discovers changes.
             */
            final AtomicBoolean raiseCrossProcessNotification = new AtomicBoolean();

            final LocalWorkspaceTransaction transaction = new LocalWorkspaceTransaction(workspace);
            try {
                transaction.execute(new AllTablesTransaction() {
                    @Override
                    public void invoke(
                        final LocalWorkspaceProperties wp,
                        final WorkspaceVersionTable lv,
                        final LocalPendingChangesTable pc) {
                        scan(wp, lv, pc);

                        if (lv.isDirty() || pc.isDirty()) {
                            raiseCrossProcessNotification.set(true);
                        }
                    }
                });
            } finally {
                try {
                    transaction.close();
                } catch (final Exception e) {
                    throw new VersionControlException(e);
                }
            }

            if (raiseCrossProcessNotification.get()) {
                Workstation.getCurrent(
                    workspace.getClient().getConnection().getPersistenceStoreProvider()).notifyForWorkspace(
                        workspace,
                        Notification.VERSION_CONTROL_LOCAL_WORKSPACE_SCAN);
            }
        }
    }

    public void removeSkippedItem(final String localItem) {
        if (WorkspaceLocation.LOCAL != workspace.getLocation()) {
            return;
        }

        synchronized (skippedItems) {
            skippedItems.remove(localItem);
        }
    }

    /**
     * Destructively retrieves each PathWatcher's report and aggregates them
     * together to form a workspace report. The caller must have already entered
     * the m_lock monitor.
     */
    private PathWatcherReport poll() {
        final PathWatcherReport toReturn = new PathWatcherReport(false /* initiallyInvalidated */);

        for (final PathWatcher pathWatcher : pathWatchers.EnumSubTreeReferencedObjects(
            null,
            EnumSubTreeOptions.NONE,
            Integer.MAX_VALUE)) {
            if (!pathWatcher.isWatching()) {
                pathWatcher.startWatching();
            }

            toReturn.unionWith(pathWatcher.poll());
        }

        return toReturn;
    }

    private void updatePathWatchers(final LocalWorkspaceProperties wp, final WorkspaceVersionTable lv) {
        final SparseTree<String> newRoots =
            new SparseTree<String>(LocalPath.TFS_PREFERRED_LOCAL_PATH_SEPARATOR, String.CASE_INSENSITIVE_ORDER);

        for (final String workingFolderRoot : WorkingFolder.getWorkspaceRoots(wp.getWorkingFolders())) {
            newRoots.add(workingFolderRoot, workingFolderRoot);
        }

        // The local item index of the local version table is a multi-rooted
        // tree. Query for its roots and add them to the new roots tree, if they
        // aren't already covered. This is for the case where the user has
        // changed their mappings but not yet done a get to move everything
        // under the new mappings.
        for (final WorkspaceLocalItem lvEntry : lv.queryLocalItemRoots()) {

            if (newRoots.EnumParents(lvEntry.getLocalItem(), EnumParentsOptions.NONE).iterator().hasNext()) {
                // This item already has a parent in the tree.
                continue;
            }

            // Before we add this item, ensure that any children of this item
            // that are already present are removed.
            newRoots.remove(lvEntry.getLocalItem(), true);
            newRoots.add(lvEntry.getLocalItem(), lvEntry.getLocalItem());
        }

        synchronized (lock) {
            for (final String newRoot : newRoots.EnumSubTreeReferencedObjects(
                null,
                EnumSubTreeOptions.NONE,
                Integer.MAX_VALUE)) {
                final PathWatcher pathWatcher = pathWatchers.get(newRoot);

                if (pathWatcher == null) {
                    pathWatchers.add(newRoot, newPathWatcher(newRoot));
                }
            }

            final List<String> toRemove = new ArrayList<String>();

            for (final PathWatcher pathWatcher : pathWatchers.EnumSubTreeReferencedObjects(
                null,
                EnumSubTreeOptions.NONE,
                Integer.MAX_VALUE)) {
                if (newRoots.get(pathWatcher.getPath()) == null) {
                    toRemove.add(pathWatcher.getPath());
                }
            }

            for (final String path : toRemove) {
                pathWatchers.remove(path, false);
            }
        }
    }

    private PathWatcher newPathWatcher(final String path) {
        return workspace.getClient().getPathWatcherFactory().newPathWatcher(path, this);
    }
}
