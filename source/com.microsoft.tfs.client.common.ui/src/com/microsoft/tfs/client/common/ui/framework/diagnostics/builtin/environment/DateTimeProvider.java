// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.framework.diagnostics.builtin.environment;

import java.util.Date;

import com.microsoft.tfs.client.common.ui.framework.diagnostics.extend.DataProvider;
import com.microsoft.tfs.client.common.ui.framework.diagnostics.extend.NonLocalizedDataProvider;

public class DateTimeProvider extends NonLocalizedDataProvider implements DataProvider {
    @Override
    public Object getData() {
        return new Date();
    }
}
