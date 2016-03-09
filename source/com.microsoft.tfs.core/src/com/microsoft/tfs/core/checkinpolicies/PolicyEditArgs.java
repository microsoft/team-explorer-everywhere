// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.checkinpolicies;

import com.microsoft.tfs.core.clients.versioncontrol.TeamProject;
import com.microsoft.tfs.util.Check;

/**
 * <p>
 * {@link PolicyEditArgs} contains information that is passed to checkin
 * policies when they are edited. This information includes some edit-specific
 * values and any number of properties available in a context object. The
 * contextual values are stored as keyed properties and are defined by the
 * execution environment that is hosting the checkin policy.
 * </p>
 * <p>
 * For example, the Plug-in for Eclipse and Explorer will put an SWT Shell
 * object into the context that can be used to raise UI.
 * </p>
 *
 * @since TEE-SDK-10.1
 * @threadsafety immutable
 */
public class PolicyEditArgs {
    private final boolean isNew;
    private final TeamProject teamProject;
    private final PolicyContext context;

    /**
     * Creates arguments with all required (always available) items set.
     *
     * @param isNew
     *        true if this edit action is for a newly-created (installed or
     *        added to a Team Project) policy, false if it is for an existing
     *        (already installed) policy.
     * @param teamProject
     *        the Team Project where this policy is being configured (must not
     *        be <code>null</code>)
     */
    public PolicyEditArgs(final boolean isNew, final TeamProject teamProject) {
        super();

        Check.notNull(teamProject, "teamProject"); //$NON-NLS-1$

        this.isNew = isNew;
        this.teamProject = teamProject;
        context = new PolicyContext();
    }

    /**
     * @return <code>true</code> if this edit action is for a newly-created
     *         (installed or added to a Team Project) policy, <code>false</code>
     *         if it is for an existing (already installed) policy
     */
    public boolean isNew() {
        return isNew;
    }

    /**
     * @return the Team Project where this policy is being configured (must not
     *         be <code>null</code>)
     */
    public TeamProject getTeamProject() {
        return teamProject;
    }

    /**
     * @return the context that contains optional values (must not be
     *         <code>null</code>)
     */
    public PolicyContext getContext() {
        return context;
    }
}
