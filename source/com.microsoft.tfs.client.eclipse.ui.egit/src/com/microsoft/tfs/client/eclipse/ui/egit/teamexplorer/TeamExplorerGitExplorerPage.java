// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.eclipse.ui.egit.teamexplorer;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.widgets.FormToolkit;

import com.microsoft.tfs.client.common.ui.teamexplorer.TeamExplorerContext;
import com.microsoft.tfs.client.common.ui.teamexplorer.pages.TeamExplorerBasePage;
import com.microsoft.tfs.util.Check;

public class TeamExplorerGitExplorerPage extends TeamExplorerBasePage {

    public static final String PAGE_ID =
        "com.microsoft.tfs.client.eclipse.ui.egit.teamexplorer.TeamExplorerGitExplorerPage"; //$NON-NLS-1$

    @Override
    public void refresh(final IProgressMonitor monitor, final TeamExplorerContext context) {
        Check.notNull(context, "context"); //$NON-NLS-1$
    }

    @Override
    public Composite getPageContent(
        final FormToolkit toolkit,
        final Composite parent,
        final int style,
        final TeamExplorerContext context) {
        return null;
    }
}
