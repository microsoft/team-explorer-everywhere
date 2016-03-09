// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core;

import java.text.MessageFormat;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.microsoft.tfs.core.clients.authorization.QueryMembership;
import com.microsoft.tfs.core.clients.authorization.SearchFactor;
import com.microsoft.tfs.core.clients.framework.ServerDataProvider;
import com.microsoft.tfs.core.clients.framework.location.AccessMapping;
import com.microsoft.tfs.core.clients.framework.location.ConnectOptions;
import com.microsoft.tfs.core.clients.framework.location.LocationService;
import com.microsoft.tfs.core.clients.framework.location.ServiceDefinition;
import com.microsoft.tfs.core.clients.groupsecurity.GroupSecurityClient;
import com.microsoft.tfs.core.clients.registration.RegistrationClient;
import com.microsoft.tfs.core.clients.registration.RegistrationEntry;
import com.microsoft.tfs.core.clients.registration.ServiceInterface;
import com.microsoft.tfs.core.clients.registration.ToolNames;
import com.microsoft.tfs.core.clients.serverstatus.ServerStatusClient;
import com.microsoft.tfs.core.clients.webservices.TeamFoundationIdentity;
import com.microsoft.tfs.core.exceptions.FrameworkMethodNotImplementedException;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.GUID;

/**
 * <p>
 * Gets server configuration information from Team Foundation Servers which
 * predate the TFS 2010 framework services (TFS 2005, TFS 2008).
 * </p>
 *
 * @since TEE-SDK-10.1
 * @threadsafety thread-safe
 */
public class PreFrameworkServerDataProvider implements ServerDataProvider {
    private final static Log log = LogFactory.getLog(PreFrameworkServerDataProvider.class);

    private final TFSTeamProjectCollection teamProjectCollection;

    /**
     * This is the client we use to talk to registration to get our data. Lazily
     * initialized by {@link #getRegistrationClient()}.
     */
    private RegistrationClient registrationClient;
    private final Object registrationClientLock = new Object();

    /**
     * Lazily initialized by {@link #getAuthenticatedIdentity()}.
     */
    private volatile TeamFoundationIdentity authenticatedIdentity;

