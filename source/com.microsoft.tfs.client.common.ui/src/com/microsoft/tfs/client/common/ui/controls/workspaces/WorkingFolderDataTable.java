// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.controls.workspaces;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.viewers.ComboBoxCellEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.TraverseEvent;
import org.eclipse.swt.events.TraverseListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;

import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.controls.LocalPathCellEditor;
import com.microsoft.tfs.client.common.ui.controls.vc.ServerFolderPathCellEditor;
import com.microsoft.tfs.client.common.ui.framework.action.StandardActionConstants;
import com.microsoft.tfs.client.common.ui.framework.action.keybinding.ActionKeyBindingSupport;
import com.microsoft.tfs.client.common.ui.framework.action.keybinding.ActionKeyBindingSupportFactory;
import com.microsoft.tfs.client.common.ui.framework.action.keybinding.CommandIDs;
import com.microsoft.tfs.client.common.ui.framework.helper.ContentProviderAdapter;
import com.microsoft.tfs.client.common.ui.framework.helper.TableViewerSorter;
import com.microsoft.tfs.client.common.ui.framework.sizing.ControlSize;
import com.microsoft.tfs.client.common.ui.framework.table.TableColumnData;
import com.microsoft.tfs.client.common.ui.framework.table.TableControl;
import com.microsoft.tfs.client.common.ui.framework.validation.ActionValidatorBinding;
import com.microsoft.tfs.core.TFSTeamProjectCollection;
import com.microsoft.tfs.core.clients.versioncontrol.exceptions.ServerPathFormatException;
import com.microsoft.tfs.core.clients.versioncontrol.path.ServerPath;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.WorkingFolderType;
import com.microsoft.tfs.util.listeners.SingleListenerFacade;
import com.microsoft.tfs.util.valid.IValidity;
import com.microsoft.tfs.util.valid.Validity;

public class WorkingFolderDataTable extends TableControl {
    public static final String COLUMN_TYPE = "type"; //$NON-NLS-1$
    public static final String COLUMN_SERVER = "server"; //$NON-NLS-1$
    public static final String COLUMN_LOCAL = "local"; //$NON-NLS-1$

    private static final Object LAST_ROW_PLACEHOLDER = new Object();

    private static final String TYPE_ACTIVE = Messages.getString("WorkingFolderDataTable.TypeActive"); //$NON-NLS-1$
    private static final String TYPE_CLOAKED = Messages.getString("WorkingFolderDataTable.TypeCloaked"); //$NON-NLS-1$

    private static final String NEWLINE = System.getProperty("line.separator"); //$NON-NLS-1$

    private final SingleListenerFacade editListeners = new SingleListenerFacade(WorkingFolderDataEditListener.class);

    private IAction cutAction;
    private IAction copyAction;
    private IAction pasteAction;
    private IAction deleteAction;
    private IAction selectAllAction;

    private ActionKeyBindingSupport actionCommandSupport;

    public WorkingFolderDataTable(final Composite parent, final int style) {
        this(parent, style, null, null);
    }

    public WorkingFolderDataTable(final Composite parent, final int style, final TFSTeamProjectCollection connection) {
        this(parent, style, null, connection);
    }

    public WorkingFolderDataTable(final Composite parent, final int style, final String viewDataKey) {
        this(parent, style, viewDataKey, null);
    }

