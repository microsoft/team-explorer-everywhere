// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.eclipse.ui.filemodification;

import java.text.MessageFormat;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.widgets.Shell;

import com.microsoft.tfs.client.common.commands.vc.GetCommand;
import com.microsoft.tfs.client.common.framework.resources.command.ResourceChangingCommand;
import com.microsoft.tfs.client.common.repository.TFSRepository;
import com.microsoft.tfs.client.common.ui.framework.command.UICommandExecutorFactory;
import com.microsoft.tfs.client.eclipse.filemodification.TFSFileModificationStatusData;
import com.microsoft.tfs.client.eclipse.filemodification.TFSFileModificationStatusReporter;
import com.microsoft.tfs.client.eclipse.ui.Messages;
import com.microsoft.tfs.client.eclipse.ui.dialogs.vc.FileModificationFailureDialog;
import com.microsoft.tfs.core.clients.versioncontrol.GetOptions;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.GetRequest;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.RecursionType;
import com.microsoft.tfs.core.clients.versioncontrol.specs.ItemSpec;
import com.microsoft.tfs.core.clients.versioncontrol.specs.version.WorkspaceVersionSpec;
import com.microsoft.tfs.util.Check;

public class TFSFileModificationUIStatusReporter extends TFSFileModificationStatusReporter {
    private final Log log = LogFactory.getLog(TFSFileModificationUIStatusReporter.class);

    private final Shell shell;

    public TFSFileModificationUIStatusReporter(final Shell shell) {
        Check.notNull(shell, "shell"); //$NON-NLS-1$

        this.shell = shell;
    }

    @Override
    public void reportError(final String title, final IStatus status) {
        super.reportError(title, status);

        shell.getDisplay().asyncExec(new Runnable() {
            @Override
            public void run() {
                ErrorDialog.openError(shell, title, null, status);
            }
        });
    }

    @Override
    public void reportStatus(
        final TFSRepository repository,
        final TFSFileModificationStatusData[] statusData,
        final IStatus status) {
        super.reportStatus(repository, statusData, status);

        shell.getDisplay().asyncExec(new Runnable() {
            @Override
            public void run() {
                final FileModificationFailureDialog failureDialog =
                    new FileModificationFailureDialog(shell, statusData, status);

                if (failureDialog.open() == IDialogConstants.OK_ID) {
                    final GetRequest[] getRequests = new GetRequest[statusData.length];

                    for (int i = 0; i < getRequests.length; i++) {
                        getRequests[i] = new GetRequest(
                            new ItemSpec(statusData[i].getFile().getLocation().toOSString(), RecursionType.NONE),
                            new WorkspaceVersionSpec(repository.getWorkspace()));
                    }

                    final GetCommand revertCommand =
                        new GetCommand(repository, getRequests, GetOptions.combine(new GetOptions[] {
                            GetOptions.GET_ALL,
                            GetOptions.OVERWRITE
                    }));
                    final IStatus revertStatus = UICommandExecutorFactory.newUICommandExecutor(shell).execute(
                        new ResourceChangingCommand(revertCommand));

                    if (!revertStatus.isOK()) {
                        ErrorDialog.openError(
                            shell,
                            Messages.getString("TFSFileModificationUiStatusReporter.CouldNotRevertDialogTitle"), //$NON-NLS-1$
                            null,
                            revertStatus);
                    } else {
                        for (int i = 0; i < statusData.length; i++) {
                            try {
                                statusData[i].getFile().refreshLocal(IResource.DEPTH_ZERO, null);
                            } catch (final Exception e) {
                                final String messageFormat = "Could not refresh local file contents for {0}"; //$NON-NLS-1$
                                final String message = MessageFormat.format(
                                    messageFormat,
                                    statusData[i].getFile().getFullPath().toString());
                                log.error(message, e);
                            }
                        }
                    }
                }
            }
        });
    }
}
