// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol;

import com.microsoft.tfs.util.TypesafeEnum;

public class ResolveErrorOptions extends TypesafeEnum {
    public static final ResolveErrorOptions NONE = new ResolveErrorOptions(0);
    public static final ResolveErrorOptions THROW_ON_ERROR = new ResolveErrorOptions(1);
    public static final ResolveErrorOptions RAISE_WARNINGS_FOR_ERROR = new ResolveErrorOptions(2);

    private ResolveErrorOptions(final int value) {
        super(value);
    }
}
