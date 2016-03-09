// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.build.internal.soapextensions;

import java.util.Calendar;

import com.microsoft.tfs.core.internal.wrappers.WebServiceObjectWrapper;
import com.microsoft.tfs.core.internal.wrappers.WrapperUtils;

import ms.tfs.build.buildservice._03._WorkspaceMapping;
import ms.tfs.build.buildservice._03._WorkspaceTemplate;

public class WorkspaceTemplate2010 extends WebServiceObjectWrapper {
    private WorkspaceTemplate2010() {
        this(new _WorkspaceTemplate());
    }

    public WorkspaceTemplate2010(final _WorkspaceTemplate value) {
        super(value);
    }

    public WorkspaceTemplate2010(final WorkspaceTemplate template) {
        this();

        setDefinitionURI(template.getDefinitionURI());
        setInternalMappings(TFS2010Helper.convert(template.getInternalMappings()));
        setLastModifiedBy(template.getLastModifiedBy());
        setLastModifiedDate(template.getLastModifiedDate());
    }

    public _WorkspaceTemplate getWebServiceObject() {
        return (_WorkspaceTemplate) webServiceObject;
    }

    public String getDefinitionURI() {
        return getWebServiceObject().getDefinitionUri();
    }

    public WorkspaceMapping2010[] getInternalMappings() {
        return (WorkspaceMapping2010[]) WrapperUtils.wrap(
            WorkspaceMapping2010.class,
            getWebServiceObject().getMappings());
    }

    public String getLastModifiedBy() {
        return getWebServiceObject().getLastModifiedBy();
    }

    public Calendar getLastModifiedDate() {
        return getWebServiceObject().getLastModifiedDate();
    }

    public void setDefinitionURI(final String value) {
        getWebServiceObject().setDefinitionUri(value);
    }

    public void setInternalMappings(final WorkspaceMapping2010[] value) {
        getWebServiceObject().setMappings((_WorkspaceMapping[]) WrapperUtils.unwrap(_WorkspaceMapping.class, value));
    }

    public void setLastModifiedBy(final String value) {
        getWebServiceObject().setLastModifiedBy(value);
    }

    public void setLastModifiedDate(final Calendar value) {
        getWebServiceObject().setLastModifiedDate(value);
    }
}
