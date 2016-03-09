// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.build.internal.soapextensions;

import java.util.ArrayList;
import java.util.List;

import com.microsoft.tfs.core.clients.build.AdministrationWebService;
import com.microsoft.tfs.core.clients.build.BuildQueueWebService;
import com.microsoft.tfs.core.clients.build.BuildWebService;
import com.microsoft.tfs.core.clients.build.IBuildAgentQueryResult;
import com.microsoft.tfs.core.clients.build.IBuildAgentSpec;
import com.microsoft.tfs.core.clients.build.IBuildControllerQueryResult;
import com.microsoft.tfs.core.clients.build.IBuildControllerSpec;
import com.microsoft.tfs.core.clients.build.IBuildDefinition;
import com.microsoft.tfs.core.clients.build.IBuildDefinitionQueryResult;
import com.microsoft.tfs.core.clients.build.IBuildDefinitionSpec;
import com.microsoft.tfs.core.clients.build.IBuildDeletionResult;
import com.microsoft.tfs.core.clients.build.IBuildDetail;
import com.microsoft.tfs.core.clients.build.IBuildDetailSpec;
import com.microsoft.tfs.core.clients.build.IBuildRequest;
import com.microsoft.tfs.core.clients.build.IQueuedBuild;
import com.microsoft.tfs.core.clients.build.IQueuedBuildSpec;
import com.microsoft.tfs.core.clients.build.flags.DeleteOptions;
import com.microsoft.tfs.core.clients.build.flags.QueryDeletedOption;
import com.microsoft.tfs.core.clients.build.flags.QueryOptions;
import com.microsoft.tfs.core.clients.build.flags.QueueOptions;
import com.microsoft.tfs.core.clients.build.soapextensions.DefinitionTriggerType;
import com.microsoft.tfs.core.clients.build.soapextensions.ProcessTemplateType;

public class Build2010Helper extends TFS2010Helper {
    private final BuildServer buildServer;
    private final BuildWebService buildService;
    private final BuildQueueWebService buildQueueService;
    private final AdministrationWebService administrationService;

    public Build2010Helper(final BuildServer buildServer) {
        this.buildServer = buildServer;
        this.buildService = new BuildWebService(buildServer.getConnection());
        this.buildQueueService = new BuildQueueWebService(buildServer.getConnection());
        this.administrationService = new AdministrationWebService(buildServer.getConnection());
    }

    public BuildQueueQueryResult queryQueuedBuildsById(final int[] queuedBuildIds, final QueryOptions options) {
        final BuildQueueQueryResult2010 result = buildQueueService.queryBuildsById(queuedBuildIds, convert(options));
        return convert(buildServer, result);
    }

    public QueuedBuild[] queueBuilds(final IBuildRequest[] requests, final QueueOptions options) {
        final QueuedBuild2010[] results = buildQueueService.queueBuilds(convert(requests), convert(options));

        final List<QueuedBuild> newBuilds = new ArrayList<QueuedBuild>(results.length);
        for (int i = 0; i < results.length; i++) {
            final QueuedBuild newBuild = convert(buildServer, results[i]);
            newBuild.setBuildController(requests[i].getBuildController());
            newBuild.setBuildDefinition(requests[i].getBuildDefinition());
            newBuilds.add(newBuild);
        }

        return newBuilds.toArray(new QueuedBuild[newBuilds.size()]);
    }

    public IQueuedBuild[] updateQueuedBuilds(final QueuedBuildUpdateOptions[] updates) {
        final QueuedBuild2010[] results = buildQueueService.updateBuilds(convert(updates));
        return convert(buildServer, results);
    }

    public BuildDetail notifyBuildCompleted(final String uri) {
        final BuildDetail2010 results = buildService.notifyBuildCompleted(uri);
        return convert(buildServer, results);
    }

    public BuildDetail[] updateBuilds(final BuildUpdateOptions[] updateOptions) {
        final BuildDetail2010[] results = buildService.updateBuilds(convert(updateOptions));
        return convert(buildServer, results);
    }

    public BuildInformationNode[] updateBuildInformation(final InformationChangeRequest[] requests) {
        final BuildInformationNode2010[] results = buildService.updateBuildInformation(convert(requests));
        return convert(results);
    }

