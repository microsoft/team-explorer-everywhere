// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.controls.vc;

import java.lang.reflect.Method;
import java.text.DateFormat;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.widgets.Composite;

import com.microsoft.tfs.client.common.repository.TFSRepository;
import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.framework.action.keybinding.ActionKeyBindingSupport;
import com.microsoft.tfs.client.common.ui.framework.action.keybinding.ActionKeyBindingSupportFactory;
import com.microsoft.tfs.client.common.ui.framework.action.keybinding.CommandIDs;
import com.microsoft.tfs.client.common.ui.framework.helper.TableViewerSorter;
import com.microsoft.tfs.client.common.ui.framework.helper.TableViewerSorter.SortDirection;
import com.microsoft.tfs.client.common.ui.framework.table.TableColumnData;
import com.microsoft.tfs.client.common.ui.framework.table.TableControl;
import com.microsoft.tfs.client.common.util.DateHelper;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Shelveset;
import com.microsoft.tfs.core.util.TSWAHyperlinkBuilder;
import com.microsoft.tfs.core.util.UserNameUtil;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.NewlineUtils;

/**
 * <p>
 * {@link ShelvesetsTable} is a control that displays a collection of
 * {@link Shelveset}s in a table.
 * </p>
 *
 * <p>
 * The supported style bits that can be used with {@link ShelvesetsTable} are
 * defined by the base class {@link TableControl}.
 * </p>
 *
 * @see Shelveset
 * @see TableControl
 */
public class ShelvesetsTable extends TableControl {
    private static final String COMMENT_COLUMN_ID = "comment"; //$NON-NLS-1$
    private static final String DATE_COLUMN_ID = "date"; //$NON-NLS-1$
    private static final String OWNER_COLUMN_ID = "owner"; //$NON-NLS-1$
    private static final String SHELVESET_NAME_COLUMN_ID = "name"; //$NON-NLS-1$
    private DateFormat dateFormat = DateHelper.getDefaultDateTimeFormat();

    private TSWAHyperlinkBuilder tswaHyperlinkBuilder;
    private IAction copyAction;
    private IAction selectAllAction;

    private ActionKeyBindingSupport actionCommandSupport;

    /**
     * Creates a new {@link ShelvesetsTable} that is initially empty. Populate
     * it by calling {@link #setShelvesets(Shelveset[])}.
     *
     * @param parent
     *        the parent of this control (must not be <code>null</code>)
     * @param style
     *        the style bits to use (see the class documentation for supported
     *        styles)
     */
    public ShelvesetsTable(final Composite parent, final int style) {
        this(parent, style, null);
    }

    /**
     * <p>
     * Creates a new {@link ShelvesetsTable} that is initially empty. Populate
     * it by calling {@link #setShelvesets(Shelveset[])}.
     * </p>
     *
     * <p>
     * This {@link ShelvesetsTable} uses the specified key to persist view data
     * such as table column widths. If the key is <code>null</code>, a default
     * key is used. Manually specifying a view data key is useful when the
     * control will be used in more than one situation, and each situation
     * should persist a separate set of view data.
     * </p>
     *
     * @param parent
     *        the parent of this control (must not be <code>null</code>)
     * @param style
     *        the style bits to use (see the class documentation for supported
     *        styles)
     * @param viewDataKey
     *        a key used to persist view data or <code>null</code> to use a
     *        default key
     */
    public ShelvesetsTable(final Composite parent, final int style, final String viewDataKey) {
        super(parent, style, Shelveset.class, viewDataKey);

        final TableColumnData[] columnData = new TableColumnData[] {
            new TableColumnData(
                Messages.getString("ShelvesetsTable.ColumnHeaderShelvesetName"), //$NON-NLS-1$
                100,
                0.33F,
                SHELVESET_NAME_COLUMN_ID),
            new TableColumnData(Messages.getString("ShelvesetsTable.ColumnHeaderOwner"), 50, 0.12F, OWNER_COLUMN_ID), //$NON-NLS-1$
            new TableColumnData(Messages.getString("ShelvesetsTable.ColumnHeaderDate"), 50, 0.15F, DATE_COLUMN_ID), //$NON-NLS-1$
            new TableColumnData(
                Messages.getString("ShelvesetsTable.ColumnHeaderComment"), //$NON-NLS-1$
                150,
                0.40F,
                COMMENT_COLUMN_ID)
        };
        setupTable(true, true, columnData);

        setUseViewerDefaults();
        setEnableTooltips(true);

        final TableViewerSorter sorter = (TableViewerSorter) getViewer().getSorter();
        sorter.setComparator(DATE_COLUMN_ID, new ShelvesetDateComparator());
        sorter.sort(DATE_COLUMN_ID, SortDirection.DESCENDING);

        createTransfers();
        createActions();
        createContextMenu();
    }

