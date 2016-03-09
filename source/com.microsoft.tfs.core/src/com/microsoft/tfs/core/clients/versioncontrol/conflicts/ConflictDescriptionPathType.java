// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.conflicts;

import com.microsoft.tfs.util.TypesafeEnum;

/**
 * @since TEE-SDK-10.1
 */
public class ConflictDescriptionPathType extends TypesafeEnum {
    public static final ConflictDescriptionPathType BASE = new ConflictDescriptionPathType(0);
    public static final ConflictDescriptionPathType SOURCE = new ConflictDescriptionPathType(1);
    public static final ConflictDescriptionPathType TARGET = new ConflictDescriptionPathType(2);

    public ConflictDescriptionPathType(final int value) {
        super(value);
    }
}
