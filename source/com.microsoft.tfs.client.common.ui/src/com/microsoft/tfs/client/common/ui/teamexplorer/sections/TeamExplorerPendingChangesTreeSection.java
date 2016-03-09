// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.teamexplorer.sections;

import java.text.MessageFormat;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.widgets.FormToolkit;

import com.microsoft.tfs.client.common.codemarker.CodeMarker;
import com.microsoft.tfs.client.common.codemarker.CodeMarkerDispatch;
import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.TFSCommonUIClientPlugin;
import com.microsoft.tfs.client.common.ui.framework.helper.SWTUtil;
import com.microsoft.tfs.client.common.ui.framework.helper.UIHelpers;
import com.microsoft.tfs.client.common.ui.framework.image.ImageHelper;
import com.microsoft.tfs.client.common.ui.framework.layout.GridDataBuilder;
import com.microsoft.tfs.client.common.ui.helpers.AutomationIDHelper;
import com.microsoft.tfs.client.common.ui.teamexplorer.TeamExplorerContext;
import com.microsoft.tfs.client.common.ui.teamexplorer.TeamExplorerEvents;
import com.microsoft.tfs.client.common.ui.teamexplorer.TeamExplorerResizeListener;
import com.microsoft.tfs.client.common.ui.teamexplorer.internal.TeamExplorerHelpers;
import com.microsoft.tfs.client.common.ui.teamexplorer.internal.pendingchanges.PendingChangesTree;
import com.microsoft.tfs.client.common.ui.teamexplorer.internal.pendingchanges.PendingChangesTreeContentProvider;
import com.microsoft.tfs.client.common.ui.teamexplorer.internal.pendingchanges.PendingChangesTreeDoubleClickListener;
import com.microsoft.tfs.client.common.ui.teamexplorer.internal.pendingchanges.PendingChangesTreeLabelProvider;
import com.microsoft.tfs.core.clients.versioncontrol.WorkspaceLocation;
import com.microsoft.tfs.core.clients.versioncontrol.events.PendingChangesChangedListener;
import com.microsoft.tfs.core.clients.versioncontrol.events.PendingChangesCountChangedListener;
import com.microsoft.tfs.core.clients.versioncontrol.events.WorkspaceEvent;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.PendingChange;
import com.microsoft.tfs.util.Check;

public abstract class TeamExplorerPendingChangesTreeSection extends TeamExplorerPendingChangesBaseSection {
    public static final String PENDING_CHANGES_TREE_ID =
        "com.microsoft.tfs.client.common.ui.teamexplorer.sections.TeamExplorerPendingChangesTreeSection.PendingChangesTree"; //$NON-NLS-1$

    public static final CodeMarker AFTER_TREE_REFRESH = new CodeMarker(
        "com.microsoft.tfs.client.common.ui.teamexplorer.sections.TeamExplorerPendingChangesTreeSection#afterTreeRefresh"); //$NON-NLS-1$

    ImageHelper imageHelper = new ImageHelper(TFSCommonUIClientPlugin.PLUGIN_ID);

    protected Composite treeComposite;

    private TreeViewer treeViewer;
    private Label emptyLabel;
    private Text textFilter;
    private PendingChangesTree pendingChangesTree;

    private final PendingChangesListener pendingChangesListener = new PendingChangesListener();

    private final PendingChangesCountListener pendingChangesCountListener = new PendingChangesCountListener();

    public abstract PendingChangesTree createTreeInstance();

    public abstract PendingChange[] getFilteredChangesForTree();

    private TeamExplorerPendingChangesTreeState state;

    public abstract int getUnfilteredChangeCount();

    public abstract int getFilteredChangeCount();

    public abstract void createCompositeHeader(FormToolkit toolkit, Composite composite, TeamExplorerContext context);

    public abstract boolean getFilterEnabled();

    public abstract void setFilterEnabled(final boolean enabled);

    public abstract String getFilterText();

    public abstract void setFilterText(final String filterText);

    public abstract void addPendingChangesChangedListener(final PendingChangesListener listener);

    public abstract void removePendingChangesChangedListener(final PendingChangesListener listener);

    public abstract void addPendingChangesCountChangedListener(final PendingChangesCountListener listener);

    public abstract void removePendingChangesCountChangedListener(final PendingChangesCountListener listener);

    public abstract void onPendingChangesChanged();

    @Override
    public void initialize(final IProgressMonitor monitor, final TeamExplorerContext context) {
        super.initialize(monitor, context);

        createTree();
    }

    @Override
    public void initialize(final IProgressMonitor monitor, final TeamExplorerContext context, final Object state) {
        initialize(monitor, context);

        if (state instanceof TeamExplorerPendingChangesTreeState) {
            this.state = (TeamExplorerPendingChangesTreeState) state;
        }
    }