    public WorkingFolderDataTable(
        final Composite parent,
        final int style,
        final String viewDataKey,
        final TFSTeamProjectCollection connection) {
        super(parent, style, WorkingFolderData.class, viewDataKey);

        final TableColumnData[] columnData = new TableColumnData[] {
            new TableColumnData().setText(Messages.getString("WorkingFolderDataTable.ColumnNameStatus")).setIdentifier( //$NON-NLS-1$
                COLUMN_TYPE).setCharWidth(20),
            new TableColumnData().setText(
                Messages.getString("WorkingFolderDataTable.ColumnNameServerFolder")).setIdentifier( //$NON-NLS-1$
                    COLUMN_SERVER).setCharWidth(40),
            new TableColumnData().setText(
                Messages.getString("WorkingFolderDataTable.ColumnNameLocalFolder")).setIdentifier( //$NON-NLS-1$
                    COLUMN_LOCAL).setCharWidth(40)
        };

        setupTable(true, true, columnData);

        setUseDefaultLabelProvider();
        getViewer().setContentProvider(new ContentProvider());
        final TableViewerSorter sorter = new TableViewerSorter(getViewer());
        sorter.setCategoryProvider(new TableViewerSorter.CategoryProvider() {
            @Override
            public int getCategory(final Object element) {
                if (LAST_ROW_PLACEHOLDER == element) {
                    return 1;
                }
                return 0;
            }
        });
        getViewer().setSorter(sorter);

        setCellEditor(COLUMN_TYPE, new ComboBoxCellEditor(getTable(), new String[] {
            TYPE_ACTIVE,
            TYPE_CLOAKED
        }));
        setCellEditor(COLUMN_SERVER, new ServerFolderPathCellEditor(getTable(), SWT.NONE, connection));
        setCellEditor(COLUMN_LOCAL, new LocalPathCellEditor(getTable(), SWT.NONE));

        getTable().addTraverseListener(new TraverseListener() {
            @Override
            public void keyTraversed(final TraverseEvent e) {
                WorkingFolderDataTable.this.keyTraversed(e);
            }
        });

        // Ensure doubleclick edits column.
        getTable().addListener(SWT.MouseDoubleClick, new Listener() {
            @Override
            public void handleEvent(final Event event) {
                final Table table = WorkingFolderDataTable.this.getTable();
                final TableItem[] selection = table.getSelection();

                if (selection.length != 1) {
                    return;
                }

                final TableItem item = table.getSelection()[0];

                for (int i = 0; i < table.getColumnCount(); i++) {
                    if (item.getBounds(i).contains(event.x, event.y)) {
                        WorkingFolderDataTable.this.getViewer().editElement(item.getData(), i);
                        break;
                    }
                }
            }
        });

        createActions();

        getContextMenu().addMenuListener(new IMenuListener() {
            @Override
            public void menuAboutToShow(final IMenuManager manager) {
                WorkingFolderDataTable.this.onMenuAboutToShow(manager);
            }
        });

        setClipboardTransferType(TextTransfer.getInstance());
    }

    public void pasteFromClipboard() {
        final String text = (String) getClipboard().getContents(TextTransfer.getInstance());
        if (text == null) {
            return;
        }

        final WorkingFolderData[] workingFolders = getWorkingFoldersFromClipboardText(text);
        if (workingFolders.length == 0) {
            return;
        }

        final WorkingFolderDataCollection collection = getWorkingFolderDataCollection();
        for (int i = 0; i < workingFolders.length; i++) {
            collection.add(workingFolders[i]);
        }

        refresh();
    }

    public void cutSelectionToClipboard() {
        copySelectionToClipboard();
        removeSelectedWorkingFolders();
    }

    public void setConnection(final TFSTeamProjectCollection connection) {
        final ServerFolderPathCellEditor cellEditor = (ServerFolderPathCellEditor) getCellEditor(COLUMN_SERVER);
        cellEditor.setConnection(connection);
    }

    public void setWorkingFolderDataCollection(final WorkingFolderDataCollection collection) {
        setInput(collection);
    }

    public WorkingFolderDataCollection getWorkingFolderDataCollection() {
        return (WorkingFolderDataCollection) getViewer().getInput();
    }

    public void setWorkingFolders(final WorkingFolderData[] workingFolders) {
        setWorkingFolderDataCollection(new WorkingFolderDataCollection(workingFolders));
    }

    public WorkingFolderData[] getWorkingFolders() {
        return getWorkingFolderDataCollection().getWorkingFolderData();
    }

    public void addEditListener(final WorkingFolderDataEditListener listener) {
        editListeners.addListener(listener);
    }

    public void removeEditListener(final WorkingFolderDataEditListener listener) {
        editListeners.removeListener(listener);
    }

    public void setSelectedWorkingFolders(final WorkingFolderData[] workingFolders) {
        setSelectedElements(workingFolders);
    }

    public void setSelectedWorkingFolder(final WorkingFolderData workingFolder) {
        setSelectedElement(workingFolder);
    }

    public WorkingFolderData[] getSelectedWorkingFolders() {
        return (WorkingFolderData[]) getSelectedElements();
    }

