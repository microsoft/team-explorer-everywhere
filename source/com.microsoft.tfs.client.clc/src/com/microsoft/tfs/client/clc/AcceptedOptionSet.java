// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.clc;

import java.util.ArrayList;
import java.util.Arrays;

import com.microsoft.tfs.util.Check;

/**
 * Contains the options (optional and required switches, as well as free
 * argument description string) a command accepts. A command must support at
 * lest one {@link AcceptedOptionSet}. This class is used to print a command's
 * help text as well as when parsing the user's command-line arguments.
 * <p>
 * The order of the options arrays submitted to the constructor is persisted for
 * use in help display.
 */
public class AcceptedOptionSet {
    private final ArrayList optionalOptions = new ArrayList();
    private final ArrayList requiredOptions = new ArrayList();
    private String freeArgumentsSyntax;

    public AcceptedOptionSet() {
    }

    public AcceptedOptionSet(final String freeArgumentsSyntax) {
        Check.notNull(freeArgumentsSyntax, "freeArgumentsSyntax"); //$NON-NLS-1$
        this.freeArgumentsSyntax = freeArgumentsSyntax;
    }

    /**
     * Construct and option set with the given syntax requirements.
     *
     * @param optionalOptions
     *        options which the user does not need to specify to satisty this
     *        set (not null).
     * @param freeArgumentsSyntax
     *        the free arguments this set supports (may be null or empty).
     */
    public AcceptedOptionSet(final Class[] optionalOptions, final String freeArgumentsSyntax) {
        Check.notNull(optionalOptions, "optionalOptions"); //$NON-NLS-1$
        Check.notNull(optionalOptions, "optionalOptions"); //$NON-NLS-1$

        this.freeArgumentsSyntax = freeArgumentsSyntax;
        this.optionalOptions.addAll(Arrays.asList(optionalOptions));
    }

    /**
     * Construct and option set with the given syntax requirements.
     *
     * @param optionalOptions
     *        options which the user does not need to specify to satisty this
     *        set (not null).
     * @param freeArgumentsSyntax
     *        the free arguments this set supports (may be null or empty).
     * @param requiredOptions
     *        options which the user MUST provide to satisfy this set (not
     *        null).
     */
    public AcceptedOptionSet(
        final Class[] optionalOptions,
        final String freeArgumentsSyntax,
        final Class[] requiredOptions) {
        Check.notNull(optionalOptions, "optionalOptions"); //$NON-NLS-1$
        Check.notNull(requiredOptions, "requiredOptions"); //$NON-NLS-1$

        this.freeArgumentsSyntax = freeArgumentsSyntax;
        this.optionalOptions.addAll(Arrays.asList(optionalOptions));
        this.requiredOptions.addAll(Arrays.asList(requiredOptions));
    }

    /**
     * Gets the free arguments syntax valid for this option set. May be null.
     *
     * @return the free-form string describing the free arguments syntax.
     */
    public String getFreeArgumentsSyntax() {
        return freeArgumentsSyntax;
    }

    /**
     * Get the options that are optional for this set. The order of the array
     * will match the order of the options provided to the constructor.
     *
     * @return the options that are optional for this set, never null but may be
     *         empty.
     */
    public Class[] getOptionalOptions() {
        return (Class[]) optionalOptions.toArray(new Class[0]);
    }

    /**
     * Get the options that are required for this set. The order of the array
     * will match the order of the options provided to the constructor.
     *
     * @return the options that are required for this set, never null but may be
     *         empty.
     */
    public Class[] getRequiredOptions() {
        return (Class[]) requiredOptions.toArray(new Class[0]);
    }
}
