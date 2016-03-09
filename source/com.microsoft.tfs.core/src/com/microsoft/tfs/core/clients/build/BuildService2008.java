// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.build;

import com.microsoft.tfs.core.TFSTeamProjectCollection;
import com.microsoft.tfs.core.clients.build.flags.QueryOptions2010;
import com.microsoft.tfs.core.clients.build.flags.QueueOptions2010;
import com.microsoft.tfs.core.clients.build.internal.BuildGroupQueryResult2008;
import com.microsoft.tfs.core.clients.build.internal.soapextensions.BuildAgent2008;
import com.microsoft.tfs.core.clients.build.internal.soapextensions.BuildDefinition2010;
import com.microsoft.tfs.core.clients.build.internal.soapextensions.BuildDeletionResult2010;
import com.microsoft.tfs.core.clients.build.internal.soapextensions.BuildDetail2010;
import com.microsoft.tfs.core.clients.build.internal.soapextensions.BuildDetailSpec2010;
import com.microsoft.tfs.core.clients.build.internal.soapextensions.BuildGroupItemSpec2010;
import com.microsoft.tfs.core.clients.build.internal.soapextensions.BuildInformationNode2010;
import com.microsoft.tfs.core.clients.build.internal.soapextensions.BuildQueryResult2008;
import com.microsoft.tfs.core.clients.build.internal.soapextensions.BuildQueueQueryResult2008;
import com.microsoft.tfs.core.clients.build.internal.soapextensions.BuildQueueSpec2008;
import com.microsoft.tfs.core.clients.build.internal.soapextensions.BuildRequest2008;
import com.microsoft.tfs.core.clients.build.internal.soapextensions.BuildUpdateOptions2010;
import com.microsoft.tfs.core.clients.build.internal.soapextensions.InformationChangeRequest2010;
import com.microsoft.tfs.core.clients.build.internal.soapextensions.QueuedBuild2008;
import com.microsoft.tfs.core.clients.build.internal.soapextensions.QueuedBuildUpdateOptions2010;
import com.microsoft.tfs.core.internal.wrappers.WrapperUtils;

import ms.tfs.build.buildservice._03._BuildAgent2008;
import ms.tfs.build.buildservice._03._BuildDefinition;
import ms.tfs.build.buildservice._03._BuildDeletionResult;
import ms.tfs.build.buildservice._03._BuildDetail;
import ms.tfs.build.buildservice._03._BuildDetailSpec;
import ms.tfs.build.buildservice._03._BuildGroupItemSpec;
import ms.tfs.build.buildservice._03._BuildGroupQueryResult;
import ms.tfs.build.buildservice._03._BuildInformationNode;
import ms.tfs.build.buildservice._03._BuildQueryResult2008;
import ms.tfs.build.buildservice._03._BuildQueueQueryResult2008;
import ms.tfs.build.buildservice._03._BuildQueueSpec2008;
import ms.tfs.build.buildservice._03._BuildRequest2008;
import ms.tfs.build.buildservice._03._BuildServiceSoap;
import ms.tfs.build.buildservice._03._BuildUpdateOptions;
import ms.tfs.build.buildservice._03._InformationChangeRequest;
import ms.tfs.build.buildservice._03._QueryOptions;
import ms.tfs.build.buildservice._03._QueueOptions;
import ms.tfs.build.buildservice._03._QueuedBuild2008;
import ms.tfs.build.buildservice._03._QueuedBuildUpdateOptions;

public class BuildService2008 {
    private final _BuildServiceSoap webService;

    public BuildService2008(final TFSTeamProjectCollection tfs) {
        webService = (_BuildServiceSoap) tfs.getWebService(_BuildServiceSoap.class);
    }

