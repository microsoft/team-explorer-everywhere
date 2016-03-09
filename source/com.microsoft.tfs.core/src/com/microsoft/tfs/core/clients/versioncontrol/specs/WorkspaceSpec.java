// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.specs;

import java.text.MessageFormat;

import com.microsoft.tfs.core.Messages;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Workspace;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.FileHelpers;

/**
 * Describes a {@link Workspace} by name and owner.
 *
 * @since TEE-SDK-10.1
 * @threadsafety immutable
 */
public final class WorkspaceSpec {
    private final String workspaceName;
    private final String owner;

    private final static char WORKSPACE_AND_OWNER_SEPARATOR = ';';

    /**
     * Constructs an {@link WorkspaceSpec}.
     *
     * @param workspaceName
     *        the name of the workspace (must not be <code>null</code>)
     * @param owner
     *        the owner's unique name or display name (may be <code>null</code>
     *        ).
     *
     */
    public WorkspaceSpec(final String workspaceName, final String owner) {
        Check.notNullOrEmpty(workspaceName, "workspaceName"); //$NON-NLS-1$

        this.workspaceName = workspaceName;
        this.owner = owner;
    }

    /**
     * Parse the values from the given spec string into this instance, using the
     * given fallback owner if the owner isn't part of the combined spec.
     *
     * {@link WorkspaceSpecParseException} is thrown if the workspace name or
     * owner contains wildcard characters.
     *
     * @param specString
     *        the workspace spec string including the name of the workspace, and
     *        possible the owner.
     * @param fallbackOwner
     *        the owner username to use if the combined workspace spec is
     *        missing one.
     * @throws WorkspaceSpecParseException
     *         if the string cannot be parsed into a WorkspaceSpec.
     */
    public static WorkspaceSpec parse(final String specString, final String fallbackOwner)
        throws WorkspaceSpecParseException {
        return parse(specString, fallbackOwner, false);
    }

    /**
     * Parse the values from the given spec string into this instance, using the
     * given fallback owner if the owner isn't part of the combined spec.
     *
     * @param specString
     *        the workspace spec string including the name of the workspace, and
     *        possible the owner.
     * @param fallbackOwner
     *        the owner username to use if the combined workspace spec is
     *        missing one.
     * @param permitWildcards
     *        if <code>true</code>, the workspace name and owner may contain
     *        wildcards, if <code>false</code>
     *        {@link WorkspaceSpecParseException} is thrown if the name or owner
     *        contains wildcards
     * @throws WorkspaceSpecParseException
     *         if the string cannot be parsed into a WorkspaceSpec.
     */
    public static WorkspaceSpec parse(
        final String specString,
        final String fallbackOwner,
        final boolean permitWildcards) throws WorkspaceSpecParseException {
        // Parse the spec out.
        final int sep = specString.indexOf(WORKSPACE_AND_OWNER_SEPARATOR);

        if (sep == -1) {
            if (FileHelpers.isValidNTFSFileName(specString, permitWildcards) == false) {
                throw new WorkspaceSpecParseException(
                    MessageFormat.format(
                        Messages.getString("WorkspaceSpec.WorkspaceNamecontainsInvalidCharactersFormat"), //$NON-NLS-1$
                        specString));
            }

            return new WorkspaceSpec(specString, fallbackOwner);
        } else {
            String workspaceName;

            if (sep == 0) {
                throw new WorkspaceSpecParseException(
                    MessageFormat.format(
                        Messages.getString("WorkspaceSpec.CouldNotParseAWorkspaceNameFromSpecFormat"), //$NON-NLS-1$
                        specString));
            } else {
                /*
                 * Make sure the name is valid.
                 */
                workspaceName = specString.substring(0, sep);

                if (FileHelpers.isValidNTFSFileName(workspaceName, permitWildcards) == false) {
                    throw new WorkspaceSpecParseException(
                        MessageFormat.format(
                            Messages.getString("WorkspaceSpec.WorkspaceNamecontainsInvalidCharactersFormat"), //$NON-NLS-1$
                            workspaceName));
                }
            }

            String owner;

            if (sep + 1 == specString.length()) {
                owner = fallbackOwner;
            } else {
                owner = specString.substring(sep + 1);
            }

            return new WorkspaceSpec(workspaceName, owner);
        }
    }

    public String getName() {
        return workspaceName;
    }

    public String getOwner() {
        return owner;
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#toString()
     */
    @Override
    public synchronized String toString() {
        if (owner != null) {
            return workspaceName + WORKSPACE_AND_OWNER_SEPARATOR + owner;
        } else {
            return workspaceName;
        }
    }
}