    protected void verifyModelLoaded() {
        Check.notNull(getModel(), "Pending change model is not loaded"); //$NON-NLS-1$
    }

    private void createTree() {
        pendingChangesTree = createTreeInstance();

        for (final PendingChange pendingChange : getFilteredChangesForTree()) {
            pendingChangesTree.addPendingChange(pendingChange);
        }

        pendingChangesTree.collapseRedundantLevels();
    }

    private void showOrHideTree(final boolean show) {
        final GridData gridDataTree = (GridData) treeViewer.getTree().getLayoutData();
        final GridData gridDataLabel = (GridData) emptyLabel.getLayoutData();

        if (show) {
            emptyLabel.setVisible(false);
            treeViewer.getTree().setVisible(true);

            gridDataLabel.exclude = true;
            gridDataTree.exclude = false;
        } else {
            emptyLabel.setVisible(true);
            treeViewer.getTree().setVisible(false);

            gridDataLabel.exclude = false;
            gridDataTree.exclude = true;
        }
    }

    private void refreshTree() {
        UIHelpers.runOnUIThread(true, new Runnable() {
            @Override
            public void run() {
                if (treeViewer == null || treeViewer.getTree().isDisposed()) {
                    return;
                }

                saveState();

                final boolean wasEmpty = pendingChangesTree.isEmpty();

                createTree();

                final boolean isEmpty = pendingChangesTree.isEmpty();

                if (wasEmpty != isEmpty) {
                    showOrHideTree(!isEmpty);
                }

                treeViewer.setInput(pendingChangesTree);
                restoreState();
                treeViewer.refresh();

                TeamExplorerHelpers.updateContainingSectionTitle(treeComposite, getTitle());
                TeamExplorerHelpers.relayoutContainingScrolledComposite(treeComposite);
            }
        });
    }

    private void refreshTitle() {
        UIHelpers.runOnUIThread(true, new Runnable() {
            @Override
            public void run() {
                TeamExplorerHelpers.updateContainingSectionTitle(treeComposite, getTitle());
            }

        });
    }

    @Override
    public Object saveState() {
        if (state == null) {
            state = new TeamExplorerPendingChangesTreeState(treeViewer);
        } else {
            state.updateTreeState(treeViewer);
        }
        return state;
    }

    public void restoreState() {
        if (state == null) {
            treeViewer.expandAll();
        } else {
            state.restoreTreeState(treeViewer, pendingChangesTree);
        }
        treeViewer.refresh();
    }

    @Override
    public String getTitle() {
        final int filteredCount = getFilteredChangeCount();
        final int unfilteredCount = getUnfilteredChangeCount();

        if (unfilteredCount == 0) {
            return baseTitle;
        } else if (filteredCount == unfilteredCount) {
            final String format = Messages.getString("TeamExplorerCommon.TitleWithCountFormat"); //$NON-NLS-1$
            return MessageFormat.format(format, baseTitle, unfilteredCount);
        } else {
            final String format = Messages.getString("TeamExplorerCommon.TitleWithFilteredCountFormat"); //$NON-NLS-1$
            return MessageFormat.format(format, baseTitle, filteredCount, unfilteredCount);
        }
    }

    @Override
    public Composite getSectionContent(
        final FormToolkit toolkit,
        final Composite parent,
        final int style,
        final TeamExplorerContext context) {
        final Composite composite = toolkit.createComposite(parent);

        // Text controls present in this composite, enable form-style borders,
        // must have at least 1 pixel margins
        toolkit.paintBordersFor(composite);
        SWTUtil.gridLayout(composite, 1, false, 1, 1);

        createCompositeHeader(toolkit, composite, context);

        createFilterControl(toolkit, composite);
        setFilterVisibility(getFilterEnabled());

        treeComposite = toolkit.createComposite(composite);
        final GridLayout treeLayout = SWTUtil.gridLayout(treeComposite, 1, false, 0, 0);
        treeLayout.marginWidth = 0;
        treeLayout.marginHeight = 0;
        treeComposite.setLayout(treeLayout);

        if (context.isConnectedToCollection()
            || (context.getDefaultRepository() != null
                && context.getDefaultRepository().getWorkspace() != null
                && context.getDefaultRepository().getWorkspace().getLocation() == WorkspaceLocation.LOCAL)) {
            createEmptyLabel(toolkit, treeComposite);
            createTreeViewer(toolkit, treeComposite);

            showOrHideTree(!pendingChangesTree.isEmpty());
        } else {
            createDisconnectedContent(toolkit, treeComposite);
        }

        // Add a resize listener to limit the width of the filter textbox.
        final TeamExplorerResizeListener resizeListener = new TeamExplorerResizeListener(textFilter, 13);
        context.getEvents().addListener(TeamExplorerEvents.FORM_RESIZED, resizeListener);

        treeComposite.addDisposeListener(new DisposeListener() {
            @Override
            public void widgetDisposed(final DisposeEvent e) {
                imageHelper.dispose();
                context.getEvents().removeListener(TeamExplorerEvents.FORM_RESIZED, resizeListener);
            }
        });

        return composite;
    }

