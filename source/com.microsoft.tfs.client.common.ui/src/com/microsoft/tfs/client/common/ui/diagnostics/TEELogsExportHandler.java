// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.diagnostics;

import java.io.File;
import java.io.IOException;
import java.util.zip.ZipOutputStream;

import com.microsoft.tfs.client.common.ui.framework.diagnostics.SupportPluginUtils;
import com.microsoft.tfs.client.common.ui.framework.diagnostics.data.Row;
import com.microsoft.tfs.client.common.ui.framework.diagnostics.data.TabularData;
import com.microsoft.tfs.client.common.ui.framework.diagnostics.extend.ExportHandler;

public class TEELogsExportHandler implements ExportHandler {
    @Override
    public void export(final Object data, final String configData, final ZipOutputStream zipout) throws IOException {
        final TabularData table = (TabularData) data;

        final Row[] rows = table.getRows();
        for (int i = 0; i < rows.length; i++) {
            final Row row = rows[i];

            final boolean inUse = ((String) row.getValues()[0]).length() > 0;
            final String logType = (String) row.getValues()[1];
            final File file = (File) row.getTag();

            final StringBuffer zipName = new StringBuffer();
            zipName.append(logType);
            zipName.append(" "); //$NON-NLS-1$
            zipName.append("logs"); //$NON-NLS-1$
            zipName.append(File.separatorChar);
            if (inUse) {
                zipName.append("CURRENT_"); //$NON-NLS-1$
            }
            zipName.append(file.getName());

            SupportPluginUtils.addFileToZip(file, zipName.toString(), zipout);
        }
    }
}
