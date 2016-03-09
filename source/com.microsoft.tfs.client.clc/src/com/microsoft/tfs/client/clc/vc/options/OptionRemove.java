// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.clc.vc.options;

import com.microsoft.tfs.client.clc.OptionsMap;
import com.microsoft.tfs.client.clc.options.SingleValueOption;

/**
 *         Used to remove a workspace from the local workspace cache.
 */
public final class OptionRemove extends SingleValueOption {
    public OptionRemove() {
        super();
    }

    @Override
    public String getSyntaxString() {
        return OptionsMap.getPreferredOptionPrefix() + getMatchedAlias() + ":<workspace1>[,<workspace2>,...]"; //$NON-NLS-1$
    }

    @Override
    protected String[] getValidOptionValues() {
        /*
         * null means that all values are permitted for this option.
         */
        return null;
    }
}
