// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.build.internal.soapextensions;

import com.microsoft.tfs.core.clients.build.IBuildAgentSpec;
import com.microsoft.tfs.core.exceptions.NotSupportedException;

import ms.tfs.build.buildservice._03._BuildAgentSpec2008;

public class BuildAgentSpec2008 extends BuildGroupItemSpec2010 implements IBuildAgentSpec {
    public BuildAgentSpec2008() {
        this(new _BuildAgentSpec2008());
    }

    public BuildAgentSpec2008(final _BuildAgentSpec2008 value) {
        super(value);
    }

    @Override
    public _BuildAgentSpec2008 getWebServiceObject() {
        return (_BuildAgentSpec2008) this.webServiceObject;
    }

    @Override
    public String getServiceHostName() {
        throw new NotSupportedException();
    }

    @Override
    public void setServiceHostName(final String value) {
        throw new NotSupportedException();
    }

    @Override
    public String[] getTags() {
        throw new NotSupportedException();
    }

    @Override
    public void setTags(final String[] value) {
        throw new NotSupportedException();
    }

    @Override
    public String[] getPropertyNameFilters() {
        throw new NotSupportedException();
    }

    @Override
    public void setPropertyNameFilters(final String[] value) {
        throw new NotSupportedException();
    }
}
