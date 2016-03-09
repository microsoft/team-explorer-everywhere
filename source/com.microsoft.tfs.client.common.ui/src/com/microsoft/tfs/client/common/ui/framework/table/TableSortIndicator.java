// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.framework.table;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;

public class TableSortIndicator {
    private static boolean enabled = true;

    public static TableColumn setSortIndicator(final Table table, final TableColumn sortColumn, final int direction) {
        if (!enabled) {
            return null;
        }

        try {
            final TableColumn oldSortColumn =
                (TableColumn) table.getClass().getMethod("getSortColumn", (Class[]) null).invoke( //$NON-NLS-1$
                    table,
                    (Object[]) null);

            table.getClass().getMethod("setSortColumn", new Class[] //$NON-NLS-1$
            {
                TableColumn.class
            }).invoke(table, new Object[] {
                sortColumn
            });

            if (sortColumn != null) {
                table.getClass().getMethod("setSortDirection", new Class[] //$NON-NLS-1$
                {
                    Integer.TYPE
                }).invoke(table, new Object[] {
                    new Integer(direction)
                });
            }

            return oldSortColumn;
        } catch (final InvocationTargetException e) {
            final Throwable targetException = e.getTargetException();

            if (targetException instanceof RuntimeException) {
                throw (RuntimeException) targetException;
            } else {
                throw new RuntimeException(targetException);
            }
        } catch (final Exception e) {
            enabled = false;
        }

        return null;
    }
}
