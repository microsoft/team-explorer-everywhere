// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.externaltools.validators;

/**
 * <p>
 * Validates arguments for compare tools.
 * </p>
 *
 * @since TEE-SDK-10.1
 * @threadsafety immutable
 */
public final class CompareToolValidator extends AbstractToolValidator {
    public CompareToolValidator() {
        super();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected String[] getForbiddenSubstitutions() {
        return new String[] {
            "%3", //$NON-NLS-1$
            "%4", //$NON-NLS-1$
            "%8" //$NON-NLS-1$
        };
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected String[] getRequiredSubstitutions() {
        return new String[] {
            "%1", //$NON-NLS-1$
            "%2" //$NON-NLS-1$
        };
    }
}
