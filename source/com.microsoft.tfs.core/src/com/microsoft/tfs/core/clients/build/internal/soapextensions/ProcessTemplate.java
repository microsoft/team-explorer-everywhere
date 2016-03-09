// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.build.internal.soapextensions;

import com.microsoft.tfs.core.clients.build.IBuildServer;
import com.microsoft.tfs.core.clients.build.IProcessTemplate;
import com.microsoft.tfs.core.clients.build.flags.BuildReason;
import com.microsoft.tfs.core.clients.build.soapextensions.ProcessTemplateType;
import com.microsoft.tfs.core.exceptions.NotSupportedException;
import com.microsoft.tfs.core.internal.wrappers.WebServiceObjectWrapper;
import com.microsoft.tfs.util.Check;

import ms.tfs.build.buildservice._04._ProcessTemplate;

public class ProcessTemplate extends WebServiceObjectWrapper implements IProcessTemplate {
    private IBuildServer buildServer;

    public ProcessTemplate(final IBuildServer buildServer) {
        this(buildServer, new _ProcessTemplate());
    }

    public ProcessTemplate(final IBuildServer buildServer, final _ProcessTemplate webServiceObject) {
        super(webServiceObject);
    }

    public ProcessTemplate(final IBuildServer buildServer, final String teamProject, final String serverPath) {
        this(buildServer);

        this.buildServer = buildServer;
        getWebServiceObject().setTeamProject(teamProject);
        getWebServiceObject().setServerPath(serverPath);
    }

    public ProcessTemplate(final IBuildServer buildServer, final ProcessTemplate2010 template2010) {
        this(buildServer);
        final _ProcessTemplate template = getWebServiceObject();

        this.buildServer = template2010.getBuildServer();
        template.setDescription(template2010.getDescription());
        template.setFileExists(template2010.isFileExists());
        template.setId(template2010.getID());
        template.setParameters(template2010.getParameters());
        template.setServerPath(template2010.getServerPath());
        template.setSupportedReasons(TFS2010Helper.convert(template2010.getSupportedReasons()).getWebServiceObject());
        template.setTeamProject(template2010.getTeamProject());
        template.setTemplateType(TFS2010Helper.convert(template2010.getTemplateType()).getWebServiceObject());
    }

    public _ProcessTemplate getWebServiceObject() {
        return (_ProcessTemplate) this.webServiceObject;
    }

    /**
     * Gets or sets the description. {@inheritDoc}
     */
    @Override
    public String getDescription() {
        return getWebServiceObject().getDescription();
    }

    @Override
    public void setDescription(final String value) {
        getWebServiceObject().setDescription(value);
    }

    /**
     * Gets or sets the version control server location of the process template
     * XAML file. {@inheritDoc}
     */
    @Override
    public String getServerPath() {
        return getWebServiceObject().getServerPath();
    }

    /**
     * Gets or sets the reasons that the template supports. {@inheritDoc}
     */
    @Override
    public BuildReason getSupportedReasons() {
        return BuildReason.fromWebServiceObject(getWebServiceObject().getSupportedReasons());
    }

    @Override
    public void setSupportedReasons(final BuildReason value) {
        getWebServiceObject().setSupportedReasons(value.getWebServiceObject());
    }

    /**
     * Gets or sets the team project to which this template belongs.
     * {@inheritDoc}
     */
    @Override
    public String getTeamProject() {
        return getWebServiceObject().getTeamProject();
    }

    /**
     * Gets or sets the type of this template. {@inheritDoc}
     */
    @Override
    public ProcessTemplateType getTemplateType() {
        return ProcessTemplateType.fromWebServiceObject(getWebServiceObject().getTemplateType());
    }

    @Override
    public void setTemplateType(final ProcessTemplateType value) {
        getWebServiceObject().setTemplateType(value.getWebServiceObject());
    }

    public boolean isFileExists() {
        return getWebServiceObject().isFileExists();
    }

    @Override
    public int getID() {
        return getWebServiceObject().getId();
    }

    public void setID(final int value) {
        getWebServiceObject().setId(value);
    }

    @Override
    public String getParameters() {
        return getWebServiceObject().getParameters();
    }

    public void setParameters(final String value) {
        getWebServiceObject().setParameters(value);
    }

    @Override
    public String getVersion() {
        return getWebServiceObject().getVersion();
    }

    public IBuildServer getBuildServer() {
        return buildServer;
    }

    @Override
    public String download() {
        return download(null);
    }

    @Override
    public String download(final String sourceGetVersion) {
        throw new NotSupportedException();
    }

    @Override
    public void save() {
        buildServer.saveProcessTemplates(new IProcessTemplate[] {
            this
        });
    }

    @Override
    public void delete() {
        buildServer.deleteProcessTemplates(new IProcessTemplate[] {
            this
        });
        getWebServiceObject().setId(-1);
    }

    @Override
    public void copyFrom(final IProcessTemplate processTemplate) {
        Check.notNull(processTemplate, "processTemplate"); //$NON-NLS-1$

        final ProcessTemplate source = (ProcessTemplate) processTemplate;
        final _ProcessTemplate template = getWebServiceObject();

        template.setDescription(source.getDescription());
        template.setFileExists(source.isFileExists());
        template.setId(source.getID());
        template.setParameters(source.getParameters());
        template.setServerPath(source.getServerPath());
        template.setSupportedReasons(source.getSupportedReasons().getWebServiceObject());
        template.setTeamProject(source.getTeamProject());
        template.setTemplateType(source.getTemplateType().getWebServiceObject());
    }

    public void prepareToSave() {
    }
}
