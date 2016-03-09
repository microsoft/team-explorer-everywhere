// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.build.exceptions;

import java.text.MessageFormat;

import com.microsoft.tfs.core.Messages;
import com.microsoft.tfs.core.clients.build.IFailure;

/**
 * Exception raised when an attempt to query build agents failed.
 *
 * @since TEE-SDK-10.1
 */
public class BuildAgentFailureException extends BuildException {
    private final String teamProject;
    private final String agentName;
    private final IFailure failure;

    public BuildAgentFailureException(final String teamProject, final String agentName, final IFailure failure) {
        super(MessageFormat.format(
            Messages.getString("BuildAgentFailureException.MessageFormat"), //$NON-NLS-1$
            agentName,
            teamProject,
            failure.getMessage()));

        this.teamProject = teamProject;
        this.agentName = agentName;
        this.failure = failure;
    }

    /**
     * @return the teamProject
     */
    public String getTeamProject() {
        return teamProject;
    }

    /**
     * @return the agentName
     */
    public String getAgentName() {
        return agentName;
    }

    /**
     * @return the failure
     */
    public IFailure getFailure() {
        return failure;
    }

}
