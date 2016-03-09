// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.teamexplorer.internal;

import org.eclipse.core.runtime.IConfigurationElement;

public abstract class TeamExplorerOrderedComponent extends TeamExplorerBaseConfig {
    private final int displayPriority;

    public TeamExplorerOrderedComponent(final IConfigurationElement element) {
        super(element);
        this.displayPriority = TeamExplorerConfigHelpers.getInteger(element, PRIORITY_ATTR_NAME);
    }

    public int getDisplayPriority() {
        return displayPriority;
    }
}
