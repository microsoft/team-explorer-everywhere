// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.build.internal.soapextensions;

import com.microsoft.tfs.core.clients.build.IWorkspaceMapping;
import com.microsoft.tfs.core.clients.build.flags.WorkspaceMappingDepth;
import com.microsoft.tfs.core.clients.build.soapextensions.WorkspaceMappingType;
import com.microsoft.tfs.core.internal.wrappers.WebServiceObjectWrapper;

import ms.tfs.build.buildservice._04._WorkspaceMapping;

public class WorkspaceMapping extends WebServiceObjectWrapper implements IWorkspaceMapping {
    public WorkspaceMapping() {
        this(new _WorkspaceMapping());

        getWebServiceObject().setLocalItem(null);
        getWebServiceObject().setServerItem(null);
        getWebServiceObject().setMappingType(WorkspaceMappingType.MAP.getWebServiceObject());
        getWebServiceObject().setDepth(WorkspaceMappingDepth.FULL.getValue());
    }

    public WorkspaceMapping(final _WorkspaceMapping webServiceObject) {
        super(webServiceObject);
    }

    public WorkspaceMapping(final WorkspaceMapping2010 mapping) {
        this();
        getWebServiceObject().setDepth(mapping.getDepth());
        getWebServiceObject().setLocalItem(mapping.getLocalItem());
        getWebServiceObject().setMappingType(TFS2010Helper.convert(mapping.getMappingType()).getWebServiceObject());
        getWebServiceObject().setServerItem(mapping.getServerItem());
    }

    public _WorkspaceMapping getWebServiceObject() {
        return (_WorkspaceMapping) this.webServiceObject;
    }

    @Override
    public String getLocalItem() {
        return getWebServiceObject().getLocalItem();
    }

    @Override
    public void setLocalItem(final String value) {
        getWebServiceObject().setLocalItem(value);
    }

    @Override
    public WorkspaceMappingType getMappingType() {
        return WorkspaceMappingType.fromWebServiceObject(getWebServiceObject().getMappingType());
    }

    @Override
    public void setMappingType(final WorkspaceMappingType value) {
        getWebServiceObject().setMappingType(value.getWebServiceObject());
    }

    @Override
    public String getServerItem() {
        return getWebServiceObject().getServerItem();
    }

    @Override
    public void setServerItem(final String value) {
        getWebServiceObject().setServerItem(value);
    }

    @Override
    public WorkspaceMappingDepth getDepth() {
        return WorkspaceMappingDepth.fromValue(getWebServiceObject().getDepth());
    }

    @Override
    public void setDepth(final WorkspaceMappingDepth value) {
        getWebServiceObject().setDepth(value.getValue());
    }
}
