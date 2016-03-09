// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.tasks.vc;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.widgets.Shell;

import com.microsoft.tfs.client.common.repository.TFSRepository;
import com.microsoft.tfs.client.common.ui.dialogs.vc.GetDialog;
import com.microsoft.tfs.client.common.ui.vc.serveritem.TypedServerItem;
import com.microsoft.tfs.core.clients.versioncontrol.GetOptions;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.GetRequest;

/**
 * Raises the dialog to choose a specific version of items to get.
 */
public class GetSpecificTask extends AbstractGetTask {
    private final TypedServerItem[] serverItems;

    private GetRequest[] getRequests;
    private GetOptions getOptions;

    /**
     * @param shell
     *        the shell to use (must not be <code>null</code>)
     * @param repository
     *        the repository to use (must not be <code>null</code>)
     * @param serverItems
     *        the server items to show in the dialog; pass <code>null</code> or
     *        empty to get the whole workspace
     */
    public GetSpecificTask(final Shell shell, final TFSRepository repository, final TypedServerItem[] serverItems) {
        super(shell, repository);

        this.serverItems = serverItems;
    }

    @Override
    protected boolean init() {
        final GetDialog getDialog = new GetDialog(getShell(), getRepository(), serverItems);

        if (getDialog.open() != IDialogConstants.OK_ID) {
            return false;
        }

        getRequests = getDialog.getGetRequests();
        getOptions = getDialog.getGetOptions();

        return true;
    }

    @Override
    protected GetRequest[] getGetRequests() {
        return getRequests;
    }

    @Override
    protected GetOptions getGetOptions() {
        return getOptions;
    }
}
