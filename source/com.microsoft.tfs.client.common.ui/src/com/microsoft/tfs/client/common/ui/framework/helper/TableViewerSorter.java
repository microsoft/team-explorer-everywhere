// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.framework.helper;

import java.lang.reflect.Method;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;

import com.microsoft.tfs.client.common.ui.framework.table.TableViewerUtils;
import com.microsoft.tfs.util.Check;

/**
 * <p>
 * A {@link ViewerSorter} implementation that can be used as a general purpose
 * sorter for {@link TableViewer}s.
 * </p>
 *
 * <p>
 * Like {@link ViewerSorter}, this class performs a 2-level sort. The primary
 * sort is by category. The default category for all elements is the same (
 * <code>0</code>). To specify categories, call the
 * {@link #setCategoryProvider(CategoryProvider)} method. The secondary sort is
 * a column-specific sort on the elements. The default behavior is to compare
 * the labels returned by the viewer's label provider. This default behavior can
 * be overridden on a per-column basis by calling
 * {@link #setComparator(int, Comparator)}.
 * </p>
 *
 * <p>
 * This class adds listeners to the SWT {@link Table}'s columns. When a column
 * header is clicked, the {@link #sort(int)} method is called with the column's
 * index. This class performs a multi-level sort. Each time a column is clicked,
 * that column moves to the top of the sort stack. If the column was previously
 * at the top of the sort stack, its sort direction is reversed. If the column
 * was in the sort stack but not at the top, its existing sort direction is
 * preserved. Otherwise, a default sort direction is used. The default sort
 * direction can be overridden on a per-column basis by calling
 * {@link #setDefaultSortDirection(int, SortDirection)}.
 * </p>
 *
 * @see ViewerSorter
 */
public class TableViewerSorter extends ViewerSorter {
    /**
     * {@link CategoryProvider} is used to provide category values for elements
     * being sorted by a {@link TableViewerSorter}. The category values are used
     * as the inputs to a first-level sort.
     */
    public interface CategoryProvider {
        /**
         * Returns the category of the specified element.
         *
         * @see ViewerSorter#category(Object)
         *
         * @param element
         *        an element to get a category for
         */
        public int getCategory(Object element);
    }

    /**
     * {@link SortDirection} tracks the two sort directions that are possible
     * for a column.
     */
    public static class SortDirection {
        /**
         * The ascending sort direction - this is consistent with the natural
         * ordering of the elements.
         */
        public static final SortDirection ASCENDING = new SortDirection("ASC"); //$NON-NLS-1$

        /**
         * The descending sort direction - this is the opposite of the natural
         * ordering of the elements.
         */
        public static final SortDirection DESCENDING = new SortDirection("DESC"); //$NON-NLS-1$

        private final String type;

        private SortDirection(final String type) {
            this.type = type;
        }

        @Override
        public String toString() {
            return type;
        }
    }

    /**
     * Inverts the specified {@link SortDirection}.
     *
     * @param in
     *        a {@link SortDirection} to invert (must not be <code>null</code>)
     * @return the inverse of the input {@link SortDirection}
     */
    public static SortDirection invert(final SortDirection in) {
        Check.notNull(in, "in"); //$NON-NLS-1$

        return SortDirection.ASCENDING == in ? SortDirection.DESCENDING : SortDirection.ASCENDING;
    }

    /**
     * The {@link TableViewer} that this {@link TableViewerSorter} is sorting.
     */
    private final TableViewer viewer;

    /**
     * Keeps track of default sort directions that are set through
     * {@link #setDefaultSortDirection(int, SortDirection)}. The keys are
     * {@link Integer}s (column indices) and the values are
     * {@link SortDirection}s.
     */
    private final Map defaultSortDirections = new HashMap();

    /**
     * Keeps track of column comparators that are set through
     * {@link #setComparator(int, Comparator)}. The keys are {@link Integer}s
     * (column indices) and the values are {@link Comparator}s.
     */
    private final Map comparators = new HashMap();

    /**
     * A stack that keeps track of current sort data. The elements are
     * {@link ColumnSortData}s. The {@link ColumnSortData} at the top of the
     * stack (index <code>0</code>) has the greatest priority.
     */
    private final List currentSortData = new ArrayList();

