// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.framework.location;

import com.microsoft.tfs.core.TFSConnection;
import com.microsoft.tfs.util.BitField;

/**
 * Enumeration describing the connection options for a {@link TFSConnection}.
 * This determines the level of information obtained during a connect.
 *
 * @threadsafety thread-compatible
 * @since TEE-SDK-10.1
 */
public class ConnectOptions extends BitField {
    /**
     * Retrieve no optional data.
     */
    public static final ConnectOptions NONE = new ConnectOptions(0, "None"); //$NON-NLS-1$

    /**
     * Includes information about the services supplied in the serviceFilter
     * parameter.
     */
    public static final ConnectOptions INCLUDE_SERVICES = new ConnectOptions(1, "IncludeServices"); //$NON-NLS-1$

    public static ConnectOptions combine(final ConnectOptions[] changeTypes) {
        return new ConnectOptions(BitField.combine(changeTypes));
    }

    private ConnectOptions(final int flags, final String name) {
        super(flags);
        registerStringValue(getClass(), flags, name);
    }

    private ConnectOptions(final int flags) {
        super(flags);
    }

    public boolean containsAll(final ConnectOptions other) {
        return containsAllInternal(other);
    }

    public boolean contains(final ConnectOptions other) {
        return containsInternal(other);
    }

    public boolean containsAny(final ConnectOptions other) {
        return containsAnyInternal(other);
    }

    public ConnectOptions remove(final ConnectOptions other) {
        return new ConnectOptions(removeInternal(other));
    }

    public ConnectOptions retain(final ConnectOptions other) {
        return new ConnectOptions(retainInternal(other));
    }

    public ConnectOptions combine(final ConnectOptions other) {
        return new ConnectOptions(combineInternal(other));
    }

}
