// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.build.internal.soapextensions;

import java.util.Calendar;

import com.microsoft.tfs.core.internal.wrappers.WebServiceObjectWrapper;
import com.microsoft.tfs.core.internal.wrappers.WrapperUtils;

import ms.tfs.build.buildservice._03._BuildInformationNode;

public class BuildInformationNode2010 extends WebServiceObjectWrapper {
    public BuildInformationNode2010(final _BuildInformationNode value) {
        super(value);
    }

    public _BuildInformationNode getWebServiceObject() {
        return (_BuildInformationNode) webServiceObject;
    }

    public InformationField2010[] getInternalFields() {
        return (InformationField2010[]) WrapperUtils.wrap(
            InformationField2010.class,
            getWebServiceObject().getFields());
    }

    public String getLastModifiedBy() {
        return getWebServiceObject().getLastModifiedBy();
    }

    public Calendar getLastModifiedDate() {
        return getWebServiceObject().getLastModifiedDate();
    }

    public int getNodeID() {
        return getWebServiceObject().getNodeId();
    }

    public int getParentID() {
        return getWebServiceObject().getParentId();
    }

    public String getType() {
        return getWebServiceObject().getType();
    }

    public void setNodeID(final int value) {
        getWebServiceObject().setNodeId(value);
    }

    public void setParentID(final int value) {
        getWebServiceObject().setParentId(value);
    }

    public void getType(final String value) {
        getWebServiceObject().setType(value);
    }
}