    private void createActions() {
        copyAction = new Action() {
            @Override
            public void run() {
                ShelvesetsTable.this.copySelectionToClipboard();
            }
        };
        copyAction.setText(Messages.getString("ShelvesetsTable.Copy")); //$NON-NLS-1$
        copyAction.setActionDefinitionId("org.eclipse.ui.edit.copy"); //$NON-NLS-1$

        selectAllAction = new Action() {
            @Override
            public void run() {
                ShelvesetsTable.this.selectAll();
            }
        };
        selectAllAction.setText(Messages.getString("ShelvesetsTable.SelectAll")); //$NON-NLS-1$
        selectAllAction.setActionDefinitionId(CommandIDs.SELECT_ALL);

        /*
         * IMPORTANT: this keybinding support is only appropriate for
         * dialogs/wizards. If this control is ever hosted in a view or editor
         * (or standalone in a SWT/JFace application outside the workbench) then
         * this support will need to be made optional instead of hardcoded.
         */
        actionCommandSupport = ActionKeyBindingSupportFactory.newInstance(getShell());
        actionCommandSupport.addAction(copyAction);
        actionCommandSupport.addAction(selectAllAction);

    }

    private void createContextMenu() {
        this.getContextMenu().addMenuListener(new IMenuListener() {
            @Override
            public void menuAboutToShow(final IMenuManager manager) {
                manager.add(copyAction);
            }
        });
    }

    private void createTransfers() {
        final List transferList = new ArrayList();
        transferList.add(TextTransfer.getInstance());

        /*
         * Use reflection to try to create an html transfer - new in Eclipse 3.2
         */
        Transfer htmlTransfer = null;

        try {
            final Class htmlTransferClass = Class.forName("org.eclipse.swt.dnd.HTMLTransfer"); //$NON-NLS-1$
            final Method instanceMethod = htmlTransferClass.getMethod("getInstance", new Class[0]); //$NON-NLS-1$

            htmlTransfer = (Transfer) instanceMethod.invoke(htmlTransferClass, new Object[0]);
        } catch (final Exception e) {
            /* Suppress */
        }

        if (htmlTransfer != null) {
            transferList.add(htmlTransfer);
        }

        setClipboardTransferTypes((Transfer[]) transferList.toArray(new Transfer[transferList.size()]));
    }

    @Override
    protected String getColumnText(final Object element, final String propertyName) {
        final Shelveset shelveset = (Shelveset) element;

        if (propertyName.equals(SHELVESET_NAME_COLUMN_ID)) {
            return shelveset.getName();
        } else if (propertyName.equals(OWNER_COLUMN_ID)) {
            return UserNameUtil.getName(shelveset.getOwnerDisplayName());
        } else if (propertyName.equals(DATE_COLUMN_ID)) {
            return dateFormat.format(shelveset.getCreationDate().getTime());
        } else if (propertyName.equals(COMMENT_COLUMN_ID)) {
            return shelveset.getComment();
        }

        return Messages.getString("ShelvesetsTable.UnknownColumnText"); //$NON-NLS-1$
    }

    @Override
    public String getTooltipText(final Object element, final int columnIndex) {
        final Shelveset shelveset = (Shelveset) element;

        final StringBuffer tooltip = new StringBuffer();

        tooltip.append(MessageFormat.format(
            Messages.getString("ShelvesetsTable.TooltipNameDateOwnerFormat"), //$NON-NLS-1$
            shelveset.getName(),
            dateFormat.format(shelveset.getCreationDate().getTime()),
            shelveset.getOwnerDisplayName()));

        String comment = shelveset.getComment();

        if (comment != null) {
            if (comment.length() > 500) {
                comment = MessageFormat.format("{0}...", comment.substring(0, 500)); //$NON-NLS-1$
            }

            tooltip.append(
                MessageFormat.format(
                    Messages.getString("ShelvesetsTable.TooltipAdditionalCommentLineFormat"), //$NON-NLS-1$
                    comment));
        }

        return tooltip.toString();
    }

    /**
     * Populates this {@link ShelvesetsTable} by specifying a collection of
     * {@link Shelveset}s to display.
     *
     * @param shelvesets
     *        the {@link Shelveset}s to display or <code>null</code> to empty
     *        this {@link ShelvesetsTable}
     * @param repository
     *        the {@link TFSRepository} used for the shelvesets. This is used to
     *        create any TSWA Hyperlinks etc.
     */
    public void setShelvesets(final Shelveset[] shelvesets, final TFSRepository repository) {
        setElements(shelvesets);
        if (repository != null) {
            tswaHyperlinkBuilder = new TSWAHyperlinkBuilder(repository.getVersionControlClient().getConnection());
        }
    }

