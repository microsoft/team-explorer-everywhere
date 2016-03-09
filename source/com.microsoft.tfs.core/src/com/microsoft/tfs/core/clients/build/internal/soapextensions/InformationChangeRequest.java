// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.build.internal.soapextensions;

import com.microsoft.tfs.core.internal.wrappers.WebServiceObjectWrapper;

import ms.tfs.build.buildservice._04._InformationChangeRequest;

public class InformationChangeRequest extends WebServiceObjectWrapper {
    protected InformationChangeRequest(final _InformationChangeRequest request) {
        super(request);
    }

    public _InformationChangeRequest getWebServiceObject() {
        return (_InformationChangeRequest) this.webServiceObject;
    }

    public String getBuildURI() {
        return getWebServiceObject().getBuildUri();
    }

    public void setBuildURI(final String value) {
        getWebServiceObject().setBuildUri(value);
    }
}
