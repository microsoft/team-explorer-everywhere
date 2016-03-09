// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.offline;

import com.microsoft.tfs.util.TypesafeEnum;

/**
 * <p>
 * Enumerates the method for detecting offline changes.
 * </p>
 *
 * @since TEE-SDK-10.1
 * @threadsafety immutable
 */
public class OfflineSynchronizerMethod extends TypesafeEnum {
    public final static OfflineSynchronizerMethod WRITABLE_FILES = new OfflineSynchronizerMethod(1);
    public final static OfflineSynchronizerMethod MD5_HASH = new OfflineSynchronizerMethod(2);

    private OfflineSynchronizerMethod(final int value) {
        super(value);
    }
}
