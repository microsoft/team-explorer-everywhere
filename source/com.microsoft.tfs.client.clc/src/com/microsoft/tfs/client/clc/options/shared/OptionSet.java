// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.clc.options.shared;

import com.microsoft.tfs.client.clc.options.SingleValueOption;

public final class OptionSet extends SingleValueOption {
    public OptionSet() {
        super();
    }

    /*
     * (non-Javadoc)
     *
     * @see com.microsoft.tfs.client.clc.options.SingleValueOption#
     * getValidOptionValues ()
     */
    @Override
    protected String[] getValidOptionValues() {
        return null;
    }
}
