// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.diagnostics;

import java.util.Locale;
import java.util.Properties;

import com.microsoft.tfs.client.common.license.LicenseManager;
import com.microsoft.tfs.client.common.license.ProductID;
import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.framework.diagnostics.extend.DataProvider;
import com.microsoft.tfs.client.common.ui.framework.diagnostics.extend.LocalizedDataProvider;

public class TEEProductKeyDataProvider extends LocalizedDataProvider implements DataProvider {
    @Override
    protected Object getData(final Locale locale) {
        final Properties properties = new Properties();

        final ProductID productId = LicenseManager.getInstance().getProductID();

        if (productId == null) {
            properties.setProperty(
                Messages.getString("TEEProductKeyDataProvider.ProductIDProperty", locale), //$NON-NLS-1$
                Messages.getString("TEEProductKeyDataProvider.NoProductIdProvided", locale)); //$NON-NLS-1$
        } else {
            properties.setProperty(
                Messages.getString("TEEProductKeyDataProvider.ProductIDProperty", locale), //$NON-NLS-1$
                productId.getID());
        }

        return properties;
    }
}
