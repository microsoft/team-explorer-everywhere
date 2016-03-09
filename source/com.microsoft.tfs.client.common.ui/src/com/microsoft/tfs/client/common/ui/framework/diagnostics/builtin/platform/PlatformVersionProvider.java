// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.framework.diagnostics.builtin.platform;

import java.util.Locale;
import java.util.Properties;

import org.eclipse.core.runtime.Platform;

import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.framework.diagnostics.extend.DataProvider;
import com.microsoft.tfs.client.common.ui.framework.diagnostics.extend.LocalizedDataProvider;

public class PlatformVersionProvider extends LocalizedDataProvider implements DataProvider {
    @Override
    protected Object getData(final Locale locale) {
        final Properties properties = new Properties();

        String productVersion = Messages.getString("PlatformVersionProvider.NotDefined", locale); //$NON-NLS-1$

        if (Platform.getProduct() != null) {
            productVersion = (String) Platform.getProduct().getDefiningBundle().getHeaders().get(
                org.osgi.framework.Constants.BUNDLE_VERSION);
        }

        String eclipseApplication = System.getProperty("eclipse.application"); //$NON-NLS-1$
        if (eclipseApplication == null) {
            eclipseApplication = Messages.getString("PlatformVersionProvider.NotDefined", locale); //$NON-NLS-1$
        }

        String eclipseProduct = System.getProperty("eclipse.product"); //$NON-NLS-1$
        if (eclipseProduct == null) {
            eclipseProduct = Messages.getString("PlatformVersionProvider.NotDefined", locale); //$NON-NLS-1$
        }

        properties.setProperty(
            Messages.getString("PlatformVersionProvider.EclipseApplicationProperty", locale), //$NON-NLS-1$
            eclipseApplication);
        properties.setProperty(
            Messages.getString("PlatformVersionProvider.EclipseProductProperty", locale), //$NON-NLS-1$
            eclipseProduct);
        properties.setProperty(
            Messages.getString("PlatformVersionProvider.EclipseProductVersionProperty", locale), //$NON-NLS-1$
            productVersion);

        return properties;
    }
}
