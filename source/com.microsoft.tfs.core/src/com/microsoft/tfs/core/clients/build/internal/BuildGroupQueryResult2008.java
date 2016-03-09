// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.build.internal;

import com.microsoft.tfs.core.clients.build.internal.soapextensions.BuildAgent2008;
import com.microsoft.tfs.core.clients.build.internal.soapextensions.BuildDefinition2010;
import com.microsoft.tfs.core.internal.wrappers.WebServiceObjectWrapper;
import com.microsoft.tfs.core.internal.wrappers.WrapperUtils;

import ms.tfs.build.buildservice._03._BuildAgent2008;
import ms.tfs.build.buildservice._03._BuildDefinition;
import ms.tfs.build.buildservice._03._BuildGroupQueryResult;

public class BuildGroupQueryResult2008 extends WebServiceObjectWrapper {
    public BuildGroupQueryResult2008(final _BuildGroupQueryResult webServiceObject) {
        super(webServiceObject);
    }

    public _BuildGroupQueryResult getWebServiceObject() {
        return (_BuildGroupQueryResult) this.webServiceObject;
    }

    public BuildAgent2008[] getAgents() {
        final _BuildAgent2008[] _agents = getWebServiceObject().getAgents();
        return (BuildAgent2008[]) WrapperUtils.wrap(BuildAgent2008.class, _agents);
    }

    public BuildDefinition2010[] getDefinitions() {
        final _BuildDefinition[] _definitions = getWebServiceObject().getDefinitions();
        return (BuildDefinition2010[]) WrapperUtils.wrap(BuildDefinition2010.class, _definitions);
    }
}
