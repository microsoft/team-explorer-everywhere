// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.framework.diagnostics.builtin;

import java.io.File;

import org.eclipse.swt.widgets.Shell;

import com.microsoft.tfs.client.common.ui.framework.diagnostics.InternalSupportUtils;
import com.microsoft.tfs.client.common.ui.framework.diagnostics.extend.DataProviderAction;

public class OpenFolderOfFileAction implements DataProviderAction {
    @Override
    public void run(final Shell shell, final Object data) {
        final File file = (File) data;
        InternalSupportUtils.openFolderOfFile(file);
    }
}
