// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.controls.vc.checkin.actions;

import java.io.File;
import java.text.MessageFormat;

import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Shell;

import com.microsoft.tfs.client.common.repository.TFSRepository;
import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.RepositoryAction;
import com.microsoft.tfs.client.common.ui.TFSCommonUIImages;
import com.microsoft.tfs.client.common.ui.compare.TFSItemContentComparator;
import com.microsoft.tfs.client.common.ui.compare.TFSItemNode;
import com.microsoft.tfs.client.common.ui.compare.UserPreferenceExternalCompareHandler;
import com.microsoft.tfs.client.common.ui.framework.compare.Compare;
import com.microsoft.tfs.client.common.ui.framework.compare.CompareUIType;
import com.microsoft.tfs.core.clients.versioncontrol.exceptions.ServerPathFormatException;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Change;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.ChangeType;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Changeset;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Item;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.ItemType;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.RecursionType;
import com.microsoft.tfs.core.clients.versioncontrol.specs.version.ChangesetVersionSpec;
import com.microsoft.tfs.core.clients.versioncontrol.specs.version.WorkspaceVersionSpec;
import com.microsoft.tfs.util.Check;

public class CompareChangeWithWorkspaceVersionAction extends RepositoryAction {
    private final CompareUIType compareUIType;
    private final Shell shell;

    public CompareChangeWithWorkspaceVersionAction(
        final ISelectionProvider selectionProvider,
        final CompareUIType compareUIType,
        final Shell shell) {
        this(selectionProvider, null, compareUIType, shell);
    }

    public CompareChangeWithWorkspaceVersionAction(
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
            setText(Messages.getString("CompareChangeWithWorkspaceVersionAction.ActionText")); //$NON-NLS-1$
            setToolTipText(Messages.getString("CompareChangeWithWorkspaceVersionAction.ActionTooltip")); //$NON-NLS-1$
        }
    }

    @Override
    public void setRepository(final TFSRepository repository) {
        super.setRepository(repository);

        final String messageFormat = Messages.getString("CompareChangeWithWorkspaceVersionAction.ActionTextFormat"); //$NON-NLS-1$
        final String text = MessageFormat.format(messageFormat, repository.getWorkspace().getName());

        setText(text);
        setToolTipText(text);
    }

    @Override
    protected void doRun(final TFSRepository repository) {
        final Change change = (Change) adaptSelectionFirstElement(Change.class);
        final WorkspaceVersionSpec workspaceVersionSpec = new WorkspaceVersionSpec(repository.getWorkspace());

        Changeset[] changesets;

        changesets = repository.getVersionControlClient().queryHistory(
            change.getItem().getServerItem(),
            new ChangesetVersionSpec(change.getItem().getChangeSetID()),
            change.getItem().getDeletionID(),
            RecursionType.NONE,
            null,
            null,
            workspaceVersionSpec,
            1,
            true,
            false,
            false,
            false);

        Change change2 = null;

        if (changesets != null && changesets.length > 0) {
            if (changesets[0].getChanges() != null && changesets[0].getChanges().length > 0) {
                change2 = changesets[0].getChanges()[0];
            }
        }

        if (change2 == null) {
            throw new RuntimeException("TODO: item not found"); //$NON-NLS-1$
        }

        final Item item1 = change.getItem();

        final Item item2 = change2.getItem();

        final Compare compare = new Compare();

        compare.setModified(new TFSItemNode(item1, repository.getVersionControlClient()));

        compare.setOriginal(new TFSItemNode(item2, repository.getVersionControlClient()));

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

        String mappedLocalPath;
        try {
            mappedLocalPath = getRepository().getWorkspace().getMappedLocalPath(change.getItem().getServerItem());
        } catch (final ServerPathFormatException e) {
            return false;
        }
        if (mappedLocalPath == null || !new File(mappedLocalPath).exists()) {
            return false;
        }

        if (change.getChangeType().contains(ChangeType.DELETE)) {
            return false;
        }

        return true;
    }
}
