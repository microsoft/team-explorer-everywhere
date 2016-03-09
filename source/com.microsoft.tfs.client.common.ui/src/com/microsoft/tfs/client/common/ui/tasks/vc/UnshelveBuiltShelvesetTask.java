// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.tasks.vc;

import java.text.MessageFormat;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Shell;

import com.microsoft.tfs.client.common.commands.vc.QueryShelvesetsCommand;
import com.microsoft.tfs.client.common.repository.TFSRepository;
import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.TFSCommonUIClientPlugin;
import com.microsoft.tfs.client.common.ui.controls.vc.changes.ChangeItem;
import com.microsoft.tfs.client.common.ui.dialogs.vc.ShelvesetDetailsDialog;
import com.microsoft.tfs.client.common.ui.prefs.UIPreferenceConstants;
import com.microsoft.tfs.core.clients.build.IBuildDetail;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.RecursionType;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Shelveset;
import com.microsoft.tfs.core.clients.versioncontrol.specs.ItemSpec;
import com.microsoft.tfs.core.clients.versioncontrol.specs.WorkspaceSpec;
import com.microsoft.tfs.util.Check;

public class UnshelveBuiltShelvesetTask extends AbstractUnshelveTask {
    private static final Log log = LogFactory.getLog(UnshelveBuiltShelvesetTask.class);

    private final TFSRepository repository;
    private final String shelvesetName;
    private final boolean promptForDetails;

    public UnshelveBuiltShelvesetTask(
        final Shell shell,
        final TFSRepository repository,
        final String shelvesetName,
        final boolean promptForDetails) {
        super(shell, repository);

        Check.notNull(repository, "repository"); //$NON-NLS-1$
        Check.notNull(shelvesetName, "shelvesetName"); //$NON-NLS-1$

        this.repository = repository;
        this.shelvesetName = shelvesetName;
        this.promptForDetails = promptForDetails;
    }

    public UnshelveBuiltShelvesetTask(
        final Shell shell,
        final TFSRepository repository,
        final IBuildDetail buildDetail,
        final boolean promptForDetails) {
        super(shell, repository);

        Check.notNull(repository, "repository"); //$NON-NLS-1$
        Check.notNull(buildDetail, "buildDetail"); //$NON-NLS-1$

        this.repository = repository;
        shelvesetName = buildDetail.getShelvesetName();
        this.promptForDetails = promptForDetails;
    }

    @Override
    public IStatus run() {
        if (shelvesetName == null) {
            final String message =
                Messages.getString("UnshelveBuiltShelvesetTask.BuildDoesNotContainShelvesetErrorMessage"); //$NON-NLS-1$
            MessageDialog.openError(
                getShell(),
                Messages.getString("UnshelveBuiltShelvesetTask.BuildDoesNotContainShelvesetErrorTitle"), //$NON-NLS-1$
                message);

            return new Status(IStatus.ERROR, TFSCommonUIClientPlugin.PLUGIN_ID, 0, message, null);
        }

        final WorkspaceSpec shelvesetSpec;

        try {
            shelvesetSpec = WorkspaceSpec.parse(
                shelvesetName,
                repository.getVersionControlClient().getConnection().getAuthorizedAccountName());
        } catch (final Exception e) {
            log.error("Could not parse shelveset in build details", e); //$NON-NLS-1$
            return new Status(IStatus.ERROR, TFSCommonUIClientPlugin.PLUGIN_ID, 0, e.getLocalizedMessage(), e);
        }

        final String shelvesetName = shelvesetSpec.getName();
        final String shelvesetOwner = shelvesetSpec.getOwner();

        final QueryShelvesetsCommand queryCommand =
            new QueryShelvesetsCommand(repository.getVersionControlClient(), shelvesetName, shelvesetOwner);

        final IStatus queryStatus = getCommandExecutor().execute(queryCommand);

        if (!queryStatus.isOK()) {
            return queryStatus;
        }

        final Shelveset[] shelvesets = queryCommand.getShelvesets();

        if (shelvesets.length != 1) {
            final String message =
                MessageFormat.format(
                    Messages.getString("UnshelveSpecificTask.ShelvesetNotFoundMessageFormat"), //$NON-NLS-1$
                    shelvesetName);

            MessageDialog.openError(
                getShell(),
                Messages.getString("UnshelveSpecificTask.ShelvesetNotFoundTitle"), //$NON-NLS-1$
                message);
            return new Status(IStatus.ERROR, TFSCommonUIClientPlugin.PLUGIN_ID, 0, message, null);
        }

        final Shelveset shelveset = shelvesets[0];
        ItemSpec[] itemSpecs = null;
        boolean preserveShelveset = true;
        boolean restoreData = true;

        final boolean autoResolveConflicts = TFSCommonUIClientPlugin.getDefault().getPreferenceStore().getBoolean(
            UIPreferenceConstants.AUTO_RESOLVE_CONFLICTS);

        if (promptForDetails) {
            final ShelvesetDetailsDialog detailsDialog =
                new ShelvesetDetailsDialog(getShell(), shelveset, repository, true);

            if (detailsDialog.open() != IDialogConstants.OK_ID) {
                return Status.CANCEL_STATUS;
            }

            final ChangeItem[] checkedChangeItems = detailsDialog.getCheckedChangeItems();
            preserveShelveset = detailsDialog.isPreserveShelveset();
            restoreData = detailsDialog.isRestoreData();

            if (checkedChangeItems != null) {
                itemSpecs = new ItemSpec[checkedChangeItems.length];

                for (int i = 0; i < checkedChangeItems.length; i++) {
                    itemSpecs[i] = new ItemSpec(checkedChangeItems[i].getServerItem(), RecursionType.NONE);
                }
            }
        }

        return unshelve(shelveset, itemSpecs, (preserveShelveset == false), restoreData, autoResolveConflicts);
    }
}
