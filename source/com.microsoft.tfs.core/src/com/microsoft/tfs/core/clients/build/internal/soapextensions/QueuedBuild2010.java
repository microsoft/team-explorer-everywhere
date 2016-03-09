// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.build.internal.soapextensions;

import java.util.Calendar;

import com.microsoft.tfs.core.clients.build.flags.BuildReason2010;
import com.microsoft.tfs.core.clients.build.flags.GetOption2010;
import com.microsoft.tfs.core.clients.build.flags.QueuePriority2010;
import com.microsoft.tfs.core.clients.build.flags.QueueStatus2010;
import com.microsoft.tfs.core.internal.wrappers.WebServiceObjectWrapper;
import com.microsoft.tfs.util.datetime.DotNETDate;

import ms.tfs.build.buildservice._03._BuildDetail;
import ms.tfs.build.buildservice._03._QueuedBuild;

public class QueuedBuild2010 extends WebServiceObjectWrapper {
    private BuildServer buildServer;

    public QueuedBuild2010(final _QueuedBuild value) {
        super(value);
    }

    public _QueuedBuild getWebServiceObject() {
        return (_QueuedBuild) webServiceObject;
    }

    public BuildServer getBuildServer() {
        return buildServer;
    }

    public void setBuildServer(final BuildServer value) {
        buildServer = value;
    }

    public BuildDetail2010 getBuild() {
        final _BuildDetail _detail = getWebServiceObject().getBuild();
        return _detail == null ? null : new BuildDetail2010(_detail);
    }

    public String getBuildControllerUri() {
        return getWebServiceObject().getBuildControllerUri();
    }

    public String getBuildDefinitionUri() {
        return getWebServiceObject().getBuildDefinitionUri();
    }

    public String getCustomGetVersion() {
        return getWebServiceObject().getCustomGetVersion();
    }

    public String getDropLocation() {
        return getWebServiceObject().getDropLocation();
    }

    public GetOption2010 getGetOption() {
        return GetOption2010.fromWebServiceObject(getWebServiceObject().getGetOption());
    }

    public int getID() {
        return getWebServiceObject().getId();
    }

    public QueuePriority2010 getPriority() {
        return QueuePriority2010.fromWebServiceObject(getWebServiceObject().getPriority());
    }

    public String getProcessParameters() {
        return getWebServiceObject().getProcessParameters();
    }

    public int getQueuePosition() {
        return getWebServiceObject().getQueuePosition();
    }

    public Calendar getQueueTime() {
        if (getWebServiceObject().getQueueTime() == null) {
            return DotNETDate.MIN_CALENDAR;
        }
        return getWebServiceObject().getQueueTime();
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

    public QueueStatus2010 getStatus() {
        return QueueStatus2010.fromWebServiceObject(getWebServiceObject().getStatus());
    }

    public String getTeamProject() {
        return getWebServiceObject().getTeamProject();
    }

    public void setBuild(final BuildDetail2010 value) {
        getWebServiceObject().setBuild(value.getWebServiceObject());
    }

    public void setBuildControllerUri(final String value) {
        getWebServiceObject().setBuildControllerUri(value);
    }

    public void setBuildDefinitionUri(final String value) {
        getWebServiceObject().setBuildDefinitionUri(value);
    }

    public void setCustomGetVersion(final String value) {
        getWebServiceObject().setCustomGetVersion(value);
    }

    public void setDropLocation(final String value) {
        getWebServiceObject().setDropLocation(value);
    }

    public void setGetOption(final GetOption2010 value) {
        getWebServiceObject().setGetOption(value.getWebServiceObject());
    }

    public void setID(final int value) {
        getWebServiceObject().setId(value);
    }

    public void setPriority(final QueuePriority2010 value) {
        getWebServiceObject().setPriority(value.getWebServiceObject());
    }

    public void setProcessParameters(final String value) {
        getWebServiceObject().setProcessParameters(value);
    }

    public void setQueuePosition(final int value) {
        getWebServiceObject().setQueuePosition(value);
    }

    public void setQueueTime(final Calendar value) {
        getWebServiceObject().setQueueTime(value);
    }

    public void setReason(final BuildReason2010 value) {
        getWebServiceObject().setReason(value.getWebServiceObject());
    }

    public void setRequestedBy(final String value) {
        getWebServiceObject().setRequestedBy(value);
    }

    public void setRequestedFor(final String value) {
        getWebServiceObject().setRequestedFor(value);
    }

    public void getShelvesetName(final String value) {
        getWebServiceObject().setShelvesetName(value);
    }

    public void setStatus(final QueueStatus2010 value) {
        getWebServiceObject().setStatus(value.getWebServiceObject());
    }
}
