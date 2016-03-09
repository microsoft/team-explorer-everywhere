// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.repository.cache.pendingchange;

import java.text.MessageFormat;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.microsoft.tfs.core.clients.versioncontrol.path.LocalPath;
import com.microsoft.tfs.core.clients.versioncontrol.path.ServerPath;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.ChangeType;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.ItemType;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.PendingChange;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Workspace;
import com.microsoft.tfs.util.Check;

/**
 * {@link PendingChangeCollection} is an internal class intended for use only by
 * {@link PendingChangeCache}. It stores the cached pending change data and
 * provides several indexes over the cached pending changes (by server path, by
 * local path, and by parentage so that you can query all pending changes
 * beneath a given path.)
 */
public class PendingChangeCollection {
    private final Workspace workspace;

    private static final Log log = LogFactory.getLog(PendingChangeCollection.class);

    /**
     * Canonical server path ({@link String}) to {@link PendingChange}. This is
     * the authoritative list of pending changes.
     */
    private final Map<String, PendingChange> changesByServerPath = new HashMap<String, PendingChange>();

    /**
     * Canonical parent server path ({@link String}) to {@link Set} of
     * {@link PendingChange}.
     */
    private final Map<String, Set<PendingChange>> changesByParentServerPath = new HashMap<String, Set<PendingChange>>();

    /**
     * Canonical local path ({@link String}) to {@link PendingChange}.
     */
    private final Map<String, PendingChange> changesByLocalPath = new HashMap<String, PendingChange>();

    /**
     * Canonical parent local path ({@link String}) to {@link Set} of
     * {@link PendingChange}.
     */
    private final Map<String, Set<PendingChange>> changesByParentLocalPath = new HashMap<String, Set<PendingChange>>();

    /**
     * A lock acquired to read/write any of the collection fields or to
     * read/write any of the value items (Sets and Collections) inside the
     * collection fields.
     */
    private final Object lock = new Object();

    public PendingChangeCollection(final Workspace workspace) {
        Check.notNull(workspace, "workspace"); //$NON-NLS-1$

        this.workspace = workspace;
    }

    /**
     * Clears all pending change data held by this collection.
     */
    public void clear() {
        synchronized (lock) {
            changesByServerPath.clear();
            changesByParentServerPath.clear();
            changesByLocalPath.clear();
            changesByParentLocalPath.clear();
        }
    }

    /**
     * Adds a new pending change to this collection. If <code>forRefill</code>
     * is <code>true</code>, no attempt is made to remove any existing pending
     * change with the same server path. This flag should be passed as
     * <code>true</code> when refilling this collection either initially or
     * after clearing it. If <code>forRefill</code> is <code>false</code> and
     * there is an existing pending change with the same server path, the
     * existing pending change is removed and returned.
     *
     * @param newPendingChange
     *        the new {@link PendingChange} to add
     * @param forRefill
     *        <code>true</code> to skip checking for an existing pending change
     *        with the same server path
     * @return an existing pending change with the same server path that was
     *         replaced by the new pending change, or <code>null</code> if no
     *         such pending change exists
     */
    public PendingChange add(final PendingChange newPendingChange, final boolean forRefill) {
        Check.notNull(newPendingChange, "newPendingChange"); //$NON-NLS-1$
        Check.notNull(newPendingChange.getServerItem(), "newPendingChange.serverItem"); //$NON-NLS-1$

        PendingChange oldPendingChange = null;

        synchronized (lock) {
            if (!forRefill) {
                oldPendingChange = remove(newPendingChange);
            }

            addInternal(newPendingChange);
        }

        return oldPendingChange;
    }

    /**
     * Only adds the pending changes to the maps.
     *
     * You must acquire the lock before calling this method.
     *
     * @param newPendingChange
     *        the new {@link PendingChange} to add
     * @return an existing pending change with the same server path that was
     *         replaced by the new pending change, or <code>null</code> if no
     *         such pending change exists
     */
    private void addInternal(final PendingChange newPendingChange) {
        Check.notNull(newPendingChange, "newPendingChange"); //$NON-NLS-1$
        Check.notNull(newPendingChange.getServerItem(), "newPendingChange.serverItem"); //$NON-NLS-1$

        final String serverPath = ServerPath.canonicalize(newPendingChange.getServerItem());
        final String[] serverHierarchy = ServerPath.getHierarchy(serverPath);

        String localPath = null;
        String[] localHierarchy = null;

        if (newPendingChange.getLocalItem() != null) {
            localPath = LocalPath.canonicalize(newPendingChange.getLocalItem());
            localHierarchy = LocalPath.getHierarchy(localPath);
        }

        changesByServerPath.put(serverPath, newPendingChange);

        for (int i = 0; i < serverHierarchy.length; i++) {
            Set<PendingChange> changesForPath = changesByParentServerPath.get(serverHierarchy[i]);
            if (changesForPath == null) {
                changesForPath = new HashSet<PendingChange>();
                changesByParentServerPath.put(serverHierarchy[i], changesForPath);
            }
            changesForPath.add(newPendingChange);
        }

        if (localPath != null) {
            changesByLocalPath.put(localPath, newPendingChange);

            for (int i = 0; i < localHierarchy.length; i++) {
                Set<PendingChange> changesForPath = changesByParentLocalPath.get(localHierarchy[i]);
                if (changesForPath == null) {
                    changesForPath = new HashSet<PendingChange>();
                    changesByParentLocalPath.put(localHierarchy[i], changesForPath);
                }
                changesForPath.add(newPendingChange);
            }
        }
    }

