// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.build.internal.soapextensions;

import com.microsoft.tfs.core.clients.build.BuildConstants;
import com.microsoft.tfs.core.clients.build.IBuildControllerSpec;
import com.microsoft.tfs.core.internal.wrappers.WebServiceObjectWrapper;
import com.microsoft.tfs.util.StringUtil;

import ms.tfs.build.buildservice._04._BuildControllerSpec;

public class BuildControllerSpec extends WebServiceObjectWrapper implements IBuildControllerSpec {
    private BuildControllerSpec() {
        super(new _BuildControllerSpec());
    }

    public BuildControllerSpec(final _BuildControllerSpec spec) {
        super(spec);
    }

    public BuildControllerSpec(final String name, final String computer) {
        this(name, computer, true);
    }

    public BuildControllerSpec(final String name, final String serviceHostName, final boolean includeAgents) {
        this(name, serviceHostName, null, includeAgents);
    }

    public BuildControllerSpec(
        final String name,
        final String serviceHostName,
        final String[] propertyNameFilters,
        final boolean includeAgents) {
        this();

        final _BuildControllerSpec spec = getWebServiceObject();

        spec.setName(StringUtil.isNullOrEmpty(name) ? BuildConstants.STAR : name);
        spec.setServiceHostName(StringUtil.isNullOrEmpty(serviceHostName) ? BuildConstants.STAR : serviceHostName);

        if (propertyNameFilters != null) {
            spec.setPropertyNameFilters(propertyNameFilters);
        }

        spec.setIncludeAgents(includeAgents);
    }

    public _BuildControllerSpec getWebServiceObject() {
        return (_BuildControllerSpec) this.webServiceObject;
    }

    /**
     * Gets or sets a value indicating whether or not the associated build
     * agents should be returned. {@inheritDoc}
     */
    @Override
    public boolean isIncludeAgents() {
        return getWebServiceObject().isIncludeAgents();
    }

    @Override
    public void setIncludeAgents(final boolean value) {
        getWebServiceObject().setIncludeAgents(value);
    }

    /**
     * Gets or sets the build controller name filter. Wildcards are allowed.
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return getWebServiceObject().getName();
    }

    @Override
    public void setName(final String value) {
        getWebServiceObject().setName(value);
    }

    /**
     * Gets or sets the build service host name filter. Wildcards are allowed.
     * {@inheritDoc}
     */
    @Override
    public String getServiceHostName() {
        return getWebServiceObject().getServiceHostName();
    }

    @Override
    public void setServiceHostName(final String value) {
        getWebServiceObject().setServiceHostName(value);
    }

    @Override
    public String[] getPropertyNameFilters() {
        return getWebServiceObject().getPropertyNameFilters();
    }

    @Override
    public void setPropertyNameFilters(final String[] value) {
        getWebServiceObject().setPropertyNameFilters(value);
    }
}
