// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.config.client;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;

import com.microsoft.tfs.core.TFSConfigurationServer;
import com.microsoft.tfs.core.TFSConnection;
import com.microsoft.tfs.core.TFSTeamProjectCollection;
import com.microsoft.tfs.core.clients.build.IBuildServer;
import com.microsoft.tfs.core.clients.build.internal.soapextensions.BuildServer;
import com.microsoft.tfs.core.clients.commonstructure.CommonStructureClient;
import com.microsoft.tfs.core.clients.commonstructure.ProjectInfo;
import com.microsoft.tfs.core.clients.framework.catalog.CatalogService;
import com.microsoft.tfs.core.clients.framework.catalog.ICatalogService;
import com.microsoft.tfs.core.clients.framework.location.ILocationService;
import com.microsoft.tfs.core.clients.framework.location.LocationService;
import com.microsoft.tfs.core.clients.groupsecurity.GroupSecurityClient;
import com.microsoft.tfs.core.clients.linking.LinkingClient;
import com.microsoft.tfs.core.clients.registration.RegistrationClient;
import com.microsoft.tfs.core.clients.reporting.ReportingClient;
import com.microsoft.tfs.core.clients.serverstatus.ServerStatusClient;
import com.microsoft.tfs.core.clients.sharepoint.WSSClient;
import com.microsoft.tfs.core.clients.team.TeamService;
import com.microsoft.tfs.core.clients.teamsettings.TeamSettingsConfigurationService;
import com.microsoft.tfs.core.clients.teamstore.TeamProjectCollectionTeamStore;
import com.microsoft.tfs.core.clients.versioncontrol.VersionControlClient;
import com.microsoft.tfs.core.clients.webservices.IIdentityManagementService;
import com.microsoft.tfs.core.clients.webservices.IIdentityManagementService2;
import com.microsoft.tfs.core.clients.webservices.IdentityManagementService;
import com.microsoft.tfs.core.clients.webservices.IdentityManagementService2;
import com.microsoft.tfs.core.clients.workitem.WorkItemClient;
import com.microsoft.tfs.core.clients.workitemconfiguration.WorkItemConfigurationSettingsClient;
import com.microsoft.tfs.util.Check;

import ms.sql.reporting.reportingservices._ReportingService2005Soap;
import ms.tfs.services.classification._03._Classification4Soap;
import ms.tfs.services.classification._03._ClassificationSoap;
import ms.tfs.services.groupsecurity._03._GroupSecurityServiceSoap;
import ms.tfs.services.serverstatus._03._ServerStatusSoap;
import ms.tfs.services.teamconfiguration._01._TeamConfigurationServiceSoap;
import ms.tfs.versioncontrol.clientservices._03._Repository4Soap;
import ms.tfs.versioncontrol.clientservices._03._Repository5Soap;
import ms.tfs.versioncontrol.clientservices._03._RepositoryExtensionsSoap;
import ms.tfs.versioncontrol.clientservices._03._RepositorySoap;
import ms.tfs.workitemtracking.clientservices._03._ClientService2Soap;
import ms.tfs.workitemtracking.clientservices._03._ClientService3Soap;
import ms.tfs.workitemtracking.clientservices._03._ClientService5Soap;
import ms.tfs.workitemtracking.configurationsettingsservice._03._ConfigurationSettingsServiceSoap;
import ms.wss._ListsSoap;

/**
 * <p>
 * A default implementation of the {@link ClientFactory} interface.
 * </p>
 *
 * @see ClientFactory
 *
 * @since TEE-SDK-10.1
 * @threadsafety thread-safe
 */

@SuppressWarnings("rawtypes")
public class DefaultClientFactory implements ClientFactory {
    private final Object initLock = new Object();
    private boolean initialized = false;
    private final Map<Class, ClientInstantiator> instantiatorMap = new HashMap<Class, ClientInstantiator>();

