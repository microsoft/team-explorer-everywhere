// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.build.internal.soapextensions;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import com.microsoft.tfs.core.Messages;
import com.microsoft.tfs.core.clients.build.BuildConstants;
import com.microsoft.tfs.core.clients.build.BuildService2008;
import com.microsoft.tfs.core.clients.build.IBuildControllerSpec;
import com.microsoft.tfs.core.clients.build.IBuildDefinitionSpec;
import com.microsoft.tfs.core.clients.build.IBuildDetail;
import com.microsoft.tfs.core.clients.build.IBuildDetailSpec;
import com.microsoft.tfs.core.clients.build.IQueuedBuildSpec;
import com.microsoft.tfs.core.clients.build.flags.BuildReason2010;
import com.microsoft.tfs.core.clients.build.flags.BuildServerVersion;
import com.microsoft.tfs.core.clients.build.flags.BuildStatus2010;
import com.microsoft.tfs.core.clients.build.flags.DeleteOptions;
import com.microsoft.tfs.core.clients.build.flags.QueryDeletedOption;
import com.microsoft.tfs.core.clients.build.flags.QueryOptions;
import com.microsoft.tfs.core.clients.build.flags.QueryOptions2010;
import com.microsoft.tfs.core.clients.build.flags.QueueOptions;
import com.microsoft.tfs.core.clients.build.flags.QueueOptions2010;
import com.microsoft.tfs.core.clients.build.internal.BuildGroupQueryResult2008;
import com.microsoft.tfs.core.clients.build.soapextensions.Agent2008Status;
import com.microsoft.tfs.core.clients.build.soapextensions.ContinuousIntegrationType;
import com.microsoft.tfs.core.clients.build.soapextensions.ControllerStatus;
import com.microsoft.tfs.core.clients.build.soapextensions.DefinitionTriggerType;
import com.microsoft.tfs.core.exceptions.NotSupportedException;
import com.microsoft.tfs.util.StringUtil;

public class TFS2008Helper {
    private final BuildServer buildServer;
    private final BuildService2008 buildService;

    public TFS2008Helper(final BuildServer buildServer) {
        this.buildServer = buildServer;
        this.buildService = new BuildService2008(buildServer.getConnection());
    }

    public static ControllerStatus convert(final Agent2008Status status) {
        if (status.equals(Agent2008Status.ENABLED)) {
            return ControllerStatus.AVAILABLE;
        } else {
            return ControllerStatus.UNAVAILABLE;
        }
    }

    public static BuildAgentSpec2008 convert(final IBuildControllerSpec spec, String defaultTeamProjectName) {
        final BuildAgentSpec2008 spec2008 = new BuildAgentSpec2008();

        // Must escape backslash for String and RegEx, thus 4-backslashes in the
        // regex.
        final String[] parts = StringUtil.splitRemoveEmpties(spec.getName(), "\\\\"); //$NON-NLS-1$
        if (parts.length == 2) {
            spec2008.setTeamProject(parts[0]);
            spec2008.setName(parts[1]);
        } else {
            if (StringUtil.isNullOrEmpty(defaultTeamProjectName)) {
                defaultTeamProjectName = BuildConstants.STAR;
            }
            spec2008.setTeamProject(defaultTeamProjectName);
            spec2008.setName(spec.getName());
        }
        return spec2008;
    }

    public static BuildDefinition convert(final BuildServer buildServer, final BuildDefinition2010 definition) {
        return TFS2010Helper.convert(buildServer, definition);
    }

    public static BuildDefinitionSpec2010 convert(final IBuildDefinitionSpec spec) {
        final BuildDefinitionSpec2010 spec2010 = TFS2010Helper.convert(spec);
        spec2010.setOptions(convert(spec2010.getOptions()));
        return spec2010;
    }

    public static BuildGroupItemSpec2010[] convert(final BuildServer buildServer, final IBuildControllerSpec[] specs) {
        final BuildGroupItemSpec2010[] newSpecs = new BuildGroupItemSpec2010[specs.length];
        for (int i = 0; i < specs.length; i++) {
            newSpecs[i] = convert(specs[i], StringUtil.EMPTY);
        }
        return newSpecs;
    }

