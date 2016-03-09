// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.teamexplorer.sections;

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
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.widgets.FormToolkit;

import com.microsoft.tfs.client.common.server.TFSServer;
import com.microsoft.tfs.client.common.ui.TFSCommonUIClientPlugin;
import com.microsoft.tfs.client.common.ui.framework.helper.SWTUtil;
import com.microsoft.tfs.client.common.ui.framework.image.ImageHelper;
import com.microsoft.tfs.client.common.ui.framework.layout.GridDataBuilder;
import com.microsoft.tfs.client.common.ui.framework.tree.TreeContentProvider;
import com.microsoft.tfs.client.common.ui.teamexplorer.TeamExplorerContext;
import com.microsoft.tfs.client.common.ui.teamexplorer.helpers.WSSHelper;
import com.microsoft.tfs.core.clients.commonstructure.ProjectInfo;
import com.microsoft.tfs.core.clients.sharepoint.WSSClient;
import com.microsoft.tfs.core.clients.sharepoint.WSSDocument;
import com.microsoft.tfs.core.clients.sharepoint.WSSDocumentLibrary;
import com.microsoft.tfs.core.clients.sharepoint.WSSFolder;
import com.microsoft.tfs.core.clients.sharepoint.WSSNode;
import com.microsoft.tfs.core.clients.sharepoint.WSSUtils;
import com.microsoft.tfs.util.Check;

public class TeamExplorerDocumentsSection extends TeamExplorerBaseSection {
    ImageHelper imageHelper = new ImageHelper(TFSCommonUIClientPlugin.PLUGIN_ID);
    Image libraryIcon = imageHelper.getImage("images/common/document_library.gif"); //$NON-NLS-1$

    private TreeViewer treeViewer;
    private volatile WSSDocumentLibrary[] documentLibraries;

    @Override
    public boolean isVisible(final TeamExplorerContext context) {
        return context.isConnected()
            && WSSUtils.isWSSConfigured(context.getServer().getConnection(), context.getCurrentProjectInfo());
    }

    @Override
    public boolean initializeInBackground(final TeamExplorerContext context) {
        return true;
    }

    @Override
    public void initialize(final IProgressMonitor monitor, final TeamExplorerContext context) {
        final WSSClient wssClient = context.getServer().getConnection().getWSSClient(context.getCurrentProjectInfo());
        documentLibraries = wssClient.getDocumentLibraries(false);
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
            treeViewer.setContentProvider(new DocumentContentProvider());
            treeViewer.setLabelProvider(new DocumentLabelProvider());
            treeViewer.addDoubleClickListener(new DocumentDoubleClickListener(context));
            treeViewer.addTreeListener(new SectionTreeViewerListener());
            treeViewer.setInput(documentLibraries);
            GridDataBuilder.newInstance().align(SWT.FILL, SWT.FILL).grab(true, true).applyTo(treeViewer.getControl());

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

    private class DocumentContentProvider extends TreeContentProvider {
        @Override
        public Object[] getElements(final Object inputElement) {
            Check.isTrue(inputElement instanceof WSSDocumentLibrary[], "inputElement instanceof WSSDocumentLibrary[]"); //$NON-NLS-1$
            return (WSSDocumentLibrary[]) inputElement;
        }

        @Override
        public Object[] getChildren(final Object parentElement) {
            if (parentElement instanceof WSSDocumentLibrary) {
                return ((WSSDocumentLibrary) parentElement).getChildren();
            } else if (parentElement instanceof WSSFolder) {
                return ((WSSFolder) parentElement).getChildren();
            } else {
                throw new IllegalArgumentException("parentElement instanceof WSS item"); //$NON-NLS-1$
            }
        }

        @Override
        public boolean hasChildren(final Object element) {
            if (element instanceof WSSDocumentLibrary) {
                return ((WSSDocumentLibrary) element).hasChildren();
            } else if (element instanceof WSSFolder) {
                return ((WSSFolder) element).hasChildren();
            } else if (element instanceof WSSDocument) {
                return false;
            } else {
                throw new IllegalArgumentException("element instanceof WSS item"); //$NON-NLS-1$
            }
        }
    }

    private class DocumentLabelProvider extends LabelProvider {
        @Override
        public String getText(final Object element) {
            if (element instanceof WSSDocumentLibrary) {
                return ((WSSDocumentLibrary) element).getLabel();
            } else if (element instanceof WSSFolder) {
                return ((WSSFolder) element).getLabel();
            } else if (element instanceof WSSDocument) {
                return ((WSSDocument) element).getLabel();
            } else {
                throw new IllegalArgumentException("element instanceof WSS item"); //$NON-NLS-1$
            }
        }

        @Override
        public Image getImage(final Object element) {
            if (element instanceof WSSDocumentLibrary) {
                return libraryIcon;
            } else if (element instanceof WSSFolder) {
                return PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJ_FOLDER);
            } else if (element instanceof WSSDocument) {
                return PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJ_FILE);
            } else {
                throw new IllegalArgumentException("element instanceof WSS item"); //$NON-NLS-1$
            }
        }
    }

    private class DocumentDoubleClickListener implements IDoubleClickListener {
        private final TeamExplorerContext context;

        public DocumentDoubleClickListener(final TeamExplorerContext context) {
            this.context = context;
        }

        @Override
        public void doubleClick(final DoubleClickEvent event) {
            final IStructuredSelection selection = (IStructuredSelection) event.getSelection();
            final Object element = selection.getFirstElement();

            final Shell shell = treeViewer.getControl().getShell();
            final TFSServer server = context.getServer();
            final ProjectInfo projectInfo = context.getCurrentProjectInfo();

            if (element instanceof WSSNode) {
                final WSSNode wssNode = (WSSNode) element;
                WSSHelper.openWSSNode(shell, server, projectInfo, wssNode);
            } else if (element instanceof WSSDocumentLibrary) {
                final WSSDocumentLibrary library = (WSSDocumentLibrary) element;
                WSSHelper.openWSSDocumentLibrary(shell, server, projectInfo, library);
            }
        }
    }
}