    /**
     * {@inheritDoc}
     */
    @Override
    public Object newClient(final Class clientType, final TFSConnection connection) {
        Check.notNull(clientType, "clientType"); //$NON-NLS-1$
        Check.notNull(connection, "connection"); //$NON-NLS-1$

        final ClientInstantiator instantiator = getClientInstantiator(clientType);

        if (instantiator == null) {
            throw new UnknownClientException(clientType);
        }

        return instantiator.newClient(connection);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public WSSClient newWSSClient(final TFSTeamProjectCollection connection, final ProjectInfo projectInfo) {
        Check.notNull(projectInfo, "projectInfo"); //$NON-NLS-1$
        Check.notNull(connection, "connection"); //$NON-NLS-1$

        final _ListsSoap webService = connection.getWSSWebService(projectInfo);
        Check.notNull(webService, "webService"); //$NON-NLS-1$

        return new WSSClient(connection, webService, projectInfo.getName());
    }

    private ClientInstantiator getClientInstantiator(final Class clientType) {
        synchronized (initLock) {
            if (!initialized) {
                initialize();
                initialized = true;
            }

            return instantiatorMap.get(clientType);
        }
    }

    private void initialize() {
        /*
         * Common Structure
         */
        instantiatorMap.put(CommonStructureClient.class, new ClientInstantiator() {
            @Override
            public Object newClient(final TFSConnection connection) {
                throwIfNotProjectCollection(connection, CommonStructureClient.class);

                final _ClassificationSoap webService =
                    (_ClassificationSoap) connection.getWebService(_ClassificationSoap.class);

                final _Classification4Soap webService4 =
                    (_Classification4Soap) connection.getWebService(_Classification4Soap.class);

                return new CommonStructureClient((TFSTeamProjectCollection) connection, webService, webService4);
            }
        });

        /*
         * Group Security
         */
        instantiatorMap.put(GroupSecurityClient.class, new ClientInstantiator() {
            @Override
            public Object newClient(final TFSConnection connection) {
                throwIfNotProjectCollection(connection, GroupSecurityClient.class);

                final _GroupSecurityServiceSoap webService =
                    (_GroupSecurityServiceSoap) connection.getWebService(_GroupSecurityServiceSoap.class);
                return new GroupSecurityClient((TFSTeamProjectCollection) connection, webService);
            }
        });

        /*
         * Linking
         */
        instantiatorMap.put(LinkingClient.class, new ClientInstantiator() {
            @Override
            public Object newClient(final TFSConnection connection) {
                throwIfNotProjectCollection(connection, LinkingClient.class);

                return new LinkingClient((TFSTeamProjectCollection) connection);
            }
        });

        /*
         * Registration
         */
        instantiatorMap.put(RegistrationClient.class, new ClientInstantiator() {
            @Override
            public Object newClient(final TFSConnection connection) {
                throwIfNotProjectCollection(connection, RegistrationClient.class);

                return new RegistrationClient((TFSTeamProjectCollection) connection);
            }
        });

        /*
         * Reporting
         */
        instantiatorMap.put(ReportingClient.class, new ClientInstantiator() {
            @Override
            public Object newClient(final TFSConnection connection) {
                throwIfNotProjectCollection(connection, ReportingClient.class);

                final _ReportingService2005Soap webService =
                    (_ReportingService2005Soap) connection.getWebService(_ReportingService2005Soap.class);
                return new ReportingClient((TFSTeamProjectCollection) connection, webService);
            }
        });

        /*
         * Server Status
         */
        instantiatorMap.put(ServerStatusClient.class, new ClientInstantiator() {
            @Override
            public Object newClient(final TFSConnection connection) {
                throwIfNotProjectCollection(connection, ServerStatusClient.class);

                final _ServerStatusSoap webService =
                    (_ServerStatusSoap) connection.getWebService(_ServerStatusSoap.class);
                return new ServerStatusClient(webService);
            }
        });

        /*
         * Version Control
         */
        instantiatorMap.put(VersionControlClient.class, new ClientInstantiator() {
            @Override
            public Object newClient(final TFSConnection connection) {
                throwIfNotProjectCollection(connection, VersionControlClient.class);

                // Pre-2010 and later
                final _RepositorySoap repository = (_RepositorySoap) connection.getWebService(_RepositorySoap.class);

                // 2010 and later
                final _RepositoryExtensionsSoap repositoryExtensions =
                    (_RepositoryExtensionsSoap) connection.getWebService(_RepositoryExtensionsSoap.class);

                // 2012 and later
                final _Repository4Soap repository4 =
                    (_Repository4Soap) connection.getWebService(_Repository4Soap.class);

                // 2012 QU1 and later
                final _Repository5Soap repository5 =
                    (_Repository5Soap) connection.getWebService(_Repository5Soap.class);

                return new VersionControlClient(
                    (TFSTeamProjectCollection) connection,
                    repository,
                    repositoryExtensions,
                    repository4,
                    repository5);
            }
        });

        /*
         * Work Item Configuration
         */
        instantiatorMap.put(WorkItemConfigurationSettingsClient.class, new ClientInstantiator() {
            @Override
            public Object newClient(final TFSConnection connection) {
                throwIfNotProjectCollection(connection, WorkItemConfigurationSettingsClient.class);

                final _ConfigurationSettingsServiceSoap webService =
                    (_ConfigurationSettingsServiceSoap) connection.getWebService(
                        _ConfigurationSettingsServiceSoap.class);
                return new WorkItemConfigurationSettingsClient(webService);
            }
        });

        /*
         * Work Item
         */
        instantiatorMap.put(WorkItemClient.class, new ClientInstantiator() {
            @Override
            public Object newClient(final TFSConnection connection) {
                throwIfNotProjectCollection(connection, WorkItemClient.class);

                final _ClientService2Soap webService2 =
                    (_ClientService2Soap) connection.getWebService(_ClientService2Soap.class);

                // Only in TFS 2010 so it may be null.
                final _ClientService3Soap webService3 =
                    (_ClientService3Soap) connection.getWebService(_ClientService3Soap.class);

                // Only in Dev11 so it may be null.
                final _ClientService5Soap webService5 =
                    (_ClientService5Soap) connection.getWebService(_ClientService5Soap.class);

                return new WorkItemClient((TFSTeamProjectCollection) connection, webService2, webService3, webService5);
            }
        });

        /*
         * Build
         */
        instantiatorMap.put(IBuildServer.class, new ClientInstantiator() {
            @Override
            public Object newClient(final TFSConnection connection) {
                throwIfNotProjectCollection(connection, IBuildServer.class);

                return new BuildServer((TFSTeamProjectCollection) connection);
            }
        });

        /*
         * Location
         */
        instantiatorMap.put(ILocationService.class, new ClientInstantiator() {
            @Override
            public Object newClient(final TFSConnection connection) {
                return new LocationService(connection);
            }
        });

        /*
         * Catalog
         */
        instantiatorMap.put(ICatalogService.class, new ClientInstantiator() {
            @Override
            public Object newClient(final TFSConnection connection) {
                throwIfNotConfigurationServer(connection, ICatalogService.class);

                return new CatalogService((TFSConfigurationServer) connection);
            }
        });

        /*
         * Collection Identity Management v1
         */
        instantiatorMap.put(IIdentityManagementService.class, new ClientInstantiator() {
            @Override
            public Object newClient(final TFSConnection connection) {
                // Can connect to either collection or configuration server

                return new IdentityManagementService(connection);
            }
        });

        /*
         * Collection Identity Management v2
         */
        instantiatorMap.put(IIdentityManagementService2.class, new ClientInstantiator() {
            @Override
            public Object newClient(final TFSConnection connection) {
                // Can connect to either collection or configuration server

                return new IdentityManagementService2(connection);
            }
        });

        /*
         * Team management
         */
        instantiatorMap.put(TeamService.class, new ClientInstantiator() {
            @Override
            public Object newClient(final TFSConnection connection) {
                // Can connect to either collection or configuration server

                return new TeamService(connection);
            }
        });

        /*
         * Team Configuration
         */
        instantiatorMap.put(TeamSettingsConfigurationService.class, new ClientInstantiator() {
            @Override
            public Object newClient(final TFSConnection connection) {
                throwIfNotProjectCollection(connection, TeamSettingsConfigurationService.class);

                // Only in Dev11 so it may be null.
                final _TeamConfigurationServiceSoap webService =
                    (_TeamConfigurationServiceSoap) connection.getWebService(_TeamConfigurationServiceSoap.class);

                return new TeamSettingsConfigurationService(webService);
            }
        });

        /*
         * Team Project Collection-specific Team Configuration Cache
         */
        instantiatorMap.put(TeamProjectCollectionTeamStore.class, new ClientInstantiator() {
            @Override
            public Object newClient(final TFSConnection connection) {
                throwIfNotProjectCollection(connection, TeamProjectCollectionTeamStore.class);

                return new TeamProjectCollectionTeamStore((TFSTeamProjectCollection) connection);
            }
        });
    }

    /**
     * Throws an {@link IllegalArgumentException} if the given connection type
     * is not a {@link TFSTeamProjectCollection}. The given client class's name
     * is mixed into the error message.
     */
    private static void throwIfNotProjectCollection(final TFSConnection connection, final Class clientClass) {
        if (connection instanceof TFSTeamProjectCollection == false) {
            throw new IllegalArgumentException(
                MessageFormat.format(
                    "Client class {0} can only be created with a {1}, {2} is not supported", //$NON-NLS-1$
                    clientClass.getName(),
                    TFSTeamProjectCollection.class.getName(),
                    connection.getClass().getName()));
        }
    }

    /**
     * Throws an {@link IllegalArgumentException} if the given connection type
     * is not a {@link TFSConfigurationServer}. The given client class's name is
     * mixed into the error message.
     */
    private static void throwIfNotConfigurationServer(final TFSConnection connection, final Class clientClass) {
        if (connection instanceof TFSConfigurationServer == false) {
            throw new IllegalArgumentException(
                MessageFormat.format(
                    "Client class {0} can only be created with a {1}, {2} is not supported", //$NON-NLS-1$
                    clientClass.getName(),
                    TFSConfigurationServer.class.getName(),
                    connection.getClass().getName()));
        }
    }

    private static interface ClientInstantiator {
        public Object newClient(TFSConnection connection);
    }
}
