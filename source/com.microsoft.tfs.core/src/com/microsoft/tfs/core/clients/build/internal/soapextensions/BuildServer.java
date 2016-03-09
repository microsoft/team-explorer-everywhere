// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.build.internal.soapextensions;

import java.net.URI;
import java.net.URISyntaxException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.microsoft.tfs.core.Messages;
import com.microsoft.tfs.core.TFSTeamProjectCollection;
import com.microsoft.tfs.core.clients.build.AdministrationWebService4;
import com.microsoft.tfs.core.clients.build.BuildConstants;
import com.microsoft.tfs.core.clients.build.BuildQueueWebService4;
import com.microsoft.tfs.core.clients.build.BuildWebService4;
import com.microsoft.tfs.core.clients.build.IBuildAgent;
import com.microsoft.tfs.core.clients.build.IBuildAgentQueryResult;
import com.microsoft.tfs.core.clients.build.IBuildAgentSpec;
import com.microsoft.tfs.core.clients.build.IBuildController;
import com.microsoft.tfs.core.clients.build.IBuildControllerQueryResult;
import com.microsoft.tfs.core.clients.build.IBuildControllerSpec;
import com.microsoft.tfs.core.clients.build.IBuildDefinition;
import com.microsoft.tfs.core.clients.build.IBuildDefinitionQueryResult;
import com.microsoft.tfs.core.clients.build.IBuildDefinitionSpec;
import com.microsoft.tfs.core.clients.build.IBuildDeletionResult;
import com.microsoft.tfs.core.clients.build.IBuildDetail;
import com.microsoft.tfs.core.clients.build.IBuildDetailSpec;
import com.microsoft.tfs.core.clients.build.IBuildQueryResult;
import com.microsoft.tfs.core.clients.build.IBuildRequest;
import com.microsoft.tfs.core.clients.build.IBuildServer;
import com.microsoft.tfs.core.clients.build.IBuildServiceHost;
import com.microsoft.tfs.core.clients.build.IFailure;
import com.microsoft.tfs.core.clients.build.IProcessTemplate;
import com.microsoft.tfs.core.clients.build.IQueuedBuild;
import com.microsoft.tfs.core.clients.build.IQueuedBuildQueryResult;
import com.microsoft.tfs.core.clients.build.IQueuedBuildSpec;
import com.microsoft.tfs.core.clients.build.exceptions.BuildAgentNotFoundForURIException;
import com.microsoft.tfs.core.clients.build.exceptions.BuildAgentNotReadyToSaveException;
import com.microsoft.tfs.core.clients.build.exceptions.BuildControllerNotFoundException;
import com.microsoft.tfs.core.clients.build.exceptions.BuildControllerNotFoundForURIException;
import com.microsoft.tfs.core.clients.build.exceptions.BuildControllerNotReadyToSaveException;
import com.microsoft.tfs.core.clients.build.exceptions.BuildControllerSpecNotUniqueException;
import com.microsoft.tfs.core.clients.build.exceptions.BuildDefinitionFailureException;
import com.microsoft.tfs.core.clients.build.exceptions.BuildDefinitionNotFoundException;
import com.microsoft.tfs.core.clients.build.exceptions.BuildDefinitionNotFoundForURIException;
import com.microsoft.tfs.core.clients.build.exceptions.BuildDefinitionSpecNotUniqueException;
import com.microsoft.tfs.core.clients.build.exceptions.BuildFailureException;
import com.microsoft.tfs.core.clients.build.exceptions.BuildNotFoundException;
import com.microsoft.tfs.core.clients.build.exceptions.BuildNotFoundForURIException;
import com.microsoft.tfs.core.clients.build.exceptions.BuildServiceHostNotFoundException;
import com.microsoft.tfs.core.clients.build.exceptions.BuildServiceHostNotFoundForURIException;
import com.microsoft.tfs.core.clients.build.exceptions.BuildServiceHostSpecNotUniqueException;
import com.microsoft.tfs.core.clients.build.exceptions.BuildSpecNotUniqueException;
import com.microsoft.tfs.core.clients.build.flags.BuildAgentUpdate;
import com.microsoft.tfs.core.clients.build.flags.BuildControllerUpdate;
import com.microsoft.tfs.core.clients.build.flags.BuildQueryOrder;
import com.microsoft.tfs.core.clients.build.flags.BuildServerVersion;
import com.microsoft.tfs.core.clients.build.flags.BuildServiceHostUpdate;
import com.microsoft.tfs.core.clients.build.flags.DeleteOptions;
import com.microsoft.tfs.core.clients.build.flags.QueryDeletedOption;
import com.microsoft.tfs.core.clients.build.flags.QueryOptions;
import com.microsoft.tfs.core.clients.build.flags.QueueOptions;
import com.microsoft.tfs.core.clients.build.flags.QueuedBuildRetryOption;
import com.microsoft.tfs.core.clients.build.soapextensions.Agent2008Status;
import com.microsoft.tfs.core.clients.build.soapextensions.ContinuousIntegrationType;
import com.microsoft.tfs.core.clients.build.soapextensions.DefinitionTriggerType;
import com.microsoft.tfs.core.clients.build.soapextensions.ProcessTemplateType;
import com.microsoft.tfs.core.clients.framework.internal.ServiceInterfaceNames;
import com.microsoft.tfs.core.clients.registration.RegistrationClient;
import com.microsoft.tfs.core.clients.registration.ServiceInterface;
import com.microsoft.tfs.core.clients.registration.ToolNames;
import com.microsoft.tfs.core.clients.versioncontrol.VersionControlConstants;
import com.microsoft.tfs.core.clients.webservices.IdentityHelper;
import com.microsoft.tfs.core.exceptions.NotSupportedException;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.GUID;

public class BuildServer implements IBuildServer {

    private final TFSTeamProjectCollection tfs;
    private BuildServerVersion serverVersion;

    private Build2008Helper build2008Helper;
    private Build2010Helper build2010Helper;
    private BuildWebService4 buildService4;
    private BuildQueueWebService4 buildQueueService4;
    private AdministrationWebService4 buildAdminService4;

    private static boolean COMPATIBILITY_ENABLED = true;
    private static ProcessTemplateType[] ALL_PROCESS_TEMPLATE_TYPES = new ProcessTemplateType[] {
        ProcessTemplateType.CUSTOM,
        ProcessTemplateType.DEFAULT,
        ProcessTemplateType.UPGRADE,
    };

    /**
     * Creates a {@link BuildClient} with the given
     * {@link TFSTeamProjectCollection} .
     *
     * @param connection
     *        a valid {@link TFSTeamProjectCollection} (must not be
     *        <code>null</code>)
     */
    public BuildServer(final TFSTeamProjectCollection tfs) {
        Check.notNull(tfs, "tfs"); //$NON-NLS-1$

        this.tfs = tfs;
        initialize(tfs);
    }

    void initialize(final TFSTeamProjectCollection tfs) {
        // Determine the version of the team foundation server that we are
        // communicating with.
        final RegistrationClient registrationClient = tfs.getRegistrationClient();
        ServiceInterface buildService =
            registrationClient.getServiceInterface(ToolNames.TEAM_BUILD, ServiceInterfaceNames.BUILD_4);

        if (buildService != null) {
            serverVersion = BuildServerVersion.V4;
        } else {
            buildService = registrationClient.getServiceInterface(ToolNames.TEAM_BUILD, ServiceInterfaceNames.BUILD_3);
            if (buildService != null) {
                serverVersion = BuildServerVersion.V3;
            } else {
                serverVersion = BuildServerVersion.V2;
            }
        }

        if (serverVersion.isLessThanV3() && (!isCompatibilityEnabled() || serverVersion.isLessThanV2())) {
            throw new NotSupportedException(Messages.getString("BuildServer2012.ServerNotSupported")); //$NON-NLS-1$
        }
    }

    // / <summary>
    // / Internal flag that controls whether or not compatibility with V1 should
    // be enabled.
    // / </summary>
    public static boolean isCompatibilityEnabled() {
        return COMPATIBILITY_ENABLED;
    }

    public static void setCompatibilityEnabled(final boolean value) {
        COMPATIBILITY_ENABLED = value;
    }

    @Override
    public BuildServerVersion getBuildServerVersion() {
        return serverVersion;
    }

    @Override
    public String getNoCICheckInComment() {
        return VersionControlConstants.NO_CI_CHECKIN_COMMENT;
    }

    @Override
    public TFSTeamProjectCollection getConnection() {
        return tfs;
    }

    // / <summary>
    // / Create a new build definition owned by this build server.
    // / </summary>
    // / <param name="teamProject">The team project under which the build
    // definition will be created.</param>
    // / <returns>The new build definition.</returns>
    @Override
    public IBuildDefinition createBuildDefinition(final String teamProject) {
        return new BuildDefinition(this, teamProject);
    }

    // / <summary>
    // / Creates a new build detail specification that can be used to query
    // builds for a particular team project.
    // / </summary>
    // / <param name="teamProject">The team project for which builds can be
    // queried.</param>
    // / <returns>The new build detail specification.</returns>
    @Override
    public IBuildDetailSpec createBuildDetailSpec(final String teamProject) {
        return new BuildDetailSpec(teamProject);
    }

    // / <summary>
    // / Creates a new build detail specification that can be used to query
    // builds for a particular team project and definition.
    // / </summary>
    // / <param name="teamProject">The team project for which builds can be
    // queried.</param>
    // / <param name="definitionName">The build definition for which buidls can
    // be queried.</param>
    // / <returns>The new build detail specification.</returns>
    @Override
    public IBuildDetailSpec createBuildDetailSpec(final String teamProject, final String definitionName) {
        return new BuildDetailSpec(teamProject, definitionName);
    }

    // / <summary>
    // / Creates a new build detail specification that can be used to query
    // builds for a particular team project and definition.
    // / </summary>
    // / <param name="definitionSpec">A build definition specification that
    // includes the team project and definition for which builds can be
    // queried.</param>
    // / <returns>The new build detail specification.</returns>
    @Override
    public IBuildDetailSpec createBuildDetailSpec(final IBuildDefinitionSpec definitionSpec) {
        return new BuildDetailSpec(definitionSpec);
    }

    // / <summary>
    // / Creates a new build detail specification that can be used to query
    // builds for a particular definition.
    // / </summary>
    // / <param name="definition">The build definition for which builds can be
    // queried.</param>
    // / <returns>The new build detail specification.</returns>
    @Override
    public IBuildDetailSpec createBuildDetailSpec(final IBuildDefinition definition) {
        return new BuildDetailSpec(definition);
    }

    // / <summary>
    // / Creates a new build agent specification that can be used to query build
    // agents.
    // / </summary>
    // / <returns>The new build agent specification.</returns>
    @Override
    public IBuildAgentSpec createBuildAgentSpec() {
        // Make sure the default will return everything, rather than nothing.
        return new BuildAgentSpec(BuildConstants.STAR, BuildConstants.STAR);
    }

    // / <summary>
    // / Creates a new build agent specification that can be used to query build
    // agents.
    // / </summary>
    // / <param name="agent">The agent for which a specification is
    // created.</param>
    // / <returns>The new build agent specification.</returns>
    @Override
    public IBuildAgentSpec createBuildAgentSpec(final IBuildAgent agent) {
        return new BuildAgentSpec(agent.getName(), agent.getServiceHost().getName(), agent.getTags());
    }

    // / <summary>
    // / Creates a new build agent specification that can be used to query build
    // agents.
    // / </summary>
    // / <param name="name">The agent name to query - supports
    // wildcards.</param>
    // / <param name="computer">The computer to query - null/empty means
    // "don't care".</param>
    // / <param name="tags">The tags to query - null/empty means
    // "don't care".</param>
    // / <returns>The new build agent specification.</returns>
    @Override
    public IBuildAgentSpec createBuildAgentSpec(final String name, final String computer, final String[] tags) {
        return new BuildAgentSpec(name, computer, tags);
    }

    // / <summary>
    // / Creates a new build agent specification that can be used to query build
    // agents.
    // / </summary>
    // / <param name="name">The agent name to query - supports
    // wildcards.</param>
    // / <param name="computer">The computer to query - null/empty means
    // "don't care".</param>
    // / <param name="propertyNameFilters">Property name filters</param>
    // / <param name="tags">The tags to query - null/empty means
    // "don't care".</param>
    // / <returns>The new build agent specification.</returns>
    @Override
    public IBuildAgentSpec createBuildAgentSpec(
        final String name,
        final String computer,
        final String[] propertyNameFilters,
        final String[] tags) {
        return new BuildAgentSpec(name, computer, propertyNameFilters, tags);
    }