    public static BuildDefinition2010 convert(final BuildServer buildServer, final BuildDefinition definition) {
        if (definition == null) {
            return null;
        }

        // Make sure the definition is ready to save
        definition.prepareToSave();

        final BuildDefinition2010 oldDefinition = new BuildDefinition2010(BuildServerVersion.V2, definition);
        for (final RetentionPolicy2010 policy : oldDefinition.getRetentionPolicies()) {
            if (!policy.getBuildReason().equals(BuildReason2010.ALL)) {
                // ERROR - Orcas servers do not support retention policies for
                // particular Reasons.
                final String format = Messages.getString("TFS2008Helper.RetentionNotSupportedFormat"); //$NON-NLS-1$
                final String message =
                    MessageFormat.format(format, buildServer.getDisplayText(policy.getBuildReason()));
                throw new NotSupportedException(message);
            }
        }

        return oldDefinition;
    }

    public BuildDefinition2010[] convert(final BuildDefinition[] definitions) {
        final BuildDefinition2010[] newDefinitions = new BuildDefinition2010[definitions.length];
        for (int i = 0; i < definitions.length; i++) {
            newDefinitions[i] = convert(buildServer, definitions[i]);
        }
        return newDefinitions;
    }

    public static BuildDefinition[] convert(final BuildServer buildServer, final BuildDefinition2010[] definitions) {
        final BuildDefinition[] newResults = TFS2010Helper.convert(buildServer, definitions);
        return newResults;
    }

    public static BuildGroupItemSpec2010[] convert(final IBuildDefinitionSpec[] specs) {
        final BuildGroupItemSpec2010[] newSpecs = new BuildGroupItemSpec2010[specs.length];
        for (int i = 0; i < specs.length; i++) {
            newSpecs[i] = convert(specs[i]);
        }
        return newSpecs;
    }

    public static BuildDetail convert(final BuildServer buildServer, final BuildDetail2010 build) {
        return TFS2010Helper.convert(buildServer, build);
    }

    public static BuildDetail[] convert(final BuildServer buildServer, final BuildDetail2010[] builds) {
        final BuildDetail[] newResults = TFS2010Helper.convert(buildServer, builds);
        return newResults;
    }

    public static BuildDetailSpec2010 convert(final BuildServer buildServer, final IBuildDetailSpec spec) {
        final BuildDetailSpec2010 spec2010 = TFS2010Helper.convert(buildServer, spec);
        spec2010.setQueryOptions(convert(spec2010.getQueryOptions()));
        spec2010.setStatus(convert(spec2010.getStatus()));
        return spec2010;
    }

    public static BuildDetailSpec2010[] convert(final BuildServer buildServer, final IBuildDetailSpec[] specs) {
        final BuildDetailSpec2010[] newSpecs = new BuildDetailSpec2010[specs.length];
        for (int i = 0; i < specs.length; i++) {
            newSpecs[i] = convert(buildServer, specs[i]);
        }
        return newSpecs;
    }

    public static BuildDeletionResult[] convert(final BuildDeletionResult2010[] results) {
        final BuildDeletionResult[] newResults = TFS2010Helper.convert(results);
        return newResults;
    }

    public void convert(
        final BuildAgent2008[] agents,
        final AtomicReference<BuildController[]> outControllers,
        final AtomicReference<BuildServiceHost[]> outServiceHosts) {
        final List<BuildController> controllers = new ArrayList<BuildController>();
        final List<BuildServiceHost> serviceHosts = new ArrayList<BuildServiceHost>();

        if (agents == null) {
            return;
        }

        for (final BuildAgent2008 agent2008 : agents) {
            if (agent2008 == null) {
                controllers.add(null);
            } else {
                controllers.add(new BuildController(buildServer, agent2008));
                serviceHosts.add(new BuildServiceHost(buildServer, agent2008));
            }
        }

        outControllers.set(controllers.toArray(new BuildController[controllers.size()]));
        outServiceHosts.set(serviceHosts.toArray(new BuildServiceHost[serviceHosts.size()]));
    }

    public BuildControllerQueryResult convertToControllerResult(final BuildGroupQueryResult2008 result) {
        final AtomicReference<BuildController[]> outControllers = new AtomicReference<BuildController[]>();
        final AtomicReference<BuildServiceHost[]> outServiceHosts = new AtomicReference<BuildServiceHost[]>();

        convert(result.getAgents(), outControllers, outServiceHosts);

        return new BuildControllerQueryResult(
            buildServer,
            new BuildAgent[0],
            outControllers.get(),
            outServiceHosts.get());
    }