    public WorkingFolderData getSelectedWorkingFolder() {
        return (WorkingFolderData) getSelectedElement();
    }

    public void setCheckedWorkingFolders(final WorkingFolderData[] workingFolders) {
        setCheckedElements(workingFolders);
    }

    public WorkingFolderData[] getCheckedWorkingFolders() {
        return (WorkingFolderData[]) getCheckedElements();
    }

    public WorkingFolderData[] removeSelectedWorkingFolders() {
        final WorkingFolderData[] workingFoldersToRemove = getSelectedWorkingFolders();

        if (workingFoldersToRemove.length > 0) {
            final int ix = getMaxSelectionIndex();

            Object elementToSelect = null;
            if (ix == getTable().getItemCount() - 1) {
                elementToSelect = LAST_ROW_PLACEHOLDER;
            } else {
                elementToSelect = getElement(ix + 1);
            }

            final WorkingFolderDataCollection collection = getWorkingFolderDataCollection();

            for (int i = 0; i < workingFoldersToRemove.length; i++) {
                collection.remove(workingFoldersToRemove[i]);
            }

            refresh();
            setSelectedElement(elementToSelect);
        }

        return workingFoldersToRemove;
    }

    public boolean editLastSelectedWorkingFolder() {
        final int ix = getMaxSelectionIndex();

        if (ix != -1) {
            editElement(getElement(ix), COLUMN_SERVER);
            return true;
        }

        return false;
    }

    public IValidity validate() {
        final WorkingFolderData[] workingFolderData = getWorkingFolderDataCollection().getWorkingFolderData();

        for (int i = 0; i < workingFolderData.length; i++) {
            final String serverItem = workingFolderData[i].getServerItem();

            if (serverItem == null || serverItem.trim().length() == 0) {
                return Validity.invalid(Messages.getString("WorkingFolderDataTable.SpecifyServerFolder")); //$NON-NLS-1$
            }

            try {
                final String canonicalServerItem = ServerPath.canonicalize(serverItem);
                workingFolderData[i].setServerItem(canonicalServerItem);
            } catch (final ServerPathFormatException e) {
                final String messageFormat = Messages.getString("WorkingFolderDataTable.ProblemWithServerPathFormat"); //$NON-NLS-1$
                final String message =
                    MessageFormat.format(messageFormat, workingFolderData[i].getServerItem(), e.getMessage());

                return Validity.invalid(message);
            }

            final String localItem = workingFolderData[i].getLocalItem();
            if (!workingFolderData[i].isCloak() && (localItem == null || localItem.trim().length() == 0)) {
                return Validity.invalid(Messages.getString("WorkingFolderDataTable.SpecifyLocalFolder")); //$NON-NLS-1$
            }
        }

        return Validity.VALID;
    }

    @Override
    public Point computeSize(final int wHint, final int hHint, final boolean changed) {
        return ControlSize.computeCharSize(wHint, hHint, getViewer().getControl(), 110, 15);
    }

    @Override
    protected String getElementsValidatorErrorMessage() {
        return Messages.getString("WorkingFolderDataTable.NeedAtLeastOneFolderMapping"); //$NON-NLS-1$
    }

    protected void onMenuAboutToShow(final IMenuManager manager) {
        manager.add(new Separator(StandardActionConstants.HOSTING_CONTROL_CONTRIBUTIONS));
        manager.add(new Separator(StandardActionConstants.PRIVATE_CONTRIBUTIONS));

        manager.appendToGroup(StandardActionConstants.PRIVATE_CONTRIBUTIONS, cutAction);
        manager.appendToGroup(StandardActionConstants.PRIVATE_CONTRIBUTIONS, copyAction);
        manager.appendToGroup(StandardActionConstants.PRIVATE_CONTRIBUTIONS, pasteAction);
        manager.appendToGroup(StandardActionConstants.PRIVATE_CONTRIBUTIONS, deleteAction);
        manager.appendToGroup(StandardActionConstants.PRIVATE_CONTRIBUTIONS, new Separator());
        manager.appendToGroup(StandardActionConstants.PRIVATE_CONTRIBUTIONS, selectAllAction);
    }

