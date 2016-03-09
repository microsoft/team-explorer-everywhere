// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.framework.diagnostics.builtin;

import com.microsoft.tfs.client.common.ui.framework.diagnostics.extend.AvailableCallback;
import com.microsoft.tfs.client.common.ui.framework.diagnostics.extend.DataProvider;

public class NullDataProvider implements DataProvider, AvailableCallback {
    @Override
    public Object getData() {
        return null;
    }

    @Override
    public Object getDataNOLOC() {
        return null;
    }

    @Override
    public boolean isAvailable() {
        return false;
    }
}
