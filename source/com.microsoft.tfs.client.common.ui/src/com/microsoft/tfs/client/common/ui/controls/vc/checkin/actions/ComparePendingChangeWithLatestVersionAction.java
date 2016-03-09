// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.controls.vc.checkin.actions;

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

public class ComparePendingChangeWithLatestVersionAction extends RepositoryAction {
    private final CompareUIType compareUIType;
    private final Shell shell;

    public ComparePendingChangeWithLatestVersionAction(
        final ISelectionProvider selectionProvider,
        final CompareUIType compareUIType,
        final Shell shell) {
        this(selectionProvider, null, compareUIType, shell);
    }

    public ComparePendingChangeWithLatestVersionAction(
        final ISelectionProvider selectionProvider,
        final TFSRepository repository,
        final CompareUIType compareUIType,
        final Shell shell) {
        super(selectionProvider, repository);

        Check.notNull(compareUIType, "compareUIType"); //$NON-NLS-1$
        Check.notNull(shell, "shell"); //$NON-NLS-1$

        this.compareUIType = compareUIType;
        this.shell = shell;

        final String text = Messages.getString("ComparePendingChangeWithLatestVersionAction.ActionText"); //$NON-NLS-1$
        setText(text);
        setToolTipText(text);
        setImageDescriptor(TFSCommonUIImages.getImageDescriptor(TFSCommonUIImages.IMG_COMPARE));
    }

    @Override
    protected void doRun(final TFSRepository repository) {
        final PendingChange pendingChange = (PendingChange) adaptSelectionFirstElement(PendingChange.class);
        PendingChangesHelpers.compareWithLatestVersion(shell, repository, pendingChange, compareUIType);
    }

    @Override
    protected boolean computeEnablement(final IStructuredSelection selection) {
        if (selection.size() != 1) {
            return false;
        }

        final PendingChange pendingChange = (PendingChange) adaptSelectionFirstElement(PendingChange.class);
        return PendingChangesHelpers.canCompareWithLatestVersion(pendingChange);
    }
}
