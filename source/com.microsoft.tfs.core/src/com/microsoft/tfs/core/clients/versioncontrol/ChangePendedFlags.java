// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol;

import com.microsoft.tfs.util.BitField;

/**
 * Contains information on things done by the server after changes were pended
 * (for example, by merge).
 *
 * @since TEE-SDK-11.0
 */
public class ChangePendedFlags extends BitField {
    /**
     * Server does not support this mechanism.
     */
    public static final ChangePendedFlags UNKNOWN = new ChangePendedFlags(0, "Unknown"); //$NON-NLS-1$

    /**
     * No flags were set.
     */
    public static final ChangePendedFlags NONE = new ChangePendedFlags(1, "None"); //$NON-NLS-1$

    /**
     * One or more working folders was updated in this call.
     */
    public static final ChangePendedFlags WORKING_FOLDER_MAPPINGS_UPDATED =
        new ChangePendedFlags(2, "WorkingFolderMappingsUpdated"); //$NON-NLS-1$

    public static ChangePendedFlags combine(final ChangePendedFlags[] values) {
        return new ChangePendedFlags(BitField.combine(values));
    }

    private ChangePendedFlags(final int flags, final String name) {
        super(flags);
        registerStringValue(getClass(), flags, name);
    }

    public ChangePendedFlags(final int flags) {
        super(flags);
    }

    public boolean containsAll(final ChangePendedFlags other) {
        return containsAllInternal(other);
    }

    public boolean contains(final ChangePendedFlags other) {
        return containsInternal(other);
    }

    public boolean containsAny(final ChangePendedFlags other) {
        return containsAnyInternal(other);
    }

    public ChangePendedFlags remove(final ChangePendedFlags other) {
        return new ChangePendedFlags(removeInternal(other));
    }

    public ChangePendedFlags retain(final ChangePendedFlags other) {
        return new ChangePendedFlags(retainInternal(other));
    }

    public ChangePendedFlags combine(final ChangePendedFlags other) {
        return new ChangePendedFlags(combineInternal(other));
    }

}
