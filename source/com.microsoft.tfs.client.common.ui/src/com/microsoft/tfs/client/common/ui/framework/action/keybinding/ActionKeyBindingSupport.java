// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.framework.action.keybinding;

import org.eclipse.jface.action.IAction;

public interface ActionKeyBindingSupport {
    void addAction(IAction action);

    void dispose();
}
