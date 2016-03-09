// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.controls.vc.history;

import java.text.DateFormat;
import java.util.Iterator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.IWorkbenchPartSite;

import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.TFSCommonUIImages;
import com.microsoft.tfs.client.common.ui.controls.generic.compatibility.table.CompatibilityVirtualTable;
import com.microsoft.tfs.client.common.ui.framework.action.SelectionProviderAction;
import com.microsoft.tfs.client.common.ui.framework.action.StandardActionConstants;
import com.microsoft.tfs.client.common.ui.framework.helper.UIHelpers;
import com.microsoft.tfs.client.common.ui.framework.sizing.ControlSize;
import com.microsoft.tfs.client.common.ui.helpers.AutomationIDHelper;
import com.microsoft.tfs.client.common.util.DateHelper;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Changeset;
import com.microsoft.tfs.core.exceptions.TECoreException;
import com.microsoft.tfs.core.ws.runtime.exceptions.SOAPFault;

/**
 * <p>
 * {@link HistoryCachedTableControl} displays the results of a history query in
 * a virtual table. The input to a {@link HistoryCachedTableControl} is defined
 * by a {@link HistoryInput}.
 * </p>
 *
 * <p>
 * Similar to:
 * <code>Microsoft.TeamFoundation.VersionControl.Controls.ControlHistory</code>.
 * </p>
 *
 * @see HistoryInput
 */
public class HistoryCachedTableControl extends CompatibilityVirtualTable implements IHistoryControl {
    private static final String TAB = "\t"; //$NON-NLS-1$
    private static final String NEWLINE = System.getProperty("line.separator"); //$NON-NLS-1$
    /**
     * The current {@link HistoryInput}.
     */
    private HistoryInput historyInput;

    /**
     * A cache of results from the last history query.
     */
    private HistoryIteratorCache changesetsCache;

    /**
     * The <code>DateFormat</code> used to convert changeset dates to strings
     * for display.
     */
    private DateFormat dateFormat = DateHelper.getDefaultDateTimeFormat();

    private final CopyAction copyAction;

    private static final Log log = LogFactory.getLog(HistoryCachedTableControl.class);

    HistoryCachedTableControl(final Composite parent, final int style) {
        super(parent, style, Changeset.class);

        setupTable(getTable());

        copyAction = new CopyAction(this);
    }

    @Override
    public Point computeSize(final int wHint, final int hHint, final boolean changed) {
        return ControlSize.computeCharSize(wHint, hHint, this, 100, 20);
    }

    protected void setupTable(final Table table) {
        table.setHeaderVisible(true);
        table.setLinesVisible(true);
        AutomationIDHelper.setWidgetID(table, HistoryTreeControl.HISTORY_TABLE_ID);

        TableColumn column = new TableColumn(table, SWT.NONE);
        column.setText(Messages.getString("HistoryCachedTableControl.ColumnNameChangeset")); //$NON-NLS-1$
        column.setWidth(75);

        column = new TableColumn(table, SWT.NONE);
        column.setText(Messages.getString("HistoryCachedTableControl.ColumnNameUser")); //$NON-NLS-1$
        column.setWidth(150);

        column = new TableColumn(table, SWT.NONE);
        column.setText(Messages.getString("HistoryCachedTableControl.ColumnNameDate")); //$NON-NLS-1$
        column.setWidth(150);

        column = new TableColumn(table, SWT.NONE);
        column.setText(Messages.getString("HistoryCachedTableControl.ColumnNameComment")); //$NON-NLS-1$
        column.setWidth(300);
    }

    @Override
    public IAction getCopyAction() {
        return copyAction;
    }

    public void setDateFormat(final DateFormat dateFormat) {
        this.dateFormat = dateFormat;
    }

    @Override
    public void setInput(final HistoryInput input) {
        historyInput = input;

        final Table table = getTable();

        if (input == null) {
            table.setItemCount(0);
            return;
        }

        try {
            table.setRedraw(false);

            table.setItemCount(0);

            if (input.isSingleItem()) {
                if (table.getColumnCount() != 5) {
                    final TableColumn column = new TableColumn(table, SWT.NONE, 1);
                    column.setText(Messages.getString("HistoryCachedTableControl.ColumnNameChange")); //$NON-NLS-1$
                    column.setWidth(100);
                }
            } else {
                if (table.getColumnCount() == 5) {
                    table.getColumn(1).dispose();
                }
            }

            changesetsCache = new HistoryIteratorCache(input.queryHistory());
            changesetsCache.cacheItems(256);
            int tableItemCount = changesetsCache.size();
            if (!changesetsCache.isEndOfList()) {
                ++tableItemCount;
            }
            table.setItemCount(tableItemCount);
        } catch (final SOAPFault e) {
            MessageDialog.openError(
                getShell(),
                Messages.getString("HistoryCachedTableControl.ViewErrorDialogTitle"), //$NON-NLS-1$
                e.getMessage());
        } catch (final TECoreException e) {
            MessageDialog.openError(
                getShell(),
                Messages.getString("HistoryCachedTableControl.ViewErrorDialogTitle"), //$NON-NLS-1$
                e.getMessage());
        } catch (final Exception e) {
            log.error(Messages.getString("HistoryCachedTableControl.HistoryErrorDialogTitle"), e); //$NON-NLS-1$
        } finally {
            table.setRedraw(true);
            selectedElementsChanged();
        }

        fireSelectionChanged();
    }

