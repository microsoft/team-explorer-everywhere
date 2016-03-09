// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.controls.vc.checkin.actions;

import java.text.MessageFormat;

import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;

import com.microsoft.tfs.client.common.repository.TFSRepository;
import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.RepositoryAction;
import com.microsoft.tfs.client.common.ui.TFSCommonUIImages;
import com.microsoft.tfs.client.common.ui.helpers.ViewFileHelper;
import com.microsoft.tfs.core.clients.versioncontrol.GetItemsOptions;
import com.microsoft.tfs.core.clients.versioncontrol.path.ServerPath;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.ChangeType;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.DeletedState;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Item;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.ItemType;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.PendingChange;
import com.microsoft.tfs.core.clients.versioncontrol.specs.version.LatestVersionSpec;
import com.microsoft.tfs.util.Check;

public class ViewPendingChangeAction extends RepositoryAction {
    private final boolean inModalContext;
    private final ViewVersionType viewVersionType;

    public ViewPendingChangeAction(
        final ISelectionProvider selectionProvider,
        final boolean inModalContext,
        final ViewVersionType viewVersionType) {
        this(selectionProvider, null, inModalContext, viewVersionType);
    }

    public ViewPendingChangeAction(
        final ISelectionProvider selectionProvider,
        final TFSRepository repository,
        final boolean inModalContext,
        final ViewVersionType viewVersionType) {
        super(selectionProvider, repository);

        Check.notNull(viewVersionType, "viewVersionType"); //$NON-NLS-1$

        this.inModalContext = inModalContext;
        this.viewVersionType = viewVersionType;

        setText(viewVersionType.getText());
        setToolTipText(viewVersionType.getTooltipText());
        setImageDescriptor(TFSCommonUIImages.getImageDescriptor(TFSCommonUIImages.IMG_VIEW));
    }

    @Override
    protected void doRun(final TFSRepository repository) {
        final PendingChange pendingChange = (PendingChange) adaptSelectionFirstElement(PendingChange.class);

        String localItemToView = pendingChange.getLocalItem();
        ViewVersionType thisVersionType = viewVersionType;

        if (viewVersionType == ViewVersionType.DEFAULT
            && (localItemToView == null
                || localItemToView.length() == 0
                || pendingChange.getChangeType().contains(ChangeType.DELETE))) {
            thisVersionType = ViewVersionType.UNMODIFIED;
        }

        if (thisVersionType == ViewVersionType.UNMODIFIED || thisVersionType == ViewVersionType.SHELVED) {
            final String fileName = ServerPath.getFileName(pendingChange.getServerItem());

            if (thisVersionType == ViewVersionType.SHELVED) {
                localItemToView = pendingChange.downloadShelvedFileToTempLocation(
                    repository.getVersionControlClient(),
                    fileName).getAbsolutePath();
            } else {
                localItemToView = pendingChange.downloadBaseFileToTempLocation(
                    repository.getVersionControlClient(),
                    fileName).getAbsolutePath();
            }
        } else if (thisVersionType == ViewVersionType.LATEST) {
            final Item latestItem = repository.getVersionControlClient().getItem(
                pendingChange.getServerItem(),
                LatestVersionSpec.INSTANCE,
                DeletedState.ANY,
                GetItemsOptions.DOWNLOAD.combine(GetItemsOptions.INCLUDE_SOURCE_RENAMES));

            final String fileName = ServerPath.getFileName(latestItem.getServerItem());

            localItemToView =
                latestItem.downloadFileToTempLocation(repository.getVersionControlClient(), fileName).getAbsolutePath();
        }

        final IWorkbenchPage workbenchPage = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
        ViewFileHelper.viewLocalFileOrFolder(localItemToView, workbenchPage, inModalContext);
    }

    @Override
    protected boolean computeEnablement(final IStructuredSelection selection) {
        final boolean enabled = computeEnablementInternal(selection);

        if (!enabled && ViewVersionType.UNMODIFIED == viewVersionType) {
            setText(viewVersionType.getText());
            setToolTipText(viewVersionType.getTooltipText());
        }

        return enabled;
    }

    private boolean computeEnablementInternal(final IStructuredSelection selection) {
        if (selection.size() != 1) {
            return false;
        }

        final PendingChange pendingChange = (PendingChange) adaptSelectionFirstElement(PendingChange.class);

        if (pendingChange.getItemType() != ItemType.FILE) {
            return false;
        }

        if (pendingChange.getChangeType().contains(ChangeType.DELETE) && ViewVersionType.DEFAULT == viewVersionType) {
            return false;
        }

        if (viewVersionType == ViewVersionType.UNMODIFIED) {
            final String messageFormat = Messages.getString("ViewPendingChangeAction.VersionFormat"); //$NON-NLS-1$

            String message = MessageFormat.format(
                messageFormat,
                viewVersionType.getText(),
                Integer.toString(pendingChange.getVersion()));
            setText(message);

            message = MessageFormat.format(
                messageFormat,
                viewVersionType.getTooltipText(),
                Integer.toString(pendingChange.getVersion()));
            setToolTipText(message);
        }

        return true;
    }
}
