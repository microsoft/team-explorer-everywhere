// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.workspacecache;

import com.microsoft.tfs.util.TypesafeEnum;

public class WorkstationType extends TypesafeEnum {
    public static final WorkstationType CURRENT = new WorkstationType(0);
    public static final WorkstationType REMOTE = new WorkstationType(1);

    private WorkstationType(final int value) {
        super(value);
    }
}