    @Override
    public void refresh() {
        if (historyInput != null) {
            setInput(historyInput);
        }
    }

    @Override
    public Changeset getSelectedChangeset() {
        return (Changeset) getSelectedElement();
    }

    public Changeset[] getSelectedChangesets() {
        return (Changeset[]) getSelectedElements();
    }

    @Override
    protected void fillMenu(final IMenuManager manager) {
        manager.add(new Separator(StandardActionConstants.PRIVATE_CONTRIBUTIONS));
        manager.add(new Separator(StandardActionConstants.HOSTING_CONTROL_CONTRIBUTIONS));
        manager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));

        manager.appendToGroup(StandardActionConstants.PRIVATE_CONTRIBUTIONS, copyAction);
    }

    @Override
    public void addMenuListener(final IMenuListener listener) {
        getContextMenu().addMenuListener(listener);
    }

    @Override
    protected void populateTableItem(final TableItem tableItem) {
        final int index = getTable().indexOf(tableItem);

        changesetsCache.cacheItems(index + 1);

        if (changesetsCache.isEndOfList()) {
            if (getTable().getItemCount() > changesetsCache.size()) {
                getTable().setItemCount(changesetsCache.size());
            }
        } else {
            if (index + 16 > getTable().getItemCount()) {
                changesetsCache.cacheItems(changesetsCache.size() + 256);
                getTable().setItemCount(changesetsCache.size());
            }
        }

        if (index >= changesetsCache.size()) {
            return;
        }

        final Changeset changeset = changesetsCache.get(index);
        populateTableItem(tableItem, changeset);
    }

    private void populateTableItem(final TableItem tableItem, final Changeset changeset) {
        tableItem.setData(changeset);

        int colIndex = 0;

        tableItem.setText(colIndex++, ChangesetDisplayFormatter.getIDString(changeset));
        if (historyInput.isSingleItem()) {
            tableItem.setText(colIndex++, ChangesetDisplayFormatter.getChangeString(changeset));
        }
        tableItem.setText(colIndex++, ChangesetDisplayFormatter.getUserString(historyInput.getRepository(), changeset));
        tableItem.setText(colIndex++, getDateString(changeset));
        tableItem.setText(colIndex++, ChangesetDisplayFormatter.getCommentString(changeset));
    }

    private String getDateString(final Changeset changeset) {
        return dateFormat.format(changeset.getDate().getTime());
    }

    private class CopyAction extends SelectionProviderAction {
        public CopyAction(final ISelectionProvider selectionProvider) {
            super(selectionProvider);

            setText(Messages.getString("HistoryCachedTableControl.CopyActionText")); //$NON-NLS-1$
            setToolTipText(Messages.getString("HistoryCachedTableControl.CopyActionTooltip")); //$NON-NLS-1$
            setImageDescriptor(TFSCommonUIImages.getImageDescriptor(TFSCommonUIImages.IMG_COPY));
        }

        @Override
        public void doRun() {
            final StringBuffer buffer = new StringBuffer();

            for (final Iterator it = getStructuredSelection().iterator(); it.hasNext();) {
                final Changeset changeset = (Changeset) it.next();

                buffer.append(ChangesetDisplayFormatter.getIDString(changeset));
                buffer.append(TAB);

                if (historyInput.isSingleItem()) {
                    buffer.append(ChangesetDisplayFormatter.getChangeString(changeset));
                    buffer.append(TAB);
                }

                buffer.append(ChangesetDisplayFormatter.getUserString(historyInput.getRepository(), changeset));
                buffer.append(TAB);

                buffer.append(getDateString(changeset));
                buffer.append(TAB);

                buffer.append(ChangesetDisplayFormatter.getCommentString(changeset));

                if (it.hasNext()) {
                    buffer.append(NEWLINE);
                }
            }

            UIHelpers.copyToClipboard(buffer.toString());
        }

        @Override
        protected boolean computeEnablement(final IStructuredSelection selection) {
            return selection.size() > 0;
        }
    }

    @Override
    public void registerContextMenu(final IWorkbenchPartSite site) {
        site.registerContextMenu(getContextMenu(), this);
    }
}
