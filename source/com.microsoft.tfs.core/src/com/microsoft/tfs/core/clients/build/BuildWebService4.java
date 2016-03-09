// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.build;

import com.microsoft.tfs.core.TFSTeamProjectCollection;
import com.microsoft.tfs.core.clients.build.flags.DeleteOptions;
import com.microsoft.tfs.core.clients.build.flags.QueryDeletedOption;
import com.microsoft.tfs.core.clients.build.flags.QueryOptions;
import com.microsoft.tfs.core.clients.build.internal.soapextensions.BuildDefinitionQueryResult;
import com.microsoft.tfs.core.clients.build.internal.soapextensions.BuildDetail;
import com.microsoft.tfs.core.clients.build.internal.soapextensions.BuildInformationNode;
import com.microsoft.tfs.core.clients.build.internal.soapextensions.BuildQueryResult;
import com.microsoft.tfs.core.clients.build.internal.soapextensions.BuildUpdateOptions;
import com.microsoft.tfs.core.clients.build.internal.soapextensions.InformationChangeRequest;
import com.microsoft.tfs.core.clients.build.internal.soapextensions.ProcessTemplate;
import com.microsoft.tfs.core.clients.build.internal.utils.BuildTypeConvertor;
import com.microsoft.tfs.core.clients.build.soapextensions.DefinitionTriggerType;
import com.microsoft.tfs.core.clients.build.soapextensions.ProcessTemplateType;
import com.microsoft.tfs.core.internal.wrappers.WrapperUtils;
import com.microsoft.tfs.util.GUID;

import ms.tfs.build.buildservice._04._BuildDefinition;
import ms.tfs.build.buildservice._04._BuildDefinitionQueryResult;
import ms.tfs.build.buildservice._04._BuildDefinitionSpec;
import ms.tfs.build.buildservice._04._BuildDeletionResult;
import ms.tfs.build.buildservice._04._BuildDetail;
import ms.tfs.build.buildservice._04._BuildDetailSpec;
import ms.tfs.build.buildservice._04._BuildInformationNode;
import ms.tfs.build.buildservice._04._BuildQueryResult;
import ms.tfs.build.buildservice._04._BuildServiceSoap;
import ms.tfs.build.buildservice._04._BuildUpdateOptions;
import ms.tfs.build.buildservice._04._DefinitionTriggerType;
import ms.tfs.build.buildservice._04._DeleteOptions;
import ms.tfs.build.buildservice._04._InformationChangeRequest;
import ms.tfs.build.buildservice._04._ProcessTemplate;
import ms.tfs.build.buildservice._04._ProcessTemplateType;
import ms.tfs.build.buildservice._04._QueryDeletedOption;
import ms.tfs.build.buildservice._04._QueryOptions;

public class BuildWebService4 {
    private final _BuildServiceSoap webService;
    private final IBuildServer buildServer;

    public BuildWebService4(final TFSTeamProjectCollection tfs) {
        webService = (_BuildServiceSoap) tfs.getWebService(_BuildServiceSoap.class);
        buildServer = tfs.getBuildServer();
    }

    public BuildDetail notifyBuildCompleted(final String uri) {
        final _BuildDetail result = webService.notifyBuildCompleted(uri);
        return new BuildDetail(buildServer, result);
    }

    public BuildInformationNode[] updateBuildInformation(final InformationChangeRequest[] changeRequests) {
        final _InformationChangeRequest[] changes =
            (_InformationChangeRequest[]) WrapperUtils.unwrap(_InformationChangeRequest.class, changeRequests);
        final _BuildInformationNode[] _results = webService.updateBuildInformation(changes);
        return BuildTypeConvertor.toBuildInformationNodeArray(_results);
    }

    public GUID requestIntermediateLogs(final String uri) {
        final String result = webService.requestIntermediateLogs(uri);
        return new GUID(result);
    }

    public BuildDetail[] updateBuilds(final BuildUpdateOptions[] buildUpdateOptions) {
        final _BuildUpdateOptions[] updateOptions =
            (_BuildUpdateOptions[]) WrapperUtils.unwrap(_BuildUpdateOptions.class, buildUpdateOptions);
        final _BuildDetail[] results = webService.updateBuilds(updateOptions);
        return BuildTypeConvertor.toBuildDetailArray(buildServer, results);
    }

    public IBuildDefinition[] getAffectedBuildDefinitions(
        final String[] serverItems,
        final DefinitionTriggerType triggerType) {
        final _DefinitionTriggerType trigger = triggerType.getWebServiceObject();
        final _BuildDefinition[] results = webService.getAffectedBuildDefinitions(serverItems, trigger);
        return BuildTypeConvertor.toBuildDefinitionArray(buildServer, results);
    }

