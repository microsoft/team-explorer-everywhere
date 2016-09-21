// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.framework.internal;

/**
 * TFS 2010 service names. Some of these have changed from 2008 (WSS, most
 * notably).
 *
 * @threadsafety immutable
 */
public final class ServiceInterfaceNames {
    public static final String ADMINMISTRATION = "AdministrationService"; //$NON-NLS-1$
    public static final String BUILD = "BuildService"; //$NON-NLS-1$
    public static final String BUILD_3 = "BuildService3"; //$NON-NLS-1$
    public static final String BUILD_4 = "BuildService4"; //$NON-NLS-1$
    public static final String BUILD_CONTROLLER = "BuildControllerService"; //$NON-NLS-1$
    public static final String BUILD_STORE = "BuildStoreService"; //$NON-NLS-1$
    public static final String BUILD_QUEUE_SERVICE = "BuildQueueService"; //$NON-NLS-1$
    public static final String BUILD_QUEUE_SERVICE_4 = "BuildQueueService4"; //$NON-NLS-1$
    public static final String BUILD_ADMINISTRATION_SERVICE = "AdministrationService"; //$NON-NLS-1$
    public static final String BUILD_ADMINISTRATION_SERVICE_4 = "AdministrationService4"; //$NON-NLS-1$
    public static final String BUILD_AGENT_RESERVATION_SERVICE = "AgentReservationService"; //$NON-NLS-1$
    public static final String CATALOG = "CatalogService"; //$NON-NLS-1$
    public static final String COMMON_STRUCTURE = "CommonStructure"; //$NON-NLS-1$
    public static final String COMMON_STRUCTURE_3 = "CommonStructure3"; //$NON-NLS-1$
    public static final String COMMON_STRUCTURE_4 = "CommonStructure4"; //$NON-NLS-1$
    public static final String DATA_SOURCE_SERVER = "DataSourceServer"; //$NON-NLS-1$
    public static final String EVENTING = "Eventing"; //$NON-NLS-1$
    public static final String FILE_CONTAINER_SERVICE = "FileContainersResource"; //$NON-NLS-1$
    public static final String GROUP_SECURITY = "GroupSecurity"; //$NON-NLS-1$
    public static final String LOCATION = "LocationService"; //$NON-NLS-1$
    public static final String SECURITY = "SecurityService"; //$NON-NLS-1$
    public static final String PROPERTY = "PropertyService"; //$NON-NLS-1$
    public static final String IDENTITY_MANAGEMENT = "IdentityManagementService"; //$NON-NLS-1$
    public static final String IDENTITY_MANAGEMENT_2 = "IdentityManagementService2"; //$NON-NLS-1$
    public static final String LINKING = "LinkingProviderService"; //$NON-NLS-1$
    public static final String METHODOLOGY_UPLOAD = "MethodologyUpload"; //$NON-NLS-1$
    public static final String METHODOLOGY = "Methodology"; //$NON-NLS-1$
    public static final String REGISTRATION = "RegistrationService"; //$NON-NLS-1$
    public static final String REGISTRY = "RegistryService"; //$NON-NLS-1$
    public static final String SERVER_STATUS = "ServerStatus"; //$NON-NLS-1$
    public static final String TEAM_CONFIGURATION = "TeamConfigurationService"; //$NON-NLS-1$
    public static final String TEAM_PROJECT_COLLECTION = "TeamProjectCollectionService"; //$NON-NLS-1$
    public static final String TEST_RESULTS = "TestResultsService"; //$NON-NLS-1$
    public static final String TEST_IMPACT = "TestImpactService"; //$NON-NLS-1$
    public static final String VERSION_CONTROL = "ISCCProvider"; //$NON-NLS-1$
    public static final String VERSION_CONTROL_ADMIN = "ISCCAdmin"; //$NON-NLS-1$
    public static final String VERSION_CONTROL_3 = "ISCCProvider3"; //$NON-NLS-1$
    public static final String VERSION_CONTROL_4 = "ISCCProvider4"; //$NON-NLS-1$
    public static final String VERSION_CONTROL_4_DOT_1 = "ISCCProvider4.1"; //$NON-NLS-1$
    public static final String VERSION_CONTROL_4_DOT_2 = "ISCCProvider4.2"; //$NON-NLS-1$
    public static final String VERSION_CONTROL_4_DOT_3 = "ISCCProvider4.3"; //$NON-NLS-1$
    public static final String VERSION_CONTROL_5 = "ISCCProvider5"; //$NON-NLS-1$
    public static final String VERSION_CONTROL_5_DOT_1 = "ISCCProvider5.1"; //$NON-NLS-1$

    public static final String VERSION_CONTROL_UPLOAD = "Upload"; //$NON-NLS-1$
    public static final String VERSION_CONTROL_DOWNLOAD = "Download"; //$NON-NLS-1$
    public static final String WORK_ITEM = "WorkitemService"; //$NON-NLS-1$
    public static final String WORK_ITEM_2 = "WorkitemService2"; //$NON-NLS-1$
    public static final String WORK_ITEM_3 = "WorkitemService3"; //$NON-NLS-1$
    public static final String WORK_ITEM_5 = "WorkitemService5"; //$NON-NLS-1$
    public static final String WORK_ITEM_6 = "WorkitemService6"; //$NON-NLS-1$
    public static final String WORK_ITEM_7 = "WorkitemService7"; //$NON-NLS-1$
    public static final String WORK_ITEM_8 = "WorkitemService8"; //$NON-NLS-1$
    public static final String WORK_ITEM_ATTACHMENT_HANDLER = "WorkitemAttachmentHandler"; //$NON-NLS-1$
    public static final String WORK_ITEM_CONFIG = "ConfigurationSettingsUrl"; //$NON-NLS-1$

