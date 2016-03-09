// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.build.internal.soapextensions;

import ms.tfs.build.buildservice._03._InformationDeleteRequest;

public class InformationDeleteRequest2010 extends InformationChangeRequest2010 {
    public InformationDeleteRequest2010() {
        this(new _InformationDeleteRequest());
    }

    public InformationDeleteRequest2010(final _InformationDeleteRequest value) {
        super(value);
    }

    @Override
    public _InformationDeleteRequest getWebServiceObject() {
        return (_InformationDeleteRequest) webServiceObject;
    }

    public int getNodeID() {
        return getWebServiceObject().getNodeId();
    }

    public void setNodeID(final int value) {
        getWebServiceObject().setNodeId(value);
    }
}