    // / <summary>
    // / Creates a new build controller specification that can be used to query
    // build controllers.
    // / </summary>
    // / <returns>The new build controller specification.</returns>
    @Override
    public IBuildControllerSpec createBuildControllerSpec() {
        // Make sure the default will return everything, rather than nothing.
        return new BuildControllerSpec(BuildConstants.STAR, BuildConstants.STAR);
    }

    // / <summary>
    // / Creates a new build controller specification that can be used to query
    // build controllers.
    // / </summary>
    // / <param name="name">The controller name to query - supports
    // wildcards.</param>
    // / <param name="computer">The computer to query - null/empty means
    // "don't care".</param>
    // / <returns>The new build controller specification.</returns>
    @Override
    public IBuildControllerSpec createBuildControllerSpec(final String name, final String computer) {
        return new BuildControllerSpec(name, computer);
    }

    // / <summary>
    // / Creates a new build controller specification that can be used to query
    // build controllers.
    // / </summary>
    // / <param name="name">The controller name to query - supports
    // wildcards.</param>
    // / <param name="computer">The computer to query - null/empty means
    // "don't care".</param>
    // / <param name="propertyNameFilters">Property name filters</param>
    // / <param name="includeAgents">Include agents into the query or
    // not</param>
    // / <returns>The new build controller specification.</returns>
    @Override
    public IBuildControllerSpec createBuildControllerSpec(
        final String name,
        final String computer,
        final String[] propertyNameFilters,
        final boolean includeAgents) {
        return new BuildControllerSpec(name, computer, propertyNameFilters, includeAgents);
    }

    // / <summary>
    // / Creates a new build definition specification that can be used to query
    // build definitions.
    // / </summary>
    // / <param name="definition">The definition for which a specification is
    // created.</param>
    // / <returns>The new build definition specification.</returns>
    @Override
    public IBuildDefinitionSpec createBuildDefinitionSpec(final IBuildDefinition definition) {
        return new BuildDefinitionSpec(definition);
    }

    // / <summary>
    // / Creates a new build definition specification that can be used to query
    // build definitions.
    // / </summary>
    // / <param name="teamProject">The team project for which agents can be
    // queried.</param>
    // / <returns>The new build definition specification.</returns>
    @Override
    public IBuildDefinitionSpec createBuildDefinitionSpec(final String teamProject) {
        return new BuildDefinitionSpec(teamProject);
    }

    // / <summary>
    // / Creates a new build definition specification that can be used to query
    // build definitions.
    // / </summary>
    // / <param name="teamProject">The team project for which agents can be
    // queried.</param>
    // / <param name="definitionName">The definition name to query - support
    // wildcards.</param>
    // / <returns>The new build definition specification.</returns>
    @Override
    public IBuildDefinitionSpec createBuildDefinitionSpec(final String teamProject, final String definitionName) {
        return new BuildDefinitionSpec(teamProject, definitionName);
    }

    // / <summary>
    // / Creates a new build definition specification that can be used to query
    // build definitions.
    // / </summary>
    // / <param name="teamProject">The team project for which agents can be
    // queried.</param>
    // / <param name="definitionName">The definition name to query - support
    // wildcards.</param>
    // / <param name="propertyNameFilters">Property name filters</param>
    // / <returns>The new build definition specification.</returns>
    @Override
    public IBuildDefinitionSpec createBuildDefinitionSpec(
        final String teamProject,
        final String definitionName,
        final String[] propertyNameFilters) {
        return new BuildDefinitionSpec(teamProject, definitionName, propertyNameFilters);
    }

    // / <summary>
    // / Creates a new build queue specification that can be used to query
    // queued builds.
    // / </summary>
    // / <param name="teamProject">The team project for which queued builds can
    // be queried.</param>
    // / <returns>The new build queue specification.</returns>
    @Override
    public IQueuedBuildSpec createBuildQueueSpec(final String teamProject) {
        return new BuildQueueSpec(teamProject);
    }

    // / <summary>
    // / Creates a new build queue specification that can be used to query
    // queued builds.
    // / </summary>
    // / <param name="teamProject">The team project for which queued builds can
    // be queried.</param>
    // / <param name="definitionName">The definition name to query - support
    // wildcards.</param>
    // / <returns>The new build queue specification.</returns>
    @Override
    public IQueuedBuildSpec createBuildQueueSpec(final String teamProject, final String definitionName) {
        return new BuildQueueSpec(teamProject, definitionName);
    }

    // / <summary>
    // / Creates a new build queue specification that can be used to query
    // queued builds.
    // / </summary>
    // / <param name="definitionUris">The definitions to query.</param>
    // / <returns>A new build queue specification.</returns>
    @Override
    public IQueuedBuildSpec createBuildQueueSpec(final String[] definitionUris) {
        if (getBuildServerVersion().isV3OrGreater()) {
            return new BuildQueueSpec(definitionUris);
        }

        throwOperationNotSupported("IQueuedBuildSpec CreateBuildQueueSpec(IEnumerable<Uri>)"); //$NON-NLS-1$
        return null; // Not reached
    }

    // / <summary>
    // / Creates a new build request for the specified build definition.
    // / </summary>
    // / <param name="buildDefinitionUri">The build definition</param>
    // / <returns>A new build request</returns>
    @Override
    public IBuildRequest createBuildRequest(final String buildDefinitionUri) {
        return new BuildRequest(this, buildDefinitionUri, null);
    }

    // / <summary>
    // / Creates a new build request for the specified build definition and
    // controller.
    // / </summary>
    // / <param name="buildDefinitionUri">The build definiton</param>
    // / <param name="buildControllerUri">The build controller</param>
    // / <returns>A new build request</returns>
    @Override
    public IBuildRequest createBuildRequest(final String buildDefinitionUri, final String buildControllerUri) {
        return new BuildRequest(this, buildDefinitionUri, buildControllerUri);
    }

    // / <summary>
    // / Creates a new service host with the specified name and base URL.
    // / </summary>
    // / <param name="name">The name of the service host</param>
    // / <param name="baseUrl">The base URL of the service host.</param>
    // / <returns>The new service host.</returns>
    @Override
    public IBuildServiceHost createBuildServiceHost(final String name, final String baseUrl) {
        return new BuildServiceHost(this, name, baseUrl);
    }

    // / <summary>
    // / Creates a new service host with the specified name, and with a base URL
    // consisting of the specified scheme,
    // / host, and port and the default path (Build/v3.0/Services).
    // / </summary>
    // / <param name="name">The name of the service host.</param>
    // / <param name="scheme">The scheme of the base URL of the service
    // host.</param>
    // / <param name="host">The host of the base URL of the service
    // host.</param>
    // / <param name="port">The port of the base URL of the service
    // host.</param>
    // / <returns>The new service host.</returns>
    @Override
    public IBuildServiceHost createBuildServiceHost(
        final String name,
        final String scheme,
        final String host,
        final int port) {
        String uriString;
        try {
            uriString =
                new URI(scheme, null, host, port, BuildConstants.DEFAULT_SERVICE_HOST_URL_PATH, null, null).toString();
        } catch (final URISyntaxException e) {
            uriString = scheme + ":\\" + host + ":" + port; //$NON-NLS-1$ //$NON-NLS-2$
        }

        return new BuildServiceHost(this, name, uriString);
    }

    // / <summary>
    // / Gets a list of all build definitions which are affected by the provided
    // TFS Version Control
    // / server paths. Definitions which may be affected include the Batch and
    // Schedule
    // / ContinuousIntegrationType.
    // / </summary>
    // / <param name="serverItems">An array of TFS Version Control server
    // paths</param>
    // / <returns>An array of build definitions </returns>
    @Override
    public IBuildDefinition[] getAffectedBuildDefinitions(final String[] serverItems) {
        return getAffectedBuildDefinitions(serverItems, DefinitionTriggerType.ALL);
    }

    @Override
    public IBuildDefinition[] getAffectedBuildDefinitions(
        final String[] serverItems,
        final ContinuousIntegrationType continuousIntegrationType) {
        return getAffectedBuildDefinitions(serverItems, TFS2010Helper.convert(continuousIntegrationType));
    }

    // / <summary>
    // / Gets a list of all build definitions which are affected by the provided
    // TFS Version Control
    // / server paths and are configured with a trigger supplied in the filter.
    // / </summary>
    // / <param name="serverItems">An array of TFS Version Control server
    // paths</param>
    // / <param name="triggerType">The type of trigger(s) which should be
    // queried</param>
    // / <returns>An array of build definitions</returns>
    @Override
    public IBuildDefinition[] getAffectedBuildDefinitions(
        final String[] serverItems,
        final DefinitionTriggerType triggerType) {
        if (getBuildServerVersion().isV2()) {
            return getBuild2008Helper().getAffectedBuildDefinitions(serverItems, triggerType);
        } else if (getBuildServerVersion().isV3()) {
            return getBuild2010Helper().getAffectedBuildDefinitions(serverItems, triggerType);
        } else {
            return getBuildService().getAffectedBuildDefinitions(serverItems, triggerType);
        }
    }

    // / <summary>
    // / Gets a single build with no agent, definition, or information nodes.
    // / </summary>
    // / <param name="buildUri">The Uri of the build.</param>
    // / <returns>The build with the given Uri, without any details.</returns>
    @Override
    public IBuildDetail getMinimalBuildDetails(final String buildUri) {
        return getBuild(buildUri, null, QueryOptions.CONTROLLERS);
    }

    // / <summary>
    // / Gets a single build with agent, definition, and all information nodes.
    // / </summary>
    // / <param name="buildUri">The Uri of the build.</param>
    // / <returns>The build with the given Uri, with all details.</returns>
    @Override
    public IBuildDetail getAllBuildDetails(final String buildUri) {
        return getBuild(buildUri, BuildConstants.ALL_INFORMATION_TYPES, QueryOptions.ALL);
    }

    // / <summary>
    // / Gets a single build with all information types and query options.
    // / </summary>
    // / <param name="buildUri">The URI of the build.</param>
    // / <returns>The build with the given URI.</returns>
    @Override
    public IBuildDetail getBuild(final String buildUri) {
        return getBuild(buildUri, BuildConstants.ALL_INFORMATION_TYPES, QueryOptions.ALL);
    }

    // / <summary>
    // / Gets a single build by Uri.
    // / </summary>
    // / <param name="buildUri">The Uri of the build to get.</param>
    // / <param name="informationTypes">The information types which should be
    // retrieved.</param>
    // / <param name="queryOptions">The query options.</param>
    // / <returns>The build with the given Uri.</returns>
    @Override
    public IBuildDetail getBuild(
        final String buildUri,
        final String[] informationTypes,
        final QueryOptions queryOptions) {
        return getBuild(buildUri, informationTypes, queryOptions, QueryDeletedOption.EXCLUDE_DELETED);
    }

    // / <summary>
    // / Gets a single build by Uri.
    // / </summary>
    // / <param name="buildUri">The Uri of the build to get.</param>
    // / <param name="informationTypes">The information types which should be
    // retrieved.</param>
    // / <param name="queryOptions">The query options.</param>
    // / <param name="queryDeletedOption">The deleted options.</param>
    // / <returns>The build with the given Uri.</returns>
    @Override
    public IBuildDetail getBuild(
        final String buildUri,
        final String[] informationTypes,
        final QueryOptions queryOptions,
        final QueryDeletedOption queryDeletedOption) {
        final IBuildDetail[] results = queryBuildsByURI(new String[] {
            buildUri
        }, informationTypes, queryOptions, queryDeletedOption);

        if (results[0] == null) {
            throw new BuildNotFoundForURIException(buildUri, getDomainUserName());
        }

        return results[0];
    }

    // / <summary>
    // / Gets a single build by build definition path and build number.
    // / </summary>
    // / <param name="buildDefinitionPath">Specification of the build definition
    // from which the build was created.</param>
    // / <param name="buildNumber">Build number of the build.</param>
    // / <returns>The build number with the given build number created from the
    // given build definition.</returns>
    @Override
    public IBuildDetail getBuild(
        final IBuildDefinitionSpec buildDefinitionSpec,
        final String buildNumber,
        final String[] informationTypes,
        final QueryOptions queryOptions) {
        final BuildDetailSpec buildSpec = new BuildDetailSpec(buildDefinitionSpec);
        buildSpec.setBuildNumber(buildNumber);
        buildSpec.setInformationTypes(informationTypes);
        buildSpec.setQueryOptions(queryOptions);

        final IBuildQueryResult queryResult = queryBuilds(buildSpec);

        if (queryResult.getBuilds().length == 0) {
            // No builds were found matching the buildDefinitionPath and
            // buildNumber.
            throw new BuildNotFoundException(buildNumber, buildDefinitionSpec.getFullPath());
        } else if (queryResult.getBuilds().length > 1) {
            // More than one build matched the buildDefinitionPath and
            // buildNumber.
            throw new BuildSpecNotUniqueException(buildNumber, buildDefinitionSpec.getFullPath());
        }

        return queryResult.getBuilds()[0];
    }

