// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.pendingcheckin;

import com.microsoft.tfs.util.BitField;

/**
 * <p>
 * A set of boolean options which determine which parts of the check-in are
 * evaluated.
 * </p>
 *
 * @since TEE-SDK-10.1
 * @threadsafety immutable
 */
public final class CheckinEvaluationOptions extends BitField {
    /**
     * Do not evaluate any part of the check-in.
     */
    public static final CheckinEvaluationOptions NONE = new CheckinEvaluationOptions(0, "None"); //$NON-NLS-1$

    /**
     * Evaluate the check-in notes.
     */
    public static final CheckinEvaluationOptions NOTES = new CheckinEvaluationOptions(1, "Notes"); //$NON-NLS-1$

    /**
     * Evaluate check-in policies.
     */
    public static final CheckinEvaluationOptions POLICIES = new CheckinEvaluationOptions(2, "Policies"); //$NON-NLS-1$

    /**
     * Check for version control conflicts.
     */
    public static final CheckinEvaluationOptions CONFLICTS = new CheckinEvaluationOptions(4, "Conflicts"); //$NON-NLS-1$

    /**
     * Check all parts of the check-in.
     */
    public static final CheckinEvaluationOptions ALL = new CheckinEvaluationOptions(7, "All"); //$NON-NLS-1$

    public static CheckinEvaluationOptions combine(final CheckinEvaluationOptions[] changeTypes) {
        return new CheckinEvaluationOptions(BitField.combine(changeTypes));
    }

    private CheckinEvaluationOptions(final int flags, final String name) {
        super(flags);
        registerStringValue(getClass(), flags, name);
    }

    private CheckinEvaluationOptions(final int flags) {
        super(flags);
    }

    public boolean containsAll(final CheckinEvaluationOptions other) {
        return containsAllInternal(other);
    }

    public boolean contains(final CheckinEvaluationOptions other) {
        return containsInternal(other);
    }

    public boolean containsAny(final CheckinEvaluationOptions other) {
        return containsAnyInternal(other);
    }

    public CheckinEvaluationOptions remove(final CheckinEvaluationOptions other) {
        return new CheckinEvaluationOptions(removeInternal(other));
    }

    public CheckinEvaluationOptions retain(final CheckinEvaluationOptions other) {
        return new CheckinEvaluationOptions(retainInternal(other));
    }

    public CheckinEvaluationOptions combine(final CheckinEvaluationOptions other) {
        return new CheckinEvaluationOptions(combineInternal(other));
    }
}
