// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.build.internal.soapextensions;

import java.util.Properties;

import com.microsoft.tfs.core.clients.build.IBuildDefinition;
import com.microsoft.tfs.core.clients.build.IRetentionPolicy;
import com.microsoft.tfs.core.clients.build.ISchedule;
import com.microsoft.tfs.core.clients.build.flags.BuildServerVersion;
import com.microsoft.tfs.core.clients.build.flags.DefinitionQueueStatus;
import com.microsoft.tfs.core.clients.build.internal.utils.XamlHelper;
import com.microsoft.tfs.core.clients.build.soapextensions.ContinuousIntegrationType;
import com.microsoft.tfs.core.internal.wrappers.WrapperUtils;
import com.microsoft.tfs.util.StringUtil;

import ms.tfs.build.buildservice._03._BuildDefinition;
import ms.tfs.build.buildservice._03._ContinuousIntegrationType;
import ms.tfs.build.buildservice._03._ProcessTemplate;
import ms.tfs.build.buildservice._03._RetentionPolicy;
import ms.tfs.build.buildservice._03._Schedule;
import ms.tfs.build.buildservice._03._WorkspaceTemplate;

/**
 * Implementation of {@link IBuildDefinition}.
 *
 * @see IBuildDefinition
 */
public class BuildDefinition2010 extends BuildGroupItem2010 {
    public BuildDefinition2010(final _BuildDefinition webServiceObject) {
        super(webServiceObject);
    }

    public BuildDefinition2010(final BuildServerVersion version, final BuildDefinition definition) {
        super(new _BuildDefinition());

        setBuildControllerURI(definition.getBuildControllerURI());
        setContinuousIntegrationQuietPeriod(definition.getContinuousIntegrationQuietPeriod());
        setContinuousIntegrationType(TFS2010Helper.convert(definition.getTriggerType()));
        setDefaultDropLocation(definition.getDefaultDropLocation());
        setDescription(definition.getDescription());
        setEnabled(
            definition.getQueueStatus().equals(DefinitionQueueStatus.ENABLED)
                || definition.getQueueStatus().equals(DefinitionQueueStatus.PAUSED));
        setLastBuildUri(definition.getLastBuildURI());
        setLastGoodBuildLabel(definition.getLastGoodBuildLabel());
        setLastGoodBuildUri(definition.getLastGoodBuildURI());

        final IRetentionPolicy[] policies = definition.getRetentionPolicies();
        setRetentionPolicies(TFS2010Helper.convert(policies));

        final ISchedule[] schedules = definition.getSchedules();
        setSchedules(TFS2010Helper.convert(schedules));

        setURI(definition.getURI());
        setWorkspaceTemplate(TFS2010Helper.convert((WorkspaceTemplate) definition.getWorkspace()));

        // Set the full path using the propery to expand the team project and
        // name
        setFullPath(definition.getWebServiceObject().getFullPath());

        if (version.isV2()) {
            setDefaultBuildAgentURI(getBuildControllerURI());
            if (!StringUtil.isNullOrEmpty(definition.getProcessParameters())) {
                final Properties parameters = XamlHelper.loadPartial(definition.getProcessParameters());
                final String key = "ConfigurationFolderPath"; //$NON-NLS-1$

                if (parameters != null && parameters.containsKey(key)) {
                    final String configurationFolderPath = (String) parameters.get(key);
                    setConfigurationFolderURI(definition.getConfigurationFolderUri(configurationFolderPath));
                }
            } else if (definition.getConfigurationFolderPath() != null) {
                setConfigurationFolderURI(
                    definition.getConfigurationFolderUri(definition.getConfigurationFolderPath()));
            }
        } else if (version.isV3()) {
            setProcess(TFS2010Helper.convert(definition.getProcess()));
            setProcessParameters(definition.getProcessParameters());
        }
    }

    @Override
    public _BuildDefinition getWebServiceObject() {
        return (_BuildDefinition) webServiceObject;
    }

    public String getBuildControllerURI() {
        return getWebServiceObject().getBuildControllerUri();
    }

    public String getConfigurationFolderURI() {
        return getWebServiceObject().getConfigurationFolderUri();
    }

    public int getContinuousIntegrationQuietPeriod() {
        return getWebServiceObject().getContinuousIntegrationQuietPeriod();
    }

