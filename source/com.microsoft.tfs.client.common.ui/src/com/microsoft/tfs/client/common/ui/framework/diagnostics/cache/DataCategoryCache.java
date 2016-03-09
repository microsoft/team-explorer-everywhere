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

public class DataCategoryCache {
    private Map categories;

    public DataCategory getByID(final String id) {
        synchronized (this) {
            if (categories == null) {
                populate();
            }
        }
        return (DataCategory) categories.get(id);
    }

    private void populate() {
        categories = new HashMap();

        final IExtension[] extensions = SupportManager.getExtensions(Constants.DATA_CATEGORIES_EXTENSION_POINT_NAME);

        for (int i = 0; i < extensions.length; i++) {
            final IConfigurationElement[] configElements = extensions[i].getConfigurationElements();
            for (int j = 0; j < configElements.length; j++) {
                final DataCategory dataCategory = getDataCategory(configElements[j]);
                if (dataCategory != null) {
                    if (categories.containsKey(dataCategory.getID())) {
                        final String messageFormat = "data category [{0}] has been defined multiple times {1}"; //$NON-NLS-1$
                        final String message = MessageFormat.format(
                            messageFormat,
                            dataCategory.getID(),
                            Utils.messageFor(configElements[j], Constants.DATA_CATEGORIES_EXTENSION_POINT_NAME));

                        SupportManager.log(IStatus.WARNING, message);
                    }

                    categories.put(dataCategory.getID(), dataCategory);
                }
            }
        }
    }

    private DataCategory getDataCategory(final IConfigurationElement element) {
        if (!Utils.checkConfigElement(
            element,
            Constants.ELEMENT_NAME_CATEGORY,
            Constants.DATA_CATEGORIES_EXTENSION_POINT_NAME)) {
            return null;
        }

        final String id = Utils.getRequiredAttribute(
            element,
            Constants.ATTRIBUTE_NAME_ID,
            Constants.DATA_CATEGORIES_EXTENSION_POINT_NAME);

        if (id == null) {
            return null;
        }

        final String label = Utils.getRequiredAttribute(
            element,
            Constants.ATTRIBUTE_NAME_LABEL,
            Constants.DATA_CATEGORIES_EXTENSION_POINT_NAME);

        if (label == null) {
            return null;
        }

        final String labelNOLOC = Utils.getRequiredAttribute(
            element,
            Constants.ATTRIBUTE_NAME_LABEL_NOLOC,
            Constants.DATA_CATEGORIES_EXTENSION_POINT_NAME);

        if (labelNOLOC == null) {
            return null;
        }

        return new DataCategory(id, label, labelNOLOC);
    }
}
