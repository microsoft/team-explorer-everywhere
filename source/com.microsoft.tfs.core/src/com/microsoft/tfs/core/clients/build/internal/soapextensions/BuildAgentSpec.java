// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.build.internal.soapextensions;

import com.microsoft.tfs.core.clients.build.BuildConstants;
import com.microsoft.tfs.core.clients.build.IBuildAgentSpec;
import com.microsoft.tfs.core.internal.wrappers.WebServiceObjectWrapper;
import com.microsoft.tfs.util.StringUtil;

import ms.tfs.build.buildservice._04._BuildAgentSpec;

public class BuildAgentSpec extends WebServiceObjectWrapper implements IBuildAgentSpec {
    private BuildAgentSpec() {
        super(new _BuildAgentSpec());
    }

    public BuildAgentSpec(final String name, final String serviceHostName) {
        this(name, serviceHostName, null);
    }

    public BuildAgentSpec(final String name, final String serviceHostName, final String[] tags) {
        this(name, serviceHostName, null, tags);
    }

    public BuildAgentSpec(
        final String name,
        final String serviceHostName,
        final String[] propertyNameFilters,
        final String[] tags) {
        this();
        setName(StringUtil.isNullOrEmpty(name) ? BuildConstants.STAR : name);
        setServiceHostName(StringUtil.isNullOrEmpty(serviceHostName) ? BuildConstants.STAR : serviceHostName);

        if (propertyNameFilters != null) {
            setPropertyNameFilters(propertyNameFilters);
        }

        if (tags != null) {
            setTags(tags);
        }

        // Default to any controller.
        setControllerName(BuildConstants.STAR);
    }

    public _BuildAgentSpec getWebServiceObject() {
        return (_BuildAgentSpec) this.webServiceObject;
    }

    public String getControllerName() {
        return getWebServiceObject().getControllerName();
    }

    public void setControllerName(final String value) {
        getWebServiceObject().setControllerName(value);
    }

    @Override
    public String getName() {
        return getWebServiceObject().getName();
    }

    @Override
    public void setName(final String value) {
        getWebServiceObject().setName(value);
    }

    @Override
    public String getServiceHostName() {
        return getWebServiceObject().getServiceHostName();
    }

    @Override
    public void setServiceHostName(final String value) {
        getWebServiceObject().setServiceHostName(value);
    }

    @Override
    public String[] getTags() {
        return getWebServiceObject().getTags();
    }

    @Override
    public void setTags(final String[] value) {
        getWebServiceObject().setTags(value);
    }

    @Override
    public String[] getPropertyNameFilters() {
        return getWebServiceObject().getPropertyNameFilters();
    }

    @Override
    public void setPropertyNameFilters(final String[] value) {
        getWebServiceObject().setTags(value);
    }
}