    @Override
    protected void onKeyPressed(final KeyEvent e) {
        super.onKeyPressed(e);

        if (e.keyCode == SWT.DEL && e.stateMask == SWT.NONE) {
            removeSelectedWorkingFolders();
        } else if (e.keyCode == SWT.F2 && e.stateMask == SWT.NONE) {
            editLastSelectedWorkingFolder();
        }
    }

    @Override
    protected boolean hideElementFromCollections(final Object element) {
        return LAST_ROW_PLACEHOLDER == element;
    }

    @Override
    protected String getColumnText(final Object element, final String columnPropertyName) {
        if (LAST_ROW_PLACEHOLDER == element) {
            if (COLUMN_SERVER.equals(columnPropertyName)) {
                return Messages.getString("WorkingFolderDataTable.ClickHereToAddWorkingFolder"); //$NON-NLS-1$
            }
        } else {
            final WorkingFolderData workingFolder = (WorkingFolderData) element;

            if (COLUMN_TYPE.equals(columnPropertyName)) {
                if (workingFolder.isCloak()) {
                    return TYPE_CLOAKED;
                } else {
                    return TYPE_ACTIVE;
                }
            }

            if (COLUMN_SERVER.equals(columnPropertyName)) {
                return workingFolder.getServerItem();
            }

            if (COLUMN_LOCAL.equals(columnPropertyName)) {
                return workingFolder.getLocalItem();
            }
        }

        return super.getColumnText(element, columnPropertyName);
    }

    @Override
    protected Color getForegroundColor(final Object element) {
        if (LAST_ROW_PLACEHOLDER == element) {
            return getDisplay().getSystemColor(SWT.COLOR_DARK_GRAY);
        }

        return super.getForegroundColor(element);
    }

    @Override
    protected boolean canModifyElement(final Object element, final String columnPropertyName) {
        if (LAST_ROW_PLACEHOLDER == element) {
            // We can always edit the last element.
            return true;
        }

        // Allow the user to select a cell without editing it. First click
        // selects,
        // second click goes through to be checked if modification allowed.
        if (element != getSelectedElement()) {
            return false;
        }

        if (COLUMN_LOCAL.equals(columnPropertyName)) {
            // Local path can only be edited when not cloaked.
            final WorkingFolderData workingFolder = (WorkingFolderData) element;
            return !workingFolder.isCloak();
        }

        return true;
    }

    @Override
    protected Object getValueToModify(final Object element, final String columnPropertyName) {
        if (LAST_ROW_PLACEHOLDER == element) {
            if (COLUMN_TYPE.equals(columnPropertyName)) {
                return new Integer(-1);
            }
        } else {
            final WorkingFolderData workingFolder = (WorkingFolderData) element;

            if (COLUMN_TYPE.equals(columnPropertyName)) {
                if (workingFolder.isCloak()) {
                    // index of "Cloaked" in the ComboBoxCellEditor's values
                    return new Integer(1);
                } else {
                    // index of "Active" in the ComboBoxCellEditor's values
                    return new Integer(0);
                }
            }
            if (COLUMN_SERVER.equals(columnPropertyName)) {
                return workingFolder.getServerItem();
            }
            if (COLUMN_LOCAL.equals(columnPropertyName)) {
                return workingFolder.getLocalItem();
            }
        }

        return null;
    }