    public BuildControllerQueryResult[] convertToControllerResult(final BuildGroupQueryResult2008[] results) {
        final BuildControllerQueryResult[] newResults = new BuildControllerQueryResult[results.length];
        for (int i = 0; i < results.length; i++) {
            newResults[i] = convertToControllerResult(results[i]);
        }
        return newResults;
    }

    public BuildDefinitionQueryResult convertToDefinitionResult(final BuildGroupQueryResult2008 result) {
        final AtomicReference<BuildController[]> outControllers = new AtomicReference<BuildController[]>();
        final AtomicReference<BuildServiceHost[]> outServiceHosts = new AtomicReference<BuildServiceHost[]>();

        convert(result.getAgents(), outControllers, outServiceHosts);
        final BuildDefinition[] definitions = convert(buildServer, result.getDefinitions());

        return new BuildDefinitionQueryResult(
            buildServer,
            new BuildAgent[0],
            outControllers.get(),
            definitions,
            outServiceHosts.get());
    }

    public BuildDefinitionQueryResult[] convertToDefinitionResult(final BuildGroupQueryResult2008[] results) {
        final BuildDefinitionQueryResult[] newResults = new BuildDefinitionQueryResult[results.length];
        for (int i = 0; i < results.length; i++) {
            newResults[i] = convertToDefinitionResult(results[i]);
        }
        return newResults;
    }

    public static BuildInformationNode[] convert(final BuildInformationNode2010[] nodes) {
        return TFS2010Helper.convert(nodes);
    }

    public static QueryOptions2010 convert(final QueryOptions options) {
        return convert(TFS2010Helper.convert(options));
    }

    public static QueryOptions2010 convert(QueryOptions2010 options) {
        // Convert Controllers to Agents
        if (options.contains(QueryOptions2010.CONTROLLERS)) {
            options = options.remove(QueryOptions2010.CONTROLLERS);
            options = options.combine(QueryOptions2010.AGENTS);
        }

        // Remove Process if it is present
        if (options.contains(QueryOptions2010.PROCESS)) {
            options = options.remove(QueryOptions2010.PROCESS);
            options = options.combine(QueryOptions2010.DEFINITIONS);
        }

        return options;
    }

    public BuildQueryResult convert(final BuildQueryResult2008 result) {
        final AtomicReference<BuildController[]> outControllers = new AtomicReference<BuildController[]>();
        final AtomicReference<BuildServiceHost[]> outServiceHosts = new AtomicReference<BuildServiceHost[]>();

        convert(result.getAgents(), outControllers, outServiceHosts);
        final BuildDefinition[] definitions = convert(buildServer, result.getDefinitions());
        final BuildDetail[] builds = convert(buildServer, result.getBuilds());

        return new BuildQueryResult(
            buildServer,
            new BuildAgent[0],
            outControllers.get(),
            definitions,
            builds,
            outServiceHosts.get());
    }

    public BuildQueryResult[] convert(final BuildQueryResult2008[] results) {
        final BuildQueryResult[] newResults = new BuildQueryResult[results.length];
        for (int i = 0; i < results.length; i++) {
            newResults[i] = convert(results[i]);
        }
        return newResults;
    }

    public BuildQueueQueryResult convert(final BuildQueueQueryResult2008 result) {
        final AtomicReference<BuildController[]> outControllers = new AtomicReference<BuildController[]>();
        final AtomicReference<BuildServiceHost[]> outServiceHosts = new AtomicReference<BuildServiceHost[]>();

        convert(result.getAgents(), outControllers, outServiceHosts);
        final BuildDefinition[] definitions = convert(buildServer, result.getDefinitions());
        final QueuedBuild[] queuedBuilds = convert(buildServer, result.getBuilds());

        return new BuildQueueQueryResult(
            buildServer,
            new BuildAgent[0],
            outControllers.get(),
            definitions,
            queuedBuilds,
            outServiceHosts.get(),
            new BuildDetail[0]);
    }

    public BuildQueueQueryResult[] convert(final BuildQueueQueryResult2008[] results) {
        final BuildQueueQueryResult[] newResults = new BuildQueueQueryResult[results.length];
        for (int i = 0; i < results.length; i++) {
            newResults[i] = convert(results[i]);
        }
        return newResults;
    }

    public BuildRequest2008 convert(final BuildRequest request) {
        return new BuildRequest2008(request);
    }

