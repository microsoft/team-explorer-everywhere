// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.sdk.samples.teamexplorer;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.forms.widgets.FormToolkit;

import com.microsoft.tfs.client.common.ui.framework.helper.SWTUtil;
import com.microsoft.tfs.client.common.ui.framework.layout.GridDataBuilder;
import com.microsoft.tfs.client.common.ui.teamexplorer.TeamExplorerContext;
import com.microsoft.tfs.client.common.ui.teamexplorer.sections.ITeamExplorerSection;
import com.microsoft.tfs.client.common.ui.teamexplorer.sections.TeamExplorerSectionRegenerateListener;

public class TeamExplorerSampleBaseSection1 implements ITeamExplorerSection {
    private String id;
    private String title;

    @Override
    public boolean isVisible(final TeamExplorerContext context) {
        return true;
    }

    @Override
    public boolean initializeInBackground(final TeamExplorerContext context) {
        return false;
    }

    @Override
    public void initialize(final IProgressMonitor monitor, final TeamExplorerContext context, final Object state) {
    }

    @Override
    public Composite getSectionContent(
        final FormToolkit toolkit,
        final Composite parent,
        final int style,
        final TeamExplorerContext context) {
        final Composite composite = toolkit.createComposite(parent);

        // Form-style border painting not enabled (0 pixel margins OK) because
        // no applicable controls in this composite
        SWTUtil.gridLayout(composite, 1, false, 0, 5);

        final Label label = toolkit.createLabel(composite, "Sample Label"); //$NON-NLS-1$
        label.setBackground(label.getDisplay().getSystemColor(SWT.COLOR_YELLOW));
        GridDataBuilder.newInstance().hAlignFill().hGrab().hSpan(2).applyTo(label);

        return composite;
    }

    @Override
    public String getID() {
        return id;
    }

    @Override
    public void setID(final String id) {
        this.id = id;
    }

    @Override
    public String getTitle() {
        return title;
    }

    @Override
    public void setTitle(final String title) {
        this.title = title;
    }

    @Override
    public void addSectionRegenerateListener(final TeamExplorerSectionRegenerateListener listener) {
    }

    @Override
    public void removeSectionRegenerateListener(final TeamExplorerSectionRegenerateListener listener) {
    }

    @Override
    public Object saveState() {
        return null;
    }
}
