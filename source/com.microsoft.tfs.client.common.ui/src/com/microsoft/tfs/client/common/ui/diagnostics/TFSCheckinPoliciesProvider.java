// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.diagnostics;

import java.text.MessageFormat;
import java.util.Locale;

import com.microsoft.tfs.checkinpolicies.ExtensionPointPolicyLoader;
import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.framework.diagnostics.data.Row;
import com.microsoft.tfs.client.common.ui.framework.diagnostics.data.TabularData;
import com.microsoft.tfs.client.common.ui.framework.diagnostics.extend.DataProvider;
import com.microsoft.tfs.client.common.ui.framework.diagnostics.extend.LocalizedDataProvider;
import com.microsoft.tfs.core.checkinpolicies.PolicyInstance;
import com.microsoft.tfs.core.checkinpolicies.PolicyLoaderException;

public class TFSCheckinPoliciesProvider extends LocalizedDataProvider implements DataProvider {
    @Override
    protected Object getData(final Locale locale) {
        final TabularData table = new TabularData(new String[] {
            Messages.getString("TFSCheckinPoliciesProvider.ColumnNameId", locale), //$NON-NLS-1$
            Messages.getString("TFSCheckinPoliciesProvider.ColumnNameName", locale), //$NON-NLS-1$
            Messages.getString("TFSCheckinPoliciesProvider.ColumnNameShortDesc", locale) //$NON-NLS-1$
        });

        final ExtensionPointPolicyLoader loader = new ExtensionPointPolicyLoader();

        final String[] ids = loader.getAvailablePolicyTypeIDs();

        for (int i = 0; i < ids.length; i++) {
            final String id = ids[i];

            if (id == null || id.length() == 0) {
                continue;
            }

            PolicyInstance instance;
            try {
                instance = loader.load(id);
            } catch (final PolicyLoaderException e) {
                instance = null;

                final String messageFormat = Messages.getString("TFSCheckinPoliciesProvider.ExceptionFormat"); //$NON-NLS-1$
                final String message = MessageFormat.format(messageFormat, e.getMessage());

                table.addRow(new Row(new Object[] {
                    id,
                    Messages.getString("TFSCheckinPoliciesProvider.ExceptionLoadingPolicy"), //$NON-NLS-1$
                    message
                }));
                continue;
            }

            if (instance != null) {
                table.addRow(new Row(new Object[] {
                    id,
                    instance.getPolicyType().getName(),
                    instance.getPolicyType().getShortDescription()
                }));
            }
        }

        return table;
    }
}
