// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.clc.vc.options;

import com.microsoft.tfs.client.clc.OptionsMap;
import com.microsoft.tfs.client.clc.options.SingleValueOption;

public final class OptionUpdateUserName extends SingleValueOption {
    public OptionUpdateUserName() {
        super();
    }

    @Override
    protected String[] getValidOptionValues() {
        return null;
    }

    @Override
    public String getSyntaxString() {
        return OptionsMap.getPreferredOptionPrefix() + getMatchedAlias() + ":<user@domain>|<domain\\user>"; //$NON-NLS-1$
    }
}
