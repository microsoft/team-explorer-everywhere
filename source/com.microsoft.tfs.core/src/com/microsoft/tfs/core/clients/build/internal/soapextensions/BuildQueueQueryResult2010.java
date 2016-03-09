// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.build.internal.soapextensions;

import com.microsoft.tfs.core.internal.wrappers.WebServiceObjectWrapper;
import com.microsoft.tfs.core.internal.wrappers.WrapperUtils;

import ms.tfs.build.buildservice._03._BuildDefinition;
import ms.tfs.build.buildservice._03._BuildQueueQueryResult;
import ms.tfs.build.buildservice._03._QueuedBuild;

public class BuildQueueQueryResult2010 extends WebServiceObjectWrapper {
    public BuildQueueQueryResult2010(final _BuildQueueQueryResult value) {
        super(value);
    }

    public _BuildQueueQueryResult getWebServiceObject() {
        return (_BuildQueueQueryResult) webServiceObject;
    }

    public BuildAgent2010[] getAgents() {
        return (BuildAgent2010[]) WrapperUtils.wrap(BuildAgent2010.class, getWebServiceObject().getAgents());
    }

    public BuildController2010[] getControllers() {
        return (BuildController2010[]) WrapperUtils.wrap(
            BuildController2010.class,
            getWebServiceObject().getControllers());
    }

    public QueuedBuild2010[] getBuilds() {
        return (QueuedBuild2010[]) WrapperUtils.wrap(QueuedBuild2010.class, getWebServiceObject().getBuilds());
    }

    public BuildServiceHost2010[] getServiceHosts() {
        return (BuildServiceHost2010[]) WrapperUtils.wrap(
            BuildServiceHost2010.class,
            getWebServiceObject().getServiceHosts());
    }

    public BuildDefinition2010[] getDefinitions() {
        return (BuildDefinition2010[]) WrapperUtils.wrap(
            BuildDefinition2010.class,
            getWebServiceObject().getDefinitions());
    }

    public Failure2010[] getInternalFailures() {
        return (Failure2010[]) WrapperUtils.wrap(Failure2010.class, getWebServiceObject().getFailures());
    }

    public void setBuilds(final QueuedBuild2010[] value) {
        getWebServiceObject().setBuilds((_QueuedBuild[]) WrapperUtils.unwrap(_QueuedBuild.class, value));
    }

    public void setDefinitions(final BuildDefinition2010[] value) {
        getWebServiceObject().setDefinitions((_BuildDefinition[]) WrapperUtils.unwrap(_BuildDefinition.class, value));
    }
}
