// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.pendingcheckin;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

import com.microsoft.tfs.core.clients.versioncontrol.path.ServerPath;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.PendingChange;
import com.microsoft.tfs.util.Check;

/**
 * <p>
 * Contains entries for team projects affected by a pending checkin.
 * </p>
 *
 * @since TEE-SDK-10.1
 * @threadsafety thread-safe
 */
public class AffectedTeamProjects {
    private final TreeSet<String> serverPaths = new TreeSet<String>(ServerPath.TOP_DOWN_COMPARATOR);

    public AffectedTeamProjects() {
    }

    public AffectedTeamProjects(final PendingChange[] changes) {
        set(changes);
    }

    public AffectedTeamProjects(final String[] teamProjectPaths) {
        set(teamProjectPaths);
    }

    /**
     * @return all the team project paths known by this object.
     */
    public String[] getTeamProjectPaths() {
        synchronized (serverPaths) {
            return serverPaths.toArray(new String[serverPaths.size()]);
        }
    }

    /**
     * Sets the affected team projects from the server paths of the specified
     * pending changes.
     *
     * @param changes
     *        the changes to update the affected team projects from (must not be
     *        <code>null</code>)
     * @return true if there were changes since the old list of affected team
     *         projects, false if no changes were made.
     */
    public boolean set(final PendingChange[] changes) {
        Check.notNull(changes, "changes"); //$NON-NLS-1$

        synchronized (serverPaths) {
            // Hash set is fine here because we don't need a custom comparator
            // for an immutable copy
            final Set<String> copy = new HashSet<String>(serverPaths);

            serverPaths.clear();

            for (final PendingChange change : changes) {
                serverPaths.add(ServerPath.getTeamProject(change.getServerItem()));
            }

            return !serverPaths.equals(copy);
        }
    }

    /**
     * Sets the affected team projects from a list of canonical server paths to
     * team projects.
     *
     * @param teamProjectPaths
     *        the team projects (must not be <code>null</code>)
     * @return true if the set was modified, false if the set was not modified
     */
    public boolean set(final String[] teamProjectPaths) {
        Check.notNull(teamProjectPaths, "teamProjectPaths"); //$NON-NLS-1$

        synchronized (serverPaths) {
            // Hash set is fine here because we don't need a custom comparator
            // for an immutable copy
            final Set<String> copy = new HashSet<String>(serverPaths);

            serverPaths.clear();
            serverPaths.addAll(Arrays.asList(teamProjectPaths));

            return !serverPaths.equals(copy);
        }
    }
}
