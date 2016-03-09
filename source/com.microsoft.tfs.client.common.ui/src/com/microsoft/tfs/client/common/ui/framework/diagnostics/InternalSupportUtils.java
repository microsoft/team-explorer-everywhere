// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.framework.diagnostics;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Shell;

import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.TFSCommonUIClientPlugin;
import com.microsoft.tfs.client.common.ui.framework.diagnostics.cache.CollectionRunnable;
import com.microsoft.tfs.client.common.ui.framework.diagnostics.cache.DataProviderCollection;
import com.microsoft.tfs.client.common.ui.framework.diagnostics.cache.DataProviderInfo;
import com.microsoft.tfs.client.common.ui.framework.diagnostics.cache.SupportProvider;
import com.microsoft.tfs.client.common.ui.framework.diagnostics.export.ExportRunnable;
import com.microsoft.tfs.client.common.ui.framework.launcher.Launcher;
import com.microsoft.tfs.client.common.ui.framework.runnable.DeferredProgressMonitorDialogContext;

public class InternalSupportUtils {
    private static final Log log = LogFactory.getLog(InternalSupportUtils.class);

    private static final String EXPORT_FILE_DATE_FORMAT = "MM-dd-yy_HH-mm-ss"; //$NON-NLS-1$

    public static String promptForExportFile(final Shell shell) {
        final FileDialog dlg = new FileDialog(shell, SWT.SAVE);
        dlg.setFilterNames(new String[] {
            "*.zip" //$NON-NLS-1$
        });
        dlg.setFilterExtensions(new String[] {
            "*.zip" //$NON-NLS-1$
        });

        final SupportProvider supportProvider =
            SupportManager.getInstance().getSupportProviderCache().getSupportProvider();
        if (supportProvider == null) {
            throw new IllegalStateException();
        }
        final SimpleDateFormat dateFormat = new SimpleDateFormat(EXPORT_FILE_DATE_FORMAT);
        final String name = supportProvider.getExportFilenamePrefix() + dateFormat.format(new Date()) + ".zip"; //$NON-NLS-1$

        dlg.setFileName(name);
        dlg.setText(Messages.getString("InternalSupportUtils.DialogTitle")); //$NON-NLS-1$
        return dlg.open();
    }

    public static boolean doExport(
        final Shell shell,
        final DataProviderCollection dataProviderCollection,
        final File outputFile) {
        final ExportRunnable runnable = new ExportRunnable(dataProviderCollection, outputFile);

        Throwable error = null;

        final DeferredProgressMonitorDialogContext context = new DeferredProgressMonitorDialogContext(shell, 500);

        try {
            context.run(true, true, runnable);
            error = runnable.getError();
        } catch (final InvocationTargetException e) {
            error = e.getTargetException();
        } catch (final InterruptedException e) {
            return false;
        }

        if (runnable.wasCancelled()) {
            return false;
        }

        if (error != null) {
            log.error(error);

            final String messageFormat = Messages.getString("InternalSupportUtils.ExportErrorStatusFormat"); //$NON-NLS-1$
            final String message = MessageFormat.format(messageFormat, error.getMessage());

            final Status status =
                new Status(IStatus.ERROR, TFSCommonUIClientPlugin.PLUGIN_ID, IStatus.OK, message, error);

            ErrorDialog.openError(
                shell,
                Messages.getString("InternalSupportUtils.ErrorDialogTitle"), //$NON-NLS-1$
                error.getMessage(),
                status);
            return false;
        }

        return true;
    }

    public static DataProviderCollection createDataProviderCollection(final Shell shell) {
        final DataProviderInfo[] dataProviders = SupportManager.getInstance().getDataProviderCache().getProviders();

        final CollectionRunnable runnable = new CollectionRunnable(dataProviders);

        final DeferredProgressMonitorDialogContext context = new DeferredProgressMonitorDialogContext(shell, 500);
        try {
            context.run(true, true, runnable);
        } catch (final InvocationTargetException e) {
            throw new RuntimeException(e);
        } catch (final InterruptedException e) {
            return null;
        }

        if (runnable.wasCancelled()) {
            return null;
        }

        return new DataProviderCollection(runnable.getDataProviderWrappers());
    }

    public static void openFolderOfFile(final File file) {
        if (file.getParentFile() != null) {
            Launcher.launch(file.getParentFile().getAbsolutePath());
        }
    }

    public static void openFile(final File file) {
        Launcher.launch(file.getAbsolutePath());
    }

    public static boolean promptAndPerformExport(final Shell shell, final DataProviderCollection collection) {
        final String exportFileName = InternalSupportUtils.promptForExportFile(shell);
        if (exportFileName == null) {
            /*
             * cancelled
             */
            return false;
        }

        final File exportFile = new File(exportFileName);
        return InternalSupportUtils.doExport(shell, collection, exportFile);
    }
}
