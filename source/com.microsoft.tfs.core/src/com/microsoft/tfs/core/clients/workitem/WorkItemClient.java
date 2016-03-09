// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem;

import java.io.File;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import com.microsoft.tfs.core.Messages;
import com.microsoft.tfs.core.TFSTeamProjectCollection;
import com.microsoft.tfs.core.artifact.ArtifactID;
import com.microsoft.tfs.core.clients.framework.internal.ServiceInterfaceIdentifiers;
import com.microsoft.tfs.core.clients.framework.internal.ServiceInterfaceNames;
import com.microsoft.tfs.core.clients.workitem.events.WorkItemEventEngine;
import com.microsoft.tfs.core.clients.workitem.exceptions.DeniedOrNotExistException;
import com.microsoft.tfs.core.clients.workitem.fields.FieldDefinitionCollection;
import com.microsoft.tfs.core.clients.workitem.internal.WITContext;
import com.microsoft.tfs.core.clients.workitem.internal.WorkItemFieldIDs;
import com.microsoft.tfs.core.clients.workitem.internal.WorkItemImpl;
import com.microsoft.tfs.core.clients.workitem.internal.fields.FieldDefinitionCollectionImpl;
import com.microsoft.tfs.core.clients.workitem.internal.link.RegisteredLinkTypeCollectionImpl;
import com.microsoft.tfs.core.clients.workitem.internal.link.WorkItemLinkTypeCollectionImpl;
import com.microsoft.tfs.core.clients.workitem.internal.metadata.IMetadataChangeListener;
import com.microsoft.tfs.core.clients.workitem.internal.metadata.Metadata;
import com.microsoft.tfs.core.clients.workitem.internal.project.ProjectCollectionImpl;
import com.microsoft.tfs.core.clients.workitem.internal.query.NullQueryImpl;
import com.microsoft.tfs.core.clients.workitem.internal.query.QueryImpl;
import com.microsoft.tfs.core.clients.workitem.internal.query.StoredQueryImpl;
import com.microsoft.tfs.core.clients.workitem.internal.update.DestroyWorkItemTypeUpdatePackage;
import com.microsoft.tfs.core.clients.workitem.internal.update.DestroyWorkItemUpdatePackage;
import com.microsoft.tfs.core.clients.workitem.link.RegisteredLinkTypeCollection;
import com.microsoft.tfs.core.clients.workitem.link.WorkItemLinkTypeCollection;
import com.microsoft.tfs.core.clients.workitem.project.Project;
import com.microsoft.tfs.core.clients.workitem.project.ProjectCollection;
import com.microsoft.tfs.core.clients.workitem.query.BatchReadParameter;
import com.microsoft.tfs.core.clients.workitem.query.BatchReadParameterCollection;
import com.microsoft.tfs.core.clients.workitem.query.InvalidQueryTextException;
import com.microsoft.tfs.core.clients.workitem.query.Query;
import com.microsoft.tfs.core.clients.workitem.query.StoredQuery;
import com.microsoft.tfs.core.clients.workitem.query.WorkItemCollection;
import com.microsoft.tfs.core.clients.workitem.queryhierarchy.QueryFolder;
import com.microsoft.tfs.core.clients.workitem.queryhierarchy.QueryItem;
import com.microsoft.tfs.core.clients.workitem.wittype.WorkItemType;
import com.microsoft.tfs.core.internal.db.ConnectionConfiguration;
import com.microsoft.tfs.core.pguidance.IProcessGuidance;
import com.microsoft.tfs.core.pguidance.internal.WSSProcessGuidance;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.Closable;
import com.microsoft.tfs.util.GUID;
import com.microsoft.tfs.util.GUID.GUIDStringFormat;

import ms.tfs.workitemtracking.clientservices._03._ClientService2Soap;
import ms.tfs.workitemtracking.clientservices._03._ClientService3Soap;
import ms.tfs.workitemtracking.clientservices._03._ClientService5Soap;

/**
 * Provides access to the Work Item Tracking services.
 *
 * @since TEE-SDK-10.1
 */
public final class WorkItemClient implements Closable {
    /*
     * This class is roughly equivalent to the public portions of
     * "WorkItemStore" in MS code.
     */

