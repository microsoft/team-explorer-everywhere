// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.soapextensions;

import com.microsoft.tfs.util.TypesafeEnum;

/**
 * Different types of pending change comparisons.
 *
 * @since TEE-SDK-10.1
 */
public final class PendingChangeComparatorType extends TypesafeEnum {
    private PendingChangeComparatorType(final int value) {
        super(value);
    }

    public final static PendingChangeComparatorType LOCAL_ITEM = new PendingChangeComparatorType(0);
    public final static PendingChangeComparatorType LOCAL_ITEM_REVERSE = new PendingChangeComparatorType(1);
    public final static PendingChangeComparatorType SERVER_ITEM = new PendingChangeComparatorType(2);
}
