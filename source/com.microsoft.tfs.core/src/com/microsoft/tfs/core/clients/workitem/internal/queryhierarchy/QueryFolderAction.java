// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.internal.queryhierarchy;

import com.microsoft.tfs.util.TypesafeEnum;

public class QueryFolderAction extends TypesafeEnum {
    public static final QueryFolderAction ADDED = new QueryFolderAction(0);
    public static final QueryFolderAction REMOVED = new QueryFolderAction(1);
    public static final QueryFolderAction CHANGED = new QueryFolderAction(2);

    private QueryFolderAction(final int value) {
        super(value);
    }
}
