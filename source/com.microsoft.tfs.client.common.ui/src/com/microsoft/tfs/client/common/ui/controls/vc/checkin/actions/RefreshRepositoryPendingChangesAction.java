// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.controls.vc.checkin.actions;

import java.text.MessageFormat;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.Action;

import com.microsoft.tfs.client.common.codemarker.CodeMarker;
import com.microsoft.tfs.client.common.codemarker.CodeMarkerDispatch;
import com.microsoft.tfs.client.common.repository.TFSRepository;
import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.TFSCommonUIImages;

public class RefreshRepositoryPendingChangesAction extends Action {
    public static final CodeMarker CODEMARKER_PENDING_CHANGES_VIEW_REFRESHED = new CodeMarker(
        "com.microsoft.tfs.client.common.ui.controls.vc.checkin.actions.RefreshRepositoryPendingChangesAction#ViewRefreshed"); //$NON-NLS-1$

    private TFSRepository repository;

    public RefreshRepositoryPendingChangesAction() {
        this(null);
    }

    public RefreshRepositoryPendingChangesAction(final TFSRepository repository) {
        setRepository(repository);

        setText(Messages.getString("RefreshRepositoryPendingChangesAction.ActionText")); //$NON-NLS-1$
        setToolTipText(Messages.getString("RefreshRepositoryPendingChangesAction.ActionTooltip")); //$NON-NLS-1$
        setImageDescriptor(TFSCommonUIImages.getImageDescriptor(TFSCommonUIImages.IMG_REFRESH));
    }

    public void setRepository(final TFSRepository repository) {
        this.repository = repository;
        setEnabled(repository != null);
    }

    @Override
    public void run() {
        final TFSRepository repository = this.repository;

        final String messageFormat =
            Messages.getString("RefreshRepositoryPendingChangesAction.RefreshingPendingChangesJobFormat"); //$NON-NLS-1$
        final String message = MessageFormat.format(messageFormat, this.repository.getWorkspace().getName());

        final Job refreshJob = new Job(message) {
            @Override
            protected IStatus run(final IProgressMonitor monitor) {
                repository.getPendingChangeCache().refresh();

                CodeMarkerDispatch.dispatch(CODEMARKER_PENDING_CHANGES_VIEW_REFRESHED);
                return Status.OK_STATUS;
            }
        };

        refreshJob.schedule();
    }
}
