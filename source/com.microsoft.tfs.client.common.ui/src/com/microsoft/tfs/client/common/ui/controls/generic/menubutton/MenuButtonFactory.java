// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.controls.generic.menubutton;

import org.eclipse.swt.widgets.Composite;

import com.microsoft.tfs.client.common.ui.framework.WindowSystem;

public final class MenuButtonFactory {
    public static MenuButton getMenuButton(final Composite parent, final int style) {
        if (WindowSystem.isCurrentWindowSystem(WindowSystem.AQUA)) {
            return new MacMenuButton(parent, style);
        } else {
            return new GenericMenuButton(parent, style);
        }
    }
}
