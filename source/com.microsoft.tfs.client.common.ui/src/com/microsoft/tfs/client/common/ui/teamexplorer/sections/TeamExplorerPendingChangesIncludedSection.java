// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.teamexplorer.sections;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Hyperlink;
import org.eclipse.ui.forms.widgets.ImageHyperlink;

import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.framework.helper.SWTUtil;
import com.microsoft.tfs.client.common.ui.framework.layout.GridDataBuilder;
import com.microsoft.tfs.client.common.ui.helpers.AutomationIDHelper;
import com.microsoft.tfs.client.common.ui.teamexplorer.TeamExplorerContext;
import com.microsoft.tfs.client.common.ui.teamexplorer.helpers.PageHelpers;
import com.microsoft.tfs.client.common.ui.teamexplorer.internal.TeamExplorerHelpers;
import com.microsoft.tfs.client.common.ui.teamexplorer.internal.pendingchanges.PendingChangesIncludedTree;
import com.microsoft.tfs.client.common.ui.teamexplorer.internal.pendingchanges.PendingChangesTree;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.PendingChange;

public class TeamExplorerPendingChangesIncludedSection extends TeamExplorerPendingChangesTreeSection {
    public static final String EXCLUDE_ALL_HYPERLINK_ID =
        "com.microsoft.tfs.client.common.ui.teamexplorer.sections.TeamExplorerPendingChangesIncludedSection#excludeAllHyperlink"; //$NON-NLS-1$

    @Override
    public PendingChangesTree createTreeInstance() {
        return new PendingChangesIncludedTree();
    }

    @Override
    public PendingChange[] getFilteredChangesForTree() {
        verifyModelLoaded();
        return getModel().getIncludedPendingChanges();
    }

    @Override
    public int getUnfilteredChangeCount() {
        return getModel() == null ? 0 : getModel().getIncludedUnfilteredChangeCount();
    }

    @Override
    public int getFilteredChangeCount() {
        return getModel() == null ? 0 : getModel().getIncludedFilteredChangeCount();
    }

    @Override
    public boolean getFilterEnabled() {
        return getModel() == null ? false : getModel().getIncludedFilterEnabled();
    }

    @Override
    public void setFilterEnabled(final boolean enabled) {
        verifyModelLoaded();

        if (getModel().getIncludedFilterEnabled() != enabled) {
            getModel().setIncludedFilterEnabled(enabled);
            getModel().setIncludedFilterText(""); //$NON-NLS-1$
        }
    }

    @Override
    public String getFilterText() {
        return getModel() == null ? "" : getModel().getIncludedFilterText(); //$NON-NLS-1$
    }

    @Override
    public void setFilterText(final String filterText) {
        verifyModelLoaded();
        getModel().setIncludedFilterText(filterText);
    }

    @Override
    public void addPendingChangesChangedListener(final PendingChangesListener listener) {
        verifyModelLoaded();
        getModel().addIncludedPendingChangesChangedListener(listener);
    }

    @Override
    public void removePendingChangesChangedListener(final PendingChangesListener listener) {
        verifyModelLoaded();
        getModel().removeIncludedPendingChangesChangedListener(listener);
    }

    @Override
    public void addPendingChangesCountChangedListener(final PendingChangesCountListener listener) {
        verifyModelLoaded();
        getModel().addIncludedPendingChangesCountChangedListener(listener);
    }

    @Override
    public void removePendingChangesCountChangedListener(final PendingChangesCountListener listener) {
        verifyModelLoaded();
        getModel().removeIncludedPendingChangesCountChangedListener(listener);
    }

    @Override
    public void onPendingChangesChanged() {
        if (getModel().getIncludedUnfilteredChangeCount() == 0) {
            getModel().clearPolicyWarnings();
        }
    }

    @Override
    public void createCompositeHeader(
        final FormToolkit toolkit,
        final Composite composite,
        final TeamExplorerContext context) {
        final Composite headerComposite = toolkit.createComposite(composite);

        // Form-style border painting not enabled (0 pixel margins OK) because
        // no applicable controls in this composite
        SWTUtil.gridLayout(headerComposite, 3, false, 0, 0);
        GridDataBuilder.newInstance().applyTo(headerComposite);

        final String title = Messages.getString("TeamExplorerPendingChangesIncludedSection.ExcludeAllLinkText"); //$NON-NLS-1$
        final Hyperlink includeAllHyperlink = toolkit.createHyperlink(headerComposite, title, SWT.WRAP);
        includeAllHyperlink.setUnderlined(false);
        AutomationIDHelper.setWidgetID(includeAllHyperlink, EXCLUDE_ALL_HYPERLINK_ID);
        includeAllHyperlink.addHyperlinkListener(new HyperlinkAdapter() {
            @Override
            public void linkActivated(final HyperlinkEvent e) {
                getModel().excludeAllPendingChanges();
                clearFilterTextbox();
                TeamExplorerHelpers.updateContainingSectionTitle(treeComposite, getTitle());
            }
        });
        GridDataBuilder.newInstance().applyTo(includeAllHyperlink);

        final Label separator = toolkit.createLabel(headerComposite, "|", SWT.VERTICAL); //$NON-NLS-1$
        GridDataBuilder.newInstance().vFill().applyTo(separator);

        final String linkText = Messages.getString("TeamExplorerPendingChangesIncludedSection.FilterLinkText"); //$NON-NLS-1$
        final Menu menu = createFilterMenu(composite.getShell());
        final ImageHyperlink link = PageHelpers.createDropHyperlink(toolkit, headerComposite, linkText, menu);
        GridDataBuilder.newInstance().applyTo(link);
    }
}
