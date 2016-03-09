// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.build;

import com.microsoft.tfs.core.TFSTeamProjectCollection;
import com.microsoft.tfs.core.clients.build.flags.QueryOptions;
import com.microsoft.tfs.core.clients.build.flags.QueueOptions;
import com.microsoft.tfs.core.clients.build.internal.soapextensions.BuildQueueQueryResult;
import com.microsoft.tfs.core.clients.build.internal.soapextensions.QueuedBuildUpdateOptions;
import com.microsoft.tfs.core.clients.build.internal.utils.BuildTypeConvertor;
import com.microsoft.tfs.core.internal.wrappers.WrapperUtils;

import ms.tfs.build.buildservice._04._BuildQueueQueryResult;
import ms.tfs.build.buildservice._04._BuildQueueServiceSoap;
import ms.tfs.build.buildservice._04._BuildQueueSpec;
import ms.tfs.build.buildservice._04._BuildRequest;
import ms.tfs.build.buildservice._04._QueryOptions;
import ms.tfs.build.buildservice._04._QueueOptions;
import ms.tfs.build.buildservice._04._QueuedBuildUpdateOptions;

public class BuildQueueWebService4 {
    private final _BuildQueueServiceSoap webService;
    private final IBuildServer buildServer;

    public BuildQueueWebService4(final TFSTeamProjectCollection tfs) {
        webService = (_BuildQueueServiceSoap) tfs.getWebService(_BuildQueueServiceSoap.class);
        buildServer = tfs.getBuildServer();
    }

    public BuildQueueQueryResult queryBuildsById(
        final int[] ids,
        final String[] informationTypes,
        final QueryOptions queryOptions) {
        final _QueryOptions _options = queryOptions.getWebServiceObject();
        final _BuildQueueQueryResult _result = webService.queryBuildsById(ids, informationTypes, _options);
        return new BuildQueueQueryResult(buildServer, _result);
    }

    public BuildQueueQueryResult updateBuilds(final QueuedBuildUpdateOptions[] updateOptions) {
        final _QueuedBuildUpdateOptions[] _options =
            (_QueuedBuildUpdateOptions[]) WrapperUtils.unwrap(_QueuedBuildUpdateOptions.class, updateOptions);
        final _BuildQueueQueryResult _result = webService.updateBuilds(_options);
        return new BuildQueueQueryResult(buildServer, _result);
    }

    public IQueuedBuildQueryResult[] queryBuilds(final IQueuedBuildSpec[] buildQueueSpecs) {
        final _BuildQueueSpec[] _specs =
            (_BuildQueueSpec[]) WrapperUtils.unwrap(_BuildQueueSpec.class, buildQueueSpecs);
        final _BuildQueueQueryResult[] _results = webService.queryBuilds(_specs);
        return BuildTypeConvertor.toBuildQueueQueryResults(buildServer, _results);
    }

    public IQueuedBuildQueryResult queueBuilds(final IBuildRequest[] buildRequests, final QueueOptions queryOptions) {
        final _QueueOptions _options = queryOptions.getWebServiceObject();
        final _BuildRequest[] _requests = (_BuildRequest[]) WrapperUtils.unwrap(_BuildRequest.class, buildRequests);
        final _BuildQueueQueryResult result = webService.queueBuilds(_requests, _options);
        return new BuildQueueQueryResult(buildServer, result);
    }

    public void cancelBuilds(final int[] ids) {
        webService.cancelBuilds(ids);
    }

    public IQueuedBuildQueryResult startBuildsNow(final int[] ids) {
        final _BuildQueueQueryResult _result = webService.startBuildsNow(ids);
        return new BuildQueueQueryResult(buildServer, _result);
    }
}
