// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.teamexplorer.pages;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Hyperlink;
import org.eclipse.ui.forms.widgets.ImageHyperlink;

import com.microsoft.tfs.client.common.codemarker.CodeMarker;
import com.microsoft.tfs.client.common.codemarker.CodeMarkerDispatch;
import com.microsoft.tfs.client.common.commands.wit.RefreshStoredQueriesCommand;
import com.microsoft.tfs.client.common.server.TFSServer;
import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.framework.command.UICommandExecutorFactory;
import com.microsoft.tfs.client.common.ui.framework.helper.SWTUtil;
import com.microsoft.tfs.client.common.ui.framework.layout.GridDataBuilder;
import com.microsoft.tfs.client.common.ui.helpers.WorkItemEditorHelper;
import com.microsoft.tfs.client.common.ui.teamexplorer.TeamExplorerContext;
import com.microsoft.tfs.client.common.ui.teamexplorer.helpers.PageHelpers;
import com.microsoft.tfs.client.common.ui.teamexplorer.helpers.WorkItemHelpers;
import com.microsoft.tfs.core.clients.workitem.WorkItem;
import com.microsoft.tfs.core.clients.workitem.WorkItemClient;
import com.microsoft.tfs.core.clients.workitem.project.Project;
import com.microsoft.tfs.core.clients.workitem.project.ProjectCollection;
import com.microsoft.tfs.core.clients.workitem.wittype.WorkItemType;
import com.microsoft.tfs.util.Check;

public class TeamExplorerWorkItemPage extends TeamExplorerBasePage {
    private static final Log log = LogFactory.getLog(TeamExplorerWorkItemPage.class);

    public static final CodeMarker WORKITEMS_PAGE_LOADED = new CodeMarker(
        "com.microsoft.tfs.client.common.ui.teamexplorer.pages.TeamExplorerWorkItemPage#workitemsPageLoaded"); //$NON-NLS-1$

    @Override
    public void refresh(final IProgressMonitor monitor, final TeamExplorerContext context) {
        Check.notNull(context, "context"); //$NON-NLS-1$

        final TFSServer server = context.getServer();
        if (server == null) {
            return;
        }

        final ProjectCollection projects = server.getConnection().getWorkItemClient().getProjects();
        final Shell shell = context.getWorkbenchPart().getSite().getShell();

        final RefreshStoredQueriesCommand refreshCommand = new RefreshStoredQueriesCommand(server, projects);
        final IStatus status = UICommandExecutorFactory.newUIJobCommandExecutor(shell).execute(refreshCommand);

        if (!status.isOK()) {
            log.error("Failed to refresh work item page: " + status.getMessage(), status.getException()); //$NON-NLS-1$
        }
    }

    @Override
    public Composite getPageContent(
        final FormToolkit toolkit,
        final Composite parent,
        final int style,
        final TeamExplorerContext context) {
        final Composite composite = toolkit.createComposite(parent);

        // Form-style border painting not enabled (0 pixel margins OK) because
        // no applicable controls in this composite
        SWTUtil.gridLayout(composite, 3, false, 0, 5);

        // Create the new work item hyper-link
        final String linkText = Messages.getString("TeamExplorerWorkItemPage.NewWorkItemLinkText"); //$NON-NLS-1$
        final Menu menu = createNewWorkItemMenu(composite.getShell(), context);
        final ImageHyperlink link = PageHelpers.createDropHyperlink(toolkit, composite, linkText, menu);

        GridDataBuilder.newInstance().applyTo(link);

        final Label separator = toolkit.createLabel(composite, "|", SWT.VERTICAL); //$NON-NLS-1$
        GridDataBuilder.newInstance().vFill().applyTo(separator);

        // Create the new query hyper-link.
        final String title = Messages.getString("TeamExplorerWorkItemsQueriesSection.NewQueryLinkText"); //$NON-NLS-1$
        final Hyperlink newQueryHyperlink = toolkit.createHyperlink(composite, title, SWT.WRAP);
        newQueryHyperlink.setUnderlined(false);
        newQueryHyperlink.addHyperlinkListener(new HyperlinkAdapter() {
            @Override
            public void linkActivated(final HyperlinkEvent e) {
                WorkItemHelpers.openNewQuery(context);
            }
        });

        GridDataBuilder.newInstance().applyTo(newQueryHyperlink);

        CodeMarkerDispatch.dispatch(WORKITEMS_PAGE_LOADED);
        return composite;
    }

    private Menu createNewWorkItemMenu(final Shell shell, final TeamExplorerContext context) {
        final WorkItemClient client = context.getServer().getConnection().getWorkItemClient();
        final Project project = client.getProjects().get(context.getCurrentProjectInfo().getName());
        final Menu menu = new Menu(shell, SWT.POP_UP);

        for (final WorkItemType workItemType : project.getVisibleWorkItemTypes()) {
            final MenuItem menuItem = new MenuItem(menu, SWT.PUSH);
            menuItem.setText(workItemType.getName());
            menuItem.setData(workItemType);
            menuItem.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(final SelectionEvent e) {
                    final WorkItem workItem = client.newWorkItem(workItemType);
                    WorkItemEditorHelper.openEditor(context.getServer(), workItem);
                }
            });
        }

        return menu;
    }
}
