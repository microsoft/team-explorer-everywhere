// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.workspacecache.internal;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import com.microsoft.tfs.core.Messages;
import com.microsoft.tfs.core.clients.versioncontrol.VersionControlClient;
import com.microsoft.tfs.core.clients.versioncontrol.WorkspacePermissions;
import com.microsoft.tfs.core.clients.versioncontrol.exceptions.MultipleWorkspacesFoundException;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Workspace;
import com.microsoft.tfs.core.clients.versioncontrol.specs.WorkspaceSpec;
import com.microsoft.tfs.core.clients.versioncontrol.workspacecache.WorkspaceInfo;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.GUID;

/**
 * In-memory cache of {@link Workspace} objects whose goal is to preserve an
 * instance count of 1 for {@link Workspace}s for a given name and owner.
 *
 * @threadsafety thread-safe
 */
public class RuntimeWorkspaceCache {
    private final Map<String, WeakReference<Workspace>> m_cache =
        new TreeMap<String, WeakReference<Workspace>>(String.CASE_INSENSITIVE_ORDER);

    private final VersionControlClient client;

    // TODO use a non-re-entrant lock like VS does?
    private final ReadWriteLock m_rwLock = new ReentrantReadWriteLock();

    public RuntimeWorkspaceCache(final VersionControlClient client) {
        Check.notNull(client, "client"); //$NON-NLS-1$
        this.client = client;
    }

    /**
     * Calls {@link Workspace#invalidate()} on each {@link Workspace} object in
     * the cache.
     */
    public void invalidateAllWorkspaces() {
        m_rwLock.readLock().lock();
        try {
            for (final Entry<String, WeakReference<Workspace>> entry : m_cache.entrySet()) {
                final Workspace workspace = entry.getValue().get();

                if (null != workspace) {
                    workspace.invalidate();
                }
            }
        } finally {
            m_rwLock.readLock().unlock();
        }
    }

    /**
     * If a Workspace instance is in the runtime cache which matches the name
     * and owner of the given WorkspaceInfo object, that Workspace instance will
     * be returned. Otherwise null will be returned.
     *
     * @param workspaceInfo
     *        the WorkspaceInfo object containing the name and owner of the
     *        workspace to look up.
     * @return the cached workspace if it is in the cache, null otherwise
     */
    public Workspace tryGetWorkspace(final WorkspaceInfo workspaceInfo) {
        Check.notNull(workspaceInfo, "workspaceInfo"); //$NON-NLS-1$
        checkServerGUID(workspaceInfo.getServerGUID(), "workspaceInfo"); //$NON-NLS-1$

        return tryGetWorkspace(workspaceInfo.getName(), workspaceInfo.getOwnerName());
    }

    /**
     * If a Workspace instance is in the runtime cache matching the name and
     * owner provided, then that Workspace instance will be returned. Otherwise
     * null will be returned.
     *
     * @param workspaceName
     *        The name of the workspace to look up (must not be
     *        <code>null</code> or empty)
     * @param workspaceOwner
     *        The owner of the workspace to look up (must not be
     *        <code>null</code> or empty)
     * @return The cached workspace if it is in the cache, null otherwise
     */
    public Workspace tryGetWorkspace(final String workspaceName, final String workspaceOwner) {
        Check.notNullOrEmpty(workspaceName, "workspaceName"); //$NON-NLS-1$
        Check.notNullOrEmpty(workspaceOwner, "workspaceOwner"); //$NON-NLS-1$

        final List<Workspace> matchingWorkspaces = tryGetWorkspacesHelper(workspaceName, workspaceOwner);
        if (matchingWorkspaces.size() > 1) {
            final List<String> specs = new ArrayList<String>(matchingWorkspaces.size());
            for (final Workspace workspace : matchingWorkspaces) {
                specs.add(workspace.getQualifiedName());
            }

            throw new MultipleWorkspacesFoundException(workspaceName, workspaceOwner, specs);
        }

        if (matchingWorkspaces.size() > 0) {
            return matchingWorkspaces.get(0);
        }

        return null;
    }

