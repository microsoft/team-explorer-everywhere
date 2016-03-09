// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.build.internal.soapextensions;

import com.microsoft.tfs.core.clients.build.IFailure;
import com.microsoft.tfs.core.internal.wrappers.WebServiceObjectWrapper;

import ms.tfs.build.buildservice._04._Failure;

public class Failure extends WebServiceObjectWrapper implements IFailure {
    private Failure() {
        super(new _Failure());
    }

    public Failure(final _Failure webServiceObject) {
        super(webServiceObject);
    }

    public Failure(final Failure2010 failure) {
        this();
        getWebServiceObject().setCode(failure.getCode());
        getWebServiceObject().setMessage(failure.getMessage());
    }

    public _Failure getWebServiceObject() {
        return (_Failure) this.webServiceObject;
    }

    /**
     * Gets or sets a code. Typically this is the name of the exception.
     * {@inheritDoc}
     */
    @Override
    public String getCode() {
        return getWebServiceObject().getCode();
    }

    public void setCode(final String value) {
        getWebServiceObject().setCode(value);
    }

    /**
     * Gets or sets the message. Typically this is the message of the exception.
     * {@inheritDoc}
     */
    @Override
    public String getMessage() {
        return getWebServiceObject().getMessage();
    }

    public void setMessage(final String value) {
        getWebServiceObject().setMessage(value);
    }
}
