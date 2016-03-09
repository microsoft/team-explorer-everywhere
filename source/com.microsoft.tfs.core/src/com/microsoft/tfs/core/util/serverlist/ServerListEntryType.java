// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.util.serverlist;

import java.util.HashMap;
import java.util.Map;

import com.microsoft.tfs.util.TypesafeEnum;

public class ServerListEntryType extends TypesafeEnum {
    private static final Map<Integer, ServerListEntryType> instanceMap = new HashMap<Integer, ServerListEntryType>();

    public static final ServerListEntryType CONFIGURATION_SERVER = new ServerListEntryType(0);
    public static final ServerListEntryType TEAM_PROJECT_COLLECTION = new ServerListEntryType(1);

    private ServerListEntryType(final int value) {
        super(value);

        instanceMap.put(value, this);
    }

    public static ServerListEntryType fromValue(final int value) {
        return instanceMap.get(value);
    }
}
