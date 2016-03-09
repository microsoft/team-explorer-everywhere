// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.build;

import com.microsoft.tfs.core.TFSTeamProjectCollection;
import com.microsoft.tfs.core.clients.build.flags.QueryOptions2010;
import com.microsoft.tfs.core.clients.build.flags.QueueOptions2010;
import com.microsoft.tfs.core.clients.build.internal.soapextensions.BuildQueueQueryResult2010;
import com.microsoft.tfs.core.clients.build.internal.soapextensions.BuildQueueSpec2010;
import com.microsoft.tfs.core.clients.build.internal.soapextensions.BuildRequest2010;
import com.microsoft.tfs.core.clients.build.internal.soapextensions.QueuedBuild2010;
import com.microsoft.tfs.core.clients.build.internal.soapextensions.QueuedBuildUpdateOptions2010;
import com.microsoft.tfs.core.internal.wrappers.WrapperUtils;

import ms.tfs.build.buildservice._03._BuildQueueQueryResult;
import ms.tfs.build.buildservice._03._BuildQueueSpec;
import ms.tfs.build.buildservice._03._BuildQueueWebServiceSoap;
import ms.tfs.build.buildservice._03._BuildRequest;
import ms.tfs.build.buildservice._03._QueuedBuild;
import ms.tfs.build.buildservice._03._QueuedBuildUpdateOptions;

public class BuildQueueWebService {
    private final _BuildQueueWebServiceSoap webService;

    public BuildQueueWebService(final TFSTeamProjectCollection tfs) {
        webService = (_BuildQueueWebServiceSoap) tfs.getWebService(_BuildQueueWebServiceSoap.class);
    }

    public BuildQueueQueryResult2010 queryBuildsById(final int[] ids, final QueryOptions2010 options) {
        final _BuildQueueQueryResult _result = webService.queryBuildsById(ids, options.getWebServiceObject());
        return new BuildQueueQueryResult2010(_result);
    }

    public QueuedBuild2010[] queueBuilds(final BuildRequest2010[] requests, final QueueOptions2010 options) {
        final _BuildRequest[] _requests = (_BuildRequest[]) WrapperUtils.unwrap(_BuildRequest.class, requests);
        final _QueuedBuild[] _results = webService.queueBuilds(_requests, options.getWebServiceObject());
        return (QueuedBuild2010[]) WrapperUtils.wrap(QueuedBuild2010.class, _results);
    }

    public QueuedBuild2010[] updateBuilds(final QueuedBuildUpdateOptions2010[] updates) {
        final _QueuedBuildUpdateOptions[] _updates =
            (_QueuedBuildUpdateOptions[]) WrapperUtils.unwrap(_QueuedBuildUpdateOptions.class, updates);
        final _QueuedBuild[] _results = webService.updateBuilds(_updates);
        return (QueuedBuild2010[]) WrapperUtils.wrap(QueuedBuild2010.class, _results);
    }

    public void cancelBuilds(final int[] ids) {
        webService.cancelBuilds(ids);
    }

    public BuildQueueQueryResult2010[] queryBuilds(final BuildQueueSpec2010[] specs) {
        final _BuildQueueSpec[] _specs = (_BuildQueueSpec[]) WrapperUtils.unwrap(_BuildQueueSpec.class, specs);
        final _BuildQueueQueryResult[] _results = webService.queryBuilds(_specs);
        return (BuildQueueQueryResult2010[]) WrapperUtils.wrap(BuildQueueQueryResult2010.class, _results);
    }
}
