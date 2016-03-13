// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.build.internal.soapextensions;

import com.microsoft.tfs.core.clients.build.BuildConstants;
import com.microsoft.tfs.core.clients.build.IBuildDefinition;
import com.microsoft.tfs.core.clients.build.IBuildDefinitionSpec;
import com.microsoft.tfs.core.clients.build.flags.QueryOptions;
import com.microsoft.tfs.core.clients.build.soapextensions.ContinuousIntegrationType;
import com.microsoft.tfs.core.clients.build.soapextensions.DefinitionTriggerType;
import com.microsoft.tfs.core.clients.build.utils.BuildPath;
import com.microsoft.tfs.core.internal.wrappers.WebServiceObjectWrapper;
import com.microsoft.tfs.util.StringUtil;

import ms.tfs.build.buildservice._04._BuildDefinitionSpec;

public class BuildDefinitionSpec extends WebServiceObjectWrapper implements IBuildDefinitionSpec {
    private String name;
    private String teamProject;

    private BuildDefinitionSpec() {
        super(new _BuildDefinitionSpec());
    }

    public BuildDefinitionSpec(final _BuildDefinitionSpec value) {
        super(value);

        final String fullPath = value.getFullPath();
        if (fullPath != null) {
            name = BuildPath.getItemName(fullPath);
            teamProject = BuildPath.getTeamProject(fullPath);
        }
        if (StringUtil.isNullOrEmpty(name)) {
            name = BuildConstants.STAR;
        }
        if (StringUtil.isNullOrEmpty(teamProject)) {
            teamProject = BuildConstants.STAR;
        }
    }

    public BuildDefinitionSpec(final IBuildDefinition definition) {
        this(definition.getTeamProject(), definition.getName());
    }

    public BuildDefinitionSpec(final String teamProject) {
        this(teamProject, BuildConstants.STAR);
    }

    public BuildDefinitionSpec(final String teamProject, final String definitionName) {
        this(teamProject, definitionName, null);
    }

    public BuildDefinitionSpec(final String teamProject, final String name, final String[] propertyNameFilters) {
        this();

        final _BuildDefinitionSpec spec = getWebServiceObject();

        this.name = StringUtil.isNullOrEmpty(name) ? BuildConstants.STAR : name;
        this.teamProject = StringUtil.isNullOrEmpty(teamProject) ? BuildConstants.STAR : teamProject;
        spec.setFullPath(BuildPath.combine(teamProject, name));

        if (propertyNameFilters != null) {
            spec.setPropertyNameFilters(propertyNameFilters);
        }
    }

    public _BuildDefinitionSpec getWebServiceObject() {
        return (_BuildDefinitionSpec) this.webServiceObject;
    }

    @Override
    public QueryOptions getOptions() {
        return QueryOptions.fromWebServiceObject(getWebServiceObject().getOptions());
    }

    @Override
    public void setOptions(final QueryOptions value) {
        getWebServiceObject().setOptions(value.getWebServiceObject());
    }

    @Override
    public DefinitionTriggerType getTriggerType() {
        return new DefinitionTriggerType(getWebServiceObject().getTriggerType());
    }

    @Override
    public void setTriggerType(final DefinitionTriggerType value) {
        getWebServiceObject().setTriggerType(value.getWebServiceObject());
    }

    @Override
    public String getFullPath() {
        return getWebServiceObject().getFullPath();
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(final String value) {
        name = value;
        getWebServiceObject().setFullPath(BuildPath.root(teamProject, name));
    }

    @Override
    public String getTeamProject() {
        return teamProject;
    }

    public void setTeamProject(final String value) {
        teamProject = value;
        getWebServiceObject().setFullPath(BuildPath.root(teamProject, name));
    }

    @Override
    public ContinuousIntegrationType getContinuousIntegrationType() {
        return TFS2010Helper.convert(new DefinitionTriggerType(getWebServiceObject().getTriggerType()));
    }

    @Override
    public void setContinuousIntegrationType(final ContinuousIntegrationType value) {
        getWebServiceObject().setTriggerType(TFS2010Helper.convert(value).getWebServiceObject());
    }

    @Override
    public String[] getPropertyNameFilters() {
        return getWebServiceObject().getPropertyNameFilters();
    }

    @Override
    public void setPropertyNameFilters(final String[] value) {
        getWebServiceObject().setPropertyNameFilters(value);
    }
}
