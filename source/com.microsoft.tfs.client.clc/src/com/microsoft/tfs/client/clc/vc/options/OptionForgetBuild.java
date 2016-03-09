// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.clc.vc.options;

import com.microsoft.tfs.client.clc.OptionsMap;
import com.microsoft.tfs.client.clc.options.SingleValueOption;

public final class OptionForgetBuild extends SingleValueOption {
    public OptionForgetBuild() {
        super();
    }

    @Override
    public String getSyntaxString() {
        return OptionsMap.getPreferredOptionPrefix() + getMatchedAlias() + ":<buildName>"; //$NON-NLS-1$
    }

    @Override
    protected String[] getValidOptionValues() {
        /*
         * null means that all values are permitted for this option.
         */
        return null;
    }
}
