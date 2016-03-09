// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.build.internal.soapextensions;

import com.microsoft.tfs.core.clients.build.flags.WorkspaceMappingDepth;
import com.microsoft.tfs.core.clients.build.flags.WorkspaceMappingType2010;
import com.microsoft.tfs.core.internal.wrappers.WebServiceObjectWrapper;

import ms.tfs.build.buildservice._03._WorkspaceMapping;

public class WorkspaceMapping2010 extends WebServiceObjectWrapper {
    private WorkspaceMapping2010() {
        this(new _WorkspaceMapping());
    }

    public WorkspaceMapping2010(final _WorkspaceMapping value) {
        super(value);
    }

    public WorkspaceMapping2010(final WorkspaceMapping mapping) {
        this();

        if (mapping.getDepth().equals(WorkspaceMappingDepth.FULL)) {
            setDepth(120);
        } else if (mapping.getDepth().equals(WorkspaceMappingDepth.ONE_LEVEL)) {
            setDepth(1);
        }

        setLocalItem(mapping.getLocalItem());
        setMappingType(TFS2010Helper.convert(mapping.getMappingType()));
        setServerItem(mapping.getServerItem());
    }

    public _WorkspaceMapping getWebServiceObject() {
        return (_WorkspaceMapping) webServiceObject;
    }

    public int getDepth() {
        return getWebServiceObject().getDepth();
    }

    public String getLocalItem() {
        return getWebServiceObject().getLocalItem();
    }

    public WorkspaceMappingType2010 getMappingType() {
        return WorkspaceMappingType2010.fromWebServiceObject(getWebServiceObject().getMappingType());
    }

    public String getServerItem() {
        return getWebServiceObject().getServerItem();
    }

    public void setDepth(final int value) {
        getWebServiceObject().setDepth(value);
    }

    public void setLocalItem(final String value) {
        getWebServiceObject().setLocalItem(value);
    }

    public void setMappingType(final WorkspaceMappingType2010 value) {
        getWebServiceObject().setMappingType(value.getWebServiceObject());
    }

    public void setServerItem(final String value) {
        getWebServiceObject().setServerItem(value);
    }
}
