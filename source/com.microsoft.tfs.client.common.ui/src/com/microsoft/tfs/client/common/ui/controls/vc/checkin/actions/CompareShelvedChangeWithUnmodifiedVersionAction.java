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
import com.microsoft.tfs.client.common.ui.compare.ServerItemByItemVersionGenerator;
import com.microsoft.tfs.client.common.ui.compare.TFSShelvedChangeNode;
import com.microsoft.tfs.client.common.ui.compare.UserPreferenceExternalCompareHandler;
import com.microsoft.tfs.client.common.ui.framework.compare.Compare;
import com.microsoft.tfs.client.common.ui.framework.compare.CompareUIType;
import com.microsoft.tfs.client.common.ui.teamexplorer.helpers.PendingChangesHelpers;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.ChangeType;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.ItemType;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.PendingChange;
import com.microsoft.tfs.core.clients.versioncontrol.specs.version.ChangesetVersionSpec;
import com.microsoft.tfs.util.Check;

public class CompareShelvedChangeWithUnmodifiedVersionAction extends RepositoryAction {
    private final CompareUIType compareUIType;
    private String shelvesetName;
    private String shelvesetOwner;
    private final Shell shell;

    public CompareShelvedChangeWithUnmodifiedVersionAction(
        final ISelectionProvider selectionProvider,
        final CompareUIType compareUIType,
        final String shelvesetName,
        final String shelvesetOwner,
        final Shell shell) {
        this(selectionProvider, null, compareUIType, shelvesetName, shelvesetOwner, shell);
    }

    public CompareShelvedChangeWithUnmodifiedVersionAction(
        final ISelectionProvider selectionProvider,
        final TFSRepository repository,
        final CompareUIType compareUIType,
        final String shelvesetName,
        final String shelvesetOwner,
        final Shell shell) {
        super(selectionProvider, repository);

        Check.notNull(compareUIType, "compareUIType"); //$NON-NLS-1$
        Check.notNull(shell, "shell"); //$NON-NLS-1$

        this.compareUIType = compareUIType;
        this.shell = shell;

        setShelvesetInfo(shelvesetName, shelvesetOwner);

        setText(Messages.getString("CompareShelvedChangeWithUnmodifiedVersionAction.ActionText")); //$NON-NLS-1$
        setToolTipText(Messages.getString("CompareShelvedChangeWithUnmodifiedVersionAction.ActionTooltip")); //$NON-NLS-1$
        setImageDescriptor(TFSCommonUIImages.getImageDescriptor(TFSCommonUIImages.IMG_COMPARE));
    }

    public void setShelvesetInfo(final String shelvesetName, final String shelvesetOwner) {
        this.shelvesetName = shelvesetName;
        this.shelvesetOwner = shelvesetOwner;
    }

    @Override
    protected void doRun(final TFSRepository repository) {
        final PendingChange pendingChange = (PendingChange) adaptSelectionFirstElement(PendingChange.class);

        final Compare compare = new Compare();

        compare.setModified(new TFSShelvedChangeNode(pendingChange, shelvesetName, shelvesetOwner, repository));

        String originalItem = pendingChange.getServerItem();
        if (pendingChange.getChangeType().contains(ChangeType.RENAME) && pendingChange.getSourceServerItem() != null) {
            originalItem = pendingChange.getSourceServerItem();
        }

        compare.setOriginal(
            new ServerItemByItemVersionGenerator(
                repository,
                originalItem,
                new ChangesetVersionSpec(pendingChange.getVersion()),
                new ChangesetVersionSpec(pendingChange.getVersion())));

        compare.setUIType(compareUIType);

        compare.setExternalCompareHandler(new UserPreferenceExternalCompareHandler(shell));
        compare.open();
    }

    @Override
    protected boolean computeEnablement(final IStructuredSelection selection) {
        if (selection.size() != 1) {
            return false;
        }

        final PendingChange pendingChange = (PendingChange) adaptSelectionFirstElement(PendingChange.class);

        if (pendingChange.getItemType() != ItemType.FILE
            || PendingChangesHelpers.containsSymlinkChange(pendingChange)) {
            return false;
        }

        if (pendingChange.getChangeType().contains(ChangeType.ADD)
            || pendingChange.getChangeType().contains(ChangeType.BRANCH)
            || pendingChange.getChangeType().contains(ChangeType.DELETE)) {
            return false;
        }

        if (pendingChange.getChangeType().contains(ChangeType.RENAME)
            && !pendingChange.getChangeType().contains(ChangeType.EDIT)) {
            return false;
        }

        return true;
    }
}
