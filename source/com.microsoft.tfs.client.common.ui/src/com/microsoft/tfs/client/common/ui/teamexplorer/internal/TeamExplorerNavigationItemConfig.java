// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.teamexplorer.internal;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.swt.graphics.Image;

import com.microsoft.tfs.client.common.ui.teamexplorer.items.ITeamExplorerNavigationItem;

public class TeamExplorerNavigationItemConfig extends TeamExplorerOrderedComponent {
    private final Image icon;
    protected String targetPageID;
    private final String viewID;

    public TeamExplorerNavigationItemConfig(final IConfigurationElement element) {
        super(element);
        this.targetPageID = element.getAttribute(TARGETPAGEID_ATTR_NAME);
        this.icon = TeamExplorerConfigHelpers.getIcon(element, ICON_ATTR_NAME);
        this.viewID = element.getAttribute(TARGETVIEWID_ATTR_NAME);
    }

    public Image getIcon() {
        return icon;
    }

    public String getTargetPageID() {
        return targetPageID;
    }

    /**
     * This function returns null when a page can not be undocked; viewID not
     * null means this page can be undocked, viewID corresponds to the target
     * view ID.
     *
     * @return
     */
    public String getViewID() {
        return viewID;
    }

    public ITeamExplorerNavigationItem createInstance() {
        return (ITeamExplorerNavigationItem) TeamExplorerConfigHelpers.createInstance(getElement(), getID());
    }

    public static TeamExplorerNavigationItemConfig fromConfigurationElement(final IConfigurationElement element) {
        return new TeamExplorerNavigationItemConfig(element);
    }
}