    private final WorkItemEventEngine eventEngine = new WorkItemEventEngine();

    private final _ClientService2Soap webService2;
    private final _ClientService3Soap webService3;
    private final _ClientService5Soap webService5;
    private final TFSTeamProjectCollection connection;
    private ProjectCollection projectCollection;
    private FieldDefinitionCollection fieldDefinitions;
    private WorkItemLinkTypeCollection linkTypes;
    private HashMap<String, GroupDataProvider> mapGroupDataProviders;
    private WITContext witContext;

    private WorkItemServerVersion version;

    public WorkItemClient(
        final TFSTeamProjectCollection connection,
        final _ClientService2Soap webService2,
        final _ClientService3Soap webService3,
        final _ClientService5Soap webService5) {
        Check.notNull(connection, "connection"); //$NON-NLS-1$
        Check.notNull(webService2, "webService2"); //$NON-NLS-1$

        this.connection = connection;
        this.webService2 = webService2;
        this.webService3 = webService3;
        this.webService5 = webService5;

        detectVersion();
    }

    public boolean supportsWorkItemLinkTypes() {
        return version.getValue() >= WorkItemServerVersion.V3.getValue();
    }

    public boolean supportsReadOnlyLinkTypes() {
        return version.getValue() >= WorkItemServerVersion.V3.getValue();
    }

    public boolean supportsLinkQueries() {
        return version.getValue() >= WorkItemServerVersion.V3.getValue();
    }

    public boolean supportsWIQLFieldAndGroupOperators() {
        return version.getValue() >= WorkItemServerVersion.V3.getValue();
    }

    public boolean supportsWebAccessWorkItems() {
        return version.getValue() >= WorkItemServerVersion.V3.getValue();
    }

    public boolean supportsWIQLEvaluationOnServer() {
        return getContext().getServerInfo().isSupported(SupportedFeatures.WIQL_EVALUATION_ON_SERVER);
    }

    private void detectVersion() {
        final String location8 = connection.getServerDataProvider().locationForCurrentConnection(
            ServiceInterfaceNames.WORK_ITEM_8,
            ServiceInterfaceIdentifiers.WORK_ITEM_8);

        final String location5 = connection.getServerDataProvider().locationForCurrentConnection(
            ServiceInterfaceNames.WORK_ITEM_5,
            ServiceInterfaceIdentifiers.WORK_ITEM_5);

        final String location3 = connection.getServerDataProvider().locationForCurrentConnection(
            ServiceInterfaceNames.WORK_ITEM_3,
            ServiceInterfaceIdentifiers.WORK_ITEM_3);

        if (location8 != null) {
            version = WorkItemServerVersion.V8;
        } else if (location5 != null) {
            version = WorkItemServerVersion.V5;
        } else if (location3 != null) {
            version = WorkItemServerVersion.V3;
        } else {
            version = WorkItemServerVersion.V1;
        }
    }

    public File getDatabaseDirectory() {
        return getContext().getConnectionConfiguration().getDatabaseDiskDirectory();
    }

