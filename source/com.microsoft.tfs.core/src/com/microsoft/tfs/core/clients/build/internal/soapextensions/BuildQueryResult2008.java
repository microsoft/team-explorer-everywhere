// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.build.internal.soapextensions;

import com.microsoft.tfs.core.internal.wrappers.WebServiceObjectWrapper;
import com.microsoft.tfs.core.internal.wrappers.WrapperUtils;

import ms.tfs.build.buildservice._03._BuildQueryResult2008;

public class BuildQueryResult2008 extends WebServiceObjectWrapper {
    public BuildQueryResult2008(final _BuildQueryResult2008 webServiceObject) {
        super(webServiceObject);
    }

    public _BuildQueryResult2008 getWebServiceObject() {
        return (_BuildQueryResult2008) this.webServiceObject;
    }

    public BuildAgent2008[] getAgents() {
        return (BuildAgent2008[]) WrapperUtils.wrap(BuildAgent2008.class, getWebServiceObject().getAgents());
    }

    public BuildDetail2010[] getBuilds() {
        return (BuildDetail2010[]) WrapperUtils.wrap(BuildDetail2010.class, getWebServiceObject().getBuilds());
    }

    public BuildDefinition2010[] getDefinitions() {
        return (BuildDefinition2010[]) WrapperUtils.wrap(
            BuildDefinition2010.class,
            getWebServiceObject().getDefinitions());
    }

    public Failure2010[] getInternalFailures() {
        return (Failure2010[]) WrapperUtils.wrap(Failure2010.class, getWebServiceObject().getFailures());
    }
}
