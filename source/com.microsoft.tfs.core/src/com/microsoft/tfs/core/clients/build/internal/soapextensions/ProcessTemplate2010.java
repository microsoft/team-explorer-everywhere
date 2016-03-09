// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.build.internal.soapextensions;

import com.microsoft.tfs.core.clients.build.IBuildServer;
import com.microsoft.tfs.core.clients.build.flags.BuildReason2010;
import com.microsoft.tfs.core.clients.build.flags.ProcessTemplateType2010;
import com.microsoft.tfs.core.internal.wrappers.WebServiceObjectWrapper;

import ms.tfs.build.buildservice._03._ProcessTemplate;

public class ProcessTemplate2010 extends WebServiceObjectWrapper {
    private IBuildServer buildServer;

    private ProcessTemplate2010() {
        this(new _ProcessTemplate());
    }

    public ProcessTemplate2010(final _ProcessTemplate value) {
        super(value);
    }

    public ProcessTemplate2010(final ProcessTemplate template) {
        this();
        setDescription(template.getDescription());
        setFileExists(template.isFileExists());
        setID(template.getID());
        setParameters(template.getParameters());
        setServerPath(template.getServerPath());
        setSupportedReasons(TFS2010Helper.convert(template.getSupportedReasons()));
        setTeamProject(template.getTeamProject());
        setTemplateType(TFS2010Helper.convert(template.getTemplateType()));
    }

    public _ProcessTemplate getWebServiceObject() {
        return (_ProcessTemplate) webServiceObject;
    }

    public IBuildServer getBuildServer() {
        return buildServer;
    }

    public String getDescription() {
        return getWebServiceObject().getDescription();
    }

    public boolean isFileExists() {
        return getWebServiceObject().isFileExists();
    }

    public int getID() {
        return getWebServiceObject().getId();
    }

    public String getParameters() {
        return getWebServiceObject().getParameters();
    }

    public String getServerPath() {
        return getWebServiceObject().getServerPath();
    }

    public BuildReason2010 getSupportedReasons() {
        return BuildReason2010.fromWebServiceObject(getWebServiceObject().getSupportedReasons());
    }

    public String getTeamProject() {
        return getWebServiceObject().getTeamProject();
    }

    public void setBuildServer(final IBuildServer value) {
        buildServer = value;
    }

    public void setDescription(final String value) {
        getWebServiceObject().setDescription(value);
    }

    public void setFileExists(final boolean value) {
        getWebServiceObject().setFileExists(value);
    }

    public void setID(final int value) {
        getWebServiceObject().setId(value);
    }

    public void setParameters(final String value) {
        getWebServiceObject().setParameters(value);
    }

    public void setServerPath(final String value) {
        getWebServiceObject().setServerPath(value);
    }

    public void setSupportedReasons(final BuildReason2010 value) {
        getWebServiceObject().setSupportedReasons(value.getWebServiceObject());
    }

    public void setTeamProject(final String value) {
        getWebServiceObject().setTeamProject(value);
    }

    public ProcessTemplateType2010 getTemplateType() {
        return ProcessTemplateType2010.fromWebServiceObject(getWebServiceObject().getTemplateType());
    }

    public void setTemplateType(final ProcessTemplateType2010 value) {
        getWebServiceObject().setTemplateType(value.getWebServiceObject());
    }
}
