// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.build.exceptions;

import java.text.MessageFormat;

import com.microsoft.tfs.core.Messages;
import com.microsoft.tfs.core.clients.build.IFailure;

/**
 * Exception for a build failure.
 *
 * @since TEE-SDK-10.1
 */
public class BuildFailureException extends BuildException {
    private final String teamproject;
    private final String name;
    private final IFailure failure;

    public BuildFailureException(final String teamproject, final String name, final IFailure failure) {
        super(MessageFormat.format(
            Messages.getString("BuildFailureException.MessageFormat"), //$NON-NLS-1$
            name,
            teamproject,
            failure.getMessage()));
        this.teamproject = teamproject;
        this.name = name;
        this.failure = failure;
    }

    /**
     * @return the teamproject
     */
    public String getTeamproject() {
        return teamproject;
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
