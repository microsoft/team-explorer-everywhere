// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.eclipse.ui.resourcechange;

import java.text.MessageFormat;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import com.microsoft.tfs.client.common.ui.framework.helper.ShellUtils;
import com.microsoft.tfs.client.eclipse.resourcechange.TFSResourceChangeStatusReporter;
import com.microsoft.tfs.client.eclipse.ui.Messages;
import com.microsoft.tfs.client.eclipse.ui.TFSEclipseClientUIPlugin;
import com.microsoft.tfs.core.clients.versioncontrol.events.NonFatalErrorEvent;

/**
 * Notifies the user in pop-up dialogs about the statuses that resulted from
 * pending changes on Eclipse resources.
 */
public class TFSResourceChangeUIStatusReporter implements TFSResourceChangeStatusReporter {
    @Override
    public void reportNonOKVisitorStatus(final IStatus status) {
        if (status.isOK()) {
            return;
        }

        Display.getDefault().asyncExec(new Runnable() {
            @Override
            public void run() {
                final Shell shell = ShellUtils.getBestParent(ShellUtils.getWorkbenchShell());
                ErrorDialog.openError(
                    shell,
                    Messages.getString("TFSResourceChangeUiProvider.VisitorErrorTitle"), //$NON-NLS-1$
                    null,
                    status);
            }
        });
    }

    @Override
    public void reportConvertToEditStatus(final IStatus status, final NonFatalErrorEvent[] nonFatals) {
        reportStatus(
            Messages.getString("TFSResourceChangeUIStatusReporter.ConvertToEditErrorTitle"), //$NON-NLS-1$
            Messages.getString("TFSResourceChangeUIStatusReporter.ConvertToEditSingleErrorMessageFormat"), //$NON-NLS-1$
            Messages.getString("TFSResourceChangeUIStatusReporter.ConvertToEditMultipleErrorMessageFormat"), //$NON-NLS-1$
            status,
            nonFatals);
    }

    @Override
    public void reportAdditionStatus(final IStatus status, final NonFatalErrorEvent[] nonFatals) {
        /*
         * Special case for addition status handling: we do not want to raise UI
         * on successful adds that had non-fatals. Let non-fatals go only to the
         * console.
         */
        if (status.isOK()) {
            return;
        }

        reportStatus(
            Messages.getString("TFSResourceChangeUiProvider.AdditionErrorTitle"), //$NON-NLS-1$
            Messages.getString("TFSResourceChangeUiProvider.AdditionSingleErrorMessageFormat"), //$NON-NLS-1$
            Messages.getString("TFSResourceChangeUiProvider.AdditionMultipleErrorMessageFormat"), //$NON-NLS-1$
            status,
            nonFatals);
    }

    @Override
    public void reportScanStatus(final IStatus status, final NonFatalErrorEvent[] nonFatals) {
        reportStatus(
            Messages.getString("TFSResourceChangeUiProvider.EditErrorTitle"), //$NON-NLS-1$
            Messages.getString("TFSResourceChangeUiProvider.EditSingleErrorMessageFormat"), //$NON-NLS-1$
            Messages.getString("TFSResourceChangeUiProvider.EditMultipleErrorMessageFormat"), //$NON-NLS-1$
            status,
            nonFatals);
    }

    private void reportStatus(
        final String messageTitle,
        final String singleMessageFormat,
        final String multipleMessageFormat,
        final IStatus status,
        final NonFatalErrorEvent[] nonFatals) {
        final IStatus reportStatus;

        /* No errors */
        if (status.isOK() && (nonFatals == null || nonFatals.length == 0)) {
            return;
        }

        /* Command execution error (eg, server is unavailable) */
        else if (!status.isOK() && (nonFatals == null || nonFatals.length == 0)) {
            reportStatus = new Status(
                IStatus.ERROR,
                TFSEclipseClientUIPlugin.PLUGIN_ID,
                0,
                MessageFormat.format(singleMessageFormat, status.getMessage()),
                status.getException());
        }

        /*
         * Command succeeded but produced a single non-fatal. Special case to
         * not use a MultiStatus, which shows up ugly in MessageDialog.
         */
        else if (status.isOK() && nonFatals != null && nonFatals.length == 1) {
            final String message = scrubNonFatalMessage(nonFatals[0].getMessage());

            reportStatus = new Status(
                IStatus.ERROR,
                TFSEclipseClientUIPlugin.PLUGIN_ID,
                0,
                MessageFormat.format(singleMessageFormat, message),
                null);
        }

        /* Multiple errors */
        else {
            final int errorCount = nonFatals.length + (status.isOK() ? 0 : 1);
            final MultiStatus multiStatus = new MultiStatus(
                TFSEclipseClientUIPlugin.PLUGIN_ID,
                0,
                MessageFormat.format(multipleMessageFormat, errorCount),
                null);

            if (!status.isOK()) {
                multiStatus.merge(status);
            }

            for (int i = 0; i < nonFatals.length; i++) {
                multiStatus.add(
                    new Status(
                        IStatus.ERROR,
                        TFSEclipseClientUIPlugin.PLUGIN_ID,
                        0,
                        scrubNonFatalMessage(nonFatals[i].getMessage()),
                        null));
            }

            reportStatus = multiStatus;
        }

        Display.getDefault().asyncExec(new Runnable() {
            @Override
            public void run() {
                final Shell shell = ShellUtils.getBestParent(ShellUtils.getWorkbenchShell());
                ErrorDialog.openError(shell, messageTitle, null, reportStatus);
            }
        });
    }

    private String scrubNonFatalMessage(final String message) {
        return message.replaceFirst("^TF[\\d]+: ", ""); //$NON-NLS-1$ //$NON-NLS-2$
    }
}
