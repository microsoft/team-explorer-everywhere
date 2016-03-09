// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.framework.diagnostics.builtin.environment;

import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

import com.microsoft.tfs.client.common.ui.framework.diagnostics.extend.AvailableCallback;
import com.microsoft.tfs.client.common.ui.framework.diagnostics.extend.DataProvider;
import com.microsoft.tfs.client.common.ui.framework.diagnostics.extend.NonLocalizedDataProvider;
import com.microsoft.tfs.client.common.ui.framework.diagnostics.extend.PopulateCallback;

public class EnvironmentVariablesProvider extends NonLocalizedDataProvider
    implements DataProvider, PopulateCallback, AvailableCallback {
    private Map<String, String> environmentVariableMap;

    @Override
    public boolean isAvailable() {
        return environmentVariableMap != null;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void populate() throws Exception {
        environmentVariableMap = null;

        try {
            final Method method = System.class.getDeclaredMethod("getenv", (Class[]) null); //$NON-NLS-1$
            environmentVariableMap = (Map<String, String>) method.invoke(null, (Object[]) null);
        } catch (final Exception e) {
            /*
             * ignore, the java.lang.System getenv method must not be available
             * (non java 5+)
             */
        }
    }

    @Override
    public Object getData() {
        final Properties properties = new Properties();

        for (final Iterator<String> it = environmentVariableMap.keySet().iterator(); it.hasNext();) {
            final String key = it.next();
            final String value = environmentVariableMap.get(key);
            properties.setProperty(key, value);
        }

        return properties;
    }
}
