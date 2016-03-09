// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.clc.vc.options;

import com.microsoft.tfs.client.clc.options.SingleValueOption;

public final class OptionAuto extends SingleValueOption {
    public static final String AUTO_MERGE = "AutoMerge"; //$NON-NLS-1$
    public static final String TAKE_THEIRS = "TakeTheirs"; //$NON-NLS-1$
    public static final String KEEP_YOURS = "KeepYours"; //$NON-NLS-1$
    public static final String OVERWRITE_LOCAL = "OverwriteLocal"; //$NON-NLS-1$
    public static final String DELETE_CONFLICT = "DeleteConflict"; //$NON-NLS-1$
    public static final String KEEP_YOURS_RENAME_THEIRS = "KeepYoursRenameTheirs"; //$NON-NLS-1$
    public static final String EXTERNAL_TOOL = "External"; //$NON-NLS-1$

    public OptionAuto() {
        super();
    }

    @Override
    protected String[] getValidOptionValues() {
        return new String[] {
            AUTO_MERGE,
            TAKE_THEIRS,
            KEEP_YOURS,
            OVERWRITE_LOCAL,
            DELETE_CONFLICT,
            KEEP_YOURS_RENAME_THEIRS,
            EXTERNAL_TOOL
        };
    }
}
