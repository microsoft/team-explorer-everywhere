// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.clc.externaltools;

import java.text.MessageFormat;

import com.microsoft.tfs.client.clc.EnvironmentVariables;
import com.microsoft.tfs.client.clc.Messages;
import com.microsoft.tfs.client.clc.exceptions.CLCException;
import com.microsoft.tfs.core.externaltools.ExternalTool;
import com.microsoft.tfs.core.externaltools.validators.CompareToolValidator;
import com.microsoft.tfs.core.externaltools.validators.ExternalToolException;
import com.microsoft.tfs.core.externaltools.validators.ExternalToolValidator;
import com.microsoft.tfs.core.externaltools.validators.MergeToolValidator;
import com.microsoft.tfs.jni.PlatformMiscUtils;

public abstract class CLCTools {
    /**
     * Gets an {@link ExternalTool} by parsing the
     * {@link EnvironmentVariables#EXTERNAL_MERGE_COMMAND} environment variable.
     * Throws if the environment variable is not set or if the arguments are
     * invalid.
     *
     * @return the {@link ExternalTool}, never <code>null</code>
     * @throws CLCException
     *         if the environment variable was not set or was set to an empty
     *         string or had illegal substitutions
     */
    public static ExternalTool getMergeTool() throws CLCException {
        return getTool(EnvironmentVariables.EXTERNAL_MERGE_COMMAND, new MergeToolValidator());
    }

    /**
     * Gets an {@link ExternalTool} by parsing the
     * {@link EnvironmentVariables#EXTERNAL_DIFF_COMMAND} environment variable.
     * Throws if the environment variable is not set or if the arguments are
     * invalid.
     *
     * @return the {@link ExternalTool}, never <code>null</code>
     * @throws CLCException
     *         if the environment variable was not set or was set to an empty
     *         string or had illegal substitutions
     */
    public static ExternalTool getCompareTool() throws CLCException {
        return getTool(EnvironmentVariables.EXTERNAL_DIFF_COMMAND, new CompareToolValidator());
    }

    private static ExternalTool getTool(final String environmentVariable, final ExternalToolValidator validator)
        throws CLCException {
        final String value = PlatformMiscUtils.getInstance().getEnvironmentVariable(environmentVariable);

        if (value == null || value.length() == 0) {
            throw new CLCException(MessageFormat.format(
                Messages.getString("CLCTools.EnvironmentVariableNotSetFormat"), //$NON-NLS-1$
                environmentVariable));
        }

        final ExternalTool tool = new ExternalTool(value);

        try {
            validator.validate(tool);
        } catch (final ExternalToolException e) {
            throw new CLCException(
                MessageFormat.format(
                    Messages.getString("CLCTools.CorrectFollowingProblemsWithVariableFormat"), //$NON-NLS-1$
                    environmentVariable,
                    e.getLocalizedMessage()));
        }

        return tool;
    }
}