    // / <summary>
    // / Gets an array of builds by Uri. The informationTypes array can be of
    // the following
    // / format: empty or null gets none, 1 element with a '*' gets all,
    // otherwise the types
    // / are matched as-is.
    // / </summary>
    // / <param name="buildUris">The Uris of the builds to get.</param>
    // / <param name="informationTypes">The information type names which should
    // be retrieved</param>
    // / <param name="queryOptions">Specify the related information to query
    // for.</param>
    // / <returns>An array of the builds with the given Uris (or null if a Uri
    // was invalid).</returns>
    @Override
    public IBuildDetail[] queryBuildsByURI(
        final String[] buildUris,
        final String[] informationTypes,
        final QueryOptions queryOptions) {
        return queryBuildsByURI(buildUris, informationTypes, queryOptions, QueryDeletedOption.EXCLUDE_DELETED);
    }

    // / <summary>
    // / Gets an array of builds by Uri. The informationTypes array can be of
    // the following
    // / format: empty or null gets none, 1 element with a '*' gets all,
    // otherwise the types
    // / are matched as-is.
    // / </summary>
    // / <param name="buildUris">The Uris of the builds to get.</param>
    // / <param name="informationTypes">The information type names which should
    // be retrieved</param>
    // / <param name="queryOptions">Specify the related information to query
    // for.</param>
    // / <param name="queryDeletedOption">Specify whether to include deleted
    // builds in the query.</param>
    // / <returns>An array of the builds with the given Uris (or null if a Uri
    // was invalid).</returns>
    @Override
    public IBuildDetail[] queryBuildsByURI(
        final String[] buildUris,
        final String[] informationTypes,
        final QueryOptions queryOptions,
        final QueryDeletedOption queryDeletedOption) {
        if (getBuildServerVersion().isV2()) {
            return getBuild2008Helper().queryBuildsByUri(buildUris, informationTypes, queryOptions, queryDeletedOption);
        } else if (getBuildServerVersion().isV3()) {
            return getBuild2010Helper().queryBuildsByUri(buildUris, informationTypes, queryOptions, queryDeletedOption);
        } else {
            return getBuildService().queryBuildsByUri(
                buildUris,
                informationTypes,
                queryOptions,
                queryDeletedOption).getBuilds();
        }
    }

    // / <summary>
    // / Gets all of the builds for a team project.
    // / </summary>
    // / <param name="teamProject">The team project for which builds are
    // retrieved.</param>
    // / <returns>The builds for the given team project.</returns>
    @Override
    public IBuildDetail[] queryBuilds(final String teamProject) {
        return queryBuilds(createBuildDefinitionSpec(teamProject));
    }

    // / <summary>
    // / Gets all of the builds for a team project and definition.
    // / </summary>
    // / <param name="teamProject">The team project for which builds are
    // retrieved.</param>
    // / <param name="definitionName">The definition for which builds are
    // retrieved.</param>
    // / <returns>The builds for the given team project and
    // definition.</returns>
    @Override
    public IBuildDetail[] queryBuilds(final String teamProject, final String definitionName) {
        return queryBuilds(createBuildDefinitionSpec(teamProject, definitionName));
    }

    // / <summary>
    // / Gets all of the builds for a build definition specification.
    // / </summary>
    // / <param name="definitionSpec">The build definition specification for
    // which builds are retrieved.</param>
    // / <returns>The builds for the given build definition
    // specification.</returns>
    @Override
    public IBuildDetail[] queryBuilds(final IBuildDefinitionSpec definitionSpec) {
        final IBuildQueryResult queryResult = queryBuilds(createBuildDetailSpec(definitionSpec));

        if (queryResult.getFailures().length > 0) {
            final IFailure failure = queryResult.getFailures()[0];
            throw new BuildFailureException(definitionSpec.getTeamProject(), definitionSpec.getName(), failure);
        }

        return queryResult.getBuilds();
    }

    // / <summary>
    // / Gets all of the builds for a build definition.
    // / </summary>
    // / <param name="definition">The build definition for which builds are
    // retrieved.</param>
    // / <returns>The builds for the given build definition.</returns>
    @Override
    public IBuildDetail[] queryBuilds(final IBuildDefinition definition) {
        return queryBuilds(new BuildDefinitionSpec(definition));
    }

    // / <summary>
    // / Gets a single build query result matching a build detail specification.
    // / </summary>
    // / <param name="buildDetailSpec">The build specification for which a build
    // query result is retrieved.</param>
    // / <returns>The matching build query result.</returns>
    @Override
    public IBuildQueryResult queryBuilds(final IBuildDetailSpec buildDetailSpec) {
        return queryBuilds(new IBuildDetailSpec[] {
            buildDetailSpec
        })[0];
    }

    // / <summary>
    // / Gets an array of build query results matching an array of build detail
    // specifications.
    // / </summary>
    // / <param name="buildDetailSpecs">The build detail specifications for
    // which build query results are
    // / retrieved.</param>
    // / <returns>The matching build query results.</returns>
    @Override
    public IBuildQueryResult[] queryBuilds(final IBuildDetailSpec[] buildDetailSpecs) {
        IBuildQueryResult[] results = null;

        if (getBuildServerVersion().isV2()) {
            results = getBuild2008Helper().queryBuilds(buildDetailSpecs);
        } else if (getBuildServerVersion().isV3()) {
            results = getBuild2010Helper().queryBuilds(buildDetailSpecs);
        } else {
            results = getBuildService().queryBuilds(buildDetailSpecs);
        }

        // We need to sort the results in the order of the query since this is
        // what the caller will most likely
        // expect. The server does not perform any sorting in the interest of
        // performance.
        sortBuildsOfResults(buildDetailSpecs, results);

        return results;
    }

    // / <summary>
    // / Sorts the builds within each BuildQueryResult according to the sort
    // preference
    // / on the associated IBuildDetailSpec.
    // / </summary>
    private static void sortBuildsOfResults(
        final IBuildDetailSpec[] buildDetailSpecs,
        final IBuildQueryResult[] results) {
        for (int i = 0; i < buildDetailSpecs.length; i++) {
            final IBuildDetailSpec buildDetailSpec = buildDetailSpecs[i];
            final BuildQueryOrder order = buildDetailSpec.getQueryOrder();

            Comparator<IBuildDetail> comparer;
            if (order.equals(BuildQueryOrder.FINISH_TIME_ASCENDING)) {
                comparer = new Comparator<IBuildDetail>() {
                    @Override
                    public int compare(final IBuildDetail o1, final IBuildDetail o2) {
                        return o1.getFinishTime().compareTo(o2.getFinishTime());
                    }
                };
            } else if (order.equals(BuildQueryOrder.FINISH_TIME_DESCENDING)) {
                comparer = new Comparator<IBuildDetail>() {
                    @Override
                    public int compare(final IBuildDetail o1, final IBuildDetail o2) {
                        return -1 * o1.getFinishTime().compareTo(o2.getFinishTime());
                    }
                };

            } else if (order.equals(BuildQueryOrder.START_TIME_DESCENDING)) {
                comparer = new Comparator<IBuildDetail>() {
                    @Override
                    public int compare(final IBuildDetail o1, final IBuildDetail o2) {
                        return -1 * o1.getStartTime().compareTo(o2.getStartTime());
                    }
                };
            } else {
                // Start time ascending
                comparer = new Comparator<IBuildDetail>() {
                    @Override
                    public int compare(final IBuildDetail o1, final IBuildDetail o2) {
                        return o1.getStartTime().compareTo(o2.getStartTime());
                    }
                };
            }

            Arrays.sort(results[i].getBuilds(), comparer);
        }
    }

    // / <summary>
    // / Deletes an array of builds from the server. All parts of the build are
    // deleted.
    // / </summary>
    // / <param name="builds">The builds to be deleted.</param>
    @Override
    public IBuildDeletionResult[] deleteBuilds(final IBuildDetail[] builds) {
        return deleteBuilds(builds, DeleteOptions.ALL);
    }

    // / <summary>
    // / Deletes an array of builds from the server.
    // / </summary>
    // / <param name="builds">The builds to be deleted.</param>
    // / <param name="options">The parts of the build to delete.</param>
    @Override
    public IBuildDeletionResult[] deleteBuilds(final IBuildDetail[] builds, final DeleteOptions options) {
        return deleteBuilds(getUrisForBuilds(builds), options);
    }

    // / <summary>
    // / Deletes an array of builds from the server. All parts of the build are
    // deleted.
    // / </summary>
    // / <param name="buildUris">The URIs of the builds to be deleted.</param>
    @Override
    public IBuildDeletionResult[] deleteBuilds(final String[] uris) {
        return deleteBuilds(uris, DeleteOptions.ALL);
    }

    // / <summary>
    // / Deletes an array of builds from the server.
    // / </summary>
    // / <param name="buildUris">The URIs of the builds to be deleted.</param>
    // / <param name="options">The parts of the build to delete.</param>
    @Override
    public IBuildDeletionResult[] deleteBuilds(final String[] uris, final DeleteOptions options) {
        // Ignore null/empty inputs
        if (uris == null || uris.length == 0) {
            return new IBuildDeletionResult[0];
        }

        if (getBuildServerVersion().isLessThanV3() && !options.equals(DeleteOptions.ALL)) {
            // Older versions only support deleting everything, so error out.
            final String format = Messages.getString("BuildServer2012.DeleteOptionsNotSupportedFormat"); //$NON-NLS-1$
            throw new NotSupportedException(MessageFormat.format(format, options));
        }

        if (getBuildServerVersion().isV2()) {
            return getBuild2008Helper().deleteBuilds(uris, options);
        } else if (getBuildServerVersion().isV3()) {
            return getBuild2010Helper().deleteBuilds(uris, options);
        } else {
            return getBuildService().deleteBuilds(uris, options);
        }
    }

    // / <summary>
    // / Destroys builds from the server.
    // / </summary>
    // / <param name="builds">The builds to be deleted.</param>
    // / <remarks>
    // / Unlike DeleteBuilds, this method will remove the build records from
    // / the database completely. This method will not delete build artifacts
    // / such as the drop location and test results.
    // / </remarks>
    @Override
    public void destroyBuilds(final IBuildDetail[] builds) {
        destroyBuilds(getUrisForBuilds(builds));
    }

    // / <summary>
    // / Destroys builds from the server.
    // / </summary>
    // / <param name="buildUris">The URIs of the builds to be deleted.</param>
    // / <remarks>
    // / Unlike DeleteBuilds, this method will remove the build records from
    // / the database completely. This method will not delete build artifacts
    // / such as the drop location and test results.
    // / </remarks>
    @Override
    public void destroyBuilds(final String[] buildUris) {
        if (getBuildServerVersion().isV3()) {
            getBuild2010Helper().destroyBuilds(buildUris);
        } else if (getBuildServerVersion().isV4()) {
            getBuildService().destroyBuilds(buildUris);
        }
    }

    // / <summary>
    // / Saves any changes made to an array of builds to the server.
    // / </summary>
    // / <param name="builds">The builds to be saved.</param>
    // / <returns>An array of the saved builds.</returns>
    @Override
    public IBuildDetail[] saveBuilds(final IBuildDetail[] builds) {
        // Ignore null/empty input.
        if (builds == null || builds.length == 0) {
            return builds;
        }

        IBuildDetail[] result;
        final BuildUpdateOptions[] updateOptions = new BuildUpdateOptions[builds.length];
        final Map<String, BuildUpdateOptions> actualUpdates = new HashMap<String, BuildUpdateOptions>();

        // Update builds on the server.
        for (int i = 0; i < builds.length; i++) {
            updateOptions[i] = ((BuildDetail) builds[i]).getUpdateOptions();
        }

        if (getBuildServerVersion().isV2()) {
            result = getBuild2008Helper().updateBuilds(updateOptions);
        } else if (getBuildServerVersion().isV3()) {
            result = getBuild2010Helper().updateBuilds(updateOptions);
        } else {
            result = getBuildService().updateBuilds(updateOptions);
        }

        // If successful, set the UpdateOptions for each build. If updateOptions
        // contained multiple
        // updates for the same Build URI, only the last one will have been used
        // in prc_UpdateBuilds.
        // We need to make sure that the input builds reflect this.
        for (final IBuildDetail resultBuild : result) {
            if (resultBuild != null) {
                final BuildDetail build = (BuildDetail) resultBuild;
                actualUpdates.put(resultBuild.getURI(), build.getUpdateOptions());
            }
        }

        for (final IBuildDetail build : builds) {
            if (build != null) {
                ((BuildDetail) build).setUpdateOptions(actualUpdates.get(build.getURI()));
            }
        }

        return result;
    }

