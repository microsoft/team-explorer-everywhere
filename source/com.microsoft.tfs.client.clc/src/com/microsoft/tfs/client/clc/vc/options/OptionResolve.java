// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.clc.vc.options;

import com.microsoft.tfs.client.clc.OptionsMap;
import com.microsoft.tfs.client.clc.options.MultipleIntegerValueOption;

public class OptionResolve extends MultipleIntegerValueOption {
    @Override
    public String getSyntaxString() {
        return OptionsMap.getPreferredOptionPrefix() + getMatchedAlias() + ":" + "<workItemID>[,<workItemID>...]"; //$NON-NLS-1$ //$NON-NLS-2$
    }
}
