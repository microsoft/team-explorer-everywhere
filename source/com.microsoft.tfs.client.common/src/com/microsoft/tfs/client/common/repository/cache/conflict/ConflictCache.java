// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.repository.cache.conflict;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import com.microsoft.tfs.core.clients.versioncontrol.path.LocalPath;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Conflict;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Workspace;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.listeners.ListenerCategory;
import com.microsoft.tfs.util.listeners.MultiListenerList;

/**
 * A ConflictCache will hold all the conflicts seen in a particular workspace.
 * Any command which can generate conflicts should update this list after
 * querying conflicts.
 */
public class ConflictCache {
    // the workspace for this conflict manager
    private final Workspace workspace;

    // hash by conflict id and path
    private final Map<Integer, Conflict> conflicts = new HashMap<Integer, Conflict>();

    private final Map<String, Conflict> conflictsByLocalPath =
        new TreeMap<String, Conflict>(LocalPath.TOP_DOWN_COMPARATOR);

    // lock for the conflict list / maps
    private final Object conflictLock = new Object();

    // all listeners for the conflict manager
    private final MultiListenerList conflictListeners = new MultiListenerList();

    // lock for the listeners
    private final Object listenerLock = new Object();

    private static final ListenerCategory ADDED =
        new ListenerCategory(ConflictCacheEvent.ADDED, ConflictCacheListener.class);
    private static final ListenerCategory MODIFIED =
        new ListenerCategory(ConflictCacheEvent.MODIFIED, ConflictCacheListener.class);
    private static final ListenerCategory REMOVED =
        new ListenerCategory(ConflictCacheEvent.REMOVED, ConflictCacheListener.class);

    public ConflictCache(final Workspace workspace) {
        Check.notNull(workspace, "workspace"); //$NON-NLS-1$

        this.workspace = workspace;
    }

    /**
     * Refreshes the conflicts for this workspace.
     */
    public void refresh() {
        Check.notNull(workspace, "workspace"); //$NON-NLS-1$

        final Conflict[] allConflicts = workspace.queryConflicts(null);

        for (final Conflict conflict : allConflicts) {
            addConflict(conflict);
        }
    }

    /**
     * Adds the given conflict to the cache. If this conflict exists already
     * (ie, was previously returned from another event) then the given conflict
     * will be updated (and a modified event will be fired), otherwise it will
     * be added (and an added event will be fired)
     *
     * @param conflict
     *        the conflict to add (may not be <code>null</code>)
     */
    public void addConflict(final Conflict conflict) {
        Check.notNull(conflict, "conflict"); //$NON-NLS-1$

        ListenerCategory listenerType;
        int eventType;

        synchronized (conflictLock) {
            // a duplicate conflict from a new operation
            if (conflicts.containsKey(conflict.getConflictID())) {
                final Conflict oldConflict = conflicts.remove(conflict.getConflictID());

                conflictsByLocalPath.remove(oldConflict.getLocalPath());

                listenerType = MODIFIED;
                eventType = ConflictCacheEvent.MODIFIED;
            }
            // fresh, new conflict
            else {
                listenerType = ADDED;
                eventType = ConflictCacheEvent.ADDED;
            }

            conflicts.put(conflict.getConflictID(), conflict);
            conflictsByLocalPath.put(conflict.getLocalPath(), conflict);
        }

        synchronized (listenerLock) {
            final ConflictCacheListener listener = (ConflictCacheListener) conflictListeners.getListener(listenerType);
            final ConflictCacheEvent e = new ConflictCacheEvent(this, eventType, conflict);

            listener.onConflictEvent(e);
        }
    }

