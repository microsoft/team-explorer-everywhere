// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.build.internal.soapextensions;

import java.util.HashMap;
import java.util.Map;

import com.microsoft.tfs.core.clients.build.IBuildServer;
import com.microsoft.tfs.core.clients.build.IFailure;
import com.microsoft.tfs.core.clients.build.IQueuedBuild;
import com.microsoft.tfs.core.clients.build.IQueuedBuildQueryResult;
import com.microsoft.tfs.core.clients.build.internal.utils.BuildTypeConvertor;
import com.microsoft.tfs.core.clients.build.internal.utils.QueryResultHelper;
import com.microsoft.tfs.core.internal.wrappers.WebServiceObjectWrapper;
import com.microsoft.tfs.core.internal.wrappers.WrapperUtils;
import com.microsoft.tfs.util.Check;

import ms.tfs.build.buildservice._04._BuildAgent;
import ms.tfs.build.buildservice._04._BuildController;
import ms.tfs.build.buildservice._04._BuildDefinition;
import ms.tfs.build.buildservice._04._BuildDetail;
import ms.tfs.build.buildservice._04._BuildQueueQueryResult;
import ms.tfs.build.buildservice._04._BuildServiceHost;
import ms.tfs.build.buildservice._04._QueuedBuild;

public class BuildQueueQueryResult extends WebServiceObjectWrapper implements IQueuedBuildQueryResult {
    private final QueuedBuild[] queuedBuilds;
    private final BuildAgent[] agents;
    private final BuildController[] controllers;
    private final BuildDefinition[] definitions;
    private final BuildDetail[] builds;
    private final BuildServiceHost[] serviceHosts;

    public BuildQueueQueryResult(final IBuildServer buildServer, final _BuildQueueQueryResult webServiceObject) {
        super(webServiceObject);

        Check.notNull(buildServer, "buildServer"); //$NON-NLS-1$

        serviceHosts = BuildTypeConvertor.toBuildServiceHostArray(buildServer, getWebServiceObject().getServiceHosts());
        controllers = BuildTypeConvertor.toBuildControllersArray(buildServer, getWebServiceObject().getControllers());
        agents = BuildTypeConvertor.toBuildAgentArray(getWebServiceObject().getAgents());
        definitions = BuildTypeConvertor.toBuildDefinitionArray(buildServer, getWebServiceObject().getDefinitions());
        builds = BuildTypeConvertor.toBuildDetailArray(buildServer, getWebServiceObject().getBuilds());
        queuedBuilds = BuildTypeConvertor.toQueuedBuildArray(buildServer, getWebServiceObject().getQueuedBuilds());

        afterDeserialize(buildServer);
    }

    public BuildQueueQueryResult(
        final BuildServer buildServer,
        final BuildAgent[] agents,
        final BuildController[] controllers,
        final BuildDefinition[] definitions,
        final QueuedBuild[] queuedBuilds,
        final BuildServiceHost[] serviceHosts,
        final BuildDetail[] buildDetails) {
        super(new _BuildQueueQueryResult());

        Check.notNull(buildServer, "buildServer"); //$NON-NLS-1$
        Check.notNull(agents, "agents"); //$NON-NLS-1$
        Check.notNull(controllers, "controllers"); //$NON-NLS-1$
        Check.notNull(definitions, "definitions"); //$NON-NLS-1$
        Check.notNull(queuedBuilds, "queuedBuilds"); //$NON-NLS-1$
        Check.notNull(serviceHosts, "serviceHosts"); //$NON-NLS-1$
        Check.notNull(buildDetails, "buildDetails"); //$NON-NLS-1$

        this.agents = agents;
        this.controllers = controllers;
        this.definitions = definitions;
        this.queuedBuilds = queuedBuilds;
        this.serviceHosts = serviceHosts;
        this.builds = buildDetails;

        final _BuildAgent[] _agents = (_BuildAgent[]) WrapperUtils.unwrap(_BuildAgent.class, agents);
        final _BuildController[] _controllers =
            (_BuildController[]) WrapperUtils.unwrap(_BuildController.class, controllers);
        final _BuildDefinition[] _definitions =
            (_BuildDefinition[]) WrapperUtils.unwrap(_BuildDefinition.class, definitions);
        final _QueuedBuild[] _queuedBuilds = (_QueuedBuild[]) WrapperUtils.unwrap(_QueuedBuild.class, queuedBuilds);
        final _BuildServiceHost[] _serviceHosts =
            (_BuildServiceHost[]) WrapperUtils.unwrap(_BuildServiceHost.class, serviceHosts);
        final _BuildDetail[] _buildDetails = (_BuildDetail[]) WrapperUtils.unwrap(_BuildDetail.class, buildDetails);

        getWebServiceObject().setAgents(_agents);
        getWebServiceObject().setControllers(_controllers);
        getWebServiceObject().setDefinitions(_definitions);
        getWebServiceObject().setQueuedBuilds(_queuedBuilds);
        getWebServiceObject().setServiceHosts(_serviceHosts);
        getWebServiceObject().setBuilds(_buildDetails);

        afterDeserialize(buildServer);
    }

    public _BuildQueueQueryResult getWebServiceObject() {
        return (_BuildQueueQueryResult) this.webServiceObject;
    }

    @Override
    public IQueuedBuild[] getQueuedBuilds() {
        return queuedBuilds;
    }

    @Override
    public IFailure[] getFailures() {
        return new IFailure[0];
    }

    private void afterDeserialize(final IBuildServer buildServer) {
        final Map<String, BuildController> controllerDict = QueryResultHelper.match(serviceHosts, controllers, agents);
        final Map<String, BuildDefinition> definitionDict = new HashMap<String, BuildDefinition>();
        final Map<String, BuildDetail> buildDict = new HashMap<String, BuildDetail>();

        for (final BuildDefinition definition : definitions) {
            definitionDict.put(definition.getURI(), definition);

            if (controllerDict.containsKey(definition.getBuildControllerURI())) {
                definition.setBuildController(controllerDict.get(definition.getBuildControllerURI()));
            }
        }

        for (final BuildDetail build : builds) {
            buildDict.put(build.getURI(), build);

            if (controllerDict.containsKey(build.getBuildControllerURI())) {
                build.setBuildController(controllerDict.get(build.getBuildControllerURI()));
            }

            if (definitionDict.containsKey(build.getBuildDefinitionURI())) {
                build.setBuildDefinition(definitionDict.get(build.getBuildDefinitionURI()));
            }

            for (final BuildInformationNode informationNode : build.getInternalInformation()) {
                informationNode.setBuild(build);
            }
        }

        for (final QueuedBuild build : queuedBuilds) {
            if (build == null) {
                continue;
            }

            if (controllerDict.containsKey(build.getBuildControllerURI())) {
                build.setBuildController(controllerDict.get(build.getBuildControllerURI()));
            }

            if (definitionDict.containsKey(build.getBuildDefinitionURI())) {
                build.setBuildDefinition(definitionDict.get(build.getBuildDefinitionURI()));
            }

            for (final String buildUri : build.getBuildURIs()) {
                final BuildDetail buildDetail = buildDict.get(buildUri);
                if (buildDetail != null) {
                    build.getAllBuilds().add(buildDetail);
                    buildDetail.addRequest(build);
                }
            }
        }
    }
}
