// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

 /*
* ---------------------------------------------------------
* Generated file, DO NOT EDIT
* ---------------------------------------------------------
*
* See following wiki page for instructions on how to regenerate:
*   https://vsowiki.com/index.php?title=Rest_Client_Generation
*/

package com.microsoft.teamfoundation.build.webapi;

import java.io.InputStream;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import com.fasterxml.jackson.core.type.TypeReference;
import com.microsoft.teamfoundation.build.webapi.model.AgentPoolQueue;
import com.microsoft.teamfoundation.build.webapi.model.Build;
import com.microsoft.teamfoundation.build.webapi.model.BuildArtifact;
import com.microsoft.teamfoundation.build.webapi.model.BuildController;
import com.microsoft.teamfoundation.build.webapi.model.BuildDefinition;
import com.microsoft.teamfoundation.build.webapi.model.BuildDefinitionRevision;
import com.microsoft.teamfoundation.build.webapi.model.BuildDefinitionTemplate;
import com.microsoft.teamfoundation.build.webapi.model.BuildLog;
import com.microsoft.teamfoundation.build.webapi.model.BuildOptionDefinition;
import com.microsoft.teamfoundation.build.webapi.model.BuildReason;
import com.microsoft.teamfoundation.build.webapi.model.BuildResult;
import com.microsoft.teamfoundation.build.webapi.model.BuildSettings;
import com.microsoft.teamfoundation.build.webapi.model.BuildStatus;
import com.microsoft.teamfoundation.build.webapi.model.Change;
import com.microsoft.teamfoundation.build.webapi.model.DefinitionReference;
import com.microsoft.teamfoundation.build.webapi.model.DefinitionType;
import com.microsoft.teamfoundation.build.webapi.model.Deployment;
import com.microsoft.teamfoundation.build.webapi.model.Timeline;
import com.microsoft.visualstudio.services.webapi.model.ResourceRef;
import com.microsoft.vss.client.core.model.ApiResourceVersion;
import com.microsoft.vss.client.core.model.NameValueCollection;
import com.microsoft.vss.client.core.VssHttpClientBase;

