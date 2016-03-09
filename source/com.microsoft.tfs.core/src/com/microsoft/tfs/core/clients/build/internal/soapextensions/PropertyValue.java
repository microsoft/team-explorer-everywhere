// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.build.internal.soapextensions;

import com.microsoft.tfs.core.internal.wrappers.WebServiceObjectWrapper;

import ms.tfs.build.buildservice._04._PropertyValue;

public class PropertyValue extends WebServiceObjectWrapper {
    private PropertyValue() {
        super(new _PropertyValue());
    }

    public PropertyValue(final String name, final Object value) {
        this();
        setPropertyName(name);
        setInternalValue(value);
    }

    public _PropertyValue getWebServiceObject() {
        return (_PropertyValue) this.webServiceObject;
    }

    public Object getInternalValue() {
        return getWebServiceObject().getVal();
    }

    public void setInternalValue(final Object value) {
        getWebServiceObject().setVal(value);
    }

    public String getPropertyName() {
        return getWebServiceObject().getPname();
    }

    public void setPropertyName(final String value) {
        getWebServiceObject().setPname(value);
    }
}
