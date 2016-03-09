// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.clc.options.shared;

import com.microsoft.tfs.client.clc.options.SingleValueOption;

public final class OptionFormat extends SingleValueOption {
    public static final String BRIEF = "brief"; //$NON-NLS-1$
    public static final String DETAILED = "detailed"; //$NON-NLS-1$
    public static final String XML = "xml"; //$NON-NLS-1$

    public OptionFormat() {
        super();
    }

    @Override
    protected String[] getValidOptionValues() {
        return new String[] {
            BRIEF,
            DETAILED,
            XML
        };
    }
}
