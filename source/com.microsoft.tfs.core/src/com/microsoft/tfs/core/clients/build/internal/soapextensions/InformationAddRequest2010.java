// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.build.internal.soapextensions;

import com.microsoft.tfs.core.internal.wrappers.WrapperUtils;

import ms.tfs.build.buildservice._03._InformationAddRequest;
import ms.tfs.build.buildservice._03._InformationField;

public class InformationAddRequest2010 extends InformationChangeRequest2010 {
    public InformationAddRequest2010() {
        this(new _InformationAddRequest());
    }

    public InformationAddRequest2010(final _InformationAddRequest value) {
        super(value);
    }

    @Override
    public _InformationAddRequest getWebServiceObject() {
        return (_InformationAddRequest) webServiceObject;
    }

    public InformationField2010[] getFields() {
        return (InformationField2010[]) WrapperUtils.wrap(
            InformationField2010.class,
            getWebServiceObject().getFields());
    }

    public int getNodeID() {
        return getWebServiceObject().getNodeId();
    }

    public String getNodeType() {
        return getWebServiceObject().getNodeType();
    }

    public int getParentID() {
        return getWebServiceObject().getParentId();
    }

    public void setFields(final InformationField2010[] value) {
        getWebServiceObject().setFields((_InformationField[]) WrapperUtils.unwrap(_InformationField.class, value));
    }

    public void setNodeID(final int value) {
        getWebServiceObject().setNodeId(value);
    }

    public void setNodeType(final String value) {
        getWebServiceObject().setNodeType(value);
    }

    public void setParentID(final int value) {
        getWebServiceObject().setParentId(value);
    }
}
