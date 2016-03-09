// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.build;

import com.microsoft.tfs.core.TFSTeamProjectCollection;
import com.microsoft.tfs.core.clients.build.internal.soapextensions.BuildAgent;
import com.microsoft.tfs.core.clients.build.internal.soapextensions.BuildAgentQueryResult;
import com.microsoft.tfs.core.clients.build.internal.soapextensions.BuildAgentUpdateOptions;
import com.microsoft.tfs.core.clients.build.internal.soapextensions.BuildController;
import com.microsoft.tfs.core.clients.build.internal.soapextensions.BuildControllerQueryResult;
import com.microsoft.tfs.core.clients.build.internal.soapextensions.BuildControllerUpdateOptions;
import com.microsoft.tfs.core.clients.build.internal.soapextensions.BuildServiceHost;
import com.microsoft.tfs.core.clients.build.internal.soapextensions.BuildServiceHostQueryResult;
import com.microsoft.tfs.core.clients.build.internal.soapextensions.BuildServiceHostUpdateOptions;
import com.microsoft.tfs.core.clients.build.internal.utils.BuildTypeConvertor;
import com.microsoft.tfs.core.internal.wrappers.WrapperUtils;

import ms.tfs.build.buildservice._04._AdministrationServiceSoap;
import ms.tfs.build.buildservice._04._BuildAgent;
import ms.tfs.build.buildservice._04._BuildAgentQueryResult;
import ms.tfs.build.buildservice._04._BuildAgentSpec;
import ms.tfs.build.buildservice._04._BuildAgentUpdateOptions;
import ms.tfs.build.buildservice._04._BuildController;
import ms.tfs.build.buildservice._04._BuildControllerQueryResult;
import ms.tfs.build.buildservice._04._BuildControllerSpec;
import ms.tfs.build.buildservice._04._BuildControllerUpdateOptions;
import ms.tfs.build.buildservice._04._BuildServiceHost;
import ms.tfs.build.buildservice._04._BuildServiceHostQueryResult;

public class AdministrationWebService4 {
    private final _AdministrationServiceSoap webService;
    private final IBuildServer buildServer;

    public AdministrationWebService4(final TFSTeamProjectCollection tfs) {
        webService = (_AdministrationServiceSoap) tfs.getWebService(_AdministrationServiceSoap.class);
        buildServer = tfs.getBuildServer();
    }

    public void deleteBuildServiceHost(final String serviceHostUri) {
        webService.deleteBuildServiceHost(serviceHostUri);
    }

    public BuildServiceHostQueryResult queryBuildServiceHosts(final String computer) {
        final _BuildServiceHostQueryResult _result = webService.queryBuildServiceHosts(computer);
        return new BuildServiceHostQueryResult(buildServer, _result);
    }

    public BuildServiceHostQueryResult queryBuildServiceHostsByUri(final String[] serviceHostUris) {
        final _BuildServiceHostQueryResult _result = webService.queryBuildServiceHostsByUri(serviceHostUris);
        return new BuildServiceHostQueryResult(buildServer, _result);
    }

    public BuildServiceHost addBuildServiceHost(final BuildServiceHost serviceHost) {
        final _BuildServiceHost _result = webService.addBuildServiceHost(serviceHost.getWebServiceObject());
        return new BuildServiceHost(buildServer, _result);
    }

    public void updateBuildServiceHost(final BuildServiceHostUpdateOptions update) {
        webService.updateBuildServiceHost(update.getWebServiceObject());
    }

    public IBuildAgentQueryResult queryBuildAgentsByUri(final String[] agentUris, final String[] propertyNameFilters) {
        final _BuildAgentQueryResult _result = webService.queryBuildAgentsByUri(agentUris, propertyNameFilters);
        return new BuildAgentQueryResult(buildServer, _result);
    }

    public IBuildAgentQueryResult[] queryBuildAgents(final IBuildAgentSpec[] buildAgentSpecs) {
        final _BuildAgentSpec[] _specs =
            (_BuildAgentSpec[]) WrapperUtils.unwrap(_BuildAgentSpec.class, buildAgentSpecs);
        final _BuildAgentQueryResult[] _results = webService.queryBuildAgents(_specs);
        return BuildTypeConvertor.toBuildAgentQueryResultArray(buildServer, _results);
    }

    public void deleteBuildAgents(final String[] agentUris) {
        webService.deleteBuildAgents(agentUris);
    }

    public BuildAgent[] addBuildAgents(final BuildAgent[] addRequests) {
        final _BuildAgent[] _agents = (_BuildAgent[]) WrapperUtils.unwrap(_BuildAgent.class, addRequests);
        final _BuildAgent[] _results = webService.addBuildAgents(_agents);
        return BuildTypeConvertor.toBuildAgentArray(_results);
    }

    public void updateBuildAgents(final BuildAgentUpdateOptions[] updateOptions) {
        final _BuildAgentUpdateOptions[] _updates =
            (_BuildAgentUpdateOptions[]) WrapperUtils.unwrap(_BuildAgentUpdateOptions.class, updateOptions);
        webService.updateBuildAgents(_updates);
    }

    public IBuildControllerQueryResult queryBuildControllersByUri(
        final String[] controllerUris,
        final String[] propertyNameFilters,
        final boolean includeAgents) {
        final _BuildControllerQueryResult _result =
            webService.queryBuildControllersByUri(controllerUris, propertyNameFilters, includeAgents);
        return new BuildControllerQueryResult(buildServer, _result);
    }

    public IBuildControllerQueryResult[] queryBuildControllers(final IBuildControllerSpec[] controllerSpecs) {
        final _BuildControllerSpec[] _specs =
            (_BuildControllerSpec[]) WrapperUtils.unwrap(_BuildControllerSpec.class, controllerSpecs);
        final _BuildControllerQueryResult[] _results = webService.queryBuildControllers(_specs);
        return BuildTypeConvertor.toBuildControllerQueryResultArray(buildServer, _results);
    }

    public void deleteBuildControllers(final String[] controllerUris) {
        webService.deleteBuildControllers(controllerUris);
    }

    public BuildController[] addBuildControllers(final BuildController[] addRequests) {
        final _BuildController[] _controllers =
            (_BuildController[]) WrapperUtils.unwrap(_BuildController.class, addRequests);
        final _BuildController[] _results = webService.addBuildControllers(_controllers);
        return BuildTypeConvertor.toBuildControllersArray(buildServer, _results);
    }

    public void updateBuildControllers(final BuildControllerUpdateOptions[] updateOptions) {
        final _BuildControllerUpdateOptions[] _updates =
            (_BuildControllerUpdateOptions[]) WrapperUtils.unwrap(_BuildControllerUpdateOptions.class, updateOptions);
        webService.updateBuildControllers(_updates);
    }
}
