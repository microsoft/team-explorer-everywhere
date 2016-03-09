// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.controls.vc;

import java.util.List;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IBaseLabelProvider;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.IPostSelectionProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;

import com.microsoft.tfs.client.common.codemarker.CodeMarker;
import com.microsoft.tfs.client.common.codemarker.CodeMarkerDispatch;
import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.framework.action.StandardActionConstants;
import com.microsoft.tfs.client.common.ui.framework.sizing.ControlSize;
import com.microsoft.tfs.client.common.ui.framework.tree.TreeContentProvider;
import com.microsoft.tfs.client.common.ui.framework.validation.ActionValidatorBinding;
import com.microsoft.tfs.client.common.ui.framework.validation.NumericConstraint;
import com.microsoft.tfs.client.common.ui.framework.validation.SelectionProviderValidator;
import com.microsoft.tfs.client.common.ui.helpers.AutomationIDHelper;
import com.microsoft.tfs.client.common.ui.vc.serveritem.ServerItemLabelProvider;
import com.microsoft.tfs.client.common.ui.vc.serveritem.ServerItemSource;
import com.microsoft.tfs.client.common.ui.vc.serveritem.ServerItemType;
import com.microsoft.tfs.client.common.ui.vc.serveritem.TypedServerItem;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.valid.Validator;

public class ServerItemTreeControl extends Composite implements IPostSelectionProvider, ServerItemControl {
    /**
     * SWT table styles that will always be used.
     */
    private static int TREE_STYLES = SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL;

    /**
     * SWT table styles that will only be used if the client passes them in.
     */
    private static int OPTIONAL_TREE_STYLES = SWT.MULTI | SWT.SINGLE;

    public static final String TREE_VIEWER_ID = "ServerItemTreeControl.viewer"; //$NON-NLS-1$

    public static final CodeMarker CODEMARKER_CHILD_NODES_FETCH_START =
        new CodeMarker("com.microsoft.tfs.client.common.ui.controls.vc.ServerItemTreeControl#childNodesFetchStart"); //$NON-NLS-1$
    public static final CodeMarker CODEMARKER_CHILD_NODES_FETCH_COMPLETE =
        new CodeMarker("com.microsoft.tfs.client.common.ui.controls.vc.ServerItemTreeControl#childNodesFetchComplete"); //$NON-NLS-1$

    private final TreeViewer viewer;
    private final MenuManager contextMenu;

    private IAction refreshAction;

    private TypedServerItem[] selectedItems;

    private ServerItemSource serverItemSource;

    public ServerItemTreeControl(final Composite parent, final int style) {
        super(parent, style);

        final FillLayout layout = new FillLayout();
        setLayout(layout);

        viewer = new TreeViewer(this, TREE_STYLES | (style & OPTIONAL_TREE_STYLES));
        AutomationIDHelper.setWidgetID(viewer.getTree(), TREE_VIEWER_ID);

        viewer.setUseHashlookup(true);
        viewer.setContentProvider(new ContentProvider());
        viewer.setLabelProvider(new ServerItemLabelProvider());
        viewer.addFilter(new Filter());
        viewer.setSorter(new ViewerSorter() {
            @Override
            public int compare(final Viewer viewer, final Object e1, final Object e2) {
                final TypedServerItem node1 = (TypedServerItem) e1;
                final TypedServerItem node2 = (TypedServerItem) e2;
                return node1.getName().compareToIgnoreCase(node2.getName());
            }
        });

        contextMenu = createContextMenu();
        createActions();
        contributeActions();

        viewer.addSelectionChangedListener(new ISelectionChangedListener() {
            @Override
            public void selectionChanged(final SelectionChangedEvent event) {
                final IStructuredSelection selection = (IStructuredSelection) event.getSelection();
                if (selection.size() == 0) {
                    selectedItems = null;
                } else {
                    @SuppressWarnings("unchecked")
                    final List<TypedServerItem> l = selection.toList();
                    selectedItems = l.toArray(new TypedServerItem[l.size()]);
                }
            }
        });

        viewer.addDoubleClickListener(new IDoubleClickListener() {
            @Override
            public void doubleClick(final DoubleClickEvent event) {
                final TypedServerItem doubleClickedElement =
                    (TypedServerItem) ((IStructuredSelection) event.getSelection()).getFirstElement();

                if (ServerItemType.isFolder(doubleClickedElement.getType())) {
                    final boolean expandedState = viewer.getExpandedState(doubleClickedElement);
                    viewer.setExpandedState(doubleClickedElement, !expandedState);
                }
            }
        });
    }

