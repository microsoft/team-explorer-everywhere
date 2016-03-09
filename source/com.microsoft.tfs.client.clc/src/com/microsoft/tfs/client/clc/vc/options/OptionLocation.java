// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.clc.vc.options;

import com.microsoft.tfs.client.clc.options.SingleValueOption;

public final class OptionLocation extends SingleValueOption {
    public static final String SERVER = "server"; //$NON-NLS-1$
    public static final String LOCAL = "local"; //$NON-NLS-1$

    public OptionLocation() {
        super();
    }

    @Override
    protected String[] getValidOptionValues() {
        return new String[] {
            SERVER,
            LOCAL
        };
    }
}
