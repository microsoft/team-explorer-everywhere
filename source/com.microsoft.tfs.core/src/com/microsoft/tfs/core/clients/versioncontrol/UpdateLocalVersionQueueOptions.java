// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol;

import com.microsoft.tfs.util.BitField;

/**
 * Options to control where and how the local versions are updated.
 *
 * @threadsafety thread-safe
 * @since TEE-SDK-11.0
 */
public class UpdateLocalVersionQueueOptions extends BitField {
    public final static UpdateLocalVersionQueueOptions UPDATE_LOCAL =
        new UpdateLocalVersionQueueOptions(1, "UpdateLocal"); //$NON-NLS-1$

    public final static UpdateLocalVersionQueueOptions UPDATE_SERVER =
        new UpdateLocalVersionQueueOptions(2, "UpdateServer"); //$NON-NLS-1$

    public final static UpdateLocalVersionQueueOptions UPDATE_BOTH =
        new UpdateLocalVersionQueueOptions(UPDATE_LOCAL.combine(UPDATE_SERVER).toIntFlags(), "UpdateBoth"); //$NON-NLS-1$

    private UpdateLocalVersionQueueOptions(final int flags, final String name) {
        super(flags);

        registerStringValue(getClass(), flags, name);
    }

    private UpdateLocalVersionQueueOptions(final int flags) {
        super(flags);
    }

    public boolean containsAll(final UpdateLocalVersionQueueOptions other) {
        return containsAllInternal(other);
    }

    public boolean contains(final UpdateLocalVersionQueueOptions other) {
        return containsInternal(other);
    }

    public boolean containsAny(final UpdateLocalVersionQueueOptions other) {
        return containsAnyInternal(other);
    }

    public UpdateLocalVersionQueueOptions remove(final UpdateLocalVersionQueueOptions other) {
        return new UpdateLocalVersionQueueOptions(removeInternal(other));
    }

    public UpdateLocalVersionQueueOptions retain(final UpdateLocalVersionQueueOptions other) {
        return new UpdateLocalVersionQueueOptions(retainInternal(other));
    }

    public UpdateLocalVersionQueueOptions combine(final UpdateLocalVersionQueueOptions other) {
        return new UpdateLocalVersionQueueOptions(combineInternal(other));
    }

}