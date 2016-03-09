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
import com.microsoft.tfs.client.common.ui.framework.diagnostics.extend.DataProvider;
import com.microsoft.tfs.client.common.ui.framework.diagnostics.extend.ExportType;

public class DataProviderCache {
    private final DataCategoryCache dataCategoryCache;
    private final DataProviderActionCache dataProviderActionCache;
    private final ExportHandlerCache exportHandlerCache;
    private Map providers;

    public DataProviderCache(
        final DataCategoryCache dataCategoryCache,
        final DataProviderActionCache dataProviderActionCache,
        final ExportHandlerCache exportHandlerCache) {
        this.dataCategoryCache = dataCategoryCache;
        this.dataProviderActionCache = dataProviderActionCache;
        this.exportHandlerCache = exportHandlerCache;
    }

    public synchronized void contributeDataProvider(
        final String id,
        final String label,
        final String labelNOLOC,
        final DataProvider dataProvider,
        final String categoryId,
        final ExportType exportType,
        final boolean isOwnTab) {
        if (providers == null) {
            populate();
        }

        if (providers.containsKey(id)) {
            final String messageFormat = "the id [{0}] already exists"; //$NON-NLS-1$
            final String message = MessageFormat.format(messageFormat, id);
            throw new IllegalArgumentException(message);
        }

        final DataCategory dataCategory = dataCategoryCache.getByID(categoryId);

        if (dataCategory == null) {
            final String messageFormat = "the data category id [{0}] is invalid"; //$NON-NLS-1$
            final String message = MessageFormat.format(messageFormat, categoryId);
            throw new IllegalArgumentException(message);
        }

        final DataProviderInfo dpi =
            new DataProviderInfo(id, label, labelNOLOC, dataProvider, dataCategory, exportType, isOwnTab);

        providers.put(dpi.getID(), dpi);
    }

    public synchronized void replace(final String id, final DataProvider provider) {
        if (providers == null) {
            populate();
        }

        final DataProviderInfo oldDpi = (DataProviderInfo) providers.get(id);
        if (oldDpi == null) {
            final String messageFormat = "the id [{0}] does not exist"; //$NON-NLS-1$
            final String message = MessageFormat.format(messageFormat, id);
            throw new IllegalArgumentException(message);
        }

        final DataProviderInfo newDpi = new DataProviderInfo(
            oldDpi.getID(),
            oldDpi.getLabel(),
            oldDpi.getLabelNOLOC(),
            provider,
            oldDpi.getCategory(),
            oldDpi.getExportType(),
            oldDpi.isOwnTab());

        providers.put(id, newDpi);
    }

    public void replaceWith(final DataProvider provider) {
        replace(provider.getClass().getName(), provider);
    }

    public DataProviderInfo[] getProviders() {
        synchronized (this) {
            if (providers == null) {
                populate();
            }
        }
        return (DataProviderInfo[]) providers.values().toArray(new DataProviderInfo[providers.size()]);
    }

    private void populate() {
        providers = new HashMap();

        final IExtension[] extensions = SupportManager.getExtensions(Constants.DATA_PROVIDERS_EXTENSION_POINT_NAME);

        for (int i = 0; i < extensions.length; i++) {
            final IConfigurationElement[] configElements = extensions[i].getConfigurationElements();
            for (int j = 0; j < configElements.length; j++) {
                final DataProviderInfo dataProvider = getDataProvider(configElements[j]);
                if (dataProvider != null) {
                    providers.put(dataProvider.getID(), dataProvider);
                }
            }
        }
    }

