// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.internal;

import java.text.MessageFormat;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.microsoft.tfs.core.Messages;
import com.microsoft.tfs.core.TFSTeamProjectCollection;
import com.microsoft.tfs.core.clients.registration.RegistrationClient;
import com.microsoft.tfs.core.clients.registration.ToolNames;
import com.microsoft.tfs.core.clients.versioncontrol.VersionControlClient;
import com.microsoft.tfs.core.clients.workitem.ServerInfo;
import com.microsoft.tfs.core.clients.workitem.WorkItemClient;
import com.microsoft.tfs.core.clients.workitem.WorkItemServerVersion;
import com.microsoft.tfs.core.clients.workitem.internal.fields.DatastoreItemFieldUsagesCollection;
import com.microsoft.tfs.core.clients.workitem.internal.fields.FieldDefinitionCollectionImpl;
import com.microsoft.tfs.core.clients.workitem.internal.fields.FieldDefinitionImpl;
import com.microsoft.tfs.core.clients.workitem.internal.fields.FieldModificationType;
import com.microsoft.tfs.core.clients.workitem.internal.metadata.IMetadata;
import com.microsoft.tfs.core.clients.workitem.internal.metadata.IMetadataChangeListener;
import com.microsoft.tfs.core.clients.workitem.internal.metadata.IMetadataUpdateHandler;
import com.microsoft.tfs.core.clients.workitem.internal.metadata.Metadata;
import com.microsoft.tfs.core.clients.workitem.internal.metadata.dao.ConstantMetadata;
import com.microsoft.tfs.core.clients.workitem.internal.metadata.dao.NodeMetadata;
import com.microsoft.tfs.core.clients.workitem.internal.node.NodeImpl;
import com.microsoft.tfs.core.clients.workitem.internal.query.StoredQueryProviderImpl;
import com.microsoft.tfs.core.clients.workitem.internal.rules.cache.IRuleCache;
import com.microsoft.tfs.core.clients.workitem.internal.rules.cache.RuleCache;
import com.microsoft.tfs.core.clients.workitem.wittype.WorkItemType;
import com.microsoft.tfs.core.internal.db.ConnectionConfiguration;
import com.microsoft.tfs.core.internal.db.ConnectionPool;
import com.microsoft.tfs.util.tasks.TaskMonitorService;

import ms.tfs.workitemtracking.clientservices._03._ClientService2Soap;
import ms.tfs.workitemtracking.clientservices._03._ClientService3Soap;
import ms.tfs.workitemtracking.clientservices._03._ClientService5Soap;

/**
 * Internal class that represents a WIT context.
 *
 * This class is roughly equivalent to the internal portions of "WorkItemStore"
 * in MS code. There are also some similarities between this class and the
 * "DatastoreClass" in MS code.
 */
public class WITContext implements IWITContext, IMetadataChangeListener {
    private static final Log log = LogFactory.getLog(WITContext.class);

    private final WorkItemClient client;

    private final Metadata metadata;
    private ConstantMetadata currentUserConstant;
    private final String productValue;
    private final ConnectionPool connectionPool;
    private final ConnectionConfiguration connectionConfiguration;
    private String attachmentServerUrl;
    private NodeImpl rootNode;
    private FieldDefinitionCollectionImpl fieldDefinitions;
    private DatastoreItemFieldUsagesCollection workItemFieldUsages;
    private final StoredQueryProviderImpl queryProvider;
    private final QueryHierarchyProvider queryHierarchyProvider;
    private final RuleCache ruleCache;
    private final ServerInfo serverInfo;
    private final WorkItemServerVersion version;