    public BuildDefinition2010[] addBuildDefinitions(final BuildDefinition2010[] definitions) {
        final _BuildDefinition[] _definitions =
            (_BuildDefinition[]) WrapperUtils.unwrap(_BuildDefinition.class, definitions);
        final _BuildDefinition[] _results = webService.addBuildDefinitions(_definitions);
        return (BuildDefinition2010[]) WrapperUtils.wrap(BuildDefinition2010.class, _results);
    }

    public void addBuildQualities(final String teamProject, final String[] qualities) {
        webService.addBuildQualities(teamProject, qualities);
    }

    public void cancelBuilds(final int[] ids) {
        webService.cancelBuilds(ids);
    }

    public void deleteBuildDefinitions(final String[] uris) {
        webService.deleteBuildDefinitions(uris);
    }

    public void deleteBuildQualities(final String teamProject, final String[] qualities) {
        webService.deleteBuildQualities(teamProject, qualities);
    }

    public BuildDeletionResult2010[] deleteBuilds(final String[] uris) {
        final _BuildDeletionResult[] _results = webService.deleteBuilds(uris);
        return (BuildDeletionResult2010[]) WrapperUtils.wrap(BuildDeletionResult2010.class, _results);
    }

    public void evaluateSchedules() {
        webService.evaluateSchedules();
    }

    public BuildDefinition2010[] getAffectedBuildDefinitions(final String[] serverItems) {
        final _BuildDefinition[] _results = webService.getAffectedBuildDefinitions(serverItems);
        return (BuildDefinition2010[]) WrapperUtils.wrap(BuildDefinition2010.class, _results);
    }

    public String[] getBuildQualities(final String teamProject) {
        return webService.getBuildQualities(teamProject);
    }

    public BuildAgent2008[] queryBuildAgentsByUri(final String[] uris) {
        final _BuildAgent2008[] _results = webService.queryBuildAgentsByUri(uris);
        return (BuildAgent2008[]) WrapperUtils.wrap(BuildAgent2008.class, _results);
    }

    public BuildGroupQueryResult2008 queryBuildDefinitionsByUri(final String[] uris) {
        final _BuildGroupQueryResult _result = webService.queryBuildDefinitionsByUri(uris);
        return new BuildGroupQueryResult2008(_result);
    }

    public BuildGroupQueryResult2008[] queryBuildGroups(final BuildGroupItemSpec2010[] specs) {
        final _BuildGroupItemSpec[] _specs =
            (_BuildGroupItemSpec[]) WrapperUtils.unwrap(_BuildGroupItemSpec.class, specs);
        final _BuildGroupQueryResult[] _results = webService.queryBuildGroups(_specs);
        return (BuildGroupQueryResult2008[]) WrapperUtils.wrap(BuildGroupQueryResult2008.class, _results);
    }

    public BuildQueryResult2008[] queryBuilds(final BuildDetailSpec2010[] specs) {
        final _BuildDetailSpec[] _specs = (_BuildDetailSpec[]) WrapperUtils.unwrap(_BuildDetailSpec.class, specs);
        final _BuildQueryResult2008[] _results = webService.queryBuilds(_specs);
        return (BuildQueryResult2008[]) WrapperUtils.wrap(BuildQueryResult2008.class, _results);
    }

    public BuildQueryResult2008 queryBuildsByUri(
        final String[] uris,
        final String[] informationTypes,
        final QueryOptions2010 options) {
        final _QueryOptions _options = options.getWebServiceObject();
        final _BuildQueryResult2008 _result = webService.queryBuildsByUri(uris, informationTypes, _options);
        return new BuildQueryResult2008(_result);
    }

    public BuildQueueQueryResult2008[] queryBuildQueue(final BuildQueueSpec2008[] specs) {
        final _BuildQueueSpec2008[] _specs =
            (_BuildQueueSpec2008[]) WrapperUtils.unwrap(_BuildQueueSpec2008.class, specs);
        final _BuildQueueQueryResult2008[] _results = webService.queryBuildQueue(_specs);
        return (BuildQueueQueryResult2008[]) WrapperUtils.wrap(BuildQueueQueryResult2008.class, _results);
    }

