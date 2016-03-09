// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.build;

import com.microsoft.tfs.core.TFSTeamProjectCollection;
import com.microsoft.tfs.core.clients.build.flags.BuildServerVersion;
import com.microsoft.tfs.core.clients.build.flags.DeleteOptions;
import com.microsoft.tfs.core.clients.build.flags.QueryDeletedOption;
import com.microsoft.tfs.core.clients.build.flags.QueryOptions;
import com.microsoft.tfs.core.clients.build.flags.QueueOptions;
import com.microsoft.tfs.core.clients.build.flags.QueuedBuildRetryOption;
import com.microsoft.tfs.core.clients.build.soapextensions.ContinuousIntegrationType;
import com.microsoft.tfs.core.clients.build.soapextensions.DefinitionTriggerType;
import com.microsoft.tfs.core.clients.build.soapextensions.ProcessTemplateType;
import com.microsoft.tfs.util.GUID;

public interface IBuildServer {
    // / <summary>
    // / The version (V1 or V2) of the build server.
    // / </summary>
    public BuildServerVersion getBuildServerVersion();

    // / <summary>
    // / The comment string which signals the continuous integration system to
    // ignore a
    // / check-in. Any check-in with this string at any point in the comment
    // will be
    // / ignored (i.e. will not trigger a new build).
    // / </summary>
    public String getNoCICheckInComment();

    // / <summary>
    // / The Team Project Collection object that created this IBuildServer.
    // / </summary>
    public TFSTeamProjectCollection getConnection();

    // / <summary>
    // / Create a new build definition owned by the build server.
    // / </summary>
    // / <param name="teamProject">The team project under which the build
    // definition will be created.</param>
    // / <returns>The new build definition.</returns>
    public IBuildDefinition createBuildDefinition(String teamProject);

    // / <summary>
    // / Creates a new build detail specification that can be used to query
    // builds for a particular team project.
    // / </summary>
    // / <param name="teamProject">The team project for which builds can be
    // queried.</param>
    // / <returns>The new build detail specification.</returns>
    public IBuildDetailSpec createBuildDetailSpec(String teamProject);

    // / <summary>
    // / Creates a new build detail specification that can be used to query
    // builds for a particular team project and definition.
    // / </summary>
    // / <param name="teamProject">The team project for which builds can be
    // queried.</param>
    // / <param name="definitionName">The build definition for which builds can
    // be queried. Wildcards supported.</param>
    // / <returns>The new build detail specification.</returns>
    public IBuildDetailSpec createBuildDetailSpec(String teamProject, String definitionName);

    // / <summary>
    // / Creates a new build detail specification that can be used to query
    // builds for a particular team project and definition.
    // / </summary>
    // / <param name="definitionSpec">A build definition specification that
    // includes the team project and definition for which builds can be
    // queried.</param>
    // / <returns>The new build detail specification.</returns>
    public IBuildDetailSpec createBuildDetailSpec(IBuildDefinitionSpec definitionSpec);

    // / <summary>
    // / Creates a new build detail specification that can be used to query
    // builds for a particular definition.
    // / </summary>
    // / <param name="definition">The build definition for which builds can be
    // queried.</param>
    // / <returns>The new build detail specification.</returns>
    public IBuildDetailSpec createBuildDetailSpec(IBuildDefinition definition);

    // / <summary>
    // / Creates a new build agent specification that can be used to query build
    // agents.
    // / </summary>
    // / <returns>The new build agent specification.</returns>
    public IBuildAgentSpec createBuildAgentSpec();

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
    public IBuildAgentSpec createBuildAgentSpec(String name, String computer, String[] tags);

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
    public IBuildAgentSpec createBuildAgentSpec(
        String name,
        String computer,
        String[] propertyNameFilters,
        String[] tags);

    // / <summary>
    // / Creates a new build agent specification that can be used to query build
    // agents.
    // / </summary>
    // / <param name="agent">The agent for which a specification is
    // created.</param>
    // / <returns>The new build agent specification.</returns>
    public IBuildAgentSpec createBuildAgentSpec(IBuildAgent agent);

    // / <summary>
    // / Creates a new build controller specification that can be used to query
    // build controllers.
    // / </summary>
    // / <returns>The new build controller specification.</returns>
    public IBuildControllerSpec createBuildControllerSpec();

    // / <summary>
    // / Creates a new build controller specification that can be used to query
    // build controllers.
    // / </summary>
    // / <param name="name">The controller name to query - supports
    // wildcards.</param>
    // / <param name="computer">The computer to query - null/empty means
    // "don't care".</param>
    // / <returns>The new build controller specification.</returns>
    public IBuildControllerSpec createBuildControllerSpec(String name, String computer);

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
    public IBuildControllerSpec createBuildControllerSpec(
        String name,
        String computer,
        String[] propertyNameFilters,
        boolean includeAgents);

    // / <summary>
    // / Creates a new build definition specification that can be used to query
    // build definitions.
    // / </summary>
    // / <param name="definition">The definition for which a specification is
    // created.</param>
    // / <returns>The new build definition specification.</returns>
    public IBuildDefinitionSpec createBuildDefinitionSpec(IBuildDefinition definition);

    // / <summary>
    // / Creates a new build definition specification that can be used to query
    // build definitions.
    // / </summary>
    // / <param name="teamProject">The team project for which definitions can be
    // queried.</param>
    // / <returns>The new build definition specification.</returns>
    public IBuildDefinitionSpec createBuildDefinitionSpec(String teamProject);