    /**
     * Gets localized database configuration properties for use in support
     * situations.
     *
     * @param locale
     *        the {@link Locale} to localize info strings to (<code>null</code>
     *        to use the default locale)
     * @return localized properties describing the current database
     *         configuration (never <code>null</code>)
     */
    public Properties getDatabaseConfigurationDebugInfo(final Locale locale) {
        final ConnectionConfiguration config = getContext().getConnectionConfiguration();
        final Properties props = new Properties();

        /*
         * JDBC configuration
         */
        props.setProperty(Messages.getString("WorkItemClient.DebugInfoDriver", locale), config.getDriverClass()); //$NON-NLS-1$
        props.setProperty(Messages.getString("WorkItemClient.DebugInfoURL", locale), config.getURL()); //$NON-NLS-1$
        props.setProperty(Messages.getString("WorkItemClient.DebugInfoUsername", locale), config.getUsername()); //$NON-NLS-1$
        props.setProperty(Messages.getString("WorkItemClient.DebugInfoPassword", locale), config.getPassword()); //$NON-NLS-1$

        /*
         * Driver information
         */
        props.setProperty(
            Messages.getString("WorkItemClient.DebugInfoDriverVersion", locale), //$NON-NLS-1$
            config.getDriverMajorVersion() + "." + config.getDriverMinorVersion()); //$NON-NLS-1$
        props.setProperty(
            Messages.getString("WorkItemClient.DebugInfoDriverClassURL", locale), //$NON-NLS-1$
            config.getDriverClassURL().toExternalForm());

        /*
         * Metadata schema version
         */
        props.setProperty(
            Messages.getString("WorkItemClient.DebugInfoMetadataSchemaVersion", locale), //$NON-NLS-1$
            Metadata.SCHEMA_VERSION);

        /*
         * Path identifier
         */
        props.setProperty(
            Messages.getString("WorkItemClient.DebugInfoPathIdentifier", locale), //$NON-NLS-1$
            config.getPathIdentifier());

        /*
         * Disk directory
         */
        final File diskDirectory = config.getDatabaseDiskDirectory();
        if (diskDirectory == null) {
            props.setProperty(
                Messages.getString("WorkItemClient.DebugInfoDatabaseDiskDirectory", locale), //$NON-NLS-1$
                Messages.getString("WorkItemClient.DebugInfoDatabaseDiskDirectoryNotDefined", locale)); //$NON-NLS-1$
        } else {
            props.setProperty(
                Messages.getString("WorkItemClient.DebugInfoDatabaseDiskDirectory", locale), //$NON-NLS-1$
                diskDirectory.getAbsolutePath());
        }

        return props;
    }

    public File getDatabaseDiskDirectoryForDebugInfo() {
        return getContext().getConnectionConfiguration().getDatabaseDiskDirectory();
    }

    /**
     * Retrieves a work item by ID from the TFS server. The returned work item
     * is open. If the ID does not correspond to a valid work item ID on the
     * server, or if the current user does not have permission to access the
     * work item with that ID, this method returns null.
     *
     * @param id
     *        ID of the work item to retrieve
     * @return an open WorkItem or null
     */
    public WorkItem getWorkItemByID(final int id) {
        /*
         * create a new work item instance
         */
        final WorkItemImpl workItem = new WorkItemImpl(getContext());

        /*
         * Set the ID field value to the requested ID. Note that this ID may not
         * exist on the server, or the current user may not have access to the
         * work item with that ID.
         */
        workItem.getFieldsInternal().addOriginalFieldValueLocal(WorkItemFieldIDs.ID, new Integer(id), true);

        /*
         * open the work item
         */
        try {
            workItem.open();
        } catch (final DeniedOrNotExistException ex) {
            /*
             * return null if the ID was invalid or if the work item did not
             * exist
             */
            return null;
        }

        return workItem;
    }

    public void deleteWorkItemByID(final int id) {
        final DestroyWorkItemUpdatePackage updatePackage = new DestroyWorkItemUpdatePackage(id, getContext());
        updatePackage.update();
    }

    public void deleteWorkItemType(final String projectName, final String workItemTypeName) {
        Check.notNull(projectName, "projectName"); //$NON-NLS-1$
        Check.notNull(workItemTypeName, "workItemTypeName"); //$NON-NLS-1$

        final DestroyWorkItemTypeUpdatePackage updatePackage =
            new DestroyWorkItemTypeUpdatePackage(projectName, workItemTypeName, getContext());
        updatePackage.update();

        /*
         * Deleting a work item type implies metadata updates. Actually, the
         * stored procedure that does the work executes a StampDB to completely
         * invalidate all client metadata caches. The new metadata updates don't
         * come back in the update() round trip, so we need to do a separate
         * round trip to get them.
         */
        refreshCache();
    }

    /**
     * @return the version of the work item client in use.
     */
    public WorkItemServerVersion getVersion() {
        return version;
    }

    /**
     * @return the Work Item Tracking web service proxy
     */
    public _ClientService2Soap getWebService2() {
        return webService2;
    }

    /**
     * @return the Work Item Tracking web service proxy (v3, 2010) (may be
     *         <code>null</code>)
     * @since TFS 2010
     */
    public _ClientService3Soap getWebService3() {
        return webService3;
    }

    /**
     * @return the Work Item Tracking web service proxy (v5, Dev11) (may be
     *         <code>null</code>)
     * @since TFS 2012
     */
    public _ClientService5Soap getWebService5() {
        return webService5;
    }

