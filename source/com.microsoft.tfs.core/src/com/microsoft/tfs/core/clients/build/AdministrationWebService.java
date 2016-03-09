// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.build;

import com.microsoft.tfs.core.TFSTeamProjectCollection;
import com.microsoft.tfs.core.clients.build.internal.soapextensions.BuildAgent2010;
import com.microsoft.tfs.core.clients.build.internal.soapextensions.BuildAgentQueryResult2010;
import com.microsoft.tfs.core.clients.build.internal.soapextensions.BuildAgentSpec2010;
import com.microsoft.tfs.core.clients.build.internal.soapextensions.BuildAgentUpdateOptions2010;
import com.microsoft.tfs.core.clients.build.internal.soapextensions.BuildController2010;
import com.microsoft.tfs.core.clients.build.internal.soapextensions.BuildControllerQueryResult2010;
import com.microsoft.tfs.core.clients.build.internal.soapextensions.BuildControllerSpec2010;
import com.microsoft.tfs.core.clients.build.internal.soapextensions.BuildControllerUpdateOptions2010;
import com.microsoft.tfs.core.clients.build.internal.soapextensions.BuildServiceHost2010;
import com.microsoft.tfs.core.clients.build.internal.soapextensions.BuildServiceHostQueryResult2010;
import com.microsoft.tfs.core.clients.build.internal.soapextensions.BuildServiceHostUpdateOptions2010;
import com.microsoft.tfs.core.internal.wrappers.WrapperUtils;

import ms.tfs.build.buildservice._03._AdministrationWebServiceSoap;
import ms.tfs.build.buildservice._03._BuildAgent;
import ms.tfs.build.buildservice._03._BuildAgentQueryResult;
import ms.tfs.build.buildservice._03._BuildAgentSpec;
import ms.tfs.build.buildservice._03._BuildAgentUpdateOptions;
import ms.tfs.build.buildservice._03._BuildController;
import ms.tfs.build.buildservice._03._BuildControllerQueryResult;
import ms.tfs.build.buildservice._03._BuildControllerSpec;
import ms.tfs.build.buildservice._03._BuildControllerUpdateOptions;
import ms.tfs.build.buildservice._03._BuildServiceHost;
import ms.tfs.build.buildservice._03._BuildServiceHostQueryResult;

public class AdministrationWebService {
    private final _AdministrationWebServiceSoap webService;

    public AdministrationWebService(final TFSTeamProjectCollection tfs) {
        webService = (_AdministrationWebServiceSoap) tfs.getWebService(_AdministrationWebServiceSoap.class);
    }

    public void deleteBuildServiceHost(final String serviceHostUri) {
        webService.deleteBuildServiceHost(serviceHostUri);
    }

    public BuildServiceHostQueryResult2010 queryBuildServiceHosts(final String computer) {
        final _BuildServiceHostQueryResult _result = webService.queryBuildServiceHosts(computer);
        return new BuildServiceHostQueryResult2010(_result);
    }

    public void updateBuildServiceHost(final BuildServiceHostUpdateOptions2010 update) {
        webService.updateBuildServiceHost(update.getWebServiceObject());
    }

    public BuildAgentQueryResult2010 testBuildAgentConnection(final String agentUri) {
        final _BuildAgentQueryResult _result = webService.testBuildAgentConnection(agentUri);
        return new BuildAgentQueryResult2010(_result);
    }

    public BuildControllerQueryResult2010 testBuildControllerConnection(final String controllerUri) {
        final _BuildControllerQueryResult _result = webService.testBuildControllerConnection(controllerUri);
        return new BuildControllerQueryResult2010(_result);
    }

    public BuildServiceHostQueryResult2010 testBuildServiceHostConnections(final String hostUri) {
        final _BuildServiceHostQueryResult _result = webService.testBuildServiceHostConnections(hostUri);
        return new BuildServiceHostQueryResult2010(_result);
    }

