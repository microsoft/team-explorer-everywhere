// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.build.internal.soapextensions;

import com.microsoft.tfs.core.clients.build.flags.BuildPhaseStatus2010;
import com.microsoft.tfs.core.clients.build.flags.BuildStatus2010;
import com.microsoft.tfs.core.clients.build.flags.BuildUpdate2010;
import com.microsoft.tfs.core.internal.wrappers.WebServiceObjectWrapper;

import ms.tfs.build.buildservice._03._BuildUpdateOptions;

public class BuildUpdateOptions2010 extends WebServiceObjectWrapper {
    private BuildUpdateOptions2010() {
        this(new _BuildUpdateOptions());
    }

    public BuildUpdateOptions2010(final _BuildUpdateOptions value) {
        super(value);
    }

    public BuildUpdateOptions2010(final BuildUpdateOptions updateOptions) {
        this();

        final _BuildUpdateOptions o = getWebServiceObject();
        o.setBuildNumber(updateOptions.getBuildNumber());
        o.setCompilationStatus(TFS2010Helper.convert(updateOptions.getCompilationStatus()).getWebServiceObject());
        o.setDropLocation(updateOptions.getDropLocation());
        o.setFields(TFS2010Helper.convert(updateOptions.getFields()).getWebServiceObject());
        o.setKeepForever(updateOptions.isKeepForever());
        o.setLabelName(updateOptions.getLabelName());
        o.setLogLocation(updateOptions.getLogLocation());
        o.setQuality(updateOptions.getQuality());
        o.setSourceGetVersion(updateOptions.getSourceGetVersion());
        o.setStatus(TFS2010Helper.convert(updateOptions.getStatus()).getWebServiceObject());
        o.setTestStatus(TFS2010Helper.convert(updateOptions.getTestStatus()).getWebServiceObject());
        o.setUri(updateOptions.getURI());
    }

    public _BuildUpdateOptions getWebServiceObject() {
        return (_BuildUpdateOptions) webServiceObject;
    }

    public String getBuildNumber() {
        return getWebServiceObject().getBuildNumber();
    }

    public BuildPhaseStatus2010 getCompilationStatus() {
        return BuildPhaseStatus2010.fromWebServiceObject(getWebServiceObject().getCompilationStatus());
    }

    public String getDropLocation() {
        return getWebServiceObject().getDropLocation();
    }

    public BuildUpdate2010 getFields() {
        return new BuildUpdate2010(getWebServiceObject().getFields());
    }

    public boolean isKeepForever() {
        return getWebServiceObject().isKeepForever();
    }

    public String getLabelName() {
        return getWebServiceObject().getLabelName();
    }

    public String getLogLocation() {
        return getWebServiceObject().getLogLocation();
    }

    public String getQuality() {
        return getWebServiceObject().getQuality();
    }

    public String getSourceGetVersion() {
        return getWebServiceObject().getSourceGetVersion();
    }

    public BuildStatus2010 getStatus() {
        return BuildStatus2010.fromWebServiceObject(getWebServiceObject().getStatus());
    }

    public BuildPhaseStatus2010 getTestStatus() {
        return BuildPhaseStatus2010.fromWebServiceObject(getWebServiceObject().getTestStatus());
    }

    public String getURI() {
        return getWebServiceObject().getUri();
    }
}
