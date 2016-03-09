// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.framework.table;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.osgi.service.prefs.BackingStoreException;
import org.osgi.service.prefs.Preferences;

import com.microsoft.tfs.client.common.ui.TFSCommonUIClientPlugin;
import com.microsoft.tfs.util.Check;

/**
 * <p>
 * {@link TableColumnWidthsPersistence} implements persistence for an
 * <code>SWT</code> {@link Table}'s column widths. When a {@link Table} is
 * disposed, the current column widths are persisted. The next time the
 * {@link Table} is shown, the persisted widths can be restored.
 * </p>
 *
 * <p>
 * To use this class, you must first create an instance of it, specifying an
 * existing <code>SWT</code> {@link Table} to track and a {@link String} used as
 * a persistence key:
 *
 * <pre>
 * Table table = ...
 * TableColumnWidthsPersistence persistence =
 *    new TableColumnWidthsPersistence(table, &quot;MyKey&quot;);
 * </pre>
 *
 * You must then add mappings to the {@link TableColumnWidthsPersistence}
 * object. Mappings specify which columns in the {@link Table} to track. Columns
 * that are not specified by mappings are ignored. A mapping is from a
 * {@link String} key to an <code>int</code> column index:
 *
 * <pre>
 * persistence.addMapping(&quot;ColumnX&quot;, 0);
 * persistence.addMapping(&quot;ColumnY&quot;, 1);
 * </pre>
 *
 * The mapping mechanism allows you to add, remove, or reorder a {@link Table}'s
 * columns and remain compatible with previously persisted width data. The
 * mapping mechanism also allows you to selectively persist widths for only some
 * of the {@link Table}'s columns. The mapping keys can be anything you like -
 * they are not shown in the UI.
 * </p>
 *
 * <p>
 * As a shortcut, you can add implicit mappings for a {@link Table}. This is not
 * recommended, but could be useful for prototype code. Implicit mappings are
 * added by calling:
 *
 * <pre>
 * persistence.addDefaultMappings();
 * </pre>
 *
 * This method adds a mapping for each column in the {@link Table}, using that
 * column's name as the mapping key.
 * </p>
 *
 * <p>
 * Once the {@link TableColumnWidthsPersistence} object has been set up with
 * mappings, you can restore any previously persisted column widths:
 *
 * <pre>
 * persistence.restore();
 * </pre>
 *
 * </p>
 *
 * <p>
 * When the {@link Table} is disposed, the column widths corresponding to the
 * mappings with be persisted automatically. If needed, you can explicitly
 * persist the widths:
 *
 * <pre>
 * persistence.persist();
 * </pre>
 *
 * </p>
 *
 * <p>
 * You should normally create a {@link TableColumnWidthsPersistence} object
 * after adding all columns to a {@link Table}. If you need to add columns to a
 * {@link Table} after a {@link TableColumnWidthsPersistence} instance has been
 * created, you must call a method to add listeners to the new columns:
 *
 * <pre>
 * persistence.hookColumns();
 * </pre>
 *
 * </p>
 *
 * <p>
 * When persisting or restoring column widths, if a mapping is encountered that
 * does not correspond to an existing column index, a warning will be logged.
 * When restoring column widths only persisted widths that correspond to current
 * mappings will be considered.
 * </p>
 */
public class TableColumnWidthsPersistence {
    /**
     * The static preference node name that we use as a first-level key for all
     * data persisted by this class.
     */
    private static final String TABLE_COLUMN_WIDTHS_NODE_NAME = "table-column-widths"; //$NON-NLS-1$

    private static final Log log = LogFactory.getLog(TableColumnWidthsPersistence.class);

    /**
     * Stores the mappings. The keys are {@link String}s and the values are
     * <code>int</code>s.
     */
    private final Map mappings = new HashMap();

    /**
     * The instance key for this {@link TableColumnWidthsPersistence}. Used as
     * second-level key for persisted data.
     */
    private final String key;

    /**
     * The current {@link Table} column widths. Every time a column is resized,
     * we update this array.
     */
    private int[] tableColumnWidths;

    /**
     * The {@link Table} this instance is tracking.
     */
    private final Table table;

    /**
     * The {@link ControlListener} we add to table columns to catch resizes.
     */
    private final ControlListener tableColumnResizedListener = new TableColumnResizedListener();

    /**
     * Create a new {@link TableColumnWidthsPersistence} object. The given
     * {@link Table} is immediately hooked by calling {@link #hookColumns()}.
     *
     * @param table
     *        Table to restore and persist widths for
     * @param key
     *        the instance-specific key used for the persistence
     */
    public TableColumnWidthsPersistence(final Table table, final String key) {
        Check.notNull(table, "table"); //$NON-NLS-1$

        this.table = table;
        this.key = key;

        hookColumns();
        table.addDisposeListener(new TableDisposedListener());
    }

