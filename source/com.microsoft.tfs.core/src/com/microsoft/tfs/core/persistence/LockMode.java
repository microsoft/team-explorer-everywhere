// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.persistence;

import com.microsoft.tfs.util.TypesafeEnum;

/**
 * Defines the lock modes used for serialization and deserialization with
 * {@link PersistenceStore}.
 *
 * @since TEE-SDK-10.1
 * @threadsafety immutable
 */
public class LockMode extends TypesafeEnum {
    private LockMode(final int value) {
        super(value);
    }

    /**
     * Do not perform any locking. Items are opened for read or write without
     * regard to concurrent access by other processes.
     */
    public static final LockMode NONE = new LockMode(0);

    /**
     * Wait forever to acquire the lock (or until the thread is interrupted).
     */
    public static final LockMode WAIT_FOREVER = new LockMode(1);

    /**
     * Attempt to acquire the lock but return immediately if it could not be
     * acquired.
     */
    public static final LockMode NO_WAIT = new LockMode(2);
}
