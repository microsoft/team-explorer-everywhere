// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.teamexplorer.internal;

import org.eclipse.core.runtime.IConfigurationElement;

import com.microsoft.tfs.client.common.ui.teamexplorer.pages.ITeamExplorerPage;

public class TeamExplorerPageConfig extends TeamExplorerBaseConfig {
    public TeamExplorerPageConfig(final IConfigurationElement element) {
        super(element);
    }

    public ITeamExplorerPage createInstance() {
        return (ITeamExplorerPage) TeamExplorerConfigHelpers.createInstance(getElement(), getID());
    }

    public static TeamExplorerPageConfig fromConfigurationElement(final IConfigurationElement element) {
        return new TeamExplorerPageConfig(element);
    }
}
