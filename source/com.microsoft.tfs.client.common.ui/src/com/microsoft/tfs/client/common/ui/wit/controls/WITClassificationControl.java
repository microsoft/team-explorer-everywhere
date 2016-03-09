// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.wit.controls;

import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeViewerListener;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;

import com.microsoft.tfs.client.common.ui.framework.tree.TreeContentProvider;
import com.microsoft.tfs.client.common.ui.framework.tree.TreeViewerDoubleClickListener;
import com.microsoft.tfs.core.clients.workitem.node.Node;
import com.microsoft.tfs.core.clients.workitem.project.Project;

public class WITClassificationControl extends Composite {
    private final Project project;
    private final Node.TreeType treeType;

    private final TreeViewer treeViewer;
    private Object selectedObject;

    public WITClassificationControl(
        final Composite parent,
        final int style,
        final Project project,
        final Node.TreeType treeType,
        final String initialPath) {
        super(parent, style);
        this.project = project;
        this.treeType = treeType;

        final FillLayout layout = new FillLayout();
        setLayout(layout);

        treeViewer = new TreeViewer(this, SWT.NONE);
        treeViewer.setContentProvider(new WITClassificationControlContentProvider());
        treeViewer.setLabelProvider(new WITClassificationControlLabelProvider());
        treeViewer.addDoubleClickListener(new TreeViewerDoubleClickListener(treeViewer));
        treeViewer.addSelectionChangedListener(new ISelectionChangedListener() {
            @Override
            public void selectionChanged(final SelectionChangedEvent event) {
                selectedObject = ((IStructuredSelection) event.getSelection()).getFirstElement();
            }
        });

        treeViewer.setInput(new Object());

        Object initialSelection = null;

        if (initialPath != null && initialPath.trim().length() > 0) {
            initialSelection = project.resolvePath(initialPath.trim(), treeType);
        }

        if (initialSelection == null) {
            initialSelection = project;
        }

        treeViewer.setSelection(new StructuredSelection(initialSelection), true);
        selectedObject = initialSelection;

        final Object[] currentExpandedElements = treeViewer.getExpandedElements();
        final Object[] newExpandedElements = new Object[currentExpandedElements.length + 1];
        System.arraycopy(currentExpandedElements, 0, newExpandedElements, 0, currentExpandedElements.length);
        newExpandedElements[newExpandedElements.length - 1] = selectedObject;

        treeViewer.setExpandedElements(newExpandedElements);
    }

    public void addSelectionChangedListener(final ISelectionChangedListener listener) {
        treeViewer.addSelectionChangedListener(listener);
    }

    public void addTreeListener(final ITreeViewerListener listener) {
        treeViewer.addTreeListener(listener);
    }

    public String getSelectedPath() {
        if (selectedObject instanceof Project) {
            return ((Project) selectedObject).getName();
        } else {
            return ((Node) selectedObject).getPath();
        }
    }

    private class WITClassificationControlContentProvider extends TreeContentProvider {
        @Override
        public Object[] getChildren(final Object parentElement) {
            if (parentElement instanceof Project) {
                if (treeType == Node.TreeType.AREA) {
                    return ((Project) parentElement).getAreaRootNodes().getNodes();
                } else {
                    return ((Project) parentElement).getIterationRootNodes().getNodes();
                }
            }

            if (parentElement instanceof Node) {
                return ((Node) parentElement).getChildNodes().getNodes();
            }

            return null;
        }

        @Override
        public boolean hasChildren(final Object element) {
            if (element instanceof Project) {
                if (treeType == Node.TreeType.AREA) {
                    return ((Project) element).getAreaRootNodes().size() > 0;
                } else {
                    return ((Project) element).getIterationRootNodes().size() > 0;
                }
            }

            if (element instanceof Node) {
                return ((Node) element).getChildNodes().size() > 0;
            }

            return false;
        }

        @Override
        public Object[] getElements(final Object inputElement) {
            return new Object[] {
                project
            };
        }

        @Override
        public Object getParent(final Object element) {
            if (element instanceof Node) {
                if (treeType == Node.TreeType.AREA) {
                    if (project.getAreaRootNodes().contains((Node) element)) {
                        return project;
                    }
                } else {
                    if (project.getIterationRootNodes().contains((Node) element)) {
                        return project;
                    }
                }

                return ((Node) element).getParent();
            }

            return null;
        }
    }

    private class WITClassificationControlLabelProvider extends LabelProvider {
        @Override
        public String getText(final Object element) {
            if (element instanceof Project) {
                return ((Project) element).getName();
            }
            if (element instanceof Node) {
                return ((Node) element).getName();
            }
            return null;
        }
    }
}
