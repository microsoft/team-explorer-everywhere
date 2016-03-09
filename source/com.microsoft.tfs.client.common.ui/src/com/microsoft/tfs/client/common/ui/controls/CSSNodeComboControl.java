// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.controls;

import java.util.ArrayList;

import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeViewerListener;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeExpansionEvent;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;

import com.microsoft.tfs.client.common.ui.controls.generic.AbstractCombo;
import com.microsoft.tfs.client.common.ui.framework.sizing.ControlSize;
import com.microsoft.tfs.client.common.ui.framework.tree.TreeContentProvider;
import com.microsoft.tfs.core.clients.commonstructure.CSSNode;
import com.microsoft.tfs.util.Check;

public class CSSNodeComboControl extends AbstractCombo {

    private static final String COLLAPSE_EVENT_KEY = "collapse-event"; //$NON-NLS-1$

    private final CSSNode rootNode;
    private CSSNode selectedNode;
    private final CSSNode skipNode;

    public CSSNodeComboControl(
        final Composite parent,
        final int style,
        final CSSNode rootNode,
        final CSSNode initialNode,
        final CSSNode skipNode) {
        super(parent, style);
        this.rootNode = rootNode;
        selectedNode = initialNode;
        this.skipNode = skipNode;
        setPackPopup(false);
        text.setText(initialNode.getPath());
        text.selectAll();
    }

    public CSSNode getSelectedNode() {
        final CSSNode selectedNode = CSSNode.resolveNode(rootNode, text.getText().trim());
        if (!selectedNode.getPath().equals(text.getText().trim())) {
            // We have typed a node that does not exist - return null;
            return null;
        }
        return selectedNode;
    }

    @Override
    public Composite getPopup(final Composite parent) {
        final Composite composite = new Composite(parent, SWT.NONE);
        final GridLayout layout = new GridLayout(1, true);
        layout.marginHeight = 0;
        layout.marginWidth = 0;
        composite.setLayout(layout);

        final CSSNodeControl control =
            new CSSNodeControl(composite, SWT.NONE, rootNode, text.getText().trim(), skipNode);

        control.addSelectionChangedListener(new ISelectionChangedListener() {
            @Override
            public void selectionChanged(final SelectionChangedEvent event) {
                selectedNode = control.getSelectedNode();
                text.setText(selectedNode.getPath());

                final Object collapseEventFlag = ((Viewer) event.getSelectionProvider()).getData(COLLAPSE_EVENT_KEY);
                if (collapseEventFlag != null) {
                    /*
                     * this selection event is happening because of a node
                     * collapse - do not auto-close the popup and reset the
                     * collapse flag
                     */
                    ((Viewer) event.getSelectionProvider()).setData(COLLAPSE_EVENT_KEY, null);
                } else {
                    closePopup();
                }
            }
        });

        control.addTreeListener(new ITreeViewerListener() {
            @Override
            public void treeCollapsed(final TreeExpansionEvent event) {
                /*
                 * collapsing a tree node generates a selection event - we don't
                 * want this selection event to trigger a auto-close of the
                 * popup as all other selection events would
                 *
                 * we record that a tree collapse happened and then check for
                 * this case in the selection listener
                 */
                event.getTreeViewer().setData(COLLAPSE_EVENT_KEY, Boolean.TRUE);
            }

            @Override
            public void treeExpanded(final TreeExpansionEvent event) {
            }
        });

        control.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));
        ControlSize.setCharHeightHint(control, 15);

        return composite;
    }

    private static class CSSNodeControl extends Composite {
        private final TreeViewer treeViewer;

        private final CSSNode rootNode;
        private CSSNode selectedNode;
        private final CSSNode skipNode;

        public CSSNodeControl(
            final Composite parent,
            final int style,
            final CSSNode rootNode,
            final String initialPath,
            final CSSNode skipNode) {
            super(parent, style);
            this.rootNode = rootNode;
            selectedNode = CSSNode.resolveNode(rootNode, initialPath);
            this.skipNode = skipNode;

            final FillLayout layout = new FillLayout();
            setLayout(layout);

            treeViewer = new TreeViewer(this, SWT.NONE);
            treeViewer.setUseHashlookup(true);
            treeViewer.setContentProvider(new ContentProvider(this.skipNode));
            treeViewer.setLabelProvider(new LabelProvider());

            treeViewer.addSelectionChangedListener(new ISelectionChangedListener() {
                @Override
                public void selectionChanged(final SelectionChangedEvent event) {
                    selectedNode = (CSSNode) ((IStructuredSelection) event.getSelection()).getFirstElement();
                }
            });

            final CSSNode dummyRoot = new CSSNode(this.rootNode.getStructureType(), ""); //$NON-NLS-1$
            dummyRoot.addChild(this.rootNode);
            treeViewer.setInput(dummyRoot);

            treeViewer.setSelection(new StructuredSelection(selectedNode), true);

            final Object[] currentExpandedElements = treeViewer.getExpandedElements();
            final Object[] newExpandedElements = new Object[currentExpandedElements.length + 1];
            System.arraycopy(currentExpandedElements, 0, newExpandedElements, 0, currentExpandedElements.length);
            newExpandedElements[newExpandedElements.length - 1] = selectedNode;

            treeViewer.setExpandedElements(newExpandedElements);
        }

        public void addSelectionChangedListener(final ISelectionChangedListener listener) {
            treeViewer.addSelectionChangedListener(listener);
        }

        public void addTreeListener(final ITreeViewerListener listener) {
            treeViewer.addTreeListener(listener);
        }

        public CSSNode getSelectedNode() {
            return selectedNode;
        }

    }

    private static class ContentProvider extends TreeContentProvider {
        private final CSSNode skipNode;

        public ContentProvider(final CSSNode skipNode) {
            super();
            Check.notNull(skipNode, "skipNode"); //$NON-NLS-1$
            this.skipNode = skipNode;
        }

        @Override
        public Object[] getChildren(final Object parentElement) {
            final CSSNode parent = (CSSNode) parentElement;
            final CSSNode[] children = (CSSNode[]) parent.getChildren();

            final ArrayList filteredChildren = new ArrayList();
            for (int i = 0; i < children.length; i++) {
                if (!children[i].getURI().equals(skipNode.getURI())) {
                    filteredChildren.add(children[i]);
                }
            }

            return filteredChildren.toArray();
        }

        @Override
        public boolean hasChildren(final Object element) {
            final CSSNode node = (CSSNode) element;
            return node.hasChildren();
        }

        @Override
        public Object[] getElements(final Object inputElement) {
            return getChildren(inputElement);
        }

        @Override
        public Object getParent(final Object element) {
            final CSSNode node = (CSSNode) element;
            return node.getParent();
        }
    }

}
