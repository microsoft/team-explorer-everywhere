// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.build.internal.soapextensions;

import com.microsoft.tfs.core.internal.wrappers.WrapperUtils;

import ms.tfs.build.buildservice._04._InformationAddRequest;
import ms.tfs.build.buildservice._04._InformationField;

public class InformationAddRequest extends InformationChangeRequest {
    private BuildInformationNode node;

    protected InformationAddRequest() {
        super(new _InformationAddRequest());
    }

    @Override
    public _InformationAddRequest getWebServiceObject() {
        return (_InformationAddRequest) this.webServiceObject;
    }

    /**
     * The fields to associate with the node.
     *
     *
     * @return
     */
    public InformationField[] getFields() {
        return (InformationField[]) WrapperUtils.wrap(InformationField.class, getWebServiceObject().getFields());
    }

    public void setFields(final InformationField[] value) {
        getWebServiceObject().setFields((_InformationField[]) WrapperUtils.unwrap(_InformationField.class, value));
    }

    /**
     * Gets or sets the node ID. This value must be negative.
     *
     *
     * @return
     */
    public int getNodeID() {
        return getWebServiceObject().getNodeId();
    }

    public void setNodeID(final int value) {
        getWebServiceObject().setNodeId(value);
    }

    /**
     * Gets or sets the type of information node.
     *
     *
     * @return
     */
    public String getNodeType() {
        return getWebServiceObject().getNodeType();
    }

    public void setNodeType(final String value) {
        getWebServiceObject().setNodeType(value);
    }

    /**
     * Gets or sets the parent node ID. This must reference a node being added
     * in the same request or an existing node for the target build.
     *
     *
     * @return
     */
    public int getParentID() {
        return getWebServiceObject().getParentId();
    }

    public void setParentID(final int value) {
        getWebServiceObject().setParentId(value);
    }

    public BuildInformationNode getNode() {
        return node;
    }

    public void setNode(final BuildInformationNode value) {
        node = value;
    }
}