    // Whidbey/Orcas
    public static final String WSS_BASE_SITE_URL = "BaseSiteUrl"; //$NON-NLS-1$
    public static final String REPORTING = "ReportsService"; //$NON-NLS-1$
    public static final String REPORTING_WEB_SERVICE_URL = "ReportWebServiceUrl"; //$NON-NLS-1$
    public static final String REPORTING_MANAGER_URL = "BaseReportsUrl"; //$NON-NLS-1$

    // Rosario
    public static final String WSS_ROOT_SITE_URL = "WssRootUrl"; //$NON-NLS-1$

    // TSWA
    public static final String TSWA_HOME = "TSWAHome"; //$NON-NLS-1$
    public static final String TSWA_WORK_ITEM_EDITOR = "WorkItemEditor"; //$NON-NLS-1$
    public static final String TSWA_CHANGESET_DETAIL = "ChangesetDetail"; //$NON-NLS-1$
    public static final String TSWA_DIFFERENCE = "Difference"; //$NON-NLS-1$
    public static final String TSWA_VIEW_ITEM = "ViewItem"; //$NON-NLS-1$
    public static final String TSWA_SHELVESET_DETAIL = "ShelvesetDetail"; //$NON-NLS-1$
    public static final String TSWA_QUERY_RESULTS = "QueryResults"; //$NON-NLS-1$
    public static final String TSWA_ANNOTATE = "Annotate"; //$NON-NLS-1$
    public static final String TSWA_SOURCE_EXPLORER = "SourceExplorer"; //$NON-NLS-1$
    public static final String TSWA_OPEN_WORK_ITEM = "OpenWorkItem"; //$NON-NLS-1$
    public static final String TSWA_CREATE_WORK_ITEM = "CreateWorkItem"; //$NON-NLS-1$
    public static final String TSWA_VIEW_SERVER_QUERY_RESULTS = "ViewServerQueryResults"; //$NON-NLS-1$
    public static final String TSWA_VIEW_WIQL_QUERY_RESULTS = "ViewWiqlQueryResults"; //$NON-NLS-1$
    public static final String TSWA_EXPLORE_SOURCE_CONTROL_PATH = "ExploreSourceControlPath"; //$NON-NLS-1$
    public static final String TSWA_FIND_SHELVESET = "FindShelveset"; //$NON-NLS-1$
    public static final String TSWA_VIEW_SHELVESET_DETAILS = "ViewShelvesetDetails"; //$NON-NLS-1$
    public static final String TSWA_FIND_CHANGESET = "FindChangeset"; //$NON-NLS-1$
    public static final String TSWA_VIEW_CHANGESET_DETAILS = "ViewChangesetDetails"; //$NON-NLS-1$
    public static final String TSWA_VIEW_SOURCE_CONTROL_ITEM = "ViewSourceControlItem"; //$NON-NLS-1$
    public static final String TSWA_DOWNLOAD_SOURCE_CONTROL_ITEM = "DownloadSourceControlItem"; //$NON-NLS-1$
    public static final String TSWA_DIFF_SOURCE_CONTROL_ITEMS = "DiffSourceControlItems"; //$NON-NLS-1$
    public static final String TSWA_ANNOTATE_SOURCE_CONTROL_ITEM = "AnnotateSourceControlItem"; //$NON-NLS-1$
    public static final String TSWA_VIEW_SOURCE_CONTROL_ITEM_HISTORY = "ViewSourceControlItemHistory"; //$NON-NLS-1$
    public static final String TSWA_VIEW_BUILD_DETAILS = "ViewBuildDetails"; //$NON-NLS-1$
    public static final String TSWA_VIEW_SOURCE_CONTROL_SHELVED_ITEM = "ViewSourceControlShelvedItem"; //$NON-NLS-1$
    public static final String TSWA_DIFF_SOURCE_CONTROL_SHELVED_ITEM = "DiffSourceControlShelvedItem"; //$NON-NLS-1$

    public static final String TSWA_IDENTITY_MANAGEMENT = "IdentityManagementWeb"; //$NON-NLS-1$
    public static final String TSWA_SECURITY_MANAGEMENT = "SecurityManagementWeb"; //$NON-NLS-1$
    public static final String TSWA_AREAS_MANAGEMENT = "AreasManagementWeb"; //$NON-NLS-1$
    public static final String TSWA_ITERATIONS_MANAGEMENT = "IterationsManagementWeb"; //$NON-NLS-1$
    public static final String TSWA_PROJECT_ALERTS = "ProjectAlertsWeb"; //$NON-NLS-1$

    public static final String TF_VIEW_SOURCE_CONTROL = "SourceControlWeb"; //$NON-NLS-1$
    public static final String TF_VIEW_WORK_ITEMS = "WorkItemsWeb"; //$NON-NLS-1$
    public static final String TF_VIEW_BUILDS = "BuildsWeb"; //$NON-NLS-1$
    public static final String TF_VIEW_SETTINGS = "AdministrationsWeb"; //$NON-NLS-1$

    public static final String GIT_VIEW_COMMIT_DETAILS = "CommitDetailsWeb"; //$NON-NLS-1$
    public static final String GIT_VIEW_REF_DETAILS = "GitRefDetailsWeb"; //$NON-NLS-1$
}