    @Override
    public IBuildDefinition getBuildDefinition(final String buildDefinitionUri) {
        return getBuildDefinition(buildDefinitionUri, BuildConstants.NO_PROPERTY_NAMES, QueryOptions.CONTROLLERS);
    }

    // / <summary>
    // / Gets a single build definition by Uri.
    // / </summary>
    // / <param name="buildDefinitionUri">The Uri of the build definition to
    // get.</param>
    // / <param name="options">Query options</param>
    // / <returns>The build definition with the given Uri.</returns>
    @Override
    public IBuildDefinition getBuildDefinition(final String buildDefinitionUri, final QueryOptions options) {
        return getBuildDefinition(buildDefinitionUri, BuildConstants.NO_PROPERTY_NAMES, options);
    }

    // / <summary>
    // / Gets a single build definition by Uri.
    // / </summary>
    // / <param name="buildDefinitionUri">The Uri of the build definition to
    // get.</param>
    // / <param name="propertyNameFilters">Property names to query</param>
    // / <param name="options">Query options</param>
    // / <returns>The build definition with the given Uri.</returns>
    @Override
    public IBuildDefinition getBuildDefinition(
        final String buildDefinitionUri,
        final String[] propertyNameFilters,
        final QueryOptions options) {
        final IBuildDefinition[] results = queryBuildDefinitionsByURI(new String[] {
            buildDefinitionUri
        }, propertyNameFilters, options);

        if (results[0] == null) {
            throw new BuildDefinitionNotFoundForURIException(buildDefinitionUri, getDomainUserName());
        }

        return results[0];
    }

    @Override
    public IBuildDefinition getBuildDefinition(final String teamProject, final String name) {
        return getBuildDefinition(teamProject, name, QueryOptions.CONTROLLERS);
    }

    // / <summary>
    // / Gets a single build definition by team project and name.
    // / </summary>
    // / <param name="teamProject">The team project that owns the build
    // definition.</param>
    // / <param name="name">The name of the build definition.</param>
    // / <param name="options">Query options</param>
    // / <returns>The build definition owned by the given team project and
    // having the given name.</returns>
    @Override
    public IBuildDefinition getBuildDefinition(
        final String teamProject,
        final String name,
        final QueryOptions options) {
        final BuildDefinitionSpec definitionSpec = new BuildDefinitionSpec(teamProject, name);
        definitionSpec.setOptions(options);

        final IBuildDefinitionQueryResult queryResult = queryBuildDefinitions(definitionSpec);

        if (queryResult.getFailures().length > 0) {
            // Unable to retrieve exactly one build definition - throw exception
            // with inner failure.
            final IFailure failure = queryResult.getFailures()[0];
            throw new BuildDefinitionFailureException(teamProject, name, failure);
        } else if (queryResult.getDefinitions().length == 0) {
            // No build definition was found matching the teamProject and name.
            throw new BuildDefinitionNotFoundException(teamProject, name);
        } else if (queryResult.getDefinitions().length > 1) {
            // More than one build definition was found matching teamProject and
            // name.
            throw new BuildDefinitionSpecNotUniqueException(teamProject, name);
        }

        return queryResult.getDefinitions()[0];
    }

    // / <summary>
    // / Gets an array of build definitions by Uri.
    // / </summary>
    // / <param name="buildDefinitionUris">The Uris of the build definitions to
    // get.</param>
    // / <returns>The build definitions with the given Uris (or null if a Uri
    // was invalid).</returns>
    @Override
    public IBuildDefinition[] queryBuildDefinitionsByURI(final String[] buildDefinitionUris) {
        return queryBuildDefinitionsByURI(buildDefinitionUris, BuildConstants.NO_PROPERTY_NAMES, QueryOptions.NONE);
    }

    // / <summary>
    // / Gets an array of build definitions by Uri and optionally retrieves the
    // build controllers/agents which the
    // / definition references depending on the provided query options.
    // / </summary>
    // / <param name="buildDefinitionUris"></param>
    // / <param name="options">Query options</param>
    // / <returns></returns>
    @Override
    public IBuildDefinition[] queryBuildDefinitionsByURI(
        final String[] buildDefinitionUris,
        final QueryOptions options) {
        return queryBuildDefinitionsByURI(buildDefinitionUris, BuildConstants.NO_PROPERTY_NAMES, options);
    }

    // / <summary>
    // / Gets an array of build definitions by Uri and optionally retrieves the
    // build controllers/agents which the
    // / definition references depending on the provided query options.
    // / </summary>
    // / <param name="buildDefinitionUris"></param>
    // / <param name="propertyNameFilters">Property names to query</param>
    // / <param name="options">Query options</param>
    // / <returns></returns>
    @Override
    public IBuildDefinition[] queryBuildDefinitionsByURI(
        final String[] buildDefinitionUris,
        final String[] propertyNameFilters,
        final QueryOptions options) {
        if (getBuildServerVersion().isV2()) {
            return getBuild2008Helper().queryBuildDefinitionsByUri(buildDefinitionUris).getDefinitions();
        } else if (getBuildServerVersion().isV3()) {
            return getBuild2010Helper().queryBuildDefinitionsByUri(buildDefinitionUris, options).getDefinitions();
        } else {
            return getBuildService().queryBuildDefinitionsByUri(
                buildDefinitionUris,
                propertyNameFilters,
                options).getDefinitions();
        }
    }

    @Override
    public IBuildDefinition[] queryBuildDefinitions(final String teamProject) {
        return queryBuildDefinitions(teamProject, QueryOptions.CONTROLLERS);
    }

    // / <summary>
    // / Gets the build definitions for the given team project.
    // / </summary>
    // / <param name="teamProject">The team project for which build definitions
    // are retrieved.</param>
    // / <param name="options">Query options</param>
    // / <returns>The build definitions for the given team project.</returns>
    @Override
    public IBuildDefinition[] queryBuildDefinitions(final String teamProject, final QueryOptions options) {
        final IBuildDefinitionSpec definitionSpec = createBuildDefinitionSpec(teamProject);
        definitionSpec.setOptions(options);

        final IBuildDefinitionQueryResult queryResult = queryBuildDefinitions(definitionSpec);

        if (queryResult.getFailures().length > 0) {
            final IFailure failure = queryResult.getFailures()[0];
            throw new BuildDefinitionFailureException(teamProject, BuildConstants.STAR, failure);
        }

        return queryResult.getDefinitions();
    }

    // / <summary>
    // / Gets a single build definition query result matching a build definition
    // specification.
    // / </summary>
    // / <param name="buildDefinitionSpec">The build definition specification
    // for which a build definition
    // / query result is retrieved.</param>
    // / <returns>The matching build definition query result.</returns>
    @Override
    public IBuildDefinitionQueryResult queryBuildDefinitions(final IBuildDefinitionSpec buildDefinitionSpec) {
        return queryBuildDefinitions(new IBuildDefinitionSpec[] {
            buildDefinitionSpec
        })[0];
    }

    // / <summary>
    // / Gets an array of build definition query results matching an array of
    // build definition specifications
    // / (of the form \TeamProject\BuildDefinitionName, and supporting
    // wildcards).
    // / </summary>
    // / <param name="buildDefinitionSpecs">The build definition specifications
    // for which build definition query
    // / results are retrieved.</param>
    // / <returns>The matching build definition query results.</returns>
    @Override
    public IBuildDefinitionQueryResult[] queryBuildDefinitions(final IBuildDefinitionSpec[] buildDefinitionSpecs) {
        if (getBuildServerVersion().isV2()) {
            return getBuild2008Helper().queryBuildDefinitions(buildDefinitionSpecs);
        } else if (getBuildServerVersion().isV3()) {
            return getBuild2010Helper().queryBuildDefinitions(buildDefinitionSpecs);
        } else {
            return getBuildService().queryBuildDefinitions(buildDefinitionSpecs);
        }
    }

    // / <summary>
    // / Deletes an array of build definitions from the server.
    // / </summary>
    // / <param name="definitions">The definitions to be deleted.</param>
    @Override
    public void deleteBuildDefinitions(final IBuildDefinition[] definitions) {
        // Ignore null/empty input.
        if (definitions == null || definitions.length == 0) {
            return;
        }

        final String[] uris = new String[definitions.length];

        for (int i = 0; i < definitions.length; i++) {
            uris[i] = definitions[i].getURI();
        }

        deleteBuildDefinitions(uris);
    }

    // / <summary>
    // / Deletes an array of build definitions from the server.
    // / </summary>
    // / <param name="definitionUris">The URIs of the definitions to be
    // deleted.</param>
    @Override
    public void deleteBuildDefinitions(final String[] definitionUris) {
        // Ignore null/empty input.
        if (definitionUris == null || definitionUris.length == 0) {
            return;
        }

        if (getBuildServerVersion().isV2()) {
            getBuild2008Helper().deleteBuildDefinitions(definitionUris);
        } else if (getBuildServerVersion().isV3()) {
            getBuild2010Helper().deleteBuildDefinitions(definitionUris);
        } else {
            getBuildService().deleteBuildDefinitions(definitionUris);
        }
    }

    // / <summary>
    // / Saves any changes made to an array of build definitions to the server.
    // / </summary>
    // / <param name="definitions">The definitions to be saved.</param>
    // / <returns>An array of the added / updated definitions.</returns>
    @Override
    public IBuildDefinition[] saveBuildDefinitions(final IBuildDefinition[] definitions) {
        final List<BuildDefinition> definitionsToAdd = new ArrayList<BuildDefinition>();
        final List<BuildDefinition> definitionsToUpdate = new ArrayList<BuildDefinition>();

        // Return an array (a) of the same size as the input array, and (b) with
        // the "same" objects
        // at the same indices. For adds, all we need to do is map the index in
        // definitionsToAdd
        // to the index in the input array...
        final Map<Integer, Integer> addIndices = new HashMap<Integer, Integer>();

        // ...for updates, we need the index to index mapping, and also a
        // Dictionary mapping Uris
        // to the returned IBuildDefinitions, sine the return value of
        // BuildService.UpdateBuildDefinitions
        // will not necessarily be the same length as the input.
        final Map<Integer, Integer> updateIndices = new HashMap<Integer, Integer>();
        final Map<String, IBuildDefinition> updatedDefinitions = new HashMap<String, IBuildDefinition>();

        final IBuildDefinition[] result = new IBuildDefinition[definitions.length];

        // Sort definitions into Adds and Updates.
        for (int i = 0; i < definitions.length; i++) {
            final BuildDefinition definition = (BuildDefinition) definitions[i];

            definition.prepareToSave();

            if (definition.getURI() == null) {
                definitionsToAdd.add(definition);
                addIndices.put(definitionsToAdd.size() - 1, i);
            } else {
                definitionsToUpdate.add(definition);
                updateIndices.put(definitionsToUpdate.size() - 1, i);
            }
        }

        // Add all the definitions-to-add and update their Uris.
        if (definitionsToAdd.size() > 0) {
            IBuildDefinition[] addResult;
            final BuildDefinition[] toAdd = definitionsToAdd.toArray(new BuildDefinition[definitionsToAdd.size()]);

            if (getBuildServerVersion().isV2()) {
                addResult = getBuild2008Helper().addBuildDefinitions(toAdd);
            } else if (getBuildServerVersion().isV3()) {
                addResult = getBuild2010Helper().addBuildDefinitions(toAdd);
            } else {
                addResult = getBuildService().addBuildDefinitions(toAdd);
            }

            for (int i = 0; i < addResult.length; i++) {
                // Copy attached properties and reset the delta
                final BuildDefinition definition = (BuildDefinition) addResult[i];
                definition.setAttachedProperties(definitionsToAdd.get(i).getAttachedProperties());
                definition.getAttachedProperties().clearChangedProperties();

                definitionsToAdd.get(i).setURI(addResult[i].getURI());

                // Put the add in the appropriate spot in the result array.
                result[addIndices.get(i)] = addResult[i];
            }
        }

        // Update all the definitions-to-update.
        if (definitionsToUpdate.size() > 0) {
            IBuildDefinition[] updateResult;
            final BuildDefinition[] toUpdate =
                definitionsToUpdate.toArray(new BuildDefinition[definitionsToUpdate.size()]);

            if (getBuildServerVersion().isV2()) {
                updateResult = getBuild2008Helper().updateBuildDefinitions(toUpdate);
            } else if (getBuildServerVersion().isV3()) {
                updateResult = getBuild2010Helper().updateBuildDefinitions(toUpdate);
            } else {
                updateResult = getBuildService().updateBuildDefinitions(toUpdate);
            }

            if (definitionsToAdd.size() == 0) {
                // Don't waste time trying to sort the udpates if we had no
                // adds.
                return updateResult;
            } else {
                // Put the updates in the appropriate spot(s) in the result
                // array.
                for (final IBuildDefinition updatedDefinition : updateResult) {
                    updatedDefinitions.put(updatedDefinition.getURI(), updatedDefinition);
                }

                for (int i = 0; i < definitionsToUpdate.size(); i++) {
                    final int updatedIndex = updateIndices.get(i);
                    result[updatedIndex] = updatedDefinitions.get(definitions[updatedIndex].getURI());

                    // Copy attached properties and reset the delta
                    final BuildDefinition definition = (BuildDefinition) result[updatedIndex];
                    definition.setAttachedProperties(
                        ((BuildDefinition) definitions[updatedIndex]).getAttachedProperties());
                    definition.getAttachedProperties().clearChangedProperties();
                }
            }
        }

        return result;
    }

