// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.build.exceptions;

import java.text.MessageFormat;

import com.microsoft.tfs.core.Messages;
import com.microsoft.tfs.core.clients.build.IFailure;

/**
 * Exception thrown when failure encountered retrieving the build definition
 * from the server.
 *
 * @since TEE-SDK-10.1
 */
public class BuildDefinitionFailureException extends BuildException {
    private final String teamProject;
    private final String name;
    private final IFailure failure;

    /**
     * Exception thrown when failure encountered retrieving the build definition
     * from the server.
     *
     * @param teamProject
     *        The team project for which the build definition was being
     *        retrieved.
     * @param name
     *        The build definition name.
     * @param failure
     *        the failure encountered.
     */
    public BuildDefinitionFailureException(final String teamProject, final String name, final IFailure failure) {
        super(MessageFormat.format(
            Messages.getString("BuildDefinitionFailureException.MessageFormat"), //$NON-NLS-1$
            name,
            teamProject,
            failure.getMessage()));

        this.teamProject = teamProject;
        this.name = name;
        this.failure = failure;
    }

    /**
     * @return the teamProject
     */
    public String getTeamProject() {
        return teamProject;
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @return the failure
     */
    public IFailure getFailure() {
        return failure;
    }

}