    /**
     * Removes the given conflict from the cache
     *
     * @param conflict
     *        the conflict to remove (may not be <code>null</code>)
     */
    public boolean removeConflict(final Conflict conflict) {
        Check.notNull(conflict, "conflict"); //$NON-NLS-1$

        Conflict removedConflict;

        synchronized (conflictLock) {
            if (conflicts.containsKey(conflict.getConflictID()) == false) {
                return false;
            }

            /*
             * Note, fire an event with the actual conflict in the list, in case
             * listeners are (probably erroneously) holding on to conflicts
             * fired by events from this manager.
             */
            removedConflict = conflicts.remove(conflict.getConflictID());
            conflictsByLocalPath.remove(removedConflict.getLocalPath());
        }

        synchronized (listenerLock) {
            final ConflictCacheListener listener = (ConflictCacheListener) conflictListeners.getListener(REMOVED);
            final ConflictCacheEvent e = new ConflictCacheEvent(this, ConflictCacheEvent.REMOVED, removedConflict);

            listener.onConflictEvent(e);
        }

        return true;
    }

    /**
     * Removes all given conflicts from the cache
     *
     * @param conflicts
     *        an array of conflicts to remove (may not be <code>null</code>)
     */
    public void removeConflicts(final Conflict[] conflicts) {
        Check.notNull(conflicts, "conflicts"); //$NON-NLS-1$

        for (int i = 0; i < conflicts.length; i++) {
            removeConflict(conflicts[i]);
        }
    }

    /**
     * Get all conflicts currently in the cache
     *
     * @return an array of conflicts (never <code>null</code>)
     */
    public Conflict[] getConflicts() {
        synchronized (conflictLock) {
            return conflicts.values().toArray(new Conflict[conflicts.values().size()]);
        }
    }

    /**
     * Add a listener to be notified when conflicts are added
     *
     * @param listener
     *        listener object (not null)
     */
    public void addConflictAddedListener(final ConflictCacheListener listener) {
        Check.notNull(listener, "listener"); //$NON-NLS-1$

        synchronized (listenerLock) {
            conflictListeners.addListener(listener, ADDED);
        }
    }

    public Conflict getConflictByLocalPath(final String localPath) {
        synchronized (conflictLock) {
            return conflictsByLocalPath.get(localPath);
        }
    }

    /**
     * Remove an existing listener for conflict add events
     *
     * @param listener
     *        listener object (not null)
     */
    public void removeConflictAddedListener(final ConflictCacheListener listener) {
        Check.notNull(listener, "listener"); //$NON-NLS-1$

        synchronized (listenerLock) {
            conflictListeners.removeListener(listener, ADDED);
        }
    }

    /**
     * Add a listener to be notified when conflicts are modified
     *
     * @param listener
     *        listener object (not null)
     */
    public void addConflictModifiedListener(final ConflictCacheListener listener) {
        Check.notNull(listener, "listener"); //$NON-NLS-1$

        synchronized (listenerLock) {
            conflictListeners.addListener(listener, MODIFIED);
        }
    }

    /**
     * Remove an existing listener for conflict modified events
     *
     * @param listener
     *        listener object (not null)
     */
    public void removeConflictModifiedListener(final ConflictCacheListener listener) {
        Check.notNull(listener, "listener"); //$NON-NLS-1$

        synchronized (listenerLock) {
            conflictListeners.removeListener(listener, MODIFIED);
        }
    }

    /**
     * Add a listener to be notified when conflicts are removed
     *
     * @param listener
     *        listener object (not null)
     */
    public void addConflictRemovedListener(final ConflictCacheListener listener) {
        Check.notNull(listener, "listener"); //$NON-NLS-1$

        synchronized (listenerLock) {
            conflictListeners.addListener(listener, REMOVED);
        }
    }

    /**
     * Remove an existing listener for conflict remove events
     *
     * @param listener
     *        listener object (not null)
     */
    public void removeConflictRemovedListener(final ConflictCacheListener listener) {
        Check.notNull(listener, "listener"); //$NON-NLS-1$

        synchronized (listenerLock) {
            conflictListeners.removeListener(listener, REMOVED);
        }
    }
}