    public ContinuousIntegrationType getContinuousIntegrationType() {
        final _ContinuousIntegrationType _cit = getWebServiceObject().getContinuousIntegrationType();
        return _cit == null ? null : new ContinuousIntegrationType(_cit);
    }

    public String getDefaultBuildAgentURI() {
        return getWebServiceObject().getDefaultBuildAgentUri();
    }

    public String getDefaultDropLocation() {
        return getWebServiceObject().getDefaultDropLocation();
    }

    public String getDescription() {
        return getWebServiceObject().getDescription();
    }

    public boolean isEnabled() {
        return getWebServiceObject().isEnabled();
    }

    public String getLastBuildURI() {
        return getWebServiceObject().getLastBuildUri();
    }

    public String getLastGoodBuildLabel() {
        return getWebServiceObject().getLastGoodBuildLabel();
    }

    public String getLastGoodBuildURI() {
        return getWebServiceObject().getLastGoodBuildUri();
    }

    public int getMaxTimeout() {
        return getWebServiceObject().getMaxTimeout();
    }

    public ProcessTemplate2010 getProcess() {
        final _ProcessTemplate _process = getWebServiceObject().getProcess();
        return _process == null ? null : new ProcessTemplate2010(_process);
    }

    public String getProcessParameters() {
        return getWebServiceObject().getProcessParameters();
    }

    public RetentionPolicy2010[] getRetentionPolicies() {
        return (RetentionPolicy2010[]) WrapperUtils.wrap(
            RetentionPolicy2010.class,
            getWebServiceObject().getRetentionPolicies());
    }

    public Schedule2010[] getSchedules() {
        return (Schedule2010[]) WrapperUtils.wrap(Schedule2010.class, getWebServiceObject().getSchedules());
    }

    public WorkspaceTemplate2010 getWorkspaceTemplate() {
        final _WorkspaceTemplate _template = getWebServiceObject().getWorkspaceTemplate();
        return _template == null ? null : new WorkspaceTemplate2010(_template);
    }

    public void setBuildControllerURI(final String value) {
        getWebServiceObject().setBuildControllerUri(value);
    }

    public void setConfigurationFolderURI(final String value) {
        getWebServiceObject().setConfigurationFolderUri(value);
    }

    public void setContinuousIntegrationQuietPeriod(final int value) {
        getWebServiceObject().setContinuousIntegrationQuietPeriod(value);
    }

    public void setContinuousIntegrationType(final ContinuousIntegrationType continuousIntegrationType) {
        getWebServiceObject().setContinuousIntegrationType(continuousIntegrationType.getWebServiceObject());
    }

    public void setDefaultBuildAgentURI(final String value) {
        getWebServiceObject().setDefaultBuildAgentUri(value);
    }

    public void setDefaultDropLocation(final String value) {
        getWebServiceObject().setDefaultDropLocation(value);
    }

    public void setDescription(final String description) {
        getWebServiceObject().setDescription(description);
    }

    public void setEnabled(final boolean enabled) {
        getWebServiceObject().setEnabled(enabled);
    }

    public void setLastBuildUri(final String value) {
        getWebServiceObject().setLastBuildUri(value);
    }

    public void setLastGoodBuildLabel(final String value) {
        getWebServiceObject().setLastGoodBuildLabel(value);
    }

    public void setLastGoodBuildUri(final String value) {
        getWebServiceObject().setLastGoodBuildUri(value);
    }

    public void setMaxTimeout(final int value) {
        getWebServiceObject().setMaxTimeout(value);
    }

    public void setProcess(final ProcessTemplate2010 value) {
        getWebServiceObject().setProcess(value.getWebServiceObject());
    }

    public void setProcessParameters(final String processParamaters) {
        getWebServiceObject().setProcessParameters(processParamaters);
    }

    public void setRetentionPolicies(final RetentionPolicy2010[] value) {
        getWebServiceObject().setRetentionPolicies(
            (_RetentionPolicy[]) WrapperUtils.unwrap(_RetentionPolicy.class, value));
    }

    public void setSchedules(final Schedule2010[] value) {
        getWebServiceObject().setSchedules((_Schedule[]) WrapperUtils.unwrap(_Schedule.class, value));
    }

    public void setWorkspaceTemplate(final WorkspaceTemplate2010 value) {
        getWebServiceObject().setWorkspaceTemplate(value.getWebServiceObject());
    }

    @Override
    public void setURI(final String value) {
        getWebServiceObject().setUri(value);
    }
}
