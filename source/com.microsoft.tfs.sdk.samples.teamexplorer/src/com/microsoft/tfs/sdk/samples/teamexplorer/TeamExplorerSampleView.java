// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.sdk.samples.teamexplorer;

import com.microsoft.tfs.client.common.ui.views.TeamExplorerDockableView;

/**
 * This is a sample Team Explorer dockable view for the sample page
 *
 */
public class TeamExplorerSampleView extends TeamExplorerDockableView {

    /**
     * {@inheritDoc}
     */
    @Override
    protected String getPageID() {
        /**
         * The pageID of the page which you want to display in this view.
         */
        return "com.microsoft.tfs.sdk.samples.teamexplorer.TeamExplorerSamplePage2"; //$NON-NLS-1$
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void initialize() {
        // do any specific initialization here
    }
}
