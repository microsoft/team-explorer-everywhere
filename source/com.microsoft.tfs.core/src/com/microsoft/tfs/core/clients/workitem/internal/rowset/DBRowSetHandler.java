// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.internal.rowset;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import com.microsoft.tfs.core.clients.workitem.internal.metadata.Metadata;
import com.microsoft.tfs.core.clients.workitem.internal.metadata.TableIndexInfo;
import com.microsoft.tfs.core.clients.workitem.internal.metadata.TablePrimaryKeys;
import com.microsoft.tfs.core.clients.workitem.internal.metadata.mapper.SQLMapper;
import com.microsoft.tfs.core.clients.workitem.internal.metadata.mapper.SQLMapperFactory;
import com.microsoft.tfs.core.internal.db.DBConnection;
import com.microsoft.tfs.core.internal.db.DBStatement;

/**
 * A table handler that does the creating and updating of the main metadata
 * tables.
 */
public class DBRowSetHandler implements RowSetParseHandler {
    private final DBConnection connection;
    private final SQLMapper sqlMapper;
    private String tableName;
    private final List<String> columnNames = new ArrayList<String>();
    private final List<String> columnTypes = new ArrayList<String>();

    private int pkIndex;
    private String pkColName;
    private String pkType;

    private int fDeletedIndex;
    private int cacheStampIndex;

    private DBStatement deleteStatement;
    private DBStatement insertStatement;

    private boolean existingTable;

    private int deleteCount;
    private int insertCount;
    private int skipCount;
    private long startTime;
    private long maxCacheStamp;

    private final boolean verbose;

    public DBRowSetHandler(final DBConnection connection, final boolean verbose) {
        this.connection = connection;
        this.verbose = verbose;
        sqlMapper = SQLMapperFactory.getSQLMapper(connection);
    }

    public String getTableName() {
        return tableName;
    }

    @Override
    public void handleBeginParsing() {
        deleteCount = 0;
        insertCount = 0;
        skipCount = 0;
        startTime = System.currentTimeMillis();
        maxCacheStamp = 0;
    }

    @Override
    public void handleTableName(final String tableName) {
        this.tableName = tableName;
    }

    @Override
    public void handleColumn(final String name, final String type) {
        columnNames.add(name);
        columnTypes.add(type);
    }

