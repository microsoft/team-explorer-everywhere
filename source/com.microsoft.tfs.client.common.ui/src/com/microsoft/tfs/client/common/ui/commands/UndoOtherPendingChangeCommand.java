// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.commands;

import java.text.MessageFormat;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import com.microsoft.tfs.client.common.commands.TFSCommand;
import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.core.clients.versioncontrol.ChangePendedFlags;
import com.microsoft.tfs.core.clients.versioncontrol.GetOptions;
import com.microsoft.tfs.core.clients.versioncontrol.VersionControlClient;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Failure;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Workspace;
import com.microsoft.tfs.core.clients.versioncontrol.specs.ItemSpec;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.LocaleUtil;

public class UndoOtherPendingChangeCommand extends TFSCommand {
    private final Workspace workspace;

    private final VersionControlClient vcClient;
    private final String workspaceName;
    private final String workspaceOwner;

    private final ItemSpec[] items;

    private final AtomicReference<Failure[]> failuresHolder = new AtomicReference<Failure[]>();
    private final AtomicBoolean onlineOperationHolder = new AtomicBoolean();
    private final AtomicReference<ChangePendedFlags> changePendedFlagsHolder = new AtomicReference<ChangePendedFlags>();

    public UndoOtherPendingChangeCommand(final Workspace workspace, final ItemSpec[] items) {
        Check.notNull(workspace, "workspace"); //$NON-NLS-1$
        Check.notNull(items, "items"); //$NON-NLS-1$

        this.workspace = workspace;
        this.vcClient = null;
        this.workspaceName = null;
        this.workspaceOwner = null;
        this.items = items;
    }

    public UndoOtherPendingChangeCommand(
        final VersionControlClient vcClient,
        final String workspaceName,
        final String workspaceOwner,
        final ItemSpec[] items) {
        Check.notNull(vcClient, "vcClient"); //$NON-NLS-1$
        Check.notNull(workspaceName, "workspaceName"); //$NON-NLS-1$
        Check.notNull(workspaceOwner, "workspaceOwner"); //$NON-NLS-1$
        Check.notNull(items, "items"); //$NON-NLS-1$

        this.workspace = null;
        this.vcClient = vcClient;
        this.workspaceName = workspaceName;
        this.workspaceOwner = workspaceOwner;
        this.items = items;
    }

    @Override
    public String getName() {
        if (items.length == 1) {
            return MessageFormat.format(
                Messages.getString("UndoOtherPendingChangeCommand.CommandNameSingleChangeFormat"), //$NON-NLS-1$
                items[0].getItem());
        } else {
            return Messages.getString("UndoOtherPendingChangeCommand.CommandNameMultipleChanges"); //$NON-NLS-1$
        }
    }

    @Override
    public String getErrorDescription() {
        if (items.length == 1) {
            return Messages.getString("UndoOtherPendingChangeCommand.ErrorDescriptionSingleChange"); //$NON-NLS-1$
        } else {
            return Messages.getString("UndoOtherPendingChangeCommand.ErrorDescriptionMultipleChanges"); //$NON-NLS-1$
        }
    }

    @Override
    public String getLoggingDescription() {
        if (items.length == 1) {
            return MessageFormat.format(
                Messages.getString("UndoOtherPendingChangeCommand.CommandNameSingleChangeFormat", LocaleUtil.ROOT), //$NON-NLS-1$
                items[0].getItem());
        } else {
            return Messages.getString("UndoOtherPendingChangeCommand.CommandNameMultipleChanges", LocaleUtil.ROOT); //$NON-NLS-1$
        }
    }

    @Override
    protected IStatus doRun(final IProgressMonitor progressMonitor) throws Exception {
        if (workspace != null) {
            workspace.undo(items, GetOptions.NONE);
        } else {
            // Call the service directly for non-local workspaces
            vcClient.getWebServiceLayer().undoPendingChanges(
                workspaceName,
                workspaceOwner,
                items,
                failuresHolder,
                null,
                null,
                onlineOperationHolder,
                false,
                changePendedFlagsHolder);
        }

        return Status.OK_STATUS;
    }

    public Failure[] getFailures() {
        return failuresHolder.get();
    }

    public boolean isOnlineOperation() {
        return onlineOperationHolder.get();
    }

    public ChangePendedFlags getChangePendedFlags() {
        return changePendedFlagsHolder.get();
    }

}