    public MenuManager getContextMenu() {
        return contextMenu;
    }

    public void setServerItemSource(final ServerItemSource serverItemSource) {
        this.serverItemSource = serverItemSource;

        ((ServerItemLabelProvider) viewer.getLabelProvider()).setServerItemSource(serverItemSource);
        viewer.setInput(serverItemSource);

        if (serverItemSource != null) {
            viewer.setExpandedState(TypedServerItem.ROOT, true);
        }
    }

    public void setLabelProvider(final IBaseLabelProvider labelProvider) {
        viewer.setLabelProvider(labelProvider);
    }

    public void setVisibleServerItemTypes(final ServerItemType[] visibleTypes) {
        Check.notNull(visibleTypes, "visibleTypes"); //$NON-NLS-1$

        final ViewerFilter[] filters = viewer.getFilters();
        for (int i = 0; i < filters.length; i++) {
            if (filters[i] instanceof Filter) {
                ((Filter) filters[i]).setVisibleTypes(visibleTypes);
                break;
            }
        }
    }

    private int getVisibleTypeFlags() {
        final ViewerFilter[] filters = viewer.getFilters();
        for (int i = 0; i < filters.length; i++) {
            if (filters[i] instanceof Filter) {
                return ((Filter) filters[i]).getVisibleTypeFlags();
            }
        }

        return -1;
    }

    public TypedServerItem[] getSelectedItems() {
        if (selectedItems == null) {
            return new TypedServerItem[0];
        } else {
            return selectedItems.clone();
        }
    }

    @Override
    public TypedServerItem getSelectedItem() {
        if (selectedItems == null) {
            return null;
        }

        return selectedItems[0];
    }

    @Override
    public void setSelectedItem(final TypedServerItem serverItem) {
        if (serverItem == null) {
            setSelection(StructuredSelection.EMPTY);
        } else {
            setSelection(new StructuredSelection(serverItem));
        }
    }

    public void setSelectedItems(final TypedServerItem[] serverItems) {
        if (serverItems == null) {
            setSelection(StructuredSelection.EMPTY);
        } else {
            setSelection(new StructuredSelection(serverItems), true);
        }
    }

    public void addDoubleClickListener(final IDoubleClickListener listener) {
        viewer.addDoubleClickListener(listener);
    }

    public void removeDoubleClickListener(final IDoubleClickListener listener) {
        viewer.removeDoubleClickListener(listener);
    }

    @Override
    public void addPostSelectionChangedListener(final ISelectionChangedListener listener) {
        viewer.addPostSelectionChangedListener(listener);
    }

    @Override
    public void removePostSelectionChangedListener(final ISelectionChangedListener listener) {
        viewer.removePostSelectionChangedListener(listener);
    }

    @Override
    public void addSelectionChangedListener(final ISelectionChangedListener listener) {
        viewer.addSelectionChangedListener(listener);
    }

    @Override
    public void removeSelectionChangedListener(final ISelectionChangedListener listener) {
        viewer.removeSelectionChangedListener(listener);
    }

    @Override
    public ISelection getSelection() {
        return viewer.getSelection();
    }

    @Override
    public void setSelection(final ISelection selection) {
        viewer.setSelection(selection);
    }

    public void setSelection(final ISelection selection, final boolean reveal) {
        viewer.setSelection(selection, reveal);
    }

    public void expandItem(final TypedServerItem item, final int level) {
        viewer.expandToLevel(item, level);
    }

    public Validator getSelectionValidator() {
        return new SelectionProviderValidator(
            this,
            NumericConstraint.ONE_OR_MORE,
            Messages.getString("ServerItemTreeControl.YouMustSelectAtLeastOneServerItem")); //$NON-NLS-1$
    }

    public Validator getSingleSelectionValidator() {
        return new SelectionProviderValidator(
            this,
            NumericConstraint.EXACTLY_ONE,
            Messages.getString("ServerItemTreeControl.YouMustSelectExactlyOneServerItem")); //$NON-NLS-1$
    }

    @Override
    public boolean setFocus() {
        return viewer.getControl().setFocus();
    }

    @Override
    public Point computeSize(final int wHint, final int hHint, final boolean changed) {
        return ControlSize.computeCharSize(wHint, hHint, viewer.getControl(), 60, 10);
    }