    public IBuildQueryResult queryBuildsByUri(
        final String[] buildUris,
        final String[] informationTypes,
        final QueryOptions queryOptions,
        final QueryDeletedOption queryDeletedOption) {
        final _QueryOptions options = queryOptions.getWebServiceObject();
        final _QueryDeletedOption deletedOption = queryDeletedOption.getWebServiceObject();
        final _BuildQueryResult result =
            webService.queryBuildsByUri(buildUris, informationTypes, options, deletedOption);
        return new BuildQueryResult(buildServer, result);
    }

    public BuildQueryResult[] queryBuilds(final IBuildDetailSpec[] buildDetailSpecs) {
        final _BuildDetailSpec[] specs =
            (_BuildDetailSpec[]) WrapperUtils.unwrap(_BuildDetailSpec.class, buildDetailSpecs);
        final _BuildQueryResult[] results = webService.queryBuilds(specs);
        return BuildTypeConvertor.toBuildQueryResults(buildServer, results);
    }

    public IBuildDeletionResult[] deleteBuilds(final String[] uris, final DeleteOptions options) {
        final _DeleteOptions deleteOptions = options.getWebServiceObject();
        final _BuildDeletionResult[] results = webService.deleteBuilds(uris, deleteOptions);
        return BuildTypeConvertor.toBuildDeletionResultAray(results);
    }

    public void destroyBuilds(final String[] buildUris) {
        webService.destroyBuilds(buildUris);
    }

    public IBuildDefinitionQueryResult queryBuildDefinitionsByUri(
        final String[] buildDefinitionUris,
        final String[] propertyNameFilters,
        final QueryOptions queryOptions) {
        final _QueryOptions options = queryOptions.getWebServiceObject();
        final _BuildDefinitionQueryResult result =
            webService.queryBuildDefinitionsByUri(buildDefinitionUris, propertyNameFilters, options);
        return new BuildDefinitionQueryResult(buildServer, result);
    }

    public IBuildDefinitionQueryResult[] queryBuildDefinitions(final IBuildDefinitionSpec[] buildDefinitionSpecs) {
        final _BuildDefinitionSpec[] specs =
            (_BuildDefinitionSpec[]) WrapperUtils.unwrap(_BuildDefinitionSpec.class, buildDefinitionSpecs);
        final _BuildDefinitionQueryResult[] results = webService.queryBuildDefinitions(specs, false);
        return BuildTypeConvertor.toBuildDefinitionQueryResultArray(buildServer, results);
    }

    public void deleteBuildDefinitions(final String[] definitionUris) {
        webService.deleteBuildDefinitions(definitionUris);
    }

    public IBuildDefinition[] addBuildDefinitions(final IBuildDefinition[] toAdd) {
        final _BuildDefinition[] definitions = (_BuildDefinition[]) WrapperUtils.unwrap(_BuildDefinition.class, toAdd);
        final _BuildDefinition[] results = webService.addBuildDefinitions(definitions);
        return BuildTypeConvertor.toBuildDefinitionArray(buildServer, results);
    }

    public IBuildDefinition[] updateBuildDefinitions(final IBuildDefinition[] toUpdate) {
        final _BuildDefinition[] definitions =
            (_BuildDefinition[]) WrapperUtils.unwrap(_BuildDefinition.class, toUpdate);
        final _BuildDefinition[] results = webService.updateBuildDefinitions(definitions);
        return BuildTypeConvertor.toBuildDefinitionArray(buildServer, results);
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

    public ProcessTemplate[] queryProcessTemplates(final String teamProject, final ProcessTemplateType[] types) {
        final _ProcessTemplateType[] queryTypes =
            (_ProcessTemplateType[]) WrapperUtils.unwrap(_ProcessTemplateType.class, types);
        final _ProcessTemplate[] results = webService.queryProcessTemplates(teamProject, queryTypes);
        return BuildTypeConvertor.toProcessTemplateArray(buildServer, results);
    }

    public void deleteProcessTemplates(final int[] templateIds) {
        webService.deleteProcessTemplates(templateIds);
    }

    public ProcessTemplate[] addProcessTemplates(final ProcessTemplate[] processTemplates) {
        final _ProcessTemplate[] templates =
            (_ProcessTemplate[]) WrapperUtils.unwrap(_ProcessTemplate.class, processTemplates);
        final _ProcessTemplate[] results = webService.addProcessTemplates(templates);
        return BuildTypeConvertor.toProcessTemplateArray(buildServer, results);
    }

    public ProcessTemplate[] updateProcessTemplates(final ProcessTemplate[] toUpdate) {
        final _ProcessTemplate[] templates = (_ProcessTemplate[]) WrapperUtils.unwrap(_ProcessTemplate.class, toUpdate);
        final _ProcessTemplate[] results = webService.updateProcessTemplates(templates);
        return BuildTypeConvertor.toProcessTemplateArray(buildServer, results);
    }
}