    /**
     * Removes an existing pending change from this collection that has the same
     * server path as the specified pending change.
     *
     * @param changeToRemove
     *        specifies which server item to remove
     * @return the existing pending change that was removed, or
     *         <code>null</code> if no such pending change exists
     */
    public PendingChange remove(final PendingChange changeToRemove) {
        Check.notNull(changeToRemove, "changeToRemove"); //$NON-NLS-1$
        Check.notNull(changeToRemove.getServerItem(), "changeToRemove.serverItem"); //$NON-NLS-1$

        PendingChange removedChange;

        synchronized (lock) {
            removedChange = removeInternal(changeToRemove);

            /*
             * If the undone pending change was a renamed folder, this means
             * that any children have had their paths updated. (We are not given
             * new pending changes for the children in this case.)
             */
            if (removedChange != null
                && removedChange.getChangeType().contains(ChangeType.RENAME)
                && removedChange.getItemType().equals(ItemType.FOLDER)) {
                retargetChildrenOfUndoneRename(removedChange);
            }
        }

        return removedChange;
    }

    private PendingChange removeInternal(final PendingChange changeToRemove) {
        Check.notNull(changeToRemove, "changeToRemove"); //$NON-NLS-1$
        Check.notNull(changeToRemove.getServerItem(), "changeToRemove.serverItem"); //$NON-NLS-1$

        String serverPath = ServerPath.canonicalize(changeToRemove.getServerItem());
        PendingChange removedChange = changesByServerPath.remove(ServerPath.canonicalize(serverPath));

        /*
         * See if this is a rename pending change - we may need to remove the
         * source item
         */
        if (removedChange == null && changeToRemove.getSourceServerItem() != null) {
            serverPath = ServerPath.canonicalize(changeToRemove.getSourceServerItem());
            removedChange = changesByServerPath.remove(ServerPath.canonicalize(serverPath));
        }

        if (removedChange == null && changeToRemove.getSourceLocalItem() != null) {
            serverPath = workspace.getMappedServerPath(changeToRemove.getSourceLocalItem());

            if (serverPath != null) {
                serverPath = ServerPath.canonicalize(serverPath);
                removedChange = changesByServerPath.remove(serverPath);
            }
        }

        if (removedChange == null) {
            return null;
        }

        /* Update the server path hierarchy */
        final String[] serverHierarchy = ServerPath.getHierarchy(serverPath);
        for (int i = 0; i < serverHierarchy.length; i++) {
            final Set<PendingChange> changesForPath = changesByParentServerPath.get(serverHierarchy[i]);
            changesForPath.remove(removedChange);

            if (changesForPath.size() == 0) {
                changesByParentServerPath.remove(serverHierarchy[i]);
            }
        }

        if (removedChange.getLocalItem() != null) {
            final String localPath = LocalPath.canonicalize(removedChange.getLocalItem());
            final String[] localHierarchy = LocalPath.getHierarchy(localPath);

            changesByLocalPath.remove(localPath);

            for (int i = 0; i < localHierarchy.length; i++) {
                final Set<PendingChange> changesForPath = changesByParentLocalPath.get(localHierarchy[i]);
                changesForPath.remove(removedChange);

                if (changesForPath.size() == 0) {
                    changesByParentLocalPath.remove(localHierarchy[i]);
                }
            }
        }

        return removedChange;
    }