    private DataProviderInfo getDataProvider(final IConfigurationElement element) {
        if (!Utils.checkConfigElement(
            element,
            Constants.ELEMENT_NAME_PROVIDER,
            Constants.DATA_PROVIDERS_EXTENSION_POINT_NAME)) {
            return null;
        }

        final String id = Utils.getRequiredAttribute(
            element,
            Constants.ATTRIBUTE_NAME_ID,
            Constants.DATA_PROVIDERS_EXTENSION_POINT_NAME);

        if (id == null) {
            return null;
        }

        final String label = Utils.getRequiredAttribute(
            element,
            Constants.ATTRIBUTE_NAME_LABEL,
            Constants.DATA_PROVIDERS_EXTENSION_POINT_NAME);

        if (label == null) {
            return null;
        }

        final String labelNOLOC = Utils.getRequiredAttribute(
            element,
            Constants.ATTRIBUTE_NAME_LABEL_NOLOC,
            Constants.DATA_PROVIDERS_EXTENSION_POINT_NAME);

        if (labelNOLOC == null) {
            return null;
        }

        final String categoryId = Utils.getRequiredAttribute(
            element,
            Constants.ATTRIBUTE_NAME_CATEGORY_ID,
            Constants.DATA_PROVIDERS_EXTENSION_POINT_NAME);

        if (categoryId == null) {
            return null;
        }

        final DataCategory dataCategory = dataCategoryCache.getByID(categoryId);

        if (dataCategory == null) {
            final String messageFormat = "data category id [{0}] is undefined {1}"; //$NON-NLS-1$
            final String message = MessageFormat.format(
                messageFormat,
                categoryId,
                Utils.messageFor(element, Constants.DATA_PROVIDERS_EXTENSION_POINT_NAME));

            SupportManager.log(IStatus.WARNING, message);
            return null;
        }

        final DataProvider dataProvider = (DataProvider) Utils.createExecutableExtension(
            element,
            Constants.ATTRIBUTE_NAME_CLASS,
            DataProvider.class,
            Constants.DATA_PROVIDERS_EXTENSION_POINT_NAME);

        if (dataProvider == null) {
            return null;
        }

        ExportType exportType;
        final String exportTypeString = element.getAttribute(Constants.ATTRIBUTE_NAME_EXPORT_TYPE);
        if (exportTypeString != null && exportTypeString.trim().length() > 0) {
            try {
                exportType = ExportType.fromString(exportTypeString);
            } catch (final IllegalArgumentException ex) {
                final String messageFormat = "invalid export type [{0}] {1}"; //$NON-NLS-1$
                final String message = MessageFormat.format(
                    messageFormat,
                    exportTypeString,
                    Utils.messageFor(element, Constants.DATA_PROVIDERS_EXTENSION_POINT_NAME));

                SupportManager.log(IStatus.WARNING, message);
                return null;
            }
        } else {
            exportType = ExportType.ALWAYS;
        }

        final boolean ownTab = Utils.getNonRequiredBooleanAttribute(element, Constants.ATTRIBUTE_NAME_OWN_TAB, false);

        final DataProviderInfo info =
            new DataProviderInfo(id, label, labelNOLOC, dataProvider, dataCategory, exportType, ownTab);
        populateActions(info, element);
        populateExportHandlers(info, element);
        return info;
    }

    private void populateActions(final DataProviderInfo dpi, final IConfigurationElement configElement) {
        final IConfigurationElement[] children = configElement.getChildren();
        for (int i = 0; i < children.length; i++) {
            if (Constants.ELEMENT_NAME_ACTION.equals(children[i].getName())) {
                final String id = Utils.getRequiredAttribute(
                    children[i],
                    Constants.ATTRIBUTE_NAME_ID,
                    Constants.DATA_PROVIDERS_EXTENSION_POINT_NAME);
                if (id == null) {
                    continue;
                }

                final boolean isDefault =
                    Utils.getNonRequiredBooleanAttribute(children[i], Constants.ATTRIBUTE_NAME_DEFAULT, false);

                final DataProviderActionInfo action = dataProviderActionCache.getByID(id);

                if (action == null) {
                    final String messageFormat = "unknown data provider action id [{0}] {1}"; //$NON-NLS-1$
                    final String message = MessageFormat.format(
                        messageFormat,
                        id,
                        Utils.messageFor(children[i], Constants.DATA_PROVIDERS_EXTENSION_POINT_NAME));

                    SupportManager.log(IStatus.WARNING, message);
                    continue;
                }

                dpi.addAction(action, isDefault);
            }
        }
    }

    private void populateExportHandlers(final DataProviderInfo dpi, final IConfigurationElement configElement) {
        final IConfigurationElement[] children = configElement.getChildren();
        for (int i = 0; i < children.length; i++) {
            if (Constants.ELEMENT_NAME_EXPORT_HANDLER.equals(children[i].getName())) {
                final String id = Utils.getRequiredAttribute(
                    children[i],
                    Constants.ATTRIBUTE_NAME_ID,
                    Constants.DATA_PROVIDERS_EXTENSION_POINT_NAME);
                if (id == null) {
                    continue;
                }

                final ExportHandlerInfo exportHandler = exportHandlerCache.getByID(id);

                if (exportHandler == null) {
                    final String messageFormat = "unknown export handler id [{0}] {1}"; //$NON-NLS-1$
                    final String message = MessageFormat.format(
                        messageFormat,
                        id,
                        Utils.messageFor(children[i], Constants.DATA_PROVIDERS_EXTENSION_POINT_NAME));

                    SupportManager.log(IStatus.WARNING, message);
                    continue;
                }

                final String configData = children[i].getAttribute(Constants.ATTRIBUTE_NAME_CONFIG_DATA);

                dpi.addExportHandler(new ExportHandlerReference(exportHandler, configData));
            }
        }
    }
}
