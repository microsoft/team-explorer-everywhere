// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.clc.vc.options;

import com.microsoft.tfs.client.clc.options.SingleValueOption;

/**
 *         Represents how to label children: merge will preserve the versions in
 *         an existing label of child items affected by a label. Replace will
 *         update the existing labels to point to the new version being labeled.
 */
public final class OptionChild extends SingleValueOption {
    public static final String FAIL = "fail"; //$NON-NLS-1$
    public static final String REPLACE = "replace"; //$NON-NLS-1$
    public static final String MERGE = "merge"; //$NON-NLS-1$

    public OptionChild() {
        super();
    }

    @Override
    protected String[] getValidOptionValues() {
        return new String[] {
            FAIL,
            REPLACE,
            MERGE
        };
    }
}
