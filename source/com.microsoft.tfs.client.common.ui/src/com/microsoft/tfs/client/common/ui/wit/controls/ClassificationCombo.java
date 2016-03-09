// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.wit.controls;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;

import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.framework.WindowSystem;
import com.microsoft.tfs.client.common.ui.helpers.ComboHelper;
import com.microsoft.tfs.core.clients.workitem.CoreFieldReferenceNames;
import com.microsoft.tfs.core.clients.workitem.node.Node;
import com.microsoft.tfs.core.clients.workitem.node.Node.TreeType;
import com.microsoft.tfs.core.clients.workitem.node.NodeCollection;
import com.microsoft.tfs.core.clients.workitem.project.Project;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.GUID;

/**
 * Classification combo displays WIT classification fields (areas, iteration) in
 * a hierarchical format.
 *
 * In the combo data, we set the items as the name of each item (sans parents),
 * but once selected, we display the full path in the combo box's text field.
 *
 * The temptation here is to use AutocompleteCombo to reduce code replication in
 * the autocompletion. It will even appear to work, but you'll be stuck
 * debugging the Modify event handling on GTK for, approximately, ever.
 */
public class ClassificationCombo extends Composite {
    private final Combo combo;

    private Project project;
    private TreeType treeType;

    private Node[] itemNodes;
    private String[] itemTitles;

    private Node selectedNode;

