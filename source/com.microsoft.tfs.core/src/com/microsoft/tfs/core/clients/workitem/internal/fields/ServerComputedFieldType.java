// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.internal.fields;

public class ServerComputedFieldType {
    public static final ServerComputedFieldType DATE_TIME = new ServerComputedFieldType("ServerDateTime"); //$NON-NLS-1$
    public static final ServerComputedFieldType CURRENT_USER = new ServerComputedFieldType("ServerCurrentUser"); //$NON-NLS-1$
    public static final ServerComputedFieldType RANDOM_GUID = new ServerComputedFieldType("ServerRandomGuid"); //$NON-NLS-1$

    private final String name;

    private ServerComputedFieldType(final String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
