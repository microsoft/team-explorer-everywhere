// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.teamexplorer.sections;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.widgets.FormToolkit;

import com.microsoft.tfs.client.common.ui.teamexplorer.TeamExplorerContext;

public interface ITeamExplorerSection {
    public boolean isVisible(TeamExplorerContext context);

    public boolean initializeInBackground(TeamExplorerContext context);

    public void initialize(IProgressMonitor monitor, TeamExplorerContext context, Object state);

    public String getID();

    public void setID(String id);

    public String getTitle();

    public void setTitle(String title);

    public Composite getSectionContent(
        final FormToolkit toolkit,
        final Composite parent,
        final int style,
        final TeamExplorerContext context);

    public void addSectionRegenerateListener(TeamExplorerSectionRegenerateListener listener);

    public void removeSectionRegenerateListener(TeamExplorerSectionRegenerateListener listener);

    public Object saveState();
}