    /**
     * Initial column to sort on. (This is not pushed onto currentSortData
     * stack.)
     */
    private final int initialSortColumn;
    private final SortDirection initialSortDirection;

    /**
     * The {@link CategoryProvider} that is set through
     * {@link #setCategoryProvider(CategoryProvider)}. May be <code>null</code>
     * if no {@link CategoryProvider} has been set.
     */
    private CategoryProvider categoryProvider;

    /**
     * Creates a new {@link TableViewerSorter} for the specified
     * {@link TableViewer}. The sorter uses a default {@link Collator}. The
     * table will initially be sorted on the first column, ascending.
     *
     * @param viewer
     *        a {@link TableViewer} to sort (must not be <code>null</code>)
     */
    public TableViewerSorter(final TableViewer viewer) {
        this(viewer, null);
    }

    /**
     * Creates a new {@link TableViewerSorter} for the specified
     * {@link TableViewer}. The specified {@link Collator} is used. The table
     * will initially be sorted on the first column, ascending.
     *
     * @param viewer
     *        a {@link TableViewer} to sort (must not be <code>null</code>)
     * @param collator
     *        a {@link Collator} to use, or <code>null</code> to use a default
     *        {@link Collator}
     */
    public TableViewerSorter(final TableViewer viewer, final Collator collator) {
        this(viewer, collator, 0, null);
    }

