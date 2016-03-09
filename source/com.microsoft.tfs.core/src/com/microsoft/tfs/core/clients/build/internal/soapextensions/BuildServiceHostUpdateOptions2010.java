// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.build.internal.soapextensions;

import com.microsoft.tfs.core.clients.build.flags.BuildServiceHostUpdate2010;
import com.microsoft.tfs.core.internal.wrappers.WebServiceObjectWrapper;

import ms.tfs.build.buildservice._03._BuildServiceHostUpdateOptions;

public class BuildServiceHostUpdateOptions2010 extends WebServiceObjectWrapper {
    private BuildServiceHostUpdateOptions2010() {
        this(new _BuildServiceHostUpdateOptions());
    }

    public BuildServiceHostUpdateOptions2010(final _BuildServiceHostUpdateOptions value) {
        super(value);
    }

    public BuildServiceHostUpdateOptions2010(final BuildServiceHostUpdateOptions updateOptions) {
        this();

        final _BuildServiceHostUpdateOptions o = getWebServiceObject();
        o.setBaseUrl(updateOptions.getBaseURL());
        o.setFields(TFS2010Helper.convert(updateOptions.getFields()).getWebServiceObject());
        o.setName(updateOptions.getName());
        o.setRequireClientCertificates(updateOptions.isRequireClientCertificates());
        o.setUri(updateOptions.getURI());
    }

    public _BuildServiceHostUpdateOptions getWebServiceObject() {
        return (_BuildServiceHostUpdateOptions) webServiceObject;
    }

    public String getBaseURL() {
        return getWebServiceObject().getBaseUrl();
    }

    public BuildServiceHostUpdate2010 getFields() {
        return BuildServiceHostUpdate2010.fromWebServiceObject(getWebServiceObject().getFields());
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

    public void setFields(final BuildServiceHostUpdate2010 value) {
        getWebServiceObject().setFields(value.getWebServiceObject());
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
