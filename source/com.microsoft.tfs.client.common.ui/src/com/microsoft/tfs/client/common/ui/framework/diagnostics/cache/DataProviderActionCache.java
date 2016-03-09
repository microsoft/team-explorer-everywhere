// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.framework.diagnostics.cache;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IStatus;

import com.microsoft.tfs.client.common.ui.framework.diagnostics.Constants;
import com.microsoft.tfs.client.common.ui.framework.diagnostics.SupportManager;
import com.microsoft.tfs.client.common.ui.framework.diagnostics.Utils;
import com.microsoft.tfs.client.common.ui.framework.diagnostics.extend.DataProviderAction;

public class DataProviderActionCache {
    private Map dataProviderActions;

    public DataProviderActionInfo getByID(final String id) {
        synchronized (this) {
            if (dataProviderActions == null) {
                populate();
            }
        }
        return (DataProviderActionInfo) dataProviderActions.get(id);
    }

    private void populate() {
        dataProviderActions = new HashMap();

        final IExtension[] extensions =
            SupportManager.getExtensions(Constants.DATA_PROVIDER_ACTIONS_EXTENSION_POINT_NAME);

        for (int i = 0; i < extensions.length; i++) {
            final IConfigurationElement[] configElements = extensions[i].getConfigurationElements();
            for (int j = 0; j < configElements.length; j++) {
                final DataProviderActionInfo dataProviderAction = getDataProviderAction(configElements[j]);
                if (dataProviderAction != null) {
                    if (dataProviderActions.containsKey(dataProviderAction.getID())) {
                        final String messageFormat = "data provider action [{0}] has been defined multiple times {1}"; //$NON-NLS-1$
                        final String message = MessageFormat.format(
                            messageFormat,
                            dataProviderAction.getID(),
                            Utils.messageFor(configElements[j], Constants.DATA_PROVIDER_ACTIONS_EXTENSION_POINT_NAME));

                        SupportManager.log(IStatus.WARNING, message);
                    }

                    dataProviderActions.put(dataProviderAction.getID(), dataProviderAction);
                }
            }
        }
    }

    private DataProviderActionInfo getDataProviderAction(final IConfigurationElement element) {
        if (!Utils.checkConfigElement(
            element,
            Constants.ELEMENT_NAME_ACTION,
            Constants.DATA_PROVIDER_ACTIONS_EXTENSION_POINT_NAME)) {
            return null;
        }

        final String id = Utils.getRequiredAttribute(
            element,
            Constants.ATTRIBUTE_NAME_ID,
            Constants.DATA_PROVIDER_ACTIONS_EXTENSION_POINT_NAME);

        if (id == null) {
            return null;
        }

        final String label = Utils.getRequiredAttribute(
            element,
            Constants.ATTRIBUTE_NAME_LABEL,
            Constants.DATA_PROVIDER_ACTIONS_EXTENSION_POINT_NAME);

        if (label == null) {
            return null;
        }

        final DataProviderAction action = (DataProviderAction) Utils.createExecutableExtension(
            element,
            Constants.ATTRIBUTE_NAME_CLASS,
            DataProviderAction.class,
            Constants.DATA_PROVIDER_ACTIONS_EXTENSION_POINT_NAME);

        if (action == null) {
            return null;
        }

        return new DataProviderActionInfo(id, label, action);
    }
}
