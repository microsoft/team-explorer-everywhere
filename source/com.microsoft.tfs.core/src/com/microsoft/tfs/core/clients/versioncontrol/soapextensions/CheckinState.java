// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.soapextensions;

import java.util.HashMap;
import java.util.Map;

import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.TypesafeEnum;

/**
 * Represents checkin state.
 *
 * @threadsafety immutable
 * @since TEE-SDK-11.0
 */
public class CheckinState extends TypesafeEnum {
    private static final Map<Integer, CheckinState> VALUE_MAP = new HashMap<Integer, CheckinState>();

    /**
     * Default initial state.
     */
    public final static CheckinState UNKNOWN = new CheckinState(0);

    /**
     * Checkin is being paged up to the server.
     */
    public final static CheckinState PAGING = new CheckinState(1);

    /**
     * Checkin operation was commited.
     */
    public final static CheckinState COMMITTED = new CheckinState(2);

    /**
     * CheckIn() did not throw an exception, but nothing was checked in (e.g.
     * all pending changes undone).
     */
    public final static CheckinState FAILED = new CheckinState(3);

    private CheckinState(final int value) {
        super(value);

        final Integer key = new Integer(value);
        Check.isTrue(VALUE_MAP.containsKey(key), "duplicate key"); //$NON-NLS-1$
        VALUE_MAP.put(key, this);
    }

    /**
     * Returns the {@link CheckinState} for the specified integer value.
     *
     * @param value
     *        The integer value.
     * @return The {@link CheckinState} corresponding to this value, or
     *         <code>null</code> if none matched
     */
    public static CheckinState fromInteger(final int value) {
        final Integer key = new Integer(value);
        Check.isTrue(VALUE_MAP.containsKey(key), "VALUE_MAP.containsKey(key)"); //$NON-NLS-1$
        return VALUE_MAP.get(key);
    }
}
