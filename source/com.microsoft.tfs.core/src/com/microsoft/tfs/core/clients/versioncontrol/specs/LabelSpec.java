// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.specs;

import java.text.MessageFormat;

import com.microsoft.tfs.core.Messages;
import com.microsoft.tfs.core.clients.versioncontrol.VersionControlConstants;
import com.microsoft.tfs.core.clients.versioncontrol.path.ServerPath;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.FileHelpers;

/**
 * A label specification. Each label specification has a required label name,
 * but the scope (a server path) is optional and is represented as
 * <code>null</code> in this object if it was not specified. When a label spec
 * with a <code>null</code> scope is sent to the server, the server will default
 * the scope to the team project containing the labeled item.
 *
 * @since TEE-SDK-10.1
 */
public final class LabelSpec {
    private static final String LABEL_AND_SCOPE_SEPARATOR = "@"; //$NON-NLS-1$

    private final String label;
    private final String scope;

    /**
     * Creates a label spec.
     *
     * @param label
     *        the name of the label (must not be <code>null</code> or empty).
     * @param scope
     *        the scope (repository folder path) where the label resides (may be
     *        <code>null</code> to indiciate the default scope).
     */
    public LabelSpec(final String label, final String scope) {
        Check.notNullOrEmpty(label, "label"); //$NON-NLS-1$

        this.label = label;
        this.scope = scope;
    }

    /**
     * Parses the values from the given spec string, using the given default
     * scope if none can be parsed.
     *
     * @param specString
     *        the spec string to parse into this object (must not be
     *        <code>null</code> or empty)
     * @param defaultScope
     *        the scope to use if the spec string does not specify one (may be
     *        {@link NullPointerException} to indicate the default scope).
     * @param permitWildcardsInLabel
     *        whether to allow wildcards in the label names
     * @throws LabelSpecParseException
     *         if an error occurred parsing the spec string
     * @return a {@link LabelSpec} with the parsed values
     */
    public static LabelSpec parse(
        final String specString,
        final String defaultScope,
        final boolean permitWildcardsInLabel) throws LabelSpecParseException {
        Check.notNullOrEmpty(specString, "specString"); //$NON-NLS-1$

        final int index = specString.indexOf(LABEL_AND_SCOPE_SEPARATOR);

        String label;
        String scope;

        if (index < 0) {
            label = specString;
            scope = defaultScope;
        } else {
            if (index == 0) {
                throw new LabelSpecParseException(Messages.getString("LabelSpec.LabelSpecMustIncludeName")); //$NON-NLS-1$
            }

            label = specString.substring(0, index);

            if (index + 1 == specString.length()) {
                scope = defaultScope;
            } else {
                scope = specString.substring(index + 1);
            }
        }

        /*
         * Validate the label name.
         */
        if (isValidLabelName(label, permitWildcardsInLabel) == false) {
            if (permitWildcardsInLabel) {
                throw new LabelSpecParseException(
                    MessageFormat.format(
                        Messages.getString("LabelSpec.LabelNameIsNotValidMayContainWildcardsFormat"), //$NON-NLS-1$
                        label));
            } else {
                throw new LabelSpecParseException(
                    MessageFormat.format(
                        Messages.getString("LabelSpec.LabelNameIsNotValidMayNotContainWildcardsFormat"), //$NON-NLS-1$
                        label));
            }
        }

        /*
         * Validate the scope of the labels.
         */
        if (scope != null && isValidScope(scope) == false) {
            /*
             * We got here because the scope was invalid. We need to figure out
             * why, and let the user know.
             */
            if (ServerPath.isWildcard(scope)) {
                throw new LabelSpecParseException(Messages.getString("LabelSpec.WildcardsAreNotAllowedInLabelScopes")); //$NON-NLS-1$
            }

            /*
             * If we got here, it was some other problem.
             */
            throw new LabelSpecParseException(
                MessageFormat.format(Messages.getString("LabelSpec.LabelScopeIsNotValidFormat"), scope)); //$NON-NLS-1$
        }

        return new LabelSpec(label, scope);
    }

    /**
     * Validates the scope string. A scope may be null, or must be a server path
     * without wildcards.
     *
     * @param scope
     *        the scope string to validate (may be <code>null</code>)
     * @return true if the scope string is valid, false if it is not valid.
     */
    private static boolean isValidScope(final String scope) {
        return (scope == null) || (ServerPath.isServerPath(scope) && ServerPath.isWildcard(scope) == false);
    }

    /**
     * Validates a label name. A valid label name is not null, is a valid server
     * path, and may contain wildcards (or not) depending on the given boolean
     * parameter.
     *
     * @param labelName
     *        the name to validate (may be <code>null</code>)
     * @param permitWildcards
     *        whether to permit wildcard characters in the label name
     * @return true if the label name is valid, falise if it is invalid
     */
    private static boolean isValidLabelName(final String labelName, final boolean permitWildcards) {
        if (labelName == null) {
            return false;
        }

        /*
         * The length must be under or equal to the maximum.
         */
        if (labelName.length() > VersionControlConstants.MAX_LABEL_NAME_SIZE) {
            return false;
        }

        /*
         * The label name can't contain any separators. They should have already
         * been parsed.
         */
        if (labelName.indexOf(LABEL_AND_SCOPE_SEPARATOR) != -1) {
            return false;
        }

        /*
         * Now we just have to test the characters...
         */
        return FileHelpers.isValidNTFSFileName(labelName, permitWildcards);
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#toString()
     */
    @Override
    public synchronized String toString() {
        return label + LABEL_AND_SCOPE_SEPARATOR + scope;
    }

    /**
     * @return Returns the label.
     */
    public String getLabel() {
        return label;
    }

    /**
     * @return Returns the scope, which may be null to indicate the default
     *         scope.
     */
    public String getScope() {
        return scope;
    }
}
