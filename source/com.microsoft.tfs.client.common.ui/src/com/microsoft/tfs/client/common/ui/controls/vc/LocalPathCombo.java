// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.controls.vc;

import java.io.File;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;

import com.microsoft.tfs.core.clients.versioncontrol.path.LocalPath;
import com.microsoft.tfs.util.StringUtil;

public class LocalPathCombo extends AbstractPathCombo {

    private String restrictingPath = null;

    public LocalPathCombo(final Composite parent, final int style) {
        super(parent, style);
    }

    @Override
    protected String validatePath(final String path) {
        final File file = new File(path);

        if (restrictingPath != null && LocalPath.equals(path, restrictingPath)) {
            this.upButton.setEnabled(false);
        } else {
            this.upButton.setEnabled(true);
        }

        if (file.exists()) {
            return file.getAbsolutePath();
        }

        return null;
    }

    @Override
    protected AbstractPathComboItem[] getHierarchy(final String path) {
        final String[] pathHierarchy = LocalPath.getHierarchy(path, restrictingPath);
        final AbstractPathComboItem[] itemHierarchy = new AbstractPathComboItem[pathHierarchy.length];

        for (int i = 0; i < pathHierarchy.length; i++) {
            final String shortName;

            if (i == 0) {
                shortName = pathHierarchy[i];
            } else {
                shortName = LocalPath.getFileName(pathHierarchy[i]);
            }

            itemHierarchy[i] = new AbstractPathComboItem(pathHierarchy[i], shortName);
        }

        return itemHierarchy;
    }

    @Override
    protected String browsePressed() {
        final DirectoryDialog browseDialog = new DirectoryDialog(getShell(), SWT.OPEN);

        browseDialog.setFilterPath(getPath());

        return browseDialog.open();
    }

    @Override
    protected String getParentPath(final String path) {
        if (StringUtil.isNullOrEmpty(path)) {
            return null;
        }
        final String parentPath = LocalPath.getParent(path);

        if (LocalPath.equals(path, parentPath)) {
            return null;
        }

        return parentPath;
    }

    public void setRestrictingPath(final String path) {
        this.restrictingPath = path;
    }
}