    private void retargetChildrenOfUndoneRename(final PendingChange parentChange) {
        Check.notNull(parentChange, "parentChange"); //$NON-NLS-1$
        Check.notNull(parentChange.getServerItem(), "parentChange.serverItem"); //$NON-NLS-1$

        final Set<PendingChange> childChanges = changesByParentServerPath.get(parentChange.getServerItem());

        if (childChanges == null || childChanges.size() == 0) {
            return;
        }

        final String oldServerPath = parentChange.getServerItem(); // never null
        final String oldLocalPath = parentChange.getLocalItem() != null ? parentChange.getLocalItem()
            : workspace.getMappedLocalPath(oldServerPath);
        String newServerPath = parentChange.getSourceServerItem();
        String newLocalPath = parentChange.getSourceLocalItem();

        if (newServerPath == null && newLocalPath != null) {
            newServerPath = workspace.getMappedServerPath(newLocalPath);
        } else if (newLocalPath == null && newServerPath != null) {
            newLocalPath = workspace.getMappedLocalPath(newServerPath);
        }

        if (oldLocalPath == null || newLocalPath == null || newServerPath == null) {
            log.warn(MessageFormat.format(
                "Could not retarget children of undone rename pending change for {0}", //$NON-NLS-1$
                oldServerPath));

            return;
        }

        for (final PendingChange childChange : childChanges) {
            /*
             * Dup this pending change before modifying it - callers that added
             * this change may still have a reference.
             */
            final PendingChange newChildChange = new PendingChange(childChange);

            if (newChildChange.getServerItem() != null) {
                newChildChange.setServerItem(
                    ServerPath.combine(
                        newServerPath,
                        ServerPath.makeRelative(newChildChange.getServerItem(), oldServerPath)));
            }

            if (newChildChange.getLocalItem() != null) {
                newChildChange.setLocalItem(
                    LocalPath.combine(
                        newLocalPath,
                        LocalPath.makeRelative(newChildChange.getLocalItem(), oldLocalPath)));
            }

            removeInternal(childChange);
            addInternal(newChildChange);
        }
    }

    /**
     * @return all of the pending changes currently held by this collection
     */
    public PendingChange[] getValues() {
        /*
         * The conversion to an array must happen inside the lock because the
         * lock covers the values in the map.
         */
        synchronized (lock) {
            final Collection<PendingChange> values = changesByServerPath.values();

            if (values == null) {
                return new PendingChange[0];
            }

            return values.toArray(new PendingChange[values.size()]);
        }
    }

    /**
     * Queries for a pending change with the specified server path. For a given
     * workspace, there is at most one pending change at any time for a given
     * server path.
     *
     * @param serverPath
     *        the server path to identify a pending change with
     * @return the corresponding pending change or <code>null</code> if no such
     *         pending change exists
     */
    public PendingChange getValueByServerPath(String serverPath) {
        serverPath = ServerPath.canonicalize(serverPath);

        synchronized (lock) {
            return changesByServerPath.get(serverPath);
        }
    }

    /**
     * Queries for all pending changes for the specified server path or that
     * have the specified local path as a parent.
     *
     * @param serverPath
     *        the parent local path to identify pending changes with
     * @return each pending change that has the server path as a parent (never
     *         <code>null</code>)
     */
    public PendingChange[] getValuesByServerPathRecursive(String serverPath) {
        serverPath = ServerPath.canonicalize(serverPath);

        /*
         * The conversion to an array must happen inside the lock because the
         * lock covers the values in the map.
         */
        synchronized (lock) {
            final Set<PendingChange> changes = changesByParentServerPath.get(serverPath);

            if (changes == null) {
                return new PendingChange[0];
            }

            return changes.toArray(new PendingChange[changes.size()]);
        }
    }

    /**
     * Queries for a pending change with the specified local path. For a given
     * workspace, there is at most one pending change at any time for a given
     * local path.
     *
     * @param localPath
     *        the local path to identify a pending change with
     * @return the corresponding pending change or <code>null</code> if no such
     *         pending change exists
     */
    public PendingChange getValueByLocalPath(String localPath) {
        localPath = LocalPath.canonicalize(localPath);

        synchronized (lock) {
            return changesByLocalPath.get(localPath);
        }
    }

    /**
     * Queries for all pending changes for the specified local path or that have
     * the specified local path as a parent.
     *
     * @param localPath
     *        the parent local path to identify pending changes with
     * @return each pending change that has the local path as a parent (never
     *         <code>null</code>)
     */
    public PendingChange[] getValuesByLocalPathRecursive(String localPath) {
        localPath = LocalPath.canonicalize(localPath);

        /*
         * The conversion to an array must happen inside the lock because the
         * lock covers the values in the map.
         */
        synchronized (lock) {
            final Set<PendingChange> changes = changesByParentLocalPath.get(localPath);

            if (changes == null) {
                return new PendingChange[0];
            }

            return changes.toArray(new PendingChange[changes.size()]);
        }
    }

    /**
     * Tests whether there are changes for the specified local path or that have
     * the specified local path as a parent.
     *
     * @param localPath
     *        the parent local path to identify pending changes with
     * @return <code>true</code> if the given path has pending changes,
     *         <code>false</code> if it does not
     */
    public boolean hasValuesByLocalPathRecursive(String localPath) {
        localPath = LocalPath.canonicalize(localPath);

        synchronized (lock) {
            final Set<PendingChange> changes = changesByParentLocalPath.get(localPath);

            return changes != null && changes.size() > 0;
        }
    }

    /**
     * @return the current number of pending changes held by this collection
     */
    public int size() {
        synchronized (lock) {
            return changesByServerPath.size();
        }
    }
}
