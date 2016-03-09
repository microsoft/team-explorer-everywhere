// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.controls.vc;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;

import com.microsoft.tfs.core.clients.versioncontrol.path.ServerPath;
import com.microsoft.tfs.core.exceptions.NotSupportedException;

public class ServerPathCombo extends AbstractPathCombo {
    private String serverName;

    public ServerPathCombo(final Composite parent, final int style) {
        super(parent, style | SWT.READ_ONLY);
    }

    public void setServerName(final String serverName) {
        this.serverName = serverName;
    }

    @Override
    protected String validatePath(final String path) {
        if (!path.startsWith("$/")) //$NON-NLS-1$
        {
            return null;
        }

        return ServerPath.canonicalize(path);
    }

    @Override
    protected String getParentPath(final String path) {
        final String parentPath = ServerPath.getParent(path);

        if (ServerPath.equals(path, parentPath)) {
            return null;
        }

        return parentPath;
    }

    @Override
    protected AbstractPathComboItem[] getHierarchy(final String path) {
        final String[] pathHierarchy = ServerPath.getHierarchy(path);
        final AbstractPathComboItem[] itemHierarchy = new AbstractPathComboItem[pathHierarchy.length];

        for (int i = 0; i < pathHierarchy.length; i++) {
            final String shortName;

            if (i == 0) {
                shortName = (serverName != null) ? serverName : ServerPath.ROOT;
            } else {
                shortName = ServerPath.getFileName(pathHierarchy[i]);
            }

            itemHierarchy[i] = new AbstractPathComboItem(pathHierarchy[i], shortName);
        }

        return itemHierarchy;
    }

    @Override
    protected String browsePressed() {
        throw new NotSupportedException();
    }
}
