// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol;

import com.microsoft.tfs.util.TypesafeEnum;

/**
 * Represents the status of an item during an operation. Thes types of
 * operations are set in events that are fired by the client, so that the
 * consumer of the event can determine what kind of processing was going on when
 * the event was fired.
 *
 * @threadsafety immutable
 * @since TEE-SDK-10.1
 */
public final class OperationStatus extends TypesafeEnum {
    private OperationStatus(final int value) {
        super(value);
    }

    /*
     * The values here must start with 0 and match the order of Visual Studio's
     * implementation. The integer values are sent to/from the server to
     * identify conflict reasons.
     */

    public final static OperationStatus CONFLICT = new OperationStatus(0);
    public final static OperationStatus SOURCE_WRITABLE = new OperationStatus(1);
    public final static OperationStatus TARGET_LOCAL_PENDING = new OperationStatus(2);
    public final static OperationStatus TARGET_WRITABLE = new OperationStatus(3);
    public final static OperationStatus GETTING = new OperationStatus(4);
    public final static OperationStatus REPLACING = new OperationStatus(5);
    public final static OperationStatus DELETING = new OperationStatus(6);
    public final static OperationStatus SOURCE_DIRECTORY_NOT_EMPTY = new OperationStatus(7);
    public final static OperationStatus TARGET_IS_DIRECTORY = new OperationStatus(8);
    public final static OperationStatus UNABLE_TO_REFRESH = new OperationStatus(9);
}
