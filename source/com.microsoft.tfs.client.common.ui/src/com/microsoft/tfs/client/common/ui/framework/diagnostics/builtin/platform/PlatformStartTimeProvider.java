// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.framework.diagnostics.builtin.platform;

import java.util.Date;

import com.microsoft.tfs.client.common.ui.framework.diagnostics.extend.AvailableCallback;
import com.microsoft.tfs.client.common.ui.framework.diagnostics.extend.DataProvider;
import com.microsoft.tfs.client.common.ui.framework.diagnostics.extend.NonLocalizedDataProvider;
import com.microsoft.tfs.client.common.ui.framework.diagnostics.extend.PopulateCallback;

public class PlatformStartTimeProvider extends NonLocalizedDataProvider
    implements DataProvider, AvailableCallback, PopulateCallback {
    private Date startTime;

    @Override
    public void populate() throws Exception {
        startTime = null;
        try {
            startTime = new Date(Long.parseLong(System.getProperty("eclipse.startTime"))); //$NON-NLS-1$
        } catch (final Exception ex) {

        }
    }

    @Override
    public boolean isAvailable() {
        return startTime != null;
    }

    @Override
    public Object getData() {
        return startTime;
    }
}
