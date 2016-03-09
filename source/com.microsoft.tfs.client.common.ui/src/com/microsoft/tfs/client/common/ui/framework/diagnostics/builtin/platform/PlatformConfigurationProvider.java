// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.framework.diagnostics.builtin.platform;

import java.util.Properties;

import org.eclipse.core.runtime.Platform;

import com.microsoft.tfs.client.common.ui.framework.diagnostics.extend.DataProvider;
import com.microsoft.tfs.client.common.ui.framework.diagnostics.extend.NonLocalizedDataProvider;

public class PlatformConfigurationProvider extends NonLocalizedDataProvider implements DataProvider {
    @Override
    public Object getData() {
        final Properties properties = new Properties();

        properties.setProperty("WS", Platform.getWS()); //$NON-NLS-1$
        properties.setProperty("OS", Platform.getOS()); //$NON-NLS-1$
        properties.setProperty("ARCH", Platform.getOSArch()); //$NON-NLS-1$
        properties.setProperty("NL", Platform.getNL()); //$NON-NLS-1$

        return properties;
    }
}
