// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.build.internal.soapextensions;

import java.util.HashMap;
import java.util.Map;

import com.microsoft.tfs.core.clients.build.IBuildDetail;
import com.microsoft.tfs.core.clients.build.IBuildQueryResult;
import com.microsoft.tfs.core.clients.build.IBuildServer;
import com.microsoft.tfs.core.clients.build.IFailure;
import com.microsoft.tfs.core.clients.build.internal.utils.BuildTypeConvertor;
import com.microsoft.tfs.core.clients.build.internal.utils.QueryResultHelper;
import com.microsoft.tfs.core.internal.wrappers.WebServiceObjectWrapper;
import com.microsoft.tfs.core.internal.wrappers.WrapperUtils;
import com.microsoft.tfs.util.Check;

import ms.tfs.build.buildservice._04._BuildAgent;
import ms.tfs.build.buildservice._04._BuildController;
import ms.tfs.build.buildservice._04._BuildDefinition;
import ms.tfs.build.buildservice._04._BuildQueryResult;
import ms.tfs.build.buildservice._04._BuildServiceHost;
import ms.tfs.build.buildservice._04._QueuedBuild;

public class BuildQueryResult extends WebServiceObjectWrapper implements IBuildQueryResult {
    private final BuildServiceHost[] serviceHosts;
    private final BuildController[] controllers;
    private final BuildAgent[] agents;
    private final BuildDefinition[] definitions;
    private final QueuedBuild[] queuedBuilds;
    private final BuildDetail[] builds;

    public BuildQueryResult(final IBuildServer buildServer, final _BuildQueryResult webServiceObject) {
        super(webServiceObject);

        Check.notNull(buildServer, "buildServer"); //$NON-NLS-1$

        serviceHosts = BuildTypeConvertor.toBuildServiceHostArray(buildServer, getWebServiceObject().getServiceHosts());
        controllers = BuildTypeConvertor.toBuildControllersArray(buildServer, getWebServiceObject().getControllers());
        agents = BuildTypeConvertor.toBuildAgentArray(getWebServiceObject().getAgents());
        definitions = BuildTypeConvertor.toBuildDefinitionArray(buildServer, getWebServiceObject().getDefinitions());
        queuedBuilds = BuildTypeConvertor.toQueuedBuildArray(buildServer, getWebServiceObject().getQueuedBuilds());
        builds = BuildTypeConvertor.toBuildDetailArray(buildServer, getWebServiceObject().getBuilds());

        afterDeserialize(buildServer);
    }

    public BuildQueryResult(
        final IBuildServer buildServer,
        final BuildAgent[] agents,
        final BuildController[] controllers,
        final BuildDefinition[] definitions,
        final BuildDetail[] builds,
        final BuildServiceHost[] serviceHosts) {
        super(new _BuildQueryResult());

        Check.notNull(buildServer, "buildServer"); //$NON-NLS-1$
        Check.notNull(agents, "agents"); //$NON-NLS-1$
        Check.notNull(controllers, "controllers"); //$NON-NLS-1$
        Check.notNull(definitions, "definitions"); //$NON-NLS-1$
        Check.notNull(builds, "builds"); //$NON-NLS-1$
        Check.notNull(serviceHosts, "serviceHosts"); //$NON-NLS-1$

        this.agents = agents;
        this.controllers = controllers;
        this.definitions = definitions;
        this.builds = builds;
        this.serviceHosts = serviceHosts;
        this.queuedBuilds = new QueuedBuild[0];

        final _BuildAgent[] _agents = (_BuildAgent[]) WrapperUtils.unwrap(_BuildAgent.class, agents);
        final _BuildController[] _controllers =
            (_BuildController[]) WrapperUtils.unwrap(_BuildController.class, controllers);
        final _BuildDefinition[] _definitions =
            (_BuildDefinition[]) WrapperUtils.unwrap(_BuildDefinition.class, definitions);
        final _QueuedBuild[] _queuedBuilds = (_QueuedBuild[]) WrapperUtils.unwrap(_QueuedBuild.class, queuedBuilds);
        final _BuildServiceHost[] _serviceHosts =
            (_BuildServiceHost[]) WrapperUtils.unwrap(_BuildServiceHost.class, serviceHosts);

        getWebServiceObject().setAgents(_agents);
        getWebServiceObject().setControllers(_controllers);
        getWebServiceObject().setDefinitions(_definitions);
        getWebServiceObject().setQueuedBuilds(_queuedBuilds);
        getWebServiceObject().setServiceHosts(_serviceHosts);

        afterDeserialize(buildServer);
    }

    public _BuildQueryResult getWebServiceObject() {
        return (_BuildQueryResult) this.webServiceObject;
    }

    @Override
    public IBuildDetail[] getBuilds() {
        return builds;
    }

    @Override
    public IFailure[] getFailures() {
        return new IFailure[0];
    }

    // Match up Definitions.
    private void afterDeserialize(final IBuildServer buildServer) {
        final Map<String, BuildController> controllerMap = QueryResultHelper.match(serviceHosts, controllers, agents);
        final Map<String, BuildDefinition> definitionMap = new HashMap<String, BuildDefinition>();

        for (final BuildDefinition definition : definitions) {
            definitionMap.put(definition.getURI(), definition);

            final BuildController controller = controllerMap.get(definition.getBuildControllerURI());
            if (controller != null) {
                definition.setBuildController(controller);
            }
        }

        final Map<Integer, QueuedBuild> buildMap = new HashMap<Integer, QueuedBuild>();
        for (final QueuedBuild build : queuedBuilds) {
            buildMap.put(build.getID(), build);

            final BuildController controller = controllerMap.get(build.getBuildControllerURI());
            if (controller != null) {
                build.setBuildController(controller);
            }

            final BuildDefinition definition = definitionMap.get(build.getBuildDefinitionURI());
            if (definition != null) {
                build.setBuildDefinition(definition);
            }
        }

        for (final BuildDetail build : builds) {
            if (build == null) {
                continue;
            }

            final BuildController controller = controllerMap.get(build.getBuildControllerURI());
            if (controller != null) {
                build.setBuildController(controller);
            }

            final BuildDefinition definition = definitionMap.get(build.getBuildDefinitionURI());
            if (definition != null) {
                build.setBuildDefinition(definition);
            }

            for (final BuildInformationNode informationNode : build.getInternalInformation()) {
                informationNode.setBuild(build);
            }

            for (final int queueId : build.getRequestIDs()) {
                final QueuedBuild queuedBuild = buildMap.get(queueId);
                if (queuedBuild != null) {
                    build.addRequest(queuedBuild);
                    queuedBuild.getAllBuilds().add(build);
                }
            }
        }
    }
}
