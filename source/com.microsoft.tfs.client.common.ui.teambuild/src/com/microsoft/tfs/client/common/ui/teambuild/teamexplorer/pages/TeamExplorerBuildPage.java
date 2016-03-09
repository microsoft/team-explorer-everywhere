// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.teambuild.teamexplorer.pages;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.widgets.FormToolkit;

import com.microsoft.tfs.client.common.ui.teamexplorer.TeamExplorerContext;
import com.microsoft.tfs.client.common.ui.teamexplorer.pages.TeamExplorerBasePage;

public class TeamExplorerBuildPage extends TeamExplorerBasePage {
    public static final String ID =
        "com.microsoft.tfs.client.common.ui.teambuild.teamexplorer.pages.TeamExplorerBuildPage"; //$NON-NLS-1$

    @Override
    public Composite getPageContent(
        final FormToolkit toolkit,
        final Composite parent,
        final int style,
        final TeamExplorerContext context) {
        // Text controls present in this composite, enable form-style borders,
        // must have at least 1 pixel margins
        toolkit.paintBordersFor(parent);
        return parent;
    }
}
