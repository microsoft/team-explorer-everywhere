// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.diagnostics;

import java.text.MessageFormat;
import java.util.Locale;

import org.osgi.framework.Bundle;

import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.TFSCommonUIClientPlugin;
import com.microsoft.tfs.client.common.ui.framework.diagnostics.data.Row;
import com.microsoft.tfs.client.common.ui.framework.diagnostics.data.TabularData;
import com.microsoft.tfs.client.common.ui.framework.diagnostics.extend.AvailableCallback;
import com.microsoft.tfs.client.common.ui.framework.diagnostics.extend.DataProvider;
import com.microsoft.tfs.client.common.ui.framework.diagnostics.extend.LocalizedDataProvider;

public class TEEPluginsVersionProvider extends LocalizedDataProvider implements DataProvider, AvailableCallback {
    private static final String[] MICROSOFT_TFS_PREFIXES = new String[] {
        "com.microsoft.tfs" //$NON-NLS-1$
    };

    @Override
    protected Object getData(final Locale locale) {
        final TabularData table = new TabularData(new String[] {
            Messages.getString("TEEPluginsVersionProvider.ColumnNameId", locale), //$NON-NLS-1$
            Messages.getString("TEEPluginsVersionProvider.ColumnNameVersion", locale), //$NON-NLS-1$
            Messages.getString("TEEPluginsVersionProvider.ColumnNameState", locale), //$NON-NLS-1$
            Messages.getString("TEEPluginsVersionProvider.ColumnNameLocation", locale) //$NON-NLS-1$
        });

        final Bundle[] bundles = TFSCommonUIClientPlugin.getDefault().getBundle().getBundleContext().getBundles();

        for (int i = 0; i < bundles.length; i++) {
            final String id = bundles[i].getSymbolicName();
            if (isTFSBundle(id)) {
                table.addRow(new Row(new Object[] {
                    id,
                    bundles[i].getHeaders().get(org.osgi.framework.Constants.BUNDLE_VERSION),
                    getStateString(locale, bundles[i].getState()),
                    bundles[i].getLocation()
                }));
            }
        }

        return table;
    }

    private boolean isTFSBundle(final String id) {
        for (int i = 0; i < MICROSOFT_TFS_PREFIXES.length; i++) {
            if (id.startsWith(MICROSOFT_TFS_PREFIXES[i])) {
                return true;
            }
        }
        return false;
    }

    private String getStateString(final Locale locale, final int state) {
        switch (state) {
            case Bundle.ACTIVE:
                return Messages.getString("TEEPluginsVersionProvider.ACTIVE", locale); //$NON-NLS-1$
            case Bundle.INSTALLED:
                return Messages.getString("TEEPluginsVersionProvider.INSTALLED", locale); //$NON-NLS-1$
            case Bundle.RESOLVED:
                return Messages.getString("TEEPluginsVersionProvider.RESOLVED", locale); //$NON-NLS-1$
            case Bundle.STARTING:
                return Messages.getString("TEEPluginsVersionProvider.STARTING", locale); //$NON-NLS-1$
            case Bundle.STOPPING:
                return Messages.getString("TEEPluginsVersionProvider.STOPPING", locale); //$NON-NLS-1$
            case Bundle.UNINSTALLED:
                return Messages.getString("TEEPluginsVersionProvider.UNINSTALLED", locale); //$NON-NLS-1$
            default:
                return MessageFormat.format(
                    Messages.getString("TEEPluginsVersionProvider.UnknownFormat", locale), //$NON-NLS-1$
                    state);
        }
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
