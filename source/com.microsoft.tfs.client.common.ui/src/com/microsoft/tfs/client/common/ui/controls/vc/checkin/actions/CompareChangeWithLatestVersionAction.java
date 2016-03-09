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
import com.microsoft.tfs.client.common.ui.compare.TFSItemContentComparator;
import com.microsoft.tfs.client.common.ui.compare.TFSItemNode;
import com.microsoft.tfs.client.common.ui.compare.UserPreferenceExternalCompareHandler;
import com.microsoft.tfs.client.common.ui.framework.compare.Compare;
import com.microsoft.tfs.client.common.ui.framework.compare.CompareUIType;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Change;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.ChangeType;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Item;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.ItemType;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.RecursionType;
import com.microsoft.tfs.core.clients.versioncontrol.specs.ItemSpec;
import com.microsoft.tfs.core.clients.versioncontrol.specs.version.ChangesetVersionSpec;
import com.microsoft.tfs.core.clients.versioncontrol.specs.version.LatestVersionSpec;
import com.microsoft.tfs.util.Check;

public class CompareChangeWithLatestVersionAction extends RepositoryAction {
    private final CompareUIType compareUIType;
    private final Shell shell;

    public CompareChangeWithLatestVersionAction(
        final ISelectionProvider selectionProvider,
        final CompareUIType compareUIType,
        final Shell shell) {
        this(selectionProvider, null, compareUIType, shell);
    }

    public CompareChangeWithLatestVersionAction(
        final ISelectionProvider selectionProvider,
        final TFSRepository repository,
        final CompareUIType compareUIType,
        final Shell shell) {
        super(selectionProvider, repository);

        Check.notNull(compareUIType, "compareUIType"); //$NON-NLS-1$
        Check.notNull(shell, "shell"); //$NON-NLS-1$

        this.compareUIType = compareUIType;
        this.shell = shell;

        setText(Messages.getString("CompareChangeWithLatestVersionAction.ActionText")); //$NON-NLS-1$
        setToolTipText(Messages.getString("CompareChangeWithLatestVersionAction.ActionTooltip")); //$NON-NLS-1$
        setImageDescriptor(TFSCommonUIImages.getImageDescriptor(TFSCommonUIImages.IMG_COMPARE));
    }

    @Override
    protected void doRun(final TFSRepository repository) {
        final Change change = (Change) adaptSelectionFirstElement(Change.class);

        final Item item = change.getItem();

        final Compare compare = new Compare();

        compare.setModified(new TFSItemNode(item, repository.getVersionControlClient()));

        compare.setOriginal(
            new ServerItemByItemVersionGenerator(
                repository,
                new ItemSpec(item.getServerItem(), RecursionType.NONE, item.getDeletionID()),
                new ChangesetVersionSpec(item.getChangeSetID()),
                LatestVersionSpec.INSTANCE));

        compare.addComparator(TFSItemContentComparator.INSTANCE);

        compare.setUIType(compareUIType);

        compare.setExternalCompareHandler(new UserPreferenceExternalCompareHandler(shell));
        compare.open();
    }

    @Override
    protected boolean computeEnablement(final IStructuredSelection selection) {
        if (selection.size() != 1) {
            return false;
        }

        final Change change = (Change) adaptSelectionFirstElement(Change.class);

        if (change.getItem().getItemType() != ItemType.FILE || containsSymlinkChange(change)) {
            return false;
        }

        return !change.getChangeType().contains(ChangeType.DELETE);
    }
}
