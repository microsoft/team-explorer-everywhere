// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.framework.diagnostics.builtin.java;

import java.util.Locale;
import java.util.Properties;

import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.framework.diagnostics.extend.DataProvider;
import com.microsoft.tfs.client.common.ui.framework.diagnostics.extend.LocalizedDataProvider;

public class JVMVersionProvider extends LocalizedDataProvider implements DataProvider {
    @Override
    protected Object getData(final Locale locale) {
        final Properties properties = new Properties();

        properties.setProperty(
            Messages.getString("JVMVersionProvider.JavaRuntime", locale), //$NON-NLS-1$
            System.getProperty("java.vendor") //$NON-NLS-1$
                + " " //$NON-NLS-1$
                + System.getProperty("java.version") //$NON-NLS-1$
                + " (" //$NON-NLS-1$
                + System.getProperty("java.home") //$NON-NLS-1$
                + ")"); //$NON-NLS-1$

        properties.setProperty(
            Messages.getString("JVMVersionProvider.VMImplementation", locale), //$NON-NLS-1$
            System.getProperty("java.vm.name") //$NON-NLS-1$
                + ", " //$NON-NLS-1$
                + System.getProperty("java.vm.info") //$NON-NLS-1$
                + ", " //$NON-NLS-1$
                + System.getProperty("java.vm.version") //$NON-NLS-1$
                + " (" //$NON-NLS-1$
                + System.getProperty("java.vm.vendor") //$NON-NLS-1$
                + ")"); //$NON-NLS-1$

        properties.setProperty(
            Messages.getString("JVMVersionProvider.VMSpecification", locale), //$NON-NLS-1$
            System.getProperty("java.vm.specification.name") //$NON-NLS-1$
                + ", " //$NON-NLS-1$
                + System.getProperty("java.vm.specification.version") //$NON-NLS-1$
                + " (" //$NON-NLS-1$
                + System.getProperty("java.vm.specification.vendor") //$NON-NLS-1$
                + ")"); //$NON-NLS-1$

        properties.setProperty(
            Messages.getString("JVMVersionProvider.RuntimeSpecification", locale), //$NON-NLS-1$
            System.getProperty("java.specification.name") //$NON-NLS-1$
                + ", " //$NON-NLS-1$
                + System.getProperty("java.specification.version") //$NON-NLS-1$
                + " (" //$NON-NLS-1$
                + System.getProperty("java.specification.vendor") //$NON-NLS-1$
                + ")"); //$NON-NLS-1$

        properties.setProperty(
            Messages.getString("JVMVersionProvider.ClassFomatVersion", locale), //$NON-NLS-1$
            System.getProperty("java.class.version")); //$NON-NLS-1$

        return properties;
    }
}
