// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.clc.vc.options;

import com.microsoft.tfs.client.clc.options.SingleValueOption;

public class OptionPermission extends SingleValueOption {
    public static final String PRIVATE = "Private"; //$NON-NLS-1$
    public static final String PUBLICLIMITED = "PublicLimited"; //$NON-NLS-1$
    public static final String PUBLIC = "Public"; //$NON-NLS-1$

    public OptionPermission() {
        super();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected String[] getValidOptionValues() {
        return new String[] {
            PRIVATE,
            PUBLICLIMITED,
            PUBLIC
        };
    }

}
