// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem;

/**
 * A list of IDs for all known supported features of the work item services of a
 * Team Foundation Server.
 *
 * @since TEE-SDK-10.1
 */
public class SupportedFeatures {
    // Field types
    public static final String GUID_FIELDS = "GuidFields"; //$NON-NLS-1$
    public static final String BOOLEAN_FIELDS = "BooleanFields"; //$NON-NLS-1$

    public static final String QUERY_FOLDERS = "QueryFolders"; //$NON-NLS-1$
    public static final String QUERY_FOLDER_PERMISSIONS = "QueryFolderPermissions"; //$NON-NLS-1$
    public static final String QUERY_FOLDER_SET_OWNER = "QueryFolderSetOwner"; //$NON-NLS-1$
    public static final String QUERY_FIELDS_COMPARISON = "QueryFieldsComparison"; //$NON-NLS-1$
    public static final String QUERY_HISTORICAL_REVISIONS = "QueryHistoricalRevisions"; //$NON-NLS-1$
    public static final String QUERY_IN_GROUP_FILTER = "QueryInGroup"; //$NON-NLS-1$

    public static final String WORK_ITEM_TYPE_CATEGORIES = "WorkItemTypeCategories"; //$NON-NLS-1$
    public static final String WORK_ITEM_TYPE_CATEGORY_MEMBERS = "WorkItemTypeCategoryMembers"; //$NON-NLS-1$
    public static final String WORK_ITEM_LINKS = "WorkItemLinks"; //$NON-NLS-1$
    public static final String WORK_ITEM_LINK_LOCKS = "WorkItemLinkLocks"; //$NON-NLS-1$

    public static final String BATCH_SAVE_WORK_ITEMS_FROM_DIFFERENT_PROJECTS =
        "BatchSaveWorkItemsFromDifferentProjects"; //$NON-NLS-1$

    public static final String QUERY_RECURSIVE_RETURN_MATCHING_CHILDREN = "QueryRecursiveReturnMatchingChildren"; //$NON-NLS-1$
    public static final String PROVISION_PERMISSION = "ProvisionPermission"; //$NON-NLS-1$
    public static final String CONFIGURABLE_BULK_UPDATE_BATCH_SIZE = "ConfigurableBulkUpdateBatchSize"; //$NON-NLS-1$

    public static final String SYNC_NAME_CHANGES = "SyncNameChanges"; //$NON-NLS-1$
    public static final String REPORTING_NAMES = "ReportingNames"; //$NON-NLS-1$
    public static final String SET_REPORTING_TYPE_TO_NONE = "SetReportingTypeToNone"; //$NON-NLS-1$

    public static final String WIQL_EVALUATION_ON_SERVER = "WiqlEvaluationOnServer"; //$NON-NLS-1$

    private SupportedFeatures() {
        // DO NOT CONSTRUCT
    };

}
