// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.pendingcheckin.events;

import com.microsoft.tfs.core.clients.CoreClientEvent;
import com.microsoft.tfs.core.clients.versioncontrol.events.EventSource;
import com.microsoft.tfs.core.pendingcheckin.PendingCheckinPendingChanges;
import com.microsoft.tfs.util.Check;

/**
 * <p>
 * Event fired when the collection of affected team projects changes in a
 * {@link PendingCheckinPendingChanges} object.
 * </p>
 *
 * @since TEE-SDK-10.1
 * @threadsafety thread-safe
 */
public class AffectedTeamProjectsChangedEvent extends CoreClientEvent {
    private final String[] newTeamProjects;

    /**
     * Describes a change in affected team projects.
     *
     * @param newTeamProjects
     *        the new team project paths affected (must not be <code>null</code>
     *        )
     */
    public AffectedTeamProjectsChangedEvent(final EventSource source, final String[] newTeamProjects) {
        super(source);

        Check.notNull(newTeamProjects, "newTeamProjects"); //$NON-NLS-1$

        this.newTeamProjects = newTeamProjects;
    }

    /**
     * @return the new affected set of team project paths.
     */
    public String[] getNewTeamProjects() {
        return newTeamProjects;
    }
}
