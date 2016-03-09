// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.eclipse.ui.wizard.importwizard.support;

import com.microsoft.tfs.core.clients.versioncontrol.path.ServerPath;

/**
 * A base class for ImportFolder and ImportGitRepository classes.
 */
public abstract class ImportItemBase {
    private final String itemPath;

    public ImportItemBase(final String itemPath) {
        this.itemPath = itemPath;
    }

    /**
     * @return the full server path of this SelectedPath
     */
    public String getFullPath() {
        return itemPath;
    }

    /**
     * @return the name portion of this SelectedPath
     */
    public String getName() {
        return ServerPath.getFileName(itemPath);
    }
}