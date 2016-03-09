// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.persistence;

import com.microsoft.tfs.util.TypesafeEnum;

public class PersistenceSecurity extends TypesafeEnum {
    public static final PersistenceSecurity PUBLIC = new PersistenceSecurity(0);
    public static final PersistenceSecurity PRIVATE = new PersistenceSecurity(1);

    private PersistenceSecurity(final int value) {
        super(value);
    }
}