    public BuildAgentQueryResult2010 queryBuildAgentsByUri(final String[] agentUris) {
        final _BuildAgentQueryResult _result = webService.queryBuildAgentsByUri(agentUris);
        return new BuildAgentQueryResult2010(_result);
    }

    public BuildAgentQueryResult2010[] queryBuildAgents(final BuildAgentSpec2010[] specs) {
        final _BuildAgentSpec[] _specs = (_BuildAgentSpec[]) WrapperUtils.unwrap(_BuildAgentSpec.class, specs);
        final _BuildAgentQueryResult[] _results = webService.queryBuildAgents(_specs);
        return (BuildAgentQueryResult2010[]) WrapperUtils.wrap(BuildAgentQueryResult2010.class, _results);
    }

    public BuildControllerQueryResult2010 queryBuildControllersByUri(final String[] uris, final boolean includeAgents) {
        final _BuildControllerQueryResult _result = webService.queryBuildControllersByUri(uris, includeAgents);
        return new BuildControllerQueryResult2010(_result);
    }

    public BuildControllerQueryResult2010[] queryBuildControllers(final BuildControllerSpec2010[] specs) {
        final _BuildControllerSpec[] _specs =
            (_BuildControllerSpec[]) WrapperUtils.unwrap(_BuildControllerSpec.class, specs);
        final _BuildControllerQueryResult[] _results = webService.queryBuildControllers(_specs);
        return (BuildControllerQueryResult2010[]) WrapperUtils.wrap(BuildControllerQueryResult2010.class, _results);
    }

    public void deleteBuildAgents(final String[] agentUris) {
        webService.deleteBuildAgents(agentUris);
    }

    public void deleteBuildControllers(final String[] controllerUris) {
        webService.deleteBuildControllers(controllerUris);
    }

    public BuildAgent2010[] addBuildAgents(final BuildAgent2010[] agents) {
        final _BuildAgent[] _agents = (_BuildAgent[]) WrapperUtils.unwrap(_BuildAgent.class, agents);
        final _BuildAgent[] _results = webService.addBuildAgents(_agents);
        return (BuildAgent2010[]) WrapperUtils.wrap(BuildAgent2010.class, _results);
    }

    public BuildServiceHostQueryResult2010 queryBuildServiceHostsByUri(final String[] uris) {
        final _BuildServiceHostQueryResult _result = webService.queryBuildServiceHostsByUri(uris);
        return new BuildServiceHostQueryResult2010(_result);
    }

    public void updateBuildControllers(final BuildControllerUpdateOptions2010[] updates) {
        final _BuildControllerUpdateOptions[] _updates =
            (_BuildControllerUpdateOptions[]) WrapperUtils.unwrap(_BuildControllerUpdateOptions.class, updates);
        webService.updateBuildControllers(_updates);
    }

    public BuildController2010[] addBuildControllers(final BuildController2010[] controllers) {
        final _BuildController[] _controllers =
            (_BuildController[]) WrapperUtils.unwrap(_BuildController.class, controllers);
        final _BuildController[] _results = webService.addBuildControllers(_controllers);
        return (BuildController2010[]) WrapperUtils.wrap(BuildController2010.class, _results);
    }

    public void updateBuildAgents(final BuildAgentUpdateOptions2010[] updates) {
        final _BuildAgentUpdateOptions[] _updates =
            (_BuildAgentUpdateOptions[]) WrapperUtils.unwrap(_BuildAgentUpdateOptions.class, updates);
        webService.updateBuildAgents(_updates);
    }

    public BuildServiceHost2010 addBuildServiceHost(final BuildServiceHost2010 serviceHost) {
        final _BuildServiceHost _serviceHost = serviceHost.getWebServiceObject();
        final _BuildServiceHost _result = webService.addBuildServiceHost(_serviceHost);
        return new BuildServiceHost2010(_result);
    }
}