    public WITContext(final WorkItemClient client) {
        this.client = client;

        /*
         * use the version control client to get the server GUID of the
         * connected server
         */
        final VersionControlClient vcClient =
            (VersionControlClient) client.getConnection().getClient(VersionControlClient.class);
        final String guid = vcClient.getServerGUID().getGUIDString();

        /*
         * Set up the database configuration and create a database connection
         * pool
         */
        connectionConfiguration = new ConnectionConfiguration(
            client.getConnection().getPersistenceStoreProvider().getCachePersistenceStore(),
            guid);
        connectionPool = new ConnectionPool(connectionConfiguration);

        /*
         * metadata
         */
        metadata = new Metadata(
            connectionPool,
            client.getVersion(),
            client.getWebService2(),
            client.getWebService3(),
            client.getWebService5());
        TaskMonitorService.getTaskMonitor().setCurrentWorkDescription(
            Messages.getString("WITContext.TaskUpdatingClientMetadata")); //$NON-NLS-1$
        metadata.update();
        metadata.addMetadataChangeListener(this);

        /*
         * current user display name and constid
         */
        calculateCurrentUser();

        /*
         * Server Version
         */
        version = client.getVersion();

        /*
         * product value
         */
        if (isVersion2()) {
            productValue = client.getConnection().getWebServiceURI(client.getWebService2()).toString();
        } else {
            productValue = client.getConnection().getWebServiceURI(client.getWebService3()).toString();
        }

        /*
         * server info
         */
        serverInfo = new ServerInfo(client.getVersion());

        /*
         * stored query provider
         */
        queryProvider = new StoredQueryProviderImpl(this);

        /*
         * query hierarchy provider
         */
        queryHierarchyProvider = new QueryHierarchyProvider(this);

        /*
         * rule cache
         */
        ruleCache = new RuleCache(this);
    }

    @Override
    public synchronized void metadataChanged(final Set<String> tableNames) {
        ruleCache.clearCache();
        rootNode = null;
        fieldDefinitions = null;
        workItemFieldUsages = null;
    }

    private void calculateCurrentUser() {
        final String currentUserAndDomain = client.getConnection().getAuthorizedTFSUser().toString();

        currentUserConstant = getMetadata().getConstantsTable().getConstantByString(currentUserAndDomain);

        if (currentUserConstant == null) {
            final String messageFormat = Messages.getString("WITContext.CurrentUserNotFoundFormat"); //$NON-NLS-1$
            final String message = MessageFormat.format(messageFormat, currentUserAndDomain);
            throw new IllegalStateException(message);
        }
    }

    /**
     * Equivalent to "WorkItemStore#PsFieldDefinitions" in MS code.
     */
    public synchronized FieldDefinitionCollectionImpl getFieldDefinitions() {
        if (fieldDefinitions == null) {
            fieldDefinitions = new FieldDefinitionCollectionImpl(true, this, null);
        }
        return fieldDefinitions;
    }

    /**
     * Equivalent to "WorkItemStore#PsFieldUsages" in MS code.
     */
    public synchronized DatastoreItemFieldUsagesCollection getWorkItemFieldUsages() {
        if (workItemFieldUsages == null) {
            final FieldDefinitionImpl workItemFieldDefinition =
                getFieldDefinitions().getFieldDefinitionInternal(WorkItemFieldIDs.WORK_ITEM);
            workItemFieldUsages = workItemFieldDefinition.getFieldUsageMetadata();
        }
        return workItemFieldUsages;
    }

    DatastoreItemFieldUsagesCollection workItemLinkFieldUsages;

    public synchronized DatastoreItemFieldUsagesCollection getWorkItemLinkFieldUsages() {
        if (workItemLinkFieldUsages == null) {
            final FieldDefinitionImpl workItemFieldDefinition =
                getFieldDefinitions().getFieldDefinitionInternal(WorkItemFieldIDs.WORK_ITEM_LINK);
            workItemLinkFieldUsages = workItemFieldDefinition.getFieldUsageMetadata();
        }
        return workItemLinkFieldUsages;
    }

    public synchronized String getAttachmentServerURL() {
        if (attachmentServerUrl == null) {
            final RegistrationClient registrationClient = getConnection().getRegistrationClient();

            attachmentServerUrl = registrationClient.getExtendedAttributeValue(
                ToolNames.WORK_ITEM_TRACKING,
                InternalWorkItemConstants.ATTACHMENT_SERVER_URL_EXTENDED_ATTRIBUTE_NAME);

            if (attachmentServerUrl == null) {
                log.error("attachment server url attribute was not located"); //$NON-NLS-1$
            } else {
                if (!attachmentServerUrl.toLowerCase().startsWith("http")) //$NON-NLS-1$
                {
                    if (attachmentServerUrl.startsWith("/")) //$NON-NLS-1$
                    {
                        attachmentServerUrl = attachmentServerUrl.substring(1);
                    }

                    attachmentServerUrl = getConnection().getBaseURI().resolve(attachmentServerUrl).toString();
                }
            }
        }
        return attachmentServerUrl;
    }

