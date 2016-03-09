// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.framework.diagnostics.builtin.java;

import java.io.File;

import com.microsoft.tfs.client.common.ui.framework.diagnostics.extend.DataProvider;
import com.microsoft.tfs.client.common.ui.framework.diagnostics.extend.NonLocalizedDataProvider;

public class JavaHomeProvider extends NonLocalizedDataProvider implements DataProvider {
    @Override
    public Object getData() {
        return new File(System.getProperty("java.home")); //$NON-NLS-1$
    }
}
