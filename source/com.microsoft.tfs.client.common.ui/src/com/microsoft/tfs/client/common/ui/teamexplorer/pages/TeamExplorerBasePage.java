// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.teamexplorer.pages;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.widgets.FormToolkit;

import com.microsoft.tfs.client.common.ui.teamexplorer.TeamExplorerContext;

public class TeamExplorerBasePage implements ITeamExplorerPage {
    public void initialize(final IProgressMonitor monitor, final TeamExplorerContext context) {
    }

    @Override
    public void initialize(final IProgressMonitor monitor, final TeamExplorerContext context, final Object state) {
        initialize(monitor, context);
    }

    @Override
    public void refresh(final IProgressMonitor monitor, final TeamExplorerContext context) {
    }

    @Override
    public Composite getPageContent(
        final FormToolkit toolkit,
        final Composite parent,
        final int style,
        final TeamExplorerContext context) {
        return null;
    }

    @Override
    public Object saveState() {
        return null;
    }
}
