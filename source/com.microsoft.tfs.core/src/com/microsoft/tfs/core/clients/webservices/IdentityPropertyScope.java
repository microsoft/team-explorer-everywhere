// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.webservices;

import com.microsoft.tfs.util.TypesafeEnum;

/**
 * The scope which a property will be scoped to.
 *
 * @threadsafety immutable
 */
public class IdentityPropertyScope extends TypesafeEnum {
    public static final IdentityPropertyScope NONE = new IdentityPropertyScope(0);
    public static final IdentityPropertyScope GLOBAL = new IdentityPropertyScope(1);
    public static final IdentityPropertyScope LOCAL = new IdentityPropertyScope(2);
    public static final IdentityPropertyScope BOTH = new IdentityPropertyScope(3);

    private IdentityPropertyScope(final int value) {
        super(value);
    }
}