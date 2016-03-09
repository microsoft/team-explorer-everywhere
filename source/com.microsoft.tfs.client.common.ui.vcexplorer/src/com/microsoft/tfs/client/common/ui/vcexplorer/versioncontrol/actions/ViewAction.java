// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.vcexplorer.versioncontrol.actions;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;

import com.microsoft.tfs.client.common.ui.helpers.ViewFileHelper;
import com.microsoft.tfs.client.common.ui.vc.tfsitem.TFSFile;
import com.microsoft.tfs.client.common.ui.vc.tfsitem.TFSFolder;
import com.microsoft.tfs.client.common.ui.vc.tfsitem.TFSItem;
import com.microsoft.tfs.client.common.ui.vcexplorer.Messages;

/**
 * Views the currently selected item (file or folder, local or server, latest or
 * not) with the appropriate viewer program. The user may be prompted to
 * download the latest version of the file if it is not available on disk.
 *
 */
public class ViewAction extends TeamViewerAction {
    private static final Log log = LogFactory.getLog(ViewAction.class);
    private TFSItem item;

    /**
     * {@inheritDoc}
     */
    @Override
    public void doRun(final IAction action) {
        final IWorkbenchPage workbenchPage = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();

        /*
         * TODO can we detect whether we're in a modal context and use the
         * correct value? Can our callers pass it to this action on
         * construction?
         */

        final boolean inModalContext = false;
        if (item instanceof TFSFile) {
            ViewFileHelper.viewTFSFile(getCurrentRepository(), (TFSFile) item, workbenchPage, inModalContext);
        } else if (item instanceof TFSFolder) {
            ViewFileHelper.viewLocalFileOrFolder(item.getPath(), workbenchPage, inModalContext);
        } else {
            /*
             * Should not happen.
             */
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onSelectionChanged(final IAction action, final ISelection selection) {
        if (action.isEnabled() == false) {
            return;
        }

        item = (TFSItem) adaptSelectionFirstElement(TFSItem.class);

        if (item != null) {
            if (item instanceof TFSFolder) {
                action.setEnabled(((TFSFolder) item).isLocal());
                action.setText(Messages.getString("ViewAction.ViewFolderActionText")); //$NON-NLS-1$
                action.setToolTipText(Messages.getString("ViewAction.OpenFolderToolTipText")); //$NON-NLS-1$
            } else {
                action.setText(Messages.getString("ViewAction.ViewActionText")); //$NON-NLS-1$
                action.setToolTipText(Messages.getString("ViewAction.ViewToolTipText")); //$NON-NLS-1$
            }
        }
    }
}