public abstract class BuildHttpClientBase
    extends VssHttpClientBase {

    private final static Map<String, Class<? extends Exception>> TRANSLATED_EXCEPTIONS;

    static {
        TRANSLATED_EXCEPTIONS = new HashMap<String, Class<? extends Exception>>();
    }

    /**
    * Create a new instance of BuildHttpClientBase
    *
    * @param jaxrsClient
    *            an initialized instance of a JAX-RS Client implementation
    * @param baseUrl
    *            a TFS project collection URL
    */
    public BuildHttpClientBase(final Object jaxrsClient, final URI baseUrl) {
        super(jaxrsClient, baseUrl);
    }

    /**
    * Create a new instance of BuildHttpClientBase
    *
    * @param tfsConnection
    *            an initialized instance of a TfsTeamProjectCollection
    */
    public BuildHttpClientBase(final Object tfsConnection) {
        super(tfsConnection);
    }

    @Override
    protected Map<String, Class<? extends Exception>> getTranslatedExceptions() {
        return TRANSLATED_EXCEPTIONS;
    }

    /** 
     * Associates an artifact with a build
     * 
     * @param artifact 
     *            
     * @param buildId 
     *            
     * @return BuildArtifact
     */
    public BuildArtifact createArtifact(
        final BuildArtifact artifact, 
        final int buildId) {

        final UUID locationId = UUID.fromString("1db06c96-014e-44e1-ac91-90b2d4b3e984"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("buildId", buildId); //$NON-NLS-1$

        final Object httpRequest = super.createRequest(HttpMethod.POST,
                                                       locationId,
                                                       routeValues,
                                                       apiVersion,
                                                       artifact,
                                                       APPLICATION_JSON_TYPE,
                                                       APPLICATION_JSON_TYPE);

        return super.sendRequest(httpRequest, BuildArtifact.class);
    }

    /** 
     * Associates an artifact with a build
     * 
     * @param artifact 
     *            
     * @param project 
     *            Project ID or project name
     * @param buildId 
     *            
     * @return BuildArtifact
     */
    public BuildArtifact createArtifact(
        final BuildArtifact artifact, 
        final String project, 
        final int buildId) {

        final UUID locationId = UUID.fromString("1db06c96-014e-44e1-ac91-90b2d4b3e984"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("project", project); //$NON-NLS-1$
        routeValues.put("buildId", buildId); //$NON-NLS-1$

        final Object httpRequest = super.createRequest(HttpMethod.POST,
                                                       locationId,
                                                       routeValues,
                                                       apiVersion,
                                                       artifact,
                                                       APPLICATION_JSON_TYPE,
                                                       APPLICATION_JSON_TYPE);

        return super.sendRequest(httpRequest, BuildArtifact.class);
    }

    /** 
     * Associates an artifact with a build
     * 
     * @param artifact 
     *            
     * @param project 
     *            Project ID
     * @param buildId 
     *            
     * @return BuildArtifact
     */
    public BuildArtifact createArtifact(
        final BuildArtifact artifact, 
        final UUID project, 
        final int buildId) {

        final UUID locationId = UUID.fromString("1db06c96-014e-44e1-ac91-90b2d4b3e984"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("project", project); //$NON-NLS-1$
        routeValues.put("buildId", buildId); //$NON-NLS-1$

        final Object httpRequest = super.createRequest(HttpMethod.POST,
                                                       locationId,
                                                       routeValues,
                                                       apiVersion,
                                                       artifact,
                                                       APPLICATION_JSON_TYPE,
                                                       APPLICATION_JSON_TYPE);

        return super.sendRequest(httpRequest, BuildArtifact.class);
    }

    /** 
     * Gets a specific artifact for a build
     * 
     * @param buildId 
     *            
     * @param artifactName 
     *            
     * @return BuildArtifact
     */
    public BuildArtifact getArtifact(
        final int buildId, 
        final String artifactName) {

        final UUID locationId = UUID.fromString("1db06c96-014e-44e1-ac91-90b2d4b3e984"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("buildId", buildId); //$NON-NLS-1$
        routeValues.put("artifactName", artifactName); //$NON-NLS-1$

        final Object httpRequest = super.createRequest(HttpMethod.GET,
                                                       locationId,
                                                       routeValues,
                                                       apiVersion,
                                                       APPLICATION_JSON_TYPE);

        return super.sendRequest(httpRequest, BuildArtifact.class);
    }

    /** 
     * Gets a specific artifact for a build
     * 
     * @param project 
     *            Project ID or project name
     * @param buildId 
     *            
     * @param artifactName 
     *            
     * @return BuildArtifact
     */
    public BuildArtifact getArtifact(
        final String project, 
        final int buildId, 
        final String artifactName) {

        final UUID locationId = UUID.fromString("1db06c96-014e-44e1-ac91-90b2d4b3e984"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("project", project); //$NON-NLS-1$
        routeValues.put("buildId", buildId); //$NON-NLS-1$
        routeValues.put("artifactName", artifactName); //$NON-NLS-1$

        final Object httpRequest = super.createRequest(HttpMethod.GET,
                                                       locationId,
                                                       routeValues,
                                                       apiVersion,
                                                       APPLICATION_JSON_TYPE);

        return super.sendRequest(httpRequest, BuildArtifact.class);
    }

    /** 
     * Gets a specific artifact for a build
     * 
     * @param project 
     *            Project ID
     * @param buildId 
     *            
     * @param artifactName 
     *            
     * @return BuildArtifact
     */
    public BuildArtifact getArtifact(
        final UUID project, 
        final int buildId, 
        final String artifactName) {

        final UUID locationId = UUID.fromString("1db06c96-014e-44e1-ac91-90b2d4b3e984"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("project", project); //$NON-NLS-1$
        routeValues.put("buildId", buildId); //$NON-NLS-1$
        routeValues.put("artifactName", artifactName); //$NON-NLS-1$

        final Object httpRequest = super.createRequest(HttpMethod.GET,
                                                       locationId,
                                                       routeValues,
                                                       apiVersion,
                                                       APPLICATION_JSON_TYPE);

        return super.sendRequest(httpRequest, BuildArtifact.class);
    }

    /** 
     * Gets a specific artifact for a build
     * 
     * @param buildId 
     *            
     * @param artifactName 
     *            
     * @return InputStream
     */
    public InputStream getArtifactContentZip(
        final int buildId, 
        final String artifactName) {

        final UUID locationId = UUID.fromString("1db06c96-014e-44e1-ac91-90b2d4b3e984"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("buildId", buildId); //$NON-NLS-1$
        routeValues.put("artifactName", artifactName); //$NON-NLS-1$

        final Object httpRequest = super.createRequest(HttpMethod.GET,
                                                       locationId,
                                                       routeValues,
                                                       apiVersion,
                                                       APPLICATION_ZIP_TYPE);

        return super.sendRequest(httpRequest, InputStream.class);
    }

    /** 
     * Gets a specific artifact for a build
     * 
     * @param project 
     *            Project ID or project name
     * @param buildId 
     *            
     * @param artifactName 
     *            
     * @return InputStream
     */
    public InputStream getArtifactContentZip(
        final String project, 
        final int buildId, 
        final String artifactName) {

        final UUID locationId = UUID.fromString("1db06c96-014e-44e1-ac91-90b2d4b3e984"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("project", project); //$NON-NLS-1$
        routeValues.put("buildId", buildId); //$NON-NLS-1$
        routeValues.put("artifactName", artifactName); //$NON-NLS-1$

        final Object httpRequest = super.createRequest(HttpMethod.GET,
                                                       locationId,
                                                       routeValues,
                                                       apiVersion,
                                                       APPLICATION_ZIP_TYPE);

        return super.sendRequest(httpRequest, InputStream.class);
    }

    /** 
     * Gets a specific artifact for a build
     * 
     * @param project 
     *            Project ID
     * @param buildId 
     *            
     * @param artifactName 
     *            
     * @return InputStream
     */
    public InputStream getArtifactContentZip(
        final UUID project, 
        final int buildId, 
        final String artifactName) {

        final UUID locationId = UUID.fromString("1db06c96-014e-44e1-ac91-90b2d4b3e984"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("project", project); //$NON-NLS-1$
        routeValues.put("buildId", buildId); //$NON-NLS-1$
        routeValues.put("artifactName", artifactName); //$NON-NLS-1$

        final Object httpRequest = super.createRequest(HttpMethod.GET,
                                                       locationId,
                                                       routeValues,
                                                       apiVersion,
                                                       APPLICATION_ZIP_TYPE);

        return super.sendRequest(httpRequest, InputStream.class);
    }

    /** 
     * Gets all artifacts for a build
     * 
     * @param buildId 
     *            
     * @return List<BuildArtifact>
     */
    public List<BuildArtifact> getArtifacts(
        final int buildId) {

        final UUID locationId = UUID.fromString("1db06c96-014e-44e1-ac91-90b2d4b3e984"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("buildId", buildId); //$NON-NLS-1$

        final Object httpRequest = super.createRequest(HttpMethod.GET,
                                                       locationId,
                                                       routeValues,
                                                       apiVersion,
                                                       APPLICATION_JSON_TYPE);

        return super.sendRequest(httpRequest, new TypeReference<List<BuildArtifact>>() {});
    }

    /** 
     * Gets all artifacts for a build
     * 
     * @param project 
     *            Project ID or project name
     * @param buildId 
     *            
     * @return List<BuildArtifact>
     */
    public List<BuildArtifact> getArtifacts(
        final String project, 
        final int buildId) {

        final UUID locationId = UUID.fromString("1db06c96-014e-44e1-ac91-90b2d4b3e984"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("project", project); //$NON-NLS-1$
        routeValues.put("buildId", buildId); //$NON-NLS-1$

        final Object httpRequest = super.createRequest(HttpMethod.GET,
                                                       locationId,
                                                       routeValues,
                                                       apiVersion,
                                                       APPLICATION_JSON_TYPE);

        return super.sendRequest(httpRequest, new TypeReference<List<BuildArtifact>>() {});
    }

    /** 
     * Gets all artifacts for a build
     * 
     * @param project 
     *            Project ID
     * @param buildId 
     *            
     * @return List<BuildArtifact>
     */
    public List<BuildArtifact> getArtifacts(
        final UUID project, 
        final int buildId) {

        final UUID locationId = UUID.fromString("1db06c96-014e-44e1-ac91-90b2d4b3e984"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("project", project); //$NON-NLS-1$
        routeValues.put("buildId", buildId); //$NON-NLS-1$

        final Object httpRequest = super.createRequest(HttpMethod.GET,
                                                       locationId,
                                                       routeValues,
                                                       apiVersion,
                                                       APPLICATION_JSON_TYPE);

        return super.sendRequest(httpRequest, new TypeReference<List<BuildArtifact>>() {});
    }

    /** 
     * @param project 
     *            
     * @param definitionId 
     *            
     * @param branchName 
     *            
     * @return String
     */
    public String getBadge(
        final UUID project, 
        final int definitionId, 
        final String branchName) {

        final UUID locationId = UUID.fromString("de6a4df8-22cd-44ee-af2d-39f6aa7a4261"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("project", project); //$NON-NLS-1$
        routeValues.put("definitionId", definitionId); //$NON-NLS-1$

        final NameValueCollection queryParameters = new NameValueCollection();
        queryParameters.addIfNotEmpty("branchName", branchName); //$NON-NLS-1$

        final Object httpRequest = super.createRequest(HttpMethod.GET,
                                                       locationId,
                                                       routeValues,
                                                       apiVersion,
                                                       queryParameters,
                                                       APPLICATION_JSON_TYPE);

        return super.sendRequest(httpRequest, String.class);
    }

    /** 
     * Deletes a build
     * 
     * @param buildId 
     *            
     */
    public void deleteBuild(
        final int buildId) {

        final UUID locationId = UUID.fromString("0cd358e1-9217-4d94-8269-1c1ee6f93dcf"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("buildId", buildId); //$NON-NLS-1$

        final Object httpRequest = super.createRequest(HttpMethod.DELETE,
                                                       locationId,
                                                       routeValues,
                                                       apiVersion,
                                                       APPLICATION_JSON_TYPE);

        super.sendRequest(httpRequest);
    }

    /** 
     * Deletes a build
     * 
     * @param project 
     *            Project ID or project name
     * @param buildId 
     *            
     */
    public void deleteBuild(
        final String project, 
        final int buildId) {

        final UUID locationId = UUID.fromString("0cd358e1-9217-4d94-8269-1c1ee6f93dcf"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("project", project); //$NON-NLS-1$
        routeValues.put("buildId", buildId); //$NON-NLS-1$

        final Object httpRequest = super.createRequest(HttpMethod.DELETE,
                                                       locationId,
                                                       routeValues,
                                                       apiVersion,
                                                       APPLICATION_JSON_TYPE);

        super.sendRequest(httpRequest);
    }

    /** 
     * Deletes a build
     * 
     * @param project 
     *            Project ID
     * @param buildId 
     *            
     */
    public void deleteBuild(
        final UUID project, 
        final int buildId) {

        final UUID locationId = UUID.fromString("0cd358e1-9217-4d94-8269-1c1ee6f93dcf"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("project", project); //$NON-NLS-1$
        routeValues.put("buildId", buildId); //$NON-NLS-1$

        final Object httpRequest = super.createRequest(HttpMethod.DELETE,
                                                       locationId,
                                                       routeValues,
                                                       apiVersion,
                                                       APPLICATION_JSON_TYPE);

        super.sendRequest(httpRequest);
    }

    /** 
     * Gets a build
     * 
     * @param project 
     *            Project ID or project name
     * @param buildId 
     *            
     * @param propertyFilters 
     *            A comma-delimited list of properties to include in the results
     * @return Build
     */
    public Build getBuild(
        final String project, 
        final int buildId, 
        final String propertyFilters) {

        final UUID locationId = UUID.fromString("0cd358e1-9217-4d94-8269-1c1ee6f93dcf"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("project", project); //$NON-NLS-1$
        routeValues.put("buildId", buildId); //$NON-NLS-1$

        final NameValueCollection queryParameters = new NameValueCollection();
        queryParameters.addIfNotEmpty("propertyFilters", propertyFilters); //$NON-NLS-1$

        final Object httpRequest = super.createRequest(HttpMethod.GET,
                                                       locationId,
                                                       routeValues,
                                                       apiVersion,
                                                       queryParameters,
                                                       APPLICATION_JSON_TYPE);

        return super.sendRequest(httpRequest, Build.class);
    }

    /** 
     * Gets a build
     * 
     * @param project 
     *            Project ID
     * @param buildId 
     *            
     * @param propertyFilters 
     *            A comma-delimited list of properties to include in the results
     * @return Build
     */
    public Build getBuild(
        final UUID project, 
        final int buildId, 
        final String propertyFilters) {

        final UUID locationId = UUID.fromString("0cd358e1-9217-4d94-8269-1c1ee6f93dcf"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("project", project); //$NON-NLS-1$
        routeValues.put("buildId", buildId); //$NON-NLS-1$

        final NameValueCollection queryParameters = new NameValueCollection();
        queryParameters.addIfNotEmpty("propertyFilters", propertyFilters); //$NON-NLS-1$

        final Object httpRequest = super.createRequest(HttpMethod.GET,
                                                       locationId,
                                                       routeValues,
                                                       apiVersion,
                                                       queryParameters,
                                                       APPLICATION_JSON_TYPE);

        return super.sendRequest(httpRequest, Build.class);
    }

    /** 
     * Gets a build
     * 
     * @param buildId 
     *            
     * @param propertyFilters 
     *            A comma-delimited list of properties to include in the results
     * @return Build
     */
    public Build getBuild(
        final int buildId, 
        final String propertyFilters) {

        final UUID locationId = UUID.fromString("0cd358e1-9217-4d94-8269-1c1ee6f93dcf"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("buildId", buildId); //$NON-NLS-1$

        final NameValueCollection queryParameters = new NameValueCollection();
        queryParameters.addIfNotEmpty("propertyFilters", propertyFilters); //$NON-NLS-1$

        final Object httpRequest = super.createRequest(HttpMethod.GET,
                                                       locationId,
                                                       routeValues,
                                                       apiVersion,
                                                       queryParameters,
                                                       APPLICATION_JSON_TYPE);

        return super.sendRequest(httpRequest, Build.class);
    }

    /** 
     * Gets builds
     * 
     * @param project 
     *            Project ID or project name
     * @param definitions 
     *            A comma-delimited list of definition ids
     * @param queues 
     *            A comma-delimited list of queue ids
     * @param buildNumber 
     *            
     * @param minFinishTime 
     *            
     * @param maxFinishTime 
     *            
     * @param requestedFor 
     *            
     * @param reasonFilter 
     *            
     * @param statusFilter 
     *            
     * @param resultFilter 
     *            
     * @param tagFilters 
     *            A comma-delimited list of tags
     * @param properties 
     *            A comma-delimited list of properties to include in the results
     * @param type 
     *            The definition type
     * @param top 
     *            The maximum number of builds to retrieve
     * @param continuationToken 
     *            
     * @param maxBuildsPerDefinition 
     *            
     * @return List<Build>
     */
    public List<Build> getBuilds(
        final String project, 
        final List<Integer> definitions, 
        final List<Integer> queues, 
        final String buildNumber, 
        final java.util.Date minFinishTime, 
        final java.util.Date maxFinishTime, 
        final String requestedFor, 
        final BuildReason reasonFilter, 
        final BuildStatus statusFilter, 
        final BuildResult resultFilter, 
        final List<String> tagFilters, 
        final List<String> properties, 
        final DefinitionType type, 
        final Integer top, 
        final String continuationToken, 
        final Integer maxBuildsPerDefinition) {

        final UUID locationId = UUID.fromString("0cd358e1-9217-4d94-8269-1c1ee6f93dcf"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("project", project); //$NON-NLS-1$

        final NameValueCollection queryParameters = new NameValueCollection();
        queryParameters.addIfNotNull("definitions", definitions); //$NON-NLS-1$
        queryParameters.addIfNotNull("queues", queues); //$NON-NLS-1$
        queryParameters.addIfNotEmpty("buildNumber", buildNumber); //$NON-NLS-1$
        queryParameters.addIfNotNull("minFinishTime", minFinishTime); //$NON-NLS-1$
        queryParameters.addIfNotNull("maxFinishTime", maxFinishTime); //$NON-NLS-1$
        queryParameters.addIfNotEmpty("requestedFor", requestedFor); //$NON-NLS-1$
        queryParameters.addIfNotNull("reasonFilter", reasonFilter); //$NON-NLS-1$
        queryParameters.addIfNotNull("statusFilter", statusFilter); //$NON-NLS-1$
        queryParameters.addIfNotNull("resultFilter", resultFilter); //$NON-NLS-1$
        queryParameters.addIfNotNull("tagFilters", tagFilters); //$NON-NLS-1$
        queryParameters.addIfNotNull("properties", properties); //$NON-NLS-1$
        queryParameters.addIfNotNull("type", type); //$NON-NLS-1$
        queryParameters.addIfNotNull("$top", top); //$NON-NLS-1$
        queryParameters.addIfNotEmpty("continuationToken", continuationToken); //$NON-NLS-1$
        queryParameters.addIfNotNull("maxBuildsPerDefinition", maxBuildsPerDefinition); //$NON-NLS-1$

        final Object httpRequest = super.createRequest(HttpMethod.GET,
                                                       locationId,
                                                       routeValues,
                                                       apiVersion,
                                                       queryParameters,
                                                       APPLICATION_JSON_TYPE);

        return super.sendRequest(httpRequest, new TypeReference<List<Build>>() {});
    }

    /** 
     * Gets builds
     * 
     * @param project 
     *            Project ID
     * @param definitions 
     *            A comma-delimited list of definition ids
     * @param queues 
     *            A comma-delimited list of queue ids
     * @param buildNumber 
     *            
     * @param minFinishTime 
     *            
     * @param maxFinishTime 
     *            
     * @param requestedFor 
     *            
     * @param reasonFilter 
     *            
     * @param statusFilter 
     *            
     * @param resultFilter 
     *            
     * @param tagFilters 
     *            A comma-delimited list of tags
     * @param properties 
     *            A comma-delimited list of properties to include in the results
     * @param type 
     *            The definition type
     * @param top 
     *            The maximum number of builds to retrieve
     * @param continuationToken 
     *            
     * @param maxBuildsPerDefinition 
     *            
     * @return List<Build>
     */
    public List<Build> getBuilds(
        final UUID project, 
        final List<Integer> definitions, 
        final List<Integer> queues, 
        final String buildNumber, 
        final java.util.Date minFinishTime, 
        final java.util.Date maxFinishTime, 
        final String requestedFor, 
        final BuildReason reasonFilter, 
        final BuildStatus statusFilter, 
        final BuildResult resultFilter, 
        final List<String> tagFilters, 
        final List<String> properties, 
        final DefinitionType type, 
        final Integer top, 
        final String continuationToken, 
        final Integer maxBuildsPerDefinition) {

        final UUID locationId = UUID.fromString("0cd358e1-9217-4d94-8269-1c1ee6f93dcf"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("project", project); //$NON-NLS-1$

        final NameValueCollection queryParameters = new NameValueCollection();
        queryParameters.addIfNotNull("definitions", definitions); //$NON-NLS-1$
        queryParameters.addIfNotNull("queues", queues); //$NON-NLS-1$
        queryParameters.addIfNotEmpty("buildNumber", buildNumber); //$NON-NLS-1$
        queryParameters.addIfNotNull("minFinishTime", minFinishTime); //$NON-NLS-1$
        queryParameters.addIfNotNull("maxFinishTime", maxFinishTime); //$NON-NLS-1$
        queryParameters.addIfNotEmpty("requestedFor", requestedFor); //$NON-NLS-1$
        queryParameters.addIfNotNull("reasonFilter", reasonFilter); //$NON-NLS-1$
        queryParameters.addIfNotNull("statusFilter", statusFilter); //$NON-NLS-1$
        queryParameters.addIfNotNull("resultFilter", resultFilter); //$NON-NLS-1$
        queryParameters.addIfNotNull("tagFilters", tagFilters); //$NON-NLS-1$
        queryParameters.addIfNotNull("properties", properties); //$NON-NLS-1$
        queryParameters.addIfNotNull("type", type); //$NON-NLS-1$
        queryParameters.addIfNotNull("$top", top); //$NON-NLS-1$
        queryParameters.addIfNotEmpty("continuationToken", continuationToken); //$NON-NLS-1$
        queryParameters.addIfNotNull("maxBuildsPerDefinition", maxBuildsPerDefinition); //$NON-NLS-1$

        final Object httpRequest = super.createRequest(HttpMethod.GET,
                                                       locationId,
                                                       routeValues,
                                                       apiVersion,
                                                       queryParameters,
                                                       APPLICATION_JSON_TYPE);

        return super.sendRequest(httpRequest, new TypeReference<List<Build>>() {});
    }

    /** 
     * Gets builds
     * 
     * @param definitions 
     *            A comma-delimited list of definition ids
     * @param queues 
     *            A comma-delimited list of queue ids
     * @param buildNumber 
     *            
     * @param minFinishTime 
     *            
     * @param maxFinishTime 
     *            
     * @param requestedFor 
     *            
     * @param reasonFilter 
     *            
     * @param statusFilter 
     *            
     * @param resultFilter 
     *            
     * @param tagFilters 
     *            A comma-delimited list of tags
     * @param properties 
     *            A comma-delimited list of properties to include in the results
     * @param type 
     *            The definition type
     * @param top 
     *            The maximum number of builds to retrieve
     * @param continuationToken 
     *            
     * @param maxBuildsPerDefinition 
     *            
     * @return List<Build>
     */
    public List<Build> getBuilds(
        final List<Integer> definitions, 
        final List<Integer> queues, 
        final String buildNumber, 
        final java.util.Date minFinishTime, 
        final java.util.Date maxFinishTime, 
        final String requestedFor, 
        final BuildReason reasonFilter, 
        final BuildStatus statusFilter, 
        final BuildResult resultFilter, 
        final List<String> tagFilters, 
        final List<String> properties, 
        final DefinitionType type, 
        final Integer top, 
        final String continuationToken, 
        final Integer maxBuildsPerDefinition) {

        final UUID locationId = UUID.fromString("0cd358e1-9217-4d94-8269-1c1ee6f93dcf"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0"); //$NON-NLS-1$

        final NameValueCollection queryParameters = new NameValueCollection();
        queryParameters.addIfNotNull("definitions", definitions); //$NON-NLS-1$
        queryParameters.addIfNotNull("queues", queues); //$NON-NLS-1$
        queryParameters.addIfNotEmpty("buildNumber", buildNumber); //$NON-NLS-1$
        queryParameters.addIfNotNull("minFinishTime", minFinishTime); //$NON-NLS-1$
        queryParameters.addIfNotNull("maxFinishTime", maxFinishTime); //$NON-NLS-1$
        queryParameters.addIfNotEmpty("requestedFor", requestedFor); //$NON-NLS-1$
        queryParameters.addIfNotNull("reasonFilter", reasonFilter); //$NON-NLS-1$
        queryParameters.addIfNotNull("statusFilter", statusFilter); //$NON-NLS-1$
        queryParameters.addIfNotNull("resultFilter", resultFilter); //$NON-NLS-1$
        queryParameters.addIfNotNull("tagFilters", tagFilters); //$NON-NLS-1$
        queryParameters.addIfNotNull("properties", properties); //$NON-NLS-1$
        queryParameters.addIfNotNull("type", type); //$NON-NLS-1$
        queryParameters.addIfNotNull("$top", top); //$NON-NLS-1$
        queryParameters.addIfNotEmpty("continuationToken", continuationToken); //$NON-NLS-1$
        queryParameters.addIfNotNull("maxBuildsPerDefinition", maxBuildsPerDefinition); //$NON-NLS-1$

        final Object httpRequest = super.createRequest(HttpMethod.GET,
                                                       locationId,
                                                       apiVersion,
                                                       queryParameters,
                                                       APPLICATION_JSON_TYPE);

        return super.sendRequest(httpRequest, new TypeReference<List<Build>>() {});
    }

    /** 
     * Queues a build
     * 
     * @param build 
     *            
     * @param ignoreWarnings 
     *            
     * @return Build
     */
    public Build queueBuild(
        final Build build, 
        final Boolean ignoreWarnings) {

        final UUID locationId = UUID.fromString("0cd358e1-9217-4d94-8269-1c1ee6f93dcf"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0"); //$NON-NLS-1$

        final NameValueCollection queryParameters = new NameValueCollection();
        queryParameters.addIfNotNull("ignoreWarnings", ignoreWarnings); //$NON-NLS-1$

        final Object httpRequest = super.createRequest(HttpMethod.POST,
                                                       locationId,
                                                       apiVersion,
                                                       build,
                                                       APPLICATION_JSON_TYPE,
                                                       queryParameters,
                                                       APPLICATION_JSON_TYPE);

        return super.sendRequest(httpRequest, Build.class);
    }

    /** 
     * Queues a build
     * 
     * @param build 
     *            
     * @param project 
     *            Project ID or project name
     * @param ignoreWarnings 
     *            
     * @return Build
     */
    public Build queueBuild(
        final Build build, 
        final String project, 
        final Boolean ignoreWarnings) {

        final UUID locationId = UUID.fromString("0cd358e1-9217-4d94-8269-1c1ee6f93dcf"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("project", project); //$NON-NLS-1$

        final NameValueCollection queryParameters = new NameValueCollection();
        queryParameters.addIfNotNull("ignoreWarnings", ignoreWarnings); //$NON-NLS-1$

        final Object httpRequest = super.createRequest(HttpMethod.POST,
                                                       locationId,
                                                       routeValues,
                                                       apiVersion,
                                                       build,
                                                       APPLICATION_JSON_TYPE,
                                                       queryParameters,
                                                       APPLICATION_JSON_TYPE);

        return super.sendRequest(httpRequest, Build.class);
    }

    /** 
     * Queues a build
     * 
     * @param build 
     *            
     * @param project 
     *            Project ID
     * @param ignoreWarnings 
     *            
     * @return Build
     */
    public Build queueBuild(
        final Build build, 
        final UUID project, 
        final Boolean ignoreWarnings) {

        final UUID locationId = UUID.fromString("0cd358e1-9217-4d94-8269-1c1ee6f93dcf"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("project", project); //$NON-NLS-1$

        final NameValueCollection queryParameters = new NameValueCollection();
        queryParameters.addIfNotNull("ignoreWarnings", ignoreWarnings); //$NON-NLS-1$

        final Object httpRequest = super.createRequest(HttpMethod.POST,
                                                       locationId,
                                                       routeValues,
                                                       apiVersion,
                                                       build,
                                                       APPLICATION_JSON_TYPE,
                                                       queryParameters,
                                                       APPLICATION_JSON_TYPE);

        return super.sendRequest(httpRequest, Build.class);
    }

    /** 
     * Updates a build
     * 
     * @param build 
     *            
     * @param buildId 
     *            
     * @return Build
     */
    public Build updateBuild(
        final Build build, 
        final int buildId) {

        final UUID locationId = UUID.fromString("0cd358e1-9217-4d94-8269-1c1ee6f93dcf"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("buildId", buildId); //$NON-NLS-1$

        final Object httpRequest = super.createRequest(HttpMethod.PATCH,
                                                       locationId,
                                                       routeValues,
                                                       apiVersion,
                                                       build,
                                                       APPLICATION_JSON_TYPE,
                                                       APPLICATION_JSON_TYPE);

        return super.sendRequest(httpRequest, Build.class);
    }

    /** 
     * Updates a build
     * 
     * @param build 
     *            
     * @param project 
     *            Project ID or project name
     * @param buildId 
     *            
     * @return Build
     */
    public Build updateBuild(
        final Build build, 
        final String project, 
        final int buildId) {

        final UUID locationId = UUID.fromString("0cd358e1-9217-4d94-8269-1c1ee6f93dcf"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("project", project); //$NON-NLS-1$
        routeValues.put("buildId", buildId); //$NON-NLS-1$

        final Object httpRequest = super.createRequest(HttpMethod.PATCH,
                                                       locationId,
                                                       routeValues,
                                                       apiVersion,
                                                       build,
                                                       APPLICATION_JSON_TYPE,
                                                       APPLICATION_JSON_TYPE);

        return super.sendRequest(httpRequest, Build.class);
    }

    /** 
     * Updates a build
     * 
     * @param build 
     *            
     * @param project 
     *            Project ID
     * @param buildId 
     *            
     * @return Build
     */
    public Build updateBuild(
        final Build build, 
        final UUID project, 
        final int buildId) {

        final UUID locationId = UUID.fromString("0cd358e1-9217-4d94-8269-1c1ee6f93dcf"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("project", project); //$NON-NLS-1$
        routeValues.put("buildId", buildId); //$NON-NLS-1$

        final Object httpRequest = super.createRequest(HttpMethod.PATCH,
                                                       locationId,
                                                       routeValues,
                                                       apiVersion,
                                                       build,
                                                       APPLICATION_JSON_TYPE,
                                                       APPLICATION_JSON_TYPE);

        return super.sendRequest(httpRequest, Build.class);
    }

    /** 
     * Gets the changes associated with a build
     * 
     * @param project 
     *            Project ID or project name
     * @param buildId 
     *            
     * @param top 
     *            The maximum number of changes to return
     * @return List<Change>
     */
    public List<Change> getBuildCommits(
        final String project, 
        final int buildId, 
        final Integer top) {

        final UUID locationId = UUID.fromString("54572c7b-bbd3-45d4-80dc-28be08941620"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("project", project); //$NON-NLS-1$
        routeValues.put("buildId", buildId); //$NON-NLS-1$

        final NameValueCollection queryParameters = new NameValueCollection();
        queryParameters.addIfNotNull("$top", top); //$NON-NLS-1$

        final Object httpRequest = super.createRequest(HttpMethod.GET,
                                                       locationId,
                                                       routeValues,
                                                       apiVersion,
                                                       queryParameters,
                                                       APPLICATION_JSON_TYPE);

        return super.sendRequest(httpRequest, new TypeReference<List<Change>>() {});
    }

    /** 
     * Gets the changes associated with a build
     * 
     * @param project 
     *            Project ID
     * @param buildId 
     *            
     * @param top 
     *            The maximum number of changes to return
     * @return List<Change>
     */
    public List<Change> getBuildCommits(
        final UUID project, 
        final int buildId, 
        final Integer top) {

        final UUID locationId = UUID.fromString("54572c7b-bbd3-45d4-80dc-28be08941620"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("project", project); //$NON-NLS-1$
        routeValues.put("buildId", buildId); //$NON-NLS-1$

        final NameValueCollection queryParameters = new NameValueCollection();
        queryParameters.addIfNotNull("$top", top); //$NON-NLS-1$

        final Object httpRequest = super.createRequest(HttpMethod.GET,
                                                       locationId,
                                                       routeValues,
                                                       apiVersion,
                                                       queryParameters,
                                                       APPLICATION_JSON_TYPE);

        return super.sendRequest(httpRequest, new TypeReference<List<Change>>() {});
    }

    /** 
     * Gets a controller
     * 
     * @param controllerId 
     *            
     * @return BuildController
     */
    public BuildController getBuildController(
        final int controllerId) {

        final UUID locationId = UUID.fromString("fcac1932-2ee1-437f-9b6f-7f696be858f6"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("controllerId", controllerId); //$NON-NLS-1$

        final Object httpRequest = super.createRequest(HttpMethod.GET,
                                                       locationId,
                                                       routeValues,
                                                       apiVersion,
                                                       APPLICATION_JSON_TYPE);

        return super.sendRequest(httpRequest, BuildController.class);
    }

    /** 
     * Gets controller, optionally filtered by name
     * 
     * @param name 
     *            
     * @return List<BuildController>
     */
    public List<BuildController> getBuildControllers(
        final String name) {

        final UUID locationId = UUID.fromString("fcac1932-2ee1-437f-9b6f-7f696be858f6"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0"); //$NON-NLS-1$

        final NameValueCollection queryParameters = new NameValueCollection();
        queryParameters.addIfNotEmpty("name", name); //$NON-NLS-1$

        final Object httpRequest = super.createRequest(HttpMethod.GET,
                                                       locationId,
                                                       apiVersion,
                                                       queryParameters,
                                                       APPLICATION_JSON_TYPE);

        return super.sendRequest(httpRequest, new TypeReference<List<BuildController>>() {});
    }

    /** 
     * Creates a new definition
     * 
     * @param definition 
     *            
     * @param definitionToCloneId 
     *            
     * @param definitionToCloneRevision 
     *            
     * @return BuildDefinition
     */
    public BuildDefinition createDefinition(
        final BuildDefinition definition, 
        final Integer definitionToCloneId, 
        final Integer definitionToCloneRevision) {

        final UUID locationId = UUID.fromString("dbeaf647-6167-421a-bda9-c9327b25e2e6"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0"); //$NON-NLS-1$

        final NameValueCollection queryParameters = new NameValueCollection();
        queryParameters.addIfNotNull("definitionToCloneId", definitionToCloneId); //$NON-NLS-1$
        queryParameters.addIfNotNull("definitionToCloneRevision", definitionToCloneRevision); //$NON-NLS-1$

        final Object httpRequest = super.createRequest(HttpMethod.POST,
                                                       locationId,
                                                       apiVersion,
                                                       definition,
                                                       APPLICATION_JSON_TYPE,
                                                       queryParameters,
                                                       APPLICATION_JSON_TYPE);

        return super.sendRequest(httpRequest, BuildDefinition.class);
    }

    /** 
     * Creates a new definition
     * 
     * @param definition 
     *            
     * @param project 
     *            Project ID or project name
     * @param definitionToCloneId 
     *            
     * @param definitionToCloneRevision 
     *            
     * @return BuildDefinition
     */
    public BuildDefinition createDefinition(
        final BuildDefinition definition, 
        final String project, 
        final Integer definitionToCloneId, 
        final Integer definitionToCloneRevision) {

        final UUID locationId = UUID.fromString("dbeaf647-6167-421a-bda9-c9327b25e2e6"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("project", project); //$NON-NLS-1$

        final NameValueCollection queryParameters = new NameValueCollection();
        queryParameters.addIfNotNull("definitionToCloneId", definitionToCloneId); //$NON-NLS-1$
        queryParameters.addIfNotNull("definitionToCloneRevision", definitionToCloneRevision); //$NON-NLS-1$

        final Object httpRequest = super.createRequest(HttpMethod.POST,
                                                       locationId,
                                                       routeValues,
                                                       apiVersion,
                                                       definition,
                                                       APPLICATION_JSON_TYPE,
                                                       queryParameters,
                                                       APPLICATION_JSON_TYPE);

        return super.sendRequest(httpRequest, BuildDefinition.class);
    }

    /** 
     * Creates a new definition
     * 
     * @param definition 
     *            
     * @param project 
     *            Project ID
     * @param definitionToCloneId 
     *            
     * @param definitionToCloneRevision 
     *            
     * @return BuildDefinition
     */
    public BuildDefinition createDefinition(
        final BuildDefinition definition, 
        final UUID project, 
        final Integer definitionToCloneId, 
        final Integer definitionToCloneRevision) {

        final UUID locationId = UUID.fromString("dbeaf647-6167-421a-bda9-c9327b25e2e6"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("project", project); //$NON-NLS-1$

        final NameValueCollection queryParameters = new NameValueCollection();
        queryParameters.addIfNotNull("definitionToCloneId", definitionToCloneId); //$NON-NLS-1$
        queryParameters.addIfNotNull("definitionToCloneRevision", definitionToCloneRevision); //$NON-NLS-1$

        final Object httpRequest = super.createRequest(HttpMethod.POST,
                                                       locationId,
                                                       routeValues,
                                                       apiVersion,
                                                       definition,
                                                       APPLICATION_JSON_TYPE,
                                                       queryParameters,
                                                       APPLICATION_JSON_TYPE);

        return super.sendRequest(httpRequest, BuildDefinition.class);
    }

    /** 
     * Deletes a definition and all associated builds
     * 
     * @param definitionId 
     *            
     */
    public void deleteDefinition(
        final int definitionId) {

        final UUID locationId = UUID.fromString("dbeaf647-6167-421a-bda9-c9327b25e2e6"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("definitionId", definitionId); //$NON-NLS-1$

        final Object httpRequest = super.createRequest(HttpMethod.DELETE,
                                                       locationId,
                                                       routeValues,
                                                       apiVersion,
                                                       APPLICATION_JSON_TYPE);

        super.sendRequest(httpRequest);
    }

    /** 
     * Deletes a definition and all associated builds
     * 
     * @param project 
     *            Project ID or project name
     * @param definitionId 
     *            
     */
    public void deleteDefinition(
        final String project, 
        final int definitionId) {

        final UUID locationId = UUID.fromString("dbeaf647-6167-421a-bda9-c9327b25e2e6"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("project", project); //$NON-NLS-1$
        routeValues.put("definitionId", definitionId); //$NON-NLS-1$

        final Object httpRequest = super.createRequest(HttpMethod.DELETE,
                                                       locationId,
                                                       routeValues,
                                                       apiVersion,
                                                       APPLICATION_JSON_TYPE);

        super.sendRequest(httpRequest);
    }

    /** 
     * Deletes a definition and all associated builds
     * 
     * @param project 
     *            Project ID
     * @param definitionId 
     *            
     */
    public void deleteDefinition(
        final UUID project, 
        final int definitionId) {

        final UUID locationId = UUID.fromString("dbeaf647-6167-421a-bda9-c9327b25e2e6"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("project", project); //$NON-NLS-1$
        routeValues.put("definitionId", definitionId); //$NON-NLS-1$

        final Object httpRequest = super.createRequest(HttpMethod.DELETE,
                                                       locationId,
                                                       routeValues,
                                                       apiVersion,
                                                       APPLICATION_JSON_TYPE);

        super.sendRequest(httpRequest);
    }

    /** 
     * Gets a definition, optionally at a specific revision
     * 
     * @param project 
     *            Project ID or project name
     * @param definitionId 
     *            
     * @param revision 
     *            
     * @param propertyFilters 
     *            
     * @return DefinitionReference
     */
    public DefinitionReference getDefinition(
        final String project, 
        final int definitionId, 
        final Integer revision, 
        final List<String> propertyFilters) {

        final UUID locationId = UUID.fromString("dbeaf647-6167-421a-bda9-c9327b25e2e6"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("project", project); //$NON-NLS-1$
        routeValues.put("definitionId", definitionId); //$NON-NLS-1$

        final NameValueCollection queryParameters = new NameValueCollection();
        queryParameters.addIfNotNull("revision", revision); //$NON-NLS-1$
        queryParameters.addIfNotNull("propertyFilters", propertyFilters); //$NON-NLS-1$

        final Object httpRequest = super.createRequest(HttpMethod.GET,
                                                       locationId,
                                                       routeValues,
                                                       apiVersion,
                                                       queryParameters,
                                                       APPLICATION_JSON_TYPE);

        return super.sendRequest(httpRequest, DefinitionReference.class);
    }

    /** 
     * Gets a definition, optionally at a specific revision
     * 
     * @param project 
     *            Project ID
     * @param definitionId 
     *            
     * @param revision 
     *            
     * @param propertyFilters 
     *            
     * @return DefinitionReference
     */
    public DefinitionReference getDefinition(
        final UUID project, 
        final int definitionId, 
        final Integer revision, 
        final List<String> propertyFilters) {

        final UUID locationId = UUID.fromString("dbeaf647-6167-421a-bda9-c9327b25e2e6"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("project", project); //$NON-NLS-1$
        routeValues.put("definitionId", definitionId); //$NON-NLS-1$

        final NameValueCollection queryParameters = new NameValueCollection();
        queryParameters.addIfNotNull("revision", revision); //$NON-NLS-1$
        queryParameters.addIfNotNull("propertyFilters", propertyFilters); //$NON-NLS-1$

        final Object httpRequest = super.createRequest(HttpMethod.GET,
                                                       locationId,
                                                       routeValues,
                                                       apiVersion,
                                                       queryParameters,
                                                       APPLICATION_JSON_TYPE);

        return super.sendRequest(httpRequest, DefinitionReference.class);
    }

    /** 
     * Gets a definition, optionally at a specific revision
     * 
     * @param definitionId 
     *            
     * @param revision 
     *            
     * @param propertyFilters 
     *            
     * @return DefinitionReference
     */
    public DefinitionReference getDefinition(
        final int definitionId, 
        final Integer revision, 
        final List<String> propertyFilters) {

        final UUID locationId = UUID.fromString("dbeaf647-6167-421a-bda9-c9327b25e2e6"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("definitionId", definitionId); //$NON-NLS-1$

        final NameValueCollection queryParameters = new NameValueCollection();
        queryParameters.addIfNotNull("revision", revision); //$NON-NLS-1$
        queryParameters.addIfNotNull("propertyFilters", propertyFilters); //$NON-NLS-1$

        final Object httpRequest = super.createRequest(HttpMethod.GET,
                                                       locationId,
                                                       routeValues,
                                                       apiVersion,
                                                       queryParameters,
                                                       APPLICATION_JSON_TYPE);

        return super.sendRequest(httpRequest, DefinitionReference.class);
    }

    /** 
     * Gets definitions, optionally filtered by name
     * 
     * @param project 
     *            Project ID or project name
     * @param name 
     *            
     * @param type 
     *            
     * @return List<DefinitionReference>
     */
    public List<DefinitionReference> getDefinitions(
        final String project, 
        final String name, 
        final DefinitionType type) {

        final UUID locationId = UUID.fromString("dbeaf647-6167-421a-bda9-c9327b25e2e6"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("project", project); //$NON-NLS-1$

        final NameValueCollection queryParameters = new NameValueCollection();
        queryParameters.addIfNotEmpty("name", name); //$NON-NLS-1$
        queryParameters.addIfNotNull("type", type); //$NON-NLS-1$

        final Object httpRequest = super.createRequest(HttpMethod.GET,
                                                       locationId,
                                                       routeValues,
                                                       apiVersion,
                                                       queryParameters,
                                                       APPLICATION_JSON_TYPE);

        return super.sendRequest(httpRequest, new TypeReference<List<DefinitionReference>>() {});
    }

    /** 
     * Gets definitions, optionally filtered by name
     * 
     * @param project 
     *            Project ID
     * @param name 
     *            
     * @param type 
     *            
     * @return List<DefinitionReference>
     */
    public List<DefinitionReference> getDefinitions(
        final UUID project, 
        final String name, 
        final DefinitionType type) {

        final UUID locationId = UUID.fromString("dbeaf647-6167-421a-bda9-c9327b25e2e6"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("project", project); //$NON-NLS-1$

        final NameValueCollection queryParameters = new NameValueCollection();
        queryParameters.addIfNotEmpty("name", name); //$NON-NLS-1$
        queryParameters.addIfNotNull("type", type); //$NON-NLS-1$

        final Object httpRequest = super.createRequest(HttpMethod.GET,
                                                       locationId,
                                                       routeValues,
                                                       apiVersion,
                                                       queryParameters,
                                                       APPLICATION_JSON_TYPE);

        return super.sendRequest(httpRequest, new TypeReference<List<DefinitionReference>>() {});
    }

    /** 
     * Gets definitions, optionally filtered by name
     * 
     * @param name 
     *            
     * @param type 
     *            
     * @return List<DefinitionReference>
     */
    public List<DefinitionReference> getDefinitions(
        final String name, 
        final DefinitionType type) {

        final UUID locationId = UUID.fromString("dbeaf647-6167-421a-bda9-c9327b25e2e6"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0"); //$NON-NLS-1$

        final NameValueCollection queryParameters = new NameValueCollection();
        queryParameters.addIfNotEmpty("name", name); //$NON-NLS-1$
        queryParameters.addIfNotNull("type", type); //$NON-NLS-1$

        final Object httpRequest = super.createRequest(HttpMethod.GET,
                                                       locationId,
                                                       apiVersion,
                                                       queryParameters,
                                                       APPLICATION_JSON_TYPE);

        return super.sendRequest(httpRequest, new TypeReference<List<DefinitionReference>>() {});
    }

    /** 
     * Updates an existing definition
     * 
     * @param definition 
     *            
     * @param definitionId 
     *            
     * @return BuildDefinition
     */
    public BuildDefinition updateDefinition(
        final BuildDefinition definition, 
        final int definitionId) {

        final UUID locationId = UUID.fromString("dbeaf647-6167-421a-bda9-c9327b25e2e6"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("definitionId", definitionId); //$NON-NLS-1$

        final Object httpRequest = super.createRequest(HttpMethod.PUT,
                                                       locationId,
                                                       routeValues,
                                                       apiVersion,
                                                       definition,
                                                       APPLICATION_JSON_TYPE,
                                                       APPLICATION_JSON_TYPE);

        return super.sendRequest(httpRequest, BuildDefinition.class);
    }

    /** 
     * Updates an existing definition
     * 
     * @param definition 
     *            
     * @param project 
     *            Project ID or project name
     * @param definitionId 
     *            
     * @return BuildDefinition
     */
    public BuildDefinition updateDefinition(
        final BuildDefinition definition, 
        final String project, 
        final int definitionId) {

        final UUID locationId = UUID.fromString("dbeaf647-6167-421a-bda9-c9327b25e2e6"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("project", project); //$NON-NLS-1$
        routeValues.put("definitionId", definitionId); //$NON-NLS-1$

        final Object httpRequest = super.createRequest(HttpMethod.PUT,
                                                       locationId,
                                                       routeValues,
                                                       apiVersion,
                                                       definition,
                                                       APPLICATION_JSON_TYPE,
                                                       APPLICATION_JSON_TYPE);

        return super.sendRequest(httpRequest, BuildDefinition.class);
    }

    /** 
     * Updates an existing definition
     * 
     * @param definition 
     *            
     * @param project 
     *            Project ID
     * @param definitionId 
     *            
     * @return BuildDefinition
     */
    public BuildDefinition updateDefinition(
        final BuildDefinition definition, 
        final UUID project, 
        final int definitionId) {

        final UUID locationId = UUID.fromString("dbeaf647-6167-421a-bda9-c9327b25e2e6"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("project", project); //$NON-NLS-1$
        routeValues.put("definitionId", definitionId); //$NON-NLS-1$

        final Object httpRequest = super.createRequest(HttpMethod.PUT,
                                                       locationId,
                                                       routeValues,
                                                       apiVersion,
                                                       definition,
                                                       APPLICATION_JSON_TYPE,
                                                       APPLICATION_JSON_TYPE);

        return super.sendRequest(httpRequest, BuildDefinition.class);
    }

    /** 
     * Gets the deployment information associated with a build
     * 
     * @param project 
     *            Project ID or project name
     * @param buildId 
     *            
     * @return List<Deployment>
     */
    public List<Deployment> getBuildDeployments(
        final String project, 
        final int buildId) {

        final UUID locationId = UUID.fromString("f275be9a-556a-4ee9-b72f-f9c8370ccaee"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("project", project); //$NON-NLS-1$
        routeValues.put("buildId", buildId); //$NON-NLS-1$

        final Object httpRequest = super.createRequest(HttpMethod.GET,
                                                       locationId,
                                                       routeValues,
                                                       apiVersion,
                                                       APPLICATION_JSON_TYPE);

        return super.sendRequest(httpRequest, new TypeReference<List<Deployment>>() {});
    }

    /** 
     * Gets the deployment information associated with a build
     * 
     * @param project 
     *            Project ID
     * @param buildId 
     *            
     * @return List<Deployment>
     */
    public List<Deployment> getBuildDeployments(
        final UUID project, 
        final int buildId) {

        final UUID locationId = UUID.fromString("f275be9a-556a-4ee9-b72f-f9c8370ccaee"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("project", project); //$NON-NLS-1$
        routeValues.put("buildId", buildId); //$NON-NLS-1$

        final Object httpRequest = super.createRequest(HttpMethod.GET,
                                                       locationId,
                                                       routeValues,
                                                       apiVersion,
                                                       APPLICATION_JSON_TYPE);

        return super.sendRequest(httpRequest, new TypeReference<List<Deployment>>() {});
    }

    /** 
     * Gets a log
     * 
     * @param project 
     *            Project ID or project name
     * @param buildId 
     *            
     * @param logId 
     *            
     * @param startLine 
     *            
     * @param endLine 
     *            
     * @return InputStream
     */
    public InputStream getBuildLog(
        final String project, 
        final int buildId, 
        final int logId, 
        final Integer startLine, 
        final Integer endLine) {

        final UUID locationId = UUID.fromString("35a80daf-7f30-45fc-86e8-6b813d9c90df"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("project", project); //$NON-NLS-1$
        routeValues.put("buildId", buildId); //$NON-NLS-1$
        routeValues.put("logId", logId); //$NON-NLS-1$

        final NameValueCollection queryParameters = new NameValueCollection();
        queryParameters.addIfNotNull("startLine", startLine); //$NON-NLS-1$
        queryParameters.addIfNotNull("endLine", endLine); //$NON-NLS-1$

        final Object httpRequest = super.createRequest(HttpMethod.GET,
                                                       locationId,
                                                       routeValues,
                                                       apiVersion,
                                                       queryParameters,
                                                       APPLICATION_OCTET_STREAM_TYPE);

        return super.sendRequest(httpRequest, InputStream.class);
    }

    /** 
     * Gets a log
     * 
     * @param project 
     *            Project ID
     * @param buildId 
     *            
     * @param logId 
     *            
     * @param startLine 
     *            
     * @param endLine 
     *            
     * @return InputStream
     */
    public InputStream getBuildLog(
        final UUID project, 
        final int buildId, 
        final int logId, 
        final Integer startLine, 
        final Integer endLine) {

        final UUID locationId = UUID.fromString("35a80daf-7f30-45fc-86e8-6b813d9c90df"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("project", project); //$NON-NLS-1$
        routeValues.put("buildId", buildId); //$NON-NLS-1$
        routeValues.put("logId", logId); //$NON-NLS-1$

        final NameValueCollection queryParameters = new NameValueCollection();
        queryParameters.addIfNotNull("startLine", startLine); //$NON-NLS-1$
        queryParameters.addIfNotNull("endLine", endLine); //$NON-NLS-1$

        final Object httpRequest = super.createRequest(HttpMethod.GET,
                                                       locationId,
                                                       routeValues,
                                                       apiVersion,
                                                       queryParameters,
                                                       APPLICATION_OCTET_STREAM_TYPE);

        return super.sendRequest(httpRequest, InputStream.class);
    }

    /** 
     * Gets logs for a build
     * 
     * @param project 
     *            Project ID or project name
     * @param buildId 
     *            
     * @return List<BuildLog>
     */
    public List<BuildLog> getBuildLogs(
        final String project, 
        final int buildId) {

        final UUID locationId = UUID.fromString("35a80daf-7f30-45fc-86e8-6b813d9c90df"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("project", project); //$NON-NLS-1$
        routeValues.put("buildId", buildId); //$NON-NLS-1$

        final Object httpRequest = super.createRequest(HttpMethod.GET,
                                                       locationId,
                                                       routeValues,
                                                       apiVersion,
                                                       APPLICATION_JSON_TYPE);

        return super.sendRequest(httpRequest, new TypeReference<List<BuildLog>>() {});
    }

    /** 
     * Gets logs for a build
     * 
     * @param project 
     *            Project ID
     * @param buildId 
     *            
     * @return List<BuildLog>
     */
    public List<BuildLog> getBuildLogs(
        final UUID project, 
        final int buildId) {

        final UUID locationId = UUID.fromString("35a80daf-7f30-45fc-86e8-6b813d9c90df"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("project", project); //$NON-NLS-1$
        routeValues.put("buildId", buildId); //$NON-NLS-1$

        final Object httpRequest = super.createRequest(HttpMethod.GET,
                                                       locationId,
                                                       routeValues,
                                                       apiVersion,
                                                       APPLICATION_JSON_TYPE);

        return super.sendRequest(httpRequest, new TypeReference<List<BuildLog>>() {});
    }

    /** 
     * Gets logs for a build
     * 
     * @param project 
     *            Project ID or project name
     * @param buildId 
     *            
     * @return InputStream
     */
    public InputStream getBuildLogsZip(
        final String project, 
        final int buildId) {

        final UUID locationId = UUID.fromString("35a80daf-7f30-45fc-86e8-6b813d9c90df"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("project", project); //$NON-NLS-1$
        routeValues.put("buildId", buildId); //$NON-NLS-1$

        final Object httpRequest = super.createRequest(HttpMethod.GET,
                                                       locationId,
                                                       routeValues,
                                                       apiVersion,
                                                       APPLICATION_ZIP_TYPE);

        return super.sendRequest(httpRequest, InputStream.class);
    }

    /** 
     * Gets logs for a build
     * 
     * @param project 
     *            Project ID
     * @param buildId 
     *            
     * @return InputStream
     */
    public InputStream getBuildLogsZip(
        final UUID project, 
        final int buildId) {

        final UUID locationId = UUID.fromString("35a80daf-7f30-45fc-86e8-6b813d9c90df"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("project", project); //$NON-NLS-1$
        routeValues.put("buildId", buildId); //$NON-NLS-1$

        final Object httpRequest = super.createRequest(HttpMethod.GET,
                                                       locationId,
                                                       routeValues,
                                                       apiVersion,
                                                       APPLICATION_ZIP_TYPE);

        return super.sendRequest(httpRequest, InputStream.class);
    }

    /** 
     * @return List<BuildOptionDefinition>
     */
    public List<BuildOptionDefinition> getBuildOptionDefinitions() {

        final UUID locationId = UUID.fromString("591cb5a4-2d46-4f3a-a697-5cd42b6bd332"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0"); //$NON-NLS-1$

        final Object httpRequest = super.createRequest(HttpMethod.GET,
                                                       locationId,
                                                       apiVersion,
                                                       APPLICATION_JSON_TYPE);

        return super.sendRequest(httpRequest, new TypeReference<List<BuildOptionDefinition>>() {});
    }

    /** 
     * Creates a build queue
     * 
     * @param queue 
     *            
     * @return AgentPoolQueue
     */
    public AgentPoolQueue createQueue(
        final AgentPoolQueue queue) {

        final UUID locationId = UUID.fromString("09f2a4b8-08c9-4991-85c3-d698937568be"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0"); //$NON-NLS-1$

        final Object httpRequest = super.createRequest(HttpMethod.POST,
                                                       locationId,
                                                       apiVersion,
                                                       queue,
                                                       APPLICATION_JSON_TYPE,
                                                       APPLICATION_JSON_TYPE);

        return super.sendRequest(httpRequest, AgentPoolQueue.class);
    }

    /** 
     * Deletes a build queue
     * 
     * @param id 
     *            
     */
    public void deleteQueue(
        final int id) {

        final UUID locationId = UUID.fromString("09f2a4b8-08c9-4991-85c3-d698937568be"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0"); //$NON-NLS-1$

        final NameValueCollection queryParameters = new NameValueCollection();
        queryParameters.put("id", String.valueOf(id)); //$NON-NLS-1$

        final Object httpRequest = super.createRequest(HttpMethod.DELETE,
                                                       locationId,
                                                       apiVersion,
                                                       queryParameters,
                                                       APPLICATION_JSON_TYPE);

        super.sendRequest(httpRequest);
    }

    /** 
     * Gets a queue
     * 
     * @param controllerId 
     *            
     * @return AgentPoolQueue
     */
    public AgentPoolQueue getAgentPoolQueue(
        final int controllerId) {

        final UUID locationId = UUID.fromString("09f2a4b8-08c9-4991-85c3-d698937568be"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("controllerId", controllerId); //$NON-NLS-1$

        final Object httpRequest = super.createRequest(HttpMethod.GET,
                                                       locationId,
                                                       routeValues,
                                                       apiVersion,
                                                       APPLICATION_JSON_TYPE);

        return super.sendRequest(httpRequest, AgentPoolQueue.class);
    }

    /** 
     * Gets queues, optionally filtered by name
     * 
     * @param name 
     *            
     * @return List<AgentPoolQueue>
     */
    public List<AgentPoolQueue> getQueues(
        final String name) {

        final UUID locationId = UUID.fromString("09f2a4b8-08c9-4991-85c3-d698937568be"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0"); //$NON-NLS-1$

        final NameValueCollection queryParameters = new NameValueCollection();
        queryParameters.addIfNotEmpty("name", name); //$NON-NLS-1$

        final Object httpRequest = super.createRequest(HttpMethod.GET,
                                                       locationId,
                                                       apiVersion,
                                                       queryParameters,
                                                       APPLICATION_JSON_TYPE);

        return super.sendRequest(httpRequest, new TypeReference<List<AgentPoolQueue>>() {});
    }

    /** 
     * Gets revisions of a definition
     * 
     * @param project 
     *            Project ID or project name
     * @param definitionId 
     *            
     * @return List<BuildDefinitionRevision>
     */
    public List<BuildDefinitionRevision> getDefinitionRevisions(
        final String project, 
        final int definitionId) {

        final UUID locationId = UUID.fromString("7c116775-52e5-453e-8c5d-914d9762d8c4"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("project", project); //$NON-NLS-1$
        routeValues.put("definitionId", definitionId); //$NON-NLS-1$

        final Object httpRequest = super.createRequest(HttpMethod.GET,
                                                       locationId,
                                                       routeValues,
                                                       apiVersion,
                                                       APPLICATION_JSON_TYPE);

        return super.sendRequest(httpRequest, new TypeReference<List<BuildDefinitionRevision>>() {});
    }

    /** 
     * Gets revisions of a definition
     * 
     * @param project 
     *            Project ID
     * @param definitionId 
     *            
     * @return List<BuildDefinitionRevision>
     */
    public List<BuildDefinitionRevision> getDefinitionRevisions(
        final UUID project, 
        final int definitionId) {

        final UUID locationId = UUID.fromString("7c116775-52e5-453e-8c5d-914d9762d8c4"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("project", project); //$NON-NLS-1$
        routeValues.put("definitionId", definitionId); //$NON-NLS-1$

        final Object httpRequest = super.createRequest(HttpMethod.GET,
                                                       locationId,
                                                       routeValues,
                                                       apiVersion,
                                                       APPLICATION_JSON_TYPE);

        return super.sendRequest(httpRequest, new TypeReference<List<BuildDefinitionRevision>>() {});
    }

    /** 
     * @return BuildSettings
     */
    public BuildSettings getBuildSettings() {

        final UUID locationId = UUID.fromString("aa8c1c9c-ef8b-474a-b8c4-785c7b191d0d"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0"); //$NON-NLS-1$

        final Object httpRequest = super.createRequest(HttpMethod.GET,
                                                       locationId,
                                                       apiVersion,
                                                       APPLICATION_JSON_TYPE);

        return super.sendRequest(httpRequest, BuildSettings.class);
    }

    /** 
     * Updates the build settings
     * 
     * @param settings 
     *            
     * @return BuildSettings
     */
    public BuildSettings updateBuildSettings(
        final BuildSettings settings) {

        final UUID locationId = UUID.fromString("aa8c1c9c-ef8b-474a-b8c4-785c7b191d0d"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0"); //$NON-NLS-1$

        final Object httpRequest = super.createRequest(HttpMethod.PATCH,
                                                       locationId,
                                                       apiVersion,
                                                       settings,
                                                       APPLICATION_JSON_TYPE,
                                                       APPLICATION_JSON_TYPE);

        return super.sendRequest(httpRequest, BuildSettings.class);
    }

    /** 
     * Adds a tag to a build
     * 
     * @param project 
     *            Project ID or project name
     * @param buildId 
     *            
     * @param tag 
     *            
     * @return List<String>
     */
    public List<String> addBuildTag(
        final String project, 
        final int buildId, 
        final String tag) {

        final UUID locationId = UUID.fromString("6e6114b2-8161-44c8-8f6c-c5505782427f"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("project", project); //$NON-NLS-1$
        routeValues.put("buildId", buildId); //$NON-NLS-1$
        routeValues.put("tag", tag); //$NON-NLS-1$

        final Object httpRequest = super.createRequest(HttpMethod.PUT,
                                                       locationId,
                                                       routeValues,
                                                       apiVersion,
                                                       APPLICATION_JSON_TYPE);

        return super.sendRequest(httpRequest, new TypeReference<List<String>>() {});
    }

    /** 
     * Adds a tag to a build
     * 
     * @param project 
     *            Project ID
     * @param buildId 
     *            
     * @param tag 
     *            
     * @return List<String>
     */
    public List<String> addBuildTag(
        final UUID project, 
        final int buildId, 
        final String tag) {

        final UUID locationId = UUID.fromString("6e6114b2-8161-44c8-8f6c-c5505782427f"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("project", project); //$NON-NLS-1$
        routeValues.put("buildId", buildId); //$NON-NLS-1$
        routeValues.put("tag", tag); //$NON-NLS-1$

        final Object httpRequest = super.createRequest(HttpMethod.PUT,
                                                       locationId,
                                                       routeValues,
                                                       apiVersion,
                                                       APPLICATION_JSON_TYPE);

        return super.sendRequest(httpRequest, new TypeReference<List<String>>() {});
    }

    /** 
     * Adds tag to a build
     * 
     * @param tags 
     *            
     * @param project 
     *            Project ID or project name
     * @param buildId 
     *            
     * @return List<String>
     */
    public List<String> addBuildTags(
        final List<String> tags, 
        final String project, 
        final int buildId) {

        final UUID locationId = UUID.fromString("6e6114b2-8161-44c8-8f6c-c5505782427f"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("project", project); //$NON-NLS-1$
        routeValues.put("buildId", buildId); //$NON-NLS-1$

        final Object httpRequest = super.createRequest(HttpMethod.POST,
                                                       locationId,
                                                       routeValues,
                                                       apiVersion,
                                                       tags,
                                                       APPLICATION_JSON_TYPE,
                                                       APPLICATION_JSON_TYPE);

        return super.sendRequest(httpRequest, new TypeReference<List<String>>() {});
    }

    /** 
     * Adds tag to a build
     * 
     * @param tags 
     *            
     * @param project 
     *            Project ID
     * @param buildId 
     *            
     * @return List<String>
     */
    public List<String> addBuildTags(
        final List<String> tags, 
        final UUID project, 
        final int buildId) {

        final UUID locationId = UUID.fromString("6e6114b2-8161-44c8-8f6c-c5505782427f"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("project", project); //$NON-NLS-1$
        routeValues.put("buildId", buildId); //$NON-NLS-1$

        final Object httpRequest = super.createRequest(HttpMethod.POST,
                                                       locationId,
                                                       routeValues,
                                                       apiVersion,
                                                       tags,
                                                       APPLICATION_JSON_TYPE,
                                                       APPLICATION_JSON_TYPE);

        return super.sendRequest(httpRequest, new TypeReference<List<String>>() {});
    }

    /** 
     * Deletes a tag from a build
     * 
     * @param project 
     *            Project ID or project name
     * @param buildId 
     *            
     * @param tag 
     *            
     * @return List<String>
     */
    public List<String> deleteBuildTag(
        final String project, 
        final int buildId, 
        final String tag) {

        final UUID locationId = UUID.fromString("6e6114b2-8161-44c8-8f6c-c5505782427f"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("project", project); //$NON-NLS-1$
        routeValues.put("buildId", buildId); //$NON-NLS-1$
        routeValues.put("tag", tag); //$NON-NLS-1$

        final Object httpRequest = super.createRequest(HttpMethod.DELETE,
                                                       locationId,
                                                       routeValues,
                                                       apiVersion,
                                                       APPLICATION_JSON_TYPE);

        return super.sendRequest(httpRequest, new TypeReference<List<String>>() {});
    }

    /** 
     * Deletes a tag from a build
     * 
     * @param project 
     *            Project ID
     * @param buildId 
     *            
     * @param tag 
     *            
     * @return List<String>
     */
    public List<String> deleteBuildTag(
        final UUID project, 
        final int buildId, 
        final String tag) {

        final UUID locationId = UUID.fromString("6e6114b2-8161-44c8-8f6c-c5505782427f"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("project", project); //$NON-NLS-1$
        routeValues.put("buildId", buildId); //$NON-NLS-1$
        routeValues.put("tag", tag); //$NON-NLS-1$

        final Object httpRequest = super.createRequest(HttpMethod.DELETE,
                                                       locationId,
                                                       routeValues,
                                                       apiVersion,
                                                       APPLICATION_JSON_TYPE);

        return super.sendRequest(httpRequest, new TypeReference<List<String>>() {});
    }

    /** 
     * Gets the tags for a build
     * 
     * @param project 
     *            Project ID or project name
     * @param buildId 
     *            
     * @return List<String>
     */
    public List<String> getBuildTags(
        final String project, 
        final int buildId) {

        final UUID locationId = UUID.fromString("6e6114b2-8161-44c8-8f6c-c5505782427f"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("project", project); //$NON-NLS-1$
        routeValues.put("buildId", buildId); //$NON-NLS-1$

        final Object httpRequest = super.createRequest(HttpMethod.GET,
                                                       locationId,
                                                       routeValues,
                                                       apiVersion,
                                                       APPLICATION_JSON_TYPE);

        return super.sendRequest(httpRequest, new TypeReference<List<String>>() {});
    }

    /** 
     * Gets the tags for a build
     * 
     * @param project 
     *            Project ID
     * @param buildId 
     *            
     * @return List<String>
     */
    public List<String> getBuildTags(
        final UUID project, 
        final int buildId) {

        final UUID locationId = UUID.fromString("6e6114b2-8161-44c8-8f6c-c5505782427f"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("project", project); //$NON-NLS-1$
        routeValues.put("buildId", buildId); //$NON-NLS-1$

        final Object httpRequest = super.createRequest(HttpMethod.GET,
                                                       locationId,
                                                       routeValues,
                                                       apiVersion,
                                                       APPLICATION_JSON_TYPE);

        return super.sendRequest(httpRequest, new TypeReference<List<String>>() {});
    }

    /** 
     * @param project 
     *            Project ID or project name
     * @return List<String>
     */
    public List<String> getTags(
        final String project) {

        final UUID locationId = UUID.fromString("d84ac5c6-edc7-43d5-adc9-1b34be5dea09"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("project", project); //$NON-NLS-1$

        final Object httpRequest = super.createRequest(HttpMethod.GET,
                                                       locationId,
                                                       routeValues,
                                                       apiVersion,
                                                       APPLICATION_JSON_TYPE);

        return super.sendRequest(httpRequest, new TypeReference<List<String>>() {});
    }

    /** 
     * @param project 
     *            Project ID
     * @return List<String>
     */
    public List<String> getTags(
        final UUID project) {

        final UUID locationId = UUID.fromString("d84ac5c6-edc7-43d5-adc9-1b34be5dea09"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("project", project); //$NON-NLS-1$

        final Object httpRequest = super.createRequest(HttpMethod.GET,
                                                       locationId,
                                                       routeValues,
                                                       apiVersion,
                                                       APPLICATION_JSON_TYPE);

        return super.sendRequest(httpRequest, new TypeReference<List<String>>() {});
    }

    /** 
     * Deletes a definition template
     * 
     * @param project 
     *            Project ID or project name
     * @param templateId 
     *            
     */
    public void deleteTemplate(
        final String project, 
        final String templateId) {

        final UUID locationId = UUID.fromString("e884571e-7f92-4d6a-9274-3f5649900835"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("project", project); //$NON-NLS-1$
        routeValues.put("templateId", templateId); //$NON-NLS-1$

        final Object httpRequest = super.createRequest(HttpMethod.DELETE,
                                                       locationId,
                                                       routeValues,
                                                       apiVersion,
                                                       APPLICATION_JSON_TYPE);

        super.sendRequest(httpRequest);
    }

    /** 
     * Deletes a definition template
     * 
     * @param project 
     *            Project ID
     * @param templateId 
     *            
     */
    public void deleteTemplate(
        final UUID project, 
        final String templateId) {

        final UUID locationId = UUID.fromString("e884571e-7f92-4d6a-9274-3f5649900835"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("project", project); //$NON-NLS-1$
        routeValues.put("templateId", templateId); //$NON-NLS-1$

        final Object httpRequest = super.createRequest(HttpMethod.DELETE,
                                                       locationId,
                                                       routeValues,
                                                       apiVersion,
                                                       APPLICATION_JSON_TYPE);

        super.sendRequest(httpRequest);
    }

    /** 
     * Gets definition template filtered by id
     * 
     * @param project 
     *            Project ID or project name
     * @param templateId 
     *            
     * @return BuildDefinitionTemplate
     */
    public BuildDefinitionTemplate getTemplate(
        final String project, 
        final String templateId) {

        final UUID locationId = UUID.fromString("e884571e-7f92-4d6a-9274-3f5649900835"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("project", project); //$NON-NLS-1$
        routeValues.put("templateId", templateId); //$NON-NLS-1$

        final Object httpRequest = super.createRequest(HttpMethod.GET,
                                                       locationId,
                                                       routeValues,
                                                       apiVersion,
                                                       APPLICATION_JSON_TYPE);

        return super.sendRequest(httpRequest, BuildDefinitionTemplate.class);
    }

    /** 
     * Gets definition template filtered by id
     * 
     * @param project 
     *            Project ID
     * @param templateId 
     *            
     * @return BuildDefinitionTemplate
     */
    public BuildDefinitionTemplate getTemplate(
        final UUID project, 
        final String templateId) {

        final UUID locationId = UUID.fromString("e884571e-7f92-4d6a-9274-3f5649900835"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("project", project); //$NON-NLS-1$
        routeValues.put("templateId", templateId); //$NON-NLS-1$

        final Object httpRequest = super.createRequest(HttpMethod.GET,
                                                       locationId,
                                                       routeValues,
                                                       apiVersion,
                                                       APPLICATION_JSON_TYPE);

        return super.sendRequest(httpRequest, BuildDefinitionTemplate.class);
    }

    /** 
     * @param project 
     *            Project ID or project name
     * @return List<BuildDefinitionTemplate>
     */
    public List<BuildDefinitionTemplate> getTemplates(
        final String project) {

        final UUID locationId = UUID.fromString("e884571e-7f92-4d6a-9274-3f5649900835"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("project", project); //$NON-NLS-1$

        final Object httpRequest = super.createRequest(HttpMethod.GET,
                                                       locationId,
                                                       routeValues,
                                                       apiVersion,
                                                       APPLICATION_JSON_TYPE);

        return super.sendRequest(httpRequest, new TypeReference<List<BuildDefinitionTemplate>>() {});
    }

    /** 
     * @param project 
     *            Project ID
     * @return List<BuildDefinitionTemplate>
     */
    public List<BuildDefinitionTemplate> getTemplates(
        final UUID project) {

        final UUID locationId = UUID.fromString("e884571e-7f92-4d6a-9274-3f5649900835"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("project", project); //$NON-NLS-1$

        final Object httpRequest = super.createRequest(HttpMethod.GET,
                                                       locationId,
                                                       routeValues,
                                                       apiVersion,
                                                       APPLICATION_JSON_TYPE);

        return super.sendRequest(httpRequest, new TypeReference<List<BuildDefinitionTemplate>>() {});
    }

    /** 
     * Saves a definition template
     * 
     * @param template 
     *            
     * @param project 
     *            Project ID or project name
     * @param templateId 
     *            
     * @return BuildDefinitionTemplate
     */
    public BuildDefinitionTemplate saveTemplate(
        final BuildDefinitionTemplate template, 
        final String project, 
        final String templateId) {

        final UUID locationId = UUID.fromString("e884571e-7f92-4d6a-9274-3f5649900835"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("project", project); //$NON-NLS-1$
        routeValues.put("templateId", templateId); //$NON-NLS-1$

        final Object httpRequest = super.createRequest(HttpMethod.PUT,
                                                       locationId,
                                                       routeValues,
                                                       apiVersion,
                                                       template,
                                                       APPLICATION_JSON_TYPE,
                                                       APPLICATION_JSON_TYPE);

        return super.sendRequest(httpRequest, BuildDefinitionTemplate.class);
    }

    /** 
     * Saves a definition template
     * 
     * @param template 
     *            
     * @param project 
     *            Project ID
     * @param templateId 
     *            
     * @return BuildDefinitionTemplate
     */
    public BuildDefinitionTemplate saveTemplate(
        final BuildDefinitionTemplate template, 
        final UUID project, 
        final String templateId) {

        final UUID locationId = UUID.fromString("e884571e-7f92-4d6a-9274-3f5649900835"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("project", project); //$NON-NLS-1$
        routeValues.put("templateId", templateId); //$NON-NLS-1$

        final Object httpRequest = super.createRequest(HttpMethod.PUT,
                                                       locationId,
                                                       routeValues,
                                                       apiVersion,
                                                       template,
                                                       APPLICATION_JSON_TYPE,
                                                       APPLICATION_JSON_TYPE);

        return super.sendRequest(httpRequest, BuildDefinitionTemplate.class);
    }

    /** 
     * Gets details for a build
     * 
     * @param project 
     *            Project ID or project name
     * @param buildId 
     *            
     * @param timelineId 
     *            
     * @param changeId 
     *            
     * @return Timeline
     */
    public Timeline getBuildTimeline(
        final String project, 
        final int buildId, 
        final UUID timelineId, 
        final Integer changeId) {

        final UUID locationId = UUID.fromString("8baac422-4c6e-4de5-8532-db96d92acffa"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("project", project); //$NON-NLS-1$
        routeValues.put("buildId", buildId); //$NON-NLS-1$
        routeValues.put("timelineId", timelineId); //$NON-NLS-1$

        final NameValueCollection queryParameters = new NameValueCollection();
        queryParameters.addIfNotNull("changeId", changeId); //$NON-NLS-1$

        final Object httpRequest = super.createRequest(HttpMethod.GET,
                                                       locationId,
                                                       routeValues,
                                                       apiVersion,
                                                       queryParameters,
                                                       APPLICATION_JSON_TYPE);

        return super.sendRequest(httpRequest, Timeline.class);
    }

    /** 
     * Gets details for a build
     * 
     * @param project 
     *            Project ID
     * @param buildId 
     *            
     * @param timelineId 
     *            
     * @param changeId 
     *            
     * @return Timeline
     */
    public Timeline getBuildTimeline(
        final UUID project, 
        final int buildId, 
        final UUID timelineId, 
        final Integer changeId) {

        final UUID locationId = UUID.fromString("8baac422-4c6e-4de5-8532-db96d92acffa"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("project", project); //$NON-NLS-1$
        routeValues.put("buildId", buildId); //$NON-NLS-1$
        routeValues.put("timelineId", timelineId); //$NON-NLS-1$

        final NameValueCollection queryParameters = new NameValueCollection();
        queryParameters.addIfNotNull("changeId", changeId); //$NON-NLS-1$

        final Object httpRequest = super.createRequest(HttpMethod.GET,
                                                       locationId,
                                                       routeValues,
                                                       apiVersion,
                                                       queryParameters,
                                                       APPLICATION_JSON_TYPE);

        return super.sendRequest(httpRequest, Timeline.class);
    }

    /** 
     * Gets the work item ids associated with a build commits
     * 
     * @param commitIds 
     *            
     * @param project 
     *            Project ID or project name
     * @param buildId 
     *            
     * @param top 
     *            The maximum number of workitems to return, also number of commits to consider if commitids are not sent
     * @return List<ResourceRef>
     */
    public List<ResourceRef> getBuildWorkItemsRefs(
        final List<String> commitIds, 
        final String project, 
        final int buildId, 
        final Integer top) {

        final UUID locationId = UUID.fromString("5a21f5d2-5642-47e4-a0bd-1356e6731bee"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("project", project); //$NON-NLS-1$
        routeValues.put("buildId", buildId); //$NON-NLS-1$

        final NameValueCollection queryParameters = new NameValueCollection();
        queryParameters.addIfNotNull("$top", top); //$NON-NLS-1$

        final Object httpRequest = super.createRequest(HttpMethod.POST,
                                                       locationId,
                                                       routeValues,
                                                       apiVersion,
                                                       commitIds,
                                                       APPLICATION_JSON_TYPE,
                                                       queryParameters,
                                                       APPLICATION_JSON_TYPE);

        return super.sendRequest(httpRequest, new TypeReference<List<ResourceRef>>() {});
    }

    /** 
     * Gets the work item ids associated with a build commits
     * 
     * @param commitIds 
     *            
     * @param project 
     *            Project ID
     * @param buildId 
     *            
     * @param top 
     *            The maximum number of workitems to return, also number of commits to consider if commitids are not sent
     * @return List<ResourceRef>
     */
    public List<ResourceRef> getBuildWorkItemsRefs(
        final List<String> commitIds, 
        final UUID project, 
        final int buildId, 
        final Integer top) {

        final UUID locationId = UUID.fromString("5a21f5d2-5642-47e4-a0bd-1356e6731bee"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("project", project); //$NON-NLS-1$
        routeValues.put("buildId", buildId); //$NON-NLS-1$

        final NameValueCollection queryParameters = new NameValueCollection();
        queryParameters.addIfNotNull("$top", top); //$NON-NLS-1$

        final Object httpRequest = super.createRequest(HttpMethod.POST,
                                                       locationId,
                                                       routeValues,
                                                       apiVersion,
                                                       commitIds,
                                                       APPLICATION_JSON_TYPE,
                                                       queryParameters,
                                                       APPLICATION_JSON_TYPE);

        return super.sendRequest(httpRequest, new TypeReference<List<ResourceRef>>() {});
    }
}
