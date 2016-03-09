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
import com.microsoft.tfs.client.common.ui.framework.diagnostics.extend.ExportHandler;

public class ExportHandlerCache {
    private Map exportHandlers;

    public ExportHandlerInfo getByID(final String id) {
        synchronized (this) {
            if (exportHandlers == null) {
                populate();
            }
        }
        return (ExportHandlerInfo) exportHandlers.get(id);
    }

    private void populate() {
        exportHandlers = new HashMap();

        final IExtension[] extensions = SupportManager.getExtensions(Constants.EXPORT_HANDLERS_EXTENSION_POINT_NAME);

        for (int i = 0; i < extensions.length; i++) {
            final IConfigurationElement[] configElements = extensions[i].getConfigurationElements();
            for (int j = 0; j < configElements.length; j++) {
                final ExportHandlerInfo exportHandler = getExportHandler(configElements[j]);
                if (exportHandler != null) {
                    if (exportHandlers.containsKey(exportHandler.getID())) {
                        final String messageFormat = "export handler [{0}] has been defined multiple times {1}"; //$NON-NLS-1$
                        final String message = MessageFormat.format(
                            messageFormat,
                            exportHandler.getID(),
                            Utils.messageFor(configElements[j], Constants.EXPORT_HANDLERS_EXTENSION_POINT_NAME));

                        SupportManager.log(IStatus.WARNING, message);
                    }

                    exportHandlers.put(exportHandler.getID(), exportHandler);
                }
            }
        }
    }

    private ExportHandlerInfo getExportHandler(final IConfigurationElement element) {
        if (!Utils.checkConfigElement(
            element,
            Constants.ELEMENT_NAME_EXPORT_HANDLER,
            Constants.EXPORT_HANDLERS_EXTENSION_POINT_NAME)) {
            return null;
        }

        final String id = Utils.getRequiredAttribute(
            element,
            Constants.ATTRIBUTE_NAME_ID,
            Constants.DATA_PROVIDER_ACTIONS_EXTENSION_POINT_NAME);

        if (id == null) {
            return null;
        }

        final ExportHandler exportHandler = (ExportHandler) Utils.createExecutableExtension(
            element,
            Constants.ATTRIBUTE_NAME_CLASS,
            ExportHandler.class,
            Constants.EXPORT_HANDLERS_EXTENSION_POINT_NAME);

        if (exportHandler == null) {
            return null;
        }

        return new ExportHandlerInfo(id, exportHandler);
    }
}
