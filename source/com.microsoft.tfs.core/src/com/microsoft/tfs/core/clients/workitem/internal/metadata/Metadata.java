// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.internal.metadata;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.MessageFormat;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Element;

import com.microsoft.tfs.core.clients.workitem.WorkItemServerVersion;
import com.microsoft.tfs.core.clients.workitem.exceptions.WorkItemException;
import com.microsoft.tfs.core.clients.workitem.internal.metadata.dao.ActionsTable;
import com.microsoft.tfs.core.clients.workitem.internal.metadata.dao.ConstantSetsTable;
import com.microsoft.tfs.core.clients.workitem.internal.metadata.dao.ConstantsTable;
import com.microsoft.tfs.core.clients.workitem.internal.metadata.dao.FieldUsagesTable;
import com.microsoft.tfs.core.clients.workitem.internal.metadata.dao.FieldsTable;
import com.microsoft.tfs.core.clients.workitem.internal.metadata.dao.HierarchyPropertiesTable;
import com.microsoft.tfs.core.clients.workitem.internal.metadata.dao.HierarchyTable;
import com.microsoft.tfs.core.clients.workitem.internal.metadata.dao.RulesTable;
import com.microsoft.tfs.core.clients.workitem.internal.metadata.dao.WorkItemLinkTypesTable;
import com.microsoft.tfs.core.clients.workitem.internal.metadata.dao.WorkItemTypeCategoriesTable;
import com.microsoft.tfs.core.clients.workitem.internal.metadata.dao.WorkItemTypeCategoryMembersTable;
import com.microsoft.tfs.core.clients.workitem.internal.metadata.dao.WorkItemTypeTable;
import com.microsoft.tfs.core.clients.workitem.internal.metadata.dao.WorkItemTypeUsagesTable;
import com.microsoft.tfs.core.clients.workitem.internal.metadata.impl.ConstantHandlerImpl;
import com.microsoft.tfs.core.clients.workitem.internal.rowset.DBRowSetHandler;
import com.microsoft.tfs.core.clients.workitem.internal.rowset.RowSetParser;
import com.microsoft.tfs.core.exceptions.TECoreException;
import com.microsoft.tfs.core.internal.db.ConnectionPool;
import com.microsoft.tfs.core.internal.db.DBConnection;
import com.microsoft.tfs.core.internal.db.DBTask;
import com.microsoft.tfs.core.internal.db.ResultHandler;
import com.microsoft.tfs.core.ws.runtime.types.AnyContentType;
import com.microsoft.tfs.core.ws.runtime.types.DOMAnyContentType;
import com.microsoft.tfs.core.ws.runtime.types.StaxAnyContentType;
import com.microsoft.tfs.util.Closable;

import ms.tfs.workitemtracking.clientservices._03._ClientService2Soap;
import ms.tfs.workitemtracking.clientservices._03._ClientService2Soap_GetMetadataEx2Response;
import ms.tfs.workitemtracking.clientservices._03._ClientService3Soap;
import ms.tfs.workitemtracking.clientservices._03._ClientService3Soap_GetMetadataEx2Response;
import ms.tfs.workitemtracking.clientservices._03._ClientService5Soap;
import ms.tfs.workitemtracking.clientservices._03._ClientService5Soap_GetMetadataEx2Response;
import ms.tfs.workitemtracking.clientservices._03._MetadataTableHaveEntry;

