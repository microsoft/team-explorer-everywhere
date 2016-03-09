// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.build.internal.soapextensions;

import com.microsoft.tfs.core.internal.wrappers.WebServiceObjectWrapper;
import com.microsoft.tfs.core.internal.wrappers.WrapperUtils;

import ms.tfs.build.buildservice._03._BuildQueryResult;

public class BuildQueryResult2010 extends WebServiceObjectWrapper {
    public BuildQueryResult2010(final _BuildQueryResult value) {
        super(value);
    }

    public _BuildQueryResult getWebServiceObject() {
        return (_BuildQueryResult) webServiceObject;
    }

    public BuildDefinition2010[] getDefinitions() {
        return (BuildDefinition2010[]) WrapperUtils.wrap(
            BuildDefinition2010.class,
            getWebServiceObject().getDefinitions());
    }

    public BuildAgent2010[] getAgents() {
        return (BuildAgent2010[]) WrapperUtils.wrap(BuildAgent2010.class, getWebServiceObject().getAgents());
    }

    public BuildController2010[] getControllers() {
        return (BuildController2010[]) WrapperUtils.wrap(
            BuildController2010.class,
            getWebServiceObject().getControllers());
    }

    public BuildDetail2010[] getBuilds() {
        return (BuildDetail2010[]) WrapperUtils.wrap(BuildDetail2010.class, getWebServiceObject().getBuilds());
    }

    public BuildServiceHost2010[] getServiceHosts() {
        return (BuildServiceHost2010[]) WrapperUtils.wrap(
            BuildServiceHost2010.class,
            getWebServiceObject().getServiceHosts());
    }

    public Failure2010[] getInternalFailures() {
        return (Failure2010[]) WrapperUtils.wrap(Failure2010.class, getWebServiceObject().getFailures());
    }
}
