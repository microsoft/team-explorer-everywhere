// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.teamexplorer.sections;

import java.text.MessageFormat;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Hyperlink;
import org.eclipse.ui.forms.widgets.ImageHyperlink;

import com.microsoft.tfs.client.common.repository.TFSRepository;
import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.controls.vc.changes.ChangeItem;
import com.microsoft.tfs.client.common.ui.framework.helper.SWTUtil;
import com.microsoft.tfs.client.common.ui.framework.helper.UIHelpers;
import com.microsoft.tfs.client.common.ui.framework.layout.GridDataBuilder;
import com.microsoft.tfs.client.common.ui.helpers.AutomationIDHelper;
import com.microsoft.tfs.client.common.ui.teamexplorer.TeamExplorerContext;
import com.microsoft.tfs.client.common.ui.teamexplorer.helpers.PageHelpers;
import com.microsoft.tfs.client.common.ui.teamexplorer.helpers.PendingChangesHelpers;
import com.microsoft.tfs.client.common.ui.teamexplorer.internal.TeamExplorerHelpers;
import com.microsoft.tfs.client.common.ui.teamexplorer.internal.pendingchanges.PendingChangesExcludedTree;
import com.microsoft.tfs.client.common.ui.teamexplorer.internal.pendingchanges.PendingChangesTree;
import com.microsoft.tfs.core.clients.versioncontrol.events.PendingChangeCandidatesChangedListener;
import com.microsoft.tfs.core.clients.versioncontrol.events.WorkspaceEvent;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.PendingChange;

public class TeamExplorerPendingChangesExcludedSection extends TeamExplorerPendingChangesTreeSection {
    public static final String INCLUDE_ALL_HYPERLINK_ID =
        "com.microsoft.tfs.client.common.ui.teamexplorer.sections.TeamExplorerPendingChangesExcludedSection#includeAllHyperlink"; //$NON-NLS-1$

    public static final String CANDIDATES_HYPERLINK_ID =
        "com.microsoft.tfs.client.common.ui.teamexplorer.sections.TeamExplorerPendingChangesExcludedSection#candidatesHyperlink"; //$NON-NLS-1$

    private Hyperlink linkCandidates;
    private final PendingChangeCandidatesChangedListener candidateListener = new CandidatesChangedListener();

    @Override
    public PendingChangesTree createTreeInstance() {
        return new PendingChangesExcludedTree();
    }

    @Override
    public PendingChange[] getFilteredChangesForTree() {
        verifyModelLoaded();
        return getModel().getExcludedPendingChanges();
    }

    @Override
    public int getUnfilteredChangeCount() {
        return getModel() == null ? 0 : getModel().getExcludedUnfilteredChangeCount();
    }

    @Override
    public int getFilteredChangeCount() {
        return getModel() == null ? 0 : getModel().getExcludedFilteredChangeCount();
    }

    @Override
    public boolean getFilterEnabled() {
        return getModel() == null ? false : getModel().getExcludedFilterEnabled();
    }

    @Override
    public void setFilterEnabled(final boolean enabled) {
        verifyModelLoaded();

        if (getModel().getExcludedFilterEnabled() != enabled) {
            getModel().setExcludedFilterVisible(enabled);
            getModel().setExcludedFilterText(""); //$NON-NLS-1$
        }
    }

    @Override
    public String getFilterText() {
        return getModel() == null ? "" : getModel().getExcludedFilterText(); //$NON-NLS-1$
    }

    @Override
    public void setFilterText(final String filterText) {
        verifyModelLoaded();
        getModel().setExcludedFilterText(filterText);
    }

    @Override
    public void addPendingChangesChangedListener(final PendingChangesListener listener) {
        verifyModelLoaded();
        getModel().addExcludedPendingChangesChangedListener(listener);
    }

    @Override
    public void removePendingChangesChangedListener(final PendingChangesListener listener) {
        verifyModelLoaded();
        getModel().removeExcludedPendingChangesChangedListener(listener);
    }

    @Override
    public void addPendingChangesCountChangedListener(final PendingChangesCountListener listener) {
        verifyModelLoaded();
        getModel().addExcludedPendingChangesCountChangedListener(listener);
    }

    @Override
    public void removePendingChangesCountChangedListener(final PendingChangesCountListener listener) {
        verifyModelLoaded();
        getModel().removeExcludedPendingChangesCountChangedListener(listener);
    }

