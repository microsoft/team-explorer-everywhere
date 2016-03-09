// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.tasks.vc;

import java.text.MessageFormat;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Shell;

import com.microsoft.tfs.client.common.commands.TFSCommand;
import com.microsoft.tfs.client.common.repository.TFSRepository;
import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.framework.dialog.BaseDialog;
import com.microsoft.tfs.client.common.ui.tasks.BaseTask;
import com.microsoft.tfs.client.common.ui.vc.serveritem.ServerItemType;
import com.microsoft.tfs.client.common.ui.vc.serveritem.TypedServerItem;
import com.microsoft.tfs.core.clients.versioncontrol.path.ServerPath;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.WorkingFolder;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Workspace;
import com.microsoft.tfs.util.Check;

/**
 * An abstract base class for all working folder related tasks such as add,
 * remove, cloak, and uncloak. All operations follow the pattern of displaying a
 * dialog, running a command, and then optionally retrieving the latest versions
 * for the target server path.
 *
 *
 * @threadsafety unknown
 */
public abstract class WorkingFolderTask extends BaseTask {
    final protected TFSRepository repository;
    final protected String serverPath;
    final private boolean getLatestOnSuccess;

    public WorkingFolderTask(
        final Shell shell,
        final TFSRepository repository,
        final String serverPath,
        final boolean getLatestOnSuccess) {
        super(shell);

        Check.notNull(repository, "repository"); //$NON-NLS-1$
        Check.notNull(serverPath, "serverPath"); //$NON-NLS-1$

        this.repository = repository;
        this.serverPath = serverPath;
        this.getLatestOnSuccess = getLatestOnSuccess;
    }

    /**
     * Get the dialog which will present the options for the working folder
     * command.
     *
     *
     * @return A BaseDialog for the operation.
     */
    public abstract BaseDialog getDialog();

    /**
     * Get the TFSCommand to execute the working folder operation.
     *
     *
     * @return A TFSCommand to execute.
     */
    public abstract TFSCommand getCommand();

    /**
     * Retrieve the latest version of folders and files from the target path.
     * Depending on the operation this may involve prompting with a choice to
     * skip.
     */
    public abstract void getLatest();

    @Override
    public IStatus run() {
        if (getDialog().open() == IDialogConstants.CANCEL_ID) {
            return Status.CANCEL_STATUS;
        }

        final TFSCommand command = getCommand();
        final IStatus status = getCommandExecutor().execute(command);

        if (status.isOK() && getLatestOnSuccess) {
            getLatest();
        }

        return status;
    }

    protected void getLatestForServerPath(final String promptTitle) {
        final String messageFormat = Messages.getString("WorkingFolderTask.GetLatestFolderFormat"); //$NON-NLS-1$
        final String message = MessageFormat.format(messageFormat, serverPath);
        final boolean getNow = MessageDialog.openQuestion(getShell(), promptTitle, message);

        if (getNow) {
            final TypedServerItem[] items = new TypedServerItem[] {
                new TypedServerItem(serverPath, ServerItemType.FOLDER),
            };

            final GetLatestTask task = new GetLatestTask(getShell(), repository, items);
            task.run();
        }
    }

    protected void getLatestForLocalPath(final String localPath, final boolean suppressFileUpToDateMessage) {
        final GetLatestTask task = new GetLatestTask(getShell(), repository, localPath);
        task.setSuppressFileUpToDateMessage(suppressFileUpToDateMessage);
        task.run();
    }

    /**
     * Construct a local path hint for a serverPath. Look up the parent chain to
     * find the first non-cloaked mapping. Use the local path of this mapping
     * combined with the relative difference between the original server path
     * and the mapped parent. E.g. If the original path is "$/a/b/c/d" and the
     * closest non-cloaked parent is "$/a" (mapped to c:\root) then the
     * resulting path would be "c:\root\b\c\d"
     *
     *
     * @param workspace
     *        A workspace.
     * @param serverPath
     *        The original serverPath for which to find a hint for a local path.
     * @return The suggested local path or null.
     */
    protected String getLocalPathHint(final Workspace workspace, final String serverPath) {
        Check.notNull(workspace, "workspace"); //$NON-NLS-1$
        Check.notNullOrEmpty(serverPath, "serverPath"); //$NON-NLS-1$

        WorkingFolder closestMapping;
        String closestServerPath = serverPath;

        do {
            closestServerPath = ServerPath.getParent(closestServerPath);
            closestMapping = workspace.getClosestMappingForServerPath(closestServerPath);
            if (closestMapping != null) {
                closestServerPath = closestMapping.getServerItem();
            }
        } while (closestMapping != null && closestMapping.getLocalItem() == null);

        if (closestMapping == null) {
            return null;
        }

        return ServerPath.makeLocal(serverPath, closestMapping.getServerItem(), closestMapping.getLocalItem());
    }
}
