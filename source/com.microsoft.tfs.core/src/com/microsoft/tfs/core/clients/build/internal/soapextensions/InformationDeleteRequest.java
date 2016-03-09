// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.build.internal.soapextensions;

import ms.tfs.build.buildservice._04._InformationDeleteRequest;

public class InformationDeleteRequest extends InformationChangeRequest {

    public InformationDeleteRequest() {
        super(new _InformationDeleteRequest());
    }

    @Override
    public _InformationDeleteRequest getWebServiceObject() {
        return (_InformationDeleteRequest) this.webServiceObject;
    }

    public int getNodeID() {
        return getWebServiceObject().getNodeId();
    }

    public void setNodeID(final int value) {
        getWebServiceObject().setNodeId(value);
    }
}