    /**
     * <p>
     * Hooks the columns of the {@link Table} associated with this object. This
     * hooking is neccessary so that initial values and changes to the widths of
     * the columns can be recorded.
     * </p>
     * <p>
     * This method is called automatically in the contructor of this object. If
     * table columns are added or removed, it must be manually called again.
     * </p>
     */
    public void hookColumns() {
        final TableColumn[] columns = table.getColumns();
        tableColumnWidths = new int[columns.length];

        for (int i = 0; i < columns.length; i++) {
            columns[i].addControlListener(tableColumnResizedListener);
            tableColumnWidths[i] = columns[i].getWidth();
        }
    }

    /**
     * Adds a mapping. See the javadoc above for comments on the mapping
     * mechanism. Any existing mapping with the same key will be overwritten.
     *
     * @param key
     *        the mapping key, must not be null
     * @param index
     *        the column index associated with the key
     * @throws IllegalArgumentException
     *         if an existing mapping has the same index but a different key
     */
    public void addMapping(final String key, final int index) {
        Check.notNull(key, "key"); //$NON-NLS-1$

        final Integer value = new Integer(index);

        if (mappings.containsValue(value)) {
            for (final Iterator it = mappings.keySet().iterator(); it.hasNext();) {
                final String currentKey = (String) it.next();
                if (mappings.get(currentKey).equals(value)) {
                    if (currentKey.equals(key)) {
                        /*
                         * Duplicate addition of the same key and value. Do
                         * nothing.
                         */
                        return;
                    }

                    final String messageFormat = "the index [{0}] is already mapped to key [{1}]"; //$NON-NLS-1$
                    final String message = MessageFormat.format(messageFormat, Integer.toString(index), currentKey);
                    throw new IllegalArgumentException(message);
                }
            }
        }

        mappings.put(key, value);
    }

    /**
     * Creates a default set of mappings by using each {@link TableColumn}'s
     * label (returned by {@link TableColumn#getText()}) as that column's key.
     * If a column has no text (empty or whitespace text), then no mapping will
     * be added for that column.
     */
    public void addDefaultMappings() {
        final TableColumn[] tableColumns = table.getColumns();
        for (int i = 0; i < tableColumns.length; i++) {
            final String key = tableColumns[i].getText().trim();
            if (key.length() > 0) {
                addMapping(key, i);
            }
        }
    }

    /**
     * Persists table column widths. Users of this class should have already
     * added some mappings before calling this method.
     */
    public void persist() {
        final Preferences node = getPreferencesNode(key);

        for (final Iterator it = mappings.keySet().iterator(); it.hasNext();) {
            final String columnKey = (String) it.next();
            final int ix = getIndexForKey(columnKey);
            if (ix == -1) {
                continue;
            }
            final int width = tableColumnWidths[ix];
            if (width > 0) {
                node.putInt(columnKey, width);
            }
        }

        try {
            node.flush();
        } catch (final BackingStoreException e) {
            log.warn("can't flush prefs node", e); //$NON-NLS-1$
            return;
        }
    }

    public int getColumnWidth(final String columnKey) {
        return getPreferencesNode(key).getInt(columnKey, -1);
    }

    /**
     * Restores table column widths. Users of this class should have already
     * added some mappings before calling this method. Any persisted widths
     * whose keys do not correspond to indices into the {@link Table}'s columns
     * will be ignored.
     */
    public void restore() {
        final Preferences node = getPreferencesNode(key);

        for (final Iterator it = mappings.keySet().iterator(); it.hasNext();) {
            final String columnKey = (String) it.next();
            final int ix = getIndexForKey(columnKey);
            if (ix == -1) {
                continue;
            }
            final TableColumn tableColumn = table.getColumn(ix);
            final int persistedWidth = node.getInt(columnKey, -1);
            if (persistedWidth != -1) {
                tableColumn.setWidth(persistedWidth);
            }
        }
    }

    private int getIndexForKey(final String columnKey) {
        final int columnIndex = ((Integer) mappings.get(columnKey)).intValue();
        if (columnIndex >= 0 && columnIndex < tableColumnWidths.length) {
            return columnIndex;
        } else {
            final String messageFormat =
                "mapping key [{0}] is mapped to column index [{1}] but the Table has [{2}] indice(s)"; //$NON-NLS-1$
            final String message =
                MessageFormat.format(messageFormat, columnKey, Integer.toString(columnIndex), table.getColumnCount());
            log.warn(message);
            return -1;
        }
    }

    private class TableDisposedListener implements DisposeListener {
        @Override
        public void widgetDisposed(final DisposeEvent e) {
            persist();
        }
    }

    private class TableColumnResizedListener extends ControlAdapter {
        @Override
        public void controlResized(final ControlEvent e) {
            final TableColumn column = (TableColumn) e.widget;
            final int columnIndex = column.getParent().indexOf(column);
            tableColumnWidths[columnIndex] = column.getWidth();
        }
    }

    private static Preferences getPreferencesNode(final String key) {
        final IEclipsePreferences prefs = new InstanceScope().getNode(TFSCommonUIClientPlugin.PLUGIN_ID);

        Preferences node = prefs.node(TABLE_COLUMN_WIDTHS_NODE_NAME);
        node = node.node(key);

        return node;
    }
}
