// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.tasks.vc;

import java.util.Calendar;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.swt.widgets.Shell;

import com.microsoft.tfs.client.common.codemarker.CodeMarker;
import com.microsoft.tfs.client.common.codemarker.CodeMarkerDispatch;
import com.microsoft.tfs.client.common.commands.vc.ShelveCommand;
import com.microsoft.tfs.client.common.framework.resources.command.ResourceChangingCommand;
import com.microsoft.tfs.client.common.repository.TFSRepository;
import com.microsoft.tfs.client.common.ui.controls.vc.checkin.PendingCheckinSaveableFilter;
import com.microsoft.tfs.client.common.ui.helpers.EditorHelper;
import com.microsoft.tfs.client.common.ui.tasks.BaseTask;
import com.microsoft.tfs.core.clients.versioncontrol.VersionControlConstants;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Shelveset;
import com.microsoft.tfs.core.pendingcheckin.PendingCheckin;

/**
 * Performs the shelve procedure, raising a dialog, then running the
 * {@link ShelveCommand}.
 *
 * @threadsafety unknown
 */
public abstract class AbstractShelveTask extends BaseTask {
    public static final CodeMarker SHELVE_TASK_COMPLETE =
        new CodeMarker("com.microsoft.tfs.client.common.ui.tasks.vc.ShelveTask#shelveTaskComplete"); //$NON-NLS-1$

    protected final TFSRepository repository;
    protected String selectedShelvesetName;

    protected AbstractShelveTask(final Shell shell, final TFSRepository repository) {
        super(shell);
        this.repository = repository;
    }

    protected abstract PendingCheckin getPendingCheckin();

    protected abstract String getShelvesetName();

    protected abstract boolean getPreserveChangesFlag();

    protected abstract boolean userCanceledShelve();

    @Override
    public IStatus run() {
        final PendingCheckin pendingCheckin = getPendingCheckin();
        final String shelvesetName = getShelvesetName();
        final boolean preserveFlag = getPreserveChangesFlag();

        if (userCanceledShelve()) {
            return Status.OK_STATUS;
        }

        if (EditorHelper.saveAllDirtyEditors(new PendingCheckinSaveableFilter(pendingCheckin)) == false) {
            return Status.CANCEL_STATUS;
        }

        selectedShelvesetName = shelvesetName;

        final Shelveset shelveset = new Shelveset(
            selectedShelvesetName,
            VersionControlConstants.AUTHENTICATED_USER,
            VersionControlConstants.AUTHENTICATED_USER,
            pendingCheckin.getPendingChanges().getComment(),
            null,
            pendingCheckin.getCheckinNotes().getCheckinNotes(),
            pendingCheckin.getWorkItems().getCheckedWorkItems(),
            Calendar.getInstance(),
            false,
            null);

        final ShelveCommand shelveCommand = new ShelveCommand(
            repository,
            shelveset,
            pendingCheckin.getPendingChanges().getCheckedPendingChanges(),
            true,
            preserveFlag == false);

        final IStatus status = getCommandExecutor().execute(new ResourceChangingCommand(shelveCommand));

        CodeMarkerDispatch.dispatch(SHELVE_TASK_COMPLETE);
        return status;
    }

    public String getSelectedShelvesetName() {
        return selectedShelvesetName;
    }
}