    @Override
    public void onPendingChangesChanged() {
    }

    @Override
    public void createCompositeHeader(
        final FormToolkit toolkit,
        final Composite composite,
        final TeamExplorerContext context) {
        final Shell shell = composite.getShell();
        final Composite headerComposite = toolkit.createComposite(composite);

        // Form-style border painting not enabled (0 pixel margins OK) because
        // no applicable controls in this composite
        SWTUtil.gridLayout(headerComposite, 5, false, 0, 0);
        GridDataBuilder.newInstance().hAlignFill().hGrab().applyTo(headerComposite);

        final String titleInclude = Messages.getString("TeamExplorerPendingChangesExcludedSection.IncludeAllLinkText"); //$NON-NLS-1$
        final Hyperlink linkIncludeAll = toolkit.createHyperlink(headerComposite, titleInclude, SWT.WRAP);
        linkIncludeAll.setUnderlined(false);
        AutomationIDHelper.setWidgetID(linkIncludeAll, INCLUDE_ALL_HYPERLINK_ID);
        linkIncludeAll.addHyperlinkListener(new HyperlinkAdapter() {
            @Override
            public void linkActivated(final HyperlinkEvent e) {
                getModel().includeAllPendingChanges();
                clearFilterTextbox();
                TeamExplorerHelpers.updateContainingSectionTitle(treeComposite, getTitle());
            }
        });
        GridDataBuilder.newInstance().applyTo(linkIncludeAll);

        final Label separator = toolkit.createLabel(headerComposite, "|", SWT.VERTICAL); //$NON-NLS-1$
        GridDataBuilder.newInstance().vFill().applyTo(separator);

        final String linkText = Messages.getString("TeamExplorerPendingChangesExcludedSection.FilterLinkText"); //$NON-NLS-1$
        final Menu menu = createFilterMenu(shell);
        final ImageHyperlink link = PageHelpers.createDropHyperlink(toolkit, headerComposite, linkText, menu);
        GridDataBuilder.newInstance().applyTo(link);

        if (getModel().isLocalWorkspace()) {
            final Label separator2 = toolkit.createLabel(headerComposite, "|", SWT.VERTICAL); //$NON-NLS-1$
            GridDataBuilder.newInstance().vFill().applyTo(separator2);

            linkCandidates = toolkit.createHyperlink(headerComposite, getCandidateLinkText(), SWT.WRAP);
            linkCandidates.setUnderlined(false);
            AutomationIDHelper.setWidgetID(linkCandidates, CANDIDATES_HYPERLINK_ID);
            linkCandidates.addHyperlinkListener(new HyperlinkAdapter() {
                @Override
                public void linkActivated(final HyperlinkEvent e) {
                    final TFSRepository repository = context.getDefaultRepository();
                    final PendingChange[] pendingChanges = getModel().getCandidatePendingChanges();
                    final ChangeItem[] changeItems =
                        PendingChangesHelpers.pendingChangesToChangeItems(repository, pendingChanges);
                    PendingChangesHelpers.showPromoteCandidateChanges(shell, repository, changeItems);
                }
            });
            GridDataBuilder.newInstance().applyTo(linkCandidates);
        }

        // Excluded section includes a listener for candidate changes.
        getModel().addPendingChangeCandidatesChangedListener(candidateListener);

        // Remove the listener when disposed.
        headerComposite.addDisposeListener(new DisposeListener() {
            @Override
            public void widgetDisposed(final DisposeEvent e) {
                getModel().removePendingChangeCandidatesChangedListener(candidateListener);
            }
        });
    }

    private String getCandidateLinkText() {
        final String format = Messages.getString("TeamExplorerPendingChangesExcludedSection.DetectedChangesFormat"); //$NON-NLS-1$
        return MessageFormat.format(format, getModel().getCandidateChangeCount());
    }

    protected class CandidatesChangedListener implements PendingChangeCandidatesChangedListener {
        @Override
        public void onPendingChangeCandidatesChanged(final WorkspaceEvent e) {
            UIHelpers.runOnUIThread(true, new Runnable() {
                @Override
                public void run() {
                    if (linkCandidates == null || linkCandidates.isDisposed()) {
                        return;
                    }

                    linkCandidates.setText(getCandidateLinkText());
                }
            });
        }
    }
}
