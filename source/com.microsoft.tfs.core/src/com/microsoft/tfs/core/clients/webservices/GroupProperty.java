// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.webservices;

import com.microsoft.tfs.util.TypesafeEnum;

/**
 * @threadsafety immutable
 * @since TEE-SDK-11.0
 */
public class GroupProperty extends TypesafeEnum {
    public static final GroupProperty NONE = new GroupProperty(0);

    /**
     * The name of an application group
     */
    public static final GroupProperty NAME = new GroupProperty(1);

    /**
     * The description of an application group
     */
    public static final GroupProperty DESCRIPTION = new GroupProperty(2);

    protected GroupProperty(final int value) {
        super(value);
    }
}
