// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.teamexplorer.items;

import com.microsoft.tfs.client.common.ui.teamexplorer.TeamExplorerContext;

/**
 * The interface of all Team Explorer navigation items.
 */
public interface ITeamExplorerNavigationItem {
    public boolean isVisible(final TeamExplorerContext context);

    /**
     * Execute this function when click a navigation item which does not
     * navigate to pages in Team Explorer. e.g. may open a separate view or
     * editor on click
     *
     * @param context
     */
    public void clicked(final TeamExplorerContext context);

    /**
     * Return whether this can be opened in web.
     *
     * @return
     */
    public boolean canOpenInWeb();

    /**
     * Implement this function to open the navigation item in web portal.
     *
     * @param context
     */
    public void openInWeb(final TeamExplorerContext context);
}