    @Override
    protected Object getTransferData(final Transfer transferType, final Object[] selectedElements) {
        final Shelveset[] selectedShelvesets = (Shelveset[]) selectedElements;

        if (transferType.getClass().getName().equals("org.eclipse.swt.dnd.HTMLTransfer") //$NON-NLS-1$
            && tswaHyperlinkBuilder != null) {
            // Create HTML to copy
            final StringBuffer sb = new StringBuffer();
            for (int j = 0; j < selectedShelvesets.length; j++) {
                // TODO: Consider adding more data in the title so that
                // there is a tooltip?
                final Shelveset shelveset = selectedShelvesets[j];
                sb.append("<a href=\""); //$NON-NLS-1$
                sb.append(
                    tswaHyperlinkBuilder.getShelvesetDetailsURL(
                        shelveset.getName(),
                        shelveset.getOwnerName()).toString());
                sb.append("\">"); //$NON-NLS-1$
                sb.append(shelveset.getName());
                sb.append(";"); //$NON-NLS-1$
                sb.append(shelveset.getOwnerName());
                sb.append("</a><br/>"); //$NON-NLS-1$
                sb.append(NewlineUtils.PLATFORM_NEWLINE);
            }
            // remove the last <br/>
            sb.setLength(sb.length() - NewlineUtils.PLATFORM_NEWLINE.length() - 5);
            return sb.toString();
        }

        // Assume text transfer type
        final StringBuffer sb = new StringBuffer();
        for (int j = 0; j < selectedShelvesets.length; j++) {
            final Shelveset shelveset = selectedShelvesets[j];
            sb.append(shelveset.getName());
            sb.append(";"); //$NON-NLS-1$
            sb.append(shelveset.getOwnerName());
            sb.append(NewlineUtils.PLATFORM_NEWLINE);
        }
        // remove the last newline
        sb.setLength(sb.length() - NewlineUtils.PLATFORM_NEWLINE.length());
        return sb.toString();
    }

    /**
     * @return the {@link Shelveset}s currently displayed in this
     *         {@link ShelvesetsTable}, or an empty array if this table is
     *         currently empty
     */
    public Shelveset[] getShelvesets() {
        return (Shelveset[]) getElements();
    }

    /**
     * Sets the selected {@link Shelveset}s in this {@link ShelvesetsTable} to
     * the specified {@link Shelveset}s. {@link Shelveset}s that are not
     * contained in this {@link ShelvesetsTable} are ignored.
     *
     * @param shelvesets
     *        {@link Shelveset}s to set as selected (must not be
     *        <code>null</code>)
     */
    public void setSelectedShelvesets(final Shelveset[] shelvesets) {
        setSelectedElements(shelvesets);
    }

    /**
     * Sets the selected {@link Shelveset} in this {@link ShelvesetsTable} to
     * the specified {@link Shelveset}. If the specified {@link Shelveset} is
     * not contained in this {@link ShelvesetsTable} it is ignored.
     *
     * @param shelveset
     *        {@link Shelveset} to set as selected (must not be
     *        <code>null</code>)
     */
    public void setSelectedShelveset(final Shelveset shelveset) {
        setSelectedElement(shelveset);
    }

    /**
     * @return the currently selected {@link Shelveset}s, or an empty array if
     *         no {@link Shelveset}s are currently selected
     */
    public Shelveset[] getSelectedShelvesets() {
        return (Shelveset[]) getSelectedElements();
    }

    /**
     * @return the first of the currently selected {@link Shelveset}s, or
     *         <code>null</code> if no {@link Shelveset}s are currently selected
     */
    public Shelveset getSelectedShelveset() {
        return (Shelveset) getSelectedElement();
    }

    /**
     * @return the currently checked {@link Shelveset}s, or an empty array if no
     *         {@link Shelveset}s are currently checked
     */
    public Shelveset[] getCheckedShelvesets() {
        return (Shelveset[]) getCheckedElements();
    }

    /**
     * Sets the checked {@link Shelveset}s in this {@link ShelvesetsTable} to be
     * the specified shelvesets.
     *
     * @param shelvesets
     *        {@link Shelveset}s to check, or <code>null</code> to have no
     *        shelvesets checked
     */
    public void setCheckedShelvesets(final Shelveset[] shelvesets) {
        setCheckedElements(shelvesets);
    }

    /**
     * Removes the given {@link Shelveset}s from the table.
     *
     * @param shelvesets
     *        The {@link Shelveset}s to remove.
     */
    public void removeShelvesets(final Shelveset[] shelvesets) {
        removeElements(shelvesets);
    }

    /**
     * @return the {@link DateFormat} currently being used by this control to
     *         format {@link Shelveset} creation dates
     */
    public DateFormat getDateFormat() {
        return dateFormat;
    }

    /**
     * Sets the {@link DateFormat} used by this control to format
     * {@link Shelveset} creation dates. The default {@link DateFormat} is
     * {@link DateHelper#getDefaultDateTimeFormat()}.
     *
     * @param dateFormat
     *        a new {@link DateFormat} to use (must not be <code>null</code>)
     */
    public void setDateFormat(final DateFormat dateFormat) {
        Check.notNull(dateFormat, "dateFormat"); //$NON-NLS-1$

        this.dateFormat = dateFormat;
        getViewer().refresh();
    }

    private class ShelvesetDateComparator implements Comparator {
        @Override
        public int compare(final Object one, final Object two) {
            return ((Shelveset) one).getCreationDate().getTime().compareTo(
                ((Shelveset) two).getCreationDate().getTime());
        }
    }
}