    @Override
    protected void modifyElement(final Object element, final String columnPropertyName, final Object value) {
        if (LAST_ROW_PLACEHOLDER == element) {
            if (COLUMN_SERVER.equals(columnPropertyName) || COLUMN_LOCAL.equals(columnPropertyName)) {
                String sValue = (String) value;
                if (sValue != null) {
                    sValue = sValue.trim();
                }
                if (sValue != null && sValue.length() > 0) {
                    final String serverItem = COLUMN_SERVER.equals(columnPropertyName) ? sValue : null;
                    final String localItem = COLUMN_LOCAL.equals(columnPropertyName) ? sValue : null;
                    final WorkingFolderData newWorkingFolder =
                        new WorkingFolderData(serverItem, localItem, WorkingFolderType.MAP);

                    final WorkingFolderDataEditEvent event =
                        new WorkingFolderDataEditEvent(this, newWorkingFolder, true, columnPropertyName);
                    ((WorkingFolderDataEditListener) editListeners.getListener()).onWorkingFolderDataEdit(event);

                    getWorkingFolderDataCollection().add(newWorkingFolder);
                    refresh();
                }
            }
        } else {
            final WorkingFolderData workingFolder = (WorkingFolderData) element;
            if (COLUMN_TYPE.equals(columnPropertyName)) {
                final int iValue = ((Integer) value).intValue();
                if (iValue == 1) {
                    workingFolder.setType(WorkingFolderType.CLOAK);
                    workingFolder.setLocalItem(null);
                } else {
                    workingFolder.setType(WorkingFolderType.MAP);
                }
            } else if (COLUMN_SERVER.equals(columnPropertyName)) {
                workingFolder.setServerItem((String) value);
            } else if (COLUMN_LOCAL.equals(columnPropertyName)) {
                workingFolder.setLocalItem((String) value);
            }

            final WorkingFolderDataEditEvent event =
                new WorkingFolderDataEditEvent(this, workingFolder, false, columnPropertyName);
            ((WorkingFolderDataEditListener) editListeners.getListener()).onWorkingFolderDataEdit(event);

            getViewer().update(workingFolder, new String[] {
                columnPropertyName
            });
        }
    }

    @Override
    protected Object getTransferData(final Transfer transferType, final Object[] selectedElements) {
        if (transferType == TextTransfer.getInstance()) {
            final WorkingFolderData[] workingFolders = getSelectedWorkingFolders();
            return getClipboardTextForWorkingFolders(workingFolders);
        }

        final String messageFormat = Messages.getString("WorkingFolderDataTable.UnsupportedTransferTypeFormat"); //$NON-NLS-1$
        final String message = MessageFormat.format(messageFormat, transferType);
        throw new IllegalArgumentException(message);
    }

    @Override
    protected void onDisposed() {
        if (actionCommandSupport != null) {
            actionCommandSupport.dispose();
            actionCommandSupport = null;
        }
    }

    private void keyTraversed(final TraverseEvent e) {
        if (SWT.TRAVERSE_RETURN == e.detail) {
            if (editLastSelectedWorkingFolder()) {
                e.doit = false;
            }
        }
    }

    private void createActions() {
        cutAction = new Action() {
            @Override
            public void run() {
                WorkingFolderDataTable.this.cutSelectionToClipboard();
            }
        };
        cutAction.setText(Messages.getString("WorkingFolderDataTable.CutActionText")); //$NON-NLS-1$
        cutAction.setActionDefinitionId(CommandIDs.CUT);

        copyAction = new Action() {
            @Override
            public void run() {
                WorkingFolderDataTable.this.copySelectionToClipboard();
            }
        };
        copyAction.setText(Messages.getString("WorkingFolderDataTable.CopyActionText")); //$NON-NLS-1$
        copyAction.setActionDefinitionId("org.eclipse.ui.edit.copy"); //$NON-NLS-1$

        pasteAction = new Action() {
            @Override
            public void run() {
                WorkingFolderDataTable.this.pasteFromClipboard();
            }
        };
        pasteAction.setText(Messages.getString("WorkingFolderDataTable.PasteActionText")); //$NON-NLS-1$
        pasteAction.setActionDefinitionId(CommandIDs.PASTE);

        deleteAction = new Action() {
            @Override
            public void run() {
                WorkingFolderDataTable.this.removeSelectedWorkingFolders();
            }
        };
        deleteAction.setText(Messages.getString("WorkingFolderDataTable.DeleteActionText")); //$NON-NLS-1$
        deleteAction.setAccelerator(SWT.DEL);
        deleteAction.setActionDefinitionId(CommandIDs.DELETE);

        selectAllAction = new Action() {
            @Override
            public void run() {
                WorkingFolderDataTable.this.selectAll();
            }
        };
        selectAllAction.setText(Messages.getString("WorkingFolderDataTable.SelectAllActionText")); //$NON-NLS-1$
        selectAllAction.setActionDefinitionId(CommandIDs.SELECT_ALL);

        new ActionValidatorBinding(new IAction[] {
            cutAction,
            copyAction,
            deleteAction
        }).bind(getSelectionValidator());

        /*
         * IMPORTANT: this keybinding support is only appropriate for
         * dialogs/wizards. If this control is ever hosted in a view or editor
         * (or standalone in a SWT/JFace application outside the workbench) then
         * this support will need to be made optional instead of hardcoded.
         */
        actionCommandSupport = ActionKeyBindingSupportFactory.newInstance(getShell());
        actionCommandSupport.addAction(cutAction);
        actionCommandSupport.addAction(copyAction);
        actionCommandSupport.addAction(pasteAction);
        actionCommandSupport.addAction(selectAllAction);
    }

