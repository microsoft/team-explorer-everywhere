// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.clc.vc.options;

import com.microsoft.tfs.util.TypesafeEnum;

/**
 *         Represents the types of values an option expects.
 */
public final class OptionValueStyle extends TypesafeEnum {
    private OptionValueStyle(final int value) {
        super(value);
    }

    public final static OptionValueStyle NONE = new OptionValueStyle(0);
    public final static OptionValueStyle SINGLE_STRING = new OptionValueStyle(1);
    public final static OptionValueStyle USERNAME_PASSWORD = new OptionValueStyle(2);
    public final static OptionValueStyle MULTIPLE = new OptionValueStyle(3);
}
