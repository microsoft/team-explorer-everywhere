// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.controls.vc.checkin.actions;

import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;

import com.microsoft.tfs.client.common.repository.TFSRepository;
import com.microsoft.tfs.client.common.ui.RepositoryAction;
import com.microsoft.tfs.client.common.ui.TFSCommonUIImages;
import com.microsoft.tfs.client.common.ui.helpers.ViewFileHelper;
import com.microsoft.tfs.core.clients.versioncontrol.path.ServerPath;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Change;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Item;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.ItemType;
import com.microsoft.tfs.util.Check;

public class ViewChangeAction extends RepositoryAction {
    private final boolean inModalContext;
    private final ViewVersionType viewVersionType;

    public ViewChangeAction(
        final ISelectionProvider selectionProvider,
        final boolean inModalContext,
        final ViewVersionType viewVersionType) {
        this(selectionProvider, null, inModalContext, viewVersionType);
    }

    public ViewChangeAction(
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
        final Change change = (Change) adaptSelectionFirstElement(Change.class);
        final String[] localItem = new String[1];

        BusyIndicator.showWhile(Display.getDefault(), new Runnable() {
            @Override
            public void run() {
                localItem[0] = getLocalItemToView(change, repository);
            }
        });

        final IWorkbenchPage workbenchPage = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();

        ViewFileHelper.viewLocalFileOrFolder(localItem[0], workbenchPage, inModalContext);
    }

    private String getLocalItemToView(final Change change, final TFSRepository repository) {
        Item item = change.getItem();

        if (ViewVersionType.PREVIOUS == viewVersionType || ViewVersionType.LATEST == viewVersionType) {
            int version;
            if (ViewVersionType.PREVIOUS == viewVersionType) {
                version = item.getChangeSetID() - 1;
            } else {
                // Latest
                version = Integer.MAX_VALUE;
            }
            item = repository.getVersionControlClient().getItem(item.getItemID(), version, true);
        }

        final String localFileName = ServerPath.getFileName(item.getServerItem());

        return item.downloadFileToTempLocation(repository.getVersionControlClient(), localFileName).getAbsolutePath();
    }

    @Override
    protected boolean computeEnablement(final IStructuredSelection selection) {
        if (selection.size() != 1) {
            return false;
        }

        final Change change = (Change) adaptSelectionFirstElement(Change.class);

        if (change.getItem().getItemType() != ItemType.FILE) {
            return false;
        }

        return true;
    }
}