    // / <summary>
    // / Deletes the service host and all associated services from the server.
    // / </summary>
    @Override
    public void deleteBuildServiceHost(final String serviceHostUri) {
        if (getBuildServerVersion().isV2()) {
            throwMethodNotSupported("DeleteBuildServiceHost"); //$NON-NLS-1$
        }

        if (getBuildServerVersion().isV3()) {
            getBuild2010Helper().deleteBuildServiceHost(serviceHostUri);
        } else {
            getAdministrationService().deleteBuildServiceHost(serviceHostUri);
        }
    }

    // / <summary>
    // / Gets the service host that matches the given name.
    // / </summary>
    @Override
    public IBuildServiceHost getBuildServiceHostByName(final String computer) {
        if (getBuildServerVersion().isV2()) {
            throwMethodNotSupported("GetBuildServiceHost"); //$NON-NLS-1$
        }

        final IBuildServiceHost[] results = queryBuildServiceHosts(computer);

        if (results.length == 0) {
            throw new BuildServiceHostNotFoundException(computer);
        } else if (results.length > 1) {
            throw new BuildServiceHostSpecNotUniqueException(computer);
        }

        return results[0];
    }

    // / <summary>
    // / Gets the service host that matches the given URI.
    // / </summary>
    @Override
    public IBuildServiceHost getBuildServiceHostByURI(final String buildServiceHostUri) {
        if (getBuildServerVersion().isV2()) {
            throwMethodNotSupported("GetBuildServiceHost"); //$NON-NLS-1$
        }

        final IBuildServiceHost[] results = queryBuildServiceHostsByURI(new String[] {
            buildServiceHostUri
        });

        if (results[0] == null) {
            throw new BuildServiceHostNotFoundForURIException(buildServiceHostUri, getDomainUserName());
        }

        return results[0];
    }

    // / <summary>
    // / Queries for the list of all build service hosts that are hosted on the
    // specified computers.
    // / </summary>
    @Override
    public IBuildServiceHost[] queryBuildServiceHosts(final String computer) {
        if (getBuildServerVersion().isLessThanV3()) {
            throwMethodNotSupported("QueryBuildServiceHosts"); //$NON-NLS-1$
        }

        if (getBuildServerVersion().isV3()) {
            return getBuild2010Helper().queryBuildServiceHosts(computer).getServiceHosts();
        } else {
            return getAdministrationService().queryBuildServiceHosts(computer).getServiceHosts();
        }
    }

    // / <summary>
    // / Gets the build service hosts that match the given URIs.
    // / </summary>
    @Override
    public IBuildServiceHost[] queryBuildServiceHostsByURI(final String[] buildServiceHostUris) {
        if (getBuildServerVersion().isLessThanV3()) {
            throwMethodNotSupported("QueryBuildServiceHostsByUri"); //$NON-NLS-1$
        }

        if (getBuildServerVersion().isV3()) {
            return getBuild2010Helper().queryBuildServiceHostsByUri(buildServiceHostUris).getServiceHosts();
        } else {
            return getAdministrationService().queryBuildServiceHostsByUri(buildServiceHostUris).getServiceHosts();
        }
    }

    // / <summary>
    // / Saves the service host changes to the server.
    // / </summary>
    @Override
    public void saveBuildServiceHost(final IBuildServiceHost serviceHost) {
        if (getBuildServerVersion().isV2()) {
            throwMethodNotSupported("SaveBuildServiceHost"); //$NON-NLS-1$
        }

        if (serviceHost.getURI() == null) {
            BuildServiceHost result;

            if (getBuildServerVersion().isV3()) {
                result = getBuild2010Helper().addBuildServiceHost((BuildServiceHost) serviceHost);
            } else {
                result = getAdministrationService().addBuildServiceHost((BuildServiceHost) serviceHost);
            }

            ((BuildServiceHost) serviceHost).setURI(result.getURI());

            // Update the URIs of the child objects (controller and agents)
            if (serviceHost.getController() != null) {
                ((BuildController) serviceHost.getController()).setServiceHost(serviceHost);
            }
            for (final IBuildAgent agent : serviceHost.getAgents()) {
                ((BuildAgent) agent).setServiceHost(serviceHost);
            }
        } else {
            final BuildServiceHostUpdateOptions update = ((BuildServiceHost) serviceHost).getUpdateOptions();

            if (!update.getFields().equals(BuildServiceHostUpdate.NONE)) {
                if (getBuildServerVersion().isV3()) {
                    getBuild2010Helper().updateBuildServiceHost(update);
                } else if (getBuildServerVersion().isV4()) {
                    getAdministrationService().updateBuildServiceHost(update);
                }

                ((BuildServiceHost) serviceHost).setUpdateOptions(update);
            }
        }
    }

    // / <summary>
    // / Tells AT to test connection for build machine resources and updates
    // resources statuses/status messages
    // / </summary>
    // / <param name="host">BuildServiceHost to be tested.</param>
    // / <returns>Updated BuildServiceHost with all the resources.</returns>
    @Override
    public void testConnectionsForBuildMachine(final IBuildServiceHost host) {
        if (getBuildServerVersion().isLessThanV3()) {
            throwMethodNotSupported("TestConnectionForBuildMachine"); //$NON-NLS-1$
        }

        if (getBuildServerVersion().isV3()) {
            final IBuildServiceHost result =
                getBuild2010Helper().testBuildServiceHostConnections(host.getURI()).getServiceHosts()[0];
            if (result == null) {
                throw new BuildServiceHostNotFoundForURIException(host.getURI(), getDomainUserName());
            }
        }
    }

    // / <summary>
    // / Tells AT to test connection for build controller and updates the
    // controller status/status message
    // / </summary>
    // / <param name="controller">BuildController to be tested.</param>
    @Override
    public void testConnectionForBuildController(final IBuildController controller) {
        if (getBuildServerVersion().isLessThanV3()) {
            throwMethodNotSupported("TestConnectionForBuildController"); //$NON-NLS-1$
        }

        if (getBuildServerVersion().isV3()) {
            final IBuildController result =
                getBuild2010Helper().testBuildControllerConnection(controller.getURI()).getControllers()[0];
            if (result == null) {
                throw new BuildControllerNotFoundForURIException(controller.getURI(), getDomainUserName());
            }
        }
    }

    // / <summary>
    // / Tells AT to test connection for build agent and updates the agent
    // status/status message
    // / </summary>
    // / <param name="agent">BuildAgent to be tested.</param>
    @Override
    public void testConnectionForBuildAgent(final IBuildAgent agent) {
        if (getBuildServerVersion().isLessThanV3()) {
            throwMethodNotSupported("TestConnectionForBuildAgent"); //$NON-NLS-1$
        }

        if (getBuildServerVersion().isV3()) {
            final IBuildAgent result = getBuild2010Helper().testBuildAgentConnection(agent.getURI()).getAgents()[0];
            if (result == null) {
                throw new BuildAgentNotFoundForURIException(agent.getURI(), getDomainUserName());
            }
        }
    }

    // / <summary>
    // / Gets a single build agent by Uri.
    // / </summary>
    // / <param name="buildAgentUri">The Uri of the build agent to get.</param>
    // / <returns>The build agent with the given Uri.</returns>
    @Override
    public IBuildAgent getBuildAgent(final String buildAgentUri) {
        return getBuildAgent(buildAgentUri, BuildConstants.NO_PROPERTY_NAMES);
    }

    // / <summary>
    // / Gets a single build agent by Uri.
    // / </summary>
    // / <param name="buildAgentUri">The Uri of the build agent to get.</param>
    // / <param name="propertyNameFilters">The property names to get.</param>
    // / <returns>The build agent with the given Uri.</returns>
    @Override
    public IBuildAgent getBuildAgent(final String buildAgentUri, final String[] propertyNameFilters) {
        if (getBuildServerVersion().isLessThanV3()) {
            throwMethodNotSupported("GetBuildAgent"); //$NON-NLS-1$
        }

        final IBuildAgent[] results = queryBuildAgentsByURI(new String[] {
            buildAgentUri
        }, propertyNameFilters);

        if (results[0] == null) {
            throw new BuildAgentNotFoundForURIException(buildAgentUri, getDomainUserName());
        }

        return results[0];
    }

    // / <summary>
    // / Gets an array of build agents by Uri.
    // / </summary>
    // / <param name="buildAgentUris">The Uris of the build agents to
    // get.</param>
    // / <returns>The build agents with the given Uris (or null if a Uri was
    // invalid).</returns>
    @Override
    public IBuildAgent[] queryBuildAgentsByURI(final String[] buildAgentUris) {
        return queryBuildAgentsByURI(buildAgentUris, BuildConstants.NO_PROPERTY_NAMES);
    }

    // / <summary>
    // / Gets an array of build agents by Uri.
    // / </summary>
    // / <param name="buildAgentUris">The Uris of the build agents to
    // get.</param>
    // / <param name="propertyNameFilters">The property names to get.</param>
    // / <returns>The build agents with the given Uris (or null if a Uri was
    // invalid).</returns>
    @Override
    public IBuildAgent[] queryBuildAgentsByURI(final String[] buildAgentUris, final String[] propertyNameFilters) {
        if (getBuildServerVersion().isLessThanV3()) {
            throwMethodNotSupported("QueryBuildAgentsByUri"); //$NON-NLS-1$
        }

        if (getBuildServerVersion().isV3()) {
            return getBuild2010Helper().queryBuildAgentsByUri(buildAgentUris).getAgents();
        } else {
            return getAdministrationService().queryBuildAgentsByUri(buildAgentUris, propertyNameFilters).getAgents();
        }
    }

    // / <summary>
    // / Gets a build agent query result matching a build agent specification.
    // / </summary>
    // / <param name="buildAgentSpec">The build agent specification for which a
    // build agent query result is returned.</param>
    // / <returns>The matching build agent query result.</returns>
    @Override
    public IBuildAgentQueryResult queryBuildAgents(final IBuildAgentSpec buildAgentSpec) {
        return queryBuildAgents(new IBuildAgentSpec[] {
            buildAgentSpec
        })[0];
    }

    // / <summary>
    // / Gets an array of build agent query results matching an array of build
    // agent specifications
    // / (of the form \TeamProject\BuildAgentName, and supporting wildcards).
    // / </summary>
    // / <param name="buildAgentSpecs">The build agent specifications for which
    // build agent query
    // / results are retrieved.</param>
    // / <returns>The matching build agent query results.</returns>
    @Override
    public IBuildAgentQueryResult[] queryBuildAgents(final IBuildAgentSpec[] buildAgentSpecs) {
        if (getBuildServerVersion().isLessThanV3()) {
            throwMethodNotSupported("QueryBuildAgents"); //$NON-NLS-1$
        }

        if (getBuildServerVersion().isV3()) {
            return getBuild2010Helper().queryBuildAgents(buildAgentSpecs);
        } else {
            return getAdministrationService().queryBuildAgents(buildAgentSpecs);
        }
    }

    // / <summary>
    // / Deletes an array of build agents from the server.
    // / </summary>
    // / <param name="agents">The agents to be deleted.</param>
    @Override
    public void deleteBuildAgents(final IBuildAgent[] agents) {
        if (getBuildServerVersion().isLessThanV3()) {
            throwMethodNotSupported("DeleteBuildAgents"); //$NON-NLS-1$
        }

        // Ignore null/empty input.
        if (agents == null || agents.length == 0) {
            return;
        }

        final String[] uris = new String[agents.length];

        for (int i = 0; i < agents.length; i++) {
            uris[i] = agents[i].getURI();
        }

        deleteBuildAgents(uris);
    }

    // / <summary>
    // / Deletes an array of build agents from the server.
    // / </summary>
    // / <param name="agentUris">The URIs of the agents to be deleted.</param>
    @Override
    public void deleteBuildAgents(final String[] agentUris) {
        if (getBuildServerVersion().isLessThanV3()) {
            throwMethodNotSupported("DeleteBuildAgents"); //$NON-NLS-1$
        }

        // Ignore null/empty input.
        if (agentUris == null || agentUris.length == 0) {
            return;
        }

        if (getBuildServerVersion().isV3()) {
            getBuild2010Helper().deleteBuildAgents(agentUris);
        } else {
            getAdministrationService().deleteBuildAgents(agentUris);
        }
    }

