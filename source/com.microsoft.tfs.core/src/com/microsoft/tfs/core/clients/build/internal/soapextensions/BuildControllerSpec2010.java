// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.build.internal.soapextensions;

import com.microsoft.tfs.core.internal.wrappers.WebServiceObjectWrapper;

import ms.tfs.build.buildservice._03._BuildControllerSpec;

public class BuildControllerSpec2010 extends WebServiceObjectWrapper {
    private BuildControllerSpec2010() {
        this(new _BuildControllerSpec());
    }

    public BuildControllerSpec2010(final _BuildControllerSpec value) {
        super(value);
    }

    public BuildControllerSpec2010(final BuildControllerSpec spec) {
        this();

        setIncludeAgents(spec.isIncludeAgents());
        setName(spec.getName());
        setServiceHostName(spec.getServiceHostName());
    }

    public _BuildControllerSpec getWebServiceObject() {
        return (_BuildControllerSpec) webServiceObject;
    }

    public boolean isIncludeAgents() {
        return getWebServiceObject().isIncludeAgents();
    }

    public String getName() {
        return getWebServiceObject().getName();
    }

    public String getServiceHostName() {
        return getWebServiceObject().getServiceHostName();
    }

    public void setIncludeAgents(final boolean value) {
        getWebServiceObject().setIncludeAgents(value);
    }

    public void setName(final String value) {
        getWebServiceObject().setName(value);
    }

    public void setServiceHostName(final String value) {
        getWebServiceObject().setServiceHostName(value);
    }
}
