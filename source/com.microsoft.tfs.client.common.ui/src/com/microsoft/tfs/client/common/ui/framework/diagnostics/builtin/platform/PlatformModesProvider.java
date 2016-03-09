// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.framework.diagnostics.builtin.platform;

import java.util.Locale;
import java.util.Properties;

import org.eclipse.core.runtime.Platform;

import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.framework.diagnostics.extend.DataProvider;
import com.microsoft.tfs.client.common.ui.framework.diagnostics.extend.LocalizedDataProvider;

public class PlatformModesProvider extends LocalizedDataProvider implements DataProvider {
    @Override
    protected Object getData(final Locale locale) {
        final Properties properties = new Properties();

        final boolean debugMode = Platform.inDebugMode();
        final boolean developmentMode = Platform.inDevelopmentMode();

        properties.setProperty(
            Messages.getString("PlatformModesProvider.DebugProperty", locale), //$NON-NLS-1$
            String.valueOf(debugMode));
        properties.setProperty(
            Messages.getString("PlatformModesProvider.DevelopmentProperty", locale), //$NON-NLS-1$
            String.valueOf(developmentMode));

        return properties;
    }
}