    public IBuildDefinition[] getAffectedBuildDefinitions(
        final String[] serverItems,
        final DefinitionTriggerType triggerType) {
        final BuildDefinition2010[] results =
            buildService.getAffectedBuildDefinitions(serverItems, convert(triggerType));
        return convert(buildServer, results);
    }

    public IBuildDetail[] queryBuildsByUri(
        final String[] buildUris,
        final String[] informationTypes,
        final QueryOptions queryOptions,
        final QueryDeletedOption queryDeletedOption) {
        final BuildQueryResult2010 result = buildService.queryBuildsByUri(
            buildUris,
            informationTypes,
            convert(queryOptions),
            convert(queryDeletedOption));

        return convert(buildServer, result).getBuilds();
    }

    public BuildQueryResult[] queryBuilds(final IBuildDetailSpec[] specs) {
        final BuildQueryResult2010[] results = buildService.queryBuilds(convert(buildServer, specs));
        return convert(buildServer, results);
    }

    public IBuildDeletionResult[] deleteBuilds(final String[] uris, final DeleteOptions options) {
        final BuildDeletionResult2010[] results = buildService.deleteBuilds(uris, convert(options));
        return convert(results);
    }

    public void destroyBuilds(final String[] buildUris) {
        buildService.destroyBuilds(buildUris);
    }

    public IBuildDefinitionQueryResult queryBuildDefinitionsByUri(final String[] uris, final QueryOptions options) {
        final BuildDefinitionQueryResult2010 result = buildService.queryBuildDefinitionsByUri(uris, convert(options));
        return convert(buildServer, result);
    }

    public IBuildDefinitionQueryResult[] queryBuildDefinitions(final IBuildDefinitionSpec[] specs) {
        final BuildDefinitionQueryResult2010[] results = buildService.queryBuildDefinitions(convert(specs));
        return convert(buildServer, results);
    }

    public void deleteBuildDefinitions(final String[] definitionUris) {
        buildService.deleteBuildDefinitions(definitionUris);
    }

    public IBuildDefinition[] addBuildDefinitions(final BuildDefinition[] definitions) {
        final BuildDefinition2010[] results = buildService.addBuildDefinitions(convert(definitions));
        return convert(buildServer, results);
    }

    public IBuildDefinition[] updateBuildDefinitions(final BuildDefinition[] definitions) {
        final BuildDefinition2010[] results = buildService.updateBuildDefinitions(convert(definitions));
        return convert(buildServer, results);
    }

    public void deleteBuildServiceHost(final String serviceHostUri) {
        administrationService.deleteBuildServiceHost(serviceHostUri);
    }

    public BuildServiceHostQueryResult queryBuildServiceHosts(final String computer) {
        final BuildServiceHostQueryResult2010 result = administrationService.queryBuildServiceHosts(computer);
        return convert(buildServer, result);
    }

    public BuildServiceHost addBuildServiceHost(final BuildServiceHost serviceHost) {
        final BuildServiceHost2010 result = administrationService.addBuildServiceHost(convert(serviceHost));
        return convert(buildServer, result);
    }

    public void updateBuildServiceHost(final BuildServiceHostUpdateOptions update) {
        administrationService.updateBuildServiceHost(convert(update));
    }

    public BuildServiceHostQueryResult testBuildServiceHostConnections(final String uri) {
        final BuildServiceHostQueryResult2010 result = administrationService.testBuildServiceHostConnections(uri);
        return convert(buildServer, result);
    }

    public IBuildControllerQueryResult testBuildControllerConnection(final String uri) {
        final BuildControllerQueryResult2010 result = administrationService.testBuildControllerConnection(uri);
        return convert(buildServer, result);
    }

    public BuildAgentQueryResult testBuildAgentConnection(final String uri) {
        final BuildAgentQueryResult2010 result = administrationService.testBuildAgentConnection(uri);
        return convert(buildServer, result);
    }

    public BuildAgentQueryResult queryBuildAgentsByUri(final String[] uris) {
        final BuildAgentQueryResult2010 result = administrationService.queryBuildAgentsByUri(uris);
        return convert(buildServer, result);
    }

