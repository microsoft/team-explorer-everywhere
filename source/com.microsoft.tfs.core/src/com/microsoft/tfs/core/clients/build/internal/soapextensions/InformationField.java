// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.build.internal.soapextensions;

import com.microsoft.tfs.core.internal.wrappers.WebServiceObjectWrapper;

import ms.tfs.build.buildservice._04._InformationField;

public class InformationField extends WebServiceObjectWrapper {
    private InformationField() {
        this(new _InformationField());
    }

    public InformationField(final _InformationField webServiceObject) {
        super(webServiceObject);
    }

    public InformationField(final String name, final String value) {
        this();
        getWebServiceObject().setName(name);
        getWebServiceObject().setValue(value);
    }

    public _InformationField getWebServiceObject() {
        return (_InformationField) this.webServiceObject;
    }

    public String getName() {
        return getWebServiceObject().getName();
    }

    public void setName(final String value) {
        getWebServiceObject().setName(value);
    }

    public String getValue() {
        return getWebServiceObject().getValue();
    }

    public void setValue(final String value) {
        getWebServiceObject().setValue(value);
    }
}
