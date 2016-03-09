// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.teamexplorer.internal;

import org.eclipse.core.runtime.IConfigurationElement;

import com.microsoft.tfs.client.common.ui.teamexplorer.link.ITeamExplorerNavigationLink;

public class TeamExplorerNavigationLinkConfig extends TeamExplorerOrderedComponent {
    private final String parentID;

    public TeamExplorerNavigationLinkConfig(final IConfigurationElement element) {
        super(element);
        this.parentID = element.getAttribute(PARENTID_ATTR_NAME);
    }

    public String getParentID() {
        return parentID;
    }

    public ITeamExplorerNavigationLink createInstance() {
        return (ITeamExplorerNavigationLink) TeamExplorerConfigHelpers.createInstance(getElement(), getID());
    }

    public static TeamExplorerNavigationLinkConfig fromConfigurationElement(final IConfigurationElement element) {
        return new TeamExplorerNavigationLinkConfig(element);
    }
}
