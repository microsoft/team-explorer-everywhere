// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.teamexplorer.helpers;

import org.eclipse.swt.widgets.Shell;

import com.microsoft.tfs.client.common.ui.dialogs.css.ClassificationAdminDialog;
import com.microsoft.tfs.client.common.ui.teamexplorer.TeamExplorerContext;
import com.microsoft.tfs.core.TFSTeamProjectCollection;
import com.microsoft.tfs.core.clients.workitem.WorkItemClient;

public class AreasAndIterationsHelper {
    public static void showAreasAndIterationsDialog(final Shell shell, final TeamExplorerContext context) {
        final TFSTeamProjectCollection collection = context.getServer().getConnection();

        final ClassificationAdminDialog dialog = new ClassificationAdminDialog(
            shell,
            collection,
            context.getCurrentProjectInfo().getName(),
            context.getCurrentProjectInfo().getURI());

        dialog.open();

        // Any changes in the Area/Iterations dialog result in metadata changes.
        // Ask the server for updated metadata.
        final WorkItemClient client = (WorkItemClient) collection.getClient(WorkItemClient.class);
        client.refreshCache();
    }
}
