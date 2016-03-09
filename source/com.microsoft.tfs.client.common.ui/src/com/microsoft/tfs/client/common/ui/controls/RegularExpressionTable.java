// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.controls;

import java.text.MessageFormat;
import java.util.Comparator;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.eclipse.jface.viewers.ICellModifier;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Item;
import org.eclipse.swt.widgets.MessageBox;

import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.framework.helper.TableViewerSorter;
import com.microsoft.tfs.client.common.ui.framework.table.TableColumnData;
import com.microsoft.tfs.client.common.ui.framework.table.TableControl;
import com.microsoft.tfs.client.common.ui.framework.viewer.ElementEvent;
import com.microsoft.tfs.util.Check;

/**
 * An in-place editable table of regular expressions.
 */
public class RegularExpressionTable extends TableControl {
    private static final String EXPRESSION_COLUMN_NAME = "expression"; //$NON-NLS-1$

    private final int compileFlags;

    public RegularExpressionTable(final Composite parent, final int style, final int regluarExpressionCompileFlags) {
        this(parent, style, regluarExpressionCompileFlags, null);
    }

    public RegularExpressionTable(
        final Composite parent,
        final int style,
        final int regluarExpressionCompileFlags,
        final String viewDataKey) {
        super(parent, style, RegularExpressionTableData.class, viewDataKey);

        compileFlags = regluarExpressionCompileFlags;

        final TableColumnData[] columnData = new TableColumnData[] {
            new TableColumnData(
                Messages.getString("RegularExpressionTable.ColumnNameExpression"), //$NON-NLS-1$
                300,
                EXPRESSION_COLUMN_NAME),
        };

        setupTable(true, false, columnData);

        setUseViewerDefaults();
        setEnableTooltips(false);

        final TableViewerSorter sorter = (TableViewerSorter) getViewer().getSorter();

        sorter.setComparator(EXPRESSION_COLUMN_NAME, new Comparator() {
            @Override
            public int compare(final Object o1, final Object o2) {
                return String.CASE_INSENSITIVE_ORDER.compare(
                    ((RegularExpressionTableData) o1).getExpression(),
                    ((RegularExpressionTableData) o2).getExpression());
            }
        });

        setCellEditor(EXPRESSION_COLUMN_NAME, new TextCellEditor(getTable()));
        getViewer().setCellModifier(new CellModifier(this));
    }

    public void setExpressions(final RegularExpressionTableData[] expressions) {
        setElements(expressions);
    }

    public RegularExpressionTableData[] getExpressions() {
        return (RegularExpressionTableData[]) getElements();
    }

    public void setSelectedExpressions(final RegularExpressionTableData[] expressions) {
        setSelectedElements(expressions);
    }

    public void setSelectedExpression(final RegularExpressionTableData expression) {
        setSelectedElement(expression);
    }

    public RegularExpressionTableData[] getSelectedExpressions() {
        return (RegularExpressionTableData[]) getSelectedElements();
    }

    public RegularExpressionTableData getSelectedExpression() {
        return (RegularExpressionTableData) getSelectedElement();
    }

    @Override
    protected String getColumnText(final Object element, final String columnPropertyName) {
        final RegularExpressionTableData tableData = (RegularExpressionTableData) element;

        if (EXPRESSION_COLUMN_NAME.equals(columnPropertyName)) {
            return tableData.getExpression();
        }

        return null;
    }

    /**
     * Tests whether the given expression string is a valid expression for
     * inclusion in this table. This is provided so other controls can validate
     * input before inserting into the table using the same rules this table
     * uses for cell modification.
     * <p>
     * Empty strings are not valid.
     *
     * @param expressionString
     *        the expression string to validate (not null).
     * @return true if the string is a valid regular expression, false if it is
     *         not.
     */
    public boolean isValidRegularExpression(final String expressionString) {
        Check.notNull(expressionString, "expressionString"); //$NON-NLS-1$

        if (expressionString.length() == 0) {
            return false;
        }

        try {
            Pattern.compile(expressionString, compileFlags);
            return true;
        } catch (final PatternSyntaxException e) {
            return false;
        }
    }

    /**
     * A hack so the modifier ({@link CellModifier}) can force the
     * {@link ElementEvent} to fire.
     */
    protected void forceComputeElelements() {
        computeElements();
    }

    private static class CellModifier implements ICellModifier {
        private final RegularExpressionTable table;

        public CellModifier(final RegularExpressionTable table) {
            this.table = table;
        }

        @Override
        public boolean canModify(final Object element, final String property) {
            return EXPRESSION_COLUMN_NAME.equals(property);
        }

        @Override
        public Object getValue(final Object element, final String property) {
            return ((RegularExpressionTableData) element).getExpression();
        }

        @Override
        public void modify(final Object element, final String property, final Object value) {
            final RegularExpressionTableData tableData =
                (RegularExpressionTableData) (element instanceof Item ? ((Item) element).getData() : element);

            final String newText = (String) value;

            /*
             * Disallow the edit if the string is not a valid regular
             * expression.
             */
            if (table.isValidRegularExpression(newText) == false) {
                final String messageFormat = Messages.getString("RegularExpressionTable.NotValidRegexFormat"); //$NON-NLS-1$
                final String message = MessageFormat.format(messageFormat, newText);

                final MessageBox mb = new MessageBox(table.getShell());
                mb.setMessage(message);
                mb.open();

                return;
            }

            tableData.setExpression(newText);

            table.getViewer().update(tableData, new String[] {
                property
            });

            /*
             * This is a hack. Calling forceComputeElements() causes the
             * ElementEvent to fire so controls that have a
             * RegularExpressionTable can track in-place column edits.
             */
            table.forceComputeElelements();
        }
    }
}
