// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.framework.diagnostics.export;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.text.MessageFormat;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.operation.IRunnableWithProgress;

import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.framework.diagnostics.Adapters;
import com.microsoft.tfs.client.common.ui.framework.diagnostics.DiagnosticLocale;
import com.microsoft.tfs.client.common.ui.framework.diagnostics.cache.DataCategory;
import com.microsoft.tfs.client.common.ui.framework.diagnostics.cache.DataProviderCollection;
import com.microsoft.tfs.client.common.ui.framework.diagnostics.cache.DataProviderWrapper;
import com.microsoft.tfs.client.common.ui.framework.diagnostics.cache.ExportHandlerReference;

public class ExportRunnable implements IRunnableWithProgress {
    private static final Log log = LogFactory.getLog(ExportRunnable.class);

    private static final String DATA_FILENAME = "data.txt"; //$NON-NLS-1$

    private final DataProviderCollection allProvidersCollection;
    private final File outputFile;

    private Throwable error;
    private boolean errored;
    private boolean cancelled;

    public ExportRunnable(final DataProviderCollection dataProviderCollection, final File outputFile) {
        allProvidersCollection = dataProviderCollection;
        this.outputFile = outputFile;
    }

    public Throwable getError() {
        return error;
    }

    public boolean wasCancelled() {
        return cancelled;
    }

    @Override
    public void run(final IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
        error = null;
        errored = false;

        ZipOutputStream zipout = null;

        final DataProviderWrapper[] exportableDataProviders = allProvidersCollection.getExportableDataProviders();
        final DataProviderCollection exportableCollection = new DataProviderCollection(exportableDataProviders);
        final DataProviderWrapper[] providersWithExportHandlers =
            exportableCollection.getDataProvidersWithExportHandlers();

        monitor.beginTask(
            Messages.getString("ExportRunnable.ExportingProgress"), //$NON-NLS-1$
            exportableDataProviders.length + providersWithExportHandlers.length);

        try {
            final FileOutputStream fos = new FileOutputStream(outputFile);
            final BufferedOutputStream bos = new BufferedOutputStream(fos);
            zipout = new ZipOutputStream(bos);

            processDataProviders(exportableCollection, zipout, monitor);
            processExportHandlers(providersWithExportHandlers, zipout, monitor);
        } catch (final Throwable t) {
            error = t;
            errored = true;
        } finally {
            if (zipout != null) {
                try {
                    zipout.close();
                } catch (final IOException e) {
                }
            }

            cancelled = monitor.isCanceled();

            if (cancelled || errored) {
                outputFile.delete();
            }
        }
    }

    private void processExportHandlers(
        final DataProviderWrapper[] dataProviders,
        final ZipOutputStream zipout,
        final IProgressMonitor monitor) throws IOException {
        for (int i = 0; i < dataProviders.length; i++) {
            if (monitor.isCanceled()) {
                return;
            }

            final ExportHandlerReference[] exportHandlers = dataProviders[i].getDataProviderInfo().getExportHandlers();

            for (int j = 0; j < exportHandlers.length; j++) {
                final Object data = dataProviders[i].getDataNOLOC();

                try {
                    exportHandlers[j].export(data, zipout);
                } catch (final Throwable t) {
                    final String messageFormat = "export handler [{0}] failed"; //$NON-NLS-1$
                    final String message = MessageFormat.format(messageFormat, exportHandlers[j]);
                    log.warn(message, t);
                }
            }
        }
    }

    private void processDataProviders(
        final DataProviderCollection exportableCollection,
        final ZipOutputStream zipout,
        final IProgressMonitor monitor) throws IOException {
        final ZipEntry entry = new ZipEntry(DATA_FILENAME);
        zipout.putNextEntry(entry);
        final PrintWriter pw = new PrintWriter(zipout);

        final DataCategory[] categories = exportableCollection.getSortedCategories();

        for (int i = 0; i < categories.length; i++) {
            final DataProviderWrapper[] providersForCurrentCategory =
                exportableCollection.getSortedProvidersForCategory(categories[i]);

            for (int j = 0; j < providersForCurrentCategory.length; j++) {
                if (monitor.isCanceled()) {
                    return;
                }

                /*
                 * Do not use localized data - this is for consumption by our
                 * support staff
                 */
                final String data =
                    (String) Adapters.get(providersForCurrentCategory[j].getDataNOLOC(), String.class, false);
                if (data != null) {
                    final String messageFormat =
                        //@formatter:off
                        Messages.getString("ExportRunnable.CategoryOutputFormat", DiagnosticLocale.SUPPORT_LOCALE); //$NON-NLS-1$
                        //@formatter:on
                    final String message = MessageFormat.format(
                        messageFormat,
                        categories[i].getLabelNOLOC(),
                        providersForCurrentCategory[j].getDataProviderInfo().getLabelNOLOC());

                    pw.println(message);
                    pw.println();
                    pw.println(data);
                    pw.println();
                }

                monitor.worked(1);
            }
        }

        pw.flush();
    }
}
