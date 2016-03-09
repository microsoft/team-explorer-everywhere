// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.build.internal.soapextensions;

import com.microsoft.tfs.core.clients.build.IFailure2010;
import com.microsoft.tfs.core.internal.wrappers.WebServiceObjectWrapper;

import ms.tfs.build.buildservice._03._Failure;

public class Failure2010 extends WebServiceObjectWrapper implements IFailure2010 {
    public Failure2010() {
        this(new _Failure());
    }

    public Failure2010(final _Failure value) {
        super(value);
    }

    public _Failure getWebServiceObject() {
        return (_Failure) webServiceObject;
    }

    @Override
    public String getCode() {
        return getWebServiceObject().getCode();
    }

    @Override
    public String getMessage() {
        return getWebServiceObject().getMessage();
    }

    public void setCode(final String value) {
        getWebServiceObject().setCode(value);
    }

    public void setMessage(final String value) {
        getWebServiceObject().setMessage(value);
    }
}
