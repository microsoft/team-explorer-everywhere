// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.framework.diagnostics.cache;

import java.text.MessageFormat;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.microsoft.tfs.client.common.ui.framework.diagnostics.extend.AvailableCallback;
import com.microsoft.tfs.client.common.ui.framework.diagnostics.extend.DataProvider;
import com.microsoft.tfs.client.common.ui.framework.diagnostics.extend.ExportType;
import com.microsoft.tfs.client.common.ui.framework.diagnostics.extend.PopulateCallback;

public class DataProviderWrapper implements Comparable {
    private static final Log log = LogFactory.getLog(DataProviderWrapper.class);

    private final DataProviderInfo dataProviderInfo;
    private boolean available;
    private Object data;
    private Object dataNOLOC;
    private boolean shouldExport;

    public DataProviderWrapper(final DataProviderInfo dataProviderInfo) {
        this.dataProviderInfo = dataProviderInfo;
        shouldExport = (dataProviderInfo.getExportType() == ExportType.ALWAYS);
    }

    @Override
    public int compareTo(final Object o) {
        final DataProviderWrapper other = (DataProviderWrapper) o;
        return dataProviderInfo.compareTo(other.dataProviderInfo);
    }

    public void populate() {
        available = true;
        final DataProvider provider = dataProviderInfo.getDataProvider();

        if (provider instanceof PopulateCallback) {
            final PopulateCallback callback = (PopulateCallback) provider;
            try {
                callback.populate();
            } catch (final Throwable t) {
                final String messageFormat = "data provider [{0}] failed populate"; //$NON-NLS-1$
                final String message = MessageFormat.format(messageFormat, dataProviderInfo.getID());

                log.warn(message, t);
                available = false;
                return;
            }
        }

        if (provider instanceof AvailableCallback) {
            final AvailableCallback callback = (AvailableCallback) provider;
            try {
                available = callback.isAvailable();
            } catch (final Throwable t) {
                final String messageFormat = "data provider [{0}] failed isAvailable"; //$NON-NLS-1$
                final String message = MessageFormat.format(messageFormat, dataProviderInfo.getID());

                log.warn(message, t);
                available = false;
                return;
            }
        }

        if (available) {
            try {
                data = provider.getData();
                dataNOLOC = provider.getDataNOLOC();
            } catch (final Throwable t) {
                final String messageFormat = "data provider [{0}] failed getData"; //$NON-NLS-1$
                final String message = MessageFormat.format(messageFormat, dataProviderInfo.getID());

                log.warn(message, t);
                available = false;
                return;
            }
        }
    }

    public boolean isAvailable() {
        return available;
    }

    public Object getData() {
        return data;
    }

    public Object getDataNOLOC() {
        return dataNOLOC;
    }

    public DataProviderInfo getDataProviderInfo() {
        return dataProviderInfo;
    }

    public boolean isShouldExport() {
        return shouldExport;
    }

    public void setShouldExport(final boolean shouldExport) {
        this.shouldExport = shouldExport;
    }
}
