// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.framework.diagnostics.cache;

import com.microsoft.tfs.client.common.ui.framework.diagnostics.extend.ExportHandler;

public class ExportHandlerInfo {
    private final String id;
    private final ExportHandler exportHandler;

    public ExportHandlerInfo(final String id, final ExportHandler exportHandler) {
        this.id = id;
        this.exportHandler = exportHandler;
    }

    public ExportHandler getExportHandler() {
        return exportHandler;
    }

    public String getID() {
        return id;
    }
}