    // / <summary>
    // / Saves any changes made to an array of build agents to the server.
    // / </summary>
    // / <param name="agents">The agents to be saved.</param>
    // / <returns>An array of the added / updated build agents.</returns>
    @Override
    public IBuildAgent[] saveBuildAgents(final IBuildAgent[] agents) {
        if (getBuildServerVersion().isLessThanV3()) {
            throwMethodNotSupported("SaveBuildAgents"); //$NON-NLS-1$
        }

        final List<BuildAgent> agentsToUpdate = new ArrayList<BuildAgent>();
        final Map<String, BuildAgent> agentsToAdd = new TreeMap<String, BuildAgent>(String.CASE_INSENSITIVE_ORDER);

        for (int i = 0; i < agents.length; i++) {
            final BuildAgent agent = (BuildAgent) agents[i];

            if (agent.getServiceHostURI() == null || agent.getControllerURI() == null) {
                throw new BuildAgentNotReadyToSaveException(agent.getName());
            }

            agent.prepareToSave();

            if (agent.getURI() == null) {
                final String key = agent.getServiceHostURI() + "." + agent.getName(); //$NON-NLS-1$
                agentsToAdd.put(key, agent);
            } else {
                agentsToUpdate.add(agent);
            }
        }

        if (agentsToAdd.size() > 0) {
            final BuildAgent[] addRequests = new BuildAgent[agentsToAdd.size()];
            agentsToAdd.values().toArray(addRequests);

            BuildAgent[] addResults;
            if (getBuildServerVersion().isV3()) {
                addResults = getBuild2010Helper().addBuildAgents(addRequests);
            } else {
                addResults = getAdministrationService().addBuildAgents(addRequests);
            }

            for (final BuildAgent addResult : addResults) {
                final String key = addResult.getServiceHostURI() + "." + addResult.getName(); //$NON-NLS-1$
                agentsToAdd.get(key).setURI(addResult.getURI());

                // Make sure that the snapshot is updated for the newly added
                // agent to reflect the properties that
                // were set at the time of adding.
                agentsToAdd.get(key).setUpdateOptions(addResult.getUpdateOptions());
            }
        }

        if (agentsToUpdate.size() > 0) {
            // Generate an equal size list of update option structures for the
            // agent updates which we need to make.
            final List<BuildAgentUpdateOptions> actualUpdates = new ArrayList<BuildAgentUpdateOptions>();
            final List<BuildAgentUpdateOptions> updateOptions =
                new ArrayList<BuildAgentUpdateOptions>(agentsToUpdate.size());
            for (int i = 0; i < agentsToUpdate.size(); i++) {
                final BuildAgentUpdateOptions update = agentsToUpdate.get(i).getUpdateOptions();

                // If after determining the update that would need to take place
                // there are no changes necessary
                // we insert null into our update options list
                if (!update.getFields().equals(BuildAgentUpdate.NONE)) {
                    actualUpdates.add(update);
                    updateOptions.add(i, update);
                } else {
                    updateOptions.add(i, null);
                }
            }

            // Call the server to see if the specified updates are valid.
            if (actualUpdates.size() > 0) {
                if (getBuildServerVersion().isV3()) {
                    getBuild2010Helper().updateBuildAgents(
                        actualUpdates.toArray(new BuildAgentUpdateOptions[actualUpdates.size()]));
                } else {
                    getAdministrationService().updateBuildAgents(
                        actualUpdates.toArray(new BuildAgentUpdateOptions[actualUpdates.size()]));
                }

                // Set the update options for each of the agents to ensure that
                // the snapshot is up to date with the
                // current properties which were saved to the server.
                for (int i = 0; i < agentsToUpdate.size(); i++) {
                    // If there were no fields to update then we may have a null
                    // index, so we need to be sure that
                    // this is not the case.
                    if (updateOptions.get(i) != null) {
                        agentsToUpdate.get(i).setUpdateOptions(updateOptions.get(i));
                    }
                }
            }
        }

        return agents;
    }

    // / <summary>
    // / Gets a single build controller.
    // / </summary>
    // / <param name="buildControllerUri">The URI of the build
    // controller.</param>
    // / <returns>The build controller with the given URI.</returns>
    @Override
    public IBuildController getBuildController(final String buildControllerUri, final boolean includeAgents) {
        return getBuildController(buildControllerUri, BuildConstants.NO_PROPERTY_NAMES, includeAgents);
    }

    // / <summary>
    // / Gets a single build controller.
    // / </summary>
    // / <param name="buildControllerUri">The URI of the build
    // controller.</param>
    // / <param name="propertyNameFilters">The property names to get.</param>
    // / <returns>The build controller with the given URI.</returns>
    @Override
    public IBuildController getBuildController(
        final String buildControllerUri,
        final String[] propertyNameFilters,
        final boolean includeAgents) {
        final IBuildController[] results = queryBuildControllersByURI(new String[] {
            buildControllerUri
        }, propertyNameFilters, includeAgents);

        if (results[0] == null) {
            throw new BuildControllerNotFoundForURIException(buildControllerUri, getDomainUserName());
        }

        return results[0];
    }

    // / <summary>
    // / Retrieves a single build controller using the specified display name. A
    // wild card may be specified, but
    // / if more than one controller is matched than an exception is thrown.
    // / </summary>
    @Override
    public IBuildController getBuildController(final String name) {
        final IBuildControllerQueryResult result = queryBuildControllers(
            new BuildControllerSpec(name, BuildConstants.STAR, BuildConstants.NO_PROPERTY_NAMES, true));

        if (result.getControllers().length == 0) {
            throw new BuildControllerNotFoundException(name);
        } else if (result.getControllers().length > 1) {
            throw new BuildControllerSpecNotUniqueException(name);
        }

        return result.getControllers()[0];
    }

    // / <summary>
    // / Gets all build controllers and their associated agents.
    // / </summary>
    // / <returns>All of the build controllers.</returns>
    @Override
    public IBuildController[] queryBuildControllers() {
        return queryBuildControllers(true);
    }

    // / <summary>
    // / Gets all build controllers.
    // / </summary>
    // / <returns>All of the build controllers.</returns>
    @Override
    public IBuildController[] queryBuildControllers(final boolean includeAgents) {
        final IBuildControllerSpec spec = createBuildControllerSpec(BuildConstants.STAR, BuildConstants.STAR);
        spec.setIncludeAgents(includeAgents);

        return queryBuildControllers(spec).getControllers();
    }

    // / <summary>
    // / Gets the build controllers that match the given URIs.
    // / </summary>
    // / <param name="buildControllerUris">The URIs of the build
    // controllers.</param>
    // / <returns>The build controllers with the given URIs.</returns>
    @Override
    public IBuildController[] queryBuildControllersByURI(
        final String[] buildControllerUris,
        final boolean includeAgents) {
        return queryBuildControllersByURI(buildControllerUris, BuildConstants.NO_PROPERTY_NAMES, includeAgents);
    }

    // / <summary>
    // / Gets the build controllers that match the given URIs.
    // / </summary>
    // / <param name="buildControllerUris">The URIs of the build
    // controllers.</param>
    // / <param name="propertyNameFilters">The property names to get.</param>
    // / <returns>The build controllers with the given URIs.</returns>
    @Override
    public IBuildController[] queryBuildControllersByURI(
        final String[] buildControllerUris,
        final String[] propertyNameFilters,
        final boolean includeAgents) {
        if (getBuildServerVersion().isV2()) {
            return getBuild2008Helper().queryBuildControllersByUri(buildControllerUris, includeAgents).getControllers();
        } else if (getBuildServerVersion().isV3()) {
            return getBuild2010Helper().queryBuildControllersByUri(buildControllerUris, includeAgents).getControllers();
        } else {
            return getAdministrationService().queryBuildControllersByUri(
                buildControllerUris,
                propertyNameFilters,
                includeAgents).getControllers();
        }
    }

    // / <summary>
    // / Gets a single build controller query result for a given build
    // controller specification.
    // / </summary>
    // / <param name="buildControllerSpec">The build controller
    // specification.</param>
    // / <returns>The build controller query result for the given
    // specification.</returns>
    @Override
    public IBuildControllerQueryResult queryBuildControllers(final IBuildControllerSpec buildControllerSpec) {
        return queryBuildControllers(new IBuildControllerSpec[] {
            buildControllerSpec
        })[0];
    }

    // / <summary>
    // / Gets the build controller query results for the given build controller
    // specifications.
    // / </summary>
    // / <param name="buildControllerSpecs">The build controller
    // specifications.</param>
    // / <returns>The build controller query results for the given
    // specifications.</returns>
    @Override
    public IBuildControllerQueryResult[] queryBuildControllers(final IBuildControllerSpec[] buildControllerSpecs) {
        if (getBuildServerVersion().isV2()) {
            return getBuild2008Helper().queryBuildControllers(buildControllerSpecs);
        } else if (getBuildServerVersion().isV3()) {
            return getBuild2010Helper().queryBuildControllers(buildControllerSpecs);
        } else {
            return getAdministrationService().queryBuildControllers(buildControllerSpecs);
        }
    }

    // / <summary>
    // / Deletes build controllers from the server.
    // / </summary>
    // / <param name="Controllers">The controllers to be deleted.</param>
    @Override
    public void deleteBuildControllers(final IBuildController[] controllers) {
        if (getBuildServerVersion().isLessThanV3()) {
            throwMethodNotSupported("DeleteBuildControllers"); //$NON-NLS-1$
        }

        if (controllers == null || controllers.length == 0) {
            return;
        }

        final String[] controllerUris = new String[controllers.length];

        for (int i = 0; i < controllers.length; i++) {
            controllerUris[i] = controllers[i].getURI();
        }

        deleteBuildControllers(controllerUris);
    }

    // / <summary>
    // / Deletes build controllers from the server.
    // / </summary>
    // / <param name="ControllerUris">The URIs of the controllers to be
    // deleted.</param>
    @Override
    public void deleteBuildControllers(final String[] controllerUris) {
        if (getBuildServerVersion().isLessThanV3()) {
            throwMethodNotSupported("DeleteBuildControllers"); //$NON-NLS-1$
        }

        if (getBuildServerVersion().isV3()) {
            getBuild2010Helper().deleteBuildControllers(controllerUris);
        } else {
            getAdministrationService().deleteBuildControllers(controllerUris);
        }
    }

    // / <summary>
    // / Saves any changes made to the build controllers to the server.
    // / </summary>
    // / <param name="Controllers">The controllers to be saved.</param>
    // / <returns>The saved build controllers.</returns>
    @Override
    public IBuildController[] saveBuildControllers(final IBuildController[] controllers) {
        if (getBuildServerVersion().isLessThanV3()) {
            throwMethodNotSupported("SaveBuildControllers"); //$NON-NLS-1$
        }

        final List<BuildController> controllersToUpdate = new ArrayList<BuildController>();
        final Map<String, BuildController> controllersToAdd = new HashMap<String, BuildController>();

        // There can only be one build controller per service host, so for hosts
        // which are being added we can
        // simply key them off of the service host URI. We do not need to worry
        // about duplicate build controllers
        // being added here since the server will throw an error and the entire
        // save will fail.
        for (int i = 0; i < controllers.length; i++) {
            final BuildController controller = (BuildController) controllers[i];

            if (controller.getServiceHostURI() == null) {
                throw new BuildControllerNotReadyToSaveException(controller.getName());
            }

            controller.prepareToSave();

            if (controller.getURI() == null) {
                controllersToAdd.put(controller.getServiceHostURI(), controller);
            } else {
                controllersToUpdate.add(controller);
            }
        }

        if (controllersToAdd.size() > 0) {
            final BuildController[] addRequests = new BuildController[controllersToAdd.size()];
            controllersToAdd.values().toArray(addRequests);

            BuildController[] addResults;
            if (getBuildServerVersion().isV3()) {
                addResults = getBuild2010Helper().addBuildControllers(addRequests);
            } else {
                addResults = getAdministrationService().addBuildControllers(addRequests);
            }

            for (final BuildController addResult : addResults) {
                final BuildController controller = controllersToAdd.get(addResult.getServiceHostURI());

                // Assign the URI in place to the added controllers
                controller.setURI(addResult.getURI());

                // Make sure that the snapshot is updated for the newly added
                // controller to reflect the properties that
                // were set at the time of adding.
                controllersToAdd.get(addResult.getServiceHostURI()).setUpdateOptions(addResult.getUpdateOptions());

                // Update the URIs of the child agents
                // First we take out all the agents from controller (saving them
                // in the list)
                final List<IBuildAgent> agents = new ArrayList<IBuildAgent>();
                while (controller.getAgents().length > 0) {
                    final IBuildAgent agent = controller.getAgents()[0];
                    agents.add(agent);
                    controller.removeBuildAgent(agent);
                }
                // Then we add all saved agents back to controller
                for (final IBuildAgent agent : agents) {
                    ((BuildAgent) agent).setController(controller);
                }
                // All that is done just to assign ControllerUri property for
                // all the controller's agents
            }
        }

        if (controllersToUpdate.size() > 0) {
            // Generate an equal size list of update option structures for the
            // agent updates which we need to make.
            final List<BuildControllerUpdateOptions> actualUpdates = new ArrayList<BuildControllerUpdateOptions>();
            final List<BuildControllerUpdateOptions> updateOptions =
                new ArrayList<BuildControllerUpdateOptions>(controllersToUpdate.size());
            for (int i = 0; i < controllersToUpdate.size(); i++) {
                final BuildControllerUpdateOptions update = controllersToUpdate.get(i).getUpdateOptions();

                // If after determining the update that would need to take place
                // there are no changes necessary
                // we insert null into our update options list
                if (!update.getFields().equals(BuildControllerUpdate.NONE)) {
                    actualUpdates.add(update);
                    updateOptions.add(i, update);
                } else {
                    updateOptions.add(i, null);
                }
            }

            // Call the server to see if the specified updates are valid.
            if (actualUpdates.size() > 0) {
                final BuildControllerUpdateOptions[] array = new BuildControllerUpdateOptions[actualUpdates.size()];

                if (getBuildServerVersion().isV3()) {
                    getBuild2010Helper().updateBuildControllers(actualUpdates.toArray(array));
                } else {
                    getAdministrationService().updateBuildControllers(actualUpdates.toArray(array));
                }

                // Set the update options for each of the agents to ensure that
                // the snapshot is up to date with the
                // current properties which were saved to the server.
                for (int i = 0; i < controllersToUpdate.size(); i++) {
                    // If there were no fields to update then we may have a null
                    // index, so we need to be sure that
                    // this is not the case.
                    if (updateOptions.get(i) != null) {
                        controllersToUpdate.get(i).setUpdateOptions(updateOptions.get(i));
                    }
                }
            }
        }

        return controllers;
    }

