// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.build.internal.soapextensions;

import com.microsoft.tfs.core.internal.wrappers.WebServiceObjectWrapper;

import ms.tfs.build.buildservice._03._BuildAgentSpec;

public class BuildAgentSpec2010 extends WebServiceObjectWrapper {
    private BuildAgentSpec2010() {
        this(new _BuildAgentSpec());
    }

    public BuildAgentSpec2010(final _BuildAgentSpec value) {
        super(value);
    }

    public BuildAgentSpec2010(final BuildAgentSpec spec) {
        this();

        setControllerName(spec.getControllerName());
        setName(spec.getName());
        setServiceHostName(spec.getServiceHostName());

        if (spec.getTags() != null) {
            getWebServiceObject().setTags(spec.getTags());
        }
    }

    public _BuildAgentSpec getWebServiceObject() {
        return (_BuildAgentSpec) webServiceObject;
    }

    public String getControllerName() {
        return getWebServiceObject().getControllerName();
    }

    public String getName() {
        return getWebServiceObject().getName();
    }

    public String getServiceHostName() {
        return getWebServiceObject().getServiceHostName();
    }

    public void setControllerName(final String value) {
        getWebServiceObject().setControllerName(value);
    }

    public void setName(final String value) {
        getWebServiceObject().setName(value);
    }

    public void setServiceHostName(final String value) {
        getWebServiceObject().setServiceHostName(value);
    }
}