    public static BuildStatus2010 convert(BuildStatus2010 status) {
        if (status.equals(BuildStatus2010.ALL)) {
            // Orcas does not have an All build status, it also
            // doesn't support builds that are in a NotStarted state
            // So, we can simply remove NotStarted from All to convert to an
            // Orcas status.
            status = status.remove(BuildStatus2010.NOT_STARTED);
        } else if (status.equals(BuildStatus2010.NONE)) {
            // Orcas does not have a None build status, it also
            // doesn't support builds that are in a NotStarted state
            // So, we can simply replace None with NotStarted.
            status = BuildStatus2010.NOT_STARTED;
        }

        return status;
    }

    public static BuildUpdateOptions2010 convert(final BuildUpdateOptions update) {
        final BuildUpdateOptions2010 options2010 = TFS2010Helper.convert(update);
        options2010.getWebServiceObject().setStatus(convert(options2010.getStatus()).getWebServiceObject());
        return options2010;
    }

    public static BuildUpdateOptions2010[] convert(final BuildUpdateOptions[] updates) {
        final BuildUpdateOptions2010[] newUpdates = new BuildUpdateOptions2010[updates.length];
        for (int i = 0; i < updates.length; i++) {
            newUpdates[i] = convert(updates[i]);
        }
        return newUpdates;
    }

    public static InformationChangeRequest2010[] convert(final InformationChangeRequest[] requests) {
        final InformationChangeRequest2010[] newResults = TFS2010Helper.convert(requests);
        return newResults;
    }

    public static QueuedBuild convert(final BuildServer buildServer, final QueuedBuild2008 queuedBuild) {
        if (queuedBuild == null) {
            return null;
        }
        return new QueuedBuild(buildServer, queuedBuild);
    }

    public static QueuedBuild[] convert(final BuildServer buildServer, final QueuedBuild2008[] builds) {
        final QueuedBuild[] newBuilds = new QueuedBuild[builds.length];
        for (int i = 0; i < builds.length; i++) {
            newBuilds[i] = convert(buildServer, builds[i]);
        }
        return newBuilds;
    }

    public BuildDefinition[] addBuildDefinitions(final BuildDefinition[] definitions) {
        final BuildDefinition2010[] results = buildService.addBuildDefinitions(convert(definitions));
        return convert(buildServer, results);
    }

    public void addBuildQualities(final String teamProject, final String[] qualities) {
        buildService.addBuildQualities(teamProject, qualities);
    }

    public void cancelBuilds(final int[] ids) {
        buildService.cancelBuilds(ids);
    }

    public void deleteBuildQualities(final String teamProject, final String[] qualities) {
        buildService.deleteBuildQualities(teamProject, qualities);
    }

    public BuildDeletionResult[] deleteBuilds(final String[] uris, final DeleteOptions options) {
        return convert(buildService.deleteBuilds(uris));
    }

    public void DeleteBuildAgents(final String[] agentUris) {
        buildService.deleteBuildAgents(agentUris);
    }

    public void deleteBuildDefinitions(final String[] definitionUris) {
        buildService.deleteBuildDefinitions(definitionUris);
    }

    public void evaluateSchedules() {
        buildService.evaluateSchedules();
    }

    public BuildDefinition[] getAffectedBuildDefinitions(
        final String[] serverItems,
        final DefinitionTriggerType triggerType) {
        final ContinuousIntegrationType ciType = TFS2010Helper.convert(triggerType);
        final BuildDefinition2010[] definitions = buildService.getAffectedBuildDefinitions(serverItems);

        final List<BuildDefinition> list = new ArrayList<BuildDefinition>();
        for (int i = 0; i < definitions.length; i++) {
            if (definitions[i].getContinuousIntegrationType().contains(ciType)) {
                list.add(convert(buildServer, definitions[i]));
            }
        }
        return list.toArray(new BuildDefinition[list.size()]);
    }

    public String[] getBuildQualities(final String teamProject) {
        return buildService.getBuildQualities(teamProject);
    }

    public BuildControllerQueryResult[] queryBuildControllers(final IBuildControllerSpec[] specs) {
        final BuildGroupQueryResult2008[] results = buildService.queryBuildGroups(convert(buildServer, specs));
        return convertToControllerResult(results);
    }