    private void createFilterControl(final FormToolkit toolkit, final Composite parent) {
        textFilter = toolkit.createText(parent, getFilterText());
        textFilter.setMessage(Messages.getString("TeamExplorerPendingChangesTreeSection.FilterWatermark")); //$NON-NLS-1$

        GridDataBuilder.newInstance().applyTo(textFilter);

        textFilter.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(final ModifyEvent e) {
                setFilterText(textFilter.getText());
            }
        });
    }

    private void createTreeViewer(final FormToolkit toolkit, final Composite parent) {
        treeViewer = new TreeViewer(treeComposite, SWT.MULTI | SWT.NO_SCROLL);
        AutomationIDHelper.setWidgetID(treeViewer.getTree(), PENDING_CHANGES_TREE_ID);
        treeViewer.setContentProvider(new PendingChangesTreeContentProvider());
        treeViewer.setLabelProvider(new PendingChangesTreeLabelProvider());
        treeViewer.addDoubleClickListener(new PendingChangesTreeDoubleClickListener());
        treeViewer.addTreeListener(new SectionTreeViewerListener());
        treeViewer.setInput(pendingChangesTree);
        restoreState();

        addPendingChangesChangedListener(pendingChangesListener);
        addPendingChangesCountChangedListener(pendingChangesCountListener);

        treeViewer.getTree().addDisposeListener(new DisposeListener() {
            @Override
            public void widgetDisposed(final DisposeEvent e) {
                removePendingChangesChangedListener(pendingChangesListener);
                removePendingChangesCountChangedListener(pendingChangesCountListener);
            }
        });

        GridDataBuilder.newInstance().hAlignFill().hGrab().applyTo(treeViewer.getTree());
        registerContextMenu(getContext(), treeViewer.getTree(), treeViewer);
    }

    private void createEmptyLabel(final FormToolkit toolkit, final Composite parent) {
        final String text = Messages.getString("TeamExplorerPendingChangesTreeSection.ThereAreNoPendingChanges"); //$NON-NLS-1$
        emptyLabel = toolkit.createLabel(treeComposite, text);
        GridDataBuilder.newInstance().hAlignFill().hGrab().applyTo(emptyLabel);
    }

    protected Menu createFilterMenu(final Shell shell) {
        final Menu menu = new Menu(shell, SWT.POP_UP);

        final MenuItem filterHideItem = new MenuItem(menu, SWT.CHECK);
        filterHideItem.setText(Messages.getString("TeamExplorerPendingChangesTreeSection.FilterHide")); //$NON-NLS-1$

        final MenuItem filterShowItem = new MenuItem(menu, SWT.CHECK);
        filterShowItem.setText(Messages.getString("TeamExplorerPendingChangesTreeSection.FilterShow")); //$NON-NLS-1$

        filterHideItem.setSelection(!getFilterEnabled());
        filterHideItem.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
                filterHideItem.setSelection(true);
                filterShowItem.setSelection(false);

                clearFilterTextbox();
                setFilterVisibility(false);
                TeamExplorerHelpers.relayoutContainingScrolledComposite(treeComposite);
            }
        });

        filterShowItem.setSelection(getFilterEnabled());
        filterShowItem.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
                filterHideItem.setSelection(false);
                filterShowItem.setSelection(true);

                clearFilterTextbox();
                setFilterVisibility(true);
                TeamExplorerHelpers.relayoutContainingScrolledComposite(treeComposite);
            }
        });

        return menu;
    }

    protected void clearFilterTextbox() {
        if (textFilter.getText().length() > 0) {
            textFilter.setText(""); //$NON-NLS-1$
        }
    }

    private void setFilterVisibility(final boolean visible) {
        setFilterEnabled(visible);
        textFilter.setVisible(visible);

        final GridData gridData = (GridData) textFilter.getLayoutData();
        gridData.exclude = !visible;
    }

    protected class PendingChangesListener implements PendingChangesChangedListener {
        @Override
        public void onPendingChangesChanged(final WorkspaceEvent e) {
            refreshTree();
            TeamExplorerPendingChangesTreeSection.this.onPendingChangesChanged();
            CodeMarkerDispatch.dispatch(AFTER_TREE_REFRESH);
        }
    }

    protected class PendingChangesCountListener implements PendingChangesCountChangedListener {
        @Override
        public void onPendingChangesCountChanged(final WorkspaceEvent e) {
            refreshTitle();
        }
    }
}
