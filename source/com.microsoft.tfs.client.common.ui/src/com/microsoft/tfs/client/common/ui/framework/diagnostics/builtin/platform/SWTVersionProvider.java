// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.framework.diagnostics.builtin.platform;

import java.util.Locale;
import java.util.Properties;

import org.eclipse.swt.SWT;

import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.framework.diagnostics.extend.DataProvider;
import com.microsoft.tfs.client.common.ui.framework.diagnostics.extend.LocalizedDataProvider;

public class SWTVersionProvider extends LocalizedDataProvider implements DataProvider {
    @Override
    protected Object getData(final Locale locale) {
        final Properties properties = new Properties();

        properties.setProperty(Messages.getString("SWTVersionProvider.SWTPlatformProperty", locale), SWT.getPlatform()); //$NON-NLS-1$
        properties.setProperty(
            Messages.getString("SWTVersionProvider.SWTVersionProperty", locale), //$NON-NLS-1$
            String.valueOf(SWT.getVersion()));

        return properties;
    }
}
