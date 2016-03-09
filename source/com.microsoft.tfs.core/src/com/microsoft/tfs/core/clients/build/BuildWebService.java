// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.build;

import com.microsoft.tfs.core.TFSTeamProjectCollection;
import com.microsoft.tfs.core.clients.build.flags.DeleteOptions2010;
import com.microsoft.tfs.core.clients.build.flags.ProcessTemplateType2010;
import com.microsoft.tfs.core.clients.build.flags.QueryDeletedOption2010;
import com.microsoft.tfs.core.clients.build.flags.QueryOptions2010;
import com.microsoft.tfs.core.clients.build.internal.soapextensions.BuildDefinition2010;
import com.microsoft.tfs.core.clients.build.internal.soapextensions.BuildDefinitionQueryResult2010;
import com.microsoft.tfs.core.clients.build.internal.soapextensions.BuildDefinitionSpec2010;
import com.microsoft.tfs.core.clients.build.internal.soapextensions.BuildDeletionResult2010;
import com.microsoft.tfs.core.clients.build.internal.soapextensions.BuildDetail2010;
import com.microsoft.tfs.core.clients.build.internal.soapextensions.BuildDetailSpec2010;
import com.microsoft.tfs.core.clients.build.internal.soapextensions.BuildInformationNode2010;
import com.microsoft.tfs.core.clients.build.internal.soapextensions.BuildQueryResult2010;
import com.microsoft.tfs.core.clients.build.internal.soapextensions.BuildUpdateOptions2010;
import com.microsoft.tfs.core.clients.build.internal.soapextensions.InformationChangeRequest2010;
import com.microsoft.tfs.core.clients.build.internal.soapextensions.ProcessTemplate2010;
import com.microsoft.tfs.core.clients.build.soapextensions.ContinuousIntegrationType;
import com.microsoft.tfs.core.internal.wrappers.WrapperUtils;

import ms.tfs.build.buildservice._03._BuildDefinition;
import ms.tfs.build.buildservice._03._BuildDefinitionQueryResult;
import ms.tfs.build.buildservice._03._BuildDefinitionSpec;
import ms.tfs.build.buildservice._03._BuildDeletionResult;
import ms.tfs.build.buildservice._03._BuildDetail;
import ms.tfs.build.buildservice._03._BuildDetailSpec;
import ms.tfs.build.buildservice._03._BuildInformationNode;
import ms.tfs.build.buildservice._03._BuildQueryResult;
import ms.tfs.build.buildservice._03._BuildUpdateOptions;
import ms.tfs.build.buildservice._03._BuildWebServiceSoap;
import ms.tfs.build.buildservice._03._InformationChangeRequest;
import ms.tfs.build.buildservice._03._ProcessTemplate;
import ms.tfs.build.buildservice._03._ProcessTemplateType;

public class BuildWebService {
    private final _BuildWebServiceSoap webService;

    public BuildWebService(final TFSTeamProjectCollection tfs) {
        webService = (_BuildWebServiceSoap) tfs.getWebService(_BuildWebServiceSoap.class);
    }

    public BuildDetail2010 notifyBuildCompleted(final String uri) {
        final _BuildDetail _result = webService.notifyBuildCompleted(uri);
        return new BuildDetail2010(_result);
    }

    public BuildDetail2010[] updateBuilds(final BuildUpdateOptions2010[] updates) {
        final _BuildUpdateOptions[] _updates =
            (_BuildUpdateOptions[]) WrapperUtils.unwrap(_BuildUpdateOptions.class, updates);
        final _BuildDetail[] _results = webService.updateBuilds(_updates);
        return (BuildDetail2010[]) WrapperUtils.wrap(BuildDetail2010.class, _results);
    }

    public BuildDefinition2010[] getAffectedBuildDefinitions(
        final String[] serverItems,
        final ContinuousIntegrationType type) {
        final _BuildDefinition[] _results =
            webService.getAffectedBuildDefinitions(serverItems, type.getWebServiceObject());
        return (BuildDefinition2010[]) WrapperUtils.wrap(BuildDefinition2010.class, _results);
    }

    public BuildQueryResult2010 queryBuildsByUri(
        final String[] uris,
        final String[] informationTypes,
        final QueryOptions2010 options,
        final QueryDeletedOption2010 deletedOption) {
        final _BuildQueryResult _result = webService.queryBuildsByUri(
            uris,
            informationTypes,
            options.getWebServiceObject(),
            deletedOption.getWebServiceObject());

        return new BuildQueryResult2010(_result);
    }

    public BuildDeletionResult2010[] deleteBuilds(final String[] uris, final DeleteOptions2010 deleteOptions) {
        final _BuildDeletionResult[] _results = webService.deleteBuilds(uris, deleteOptions.getWebServiceObject());
        return (BuildDeletionResult2010[]) WrapperUtils.wrap(BuildDeletionResult2010.class, _results);
    }

    public void destroyBuilds(final String[] uris) {
        webService.destroyBuilds(uris);
    }

