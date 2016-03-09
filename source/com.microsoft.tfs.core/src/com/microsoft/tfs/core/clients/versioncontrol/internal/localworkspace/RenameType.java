// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.internal.localworkspace;

import com.microsoft.tfs.util.TypesafeEnum;

public class RenameType extends TypesafeEnum {
    public static final RenameType ADDITIVE = new RenameType(0);
    public static final RenameType SUBTRACTIVE = new RenameType(1);

    private RenameType(final int value) {
        super(value);
    }
}