    /**
     * Maps new service type IDs to legacy tool names.
     */
    private static final Map<String, String> SERVICE_TYPE_IDENTIFIER_TO_TOOL =
        new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER);

    static {
        SERVICE_TYPE_IDENTIFIER_TO_TOOL.put("BuildStoreService", ToolNames.TEAM_BUILD); //$NON-NLS-1$
        SERVICE_TYPE_IDENTIFIER_TO_TOOL.put("BuildControllerService", ToolNames.TEAM_BUILD); //$NON-NLS-1$
        SERVICE_TYPE_IDENTIFIER_TO_TOOL.put(
            getServiceTypeIdentifierKey("LinkingProviderService", new GUID("204687d1-5df8-493c-bcda-e43a19b935e9")), //$NON-NLS-1$ //$NON-NLS-2$
            ToolNames.TEAM_BUILD);
        SERVICE_TYPE_IDENTIFIER_TO_TOOL.put("PublishTestResultsBuildService", ToolNames.TEAM_BUILD); //$NON-NLS-1$
        SERVICE_TYPE_IDENTIFIER_TO_TOOL.put("BuildService", ToolNames.TEAM_BUILD); //$NON-NLS-1$
        SERVICE_TYPE_IDENTIFIER_TO_TOOL.put("CommonStructure", ToolNames.CORE_SERVICES); //$NON-NLS-1$
        SERVICE_TYPE_IDENTIFIER_TO_TOOL.put("Eventing", ToolNames.CORE_SERVICES); //$NON-NLS-1$
        SERVICE_TYPE_IDENTIFIER_TO_TOOL.put("GroupSecurity", ToolNames.CORE_SERVICES); //$NON-NLS-1$
        SERVICE_TYPE_IDENTIFIER_TO_TOOL.put("GroupSecurity2", ToolNames.CORE_SERVICES); //$NON-NLS-1$
        SERVICE_TYPE_IDENTIFIER_TO_TOOL.put("Authorization", ToolNames.CORE_SERVICES); //$NON-NLS-1$
        SERVICE_TYPE_IDENTIFIER_TO_TOOL.put("ProcessTemplate", ToolNames.CORE_SERVICES); //$NON-NLS-1$
        SERVICE_TYPE_IDENTIFIER_TO_TOOL.put("Methodology", ToolNames.CORE_SERVICES); //$NON-NLS-1$
        SERVICE_TYPE_IDENTIFIER_TO_TOOL.put("MethodologyUpload", ToolNames.CORE_SERVICES); //$NON-NLS-1$
        SERVICE_TYPE_IDENTIFIER_TO_TOOL.put("RegistrationService", ToolNames.CORE_SERVICES); //$NON-NLS-1$
        SERVICE_TYPE_IDENTIFIER_TO_TOOL.put("ServerStatus", ToolNames.CORE_SERVICES); //$NON-NLS-1$
        SERVICE_TYPE_IDENTIFIER_TO_TOOL.put(
            getServiceTypeIdentifierKey("IProjectMaintenance  ", new GUID("855c71c3-4f2c-4fde-a140-fb265f0ff0fa")), //$NON-NLS-1$ //$NON-NLS-2$
            ToolNames.CORE_SERVICES);
        SERVICE_TYPE_IDENTIFIER_TO_TOOL.put("ITeamSystemTask", ToolNames.CORE_SERVICES); //$NON-NLS-1$
        SERVICE_TYPE_IDENTIFIER_TO_TOOL.put("Warehouse", ToolNames.CORE_SERVICES); //$NON-NLS-1$
        SERVICE_TYPE_IDENTIFIER_TO_TOOL.put("ReportsService", ToolNames.WAREHOUSE); //$NON-NLS-1$
        SERVICE_TYPE_IDENTIFIER_TO_TOOL.put("BaseReportsUrl", ToolNames.WAREHOUSE); //$NON-NLS-1$
        SERVICE_TYPE_IDENTIFIER_TO_TOOL.put("DataSourceServer", ToolNames.WAREHOUSE); //$NON-NLS-1$
        SERVICE_TYPE_IDENTIFIER_TO_TOOL.put("WssAdminService", ToolNames.SHAREPOINT); //$NON-NLS-1$
        SERVICE_TYPE_IDENTIFIER_TO_TOOL.put("BaseServerUrl", ToolNames.SHAREPOINT); //$NON-NLS-1$
        SERVICE_TYPE_IDENTIFIER_TO_TOOL.put("BaseSiteUrl", ToolNames.SHAREPOINT); //$NON-NLS-1$
        SERVICE_TYPE_IDENTIFIER_TO_TOOL.put("BaseSiteUnc", ToolNames.SHAREPOINT); //$NON-NLS-1$
        SERVICE_TYPE_IDENTIFIER_TO_TOOL.put("WorkitemService", ToolNames.WORK_ITEM_TRACKING); //$NON-NLS-1$
        SERVICE_TYPE_IDENTIFIER_TO_TOOL.put("ConfigurationSettingsUrl", ToolNames.WORK_ITEM_TRACKING); //$NON-NLS-1$
        SERVICE_TYPE_IDENTIFIER_TO_TOOL.put(
            getServiceTypeIdentifierKey("IBISEnablement", new GUID("3c2d1e28-018a-4b97-8989-30695a7e335f")), //$NON-NLS-1$ //$NON-NLS-2$
            ToolNames.WORK_ITEM_TRACKING);
        SERVICE_TYPE_IDENTIFIER_TO_TOOL.put(
            getServiceTypeIdentifierKey("LinkingProviderService", new GUID("40329fc1-f737-4ef8-807f-b91856676a56")), //$NON-NLS-1$ //$NON-NLS-2$
            ToolNames.WORK_ITEM_TRACKING);
        SERVICE_TYPE_IDENTIFIER_TO_TOOL.put("LinkingConsumerService", ToolNames.WORK_ITEM_TRACKING); //$NON-NLS-1$
        SERVICE_TYPE_IDENTIFIER_TO_TOOL.put(
            getServiceTypeIdentifierKey("IProjectMaintenance  ", new GUID("9de13285-caec-43cd-8c20-2848dd58d7dd")), //$NON-NLS-1$ //$NON-NLS-2$
            ToolNames.WORK_ITEM_TRACKING);
        SERVICE_TYPE_IDENTIFIER_TO_TOOL.put("ISCCProvider", ToolNames.VERSION_CONTROL); //$NON-NLS-1$
        SERVICE_TYPE_IDENTIFIER_TO_TOOL.put("ISCCAdmin", ToolNames.VERSION_CONTROL); //$NON-NLS-1$
        SERVICE_TYPE_IDENTIFIER_TO_TOOL.put(
            getServiceTypeIdentifierKey("IBISEnablement", new GUID("64de386c-b1be-41ad-b8cb-27ca22d48563")), //$NON-NLS-1$ //$NON-NLS-2$
            ToolNames.VERSION_CONTROL);
        SERVICE_TYPE_IDENTIFIER_TO_TOOL.put(
            getServiceTypeIdentifierKey("LinkingProviderService", new GUID("10a3ab2b-7140-4b4b-a72a-0feca94d5b6d")), //$NON-NLS-1$ //$NON-NLS-2$
            ToolNames.VERSION_CONTROL);
        SERVICE_TYPE_IDENTIFIER_TO_TOOL.put(
            getServiceTypeIdentifierKey("IProjectMaintenance  ", new GUID("750c198b-30f9-4088-9649-30be07853310")), //$NON-NLS-1$ //$NON-NLS-2$
            ToolNames.VERSION_CONTROL);
        SERVICE_TYPE_IDENTIFIER_TO_TOOL.put("Download", ToolNames.VERSION_CONTROL); //$NON-NLS-1$
        SERVICE_TYPE_IDENTIFIER_TO_TOOL.put("Upload", ToolNames.VERSION_CONTROL); //$NON-NLS-1$
    }

    private static String getServiceTypeIdentifierKey(final String serviceType, final GUID identifier) {
        // We only need to take the identifier into account if this is one of
        // the service
        // interfaces that had multiple implementations.
        if ("IProjectMaintenance".equalsIgnoreCase(serviceType) //$NON-NLS-1$
            || "LinkingProviderService".equalsIgnoreCase(serviceType) //$NON-NLS-1$
            || "IBISEnablement".equalsIgnoreCase(serviceType)) //$NON-NLS-1$
        {
            return serviceType + " - " + identifier.getGUIDString(); //$NON-NLS-1$
        }

        return serviceType;
    }

    /**
     * Constructs a {@link PreFrameworkServerDataProvider} using a
     * {@link TFSTeamProjectCollection}. {@link TFSTeamProjectCollection} is
     * required, instead of {@link TFSConnection}, because this is the only kind
     * of connection whose services are available in a TFS 2005/2008 server.
     *
     * @param teamProjectCollection
     *        a {@link TFSTeamProjectCollection} configured to point to the
     *        correct server (must not be <code>null</code>)
     */
    public PreFrameworkServerDataProvider(final TFSTeamProjectCollection teamProjectCollection) {
        Check.notNull(teamProjectCollection, "teamProjectCollection"); //$NON-NLS-1$
        this.teamProjectCollection = teamProjectCollection;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public GUID getInstanceID() {
        return getRegistrationClient().getInstanceID();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public GUID getCatalogResourceID() {
        /**
         * Pre-Framework servers don't support the catalog service.
         */
        return GUID.EMPTY;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public TeamFoundationIdentity getAuthenticatedIdentity() {
        if (authenticatedIdentity == null) {
            authenticate();
        }

        return authenticatedIdentity;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public TeamFoundationIdentity getAuthorizedIdentity() {
        /*
         * No impersonation supported; just return authenticated user.
         */
        return getAuthenticatedIdentity();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hasAuthenticated() {
        return authenticatedIdentity != null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void ensureAuthenticated() {
        if (hasAuthenticated() == false) {
            authenticate();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void authenticate() {
        final ServerStatusClient serverStatusClient =
            (ServerStatusClient) teamProjectCollection.getClient(ServerStatusClient.class);

        final String authenticatedUserName = serverStatusClient.checkAuthentication();

        final GroupSecurityClient groupSecurityClient =
            (GroupSecurityClient) teamProjectCollection.getClient(GroupSecurityClient.class);

        authenticatedIdentity = groupSecurityClient.convert(
            groupSecurityClient.readIdentity(SearchFactor.ACCOUNT_NAME, authenticatedUserName, QueryMembership.NONE));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String locationForCurrentConnection(final ServiceDefinition serviceDefinition) {
        return locationForCurrentConnection(
            serviceDefinition.getServiceType(),
            new GUID(serviceDefinition.getIdentifier()));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String locationForCurrentConnection(final String serviceType, final GUID serviceIdentifier) {
        final String key = getServiceTypeIdentifierKey(serviceType, serviceIdentifier);

        String toolID = ""; //$NON-NLS-1$

        if (PreFrameworkServerDataProvider.SERVICE_TYPE_IDENTIFIER_TO_TOOL.containsKey(key)) {
            toolID = PreFrameworkServerDataProvider.SERVICE_TYPE_IDENTIFIER_TO_TOOL.get(key);
        }

        // If the tool ID is "", this service didn't exist on the orcas server,
        // and we'll search all tool types.
        return findServiceLocation(serviceType, toolID);
    }

    /**
     * Finds the location of the service with the provided service type. If this
     * service can only be retrieved using the {@link LocationService}, the bis
     * name will be ignored.
     *
     * @param serviceType
     *        The service type (or service interface) being requested (must not
     *        be <code>null</code>)
     * @param toolID
     *        The toolId of the service definition (must not be
     *        <code>null</code>)
     * @return the location for the service, as a full URI
     */
    public String findServiceLocation(final String serviceType, final String toolID) {
        Check.notNull(toolID, "toolID"); //$NON-NLS-1$

        /*
         * Perform this search manually because
         * RegistrationClient.getRegistrationEntry(String toolID) will error if
         * the toolID is "".
         */
        final RegistrationEntry[] entries = getRegistrationClient().getRegistrationEntries();
        if (entries != null && entries.length > 0) {
            for (int i = 0; i < entries.length; i++) {
                final RegistrationEntry entry = entries[i];

                if (toolID.length() == 0 || toolID.toLowerCase().equals(entry.getType().toLowerCase())) {
                    final ServiceInterface[] services = entries[i].getServiceInterfaces();

                    // Look for the service interface

                    for (int j = 0; j < services.length; j++) {
                        final ServiceInterface service = services[j];

                        if (serviceType.equalsIgnoreCase(service.getName())) {
                            // We now have the URL to talk to the source control
                            // web
                            // service.
                            return service.getURL();
                        }
                    }
                }
            }
        }

        log.debug(MessageFormat.format(
            "Couldn't find a ServiceInterface for toolID {0}, serviceType {1}", //$NON-NLS-1$
            toolID,
            serviceType));
        return null;
    }

    /**
     * Gets the {@link RegistrationClient} this
     * {@link PreFrameworkServerDataProvider} uses to locate services.
     *
     * @return the {@link RegistrationClient} (never <code>null</code>)
     */
    private RegistrationClient getRegistrationClient() {
        synchronized (registrationClientLock) {
            if (registrationClient == null) {
                registrationClient = new RegistrationClient(teamProjectCollection);
            }

            return registrationClient;
        }
    }

    /*
     * Methods below not implemented for pre-framework data providers.
     */

    @Override
    public void connect(final ConnectOptions connectOptions) {
        throw new FrameworkMethodNotImplementedException("connect"); //$NON-NLS-1$
    }

    @Override
    public String findServerLocation(final GUID serverGUID) {
        throw new FrameworkMethodNotImplementedException("findServerLocation"); //$NON-NLS-1$
    }

    @Override
    public void reactToPossibleServerUpdate(final int serverLastChangeId) {
        throw new FrameworkMethodNotImplementedException("reactToPossibleServerUpdate"); //$NON-NLS-1$
    }

    @Override
    public AccessMapping configureAccessMapping(
        final String moniker,
        final String displayName,
        final String accessPoint,
        final boolean makeDefault) {
        throw new FrameworkMethodNotImplementedException("configureAccessMapping"); //$NON-NLS-1$
    }

    @Override
    public ServiceDefinition findServiceDefinition(final String serviceType, final GUID serviceIdentifier) {
        throw new FrameworkMethodNotImplementedException("findServiceDefinition"); //$NON-NLS-1$
    }

    @Override
    public ServiceDefinition[] findServiceDefinitions(final String serviceType) {
        throw new FrameworkMethodNotImplementedException("findServiceDefinitions"); //$NON-NLS-1$
    }

    @Override
    public ServiceDefinition[] findServiceDefinitionsByToolType(final String toolType) {
        throw new FrameworkMethodNotImplementedException("findServiceDefinitionsByToolType"); //$NON-NLS-1$
    }

    @Override
    public AccessMapping getAccessMapping(final String moniker) {
        throw new FrameworkMethodNotImplementedException("getAccessMapping"); //$NON-NLS-1$
    }

    @Override
    public AccessMapping getClientAccessMapping() {
        throw new FrameworkMethodNotImplementedException("getClientAccessMapping"); //$NON-NLS-1$
    }

    @Override
    public AccessMapping[] getConfiguredAccessMappings() {
        throw new FrameworkMethodNotImplementedException("getConfiguredAccessMappings"); //$NON-NLS-1$
    }

    @Override
    public AccessMapping getDefaultAccessMapping() {
        throw new FrameworkMethodNotImplementedException("getDefaultAccessMapping"); //$NON-NLS-1$
    }

    @Override
    public String locationForAccessMapping(
        final String serviceType,
        final GUID serviceIdentifier,
        final AccessMapping accessMapping) {
        throw new FrameworkMethodNotImplementedException("locationForAccessMapping"); //$NON-NLS-1$
    }

    @Override
    public String locationForAccessMapping(
        final ServiceDefinition serviceDefinition,
        final AccessMapping accessMapping) {
        throw new FrameworkMethodNotImplementedException("locationForAccessMapping"); //$NON-NLS-1$
    }

    @Override
    public String locationForAccessMapping(
        final ServiceDefinition serviceDefinition,
        final AccessMapping accessMapping,
        final boolean encodeRelativeComponents) {
        throw new FrameworkMethodNotImplementedException("locationForAccessMapping"); //$NON-NLS-1$
    }

    @Override
    public void removeAccessMapping(final String moniker) {
        throw new FrameworkMethodNotImplementedException("removeAccessMapping"); //$NON-NLS-1$
    }

    @Override
    public void removeServiceDefinition(final String serviceType, final GUID serviceIdentifier) {
        throw new FrameworkMethodNotImplementedException("removeServiceDefinition"); //$NON-NLS-1$
    }

    @Override
    public void removeServiceDefinition(final ServiceDefinition serviceDefinition) {
        throw new FrameworkMethodNotImplementedException("removeServiceDefinition"); //$NON-NLS-1$
    }

    @Override
    public void removeServiceDefinitions(final ServiceDefinition[] serviceDefinitions) {
        throw new FrameworkMethodNotImplementedException("removeServiceDefinitions"); //$NON-NLS-1$
    }

    @Override
    public void saveServiceDefinition(final ServiceDefinition serviceDefinition) {
        throw new FrameworkMethodNotImplementedException("saveServiceDefinition"); //$NON-NLS-1$
    }

    @Override
    public void saveServiceDefinitions(final ServiceDefinition[] serviceDefinitions) {
        throw new FrameworkMethodNotImplementedException("saveServiceDefinitions"); //$NON-NLS-1$
    }

    @Override
    public void setDefaultAccessMapping(final AccessMapping accessMapping) {
        throw new FrameworkMethodNotImplementedException("setDefaultAccessMapping"); //$NON-NLS-1$
    }

    @Override
    public ServerCapabilities getServerCapabilities() {
        return ServerCapabilities.NONE;
    }
}
