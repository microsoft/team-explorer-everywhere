// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.internal.localworkspace;

import java.util.HashMap;
import java.util.Map;

import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.TypesafeEnum;

public class BaselineFolderState extends TypesafeEnum {
    public static final Map<Byte, BaselineFolderState> VALUE_MAP = new HashMap<Byte, BaselineFolderState>();

    public static final BaselineFolderState UNKNOWN = new BaselineFolderState((byte) 0, "Unknown"); //$NON-NLS-1$
    public static final BaselineFolderState STALE = new BaselineFolderState((byte) 1, "Stale"); //$NON-NLS-1$
    public static final BaselineFolderState VALID = new BaselineFolderState((byte) 2, "Valid"); //$NON-NLS-1$

    private final String display;

    private BaselineFolderState(final byte value, final String display) {
        super(value);
        this.display = display;

        final Byte key = new Byte(value);
        Check.isTrue(!VALUE_MAP.containsKey(key), "!VALUE_MAP.containsKey(key)"); //$NON-NLS-1$
        VALUE_MAP.put(key, this);
    }

    @Override
    public String toString() {
        return display;
    }

    public static BaselineFolderState fromByte(final byte value) {
        final Byte key = new Byte(value);
        Check.isTrue(VALUE_MAP.containsKey(key), "VALUE_MAP.containsKey(key)"); //$NON-NLS-1$
        return VALUE_MAP.get(key);
    }

    public static BaselineFolderState fromValue(final String value) {
        if (value.equals(VALID.display)) {
            return VALID;
        } else if (value.equals(STALE.display)) {
            return STALE;
        } else {
            return UNKNOWN;
        }
    }
}
