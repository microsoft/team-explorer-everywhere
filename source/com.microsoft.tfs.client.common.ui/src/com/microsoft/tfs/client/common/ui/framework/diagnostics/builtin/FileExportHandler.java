// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.framework.diagnostics.builtin;

import java.io.File;
import java.io.IOException;
import java.util.zip.ZipOutputStream;

import com.microsoft.tfs.client.common.ui.framework.diagnostics.SupportPluginUtils;
import com.microsoft.tfs.client.common.ui.framework.diagnostics.extend.ExportHandler;

public class FileExportHandler implements ExportHandler {
    @Override
    public void export(final Object data, final String configData, final ZipOutputStream zipout) throws IOException {
        final File file = (File) data;
        String zipName = file.getName();

        if (zipName.startsWith(".")) //$NON-NLS-1$
        {
            zipName = "_" + zipName; //$NON-NLS-1$
        }

        if (configData != null) {
            zipName = configData + File.separatorChar + zipName;
        }

        SupportPluginUtils.addFileToZip(file, zipName, zipout);
    }
}
