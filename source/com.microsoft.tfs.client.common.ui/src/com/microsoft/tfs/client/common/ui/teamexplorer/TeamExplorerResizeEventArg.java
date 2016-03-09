// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.teamexplorer;

import com.microsoft.tfs.client.common.ui.TeamExplorerEventArg;

public class TeamExplorerResizeEventArg extends TeamExplorerEventArg {
    private final int formWidth;

    public TeamExplorerResizeEventArg(final int formWidth) {
        this.formWidth = formWidth;
    }

    public int getFormWidth() {
        return formWidth;
    }
}