    public void shutdown() {
        connectionPool.shutdown();
    }

    @Override
    public String getCurrentUserDisplayName() {
        return currentUserConstant.getDisplayName();
    }

    public int getCurrentUserConstID() {
        return currentUserConstant.getConstID();
    }

    @Override
    public IMetadata getMetadata() {
        return metadata;
    }

    public IMetadataUpdateHandler getMetadataUpdateHandler() {
        return metadata;
    }

    public String getProductValue() {
        return productValue;
    }

    public WorkItemServerVersion getVersion() {
        return client.getVersion();
    }

    public _ClientService2Soap getProxy() {
        return client.getWebService2();
    }

    public _ClientService3Soap getProxy3() {
        return client.getWebService3();
    }

    public _ClientService5Soap getProxy5() {
        return client.getWebService5();
    }

    public TFSTeamProjectCollection getConnection() {
        return client.getConnection();
    }

    public ConnectionConfiguration getConnectionConfiguration() {
        return connectionConfiguration;
    }

    public WorkItemClient getClient() {
        return client;
    }

    public synchronized NodeImpl getRootNode() {
        if (rootNode == null) {
            final NodeMetadata rootNodeMetadata = metadata.getHierarchyTable().getRootNode();

            rootNode = new NodeImpl(rootNodeMetadata, null, this);
        }
        return rootNode;
    }

    public ServerInfo getServerInfo() {
        return serverInfo;
    }

    /**
     * Equivalent to "WorkItemStore#QueryProvider" in MS code.
     */
    public StoredQueryProviderImpl getQueryProvider() {
        return queryProvider;
    }

    /**
     * Equivalent to "WorkItemStore#QueryHierarchyProvider" in MS code.
     */
    public synchronized QueryHierarchyProvider getQueryHierarchyProvider() {
        return queryHierarchyProvider;
    }

    @Override
    public IRuleCache getRuleCache() {
        return ruleCache;
    }

    public boolean isVersion2() {
        return version.getValue() <= WorkItemServerVersion.V2.getValue();
    }

    public boolean isVersion3() {
        return version.getValue() == WorkItemServerVersion.V3.getValue();
    }

    public boolean isVersion5() {
        return version.getValue() >= WorkItemServerVersion.V5.getValue();
    }

    public boolean isVersion3OrHigher() {
        return version.getValue() >= WorkItemServerVersion.V3.getValue();
    }

    /**
     * <p>
     * Initializes a new WorkItem object. This method should be used when
     * constructing a new, unsaved work item. Do not use this method when
     * constructing a WorkItem object corresponding to an existing work item on
     * the server.
     * </p>
     * <p>
     * This method sets several initial fields of the new work item but <b>does
     * not</b> open it.
     * </p>
     *
     * @param workItem
     *        a new WorkItem object
     */
    public void initNewWorkItem(final WorkItemImpl workItem, final WorkItemType type) {
        /*
         * add in all fields from the work item physical type
         */
        workItem.getFieldsInternal().ensureAllFieldsInWIPhysicalType();

        /*
         * set the work item type to the passed in work item type
         */
        workItem.getFieldsInternal().getFieldInternal(WorkItemFieldIDs.WORK_ITEM_TYPE).setValue(
            type.getName(),
            FieldModificationType.NEW);

        /*
         * set the area and iteration node id fields to the project node id
         * TODO: once hierarchy tree ACLs are implemented, this needs to set the
         * area and iteration to the first writeable area and iteration node for
         * this user
         */
        final Integer projectId = new Integer(type.getProject().getID());
        workItem.getFieldsInternal().getFieldInternal(WorkItemFieldIDs.AREA_ID).setValue(
            projectId,
            FieldModificationType.NEW);
        workItem.getFieldsInternal().getFieldInternal(WorkItemFieldIDs.ITERATION_ID).setValue(
            projectId,
            FieldModificationType.NEW);

        /*
         * set the id and revision to 0
         */
        workItem.getFieldsInternal().getFieldInternal(WorkItemFieldIDs.ID).setValue(
            new Integer(0),
            FieldModificationType.NEW);
        workItem.getFieldsInternal().getFieldInternal(WorkItemFieldIDs.REVISION).setValue(
            new Integer(0),
            FieldModificationType.NEW);
    }
}