    // / <summary>
    // / Creates a new build definition specification that can be used to query
    // build definitions.
    // / </summary>
    // / <param name="teamProject">The team project for which definitions can be
    // queried.</param>
    // / <param name="definitionName">The definition name to query - supports
    // wildcards.</param>
    // / <returns>The new build definition specification.</returns>
    public IBuildDefinitionSpec createBuildDefinitionSpec(String teamProject, String definitionName);

    // / <summary>
    // / Creates a new build definition specification that can be used to query
    // build definitions.
    // / </summary>
    // / <param name="teamProject">The team project for which definitions can be
    // queried.</param>
    // / <param name="definitionName">The definition name to query - supports
    // wildcards.</param>
    // / <param name="propertyNameFilters">Property name filters</param>
    // / <returns>The new build definition specification.</returns>
    public IBuildDefinitionSpec createBuildDefinitionSpec(
        String teamProject,
        String definitionName,
        String[] propertyNameFilters);

    // / <summary>
    // / Creates a new build queue specification that can be used to query
    // queued builds.
    // / </summary>
    // / <param name="teamProject">The team project for which queued builds can
    // be queried.</param>
    // / <returns>The new build queue specification.</returns>
    public IQueuedBuildSpec createBuildQueueSpec(String teamProject);

    // / <summary>
    // / Creates a new build queue specification that can be used to query
    // queued builds.
    // / </summary>
    // / <param name="teamProject">The team project for which queued builds can
    // be queried.</param>
    // / <param name="definitionName">The definition name to query - supports
    // wildcards.</param>
    // / <returns>The new build queue specification.</returns>
    public IQueuedBuildSpec createBuildQueueSpec(String teamProject, String definitionName);

    // / <summary>
    // / Creates a new build queue specification that can be used to query
    // queued builds.
    // / </summary>
    // / <param name="definitionUris">The definitions to query.</param>
    // / <returns>A new build queue specification.</returns>
    public IQueuedBuildSpec createBuildQueueSpec(String[] definitionUris);

    // / <summary>
    // / Creates a new build request for the specified build definition.
    // / </summary>
    // / <param name="buildDefinitionUri">The build definition</param>
    // / <returns>A new build request</returns>
    public IBuildRequest createBuildRequest(String buildDefinitionUri);

    // / <summary>
    // / Creates a new build request for the specified build definition and
    // controller.
    // / </summary>
    // / <param name="buildDefinitionUri">The build definiton</param>
    // / <param name="buildControllerUri">The build controller</param>
    // / <returns>A new build request</returns>
    public IBuildRequest createBuildRequest(String buildDefinitionUri, String buildControllerUri);

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
    public IBuildServiceHost createBuildServiceHost(String name, String scheme, String host, int port);

    // / <summary>
    // / Creates a new service host with the specified name and base URL.
    // / </summary>
    // / <param name="name">The name of the service host</param>
    // / <param name="baseUrl">The base URL of the service host.</param>
    // / <returns>The new service host.</returns>
    public IBuildServiceHost createBuildServiceHost(String name, String baseUrl);

    // / <summary>
    // / Gets a list of all build definitions which are affected by the provided
    // TFS Version Control
    // / server paths.
    // / </summary>
    // / <param name="serverItems">An array of TFS Version Control server
    // paths</param>
    // / <returns>An array of build definitions</returns>
    public IBuildDefinition[] getAffectedBuildDefinitions(String[] serverItems);

    // / <summary>
    // / Gets a list of all build definitions which are affected by the provided
    // TFS Version Control
    // / server paths and are configured with a trigger supplied in the filter.
    // / </summary>
    // / <param name="serverItems">An array of TFS Version Control server
    // paths</param>
    // / <param name="continuousIntegrationType">The type of trigger(s) which
    // should be queried</param>
    // / <returns>An array of build definitions</returns>
    // [Obsolete("This method has been deprecated. Use IBuildDefinition[]
    // IBuildServer.GetAffectedBuildDefinitions(String[], DefinitionTriggerType)
    // instead.",
    // false)]
    public IBuildDefinition[] getAffectedBuildDefinitions(
        String[] serverItems,
        ContinuousIntegrationType continuousIntegrationType);

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
    public IBuildDefinition[] getAffectedBuildDefinitions(String[] serverItems, DefinitionTriggerType triggerType);

    // / <summary>
    // / Gets a single build with no agent, definition, or information nodes.
    // / </summary>
    // / <param name="buildUri">The URI of the build.</param>
    // / <returns>The build with the given URI, without any details.</returns>
    public IBuildDetail getMinimalBuildDetails(String buildUri);

    // / <summary>
    // / Gets a single build with agent, definition, and all information nodes.
    // / </summary>
    // / <param name="buildUri">The URI of the build.</param>
    // / <returns>The build with the given Uri, with all details.</returns>
    public IBuildDetail getAllBuildDetails(String buildUri);

    // / <summary>
    // / Gets a single build with all information types and query options.
    // / </summary>
    // / <param name="buildUri">The URI of the build.</param>
    // / <returns>The build with the given URI.</returns>
    public IBuildDetail getBuild(String buildUri);

