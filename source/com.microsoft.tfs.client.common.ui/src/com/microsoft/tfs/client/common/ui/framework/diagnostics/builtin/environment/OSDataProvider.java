// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.framework.diagnostics.builtin.environment;

import java.util.Locale;
import java.util.Properties;

import org.eclipse.core.runtime.Platform;

import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.framework.diagnostics.extend.DataProvider;
import com.microsoft.tfs.client.common.ui.framework.diagnostics.extend.LocalizedDataProvider;

public class OSDataProvider extends LocalizedDataProvider implements DataProvider {
    @Override
    protected Object getData(final Locale locale) {
        final Properties properties = new Properties();

        properties.setProperty(Messages.getString("OSDataProvider.JavaOSName", locale), System.getProperty("os.name")); //$NON-NLS-1$ //$NON-NLS-2$
        properties.setProperty(Messages.getString("OSDataProvider.JavaOSArch", locale), System.getProperty("os.arch")); //$NON-NLS-1$ //$NON-NLS-2$
        properties.setProperty(
            Messages.getString("OSDataProvider.JavaOSVersion", locale), //$NON-NLS-1$
            System.getProperty("os.version")); //$NON-NLS-1$
        properties.setProperty(
            Messages.getString("OSDataProvider.JavaAvailProcessors", locale), //$NON-NLS-1$
            String.valueOf(Runtime.getRuntime().availableProcessors()));
        properties.setProperty(Messages.getString("OSDataProvider.PlatformWS", locale), Platform.getWS()); //$NON-NLS-1$
        properties.setProperty(Messages.getString("OSDataProvider.PlatformOS", locale), Platform.getOS()); //$NON-NLS-1$
        properties.setProperty(Messages.getString("OSDataProvider.PlatformArch", locale), Platform.getOSArch()); //$NON-NLS-1$

        return properties;
    }
}
