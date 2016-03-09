// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.sdk.samples.teamexplorer;

import java.net.URI;
import java.net.URISyntaxException;

import org.eclipse.swt.widgets.Shell;

import com.microsoft.tfs.client.common.ui.browser.BrowserFacade;
import com.microsoft.tfs.client.common.ui.browser.BrowserFacade.LaunchMode;
import com.microsoft.tfs.client.common.ui.teamexplorer.TeamExplorerContext;
import com.microsoft.tfs.client.common.ui.teamexplorer.TeamExplorerNavigator;
import com.microsoft.tfs.client.common.ui.teamexplorer.internal.TeamExplorerNavigationItemConfig;
import com.microsoft.tfs.client.common.ui.teamexplorer.link.TeamExplorerBaseNavigationLink;

public class TeamExplorerSampleNavigationLink extends TeamExplorerBaseNavigationLink {

    @Override
    public void onClick(
        final Shell shell,
        final TeamExplorerContext context,
        final TeamExplorerNavigator navigator,
        final TeamExplorerNavigationItemConfig parentNavigationItem) {
        final String uriText = TeamExplorerSettings.SAMPLE_LINK_URL;
        URI uri = null;
        try {
            uri = new URI(uriText);
        } catch (final URISyntaxException e) {

        }

        BrowserFacade.launchURL(uri, null, null, null, LaunchMode.EXTERNAL);

    }

}
