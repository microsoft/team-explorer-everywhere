// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.framework.diagnostics.cache;

import java.io.IOException;
import java.util.zip.ZipOutputStream;

public class ExportHandlerReference {
    private final ExportHandlerInfo exportHandler;
    private final String configData;

    public ExportHandlerReference(final ExportHandlerInfo exportHandler, final String configData) {
        this.exportHandler = exportHandler;
        this.configData = configData;
    }

    public void export(final Object data, final ZipOutputStream zipout) throws IOException {
        exportHandler.getExportHandler().export(data, configData, zipout);
    }
}
