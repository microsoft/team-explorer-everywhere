// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.tasks.vc;

import org.eclipse.swt.widgets.Shell;

import com.microsoft.tfs.client.common.repository.TFSRepository;
import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.TFSCommonUIClientPlugin;
import com.microsoft.tfs.client.common.ui.helpers.ToggleMessageHelper;
import com.microsoft.tfs.client.common.ui.prefs.UIPreferenceConstants;
import com.microsoft.tfs.client.common.ui.vc.serveritem.ServerItemType;
import com.microsoft.tfs.client.common.ui.vc.serveritem.TypedServerItem;
import com.microsoft.tfs.core.clients.versioncontrol.GetOptions;
import com.microsoft.tfs.core.clients.versioncontrol.GetStatus;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.GetRequest;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.RecursionType;
import com.microsoft.tfs.core.clients.versioncontrol.specs.ItemSpec;
import com.microsoft.tfs.core.clients.versioncontrol.specs.version.LatestVersionSpec;

public class GetLatestTask extends AbstractGetTask {
    private final TypedServerItem[] serverItems;
    private final String localItem;
    private boolean suppressFilesUpToDataMessage = false;

    private GetRequest[] getRequests;

    /**
     * Creates a task to "get latest" for the entire workspace.
     *
     *
     * @param shell
     *        The associated shell.
     * @param repository
     *        The TFS repository.
     */
    public GetLatestTask(final Shell shell, final TFSRepository repository) {
        super(shell, repository);
        this.serverItems = null;
        this.localItem = null;
    }

    /**
     * Creates a task to "get latest" for the specified server items.
     *
     *
     * @param shell
     *        The associated shell.
     * @param repository
     *        The TFS repository.
     * @param serverItems
     *        The server items to update.
     */
    public GetLatestTask(final Shell shell, final TFSRepository repository, final TypedServerItem[] serverItems) {
        super(shell, repository);
        this.serverItems = serverItems;
        this.localItem = null;
    }

    /**
     * Create a task to "get latest (recursive)" for the specified local path.
     *
     *
     * @param shell
     *        The associated shell.
     * @param repository
     *        The TFS repository.
     * @param localItem
     *        The local item path to update.
     */
    public GetLatestTask(final Shell shell, final TFSRepository repository, final String localItem) {
        super(shell, repository);
        this.serverItems = null;
        this.localItem = localItem;
    }

    public void setSuppressFileUpToDateMessage(final boolean suppress) {
        suppressFilesUpToDataMessage = suppress;
    }

    @Override
    protected boolean init() {
        if (serverItems == null && localItem == null) {
            getRequests = new GetRequest[] {
                new GetRequest(null, LatestVersionSpec.INSTANCE)
            };
        } else if (serverItems == null) {
            getRequests = new GetRequest[] {
                new GetRequest(new ItemSpec(localItem, RecursionType.FULL), LatestVersionSpec.INSTANCE)
            };
        } else {
            getRequests = new GetRequest[serverItems.length];

            for (int i = 0; i < serverItems.length; i++) {
                final RecursionType recursionType =
                    (ServerItemType.isFolder(serverItems[i].getType())) ? RecursionType.FULL : RecursionType.NONE;

                getRequests[i] = new GetRequest(
                    new ItemSpec(serverItems[i].getServerPath(), recursionType),
                    LatestVersionSpec.INSTANCE);
            }
        }

        return true;
    }

    @Override
    protected GetRequest[] getGetRequests() {
        return getRequests;
    }

    @Override
    protected GetOptions getGetOptions() {
        return GetOptions.NONE;
    }

    @Override
    protected void finish(final GetStatus status) {
        if (!suppressFilesUpToDataMessage && status.isNoActionNeeded() && !status.isCanceled()) {
            TFSCommonUIClientPlugin.getDefault().getConsole().printMessage(
                Messages.getString("GetLatestTask.AllFilesUpToDate")); //$NON-NLS-1$

            ToggleMessageHelper.openInformation(
                getShell(),
                Messages.getString("GetLatestTask.UpToDateDialogTitle"), //$NON-NLS-1$
                Messages.getString("GetLatestTask.UpToDateDialogText"), //$NON-NLS-1$
                Messages.getString("GetLatestTask.NeverShowButtonText"), //$NON-NLS-1$
                false,
                UIPreferenceConstants.HIDE_ALL_FILES_UP_TO_DATE_MESSAGE);
        }
    }
}
