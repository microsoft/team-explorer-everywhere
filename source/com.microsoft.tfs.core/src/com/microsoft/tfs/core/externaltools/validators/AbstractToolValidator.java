// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.externaltools.validators;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import com.microsoft.tfs.core.Messages;
import com.microsoft.tfs.core.externaltools.ExternalTool;

/**
 * Contains methods for required and forbidden substitution string checking.
 * Derived classes simply implement {@link #getForbiddenSubstitutions()} and
 * {@link #getRequiredSubstitutions()}.
 *
 * @since TEE-SDK-10.1
 * @threadsafety immutable
 */
public abstract class AbstractToolValidator implements ExternalToolValidator {
    protected AbstractToolValidator() {
    }

    /**
     * Gets the substitution strings which are not allowed in arguments for this
     * external tool.
     *
     * @return the substitution strings ("%1", etc.) which are not allowed in
     *         arguments for this external tool
     */
    protected abstract String[] getForbiddenSubstitutions();

    /**
     * Gets the substitution strings which are not required in arguments for
     * this external tool.
     *
     * @return the substitution strings ("%1", etc.) which are required in
     *         arguments for this external tool
     */
    protected abstract String[] getRequiredSubstitutions();

    /**
     * {@inheritDoc}
     */
    @Override
    public void validate(final ExternalTool externalTool) throws ExternalToolException {
        final String[] arguments = externalTool.getArguments();

        /*
         * Make sure the arguments do not contain any of the forbidden
         * substitutions.
         */
        final String[] forbiddenSubstitutions = getForbiddenSubstitutions();
        final List foundForbidden = new ArrayList();

        for (int i = 0; i < forbiddenSubstitutions.length; i++) {
            for (int j = 0; j < arguments.length; j++) {
                if (arguments[j].indexOf(forbiddenSubstitutions[i]) != -1) {
                    foundForbidden.add(forbiddenSubstitutions[i]);
                }
            }
        }

        if (foundForbidden.size() > 0) {
            final StringBuffer subs = new StringBuffer();
            for (int i = 0; i < foundForbidden.size(); i++) {
                if (i > 0) {
                    subs.append(", "); //$NON-NLS-1$
                }

                subs.append(foundForbidden.get(i));
            }

            throw new ExternalToolException(
                MessageFormat.format(
                    Messages.getString("AbstractToolValidator.FollowingNotAllowedFormat"), //$NON-NLS-1$
                    subs.toString()));
        }

        /*
         * Make sure the arguments do contain all of the required substitutions.
         */

        final String[] required = getRequiredSubstitutions();
        final List missingRequired = new ArrayList();

        for (int i = 0; i < required.length; i++) {
            boolean found = false;

            for (int j = 0; j < arguments.length; j++) {
                if (arguments[j].indexOf(required[i]) != -1) {
                    found = true;
                }
            }

            if (found == false) {
                missingRequired.add(required[i]);
            }
        }

        if (missingRequired.size() > 0) {
            final StringBuffer subs = new StringBuffer();

            for (int i = 0; i < missingRequired.size(); i++) {
                if (i > 0) {
                    subs.append(", "); //$NON-NLS-1$
                }

                subs.append(missingRequired.get(i));
            }

            throw new ExternalToolException(
                MessageFormat.format(
                    Messages.getString("AbstractToolValidator.FollowingRequiredFormat"), //$NON-NLS-1$
                    subs.toString()));
        }
    }
}
