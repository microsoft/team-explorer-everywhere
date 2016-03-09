// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.teamexplorer.pages;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.widgets.FormToolkit;

import com.microsoft.tfs.client.common.ui.teamexplorer.TeamExplorerContext;

public interface ITeamExplorerPage {
    public void initialize(IProgressMonitor monitor, TeamExplorerContext context, Object state);

    public void refresh(IProgressMonitor monitor, TeamExplorerContext context);

    public Composite getPageContent(
        final FormToolkit toolkit,
        final Composite parent,
        final int style,
        final TeamExplorerContext context);

    public Object saveState();
}
