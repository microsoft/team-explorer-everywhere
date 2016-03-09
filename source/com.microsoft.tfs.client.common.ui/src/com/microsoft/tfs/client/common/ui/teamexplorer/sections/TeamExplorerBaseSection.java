// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.teamexplorer.sections;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.forms.widgets.FormToolkit;

import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.framework.helper.SWTUtil;
import com.microsoft.tfs.client.common.ui.framework.layout.GridDataBuilder;
import com.microsoft.tfs.client.common.ui.teamexplorer.TeamExplorerContext;
import com.microsoft.tfs.util.listeners.SingleListenerFacade;

public class TeamExplorerBaseSection implements ITeamExplorerSection {
    public static final String EXTENSION_POINT_ID = "com.microsoft.tfs.client.common.ui.teamExplorer"; //$NON-NLS-1$

    private final SingleListenerFacade listeners =
        new SingleListenerFacade(TeamExplorerSectionRegenerateListener.class);
    private String sectionID;
    protected String baseTitle;

    @Override
    public boolean isVisible(final TeamExplorerContext context) {
        return true;
    }

    @Override
    public boolean initializeInBackground(final TeamExplorerContext context) {
        return false;
    }

    public void initialize(final IProgressMonitor monitor, final TeamExplorerContext context) {
    }

    @Override
    public void initialize(final IProgressMonitor monitor, final TeamExplorerContext context, final Object state) {
        initialize(monitor, context);
    }

    @Override
    public String getID() {
        return sectionID;
    }

    @Override
    public void setID(final String sectionID) {
        this.sectionID = sectionID;
    }

    @Override
    public String getTitle() {
        return baseTitle;
    }

    @Override
    public void setTitle(final String baseTitle) {
        this.baseTitle = baseTitle;
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
        SWTUtil.gridLayout(composite);

        final Label label = toolkit.createLabel(composite, "Not yet implemented"); //$NON-NLS-1$
        GridDataBuilder.newInstance().hAlignFill().hGrab().applyTo(label);

        return composite;
    }

    public void registerContextMenu(
        final TeamExplorerContext context,
        final Control control,
        final ISelectionProvider provider) {
        final MenuManager contextMenuManager = new MenuManager("#PopupMenu"); //$NON-NLS-1$
        contextMenuManager.setRemoveAllWhenShown(false);
        contextMenuManager.add(new Separator("group1")); //$NON-NLS-1$
        contextMenuManager.add(new Separator("group2")); //$NON-NLS-1$
        contextMenuManager.add(new Separator("group3")); //$NON-NLS-1$
        contextMenuManager.add(new Separator("group4")); //$NON-NLS-1$
        contextMenuManager.add(new Separator("group5")); //$NON-NLS-1$
        contextMenuManager.add(new Separator("group6")); //$NON-NLS-1$
        contextMenuManager.add(new Separator("group7")); //$NON-NLS-1$
        contextMenuManager.add(new Separator("group8")); //$NON-NLS-1$
        contextMenuManager.add(new Separator("group9")); //$NON-NLS-1$
        contextMenuManager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));

        final Menu menu = contextMenuManager.createContextMenu(control);
        control.setMenu(menu);

        context.getWorkbenchPart().getSite().registerContextMenu(EXTENSION_POINT_ID, contextMenuManager, provider);
    }

    protected void createDisconnectedContent(final FormToolkit toolkit, final Composite composite) {
        final String message = Messages.getString("TeamExplorerReportsSection.DisconnectedLabel"); //$NON-NLS-1$
        final Label label = toolkit.createLabel(composite, message);
        GridDataBuilder.newInstance().hAlignFill().hGrab().applyTo(label);
    }

    @Override
    public void addSectionRegenerateListener(final TeamExplorerSectionRegenerateListener listener) {
        listeners.addListener(listener);
    }

    @Override
    public void removeSectionRegenerateListener(final TeamExplorerSectionRegenerateListener listener) {
        listeners.removeListener(listener);
    }

    public void fireSectionRegenerateEvent() {
        ((TeamExplorerSectionRegenerateListener) listeners.getListener()).onSectionRegenerate(sectionID);
    }

    @Override
    public Object saveState() {
        return null;
    }
}
