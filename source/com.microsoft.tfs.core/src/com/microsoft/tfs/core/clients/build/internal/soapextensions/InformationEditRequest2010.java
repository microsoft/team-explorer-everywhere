// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.build.internal.soapextensions;

import com.microsoft.tfs.core.clients.build.flags.InformationEditOptions2010;
import com.microsoft.tfs.core.internal.wrappers.WrapperUtils;

import ms.tfs.build.buildservice._03._InformationEditRequest;
import ms.tfs.build.buildservice._03._InformationField;

public class InformationEditRequest2010 extends InformationChangeRequest2010 {
    public InformationEditRequest2010() {
        this(new _InformationEditRequest());
    }

    public InformationEditRequest2010(final _InformationEditRequest value) {
        super(value);
    }

    @Override
    public _InformationEditRequest getWebServiceObject() {
        return (_InformationEditRequest) webServiceObject;
    }

    public InformationField2010[] getFields() {
        return (InformationField2010[]) WrapperUtils.wrap(
            InformationField2010.class,
            getWebServiceObject().getFields());
    }

    public int getNodeID() {
        return getWebServiceObject().getNodeId();
    }

    public InformationEditOptions2010 getOptions() {
        return InformationEditOptions2010.fromWebServiceObject(getWebServiceObject().getOptions());
    }

    public void setFields(final InformationField2010[] value) {
        getWebServiceObject().setFields((_InformationField[]) WrapperUtils.unwrap(_InformationField.class, value));
    }

    public void setNodeID(final int value) {
        getWebServiceObject().setNodeId(value);
    }

    public void setOptions(final InformationEditOptions2010 value) {
        InformationEditOptions2010.fromWebServiceObject(value.getWebServiceObject());
    }
}
