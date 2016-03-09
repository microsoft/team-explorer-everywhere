// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.clc.options.shared;

import com.microsoft.tfs.client.clc.options.SingleValueOption;

/**
 * Accepts a build name (optionally with wildcards). Build names are called
 * build numbers in the core classes.
 */
public final class OptionBuildName extends SingleValueOption {
    public OptionBuildName() {
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