    // / <summary>
    // / Gets a single build.
    // / </summary>
    // / <param name="buildId">The URI of the build.</param>
    // / <param name="informationTypes">The information types which should be
    // retrieved. Valid types include "*", meaning all types, and
    // / the members of
    // Microsoft.TeamFoundation.Build.Common.InformationTypes.</param>
    // / <param name="queryOptions">The query options.</param>
    // / <returns>The build with the given URI.</returns>
    public IBuildDetail getBuild(String buildUri, String[] informationTypes, QueryOptions queryOptions);

    // / <summary>
    // / Gets a single build by Uri.
    // / </summary>
    // / <param name="buildUri">The Uri of the build to get.</param>
    // / <param name="informationTypes">The information types which should be
    // retrieved.</param>
    // / <param name="queryOptions">The query options.</param>
    // / <param name="queryDeletedOption">The deleted options.</param>
    // / <returns>The build with the given Uri.</returns>
    public IBuildDetail getBuild(
        String buildUri,
        String[] informationTypes,
        QueryOptions queryOptions,
        QueryDeletedOption queryDeletedOption);

    // / <summary>
    // / Gets a single build.
    // / </summary>
    // / <param name="buildDefinitionSpec">The specification of the build
    // definition that owns the build.</param>
    // / <param name="buildNumber">The number of the build.</param>
    // / <param name="informationTypes">The information types which should be
    // retrieved. Valid types include "*", meaning all types, and
    // / the members of
    // Microsoft.TeamFoundation.Build.Common.InformationTypes.</param>
    // / <param name="queryOptions">The query options.</param>
    // / <returns>The build with the given build definition path and
    // number.</returns>
    public IBuildDetail getBuild(
        IBuildDefinitionSpec buildDefinitionSpec,
        String buildNumber,
        String[] informationTypes,
        QueryOptions queryOptions);

    // / <summary>
    // / Gets the builds that match the given URIs.
    // / </summary>
    // / <param name="buildIds">The build URIs.</param>
    // / <param name="informationTypes">The information types which should be
    // retrieved. Valid types include "*", meaning all types, and
    // / the members of
    // Microsoft.TeamFoundation.Build.Common.InformationTypes.</param>
    // / <param name="queryOptions">The query options.</param>
    // / <returns>The builds matching the given URIs. For unmatched URIs,
    // corresponding
    // / indices will be null.</returns>
    public IBuildDetail[] queryBuildsByURI(String[] buildUris, String[] informationTypes, QueryOptions queryOptions);

    // / <summary>
    // / Gets the builds that match the given URIs.
    // / </summary>
    // / <param name="buildIds">The build URIs.</param>
    // / <param name="informationTypes">The information types which should be
    // retrieved. Valid types include "*", meaning all types, and
    // / the members of
    // Microsoft.TeamFoundation.Build.Common.InformationTypes.</param>
    // / <param name="queryOptions">The query options.</param>
    // / <param name="queryDeletedOption">Specify whether to include deleted
    // builds in the query.</param>
    // / <returns>The builds matching the given URIs. For unmatched URIs,
    // corresponding
    // / indices will be null.</returns>
    public IBuildDetail[] queryBuildsByURI(
        String[] buildUris,
        String[] informationTypes,
        QueryOptions queryOptions,
        QueryDeletedOption queryDeletedOption);

    // / <summary>
    // / Gets all of the builds for a team project.
    // / </summary>
    // / <param name="teamProject">The team project for which builds are
    // retrieved.</param>
    // / <returns>The builds for the given team project.</returns>
    public IBuildDetail[] queryBuilds(String teamProject);

    // / <summary>
    // / Gets all of the builds for a team project and definition.
    // / </summary>
    // / <param name="teamProject">The team project for which builds are
    // retrieved.</param>
    // / <param name="definitionName">The definition for which builds are
    // retrieved.</param>
    // / <returns>The builds for the given team project and
    // definition.</returns>
    public IBuildDetail[] queryBuilds(String teamProject, String definitionName);

    // / <summary>
    // / Gets all of the builds for a build definition specification.
    // / </summary>
    // / <param name="definitionSpec">The build definition specification for
    // which builds are retrieved.</param>
    // / <returns>The builds for the given build definition
    // specification.</returns>
    public IBuildDetail[] queryBuilds(IBuildDefinitionSpec definitionSpec);

    // / <summary>
    // / Gets all of the builds for a build definition.
    // / </summary>
    // / <param name="definition">The build definition for which builds are
    // retrieved.</param>
    // / <returns>The builds for the given build definition.</returns>
    public IBuildDetail[] queryBuilds(IBuildDefinition definition);

    // / <summary>
    // / Gets a single build query result for a given build specification.
    // / </summary>
    // / <param name="buildDetailSpec">The build specification.</param>
    // / <returns>The build query result for the given specification.</returns>
    public IBuildQueryResult queryBuilds(IBuildDetailSpec buildDetailSpec);

    // / <summary>
    // / Gets the build query results for a given array of build specifications.
    // / </summary>
    // / <param name="buildDetailSpec">The build specifications.</param>
    // / <returns>The build query results for the given
    // specifications.</returns>
    public IBuildQueryResult[] queryBuilds(IBuildDetailSpec[] buildDetailSpecs);

    // / <summary>
    // / Deletes builds from the server. All parts of the build are deleted.
    // / </summary>
    // / <param name="builds">The builds to be deleted.</param>
    public IBuildDeletionResult[] deleteBuilds(IBuildDetail[] builds);

