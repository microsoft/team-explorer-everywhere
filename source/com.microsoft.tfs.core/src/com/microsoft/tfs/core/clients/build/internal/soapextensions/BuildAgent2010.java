// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.build.internal.soapextensions;

import java.util.Calendar;

import com.microsoft.tfs.core.clients.build.flags.AgentStatus2010;
import com.microsoft.tfs.core.internal.wrappers.WebServiceObjectWrapper;

import ms.tfs.build.buildservice._03._BuildAgent;

public class BuildAgent2010 extends WebServiceObjectWrapper {
    private BuildAgent2010() {
        this(new _BuildAgent());
    }

    public BuildAgent2010(final _BuildAgent value) {
        super(value);
    }

    public BuildAgent2010(final BuildAgent agent) {
        this();

        final _BuildAgent o = getWebServiceObject();
        o.setBuildDirectory(agent.getBuildDirectory());
        o.setControllerUri(agent.getControllerURI());
        o.setDateCreated(agent.getDateCreated());
        o.setDateUpdated(agent.getDateUpdated());
        o.setDescription(agent.getDescription());
        o.setEnabled(agent.isEnabled());
        o.setName(agent.getName());
        o.setReservedForBuild(agent.getReservedForBuild());
        o.setServiceHostUri(agent.getServiceHostURI());
        o.setStatus(TFS2010Helper.convert(agent.getStatus()).getWebServiceObject());
        o.setStatusMessage(agent.getStatusMessage());
        o.setTags(agent.getTags());
        o.setUri(agent.getURI());
        o.setUrl(agent.getURL());
    }

    public _BuildAgent getWebServiceObject() {
        return (_BuildAgent) webServiceObject;
    }

    public String getBuildDirectory() {
        return getWebServiceObject().getBuildDirectory();
    }

    public String getControllerURI() {
        return getWebServiceObject().getControllerUri();
    }

    public Calendar getDateCreated() {
        return getWebServiceObject().getDateCreated();
    }

    public Calendar getDateUpdated() {
        return getWebServiceObject().getDateUpdated();
    }

    public String getDescription() {
        return getWebServiceObject().getDescription();
    }

    public boolean isEnabled() {
        return getWebServiceObject().isEnabled();
    }

    public String getName() {
        return getWebServiceObject().getName();
    }

    public String getReservedForBuild() {
        return getWebServiceObject().getReservedForBuild();
    }

    public String getServiceHostURI() {
        return getWebServiceObject().getServiceHostUri();
    }

    public AgentStatus2010 getStatus() {
        return AgentStatus2010.fromWebServiceObject(getWebServiceObject().getStatus());
    }

    public String getStatusMessage() {
        return getWebServiceObject().getStatusMessage();
    }

    public String getURI() {
        return getWebServiceObject().getUri();
    }

    public String getURL() {
        return getWebServiceObject().getUrl();
    }

    public String[] getTags() {
        return getWebServiceObject().getTags();
    }

    public void setBuildDirectory(final String value) {
        getWebServiceObject().setBuildDirectory(value);
    }

    public void setDescription(final String value) {
        getWebServiceObject().setDescription(value);
    }

    public void setEnabled(final boolean value) {
        getWebServiceObject().setEnabled(value);
    }

    public void setName(final String value) {
        getWebServiceObject().setName(value);
    }

    public void setStatus(final AgentStatus2010 value) {
        getWebServiceObject().setStatus(value.getWebServiceObject());
    }

    public void setStatusMessage(final String value) {
        getWebServiceObject().setStatusMessage(value);
    }

    public void setUri(final String value) {
        getWebServiceObject().setUri(value);
    }

    public void setUrl(final String value) {
        getWebServiceObject().setUrl(value);
    }
}