    // / <summary>
    // / Gets a queued build for a given ID.
    // / </summary>
    // / <param name="queuedBuildId">The ID of the queued build to be
    // retrieved.</param>
    // / <param name="queryOptions">Options specifying the additional data to be
    // returned.</param>
    // / <returns>The queued build for the given ID.</returns>
    @Override
    public IQueuedBuild getQueuedBuild(final int queuedBuildId, final QueryOptions queryOptions) {
        return getQueuedBuild(queuedBuildId, new String[0], queryOptions);
    }

    public IQueuedBuild getQueuedBuild(
        final int queuedBuildId,
        final String[] informationTypes,
        final QueryOptions queryOptions) {
        return getQueuedBuild(new int[] {
            queuedBuildId
        }, informationTypes, queryOptions)[0];
    }

    // / <summary>
    // / Gets an array of queued builds for an array of IDs.
    // / </summary>
    // / <param name="queuedBuildIds">The IDs of the queued builds to be
    // retrieved.</param>
    // / <param name="queryOptions">Options specifying the additional data to be
    // returned.</param>
    // / <returns>The queued builds for the given IDs.</returns>
    @Override
    public IQueuedBuild[] getQueuedBuild(final int[] queuedBuildIds, final QueryOptions queryOptions) {
        return getQueuedBuild(queuedBuildIds, new String[0], queryOptions);
    }

    public IQueuedBuild[] getQueuedBuild(
        final int[] queuedBuildIds,
        final String[] informationTypes,
        final QueryOptions queryOptions) {
        if (getBuildServerVersion().isV2()) {
            return getBuild2008Helper().queryQueuedBuildsById(queuedBuildIds, queryOptions).getQueuedBuilds();
        } else if (getBuildServerVersion().isV3()) {
            return getBuild2010Helper().queryQueuedBuildsById(queuedBuildIds, queryOptions).getQueuedBuilds();
        } else {
            return getBuildQueueService().queryBuildsById(
                queuedBuildIds,
                informationTypes,
                queryOptions).getQueuedBuilds();
        }
    }

    // / <summary>
    // / Gets a single build query result for a given queued build
    // specification.
    // / </summary>
    // / <param name="buildDetailSpec">The queued build specification.</param>
    // / <returns>The queued build query result for the given
    // specification.</returns>
    @Override
    public IQueuedBuildQueryResult queryQueuedBuilds(final IQueuedBuildSpec buildQueueSpec) {
        return queryQueuedBuilds(new IQueuedBuildSpec[] {
            buildQueueSpec
        })[0];
    }

    // / <summary>
    // / Gets the build query results for a given array of queued build
    // specifications.
    // / </summary>
    // / <param name="buildDetailSpec">The queued build specifications.</param>
    // / <returns>The queued build query results for the given
    // specifications.</returns>
    @Override
    public IQueuedBuildQueryResult[] queryQueuedBuilds(final IQueuedBuildSpec[] buildQueueSpecs) {
        if (getBuildServerVersion().isV2()) {
            return getBuild2008Helper().queryQueuedBuilds(buildQueueSpecs);
        } else if (getBuildServerVersion().isV3()) {
            return getBuild2010Helper().queryQueuedBuilds(buildQueueSpecs);
        } else {
            return getBuildQueueService().queryBuilds(buildQueueSpecs);
        }
    }

    // / <summary>
    // / Retries the specified builds and places all specified builds into a
    // batch together.
    // / </summary>
    // / <param name="queuedBuilds">The queued builds to be retried in a
    // batch.</param>
    // / <returns>The updated queued builds.</returns>
    @Override
    public IQueuedBuild[] retryQueuedBuilds(final IQueuedBuild[] queuedBuilds) {
        return retryQueuedBuilds(queuedBuilds, GUID.newGUID());
    }

    // / <summary>
    // / Retries the specified builds and places all specified builds into a
    // batch together.
    // / </summary>
    // / <param name="queuedBuilds">The queued builds to be retried in a
    // batch.</param>
    // / <param name="batchId">The ID of the newly created batch.</param>
    // / <returns>The updated queued builds.</returns>
    @Override
    public IQueuedBuild[] retryQueuedBuilds(final IQueuedBuild[] queuedBuilds, final GUID batchId) {
        return retryQueuedBuilds(queuedBuilds, batchId, QueuedBuildRetryOption.IN_PROGRESS_BUILD);
    }

    // / <summary>
    // / Retries the specified builds with the specific retry option and places
    // all specified builds into a batch together.
    // / </summary>
    // / <param name="queuedBuilds">The queued builds to be retried in a
    // batch.</param>
    // / <param name="batchId">The ID of the newly created batch.</param>
    // / <param name="retryOption">Option to retry a completed or an in progress
    // build</param>
    // / <returns>The updated queued builds.</returns>
    @Override
    public IQueuedBuild[] retryQueuedBuilds(
        final IQueuedBuild[] queuedBuilds,
        final GUID batchId,
        final QueuedBuildRetryOption retryOption) {
        if (getBuildServerVersion().isLessThanV4()) {
            throwMethodNotSupported("RetryQueuedBuilds"); //$NON-NLS-1$
        }

        if (queuedBuilds == null || queuedBuilds.length == 0) {
            return queuedBuilds;
        }

        for (final IQueuedBuild build : queuedBuilds) {
            build.retry(batchId, retryOption);
        }

        return saveQueuedBuilds(queuedBuilds);
    }

    // / <summary>
    // / Saves any changes made to an array of queued builds to the server.
    // / </summary>
    // / <param name="queuedBuilds">The queued builds to be saved.</param>
    // / <returns>An array of the saved queued builds.</returns>
    @Override
    public IQueuedBuild[] saveQueuedBuilds(final IQueuedBuild[] queuedBuilds) {
        // Ignore null/empty input.
        if (queuedBuilds == null || queuedBuilds.length == 0) {
            return queuedBuilds;
        }

        IQueuedBuild[] result;
        final QueuedBuildUpdateOptions[] updateOptions = new QueuedBuildUpdateOptions[queuedBuilds.length];
        final Map<Integer, QueuedBuildUpdateOptions> actualUpdates = new HashMap<Integer, QueuedBuildUpdateOptions>();

        // Update the builds on the server.
        for (int i = 0; i < queuedBuilds.length; i++) {
            updateOptions[i] = ((QueuedBuild) queuedBuilds[i]).getUpdateOptions();
        }

        if (getBuildServerVersion().isV2()) {
            result = getBuild2008Helper().updateQueuedBuilds(updateOptions);
        } else if (getBuildServerVersion().isV3()) {
            result = getBuild2010Helper().updateQueuedBuilds(updateOptions);
        } else {
            result = getBuildQueueService().updateBuilds(updateOptions).getQueuedBuilds();
        }

        // If successful, set the UpdateOptions for each build. If updateOptions
        // contained multiple
        // updates for the same Queued Build ID, only the last one will have
        // been used in prc_UpdateQueuedBuilds.
        // We need to make sure that the input builds reflect this.
        for (final IQueuedBuild resultBuild : result) {
            if (resultBuild != null) {
                actualUpdates.put(resultBuild.getID(), ((QueuedBuild) resultBuild).getUpdateOptions());
            }
        }

        for (final IQueuedBuild build : queuedBuilds) {
            if (build != null) {
                ((QueuedBuild) build).setUpdateOptions(actualUpdates.get(build.getID()));
            }
        }

        return result;
    }

    // / <summary>
    // / Queues a build for the given build definition with all default options.
    // Equivalent to
    // / calling QueueBuild(definition.CreateBuildRequest()).
    // / </summary>
    // / <param name="definition">The definition for which a build is
    // queued.</param>
    // / <returns>The queued build.</returns>
    @Override
    public IQueuedBuild queueBuild(final IBuildDefinition definition) {
        return queueBuild(definition.createBuildRequest());
    }

    // / <summary>
    // / Queues a build for the given build request with default QueueOptions.
    // / </summary>
    // / <param name="request">The parameters used in queuing the build,
    // including the BuildDefinition and BuildAgent.</param>
    // / <returns>The queued build.</returns>
    @Override
    public IQueuedBuild queueBuild(final IBuildRequest request) {
        return queueBuild(request, QueueOptions.NONE);
    }

    // / <summary>
    // / Queues a build for the given build request.
    // / </summary>
    // / <param name="request">The parameters used in queuing the build,
    // including the BuildDefinition and BuildAgent.</param>
    // / <param name="options">The options for the queuing of the build,
    // including whether or not to preview the queue position.</param>
    // / <returns>The queued build.</returns>
    @Override
    public IQueuedBuild queueBuild(final IBuildRequest request, final QueueOptions options) {
        if (getBuildServerVersion().isV2()) {
            return getBuild2008Helper().queueBuild((BuildRequest) request, options);
        } else {
            return queueBuild(new IBuildRequest[] {
                (BuildRequest) request
            }, options)[0];
        }
    }

    @Override
    public IQueuedBuild[] queueBuild(final IBuildRequest[] requests, final QueueOptions options) {
        if (getBuildServerVersion().isLessThanV3()) {
            throwMethodNotSupported("QueueBuild"); //$NON-NLS-1$
        }

        if (getBuildServerVersion().isV3()) {
            return getBuild2010Helper().queueBuilds(requests, options);
        } else {
            return getBuildQueueService().queueBuilds(requests, options).getQueuedBuilds();
        }
    }

    // / <summary>
    // / Starts the provided queued builds if they are in a paused definition
    // queue and have a status of Queued.
    // / </summary>
    // / <param name="builds">The builds which should be started</param>
    @Override
    public IQueuedBuild[] startQueuedBuildsNow(final IQueuedBuild[] builds) {
        if (builds == null || builds.length == 0) {
            return builds;
        }

        if (getBuildServerVersion().isLessThanV4()) {
            throwMethodNotSupported("StartQueuedBuildsNow"); //$NON-NLS-1$
        }

        final int[] ids = new int[builds.length];
        for (int i = 0; i < builds.length; i++) {
            ids[i] = builds[i].getID();
        }

        return startQueuedBuildsNow(ids);
    }

    // / <summary>
    // / Starts the queued builds withd the provided IDs if they are in a paused
    // definition queue and have a status of Queued.
    // / </summary>
    // / <param name="builds">The builds which should be started</param>
    @Override
    public IQueuedBuild[] startQueuedBuildsNow(final int[] ids) {
        if (ids == null || ids.length == 0) {
            return new IQueuedBuild[0];
        }

        if (getBuildServerVersion().isLessThanV4()) {
            throwMethodNotSupported("StartQueuedBuildsNow"); //$NON-NLS-1$
        }

        return getBuildQueueService().startBuildsNow(ids).getQueuedBuilds();
    }

