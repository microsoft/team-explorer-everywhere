// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.controls;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ICellEditorListener;
import org.eclipse.jface.viewers.ICellModifier;
import org.eclipse.jface.viewers.IPostSelectionProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.ViewerDropAdapter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DragSourceAdapter;
import org.eclipse.swt.dnd.DragSourceEvent;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.dnd.TransferData;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Item;
import org.eclipse.ui.plugin.AbstractUIPlugin;

import com.microsoft.tfs.client.common.commands.css.DeleteCSSNodeCommand;
import com.microsoft.tfs.client.common.commands.css.GetClassificationNodesCommand;
import com.microsoft.tfs.client.common.commands.css.NewCSSNodeCommand;
import com.microsoft.tfs.client.common.commands.css.ReParentCSSNodeCommand;
import com.microsoft.tfs.client.common.commands.css.RenameCSSNodeCommand;
import com.microsoft.tfs.client.common.commands.css.ReorderCSSNodeCommand;
import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.TFSCommonUIClientPlugin;
import com.microsoft.tfs.client.common.ui.controls.generic.BaseControl;
import com.microsoft.tfs.client.common.ui.dialogs.DeleteNodesDialog;
import com.microsoft.tfs.client.common.ui.dialogs.css.actions.DeleteNodeAction;
import com.microsoft.tfs.client.common.ui.dialogs.css.actions.DemoteNodeAction;
import com.microsoft.tfs.client.common.ui.dialogs.css.actions.MoveDownNodeAction;
import com.microsoft.tfs.client.common.ui.dialogs.css.actions.MoveUpNodeAction;
import com.microsoft.tfs.client.common.ui.dialogs.css.actions.NewNodeAction;
import com.microsoft.tfs.client.common.ui.dialogs.css.actions.PromoteNodeAction;
import com.microsoft.tfs.client.common.ui.dialogs.css.actions.RenameNodeAction;
import com.microsoft.tfs.client.common.ui.framework.WindowSystem;
import com.microsoft.tfs.client.common.ui.framework.action.StandardActionConstants;
import com.microsoft.tfs.client.common.ui.framework.command.BusyIndicatorCommandExecutor;
import com.microsoft.tfs.client.common.ui.framework.helper.DoubleClickAdapter;
import com.microsoft.tfs.client.common.ui.framework.tree.TreeContentProvider;
import com.microsoft.tfs.core.clients.commonstructure.CSSNode;
import com.microsoft.tfs.core.clients.commonstructure.CSSStructureType;
import com.microsoft.tfs.core.clients.commonstructure.CommonStructureClient;
import com.microsoft.tfs.util.Check;

/**
 * Control for editing areas or iterations.
 */
public class CommonStructureControl extends BaseControl implements IPostSelectionProvider, ISelectionProvider {
    private final CommonStructureClient css;

    /**
     * SWT table styles that will always be used.
     */
    private static int TREE_STYLES = SWT.H_SCROLL | SWT.V_SCROLL;

    /**
     * SWT table styles that will only be used if the client passes them in.
     */
    private static int OPTIONAL_TREE_STYLES = SWT.BORDER | SWT.MULTI | SWT.SINGLE;

    private final TreeViewer viewer;

    private IAction newNodeAction;
    private IAction renameNodeAction;
    private IAction deleteNodeAction;
    private IAction moveUpNodeAction;
    private IAction moveDownNodeAction;
    private IAction promoteNodeAction;
    private IAction demoteNodeAction;
    private IAction refreshAction;

    private final MenuManager contextMenu;

    private CSSNode rootNode;

    private CSSNode newNode;

    private boolean isInDragDrop;

