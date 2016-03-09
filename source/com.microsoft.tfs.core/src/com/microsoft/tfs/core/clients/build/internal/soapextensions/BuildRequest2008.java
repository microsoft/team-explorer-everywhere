// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.build.internal.soapextensions;

import com.microsoft.tfs.core.clients.build.flags.GetOption2010;
import com.microsoft.tfs.core.clients.build.flags.QueuePriority2010;
import com.microsoft.tfs.core.internal.wrappers.WebServiceObjectWrapper;

import ms.tfs.build.buildservice._03._BuildRequest2008;

public class BuildRequest2008 extends WebServiceObjectWrapper {
    public BuildRequest2008() {
        this(new _BuildRequest2008());
    }

    public BuildRequest2008(final _BuildRequest2008 webServiceObject) {
        super(webServiceObject);
    }

    public BuildRequest2008(final BuildRequest request) {
        this();
        setBuildAgentURI(request.getBuildControllerURI());
        setBuildDefinitionURI(request.getBuildDefinitionURI());
        setCommandLineArguments(request.getProcessParameters());
        setPriority(TFS2010Helper.convert(request.getPriority()));
        setPostponed(request.isPostponed());
        setDropLocation(request.getDropLocation());
        setGetOption(TFS2010Helper.convert(request.getGetOption()));
        setCustomGetVersion(request.getCustomGetVersion());
        setMaxQueuePosition(request.getMaxQueuePosition());
        setRequestedFor(request.getRequestedFor());
    }

    public _BuildRequest2008 getWebServiceObject() {
        return (_BuildRequest2008) this.webServiceObject;
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

    public int getMaxQueuePosition() {
        return getWebServiceObject().getMaxQueuePosition();
    }

    public void setMaxQueuePosition(final int value) {
        getWebServiceObject().setMaxQueuePosition(value);
    }

    public boolean isPostponed() {
        return getWebServiceObject().isPostponed();
    }

    public void setPostponed(final boolean value) {
        getWebServiceObject().setPostponed(value);
    }

    public QueuePriority2010 getPriority() {
        return QueuePriority2010.fromWebServiceObject(getWebServiceObject().getPriority());
    }

    public void setPriority(final QueuePriority2010 value) {
        getWebServiceObject().setPriority(value.getWebServiceObject());
    }

    public String getRequestedFor() {
        return getWebServiceObject().getRequestedFor();
    }

    public void setRequestedFor(final String value) {
        getWebServiceObject().setRequestedFor(value);
    }

}
