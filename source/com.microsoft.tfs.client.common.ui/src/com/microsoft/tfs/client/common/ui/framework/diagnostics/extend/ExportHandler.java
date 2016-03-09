// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.framework.diagnostics.extend;

import java.io.IOException;
import java.util.zip.ZipOutputStream;

public interface ExportHandler {
    public void export(Object data, String configData, ZipOutputStream zipout) throws IOException;
}
