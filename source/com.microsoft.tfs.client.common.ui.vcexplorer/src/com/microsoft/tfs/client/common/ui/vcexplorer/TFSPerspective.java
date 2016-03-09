// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.vcexplorer;

import org.eclipse.ui.IFolderLayout;
import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IPerspectiveFactory;

import com.microsoft.tfs.client.common.ui.teamexplorer.internal.TeamExplorerHelpers;
import com.microsoft.tfs.client.common.ui.views.TeamExplorerView;

/**
 * Perpective factory to create the initial layout for the TFS Explorer
 * Perspective.
 */
public class TFSPerspective implements IPerspectiveFactory {

    /**
     *
     */
    public TFSPerspective() {
        super();
    }

    /**
     * Defines the initial layout for a perspective.
     *
     * Implementors of this method may add additional views to a perspective.
     * The perspective already contains an editor folder with
     * <code>ID = ILayoutFactory.ID_EDITORS</code>. Add additional views to the
     * perspective in reference to the editor folder.
     *
     * This method is only called when a new perspective is created. If an old
     * perspective is restored from a persistence file then this method is not
     * called.
     *
     * @param layout
     *        the factory used to add views to the perspective
     */
    @Override
    public void createInitialLayout(final IPageLayout layout) {
        defineActions(layout);
        defineLayout(layout);

    }

    /**
     * Defines the initial actions for a page.
     *
     * @param layout
     *        The layout we are filling
     */
    public void defineActions(final IPageLayout layout) {
        // Add "show views".
        layout.addShowViewShortcut(TeamExplorerView.ID);
        layout.addShowViewShortcut(TeamExplorerHelpers.PendingChangesViewID);
        layout.addShowViewShortcut(TeamExplorerHelpers.BuildsViewID);
    }

    /**
     * Defines the initial layout for a page.
     *
     * @param layout
     *        The layout we are filling
     */
    public void defineLayout(final IPageLayout layout) {
        // Editors are placed for free.
        final String editorArea = layout.getEditorArea();

        final IFolderLayout topRight = layout.createFolder("topRight", IPageLayout.RIGHT, (float) 0.75, editorArea); //$NON-NLS-1$

        // Add the team explorer.
        topRight.addView(TeamExplorerView.ID);

        // Disable closing of these important windows.
        layout.setFixed(true);
    }
}