public class Metadata implements IMetadata, IMetadataUpdateHandler {
    /*
     * The schema version is a rough way of adding versioning capbility to the
     * metadata database schema (this is a TEE feature, nothing to do with TFS).
     *
     * The schema version is used both internally and externally. Internally, it
     * is stored in a "housekeeping" database table and checked each time we
     * start up. If there is a mismatch, we drop all the tables and recreate the
     * database. Typically this scenario will only happen in the non-HSQLDB case
     * (read on for why).
     *
     * Externally, the disk-based HSQLDB databases incorporate the schema
     * version in the directory name used. This allows side-by-side installation
     * of versions of TEE that use different schemas. (That way they will not
     * keep clobbering each other's data.)
     *
     * The schema version has the following rough format: X.YZ Where X is the
     * major TEE version, Y is the minor TEE version, and Z is an alphabet
     * character that gets bumped for each schema revision within that
     * major/minor TEE version.
     *
     * Each public version of TEE in which the schema changes from a previous
     * public version of TEE must use a new schema version identifier. Any two
     * public releases of TEE that use different schemas should be able to run
     * side-by-side without clobbering of metadata.
     *
     * Therefore 2.0A indicates the first schema version for Teampr1se 2.0.
     *
     * Schema version history: 1.0A - Teampr1se 1.0 and 1.1 2.0A - Teampr1se 2.0
     * (starting with preview 2). Added primary key constraints to the PK column
     * in each table. 2.0B - Teampr1se 2.0 (starting with final version). Added
     * indices to ConstantSets for ConstID and ParentID columns. 3.0B -
     * Teampr1se 3.1. Added MAXCOUNT table. 4.0A - Microsoft TEE initial version
     */
    public static final String SCHEMA_VERSION = "4.0A"; //$NON-NLS-1$

    private static final String HOUSEKEEPING_TABLE_NAME = "WITHousekeeping"; //$NON-NLS-1$
    private static final String DBSTAMP_COLUMN_NAME = "DBStamp"; //$NON-NLS-1$
    private static final String SCHEMA_VERSION_COLUMN_NAME = "SchemaVersion"; //$NON-NLS-1$

    public static final String MAXCOUNT_TABLE_NAME = "WITMaxCount"; //$NON-NLS-1$
    public static final String TABLE_NAME_COLUMN_NAME = "TableName"; //$NON-NLS-1$
    public static final String ROW_VERSION_COLUMN_NAME = "RowVersion"; //$NON-NLS-1$

    private static final Log log = LogFactory.getLog(Metadata.class);

    private final ConnectionPool connectionPool;
    private final Set<IMetadataChangeListener> metadataUpdateListeners = new HashSet<IMetadataChangeListener>();
    private boolean verbose = false;
    private final MetadataDAOFactory daoFactory;
    private final _ClientService2Soap clientService2;
    private final _ClientService3Soap clientService3;
    private final _ClientService5Soap clientService5;
    private final WorkItemServerVersion serverVersion;

    private final ConstantHandlerImpl constantHandler;

    private Set<Integer> distinctConstantSetIds;

    private final boolean alwaysSendUpdateNotifications =
        Boolean.getBoolean("com.microsoft.tfs.core.workitem.metadata.alwaysnotifyonupdate"); //$NON-NLS-1$

    private int userDisplayMode;

