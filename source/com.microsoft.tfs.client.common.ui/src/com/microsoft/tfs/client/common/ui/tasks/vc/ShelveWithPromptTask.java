// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.tasks.vc;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.widgets.Shell;

import com.microsoft.tfs.client.common.repository.TFSRepository;
import com.microsoft.tfs.client.common.ui.dialogs.vc.ShelveDialog;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.CheckinNote;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.PendingChange;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.WorkItemCheckinInfo;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Workspace;
import com.microsoft.tfs.core.clients.versioncontrol.workspacecache.SavedCheckin;
import com.microsoft.tfs.core.pendingcheckin.PendingCheckin;
import com.microsoft.tfs.util.Check;

public class ShelveWithPromptTask extends AbstractShelveTask {
    private static final Log log = LogFactory.getLog(ShelveWithPromptTask.class);

    private final PendingChange[] checkedPendingChanges;
    private final String comment;
    private final WorkItemCheckinInfo[] checkedWorkItems;
    private final CheckinNote checkinNotes;

    private boolean prompted = false;
    private PendingCheckin pendingCheckin = null;
    private String shelvesetName = null;
    private boolean preserveChangesFlag = true;

    /**
     * Creates a {@link AbstractShelveTask}.
     * <p>
     * The pending changes, comment, work items, and notes presented in the
     * dialog are automatically retrieved from the {@link Workspace}'s last
     * saved checkin.
     *
     * @param shell
     *        the shell (must not be <code>null</code>)
     * @param repository
     *        the repository (must not be <code>null</code>)
     */
    public ShelveWithPromptTask(final Shell shell, final TFSRepository repository) {
        this(shell, repository, null);
    }

    /**
     * Creates a {@link AbstractShelveTask}, specifying which pending changes
     * should be checked in the dialog.
     * <p>
     * The comment, work items, and notes presented in the dialog are
     * automatically retrieved from the {@link Workspace}'s last saved checkin.
     *
     * @param shell
     *        the shell (must not be <code>null</code>)
     * @param repository
     *        the repository (must not be <code>null</code>)
     * @param checkedPendingChanges
     *        the pending changes which should be checked by default in the
     *        dialog (if <code>null</code>, all changes are checked)
     */
    public ShelveWithPromptTask(
        final Shell shell,
        final TFSRepository repository,
        final PendingChange[] checkedPendingChanges) {
        super(shell, repository);
        Check.notNull(repository, "repository"); //$NON-NLS-1$

        this.checkedPendingChanges = checkedPendingChanges;

        final SavedCheckin savedCheckin = repository.getWorkspace().getLastSavedCheckin();
        if (savedCheckin != null) {
            comment = savedCheckin.getComment();
            checkedWorkItems = savedCheckin.getWorkItemsCheckinInfo(
                repository.getVersionControlClient().getConnection().getWorkItemClient());
            checkinNotes = savedCheckin.getCheckinNotes();
        } else {
            comment = null;
            checkedWorkItems = null;
            checkinNotes = null;
        }
    }

    /**
     * Creates a {@link AbstractShelveTask}, specifying all the information
     * presented in the dialog.
     *
     * @param shell
     *        the shell (must not be <code>null</code>)
     * @param repository
     *        the repository (must not be <code>null</code>)
     * @param checkedPendingChanges
     *        the pending changes which should be checked by default in the
     *        dialog (if <code>null</code>, all changes are checked)
     * @param comment
     *        the comment text the user has already entered in another control
     *        that should be used as the default in the dialog (may be
     *        <code>null</code>)
     * @param checkedWorkItems
     *        the work items that should be initially checked (may be
     *        <code>null</code>)
     */
    public ShelveWithPromptTask(
        final Shell shell,
        final TFSRepository repository,
        final PendingChange[] checkedPendingChanges,
        final String comment,
        final WorkItemCheckinInfo[] checkedWorkItems,
        final CheckinNote checkinNotes) {
        super(shell, repository);
        Check.notNull(repository, "repository"); //$NON-NLS-1$

        this.checkedPendingChanges = checkedPendingChanges;
        this.comment = comment;
        this.checkedWorkItems = checkedWorkItems;
        this.checkinNotes = checkinNotes;
    }

    @Override
    protected PendingCheckin getPendingCheckin() {
        ensurePrompted();
        return pendingCheckin;
    }

    @Override
    protected String getShelvesetName() {
        ensurePrompted();
        return shelvesetName;
    }

    @Override
    protected boolean getPreserveChangesFlag() {
        ensurePrompted();
        return preserveChangesFlag;
    }

    @Override
    protected boolean userCanceledShelve() {
        ensurePrompted();
        return pendingCheckin == null;
    }

    private void ensurePrompted() {
        if (!prompted) {
            promptForOptions();
        }

        prompted = true;
    }

    private void promptForOptions() {
        final ShelveDialog shelveDialog =
            new ShelveDialog(getShell(), repository, checkedPendingChanges, comment, checkedWorkItems, checkinNotes);

        if (shelveDialog.open() == IDialogConstants.OK_ID) {
            pendingCheckin = shelveDialog.getPendingCheckin();
            shelvesetName = shelveDialog.getShelvesetName();
            preserveChangesFlag = shelveDialog.isPreservePendingChanges();
        }
    }
}
