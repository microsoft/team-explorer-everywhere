// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.build.internal.soapextensions;

import java.util.Calendar;

import com.microsoft.tfs.core.clients.build.flags.BuildPhaseStatus2010;
import com.microsoft.tfs.core.clients.build.flags.BuildReason2010;
import com.microsoft.tfs.core.clients.build.flags.BuildStatus2010;
import com.microsoft.tfs.core.internal.wrappers.WebServiceObjectWrapper;
import com.microsoft.tfs.core.internal.wrappers.WrapperUtils;

import ms.tfs.build.buildservice._03._BuildDetail;
import ms.tfs.build.buildservice._03._BuildInformationNode;

public class BuildDetail2010 extends WebServiceObjectWrapper {
    private BuildServer buildServer;

    public BuildDetail2010(final _BuildDetail value) {
        super(value);
    }

    public _BuildDetail getWebServiceObject() {
        return (_BuildDetail) webServiceObject;
    }

    public BuildServer getBuildServer() {
        return buildServer;
    }

    public String getBuildAgentUri() {
        if (getWebServiceObject().getBuildAgentUri() == null) {
            return getWebServiceObject().getBuildControllerUri();
        } else {
            return getWebServiceObject().getBuildAgentUri();
        }
    }

    public String getBuildControllerUri() {
        return getWebServiceObject().getBuildControllerUri();
    }

    public String getBuildDefinitionUri() {
        return getWebServiceObject().getBuildDefinitionUri();
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

    public String getDropLocationRoot() {
        return getWebServiceObject().getDropLocationRoot();
    }

    public Calendar getFinishTime() {
        return getWebServiceObject().getFinishTime();
    }

    public BuildInformationNode2010[] getInternalInformation() {
        return (BuildInformationNode2010[]) WrapperUtils.wrap(
            BuildInformationNode2010.class,
            getWebServiceObject().getInformation());
    }

    public boolean isIsDeleted() {
        return getWebServiceObject().isIsDeleted();
    }

    public boolean isKeepForever() {
        return getWebServiceObject().isKeepForever();
    }

    public String getLabelName() {
        return getWebServiceObject().getLabelName();
    }

    public String getLastChangedBy() {
        return getWebServiceObject().getLastChangedBy();
    }

    public Calendar getLastChangedOn() {
        return getWebServiceObject().getLastChangedOn();
    }

    public String getLogLocation() {
        return getWebServiceObject().getLogLocation();
    }

    public String getProcessParameters() {
        return getWebServiceObject().getProcessParameters();
    }

    public String getQuality() {
        return getWebServiceObject().getQuality();
    }

    public BuildReason2010 getReason() {
        return BuildReason2010.fromWebServiceObject(getWebServiceObject().getReason());
    }

    public String getRequestedBy() {
        return getWebServiceObject().getRequestedBy();
    }

    public String getRequestedFor() {
        return getWebServiceObject().getRequestedFor();
    }

    public String getShelvesetName() {
        return getWebServiceObject().getShelvesetName();
    }

    public String getSourceGetVersion() {
        return getWebServiceObject().getSourceGetVersion();
    }

    public Calendar getStartTime() {
        return getWebServiceObject().getStartTime();
    }

    public BuildStatus2010 getStatus() {
        return BuildStatus2010.fromWebServiceObject(getWebServiceObject().getStatus());
    }

    public String getTeamProject() {
        return getWebServiceObject().getTeamProject();
    }

    public BuildPhaseStatus2010 getTestStatus() {
        return BuildPhaseStatus2010.fromWebServiceObject(getWebServiceObject().getTestStatus());
    }

    public String getURI() {
        return getWebServiceObject().getUri();
    }

    public void setBuildControllerUri(final String value) {
        getWebServiceObject().setBuildControllerUri(value);
    }

    public void setBuildDefinitionUri(final String value) {
        getWebServiceObject().setBuildDefinitionUri(value);
    }

    public void setBuildNumber(final String value) {
        getWebServiceObject().setBuildNumber(value);
    }

    public void setCompilationStatus(final BuildPhaseStatus2010 value) {
        getWebServiceObject().setCompilationStatus(value.getWebServiceObject());
    }

    public void setDropLocation(final String value) {
        getWebServiceObject().setDropLocation(value);
    }

    public void setInternalInformation(final BuildInformationNode2010[] value) {
        getWebServiceObject().setInformation(
            (_BuildInformationNode[]) WrapperUtils.unwrap(_BuildInformationNode.class, value));
    }

    public void setKeepForever(final boolean value) {
        getWebServiceObject().setKeepForever(value);
    }

    public void setLabelName(final String value) {
        getWebServiceObject().setLabelName(value);
    }

    public void setLogLocation(final String value) {
        getWebServiceObject().setLogLocation(value);
    }

    public void setQuality(final String value) {
        getWebServiceObject().setQuality(value);
    }

    public void setSourceGetVersion(final String value) {
        getWebServiceObject().setSourceGetVersion(value);
    }

    public void setStatus(final BuildStatus2010 value) {
        getWebServiceObject().setStatus(value.getWebServiceObject());
    }

    public void setTestStatus(final BuildPhaseStatus2010 value) {
        getWebServiceObject().setTestStatus(value.getWebServiceObject());
    }
}