    public CommonStructureControl(final Composite parent, final int style, final CommonStructureClient css) {
        super(parent, style);

        Check.notNull(css, "css"); //$NON-NLS-1$
        this.css = css;

        final FillLayout layout = new FillLayout();
        layout.marginWidth = 0;
        layout.marginHeight = 0;
        layout.spacing = getSpacing();
        setLayout(layout);

        viewer = new TreeViewer(this, TREE_STYLES | (style & OPTIONAL_TREE_STYLES));

        viewer.setUseHashlookup(true);
        viewer.setContentProvider(new ContentProvider());
        viewer.setLabelProvider(new LabelProvider());
        viewer.setAutoExpandLevel(TreeViewer.ALL_LEVELS);

        viewer.addDoubleClickListener(new DoubleClickAdapter() {
            @Override
            protected void doubleClick(final Object item) {
                final boolean expandedState = viewer.getExpandedState(item);
                viewer.setExpandedState(item, !expandedState);
            }
        });

        contextMenu = createContextMenu();
        createActions();
        contributeActions();

        /* Viewer cell editor support only available in Eclipse 3.1+ */
        if (SWT.getVersion() >= 3100) {
            addCellEditorSupport();
        }

        // Handle Insert and Delete key press.
        viewer.getTree().addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(final KeyEvent e) {
                if (e.keyCode == SWT.DEL && deleteNodeAction != null && deleteNodeAction.isEnabled()) {
                    deleteNodeAction.run();
                } else if (e.keyCode == SWT.INSERT && newNodeAction != null && newNodeAction.isEnabled()) {
                    newNodeAction.run();
                }
            }
        });

        addDragDropSupport();
    }

    private void createActions() {
        newNodeAction = new NewNodeAction(this);
        renameNodeAction = new RenameNodeAction(this);
        deleteNodeAction = new DeleteNodeAction(this);
        moveUpNodeAction = new MoveUpNodeAction(this);
        moveDownNodeAction = new MoveDownNodeAction(this);
        promoteNodeAction = new PromoteNodeAction(this);
        demoteNodeAction = new DemoteNodeAction(this);
        refreshAction = new Action() {
            @Override
            public void run() {
                refresh();
            }
        };
        refreshAction.setText(Messages.getString("CommonStructureControl.RefreshActionText")); //$NON-NLS-1$
        refreshAction.setImageDescriptor(
            AbstractUIPlugin.imageDescriptorFromPlugin(TFSCommonUIClientPlugin.PLUGIN_ID, "icons/Refresh.gif")); //$NON-NLS-1$
    }

    public void setRootNode(final CSSNode rootNode) {
        this.rootNode = rootNode;
        final CSSNode dummyRoot = new CSSNode(rootNode.getStructureType(), ""); //$NON-NLS-1$
        dummyRoot.addChild(rootNode);
        viewer.setInput(dummyRoot);
    }

    public CSSNode getRootNode() {
        return rootNode;
    }

    private void refresh() {
        final BusyIndicatorCommandExecutor executor = new BusyIndicatorCommandExecutor(getShell());
        final GetClassificationNodesCommand command = new GetClassificationNodesCommand(css, rootNode.getProjectURI());
        final IStatus status = executor.execute(command);

        if (status.isOK()) {
            if (CSSStructureType.PROJECT_LIFECYCLE.equals(rootNode.getStructureType())) {
                // We are doing iterations
                setRootNode(command.getIterations());
            } else if (CSSStructureType.PROJECT_MODEL_HIERARCHY.equals(rootNode.getStructureType())) {
                // We are doing areas
                setRootNode(command.getAreas());
            }
        }

    }

    public void renameNode(final CSSNode node, final String newName) {
        if (node.getParentURI() == null || node.getParentURI().length() == 0) {
            MessageDialog.openError(
                getShell(),
                Messages.getString("CommonStructureControl.ErrorDialogTitle"), //$NON-NLS-1$
                Messages.getString("CommonStructureControl.CantRenameRootDialogText")); //$NON-NLS-1$
            return;
        }

        if (newName == null || newName.length() == 0) {
            return;
        }

        final BusyIndicatorCommandExecutor executor = new BusyIndicatorCommandExecutor(getShell());
        final RenameCSSNodeCommand command = new RenameCSSNodeCommand(css, node, newName);
        final IStatus status = executor.execute(command);

        if (status.isOK()) {
            viewer.update(command.getNode(), null);
        }
    }

    public void newNode(final CSSNode newNode, final String nodeName) {
        setNewNode(null);

        if (newNode.getParentURI() == null || newNode.getParentURI().length() == 0) {
            MessageDialog.openError(
                getShell(),
                Messages.getString("CommonStructureControl.ErrorDialogTitle"), //$NON-NLS-1$
                Messages.getString("CommonStructureControl.MustHaveParentDialogText")); //$NON-NLS-1$
            return;
        }

        if (nodeName == null || nodeName.length() == 0) {
            viewer.remove(newNode);
            viewer.refresh(newNode.getParent());
            return;
        }

        final BusyIndicatorCommandExecutor executor = new BusyIndicatorCommandExecutor(getShell());
        final NewCSSNodeCommand command = new NewCSSNodeCommand(css, newNode, nodeName);
        final IStatus status = executor.execute(command);

        if (status.isOK()) {
            viewer.update(command.getNode(), null);
            // Need to refire selection changed events so that actions re-check
            // enablement.
            viewer.setSelection(viewer.getSelection());
        } else {
            final CSSNode parent = newNode.getParentNode();
            parent.removeChildNode(newNode);
            viewer.refresh(newNode.getParent());
            viewer.setSelection(new StructuredSelection(parent));
        }
    }

    public void deleteNode(CSSNode node) {
        if (node.getParentURI() == null || node.getParentURI().length() == 0) {
            MessageDialog.openError(
                getShell(),
                Messages.getString("CommonStructureControl.ErrorDialogTitle"), //$NON-NLS-1$
                Messages.getString("CommonStructureControl.CantDeleteRootDialogText")); //$NON-NLS-1$
            return;
        }

        final DeleteNodesDialog dialog = new DeleteNodesDialog(getShell(), rootNode, node);
        if (dialog.open() != IDialogConstants.OK_ID) {
            return;
        }
        node = dialog.getToDelete();
        final BusyIndicatorCommandExecutor executor = new BusyIndicatorCommandExecutor(getShell());
        final DeleteCSSNodeCommand command = new DeleteCSSNodeCommand(css, node, dialog.getReclassifyNode());
        final IStatus status = executor.execute(command);

        if (status.getSeverity() == IStatus.OK) {
            viewer.remove(node);
            final CSSNode parent = (CSSNode) node.getParent();
            viewer.setSelection(new StructuredSelection(parent));
            parent.removeChildNode(node);
            viewer.refresh(parent);
        }
    }

    public void reorderNode(final CSSNode node, final int increment) {
        final BusyIndicatorCommandExecutor executor = new BusyIndicatorCommandExecutor(getShell());
        final ReorderCSSNodeCommand command = new ReorderCSSNodeCommand(css, node, increment);
        final IStatus status = executor.execute(command);

        if (status.getSeverity() == IStatus.OK) {
            final CSSNode parent = node.getParentNode();
            final int position = parent.indexOfChild(node);
            if (increment < 0) {
                // Move Down
                parent.removeChildAt(position);
                parent.addChildAt(position + increment, node);
            } else {
                // Move Up
                final CSSNode swapWithNode = parent.getChildAt(position + increment);
                parent.removeChildAt(position + increment);
                parent.addChildAt(position, swapWithNode);
            }
            viewer.refresh(parent);
        }
        // Need to refire selection changed events so that actions re-check
        // enablement.
        viewer.setSelection(viewer.getSelection());
    }

    public void moveNode(final CSSNode node, final CSSNode newParent) {
        final BusyIndicatorCommandExecutor executor = new BusyIndicatorCommandExecutor(getShell());
        final ReParentCSSNodeCommand command = new ReParentCSSNodeCommand(css, node, newParent);
        final IStatus status = executor.execute(command);

        if (status.getSeverity() == IStatus.OK) {
            node.getParentNode().removeChildNode(node);
            newParent.addChild(node);
            viewer.refresh();
        }
        viewer.setSelection(viewer.getSelection(), true);
    }

    public CSSNode getNewNode() {
        return newNode;
    }

    public void setNewNode(final CSSNode newNode) {
        this.newNode = newNode;
    }

    public MenuManager getContextMenu() {
        return contextMenu;
    }

    public boolean isInDragDrop() {
        return isInDragDrop;
    }

    public void setInDragDrop(final boolean isInDragDrop) {
        this.isInDragDrop = isInDragDrop;
    }

    private MenuManager createContextMenu() {
        final MenuManager menuManager = new MenuManager("#popup"); //$NON-NLS-1$
        menuManager.setRemoveAllWhenShown(true);
        viewer.getControl().setMenu(menuManager.createContextMenu(viewer.getControl()));
        return menuManager;
    }

    private void contributeActions() {
        contextMenu.addMenuListener(new IMenuListener() {
            @Override
            public void menuAboutToShow(final IMenuManager manager) {
                manager.add(new Separator(StandardActionConstants.HOSTING_CONTROL_CONTRIBUTIONS));
                manager.add(new Separator(StandardActionConstants.PRIVATE_CONTRIBUTIONS));

                manager.appendToGroup(StandardActionConstants.PRIVATE_CONTRIBUTIONS, newNodeAction);
                manager.appendToGroup(StandardActionConstants.PRIVATE_CONTRIBUTIONS, renameNodeAction);
                manager.appendToGroup(StandardActionConstants.PRIVATE_CONTRIBUTIONS, deleteNodeAction);
                manager.appendToGroup(StandardActionConstants.PRIVATE_CONTRIBUTIONS, new Separator());
                manager.appendToGroup(StandardActionConstants.PRIVATE_CONTRIBUTIONS, moveUpNodeAction);
                manager.appendToGroup(StandardActionConstants.PRIVATE_CONTRIBUTIONS, moveDownNodeAction);
                manager.appendToGroup(StandardActionConstants.PRIVATE_CONTRIBUTIONS, new Separator());
                manager.appendToGroup(StandardActionConstants.PRIVATE_CONTRIBUTIONS, demoteNodeAction);
                manager.appendToGroup(StandardActionConstants.PRIVATE_CONTRIBUTIONS, promoteNodeAction);
                manager.appendToGroup(StandardActionConstants.PRIVATE_CONTRIBUTIONS, new Separator());
                manager.appendToGroup(StandardActionConstants.PRIVATE_CONTRIBUTIONS, refreshAction);
            }
        });
    }

    private void addCellEditorSupport() {
        viewer.setColumnProperties(new String[] {
            "NodeName" //$NON-NLS-1$
        });
        // Make text box have a border on systems other than Mac OS X
        final int textStyle = (WindowSystem.isCurrentWindowSystem(WindowSystem.AQUA) ? SWT.NONE : SWT.BORDER);
        final TextCellEditor editor = new TextCellEditor(viewer.getTree(), textStyle);
        // Listen for edit cancellations so that we can save the node if
        // creating a new one.
        editor.addListener(new ICellEditorListener() {
            @Override
            public void applyEditorValue() {
            }

            @Override
            public void cancelEditor() {
                if (getNewNode() != null) {
                    // We are in the middle of creating a new node - delete the
                    // half created node
                    final CSSNode newNode = getNewNode();
                    final CSSNode parent = newNode.getParentNode();

                    parent.removeChildNode(newNode);
                    viewer.setSelection(new StructuredSelection(parent));
                    viewer.refresh(parent);
                    setNewNode(null);
                }
            }

            @Override
            public void editorValueChanged(final boolean oldValidState, final boolean newValidState) {
            }
        });
        viewer.setCellEditors(new CellEditor[] {
            editor
        });
        viewer.setCellModifier(new CellModifier(this));

    }

    private void addDragDropSupport() {
        viewer.addDragSupport(DND.DROP_MOVE, new Transfer[] {
            TextTransfer.getInstance()
        }, new DragSourceAdapter() {

            @Override
            public void dragFinished(final DragSourceEvent event) {
                setInDragDrop(false);
            }

            @Override
            public void dragSetData(final DragSourceEvent event) {
                final CSSNode node = (CSSNode) ((IStructuredSelection) viewer.getSelection()).getFirstElement();
                event.data = node.getPath();
            }

            @Override
            public void dragStart(final DragSourceEvent event) {
                final CSSNode node = (CSSNode) ((IStructuredSelection) viewer.getSelection()).getFirstElement();
                event.doit = node.getParentURI() != null && node.getParentURI().length() > 0;
                setInDragDrop(true);
            }

        });
        final ViewerDropAdapter dropAdapter = new ViewerDropAdapter(viewer) {
            @Override
            public boolean performDrop(final Object data) {
                final CSSNode newParent = (CSSNode) getCurrentTarget();
                final CSSNode node = CSSNode.resolveNode(rootNode, data.toString());
                if (node == null || !node.getPath().equals(data.toString())) {
                    // This is not the node that you are looking for.
                    return false;
                }
                if (newParent != null && !node.equals(newParent) && !node.getParentNode().equals(newParent)) {
                    moveNode(node, newParent);
                    return true;
                }
                return false;
            }

            @Override
            public boolean validateDrop(final Object target, final int operation, final TransferData transferType) {
                return TextTransfer.getInstance().isSupportedType(transferType);
            }
        };
        dropAdapter.setFeedbackEnabled(false);
        dropAdapter.setScrollExpandEnabled(true);

        viewer.addDropSupport(DND.DROP_MOVE, new Transfer[] {
            TextTransfer.getInstance()
        }, dropAdapter);
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
    public ISelection getSelection() {
        return viewer.getSelection();
    }

    public CSSNode getSelectedNode() {
        return (CSSNode) ((IStructuredSelection) getSelection()).getFirstElement();
    }

    @Override
    public void removeSelectionChangedListener(final ISelectionChangedListener listener) {
        viewer.removeSelectionChangedListener(listener);
    }

    @Override
    public void setSelection(final ISelection selection) {
        viewer.setSelection(selection);
    }

    public TreeViewer getTreeViewer() {
        return viewer;
    }

    private static class ContentProvider extends TreeContentProvider {
        @Override
        public Object[] getChildren(final Object parentElement) {
            final CSSNode parent = (CSSNode) parentElement;
            return parent.getChildren();
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

    private static class CellModifier implements ICellModifier {
        private final CommonStructureControl cssControl;
        private CSSNode lastSelectedNode;

        public CellModifier(final CommonStructureControl cssControl) {
            this.cssControl = cssControl;
        }

        @Override
        public boolean canModify(final Object element, final String property) {
            final CSSNode node = (CSSNode) element;

            if (node == null) {
                lastSelectedNode = node;
                return false;
            }
            if (node.getParentURI() == null || node.getParentURI().length() == 0) {
                lastSelectedNode = node;
                return false;
            }

            // Only enable modification if the cell has been previously
            // selected.
            boolean canModify = false;
            if (node.equals(lastSelectedNode)) {
                canModify = true;
            } else {
                lastSelectedNode = node;
            }

            return canModify;
        }

        @Override
        public Object getValue(final Object element, final String property) {
            return element.toString();
        }

        @Override
        public void modify(final Object element, final String property, final Object value) {
            final CSSNode node = (CSSNode) (element instanceof Item ? ((Item) element).getData() : element);

            if (node.getURI().length() == 0) {
                // this is a new node.
                if (value == null || value.toString() == null || value.toString().trim().length() == 0) {
                    // No name provided - remove the new node.
                    final CSSNode parent = node.getParentNode();
                    parent.removeChildNode(node);
                    cssControl.getTreeViewer().setSelection(new StructuredSelection(parent));
                    cssControl.getTreeViewer().refresh(parent);
                    return;
                }
                cssControl.newNode(node, value.toString().trim());
            } else {
                // Renaming a node
                if (value == null || value.toString() == null || value.toString().trim().length() == 0) {
                    return;
                }
                cssControl.renameNode(node, value.toString().trim());
            }
        }
    }

}