    public Metadata(
        final ConnectionPool connectionPool,
        final WorkItemServerVersion serverVersion,
        final _ClientService2Soap clientService2,
        final _ClientService3Soap clientService3,
        final _ClientService5Soap clientService5) {
        this.connectionPool = connectionPool;
        this.clientService2 = clientService2;
        this.clientService3 = clientService3;
        this.clientService5 = clientService5;
        this.serverVersion = serverVersion;
        constantHandler = new ConstantHandlerImpl(this);

        daoFactory = new MetadataDAOFactory(this, connectionPool);

        connectionPool.executeWithPooledConnection(new DBTask() {
            @Override
            public void performTask(final DBConnection connection) {
                /*
                 * We're starting up the metadata manager. Before returning to
                 * the caller, we need to figure out if we need to "reset" the
                 * database - erasing old metadata tables and housekeeping
                 * values. We are in one of three states:
                 *
                 * 1) No housekeeping table exists - either because the database
                 * is from before the housekeeping table was added, or because
                 * the database is completely empty. We reset.
                 *
                 * 2) A housekeeping table exists, and the schema version stored
                 * in the housekeeping table matches the schema version constant
                 * in this class. In this case, the database is up to date and
                 * we do nothing.
                 *
                 * 3) A housekeeping table exists, and the schema version
                 * doesn't match the schema version constant in this class. We
                 * drop the housekeeping table and then reset.
                 */

                boolean reset = false;

                if (!connection.getDBSpecificOperations().tableExists(HOUSEKEEPING_TABLE_NAME)) {
                    /*
                     * Housekeeping table doesn't exist
                     */
                    log.info("unable to find housekeeping table - will reset"); //$NON-NLS-1$
                    reset = true;
                } else {
                    final String currentSchemaVersion = getSchemaVersion(connection);
                    if (!SCHEMA_VERSION.equals(currentSchemaVersion)) {
                        /*
                         * Schema version mismatch
                         */
                        log.info(MessageFormat.format(
                            "current schema [{0}] does not match [{1}] - will reset", //$NON-NLS-1$
                            currentSchemaVersion,
                            SCHEMA_VERSION));
                        dropHousekeepingTable(connection);
                        reset = true;
                    }
                }

                if (reset) {
                    dropMetadataTables(connection);

                    /*
                     * Create the housekeeping table
                     */
                    connection.createStatement("create table " //$NON-NLS-1$
                        + HOUSEKEEPING_TABLE_NAME
                        + " (" //$NON-NLS-1$
                        + DBSTAMP_COLUMN_NAME
                        + " varchar(255), " //$NON-NLS-1$
                        + SCHEMA_VERSION_COLUMN_NAME
                        + " varchar(255))").executeUpdate(); //$NON-NLS-1$

                    /*
                     * insert a row into the housekeeping table - elsewhere we
                     * assume there is always one row
                     */
                    connection.createStatement("insert into " //$NON-NLS-1$
                        + HOUSEKEEPING_TABLE_NAME
                        + " (" //$NON-NLS-1$
                        + DBSTAMP_COLUMN_NAME
                        + ", " //$NON-NLS-1$
                        + SCHEMA_VERSION_COLUMN_NAME
                        + ") " //$NON-NLS-1$
                        + "values (NULL, '" //$NON-NLS-1$
                        + SCHEMA_VERSION
                        + "')").executeUpdate(); //$NON-NLS-1$

                    createMaxCountTable(connection);

                }
            }

        });
    }

    @Override
    public int getUserDisplayMode() {
        return userDisplayMode;
    }

    @Override
    public ConstantHandler getConstantHandler() {
        return constantHandler;
    }

    @Override
    public RulesTable getRulesTable() {
        return (RulesTable) daoFactory.getDAO(RulesTable.class);
    }

    @Override
    public FieldsTable getFieldsTable() {
        return (FieldsTable) daoFactory.getDAO(FieldsTable.class);
    }

    @Override
    public HierarchyTable getHierarchyTable() {
        return (HierarchyTable) daoFactory.getDAO(HierarchyTable.class);
    }

    @Override
    public ConstantsTable getConstantsTable() {
        return (ConstantsTable) daoFactory.getDAO(ConstantsTable.class);
    }

    @Override
    public ActionsTable getActionsTable() {
        return (ActionsTable) daoFactory.getDAO(ActionsTable.class);
    }

    @Override
    public WorkItemTypeTable getWorkItemTypeTable() {
        return (WorkItemTypeTable) daoFactory.getDAO(WorkItemTypeTable.class);
    }

    @Override
    public HierarchyPropertiesTable getHierarchyPropertiesTable() {
        return (HierarchyPropertiesTable) daoFactory.getDAO(HierarchyPropertiesTable.class);
    }

    public ConstantSetsTable getConstantSetsTable() {
        return (ConstantSetsTable) daoFactory.getDAO(ConstantSetsTable.class);
    }

    @Override
    public WorkItemTypeUsagesTable getWorkItemTypeUsagesTable() {
        return (WorkItemTypeUsagesTable) daoFactory.getDAO(WorkItemTypeUsagesTable.class);
    }

    @Override
    public FieldUsagesTable getFieldUsagesTable() {
        return (FieldUsagesTable) daoFactory.getDAO(FieldUsagesTable.class);
    }