    public BuildQueueQueryResult2008 queryBuildQueueById(final int[] ids, final QueryOptions2010 options) {
        final _QueryOptions _options = options.getWebServiceObject();
        final _BuildQueueQueryResult2008 _result = webService.queryBuildQueueById(ids, _options);
        return new BuildQueueQueryResult2008(_result);
    }

    public QueuedBuild2008 queueBuild(final BuildRequest2008 buildRequest, final QueueOptions2010 options) {
        final _QueueOptions _options = options.getWebServiceObject();
        final _BuildRequest2008 _request = buildRequest.getWebServiceObject();
        final _QueuedBuild2008 _result = webService.queueBuild(_request, _options);
        return new QueuedBuild2008(_result);
    }

    public void stopBuilds(final String[] uris) {
        webService.stopBuilds(uris);
    }

    public BuildDetail2010[] updateBuilds(final BuildUpdateOptions2010[] updateOptions) {
        final _BuildUpdateOptions[] _updateOptions =
            (_BuildUpdateOptions[]) WrapperUtils.unwrap(_BuildUpdateOptions.class, updateOptions);
        final _BuildDetail[] _results = webService.updateBuilds(_updateOptions);
        return (BuildDetail2010[]) WrapperUtils.wrap(BuildDetail2010.class, _results);
    }

    public QueuedBuild2008[] updateQueuedBuilds(final QueuedBuildUpdateOptions2010[] updateOptions)

    {
        final _QueuedBuildUpdateOptions[] _updateOptions =
            (_QueuedBuildUpdateOptions[]) WrapperUtils.unwrap(_QueuedBuildUpdateOptions.class, updateOptions);
        final _QueuedBuild2008[] _results = webService.updateQueuedBuilds(_updateOptions);
        return (QueuedBuild2008[]) WrapperUtils.wrap(QueuedBuild2008.class, _results);
    }

    public BuildAgent2008[] addBuildAgents(final BuildAgent2008[] agents) {
        final _BuildAgent2008[] _agents = (_BuildAgent2008[]) WrapperUtils.unwrap(_BuildAgent2008.class, agents);
        final _BuildAgent2008[] _results = webService.addBuildAgents(_agents);
        return (BuildAgent2008[]) WrapperUtils.wrap(BuildAgent2008.class, _results);
    }

    public void deleteBuildAgents(final String[] uris) {
        webService.deleteBuildAgents(uris);
    }

    public void processChangeset(final int changesetId) {
        webService.processChangeset(changesetId);
    }

    public BuildAgent2008[] updateBuildAgents(final BuildAgent2008[] updates) {
        final _BuildAgent2008[] _updates = (_BuildAgent2008[]) WrapperUtils.unwrap(_BuildAgent2008.class, updates);
        final _BuildAgent2008[] _results = webService.updateBuildAgents(_updates);
        return (BuildAgent2008[]) WrapperUtils.wrap(BuildAgent2008.class, _results);
    }

    public BuildDefinition2010[] updateBuildDefinitions(final BuildDefinition2010[] updates) {
        final _BuildDefinition[] _updates = (_BuildDefinition[]) WrapperUtils.unwrap(_BuildDefinition.class, updates);
        final _BuildDefinition[] _results = webService.updateBuildDefinitions(_updates);
        return (BuildDefinition2010[]) WrapperUtils.wrap(BuildDefinition2010.class, _results);
    }

    public BuildInformationNode2010[] updateBuildInformation(final InformationChangeRequest2010[] changes) {
        final _InformationChangeRequest[] _changes =
            (_InformationChangeRequest[]) WrapperUtils.unwrap(_InformationChangeRequest.class, changes);
        final _BuildInformationNode[] _results = webService.updateBuildInformation(_changes);
        return (BuildInformationNode2010[]) WrapperUtils.wrap(BuildInformationNode2010.class, _results);
    }
}
