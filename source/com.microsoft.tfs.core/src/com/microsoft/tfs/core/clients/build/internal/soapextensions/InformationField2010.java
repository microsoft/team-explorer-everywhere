// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.build.internal.soapextensions;

import com.microsoft.tfs.core.internal.wrappers.WebServiceObjectWrapper;

import ms.tfs.build.buildservice._03._InformationField;

public class InformationField2010 extends WebServiceObjectWrapper {
    private InformationField2010() {
        this(new _InformationField());
    }

    public InformationField2010(final _InformationField value) {
        super(value);
    }

    public InformationField2010(final String name, final String value) {
        this();
        setName(name);
        setValue(value);
    }

    public _InformationField getWebServiceObject() {
        return (_InformationField) webServiceObject;
    }

    public String getName() {
        return getWebServiceObject().getName();
    }

    public String getValue() {
        return getWebServiceObject().getValue();
    }

    public void setName(final String value) {
        getWebServiceObject().setName(value);
    }

    public void setValue(final String value) {
        getWebServiceObject().setValue(value);
    }
}
