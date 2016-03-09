// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.framework.diagnostics.builtin.platform;

import java.util.Locale;

import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;

import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.TFSCommonUIClientPlugin;
import com.microsoft.tfs.client.common.ui.framework.diagnostics.data.Row;
import com.microsoft.tfs.client.common.ui.framework.diagnostics.data.TabularData;
import com.microsoft.tfs.client.common.ui.framework.diagnostics.extend.AvailableCallback;
import com.microsoft.tfs.client.common.ui.framework.diagnostics.extend.DataProvider;
import com.microsoft.tfs.client.common.ui.framework.diagnostics.extend.LocalizedDataProvider;

public class PluginRegistryProvider extends LocalizedDataProvider implements DataProvider, AvailableCallback {
    @Override
    protected Object getData(final Locale locale) {
        final TabularData table = new TabularData(new String[] {
            Messages.getString("PluginRegistryProvider.ColumnNameSymbolicName", locale), //$NON-NLS-1$
            Messages.getString("PluginRegistryProvider.ColumnNameVersion", locale), //$NON-NLS-1$
            Messages.getString("PluginRegistryProvider.ColumnNameName", locale), //$NON-NLS-1$
            Messages.getString("PluginRegistryProvider.ColumnNameVendor", locale) //$NON-NLS-1$
        });

        final Bundle[] bundles = TFSCommonUIClientPlugin.getDefault().getBundle().getBundleContext().getBundles();

        for (int i = 0; i < bundles.length; i++) {
            final String id = bundles[i].getSymbolicName();
            final String version = (String) bundles[i].getHeaders().get(Constants.BUNDLE_VERSION);
            final String vendor = (String) bundles[i].getHeaders().get(Constants.BUNDLE_VENDOR);
            final String name = (String) bundles[i].getHeaders().get(Constants.BUNDLE_NAME);

            table.addRow(new Row(new String[] {
                id,
                version,
                name,
                vendor
            }));
        }

        return table;
    }

    @Override
    public boolean isAvailable() {
        try {
            TFSCommonUIClientPlugin.getDefault().getBundle().getBundleContext();
        } catch (final NoSuchMethodError e) {
            return false;
        }

        return true;
    }
}