    // / <summary>
    // / Deletes builds from the server.
    // / </summary>
    // / <param name="builds">The builds to be deleted.</param>
    // / <param name="options">The parts of the build to delete.</param>
    public IBuildDeletionResult[] deleteBuilds(IBuildDetail[] builds, DeleteOptions options);

    // / <summary>
    // / Deletes builds from the server. All parts of the build are deleted.
    // / </summary>
    // / <param name="buildUris">The URIs of the builds to be deleted.</param>
    public IBuildDeletionResult[] deleteBuilds(String[] buildUris);

    // / <summary>
    // / Deletes builds from the server.
    // / </summary>
    // / <param name="buildUris">The URIs of the builds to be deleted.</param>
    // / <param name="options">The parts of the build to delete.</param>
    public IBuildDeletionResult[] deleteBuilds(String[] buildUris, DeleteOptions options);

    // / <summary>
    // / Destroys builds from the server.
    // / </summary>
    // / <param name="builds">The builds to be deleted.</param>
    // / <remarks>
    // / Unlike DeleteBuilds, this method will remove the build records from
    // / the database completely. This method will not delete build artifacts
    // / such as the drop location and test results.
    // / </remarks>
    public void destroyBuilds(IBuildDetail[] builds);

    // / <summary>
    // / Destroys builds from the server.
    // / </summary>
    // / <param name="buildUris">The URIs of the builds to be deleted.</param>
    // / <remarks>
    // / Unlike DeleteBuilds, this method will remove the build records from
    // / the database completely. This method will not delete build artifacts
    // / such as the drop location and test results.
    // / </remarks>
    public void destroyBuilds(String[] buildUris);

    // / <summary>
    // / Saves any changes made to the builds to the server.
    // / </summary>
    // / <param name="builds">The builds to be saved.</param>
    // / <returns>The saved builds.</returns>
    public IBuildDetail[] saveBuilds(IBuildDetail[] builds);

    // / <summary>
    // / Gets a single build definition.
    // / </summary>
    // / <param name="buildDefinitionUri">The URI of the build
    // definition.</param>
    // / <returns>The build definition with the given URI.</returns>
    public IBuildDefinition getBuildDefinition(String buildDefinitionUri);

    // / <summary>
    // / Gets a single build definition using the specified options to control
    // the amount of data retrieved.
    // / </summary>
    // / <param name="buildDefinitionUri">The URI fo the build
    // definition</param>
    // / <param name="options">The options to use when querying for data</param>
    // / <returns>The build definition with the given URI.</returns>
    public IBuildDefinition getBuildDefinition(String buildDefinitionUri, QueryOptions options);

    // / <summary>
    // / Gets a single build definition using the specified options to control
    // the amount of data retrieved.
    // / </summary>
    // / <param name="buildDefinitionUri">The URI fo the build
    // definition</param>
    // / <param name="propertyNameFilters">Property names to query</param>
    // / <param name="options">The options to use when querying for data</param>
    // / <returns>The build definition with the given URI.</returns>
    public IBuildDefinition getBuildDefinition(
        String buildDefinitionUri,
        String[] propertyNameFilters,
        QueryOptions options);

    // / <summary>
    // / Gets a single build definition.
    // / </summary>
    // / <param name="teamProject">The team project that owns the build
    // definition.</param>
    // / <param name="name">The name of the build definition.</param>
    // / <returns>The build definition with the given name in the given team
    // project.</returns>
    public IBuildDefinition getBuildDefinition(String teamProject, String name);

    // / <summary>
    // / Gets a single build definition using the specified options to control
    // the amount of data retrieved.
    // / </summary>
    // / <param name="teamProject">The team project that owns the build
    // definition</param>
    // / <param name="name">The name of the build definition</param>
    // / <param name="options">The options to use when querying for data</param>
    // / <returns>The build definition with the given name in the given team
    // project</returns>
    public IBuildDefinition getBuildDefinition(String teamProject, String name, QueryOptions options);

    // / <summary>
    // / Gets the build definitions that match the given URIs.
    // / </summary>
    // / <param name="buildDefinitionUris">The build definition URIs.</param>
    // / <returns>The build definitions. For unmatched URIs, corresponding
    // indices will
    // / be null.</returns>
    public IBuildDefinition[] queryBuildDefinitionsByURI(String[] buildDefinitionUris);

    // / <summary>
    // / Gets the build definitions that match the given URIs. The specified
    // query options determine the amount
    // / of data that is retrieved in the query.
    // / </summary>
    // / <param name="buildDefinitionUris">The build definition URIs.</param>
    // / <param name="options">The options to use when querying for data</param>
    // / <returns>The build definitions. For unmatched URIs, corresponding
    // indices will be null.</returns>
    public IBuildDefinition[] queryBuildDefinitionsByURI(String[] buildDefinitionUris, QueryOptions options);

    // / <summary>
    // / Gets the build definitions that match the given URIs. The specified
    // query options determine the amount
    // / of data that is retrieved in the query.
    // / </summary>
    // / <param name="buildDefinitionUris">The build definition URIs.</param>
    // / <param name="propertyNameFilters">Property names to query</param>
    // / <param name="options">The options to use when querying for data</param>
    // / <returns>The build definitions. For unmatched URIs, corresponding
    // indices will be null.</returns>
    public IBuildDefinition[] queryBuildDefinitionsByURI(
        String[] buildDefinitionUris,
        String[] propertyNameFilters,
        QueryOptions options);

