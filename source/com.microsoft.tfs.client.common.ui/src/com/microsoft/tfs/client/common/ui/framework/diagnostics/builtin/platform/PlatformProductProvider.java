// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.framework.diagnostics.builtin.platform;

import java.util.Locale;
import java.util.Properties;

import org.eclipse.core.runtime.IProduct;
import org.eclipse.core.runtime.Platform;

import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.framework.diagnostics.extend.DataProvider;
import com.microsoft.tfs.client.common.ui.framework.diagnostics.extend.LocalizedDataProvider;

public class PlatformProductProvider extends LocalizedDataProvider implements DataProvider {
    @Override
    protected Object getData(final Locale locale) {
        final Properties properties = new Properties();

        final IProduct product = Platform.getProduct();

        if (product == null) {
            properties.setProperty(
                Messages.getString("PlatformProductProvider.ProductProperty", locale), //$NON-NLS-1$
                Messages.getString("PlatformProductProvider.UndefinedProduct", locale)); //$NON-NLS-1$
        } else {
            properties.setProperty(
                Messages.getString("PlatformProductProvider.NameProperty", locale), //$NON-NLS-1$
                product.getName());
            properties.setProperty(Messages.getString("PlatformProductProvider.IdProperty", locale), product.getId()); //$NON-NLS-1$
            properties.setProperty(
                Messages.getString("PlatformProductProvider.ApplicationProperty", locale), //$NON-NLS-1$
                product.getApplication());
            properties.setProperty(
                Messages.getString("PlatformProductProvider.DefinePluginProperty", locale), //$NON-NLS-1$
                product.getDefiningBundle().getSymbolicName());
        }

        return properties;
    }
}
