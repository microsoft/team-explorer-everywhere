// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.build.internal.soapextensions;

import com.microsoft.tfs.core.clients.build.IBuildDefinitionSpec;
import com.microsoft.tfs.core.clients.build.flags.QueryOptions2010;
import com.microsoft.tfs.core.clients.build.soapextensions.ContinuousIntegrationType;

import ms.tfs.build.buildservice._03._BuildDefinitionSpec;

public class BuildDefinitionSpec2010 extends BuildGroupItemSpec2010 {
    private BuildDefinitionSpec2010() {
        this(new _BuildDefinitionSpec());
    }

    public BuildDefinitionSpec2010(final _BuildDefinitionSpec value) {
        super(value);
    }

    public BuildDefinitionSpec2010(final IBuildDefinitionSpec spec) {
        this();

        setContinuousIntegrationType(TFS2010Helper.convert(spec.getTriggerType()));
        setFullPath(spec.getFullPath());
        setOptions(TFS2010Helper.convert(spec.getOptions()));
    }

    @Override
    public _BuildDefinitionSpec getWebServiceObject() {
        return (_BuildDefinitionSpec) webServiceObject;
    }

    public ContinuousIntegrationType getContinuousIntegrationType() {
        return new ContinuousIntegrationType(getWebServiceObject().getContinuousIntegrationType());
    }

    public QueryOptions2010 getOptions() {
        return QueryOptions2010.fromWebServiceObject(getWebServiceObject().getOptions());
    }

    public void setContinuousIntegrationType(final ContinuousIntegrationType value) {
        getWebServiceObject().setContinuousIntegrationType(value.getWebServiceObject());
    }

    public void setOptions(final QueryOptions2010 value) {
        getWebServiceObject().setOptions(value.getWebServiceObject());
    }

    @Override
    public void setFullPath(final String value) {
        getWebServiceObject().setFullPath(value);
    }
}