    /**
     * Pre-caches data that is used by this client. Calling this method early,
     * in a background thread, will improve the performance of subsequent work
     * item operations.
     */
    public void precacheData() {
        getContext();
    }

    public void refreshCache() {
        getContext().getMetadataUpdateHandler().update();
    }

    /**
     * Obtain the Project objects managed by this client. Each Project object
     * represents a Team Project on the TFS server. The Project objects are
     * cached by this client.
     *
     * @return an array of Project objects
     */
    public synchronized ProjectCollection getProjects() {
        if (projectCollection == null) {
            projectCollection = new ProjectCollectionImpl(getContext());
        }
        return projectCollection;
    }

    /**
     * Retrieve the cached group data provider for the specified project name.
     *
     * @param projectName
     *        The project to get groups for.
     *
     * @return A group data provider for the specified project.
     */
    public GroupDataProvider getGroupDataProvider(final String projectName) {
        if (mapGroupDataProviders == null) {
            mapGroupDataProviders = new HashMap<String, GroupDataProvider>();
        }

        if (!mapGroupDataProviders.containsKey(projectName)) {
            mapGroupDataProviders.put(projectName, new GroupDataProvider(this, projectName));
        }

        return mapGroupDataProviders.get(projectName);
    }

    /**
     * Get list of TFS Global and Project groups
     *
     * @param serverGuid
     *        Guid for the server for which group information needs to be
     *        retrieved
     *
     * @param projectGuid
     *        Guid for the project for which group information needs to be
     *        retrieved
     *
     * @return List of TFS Global and Project group display names.
     */
    public String[] getGlobalAndProjectGroups(final GUID serverGuid, final GUID projectGuid) {
        return witContext.getMetadata().getConstantsTable().getUserGroupDisplayNames(
            serverGuid.getGUIDString(GUIDStringFormat.DASHED),
            projectGuid.getGUIDString(GUIDStringFormat.DASHED));
    }

    /**
     * Closes this work item client down. The database connection pool is closed
     * and JDBC connections are released.
     */
    @Override
    public void close() {
        if (witContext != null) {
            witContext.shutdown();
        }
    }

    synchronized WITContext getContext() {
        if (witContext == null) {
            witContext = new WITContext(this);
            witContext.getMetadataUpdateHandler().addMetadataChangeListener(new IMetadataChangeListener() {
                @Override
                public void metadataChanged(final Set<String> tableNames) {
                    metadataChangedCallback(tableNames);
                }
            });
        }
        return witContext;
    }

    private void metadataChangedCallback(final Set<String> tableNames) {
        synchronized (this) {
            if (projectCollection != null) {
                for (final Iterator<Project> it = projectCollection.iterator(); it.hasNext();) {
                    final Project project = it.next();
                    project.clearCachedWITMetadata();
                }
            }

            projectCollection = null;
            fieldDefinitions = null;
            linkTypes = null;
            mapGroupDataProviders = null;
        }
    }

    public WorkItem newWorkItem(final WorkItemType inputType) {
        final WorkItemImpl workItem = new WorkItemImpl(getContext());
        getContext().initNewWorkItem(workItem, inputType);
        workItem.open();
        return workItem;
    }

    public FieldDefinitionCollection getFieldDefinitions() {
        synchronized (this) {
            if (fieldDefinitions == null) {
                fieldDefinitions = new FieldDefinitionCollectionImpl(false, getContext(), null);
            }
            return fieldDefinitions;
        }
    }

    public RegisteredLinkTypeCollection getRegisteredLinkTypes() {
        return new RegisteredLinkTypeCollectionImpl(getContext());
    }

    public WorkItemLinkTypeCollection getLinkTypes() {
        synchronized (this) {
            if (linkTypes == null) {
                linkTypes = new WorkItemLinkTypeCollectionImpl(getContext());
            }
            return linkTypes;
        }
    }

    public String getUpdateXMLForDebugging(final WorkItem workItem) {
        return ((WorkItemImpl) workItem).getUpdateXMLForDebugging();
    }

    public TFSTeamProjectCollection getConnection() {
        return connection;
    }

