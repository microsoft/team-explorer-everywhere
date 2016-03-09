// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.build.internal.soapextensions;

import com.microsoft.tfs.core.clients.build.flags.InformationEditOptions;
import com.microsoft.tfs.core.internal.wrappers.WrapperUtils;

import ms.tfs.build.buildservice._04._InformationEditRequest;
import ms.tfs.build.buildservice._04._InformationField;

public class InformationEditRequest extends InformationChangeRequest {
    public InformationEditRequest() {
        super(new _InformationEditRequest());
    }

    @Override
    public _InformationEditRequest getWebServiceObject() {
        return (_InformationEditRequest) this.webServiceObject;
    }

    public InformationField[] getFields() {
        return (InformationField[]) WrapperUtils.wrap(InformationField.class, getWebServiceObject().getFields());
    }

    public void setFields(final InformationField[] value) {
        getWebServiceObject().setFields((_InformationField[]) WrapperUtils.unwrap(_InformationField.class, value));
    }

    public int getNodeID() {
        return getWebServiceObject().getNodeId();
    }

    public void setNodeID(final int value) {
        getWebServiceObject().setNodeId(value);
    }

    public InformationEditOptions getOptions() {
        return InformationEditOptions.fromWebServiceObject(getWebServiceObject().getOptions());
    }

    public void setOptions(final InformationEditOptions value) {
        getWebServiceObject().setOptions(value.getWebServiceObject());
    }
}