    public ClassificationCombo(final Composite parent, final int style) {
        super(parent, SWT.NONE);

        setLayout(new FillLayout());

        combo = new Combo(this, style);

        /*
         * When an item is selected, we need to update the text in the combo
         * box's text field to represent the full path to the classification
         * node.
         */
        combo.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
                updateSelectionText();

                if (WindowSystem.isCurrentWindowSystem(WindowSystem.WINDOWS)) {
                    combo.setSelection(new Point(0, getText().length()));
                }
            }
        });

        /*
         * SWT GTK hack: Selection events do not fire properly when using the
         * up/down arrows in the combo box text field.
         */
        if (WindowSystem.isCurrentWindowSystem(WindowSystem.GTK)) {
            final GtkSelectionAdapter selectionAdapter = new GtkSelectionAdapter();

            combo.addKeyListener(selectionAdapter);
            combo.addModifyListener(selectionAdapter);
        }

        /*
         * SWT Mac hack: we do not always set the selection properly when focus
         * is gained.
         */
        if (WindowSystem.isCurrentWindowSystem(WindowSystem.AQUA)) {
            combo.addFocusListener(new FocusAdapter() {
                @Override
                public void focusGained(final FocusEvent e) {
                    combo.setSelection(new Point(0, getText().length()));
                }
            });
        }
    }

    public void setProject(final Project project) {
        this.project = project;

        updateItems();
    }

    public void setTreeType(final String fieldName) {
        if (CoreFieldReferenceNames.AREA_PATH.equals(fieldName)) {
            setTreeType(TreeType.AREA);
        } else if (CoreFieldReferenceNames.ITERATION_PATH.equals(fieldName)) {
            setTreeType(TreeType.ITERATION);
        } else {
            final String messageFormat = Messages.getString("ClassificationCombo.InvalidFieldFormat"); //$NON-NLS-1$
            final String message = MessageFormat.format(messageFormat, fieldName);
            throw new IllegalArgumentException(message);
        }
    }

    public void setTreeType(final TreeType treeType) {
        this.treeType = treeType;

        updateItems();
    }

    /**
     * In the combo box's item list, use the node names with leading spaces to
     * display hierarchy.
     */
    private void updateItems() {
        final List itemNodeList = new ArrayList();
        final List itemTitleList = new ArrayList();
        final List itemPathList = new ArrayList();

        if (project != null && treeType != null) {
            final Node projectNode = new ProjectNodeAdapter();

            itemNodeList.add(projectNode);
            itemTitleList.add(project.getName());
            itemPathList.add(project.getName());

            updateItemsRecursive(itemNodeList, itemTitleList, itemPathList, 1, projectNode.getChildNodes().getNodes());
        }

        itemNodes = (Node[]) itemNodeList.toArray(new Node[itemNodeList.size()]);
        itemTitles = (String[]) itemTitleList.toArray(new String[itemTitleList.size()]);

        combo.setItems(itemTitles);
        ComboHelper.setVisibleItemCount(combo);
    }

    private void updateItemsRecursive(
        final List itemNodeList,
        final List itemTitleList,
        final List itemPathList,
        final int depth,
        final Node[] nodes) {
        Check.notNull(nodes, "nodes"); //$NON-NLS-1$

        for (int i = 0; i < nodes.length; i++) {
            final StringBuffer title = new StringBuffer();

            /* Pad with spaces for hierarchy */
            for (int j = 0; j < depth; j++) {
                title.append("     "); //$NON-NLS-1$
            }

            title.append(nodes[i].getName());

            /*
             * Mac SWT hack: Mac requires all values to be unique, otherwise it
             * will get confused between two different entries for "    Area 0",
             * and will always return the first instance as the selection. Work
             * around by appending nulls to every item.
             */
            if (WindowSystem.isCurrentWindowSystem(WindowSystem.AQUA)) {
                for (int j = 0; j < itemNodeList.size(); j++) {
                    title.append('\u0000');
                }
            }

            itemNodeList.add(nodes[i]);
            itemTitleList.add(title.toString());
            itemPathList.add(nodes[i].getPath());

            /* Depth first recursion */
            final Node[] children = nodes[i].getChildNodes().getNodes();

            if (children != null && children.length > 0) {
                updateItemsRecursive(itemNodeList, itemTitleList, itemPathList, depth + 1, children);
            }
        }
    }

    /**
     * On selection of a combo box item, update the text field to the full path
     * of the item, instead of the node name (with leading padding.)
     */
    private void updateSelectionText() {
        final int i = combo.getSelectionIndex();

        if (i >= 0 && i < itemNodes.length) {
            selectedNode = itemNodes[i];
            combo.setText(selectedNode.getPath());

            /* OS X selects the text on any new selection */
            if (WindowSystem.isCurrentWindowSystem(WindowSystem.AQUA)) {
                combo.setSelection(new Point(0, getText().length()));
            }
        }
    }

    public Combo getCombo() {
        return combo;
    }

    public void setText(final String text) {
        combo.setText(text);
    }

    public String getText() {
        return combo.getText();
    }

    public void addModifyListener(final ModifyListener listener) {
        combo.addModifyListener(listener);
    }

    public void removeModifyListener(final ModifyListener listener) {
        combo.removeModifyListener(listener);
    }

    @Override
    public void addFocusListener(final FocusListener listener) {
        combo.addFocusListener(listener);
    }

    @Override
    public void removeFocusListener(final FocusListener listener) {
        combo.removeFocusListener(listener);
    }

    @Override
    public void setBackground(final Color color) {
        combo.setBackground(color);
    }

    @Override
    public Color getBackground() {
        return combo.getBackground();
    }

    @Override
    public void setForeground(final Color color) {
        combo.setForeground(color);
    }

    @Override
    public Color getForeground() {
        return combo.getForeground();
    }

    @Override
    public void setFont(final Font font) {
        combo.setFont(font);
    }

    @Override
    public Font getFont() {
        return combo.getFont();
    }

    /**
     * A simple class to implement the Node interface so that we can jam
     * Projects into the item array.
     */
    private class ProjectNodeAdapter implements Node {
        public ProjectNodeAdapter() {
        }

        @Override
        public NodeCollection getChildNodes() {
            if (treeType == TreeType.AREA) {
                return project.getAreaRootNodes();
            } else if (treeType == TreeType.ITERATION) {
                return project.getIterationRootNodes();
            } else {
                final String messageFormat = Messages.getString("ClassificationCombo.UnknownClassificationFormat"); //$NON-NLS-1$
                final String message = MessageFormat.format(messageFormat, treeType);
                throw new IllegalArgumentException(message);
            }
        }

        @Override
        public int getID() {
            return 0;
        }

        @Override
        public GUID getGUID() {
            return project.getGUID();
        }

        @Override
        public String getName() {
            return project.getName();
        }

        @Override
        public Node getParent() {
            return null;
        }

        @Override
        public String getPath() {
            return project.getName();
        }

        @Override
        public String getURI() {
            return null;
        }

        @Override
        public int compareTo(final Node o) {
            return 0;
        }
    }

    /**
     * SWT GTK hack: when you select a combox item with the up/down arrow keys,
     * a selection event is not fired. Thus we have to hook up a key listener
     * which listens for up/down arrow keys and then runs the selection handling
     * code internally.
     *
     * TODO: If we ever hook up selection events to this class, we need to fire
     * selection events here also since the native widget is not doing so.
     */
    private class GtkSelectionAdapter implements KeyListener, ModifyListener {
        private boolean updateModification = false;

        @Override
        public void keyPressed(final KeyEvent e) {
            /*
             * It's important to check that we're not at the first/last element
             * -- ie, that this keystroke is actually going to select something
             * in the combobox -- lest we keep the update modification listener
             * running on a proper keystroke. That could potentially be bad to
             * muck with somebody's keystroke.
             */

            /*
             * If the key is up arrow or page up and we're not at the first
             * element already, update the text.
             */
            if ((e.keyCode == SWT.ARROW_UP || e.keyCode == SWT.PAGE_UP) && combo.getSelectionIndex() != 0) {
                selectCurrent();
                updateModification = true;
            }
            /*
             * Down arrow and we're not at the last element.
             */
            else if ((e.keyCode == SWT.ARROW_DOWN || e.keyCode == SWT.PAGE_DOWN)
                && (combo.getSelectionIndex() != combo.getItemCount() - 1)) {
                selectCurrent();
                updateModification = true;
            }
        }

        /*
         * In order for up/down arrow traversal to work correctly, the current
         * item must be selected properly in the combo. (After we rewrite the
         * text, the combo loses its idea of its selection.) Thus, we have to
         * select the current item before we allow it to proceed.
         */
        private void selectCurrent() {
            for (int i = 0; i < itemNodes.length; i++) {
                if (combo.getText().equals(itemNodes[i].getPath())) {
                    combo.select(i);
                    break;
                }
            }
        }

        @Override
        public void keyReleased(final KeyEvent e) {
            // ignored
        }

        @Override
        public void modifyText(final ModifyEvent e) {
            /*
             * Update the text and notify that we've consumed the modify event
             * that was triggered by the keypress.
             */
            if (updateModification) {
                updateModification = false;
                updateSelectionText();
            }
        }
    }
}
