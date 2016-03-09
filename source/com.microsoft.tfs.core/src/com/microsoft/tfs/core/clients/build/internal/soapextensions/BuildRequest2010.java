// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.build.internal.soapextensions;

import com.microsoft.tfs.core.clients.build.flags.BuildReason2010;
import com.microsoft.tfs.core.clients.build.flags.GetOption2010;
import com.microsoft.tfs.core.clients.build.flags.QueuePriority2010;
import com.microsoft.tfs.core.internal.wrappers.WebServiceObjectWrapper;

import ms.tfs.build.buildservice._03._BuildRequest;

public class BuildRequest2010 extends WebServiceObjectWrapper {
    private BuildRequest2010() {
        this(new _BuildRequest());
    }

    public BuildRequest2010(final _BuildRequest value) {
        super(value);
    }

    public BuildRequest2010(final BuildRequest request) {
        this();

        setBuildAgentURI(request.getBuildControllerURI());
        setBuildControllerURI(request.getBuildControllerURI());
        setBuildDefinitionURI(request.getBuildDefinitionURI());
        setCustomGetVersion(request.getCustomGetVersion());
        setDropLocation(request.getDropLocation());
        setGatedCheckInTicket(request.getGatedCheckInTicket());
        setGetOption(TFS2010Helper.convert(request.getGetOption()));
        setMaxQueuePosition(request.getMaxQueuePosition());
        setPostponed(request.isPostponed());
        setPriority(TFS2010Helper.convert(request.getPriority()));
        setProcessParameters(request.getProcessParameters());
        setReason(TFS2010Helper.convert(request.getReason()));
        setRequestedFor(request.getRequestedFor());
        setShelvesetName(request.getShelvesetName());
    }

    public _BuildRequest getWebServiceObject() {
        return (_BuildRequest) webServiceObject;
    }

    public String getBuildAgentURI() {
        return getWebServiceObject().getBuildAgentUri();
    }

    public String getBuildControllerURI() {
        return getWebServiceObject().getBuildControllerUri();
    }

    public String getBuildDefinitionURI() {
        return getWebServiceObject().getBuildDefinitionUri();
    }

    public String getCustomGetVersion() {
        return getWebServiceObject().getCustomGetVersion();
    }

    public String getDropLocation() {
        return getWebServiceObject().getDropLocation();
    }

    public String getGatedCheckInTicket() {
        return getWebServiceObject().getCheckInTicket();
    }

    public GetOption2010 getGetOption() {
        return GetOption2010.fromWebServiceObject(getWebServiceObject().getGetOption());
    }

    public int getMaxQueuePosition() {
        return getWebServiceObject().getMaxQueuePosition();
    }

    public boolean isPostponed() {
        return getWebServiceObject().isPostponed();
    }

    public QueuePriority2010 getPriority() {
        return QueuePriority2010.fromWebServiceObject(getWebServiceObject().getPriority());
    }

    public String getProcessParameters() {
        return getWebServiceObject().getProcessParameters();
    }

    public BuildReason2010 getReason() {
        return BuildReason2010.fromWebServiceObject(getWebServiceObject().getReason());
    }

    public String getRequestedFor() {
        return getWebServiceObject().getRequestedFor();
    }

    public String getShelvesetName() {
        return getWebServiceObject().getShelvesetName();
    }

    public void setBuildAgentURI(final String value) {
        getWebServiceObject().setBuildAgentUri(value);
    }

    public void setBuildControllerURI(final String value) {
        getWebServiceObject().setBuildControllerUri(value);
    }

    public void setBuildDefinitionURI(final String value) {
        getWebServiceObject().setBuildDefinitionUri(value);
    }

    public void setCustomGetVersion(final String value) {
        getWebServiceObject().setCustomGetVersion(value);
    }

    public void setDropLocation(final String value) {
        getWebServiceObject().setDropLocation(value);
    }

    public void setGatedCheckInTicket(final String value) {
        getWebServiceObject().setCheckInTicket(value);
    }

    public void setGetOption(final GetOption2010 value) {
        getWebServiceObject().setGetOption(value.getWebServiceObject());
    }

    public void setMaxQueuePosition(final int value) {
        getWebServiceObject().setMaxQueuePosition(value);
    }

    public void setPostponed(final boolean value) {
        getWebServiceObject().setPostponed(value);
    }

    public void setPriority(final QueuePriority2010 value) {
        getWebServiceObject().setPriority(value.getWebServiceObject());
    }

    public void setProcessParameters(final String value) {
        getWebServiceObject().setProcessParameters(value);
    }

    public void setReason(final BuildReason2010 value) {
        getWebServiceObject().setReason(value.getWebServiceObject());
    }

    public void setRequestedFor(final String value) {
        getWebServiceObject().setRequestedFor(value);
    }

    public void setShelvesetName(final String value) {
        getWebServiceObject().setShelvesetName(value);
    }
}
