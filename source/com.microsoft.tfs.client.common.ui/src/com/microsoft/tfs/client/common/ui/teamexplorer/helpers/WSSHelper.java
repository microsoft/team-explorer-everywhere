// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.teamexplorer.helpers;

import java.net.URI;
import java.net.URISyntaxException;

import org.eclipse.swt.widgets.Shell;

import com.microsoft.tfs.client.common.server.TFSServer;
import com.microsoft.tfs.client.common.ui.framework.launcher.Launcher;
import com.microsoft.tfs.core.TFSTeamProjectCollection;
import com.microsoft.tfs.core.clients.commonstructure.ProjectInfo;
import com.microsoft.tfs.core.clients.sharepoint.WSSDocumentLibrary;
import com.microsoft.tfs.core.clients.sharepoint.WSSNode;
import com.microsoft.tfs.core.clients.sharepoint.WSSUtils;
import com.microsoft.tfs.core.util.Hierarchical;

public class WSSHelper {
    public static void openWSSDocumentLibrary(
        final Shell shell,
        final TFSServer server,
        final ProjectInfo projectInfo,
        final WSSDocumentLibrary library) {
        openWSSNode(shell, server, projectInfo, library);
    }

    public static void openWSSNode(
        final Shell shell,
        final TFSServer server,
        final ProjectInfo projectInfo,
        final WSSNode wssNode) {
        openWSSNode(shell, server, projectInfo, (Hierarchical) wssNode);
    }

    private static void openWSSNode(
        final Shell shell,
        final TFSServer server,
        final ProjectInfo projectInfo,
        final Hierarchical hierarchical) {
        final TFSTeamProjectCollection collection = server.getConnection();

        try {
            final URI rootURI = new URI(WSSUtils.getWSSURL(collection, projectInfo));
            final URI uri = rootURI.resolve(WSSUtils.getViewURI(hierarchical));

            Launcher.launch(uri.toString());
        } catch (final URISyntaxException e) {
            return;
        }
    }
}