    @Override
    public WorkItemLinkTypesTable getLinkTypesTable() {
        return (WorkItemLinkTypesTable) daoFactory.getDAO(WorkItemLinkTypesTable.class);
    }

    @Override
    public WorkItemTypeCategoriesTable getWorkItemTypeCategoriesTable() {
        return (WorkItemTypeCategoriesTable) daoFactory.getDAO(WorkItemTypeCategoriesTable.class);
    }

    @Override
    public WorkItemTypeCategoryMembersTable getWorkItemTypeCategoryMembersTable() {
        return (WorkItemTypeCategoryMembersTable) daoFactory.getDAO(WorkItemTypeCategoryMembersTable.class);
    }

    @Override
    public void addMetadataChangeListener(final IMetadataChangeListener listener) {
        synchronized (metadataUpdateListeners) {
            metadataUpdateListeners.add(listener);
        }
    }

    public ConnectionPool getConnectionPool() {
        return connectionPool;
    }

    @Override
    public _MetadataTableHaveEntry[] getHaveEntries() {
        final Object[] objectHolder = new Object[1];

        connectionPool.executeWithPooledConnection(new DBTask() {
            @Override
            public void performTask(final DBConnection connection) {
                final Set<String> tables = getAllTableNames();
                final _MetadataTableHaveEntry[] entries = new _MetadataTableHaveEntry[tables.size()];
                int ix = 0;
                for (final Iterator<String> it = tables.iterator(); it.hasNext();) {
                    final String tableName = it.next();
                    final long rowVersion = getCachestamp(tableName, connection);
                    entries[ix++] = new _MetadataTableHaveEntry(tableName, rowVersion);
                }
                objectHolder[0] = entries;
            }
        });

        return (_MetadataTableHaveEntry[]) objectHolder[0];
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * com.microsoft.tfs.core.workitem.internal.metadata.IMetadataUpdateHandler#
     * updateMetadata(com.microsoft.tfs.core.ws.runtime.types.AnyContentType,
     * java.lang.String)
     */
    @Override
    public long updateMetadata(final AnyContentType metadata, final String newDbStamp) {
        final long startTime = System.currentTimeMillis();
        final boolean[] fullUpdateHolder = new boolean[1];
        fullUpdateHolder[0] = false;
        final Set<String> tableNames = new HashSet<String>();

        connectionPool.executeWithPooledConnection(new DBTask() {
            @Override
            public void performTask(final DBConnection connection) {
                if (newDbStamp != null) {
                    /*
                     * A DBStamp was returned along with metadata updates. We
                     * need to check it and see which of three states we're in:
                     *
                     * 1) If the current dbstamp is null, this is the first
                     * metadata update. Set the dbstamp to the passed value and
                     * proceed with the update.
                     *
                     * 2) If the current dbstamp is not null and doesn't match
                     * the passed dbstamp, then our cache has been invalidated.
                     * Ignore the metadata update and do a complete refresh.
                     *
                     * 3) If the current dbstamp is not null and matches the
                     * passed dbstamp, proceed with the update.
                     */

                    final String oldDbStamp = getDBStamp(connection);

                    if (oldDbStamp == null || oldDbStamp.length() == 0) {
                        setDBStamp(connection, newDbStamp);
                    } else if (!oldDbStamp.equals(newDbStamp)) {
                        log.info(MessageFormat.format(
                            "current dbstamp [{0}] does not match [{1}] - invalidating cache", //$NON-NLS-1$
                            oldDbStamp,
                            newDbStamp));
                        dropMetadataTables(connection);
                        createMaxCountTable(connection);
                        setDBStamp(connection, ""); //$NON-NLS-1$
                        fullUpdateHolder[0] = true;
                        return;
                    }
                }

                /*
                 * Normal update.
                 */

                final Iterator i = metadata.getElementIterator();
                final RowSetParser parser = new RowSetParser();

                while (i.hasNext()) {
                    final DBRowSetHandler handler = new DBRowSetHandler(connection, verbose);

                    if (metadata instanceof DOMAnyContentType) {
                        parser.parse((Element) i.next(), handler);
                    } else if (metadata instanceof StaxAnyContentType) {
                        final XMLStreamReader reader = (XMLStreamReader) i.next();
                        try {
                            parser.parse(reader, handler);
                        } finally {
                            try {
                                reader.close();
                            } catch (final XMLStreamException e) {
                                throw new TECoreException(e);
                            }
                        }
                    } else {
                        throw new WorkItemException(
                            MessageFormat.format(
                                "Can''t update metadata from unknown AnyContentType implementation {0}", //$NON-NLS-1$
                                metadata.getClass().getName()));
                    }

                    if (alwaysSendUpdateNotifications || handler.getInsertCount() > 0 || handler.getDeleteCount() > 0) {
                        tableNames.add(handler.getTableName());
                    }
                }

                if (i instanceof Closable) {
                    ((Closable) i).close();
                }
            }
        });

        if (fullUpdateHolder[0]) {
            update();
        } else {
            if (tableNames.size() > 0) {
                /*
                 * reset internal caches
                 */
                synchronized (this) {
                    if (tableNames.contains(MetadataTableNames.CONSTANT_SETS)) {
                        distinctConstantSetIds = null;
                    }
                }

                final Set<String> unmodifiableTableNameSet = Collections.unmodifiableSet(tableNames);

                /*
                 * Fire any listeners for metadata updates
                 */
                synchronized (metadataUpdateListeners) {
                    for (final Iterator<IMetadataChangeListener> it =
                        metadataUpdateListeners.iterator(); it.hasNext();) {
                        it.next().metadataChanged(unmodifiableTableNameSet);
                    }
                }
            }
        }

        return System.currentTimeMillis() - startTime;
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * com.microsoft.tfs.core.workitem.internal.metadata.IMetadataUpdateHandler#
     * update()
     */
    @Override
    public void update() {
        update(false);
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * com.microsoft.tfs.core.workitem.internal.metadata.IMetadataUpdateHandler#
     * update(boolean)
     */
    @Override
    public MetadataUpdateResults update(final boolean wantResults) {
        final _MetadataTableHaveEntry[] aomthe = getHaveEntries();

        long proxyTime;
        final long stTime = System.currentTimeMillis();

        AnyContentType metadata;
        String dbStamp;

        if (serverVersion.getValue() >= 5) {
            final _ClientService5Soap_GetMetadataEx2Response response =
                clientService5.getMetadataEx2(aomthe, true, new StaxAnyContentType());

            metadata = response.getMetadata();
            dbStamp = response.getDbStamp();
            userDisplayMode = response.getMode();
        } else if (serverVersion.getValue() >= 3) {
            final _ClientService3Soap_GetMetadataEx2Response response =
                clientService3.getMetadataEx2(aomthe, true, new StaxAnyContentType());

            metadata = response.getMetadata();
            dbStamp = response.getDbStamp();
            userDisplayMode = response.getMode();
        } else {
            final _ClientService2Soap_GetMetadataEx2Response response =
                clientService2.getMetadataEx2(aomthe, true, new StaxAnyContentType());

            metadata = response.getMetadata();
            dbStamp = response.getDbStamp();
            userDisplayMode = response.getMode();
        }

        proxyTime = System.currentTimeMillis() - stTime;

        final long updateDbTime = updateMetadata(metadata, dbStamp);

        if (wantResults) {
            /*
             * The caller must dispose of the results (which disposes the inner
             * metadata).
             */
            return new MetadataUpdateResults(proxyTime, updateDbTime, metadata, dbStamp);
        } else {
            /*
             * Dispose of the inner metadata.
             */
            metadata.dispose();

            return null;
        }
    }

    private void createMaxCountTable(final DBConnection connection) {
        /*
         * Create high water mark table
         */
        connection.createStatement("create table " //$NON-NLS-1$
            + MAXCOUNT_TABLE_NAME
            + " (" //$NON-NLS-1$
            + TABLE_NAME_COLUMN_NAME
            + " varchar(255), " //$NON-NLS-1$
            + ROW_VERSION_COLUMN_NAME
            + " bigint)").executeUpdate(); //$NON-NLS-1$
    }

    private long getCachestamp(final String tableName, final DBConnection connection) {
        if (connection.getDBSpecificOperations().tableExists(MAXCOUNT_TABLE_NAME)) {
            final Long maxCacheStamp = connection.createStatement("select " //$NON-NLS-1$
                + ROW_VERSION_COLUMN_NAME
                + " from " //$NON-NLS-1$
                + MAXCOUNT_TABLE_NAME
                + " where " //$NON-NLS-1$
                + TABLE_NAME_COLUMN_NAME
                + " = '" //$NON-NLS-1$
                + tableName
                + "'").executeLongQuery(); //$NON-NLS-1$

            /*
             * If there are no rows in the table, the query will return null
             */

            return (maxCacheStamp != null ? maxCacheStamp.longValue() : 0);
        } else {
            return 0;
        }
    }

    private String getDBStamp(final DBConnection connection) {
        return connection.createStatement(
            "select " + DBSTAMP_COLUMN_NAME + " from " + HOUSEKEEPING_TABLE_NAME).executeStringQuery(); //$NON-NLS-1$ //$NON-NLS-2$
    }

    private void setDBStamp(final DBConnection connection, final String dbStamp) {
        connection.createStatement(
            "update " + HOUSEKEEPING_TABLE_NAME + " set " + DBSTAMP_COLUMN_NAME + " = ?").executeUpdate( //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                dbStamp);
    }

    private String getSchemaVersion(final DBConnection connection) {
        return connection.createStatement(
            "select " + SCHEMA_VERSION_COLUMN_NAME + " from " + HOUSEKEEPING_TABLE_NAME).executeStringQuery(); //$NON-NLS-1$ //$NON-NLS-2$
    }

    private void dropHousekeepingTable(final DBConnection connection) {
        connection.createStatement("drop table " + HOUSEKEEPING_TABLE_NAME).executeUpdate(); //$NON-NLS-1$
    }

    private void dropMetadataTables(final DBConnection connection) {
        for (final Iterator<String> it = getAllTableNames().iterator(); it.hasNext();) {
            final String tableName = it.next();
            final String stmt = "drop table " + tableName; //$NON-NLS-1$
            try {
                connection.createStatement(stmt).executeUpdate();
            } catch (final Throwable t) {
                /*
                 * Log message but don't throw error
                 */
                log.warn(MessageFormat.format("drop metadata tables: {0}", t.getMessage())); //$NON-NLS-1$
            }
        }

        // drop high water mark table
        try {
            connection.createStatement("drop table " + MAXCOUNT_TABLE_NAME).executeUpdate(); //$NON-NLS-1$
        } catch (final Throwable t) {
            /*
             * Log message but don't throw error
             */
            log.warn(MessageFormat.format("drop maxcount table: {0}", t.getMessage())); //$NON-NLS-1$
        }

    }

    private Set<String> getAllTableNames() {
        if (serverVersion.getValue() >= 3) {
            return MetadataTableNames.allTableNamesVersion3;
        } else {
            return MetadataTableNames.allTableNamesVersion2;
        }
    }

    public void setVerbose(final boolean verbose) {
        this.verbose = verbose;
    }

    public synchronized Set<Integer> getDistinctConstantSetIDs() {
        if (distinctConstantSetIds == null) {
            distinctConstantSetIds = new HashSet<Integer>();

            getConnectionPool().executeWithPooledConnection(new DBTask() {
                @Override
                public void performTask(final DBConnection connection) {
                    connection.createStatement("select distinct ParentID from ConstantSets").executeQuery( //$NON-NLS-1$
                        new ResultHandler() {
                            @Override
                            public void handleRow(final ResultSet rset) throws SQLException {
                                final int constId = rset.getInt(1);
                                distinctConstantSetIds.add(new Integer(constId));
                            }
                        });
                }
            });

            distinctConstantSetIds = Collections.unmodifiableSet(distinctConstantSetIds);
        }

        return distinctConstantSetIds;
    }
}