    private MenuManager createContextMenu() {
        final MenuManager menuManager = new MenuManager("#popup"); //$NON-NLS-1$
        menuManager.setRemoveAllWhenShown(true);
        viewer.getControl().setMenu(menuManager.createContextMenu(viewer.getControl()));
        return menuManager;
    }

    private void createActions() {
        refreshAction = new Action() {
            @Override
            public void run() {
                final TypedServerItem item = getSelectedItem();
                serverItemSource.clearChildCache(item);
                viewer.refresh(item);
            }
        };

        refreshAction.setText(Messages.getString("ServerItemTreeControl.RefreshActionName")); //$NON-NLS-1$

        new ActionValidatorBinding(refreshAction).bind(getSelectionValidator());
    }

    public void refreshTree() {
        final TypedServerItem item = getSelectedItem();
        serverItemSource.clearChildCache(item);
        viewer.refresh(item);
    }

    private void contributeActions() {
        contextMenu.addMenuListener(new IMenuListener() {
            @Override
            public void menuAboutToShow(final IMenuManager manager) {
                manager.add(new Separator(StandardActionConstants.HOSTING_CONTROL_CONTRIBUTIONS));
                manager.add(new Separator(StandardActionConstants.PRIVATE_CONTRIBUTIONS));

                manager.appendToGroup(StandardActionConstants.PRIVATE_CONTRIBUTIONS, refreshAction);
            }
        });
    }

    private static class Filter extends ViewerFilter {
        private int visibleTypeFlags;

        public Filter() {
            setVisibleTypes(ServerItemType.ALL);
        }

        public void setVisibleTypes(final ServerItemType[] visibleTypes) {
            /*
             * Bugfix: parent types must always be visible for children to be
             * visible.
             */

            visibleTypeFlags = 0;
            for (int i = 0; i < visibleTypes.length; i++) {
                visibleTypeFlags |= getVisibleParentTypes(visibleTypes[i]);
            }
        }

        public int getVisibleTypeFlags() {
            return visibleTypeFlags;
        }

        private int getVisibleParentTypes(final ServerItemType visibleType) {
            int flags = visibleType.getFlag();

            if (visibleType == ServerItemType.FILE) {
                flags |= getVisibleParentTypes(ServerItemType.FOLDER);
            }
            if (visibleType == ServerItemType.GIT_BRANCH) {
                flags |= getVisibleParentTypes(ServerItemType.GIT_REPOSITORY);
            } else if (visibleType == ServerItemType.FOLDER || visibleType == ServerItemType.GIT_REPOSITORY) {
                flags |= getVisibleParentTypes(ServerItemType.TEAM_PROJECT);
            } else if (visibleType == ServerItemType.TEAM_PROJECT) {
                flags |= getVisibleParentTypes(ServerItemType.ROOT);
            }

            return flags;
        }

        @Override
        public boolean select(final Viewer viewer, final Object parentElement, final Object element) {
            final TypedServerItem node = (TypedServerItem) element;
            return (visibleTypeFlags & node.getType().getFlag()) > 0;
        }
    }

    private class ContentProvider extends TreeContentProvider {
        private ServerItemSource source;

        @Override
        public Object[] getChildren(final Object parentElement) {
            CodeMarkerDispatch.dispatch(CODEMARKER_CHILD_NODES_FETCH_START);

            final TypedServerItem parent = (TypedServerItem) parentElement;
            final Object[] children = source.getChildren(parent);

            CodeMarkerDispatch.dispatch(CODEMARKER_CHILD_NODES_FETCH_COMPLETE);

            if (children.length == 0) {
                return null;
            }

            return children;
        }

        @Override
        public boolean hasChildren(final Object element) {
            final TypedServerItem item = (TypedServerItem) element;

            if (item.getType().equals(ServerItemType.FILE)) {
                return false;
            }

            if (item.getType().equals(ServerItemType.GIT_REPOSITORY)) {
                return (getVisibleTypeFlags() & ServerItemType.GIT_BRANCH.getFlag()) != 0;
            }

            return true;
        }

        @Override
        public Object[] getElements(final Object inputElement) {
            source = (ServerItemSource) inputElement;

            if (source == null) {
                return new Object[] {};
            } else {
                return new Object[] {
                    TypedServerItem.ROOT
                };
            }
        }

        @Override
        public Object getParent(final Object element) {
            final TypedServerItem node = (TypedServerItem) element;
            return node.getParent();
        }
    }
}
