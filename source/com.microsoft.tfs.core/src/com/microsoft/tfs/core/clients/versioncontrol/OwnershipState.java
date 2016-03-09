// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol;

import com.microsoft.tfs.util.TypesafeEnum;

public class OwnershipState extends TypesafeEnum {
    public static final OwnershipState UNKNOWN = new OwnershipState(0);
    public static final OwnershipState OWNED_BY_AUTHORIZED_USER = new OwnershipState(1);
    public static final OwnershipState NOT_OWNED_BY_AUTHORIZED_USER = new OwnershipState(2);

    private OwnershipState(final int value) {
        super(value);
    }
}