    // / <summary>
    // / Gets the build definitions for the given team project.
    // / </summary>
    // / <param name="teamProject">The team project for which build definitions
    // are retrieved.</param>
    // / <returns>The build definitions for the given team project.</returns>
    public IBuildDefinition[] queryBuildDefinitions(String teamProject);

    // / <summary>
    // / Gets the build definitions for the given team project. The specified
    // query options determine the amount
    // / of data that is retrieved in the query.
    // / </summary>
    // / <param name="teamProject">The team project for which build definitions
    // are retrieved</param>
    // / <param name="options">The options to use when querying for data</param>
    // / <returns>The build definitions for the given team project.</returns>
    public IBuildDefinition[] queryBuildDefinitions(String teamProject, QueryOptions options);

    // / <summary>
    // / Gets a single build definition query result for a given build
    // definition specification.
    // / </summary>
    // / <param name="buildDefinitionSpec">The build definition
    // specification.</param>
    // / <returns>The build definition query result for the given
    // specification.</returns>
    public IBuildDefinitionQueryResult queryBuildDefinitions(IBuildDefinitionSpec buildDefinitionSpec);

    // / <summary>
    // / Gets the build definition query results for a given array of build
    // definition
    // / specifications.
    // / </summary>
    // / <param name="teamProject">The build definition specifications.</param>
    // / <returns>The build definition query results for the given
    // specifications</returns>
    public IBuildDefinitionQueryResult[] queryBuildDefinitions(IBuildDefinitionSpec[] buildDefinitionSpecs);

    // / <summary>
    // / Deletes build definitions from the server.
    // / </summary>
    // / <param name="definitions">The definitions to be deleted.</param>
    public void deleteBuildDefinitions(IBuildDefinition[] definitions);

    // / <summary>
    // / Deletes build definitions from the server.
    // / </summary>
    // / <param name="definitionUris">The URIs of the definitions to be
    // deleted.</param>
    public void deleteBuildDefinitions(String[] definitionUris);

    // / <summary>
    // / Saves any changes made to the build definitions to the server.
    // / </summary>
    // / <param name="definitions">The definitions to be saved.</param>
    // / <returns>The saved definitions.</returns>
    public IBuildDefinition[] saveBuildDefinitions(IBuildDefinition[] definitions);

    // / <summary>
    // / Deletes the service host and all associated services from the server.
    // / </summary>
    // / <param name="serviceHostUri">The URI of the service host to
    // delete</param>
    public void deleteBuildServiceHost(String serviceHostUri);

    // / <summary>
    // / Gets the service host that matches the given name.
    // / </summary>
    // / <param name="serviceHostName">The name of the service host</param>
    // / <returns>The build service host for the given name</returns>
    public IBuildServiceHost getBuildServiceHostByName(String serviceHostName);

    // / <summary>
    // / Gets the service host that matches the given URI.
    // / </summary>
    // / <param name="buildServiceHostUri">The URI of the build service
    // host</param>
    // / <returns>The build service host for the given URI</returns>
    public IBuildServiceHost getBuildServiceHostByURI(String buildServiceHostUri);

    // / <summary>
    // / Queries for the list of all build service hosts that are hosted on the
    // specified computers.
    // / </summary>
    // / <param name="serviceHostName">The name of the service host to match
    // (may contain wild cards)</param>
    // / <returns>A list of build service hosts that are hosted by computers
    // matching the input</returns>
    public IBuildServiceHost[] queryBuildServiceHosts(String serviceHostName);

    // / <summary>
    // / Gets the build service hosts that match the given URIs.
    // / </summary>
    // / <param name="buildServiceHostUris">The URIs of the build service
    // hosts</param>
    // / <returns>The build service hosts for the given URIs</returns>
    public IBuildServiceHost[] queryBuildServiceHostsByURI(String[] buildServiceHostUris);

    // / <summary>
    // / Saves the service host changes to the server.
    // / </summary>
    // / <param name="serviceHost">The service host which should be
    // saved</param>
    public void saveBuildServiceHost(IBuildServiceHost serviceHost);

    // / <summary>
    // / Tells AT to test connection for build machine resources and updates
    // resources statuses/status messages
    // / </summary>
    // / <param name="host">BuildServiceHost to be tested.</param>
    public void testConnectionsForBuildMachine(IBuildServiceHost host);

    // / <summary>
    // / Tells AT to test connection for build agent and updates the agent
    // status/status message
    // / </summary>
    // / <param name="agent">BuildAgent to be tested.</param>
    public void testConnectionForBuildAgent(IBuildAgent agent);

    // / <summary>
    // / Tells AT to test connection for build controller and updates the
    // controller status/status message
    // / </summary>
    // / <param name="controller">BuildController to be tested.</param>
    public void testConnectionForBuildController(IBuildController controller);

    // / <summary>
    // / Gets a single build agent.
    // / </summary>
    // / <param name="buildAgentUri">The URI of the build agent.</param>
    // / <returns>The build agent with the given URI.</returns>
    public IBuildAgent getBuildAgent(String buildAgentUri);

