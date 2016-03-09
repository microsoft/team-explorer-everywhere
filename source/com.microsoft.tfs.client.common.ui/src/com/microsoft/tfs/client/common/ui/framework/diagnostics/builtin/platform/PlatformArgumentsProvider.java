// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.framework.diagnostics.builtin.platform;

import java.util.Locale;
import java.util.Properties;

import org.eclipse.core.runtime.Platform;

import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.framework.diagnostics.extend.DataProvider;
import com.microsoft.tfs.client.common.ui.framework.diagnostics.extend.LocalizedDataProvider;

public class PlatformArgumentsProvider extends LocalizedDataProvider implements DataProvider {
    @Override
    protected Object getData(final Locale locale) {
        final String[] cmdLineArgs = Platform.getCommandLineArgs();
        final String[] appArgs = Platform.getApplicationArgs();

        final Properties properties = new Properties();

        for (int i = 0; i < cmdLineArgs.length; i++) {
            properties.setProperty(
                Messages.getString("PlatformArgumentsProvider.CmdLineArgProperty", locale) + i, //$NON-NLS-1$
                cmdLineArgs[i]);
        }

        for (int i = 0; i < appArgs.length; i++) {
            properties.setProperty(
                Messages.getString("PlatformArgumentsProvider.AppArgProperty", locale) + i, //$NON-NLS-1$
                appArgs[i]);
        }

        return properties;
    }
}
