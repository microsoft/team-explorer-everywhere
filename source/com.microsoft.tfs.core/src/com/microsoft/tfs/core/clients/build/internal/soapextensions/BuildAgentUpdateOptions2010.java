// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.build.internal.soapextensions;

import com.microsoft.tfs.core.clients.build.flags.AgentStatus2010;
import com.microsoft.tfs.core.clients.build.flags.BuildAgentUpdate2010;
import com.microsoft.tfs.core.internal.wrappers.WebServiceObjectWrapper;

import ms.tfs.build.buildservice._03._BuildAgentUpdateOptions;

public class BuildAgentUpdateOptions2010 extends WebServiceObjectWrapper {
    private BuildAgentUpdateOptions2010() {
        this(new _BuildAgentUpdateOptions());
    }

    public BuildAgentUpdateOptions2010(final _BuildAgentUpdateOptions value) {
        super(value);
    }

    public BuildAgentUpdateOptions2010(final BuildAgentUpdateOptions updateOptions) {
        this();

        final _BuildAgentUpdateOptions o = getWebServiceObject();
        o.setBuildDirectory(updateOptions.getBuildDirectory());
        o.setControllerUri(updateOptions.getControllerURI());
        o.setDescription(updateOptions.getDescription());
        o.setEnabled(updateOptions.isEnabled());
        o.setFields(TFS2010Helper.convert(updateOptions.getFields()).getWebServiceObject());
        o.setName(updateOptions.getName());
        o.setStatus(TFS2010Helper.convert(updateOptions.getStatus()).getWebServiceObject());
        o.setStatusMessage(updateOptions.getStatusMessage());
        o.setTags(updateOptions.getTags());
        o.setUri(updateOptions.getURI());
    }

    public _BuildAgentUpdateOptions getWebServiceObject() {
        return (_BuildAgentUpdateOptions) webServiceObject;
    }

    public String getBuildDirectory() {
        return getWebServiceObject().getBuildDirectory();
    }

    public String getControllerUri() {
        return getWebServiceObject().getControllerUri();
    }

    public String getDescription() {
        return getWebServiceObject().getDescription();
    }

    public boolean isEnabled() {
        return getWebServiceObject().isEnabled();
    }

    public BuildAgentUpdate2010 getFields() {
        return BuildAgentUpdate2010.fromWebServiceObject(getWebServiceObject().getFields());
    }

    public String getName() {
        return getWebServiceObject().getName();
    }

    public AgentStatus2010 getStatus() {
        return AgentStatus2010.fromWebServiceObject(getWebServiceObject().getStatus());
    }

    public String getStatusMessage() {
        return getWebServiceObject().getStatusMessage();
    }

    public String[] getTags() {
        return getWebServiceObject().getTags();
    }

    public String getURI() {
        return getWebServiceObject().getUri();
    }
}