    /**
     * Creates a new {@link TableViewerSorter} for the specified
     * {@link TableViewer}. The specified {@link Collator} is used. The table
     * will initially be sorted on the specified column in the specified
     * direction.
     *
     * @param viewer
     *        a {@link TableViewer} to sort (must not be <code>null</code>)
     * @param collator
     *        a {@link Collator} to use, or <code>null</code> to use a default
     *        {@link Collator}
     * @param initialColumn
     *        the initial column index to sort on
     * @param initialDirection
     *        a {@link SortDirection} to initially sort the table, or null to
     *        use the column sort default
     */
    public TableViewerSorter(
        final TableViewer viewer,
        final Collator collator,
        final int initialColumn,
        final SortDirection initialDirection) {
        super(collator == null ? Collator.getInstance() : collator);

        Check.notNull(viewer, "viewer"); //$NON-NLS-1$
        this.viewer = viewer;
        initialSortColumn = initialColumn;
        initialSortDirection = initialDirection;

        final TableColumn[] columns = viewer.getTable().getColumns();
        for (int i = 0; i < columns.length; i++) {
            final int columnIndex = i;
            columns[i].addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(final SelectionEvent e) {
                    sort(columnIndex);
                }
            });
        }
    }

    /**
     * Sets a {@link CategoryProvider} for this {@link TableViewerSorter} to
     * use. The specified {@link CategoryProvider} gives category values for
     * elements being sorted. The category values are used to perform a
     * first-level sort before any column-wise sort is considered.
     *
     * @param categoryProvider
     *        a {@link CategoryProvider} to use, or <code>null</code> to use
     *        this classes default categorization
     */
    public void setCategoryProvider(final CategoryProvider categoryProvider) {
        this.categoryProvider = categoryProvider;
    }

    /**
     * @return the {@link CategoryProvider} currently in use by this
     *         {@link TableViewerSorter}, or <code>null</code> if no
     *         {@link CategoryProvider} has been set
     */
    public CategoryProvider getCategoryProvider() {
        return categoryProvider;
    }

    /**
     * Sets a {@link Comparator} to use when sorting by the specified column
     * index. The default behavior is to sort the labels returned for that
     * column by the viewer's label provider. For some values (numbers, dates,
     * etc) a {@link Comparator} provided by the client is more appropriate.
     *
     * @param columnIndex
     *        the column index to provide a {@link Comparator} for
     * @param comparator
     *        a {@link Comparator} to use for the specified column index, or
     *        <code>null</code> to use the default sorting behavior for that
     *        column
     */
    public void setComparator(final int columnIndex, final Comparator comparator) {
        final Integer key = new Integer(columnIndex);

        if (comparator == null) {
            comparators.remove(key);
        } else {
            comparators.put(key, comparator);
        }
    }

    /**
     * <p>
     * Sets a {@link Comparator} to use when sorting the specified column
     * (identified by the JFace column property name). The default behavior is
     * to sort the labels returned for that column by the viewer's label
     * provider. For some values (numbers, dates, etc) a {@link Comparator}
     * provided by the client is more appropriate.
     * </p>
     *
     * <p>
     * If the specified column property name is not found, or if column property
     * names have not been defined on the JFace {@link TableViewer} in use, an
     * exception is thrown.
     * </p>
     *
     * @param columnPropertyName
     *        identifies the column to provide a comparator for
     * @param comparator
     *        a {@link Comparator} to use for the specified column, or
     *        <code>null</code> to use the default sorting behavior for that
     *        column
     */
    public void setComparator(final String columnPropertyName, final Comparator comparator) {
        final int columnIndex = TableViewerUtils.columnPropertyNameToColumnIndex(columnPropertyName, true, viewer);
        setComparator(columnIndex, comparator);
    }

    /**
     * Returns the {@link Comparator} currently being used to sort the specified
     * column index.
     *
     * @param columnIndex
     *        the column index to get a {@link Comparator} for
     * @return a {@link Comparator}, or <code>null</code> if no
     *         {@link Comparator} has been set for the specified column
     */
    public Comparator getComparator(final int columnIndex) {
        return (Comparator) comparators.get(new Integer(columnIndex));
    }

    /**
     * <p>
     * Returns the {@link Comparator} currently being used to sort the specified
     * column (identified by the JFace column property name).
     * </p>
     *
     * <p>
     * If the specified column property name is not found, or if column property
     * names have not been defined on the JFace {@link TableViewer} in use, an
     * exception is thrown.
     * </p>
     *
     * @param columnPropertyName
     *        identifies the column to provide a comparator for
     * @return a {@link Comparator}, or <code>null</code> if no
     *         {@link Comparator} has been set for the specified column
     */
    public Comparator getComparator(final String columnPropertyName) {
        final int columnIndex = TableViewerUtils.columnPropertyNameToColumnIndex(columnPropertyName, true, viewer);
        return getComparator(columnIndex);
    }

    /**
     * Sets the default {@link SortDirection} to use for the specified column
     * index. If no default {@link SortDirection} is set for a column,
     * {@link SortDirection#ASCENDING} is used. The default
     * {@link SortDirection} is used when sorting a column for the first time.
     *
     * @param columnIndex
     *        the column index to set a default sort direction for
     * @param sortDirection
     *        the default {@link SortDirection} to use for the specified column,
     *        or <code>null</code> to use {@link SortDirection#ASCENDING}
     */
    public void setDefaultSortDirection(final int columnIndex, final SortDirection sortDirection) {
        final Integer key = new Integer(columnIndex);

        if (sortDirection == null) {
            defaultSortDirections.remove(key);
        } else {
            defaultSortDirections.put(key, sortDirection);
        }
    }

    /**
     * <p>
     * Sets the default {@link SortDirection} to use for the specified column.
     * If no default {@link SortDirection} is set for a column,
     * {@link SortDirection#ASCENDING} is used. The default
     * {@link SortDirection} is used when sorting a column for the first time.
     * </p>
     *
     * <p>
     * If the specified column property name is not found, or if column property
     * names have not been defined on the JFace {@link TableViewer} in use, an
     * exception is thrown.
     * </p>
     *
     * @param columnPropertyName
     *        identifies the column to set a default sort direction for
     * @param sortDirection
     *        the default {@link SortDirection} to use for the specified column,
     *        or <code>null</code> to use {@link SortDirection#ASCENDING}
     */
    public void setDefaultSortDirection(final String columnPropertyName, final SortDirection sortDirection) {
        final int columnIndex = TableViewerUtils.columnPropertyNameToColumnIndex(columnPropertyName, true, viewer);
        setDefaultSortDirection(columnIndex, sortDirection);
    }

    /**
     * Returns the default {@link SortDirection} for the specified column index.
     * The default will be {@link SortDirection#ASCENDING} if no default has
     * been explicitly set through
     * {@link #setDefaultSortDirection(int, SortDirection)}.
     *
     * @param columnIndex
     *        the column index to get the default sort direction for
     * @return the default {@link SortDirection} for the specified column (never
     *         <code>null</code>)
     */
    public SortDirection getDefaultSortDirection(final int columnIndex) {
        SortDirection sortDirection = (SortDirection) defaultSortDirections.get(new Integer(columnIndex));
        if (sortDirection == null) {
            sortDirection = SortDirection.ASCENDING;
        }
        return sortDirection;
    }

    /**
     * <p>
     * Returns the default {@link SortDirection} for the specified column. The
     * default will be {@link SortDirection#ASCENDING} if no default has been
     * explicitly set through
     * {@link #setDefaultSortDirection(int, SortDirection)}.
     * </p>
     *
     * <p>
     * If the specified column property name is not found, or if column property
     * names have not been defined on the JFace {@link TableViewer} in use, an
     * exception is thrown.
     * </p>
     *
     * @param columnPropertyName
     *        identifies the column to get the default sort direction for
     * @return the default {@link SortDirection} for the specified column (never
     *         <code>null</code>)
     */
    public SortDirection getDefaultSortDirection(final String columnPropertyName) {
        final int columnIndex = TableViewerUtils.columnPropertyNameToColumnIndex(columnPropertyName, true, viewer);
        return getDefaultSortDirection(columnIndex);
    }

    /**
     * Performs a sort. Calling this method is equivalent to clicking on the
     * column with the specified index.
     *
     * @param columnIndex
     *        the column to sort
     */
    public void sort(final int columnIndex) {
        internalSort(columnIndex, null);
        viewer.refresh();
    }

    /**
     * <p>
     * Performs a sort. Calling this method is equivalent to clicking on the
     * column with the specified index.
     * </p>
     *
     * <p>
     * If the specified column property name is not found, or if column property
     * names have not been defined on the JFace {@link TableViewer} in use, an
     * exception is thrown.
     * </p>
     *
     * @param columnPropertyName
     *        identifies the column to sort
     */
    public void sort(final String columnPropertyName) {
        final int columnIndex = TableViewerUtils.columnPropertyNameToColumnIndex(columnPropertyName, true, viewer);
        internalSort(columnIndex, null);
        viewer.refresh();
    }

    /**
     * Performs a sort, specifying the sort type to use. If the specified column
     * is already in the sort stack, its existing {@link SortDirection} will be
     * ignored. In addition, any default sort direction set through
     * {@link #setDefaultSortDirection(int, SortDirection)} will be ignored.
     *
     * @param columnIndex
     *        the column to sort
     * @param sortDirection
     *        the {@link SortDirection} to use, or <code>null</code> to use the
     *        default behavior
     */
    public void sort(final int columnIndex, final SortDirection sortDirection) {
        internalSort(columnIndex, sortDirection);
        viewer.refresh();
    }

    /**
     * <p>
     * Performs a sort, specifying the sort type to use. If the specified column
     * is already in the sort stack, its existing {@link SortDirection} will be
     * ignored. In addition, any default sort direction set through
     * {@link #setDefaultSortDirection(int, SortDirection)} will be ignored.
     * </p>
     *
     * <p>
     * If the specified column property name is not found, or if column property
     * names have not been defined on the JFace {@link TableViewer} in use, an
     * exception is thrown.
     * </p>
     *
     * @param columnPropertyName
     *        identifies the column to sort
     * @param sortDirection
     *        the {@link SortDirection} to use, or <code>null</code> to use the
     *        default behavior
     */
    public void sort(final String columnPropertyName, final SortDirection sortDirection) {
        final int columnIndex = TableViewerUtils.columnPropertyNameToColumnIndex(columnPropertyName, true, viewer);
        internalSort(columnIndex, sortDirection);
        viewer.refresh();
    }

    /**
     * Performs a sort, specifying the sort type to use. If the specified column
     * is already in the sort stack, its existing {@link SortDirection} will be
     * ignored. In addition, any default sort direction set through
     * {@link #setDefaultSortDirection(int, SortDirection)} will be ignored.
     *
     * This does not refresh the table so is suitable for use before the table
     * is painted.
     *
     * @param columnIndex
     *        the column to sort
     * @param sortDirection
     *        the {@link SortDirection} to use, or <code>null</code> to use the
     *        default behavior
     */
    private void internalSort(final int columnIndex, SortDirection sortDirection) {
        int currentIx = 0;

        for (final Iterator it = currentSortData.iterator(); it.hasNext();) {
            final ColumnSortData sortData = (ColumnSortData) it.next();
            if (sortData.getColumnIndex() == columnIndex) {
                if (sortDirection == null) {
                    sortDirection = sortData.getSortDirection();
                    if (currentIx == 0) {
                        sortDirection = invert(sortDirection);
                    }
                }

                it.remove();
                break;
            }

            ++currentIx;
        }

        if (sortDirection == null) {
            sortDirection = (SortDirection) defaultSortDirections.get(new Integer(columnIndex));
            if (sortDirection == null) {
                sortDirection = SortDirection.ASCENDING;
            }
        }

        final ColumnSortData newSortData = new ColumnSortData(columnIndex, sortDirection);
        currentSortData.add(0, newSortData);

        setTableSortDecoration(viewer.getTable(), columnIndex, sortDirection);
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * org.eclipse.jface.viewers.ViewerComparator#compare(org.eclipse.jface.
     * viewers.Viewer, java.lang.Object, java.lang.Object)
     */
    @Override
    public int compare(final Viewer viewer, final Object e1, final Object e2) {
        /*
         * First pass, setup the default sort. We do this here (instead of, say,
         * a ctor) so that initialSortDirection can be null, and we want to give
         * consumers time to set a default sort direction. We don't do it in a
         * paint listener, as the rows are laid out before a paint.
         */
        if (currentSortData.size() == 0 && initialSortColumn >= 0) {
            internalSort(initialSortColumn, initialSortDirection);
        }

        int val = categoryCompare(e1, e2);

        if (val != 0) {
            return val;
        }

        for (final Iterator it = currentSortData.iterator(); it.hasNext();) {
            final ColumnSortData columnSortData = (ColumnSortData) it.next();
            val = columnCompare(columnSortData.getColumnIndex(), columnSortData.getSortDirection(), viewer, e1, e2);
            if (val != 0) {
                return val;
            }
        }

        return 0;
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        final StringBuffer buffer = new StringBuffer("TableViewerSorter"); //$NON-NLS-1$

        for (final Iterator it = currentSortData.iterator(); it.hasNext();) {
            final ColumnSortData columnSortData = (ColumnSortData) it.next();
            buffer.append(" "); //$NON-NLS-1$
            buffer.append(columnSortData.getColumnIndex());
            buffer.append("("); //$NON-NLS-1$
            buffer.append(columnSortData.getSortDirection());
            buffer.append(")"); //$NON-NLS-1$
            if (it.hasNext()) {
                buffer.append(" ->"); //$NON-NLS-1$
            }
        }

        return buffer.toString();
    }

    /**
     * Performs a column-wise (second-level, after categories) comparison of two
     * elements. Subclasses may override this method. The base class
     * implementation first checks whether a {@link Comparator} has been set for
     * the specified column index. If so, that {@link Comparator} is used to
     * compute the return value. Otherwise, the
     * {@link #defaultColumnCompare(int, SortDirection, Viewer, Object, Object)}
     * method is used to compute the return value.
     *
     * @param columnIndex
     *        the index of the column being sorted by
     * @param sortDirection
     *        the {@link SortDirection} to use
     * @param viewer
     *        the {@link Viewer} we are sorting for
     * @param e1
     *        the first element
     * @param e2
     *        the second element
     * @return a comparison value
     */
    protected int columnCompare(
        final int columnIndex,
        final SortDirection sortDirection,
        final Viewer viewer,
        final Object e1,
        final Object e2) {
        int value;

        final Comparator comparator = getComparator(columnIndex);

        if (comparator != null) {
            value = comparator.compare(e1, e2);
        } else {
            value = defaultColumnCompare(columnIndex, viewer, e1, e2);
        }

        if (value != 0 && SortDirection.DESCENDING == sortDirection) {
            value = (value > 0) ? -1 : 1;
        }

        return value;
    }

    /**
     * Performs a default column-wise comparison of two elements. This method is
     * called after checking for any {@link Comparator} set through
     * {@link #setComparator(int, Comparator)}. Subclasses may override. The
     * base class implementation uses this sorter's {@link Collator} to compare
     * the column text for each element provided by the viewer's label provider.
     *
     * @param columnIndex
     *        the index of the column being sorted by
     * @param viewer
     *        the {@link Viewer} we are sorting for
     * @param e1
     *        the first element
     * @param e2
     *        the second element
     * @return a comparison value
     */
    protected int defaultColumnCompare(final int columnIndex, final Viewer viewer, final Object e1, final Object e2) {
        final ITableLabelProvider labelProvider = (ITableLabelProvider) ((StructuredViewer) viewer).getLabelProvider();

        String s1 = labelProvider.getColumnText(e1, columnIndex);
        String s2 = labelProvider.getColumnText(e2, columnIndex);

        if (s1 == null) {
            s1 = ""; //$NON-NLS-1$
        }
        if (s2 == null) {
            s2 = ""; //$NON-NLS-1$
        }

        return getCollator().compare(s1, s2);
    }

    /**
     * Performs a category-wise (first level) comparison of two elements.
     * Subclasses may override. The base class implementation first gets a
     * category value for each element and then compares the two category values
     * in numeric order. The category values are either provided by a
     * {@link CategoryProvider} or by the default categorization provided by
     * this class (the {@link #category(Object)} method).
     *
     * @param e1
     *        the first element
     * @param e2
     *        the second element
     * @return a comparison value
     */
    protected int categoryCompare(final Object e1, final Object e2) {
        final int cat1 = categoryProvider != null ? categoryProvider.getCategory(e1) : category(e1);
        final int cat2 = categoryProvider != null ? categoryProvider.getCategory(e2) : category(e2);

        if (cat1 == cat2) {
            return 0;
        }

        return cat1 > cat2 ? 1 : -1;
    }

    /**
     * Called to set decoration on the table to indicate a sort. Subclasses may
     * override. The base class implementation calls the
     * {@link #setSortColumnAndDirection(Table, TableColumn, int)} method with
     * appropriate values.
     *
     * @param table
     *        the {@link Table} being sorted
     * @param columnIndex
     *        the index of the column that was sorted
     * @param sortDirection
     *        the direction of the sort
     */
    protected void setTableSortDecoration(final Table table, final int columnIndex, final SortDirection sortDirection) {
        final TableColumn sortColumn = table.getColumn(columnIndex);

        /*
         * If the column has an image, we don't want to decorate it with the
         * sort image. Windows (and possibly other platforms) only support one
         * image per column, so the sort image would hide the normal image.
         *
         * In this case, we still need to set the sort column to null and
         * direction to none to remove any existing sort decoration on a
         * different column.
         */
        if (sortColumn.getImage() != null) {
            setSortColumnAndDirection(table, null, SWT.NONE);
            return;
        }

        final int direction = SortDirection.ASCENDING == sortDirection ? SWT.UP : SWT.DOWN;
        setSortColumnAndDirection(table, sortColumn, direction);
    }

    /**
     * Called to set the column and direction decorations on the specified
     * {@link Table}. Subclasses may override. The base class implementation
     * uses reflection to attempt to call the <code>setSortColumn</code> and
     * <code>setSortDirection</code> methods that were added in Eclipse 3.2.
     *
     * @param table
     *        the {@link Table} to decorate
     * @param sortColumn
     *        the column that should be marked as the sort column
     * @param direction
     *        the direction ({@link SWT#UP} or {@link SWT#DOWN}) to use in the
     *        decoration
     */
    protected void setSortColumnAndDirection(final Table table, final TableColumn sortColumn, final int direction) {
        try {
            final Class tableClass = table.getClass();

            final Method setSortColumn = tableClass.getMethod("setSortColumn", new Class[] //$NON-NLS-1$
            {
                TableColumn.class
            });
            final Method setSortDirection = tableClass.getMethod("setSortDirection", new Class[] //$NON-NLS-1$
            {
                Integer.TYPE
            });

            setSortColumn.invoke(table, new Object[] {
                sortColumn
            });
            setSortDirection.invoke(table, new Object[] {
                new Integer(direction)
            });
        } catch (final Exception e) {

        }
    }

    /**
     * A private class used as elements on the sort stack. Each instance
     * represents a column and a sort direction.
     */
    private static class ColumnSortData {
        private final int columnIndex;
        private final SortDirection sortDirection;

        public ColumnSortData(final int columnIndex, final SortDirection sortDirection) {
            this.columnIndex = columnIndex;
            this.sortDirection = sortDirection;
        }

        public int getColumnIndex() {
            return columnIndex;
        }

        public SortDirection getSortDirection() {
            return sortDirection;
        }
    }
}
