// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.clc.options.shared;

import com.microsoft.tfs.client.clc.options.SingleValueOption;

public final class OptionUser extends SingleValueOption {

    public OptionUser() {
        super();
    }

    @Override
    public String[] getValidOptionValues() {
        /*
         * null means that all values are permitted for this option.
         */
        return null;
    }
}