    // / <summary>
    // / Gets a single build agent.
    // / </summary>
    // / <param name="buildAgentUri">The URI of the build agent.</param>
    // / <param name="propertyNameFilters">The property names to get.</param>
    // / <returns>The build agent with the given URI.</returns>
    public IBuildAgent getBuildAgent(String buildAgentUri, String[] propertyNameFilters);

    // / <summary>
    // / Gets the build agents that match the given URIs.
    // / </summary>
    // / <param name="buildAgentUris">The URIs of the build agents.</param>
    // / <returns>The build agents with the given URIs.</returns>
    public IBuildAgent[] queryBuildAgentsByURI(String[] buildAgentUris);

    // / <summary>
    // / Gets the build agents that match the given URIs.
    // / </summary>
    // / <param name="buildAgentUris">The URIs of the build agents.</param>
    // / <param name="propertyNameFilters">The property names to get.</param>
    // / <returns>The build agents with the given URIs.</returns>
    public IBuildAgent[] queryBuildAgentsByURI(String[] buildAgentUris, String[] propertyNameFilters);

    // / <summary>
    // / Gets a single build agent query result for a given build agent
    // specification.
    // / </summary>
    // / <param name="buildAgentSpec">The build agent specification.</param>
    // / <returns>The build agent query result for the given
    // specification.</returns>
    public IBuildAgentQueryResult queryBuildAgents(IBuildAgentSpec buildAgentSpec);

    // / <summary>
    // / Gets the build agent query results for the given build agent
    // specifications.
    // / </summary>
    // / <param name="teamProject">The build agent specifications.</param>
    // / <returns>The build agent query results for the given
    // specifications.</returns>
    public IBuildAgentQueryResult[] queryBuildAgents(IBuildAgentSpec[] buildAgentSpecs);

    // / <summary>
    // / Deletes build agents from the server.
    // / </summary>
    // / <param name="agents">The agents to be deleted.</param>
    public void deleteBuildAgents(IBuildAgent[] agents);

    // / <summary>
    // / Deletes build agents from the server.
    // / </summary>
    // / <param name="agentUris">The URIs of the agents to be deleted.</param>
    public void deleteBuildAgents(String[] agentUris);

    // / <summary>
    // / Saves any changes made to the build agents to the server.
    // / </summary>
    // / <param name="agents">The agents to be saved.</param>
    // / <returns>The saved build agents.</returns>
    public IBuildAgent[] saveBuildAgents(IBuildAgent[] agents);

    // / <summary>
    // / Gets a single build controller.
    // / </summary>
    // / <param name="buildControllerUri">The URI of the build
    // controller.</param>
    // / <returns>The build controller with the given URI.</returns>
    public IBuildController getBuildController(String buildControllerUri, boolean includeAgents);

    // / <summary>
    // / Gets a single build controller.
    // / </summary>
    // / <param name="buildControllerUri">The URI of the build
    // controller.</param>
    // / <param name="propertyNameFilters">The property names to get.</param>
    // / <returns>The build controller with the given URI.</returns>
    public IBuildController getBuildController(
        String buildControllerUri,
        String[] propertyNameFilters,
        boolean includeAgents);

    // / <summary>
    // / Retrieves a single build controller using the specified display name. A
    // wild card may be specified, but
    // / if more than one controller is matched than an exception is thrown.
    // / </summary>
    // / <param name="name">The display name of the build controller (wild cards
    // allowed)</param>
    // / <returns>The discovered build controller</returns>
    public IBuildController getBuildController(String name);

    // / <summary>
    // / Gets all build controllers and their associated agents.
    // / </summary>
    // / <returns>All of the build controllers.</returns>
    public IBuildController[] queryBuildControllers();

    // / <summary>
    // / Gets all build controllers.
    // / </summary>
    // / <returns>All of the build controllers.</returns>
    public IBuildController[] queryBuildControllers(boolean includeAgents);

    // / <summary>
    // / Gets the build controllers that match the given URIs.
    // / </summary>
    // / <param name="buildControllerUris">The URIs of the build
    // controllers.</param>
    // / <returns>The build controllers with the given URIs.</returns>
    public IBuildController[] queryBuildControllersByURI(String[] buildControllerUris, boolean includeAgents);

    // / <summary>
    // / Gets the build controllers that match the given URIs.
    // / </summary>
    // / <param name="buildControllerUris">The URIs of the build
    // controllers.</param>
    // / <param name="propertyNameFilters">The property names to get.</param>
    // / <returns>The build controllers with the given URIs.</returns>
    public IBuildController[] queryBuildControllersByURI(
        String[] buildControllerUris,
        String[] propertyNameFilters,
        boolean includeAgents);

    // / <summary>
    // / Gets a single build controller query result for a given build
    // controller specification.
    // / </summary>
    // / <param name="buildControllerSpec">The build controller
    // specification.</param>
    // / <returns>The build controller query result for the given
    // specification.</returns>
    public IBuildControllerQueryResult queryBuildControllers(IBuildControllerSpec buildControllerSpec);

    // / <summary>
    // / Gets the build controller query results for the given build controller
    // specifications.
    // / </summary>
    // / <param name="buildControllerSpecs">The build controller
    // specifications.</param>
    // / <returns>The build controller query results for the given
    // specifications.</returns>
    public IBuildControllerQueryResult[] queryBuildControllers(IBuildControllerSpec[] buildControllerSpecs);

