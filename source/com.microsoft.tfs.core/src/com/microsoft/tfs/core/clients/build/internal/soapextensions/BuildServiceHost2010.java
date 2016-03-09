// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.build.internal.soapextensions;

import com.microsoft.tfs.core.internal.wrappers.WebServiceObjectWrapper;

import ms.tfs.build.buildservice._03._BuildServiceHost;

public class BuildServiceHost2010 extends WebServiceObjectWrapper {
    private BuildServiceHost2010() {
        this(new _BuildServiceHost());
    }

    public BuildServiceHost2010(final _BuildServiceHost value) {
        super(value);
    }

    public BuildServiceHost2010(final BuildServiceHost serviceHost) {
        this();

        setBaseURL(serviceHost.getBaseURL());
        setName(serviceHost.getName());
        setRequireClientCertificates(serviceHost.isRequireClientCertificates());
        setURI(serviceHost.getURI());
    }

    public _BuildServiceHost getWebServiceObject() {
        return (_BuildServiceHost) webServiceObject;
    }

    public String getBaseURL() {
        return getWebServiceObject().getBaseUrl();
    }

    public boolean isIsVirtual() {
        return getWebServiceObject().isIsVirtual();
    }

    public String getName() {
        return getWebServiceObject().getName();
    }

    public boolean isRequireClientCertificates() {
        return getWebServiceObject().isRequireClientCertificates();
    }

    public String getURI() {
        return getWebServiceObject().getUri();
    }

    public void setBaseURL(final String value) {
        getWebServiceObject().setBaseUrl(value);
    }

    public void setIsVirtual(final boolean value) {
        getWebServiceObject().setIsVirtual(value);
    }

    public void setName(final String value) {
        getWebServiceObject().setName(value);
    }

    public void setRequireClientCertificates(final boolean value) {
        getWebServiceObject().setRequireClientCertificates(value);
    }

    public void setURI(final String value) {
        getWebServiceObject().setUri(value);
    }
}
