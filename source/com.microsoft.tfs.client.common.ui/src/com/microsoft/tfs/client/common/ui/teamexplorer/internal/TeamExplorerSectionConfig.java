// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.teamexplorer.internal;

import org.eclipse.core.runtime.IConfigurationElement;

import com.microsoft.tfs.client.common.ui.teamexplorer.sections.ITeamExplorerSection;

public class TeamExplorerSectionConfig extends TeamExplorerOrderedComponent {
    private final String pageID;

    public TeamExplorerSectionConfig(final IConfigurationElement element) {
        super(element);
        this.pageID = element.getAttribute("pageID"); //$NON-NLS-1$
    }

    public String getPageID() {
        return pageID;
    }

    public ITeamExplorerSection createInstance() {
        final ITeamExplorerSection section =
            (ITeamExplorerSection) TeamExplorerConfigHelpers.createInstance(getElement(), getID());

        section.setID(getID());
        section.setTitle(getTitle());

        return section;
    }

    public static TeamExplorerSectionConfig fromConfigurationElement(final IConfigurationElement element) {
        return new TeamExplorerSectionConfig(element);
    }
}
