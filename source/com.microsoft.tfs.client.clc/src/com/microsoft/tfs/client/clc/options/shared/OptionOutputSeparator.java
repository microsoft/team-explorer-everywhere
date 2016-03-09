// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.clc.options.shared;

import com.microsoft.tfs.client.clc.options.SingleValueOption;

/**
 * Defines the string (printed on its own line) to use to separate the output of
 * a one command from the next when multiple commands are run from a command
 * file.
 */
public final class OptionOutputSeparator extends SingleValueOption {
    public OptionOutputSeparator() {
        super();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected String[] getValidOptionValues() {
        /*
         * null means that all values are permitted for this option.
         */
        return null;
    }
}
