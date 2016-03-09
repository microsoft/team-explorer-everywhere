// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.path;

import com.microsoft.tfs.util.TypesafeEnum;

/**
 * Describes item path validation errors.
 */
public class ItemValidationError extends TypesafeEnum {
    public static final ItemValidationError NONE = new ItemValidationError(0);
    public static final ItemValidationError WILDCARD_NOT_ALLOWED = new ItemValidationError(1);
    public static final ItemValidationError REPOSITORY_PATH_TOO_LONG = new ItemValidationError(2);
    public static final ItemValidationError LOCAL_ITEM_REQUIRED = new ItemValidationError(3);

    private ItemValidationError(final int value) {
        super(value);
    }
}