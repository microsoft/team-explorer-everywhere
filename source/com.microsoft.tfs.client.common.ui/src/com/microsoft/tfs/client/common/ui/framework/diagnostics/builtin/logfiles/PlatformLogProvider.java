// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.framework.diagnostics.builtin.logfiles;

import java.io.File;

import org.eclipse.core.runtime.Platform;

import com.microsoft.tfs.client.common.ui.framework.diagnostics.extend.AvailableCallback;
import com.microsoft.tfs.client.common.ui.framework.diagnostics.extend.DataProvider;
import com.microsoft.tfs.client.common.ui.framework.diagnostics.extend.NonLocalizedDataProvider;
import com.microsoft.tfs.client.common.ui.framework.diagnostics.extend.PopulateCallback;

public class PlatformLogProvider extends NonLocalizedDataProvider
    implements DataProvider, PopulateCallback, AvailableCallback {
    private File logFile;

    @Override
    public void populate() throws Exception {
        logFile = Platform.getLogFileLocation().toFile();
    }

    @Override
    public boolean isAvailable() {
        return logFile.exists();
    }

    @Override
    public Object getData() {
        return logFile;
    }
}
