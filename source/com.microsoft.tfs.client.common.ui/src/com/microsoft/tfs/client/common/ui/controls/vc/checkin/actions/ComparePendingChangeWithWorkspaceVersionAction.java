// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.controls.vc.checkin.actions;

import java.text.MessageFormat;

import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Shell;

import com.microsoft.tfs.client.common.repository.TFSRepository;
import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.RepositoryAction;
import com.microsoft.tfs.client.common.ui.TFSCommonUIImages;
import com.microsoft.tfs.client.common.ui.framework.compare.CompareUIType;
import com.microsoft.tfs.client.common.ui.teamexplorer.helpers.PendingChangesHelpers;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.PendingChange;
import com.microsoft.tfs.util.Check;

public class ComparePendingChangeWithWorkspaceVersionAction extends RepositoryAction {
    private final CompareUIType compareUIType;
    private final Shell shell;

    public ComparePendingChangeWithWorkspaceVersionAction(
        final ISelectionProvider selectionProvider,
        final CompareUIType compareUIType,
        final Shell shell) {
        this(selectionProvider, null, compareUIType, shell);
    }

    public ComparePendingChangeWithWorkspaceVersionAction(
        final ISelectionProvider selectionProvider,
        final TFSRepository repository,
        final CompareUIType compareUIType,
        final Shell shell) {
        super(selectionProvider, repository);

        Check.notNull(compareUIType, "compareUIType"); //$NON-NLS-1$
        Check.notNull(shell, "shell"); //$NON-NLS-1$

        this.compareUIType = compareUIType;
        this.shell = shell;

        setImageDescriptor(TFSCommonUIImages.getImageDescriptor(TFSCommonUIImages.IMG_COMPARE));

        if (repository != null) {
            setRepository(repository);
        } else {
            setText(Messages.getString("ComparePendingChangeWithWorkspaceVersionAction.ActionText")); //$NON-NLS-1$
            setToolTipText(Messages.getString("ComparePendingChangeWithWorkspaceVersionAction.ActionTooltip")); //$NON-NLS-1$
        }
    }

    @Override
    public void setRepository(final TFSRepository repository) {
        super.setRepository(repository);

        if (repository != null) {
            final String messageFormat =
                Messages.getString("ComparePendingChangeWithWorkspaceVersionAction.ActionTextFormat"); //$NON-NLS-1$
            final String text = MessageFormat.format(messageFormat, repository.getWorkspace().getName());

            setText(text);
            setToolTipText(text);
        }
    }

    @Override
    protected void doRun(final TFSRepository repository) {
        final PendingChange pendingChange = (PendingChange) adaptSelectionFirstElement(PendingChange.class);
        PendingChangesHelpers.compareWithWorkspaceVersion(shell, repository, pendingChange, compareUIType);
    }

    @Override
    protected boolean computeEnablement(final IStructuredSelection selection) {
        if (selection.size() != 1) {
            return false;
        }

        final PendingChange pendingChange = (PendingChange) adaptSelectionFirstElement(PendingChange.class);
        return PendingChangesHelpers.canCompareWithWorkspaceVersion(getRepository(), pendingChange);
    }
}