    public BuildControllerQueryResult queryBuildControllersByUri(final String[] uris, final boolean includeAgents) {
        final BuildAgent2008[] results = buildService.queryBuildAgentsByUri(uris);

        final AtomicReference<BuildController[]> outControllers = new AtomicReference<BuildController[]>();
        final AtomicReference<BuildServiceHost[]> outServiceHosts = new AtomicReference<BuildServiceHost[]>();

        convert(results, outControllers, outServiceHosts);

        return new BuildControllerQueryResult(
            buildServer,
            new BuildAgent[0],
            outControllers.get(),
            outServiceHosts.get());
    }

    public BuildDefinitionQueryResult[] queryBuildDefinitions(final IBuildDefinitionSpec[] specs) {
        final BuildGroupQueryResult2008[] results = buildService.queryBuildGroups(convert(specs));
        return convertToDefinitionResult(results);
    }

    public BuildDefinitionQueryResult queryBuildDefinitionsByUri(final String[] uris) {
        final BuildGroupQueryResult2008 result = buildService.queryBuildDefinitionsByUri(uris);
        return convertToDefinitionResult(result);
    }

    public BuildQueryResult[] queryBuilds(final IBuildDetailSpec[] specs) {
        final BuildQueryResult2008[] results = buildService.queryBuilds(convert(buildServer, specs));
        return convert(results);
    }

    public IBuildDetail[] queryBuildsByUri(
        final String[] uris,
        final String[] informationTypes,
        final QueryOptions queryOptions,
        final QueryDeletedOption queryDeletedOption) {
        final BuildQueryResult2008 result =
            buildService.queryBuildsByUri(uris, informationTypes, convert(queryOptions));
        return convert(result).getBuilds();
    }

    public BuildDetail[] updateBuilds(final BuildUpdateOptions[] updates) {
        final BuildDetail2010[] results = buildService.updateBuilds(convert(updates));
        return convert(buildServer, results);
    }

    public BuildDefinition[] updateBuildDefinitions(final BuildDefinition[] definitions) {
        final BuildDefinition2010[] results = buildService.updateBuildDefinitions(convert(definitions));
        return convert(buildServer, results);
    }

    public void ProcessChangeset(final int changesetId) {
        buildService.processChangeset(changesetId);
    }

    public BuildQueueQueryResult[] queryQueuedBuilds(final IQueuedBuildSpec[] specs) {
        final BuildQueueSpec2008[] oldSpecs = new BuildQueueSpec2008[specs.length];
        for (int i = 0; i < specs.length; i++) {
            oldSpecs[i] = new BuildQueueSpec2008((BuildQueueSpec) specs[i]);
        }

        final BuildQueueQueryResult2008[] results = buildService.queryBuildQueue(oldSpecs);
        final BuildQueueQueryResult[] newResults = new BuildQueueQueryResult[results.length];
        for (int i = 0; i < results.length; i++) {
            newResults[i] = convert(results[i]);
        }
        return newResults;
    }

    public BuildQueueQueryResult queryQueuedBuildsById(final int[] queuedBuildIds, final QueryOptions queryOptions) {
        final BuildQueueQueryResult2008 result =
            buildService.queryBuildQueueById(queuedBuildIds, convert(queryOptions));
        return convert(result);
    }

    public QueuedBuild queueBuild(final BuildRequest request, final QueueOptions options) {
        final BuildRequest2008 request2008 = convert(request);
        final QueueOptions2010 options2010 = TFS2010Helper.convert(options);

        final QueuedBuild2008 result = buildService.queueBuild(request2008, options2010);
        final QueuedBuild queuedBuild = convert(buildServer, result);

        queuedBuild.setBuildDefinition(request.getBuildDefinition());

        if (queuedBuild.getBuild() != null) {
            ((BuildDetail) queuedBuild.getBuild()).setBuildDefinition(request.getBuildDefinition());
        }

        return queuedBuild;
    }

    public void stopBuilds(final String[] uris) {
        buildService.stopBuilds(uris);
    }

    public QueuedBuild[] updateQueuedBuilds(final QueuedBuildUpdateOptions[] updates) {
        final QueuedBuild2008[] results = buildService.updateQueuedBuilds(TFS2010Helper.convert(updates));

        final QueuedBuild[] newResults = new QueuedBuild[results.length];
        for (int i = 0; i < results.length; i++) {
            newResults[i] = convert(buildServer, results[i]);
        }
        return newResults;
    }

    public BuildInformationNode[] updateBuildInformation(final InformationChangeRequest[] requests) {
        return convert(buildService.updateBuildInformation(convert(requests)));
    }
}
