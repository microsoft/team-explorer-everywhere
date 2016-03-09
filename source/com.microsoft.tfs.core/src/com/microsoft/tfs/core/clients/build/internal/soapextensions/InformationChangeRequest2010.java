// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.build.internal.soapextensions;

import com.microsoft.tfs.core.internal.wrappers.WebServiceObjectWrapper;

import ms.tfs.build.buildservice._03._InformationChangeRequest;

public class InformationChangeRequest2010 extends WebServiceObjectWrapper {
    public InformationChangeRequest2010(final _InformationChangeRequest value) {
        super(value);
    }

    public _InformationChangeRequest getWebServiceObject() {
        return (_InformationChangeRequest) webServiceObject;
    }

    public String getBuildURI() {
        return getWebServiceObject().getBuildUri();
    }

    public void setBuildURI(final String value) {
        getWebServiceObject().setBuildUri(value);
    }
}