    private String getClipboardTextForWorkingFolders(final WorkingFolderData[] workingFolders) {
        final StringBuffer buffer = new StringBuffer();

        for (int i = 0; i < workingFolders.length; i++) {
            buffer.append(getClipboardTextRowForWorkingFolder(workingFolders[i]));
            buffer.append(NEWLINE);
        }

        return buffer.toString();
    }

    private String getClipboardTextRowForWorkingFolder(final WorkingFolderData workingFolder) {
        String serverItem = workingFolder.getServerItem();
        if (serverItem == null) {
            serverItem = ""; //$NON-NLS-1$
        }

        if (workingFolder.isCloak()) {
            final String messageFormat = Messages.getString("WorkingFolderDataTable.CloadedItemFormat"); //$NON-NLS-1$
            return MessageFormat.format(messageFormat, serverItem);
        }

        String localItem = workingFolder.getLocalItem();
        if (localItem == null) {
            localItem = ""; //$NON-NLS-1$
        } else {
            if (localItem.length() > 1
                && localItem.charAt(1) == ':'
                && Character.toUpperCase(localItem.charAt(0)) != localItem.charAt(0)) {
                localItem = Character.toUpperCase(localItem.charAt(0)) + localItem.substring(1);
            }
        }

        final String messageFormat = Messages.getString("WorkingFolderDataTable.ServerAndLocalItemFormat"); //$NON-NLS-1$
        return MessageFormat.format(messageFormat, serverItem, localItem);
    }

    private WorkingFolderData[] getWorkingFoldersFromClipboardText(final String text) {
        final List workingFolders = new ArrayList();
        final String[] rows = text.split("[\r\n]"); //$NON-NLS-1$
        for (int i = 0; i < rows.length; i++) {
            final String row = rows[i].trim();
            if (row.length() > 0) {
                final WorkingFolderData workingFolder = getWorkingFolderFromClipboardTextRow(row);
                if (workingFolder != null) {
                    workingFolders.add(workingFolder);
                }
            }
        }
        return (WorkingFolderData[]) workingFolders.toArray(new WorkingFolderData[workingFolders.size()]);
    }

    private WorkingFolderData getWorkingFolderFromClipboardTextRow(String text) {
        WorkingFolderType type = WorkingFolderType.MAP;
        String serverItem;
        String localItem = null;

        if (text.startsWith(Messages.getString("WorkingFolderDataTable.Cloaked"))) //$NON-NLS-1$
        {
            if (text.length() == 8) {
                return null;
            }

            type = WorkingFolderType.CLOAK;
            text = text.substring(9);
        }

        final int ix = text.indexOf(':');
        if (ix == -1) {
            return null;
        }

        serverItem = text.substring(0, ix).trim();

        if (serverItem.length() == 0) {
            serverItem = null;
        }

        if (WorkingFolderType.CLOAK != type && ix < text.length() - 1) {
            localItem = text.substring(ix + 1);
        }

        if (localItem != null && localItem.length() == 0) {
            localItem = null;
        }

        return new WorkingFolderData(serverItem, localItem, type);
    }

    private static class ContentProvider extends ContentProviderAdapter {
        @Override
        public Object[] getElements(final Object inputElement) {
            final WorkingFolderDataCollection collection = (WorkingFolderDataCollection) inputElement;
            final WorkingFolderData[] workingFolders = collection.getWorkingFolderData();
            final Object[] returnValue = new Object[workingFolders.length + 1];
            System.arraycopy(workingFolders, 0, returnValue, 0, workingFolders.length);
            returnValue[workingFolders.length] = LAST_ROW_PLACEHOLDER;
            return returnValue;
        }
    }
}
