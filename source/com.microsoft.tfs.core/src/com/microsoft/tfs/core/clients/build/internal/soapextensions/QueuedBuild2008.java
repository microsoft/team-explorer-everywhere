// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.build.internal.soapextensions;

import java.util.Calendar;

import com.microsoft.tfs.core.clients.build.flags.GetOption2010;
import com.microsoft.tfs.core.clients.build.flags.QueuePriority2010;
import com.microsoft.tfs.core.clients.build.flags.QueueStatus2010;
import com.microsoft.tfs.core.internal.wrappers.WebServiceObjectWrapper;

import ms.tfs.build.buildservice._03._QueuedBuild2008;

public class QueuedBuild2008 extends WebServiceObjectWrapper {
    private BuildServer buildServer;

    public QueuedBuild2008(final _QueuedBuild2008 webServiceObject) {
        super(webServiceObject);
    }

    public _QueuedBuild2008 getWebServiceObject() {
        return (_QueuedBuild2008) this.webServiceObject;
    }

    public BuildServer getBuildServer() {
        return buildServer;
    }

    public void setBuildServer(final BuildServer buildServer) {
        this.buildServer = buildServer;
    }

    public BuildDetail2010 getBuild() {
        if (getWebServiceObject().getBuild() == null) {
            return null;
        }
        return new BuildDetail2010(getWebServiceObject().getBuild());
    }

    public void setBuild(final BuildDetail2010 value) {
        getWebServiceObject().setBuild(value.getWebServiceObject());
    }

    public String getBuildAgentURI() {
        return getWebServiceObject().getBuildAgentUri();
    }

    public void setBuildAgentURI(final String value) {
        getWebServiceObject().setBuildAgentUri(value);
    }

    public String getBuildDefinitionURI() {
        return getWebServiceObject().getBuildDefinitionUri();
    }

    public void setBuildDefinitionURI(final String value) {
        getWebServiceObject().setBuildDefinitionUri(value);
    }

    public String getCommandLineArguments() {
        return getWebServiceObject().getCommandLineArguments();
    }

    public void setCommandLineArguments(final String value) {
        getWebServiceObject().setCommandLineArguments(value);
    }

    public String getCustomGetVersion() {
        return getWebServiceObject().getCustomGetVersion();
    }

    public void setCustomGetVersion(final String value) {
        getWebServiceObject().setCustomGetVersion(value);
    }

    public String getDropLocation() {
        return getWebServiceObject().getDropLocation();
    }

    public void setDropLocation(final String value) {
        getWebServiceObject().setDropLocation(value);
    }

    public GetOption2010 getGetOption() {
        return GetOption2010.fromWebServiceObject(getWebServiceObject().getGetOption());
    }

    public void setGetOption(final GetOption2010 value) {
        getWebServiceObject().setGetOption(value.getWebServiceObject());
    }

    public int getID() {
        return getWebServiceObject().getId();
    }

    public void setID(final int value) {
        getWebServiceObject().setId(value);
    }

    public QueuePriority2010 getPriority() {
        return QueuePriority2010.fromWebServiceObject(getWebServiceObject().getPriority());
    }

    public void setPriority(final QueuePriority2010 value) {
        getWebServiceObject().setPriority(value.getWebServiceObject());
    }

    public int getQueuePosition() {
        return getWebServiceObject().getQueuePosition();
    }

    public void setQueuePosition(final int value) {
        getWebServiceObject().setQueuePosition(value);
    }

    public Calendar getQueueTime() {
        return getWebServiceObject().getQueueTime();
    }

    public void setQueueTime(final Calendar value) {
        getWebServiceObject().setQueueTime(value);
    }

    public String getRequestedBy() {
        return getWebServiceObject().getRequestedBy();
    }

    public void setRequestedBy(final String value) {
        getWebServiceObject().setRequestedBy(value);
    }

    public String getRequestedFor() {
        return getWebServiceObject().getRequestedFor();
    }

    public void setRequestedFor(final String value) {
        getWebServiceObject().setRequestedFor(value);
    }

    public QueueStatus2010 getStatus() {
        return QueueStatus2010.fromWebServiceObject(getWebServiceObject().getStatus());
    }

    public void setStatus(final QueueStatus2010 value) {
        getWebServiceObject().setStatus(value.getWebServiceObject());
    }
}