// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.build.internal.soapextensions;

import com.microsoft.tfs.core.internal.wrappers.WebServiceObjectWrapper;

import ms.tfs.build.buildservice._04._NameValueField;

public class NameValueField extends WebServiceObjectWrapper {

    public NameValueField(final Object webServiceObject) {
        super(webServiceObject);
    }

    public NameValueField(final String name, final String value) {
        this(new _NameValueField(name, value));
    }

    public _NameValueField getWebServiceObject() {
        return (_NameValueField) this.webServiceObject;
    }

    public String getName() {
        return getWebServiceObject().getName();
    }

    public void setName(final String name) {
        getWebServiceObject().setName(name);
    }

    public String getValue() {
        return getWebServiceObject().getValue();
    }

    public void setValue(final String value) {
        getWebServiceObject().setValue(value);
    }
}
