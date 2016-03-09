// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.internal.localworkspace;

import com.microsoft.tfs.util.TypesafeEnum;

public class ExistenceCheckResult extends TypesafeEnum {
    public static final ExistenceCheckResult DOES_NOT_EXIST = new ExistenceCheckResult(0);
    public static final ExistenceCheckResult IS_FILE = new ExistenceCheckResult(1);
    public static final ExistenceCheckResult IS_FOLDER = new ExistenceCheckResult(2);

    private ExistenceCheckResult(final int value) {
        super(value);
    }
}