    // / <summary>
    // / Deletes build controllers from the server.
    // / </summary>
    // / <param name="Controllers">The controllers to be deleted.</param>
    public void deleteBuildControllers(IBuildController[] controllers);

    // / <summary>
    // / Deletes build controllers from the server.
    // / </summary>
    // / <param name="ControllerUris">The URIs of the controllers to be
    // deleted.</param>
    public void deleteBuildControllers(String[] controllerUris);

    // / <summary>
    // / Saves any changes made to the build controllers to the server.
    // / </summary>
    // / <param name="Controllers">The controllers to be saved.</param>
    // / <returns>The saved build controllers.</returns>
    public IBuildController[] saveBuildControllers(IBuildController[] controllers);

    // / <summary>
    // / Gets a single queued build for a given ID.
    // / </summary>
    // / <param name="queuedBuildId">The ID.</param>
    // / <param name="queryOptions">Options specifying the additional data to be
    // returned.</param>
    // / <returns>The queued build for the given ID.</returns>
    public IQueuedBuild getQueuedBuild(int queuedBuildId, QueryOptions queryOptions);

    // / <summary>
    // / Gets the queued builds for the given IDs.
    // / </summary>
    // / <param name="queuedBuildIds">The IDs.</param>
    // / <param name="queryOptions">Options specifying the additional data to be
    // returned.</param>
    // / <returns>The queued builds for the given IDs.</returns>
    public IQueuedBuild[] getQueuedBuild(int[] queuedBuildIds, QueryOptions queryOptions);

    // / <summary>
    // / Queries the build queue.
    // / </summary>
    // / <param name="spec">The build queue query specification</param>
    // / <returns>The queued build query result for the given
    // specification.</returns>
    public IQueuedBuildQueryResult queryQueuedBuilds(IQueuedBuildSpec spec);

    // / <summary>
    // / Queries the build queue.
    // / </summary>
    // / <param name="specs">The build queue query specifications</param>
    // / <returns>The build queue query results for the given
    // specifications</returns>
    public IQueuedBuildQueryResult[] queryQueuedBuilds(IQueuedBuildSpec[] specs);

    // / <summary>
    // / Retries the specified builds and places them into a batch together.
    // / </summary>
    // / <param name="queuedBuilds">The queued builds to be retried in a
    // batch.</param>
    // / <returns>The updated queued builds.</returns>
    public IQueuedBuild[] retryQueuedBuilds(IQueuedBuild[] queuedBuilds);

    // / <summary>
    // / Retries the specified builds and places them into a batch together.
    // / </summary>
    // / <param name="queuedBuilds">The queued builds to be retried in a
    // batch.</param>
    // / <param name="batchId">The ID of the newly created batch.</param>
    // / <returns>The updated queued builds.</returns>
    public IQueuedBuild[] retryQueuedBuilds(IQueuedBuild[] queuedBuilds, GUID batchId);

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
    public IQueuedBuild[] retryQueuedBuilds(
        IQueuedBuild[] queuedBuilds,
        GUID batchId,
        QueuedBuildRetryOption retryOption);

    // / <summary>
    // / Saves any changes made to the queued builds to the server.
    // / </summary>
    // / <param name="queuedBuilds">The queued builds to be saved.</param>
    // / <returns>The saved queued builds.</returns>
    public IQueuedBuild[] saveQueuedBuilds(IQueuedBuild[] queuedBuilds);

    // / <summary>
    // / Adds the build quality to the specified team project.
    // / </summary>
    // / <param name="teamProject">The target team project</param>
    // / <param name="quality">The quality which should be added</param>
    public void addBuildQuality(String teamProject, String quality);

    // / <summary>
    // / Adds the list of build qualities to the specified team project.
    // / </summary>
    // / <param name="teamProject">The target team project</param>
    // / <param name="qualities">The list of qualities which should be
    // added</param>
    public void addBuildQuality(String teamProject, String[] qualities);

    // / <summary>
    // / Deletes the specified build quality from the target team project.
    // / </summary>
    // / <param name="teamProject">The target team project</param>
    // / <param name="quality">The quality which should be removed</param>
    public void deleteBuildQuality(String teamProject, String quality);

    // / <summary>
    // / Deletes the specified list of build qualities from the target team
    // project.
    // / </summary>
    // / <param name="teamProject">The target team project</param>
    // / <param name="qualities">The list of qualities which should be
    // removed</param>
    public void deleteBuildQuality(String teamProject, String[] qualities);

    // / <summary>
    // / Retrieves the list of defined build qualities for the target team
    // project.
    // / </summary>
    // / <param name="teamProject">The target team project</param>
    // / <returns>The list of qualities currently defined</returns>
    public String[] getBuildQualities(String teamProject);

    // / <summary>
    // / Creates a new build process template, to be used by build definitions.
    // / </summary>
    // / <param name="teamProject">The Team Project for which a process template
    // is created.</param>
    // / <param name="serverPath">The version control path of the build process
    // XAML file.</param>
    // / <returns>The new buildprocess template.</returns>
    public IProcessTemplate createProcessTemplate(String teamProject, String serverPath);

    // / <summary>
    // / Gets all build process templates for a Team Project.
    // / </summary>
    // / <param name="teamProject">The Team Project for which to query for
    // process templates.</param>
    // / <returns>An array of the build process templates.</returns>
    public IProcessTemplate[] queryProcessTemplates(String teamProject);