    @Override
    public void handleFinishedColumns() {
        pkColName = TablePrimaryKeys.getPrimaryKeyColumnForTableName(tableName);
        pkIndex = columnNames.indexOf(pkColName);

        existingTable = connection.getDBSpecificOperations().tableExists(tableName);

        if (!existingTable) {
            final StringBuffer sb = new StringBuffer();
            sb.append("create table " + tableName + " ("); //$NON-NLS-1$ //$NON-NLS-2$
            for (int i = 0; i < columnNames.size(); i++) {
                final String colName = columnNames.get(i);
                final String colType = columnTypes.get(i);

                sb.append(
                    colName + " " + sqlMapper.getSQLColumnTypeFromMetadataColumnType(colType, tableName, colName)); //$NON-NLS-1$

                if (i == pkIndex) {
                    sb.append(" PRIMARY KEY"); //$NON-NLS-1$
                }

                if (i < columnNames.size() - 1) {
                    sb.append(", "); //$NON-NLS-1$
                }
            }
            sb.append(")"); //$NON-NLS-1$

            connection.createStatement(sb.toString()).executeUpdate();

            final TableIndexInfo.IndexDescription[] indexDescriptions = TableIndexInfo.getIndexesForTable(tableName);
            for (int i = 0; i < indexDescriptions.length; i++) {
                final String createIndexSql = "create index " //$NON-NLS-1$
                    + indexDescriptions[i].getIndexName()
                    + " on " //$NON-NLS-1$
                    + tableName
                    + " (" //$NON-NLS-1$
                    + indexDescriptions[i].getColumnName()
                    + ")"; //$NON-NLS-1$

                connection.createStatement(createIndexSql).executeUpdate();
            }

            // insert row version row in high water mark table.
            connection.createStatement("insert into " + Metadata.MAXCOUNT_TABLE_NAME + " values(?,0)").executeUpdate( //$NON-NLS-1$ //$NON-NLS-2$
                tableName);

        }

        fDeletedIndex = columnNames.indexOf("fDeleted"); //$NON-NLS-1$
        cacheStampIndex = columnNames.indexOf("Cachestamp"); //$NON-NLS-1$
        if (cacheStampIndex == -1) {
            // For some tables, cache stamp is camel case.
            cacheStampIndex = columnNames.indexOf("CacheStamp"); //$NON-NLS-1$
        }

        if (pkIndex == -1) {
            if (columnNames.size() == 1 && columnNames.get(0).equals("Column0")) //$NON-NLS-1$
            {
                return;
            }

            throw new RuntimeException(
                MessageFormat.format(
                    "primary key column [{0}] not found in table [{1}], columns are: {2}", //$NON-NLS-1$
                    pkColName,
                    tableName,
                    columnNames));
        }

        pkType = columnTypes.get(pkIndex);

        final StringBuffer sb = new StringBuffer("insert into " + tableName + " values ("); //$NON-NLS-1$ //$NON-NLS-2$
        for (int i = 0; i < columnNames.size(); i++) {
            sb.append("?"); //$NON-NLS-1$
            if (i < columnNames.size() - 1) {
                sb.append(","); //$NON-NLS-1$
            }
        }
        sb.append(")"); //$NON-NLS-1$

        deleteStatement = connection.createStatement("delete from " + tableName + " where " + pkColName + " = ?"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        insertStatement = connection.createStatement(sb.toString());

        deleteStatement.beginBatch();
        insertStatement.beginBatch();
    }

    @Override
    public void handleRow(final String[] rowValues) {
        if (existingTable) {
            final Object o = sqlMapper.getSQLObject(pkType, rowValues[pkIndex]);
            deleteCount += deleteStatement.executeUpdate(o);
        }

        /*
         * Work out max cachestamp for rowset
         */
        if (cacheStampIndex != -1) {
            long cacheStamp = 0;
            try {
                cacheStamp = Long.parseLong(rowValues[cacheStampIndex]);
            } catch (final NumberFormatException e) {
                // Ignore NFE
            }
            if (cacheStamp > maxCacheStamp) {
                maxCacheStamp = cacheStamp;
            }
        }

        /*
         * this check implements the "skipping" of fDeleted=true rows
         */
        if (fDeletedIndex != -1) {
            final String deletedValue = rowValues[fDeletedIndex];
            if (deletedValue != null && deletedValue.trim().equalsIgnoreCase("true")) //$NON-NLS-1$
            {
                /*
                 * don't insert rows with deleted flag set
                 */
                ++skipCount;
                return;
            }
        }

        final Object[] params = new Object[rowValues.length];
        for (int i = 0; i < rowValues.length; i++) {
            params[i] = sqlMapper.getSQLObject(columnTypes.get(i), rowValues[i]);
        }
        insertCount += insertStatement.executeUpdate(params);
    }

    @Override
    public void handleEndParsing() {
        if (deleteStatement != null) {
            deleteStatement.finishBatch();
        }
        if (insertStatement != null) {
            insertStatement.finishBatch();
        }

        // Update high water mark table
        if (maxCacheStamp > 0) {
            final String sql = "UPDATE " //$NON-NLS-1$
                + Metadata.MAXCOUNT_TABLE_NAME
                + " SET " //$NON-NLS-1$
                + Metadata.ROW_VERSION_COLUMN_NAME
                + " = " //$NON-NLS-1$
                + maxCacheStamp
                + " WHERE " //$NON-NLS-1$
                + Metadata.TABLE_NAME_COLUMN_NAME
                + " = '" //$NON-NLS-1$
                + tableName
                + "'" //$NON-NLS-1$
                + " AND   " //$NON-NLS-1$
                + Metadata.ROW_VERSION_COLUMN_NAME
                + " < " //$NON-NLS-1$
                + maxCacheStamp;
            connection.createStatement(sql).executeUpdate();
        }

        if (verbose) {
            final long elapsedTime = System.currentTimeMillis() - startTime;
            System.out.println(
                MessageFormat.format(
                    "table [{0}] deleted {1} inserted {2} skipped {3} cachestamp {4} elapsed {5}", //$NON-NLS-1$
                    tableName,
                    deleteCount,
                    insertCount,
                    skipCount,
                    maxCacheStamp,
                    elapsedTime));
        }
    }

    public int getDeleteCount() {
        return deleteCount;
    }

    public int getInsertCount() {
        return insertCount;
    }

    public int getSkipCount() {
        return skipCount;
    }
}