    // / <summary>
    // / Stops the provided builds. If a build is not currently in progress then
    // no attempt is made to stop the
    // / build and no exception is thrown. Builds which no longer exist on the
    // server cause an exception to be thrown.
    // / </summary>
    // / <param name="builds"></param>
    @Override
    public void stopBuilds(final IBuildDetail[] builds) {
        // Ignore null/empty inputs
        if (builds == null || builds.length == 0) {
            return;
        }

        final String[] uriList = new String[builds.length];

        for (int i = 0; i < builds.length; i++) {
            uriList[i] = builds[i].getURI();
        }

        stopBuilds(uriList);
    }

    // / <summary>
    // / Stops all builds with the provided URIs. For builds which are not in
    // progress the URIs are silently ignored
    // / by the server. URIs which do not exist on the server will cause an
    // exception to be thrown.
    // / </summary>
    // / <param name="uris"></param>
    @Override
    public void stopBuilds(final String[] uris) {
        // Ignore null/empty inputs
        if (uris == null || uris.length == 0) {
            return;
        }

        if (getBuildServerVersion().isV2()) {
            getBuild2008Helper().stopBuilds(uris);
        } else if (getBuildServerVersion().isV3()) {
            getBuild2010Helper().stopBuilds(uris);
        } else {
            getBuildService().stopBuilds(uris);
        }
    }

    // / <summary>
    // / Cancels the provided queued builds if they are currently in the active
    // or postponed states.
    // / </summary>
    // / <param name="builds">The builds which should be canceled</param>
    @Override
    public void cancelBuilds(final IQueuedBuild[] queuedBuilds) {
        // Ignore null/emtpy inputs.
        if (queuedBuilds == null || queuedBuilds.length == 0) {
            return;
        }

        final int[] ids = new int[queuedBuilds.length];

        for (int i = 0; i < queuedBuilds.length; i++) {
            ids[i] = queuedBuilds[i].getID();
        }

        cancelBuilds(ids);
    }

    // / <summary>
    // / Cancels queued builds with the provided ids if they are currently in
    // the active or postponed states.
    // / </summary>
    // / <param name="ids">The ids of queued builds which should be
    // canceled</param>
    @Override
    public void cancelBuilds(final int[] ids) {
        // Ignore null/emtpy inputs.
        if (ids == null || ids.length == 0) {
            return;
        }

        if (getBuildServerVersion().isV2()) {
            getBuild2008Helper().cancelBuilds(ids);
        } else if (getBuildServerVersion().isV3()) {
            getBuild2010Helper().cancelBuilds(ids);
        } else {
            getBuildQueueService().cancelBuilds(ids);
        }
    }

    @Override
    public void addBuildQuality(final String teamProject, final String quality) {
        addBuildQuality(teamProject, new String[] {
            quality
        });
    }

    @Override
    public void addBuildQuality(final String teamProject, final String[] qualities) {
        if (getBuildServerVersion().isV2()) {
            getBuild2008Helper().addBuildQualities(teamProject, qualities);
        } else if (getBuildServerVersion().isV3()) {
            getBuild2010Helper().addBuildQualities(teamProject, qualities);
        } else {
            getBuildService().addBuildQualities(teamProject, qualities);
        }
    }

    @Override
    public void deleteBuildQuality(final String teamProject, final String quality) {
        deleteBuildQuality(teamProject, new String[] {
            quality
        });
    }

    @Override
    public void deleteBuildQuality(final String teamProject, final String[] qualities) {
        if (getBuildServerVersion().isV2()) {
            getBuild2008Helper().deleteBuildQualities(teamProject, qualities);
        } else if (getBuildServerVersion().isV3()) {
            getBuild2010Helper().deleteBuildQualities(teamProject, qualities);
        } else {
            getBuildService().deleteBuildQualities(teamProject, qualities);
        }
    }

    @Override
    public String[] getBuildQualities(final String teamProject) {
        if (getBuildServerVersion().isV2()) {
            return getBuild2008Helper().getBuildQualities(teamProject);
        } else if (getBuildServerVersion().isV3()) {
            return getBuild2010Helper().getBuildQualities(teamProject);
        } else {
            return getBuildService().getBuildQualities(teamProject);
        }
    }

    @Override
    public IProcessTemplate createProcessTemplate(final String teamProject, final String serverPath) {
        return new ProcessTemplate(this, teamProject, serverPath);
    }

    @Override
    public IProcessTemplate[] queryProcessTemplates(final String teamProject) {
        return queryProcessTemplates(teamProject, ALL_PROCESS_TEMPLATE_TYPES);
    }

    @Override
    public IProcessTemplate[] queryProcessTemplates(final String teamProject, final ProcessTemplateType[] types) {
        ProcessTemplate[] getResult;
        if (getBuildServerVersion().isV3()) {
            getResult = getBuild2010Helper().queryProcessTemplates(teamProject, types);
        } else {
            getResult = getBuildService().queryProcessTemplates(teamProject, types);
        }

        return getResult;
    }

    @Override
    public IProcessTemplate[] saveProcessTemplates(final IProcessTemplate[] processTemplates) {
        final List<ProcessTemplate> templatesToAdd = new ArrayList<ProcessTemplate>();
        final List<ProcessTemplate> templatesToUpdate = new ArrayList<ProcessTemplate>();

        // Return an array (a) of the same size as the input array, and (b) with
        // the "same" objects
        // at the same indices. For adds, all we need to do is map the index in
        // templatesToAdd
        // to the index in the input array...
        final Map<Integer, Integer> addIndices = new HashMap<Integer, Integer>();

        // ...for updates, we need the index to index mapping, and also a
        // Dictionary mapping Ids
        // to the returned IProcessTemplates, since the return value of
        // BuildService.UpdateProcessTemplates
        // will not necessarily be the same length as the input.
        final Map<Integer, Integer> updateIndices = new HashMap<Integer, Integer>();
        final Map<Integer, IProcessTemplate> updatedTemplates = new HashMap<Integer, IProcessTemplate>();

        final IProcessTemplate[] result = new IProcessTemplate[processTemplates.length];

        // Sort templates into Adds and Updates.
        for (int i = 0; i < processTemplates.length; i++) {
            final ProcessTemplate template = (ProcessTemplate) processTemplates[i];

            template.prepareToSave();

            if (template.getID() < 0) {
                templatesToAdd.add(template);
                addIndices.put(templatesToAdd.size() - 1, i);
            } else {
                templatesToUpdate.add(template);
                updateIndices.put(templatesToUpdate.size() - 1, i);
            }
        }

        // Add all the templates-to-add and update their Ids
        if (templatesToAdd.size() > 0) {
            ProcessTemplate[] addResult;
            final ProcessTemplate[] toAdd = templatesToAdd.toArray(new ProcessTemplate[templatesToAdd.size()]);

            if (getBuildServerVersion().isV3()) {
                addResult = getBuild2010Helper().addProcessTemplates(toAdd);
            } else {
                addResult = getBuildService().addProcessTemplates(toAdd);
            }

            for (int i = 0; i < addResult.length; i++) {
                templatesToAdd.get(i).setID(addResult[i].getID());
                templatesToAdd.get(i).setParameters(addResult[i].getParameters());

                // Put the add in the appropriate spot in the result array.
                result[addIndices.get(i)] = addResult[i];
            }
        }

        // Update all the definitions-to-update.
        if (templatesToUpdate.size() > 0) {
            ProcessTemplate[] updateResult;
            final ProcessTemplate[] toUpdate = templatesToUpdate.toArray(new ProcessTemplate[templatesToUpdate.size()]);

            if (getBuildServerVersion().isV3()) {
                updateResult = getBuild2010Helper().updateProcessTemplates(toUpdate);
            } else {
                updateResult = getBuildService().updateProcessTemplates(toUpdate);
            }

            if (templatesToAdd.size() == 0) {
                // Don't waste time trying to sort the udpates if we had no
                // adds.
                return updateResult;
            } else {
                // Put the updates in the appropriate spot(s) in the result
                // array.
                for (final ProcessTemplate updatedTemplate : updateResult) {
                    updatedTemplates.put(updatedTemplate.getID(), updatedTemplate);
                }

                for (int i = 0; i < templatesToUpdate.size(); i++) {
                    result[updateIndices.get(i)] =
                        updatedTemplates.get(((ProcessTemplate) processTemplates[updateIndices.get(i)]).getID());
                }
            }
        }

        return result;
    }

    @Override
    public void deleteProcessTemplates(final IProcessTemplate[] processTemplates) {
        if (processTemplates != null && processTemplates.length > 0) {
            final int[] templateIds = new int[processTemplates.length];
            for (int i = 0; i < processTemplates.length; i++) {
                templateIds[i] = ((ProcessTemplate) processTemplates[i]).getID();
            }

            if (getBuildServerVersion().isV3()) {
                getBuild2010Helper().deleteProcessTemplates(templateIds);
            } else {
                getBuildService().deleteProcessTemplates(templateIds);
            }

            for (final IProcessTemplate template : processTemplates) {
                ((ProcessTemplate) template).setID(-1);
            }
        }
    }

    @Override
    public String getDisplayText(final Object value) {
        return BuildEnumerationHelper.getDisplayText(value);
    }

    @Override
    @SuppressWarnings("rawtypes")
    public String[] getDisplayTextValues(final Class enumType) {
        return BuildEnumerationHelper.getDisplayTextValues(enumType);
    }

    @Override
    @SuppressWarnings("rawtypes")
    public Object getEnumValue(final Class enumType, final String displayText, final Object defaultValue) {
        if (Agent2008Status.class.equals(enumType)) {
            if (Messages.getString("BuildClient.Agent2008StatusEnabled").equals(displayText)) //$NON-NLS-1$
            {
                return Agent2008Status.ENABLED;
            }
            if (Messages.getString("BuildClient.Agent2008StatusDisabled").equals(displayText)) //$NON-NLS-1$
            {
                return Agent2008Status.DISABLED;
            }
            if (Messages.getString("BuildClient.Agent2008StatusUnreachable").equals(displayText)) //$NON-NLS-1$
            {
                return Agent2008Status.UNREACHABLE;
            }
            if (Messages.getString("BuildClient.Agent2008StatusInitializing").equals(displayText)) //$NON-NLS-1$
            {
                return Agent2008Status.INITIALIZING;
            }
            return defaultValue;
        }

        return defaultValue;
    }

    public Build2008Helper getBuild2008Helper() {
        if (build2008Helper == null) {
            build2008Helper = new Build2008Helper(this);
        }
        return build2008Helper;
    }

    public Build2010Helper getBuild2010Helper() {
        if (build2010Helper == null) {
            build2010Helper = new Build2010Helper(this);
        }
        return build2010Helper;
    }

    public BuildWebService4 getBuildService() {
        if (buildService4 == null) {
            if (!getBuildServerVersion().isV4()) {
                throw new UnsupportedOperationException("Tried to get non V4 build web service"); //$NON-NLS-1$
            }

            buildService4 = new BuildWebService4(tfs);
        }

        return buildService4;
    }

    public BuildQueueWebService4 getBuildQueueService() {
        if (buildQueueService4 == null) {
            if (!getBuildServerVersion().isV4()) {
                throw new UnsupportedOperationException("Tried to get non V4 build queue web service"); //$NON-NLS-1$
            }

            buildQueueService4 = new BuildQueueWebService4(tfs);
        }

        return buildQueueService4;
    }

    public AdministrationWebService4 getAdministrationService() {
        if (buildAdminService4 == null) {
            if (!getBuildServerVersion().isV4()) {
                throw new UnsupportedOperationException("Tried to get non V4 build admin web service"); //$NON-NLS-1$
            }

            buildAdminService4 = new AdministrationWebService4(tfs);
        }
        return buildAdminService4;
    }

    // / <summary>
    // / Returns an array of Uris associated with the specified builds.
    // / </summary>
    private static String[] getUrisForBuilds(final IBuildDetail[] builds) {
        if (builds == null) {
            return null;
        }

        final String[] uriList = new String[builds.length];

        for (int i = 0; i < builds.length; i++) {
            uriList[i] = builds[i].getURI();
        }
        return uriList;
    }

    private void throwMethodNotSupported(final String method) throws NotSupportedException {
        final String format = Messages.getString("BuildServer2012.MethodNotSupportedFormat"); //$NON-NLS-1$
        throw new NotSupportedException(MessageFormat.format(format, method, "IBuildServer")); //$NON-NLS-1$
    }

    private void throwOperationNotSupported(final String operation) throws NotSupportedException {
        final String format = Messages.getString("BuildServer2012.OperationNotSupportedFormat"); //$NON-NLS-1$
        throw new NotSupportedException(MessageFormat.format(format, operation, "IBuildServer")); //$NON-NLS-1$
    }

    private String getDomainUserName() {
        return IdentityHelper.getDomainUserName(getConnection().getAuthorizedIdentity());
    }
}