    public IBuildAgentQueryResult[] queryBuildAgents(final IBuildAgentSpec[] specs) {
        final BuildAgentQueryResult2010[] results = administrationService.queryBuildAgents(convert(specs));
        return convert(buildServer, results);
    }

    public void deleteBuildAgents(final String[] agentUris) {
        administrationService.deleteBuildAgents(agentUris);
    }

    public BuildAgent[] addBuildAgents(final BuildAgent[] agents) {
        final BuildAgent2010[] results = administrationService.addBuildAgents(convert(agents));
        return convert(results);
    }

    public void updateBuildAgents(final BuildAgentUpdateOptions[] updates) {
        administrationService.updateBuildAgents(convert(updates));
    }

    public BuildServiceHostQueryResult queryBuildServiceHostsByUri(final String[] uris) {
        final BuildServiceHostQueryResult2010 result = administrationService.queryBuildServiceHostsByUri(uris);
        return convert(buildServer, result);
    }

    public IBuildControllerQueryResult queryBuildControllersByUri(final String[] uris, final boolean includeAgents) {
        final BuildControllerQueryResult2010 result =
            administrationService.queryBuildControllersByUri(uris, includeAgents);
        return convert(buildServer, result);
    }

    public IBuildControllerQueryResult[] queryBuildControllers(final IBuildControllerSpec[] specs) {
        final BuildControllerQueryResult2010[] results = administrationService.queryBuildControllers(convert(specs));
        return convert(buildServer, results);
    }

    public void deleteBuildControllers(final String[] controllerUris) {
        administrationService.deleteBuildControllers(controllerUris);
    }

    public BuildController[] addBuildControllers(final BuildController[] controllers) {
        final BuildController2010[] results = administrationService.addBuildControllers(convert(controllers));
        return convert(buildServer, results);
    }

    public void updateBuildControllers(final BuildControllerUpdateOptions[] updates) {
        administrationService.updateBuildControllers(convert(updates));
    }

    public BuildQueueQueryResult[] queryQueuedBuilds(final IQueuedBuildSpec[] specs) {
        final BuildQueueQueryResult2010[] results = buildQueueService.queryBuilds(convert(specs));
        return convert(buildServer, results);
    }

    public void stopBuilds(final String[] uris) {
        buildService.stopBuilds(uris);
    }

    public void cancelBuilds(final int[] ids) {
        buildQueueService.cancelBuilds(ids);
    }

    public void addBuildQualities(final String teamProject, final String[] qualities) {
        buildService.addBuildQualities(teamProject, qualities);
    }

    public void deleteBuildQualities(final String teamProject, final String[] qualities) {
        buildService.deleteBuildQualities(teamProject, qualities);
    }

    public String[] getBuildQualities(final String teamProject) {
        return buildService.getBuildQualities(teamProject);
    }

    public ProcessTemplate[] queryProcessTemplates(final String teamProject, final ProcessTemplateType[] types) {
        final ProcessTemplate2010[] results = buildService.queryProcessTemplates(teamProject, convert(types));
        return convert(buildServer, results);
    }

    public void deleteProcessTemplates(final int[] templateIds) {
        buildService.deleteProcessTemplates(templateIds);
    }

    public ProcessTemplate[] addProcessTemplates(final ProcessTemplate[] templates) {
        final ProcessTemplate2010[] results = buildService.addProcessTemplates(convert(templates));
        return convert(buildServer, results);
    }

    public ProcessTemplate[] updateProcessTemplates(final ProcessTemplate[] templates) {
        final ProcessTemplate2010[] results = buildService.updateProcessTemplates(convert(templates));
        return convert(buildServer, results);
    }

    public static BuildQueueSpec2010[] convert(final IQueuedBuildSpec[] specs) {
        final BuildQueueSpec2010[] newSpecs = new BuildQueueSpec2010[specs.length];
        for (int i = 0; i < specs.length; i++) {
            newSpecs[i] = new BuildQueueSpec2010((BuildQueueSpec) specs[i]);
        }
        return newSpecs;
    }
}
