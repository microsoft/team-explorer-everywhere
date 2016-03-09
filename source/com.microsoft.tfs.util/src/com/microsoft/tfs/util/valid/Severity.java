// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.util.valid;

/**
 * <p>
 * {@link Severity} is an enum that represents the possible severities of
 * {@link IValidity}s or {@link IValidationMessage}s.
 * </p>
 *
 * <p>
 * Each severity has an integer priority. This priority can be used to compare
 * two priorities and see which is more important. A "worse" severity is more
 * important: for example, the {@link #ERROR} severity has a higher priority
 * than the {@link #WARNING} severity.
 * </p>
 *
 * @see IValidity
 * @see IValidationMessage
 */
public class Severity {
    /**
     * A {@link Severity} that indicates nothing is wrong.
     */
    public static final Severity OK = new Severity("OK", 0); //$NON-NLS-1$

    /**
     * A {@link Severity} that indicates a warning condition. This should be
     * used when validation has determined that a subject is not invalid, but is
     * in an advisory state.
     */
    public static final Severity WARNING = new Severity("WARNING", 1); //$NON-NLS-1$

    /**
     * A {@link Severity} that indicates an error condition. This should be used
     * when validation has determined that a subject is invalid.
     */
    public static final Severity ERROR = new Severity("ERROR", 2); //$NON-NLS-1$

    private final int priority;
    private final String type;

    private Severity(final String type, final int priority) {
        this.type = type;
        this.priority = priority;
    }

    /**
     * @return this {@link Severity}'s priority
     */
    public int getPriority() {
        return priority;
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return type;
    }
}