    // / <summary>
    // / Gets all build process templates for a Team Project of a specified type
    // or types.
    // / </summary>
    // / <param name="teamProject">The Team Project for which to query for
    // process templates.</param>
    // / <param name="types">An array of ProcessTemplateType to query
    // for.</param>
    // / <returns>An array of the build process templates.</returns>
    public IProcessTemplate[] queryProcessTemplates(String teamProject, ProcessTemplateType[] types);

    // / <summary>
    // / Saves any changes made to the build process templates to the server.
    // / </summary>
    // / <param name="processTemplates">The build process templates to be
    // saved.</param>
    // / <returns>The build process templates.</returns>
    public IProcessTemplate[] saveProcessTemplates(IProcessTemplate[] processTemplates);

    // / <summary>
    // / Deletes the build process templates from the server.
    // / </summary>
    // / <param name="processTemplates">The build process templates to be
    // deleted.</param>
    public void deleteProcessTemplates(IProcessTemplate[] processTemplates);

    // / <summary>
    // / Queues a build for the given build definition with all default options.
    // Equivalent to
    // / calling QueueBuild(definition.CreateBuildRequest()).
    // / </summary>
    // / <param name="definition">The definition for which a build is
    // queued.</param>
    // / <returns>The queued build.</returns>
    public IQueuedBuild queueBuild(IBuildDefinition definition);

    // / <summary>
    // / Queues a build for the given build request with default QueueOptions.
    // / </summary>
    // / <param name="request">The parameters used in queuing the build,
    // including the BuildDefinition and BuildAgent.</param>
    // / <returns>The queued build.</returns>
    public IQueuedBuild queueBuild(IBuildRequest request);

    // / <summary>
    // / Queues a build for the given build request.
    // / </summary>
    // / <param name="request">The parameters used in queuing the build,
    // including the BuildDefinition and BuildAgent.</param>
    // / <param name="options">The options for the queuing of the build,
    // including whether or not to preview the queue position.</param>
    // / <returns>The queued build.</returns>
    public IQueuedBuild queueBuild(IBuildRequest request, QueueOptions options);

    // / <summary>
    // / Queues builds for the given build requests.
    // / </summary>
    // / <param name="request">The parameters used in queuing the build,
    // including the BuildDefinition and BuildAgent.</param>
    // / <param name="options">The options for the queuing of the build,
    // including whether or not to preview the queue position.</param>
    // / <returns>The queued build.</returns>
    public IQueuedBuild[] queueBuild(IBuildRequest[] requests, QueueOptions options);

    // / <summary>
    // / Stops the provided builds. If a build is not currently in progress then
    // no attempt is made to stop the
    // / build and no exception is thrown. Builds which no longer exist on the
    // server cause an exception to be thrown.
    // / </summary>
    // / <param name="builds"></param>
    public void stopBuilds(IBuildDetail[] builds);

    // / <summary>
    // / Stops all builds with the provided URIs. For builds which are not in
    // progress the URIs are silently ignored
    // / by the server. URIs which do not exist on the server will cause an
    // exception to be thrown.
    // / </summary>
    // / <param name="uris"></param>
    public void stopBuilds(String[] uris);

    // / <summary>
    // / Cancels the provided queued builds if they are currently in the active
    // or postponed states.
    // / </summary>
    // / <param name="builds">The builds which should be canceled</param>
    public void cancelBuilds(IQueuedBuild[] builds);

    // / <summary>
    // / Cancels queued builds with the provided IDs if they are currently in
    // the active or postponed states.
    // / </summary>
    // / <param name="ids">The IDs of queued builds which should be
    // canceled</param>
    public void cancelBuilds(int[] ids);

    // / <summary>
    // / Starts the provided queued builds if they are in a paused definition
    // queue and have a status of Queued.
    // / </summary>
    // / <param name="builds">The builds which should be started</param>
    public IQueuedBuild[] startQueuedBuildsNow(IQueuedBuild[] builds);

    // / <summary>
    // / Starts queued builds with the provided IDs if they are in a paused
    // definition queue and have a status of Queued.
    // / </summary>
    // / <param name="builds">The builds which should be started</param>
    public IQueuedBuild[] startQueuedBuildsNow(int[] ids);

    // / <summary>
    // / Gets the localized display text for known enumeration values (and the
    // ToString value for others).
    // / </summary>
    // / <param name="value">The value for which display text is
    // returned.</param>
    // / <returns>The localized display text.</returns>
    public String getDisplayText(Object value);

    // / <summary>
    // / Gets an array of the localized display text values for a known
    // enumeration (and the ToString values for others).
    // / </summary>
    // / <param name="enumType">The type of the enum.</param>
    // / <returns>The array of localized display text values.</returns>
    public String[] getDisplayTextValues(Class enumType);

    // / <summary>
    // / Gets an enumeration value from corresponding localized display text.
    // / </summary>
    // / <param name="enumType">The type of the returned enumeration.</param>
    // / <param name="displayText">The localized display text.</param>
    // / <param name="defaultValue">The default value - returned if the display
    // text cannot be converted.</param>
    // / <returns>The corresponding enumeration value, or defaultValue if the
    // conversion is unsuccessful.</returns>
    public Object getEnumValue(Class enumType, String displayText, Object defaultValue);
}