    public BuildDefinitionQueryResult2010 queryBuildDefinitionsByUri(
        final String[] uris,
        final QueryOptions2010 options) {
        final _BuildDefinitionQueryResult _result =
            webService.queryBuildDefinitionsByUri(uris, options.getWebServiceObject());
        return new BuildDefinitionQueryResult2010(_result);
    }

    public void deleteBuildDefinitions(final String[] definitionUris) {
        webService.deleteBuildDefinitions(definitionUris);
    }

    public BuildQueryResult2010[] queryBuilds(final BuildDetailSpec2010[] specs) {
        final _BuildDetailSpec[] _specs = (_BuildDetailSpec[]) WrapperUtils.unwrap(_BuildDetailSpec.class, specs);
        final _BuildQueryResult[] _results = webService.queryBuilds(_specs);
        return (BuildQueryResult2010[]) WrapperUtils.wrap(BuildQueryResult2010.class, _results);
    }

    public void stopBuilds(final String[] uris) {
        webService.stopBuilds(uris);
    }

    public void addBuildQualities(final String teamProject, final String[] qualities) {
        webService.addBuildQualities(teamProject, qualities);
    }

    public void deleteBuildQualities(final String teamProject, final String[] qualities) {
        webService.deleteBuildQualities(teamProject, qualities);
    }

    public String[] getBuildQualities(final String teamProject) {
        return webService.getBuildQualities(teamProject);
    }

    public ProcessTemplate2010[] queryProcessTemplates(
        final String teamProject,
        final ProcessTemplateType2010[] types) {
        final _ProcessTemplateType[] _types =
            (_ProcessTemplateType[]) WrapperUtils.unwrap(_ProcessTemplateType.class, types);
        final _ProcessTemplate[] _results = webService.queryProcessTemplates(teamProject, _types);
        return (ProcessTemplate2010[]) WrapperUtils.wrap(ProcessTemplate2010.class, _results);
    }

    public void deleteProcessTemplates(final int[] templateIds) {
        webService.deleteProcessTemplates(templateIds);
    }

    public ProcessTemplate2010[] addProcessTemplates(final ProcessTemplate2010[] templates) {
        final _ProcessTemplate[] _templates =
            (_ProcessTemplate[]) WrapperUtils.unwrap(_ProcessTemplate.class, templates);
        final _ProcessTemplate[] _results = webService.addProcessTemplates(_templates);
        return (ProcessTemplate2010[]) WrapperUtils.wrap(ProcessTemplate2010.class, _results);
    }

    public ProcessTemplate2010[] updateProcessTemplates(final ProcessTemplate2010[] templates) {
        final _ProcessTemplate[] _templates =
            (_ProcessTemplate[]) WrapperUtils.unwrap(_ProcessTemplate.class, templates);
        final _ProcessTemplate[] _results = webService.updateProcessTemplates(_templates);
        return (ProcessTemplate2010[]) WrapperUtils.wrap(ProcessTemplate2010.class, _results);
    }

    public BuildDefinition2010[] addBuildDefinitions(final BuildDefinition2010[] definitions) {
        final _BuildDefinition[] _definitions =
            (_BuildDefinition[]) WrapperUtils.unwrap(_BuildDefinition.class, definitions);
        final _BuildDefinition[] _results = webService.addBuildDefinitions(_definitions);
        return (BuildDefinition2010[]) WrapperUtils.wrap(BuildDefinition2010.class, _results);
    }

    public BuildDefinition2010[] updateBuildDefinitions(final BuildDefinition2010[] updates) {
        final _BuildDefinition[] _updates = (_BuildDefinition[]) WrapperUtils.unwrap(_BuildDefinition.class, updates);
        final _BuildDefinition[] _results = webService.updateBuildDefinitions(_updates);
        return (BuildDefinition2010[]) WrapperUtils.wrap(BuildDefinition2010.class, _results);
    }

    public BuildDefinitionQueryResult2010[] queryBuildDefinitions(final BuildDefinitionSpec2010[] specs) {
        final _BuildDefinitionSpec[] _specs =
            (_BuildDefinitionSpec[]) WrapperUtils.unwrap(_BuildDefinitionSpec.class, specs);
        final _BuildDefinitionQueryResult[] _results = webService.queryBuildDefinitions(_specs);
        return (BuildDefinitionQueryResult2010[]) WrapperUtils.wrap(BuildDefinitionQueryResult2010.class, _results);
    }

    public BuildInformationNode2010[] updateBuildInformation(final InformationChangeRequest2010[] changes) {
        final _InformationChangeRequest[] _changes =
            (_InformationChangeRequest[]) WrapperUtils.unwrap(_InformationChangeRequest.class, changes);
        final _BuildInformationNode[] _results = webService.updateBuildInformation(_changes);
        return (BuildInformationNode2010[]) WrapperUtils.wrap(BuildInformationNode2010.class, _results);
    }
}