    /**
     * If a Workspace instance is in the runtime cache which matches the name
     * and owner of the given WorkspaceInfo object, that Workspace instance will
     * be returned. Otherwise a Workspace object will be constructed with data
     * from the WorkspaceInfo object. The Workspace instance will not contain
     * some important properties -- such as the working folders or the effective
     * permissions. If the consumer of the Workspace instance attempts to access
     * those properties, their thread will block on a QueryWorkspace call to
     * retrieve them.
     *
     * @param workspaceName
     *        The name of the workspace to look up
     * @param workspaceOwner
     *        The owner of the workspace to look up
     * @return a {@link Workspace} instance
     */
    public Workspace getWorkspace(final WorkspaceInfo workspaceInfo) {
        Check.notNull(workspaceInfo, "workspaceInfo"); //$NON-NLS-1$

        Workspace cachedWorkspace = tryGetWorkspace(workspaceInfo);

        if (null == cachedWorkspace) {
            // We do not have a Workspace object for this name and owner in our
            // cache
            // currently. Construct a Workspace object from the WorkspaceInfo
            // data that we have.

            final Workspace constructedWorkspace = constructWorkspaceFromWorkspaceInfo(workspaceInfo);
            final String cacheKey = getCacheKey(workspaceInfo);

            m_rwLock.writeLock().lock();
            try {
                final AtomicBoolean deadReference = new AtomicBoolean();

                // We don't care if there's a dead reference. If there is, we're
                // about to replace it.
                cachedWorkspace = tryGetWorkspaceHelper(cacheKey, deadReference);

                if (null == cachedWorkspace) {
                    // We still do not have a Workspace object for this name and
                    // owner. Write
                    // the constructed instance to the cache.

                    cachedWorkspace = constructedWorkspace;
                    m_cache.put(cacheKey, new WeakReference<Workspace>(cachedWorkspace));
                }
            } finally {
                m_rwLock.writeLock().unlock();
            }
        }

        return cachedWorkspace;
    }

    /**
     * If the provided Workspace is not currently cached, it will be added into
     * the cache and the same instance will be returned. If another Workspace
     * instance is in the cache with a matching name and owner, that instance
     * will be updated with all the data from the provided Workspace instance,
     * and the cached instance will be returned.
     * <p>
     * Intended to be used by CreateWorkspace, QueryWorkspace, and
     * QueryWorkspaces, all of which return Workspace objects from the server
     * for use by the client. They need to come through here to enforce the rule
     * that only one Workspace instance exists for consumption by users of the
     * client OM.
     *
     * @param workspace
     *        a Workspace object to be cached or to use as a cache update
     * @param a
     *        Workspace object (not necessarily the same instance) from the
     *        cache which now has all the data from the provided Workspace
     *        instance
     */
    public Workspace cacheWorkspace(final Workspace workspace) {
        Check.notNull(workspace, "workspace"); //$NON-NLS-1$

        checkServerGUID(workspace.getClient().getServerGUID(), "workspace"); //$NON-NLS-1$

        Workspace cachedWorkspace = tryGetWorkspace(workspace.getName(), workspace.getOwnerName());

        if (null == cachedWorkspace) {
            final String cacheKey = getCacheKey(workspace);

            m_rwLock.writeLock().lock();
            try {
                final AtomicBoolean deadReference = new AtomicBoolean();

                // We don't care if there's a dead reference. If there is, we're
                // about to replace it.
                cachedWorkspace = tryGetWorkspaceHelper(cacheKey, deadReference);

                if (null == cachedWorkspace) {
                    cachedWorkspace = workspace;
                    m_cache.put(cacheKey, new WeakReference<Workspace>(cachedWorkspace));
                }
            } finally {
                m_rwLock.writeLock().unlock();
            }
        }

        Check.notNull(cachedWorkspace, "cachedWorkspace"); //$NON-NLS-1$

        if (workspace != cachedWorkspace) {
            // Through this codepath it is not possible for the name or owner of
            // the workspace to have changed.
            cachedWorkspace.updateFromWorkspace(workspace);
        }

        return cachedWorkspace;
    }

    /**
     * A plural form of CacheWorkspace. See CacheWorkspace for details.
     */
    public Workspace[] cacheWorkspaces(final Workspace[] workspaces) {
        Check.notNull(workspaces, "workspaces"); //$NON-NLS-1$

        final Workspace[] toReturn = new Workspace[workspaces.length];

        for (int i = 0; i < workspaces.length; i++) {
            toReturn[i] = cacheWorkspace(workspaces[i]);
        }

        return toReturn;
    }

    /**
     * Similar to CacheWorkspace in that it updates the existing cached
     * workspace with new data, but this method additionally handles a change in
     * the cache key of name;owner. Name/owner changes can never occur with
     * CacheWorkspace.
     */
    public void updateWorkspace(final Workspace existingWorkspace, final Workspace newWorkspace) {
        Check.notNull(existingWorkspace, "existingWorkspace"); //$NON-NLS-1$
        Check.notNull(newWorkspace, "newWorkspace"); //$NON-NLS-1$

        m_rwLock.writeLock().lock();
        try {
            final String oldCacheKey = getCacheKey(existingWorkspace);

            // Do this while holding the write lock on the runtime cache,
            // because
            // the cache key could change as a result of this update.
            existingWorkspace.updateFromWorkspace(newWorkspace);

            // Remove the old cache entry.
            m_cache.remove(oldCacheKey);

            // Insert a new cache entry with the new cache key.
            m_cache.put(getCacheKey(newWorkspace), new WeakReference<Workspace>(existingWorkspace));
        } finally {
            m_rwLock.writeLock().unlock();
        }
    }

