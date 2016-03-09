// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol;

import java.util.HashMap;
import java.util.Map;

import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.TypesafeEnum;

/**
 * <p>
 * Indicates the location where data (pending changes, local versions) for this
 * workspace are stored.
 * </p>
 *
 * @threadsafety immutable
 */
public class WorkspaceLocation extends TypesafeEnum {
    private static final Map<Integer, WorkspaceLocation> VALUE_MAP = new HashMap<Integer, WorkspaceLocation>();

    /**
     * Indicates a server-side (traditional) workspace, backwards compatible
     * with TFS 2005-2010.
     */
    public static final WorkspaceLocation SERVER = new WorkspaceLocation(0);

    /**
     * Indicates a local workspace, with pending changes and local version data
     * stored on the client.
     */
    public static final WorkspaceLocation LOCAL = new WorkspaceLocation(1);

    private WorkspaceLocation(final int value) {
        super(value);

        final Integer key = new Integer(value);
        Check.isTrue(!VALUE_MAP.containsKey(key), "duplicate key"); //$NON-NLS-1$
        VALUE_MAP.put(key, this);
    }

    /**
     * Returns the {@link WorkspaceLocation} for the specified integer value.
     *
     * @param value
     *        The integer value.
     * @return The {@link WorkspaceLocation} corresponding to this value, or
     *         <code>null</code> if none matched
     */
    public static WorkspaceLocation fromInteger(final int value) {
        final Integer key = new Integer(value);
        Check.isTrue(VALUE_MAP.containsKey(key), "VALUE_MAP.containsKey(key)"); //$NON-NLS-1$
        return VALUE_MAP.get(key);
    }
}
