// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.diagnostics;

import org.eclipse.swt.widgets.Shell;

import com.microsoft.tfs.client.common.ui.framework.diagnostics.SupportFacade;

public class SupportUtils {
    public static void openSupportDialog(final Shell shell, final ClassLoader classLoader) {
        SupportFacade.openSupportDialog(shell, classLoader, null);
    }
}