    private Workspace constructWorkspaceFromWorkspaceInfo(final WorkspaceInfo workspaceInfo) {
        // Working folders are uncacheable for server workspaces
        final Workspace constructedWorkspace = new Workspace(
            client,
            workspaceInfo.getName(),
            workspaceInfo.getOwnerName(),
            workspaceInfo.getOwnerDisplayName(),
            workspaceInfo.getOwnerAliases(),
            workspaceInfo.getComment(),
            workspaceInfo.getSecurityToken(),
            null, // Uncacheable property -- working folders
            workspaceInfo.getComputer(),
            workspaceInfo.getLocation(),
            WorkspacePermissions.NONE_OR_NOT_SUPPORTED,
            null,
            workspaceInfo.getOptions());

        /*
         * Some properties were not initialized (the "uncacheable" properties)
         * and so we mark this Workspace object as needing to have these
         * properties populated from the server. If a user tries to access one
         * of these uncacheable properties, that thread will block on a
         * QueryWorkspace call to fetch them.
         */
        constructedWorkspace.invalidate();

        return constructedWorkspace;
    }

    private void checkServerGUID(final GUID toCheck, final String propertyName) {
        if (null == toCheck || !toCheck.equals(client.getServerGUID())) {
            throw new IllegalArgumentException(
                Messages.getString("RuntimeWorkspaceCache.VersionControlClientMismatch")); //$NON-NLS-1$
        }
    }

    private String getCacheKey(final Workspace workspace) {
        return new WorkspaceSpec(workspace.getName(), workspace.getOwnerName()).toString();
    }

    private String getCacheKey(final WorkspaceInfo workspaceInfo) {
        return new WorkspaceSpec(workspaceInfo.getName(), workspaceInfo.getOwnerName()).toString();
    }

    /**
     * NOTE: You must be holding at least a read lock.
     */
    private Workspace tryGetWorkspaceHelper(final String cacheKey, final AtomicBoolean deadReference) {
        WeakReference<Workspace> cachedWorkspaceWeakRef;
        deadReference.set(false);

        if ((cachedWorkspaceWeakRef = m_cache.get(cacheKey)) != null) {
            final Workspace cachedWorkspace = cachedWorkspaceWeakRef.get();

            if (null != cachedWorkspace && !cachedWorkspace.isDeleted()) {
                return cachedWorkspace;
            }

            deadReference.set(true);
        }

        return null;
    }

    /**
     * NOTE: You shouldn't be holding any lock and dead references will be
     * cleaned up within this function.
     */
    private List<Workspace> tryGetWorkspacesHelper(final String workspaceName, final String workspaceOwner) {
        final List<Workspace> workspaces = new ArrayList<Workspace>();
        final List<String> deadReferenceCacheKeys = new ArrayList<String>();

        m_rwLock.readLock().lock();
        try {

            for (final Entry<String, WeakReference<Workspace>> entry : m_cache.entrySet()) {
                final Workspace workspace = entry.getValue().get();

                if (null != workspace && !workspace.isDeleted()) {
                    // If workspaceName is null then match all workspaces,
                    // otherwise look to see if the
                    // workspaceName and workspaceOwner match.
                    if (null == workspaceName
                        || (Workspace.matchName(workspaceName, workspace.getName())
                            && workspace.ownerNameMatches(workspaceOwner))) {
                        workspaces.add(workspace);
                    }
                } else {
                    deadReferenceCacheKeys.add(entry.getKey());
                }
            }
        } finally {
            m_rwLock.readLock().unlock();
        }

        if (deadReferenceCacheKeys.size() > 0) {
            m_rwLock.writeLock().lock();
            try {
                for (final String deadReferenceCacheKey : deadReferenceCacheKeys) {
                    removeDeadReference(deadReferenceCacheKey);
                }
            } finally {
                m_rwLock.writeLock().unlock();
            }
        }

        return workspaces;
    }

    /**
     * NOTE: You must be holding a write lock.
     */
    private boolean removeDeadReference(final String cacheKey) {
        final AtomicBoolean deadReference = new AtomicBoolean();
        boolean removed = false;

        // Recheck, because the reference for this key might be live now.
        if (null == tryGetWorkspaceHelper(cacheKey, deadReference) && deadReference.get()) {
            removed = m_cache.remove(cacheKey) != null;
            Check.isTrue(removed, "removed"); //$NON-NLS-1$
        }

        return removed;
    }

    /**
     * All live Workspaces currently held by this cache. /
     */
    public List<Workspace> getWorkspaces() {
        return tryGetWorkspacesHelper(null, null);
    }
}