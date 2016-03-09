// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.teamexplorer.sections;

import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.widgets.FormToolkit;

import com.microsoft.tfs.client.common.ui.TFSCommonUIClientPlugin;
import com.microsoft.tfs.client.common.ui.framework.helper.SWTUtil;
import com.microsoft.tfs.client.common.ui.framework.image.ImageHelper;
import com.microsoft.tfs.client.common.ui.framework.layout.GridDataBuilder;
import com.microsoft.tfs.client.common.ui.framework.tree.TreeContentProvider;
import com.microsoft.tfs.client.common.ui.teamexplorer.TeamExplorerContext;
import com.microsoft.tfs.client.common.ui.teamexplorer.helpers.ReportsHelper;
import com.microsoft.tfs.core.clients.reporting.ReportNode;
import com.microsoft.tfs.core.clients.reporting.ReportNodeType;
import com.microsoft.tfs.core.clients.reporting.ReportUtils;
import com.microsoft.tfs.core.clients.reporting.ReportingClient;
import com.microsoft.tfs.util.Check;

public class TeamExplorerReportsSection extends TeamExplorerBaseSection {
    private final ImageHelper imageHelper = new ImageHelper(TFSCommonUIClientPlugin.PLUGIN_ID);
    private final Image closedIcon = imageHelper.getImage("images/reports/reports_closed.gif"); //$NON-NLS-1$
    private final Image reportIcon = imageHelper.getImage("images/reports/report.gif"); //$NON-NLS-1$

    private TreeViewer treeViewer;
    private volatile ReportNode[] reports;

    @Override
    public boolean isVisible(final TeamExplorerContext context) {
        return context.isConnected() && ReportUtils.isReportingConfigured(context.getServer().getConnection());
    }

    @Override
    public boolean initializeInBackground(final TeamExplorerContext context) {
        return true;
    }

    @Override
    public void initialize(final IProgressMonitor monitor, final TeamExplorerContext context) {
        final ReportingClient client =
            (ReportingClient) context.getServer().getConnection().getClient(ReportingClient.class);
        final List<ReportNode> list = client.getReports(context.getCurrentProjectInfo(), false);
        reports = list.toArray(new ReportNode[list.size()]);
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
        SWTUtil.gridLayout(composite, 1, true, 0, 5);

        if (context.isConnected()) {
            treeViewer = new TreeViewer(composite, SWT.MULTI | SWT.NO_SCROLL);
            treeViewer.setContentProvider(new ReportContentProvider());
            treeViewer.setLabelProvider(new ReportLabelProvider());
            treeViewer.addDoubleClickListener(new ReportDoubleClickListener(context));
            treeViewer.addTreeListener(new SectionTreeViewerListener());
            GridDataBuilder.newInstance().align(SWT.FILL, SWT.FILL).grab(true, true).applyTo(treeViewer.getControl());
            treeViewer.setInput(reports);

            registerContextMenu(context, treeViewer.getControl(), treeViewer);
        } else {
            createDisconnectedContent(toolkit, composite);
        }

        composite.addDisposeListener(new DisposeListener() {
            @Override
            public void widgetDisposed(final DisposeEvent e) {
                imageHelper.dispose();
            }
        });

        return composite;
    }

    private class ReportContentProvider extends TreeContentProvider {
        @Override
        public Object[] getElements(final Object inputElement) {
            Check.isTrue(inputElement instanceof ReportNode[], "inputElement instanceof ReportNode[]"); //$NON-NLS-1$
            return (ReportNode[]) inputElement;
        }

        @Override
        public Object[] getChildren(final Object parentElement) {
            Check.isTrue(parentElement instanceof ReportNode, "parentElement instanceof ReportNode"); //$NON-NLS-1$
            return ((ReportNode) parentElement).getChildren();
        }

        @Override
        public boolean hasChildren(final Object element) {
            Check.isTrue(element instanceof ReportNode, "element instanceof ReportNode"); //$NON-NLS-1$
            return ((ReportNode) element).hasChildren();
        }
    }

    private class ReportLabelProvider extends LabelProvider {
        @Override
        public String getText(final Object element) {
            Check.isTrue(element instanceof ReportNode, "element instanceof ReportNode"); //$NON-NLS-1$
            return ((ReportNode) element).getLabel();
        }

        @Override
        public Image getImage(final Object element) {
            Check.isTrue(element instanceof ReportNode, "element instanceof TeamExplorerNode"); //$NON-NLS-1$
            return ((ReportNode) element).getType().equals(ReportNodeType.FOLDER) ? closedIcon : reportIcon;
        }
    }

    private class ReportDoubleClickListener implements IDoubleClickListener {
        private final TeamExplorerContext context;

        public ReportDoubleClickListener(final TeamExplorerContext context) {
            this.context = context;
        }

        @Override
        public void doubleClick(final DoubleClickEvent event) {
            final IStructuredSelection selection = (IStructuredSelection) event.getSelection();
            final Object element = selection.getFirstElement();

            if (element instanceof ReportNode) {
                final ReportNode reportNode = (ReportNode) element;
                ReportsHelper.openReport(event.getViewer().getControl().getShell(), context.getServer(), reportNode);
            }
        }
    }
}