    /**
     * Similar to "WorkItemStore#GetStoredQuery(Guid)" in MS code.
     */
    public StoredQuery getStoredQuery(final GUID guid) {
        return getContext().getQueryProvider().getQuery(guid);
    }

    public Query createReferencingQuery(final String artifactUri) {
        ArtifactID.checkURIIsWellFormed(artifactUri);

        final String[] referencingWorkItems;
        if (version.getValue() <= 2) {
            referencingWorkItems = getWebService2().getReferencingWorkitemUris(artifactUri);
        } else if (version.getValue() == 3) {
            referencingWorkItems = getWebService3().getReferencingWorkitemUris(artifactUri);
        } else {
            referencingWorkItems = getWebService5().getReferencingWorkitemUris(artifactUri);
        }

        final BatchReadParameterCollection batchReadParams = new BatchReadParameterCollection();

        if (referencingWorkItems != null) {
            for (int i = 0; i < referencingWorkItems.length; i++) {
                final int id = Integer.parseInt(referencingWorkItems[i]);
                batchReadParams.add(new BatchReadParameter(id));
            }
        }

        return createQuery("select [System.Id] from workitems", batchReadParams); //$NON-NLS-1$
    }

    public Query createQuery(final String wiql, final Map<String, Object> queryContext)
        throws InvalidQueryTextException {
        return new QueryImpl(getContext(), wiql, queryContext);
    }

    public Query createQuery(final String wiql) throws InvalidQueryTextException {
        return new QueryImpl(getContext(), wiql);
    }

    public Query createQuery(final String wiql, final BatchReadParameterCollection batchReadParams)
        throws InvalidQueryTextException {
        return new QueryImpl(getContext(), wiql, batchReadParams);
    }

    public Query createEmptyQuery() {
        return new NullQueryImpl(getContext());
    }

    public WorkItemCollection query(
        final String wiql,
        final Map<String, Object> queryContext,
        final boolean dayPrecision) throws InvalidQueryTextException {
        return new QueryImpl(getContext(), wiql, queryContext, dayPrecision).runQuery();
    }

    public WorkItemCollection query(final String wiql, final Map<String, Object> queryContext)
        throws InvalidQueryTextException {
        return new QueryImpl(getContext(), wiql, queryContext).runQuery();
    }

    public WorkItemCollection query(final int[] ids, final String wiql) throws InvalidQueryTextException {
        return new QueryImpl(getContext(), wiql, ids, null).runQuery();
    }

    public WorkItemCollection query(final String wiql) throws InvalidQueryTextException {
        return new QueryImpl(getContext(), wiql).runQuery();
    }

    public WorkItemCollection query(final String wiql, final BatchReadParameterCollection batchReadParams)
        throws InvalidQueryTextException {
        return new QueryImpl(getContext(), wiql, batchReadParams).runQuery();
    }

    public void validateWIQL(final String wiql) throws InvalidQueryTextException {
        StoredQueryImpl.validateWIQL(getContext(), wiql);
    }

    public IProcessGuidance getProcessGuidance() {
        return new WSSProcessGuidance(getConnection());
    }

    /*
     * Taken from DocumentService#getDefaultParent
     */
    public static QueryFolder getDefaultParent(final Project project, final boolean isPublic) {
        final QueryItem[] roots = project.getQueryHierarchy().getItems();

        for (int i = 0; i < roots.length; i++) {
            if (roots[i] instanceof QueryFolder && roots[i].isPersonal() == !isPublic) {
                return (QueryFolder) roots[i];
            }
        }

        return null;
    }

    public UserDisplayMode getUserDisplayMode() {
        final int displayMode = getContext().getMetadata().getUserDisplayMode();
        if (displayMode == 1) {
            return UserDisplayMode.ACCOUNT_NAME;
        }
        return UserDisplayMode.FRIENDLY_NAME;
    }

    public String getUserDisplayName() {
        if (UserDisplayMode.ACCOUNT_NAME == getUserDisplayMode()) {
            return getConnection().getAuthorizedAccountName();
        } else {
            return getConnection().getAuthorizedIdentity().getDisplayName();
        }
    }

    public WorkItemEventEngine getEventEngine() {
        return eventEngine;
    }
}
