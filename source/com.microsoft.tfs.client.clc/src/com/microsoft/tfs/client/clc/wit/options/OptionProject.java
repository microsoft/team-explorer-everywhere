// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.clc.wit.options;

import com.microsoft.tfs.client.clc.options.SingleValueOption;

public class OptionProject extends SingleValueOption {
    @Override
    protected String[] getValidOptionValues() {
        return null; // accept all values (team project name will be validated
                     // by the command)
    }
}
