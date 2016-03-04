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

package com.microsoft.teamfoundation.sourcecontrol.webapi;

import java.io.InputStream;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import com.fasterxml.jackson.core.type.TypeReference;
import com.microsoft.teamfoundation.sourcecontrol.webapi.model.AssociatedWorkItem;
import com.microsoft.teamfoundation.sourcecontrol.webapi.model.GitBaseVersionDescriptor;
import com.microsoft.teamfoundation.sourcecontrol.webapi.model.GitBlobRef;
import com.microsoft.teamfoundation.sourcecontrol.webapi.model.GitBranchStats;
import com.microsoft.teamfoundation.sourcecontrol.webapi.model.GitCommit;
import com.microsoft.teamfoundation.sourcecontrol.webapi.model.GitCommitChanges;
import com.microsoft.teamfoundation.sourcecontrol.webapi.model.GitCommitDiffs;
import com.microsoft.teamfoundation.sourcecontrol.webapi.model.GitCommitRef;
import com.microsoft.teamfoundation.sourcecontrol.webapi.model.GitItem;
import com.microsoft.teamfoundation.sourcecontrol.webapi.model.GitItemRequestData;
import com.microsoft.teamfoundation.sourcecontrol.webapi.model.GitPullRequest;
import com.microsoft.teamfoundation.sourcecontrol.webapi.model.GitPullRequestSearchCriteria;
import com.microsoft.teamfoundation.sourcecontrol.webapi.model.GitPush;
import com.microsoft.teamfoundation.sourcecontrol.webapi.model.GitPushSearchCriteria;
import com.microsoft.teamfoundation.sourcecontrol.webapi.model.GitQueryCommitsCriteria;
import com.microsoft.teamfoundation.sourcecontrol.webapi.model.GitRef;
import com.microsoft.teamfoundation.sourcecontrol.webapi.model.GitRefUpdate;
import com.microsoft.teamfoundation.sourcecontrol.webapi.model.GitRefUpdateResult;
import com.microsoft.teamfoundation.sourcecontrol.webapi.model.GitRepository;
import com.microsoft.teamfoundation.sourcecontrol.webapi.model.GitTargetVersionDescriptor;
import com.microsoft.teamfoundation.sourcecontrol.webapi.model.GitTreeRef;
import com.microsoft.teamfoundation.sourcecontrol.webapi.model.GitVersionDescriptor;
import com.microsoft.teamfoundation.sourcecontrol.webapi.model.IdentityRefWithVote;
import com.microsoft.teamfoundation.sourcecontrol.webapi.model.VersionControlRecursionType;
import com.microsoft.visualstudio.services.webapi.model.IdentityRef;
import com.microsoft.vss.client.core.model.ApiResourceVersion;
import com.microsoft.vss.client.core.model.NameValueCollection;
import com.microsoft.vss.client.core.VssHttpClientBase;

public abstract class GitHttpClientBase
    extends VssHttpClientBase {

    private final static Map<String, Class<? extends Exception>> TRANSLATED_EXCEPTIONS;

    static {
        TRANSLATED_EXCEPTIONS = new HashMap<String, Class<? extends Exception>>();
    }

    /**
    * Create a new instance of GitHttpClientBase
    *
    * @param jaxrsClient
    *            an initialized instance of a JAX-RS Client implementation
    * @param baseUrl
    *            a TFS project collection URL
    */
    public GitHttpClientBase(final Object jaxrsClient, final URI baseUrl) {
        super(jaxrsClient, baseUrl);
    }

    /**
    * Create a new instance of GitHttpClientBase
    *
    * @param tfsConnection
    *            an initialized instance of a TfsTeamProjectCollection
    */
    public GitHttpClientBase(final Object tfsConnection) {
        super(tfsConnection);
    }

    @Override
    protected Map<String, Class<? extends Exception>> getTranslatedExceptions() {
        return TRANSLATED_EXCEPTIONS;
    }

    /** 
     * Gets a single blob.
     * 
     * @param project 
     *            Project ID or project name
     * @param repositoryId 
     *            
     * @param sha1 
     *            
     * @param download 
     *            
     * @param fileName 
     *            
     * @return GitBlobRef
     */
    public GitBlobRef getBlob(
        final String project, 
        final String repositoryId, 
        final String sha1, 
        final Boolean download, 
        final String fileName) {

        final UUID locationId = UUID.fromString("7b28e929-2c99-405d-9c5c-6167a06e6816"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("project", project); //$NON-NLS-1$
        routeValues.put("repositoryId", repositoryId); //$NON-NLS-1$
        routeValues.put("sha1", sha1); //$NON-NLS-1$

        final NameValueCollection queryParameters = new NameValueCollection();
        queryParameters.addIfNotNull("download", download); //$NON-NLS-1$
        queryParameters.addIfNotEmpty("fileName", fileName); //$NON-NLS-1$

        final Object httpRequest = super.createRequest(HttpMethod.GET,
                                                       locationId,
                                                       routeValues,
                                                       apiVersion,
                                                       queryParameters,
                                                       APPLICATION_JSON_TYPE);

        return super.sendRequest(httpRequest, GitBlobRef.class);
    }

    /** 
     * Gets a single blob.
     * 
     * @param project 
     *            Project ID or project name
     * @param repositoryId 
     *            
     * @param sha1 
     *            
     * @param download 
     *            
     * @param fileName 
     *            
     * @return GitBlobRef
     */
    public GitBlobRef getBlob(
        final String project, 
        final UUID repositoryId, 
        final String sha1, 
        final Boolean download, 
        final String fileName) {

        final UUID locationId = UUID.fromString("7b28e929-2c99-405d-9c5c-6167a06e6816"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("project", project); //$NON-NLS-1$
        routeValues.put("repositoryId", repositoryId); //$NON-NLS-1$
        routeValues.put("sha1", sha1); //$NON-NLS-1$

        final NameValueCollection queryParameters = new NameValueCollection();
        queryParameters.addIfNotNull("download", download); //$NON-NLS-1$
        queryParameters.addIfNotEmpty("fileName", fileName); //$NON-NLS-1$

        final Object httpRequest = super.createRequest(HttpMethod.GET,
                                                       locationId,
                                                       routeValues,
                                                       apiVersion,
                                                       queryParameters,
                                                       APPLICATION_JSON_TYPE);

        return super.sendRequest(httpRequest, GitBlobRef.class);
    }

    /** 
     * Gets a single blob.
     * 
     * @param project 
     *            Project ID
     * @param repositoryId 
     *            
     * @param sha1 
     *            
     * @param download 
     *            
     * @param fileName 
     *            
     * @return GitBlobRef
     */
    public GitBlobRef getBlob(
        final UUID project, 
        final String repositoryId, 
        final String sha1, 
        final Boolean download, 
        final String fileName) {

        final UUID locationId = UUID.fromString("7b28e929-2c99-405d-9c5c-6167a06e6816"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("project", project); //$NON-NLS-1$
        routeValues.put("repositoryId", repositoryId); //$NON-NLS-1$
        routeValues.put("sha1", sha1); //$NON-NLS-1$

        final NameValueCollection queryParameters = new NameValueCollection();
        queryParameters.addIfNotNull("download", download); //$NON-NLS-1$
        queryParameters.addIfNotEmpty("fileName", fileName); //$NON-NLS-1$

        final Object httpRequest = super.createRequest(HttpMethod.GET,
                                                       locationId,
                                                       routeValues,
                                                       apiVersion,
                                                       queryParameters,
                                                       APPLICATION_JSON_TYPE);

        return super.sendRequest(httpRequest, GitBlobRef.class);
    }

    /** 
     * Gets a single blob.
     * 
     * @param project 
     *            Project ID
     * @param repositoryId 
     *            
     * @param sha1 
     *            
     * @param download 
     *            
     * @param fileName 
     *            
     * @return GitBlobRef
     */
    public GitBlobRef getBlob(
        final UUID project, 
        final UUID repositoryId, 
        final String sha1, 
        final Boolean download, 
        final String fileName) {

        final UUID locationId = UUID.fromString("7b28e929-2c99-405d-9c5c-6167a06e6816"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("project", project); //$NON-NLS-1$
        routeValues.put("repositoryId", repositoryId); //$NON-NLS-1$
        routeValues.put("sha1", sha1); //$NON-NLS-1$

        final NameValueCollection queryParameters = new NameValueCollection();
        queryParameters.addIfNotNull("download", download); //$NON-NLS-1$
        queryParameters.addIfNotEmpty("fileName", fileName); //$NON-NLS-1$

        final Object httpRequest = super.createRequest(HttpMethod.GET,
                                                       locationId,
                                                       routeValues,
                                                       apiVersion,
                                                       queryParameters,
                                                       APPLICATION_JSON_TYPE);

        return super.sendRequest(httpRequest, GitBlobRef.class);
    }

    /** 
     * Gets a single blob.
     * 
     * @param repositoryId 
     *            
     * @param sha1 
     *            
     * @param download 
     *            
     * @param fileName 
     *            
     * @return GitBlobRef
     */
    public GitBlobRef getBlob(
        final String repositoryId, 
        final String sha1, 
        final Boolean download, 
        final String fileName) {

        final UUID locationId = UUID.fromString("7b28e929-2c99-405d-9c5c-6167a06e6816"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("repositoryId", repositoryId); //$NON-NLS-1$
        routeValues.put("sha1", sha1); //$NON-NLS-1$

        final NameValueCollection queryParameters = new NameValueCollection();
        queryParameters.addIfNotNull("download", download); //$NON-NLS-1$
        queryParameters.addIfNotEmpty("fileName", fileName); //$NON-NLS-1$

        final Object httpRequest = super.createRequest(HttpMethod.GET,
                                                       locationId,
                                                       routeValues,
                                                       apiVersion,
                                                       queryParameters,
                                                       APPLICATION_JSON_TYPE);

        return super.sendRequest(httpRequest, GitBlobRef.class);
    }

    /** 
     * Gets a single blob.
     * 
     * @param repositoryId 
     *            
     * @param sha1 
     *            
     * @param download 
     *            
     * @param fileName 
     *            
     * @return GitBlobRef
     */
    public GitBlobRef getBlob(
        final UUID repositoryId, 
        final String sha1, 
        final Boolean download, 
        final String fileName) {

        final UUID locationId = UUID.fromString("7b28e929-2c99-405d-9c5c-6167a06e6816"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("repositoryId", repositoryId); //$NON-NLS-1$
        routeValues.put("sha1", sha1); //$NON-NLS-1$

        final NameValueCollection queryParameters = new NameValueCollection();
        queryParameters.addIfNotNull("download", download); //$NON-NLS-1$
        queryParameters.addIfNotEmpty("fileName", fileName); //$NON-NLS-1$

        final Object httpRequest = super.createRequest(HttpMethod.GET,
                                                       locationId,
                                                       routeValues,
                                                       apiVersion,
                                                       queryParameters,
                                                       APPLICATION_JSON_TYPE);

        return super.sendRequest(httpRequest, GitBlobRef.class);
    }

    /** 
     * Gets a single blob.
     * 
     * @param project 
     *            Project ID or project name
     * @param repositoryId 
     *            
     * @param sha1 
     *            
     * @param download 
     *            
     * @param fileName 
     *            
     * @return InputStream
     */
    public InputStream getBlobContent(
        final String project, 
        final String repositoryId, 
        final String sha1, 
        final Boolean download, 
        final String fileName) {

        final UUID locationId = UUID.fromString("7b28e929-2c99-405d-9c5c-6167a06e6816"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("project", project); //$NON-NLS-1$
        routeValues.put("repositoryId", repositoryId); //$NON-NLS-1$
        routeValues.put("sha1", sha1); //$NON-NLS-1$

        final NameValueCollection queryParameters = new NameValueCollection();
        queryParameters.addIfNotNull("download", download); //$NON-NLS-1$
        queryParameters.addIfNotEmpty("fileName", fileName); //$NON-NLS-1$

        final Object httpRequest = super.createRequest(HttpMethod.GET,
                                                       locationId,
                                                       routeValues,
                                                       apiVersion,
                                                       queryParameters,
                                                       APPLICATION_OCTET_STREAM_TYPE);

        return super.sendRequest(httpRequest, InputStream.class);
    }

    /** 
     * Gets a single blob.
     * 
     * @param project 
     *            Project ID or project name
     * @param repositoryId 
     *            
     * @param sha1 
     *            
     * @param download 
     *            
     * @param fileName 
     *            
     * @return InputStream
     */
    public InputStream getBlobContent(
        final String project, 
        final UUID repositoryId, 
        final String sha1, 
        final Boolean download, 
        final String fileName) {

        final UUID locationId = UUID.fromString("7b28e929-2c99-405d-9c5c-6167a06e6816"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("project", project); //$NON-NLS-1$
        routeValues.put("repositoryId", repositoryId); //$NON-NLS-1$
        routeValues.put("sha1", sha1); //$NON-NLS-1$

        final NameValueCollection queryParameters = new NameValueCollection();
        queryParameters.addIfNotNull("download", download); //$NON-NLS-1$
        queryParameters.addIfNotEmpty("fileName", fileName); //$NON-NLS-1$

        final Object httpRequest = super.createRequest(HttpMethod.GET,
                                                       locationId,
                                                       routeValues,
                                                       apiVersion,
                                                       queryParameters,
                                                       APPLICATION_OCTET_STREAM_TYPE);

        return super.sendRequest(httpRequest, InputStream.class);
    }

    /** 
     * Gets a single blob.
     * 
     * @param project 
     *            Project ID
     * @param repositoryId 
     *            
     * @param sha1 
     *            
     * @param download 
     *            
     * @param fileName 
     *            
     * @return InputStream
     */
    public InputStream getBlobContent(
        final UUID project, 
        final String repositoryId, 
        final String sha1, 
        final Boolean download, 
        final String fileName) {

        final UUID locationId = UUID.fromString("7b28e929-2c99-405d-9c5c-6167a06e6816"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("project", project); //$NON-NLS-1$
        routeValues.put("repositoryId", repositoryId); //$NON-NLS-1$
        routeValues.put("sha1", sha1); //$NON-NLS-1$

        final NameValueCollection queryParameters = new NameValueCollection();
        queryParameters.addIfNotNull("download", download); //$NON-NLS-1$
        queryParameters.addIfNotEmpty("fileName", fileName); //$NON-NLS-1$

        final Object httpRequest = super.createRequest(HttpMethod.GET,
                                                       locationId,
                                                       routeValues,
                                                       apiVersion,
                                                       queryParameters,
                                                       APPLICATION_OCTET_STREAM_TYPE);

        return super.sendRequest(httpRequest, InputStream.class);
    }

    /** 
     * Gets a single blob.
     * 
     * @param project 
     *            Project ID
     * @param repositoryId 
     *            
     * @param sha1 
     *            
     * @param download 
     *            
     * @param fileName 
     *            
     * @return InputStream
     */
    public InputStream getBlobContent(
        final UUID project, 
        final UUID repositoryId, 
        final String sha1, 
        final Boolean download, 
        final String fileName) {

        final UUID locationId = UUID.fromString("7b28e929-2c99-405d-9c5c-6167a06e6816"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("project", project); //$NON-NLS-1$
        routeValues.put("repositoryId", repositoryId); //$NON-NLS-1$
        routeValues.put("sha1", sha1); //$NON-NLS-1$

        final NameValueCollection queryParameters = new NameValueCollection();
        queryParameters.addIfNotNull("download", download); //$NON-NLS-1$
        queryParameters.addIfNotEmpty("fileName", fileName); //$NON-NLS-1$

        final Object httpRequest = super.createRequest(HttpMethod.GET,
                                                       locationId,
                                                       routeValues,
                                                       apiVersion,
                                                       queryParameters,
                                                       APPLICATION_OCTET_STREAM_TYPE);

        return super.sendRequest(httpRequest, InputStream.class);
    }

    /** 
     * Gets a single blob.
     * 
     * @param repositoryId 
     *            
     * @param sha1 
     *            
     * @param download 
     *            
     * @param fileName 
     *            
     * @return InputStream
     */
    public InputStream getBlobContent(
        final String repositoryId, 
        final String sha1, 
        final Boolean download, 
        final String fileName) {

        final UUID locationId = UUID.fromString("7b28e929-2c99-405d-9c5c-6167a06e6816"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("repositoryId", repositoryId); //$NON-NLS-1$
        routeValues.put("sha1", sha1); //$NON-NLS-1$

        final NameValueCollection queryParameters = new NameValueCollection();
        queryParameters.addIfNotNull("download", download); //$NON-NLS-1$
        queryParameters.addIfNotEmpty("fileName", fileName); //$NON-NLS-1$

        final Object httpRequest = super.createRequest(HttpMethod.GET,
                                                       locationId,
                                                       routeValues,
                                                       apiVersion,
                                                       queryParameters,
                                                       APPLICATION_OCTET_STREAM_TYPE);

        return super.sendRequest(httpRequest, InputStream.class);
    }

    /** 
     * Gets a single blob.
     * 
     * @param repositoryId 
     *            
     * @param sha1 
     *            
     * @param download 
     *            
     * @param fileName 
     *            
     * @return InputStream
     */
    public InputStream getBlobContent(
        final UUID repositoryId, 
        final String sha1, 
        final Boolean download, 
        final String fileName) {

        final UUID locationId = UUID.fromString("7b28e929-2c99-405d-9c5c-6167a06e6816"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("repositoryId", repositoryId); //$NON-NLS-1$
        routeValues.put("sha1", sha1); //$NON-NLS-1$

        final NameValueCollection queryParameters = new NameValueCollection();
        queryParameters.addIfNotNull("download", download); //$NON-NLS-1$
        queryParameters.addIfNotEmpty("fileName", fileName); //$NON-NLS-1$

        final Object httpRequest = super.createRequest(HttpMethod.GET,
                                                       locationId,
                                                       routeValues,
                                                       apiVersion,
                                                       queryParameters,
                                                       APPLICATION_OCTET_STREAM_TYPE);

        return super.sendRequest(httpRequest, InputStream.class);
    }

    /** 
     * Gets one or more blobs in a zip file download.
     * 
     * @param blobIds 
     *            
     * @param repositoryId 
     *            
     * @param filename 
     *            
     * @return InputStream
     */
    public InputStream getBlobsZip(
        final List<String> blobIds, 
        final String repositoryId, 
        final String filename) {

        final UUID locationId = UUID.fromString("7b28e929-2c99-405d-9c5c-6167a06e6816"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("repositoryId", repositoryId); //$NON-NLS-1$

        final NameValueCollection queryParameters = new NameValueCollection();
        queryParameters.addIfNotEmpty("filename", filename); //$NON-NLS-1$

        final Object httpRequest = super.createRequest(HttpMethod.POST,
                                                       locationId,
                                                       routeValues,
                                                       apiVersion,
                                                       blobIds,
                                                       APPLICATION_JSON_TYPE,
                                                       queryParameters,
                                                       APPLICATION_ZIP_TYPE);

        return super.sendRequest(httpRequest, InputStream.class);
    }

    /** 
     * Gets one or more blobs in a zip file download.
     * 
     * @param blobIds 
     *            
     * @param repositoryId 
     *            
     * @param filename 
     *            
     * @return InputStream
     */
    public InputStream getBlobsZip(
        final List<String> blobIds, 
        final UUID repositoryId, 
        final String filename) {

        final UUID locationId = UUID.fromString("7b28e929-2c99-405d-9c5c-6167a06e6816"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("repositoryId", repositoryId); //$NON-NLS-1$

        final NameValueCollection queryParameters = new NameValueCollection();
        queryParameters.addIfNotEmpty("filename", filename); //$NON-NLS-1$

        final Object httpRequest = super.createRequest(HttpMethod.POST,
                                                       locationId,
                                                       routeValues,
                                                       apiVersion,
                                                       blobIds,
                                                       APPLICATION_JSON_TYPE,
                                                       queryParameters,
                                                       APPLICATION_ZIP_TYPE);

        return super.sendRequest(httpRequest, InputStream.class);
    }

    /** 
     * Gets one or more blobs in a zip file download.
     * 
     * @param blobIds 
     *            
     * @param project 
     *            Project ID or project name
     * @param repositoryId 
     *            
     * @param filename 
     *            
     * @return InputStream
     */
    public InputStream getBlobsZip(
        final List<String> blobIds, 
        final String project, 
        final String repositoryId, 
        final String filename) {

        final UUID locationId = UUID.fromString("7b28e929-2c99-405d-9c5c-6167a06e6816"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("project", project); //$NON-NLS-1$
        routeValues.put("repositoryId", repositoryId); //$NON-NLS-1$

        final NameValueCollection queryParameters = new NameValueCollection();
        queryParameters.addIfNotEmpty("filename", filename); //$NON-NLS-1$

        final Object httpRequest = super.createRequest(HttpMethod.POST,
                                                       locationId,
                                                       routeValues,
                                                       apiVersion,
                                                       blobIds,
                                                       APPLICATION_JSON_TYPE,
                                                       queryParameters,
                                                       APPLICATION_ZIP_TYPE);

        return super.sendRequest(httpRequest, InputStream.class);
    }

    /** 
     * Gets one or more blobs in a zip file download.
     * 
     * @param blobIds 
     *            
     * @param project 
     *            Project ID or project name
     * @param repositoryId 
     *            
     * @param filename 
     *            
     * @return InputStream
     */
    public InputStream getBlobsZip(
        final List<String> blobIds, 
        final String project, 
        final UUID repositoryId, 
        final String filename) {

        final UUID locationId = UUID.fromString("7b28e929-2c99-405d-9c5c-6167a06e6816"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("project", project); //$NON-NLS-1$
        routeValues.put("repositoryId", repositoryId); //$NON-NLS-1$

        final NameValueCollection queryParameters = new NameValueCollection();
        queryParameters.addIfNotEmpty("filename", filename); //$NON-NLS-1$

        final Object httpRequest = super.createRequest(HttpMethod.POST,
                                                       locationId,
                                                       routeValues,
                                                       apiVersion,
                                                       blobIds,
                                                       APPLICATION_JSON_TYPE,
                                                       queryParameters,
                                                       APPLICATION_ZIP_TYPE);

        return super.sendRequest(httpRequest, InputStream.class);
    }

    /** 
     * Gets one or more blobs in a zip file download.
     * 
     * @param blobIds 
     *            
     * @param project 
     *            Project ID
     * @param repositoryId 
     *            
     * @param filename 
     *            
     * @return InputStream
     */
    public InputStream getBlobsZip(
        final List<String> blobIds, 
        final UUID project, 
        final String repositoryId, 
        final String filename) {

        final UUID locationId = UUID.fromString("7b28e929-2c99-405d-9c5c-6167a06e6816"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("project", project); //$NON-NLS-1$
        routeValues.put("repositoryId", repositoryId); //$NON-NLS-1$

        final NameValueCollection queryParameters = new NameValueCollection();
        queryParameters.addIfNotEmpty("filename", filename); //$NON-NLS-1$

        final Object httpRequest = super.createRequest(HttpMethod.POST,
                                                       locationId,
                                                       routeValues,
                                                       apiVersion,
                                                       blobIds,
                                                       APPLICATION_JSON_TYPE,
                                                       queryParameters,
                                                       APPLICATION_ZIP_TYPE);

        return super.sendRequest(httpRequest, InputStream.class);
    }

    /** 
     * Gets one or more blobs in a zip file download.
     * 
     * @param blobIds 
     *            
     * @param project 
     *            Project ID
     * @param repositoryId 
     *            
     * @param filename 
     *            
     * @return InputStream
     */
    public InputStream getBlobsZip(
        final List<String> blobIds, 
        final UUID project, 
        final UUID repositoryId, 
        final String filename) {

        final UUID locationId = UUID.fromString("7b28e929-2c99-405d-9c5c-6167a06e6816"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("project", project); //$NON-NLS-1$
        routeValues.put("repositoryId", repositoryId); //$NON-NLS-1$

        final NameValueCollection queryParameters = new NameValueCollection();
        queryParameters.addIfNotEmpty("filename", filename); //$NON-NLS-1$

        final Object httpRequest = super.createRequest(HttpMethod.POST,
                                                       locationId,
                                                       routeValues,
                                                       apiVersion,
                                                       blobIds,
                                                       APPLICATION_JSON_TYPE,
                                                       queryParameters,
                                                       APPLICATION_ZIP_TYPE);

        return super.sendRequest(httpRequest, InputStream.class);
    }

    /** 
     * Gets a single blob.
     * 
     * @param project 
     *            Project ID or project name
     * @param repositoryId 
     *            
     * @param sha1 
     *            
     * @param download 
     *            
     * @param fileName 
     *            
     * @return InputStream
     */
    public InputStream getBlobZip(
        final String project, 
        final String repositoryId, 
        final String sha1, 
        final Boolean download, 
        final String fileName) {

        final UUID locationId = UUID.fromString("7b28e929-2c99-405d-9c5c-6167a06e6816"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("project", project); //$NON-NLS-1$
        routeValues.put("repositoryId", repositoryId); //$NON-NLS-1$
        routeValues.put("sha1", sha1); //$NON-NLS-1$

        final NameValueCollection queryParameters = new NameValueCollection();
        queryParameters.addIfNotNull("download", download); //$NON-NLS-1$
        queryParameters.addIfNotEmpty("fileName", fileName); //$NON-NLS-1$

        final Object httpRequest = super.createRequest(HttpMethod.GET,
                                                       locationId,
                                                       routeValues,
                                                       apiVersion,
                                                       queryParameters,
                                                       APPLICATION_ZIP_TYPE);

        return super.sendRequest(httpRequest, InputStream.class);
    }

    /** 
     * Gets a single blob.
     * 
     * @param project 
     *            Project ID or project name
     * @param repositoryId 
     *            
     * @param sha1 
     *            
     * @param download 
     *            
     * @param fileName 
     *            
     * @return InputStream
     */
    public InputStream getBlobZip(
        final String project, 
        final UUID repositoryId, 
        final String sha1, 
        final Boolean download, 
        final String fileName) {

        final UUID locationId = UUID.fromString("7b28e929-2c99-405d-9c5c-6167a06e6816"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("project", project); //$NON-NLS-1$
        routeValues.put("repositoryId", repositoryId); //$NON-NLS-1$
        routeValues.put("sha1", sha1); //$NON-NLS-1$

        final NameValueCollection queryParameters = new NameValueCollection();
        queryParameters.addIfNotNull("download", download); //$NON-NLS-1$
        queryParameters.addIfNotEmpty("fileName", fileName); //$NON-NLS-1$

        final Object httpRequest = super.createRequest(HttpMethod.GET,
                                                       locationId,
                                                       routeValues,
                                                       apiVersion,
                                                       queryParameters,
                                                       APPLICATION_ZIP_TYPE);

        return super.sendRequest(httpRequest, InputStream.class);
    }

    /** 
     * Gets a single blob.
     * 
     * @param project 
     *            Project ID
     * @param repositoryId 
     *            
     * @param sha1 
     *            
     * @param download 
     *            
     * @param fileName 
     *            
     * @return InputStream
     */
    public InputStream getBlobZip(
        final UUID project, 
        final String repositoryId, 
        final String sha1, 
        final Boolean download, 
        final String fileName) {

        final UUID locationId = UUID.fromString("7b28e929-2c99-405d-9c5c-6167a06e6816"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("project", project); //$NON-NLS-1$
        routeValues.put("repositoryId", repositoryId); //$NON-NLS-1$
        routeValues.put("sha1", sha1); //$NON-NLS-1$

        final NameValueCollection queryParameters = new NameValueCollection();
        queryParameters.addIfNotNull("download", download); //$NON-NLS-1$
        queryParameters.addIfNotEmpty("fileName", fileName); //$NON-NLS-1$

        final Object httpRequest = super.createRequest(HttpMethod.GET,
                                                       locationId,
                                                       routeValues,
                                                       apiVersion,
                                                       queryParameters,
                                                       APPLICATION_ZIP_TYPE);

        return super.sendRequest(httpRequest, InputStream.class);
    }

    /** 
     * Gets a single blob.
     * 
     * @param project 
     *            Project ID
     * @param repositoryId 
     *            
     * @param sha1 
     *            
     * @param download 
     *            
     * @param fileName 
     *            
     * @return InputStream
     */
    public InputStream getBlobZip(
        final UUID project, 
        final UUID repositoryId, 
        final String sha1, 
        final Boolean download, 
        final String fileName) {

        final UUID locationId = UUID.fromString("7b28e929-2c99-405d-9c5c-6167a06e6816"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("project", project); //$NON-NLS-1$
        routeValues.put("repositoryId", repositoryId); //$NON-NLS-1$
        routeValues.put("sha1", sha1); //$NON-NLS-1$

        final NameValueCollection queryParameters = new NameValueCollection();
        queryParameters.addIfNotNull("download", download); //$NON-NLS-1$
        queryParameters.addIfNotEmpty("fileName", fileName); //$NON-NLS-1$

        final Object httpRequest = super.createRequest(HttpMethod.GET,
                                                       locationId,
                                                       routeValues,
                                                       apiVersion,
                                                       queryParameters,
                                                       APPLICATION_ZIP_TYPE);

        return super.sendRequest(httpRequest, InputStream.class);
    }

    /** 
     * Gets a single blob.
     * 
     * @param repositoryId 
     *            
     * @param sha1 
     *            
     * @param download 
     *            
     * @param fileName 
     *            
     * @return InputStream
     */
    public InputStream getBlobZip(
        final String repositoryId, 
        final String sha1, 
        final Boolean download, 
        final String fileName) {

        final UUID locationId = UUID.fromString("7b28e929-2c99-405d-9c5c-6167a06e6816"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("repositoryId", repositoryId); //$NON-NLS-1$
        routeValues.put("sha1", sha1); //$NON-NLS-1$

        final NameValueCollection queryParameters = new NameValueCollection();
        queryParameters.addIfNotNull("download", download); //$NON-NLS-1$
        queryParameters.addIfNotEmpty("fileName", fileName); //$NON-NLS-1$

        final Object httpRequest = super.createRequest(HttpMethod.GET,
                                                       locationId,
                                                       routeValues,
                                                       apiVersion,
                                                       queryParameters,
                                                       APPLICATION_ZIP_TYPE);

        return super.sendRequest(httpRequest, InputStream.class);
    }

    /** 
     * Gets a single blob.
     * 
     * @param repositoryId 
     *            
     * @param sha1 
     *            
     * @param download 
     *            
     * @param fileName 
     *            
     * @return InputStream
     */
    public InputStream getBlobZip(
        final UUID repositoryId, 
        final String sha1, 
        final Boolean download, 
        final String fileName) {

        final UUID locationId = UUID.fromString("7b28e929-2c99-405d-9c5c-6167a06e6816"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("repositoryId", repositoryId); //$NON-NLS-1$
        routeValues.put("sha1", sha1); //$NON-NLS-1$

        final NameValueCollection queryParameters = new NameValueCollection();
        queryParameters.addIfNotNull("download", download); //$NON-NLS-1$
        queryParameters.addIfNotEmpty("fileName", fileName); //$NON-NLS-1$

        final Object httpRequest = super.createRequest(HttpMethod.GET,
                                                       locationId,
                                                       routeValues,
                                                       apiVersion,
                                                       queryParameters,
                                                       APPLICATION_ZIP_TYPE);

        return super.sendRequest(httpRequest, InputStream.class);
    }

    /** 
     * Retrieve statistics about a single branch.
     * 
     * @param project 
     *            Project ID or project name
     * @param repositoryId 
     *            Friendly name or guid of repository
     * @param name 
     *            Name of the branch
     * @param baseVersionDescriptor 
     *            
     * @return GitBranchStats
     */
    public GitBranchStats getBranch(
        final String project, 
        final String repositoryId, 
        final String name, 
        final GitVersionDescriptor baseVersionDescriptor) {

        final UUID locationId = UUID.fromString("d5b216de-d8d5-4d32-ae76-51df755b16d3"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("project", project); //$NON-NLS-1$
        routeValues.put("repositoryId", repositoryId); //$NON-NLS-1$

        final NameValueCollection queryParameters = new NameValueCollection();
        queryParameters.addIfNotEmpty("name", name); //$NON-NLS-1$
        addModelAsQueryParams(queryParameters, baseVersionDescriptor);

        final Object httpRequest = super.createRequest(HttpMethod.GET,
                                                       locationId,
                                                       routeValues,
                                                       apiVersion,
                                                       queryParameters,
                                                       APPLICATION_JSON_TYPE);

        return super.sendRequest(httpRequest, GitBranchStats.class);
    }

    /** 
     * Retrieve statistics about a single branch.
     * 
     * @param project 
     *            Project ID or project name
     * @param repositoryId 
     *            Friendly name or guid of repository
     * @param name 
     *            Name of the branch
     * @param baseVersionDescriptor 
     *            
     * @return GitBranchStats
     */
    public GitBranchStats getBranch(
        final String project, 
        final UUID repositoryId, 
        final String name, 
        final GitVersionDescriptor baseVersionDescriptor) {

        final UUID locationId = UUID.fromString("d5b216de-d8d5-4d32-ae76-51df755b16d3"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("project", project); //$NON-NLS-1$
        routeValues.put("repositoryId", repositoryId); //$NON-NLS-1$

        final NameValueCollection queryParameters = new NameValueCollection();
        queryParameters.addIfNotEmpty("name", name); //$NON-NLS-1$
        addModelAsQueryParams(queryParameters, baseVersionDescriptor);

        final Object httpRequest = super.createRequest(HttpMethod.GET,
                                                       locationId,
                                                       routeValues,
                                                       apiVersion,
                                                       queryParameters,
                                                       APPLICATION_JSON_TYPE);

        return super.sendRequest(httpRequest, GitBranchStats.class);
    }

    /** 
     * Retrieve statistics about a single branch.
     * 
     * @param project 
     *            Project ID
     * @param repositoryId 
     *            Friendly name or guid of repository
     * @param name 
     *            Name of the branch
     * @param baseVersionDescriptor 
     *            
     * @return GitBranchStats
     */
    public GitBranchStats getBranch(
        final UUID project, 
        final String repositoryId, 
        final String name, 
        final GitVersionDescriptor baseVersionDescriptor) {

        final UUID locationId = UUID.fromString("d5b216de-d8d5-4d32-ae76-51df755b16d3"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("project", project); //$NON-NLS-1$
        routeValues.put("repositoryId", repositoryId); //$NON-NLS-1$

        final NameValueCollection queryParameters = new NameValueCollection();
        queryParameters.addIfNotEmpty("name", name); //$NON-NLS-1$
        addModelAsQueryParams(queryParameters, baseVersionDescriptor);

        final Object httpRequest = super.createRequest(HttpMethod.GET,
                                                       locationId,
                                                       routeValues,
                                                       apiVersion,
                                                       queryParameters,
                                                       APPLICATION_JSON_TYPE);

        return super.sendRequest(httpRequest, GitBranchStats.class);
    }

    /** 
     * Retrieve statistics about a single branch.
     * 
     * @param project 
     *            Project ID
     * @param repositoryId 
     *            Friendly name or guid of repository
     * @param name 
     *            Name of the branch
     * @param baseVersionDescriptor 
     *            
     * @return GitBranchStats
     */
    public GitBranchStats getBranch(
        final UUID project, 
        final UUID repositoryId, 
        final String name, 
        final GitVersionDescriptor baseVersionDescriptor) {

        final UUID locationId = UUID.fromString("d5b216de-d8d5-4d32-ae76-51df755b16d3"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("project", project); //$NON-NLS-1$
        routeValues.put("repositoryId", repositoryId); //$NON-NLS-1$

        final NameValueCollection queryParameters = new NameValueCollection();
        queryParameters.addIfNotEmpty("name", name); //$NON-NLS-1$
        addModelAsQueryParams(queryParameters, baseVersionDescriptor);

        final Object httpRequest = super.createRequest(HttpMethod.GET,
                                                       locationId,
                                                       routeValues,
                                                       apiVersion,
                                                       queryParameters,
                                                       APPLICATION_JSON_TYPE);

        return super.sendRequest(httpRequest, GitBranchStats.class);
    }

    /** 
     * Retrieve statistics about a single branch.
     * 
     * @param repositoryId 
     *            Friendly name or guid of repository
     * @param name 
     *            Name of the branch
     * @param baseVersionDescriptor 
     *            
     * @return GitBranchStats
     */
    public GitBranchStats getBranch(
        final String repositoryId, 
        final String name, 
        final GitVersionDescriptor baseVersionDescriptor) {

        final UUID locationId = UUID.fromString("d5b216de-d8d5-4d32-ae76-51df755b16d3"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("repositoryId", repositoryId); //$NON-NLS-1$

        final NameValueCollection queryParameters = new NameValueCollection();
        queryParameters.addIfNotEmpty("name", name); //$NON-NLS-1$
        addModelAsQueryParams(queryParameters, baseVersionDescriptor);

        final Object httpRequest = super.createRequest(HttpMethod.GET,
                                                       locationId,
                                                       routeValues,
                                                       apiVersion,
                                                       queryParameters,
                                                       APPLICATION_JSON_TYPE);

        return super.sendRequest(httpRequest, GitBranchStats.class);
    }

    /** 
     * Retrieve statistics about a single branch.
     * 
     * @param repositoryId 
     *            Friendly name or guid of repository
     * @param name 
     *            Name of the branch
     * @param baseVersionDescriptor 
     *            
     * @return GitBranchStats
     */
    public GitBranchStats getBranch(
        final UUID repositoryId, 
        final String name, 
        final GitVersionDescriptor baseVersionDescriptor) {

        final UUID locationId = UUID.fromString("d5b216de-d8d5-4d32-ae76-51df755b16d3"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("repositoryId", repositoryId); //$NON-NLS-1$

        final NameValueCollection queryParameters = new NameValueCollection();
        queryParameters.addIfNotEmpty("name", name); //$NON-NLS-1$
        addModelAsQueryParams(queryParameters, baseVersionDescriptor);

        final Object httpRequest = super.createRequest(HttpMethod.GET,
                                                       locationId,
                                                       routeValues,
                                                       apiVersion,
                                                       queryParameters,
                                                       APPLICATION_JSON_TYPE);

        return super.sendRequest(httpRequest, GitBranchStats.class);
    }

    /** 
     * Retrieve statistics about all branches within a repository.
     * 
     * @param project 
     *            Project ID or project name
     * @param repositoryId 
     *            Friendly name or guid of repository
     * @param baseVersionDescriptor 
     *            
     * @return List<GitBranchStats>
     */
    public List<GitBranchStats> getBranches(
        final String project, 
        final String repositoryId, 
        final GitVersionDescriptor baseVersionDescriptor) {

        final UUID locationId = UUID.fromString("d5b216de-d8d5-4d32-ae76-51df755b16d3"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("project", project); //$NON-NLS-1$
        routeValues.put("repositoryId", repositoryId); //$NON-NLS-1$

        final NameValueCollection queryParameters = new NameValueCollection();
        addModelAsQueryParams(queryParameters, baseVersionDescriptor);

        final Object httpRequest = super.createRequest(HttpMethod.GET,
                                                       locationId,
                                                       routeValues,
                                                       apiVersion,
                                                       queryParameters,
                                                       APPLICATION_JSON_TYPE);

        return super.sendRequest(httpRequest, new TypeReference<List<GitBranchStats>>() {});
    }

    /** 
     * Retrieve statistics about all branches within a repository.
     * 
     * @param project 
     *            Project ID or project name
     * @param repositoryId 
     *            Friendly name or guid of repository
     * @param baseVersionDescriptor 
     *            
     * @return List<GitBranchStats>
     */
    public List<GitBranchStats> getBranches(
        final String project, 
        final UUID repositoryId, 
        final GitVersionDescriptor baseVersionDescriptor) {

        final UUID locationId = UUID.fromString("d5b216de-d8d5-4d32-ae76-51df755b16d3"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("project", project); //$NON-NLS-1$
        routeValues.put("repositoryId", repositoryId); //$NON-NLS-1$

        final NameValueCollection queryParameters = new NameValueCollection();
        addModelAsQueryParams(queryParameters, baseVersionDescriptor);

        final Object httpRequest = super.createRequest(HttpMethod.GET,
                                                       locationId,
                                                       routeValues,
                                                       apiVersion,
                                                       queryParameters,
                                                       APPLICATION_JSON_TYPE);

        return super.sendRequest(httpRequest, new TypeReference<List<GitBranchStats>>() {});
    }

    /** 
     * Retrieve statistics about all branches within a repository.
     * 
     * @param project 
     *            Project ID
     * @param repositoryId 
     *            Friendly name or guid of repository
     * @param baseVersionDescriptor 
     *            
     * @return List<GitBranchStats>
     */
    public List<GitBranchStats> getBranches(
        final UUID project, 
        final String repositoryId, 
        final GitVersionDescriptor baseVersionDescriptor) {

        final UUID locationId = UUID.fromString("d5b216de-d8d5-4d32-ae76-51df755b16d3"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("project", project); //$NON-NLS-1$
        routeValues.put("repositoryId", repositoryId); //$NON-NLS-1$

        final NameValueCollection queryParameters = new NameValueCollection();
        addModelAsQueryParams(queryParameters, baseVersionDescriptor);

        final Object httpRequest = super.createRequest(HttpMethod.GET,
                                                       locationId,
                                                       routeValues,
                                                       apiVersion,
                                                       queryParameters,
                                                       APPLICATION_JSON_TYPE);

        return super.sendRequest(httpRequest, new TypeReference<List<GitBranchStats>>() {});
    }

    /** 
     * Retrieve statistics about all branches within a repository.
     * 
     * @param project 
     *            Project ID
     * @param repositoryId 
     *            Friendly name or guid of repository
     * @param baseVersionDescriptor 
     *            
     * @return List<GitBranchStats>
     */
    public List<GitBranchStats> getBranches(
        final UUID project, 
        final UUID repositoryId, 
        final GitVersionDescriptor baseVersionDescriptor) {

        final UUID locationId = UUID.fromString("d5b216de-d8d5-4d32-ae76-51df755b16d3"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("project", project); //$NON-NLS-1$
        routeValues.put("repositoryId", repositoryId); //$NON-NLS-1$

        final NameValueCollection queryParameters = new NameValueCollection();
        addModelAsQueryParams(queryParameters, baseVersionDescriptor);

        final Object httpRequest = super.createRequest(HttpMethod.GET,
                                                       locationId,
                                                       routeValues,
                                                       apiVersion,
                                                       queryParameters,
                                                       APPLICATION_JSON_TYPE);

        return super.sendRequest(httpRequest, new TypeReference<List<GitBranchStats>>() {});
    }

    /** 
     * Retrieve statistics about all branches within a repository.
     * 
     * @param repositoryId 
     *            Friendly name or guid of repository
     * @param baseVersionDescriptor 
     *            
     * @return List<GitBranchStats>
     */
    public List<GitBranchStats> getBranches(
        final String repositoryId, 
        final GitVersionDescriptor baseVersionDescriptor) {

        final UUID locationId = UUID.fromString("d5b216de-d8d5-4d32-ae76-51df755b16d3"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("repositoryId", repositoryId); //$NON-NLS-1$

        final NameValueCollection queryParameters = new NameValueCollection();
        addModelAsQueryParams(queryParameters, baseVersionDescriptor);

        final Object httpRequest = super.createRequest(HttpMethod.GET,
                                                       locationId,
                                                       routeValues,
                                                       apiVersion,
                                                       queryParameters,
                                                       APPLICATION_JSON_TYPE);

        return super.sendRequest(httpRequest, new TypeReference<List<GitBranchStats>>() {});
    }

    /** 
     * Retrieve statistics about all branches within a repository.
     * 
     * @param repositoryId 
     *            Friendly name or guid of repository
     * @param baseVersionDescriptor 
     *            
     * @return List<GitBranchStats>
     */
    public List<GitBranchStats> getBranches(
        final UUID repositoryId, 
        final GitVersionDescriptor baseVersionDescriptor) {

        final UUID locationId = UUID.fromString("d5b216de-d8d5-4d32-ae76-51df755b16d3"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("repositoryId", repositoryId); //$NON-NLS-1$

        final NameValueCollection queryParameters = new NameValueCollection();
        addModelAsQueryParams(queryParameters, baseVersionDescriptor);

        final Object httpRequest = super.createRequest(HttpMethod.GET,
                                                       locationId,
                                                       routeValues,
                                                       apiVersion,
                                                       queryParameters,
                                                       APPLICATION_JSON_TYPE);

        return super.sendRequest(httpRequest, new TypeReference<List<GitBranchStats>>() {});
    }

    /** 
     * Retrieve changes for a particular commit.
     * 
     * @param project 
     *            Project ID or project name
     * @param commitId 
     *            The id of the commit.
     * @param repositoryId 
     *            The id or friendly name of the repository. To use the friendly name, projectId must also be specified.
     * @param top 
     *            The maximum number of changes to return.
     * @param skip 
     *            The number of changes to skip.
     * @return GitCommitChanges
     */
    public GitCommitChanges getChanges(
        final String project, 
        final String commitId, 
        final String repositoryId, 
        final Integer top, 
        final Integer skip) {

        final UUID locationId = UUID.fromString("5bf884f5-3e07-42e9-afb8-1b872267bf16"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("project", project); //$NON-NLS-1$
        routeValues.put("commitId", commitId); //$NON-NLS-1$
        routeValues.put("repositoryId", repositoryId); //$NON-NLS-1$

        final NameValueCollection queryParameters = new NameValueCollection();
        queryParameters.addIfNotNull("top", top); //$NON-NLS-1$
        queryParameters.addIfNotNull("skip", skip); //$NON-NLS-1$

        final Object httpRequest = super.createRequest(HttpMethod.GET,
                                                       locationId,
                                                       routeValues,
                                                       apiVersion,
                                                       queryParameters,
                                                       APPLICATION_JSON_TYPE);

        return super.sendRequest(httpRequest, GitCommitChanges.class);
    }

    /** 
     * Retrieve changes for a particular commit.
     * 
     * @param project 
     *            Project ID or project name
     * @param commitId 
     *            The id of the commit.
     * @param repositoryId 
     *            The id or friendly name of the repository. To use the friendly name, projectId must also be specified.
     * @param top 
     *            The maximum number of changes to return.
     * @param skip 
     *            The number of changes to skip.
     * @return GitCommitChanges
     */
    public GitCommitChanges getChanges(
        final String project, 
        final String commitId, 
        final UUID repositoryId, 
        final Integer top, 
        final Integer skip) {

        final UUID locationId = UUID.fromString("5bf884f5-3e07-42e9-afb8-1b872267bf16"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("project", project); //$NON-NLS-1$
        routeValues.put("commitId", commitId); //$NON-NLS-1$
        routeValues.put("repositoryId", repositoryId); //$NON-NLS-1$

        final NameValueCollection queryParameters = new NameValueCollection();
        queryParameters.addIfNotNull("top", top); //$NON-NLS-1$
        queryParameters.addIfNotNull("skip", skip); //$NON-NLS-1$

        final Object httpRequest = super.createRequest(HttpMethod.GET,
                                                       locationId,
                                                       routeValues,
                                                       apiVersion,
                                                       queryParameters,
                                                       APPLICATION_JSON_TYPE);

        return super.sendRequest(httpRequest, GitCommitChanges.class);
    }

    /** 
     * Retrieve changes for a particular commit.
     * 
     * @param project 
     *            Project ID
     * @param commitId 
     *            The id of the commit.
     * @param repositoryId 
     *            The id or friendly name of the repository. To use the friendly name, projectId must also be specified.
     * @param top 
     *            The maximum number of changes to return.
     * @param skip 
     *            The number of changes to skip.
     * @return GitCommitChanges
     */
    public GitCommitChanges getChanges(
        final UUID project, 
        final String commitId, 
        final String repositoryId, 
        final Integer top, 
        final Integer skip) {

        final UUID locationId = UUID.fromString("5bf884f5-3e07-42e9-afb8-1b872267bf16"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("project", project); //$NON-NLS-1$
        routeValues.put("commitId", commitId); //$NON-NLS-1$
        routeValues.put("repositoryId", repositoryId); //$NON-NLS-1$

        final NameValueCollection queryParameters = new NameValueCollection();
        queryParameters.addIfNotNull("top", top); //$NON-NLS-1$
        queryParameters.addIfNotNull("skip", skip); //$NON-NLS-1$

        final Object httpRequest = super.createRequest(HttpMethod.GET,
                                                       locationId,
                                                       routeValues,
                                                       apiVersion,
                                                       queryParameters,
                                                       APPLICATION_JSON_TYPE);

        return super.sendRequest(httpRequest, GitCommitChanges.class);
    }

    /** 
     * Retrieve changes for a particular commit.
     * 
     * @param project 
     *            Project ID
     * @param commitId 
     *            The id of the commit.
     * @param repositoryId 
     *            The id or friendly name of the repository. To use the friendly name, projectId must also be specified.
     * @param top 
     *            The maximum number of changes to return.
     * @param skip 
     *            The number of changes to skip.
     * @return GitCommitChanges
     */
    public GitCommitChanges getChanges(
        final UUID project, 
        final String commitId, 
        final UUID repositoryId, 
        final Integer top, 
        final Integer skip) {

        final UUID locationId = UUID.fromString("5bf884f5-3e07-42e9-afb8-1b872267bf16"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("project", project); //$NON-NLS-1$
        routeValues.put("commitId", commitId); //$NON-NLS-1$
        routeValues.put("repositoryId", repositoryId); //$NON-NLS-1$

        final NameValueCollection queryParameters = new NameValueCollection();
        queryParameters.addIfNotNull("top", top); //$NON-NLS-1$
        queryParameters.addIfNotNull("skip", skip); //$NON-NLS-1$

        final Object httpRequest = super.createRequest(HttpMethod.GET,
                                                       locationId,
                                                       routeValues,
                                                       apiVersion,
                                                       queryParameters,
                                                       APPLICATION_JSON_TYPE);

        return super.sendRequest(httpRequest, GitCommitChanges.class);
    }

    /** 
     * Retrieve changes for a particular commit.
     * 
     * @param commitId 
     *            The id of the commit.
     * @param repositoryId 
     *            The id or friendly name of the repository. To use the friendly name, projectId must also be specified.
     * @param top 
     *            The maximum number of changes to return.
     * @param skip 
     *            The number of changes to skip.
     * @return GitCommitChanges
     */
    public GitCommitChanges getChanges(
        final String commitId, 
        final String repositoryId, 
        final Integer top, 
        final Integer skip) {

        final UUID locationId = UUID.fromString("5bf884f5-3e07-42e9-afb8-1b872267bf16"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("commitId", commitId); //$NON-NLS-1$
        routeValues.put("repositoryId", repositoryId); //$NON-NLS-1$

        final NameValueCollection queryParameters = new NameValueCollection();
        queryParameters.addIfNotNull("top", top); //$NON-NLS-1$
        queryParameters.addIfNotNull("skip", skip); //$NON-NLS-1$

        final Object httpRequest = super.createRequest(HttpMethod.GET,
                                                       locationId,
                                                       routeValues,
                                                       apiVersion,
                                                       queryParameters,
                                                       APPLICATION_JSON_TYPE);

        return super.sendRequest(httpRequest, GitCommitChanges.class);
    }

    /** 
     * Retrieve changes for a particular commit.
     * 
     * @param commitId 
     *            The id of the commit.
     * @param repositoryId 
     *            The id or friendly name of the repository. To use the friendly name, projectId must also be specified.
     * @param top 
     *            The maximum number of changes to return.
     * @param skip 
     *            The number of changes to skip.
     * @return GitCommitChanges
     */
    public GitCommitChanges getChanges(
        final String commitId, 
        final UUID repositoryId, 
        final Integer top, 
        final Integer skip) {

        final UUID locationId = UUID.fromString("5bf884f5-3e07-42e9-afb8-1b872267bf16"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("commitId", commitId); //$NON-NLS-1$
        routeValues.put("repositoryId", repositoryId); //$NON-NLS-1$

        final NameValueCollection queryParameters = new NameValueCollection();
        queryParameters.addIfNotNull("top", top); //$NON-NLS-1$
        queryParameters.addIfNotNull("skip", skip); //$NON-NLS-1$

        final Object httpRequest = super.createRequest(HttpMethod.GET,
                                                       locationId,
                                                       routeValues,
                                                       apiVersion,
                                                       queryParameters,
                                                       APPLICATION_JSON_TYPE);

        return super.sendRequest(httpRequest, GitCommitChanges.class);
    }

    /** 
     * Get differences in committed items between two commits.
     * 
     * @param project 
     *            Project ID or project name
     * @param repositoryId 
     *            Friendly name or guid of repository
     * @param diffCommonCommit 
     *            
     * @param top 
     *            Maximum number of changes to return
     * @param skip 
     *            Number of changes to skip
     * @param baseVersionDescriptor 
     *            
     * @param targetVersionDescriptor 
     *            
     * @return GitCommitDiffs
     */
    public GitCommitDiffs getCommitDiffs(
        final String project, 
        final String repositoryId, 
        final Boolean diffCommonCommit, 
        final Integer top, 
        final Integer skip, 
        final GitBaseVersionDescriptor baseVersionDescriptor, 
        final GitTargetVersionDescriptor targetVersionDescriptor) {

        final UUID locationId = UUID.fromString("615588d5-c0c7-4b88-88f8-e625306446e8"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("project", project); //$NON-NLS-1$
        routeValues.put("repositoryId", repositoryId); //$NON-NLS-1$

        final NameValueCollection queryParameters = new NameValueCollection();
        queryParameters.addIfNotNull("diffCommonCommit", diffCommonCommit); //$NON-NLS-1$
        queryParameters.addIfNotNull("$top", top); //$NON-NLS-1$
        queryParameters.addIfNotNull("$skip", skip); //$NON-NLS-1$
        addModelAsQueryParams(queryParameters, baseVersionDescriptor);
        addModelAsQueryParams(queryParameters, targetVersionDescriptor);

        final Object httpRequest = super.createRequest(HttpMethod.GET,
                                                       locationId,
                                                       routeValues,
                                                       apiVersion,
                                                       queryParameters,
                                                       APPLICATION_JSON_TYPE);

        return super.sendRequest(httpRequest, GitCommitDiffs.class);
    }

    /** 
     * Get differences in committed items between two commits.
     * 
     * @param project 
     *            Project ID or project name
     * @param repositoryId 
     *            Friendly name or guid of repository
     * @param diffCommonCommit 
     *            
     * @param top 
     *            Maximum number of changes to return
     * @param skip 
     *            Number of changes to skip
     * @param baseVersionDescriptor 
     *            
     * @param targetVersionDescriptor 
     *            
     * @return GitCommitDiffs
     */
    public GitCommitDiffs getCommitDiffs(
        final String project, 
        final UUID repositoryId, 
        final Boolean diffCommonCommit, 
        final Integer top, 
        final Integer skip, 
        final GitBaseVersionDescriptor baseVersionDescriptor, 
        final GitTargetVersionDescriptor targetVersionDescriptor) {

        final UUID locationId = UUID.fromString("615588d5-c0c7-4b88-88f8-e625306446e8"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("project", project); //$NON-NLS-1$
        routeValues.put("repositoryId", repositoryId); //$NON-NLS-1$

        final NameValueCollection queryParameters = new NameValueCollection();
        queryParameters.addIfNotNull("diffCommonCommit", diffCommonCommit); //$NON-NLS-1$
        queryParameters.addIfNotNull("$top", top); //$NON-NLS-1$
        queryParameters.addIfNotNull("$skip", skip); //$NON-NLS-1$
        addModelAsQueryParams(queryParameters, baseVersionDescriptor);
        addModelAsQueryParams(queryParameters, targetVersionDescriptor);

        final Object httpRequest = super.createRequest(HttpMethod.GET,
                                                       locationId,
                                                       routeValues,
                                                       apiVersion,
                                                       queryParameters,
                                                       APPLICATION_JSON_TYPE);

        return super.sendRequest(httpRequest, GitCommitDiffs.class);
    }

    /** 
     * Get differences in committed items between two commits.
     * 
     * @param project 
     *            Project ID
     * @param repositoryId 
     *            Friendly name or guid of repository
     * @param diffCommonCommit 
     *            
     * @param top 
     *            Maximum number of changes to return
     * @param skip 
     *            Number of changes to skip
     * @param baseVersionDescriptor 
     *            
     * @param targetVersionDescriptor 
     *            
     * @return GitCommitDiffs
     */
    public GitCommitDiffs getCommitDiffs(
        final UUID project, 
        final String repositoryId, 
        final Boolean diffCommonCommit, 
        final Integer top, 
        final Integer skip, 
        final GitBaseVersionDescriptor baseVersionDescriptor, 
        final GitTargetVersionDescriptor targetVersionDescriptor) {

        final UUID locationId = UUID.fromString("615588d5-c0c7-4b88-88f8-e625306446e8"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("project", project); //$NON-NLS-1$
        routeValues.put("repositoryId", repositoryId); //$NON-NLS-1$

        final NameValueCollection queryParameters = new NameValueCollection();
        queryParameters.addIfNotNull("diffCommonCommit", diffCommonCommit); //$NON-NLS-1$
        queryParameters.addIfNotNull("$top", top); //$NON-NLS-1$
        queryParameters.addIfNotNull("$skip", skip); //$NON-NLS-1$
        addModelAsQueryParams(queryParameters, baseVersionDescriptor);
        addModelAsQueryParams(queryParameters, targetVersionDescriptor);

        final Object httpRequest = super.createRequest(HttpMethod.GET,
                                                       locationId,
                                                       routeValues,
                                                       apiVersion,
                                                       queryParameters,
                                                       APPLICATION_JSON_TYPE);

        return super.sendRequest(httpRequest, GitCommitDiffs.class);
    }

    /** 
     * Get differences in committed items between two commits.
     * 
     * @param project 
     *            Project ID
     * @param repositoryId 
     *            Friendly name or guid of repository
     * @param diffCommonCommit 
     *            
     * @param top 
     *            Maximum number of changes to return
     * @param skip 
     *            Number of changes to skip
     * @param baseVersionDescriptor 
     *            
     * @param targetVersionDescriptor 
     *            
     * @return GitCommitDiffs
     */
    public GitCommitDiffs getCommitDiffs(
        final UUID project, 
        final UUID repositoryId, 
        final Boolean diffCommonCommit, 
        final Integer top, 
        final Integer skip, 
        final GitBaseVersionDescriptor baseVersionDescriptor, 
        final GitTargetVersionDescriptor targetVersionDescriptor) {

        final UUID locationId = UUID.fromString("615588d5-c0c7-4b88-88f8-e625306446e8"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("project", project); //$NON-NLS-1$
        routeValues.put("repositoryId", repositoryId); //$NON-NLS-1$

        final NameValueCollection queryParameters = new NameValueCollection();
        queryParameters.addIfNotNull("diffCommonCommit", diffCommonCommit); //$NON-NLS-1$
        queryParameters.addIfNotNull("$top", top); //$NON-NLS-1$
        queryParameters.addIfNotNull("$skip", skip); //$NON-NLS-1$
        addModelAsQueryParams(queryParameters, baseVersionDescriptor);
        addModelAsQueryParams(queryParameters, targetVersionDescriptor);

        final Object httpRequest = super.createRequest(HttpMethod.GET,
                                                       locationId,
                                                       routeValues,
                                                       apiVersion,
                                                       queryParameters,
                                                       APPLICATION_JSON_TYPE);

        return super.sendRequest(httpRequest, GitCommitDiffs.class);
    }

    /** 
     * Get differences in committed items between two commits.
     * 
     * @param repositoryId 
     *            Friendly name or guid of repository
     * @param diffCommonCommit 
     *            
     * @param top 
     *            Maximum number of changes to return
     * @param skip 
     *            Number of changes to skip
     * @param baseVersionDescriptor 
     *            
     * @param targetVersionDescriptor 
     *            
     * @return GitCommitDiffs
     */
    public GitCommitDiffs getCommitDiffs(
        final String repositoryId, 
        final Boolean diffCommonCommit, 
        final Integer top, 
        final Integer skip, 
        final GitBaseVersionDescriptor baseVersionDescriptor, 
        final GitTargetVersionDescriptor targetVersionDescriptor) {

        final UUID locationId = UUID.fromString("615588d5-c0c7-4b88-88f8-e625306446e8"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("repositoryId", repositoryId); //$NON-NLS-1$

        final NameValueCollection queryParameters = new NameValueCollection();
        queryParameters.addIfNotNull("diffCommonCommit", diffCommonCommit); //$NON-NLS-1$
        queryParameters.addIfNotNull("$top", top); //$NON-NLS-1$
        queryParameters.addIfNotNull("$skip", skip); //$NON-NLS-1$
        addModelAsQueryParams(queryParameters, baseVersionDescriptor);
        addModelAsQueryParams(queryParameters, targetVersionDescriptor);

        final Object httpRequest = super.createRequest(HttpMethod.GET,
                                                       locationId,
                                                       routeValues,
                                                       apiVersion,
                                                       queryParameters,
                                                       APPLICATION_JSON_TYPE);

        return super.sendRequest(httpRequest, GitCommitDiffs.class);
    }

    /** 
     * Get differences in committed items between two commits.
     * 
     * @param repositoryId 
     *            Friendly name or guid of repository
     * @param diffCommonCommit 
     *            
     * @param top 
     *            Maximum number of changes to return
     * @param skip 
     *            Number of changes to skip
     * @param baseVersionDescriptor 
     *            
     * @param targetVersionDescriptor 
     *            
     * @return GitCommitDiffs
     */
    public GitCommitDiffs getCommitDiffs(
        final UUID repositoryId, 
        final Boolean diffCommonCommit, 
        final Integer top, 
        final Integer skip, 
        final GitBaseVersionDescriptor baseVersionDescriptor, 
        final GitTargetVersionDescriptor targetVersionDescriptor) {

        final UUID locationId = UUID.fromString("615588d5-c0c7-4b88-88f8-e625306446e8"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("repositoryId", repositoryId); //$NON-NLS-1$

        final NameValueCollection queryParameters = new NameValueCollection();
        queryParameters.addIfNotNull("diffCommonCommit", diffCommonCommit); //$NON-NLS-1$
        queryParameters.addIfNotNull("$top", top); //$NON-NLS-1$
        queryParameters.addIfNotNull("$skip", skip); //$NON-NLS-1$
        addModelAsQueryParams(queryParameters, baseVersionDescriptor);
        addModelAsQueryParams(queryParameters, targetVersionDescriptor);

        final Object httpRequest = super.createRequest(HttpMethod.GET,
                                                       locationId,
                                                       routeValues,
                                                       apiVersion,
                                                       queryParameters,
                                                       APPLICATION_JSON_TYPE);

        return super.sendRequest(httpRequest, GitCommitDiffs.class);
    }

    /** 
     * Create a git commit for a project
     * 
     * @param repositoryId 
     *            The id or friendly name of the repository. To use the friendly name, projectId must also be specified.
     * @return GitCommit
     */
    public GitCommit createCommit(
        final String repositoryId) {

        final UUID locationId = UUID.fromString("c2570c3b-5b3f-41b8-98bf-5407bfde8d58"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("repositoryId", repositoryId); //$NON-NLS-1$

        final Object httpRequest = super.createRequest(HttpMethod.POST,
                                                       locationId,
                                                       routeValues,
                                                       apiVersion,
                                                       APPLICATION_JSON_TYPE);

        return super.sendRequest(httpRequest, GitCommit.class);
    }

    /** 
     * Create a git commit for a project
     * 
     * @param repositoryId 
     *            The id or friendly name of the repository. To use the friendly name, projectId must also be specified.
     * @return GitCommit
     */
    public GitCommit createCommit(
        final UUID repositoryId) {

        final UUID locationId = UUID.fromString("c2570c3b-5b3f-41b8-98bf-5407bfde8d58"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("repositoryId", repositoryId); //$NON-NLS-1$

        final Object httpRequest = super.createRequest(HttpMethod.POST,
                                                       locationId,
                                                       routeValues,
                                                       apiVersion,
                                                       APPLICATION_JSON_TYPE);

        return super.sendRequest(httpRequest, GitCommit.class);
    }

    /** 
     * Create a git commit for a project
     * 
     * @param project 
     *            Project ID or project name
     * @param repositoryId 
     *            The id or friendly name of the repository. To use the friendly name, projectId must also be specified.
     * @return GitCommit
     */
    public GitCommit createCommit(
        final String project, 
        final String repositoryId) {

        final UUID locationId = UUID.fromString("c2570c3b-5b3f-41b8-98bf-5407bfde8d58"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("project", project); //$NON-NLS-1$
        routeValues.put("repositoryId", repositoryId); //$NON-NLS-1$

        final Object httpRequest = super.createRequest(HttpMethod.POST,
                                                       locationId,
                                                       routeValues,
                                                       apiVersion,
                                                       APPLICATION_JSON_TYPE);

        return super.sendRequest(httpRequest, GitCommit.class);
    }

    /** 
     * Create a git commit for a project
     * 
     * @param project 
     *            Project ID or project name
     * @param repositoryId 
     *            The id or friendly name of the repository. To use the friendly name, projectId must also be specified.
     * @return GitCommit
     */
    public GitCommit createCommit(
        final String project, 
        final UUID repositoryId) {

        final UUID locationId = UUID.fromString("c2570c3b-5b3f-41b8-98bf-5407bfde8d58"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("project", project); //$NON-NLS-1$
        routeValues.put("repositoryId", repositoryId); //$NON-NLS-1$

        final Object httpRequest = super.createRequest(HttpMethod.POST,
                                                       locationId,
                                                       routeValues,
                                                       apiVersion,
                                                       APPLICATION_JSON_TYPE);

        return super.sendRequest(httpRequest, GitCommit.class);
    }

    /** 
     * Create a git commit for a project
     * 
     * @param project 
     *            Project ID
     * @param repositoryId 
     *            The id or friendly name of the repository. To use the friendly name, projectId must also be specified.
     * @return GitCommit
     */
    public GitCommit createCommit(
        final UUID project, 
        final String repositoryId) {

        final UUID locationId = UUID.fromString("c2570c3b-5b3f-41b8-98bf-5407bfde8d58"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("project", project); //$NON-NLS-1$
        routeValues.put("repositoryId", repositoryId); //$NON-NLS-1$

        final Object httpRequest = super.createRequest(HttpMethod.POST,
                                                       locationId,
                                                       routeValues,
                                                       apiVersion,
                                                       APPLICATION_JSON_TYPE);

        return super.sendRequest(httpRequest, GitCommit.class);
    }

    /** 
     * Create a git commit for a project
     * 
     * @param project 
     *            Project ID
     * @param repositoryId 
     *            The id or friendly name of the repository. To use the friendly name, projectId must also be specified.
     * @return GitCommit
     */
    public GitCommit createCommit(
        final UUID project, 
        final UUID repositoryId) {

        final UUID locationId = UUID.fromString("c2570c3b-5b3f-41b8-98bf-5407bfde8d58"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("project", project); //$NON-NLS-1$
        routeValues.put("repositoryId", repositoryId); //$NON-NLS-1$

        final Object httpRequest = super.createRequest(HttpMethod.POST,
                                                       locationId,
                                                       routeValues,
                                                       apiVersion,
                                                       APPLICATION_JSON_TYPE);

        return super.sendRequest(httpRequest, GitCommit.class);
    }

    /** 
     * Retrieve a particular commit.
     * 
     * @param project 
     *            Project ID or project name
     * @param commitId 
     *            The id of the commit.
     * @param repositoryId 
     *            The id or friendly name of the repository. To use the friendly name, projectId must also be specified.
     * @param changeCount 
     *            The number of changes to include in the result.
     * @return GitCommit
     */
    public GitCommit getCommit(
        final String project, 
        final String commitId, 
        final String repositoryId, 
        final Integer changeCount) {

        final UUID locationId = UUID.fromString("c2570c3b-5b3f-41b8-98bf-5407bfde8d58"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("project", project); //$NON-NLS-1$
        routeValues.put("commitId", commitId); //$NON-NLS-1$
        routeValues.put("repositoryId", repositoryId); //$NON-NLS-1$

        final NameValueCollection queryParameters = new NameValueCollection();
        queryParameters.addIfNotNull("changeCount", changeCount); //$NON-NLS-1$

        final Object httpRequest = super.createRequest(HttpMethod.GET,
                                                       locationId,
                                                       routeValues,
                                                       apiVersion,
                                                       queryParameters,
                                                       APPLICATION_JSON_TYPE);

        return super.sendRequest(httpRequest, GitCommit.class);
    }

    /** 
     * Retrieve a particular commit.
     * 
     * @param project 
     *            Project ID or project name
     * @param commitId 
     *            The id of the commit.
     * @param repositoryId 
     *            The id or friendly name of the repository. To use the friendly name, projectId must also be specified.
     * @param changeCount 
     *            The number of changes to include in the result.
     * @return GitCommit
     */
    public GitCommit getCommit(
        final String project, 
        final String commitId, 
        final UUID repositoryId, 
        final Integer changeCount) {

        final UUID locationId = UUID.fromString("c2570c3b-5b3f-41b8-98bf-5407bfde8d58"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("project", project); //$NON-NLS-1$
        routeValues.put("commitId", commitId); //$NON-NLS-1$
        routeValues.put("repositoryId", repositoryId); //$NON-NLS-1$

        final NameValueCollection queryParameters = new NameValueCollection();
        queryParameters.addIfNotNull("changeCount", changeCount); //$NON-NLS-1$

        final Object httpRequest = super.createRequest(HttpMethod.GET,
                                                       locationId,
                                                       routeValues,
                                                       apiVersion,
                                                       queryParameters,
                                                       APPLICATION_JSON_TYPE);

        return super.sendRequest(httpRequest, GitCommit.class);
    }

    /** 
     * Retrieve a particular commit.
     * 
     * @param project 
     *            Project ID
     * @param commitId 
     *            The id of the commit.
     * @param repositoryId 
     *            The id or friendly name of the repository. To use the friendly name, projectId must also be specified.
     * @param changeCount 
     *            The number of changes to include in the result.
     * @return GitCommit
     */
    public GitCommit getCommit(
        final UUID project, 
        final String commitId, 
        final String repositoryId, 
        final Integer changeCount) {

        final UUID locationId = UUID.fromString("c2570c3b-5b3f-41b8-98bf-5407bfde8d58"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("project", project); //$NON-NLS-1$
        routeValues.put("commitId", commitId); //$NON-NLS-1$
        routeValues.put("repositoryId", repositoryId); //$NON-NLS-1$

        final NameValueCollection queryParameters = new NameValueCollection();
        queryParameters.addIfNotNull("changeCount", changeCount); //$NON-NLS-1$

        final Object httpRequest = super.createRequest(HttpMethod.GET,
                                                       locationId,
                                                       routeValues,
                                                       apiVersion,
                                                       queryParameters,
                                                       APPLICATION_JSON_TYPE);

        return super.sendRequest(httpRequest, GitCommit.class);
    }

    /** 
     * Retrieve a particular commit.
     * 
     * @param project 
     *            Project ID
     * @param commitId 
     *            The id of the commit.
     * @param repositoryId 
     *            The id or friendly name of the repository. To use the friendly name, projectId must also be specified.
     * @param changeCount 
     *            The number of changes to include in the result.
     * @return GitCommit
     */
    public GitCommit getCommit(
        final UUID project, 
        final String commitId, 
        final UUID repositoryId, 
        final Integer changeCount) {

        final UUID locationId = UUID.fromString("c2570c3b-5b3f-41b8-98bf-5407bfde8d58"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("project", project); //$NON-NLS-1$
        routeValues.put("commitId", commitId); //$NON-NLS-1$
        routeValues.put("repositoryId", repositoryId); //$NON-NLS-1$

        final NameValueCollection queryParameters = new NameValueCollection();
        queryParameters.addIfNotNull("changeCount", changeCount); //$NON-NLS-1$

        final Object httpRequest = super.createRequest(HttpMethod.GET,
                                                       locationId,
                                                       routeValues,
                                                       apiVersion,
                                                       queryParameters,
                                                       APPLICATION_JSON_TYPE);

        return super.sendRequest(httpRequest, GitCommit.class);
    }

    /** 
     * Retrieve a particular commit.
     * 
     * @param commitId 
     *            The id of the commit.
     * @param repositoryId 
     *            The id or friendly name of the repository. To use the friendly name, projectId must also be specified.
     * @param changeCount 
     *            The number of changes to include in the result.
     * @return GitCommit
     */
    public GitCommit getCommit(
        final String commitId, 
        final String repositoryId, 
        final Integer changeCount) {

        final UUID locationId = UUID.fromString("c2570c3b-5b3f-41b8-98bf-5407bfde8d58"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("commitId", commitId); //$NON-NLS-1$
        routeValues.put("repositoryId", repositoryId); //$NON-NLS-1$

        final NameValueCollection queryParameters = new NameValueCollection();
        queryParameters.addIfNotNull("changeCount", changeCount); //$NON-NLS-1$

        final Object httpRequest = super.createRequest(HttpMethod.GET,
                                                       locationId,
                                                       routeValues,
                                                       apiVersion,
                                                       queryParameters,
                                                       APPLICATION_JSON_TYPE);

        return super.sendRequest(httpRequest, GitCommit.class);
    }

    /** 
     * Retrieve a particular commit.
     * 
     * @param commitId 
     *            The id of the commit.
     * @param repositoryId 
     *            The id or friendly name of the repository. To use the friendly name, projectId must also be specified.
     * @param changeCount 
     *            The number of changes to include in the result.
     * @return GitCommit
     */
    public GitCommit getCommit(
        final String commitId, 
        final UUID repositoryId, 
        final Integer changeCount) {

        final UUID locationId = UUID.fromString("c2570c3b-5b3f-41b8-98bf-5407bfde8d58"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("commitId", commitId); //$NON-NLS-1$
        routeValues.put("repositoryId", repositoryId); //$NON-NLS-1$

        final NameValueCollection queryParameters = new NameValueCollection();
        queryParameters.addIfNotNull("changeCount", changeCount); //$NON-NLS-1$

        final Object httpRequest = super.createRequest(HttpMethod.GET,
                                                       locationId,
                                                       routeValues,
                                                       apiVersion,
                                                       queryParameters,
                                                       APPLICATION_JSON_TYPE);

        return super.sendRequest(httpRequest, GitCommit.class);
    }

    /** 
     * Retrieve git commits for a project
     * 
     * @param project 
     *            Project ID or project name
     * @param repositoryId 
     *            The id or friendly name of the repository. To use the friendly name, projectId must also be specified.
     * @param searchCriteria 
     *            
     * @param skip 
     *            
     * @param top 
     *            
     * @return List<GitCommitRef>
     */
    public List<GitCommitRef> getCommits(
        final String project, 
        final String repositoryId, 
        final GitQueryCommitsCriteria searchCriteria, 
        final Integer skip, 
        final Integer top) {

        final UUID locationId = UUID.fromString("c2570c3b-5b3f-41b8-98bf-5407bfde8d58"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("project", project); //$NON-NLS-1$
        routeValues.put("repositoryId", repositoryId); //$NON-NLS-1$

        final NameValueCollection queryParameters = new NameValueCollection();
        addModelAsQueryParams(queryParameters, searchCriteria);
        queryParameters.addIfNotNull("$skip", skip); //$NON-NLS-1$
        queryParameters.addIfNotNull("$top", top); //$NON-NLS-1$

        final Object httpRequest = super.createRequest(HttpMethod.GET,
                                                       locationId,
                                                       routeValues,
                                                       apiVersion,
                                                       queryParameters,
                                                       APPLICATION_JSON_TYPE);

        return super.sendRequest(httpRequest, new TypeReference<List<GitCommitRef>>() {});
    }

    /** 
     * Retrieve git commits for a project
     * 
     * @param project 
     *            Project ID or project name
     * @param repositoryId 
     *            The id or friendly name of the repository. To use the friendly name, projectId must also be specified.
     * @param searchCriteria 
     *            
     * @param skip 
     *            
     * @param top 
     *            
     * @return List<GitCommitRef>
     */
    public List<GitCommitRef> getCommits(
        final String project, 
        final UUID repositoryId, 
        final GitQueryCommitsCriteria searchCriteria, 
        final Integer skip, 
        final Integer top) {

        final UUID locationId = UUID.fromString("c2570c3b-5b3f-41b8-98bf-5407bfde8d58"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("project", project); //$NON-NLS-1$
        routeValues.put("repositoryId", repositoryId); //$NON-NLS-1$

        final NameValueCollection queryParameters = new NameValueCollection();
        addModelAsQueryParams(queryParameters, searchCriteria);
        queryParameters.addIfNotNull("$skip", skip); //$NON-NLS-1$
        queryParameters.addIfNotNull("$top", top); //$NON-NLS-1$

        final Object httpRequest = super.createRequest(HttpMethod.GET,
                                                       locationId,
                                                       routeValues,
                                                       apiVersion,
                                                       queryParameters,
                                                       APPLICATION_JSON_TYPE);

        return super.sendRequest(httpRequest, new TypeReference<List<GitCommitRef>>() {});
    }

    /** 
     * Retrieve git commits for a project
     * 
     * @param project 
     *            Project ID
     * @param repositoryId 
     *            The id or friendly name of the repository. To use the friendly name, projectId must also be specified.
     * @param searchCriteria 
     *            
     * @param skip 
     *            
     * @param top 
     *            
     * @return List<GitCommitRef>
     */
    public List<GitCommitRef> getCommits(
        final UUID project, 
        final String repositoryId, 
        final GitQueryCommitsCriteria searchCriteria, 
        final Integer skip, 
        final Integer top) {

        final UUID locationId = UUID.fromString("c2570c3b-5b3f-41b8-98bf-5407bfde8d58"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("project", project); //$NON-NLS-1$
        routeValues.put("repositoryId", repositoryId); //$NON-NLS-1$

        final NameValueCollection queryParameters = new NameValueCollection();
        addModelAsQueryParams(queryParameters, searchCriteria);
        queryParameters.addIfNotNull("$skip", skip); //$NON-NLS-1$
        queryParameters.addIfNotNull("$top", top); //$NON-NLS-1$

        final Object httpRequest = super.createRequest(HttpMethod.GET,
                                                       locationId,
                                                       routeValues,
                                                       apiVersion,
                                                       queryParameters,
                                                       APPLICATION_JSON_TYPE);

        return super.sendRequest(httpRequest, new TypeReference<List<GitCommitRef>>() {});
    }

    /** 
     * Retrieve git commits for a project
     * 
     * @param project 
     *            Project ID
     * @param repositoryId 
     *            The id or friendly name of the repository. To use the friendly name, projectId must also be specified.
     * @param searchCriteria 
     *            
     * @param skip 
     *            
     * @param top 
     *            
     * @return List<GitCommitRef>
     */
    public List<GitCommitRef> getCommits(
        final UUID project, 
        final UUID repositoryId, 
        final GitQueryCommitsCriteria searchCriteria, 
        final Integer skip, 
        final Integer top) {

        final UUID locationId = UUID.fromString("c2570c3b-5b3f-41b8-98bf-5407bfde8d58"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("project", project); //$NON-NLS-1$
        routeValues.put("repositoryId", repositoryId); //$NON-NLS-1$

        final NameValueCollection queryParameters = new NameValueCollection();
        addModelAsQueryParams(queryParameters, searchCriteria);
        queryParameters.addIfNotNull("$skip", skip); //$NON-NLS-1$
        queryParameters.addIfNotNull("$top", top); //$NON-NLS-1$

        final Object httpRequest = super.createRequest(HttpMethod.GET,
                                                       locationId,
                                                       routeValues,
                                                       apiVersion,
                                                       queryParameters,
                                                       APPLICATION_JSON_TYPE);

        return super.sendRequest(httpRequest, new TypeReference<List<GitCommitRef>>() {});
    }

    /** 
     * Retrieve git commits for a project
     * 
     * @param repositoryId 
     *            The id or friendly name of the repository. To use the friendly name, projectId must also be specified.
     * @param searchCriteria 
     *            
     * @param skip 
     *            
     * @param top 
     *            
     * @return List<GitCommitRef>
     */
    public List<GitCommitRef> getCommits(
        final String repositoryId, 
        final GitQueryCommitsCriteria searchCriteria, 
        final Integer skip, 
        final Integer top) {

        final UUID locationId = UUID.fromString("c2570c3b-5b3f-41b8-98bf-5407bfde8d58"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("repositoryId", repositoryId); //$NON-NLS-1$

        final NameValueCollection queryParameters = new NameValueCollection();
        addModelAsQueryParams(queryParameters, searchCriteria);
        queryParameters.addIfNotNull("$skip", skip); //$NON-NLS-1$
        queryParameters.addIfNotNull("$top", top); //$NON-NLS-1$

        final Object httpRequest = super.createRequest(HttpMethod.GET,
                                                       locationId,
                                                       routeValues,
                                                       apiVersion,
                                                       queryParameters,
                                                       APPLICATION_JSON_TYPE);

        return super.sendRequest(httpRequest, new TypeReference<List<GitCommitRef>>() {});
    }

    /** 
     * Retrieve git commits for a project
     * 
     * @param repositoryId 
     *            The id or friendly name of the repository. To use the friendly name, projectId must also be specified.
     * @param searchCriteria 
     *            
     * @param skip 
     *            
     * @param top 
     *            
     * @return List<GitCommitRef>
     */
    public List<GitCommitRef> getCommits(
        final UUID repositoryId, 
        final GitQueryCommitsCriteria searchCriteria, 
        final Integer skip, 
        final Integer top) {

        final UUID locationId = UUID.fromString("c2570c3b-5b3f-41b8-98bf-5407bfde8d58"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("repositoryId", repositoryId); //$NON-NLS-1$

        final NameValueCollection queryParameters = new NameValueCollection();
        addModelAsQueryParams(queryParameters, searchCriteria);
        queryParameters.addIfNotNull("$skip", skip); //$NON-NLS-1$
        queryParameters.addIfNotNull("$top", top); //$NON-NLS-1$

        final Object httpRequest = super.createRequest(HttpMethod.GET,
                                                       locationId,
                                                       routeValues,
                                                       apiVersion,
                                                       queryParameters,
                                                       APPLICATION_JSON_TYPE);

        return super.sendRequest(httpRequest, new TypeReference<List<GitCommitRef>>() {});
    }

    /** 
     * Retrieve a list of commits associated with a particular push.
     * 
     * @param project 
     *            Project ID or project name
     * @param repositoryId 
     *            The id or friendly name of the repository. To use the friendly name, projectId must also be specified.
     * @param pushId 
     *            The id of the push.
     * @param top 
     *            The maximum number of commits to return ("get the top x commits").
     * @param skip 
     *            The number of commits to skip.
     * @param includeLinks 
     *            
     * @return List<GitCommitRef>
     */
    public List<GitCommitRef> getPushCommits(
        final String project, 
        final String repositoryId, 
        final int pushId, 
        final Integer top, 
        final Integer skip, 
        final Boolean includeLinks) {

        final UUID locationId = UUID.fromString("c2570c3b-5b3f-41b8-98bf-5407bfde8d58"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("project", project); //$NON-NLS-1$
        routeValues.put("repositoryId", repositoryId); //$NON-NLS-1$

        final NameValueCollection queryParameters = new NameValueCollection();
        queryParameters.put("pushId", String.valueOf(pushId)); //$NON-NLS-1$
        queryParameters.addIfNotNull("top", top); //$NON-NLS-1$
        queryParameters.addIfNotNull("skip", skip); //$NON-NLS-1$
        queryParameters.addIfNotNull("includeLinks", includeLinks); //$NON-NLS-1$

        final Object httpRequest = super.createRequest(HttpMethod.GET,
                                                       locationId,
                                                       routeValues,
                                                       apiVersion,
                                                       queryParameters,
                                                       APPLICATION_JSON_TYPE);

        return super.sendRequest(httpRequest, new TypeReference<List<GitCommitRef>>() {});
    }

    /** 
     * Retrieve a list of commits associated with a particular push.
     * 
     * @param project 
     *            Project ID or project name
     * @param repositoryId 
     *            The id or friendly name of the repository. To use the friendly name, projectId must also be specified.
     * @param pushId 
     *            The id of the push.
     * @param top 
     *            The maximum number of commits to return ("get the top x commits").
     * @param skip 
     *            The number of commits to skip.
     * @param includeLinks 
     *            
     * @return List<GitCommitRef>
     */
    public List<GitCommitRef> getPushCommits(
        final String project, 
        final UUID repositoryId, 
        final int pushId, 
        final Integer top, 
        final Integer skip, 
        final Boolean includeLinks) {

        final UUID locationId = UUID.fromString("c2570c3b-5b3f-41b8-98bf-5407bfde8d58"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("project", project); //$NON-NLS-1$
        routeValues.put("repositoryId", repositoryId); //$NON-NLS-1$

        final NameValueCollection queryParameters = new NameValueCollection();
        queryParameters.put("pushId", String.valueOf(pushId)); //$NON-NLS-1$
        queryParameters.addIfNotNull("top", top); //$NON-NLS-1$
        queryParameters.addIfNotNull("skip", skip); //$NON-NLS-1$
        queryParameters.addIfNotNull("includeLinks", includeLinks); //$NON-NLS-1$

        final Object httpRequest = super.createRequest(HttpMethod.GET,
                                                       locationId,
                                                       routeValues,
                                                       apiVersion,
                                                       queryParameters,
                                                       APPLICATION_JSON_TYPE);

        return super.sendRequest(httpRequest, new TypeReference<List<GitCommitRef>>() {});
    }

    /** 
     * Retrieve a list of commits associated with a particular push.
     * 
     * @param project 
     *            Project ID
     * @param repositoryId 
     *            The id or friendly name of the repository. To use the friendly name, projectId must also be specified.
     * @param pushId 
     *            The id of the push.
     * @param top 
     *            The maximum number of commits to return ("get the top x commits").
     * @param skip 
     *            The number of commits to skip.
     * @param includeLinks 
     *            
     * @return List<GitCommitRef>
     */
    public List<GitCommitRef> getPushCommits(
        final UUID project, 
        final String repositoryId, 
        final int pushId, 
        final Integer top, 
        final Integer skip, 
        final Boolean includeLinks) {

        final UUID locationId = UUID.fromString("c2570c3b-5b3f-41b8-98bf-5407bfde8d58"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("project", project); //$NON-NLS-1$
        routeValues.put("repositoryId", repositoryId); //$NON-NLS-1$

        final NameValueCollection queryParameters = new NameValueCollection();
        queryParameters.put("pushId", String.valueOf(pushId)); //$NON-NLS-1$
        queryParameters.addIfNotNull("top", top); //$NON-NLS-1$
        queryParameters.addIfNotNull("skip", skip); //$NON-NLS-1$
        queryParameters.addIfNotNull("includeLinks", includeLinks); //$NON-NLS-1$

        final Object httpRequest = super.createRequest(HttpMethod.GET,
                                                       locationId,
                                                       routeValues,
                                                       apiVersion,
                                                       queryParameters,
                                                       APPLICATION_JSON_TYPE);

        return super.sendRequest(httpRequest, new TypeReference<List<GitCommitRef>>() {});
    }

    /** 
     * Retrieve a list of commits associated with a particular push.
     * 
     * @param project 
     *            Project ID
     * @param repositoryId 
     *            The id or friendly name of the repository. To use the friendly name, projectId must also be specified.
     * @param pushId 
     *            The id of the push.
     * @param top 
     *            The maximum number of commits to return ("get the top x commits").
     * @param skip 
     *            The number of commits to skip.
     * @param includeLinks 
     *            
     * @return List<GitCommitRef>
     */
    public List<GitCommitRef> getPushCommits(
        final UUID project, 
        final UUID repositoryId, 
        final int pushId, 
        final Integer top, 
        final Integer skip, 
        final Boolean includeLinks) {

        final UUID locationId = UUID.fromString("c2570c3b-5b3f-41b8-98bf-5407bfde8d58"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("project", project); //$NON-NLS-1$
        routeValues.put("repositoryId", repositoryId); //$NON-NLS-1$

        final NameValueCollection queryParameters = new NameValueCollection();
        queryParameters.put("pushId", String.valueOf(pushId)); //$NON-NLS-1$
        queryParameters.addIfNotNull("top", top); //$NON-NLS-1$
        queryParameters.addIfNotNull("skip", skip); //$NON-NLS-1$
        queryParameters.addIfNotNull("includeLinks", includeLinks); //$NON-NLS-1$

        final Object httpRequest = super.createRequest(HttpMethod.GET,
                                                       locationId,
                                                       routeValues,
                                                       apiVersion,
                                                       queryParameters,
                                                       APPLICATION_JSON_TYPE);

        return super.sendRequest(httpRequest, new TypeReference<List<GitCommitRef>>() {});
    }

    /** 
     * Retrieve a list of commits associated with a particular push.
     * 
     * @param repositoryId 
     *            The id or friendly name of the repository. To use the friendly name, projectId must also be specified.
     * @param pushId 
     *            The id of the push.
     * @param top 
     *            The maximum number of commits to return ("get the top x commits").
     * @param skip 
     *            The number of commits to skip.
     * @param includeLinks 
     *            
     * @return List<GitCommitRef>
     */
    public List<GitCommitRef> getPushCommits(
        final String repositoryId, 
        final int pushId, 
        final Integer top, 
        final Integer skip, 
        final Boolean includeLinks) {

        final UUID locationId = UUID.fromString("c2570c3b-5b3f-41b8-98bf-5407bfde8d58"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("repositoryId", repositoryId); //$NON-NLS-1$

        final NameValueCollection queryParameters = new NameValueCollection();
        queryParameters.put("pushId", String.valueOf(pushId)); //$NON-NLS-1$
        queryParameters.addIfNotNull("top", top); //$NON-NLS-1$
        queryParameters.addIfNotNull("skip", skip); //$NON-NLS-1$
        queryParameters.addIfNotNull("includeLinks", includeLinks); //$NON-NLS-1$

        final Object httpRequest = super.createRequest(HttpMethod.GET,
                                                       locationId,
                                                       routeValues,
                                                       apiVersion,
                                                       queryParameters,
                                                       APPLICATION_JSON_TYPE);

        return super.sendRequest(httpRequest, new TypeReference<List<GitCommitRef>>() {});
    }

    /** 
     * Retrieve a list of commits associated with a particular push.
     * 
     * @param repositoryId 
     *            The id or friendly name of the repository. To use the friendly name, projectId must also be specified.
     * @param pushId 
     *            The id of the push.
     * @param top 
     *            The maximum number of commits to return ("get the top x commits").
     * @param skip 
     *            The number of commits to skip.
     * @param includeLinks 
     *            
     * @return List<GitCommitRef>
     */
    public List<GitCommitRef> getPushCommits(
        final UUID repositoryId, 
        final int pushId, 
        final Integer top, 
        final Integer skip, 
        final Boolean includeLinks) {

        final UUID locationId = UUID.fromString("c2570c3b-5b3f-41b8-98bf-5407bfde8d58"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("repositoryId", repositoryId); //$NON-NLS-1$

        final NameValueCollection queryParameters = new NameValueCollection();
        queryParameters.put("pushId", String.valueOf(pushId)); //$NON-NLS-1$
        queryParameters.addIfNotNull("top", top); //$NON-NLS-1$
        queryParameters.addIfNotNull("skip", skip); //$NON-NLS-1$
        queryParameters.addIfNotNull("includeLinks", includeLinks); //$NON-NLS-1$

        final Object httpRequest = super.createRequest(HttpMethod.GET,
                                                       locationId,
                                                       routeValues,
                                                       apiVersion,
                                                       queryParameters,
                                                       APPLICATION_JSON_TYPE);

        return super.sendRequest(httpRequest, new TypeReference<List<GitCommitRef>>() {});
    }

    /** 
     * Retrieve git commits for a project
     * 
     * @param searchCriteria 
     *            Search options
     * @param repositoryId 
     *            The id or friendly name of the repository. To use the friendly name, projectId must also be specified.
     * @param skip 
     *            
     * @param top 
     *            
     * @return List<GitCommitRef>
     */
    public List<GitCommitRef> getCommitsBatch(
        final GitQueryCommitsCriteria searchCriteria, 
        final String repositoryId, 
        final Integer skip, 
        final Integer top) {

        final UUID locationId = UUID.fromString("6400dfb2-0bcb-462b-b992-5a57f8f1416c"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("repositoryId", repositoryId); //$NON-NLS-1$

        final NameValueCollection queryParameters = new NameValueCollection();
        queryParameters.addIfNotNull("$skip", skip); //$NON-NLS-1$
        queryParameters.addIfNotNull("$top", top); //$NON-NLS-1$

        final Object httpRequest = super.createRequest(HttpMethod.POST,
                                                       locationId,
                                                       routeValues,
                                                       apiVersion,
                                                       searchCriteria,
                                                       APPLICATION_JSON_TYPE,
                                                       queryParameters,
                                                       APPLICATION_JSON_TYPE);

        return super.sendRequest(httpRequest, new TypeReference<List<GitCommitRef>>() {});
    }

    /** 
     * Retrieve git commits for a project
     * 
     * @param searchCriteria 
     *            Search options
     * @param repositoryId 
     *            The id or friendly name of the repository. To use the friendly name, projectId must also be specified.
     * @param skip 
     *            
     * @param top 
     *            
     * @return List<GitCommitRef>
     */
    public List<GitCommitRef> getCommitsBatch(
        final GitQueryCommitsCriteria searchCriteria, 
        final UUID repositoryId, 
        final Integer skip, 
        final Integer top) {

        final UUID locationId = UUID.fromString("6400dfb2-0bcb-462b-b992-5a57f8f1416c"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("repositoryId", repositoryId); //$NON-NLS-1$

        final NameValueCollection queryParameters = new NameValueCollection();
        queryParameters.addIfNotNull("$skip", skip); //$NON-NLS-1$
        queryParameters.addIfNotNull("$top", top); //$NON-NLS-1$

        final Object httpRequest = super.createRequest(HttpMethod.POST,
                                                       locationId,
                                                       routeValues,
                                                       apiVersion,
                                                       searchCriteria,
                                                       APPLICATION_JSON_TYPE,
                                                       queryParameters,
                                                       APPLICATION_JSON_TYPE);

        return super.sendRequest(httpRequest, new TypeReference<List<GitCommitRef>>() {});
    }

    /** 
     * Retrieve git commits for a project
     * 
     * @param searchCriteria 
     *            Search options
     * @param project 
     *            Project ID or project name
     * @param repositoryId 
     *            The id or friendly name of the repository. To use the friendly name, projectId must also be specified.
     * @param skip 
     *            
     * @param top 
     *            
     * @return List<GitCommitRef>
     */
    public List<GitCommitRef> getCommitsBatch(
        final GitQueryCommitsCriteria searchCriteria, 
        final String project, 
        final String repositoryId, 
        final Integer skip, 
        final Integer top) {

        final UUID locationId = UUID.fromString("6400dfb2-0bcb-462b-b992-5a57f8f1416c"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("project", project); //$NON-NLS-1$
        routeValues.put("repositoryId", repositoryId); //$NON-NLS-1$

        final NameValueCollection queryParameters = new NameValueCollection();
        queryParameters.addIfNotNull("$skip", skip); //$NON-NLS-1$
        queryParameters.addIfNotNull("$top", top); //$NON-NLS-1$

        final Object httpRequest = super.createRequest(HttpMethod.POST,
                                                       locationId,
                                                       routeValues,
                                                       apiVersion,
                                                       searchCriteria,
                                                       APPLICATION_JSON_TYPE,
                                                       queryParameters,
                                                       APPLICATION_JSON_TYPE);

        return super.sendRequest(httpRequest, new TypeReference<List<GitCommitRef>>() {});
    }

    /** 
     * Retrieve git commits for a project
     * 
     * @param searchCriteria 
     *            Search options
     * @param project 
     *            Project ID or project name
     * @param repositoryId 
     *            The id or friendly name of the repository. To use the friendly name, projectId must also be specified.
     * @param skip 
     *            
     * @param top 
     *            
     * @return List<GitCommitRef>
     */
    public List<GitCommitRef> getCommitsBatch(
        final GitQueryCommitsCriteria searchCriteria, 
        final String project, 
        final UUID repositoryId, 
        final Integer skip, 
        final Integer top) {

        final UUID locationId = UUID.fromString("6400dfb2-0bcb-462b-b992-5a57f8f1416c"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("project", project); //$NON-NLS-1$
        routeValues.put("repositoryId", repositoryId); //$NON-NLS-1$

        final NameValueCollection queryParameters = new NameValueCollection();
        queryParameters.addIfNotNull("$skip", skip); //$NON-NLS-1$
        queryParameters.addIfNotNull("$top", top); //$NON-NLS-1$

        final Object httpRequest = super.createRequest(HttpMethod.POST,
                                                       locationId,
                                                       routeValues,
                                                       apiVersion,
                                                       searchCriteria,
                                                       APPLICATION_JSON_TYPE,
                                                       queryParameters,
                                                       APPLICATION_JSON_TYPE);

        return super.sendRequest(httpRequest, new TypeReference<List<GitCommitRef>>() {});
    }

    /** 
     * Retrieve git commits for a project
     * 
     * @param searchCriteria 
     *            Search options
     * @param project 
     *            Project ID
     * @param repositoryId 
     *            The id or friendly name of the repository. To use the friendly name, projectId must also be specified.
     * @param skip 
     *            
     * @param top 
     *            
     * @return List<GitCommitRef>
     */
    public List<GitCommitRef> getCommitsBatch(
        final GitQueryCommitsCriteria searchCriteria, 
        final UUID project, 
        final String repositoryId, 
        final Integer skip, 
        final Integer top) {

        final UUID locationId = UUID.fromString("6400dfb2-0bcb-462b-b992-5a57f8f1416c"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("project", project); //$NON-NLS-1$
        routeValues.put("repositoryId", repositoryId); //$NON-NLS-1$

        final NameValueCollection queryParameters = new NameValueCollection();
        queryParameters.addIfNotNull("$skip", skip); //$NON-NLS-1$
        queryParameters.addIfNotNull("$top", top); //$NON-NLS-1$

        final Object httpRequest = super.createRequest(HttpMethod.POST,
                                                       locationId,
                                                       routeValues,
                                                       apiVersion,
                                                       searchCriteria,
                                                       APPLICATION_JSON_TYPE,
                                                       queryParameters,
                                                       APPLICATION_JSON_TYPE);

        return super.sendRequest(httpRequest, new TypeReference<List<GitCommitRef>>() {});
    }

    /** 
     * Retrieve git commits for a project
     * 
     * @param searchCriteria 
     *            Search options
     * @param project 
     *            Project ID
     * @param repositoryId 
     *            The id or friendly name of the repository. To use the friendly name, projectId must also be specified.
     * @param skip 
     *            
     * @param top 
     *            
     * @return List<GitCommitRef>
     */
    public List<GitCommitRef> getCommitsBatch(
        final GitQueryCommitsCriteria searchCriteria, 
        final UUID project, 
        final UUID repositoryId, 
        final Integer skip, 
        final Integer top) {

        final UUID locationId = UUID.fromString("6400dfb2-0bcb-462b-b992-5a57f8f1416c"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("project", project); //$NON-NLS-1$
        routeValues.put("repositoryId", repositoryId); //$NON-NLS-1$

        final NameValueCollection queryParameters = new NameValueCollection();
        queryParameters.addIfNotNull("$skip", skip); //$NON-NLS-1$
        queryParameters.addIfNotNull("$top", top); //$NON-NLS-1$

        final Object httpRequest = super.createRequest(HttpMethod.POST,
                                                       locationId,
                                                       routeValues,
                                                       apiVersion,
                                                       searchCriteria,
                                                       APPLICATION_JSON_TYPE,
                                                       queryParameters,
                                                       APPLICATION_JSON_TYPE);

        return super.sendRequest(httpRequest, new TypeReference<List<GitCommitRef>>() {});
    }

    /** 
     * Get Item Metadata and/or Content for a single item. The download parameter is to indicate whether the content should be available as a download or just sent as a stream in the response. Doesn't apply to zipped content which is always returned as a download.
     * 
     * @param project 
     *            Project ID or project name
     * @param repositoryId 
     *            
     * @param path 
     *            
     * @param scopePath 
     *            
     * @param recursionLevel 
     *            
     * @param includeContentMetadata 
     *            
     * @param latestProcessedChange 
     *            
     * @param download 
     *            
     * @param versionDescriptor 
     *            
     * @return GitItem
     */
    public GitItem getItem(
        final String project, 
        final String repositoryId, 
        final String path, 
        final String scopePath, 
        final VersionControlRecursionType recursionLevel, 
        final Boolean includeContentMetadata, 
        final Boolean latestProcessedChange, 
        final Boolean download, 
        final GitVersionDescriptor versionDescriptor) {

        final UUID locationId = UUID.fromString("fb93c0db-47ed-4a31-8c20-47552878fb44"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("project", project); //$NON-NLS-1$
        routeValues.put("repositoryId", repositoryId); //$NON-NLS-1$

        final NameValueCollection queryParameters = new NameValueCollection();
        queryParameters.addIfNotEmpty("path", path); //$NON-NLS-1$
        queryParameters.addIfNotEmpty("scopePath", scopePath); //$NON-NLS-1$
        queryParameters.addIfNotNull("recursionLevel", recursionLevel); //$NON-NLS-1$
        queryParameters.addIfNotNull("includeContentMetadata", includeContentMetadata); //$NON-NLS-1$
        queryParameters.addIfNotNull("latestProcessedChange", latestProcessedChange); //$NON-NLS-1$
        queryParameters.addIfNotNull("download", download); //$NON-NLS-1$
        addModelAsQueryParams(queryParameters, versionDescriptor);

        final Object httpRequest = super.createRequest(HttpMethod.GET,
                                                       locationId,
                                                       routeValues,
                                                       apiVersion,
                                                       queryParameters,
                                                       APPLICATION_JSON_TYPE);

        return super.sendRequest(httpRequest, GitItem.class);
    }

    /** 
     * Get Item Metadata and/or Content for a single item. The download parameter is to indicate whether the content should be available as a download or just sent as a stream in the response. Doesn't apply to zipped content which is always returned as a download.
     * 
     * @param project 
     *            Project ID or project name
     * @param repositoryId 
     *            
     * @param path 
     *            
     * @param scopePath 
     *            
     * @param recursionLevel 
     *            
     * @param includeContentMetadata 
     *            
     * @param latestProcessedChange 
     *            
     * @param download 
     *            
     * @param versionDescriptor 
     *            
     * @return GitItem
     */
    public GitItem getItem(
        final String project, 
        final UUID repositoryId, 
        final String path, 
        final String scopePath, 
        final VersionControlRecursionType recursionLevel, 
        final Boolean includeContentMetadata, 
        final Boolean latestProcessedChange, 
        final Boolean download, 
        final GitVersionDescriptor versionDescriptor) {

        final UUID locationId = UUID.fromString("fb93c0db-47ed-4a31-8c20-47552878fb44"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("project", project); //$NON-NLS-1$
        routeValues.put("repositoryId", repositoryId); //$NON-NLS-1$

        final NameValueCollection queryParameters = new NameValueCollection();
        queryParameters.addIfNotEmpty("path", path); //$NON-NLS-1$
        queryParameters.addIfNotEmpty("scopePath", scopePath); //$NON-NLS-1$
        queryParameters.addIfNotNull("recursionLevel", recursionLevel); //$NON-NLS-1$
        queryParameters.addIfNotNull("includeContentMetadata", includeContentMetadata); //$NON-NLS-1$
        queryParameters.addIfNotNull("latestProcessedChange", latestProcessedChange); //$NON-NLS-1$
        queryParameters.addIfNotNull("download", download); //$NON-NLS-1$
        addModelAsQueryParams(queryParameters, versionDescriptor);

        final Object httpRequest = super.createRequest(HttpMethod.GET,
                                                       locationId,
                                                       routeValues,
                                                       apiVersion,
                                                       queryParameters,
                                                       APPLICATION_JSON_TYPE);

        return super.sendRequest(httpRequest, GitItem.class);
    }

    /** 
     * Get Item Metadata and/or Content for a single item. The download parameter is to indicate whether the content should be available as a download or just sent as a stream in the response. Doesn't apply to zipped content which is always returned as a download.
     * 
     * @param project 
     *            Project ID
     * @param repositoryId 
     *            
     * @param path 
     *            
     * @param scopePath 
     *            
     * @param recursionLevel 
     *            
     * @param includeContentMetadata 
     *            
     * @param latestProcessedChange 
     *            
     * @param download 
     *            
     * @param versionDescriptor 
     *            
     * @return GitItem
     */
    public GitItem getItem(
        final UUID project, 
        final String repositoryId, 
        final String path, 
        final String scopePath, 
        final VersionControlRecursionType recursionLevel, 
        final Boolean includeContentMetadata, 
        final Boolean latestProcessedChange, 
        final Boolean download, 
        final GitVersionDescriptor versionDescriptor) {

        final UUID locationId = UUID.fromString("fb93c0db-47ed-4a31-8c20-47552878fb44"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("project", project); //$NON-NLS-1$
        routeValues.put("repositoryId", repositoryId); //$NON-NLS-1$

        final NameValueCollection queryParameters = new NameValueCollection();
        queryParameters.addIfNotEmpty("path", path); //$NON-NLS-1$
        queryParameters.addIfNotEmpty("scopePath", scopePath); //$NON-NLS-1$
        queryParameters.addIfNotNull("recursionLevel", recursionLevel); //$NON-NLS-1$
        queryParameters.addIfNotNull("includeContentMetadata", includeContentMetadata); //$NON-NLS-1$
        queryParameters.addIfNotNull("latestProcessedChange", latestProcessedChange); //$NON-NLS-1$
        queryParameters.addIfNotNull("download", download); //$NON-NLS-1$
        addModelAsQueryParams(queryParameters, versionDescriptor);

        final Object httpRequest = super.createRequest(HttpMethod.GET,
                                                       locationId,
                                                       routeValues,
                                                       apiVersion,
                                                       queryParameters,
                                                       APPLICATION_JSON_TYPE);

        return super.sendRequest(httpRequest, GitItem.class);
    }

    /** 
     * Get Item Metadata and/or Content for a single item. The download parameter is to indicate whether the content should be available as a download or just sent as a stream in the response. Doesn't apply to zipped content which is always returned as a download.
     * 
     * @param project 
     *            Project ID
     * @param repositoryId 
     *            
     * @param path 
     *            
     * @param scopePath 
     *            
     * @param recursionLevel 
     *            
     * @param includeContentMetadata 
     *            
     * @param latestProcessedChange 
     *            
     * @param download 
     *            
     * @param versionDescriptor 
     *            
     * @return GitItem
     */
    public GitItem getItem(
        final UUID project, 
        final UUID repositoryId, 
        final String path, 
        final String scopePath, 
        final VersionControlRecursionType recursionLevel, 
        final Boolean includeContentMetadata, 
        final Boolean latestProcessedChange, 
        final Boolean download, 
        final GitVersionDescriptor versionDescriptor) {

        final UUID locationId = UUID.fromString("fb93c0db-47ed-4a31-8c20-47552878fb44"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("project", project); //$NON-NLS-1$
        routeValues.put("repositoryId", repositoryId); //$NON-NLS-1$

        final NameValueCollection queryParameters = new NameValueCollection();
        queryParameters.addIfNotEmpty("path", path); //$NON-NLS-1$
        queryParameters.addIfNotEmpty("scopePath", scopePath); //$NON-NLS-1$
        queryParameters.addIfNotNull("recursionLevel", recursionLevel); //$NON-NLS-1$
        queryParameters.addIfNotNull("includeContentMetadata", includeContentMetadata); //$NON-NLS-1$
        queryParameters.addIfNotNull("latestProcessedChange", latestProcessedChange); //$NON-NLS-1$
        queryParameters.addIfNotNull("download", download); //$NON-NLS-1$
        addModelAsQueryParams(queryParameters, versionDescriptor);

        final Object httpRequest = super.createRequest(HttpMethod.GET,
                                                       locationId,
                                                       routeValues,
                                                       apiVersion,
                                                       queryParameters,
                                                       APPLICATION_JSON_TYPE);

        return super.sendRequest(httpRequest, GitItem.class);
    }

    /** 
     * Get Item Metadata and/or Content for a single item. The download parameter is to indicate whether the content should be available as a download or just sent as a stream in the response. Doesn't apply to zipped content which is always returned as a download.
     * 
     * @param repositoryId 
     *            
     * @param path 
     *            
     * @param scopePath 
     *            
     * @param recursionLevel 
     *            
     * @param includeContentMetadata 
     *            
     * @param latestProcessedChange 
     *            
     * @param download 
     *            
     * @param versionDescriptor 
     *            
     * @return GitItem
     */
    public GitItem getItem(
        final String repositoryId, 
        final String path, 
        final String scopePath, 
        final VersionControlRecursionType recursionLevel, 
        final Boolean includeContentMetadata, 
        final Boolean latestProcessedChange, 
        final Boolean download, 
        final GitVersionDescriptor versionDescriptor) {

        final UUID locationId = UUID.fromString("fb93c0db-47ed-4a31-8c20-47552878fb44"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("repositoryId", repositoryId); //$NON-NLS-1$

        final NameValueCollection queryParameters = new NameValueCollection();
        queryParameters.addIfNotEmpty("path", path); //$NON-NLS-1$
        queryParameters.addIfNotEmpty("scopePath", scopePath); //$NON-NLS-1$
        queryParameters.addIfNotNull("recursionLevel", recursionLevel); //$NON-NLS-1$
        queryParameters.addIfNotNull("includeContentMetadata", includeContentMetadata); //$NON-NLS-1$
        queryParameters.addIfNotNull("latestProcessedChange", latestProcessedChange); //$NON-NLS-1$
        queryParameters.addIfNotNull("download", download); //$NON-NLS-1$
        addModelAsQueryParams(queryParameters, versionDescriptor);

        final Object httpRequest = super.createRequest(HttpMethod.GET,
                                                       locationId,
                                                       routeValues,
                                                       apiVersion,
                                                       queryParameters,
                                                       APPLICATION_JSON_TYPE);

        return super.sendRequest(httpRequest, GitItem.class);
    }

    /** 
     * Get Item Metadata and/or Content for a single item. The download parameter is to indicate whether the content should be available as a download or just sent as a stream in the response. Doesn't apply to zipped content which is always returned as a download.
     * 
     * @param repositoryId 
     *            
     * @param path 
     *            
     * @param scopePath 
     *            
     * @param recursionLevel 
     *            
     * @param includeContentMetadata 
     *            
     * @param latestProcessedChange 
     *            
     * @param download 
     *            
     * @param versionDescriptor 
     *            
     * @return GitItem
     */
    public GitItem getItem(
        final UUID repositoryId, 
        final String path, 
        final String scopePath, 
        final VersionControlRecursionType recursionLevel, 
        final Boolean includeContentMetadata, 
        final Boolean latestProcessedChange, 
        final Boolean download, 
        final GitVersionDescriptor versionDescriptor) {

        final UUID locationId = UUID.fromString("fb93c0db-47ed-4a31-8c20-47552878fb44"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("repositoryId", repositoryId); //$NON-NLS-1$

        final NameValueCollection queryParameters = new NameValueCollection();
        queryParameters.addIfNotEmpty("path", path); //$NON-NLS-1$
        queryParameters.addIfNotEmpty("scopePath", scopePath); //$NON-NLS-1$
        queryParameters.addIfNotNull("recursionLevel", recursionLevel); //$NON-NLS-1$
        queryParameters.addIfNotNull("includeContentMetadata", includeContentMetadata); //$NON-NLS-1$
        queryParameters.addIfNotNull("latestProcessedChange", latestProcessedChange); //$NON-NLS-1$
        queryParameters.addIfNotNull("download", download); //$NON-NLS-1$
        addModelAsQueryParams(queryParameters, versionDescriptor);

        final Object httpRequest = super.createRequest(HttpMethod.GET,
                                                       locationId,
                                                       routeValues,
                                                       apiVersion,
                                                       queryParameters,
                                                       APPLICATION_JSON_TYPE);

        return super.sendRequest(httpRequest, GitItem.class);
    }

    /** 
     * Get Item Metadata and/or Content for a single item. The download parameter is to indicate whether the content should be available as a download or just sent as a stream in the response. Doesn't apply to zipped content which is always returned as a download.
     * 
     * @param project 
     *            Project ID or project name
     * @param repositoryId 
     *            
     * @param path 
     *            
     * @param scopePath 
     *            
     * @param recursionLevel 
     *            
     * @param includeContentMetadata 
     *            
     * @param latestProcessedChange 
     *            
     * @param download 
     *            
     * @param versionDescriptor 
     *            
     * @return InputStream
     */
    public InputStream getItemContent(
        final String project, 
        final String repositoryId, 
        final String path, 
        final String scopePath, 
        final VersionControlRecursionType recursionLevel, 
        final Boolean includeContentMetadata, 
        final Boolean latestProcessedChange, 
        final Boolean download, 
        final GitVersionDescriptor versionDescriptor) {

        final UUID locationId = UUID.fromString("fb93c0db-47ed-4a31-8c20-47552878fb44"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("project", project); //$NON-NLS-1$
        routeValues.put("repositoryId", repositoryId); //$NON-NLS-1$

        final NameValueCollection queryParameters = new NameValueCollection();
        queryParameters.addIfNotEmpty("path", path); //$NON-NLS-1$
        queryParameters.addIfNotEmpty("scopePath", scopePath); //$NON-NLS-1$
        queryParameters.addIfNotNull("recursionLevel", recursionLevel); //$NON-NLS-1$
        queryParameters.addIfNotNull("includeContentMetadata", includeContentMetadata); //$NON-NLS-1$
        queryParameters.addIfNotNull("latestProcessedChange", latestProcessedChange); //$NON-NLS-1$
        queryParameters.addIfNotNull("download", download); //$NON-NLS-1$
        addModelAsQueryParams(queryParameters, versionDescriptor);

        final Object httpRequest = super.createRequest(HttpMethod.GET,
                                                       locationId,
                                                       routeValues,
                                                       apiVersion,
                                                       queryParameters,
                                                       APPLICATION_OCTET_STREAM_TYPE);

        return super.sendRequest(httpRequest, InputStream.class);
    }

    /** 
     * Get Item Metadata and/or Content for a single item. The download parameter is to indicate whether the content should be available as a download or just sent as a stream in the response. Doesn't apply to zipped content which is always returned as a download.
     * 
     * @param project 
     *            Project ID or project name
     * @param repositoryId 
     *            
     * @param path 
     *            
     * @param scopePath 
     *            
     * @param recursionLevel 
     *            
     * @param includeContentMetadata 
     *            
     * @param latestProcessedChange 
     *            
     * @param download 
     *            
     * @param versionDescriptor 
     *            
     * @return InputStream
     */
    public InputStream getItemContent(
        final String project, 
        final UUID repositoryId, 
        final String path, 
        final String scopePath, 
        final VersionControlRecursionType recursionLevel, 
        final Boolean includeContentMetadata, 
        final Boolean latestProcessedChange, 
        final Boolean download, 
        final GitVersionDescriptor versionDescriptor) {

        final UUID locationId = UUID.fromString("fb93c0db-47ed-4a31-8c20-47552878fb44"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("project", project); //$NON-NLS-1$
        routeValues.put("repositoryId", repositoryId); //$NON-NLS-1$

        final NameValueCollection queryParameters = new NameValueCollection();
        queryParameters.addIfNotEmpty("path", path); //$NON-NLS-1$
        queryParameters.addIfNotEmpty("scopePath", scopePath); //$NON-NLS-1$
        queryParameters.addIfNotNull("recursionLevel", recursionLevel); //$NON-NLS-1$
        queryParameters.addIfNotNull("includeContentMetadata", includeContentMetadata); //$NON-NLS-1$
        queryParameters.addIfNotNull("latestProcessedChange", latestProcessedChange); //$NON-NLS-1$
        queryParameters.addIfNotNull("download", download); //$NON-NLS-1$
        addModelAsQueryParams(queryParameters, versionDescriptor);

        final Object httpRequest = super.createRequest(HttpMethod.GET,
                                                       locationId,
                                                       routeValues,
                                                       apiVersion,
                                                       queryParameters,
                                                       APPLICATION_OCTET_STREAM_TYPE);

        return super.sendRequest(httpRequest, InputStream.class);
    }

    /** 
     * Get Item Metadata and/or Content for a single item. The download parameter is to indicate whether the content should be available as a download or just sent as a stream in the response. Doesn't apply to zipped content which is always returned as a download.
     * 
     * @param project 
     *            Project ID
     * @param repositoryId 
     *            
     * @param path 
     *            
     * @param scopePath 
     *            
     * @param recursionLevel 
     *            
     * @param includeContentMetadata 
     *            
     * @param latestProcessedChange 
     *            
     * @param download 
     *            
     * @param versionDescriptor 
     *            
     * @return InputStream
     */
    public InputStream getItemContent(
        final UUID project, 
        final String repositoryId, 
        final String path, 
        final String scopePath, 
        final VersionControlRecursionType recursionLevel, 
        final Boolean includeContentMetadata, 
        final Boolean latestProcessedChange, 
        final Boolean download, 
        final GitVersionDescriptor versionDescriptor) {

        final UUID locationId = UUID.fromString("fb93c0db-47ed-4a31-8c20-47552878fb44"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("project", project); //$NON-NLS-1$
        routeValues.put("repositoryId", repositoryId); //$NON-NLS-1$

        final NameValueCollection queryParameters = new NameValueCollection();
        queryParameters.addIfNotEmpty("path", path); //$NON-NLS-1$
        queryParameters.addIfNotEmpty("scopePath", scopePath); //$NON-NLS-1$
        queryParameters.addIfNotNull("recursionLevel", recursionLevel); //$NON-NLS-1$
        queryParameters.addIfNotNull("includeContentMetadata", includeContentMetadata); //$NON-NLS-1$
        queryParameters.addIfNotNull("latestProcessedChange", latestProcessedChange); //$NON-NLS-1$
        queryParameters.addIfNotNull("download", download); //$NON-NLS-1$
        addModelAsQueryParams(queryParameters, versionDescriptor);

        final Object httpRequest = super.createRequest(HttpMethod.GET,
                                                       locationId,
                                                       routeValues,
                                                       apiVersion,
                                                       queryParameters,
                                                       APPLICATION_OCTET_STREAM_TYPE);

        return super.sendRequest(httpRequest, InputStream.class);
    }

    /** 
     * Get Item Metadata and/or Content for a single item. The download parameter is to indicate whether the content should be available as a download or just sent as a stream in the response. Doesn't apply to zipped content which is always returned as a download.
     * 
     * @param project 
     *            Project ID
     * @param repositoryId 
     *            
     * @param path 
     *            
     * @param scopePath 
     *            
     * @param recursionLevel 
     *            
     * @param includeContentMetadata 
     *            
     * @param latestProcessedChange 
     *            
     * @param download 
     *            
     * @param versionDescriptor 
     *            
     * @return InputStream
     */
    public InputStream getItemContent(
        final UUID project, 
        final UUID repositoryId, 
        final String path, 
        final String scopePath, 
        final VersionControlRecursionType recursionLevel, 
        final Boolean includeContentMetadata, 
        final Boolean latestProcessedChange, 
        final Boolean download, 
        final GitVersionDescriptor versionDescriptor) {

        final UUID locationId = UUID.fromString("fb93c0db-47ed-4a31-8c20-47552878fb44"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("project", project); //$NON-NLS-1$
        routeValues.put("repositoryId", repositoryId); //$NON-NLS-1$

        final NameValueCollection queryParameters = new NameValueCollection();
        queryParameters.addIfNotEmpty("path", path); //$NON-NLS-1$
        queryParameters.addIfNotEmpty("scopePath", scopePath); //$NON-NLS-1$
        queryParameters.addIfNotNull("recursionLevel", recursionLevel); //$NON-NLS-1$
        queryParameters.addIfNotNull("includeContentMetadata", includeContentMetadata); //$NON-NLS-1$
        queryParameters.addIfNotNull("latestProcessedChange", latestProcessedChange); //$NON-NLS-1$
        queryParameters.addIfNotNull("download", download); //$NON-NLS-1$
        addModelAsQueryParams(queryParameters, versionDescriptor);

        final Object httpRequest = super.createRequest(HttpMethod.GET,
                                                       locationId,
                                                       routeValues,
                                                       apiVersion,
                                                       queryParameters,
                                                       APPLICATION_OCTET_STREAM_TYPE);

        return super.sendRequest(httpRequest, InputStream.class);
    }

    /** 
     * Get Item Metadata and/or Content for a single item. The download parameter is to indicate whether the content should be available as a download or just sent as a stream in the response. Doesn't apply to zipped content which is always returned as a download.
     * 
     * @param repositoryId 
     *            
     * @param path 
     *            
     * @param scopePath 
     *            
     * @param recursionLevel 
     *            
     * @param includeContentMetadata 
     *            
     * @param latestProcessedChange 
     *            
     * @param download 
     *            
     * @param versionDescriptor 
     *            
     * @return InputStream
     */
    public InputStream getItemContent(
        final String repositoryId, 
        final String path, 
        final String scopePath, 
        final VersionControlRecursionType recursionLevel, 
        final Boolean includeContentMetadata, 
        final Boolean latestProcessedChange, 
        final Boolean download, 
        final GitVersionDescriptor versionDescriptor) {

        final UUID locationId = UUID.fromString("fb93c0db-47ed-4a31-8c20-47552878fb44"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("repositoryId", repositoryId); //$NON-NLS-1$

        final NameValueCollection queryParameters = new NameValueCollection();
        queryParameters.addIfNotEmpty("path", path); //$NON-NLS-1$
        queryParameters.addIfNotEmpty("scopePath", scopePath); //$NON-NLS-1$
        queryParameters.addIfNotNull("recursionLevel", recursionLevel); //$NON-NLS-1$
        queryParameters.addIfNotNull("includeContentMetadata", includeContentMetadata); //$NON-NLS-1$
        queryParameters.addIfNotNull("latestProcessedChange", latestProcessedChange); //$NON-NLS-1$
        queryParameters.addIfNotNull("download", download); //$NON-NLS-1$
        addModelAsQueryParams(queryParameters, versionDescriptor);

        final Object httpRequest = super.createRequest(HttpMethod.GET,
                                                       locationId,
                                                       routeValues,
                                                       apiVersion,
                                                       queryParameters,
                                                       APPLICATION_OCTET_STREAM_TYPE);

        return super.sendRequest(httpRequest, InputStream.class);
    }

    /** 
     * Get Item Metadata and/or Content for a single item. The download parameter is to indicate whether the content should be available as a download or just sent as a stream in the response. Doesn't apply to zipped content which is always returned as a download.
     * 
     * @param repositoryId 
     *            
     * @param path 
     *            
     * @param scopePath 
     *            
     * @param recursionLevel 
     *            
     * @param includeContentMetadata 
     *            
     * @param latestProcessedChange 
     *            
     * @param download 
     *            
     * @param versionDescriptor 
     *            
     * @return InputStream
     */
    public InputStream getItemContent(
        final UUID repositoryId, 
        final String path, 
        final String scopePath, 
        final VersionControlRecursionType recursionLevel, 
        final Boolean includeContentMetadata, 
        final Boolean latestProcessedChange, 
        final Boolean download, 
        final GitVersionDescriptor versionDescriptor) {

        final UUID locationId = UUID.fromString("fb93c0db-47ed-4a31-8c20-47552878fb44"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("repositoryId", repositoryId); //$NON-NLS-1$

        final NameValueCollection queryParameters = new NameValueCollection();
        queryParameters.addIfNotEmpty("path", path); //$NON-NLS-1$
        queryParameters.addIfNotEmpty("scopePath", scopePath); //$NON-NLS-1$
        queryParameters.addIfNotNull("recursionLevel", recursionLevel); //$NON-NLS-1$
        queryParameters.addIfNotNull("includeContentMetadata", includeContentMetadata); //$NON-NLS-1$
        queryParameters.addIfNotNull("latestProcessedChange", latestProcessedChange); //$NON-NLS-1$
        queryParameters.addIfNotNull("download", download); //$NON-NLS-1$
        addModelAsQueryParams(queryParameters, versionDescriptor);

        final Object httpRequest = super.createRequest(HttpMethod.GET,
                                                       locationId,
                                                       routeValues,
                                                       apiVersion,
                                                       queryParameters,
                                                       APPLICATION_OCTET_STREAM_TYPE);

        return super.sendRequest(httpRequest, InputStream.class);
    }

    /** 
     * Get Item Metadata and/or Content for a collection of items. The download parameter is to indicate whether the content should be available as a download or just sent as a stream in the response. Doesn't apply to zipped content which is always returned as a download.
     * 
     * @param project 
     *            Project ID or project name
     * @param repositoryId 
     *            
     * @param scopePath 
     *            
     * @param recursionLevel 
     *            
     * @param includeContentMetadata 
     *            
     * @param latestProcessedChange 
     *            
     * @param download 
     *            
     * @param includeLinks 
     *            
     * @param versionDescriptor 
     *            
     * @return List<GitItem>
     */
    public List<GitItem> getItems(
        final String project, 
        final String repositoryId, 
        final String scopePath, 
        final VersionControlRecursionType recursionLevel, 
        final Boolean includeContentMetadata, 
        final Boolean latestProcessedChange, 
        final Boolean download, 
        final Boolean includeLinks, 
        final GitVersionDescriptor versionDescriptor) {

        final UUID locationId = UUID.fromString("fb93c0db-47ed-4a31-8c20-47552878fb44"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("project", project); //$NON-NLS-1$
        routeValues.put("repositoryId", repositoryId); //$NON-NLS-1$

        final NameValueCollection queryParameters = new NameValueCollection();
        queryParameters.addIfNotEmpty("scopePath", scopePath); //$NON-NLS-1$
        queryParameters.addIfNotNull("recursionLevel", recursionLevel); //$NON-NLS-1$
        queryParameters.addIfNotNull("includeContentMetadata", includeContentMetadata); //$NON-NLS-1$
        queryParameters.addIfNotNull("latestProcessedChange", latestProcessedChange); //$NON-NLS-1$
        queryParameters.addIfNotNull("download", download); //$NON-NLS-1$
        queryParameters.addIfNotNull("includeLinks", includeLinks); //$NON-NLS-1$
        addModelAsQueryParams(queryParameters, versionDescriptor);

        final Object httpRequest = super.createRequest(HttpMethod.GET,
                                                       locationId,
                                                       routeValues,
                                                       apiVersion,
                                                       queryParameters,
                                                       APPLICATION_JSON_TYPE);

        return super.sendRequest(httpRequest, new TypeReference<List<GitItem>>() {});
    }

    /** 
     * Get Item Metadata and/or Content for a collection of items. The download parameter is to indicate whether the content should be available as a download or just sent as a stream in the response. Doesn't apply to zipped content which is always returned as a download.
     * 
     * @param project 
     *            Project ID or project name
     * @param repositoryId 
     *            
     * @param scopePath 
     *            
     * @param recursionLevel 
     *            
     * @param includeContentMetadata 
     *            
     * @param latestProcessedChange 
     *            
     * @param download 
     *            
     * @param includeLinks 
     *            
     * @param versionDescriptor 
     *            
     * @return List<GitItem>
     */
    public List<GitItem> getItems(
        final String project, 
        final UUID repositoryId, 
        final String scopePath, 
        final VersionControlRecursionType recursionLevel, 
        final Boolean includeContentMetadata, 
        final Boolean latestProcessedChange, 
        final Boolean download, 
        final Boolean includeLinks, 
        final GitVersionDescriptor versionDescriptor) {

        final UUID locationId = UUID.fromString("fb93c0db-47ed-4a31-8c20-47552878fb44"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("project", project); //$NON-NLS-1$
        routeValues.put("repositoryId", repositoryId); //$NON-NLS-1$

        final NameValueCollection queryParameters = new NameValueCollection();
        queryParameters.addIfNotEmpty("scopePath", scopePath); //$NON-NLS-1$
        queryParameters.addIfNotNull("recursionLevel", recursionLevel); //$NON-NLS-1$
        queryParameters.addIfNotNull("includeContentMetadata", includeContentMetadata); //$NON-NLS-1$
        queryParameters.addIfNotNull("latestProcessedChange", latestProcessedChange); //$NON-NLS-1$
        queryParameters.addIfNotNull("download", download); //$NON-NLS-1$
        queryParameters.addIfNotNull("includeLinks", includeLinks); //$NON-NLS-1$
        addModelAsQueryParams(queryParameters, versionDescriptor);

        final Object httpRequest = super.createRequest(HttpMethod.GET,
                                                       locationId,
                                                       routeValues,
                                                       apiVersion,
                                                       queryParameters,
                                                       APPLICATION_JSON_TYPE);

        return super.sendRequest(httpRequest, new TypeReference<List<GitItem>>() {});
    }

    /** 
     * Get Item Metadata and/or Content for a collection of items. The download parameter is to indicate whether the content should be available as a download or just sent as a stream in the response. Doesn't apply to zipped content which is always returned as a download.
     * 
     * @param project 
     *            Project ID
     * @param repositoryId 
     *            
     * @param scopePath 
     *            
     * @param recursionLevel 
     *            
     * @param includeContentMetadata 
     *            
     * @param latestProcessedChange 
     *            
     * @param download 
     *            
     * @param includeLinks 
     *            
     * @param versionDescriptor 
     *            
     * @return List<GitItem>
     */
    public List<GitItem> getItems(
        final UUID project, 
        final String repositoryId, 
        final String scopePath, 
        final VersionControlRecursionType recursionLevel, 
        final Boolean includeContentMetadata, 
        final Boolean latestProcessedChange, 
        final Boolean download, 
        final Boolean includeLinks, 
        final GitVersionDescriptor versionDescriptor) {

        final UUID locationId = UUID.fromString("fb93c0db-47ed-4a31-8c20-47552878fb44"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("project", project); //$NON-NLS-1$
        routeValues.put("repositoryId", repositoryId); //$NON-NLS-1$

        final NameValueCollection queryParameters = new NameValueCollection();
        queryParameters.addIfNotEmpty("scopePath", scopePath); //$NON-NLS-1$
        queryParameters.addIfNotNull("recursionLevel", recursionLevel); //$NON-NLS-1$
        queryParameters.addIfNotNull("includeContentMetadata", includeContentMetadata); //$NON-NLS-1$
        queryParameters.addIfNotNull("latestProcessedChange", latestProcessedChange); //$NON-NLS-1$
        queryParameters.addIfNotNull("download", download); //$NON-NLS-1$
        queryParameters.addIfNotNull("includeLinks", includeLinks); //$NON-NLS-1$
        addModelAsQueryParams(queryParameters, versionDescriptor);

        final Object httpRequest = super.createRequest(HttpMethod.GET,
                                                       locationId,
                                                       routeValues,
                                                       apiVersion,
                                                       queryParameters,
                                                       APPLICATION_JSON_TYPE);

        return super.sendRequest(httpRequest, new TypeReference<List<GitItem>>() {});
    }

    /** 
     * Get Item Metadata and/or Content for a collection of items. The download parameter is to indicate whether the content should be available as a download or just sent as a stream in the response. Doesn't apply to zipped content which is always returned as a download.
     * 
     * @param project 
     *            Project ID
     * @param repositoryId 
     *            
     * @param scopePath 
     *            
     * @param recursionLevel 
     *            
     * @param includeContentMetadata 
     *            
     * @param latestProcessedChange 
     *            
     * @param download 
     *            
     * @param includeLinks 
     *            
     * @param versionDescriptor 
     *            
     * @return List<GitItem>
     */
    public List<GitItem> getItems(
        final UUID project, 
        final UUID repositoryId, 
        final String scopePath, 
        final VersionControlRecursionType recursionLevel, 
        final Boolean includeContentMetadata, 
        final Boolean latestProcessedChange, 
        final Boolean download, 
        final Boolean includeLinks, 
        final GitVersionDescriptor versionDescriptor) {

        final UUID locationId = UUID.fromString("fb93c0db-47ed-4a31-8c20-47552878fb44"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("project", project); //$NON-NLS-1$
        routeValues.put("repositoryId", repositoryId); //$NON-NLS-1$

        final NameValueCollection queryParameters = new NameValueCollection();
        queryParameters.addIfNotEmpty("scopePath", scopePath); //$NON-NLS-1$
        queryParameters.addIfNotNull("recursionLevel", recursionLevel); //$NON-NLS-1$
        queryParameters.addIfNotNull("includeContentMetadata", includeContentMetadata); //$NON-NLS-1$
        queryParameters.addIfNotNull("latestProcessedChange", latestProcessedChange); //$NON-NLS-1$
        queryParameters.addIfNotNull("download", download); //$NON-NLS-1$
        queryParameters.addIfNotNull("includeLinks", includeLinks); //$NON-NLS-1$
        addModelAsQueryParams(queryParameters, versionDescriptor);

        final Object httpRequest = super.createRequest(HttpMethod.GET,
                                                       locationId,
                                                       routeValues,
                                                       apiVersion,
                                                       queryParameters,
                                                       APPLICATION_JSON_TYPE);

        return super.sendRequest(httpRequest, new TypeReference<List<GitItem>>() {});
    }

    /** 
     * Get Item Metadata and/or Content for a collection of items. The download parameter is to indicate whether the content should be available as a download or just sent as a stream in the response. Doesn't apply to zipped content which is always returned as a download.
     * 
     * @param repositoryId 
     *            
     * @param scopePath 
     *            
     * @param recursionLevel 
     *            
     * @param includeContentMetadata 
     *            
     * @param latestProcessedChange 
     *            
     * @param download 
     *            
     * @param includeLinks 
     *            
     * @param versionDescriptor 
     *            
     * @return List<GitItem>
     */
    public List<GitItem> getItems(
        final String repositoryId, 
        final String scopePath, 
        final VersionControlRecursionType recursionLevel, 
        final Boolean includeContentMetadata, 
        final Boolean latestProcessedChange, 
        final Boolean download, 
        final Boolean includeLinks, 
        final GitVersionDescriptor versionDescriptor) {

        final UUID locationId = UUID.fromString("fb93c0db-47ed-4a31-8c20-47552878fb44"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("repositoryId", repositoryId); //$NON-NLS-1$

        final NameValueCollection queryParameters = new NameValueCollection();
        queryParameters.addIfNotEmpty("scopePath", scopePath); //$NON-NLS-1$
        queryParameters.addIfNotNull("recursionLevel", recursionLevel); //$NON-NLS-1$
        queryParameters.addIfNotNull("includeContentMetadata", includeContentMetadata); //$NON-NLS-1$
        queryParameters.addIfNotNull("latestProcessedChange", latestProcessedChange); //$NON-NLS-1$
        queryParameters.addIfNotNull("download", download); //$NON-NLS-1$
        queryParameters.addIfNotNull("includeLinks", includeLinks); //$NON-NLS-1$
        addModelAsQueryParams(queryParameters, versionDescriptor);

        final Object httpRequest = super.createRequest(HttpMethod.GET,
                                                       locationId,
                                                       routeValues,
                                                       apiVersion,
                                                       queryParameters,
                                                       APPLICATION_JSON_TYPE);

        return super.sendRequest(httpRequest, new TypeReference<List<GitItem>>() {});
    }

    /** 
     * Get Item Metadata and/or Content for a collection of items. The download parameter is to indicate whether the content should be available as a download or just sent as a stream in the response. Doesn't apply to zipped content which is always returned as a download.
     * 
     * @param repositoryId 
     *            
     * @param scopePath 
     *            
     * @param recursionLevel 
     *            
     * @param includeContentMetadata 
     *            
     * @param latestProcessedChange 
     *            
     * @param download 
     *            
     * @param includeLinks 
     *            
     * @param versionDescriptor 
     *            
     * @return List<GitItem>
     */
    public List<GitItem> getItems(
        final UUID repositoryId, 
        final String scopePath, 
        final VersionControlRecursionType recursionLevel, 
        final Boolean includeContentMetadata, 
        final Boolean latestProcessedChange, 
        final Boolean download, 
        final Boolean includeLinks, 
        final GitVersionDescriptor versionDescriptor) {

        final UUID locationId = UUID.fromString("fb93c0db-47ed-4a31-8c20-47552878fb44"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("repositoryId", repositoryId); //$NON-NLS-1$

        final NameValueCollection queryParameters = new NameValueCollection();
        queryParameters.addIfNotEmpty("scopePath", scopePath); //$NON-NLS-1$
        queryParameters.addIfNotNull("recursionLevel", recursionLevel); //$NON-NLS-1$
        queryParameters.addIfNotNull("includeContentMetadata", includeContentMetadata); //$NON-NLS-1$
        queryParameters.addIfNotNull("latestProcessedChange", latestProcessedChange); //$NON-NLS-1$
        queryParameters.addIfNotNull("download", download); //$NON-NLS-1$
        queryParameters.addIfNotNull("includeLinks", includeLinks); //$NON-NLS-1$
        addModelAsQueryParams(queryParameters, versionDescriptor);

        final Object httpRequest = super.createRequest(HttpMethod.GET,
                                                       locationId,
                                                       routeValues,
                                                       apiVersion,
                                                       queryParameters,
                                                       APPLICATION_JSON_TYPE);

        return super.sendRequest(httpRequest, new TypeReference<List<GitItem>>() {});
    }

    /** 
     * Get Item Metadata and/or Content for a single item. The download parameter is to indicate whether the content should be available as a download or just sent as a stream in the response. Doesn't apply to zipped content which is always returned as a download.
     * 
     * @param project 
     *            Project ID or project name
     * @param repositoryId 
     *            
     * @param path 
     *            
     * @param scopePath 
     *            
     * @param recursionLevel 
     *            
     * @param includeContentMetadata 
     *            
     * @param latestProcessedChange 
     *            
     * @param download 
     *            
     * @param versionDescriptor 
     *            
     * @return InputStream
     */
    public InputStream getItemText(
        final String project, 
        final String repositoryId, 
        final String path, 
        final String scopePath, 
        final VersionControlRecursionType recursionLevel, 
        final Boolean includeContentMetadata, 
        final Boolean latestProcessedChange, 
        final Boolean download, 
        final GitVersionDescriptor versionDescriptor) {

        final UUID locationId = UUID.fromString("fb93c0db-47ed-4a31-8c20-47552878fb44"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("project", project); //$NON-NLS-1$
        routeValues.put("repositoryId", repositoryId); //$NON-NLS-1$

        final NameValueCollection queryParameters = new NameValueCollection();
        queryParameters.addIfNotEmpty("path", path); //$NON-NLS-1$
        queryParameters.addIfNotEmpty("scopePath", scopePath); //$NON-NLS-1$
        queryParameters.addIfNotNull("recursionLevel", recursionLevel); //$NON-NLS-1$
        queryParameters.addIfNotNull("includeContentMetadata", includeContentMetadata); //$NON-NLS-1$
        queryParameters.addIfNotNull("latestProcessedChange", latestProcessedChange); //$NON-NLS-1$
        queryParameters.addIfNotNull("download", download); //$NON-NLS-1$
        addModelAsQueryParams(queryParameters, versionDescriptor);

        final Object httpRequest = super.createRequest(HttpMethod.GET,
                                                       locationId,
                                                       routeValues,
                                                       apiVersion,
                                                       queryParameters,
                                                       TEXT_PLAIN_TYPE);

        return super.sendRequest(httpRequest, InputStream.class);
    }

    /** 
     * Get Item Metadata and/or Content for a single item. The download parameter is to indicate whether the content should be available as a download or just sent as a stream in the response. Doesn't apply to zipped content which is always returned as a download.
     * 
     * @param project 
     *            Project ID or project name
     * @param repositoryId 
     *            
     * @param path 
     *            
     * @param scopePath 
     *            
     * @param recursionLevel 
     *            
     * @param includeContentMetadata 
     *            
     * @param latestProcessedChange 
     *            
     * @param download 
     *            
     * @param versionDescriptor 
     *            
     * @return InputStream
     */
    public InputStream getItemText(
        final String project, 
        final UUID repositoryId, 
        final String path, 
        final String scopePath, 
        final VersionControlRecursionType recursionLevel, 
        final Boolean includeContentMetadata, 
        final Boolean latestProcessedChange, 
        final Boolean download, 
        final GitVersionDescriptor versionDescriptor) {

        final UUID locationId = UUID.fromString("fb93c0db-47ed-4a31-8c20-47552878fb44"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("project", project); //$NON-NLS-1$
        routeValues.put("repositoryId", repositoryId); //$NON-NLS-1$

        final NameValueCollection queryParameters = new NameValueCollection();
        queryParameters.addIfNotEmpty("path", path); //$NON-NLS-1$
        queryParameters.addIfNotEmpty("scopePath", scopePath); //$NON-NLS-1$
        queryParameters.addIfNotNull("recursionLevel", recursionLevel); //$NON-NLS-1$
        queryParameters.addIfNotNull("includeContentMetadata", includeContentMetadata); //$NON-NLS-1$
        queryParameters.addIfNotNull("latestProcessedChange", latestProcessedChange); //$NON-NLS-1$
        queryParameters.addIfNotNull("download", download); //$NON-NLS-1$
        addModelAsQueryParams(queryParameters, versionDescriptor);

        final Object httpRequest = super.createRequest(HttpMethod.GET,
                                                       locationId,
                                                       routeValues,
                                                       apiVersion,
                                                       queryParameters,
                                                       TEXT_PLAIN_TYPE);

        return super.sendRequest(httpRequest, InputStream.class);
    }

    /** 
     * Get Item Metadata and/or Content for a single item. The download parameter is to indicate whether the content should be available as a download or just sent as a stream in the response. Doesn't apply to zipped content which is always returned as a download.
     * 
     * @param project 
     *            Project ID
     * @param repositoryId 
     *            
     * @param path 
     *            
     * @param scopePath 
     *            
     * @param recursionLevel 
     *            
     * @param includeContentMetadata 
     *            
     * @param latestProcessedChange 
     *            
     * @param download 
     *            
     * @param versionDescriptor 
     *            
     * @return InputStream
     */
    public InputStream getItemText(
        final UUID project, 
        final String repositoryId, 
        final String path, 
        final String scopePath, 
        final VersionControlRecursionType recursionLevel, 
        final Boolean includeContentMetadata, 
        final Boolean latestProcessedChange, 
        final Boolean download, 
        final GitVersionDescriptor versionDescriptor) {

        final UUID locationId = UUID.fromString("fb93c0db-47ed-4a31-8c20-47552878fb44"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("project", project); //$NON-NLS-1$
        routeValues.put("repositoryId", repositoryId); //$NON-NLS-1$

        final NameValueCollection queryParameters = new NameValueCollection();
        queryParameters.addIfNotEmpty("path", path); //$NON-NLS-1$
        queryParameters.addIfNotEmpty("scopePath", scopePath); //$NON-NLS-1$
        queryParameters.addIfNotNull("recursionLevel", recursionLevel); //$NON-NLS-1$
        queryParameters.addIfNotNull("includeContentMetadata", includeContentMetadata); //$NON-NLS-1$
        queryParameters.addIfNotNull("latestProcessedChange", latestProcessedChange); //$NON-NLS-1$
        queryParameters.addIfNotNull("download", download); //$NON-NLS-1$
        addModelAsQueryParams(queryParameters, versionDescriptor);

        final Object httpRequest = super.createRequest(HttpMethod.GET,
                                                       locationId,
                                                       routeValues,
                                                       apiVersion,
                                                       queryParameters,
                                                       TEXT_PLAIN_TYPE);

        return super.sendRequest(httpRequest, InputStream.class);
    }

    /** 
     * Get Item Metadata and/or Content for a single item. The download parameter is to indicate whether the content should be available as a download or just sent as a stream in the response. Doesn't apply to zipped content which is always returned as a download.
     * 
     * @param project 
     *            Project ID
     * @param repositoryId 
     *            
     * @param path 
     *            
     * @param scopePath 
     *            
     * @param recursionLevel 
     *            
     * @param includeContentMetadata 
     *            
     * @param latestProcessedChange 
     *            
     * @param download 
     *            
     * @param versionDescriptor 
     *            
     * @return InputStream
     */
    public InputStream getItemText(
        final UUID project, 
        final UUID repositoryId, 
        final String path, 
        final String scopePath, 
        final VersionControlRecursionType recursionLevel, 
        final Boolean includeContentMetadata, 
        final Boolean latestProcessedChange, 
        final Boolean download, 
        final GitVersionDescriptor versionDescriptor) {

        final UUID locationId = UUID.fromString("fb93c0db-47ed-4a31-8c20-47552878fb44"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("project", project); //$NON-NLS-1$
        routeValues.put("repositoryId", repositoryId); //$NON-NLS-1$

        final NameValueCollection queryParameters = new NameValueCollection();
        queryParameters.addIfNotEmpty("path", path); //$NON-NLS-1$
        queryParameters.addIfNotEmpty("scopePath", scopePath); //$NON-NLS-1$
        queryParameters.addIfNotNull("recursionLevel", recursionLevel); //$NON-NLS-1$
        queryParameters.addIfNotNull("includeContentMetadata", includeContentMetadata); //$NON-NLS-1$
        queryParameters.addIfNotNull("latestProcessedChange", latestProcessedChange); //$NON-NLS-1$
        queryParameters.addIfNotNull("download", download); //$NON-NLS-1$
        addModelAsQueryParams(queryParameters, versionDescriptor);

        final Object httpRequest = super.createRequest(HttpMethod.GET,
                                                       locationId,
                                                       routeValues,
                                                       apiVersion,
                                                       queryParameters,
                                                       TEXT_PLAIN_TYPE);

        return super.sendRequest(httpRequest, InputStream.class);
    }

    /** 
     * Get Item Metadata and/or Content for a single item. The download parameter is to indicate whether the content should be available as a download or just sent as a stream in the response. Doesn't apply to zipped content which is always returned as a download.
     * 
     * @param repositoryId 
     *            
     * @param path 
     *            
     * @param scopePath 
     *            
     * @param recursionLevel 
     *            
     * @param includeContentMetadata 
     *            
     * @param latestProcessedChange 
     *            
     * @param download 
     *            
     * @param versionDescriptor 
     *            
     * @return InputStream
     */
    public InputStream getItemText(
        final String repositoryId, 
        final String path, 
        final String scopePath, 
        final VersionControlRecursionType recursionLevel, 
        final Boolean includeContentMetadata, 
        final Boolean latestProcessedChange, 
        final Boolean download, 
        final GitVersionDescriptor versionDescriptor) {

        final UUID locationId = UUID.fromString("fb93c0db-47ed-4a31-8c20-47552878fb44"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("repositoryId", repositoryId); //$NON-NLS-1$

        final NameValueCollection queryParameters = new NameValueCollection();
        queryParameters.addIfNotEmpty("path", path); //$NON-NLS-1$
        queryParameters.addIfNotEmpty("scopePath", scopePath); //$NON-NLS-1$
        queryParameters.addIfNotNull("recursionLevel", recursionLevel); //$NON-NLS-1$
        queryParameters.addIfNotNull("includeContentMetadata", includeContentMetadata); //$NON-NLS-1$
        queryParameters.addIfNotNull("latestProcessedChange", latestProcessedChange); //$NON-NLS-1$
        queryParameters.addIfNotNull("download", download); //$NON-NLS-1$
        addModelAsQueryParams(queryParameters, versionDescriptor);

        final Object httpRequest = super.createRequest(HttpMethod.GET,
                                                       locationId,
                                                       routeValues,
                                                       apiVersion,
                                                       queryParameters,
                                                       TEXT_PLAIN_TYPE);

        return super.sendRequest(httpRequest, InputStream.class);
    }

    /** 
     * Get Item Metadata and/or Content for a single item. The download parameter is to indicate whether the content should be available as a download or just sent as a stream in the response. Doesn't apply to zipped content which is always returned as a download.
     * 
     * @param repositoryId 
     *            
     * @param path 
     *            
     * @param scopePath 
     *            
     * @param recursionLevel 
     *            
     * @param includeContentMetadata 
     *            
     * @param latestProcessedChange 
     *            
     * @param download 
     *            
     * @param versionDescriptor 
     *            
     * @return InputStream
     */
    public InputStream getItemText(
        final UUID repositoryId, 
        final String path, 
        final String scopePath, 
        final VersionControlRecursionType recursionLevel, 
        final Boolean includeContentMetadata, 
        final Boolean latestProcessedChange, 
        final Boolean download, 
        final GitVersionDescriptor versionDescriptor) {

        final UUID locationId = UUID.fromString("fb93c0db-47ed-4a31-8c20-47552878fb44"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("repositoryId", repositoryId); //$NON-NLS-1$

        final NameValueCollection queryParameters = new NameValueCollection();
        queryParameters.addIfNotEmpty("path", path); //$NON-NLS-1$
        queryParameters.addIfNotEmpty("scopePath", scopePath); //$NON-NLS-1$
        queryParameters.addIfNotNull("recursionLevel", recursionLevel); //$NON-NLS-1$
        queryParameters.addIfNotNull("includeContentMetadata", includeContentMetadata); //$NON-NLS-1$
        queryParameters.addIfNotNull("latestProcessedChange", latestProcessedChange); //$NON-NLS-1$
        queryParameters.addIfNotNull("download", download); //$NON-NLS-1$
        addModelAsQueryParams(queryParameters, versionDescriptor);

        final Object httpRequest = super.createRequest(HttpMethod.GET,
                                                       locationId,
                                                       routeValues,
                                                       apiVersion,
                                                       queryParameters,
                                                       TEXT_PLAIN_TYPE);

        return super.sendRequest(httpRequest, InputStream.class);
    }

    /** 
     * Get Item Metadata and/or Content for a single item. The download parameter is to indicate whether the content should be available as a download or just sent as a stream in the response. Doesn't apply to zipped content which is always returned as a download.
     * 
     * @param project 
     *            Project ID or project name
     * @param repositoryId 
     *            
     * @param path 
     *            
     * @param scopePath 
     *            
     * @param recursionLevel 
     *            
     * @param includeContentMetadata 
     *            
     * @param latestProcessedChange 
     *            
     * @param download 
     *            
     * @param versionDescriptor 
     *            
     * @return InputStream
     */
    public InputStream getItemZip(
        final String project, 
        final String repositoryId, 
        final String path, 
        final String scopePath, 
        final VersionControlRecursionType recursionLevel, 
        final Boolean includeContentMetadata, 
        final Boolean latestProcessedChange, 
        final Boolean download, 
        final GitVersionDescriptor versionDescriptor) {

        final UUID locationId = UUID.fromString("fb93c0db-47ed-4a31-8c20-47552878fb44"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("project", project); //$NON-NLS-1$
        routeValues.put("repositoryId", repositoryId); //$NON-NLS-1$

        final NameValueCollection queryParameters = new NameValueCollection();
        queryParameters.addIfNotEmpty("path", path); //$NON-NLS-1$
        queryParameters.addIfNotEmpty("scopePath", scopePath); //$NON-NLS-1$
        queryParameters.addIfNotNull("recursionLevel", recursionLevel); //$NON-NLS-1$
        queryParameters.addIfNotNull("includeContentMetadata", includeContentMetadata); //$NON-NLS-1$
        queryParameters.addIfNotNull("latestProcessedChange", latestProcessedChange); //$NON-NLS-1$
        queryParameters.addIfNotNull("download", download); //$NON-NLS-1$
        addModelAsQueryParams(queryParameters, versionDescriptor);

        final Object httpRequest = super.createRequest(HttpMethod.GET,
                                                       locationId,
                                                       routeValues,
                                                       apiVersion,
                                                       queryParameters,
                                                       APPLICATION_ZIP_TYPE);

        return super.sendRequest(httpRequest, InputStream.class);
    }

    /** 
     * Get Item Metadata and/or Content for a single item. The download parameter is to indicate whether the content should be available as a download or just sent as a stream in the response. Doesn't apply to zipped content which is always returned as a download.
     * 
     * @param project 
     *            Project ID or project name
     * @param repositoryId 
     *            
     * @param path 
     *            
     * @param scopePath 
     *            
     * @param recursionLevel 
     *            
     * @param includeContentMetadata 
     *            
     * @param latestProcessedChange 
     *            
     * @param download 
     *            
     * @param versionDescriptor 
     *            
     * @return InputStream
     */
    public InputStream getItemZip(
        final String project, 
        final UUID repositoryId, 
        final String path, 
        final String scopePath, 
        final VersionControlRecursionType recursionLevel, 
        final Boolean includeContentMetadata, 
        final Boolean latestProcessedChange, 
        final Boolean download, 
        final GitVersionDescriptor versionDescriptor) {

        final UUID locationId = UUID.fromString("fb93c0db-47ed-4a31-8c20-47552878fb44"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("project", project); //$NON-NLS-1$
        routeValues.put("repositoryId", repositoryId); //$NON-NLS-1$

        final NameValueCollection queryParameters = new NameValueCollection();
        queryParameters.addIfNotEmpty("path", path); //$NON-NLS-1$
        queryParameters.addIfNotEmpty("scopePath", scopePath); //$NON-NLS-1$
        queryParameters.addIfNotNull("recursionLevel", recursionLevel); //$NON-NLS-1$
        queryParameters.addIfNotNull("includeContentMetadata", includeContentMetadata); //$NON-NLS-1$
        queryParameters.addIfNotNull("latestProcessedChange", latestProcessedChange); //$NON-NLS-1$
        queryParameters.addIfNotNull("download", download); //$NON-NLS-1$
        addModelAsQueryParams(queryParameters, versionDescriptor);

        final Object httpRequest = super.createRequest(HttpMethod.GET,
                                                       locationId,
                                                       routeValues,
                                                       apiVersion,
                                                       queryParameters,
                                                       APPLICATION_ZIP_TYPE);

        return super.sendRequest(httpRequest, InputStream.class);
    }

    /** 
     * Get Item Metadata and/or Content for a single item. The download parameter is to indicate whether the content should be available as a download or just sent as a stream in the response. Doesn't apply to zipped content which is always returned as a download.
     * 
     * @param project 
     *            Project ID
     * @param repositoryId 
     *            
     * @param path 
     *            
     * @param scopePath 
     *            
     * @param recursionLevel 
     *            
     * @param includeContentMetadata 
     *            
     * @param latestProcessedChange 
     *            
     * @param download 
     *            
     * @param versionDescriptor 
     *            
     * @return InputStream
     */
    public InputStream getItemZip(
        final UUID project, 
        final String repositoryId, 
        final String path, 
        final String scopePath, 
        final VersionControlRecursionType recursionLevel, 
        final Boolean includeContentMetadata, 
        final Boolean latestProcessedChange, 
        final Boolean download, 
        final GitVersionDescriptor versionDescriptor) {

        final UUID locationId = UUID.fromString("fb93c0db-47ed-4a31-8c20-47552878fb44"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("project", project); //$NON-NLS-1$
        routeValues.put("repositoryId", repositoryId); //$NON-NLS-1$

        final NameValueCollection queryParameters = new NameValueCollection();
        queryParameters.addIfNotEmpty("path", path); //$NON-NLS-1$
        queryParameters.addIfNotEmpty("scopePath", scopePath); //$NON-NLS-1$
        queryParameters.addIfNotNull("recursionLevel", recursionLevel); //$NON-NLS-1$
        queryParameters.addIfNotNull("includeContentMetadata", includeContentMetadata); //$NON-NLS-1$
        queryParameters.addIfNotNull("latestProcessedChange", latestProcessedChange); //$NON-NLS-1$
        queryParameters.addIfNotNull("download", download); //$NON-NLS-1$
        addModelAsQueryParams(queryParameters, versionDescriptor);

        final Object httpRequest = super.createRequest(HttpMethod.GET,
                                                       locationId,
                                                       routeValues,
                                                       apiVersion,
                                                       queryParameters,
                                                       APPLICATION_ZIP_TYPE);

        return super.sendRequest(httpRequest, InputStream.class);
    }

    /** 
     * Get Item Metadata and/or Content for a single item. The download parameter is to indicate whether the content should be available as a download or just sent as a stream in the response. Doesn't apply to zipped content which is always returned as a download.
     * 
     * @param project 
     *            Project ID
     * @param repositoryId 
     *            
     * @param path 
     *            
     * @param scopePath 
     *            
     * @param recursionLevel 
     *            
     * @param includeContentMetadata 
     *            
     * @param latestProcessedChange 
     *            
     * @param download 
     *            
     * @param versionDescriptor 
     *            
     * @return InputStream
     */
    public InputStream getItemZip(
        final UUID project, 
        final UUID repositoryId, 
        final String path, 
        final String scopePath, 
        final VersionControlRecursionType recursionLevel, 
        final Boolean includeContentMetadata, 
        final Boolean latestProcessedChange, 
        final Boolean download, 
        final GitVersionDescriptor versionDescriptor) {

        final UUID locationId = UUID.fromString("fb93c0db-47ed-4a31-8c20-47552878fb44"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("project", project); //$NON-NLS-1$
        routeValues.put("repositoryId", repositoryId); //$NON-NLS-1$

        final NameValueCollection queryParameters = new NameValueCollection();
        queryParameters.addIfNotEmpty("path", path); //$NON-NLS-1$
        queryParameters.addIfNotEmpty("scopePath", scopePath); //$NON-NLS-1$
        queryParameters.addIfNotNull("recursionLevel", recursionLevel); //$NON-NLS-1$
        queryParameters.addIfNotNull("includeContentMetadata", includeContentMetadata); //$NON-NLS-1$
        queryParameters.addIfNotNull("latestProcessedChange", latestProcessedChange); //$NON-NLS-1$
        queryParameters.addIfNotNull("download", download); //$NON-NLS-1$
        addModelAsQueryParams(queryParameters, versionDescriptor);

        final Object httpRequest = super.createRequest(HttpMethod.GET,
                                                       locationId,
                                                       routeValues,
                                                       apiVersion,
                                                       queryParameters,
                                                       APPLICATION_ZIP_TYPE);

        return super.sendRequest(httpRequest, InputStream.class);
    }

    /** 
     * Get Item Metadata and/or Content for a single item. The download parameter is to indicate whether the content should be available as a download or just sent as a stream in the response. Doesn't apply to zipped content which is always returned as a download.
     * 
     * @param repositoryId 
     *            
     * @param path 
     *            
     * @param scopePath 
     *            
     * @param recursionLevel 
     *            
     * @param includeContentMetadata 
     *            
     * @param latestProcessedChange 
     *            
     * @param download 
     *            
     * @param versionDescriptor 
     *            
     * @return InputStream
     */
    public InputStream getItemZip(
        final String repositoryId, 
        final String path, 
        final String scopePath, 
        final VersionControlRecursionType recursionLevel, 
        final Boolean includeContentMetadata, 
        final Boolean latestProcessedChange, 
        final Boolean download, 
        final GitVersionDescriptor versionDescriptor) {

        final UUID locationId = UUID.fromString("fb93c0db-47ed-4a31-8c20-47552878fb44"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("repositoryId", repositoryId); //$NON-NLS-1$

        final NameValueCollection queryParameters = new NameValueCollection();
        queryParameters.addIfNotEmpty("path", path); //$NON-NLS-1$
        queryParameters.addIfNotEmpty("scopePath", scopePath); //$NON-NLS-1$
        queryParameters.addIfNotNull("recursionLevel", recursionLevel); //$NON-NLS-1$
        queryParameters.addIfNotNull("includeContentMetadata", includeContentMetadata); //$NON-NLS-1$
        queryParameters.addIfNotNull("latestProcessedChange", latestProcessedChange); //$NON-NLS-1$
        queryParameters.addIfNotNull("download", download); //$NON-NLS-1$
        addModelAsQueryParams(queryParameters, versionDescriptor);

        final Object httpRequest = super.createRequest(HttpMethod.GET,
                                                       locationId,
                                                       routeValues,
                                                       apiVersion,
                                                       queryParameters,
                                                       APPLICATION_ZIP_TYPE);

        return super.sendRequest(httpRequest, InputStream.class);
    }

    /** 
     * Get Item Metadata and/or Content for a single item. The download parameter is to indicate whether the content should be available as a download or just sent as a stream in the response. Doesn't apply to zipped content which is always returned as a download.
     * 
     * @param repositoryId 
     *            
     * @param path 
     *            
     * @param scopePath 
     *            
     * @param recursionLevel 
     *            
     * @param includeContentMetadata 
     *            
     * @param latestProcessedChange 
     *            
     * @param download 
     *            
     * @param versionDescriptor 
     *            
     * @return InputStream
     */
    public InputStream getItemZip(
        final UUID repositoryId, 
        final String path, 
        final String scopePath, 
        final VersionControlRecursionType recursionLevel, 
        final Boolean includeContentMetadata, 
        final Boolean latestProcessedChange, 
        final Boolean download, 
        final GitVersionDescriptor versionDescriptor) {

        final UUID locationId = UUID.fromString("fb93c0db-47ed-4a31-8c20-47552878fb44"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("repositoryId", repositoryId); //$NON-NLS-1$

        final NameValueCollection queryParameters = new NameValueCollection();
        queryParameters.addIfNotEmpty("path", path); //$NON-NLS-1$
        queryParameters.addIfNotEmpty("scopePath", scopePath); //$NON-NLS-1$
        queryParameters.addIfNotNull("recursionLevel", recursionLevel); //$NON-NLS-1$
        queryParameters.addIfNotNull("includeContentMetadata", includeContentMetadata); //$NON-NLS-1$
        queryParameters.addIfNotNull("latestProcessedChange", latestProcessedChange); //$NON-NLS-1$
        queryParameters.addIfNotNull("download", download); //$NON-NLS-1$
        addModelAsQueryParams(queryParameters, versionDescriptor);

        final Object httpRequest = super.createRequest(HttpMethod.GET,
                                                       locationId,
                                                       routeValues,
                                                       apiVersion,
                                                       queryParameters,
                                                       APPLICATION_ZIP_TYPE);

        return super.sendRequest(httpRequest, InputStream.class);
    }

    /** 
     * Post for retrieving a creating a batch out of a set of items in a repo / project given a list of paths or a long path
     * 
     * @param requestData 
     *            
     * @param repositoryId 
     *            
     * @return List<List<GitItem>>
     */
    public List<List<GitItem>> getItemsBatch(
        final GitItemRequestData requestData, 
        final String repositoryId) {

        final UUID locationId = UUID.fromString("630fd2e4-fb88-4f85-ad21-13f3fd1fbca9"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("repositoryId", repositoryId); //$NON-NLS-1$

        final Object httpRequest = super.createRequest(HttpMethod.POST,
                                                       locationId,
                                                       routeValues,
                                                       apiVersion,
                                                       requestData,
                                                       APPLICATION_JSON_TYPE,
                                                       APPLICATION_JSON_TYPE);

        return super.sendRequest(httpRequest, new TypeReference<List<List<GitItem>>>() {});
    }

    /** 
     * Post for retrieving a creating a batch out of a set of items in a repo / project given a list of paths or a long path
     * 
     * @param requestData 
     *            
     * @param repositoryId 
     *            
     * @return List<List<GitItem>>
     */
    public List<List<GitItem>> getItemsBatch(
        final GitItemRequestData requestData, 
        final UUID repositoryId) {

        final UUID locationId = UUID.fromString("630fd2e4-fb88-4f85-ad21-13f3fd1fbca9"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("repositoryId", repositoryId); //$NON-NLS-1$

        final Object httpRequest = super.createRequest(HttpMethod.POST,
                                                       locationId,
                                                       routeValues,
                                                       apiVersion,
                                                       requestData,
                                                       APPLICATION_JSON_TYPE,
                                                       APPLICATION_JSON_TYPE);

        return super.sendRequest(httpRequest, new TypeReference<List<List<GitItem>>>() {});
    }

    /** 
     * Post for retrieving a creating a batch out of a set of items in a repo / project given a list of paths or a long path
     * 
     * @param requestData 
     *            
     * @param project 
     *            Project ID or project name
     * @param repositoryId 
     *            
     * @return List<List<GitItem>>
     */
    public List<List<GitItem>> getItemsBatch(
        final GitItemRequestData requestData, 
        final String project, 
        final String repositoryId) {

        final UUID locationId = UUID.fromString("630fd2e4-fb88-4f85-ad21-13f3fd1fbca9"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("project", project); //$NON-NLS-1$
        routeValues.put("repositoryId", repositoryId); //$NON-NLS-1$

        final Object httpRequest = super.createRequest(HttpMethod.POST,
                                                       locationId,
                                                       routeValues,
                                                       apiVersion,
                                                       requestData,
                                                       APPLICATION_JSON_TYPE,
                                                       APPLICATION_JSON_TYPE);

        return super.sendRequest(httpRequest, new TypeReference<List<List<GitItem>>>() {});
    }

    /** 
     * Post for retrieving a creating a batch out of a set of items in a repo / project given a list of paths or a long path
     * 
     * @param requestData 
     *            
     * @param project 
     *            Project ID or project name
     * @param repositoryId 
     *            
     * @return List<List<GitItem>>
     */
    public List<List<GitItem>> getItemsBatch(
        final GitItemRequestData requestData, 
        final String project, 
        final UUID repositoryId) {

        final UUID locationId = UUID.fromString("630fd2e4-fb88-4f85-ad21-13f3fd1fbca9"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("project", project); //$NON-NLS-1$
        routeValues.put("repositoryId", repositoryId); //$NON-NLS-1$

        final Object httpRequest = super.createRequest(HttpMethod.POST,
                                                       locationId,
                                                       routeValues,
                                                       apiVersion,
                                                       requestData,
                                                       APPLICATION_JSON_TYPE,
                                                       APPLICATION_JSON_TYPE);

        return super.sendRequest(httpRequest, new TypeReference<List<List<GitItem>>>() {});
    }

    /** 
     * Post for retrieving a creating a batch out of a set of items in a repo / project given a list of paths or a long path
     * 
     * @param requestData 
     *            
     * @param project 
     *            Project ID
     * @param repositoryId 
     *            
     * @return List<List<GitItem>>
     */
    public List<List<GitItem>> getItemsBatch(
        final GitItemRequestData requestData, 
        final UUID project, 
        final String repositoryId) {

        final UUID locationId = UUID.fromString("630fd2e4-fb88-4f85-ad21-13f3fd1fbca9"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("project", project); //$NON-NLS-1$
        routeValues.put("repositoryId", repositoryId); //$NON-NLS-1$

        final Object httpRequest = super.createRequest(HttpMethod.POST,
                                                       locationId,
                                                       routeValues,
                                                       apiVersion,
                                                       requestData,
                                                       APPLICATION_JSON_TYPE,
                                                       APPLICATION_JSON_TYPE);

        return super.sendRequest(httpRequest, new TypeReference<List<List<GitItem>>>() {});
    }

    /** 
     * Post for retrieving a creating a batch out of a set of items in a repo / project given a list of paths or a long path
     * 
     * @param requestData 
     *            
     * @param project 
     *            Project ID
     * @param repositoryId 
     *            
     * @return List<List<GitItem>>
     */
    public List<List<GitItem>> getItemsBatch(
        final GitItemRequestData requestData, 
        final UUID project, 
        final UUID repositoryId) {

        final UUID locationId = UUID.fromString("630fd2e4-fb88-4f85-ad21-13f3fd1fbca9"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("project", project); //$NON-NLS-1$
        routeValues.put("repositoryId", repositoryId); //$NON-NLS-1$

        final Object httpRequest = super.createRequest(HttpMethod.POST,
                                                       locationId,
                                                       routeValues,
                                                       apiVersion,
                                                       requestData,
                                                       APPLICATION_JSON_TYPE,
                                                       APPLICATION_JSON_TYPE);

        return super.sendRequest(httpRequest, new TypeReference<List<List<GitItem>>>() {});
    }

    /** 
     * Adds a reviewer to a git pull request
     * 
     * @param reviewer 
     *            
     * @param repositoryId 
     *            
     * @param pullRequestId 
     *            
     * @param reviewerId 
     *            
     * @return IdentityRefWithVote
     */
    public IdentityRefWithVote createPullRequestReviewer(
        final IdentityRefWithVote reviewer, 
        final String repositoryId, 
        final int pullRequestId, 
        final String reviewerId) {

        final UUID locationId = UUID.fromString("4b6702c7-aa35-4b89-9c96-b9abf6d3e540"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("repositoryId", repositoryId); //$NON-NLS-1$
        routeValues.put("pullRequestId", pullRequestId); //$NON-NLS-1$
        routeValues.put("reviewerId", reviewerId); //$NON-NLS-1$

        final Object httpRequest = super.createRequest(HttpMethod.PUT,
                                                       locationId,
                                                       routeValues,
                                                       apiVersion,
                                                       reviewer,
                                                       APPLICATION_JSON_TYPE,
                                                       APPLICATION_JSON_TYPE);

        return super.sendRequest(httpRequest, IdentityRefWithVote.class);
    }

    /** 
     * Adds a reviewer to a git pull request
     * 
     * @param reviewer 
     *            
     * @param repositoryId 
     *            
     * @param pullRequestId 
     *            
     * @param reviewerId 
     *            
     * @return IdentityRefWithVote
     */
    public IdentityRefWithVote createPullRequestReviewer(
        final IdentityRefWithVote reviewer, 
        final UUID repositoryId, 
        final int pullRequestId, 
        final String reviewerId) {

        final UUID locationId = UUID.fromString("4b6702c7-aa35-4b89-9c96-b9abf6d3e540"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("repositoryId", repositoryId); //$NON-NLS-1$
        routeValues.put("pullRequestId", pullRequestId); //$NON-NLS-1$
        routeValues.put("reviewerId", reviewerId); //$NON-NLS-1$

        final Object httpRequest = super.createRequest(HttpMethod.PUT,
                                                       locationId,
                                                       routeValues,
                                                       apiVersion,
                                                       reviewer,
                                                       APPLICATION_JSON_TYPE,
                                                       APPLICATION_JSON_TYPE);

        return super.sendRequest(httpRequest, IdentityRefWithVote.class);
    }

    /** 
     * Adds a reviewer to a git pull request
     * 
     * @param reviewer 
     *            
     * @param project 
     *            Project ID or project name
     * @param repositoryId 
     *            
     * @param pullRequestId 
     *            
     * @param reviewerId 
     *            
     * @return IdentityRefWithVote
     */
    public IdentityRefWithVote createPullRequestReviewer(
        final IdentityRefWithVote reviewer, 
        final String project, 
        final String repositoryId, 
        final int pullRequestId, 
        final String reviewerId) {

        final UUID locationId = UUID.fromString("4b6702c7-aa35-4b89-9c96-b9abf6d3e540"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("project", project); //$NON-NLS-1$
        routeValues.put("repositoryId", repositoryId); //$NON-NLS-1$
        routeValues.put("pullRequestId", pullRequestId); //$NON-NLS-1$
        routeValues.put("reviewerId", reviewerId); //$NON-NLS-1$

        final Object httpRequest = super.createRequest(HttpMethod.PUT,
                                                       locationId,
                                                       routeValues,
                                                       apiVersion,
                                                       reviewer,
                                                       APPLICATION_JSON_TYPE,
                                                       APPLICATION_JSON_TYPE);

        return super.sendRequest(httpRequest, IdentityRefWithVote.class);
    }

    /** 
     * Adds a reviewer to a git pull request
     * 
     * @param reviewer 
     *            
     * @param project 
     *            Project ID or project name
     * @param repositoryId 
     *            
     * @param pullRequestId 
     *            
     * @param reviewerId 
     *            
     * @return IdentityRefWithVote
     */
    public IdentityRefWithVote createPullRequestReviewer(
        final IdentityRefWithVote reviewer, 
        final String project, 
        final UUID repositoryId, 
        final int pullRequestId, 
        final String reviewerId) {

        final UUID locationId = UUID.fromString("4b6702c7-aa35-4b89-9c96-b9abf6d3e540"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("project", project); //$NON-NLS-1$
        routeValues.put("repositoryId", repositoryId); //$NON-NLS-1$
        routeValues.put("pullRequestId", pullRequestId); //$NON-NLS-1$
        routeValues.put("reviewerId", reviewerId); //$NON-NLS-1$

        final Object httpRequest = super.createRequest(HttpMethod.PUT,
                                                       locationId,
                                                       routeValues,
                                                       apiVersion,
                                                       reviewer,
                                                       APPLICATION_JSON_TYPE,
                                                       APPLICATION_JSON_TYPE);

        return super.sendRequest(httpRequest, IdentityRefWithVote.class);
    }

    /** 
     * Adds a reviewer to a git pull request
     * 
     * @param reviewer 
     *            
     * @param project 
     *            Project ID
     * @param repositoryId 
     *            
     * @param pullRequestId 
     *            
     * @param reviewerId 
     *            
     * @return IdentityRefWithVote
     */
    public IdentityRefWithVote createPullRequestReviewer(
        final IdentityRefWithVote reviewer, 
        final UUID project, 
        final String repositoryId, 
        final int pullRequestId, 
        final String reviewerId) {

        final UUID locationId = UUID.fromString("4b6702c7-aa35-4b89-9c96-b9abf6d3e540"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("project", project); //$NON-NLS-1$
        routeValues.put("repositoryId", repositoryId); //$NON-NLS-1$
        routeValues.put("pullRequestId", pullRequestId); //$NON-NLS-1$
        routeValues.put("reviewerId", reviewerId); //$NON-NLS-1$

        final Object httpRequest = super.createRequest(HttpMethod.PUT,
                                                       locationId,
                                                       routeValues,
                                                       apiVersion,
                                                       reviewer,
                                                       APPLICATION_JSON_TYPE,
                                                       APPLICATION_JSON_TYPE);

        return super.sendRequest(httpRequest, IdentityRefWithVote.class);
    }

    /** 
     * Adds a reviewer to a git pull request
     * 
     * @param reviewer 
     *            
     * @param project 
     *            Project ID
     * @param repositoryId 
     *            
     * @param pullRequestId 
     *            
     * @param reviewerId 
     *            
     * @return IdentityRefWithVote
     */
    public IdentityRefWithVote createPullRequestReviewer(
        final IdentityRefWithVote reviewer, 
        final UUID project, 
        final UUID repositoryId, 
        final int pullRequestId, 
        final String reviewerId) {

        final UUID locationId = UUID.fromString("4b6702c7-aa35-4b89-9c96-b9abf6d3e540"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("project", project); //$NON-NLS-1$
        routeValues.put("repositoryId", repositoryId); //$NON-NLS-1$
        routeValues.put("pullRequestId", pullRequestId); //$NON-NLS-1$
        routeValues.put("reviewerId", reviewerId); //$NON-NLS-1$

        final Object httpRequest = super.createRequest(HttpMethod.PUT,
                                                       locationId,
                                                       routeValues,
                                                       apiVersion,
                                                       reviewer,
                                                       APPLICATION_JSON_TYPE,
                                                       APPLICATION_JSON_TYPE);

        return super.sendRequest(httpRequest, IdentityRefWithVote.class);
    }

    /** 
     * Adds reviewers to a git pull request
     * 
     * @param reviewers 
     *            
     * @param repositoryId 
     *            
     * @param pullRequestId 
     *            
     * @return List<IdentityRefWithVote>
     */
    public List<IdentityRefWithVote> createPullRequestReviewers(
        final IdentityRef[] reviewers, 
        final String repositoryId, 
        final int pullRequestId) {

        final UUID locationId = UUID.fromString("4b6702c7-aa35-4b89-9c96-b9abf6d3e540"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("repositoryId", repositoryId); //$NON-NLS-1$
        routeValues.put("pullRequestId", pullRequestId); //$NON-NLS-1$

        final Object httpRequest = super.createRequest(HttpMethod.POST,
                                                       locationId,
                                                       routeValues,
                                                       apiVersion,
                                                       reviewers,
                                                       APPLICATION_JSON_TYPE,
                                                       APPLICATION_JSON_TYPE);

        return super.sendRequest(httpRequest, new TypeReference<List<IdentityRefWithVote>>() {});
    }

    /** 
     * Adds reviewers to a git pull request
     * 
     * @param reviewers 
     *            
     * @param repositoryId 
     *            
     * @param pullRequestId 
     *            
     * @return List<IdentityRefWithVote>
     */
    public List<IdentityRefWithVote> createPullRequestReviewers(
        final IdentityRef[] reviewers, 
        final UUID repositoryId, 
        final int pullRequestId) {

        final UUID locationId = UUID.fromString("4b6702c7-aa35-4b89-9c96-b9abf6d3e540"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("repositoryId", repositoryId); //$NON-NLS-1$
        routeValues.put("pullRequestId", pullRequestId); //$NON-NLS-1$

        final Object httpRequest = super.createRequest(HttpMethod.POST,
                                                       locationId,
                                                       routeValues,
                                                       apiVersion,
                                                       reviewers,
                                                       APPLICATION_JSON_TYPE,
                                                       APPLICATION_JSON_TYPE);

        return super.sendRequest(httpRequest, new TypeReference<List<IdentityRefWithVote>>() {});
    }

    /** 
     * Adds reviewers to a git pull request
     * 
     * @param reviewers 
     *            
     * @param project 
     *            Project ID or project name
     * @param repositoryId 
     *            
     * @param pullRequestId 
     *            
     * @return List<IdentityRefWithVote>
     */
    public List<IdentityRefWithVote> createPullRequestReviewers(
        final IdentityRef[] reviewers, 
        final String project, 
        final String repositoryId, 
        final int pullRequestId) {

        final UUID locationId = UUID.fromString("4b6702c7-aa35-4b89-9c96-b9abf6d3e540"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("project", project); //$NON-NLS-1$
        routeValues.put("repositoryId", repositoryId); //$NON-NLS-1$
        routeValues.put("pullRequestId", pullRequestId); //$NON-NLS-1$

        final Object httpRequest = super.createRequest(HttpMethod.POST,
                                                       locationId,
                                                       routeValues,
                                                       apiVersion,
                                                       reviewers,
                                                       APPLICATION_JSON_TYPE,
                                                       APPLICATION_JSON_TYPE);

        return super.sendRequest(httpRequest, new TypeReference<List<IdentityRefWithVote>>() {});
    }

    /** 
     * Adds reviewers to a git pull request
     * 
     * @param reviewers 
     *            
     * @param project 
     *            Project ID or project name
     * @param repositoryId 
     *            
     * @param pullRequestId 
     *            
     * @return List<IdentityRefWithVote>
     */
    public List<IdentityRefWithVote> createPullRequestReviewers(
        final IdentityRef[] reviewers, 
        final String project, 
        final UUID repositoryId, 
        final int pullRequestId) {

        final UUID locationId = UUID.fromString("4b6702c7-aa35-4b89-9c96-b9abf6d3e540"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("project", project); //$NON-NLS-1$
        routeValues.put("repositoryId", repositoryId); //$NON-NLS-1$
        routeValues.put("pullRequestId", pullRequestId); //$NON-NLS-1$

        final Object httpRequest = super.createRequest(HttpMethod.POST,
                                                       locationId,
                                                       routeValues,
                                                       apiVersion,
                                                       reviewers,
                                                       APPLICATION_JSON_TYPE,
                                                       APPLICATION_JSON_TYPE);

        return super.sendRequest(httpRequest, new TypeReference<List<IdentityRefWithVote>>() {});
    }

    /** 
     * Adds reviewers to a git pull request
     * 
     * @param reviewers 
     *            
     * @param project 
     *            Project ID
     * @param repositoryId 
     *            
     * @param pullRequestId 
     *            
     * @return List<IdentityRefWithVote>
     */
    public List<IdentityRefWithVote> createPullRequestReviewers(
        final IdentityRef[] reviewers, 
        final UUID project, 
        final String repositoryId, 
        final int pullRequestId) {

        final UUID locationId = UUID.fromString("4b6702c7-aa35-4b89-9c96-b9abf6d3e540"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("project", project); //$NON-NLS-1$
        routeValues.put("repositoryId", repositoryId); //$NON-NLS-1$
        routeValues.put("pullRequestId", pullRequestId); //$NON-NLS-1$

        final Object httpRequest = super.createRequest(HttpMethod.POST,
                                                       locationId,
                                                       routeValues,
                                                       apiVersion,
                                                       reviewers,
                                                       APPLICATION_JSON_TYPE,
                                                       APPLICATION_JSON_TYPE);

        return super.sendRequest(httpRequest, new TypeReference<List<IdentityRefWithVote>>() {});
    }

    /** 
     * Adds reviewers to a git pull request
     * 
     * @param reviewers 
     *            
     * @param project 
     *            Project ID
     * @param repositoryId 
     *            
     * @param pullRequestId 
     *            
     * @return List<IdentityRefWithVote>
     */
    public List<IdentityRefWithVote> createPullRequestReviewers(
        final IdentityRef[] reviewers, 
        final UUID project, 
        final UUID repositoryId, 
        final int pullRequestId) {

        final UUID locationId = UUID.fromString("4b6702c7-aa35-4b89-9c96-b9abf6d3e540"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("project", project); //$NON-NLS-1$
        routeValues.put("repositoryId", repositoryId); //$NON-NLS-1$
        routeValues.put("pullRequestId", pullRequestId); //$NON-NLS-1$

        final Object httpRequest = super.createRequest(HttpMethod.POST,
                                                       locationId,
                                                       routeValues,
                                                       apiVersion,
                                                       reviewers,
                                                       APPLICATION_JSON_TYPE,
                                                       APPLICATION_JSON_TYPE);

        return super.sendRequest(httpRequest, new TypeReference<List<IdentityRefWithVote>>() {});
    }

    /** 
     * Adds reviewers to a git pull request
     * 
     * @param repositoryId 
     *            
     * @param pullRequestId 
     *            
     * @param reviewerId 
     *            
     */
    public void deletePullRequestReviewer(
        final String repositoryId, 
        final int pullRequestId, 
        final String reviewerId) {

        final UUID locationId = UUID.fromString("4b6702c7-aa35-4b89-9c96-b9abf6d3e540"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("repositoryId", repositoryId); //$NON-NLS-1$
        routeValues.put("pullRequestId", pullRequestId); //$NON-NLS-1$
        routeValues.put("reviewerId", reviewerId); //$NON-NLS-1$

        final Object httpRequest = super.createRequest(HttpMethod.DELETE,
                                                       locationId,
                                                       routeValues,
                                                       apiVersion,
                                                       APPLICATION_JSON_TYPE);

        super.sendRequest(httpRequest);
    }

    /** 
     * Adds reviewers to a git pull request
     * 
     * @param repositoryId 
     *            
     * @param pullRequestId 
     *            
     * @param reviewerId 
     *            
     */
    public void deletePullRequestReviewer(
        final UUID repositoryId, 
        final int pullRequestId, 
        final String reviewerId) {

        final UUID locationId = UUID.fromString("4b6702c7-aa35-4b89-9c96-b9abf6d3e540"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("repositoryId", repositoryId); //$NON-NLS-1$
        routeValues.put("pullRequestId", pullRequestId); //$NON-NLS-1$
        routeValues.put("reviewerId", reviewerId); //$NON-NLS-1$

        final Object httpRequest = super.createRequest(HttpMethod.DELETE,
                                                       locationId,
                                                       routeValues,
                                                       apiVersion,
                                                       APPLICATION_JSON_TYPE);

        super.sendRequest(httpRequest);
    }

    /** 
     * Adds reviewers to a git pull request
     * 
     * @param project 
     *            Project ID or project name
     * @param repositoryId 
     *            
     * @param pullRequestId 
     *            
     * @param reviewerId 
     *            
     */
    public void deletePullRequestReviewer(
        final String project, 
        final String repositoryId, 
        final int pullRequestId, 
        final String reviewerId) {

        final UUID locationId = UUID.fromString("4b6702c7-aa35-4b89-9c96-b9abf6d3e540"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("project", project); //$NON-NLS-1$
        routeValues.put("repositoryId", repositoryId); //$NON-NLS-1$
        routeValues.put("pullRequestId", pullRequestId); //$NON-NLS-1$
        routeValues.put("reviewerId", reviewerId); //$NON-NLS-1$

        final Object httpRequest = super.createRequest(HttpMethod.DELETE,
                                                       locationId,
                                                       routeValues,
                                                       apiVersion,
                                                       APPLICATION_JSON_TYPE);

        super.sendRequest(httpRequest);
    }

    /** 
     * Adds reviewers to a git pull request
     * 
     * @param project 
     *            Project ID or project name
     * @param repositoryId 
     *            
     * @param pullRequestId 
     *            
     * @param reviewerId 
     *            
     */
    public void deletePullRequestReviewer(
        final String project, 
        final UUID repositoryId, 
        final int pullRequestId, 
        final String reviewerId) {

        final UUID locationId = UUID.fromString("4b6702c7-aa35-4b89-9c96-b9abf6d3e540"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("project", project); //$NON-NLS-1$
        routeValues.put("repositoryId", repositoryId); //$NON-NLS-1$
        routeValues.put("pullRequestId", pullRequestId); //$NON-NLS-1$
        routeValues.put("reviewerId", reviewerId); //$NON-NLS-1$

        final Object httpRequest = super.createRequest(HttpMethod.DELETE,
                                                       locationId,
                                                       routeValues,
                                                       apiVersion,
                                                       APPLICATION_JSON_TYPE);

        super.sendRequest(httpRequest);
    }

    /** 
     * Adds reviewers to a git pull request
     * 
     * @param project 
     *            Project ID
     * @param repositoryId 
     *            
     * @param pullRequestId 
     *            
     * @param reviewerId 
     *            
     */
    public void deletePullRequestReviewer(
        final UUID project, 
        final String repositoryId, 
        final int pullRequestId, 
        final String reviewerId) {

        final UUID locationId = UUID.fromString("4b6702c7-aa35-4b89-9c96-b9abf6d3e540"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("project", project); //$NON-NLS-1$
        routeValues.put("repositoryId", repositoryId); //$NON-NLS-1$
        routeValues.put("pullRequestId", pullRequestId); //$NON-NLS-1$
        routeValues.put("reviewerId", reviewerId); //$NON-NLS-1$

        final Object httpRequest = super.createRequest(HttpMethod.DELETE,
                                                       locationId,
                                                       routeValues,
                                                       apiVersion,
                                                       APPLICATION_JSON_TYPE);

        super.sendRequest(httpRequest);
    }

    /** 
     * Adds reviewers to a git pull request
     * 
     * @param project 
     *            Project ID
     * @param repositoryId 
     *            
     * @param pullRequestId 
     *            
     * @param reviewerId 
     *            
     */
    public void deletePullRequestReviewer(
        final UUID project, 
        final UUID repositoryId, 
        final int pullRequestId, 
        final String reviewerId) {

        final UUID locationId = UUID.fromString("4b6702c7-aa35-4b89-9c96-b9abf6d3e540"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("project", project); //$NON-NLS-1$
        routeValues.put("repositoryId", repositoryId); //$NON-NLS-1$
        routeValues.put("pullRequestId", pullRequestId); //$NON-NLS-1$
        routeValues.put("reviewerId", reviewerId); //$NON-NLS-1$

        final Object httpRequest = super.createRequest(HttpMethod.DELETE,
                                                       locationId,
                                                       routeValues,
                                                       apiVersion,
                                                       APPLICATION_JSON_TYPE);

        super.sendRequest(httpRequest);
    }

    /** 
     * Retrieve a reviewer from a pull request
     * 
     * @param repositoryId 
     *            
     * @param pullRequestId 
     *            
     * @param reviewerId 
     *            
     * @return IdentityRefWithVote
     */
    public IdentityRefWithVote getPullRequestReviewer(
        final String repositoryId, 
        final int pullRequestId, 
        final String reviewerId) {

        final UUID locationId = UUID.fromString("4b6702c7-aa35-4b89-9c96-b9abf6d3e540"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("repositoryId", repositoryId); //$NON-NLS-1$
        routeValues.put("pullRequestId", pullRequestId); //$NON-NLS-1$
        routeValues.put("reviewerId", reviewerId); //$NON-NLS-1$

        final Object httpRequest = super.createRequest(HttpMethod.GET,
                                                       locationId,
                                                       routeValues,
                                                       apiVersion,
                                                       APPLICATION_JSON_TYPE);

        return super.sendRequest(httpRequest, IdentityRefWithVote.class);
    }

    /** 
     * Retrieve a reviewer from a pull request
     * 
     * @param repositoryId 
     *            
     * @param pullRequestId 
     *            
     * @param reviewerId 
     *            
     * @return IdentityRefWithVote
     */
    public IdentityRefWithVote getPullRequestReviewer(
        final UUID repositoryId, 
        final int pullRequestId, 
        final String reviewerId) {

        final UUID locationId = UUID.fromString("4b6702c7-aa35-4b89-9c96-b9abf6d3e540"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("repositoryId", repositoryId); //$NON-NLS-1$
        routeValues.put("pullRequestId", pullRequestId); //$NON-NLS-1$
        routeValues.put("reviewerId", reviewerId); //$NON-NLS-1$

        final Object httpRequest = super.createRequest(HttpMethod.GET,
                                                       locationId,
                                                       routeValues,
                                                       apiVersion,
                                                       APPLICATION_JSON_TYPE);

        return super.sendRequest(httpRequest, IdentityRefWithVote.class);
    }

    /** 
     * Retrieve a reviewer from a pull request
     * 
     * @param project 
     *            Project ID or project name
     * @param repositoryId 
     *            
     * @param pullRequestId 
     *            
     * @param reviewerId 
     *            
     * @return IdentityRefWithVote
     */
    public IdentityRefWithVote getPullRequestReviewer(
        final String project, 
        final String repositoryId, 
        final int pullRequestId, 
        final String reviewerId) {

        final UUID locationId = UUID.fromString("4b6702c7-aa35-4b89-9c96-b9abf6d3e540"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("project", project); //$NON-NLS-1$
        routeValues.put("repositoryId", repositoryId); //$NON-NLS-1$
        routeValues.put("pullRequestId", pullRequestId); //$NON-NLS-1$
        routeValues.put("reviewerId", reviewerId); //$NON-NLS-1$

        final Object httpRequest = super.createRequest(HttpMethod.GET,
                                                       locationId,
                                                       routeValues,
                                                       apiVersion,
                                                       APPLICATION_JSON_TYPE);

        return super.sendRequest(httpRequest, IdentityRefWithVote.class);
    }

    /** 
     * Retrieve a reviewer from a pull request
     * 
     * @param project 
     *            Project ID or project name
     * @param repositoryId 
     *            
     * @param pullRequestId 
     *            
     * @param reviewerId 
     *            
     * @return IdentityRefWithVote
     */
    public IdentityRefWithVote getPullRequestReviewer(
        final String project, 
        final UUID repositoryId, 
        final int pullRequestId, 
        final String reviewerId) {

        final UUID locationId = UUID.fromString("4b6702c7-aa35-4b89-9c96-b9abf6d3e540"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("project", project); //$NON-NLS-1$
        routeValues.put("repositoryId", repositoryId); //$NON-NLS-1$
        routeValues.put("pullRequestId", pullRequestId); //$NON-NLS-1$
        routeValues.put("reviewerId", reviewerId); //$NON-NLS-1$

        final Object httpRequest = super.createRequest(HttpMethod.GET,
                                                       locationId,
                                                       routeValues,
                                                       apiVersion,
                                                       APPLICATION_JSON_TYPE);

        return super.sendRequest(httpRequest, IdentityRefWithVote.class);
    }

    /** 
     * Retrieve a reviewer from a pull request
     * 
     * @param project 
     *            Project ID
     * @param repositoryId 
     *            
     * @param pullRequestId 
     *            
     * @param reviewerId 
     *            
     * @return IdentityRefWithVote
     */
    public IdentityRefWithVote getPullRequestReviewer(
        final UUID project, 
        final String repositoryId, 
        final int pullRequestId, 
        final String reviewerId) {

        final UUID locationId = UUID.fromString("4b6702c7-aa35-4b89-9c96-b9abf6d3e540"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("project", project); //$NON-NLS-1$
        routeValues.put("repositoryId", repositoryId); //$NON-NLS-1$
        routeValues.put("pullRequestId", pullRequestId); //$NON-NLS-1$
        routeValues.put("reviewerId", reviewerId); //$NON-NLS-1$

        final Object httpRequest = super.createRequest(HttpMethod.GET,
                                                       locationId,
                                                       routeValues,
                                                       apiVersion,
                                                       APPLICATION_JSON_TYPE);

        return super.sendRequest(httpRequest, IdentityRefWithVote.class);
    }

    /** 
     * Retrieve a reviewer from a pull request
     * 
     * @param project 
     *            Project ID
     * @param repositoryId 
     *            
     * @param pullRequestId 
     *            
     * @param reviewerId 
     *            
     * @return IdentityRefWithVote
     */
    public IdentityRefWithVote getPullRequestReviewer(
        final UUID project, 
        final UUID repositoryId, 
        final int pullRequestId, 
        final String reviewerId) {

        final UUID locationId = UUID.fromString("4b6702c7-aa35-4b89-9c96-b9abf6d3e540"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("project", project); //$NON-NLS-1$
        routeValues.put("repositoryId", repositoryId); //$NON-NLS-1$
        routeValues.put("pullRequestId", pullRequestId); //$NON-NLS-1$
        routeValues.put("reviewerId", reviewerId); //$NON-NLS-1$

        final Object httpRequest = super.createRequest(HttpMethod.GET,
                                                       locationId,
                                                       routeValues,
                                                       apiVersion,
                                                       APPLICATION_JSON_TYPE);

        return super.sendRequest(httpRequest, IdentityRefWithVote.class);
    }

    /** 
     * Retrieve a pull request reviewers
     * 
     * @param repositoryId 
     *            
     * @param pullRequestId 
     *            
     * @return List<IdentityRefWithVote>
     */
    public List<IdentityRefWithVote> getPullRequestReviewers(
        final String repositoryId, 
        final int pullRequestId) {

        final UUID locationId = UUID.fromString("4b6702c7-aa35-4b89-9c96-b9abf6d3e540"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("repositoryId", repositoryId); //$NON-NLS-1$
        routeValues.put("pullRequestId", pullRequestId); //$NON-NLS-1$

        final Object httpRequest = super.createRequest(HttpMethod.GET,
                                                       locationId,
                                                       routeValues,
                                                       apiVersion,
                                                       APPLICATION_JSON_TYPE);

        return super.sendRequest(httpRequest, new TypeReference<List<IdentityRefWithVote>>() {});
    }

    /** 
     * Retrieve a pull request reviewers
     * 
     * @param repositoryId 
     *            
     * @param pullRequestId 
     *            
     * @return List<IdentityRefWithVote>
     */
    public List<IdentityRefWithVote> getPullRequestReviewers(
        final UUID repositoryId, 
        final int pullRequestId) {

        final UUID locationId = UUID.fromString("4b6702c7-aa35-4b89-9c96-b9abf6d3e540"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("repositoryId", repositoryId); //$NON-NLS-1$
        routeValues.put("pullRequestId", pullRequestId); //$NON-NLS-1$

        final Object httpRequest = super.createRequest(HttpMethod.GET,
                                                       locationId,
                                                       routeValues,
                                                       apiVersion,
                                                       APPLICATION_JSON_TYPE);

        return super.sendRequest(httpRequest, new TypeReference<List<IdentityRefWithVote>>() {});
    }

    /** 
     * Retrieve a pull request reviewers
     * 
     * @param project 
     *            Project ID or project name
     * @param repositoryId 
     *            
     * @param pullRequestId 
     *            
     * @return List<IdentityRefWithVote>
     */
    public List<IdentityRefWithVote> getPullRequestReviewers(
        final String project, 
        final String repositoryId, 
        final int pullRequestId) {

        final UUID locationId = UUID.fromString("4b6702c7-aa35-4b89-9c96-b9abf6d3e540"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("project", project); //$NON-NLS-1$
        routeValues.put("repositoryId", repositoryId); //$NON-NLS-1$
        routeValues.put("pullRequestId", pullRequestId); //$NON-NLS-1$

        final Object httpRequest = super.createRequest(HttpMethod.GET,
                                                       locationId,
                                                       routeValues,
                                                       apiVersion,
                                                       APPLICATION_JSON_TYPE);

        return super.sendRequest(httpRequest, new TypeReference<List<IdentityRefWithVote>>() {});
    }

    /** 
     * Retrieve a pull request reviewers
     * 
     * @param project 
     *            Project ID or project name
     * @param repositoryId 
     *            
     * @param pullRequestId 
     *            
     * @return List<IdentityRefWithVote>
     */
    public List<IdentityRefWithVote> getPullRequestReviewers(
        final String project, 
        final UUID repositoryId, 
        final int pullRequestId) {

        final UUID locationId = UUID.fromString("4b6702c7-aa35-4b89-9c96-b9abf6d3e540"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("project", project); //$NON-NLS-1$
        routeValues.put("repositoryId", repositoryId); //$NON-NLS-1$
        routeValues.put("pullRequestId", pullRequestId); //$NON-NLS-1$

        final Object httpRequest = super.createRequest(HttpMethod.GET,
                                                       locationId,
                                                       routeValues,
                                                       apiVersion,
                                                       APPLICATION_JSON_TYPE);

        return super.sendRequest(httpRequest, new TypeReference<List<IdentityRefWithVote>>() {});
    }

    /** 
     * Retrieve a pull request reviewers
     * 
     * @param project 
     *            Project ID
     * @param repositoryId 
     *            
     * @param pullRequestId 
     *            
     * @return List<IdentityRefWithVote>
     */
    public List<IdentityRefWithVote> getPullRequestReviewers(
        final UUID project, 
        final String repositoryId, 
        final int pullRequestId) {

        final UUID locationId = UUID.fromString("4b6702c7-aa35-4b89-9c96-b9abf6d3e540"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("project", project); //$NON-NLS-1$
        routeValues.put("repositoryId", repositoryId); //$NON-NLS-1$
        routeValues.put("pullRequestId", pullRequestId); //$NON-NLS-1$

        final Object httpRequest = super.createRequest(HttpMethod.GET,
                                                       locationId,
                                                       routeValues,
                                                       apiVersion,
                                                       APPLICATION_JSON_TYPE);

        return super.sendRequest(httpRequest, new TypeReference<List<IdentityRefWithVote>>() {});
    }

    /** 
     * Retrieve a pull request reviewers
     * 
     * @param project 
     *            Project ID
     * @param repositoryId 
     *            
     * @param pullRequestId 
     *            
     * @return List<IdentityRefWithVote>
     */
    public List<IdentityRefWithVote> getPullRequestReviewers(
        final UUID project, 
        final UUID repositoryId, 
        final int pullRequestId) {

        final UUID locationId = UUID.fromString("4b6702c7-aa35-4b89-9c96-b9abf6d3e540"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("project", project); //$NON-NLS-1$
        routeValues.put("repositoryId", repositoryId); //$NON-NLS-1$
        routeValues.put("pullRequestId", pullRequestId); //$NON-NLS-1$

        final Object httpRequest = super.createRequest(HttpMethod.GET,
                                                       locationId,
                                                       routeValues,
                                                       apiVersion,
                                                       APPLICATION_JSON_TYPE);

        return super.sendRequest(httpRequest, new TypeReference<List<IdentityRefWithVote>>() {});
    }

    /** 
     * Create a git pull request
     * 
     * @param gitPullRequestToCreate 
     *            
     * @param repositoryId 
     *            
     * @return GitPullRequest
     */
    public GitPullRequest createPullRequest(
        final GitPullRequest gitPullRequestToCreate, 
        final String repositoryId) {

        final UUID locationId = UUID.fromString("9946fd70-0d40-406e-b686-b4744cbbcc37"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("repositoryId", repositoryId); //$NON-NLS-1$

        final Object httpRequest = super.createRequest(HttpMethod.POST,
                                                       locationId,
                                                       routeValues,
                                                       apiVersion,
                                                       gitPullRequestToCreate,
                                                       APPLICATION_JSON_TYPE,
                                                       APPLICATION_JSON_TYPE);

        return super.sendRequest(httpRequest, GitPullRequest.class);
    }

    /** 
     * Create a git pull request
     * 
     * @param gitPullRequestToCreate 
     *            
     * @param repositoryId 
     *            
     * @return GitPullRequest
     */
    public GitPullRequest createPullRequest(
        final GitPullRequest gitPullRequestToCreate, 
        final UUID repositoryId) {

        final UUID locationId = UUID.fromString("9946fd70-0d40-406e-b686-b4744cbbcc37"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("repositoryId", repositoryId); //$NON-NLS-1$

        final Object httpRequest = super.createRequest(HttpMethod.POST,
                                                       locationId,
                                                       routeValues,
                                                       apiVersion,
                                                       gitPullRequestToCreate,
                                                       APPLICATION_JSON_TYPE,
                                                       APPLICATION_JSON_TYPE);

        return super.sendRequest(httpRequest, GitPullRequest.class);
    }

    /** 
     * Create a git pull request
     * 
     * @param gitPullRequestToCreate 
     *            
     * @param project 
     *            Project ID or project name
     * @param repositoryId 
     *            
     * @return GitPullRequest
     */
    public GitPullRequest createPullRequest(
        final GitPullRequest gitPullRequestToCreate, 
        final String project, 
        final String repositoryId) {

        final UUID locationId = UUID.fromString("9946fd70-0d40-406e-b686-b4744cbbcc37"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("project", project); //$NON-NLS-1$
        routeValues.put("repositoryId", repositoryId); //$NON-NLS-1$

        final Object httpRequest = super.createRequest(HttpMethod.POST,
                                                       locationId,
                                                       routeValues,
                                                       apiVersion,
                                                       gitPullRequestToCreate,
                                                       APPLICATION_JSON_TYPE,
                                                       APPLICATION_JSON_TYPE);

        return super.sendRequest(httpRequest, GitPullRequest.class);
    }

    /** 
     * Create a git pull request
     * 
     * @param gitPullRequestToCreate 
     *            
     * @param project 
     *            Project ID or project name
     * @param repositoryId 
     *            
     * @return GitPullRequest
     */
    public GitPullRequest createPullRequest(
        final GitPullRequest gitPullRequestToCreate, 
        final String project, 
        final UUID repositoryId) {

        final UUID locationId = UUID.fromString("9946fd70-0d40-406e-b686-b4744cbbcc37"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("project", project); //$NON-NLS-1$
        routeValues.put("repositoryId", repositoryId); //$NON-NLS-1$

        final Object httpRequest = super.createRequest(HttpMethod.POST,
                                                       locationId,
                                                       routeValues,
                                                       apiVersion,
                                                       gitPullRequestToCreate,
                                                       APPLICATION_JSON_TYPE,
                                                       APPLICATION_JSON_TYPE);

        return super.sendRequest(httpRequest, GitPullRequest.class);
    }

    /** 
     * Create a git pull request
     * 
     * @param gitPullRequestToCreate 
     *            
     * @param project 
     *            Project ID
     * @param repositoryId 
     *            
     * @return GitPullRequest
     */
    public GitPullRequest createPullRequest(
        final GitPullRequest gitPullRequestToCreate, 
        final UUID project, 
        final String repositoryId) {

        final UUID locationId = UUID.fromString("9946fd70-0d40-406e-b686-b4744cbbcc37"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("project", project); //$NON-NLS-1$
        routeValues.put("repositoryId", repositoryId); //$NON-NLS-1$

        final Object httpRequest = super.createRequest(HttpMethod.POST,
                                                       locationId,
                                                       routeValues,
                                                       apiVersion,
                                                       gitPullRequestToCreate,
                                                       APPLICATION_JSON_TYPE,
                                                       APPLICATION_JSON_TYPE);

        return super.sendRequest(httpRequest, GitPullRequest.class);
    }

    /** 
     * Create a git pull request
     * 
     * @param gitPullRequestToCreate 
     *            
     * @param project 
     *            Project ID
     * @param repositoryId 
     *            
     * @return GitPullRequest
     */
    public GitPullRequest createPullRequest(
        final GitPullRequest gitPullRequestToCreate, 
        final UUID project, 
        final UUID repositoryId) {

        final UUID locationId = UUID.fromString("9946fd70-0d40-406e-b686-b4744cbbcc37"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("project", project); //$NON-NLS-1$
        routeValues.put("repositoryId", repositoryId); //$NON-NLS-1$

        final Object httpRequest = super.createRequest(HttpMethod.POST,
                                                       locationId,
                                                       routeValues,
                                                       apiVersion,
                                                       gitPullRequestToCreate,
                                                       APPLICATION_JSON_TYPE,
                                                       APPLICATION_JSON_TYPE);

        return super.sendRequest(httpRequest, GitPullRequest.class);
    }

    /** 
     * Retrieve a pull request
     * 
     * @param project 
     *            Project ID or project name
     * @param repositoryId 
     *            
     * @param pullRequestId 
     *            
     * @param maxCommentLength 
     *            
     * @param skip 
     *            
     * @param top 
     *            
     * @return GitPullRequest
     */
    public GitPullRequest getPullRequest(
        final String project, 
        final String repositoryId, 
        final int pullRequestId, 
        final Integer maxCommentLength, 
        final Integer skip, 
        final Integer top) {

        final UUID locationId = UUID.fromString("9946fd70-0d40-406e-b686-b4744cbbcc37"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("project", project); //$NON-NLS-1$
        routeValues.put("repositoryId", repositoryId); //$NON-NLS-1$
        routeValues.put("pullRequestId", pullRequestId); //$NON-NLS-1$

        final NameValueCollection queryParameters = new NameValueCollection();
        queryParameters.addIfNotNull("maxCommentLength", maxCommentLength); //$NON-NLS-1$
        queryParameters.addIfNotNull("$skip", skip); //$NON-NLS-1$
        queryParameters.addIfNotNull("$top", top); //$NON-NLS-1$

        final Object httpRequest = super.createRequest(HttpMethod.GET,
                                                       locationId,
                                                       routeValues,
                                                       apiVersion,
                                                       queryParameters,
                                                       APPLICATION_JSON_TYPE);

        return super.sendRequest(httpRequest, GitPullRequest.class);
    }

    /** 
     * Retrieve a pull request
     * 
     * @param project 
     *            Project ID or project name
     * @param repositoryId 
     *            
     * @param pullRequestId 
     *            
     * @param maxCommentLength 
     *            
     * @param skip 
     *            
     * @param top 
     *            
     * @return GitPullRequest
     */
    public GitPullRequest getPullRequest(
        final String project, 
        final UUID repositoryId, 
        final int pullRequestId, 
        final Integer maxCommentLength, 
        final Integer skip, 
        final Integer top) {

        final UUID locationId = UUID.fromString("9946fd70-0d40-406e-b686-b4744cbbcc37"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("project", project); //$NON-NLS-1$
        routeValues.put("repositoryId", repositoryId); //$NON-NLS-1$
        routeValues.put("pullRequestId", pullRequestId); //$NON-NLS-1$

        final NameValueCollection queryParameters = new NameValueCollection();
        queryParameters.addIfNotNull("maxCommentLength", maxCommentLength); //$NON-NLS-1$
        queryParameters.addIfNotNull("$skip", skip); //$NON-NLS-1$
        queryParameters.addIfNotNull("$top", top); //$NON-NLS-1$

        final Object httpRequest = super.createRequest(HttpMethod.GET,
                                                       locationId,
                                                       routeValues,
                                                       apiVersion,
                                                       queryParameters,
                                                       APPLICATION_JSON_TYPE);

        return super.sendRequest(httpRequest, GitPullRequest.class);
    }

    /** 
     * Retrieve a pull request
     * 
     * @param project 
     *            Project ID
     * @param repositoryId 
     *            
     * @param pullRequestId 
     *            
     * @param maxCommentLength 
     *            
     * @param skip 
     *            
     * @param top 
     *            
     * @return GitPullRequest
     */
    public GitPullRequest getPullRequest(
        final UUID project, 
        final String repositoryId, 
        final int pullRequestId, 
        final Integer maxCommentLength, 
        final Integer skip, 
        final Integer top) {

        final UUID locationId = UUID.fromString("9946fd70-0d40-406e-b686-b4744cbbcc37"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("project", project); //$NON-NLS-1$
        routeValues.put("repositoryId", repositoryId); //$NON-NLS-1$
        routeValues.put("pullRequestId", pullRequestId); //$NON-NLS-1$

        final NameValueCollection queryParameters = new NameValueCollection();
        queryParameters.addIfNotNull("maxCommentLength", maxCommentLength); //$NON-NLS-1$
        queryParameters.addIfNotNull("$skip", skip); //$NON-NLS-1$
        queryParameters.addIfNotNull("$top", top); //$NON-NLS-1$

        final Object httpRequest = super.createRequest(HttpMethod.GET,
                                                       locationId,
                                                       routeValues,
                                                       apiVersion,
                                                       queryParameters,
                                                       APPLICATION_JSON_TYPE);

        return super.sendRequest(httpRequest, GitPullRequest.class);
    }

    /** 
     * Retrieve a pull request
     * 
     * @param project 
     *            Project ID
     * @param repositoryId 
     *            
     * @param pullRequestId 
     *            
     * @param maxCommentLength 
     *            
     * @param skip 
     *            
     * @param top 
     *            
     * @return GitPullRequest
     */
    public GitPullRequest getPullRequest(
        final UUID project, 
        final UUID repositoryId, 
        final int pullRequestId, 
        final Integer maxCommentLength, 
        final Integer skip, 
        final Integer top) {

        final UUID locationId = UUID.fromString("9946fd70-0d40-406e-b686-b4744cbbcc37"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("project", project); //$NON-NLS-1$
        routeValues.put("repositoryId", repositoryId); //$NON-NLS-1$
        routeValues.put("pullRequestId", pullRequestId); //$NON-NLS-1$

        final NameValueCollection queryParameters = new NameValueCollection();
        queryParameters.addIfNotNull("maxCommentLength", maxCommentLength); //$NON-NLS-1$
        queryParameters.addIfNotNull("$skip", skip); //$NON-NLS-1$
        queryParameters.addIfNotNull("$top", top); //$NON-NLS-1$

        final Object httpRequest = super.createRequest(HttpMethod.GET,
                                                       locationId,
                                                       routeValues,
                                                       apiVersion,
                                                       queryParameters,
                                                       APPLICATION_JSON_TYPE);

        return super.sendRequest(httpRequest, GitPullRequest.class);
    }

    /** 
     * Retrieve a pull request
     * 
     * @param repositoryId 
     *            
     * @param pullRequestId 
     *            
     * @param maxCommentLength 
     *            
     * @param skip 
     *            
     * @param top 
     *            
     * @return GitPullRequest
     */
    public GitPullRequest getPullRequest(
        final String repositoryId, 
        final int pullRequestId, 
        final Integer maxCommentLength, 
        final Integer skip, 
        final Integer top) {

        final UUID locationId = UUID.fromString("9946fd70-0d40-406e-b686-b4744cbbcc37"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("repositoryId", repositoryId); //$NON-NLS-1$
        routeValues.put("pullRequestId", pullRequestId); //$NON-NLS-1$

        final NameValueCollection queryParameters = new NameValueCollection();
        queryParameters.addIfNotNull("maxCommentLength", maxCommentLength); //$NON-NLS-1$
        queryParameters.addIfNotNull("$skip", skip); //$NON-NLS-1$
        queryParameters.addIfNotNull("$top", top); //$NON-NLS-1$

        final Object httpRequest = super.createRequest(HttpMethod.GET,
                                                       locationId,
                                                       routeValues,
                                                       apiVersion,
                                                       queryParameters,
                                                       APPLICATION_JSON_TYPE);

        return super.sendRequest(httpRequest, GitPullRequest.class);
    }

    /** 
     * Retrieve a pull request
     * 
     * @param repositoryId 
     *            
     * @param pullRequestId 
     *            
     * @param maxCommentLength 
     *            
     * @param skip 
     *            
     * @param top 
     *            
     * @return GitPullRequest
     */
    public GitPullRequest getPullRequest(
        final UUID repositoryId, 
        final int pullRequestId, 
        final Integer maxCommentLength, 
        final Integer skip, 
        final Integer top) {

        final UUID locationId = UUID.fromString("9946fd70-0d40-406e-b686-b4744cbbcc37"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("repositoryId", repositoryId); //$NON-NLS-1$
        routeValues.put("pullRequestId", pullRequestId); //$NON-NLS-1$

        final NameValueCollection queryParameters = new NameValueCollection();
        queryParameters.addIfNotNull("maxCommentLength", maxCommentLength); //$NON-NLS-1$
        queryParameters.addIfNotNull("$skip", skip); //$NON-NLS-1$
        queryParameters.addIfNotNull("$top", top); //$NON-NLS-1$

        final Object httpRequest = super.createRequest(HttpMethod.GET,
                                                       locationId,
                                                       routeValues,
                                                       apiVersion,
                                                       queryParameters,
                                                       APPLICATION_JSON_TYPE);

        return super.sendRequest(httpRequest, GitPullRequest.class);
    }

    /** 
     * Query for pull requests
     * 
     * @param project 
     *            Project ID or project name
     * @param repositoryId 
     *            
     * @param searchCriteria 
     *            
     * @param maxCommentLength 
     *            
     * @param skip 
     *            
     * @param top 
     *            
     * @return List<GitPullRequest>
     */
    public List<GitPullRequest> getPullRequests(
        final String project, 
        final String repositoryId, 
        final GitPullRequestSearchCriteria searchCriteria, 
        final Integer maxCommentLength, 
        final Integer skip, 
        final Integer top) {

        final UUID locationId = UUID.fromString("9946fd70-0d40-406e-b686-b4744cbbcc37"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("project", project); //$NON-NLS-1$
        routeValues.put("repositoryId", repositoryId); //$NON-NLS-1$

        final NameValueCollection queryParameters = new NameValueCollection();
        addModelAsQueryParams(queryParameters, searchCriteria);
        queryParameters.addIfNotNull("maxCommentLength", maxCommentLength); //$NON-NLS-1$
        queryParameters.addIfNotNull("$skip", skip); //$NON-NLS-1$
        queryParameters.addIfNotNull("$top", top); //$NON-NLS-1$

        final Object httpRequest = super.createRequest(HttpMethod.GET,
                                                       locationId,
                                                       routeValues,
                                                       apiVersion,
                                                       queryParameters,
                                                       APPLICATION_JSON_TYPE);

        return super.sendRequest(httpRequest, new TypeReference<List<GitPullRequest>>() {});
    }

    /** 
     * Query for pull requests
     * 
     * @param project 
     *            Project ID or project name
     * @param repositoryId 
     *            
     * @param searchCriteria 
     *            
     * @param maxCommentLength 
     *            
     * @param skip 
     *            
     * @param top 
     *            
     * @return List<GitPullRequest>
     */
    public List<GitPullRequest> getPullRequests(
        final String project, 
        final UUID repositoryId, 
        final GitPullRequestSearchCriteria searchCriteria, 
        final Integer maxCommentLength, 
        final Integer skip, 
        final Integer top) {

        final UUID locationId = UUID.fromString("9946fd70-0d40-406e-b686-b4744cbbcc37"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("project", project); //$NON-NLS-1$
        routeValues.put("repositoryId", repositoryId); //$NON-NLS-1$

        final NameValueCollection queryParameters = new NameValueCollection();
        addModelAsQueryParams(queryParameters, searchCriteria);
        queryParameters.addIfNotNull("maxCommentLength", maxCommentLength); //$NON-NLS-1$
        queryParameters.addIfNotNull("$skip", skip); //$NON-NLS-1$
        queryParameters.addIfNotNull("$top", top); //$NON-NLS-1$

        final Object httpRequest = super.createRequest(HttpMethod.GET,
                                                       locationId,
                                                       routeValues,
                                                       apiVersion,
                                                       queryParameters,
                                                       APPLICATION_JSON_TYPE);

        return super.sendRequest(httpRequest, new TypeReference<List<GitPullRequest>>() {});
    }

    /** 
     * Query for pull requests
     * 
     * @param project 
     *            Project ID
     * @param repositoryId 
     *            
     * @param searchCriteria 
     *            
     * @param maxCommentLength 
     *            
     * @param skip 
     *            
     * @param top 
     *            
     * @return List<GitPullRequest>
     */
    public List<GitPullRequest> getPullRequests(
        final UUID project, 
        final String repositoryId, 
        final GitPullRequestSearchCriteria searchCriteria, 
        final Integer maxCommentLength, 
        final Integer skip, 
        final Integer top) {

        final UUID locationId = UUID.fromString("9946fd70-0d40-406e-b686-b4744cbbcc37"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("project", project); //$NON-NLS-1$
        routeValues.put("repositoryId", repositoryId); //$NON-NLS-1$

        final NameValueCollection queryParameters = new NameValueCollection();
        addModelAsQueryParams(queryParameters, searchCriteria);
        queryParameters.addIfNotNull("maxCommentLength", maxCommentLength); //$NON-NLS-1$
        queryParameters.addIfNotNull("$skip", skip); //$NON-NLS-1$
        queryParameters.addIfNotNull("$top", top); //$NON-NLS-1$

        final Object httpRequest = super.createRequest(HttpMethod.GET,
                                                       locationId,
                                                       routeValues,
                                                       apiVersion,
                                                       queryParameters,
                                                       APPLICATION_JSON_TYPE);

        return super.sendRequest(httpRequest, new TypeReference<List<GitPullRequest>>() {});
    }

    /** 
     * Query for pull requests
     * 
     * @param project 
     *            Project ID
     * @param repositoryId 
     *            
     * @param searchCriteria 
     *            
     * @param maxCommentLength 
     *            
     * @param skip 
     *            
     * @param top 
     *            
     * @return List<GitPullRequest>
     */
    public List<GitPullRequest> getPullRequests(
        final UUID project, 
        final UUID repositoryId, 
        final GitPullRequestSearchCriteria searchCriteria, 
        final Integer maxCommentLength, 
        final Integer skip, 
        final Integer top) {

        final UUID locationId = UUID.fromString("9946fd70-0d40-406e-b686-b4744cbbcc37"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("project", project); //$NON-NLS-1$
        routeValues.put("repositoryId", repositoryId); //$NON-NLS-1$

        final NameValueCollection queryParameters = new NameValueCollection();
        addModelAsQueryParams(queryParameters, searchCriteria);
        queryParameters.addIfNotNull("maxCommentLength", maxCommentLength); //$NON-NLS-1$
        queryParameters.addIfNotNull("$skip", skip); //$NON-NLS-1$
        queryParameters.addIfNotNull("$top", top); //$NON-NLS-1$

        final Object httpRequest = super.createRequest(HttpMethod.GET,
                                                       locationId,
                                                       routeValues,
                                                       apiVersion,
                                                       queryParameters,
                                                       APPLICATION_JSON_TYPE);

        return super.sendRequest(httpRequest, new TypeReference<List<GitPullRequest>>() {});
    }

    /** 
     * Query for pull requests
     * 
     * @param repositoryId 
     *            
     * @param searchCriteria 
     *            
     * @param maxCommentLength 
     *            
     * @param skip 
     *            
     * @param top 
     *            
     * @return List<GitPullRequest>
     */
    public List<GitPullRequest> getPullRequests(
        final String repositoryId, 
        final GitPullRequestSearchCriteria searchCriteria, 
        final Integer maxCommentLength, 
        final Integer skip, 
        final Integer top) {

        final UUID locationId = UUID.fromString("9946fd70-0d40-406e-b686-b4744cbbcc37"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("repositoryId", repositoryId); //$NON-NLS-1$

        final NameValueCollection queryParameters = new NameValueCollection();
        addModelAsQueryParams(queryParameters, searchCriteria);
        queryParameters.addIfNotNull("maxCommentLength", maxCommentLength); //$NON-NLS-1$
        queryParameters.addIfNotNull("$skip", skip); //$NON-NLS-1$
        queryParameters.addIfNotNull("$top", top); //$NON-NLS-1$

        final Object httpRequest = super.createRequest(HttpMethod.GET,
                                                       locationId,
                                                       routeValues,
                                                       apiVersion,
                                                       queryParameters,
                                                       APPLICATION_JSON_TYPE);

        return super.sendRequest(httpRequest, new TypeReference<List<GitPullRequest>>() {});
    }

    /** 
     * Query for pull requests
     * 
     * @param repositoryId 
     *            
     * @param searchCriteria 
     *            
     * @param maxCommentLength 
     *            
     * @param skip 
     *            
     * @param top 
     *            
     * @return List<GitPullRequest>
     */
    public List<GitPullRequest> getPullRequests(
        final UUID repositoryId, 
        final GitPullRequestSearchCriteria searchCriteria, 
        final Integer maxCommentLength, 
        final Integer skip, 
        final Integer top) {

        final UUID locationId = UUID.fromString("9946fd70-0d40-406e-b686-b4744cbbcc37"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("repositoryId", repositoryId); //$NON-NLS-1$

        final NameValueCollection queryParameters = new NameValueCollection();
        addModelAsQueryParams(queryParameters, searchCriteria);
        queryParameters.addIfNotNull("maxCommentLength", maxCommentLength); //$NON-NLS-1$
        queryParameters.addIfNotNull("$skip", skip); //$NON-NLS-1$
        queryParameters.addIfNotNull("$top", top); //$NON-NLS-1$

        final Object httpRequest = super.createRequest(HttpMethod.GET,
                                                       locationId,
                                                       routeValues,
                                                       apiVersion,
                                                       queryParameters,
                                                       APPLICATION_JSON_TYPE);

        return super.sendRequest(httpRequest, new TypeReference<List<GitPullRequest>>() {});
    }

    /** 
     * Updates a pull request
     * 
     * @param gitPullRequestToUpdate 
     *            
     * @param repositoryId 
     *            
     * @param pullRequestId 
     *            
     * @return GitPullRequest
     */
    public GitPullRequest updatePullRequest(
        final GitPullRequest gitPullRequestToUpdate, 
        final String repositoryId, 
        final int pullRequestId) {

        final UUID locationId = UUID.fromString("9946fd70-0d40-406e-b686-b4744cbbcc37"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("repositoryId", repositoryId); //$NON-NLS-1$
        routeValues.put("pullRequestId", pullRequestId); //$NON-NLS-1$

        final Object httpRequest = super.createRequest(HttpMethod.PATCH,
                                                       locationId,
                                                       routeValues,
                                                       apiVersion,
                                                       gitPullRequestToUpdate,
                                                       APPLICATION_JSON_TYPE,
                                                       APPLICATION_JSON_TYPE);

        return super.sendRequest(httpRequest, GitPullRequest.class);
    }

    /** 
     * Updates a pull request
     * 
     * @param gitPullRequestToUpdate 
     *            
     * @param repositoryId 
     *            
     * @param pullRequestId 
     *            
     * @return GitPullRequest
     */
    public GitPullRequest updatePullRequest(
        final GitPullRequest gitPullRequestToUpdate, 
        final UUID repositoryId, 
        final int pullRequestId) {

        final UUID locationId = UUID.fromString("9946fd70-0d40-406e-b686-b4744cbbcc37"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("repositoryId", repositoryId); //$NON-NLS-1$
        routeValues.put("pullRequestId", pullRequestId); //$NON-NLS-1$

        final Object httpRequest = super.createRequest(HttpMethod.PATCH,
                                                       locationId,
                                                       routeValues,
                                                       apiVersion,
                                                       gitPullRequestToUpdate,
                                                       APPLICATION_JSON_TYPE,
                                                       APPLICATION_JSON_TYPE);

        return super.sendRequest(httpRequest, GitPullRequest.class);
    }

    /** 
     * Updates a pull request
     * 
     * @param gitPullRequestToUpdate 
     *            
     * @param project 
     *            Project ID or project name
     * @param repositoryId 
     *            
     * @param pullRequestId 
     *            
     * @return GitPullRequest
     */
    public GitPullRequest updatePullRequest(
        final GitPullRequest gitPullRequestToUpdate, 
        final String project, 
        final String repositoryId, 
        final int pullRequestId) {

        final UUID locationId = UUID.fromString("9946fd70-0d40-406e-b686-b4744cbbcc37"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("project", project); //$NON-NLS-1$
        routeValues.put("repositoryId", repositoryId); //$NON-NLS-1$
        routeValues.put("pullRequestId", pullRequestId); //$NON-NLS-1$

        final Object httpRequest = super.createRequest(HttpMethod.PATCH,
                                                       locationId,
                                                       routeValues,
                                                       apiVersion,
                                                       gitPullRequestToUpdate,
                                                       APPLICATION_JSON_TYPE,
                                                       APPLICATION_JSON_TYPE);

        return super.sendRequest(httpRequest, GitPullRequest.class);
    }

    /** 
     * Updates a pull request
     * 
     * @param gitPullRequestToUpdate 
     *            
     * @param project 
     *            Project ID or project name
     * @param repositoryId 
     *            
     * @param pullRequestId 
     *            
     * @return GitPullRequest
     */
    public GitPullRequest updatePullRequest(
        final GitPullRequest gitPullRequestToUpdate, 
        final String project, 
        final UUID repositoryId, 
        final int pullRequestId) {

        final UUID locationId = UUID.fromString("9946fd70-0d40-406e-b686-b4744cbbcc37"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("project", project); //$NON-NLS-1$
        routeValues.put("repositoryId", repositoryId); //$NON-NLS-1$
        routeValues.put("pullRequestId", pullRequestId); //$NON-NLS-1$

        final Object httpRequest = super.createRequest(HttpMethod.PATCH,
                                                       locationId,
                                                       routeValues,
                                                       apiVersion,
                                                       gitPullRequestToUpdate,
                                                       APPLICATION_JSON_TYPE,
                                                       APPLICATION_JSON_TYPE);

        return super.sendRequest(httpRequest, GitPullRequest.class);
    }

    /** 
     * Updates a pull request
     * 
     * @param gitPullRequestToUpdate 
     *            
     * @param project 
     *            Project ID
     * @param repositoryId 
     *            
     * @param pullRequestId 
     *            
     * @return GitPullRequest
     */
    public GitPullRequest updatePullRequest(
        final GitPullRequest gitPullRequestToUpdate, 
        final UUID project, 
        final String repositoryId, 
        final int pullRequestId) {

        final UUID locationId = UUID.fromString("9946fd70-0d40-406e-b686-b4744cbbcc37"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("project", project); //$NON-NLS-1$
        routeValues.put("repositoryId", repositoryId); //$NON-NLS-1$
        routeValues.put("pullRequestId", pullRequestId); //$NON-NLS-1$

        final Object httpRequest = super.createRequest(HttpMethod.PATCH,
                                                       locationId,
                                                       routeValues,
                                                       apiVersion,
                                                       gitPullRequestToUpdate,
                                                       APPLICATION_JSON_TYPE,
                                                       APPLICATION_JSON_TYPE);

        return super.sendRequest(httpRequest, GitPullRequest.class);
    }

    /** 
     * Updates a pull request
     * 
     * @param gitPullRequestToUpdate 
     *            
     * @param project 
     *            Project ID
     * @param repositoryId 
     *            
     * @param pullRequestId 
     *            
     * @return GitPullRequest
     */
    public GitPullRequest updatePullRequest(
        final GitPullRequest gitPullRequestToUpdate, 
        final UUID project, 
        final UUID repositoryId, 
        final int pullRequestId) {

        final UUID locationId = UUID.fromString("9946fd70-0d40-406e-b686-b4744cbbcc37"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("project", project); //$NON-NLS-1$
        routeValues.put("repositoryId", repositoryId); //$NON-NLS-1$
        routeValues.put("pullRequestId", pullRequestId); //$NON-NLS-1$

        final Object httpRequest = super.createRequest(HttpMethod.PATCH,
                                                       locationId,
                                                       routeValues,
                                                       apiVersion,
                                                       gitPullRequestToUpdate,
                                                       APPLICATION_JSON_TYPE,
                                                       APPLICATION_JSON_TYPE);

        return super.sendRequest(httpRequest, GitPullRequest.class);
    }

    /** 
     * [Preview API] Retrieve a pull request work items
     * 
     * @param project 
     *            Project ID or project name
     * @param repositoryId 
     *            
     * @param pullRequestId 
     *            
     * @param commitsTop 
     *            
     * @param commitsSkip 
     *            
     * @return List<AssociatedWorkItem>
     */
    public List<AssociatedWorkItem> getPullRequestWorkItems(
        final String project, 
        final String repositoryId, 
        final int pullRequestId, 
        final Integer commitsTop, 
        final Integer commitsSkip) {

        final UUID locationId = UUID.fromString("0a637fcc-5370-4ce8-b0e8-98091f5f9482"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0-preview.1"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("project", project); //$NON-NLS-1$
        routeValues.put("repositoryId", repositoryId); //$NON-NLS-1$
        routeValues.put("pullRequestId", pullRequestId); //$NON-NLS-1$

        final NameValueCollection queryParameters = new NameValueCollection();
        queryParameters.addIfNotNull("commitsTop", commitsTop); //$NON-NLS-1$
        queryParameters.addIfNotNull("commitsSkip", commitsSkip); //$NON-NLS-1$

        final Object httpRequest = super.createRequest(HttpMethod.GET,
                                                       locationId,
                                                       routeValues,
                                                       apiVersion,
                                                       queryParameters,
                                                       APPLICATION_JSON_TYPE);

        return super.sendRequest(httpRequest, new TypeReference<List<AssociatedWorkItem>>() {});
    }

    /** 
     * [Preview API] Retrieve a pull request work items
     * 
     * @param project 
     *            Project ID or project name
     * @param repositoryId 
     *            
     * @param pullRequestId 
     *            
     * @param commitsTop 
     *            
     * @param commitsSkip 
     *            
     * @return List<AssociatedWorkItem>
     */
    public List<AssociatedWorkItem> getPullRequestWorkItems(
        final String project, 
        final UUID repositoryId, 
        final int pullRequestId, 
        final Integer commitsTop, 
        final Integer commitsSkip) {

        final UUID locationId = UUID.fromString("0a637fcc-5370-4ce8-b0e8-98091f5f9482"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0-preview.1"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("project", project); //$NON-NLS-1$
        routeValues.put("repositoryId", repositoryId); //$NON-NLS-1$
        routeValues.put("pullRequestId", pullRequestId); //$NON-NLS-1$

        final NameValueCollection queryParameters = new NameValueCollection();
        queryParameters.addIfNotNull("commitsTop", commitsTop); //$NON-NLS-1$
        queryParameters.addIfNotNull("commitsSkip", commitsSkip); //$NON-NLS-1$

        final Object httpRequest = super.createRequest(HttpMethod.GET,
                                                       locationId,
                                                       routeValues,
                                                       apiVersion,
                                                       queryParameters,
                                                       APPLICATION_JSON_TYPE);

        return super.sendRequest(httpRequest, new TypeReference<List<AssociatedWorkItem>>() {});
    }

    /** 
     * [Preview API] Retrieve a pull request work items
     * 
     * @param project 
     *            Project ID
     * @param repositoryId 
     *            
     * @param pullRequestId 
     *            
     * @param commitsTop 
     *            
     * @param commitsSkip 
     *            
     * @return List<AssociatedWorkItem>
     */
    public List<AssociatedWorkItem> getPullRequestWorkItems(
        final UUID project, 
        final String repositoryId, 
        final int pullRequestId, 
        final Integer commitsTop, 
        final Integer commitsSkip) {

        final UUID locationId = UUID.fromString("0a637fcc-5370-4ce8-b0e8-98091f5f9482"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0-preview.1"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("project", project); //$NON-NLS-1$
        routeValues.put("repositoryId", repositoryId); //$NON-NLS-1$
        routeValues.put("pullRequestId", pullRequestId); //$NON-NLS-1$

        final NameValueCollection queryParameters = new NameValueCollection();
        queryParameters.addIfNotNull("commitsTop", commitsTop); //$NON-NLS-1$
        queryParameters.addIfNotNull("commitsSkip", commitsSkip); //$NON-NLS-1$

        final Object httpRequest = super.createRequest(HttpMethod.GET,
                                                       locationId,
                                                       routeValues,
                                                       apiVersion,
                                                       queryParameters,
                                                       APPLICATION_JSON_TYPE);

        return super.sendRequest(httpRequest, new TypeReference<List<AssociatedWorkItem>>() {});
    }

    /** 
     * [Preview API] Retrieve a pull request work items
     * 
     * @param project 
     *            Project ID
     * @param repositoryId 
     *            
     * @param pullRequestId 
     *            
     * @param commitsTop 
     *            
     * @param commitsSkip 
     *            
     * @return List<AssociatedWorkItem>
     */
    public List<AssociatedWorkItem> getPullRequestWorkItems(
        final UUID project, 
        final UUID repositoryId, 
        final int pullRequestId, 
        final Integer commitsTop, 
        final Integer commitsSkip) {

        final UUID locationId = UUID.fromString("0a637fcc-5370-4ce8-b0e8-98091f5f9482"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0-preview.1"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("project", project); //$NON-NLS-1$
        routeValues.put("repositoryId", repositoryId); //$NON-NLS-1$
        routeValues.put("pullRequestId", pullRequestId); //$NON-NLS-1$

        final NameValueCollection queryParameters = new NameValueCollection();
        queryParameters.addIfNotNull("commitsTop", commitsTop); //$NON-NLS-1$
        queryParameters.addIfNotNull("commitsSkip", commitsSkip); //$NON-NLS-1$

        final Object httpRequest = super.createRequest(HttpMethod.GET,
                                                       locationId,
                                                       routeValues,
                                                       apiVersion,
                                                       queryParameters,
                                                       APPLICATION_JSON_TYPE);

        return super.sendRequest(httpRequest, new TypeReference<List<AssociatedWorkItem>>() {});
    }

    /** 
     * [Preview API] Retrieve a pull request work items
     * 
     * @param repositoryId 
     *            
     * @param pullRequestId 
     *            
     * @param commitsTop 
     *            
     * @param commitsSkip 
     *            
     * @return List<AssociatedWorkItem>
     */
    public List<AssociatedWorkItem> getPullRequestWorkItems(
        final String repositoryId, 
        final int pullRequestId, 
        final Integer commitsTop, 
        final Integer commitsSkip) {

        final UUID locationId = UUID.fromString("0a637fcc-5370-4ce8-b0e8-98091f5f9482"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0-preview.1"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("repositoryId", repositoryId); //$NON-NLS-1$
        routeValues.put("pullRequestId", pullRequestId); //$NON-NLS-1$

        final NameValueCollection queryParameters = new NameValueCollection();
        queryParameters.addIfNotNull("commitsTop", commitsTop); //$NON-NLS-1$
        queryParameters.addIfNotNull("commitsSkip", commitsSkip); //$NON-NLS-1$

        final Object httpRequest = super.createRequest(HttpMethod.GET,
                                                       locationId,
                                                       routeValues,
                                                       apiVersion,
                                                       queryParameters,
                                                       APPLICATION_JSON_TYPE);

        return super.sendRequest(httpRequest, new TypeReference<List<AssociatedWorkItem>>() {});
    }

    /** 
     * [Preview API] Retrieve a pull request work items
     * 
     * @param repositoryId 
     *            
     * @param pullRequestId 
     *            
     * @param commitsTop 
     *            
     * @param commitsSkip 
     *            
     * @return List<AssociatedWorkItem>
     */
    public List<AssociatedWorkItem> getPullRequestWorkItems(
        final UUID repositoryId, 
        final int pullRequestId, 
        final Integer commitsTop, 
        final Integer commitsSkip) {

        final UUID locationId = UUID.fromString("0a637fcc-5370-4ce8-b0e8-98091f5f9482"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0-preview.1"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("repositoryId", repositoryId); //$NON-NLS-1$
        routeValues.put("pullRequestId", pullRequestId); //$NON-NLS-1$

        final NameValueCollection queryParameters = new NameValueCollection();
        queryParameters.addIfNotNull("commitsTop", commitsTop); //$NON-NLS-1$
        queryParameters.addIfNotNull("commitsSkip", commitsSkip); //$NON-NLS-1$

        final Object httpRequest = super.createRequest(HttpMethod.GET,
                                                       locationId,
                                                       routeValues,
                                                       apiVersion,
                                                       queryParameters,
                                                       APPLICATION_JSON_TYPE);

        return super.sendRequest(httpRequest, new TypeReference<List<AssociatedWorkItem>>() {});
    }

    /** 
     * Push changes to the repository.
     * 
     * @param repositoryId 
     *            The id or friendly name of the repository. To use the friendly name, a project-scoped route must be used.
     * @return GitPush
     */
    public GitPush createPush(
        final InputStream uploadStream,
        final String repositoryId) {

        final UUID locationId = UUID.fromString("ea98d07b-3c87-4971-8ede-a613694ffb55"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("repositoryId", repositoryId); //$NON-NLS-1$

        final Object httpRequest = super.createRequest(HttpMethod.POST,
                                                       locationId,
                                                       routeValues,
                                                       apiVersion,
                                                       uploadStream,
                                                       APPLICATION_OCTET_STREAM_TYPE,
                                                       APPLICATION_JSON_TYPE);

        return super.sendRequest(httpRequest, GitPush.class);
    }

    /** 
     * Push changes to the repository.
     * 
     * @param repositoryId 
     *            The id or friendly name of the repository. To use the friendly name, a project-scoped route must be used.
     * @return GitPush
     */
    public GitPush createPush(
        final InputStream uploadStream,
        final UUID repositoryId) {

        final UUID locationId = UUID.fromString("ea98d07b-3c87-4971-8ede-a613694ffb55"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("repositoryId", repositoryId); //$NON-NLS-1$

        final Object httpRequest = super.createRequest(HttpMethod.POST,
                                                       locationId,
                                                       routeValues,
                                                       apiVersion,
                                                       uploadStream,
                                                       APPLICATION_OCTET_STREAM_TYPE,
                                                       APPLICATION_JSON_TYPE);

        return super.sendRequest(httpRequest, GitPush.class);
    }

    /** 
     * Push changes to the repository.
     * 
     * @param project 
     *            Project ID or project name
     * @param repositoryId 
     *            The id or friendly name of the repository. To use the friendly name, a project-scoped route must be used.
     * @return GitPush
     */
    public GitPush createPush(
        final InputStream uploadStream,
        final String project, 
        final String repositoryId) {

        final UUID locationId = UUID.fromString("ea98d07b-3c87-4971-8ede-a613694ffb55"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("project", project); //$NON-NLS-1$
        routeValues.put("repositoryId", repositoryId); //$NON-NLS-1$

        final Object httpRequest = super.createRequest(HttpMethod.POST,
                                                       locationId,
                                                       routeValues,
                                                       apiVersion,
                                                       uploadStream,
                                                       APPLICATION_OCTET_STREAM_TYPE,
                                                       APPLICATION_JSON_TYPE);

        return super.sendRequest(httpRequest, GitPush.class);
    }

    /** 
     * Push changes to the repository.
     * 
     * @param project 
     *            Project ID or project name
     * @param repositoryId 
     *            The id or friendly name of the repository. To use the friendly name, a project-scoped route must be used.
     * @return GitPush
     */
    public GitPush createPush(
        final InputStream uploadStream,
        final String project, 
        final UUID repositoryId) {

        final UUID locationId = UUID.fromString("ea98d07b-3c87-4971-8ede-a613694ffb55"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("project", project); //$NON-NLS-1$
        routeValues.put("repositoryId", repositoryId); //$NON-NLS-1$

        final Object httpRequest = super.createRequest(HttpMethod.POST,
                                                       locationId,
                                                       routeValues,
                                                       apiVersion,
                                                       uploadStream,
                                                       APPLICATION_OCTET_STREAM_TYPE,
                                                       APPLICATION_JSON_TYPE);

        return super.sendRequest(httpRequest, GitPush.class);
    }

    /** 
     * Push changes to the repository.
     * 
     * @param project 
     *            Project ID
     * @param repositoryId 
     *            The id or friendly name of the repository. To use the friendly name, a project-scoped route must be used.
     * @return GitPush
     */
    public GitPush createPush(
        final InputStream uploadStream,
        final UUID project, 
        final String repositoryId) {

        final UUID locationId = UUID.fromString("ea98d07b-3c87-4971-8ede-a613694ffb55"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("project", project); //$NON-NLS-1$
        routeValues.put("repositoryId", repositoryId); //$NON-NLS-1$

        final Object httpRequest = super.createRequest(HttpMethod.POST,
                                                       locationId,
                                                       routeValues,
                                                       apiVersion,
                                                       uploadStream,
                                                       APPLICATION_OCTET_STREAM_TYPE,
                                                       APPLICATION_JSON_TYPE);

        return super.sendRequest(httpRequest, GitPush.class);
    }

    /** 
     * Push changes to the repository.
     * 
     * @param project 
     *            Project ID
     * @param repositoryId 
     *            The id or friendly name of the repository. To use the friendly name, a project-scoped route must be used.
     * @return GitPush
     */
    public GitPush createPush(
        final InputStream uploadStream,
        final UUID project, 
        final UUID repositoryId) {

        final UUID locationId = UUID.fromString("ea98d07b-3c87-4971-8ede-a613694ffb55"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("project", project); //$NON-NLS-1$
        routeValues.put("repositoryId", repositoryId); //$NON-NLS-1$

        final Object httpRequest = super.createRequest(HttpMethod.POST,
                                                       locationId,
                                                       routeValues,
                                                       apiVersion,
                                                       uploadStream,
                                                       APPLICATION_OCTET_STREAM_TYPE,
                                                       APPLICATION_JSON_TYPE);

        return super.sendRequest(httpRequest, GitPush.class);
    }

    /** 
     * Retrieve a particular push.
     * 
     * @param project 
     *            Project ID or project name
     * @param repositoryId 
     *            The id or friendly name of the repository. To use the friendly name, projectId must also be specified.
     * @param pushId 
     *            The id of the push.
     * @param includeCommits 
     *            The number of commits to include in the result.
     * @param includeRefUpdates 
     *            
     * @return GitPush
     */
    public GitPush getPush(
        final String project, 
        final String repositoryId, 
        final int pushId, 
        final Integer includeCommits, 
        final Boolean includeRefUpdates) {

        final UUID locationId = UUID.fromString("ea98d07b-3c87-4971-8ede-a613694ffb55"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("project", project); //$NON-NLS-1$
        routeValues.put("repositoryId", repositoryId); //$NON-NLS-1$
        routeValues.put("pushId", pushId); //$NON-NLS-1$

        final NameValueCollection queryParameters = new NameValueCollection();
        queryParameters.addIfNotNull("includeCommits", includeCommits); //$NON-NLS-1$
        queryParameters.addIfNotNull("includeRefUpdates", includeRefUpdates); //$NON-NLS-1$

        final Object httpRequest = super.createRequest(HttpMethod.GET,
                                                       locationId,
                                                       routeValues,
                                                       apiVersion,
                                                       queryParameters,
                                                       APPLICATION_JSON_TYPE);

        return super.sendRequest(httpRequest, GitPush.class);
    }

    /** 
     * Retrieve a particular push.
     * 
     * @param project 
     *            Project ID or project name
     * @param repositoryId 
     *            The id or friendly name of the repository. To use the friendly name, projectId must also be specified.
     * @param pushId 
     *            The id of the push.
     * @param includeCommits 
     *            The number of commits to include in the result.
     * @param includeRefUpdates 
     *            
     * @return GitPush
     */
    public GitPush getPush(
        final String project, 
        final UUID repositoryId, 
        final int pushId, 
        final Integer includeCommits, 
        final Boolean includeRefUpdates) {

        final UUID locationId = UUID.fromString("ea98d07b-3c87-4971-8ede-a613694ffb55"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("project", project); //$NON-NLS-1$
        routeValues.put("repositoryId", repositoryId); //$NON-NLS-1$
        routeValues.put("pushId", pushId); //$NON-NLS-1$

        final NameValueCollection queryParameters = new NameValueCollection();
        queryParameters.addIfNotNull("includeCommits", includeCommits); //$NON-NLS-1$
        queryParameters.addIfNotNull("includeRefUpdates", includeRefUpdates); //$NON-NLS-1$

        final Object httpRequest = super.createRequest(HttpMethod.GET,
                                                       locationId,
                                                       routeValues,
                                                       apiVersion,
                                                       queryParameters,
                                                       APPLICATION_JSON_TYPE);

        return super.sendRequest(httpRequest, GitPush.class);
    }

    /** 
     * Retrieve a particular push.
     * 
     * @param project 
     *            Project ID
     * @param repositoryId 
     *            The id or friendly name of the repository. To use the friendly name, projectId must also be specified.
     * @param pushId 
     *            The id of the push.
     * @param includeCommits 
     *            The number of commits to include in the result.
     * @param includeRefUpdates 
     *            
     * @return GitPush
     */
    public GitPush getPush(
        final UUID project, 
        final String repositoryId, 
        final int pushId, 
        final Integer includeCommits, 
        final Boolean includeRefUpdates) {

        final UUID locationId = UUID.fromString("ea98d07b-3c87-4971-8ede-a613694ffb55"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("project", project); //$NON-NLS-1$
        routeValues.put("repositoryId", repositoryId); //$NON-NLS-1$
        routeValues.put("pushId", pushId); //$NON-NLS-1$

        final NameValueCollection queryParameters = new NameValueCollection();
        queryParameters.addIfNotNull("includeCommits", includeCommits); //$NON-NLS-1$
        queryParameters.addIfNotNull("includeRefUpdates", includeRefUpdates); //$NON-NLS-1$

        final Object httpRequest = super.createRequest(HttpMethod.GET,
                                                       locationId,
                                                       routeValues,
                                                       apiVersion,
                                                       queryParameters,
                                                       APPLICATION_JSON_TYPE);

        return super.sendRequest(httpRequest, GitPush.class);
    }

    /** 
     * Retrieve a particular push.
     * 
     * @param project 
     *            Project ID
     * @param repositoryId 
     *            The id or friendly name of the repository. To use the friendly name, projectId must also be specified.
     * @param pushId 
     *            The id of the push.
     * @param includeCommits 
     *            The number of commits to include in the result.
     * @param includeRefUpdates 
     *            
     * @return GitPush
     */
    public GitPush getPush(
        final UUID project, 
        final UUID repositoryId, 
        final int pushId, 
        final Integer includeCommits, 
        final Boolean includeRefUpdates) {

        final UUID locationId = UUID.fromString("ea98d07b-3c87-4971-8ede-a613694ffb55"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("project", project); //$NON-NLS-1$
        routeValues.put("repositoryId", repositoryId); //$NON-NLS-1$
        routeValues.put("pushId", pushId); //$NON-NLS-1$

        final NameValueCollection queryParameters = new NameValueCollection();
        queryParameters.addIfNotNull("includeCommits", includeCommits); //$NON-NLS-1$
        queryParameters.addIfNotNull("includeRefUpdates", includeRefUpdates); //$NON-NLS-1$

        final Object httpRequest = super.createRequest(HttpMethod.GET,
                                                       locationId,
                                                       routeValues,
                                                       apiVersion,
                                                       queryParameters,
                                                       APPLICATION_JSON_TYPE);

        return super.sendRequest(httpRequest, GitPush.class);
    }

    /** 
     * Retrieve a particular push.
     * 
     * @param repositoryId 
     *            The id or friendly name of the repository. To use the friendly name, projectId must also be specified.
     * @param pushId 
     *            The id of the push.
     * @param includeCommits 
     *            The number of commits to include in the result.
     * @param includeRefUpdates 
     *            
     * @return GitPush
     */
    public GitPush getPush(
        final String repositoryId, 
        final int pushId, 
        final Integer includeCommits, 
        final Boolean includeRefUpdates) {

        final UUID locationId = UUID.fromString("ea98d07b-3c87-4971-8ede-a613694ffb55"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("repositoryId", repositoryId); //$NON-NLS-1$
        routeValues.put("pushId", pushId); //$NON-NLS-1$

        final NameValueCollection queryParameters = new NameValueCollection();
        queryParameters.addIfNotNull("includeCommits", includeCommits); //$NON-NLS-1$
        queryParameters.addIfNotNull("includeRefUpdates", includeRefUpdates); //$NON-NLS-1$

        final Object httpRequest = super.createRequest(HttpMethod.GET,
                                                       locationId,
                                                       routeValues,
                                                       apiVersion,
                                                       queryParameters,
                                                       APPLICATION_JSON_TYPE);

        return super.sendRequest(httpRequest, GitPush.class);
    }

    /** 
     * Retrieve a particular push.
     * 
     * @param repositoryId 
     *            The id or friendly name of the repository. To use the friendly name, projectId must also be specified.
     * @param pushId 
     *            The id of the push.
     * @param includeCommits 
     *            The number of commits to include in the result.
     * @param includeRefUpdates 
     *            
     * @return GitPush
     */
    public GitPush getPush(
        final UUID repositoryId, 
        final int pushId, 
        final Integer includeCommits, 
        final Boolean includeRefUpdates) {

        final UUID locationId = UUID.fromString("ea98d07b-3c87-4971-8ede-a613694ffb55"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("repositoryId", repositoryId); //$NON-NLS-1$
        routeValues.put("pushId", pushId); //$NON-NLS-1$

        final NameValueCollection queryParameters = new NameValueCollection();
        queryParameters.addIfNotNull("includeCommits", includeCommits); //$NON-NLS-1$
        queryParameters.addIfNotNull("includeRefUpdates", includeRefUpdates); //$NON-NLS-1$

        final Object httpRequest = super.createRequest(HttpMethod.GET,
                                                       locationId,
                                                       routeValues,
                                                       apiVersion,
                                                       queryParameters,
                                                       APPLICATION_JSON_TYPE);

        return super.sendRequest(httpRequest, GitPush.class);
    }

    /** 
     * Retrieves pushes associated with the specified repository.
     * 
     * @param project 
     *            Project ID or project name
     * @param repositoryId 
     *            The id or friendly name of the repository. To use the friendly name, projectId must also be specified.
     * @param skip 
     *            
     * @param top 
     *            
     * @param searchCriteria 
     *            
     * @return List<GitPush>
     */
    public List<GitPush> getPushes(
        final String project, 
        final String repositoryId, 
        final Integer skip, 
        final Integer top, 
        final GitPushSearchCriteria searchCriteria) {

        final UUID locationId = UUID.fromString("ea98d07b-3c87-4971-8ede-a613694ffb55"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("project", project); //$NON-NLS-1$
        routeValues.put("repositoryId", repositoryId); //$NON-NLS-1$

        final NameValueCollection queryParameters = new NameValueCollection();
        queryParameters.addIfNotNull("$skip", skip); //$NON-NLS-1$
        queryParameters.addIfNotNull("$top", top); //$NON-NLS-1$
        addModelAsQueryParams(queryParameters, searchCriteria);

        final Object httpRequest = super.createRequest(HttpMethod.GET,
                                                       locationId,
                                                       routeValues,
                                                       apiVersion,
                                                       queryParameters,
                                                       APPLICATION_JSON_TYPE);

        return super.sendRequest(httpRequest, new TypeReference<List<GitPush>>() {});
    }

    /** 
     * Retrieves pushes associated with the specified repository.
     * 
     * @param project 
     *            Project ID or project name
     * @param repositoryId 
     *            The id or friendly name of the repository. To use the friendly name, projectId must also be specified.
     * @param skip 
     *            
     * @param top 
     *            
     * @param searchCriteria 
     *            
     * @return List<GitPush>
     */
    public List<GitPush> getPushes(
        final String project, 
        final UUID repositoryId, 
        final Integer skip, 
        final Integer top, 
        final GitPushSearchCriteria searchCriteria) {

        final UUID locationId = UUID.fromString("ea98d07b-3c87-4971-8ede-a613694ffb55"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("project", project); //$NON-NLS-1$
        routeValues.put("repositoryId", repositoryId); //$NON-NLS-1$

        final NameValueCollection queryParameters = new NameValueCollection();
        queryParameters.addIfNotNull("$skip", skip); //$NON-NLS-1$
        queryParameters.addIfNotNull("$top", top); //$NON-NLS-1$
        addModelAsQueryParams(queryParameters, searchCriteria);

        final Object httpRequest = super.createRequest(HttpMethod.GET,
                                                       locationId,
                                                       routeValues,
                                                       apiVersion,
                                                       queryParameters,
                                                       APPLICATION_JSON_TYPE);

        return super.sendRequest(httpRequest, new TypeReference<List<GitPush>>() {});
    }

    /** 
     * Retrieves pushes associated with the specified repository.
     * 
     * @param project 
     *            Project ID
     * @param repositoryId 
     *            The id or friendly name of the repository. To use the friendly name, projectId must also be specified.
     * @param skip 
     *            
     * @param top 
     *            
     * @param searchCriteria 
     *            
     * @return List<GitPush>
     */
    public List<GitPush> getPushes(
        final UUID project, 
        final String repositoryId, 
        final Integer skip, 
        final Integer top, 
        final GitPushSearchCriteria searchCriteria) {

        final UUID locationId = UUID.fromString("ea98d07b-3c87-4971-8ede-a613694ffb55"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("project", project); //$NON-NLS-1$
        routeValues.put("repositoryId", repositoryId); //$NON-NLS-1$

        final NameValueCollection queryParameters = new NameValueCollection();
        queryParameters.addIfNotNull("$skip", skip); //$NON-NLS-1$
        queryParameters.addIfNotNull("$top", top); //$NON-NLS-1$
        addModelAsQueryParams(queryParameters, searchCriteria);

        final Object httpRequest = super.createRequest(HttpMethod.GET,
                                                       locationId,
                                                       routeValues,
                                                       apiVersion,
                                                       queryParameters,
                                                       APPLICATION_JSON_TYPE);

        return super.sendRequest(httpRequest, new TypeReference<List<GitPush>>() {});
    }

    /** 
     * Retrieves pushes associated with the specified repository.
     * 
     * @param project 
     *            Project ID
     * @param repositoryId 
     *            The id or friendly name of the repository. To use the friendly name, projectId must also be specified.
     * @param skip 
     *            
     * @param top 
     *            
     * @param searchCriteria 
     *            
     * @return List<GitPush>
     */
    public List<GitPush> getPushes(
        final UUID project, 
        final UUID repositoryId, 
        final Integer skip, 
        final Integer top, 
        final GitPushSearchCriteria searchCriteria) {

        final UUID locationId = UUID.fromString("ea98d07b-3c87-4971-8ede-a613694ffb55"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("project", project); //$NON-NLS-1$
        routeValues.put("repositoryId", repositoryId); //$NON-NLS-1$

        final NameValueCollection queryParameters = new NameValueCollection();
        queryParameters.addIfNotNull("$skip", skip); //$NON-NLS-1$
        queryParameters.addIfNotNull("$top", top); //$NON-NLS-1$
        addModelAsQueryParams(queryParameters, searchCriteria);

        final Object httpRequest = super.createRequest(HttpMethod.GET,
                                                       locationId,
                                                       routeValues,
                                                       apiVersion,
                                                       queryParameters,
                                                       APPLICATION_JSON_TYPE);

        return super.sendRequest(httpRequest, new TypeReference<List<GitPush>>() {});
    }

    /** 
     * Retrieves pushes associated with the specified repository.
     * 
     * @param repositoryId 
     *            The id or friendly name of the repository. To use the friendly name, projectId must also be specified.
     * @param skip 
     *            
     * @param top 
     *            
     * @param searchCriteria 
     *            
     * @return List<GitPush>
     */
    public List<GitPush> getPushes(
        final String repositoryId, 
        final Integer skip, 
        final Integer top, 
        final GitPushSearchCriteria searchCriteria) {

        final UUID locationId = UUID.fromString("ea98d07b-3c87-4971-8ede-a613694ffb55"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("repositoryId", repositoryId); //$NON-NLS-1$

        final NameValueCollection queryParameters = new NameValueCollection();
        queryParameters.addIfNotNull("$skip", skip); //$NON-NLS-1$
        queryParameters.addIfNotNull("$top", top); //$NON-NLS-1$
        addModelAsQueryParams(queryParameters, searchCriteria);

        final Object httpRequest = super.createRequest(HttpMethod.GET,
                                                       locationId,
                                                       routeValues,
                                                       apiVersion,
                                                       queryParameters,
                                                       APPLICATION_JSON_TYPE);

        return super.sendRequest(httpRequest, new TypeReference<List<GitPush>>() {});
    }

    /** 
     * Retrieves pushes associated with the specified repository.
     * 
     * @param repositoryId 
     *            The id or friendly name of the repository. To use the friendly name, projectId must also be specified.
     * @param skip 
     *            
     * @param top 
     *            
     * @param searchCriteria 
     *            
     * @return List<GitPush>
     */
    public List<GitPush> getPushes(
        final UUID repositoryId, 
        final Integer skip, 
        final Integer top, 
        final GitPushSearchCriteria searchCriteria) {

        final UUID locationId = UUID.fromString("ea98d07b-3c87-4971-8ede-a613694ffb55"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("repositoryId", repositoryId); //$NON-NLS-1$

        final NameValueCollection queryParameters = new NameValueCollection();
        queryParameters.addIfNotNull("$skip", skip); //$NON-NLS-1$
        queryParameters.addIfNotNull("$top", top); //$NON-NLS-1$
        addModelAsQueryParams(queryParameters, searchCriteria);

        final Object httpRequest = super.createRequest(HttpMethod.GET,
                                                       locationId,
                                                       routeValues,
                                                       apiVersion,
                                                       queryParameters,
                                                       APPLICATION_JSON_TYPE);

        return super.sendRequest(httpRequest, new TypeReference<List<GitPush>>() {});
    }

    /** 
     * Queries the provided repository for its refs and returns them.
     * 
     * @param project 
     *            Project ID or project name
     * @param repositoryId 
     *            The id or friendly name of the repository. To use the friendly name, projectId must also be specified.
     * @param filter 
     *            [optional] A filter to apply to the refs.
     * @param includeLinks 
     *            [optional] Specifies if referenceLinks should be included in the result. default is false.
     * @return List<GitRef>
     */
    public List<GitRef> getRefs(
        final String project, 
        final String repositoryId, 
        final String filter, 
        final Boolean includeLinks) {

        final UUID locationId = UUID.fromString("2d874a60-a811-4f62-9c9f-963a6ea0a55b"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("project", project); //$NON-NLS-1$
        routeValues.put("repositoryId", repositoryId); //$NON-NLS-1$

        final NameValueCollection queryParameters = new NameValueCollection();
        queryParameters.addIfNotEmpty("filter", filter); //$NON-NLS-1$
        queryParameters.addIfNotNull("includeLinks", includeLinks); //$NON-NLS-1$

        final Object httpRequest = super.createRequest(HttpMethod.GET,
                                                       locationId,
                                                       routeValues,
                                                       apiVersion,
                                                       queryParameters,
                                                       APPLICATION_JSON_TYPE);

        return super.sendRequest(httpRequest, new TypeReference<List<GitRef>>() {});
    }

    /** 
     * Queries the provided repository for its refs and returns them.
     * 
     * @param project 
     *            Project ID or project name
     * @param repositoryId 
     *            The id or friendly name of the repository. To use the friendly name, projectId must also be specified.
     * @param filter 
     *            [optional] A filter to apply to the refs.
     * @param includeLinks 
     *            [optional] Specifies if referenceLinks should be included in the result. default is false.
     * @return List<GitRef>
     */
    public List<GitRef> getRefs(
        final String project, 
        final UUID repositoryId, 
        final String filter, 
        final Boolean includeLinks) {

        final UUID locationId = UUID.fromString("2d874a60-a811-4f62-9c9f-963a6ea0a55b"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("project", project); //$NON-NLS-1$
        routeValues.put("repositoryId", repositoryId); //$NON-NLS-1$

        final NameValueCollection queryParameters = new NameValueCollection();
        queryParameters.addIfNotEmpty("filter", filter); //$NON-NLS-1$
        queryParameters.addIfNotNull("includeLinks", includeLinks); //$NON-NLS-1$

        final Object httpRequest = super.createRequest(HttpMethod.GET,
                                                       locationId,
                                                       routeValues,
                                                       apiVersion,
                                                       queryParameters,
                                                       APPLICATION_JSON_TYPE);

        return super.sendRequest(httpRequest, new TypeReference<List<GitRef>>() {});
    }

    /** 
     * Queries the provided repository for its refs and returns them.
     * 
     * @param project 
     *            Project ID
     * @param repositoryId 
     *            The id or friendly name of the repository. To use the friendly name, projectId must also be specified.
     * @param filter 
     *            [optional] A filter to apply to the refs.
     * @param includeLinks 
     *            [optional] Specifies if referenceLinks should be included in the result. default is false.
     * @return List<GitRef>
     */
    public List<GitRef> getRefs(
        final UUID project, 
        final String repositoryId, 
        final String filter, 
        final Boolean includeLinks) {

        final UUID locationId = UUID.fromString("2d874a60-a811-4f62-9c9f-963a6ea0a55b"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("project", project); //$NON-NLS-1$
        routeValues.put("repositoryId", repositoryId); //$NON-NLS-1$

        final NameValueCollection queryParameters = new NameValueCollection();
        queryParameters.addIfNotEmpty("filter", filter); //$NON-NLS-1$
        queryParameters.addIfNotNull("includeLinks", includeLinks); //$NON-NLS-1$

        final Object httpRequest = super.createRequest(HttpMethod.GET,
                                                       locationId,
                                                       routeValues,
                                                       apiVersion,
                                                       queryParameters,
                                                       APPLICATION_JSON_TYPE);

        return super.sendRequest(httpRequest, new TypeReference<List<GitRef>>() {});
    }

    /** 
     * Queries the provided repository for its refs and returns them.
     * 
     * @param project 
     *            Project ID
     * @param repositoryId 
     *            The id or friendly name of the repository. To use the friendly name, projectId must also be specified.
     * @param filter 
     *            [optional] A filter to apply to the refs.
     * @param includeLinks 
     *            [optional] Specifies if referenceLinks should be included in the result. default is false.
     * @return List<GitRef>
     */
    public List<GitRef> getRefs(
        final UUID project, 
        final UUID repositoryId, 
        final String filter, 
        final Boolean includeLinks) {

        final UUID locationId = UUID.fromString("2d874a60-a811-4f62-9c9f-963a6ea0a55b"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("project", project); //$NON-NLS-1$
        routeValues.put("repositoryId", repositoryId); //$NON-NLS-1$

        final NameValueCollection queryParameters = new NameValueCollection();
        queryParameters.addIfNotEmpty("filter", filter); //$NON-NLS-1$
        queryParameters.addIfNotNull("includeLinks", includeLinks); //$NON-NLS-1$

        final Object httpRequest = super.createRequest(HttpMethod.GET,
                                                       locationId,
                                                       routeValues,
                                                       apiVersion,
                                                       queryParameters,
                                                       APPLICATION_JSON_TYPE);

        return super.sendRequest(httpRequest, new TypeReference<List<GitRef>>() {});
    }

    /** 
     * Queries the provided repository for its refs and returns them.
     * 
     * @param repositoryId 
     *            The id or friendly name of the repository. To use the friendly name, projectId must also be specified.
     * @param filter 
     *            [optional] A filter to apply to the refs.
     * @param includeLinks 
     *            [optional] Specifies if referenceLinks should be included in the result. default is false.
     * @return List<GitRef>
     */
    public List<GitRef> getRefs(
        final String repositoryId, 
        final String filter, 
        final Boolean includeLinks) {

        final UUID locationId = UUID.fromString("2d874a60-a811-4f62-9c9f-963a6ea0a55b"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("repositoryId", repositoryId); //$NON-NLS-1$

        final NameValueCollection queryParameters = new NameValueCollection();
        queryParameters.addIfNotEmpty("filter", filter); //$NON-NLS-1$
        queryParameters.addIfNotNull("includeLinks", includeLinks); //$NON-NLS-1$

        final Object httpRequest = super.createRequest(HttpMethod.GET,
                                                       locationId,
                                                       routeValues,
                                                       apiVersion,
                                                       queryParameters,
                                                       APPLICATION_JSON_TYPE);

        return super.sendRequest(httpRequest, new TypeReference<List<GitRef>>() {});
    }

    /** 
     * Queries the provided repository for its refs and returns them.
     * 
     * @param repositoryId 
     *            The id or friendly name of the repository. To use the friendly name, projectId must also be specified.
     * @param filter 
     *            [optional] A filter to apply to the refs.
     * @param includeLinks 
     *            [optional] Specifies if referenceLinks should be included in the result. default is false.
     * @return List<GitRef>
     */
    public List<GitRef> getRefs(
        final UUID repositoryId, 
        final String filter, 
        final Boolean includeLinks) {

        final UUID locationId = UUID.fromString("2d874a60-a811-4f62-9c9f-963a6ea0a55b"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("repositoryId", repositoryId); //$NON-NLS-1$

        final NameValueCollection queryParameters = new NameValueCollection();
        queryParameters.addIfNotEmpty("filter", filter); //$NON-NLS-1$
        queryParameters.addIfNotNull("includeLinks", includeLinks); //$NON-NLS-1$

        final Object httpRequest = super.createRequest(HttpMethod.GET,
                                                       locationId,
                                                       routeValues,
                                                       apiVersion,
                                                       queryParameters,
                                                       APPLICATION_JSON_TYPE);

        return super.sendRequest(httpRequest, new TypeReference<List<GitRef>>() {});
    }

    /** 
     * Creates or updates refs with the given information
     * 
     * @param refUpdates 
     *            List of ref updates to attempt to perform
     * @param repositoryId 
     *            The id or friendly name of the repository. To use the friendly name, projectId must also be specified.
     * @param projectId 
     *            The id of the project.
     * @return List<GitRefUpdateResult>
     */
    public List<GitRefUpdateResult> updateRefs(
        final List<GitRefUpdate> refUpdates, 
        final String repositoryId, 
        final String projectId) {

        final UUID locationId = UUID.fromString("2d874a60-a811-4f62-9c9f-963a6ea0a55b"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("repositoryId", repositoryId); //$NON-NLS-1$

        final NameValueCollection queryParameters = new NameValueCollection();
        queryParameters.addIfNotEmpty("projectId", projectId); //$NON-NLS-1$

        final Object httpRequest = super.createRequest(HttpMethod.POST,
                                                       locationId,
                                                       routeValues,
                                                       apiVersion,
                                                       refUpdates,
                                                       APPLICATION_JSON_TYPE,
                                                       queryParameters,
                                                       APPLICATION_JSON_TYPE);

        return super.sendRequest(httpRequest, new TypeReference<List<GitRefUpdateResult>>() {});
    }

    /** 
     * Creates or updates refs with the given information
     * 
     * @param refUpdates 
     *            List of ref updates to attempt to perform
     * @param repositoryId 
     *            The id or friendly name of the repository. To use the friendly name, projectId must also be specified.
     * @param projectId 
     *            The id of the project.
     * @return List<GitRefUpdateResult>
     */
    public List<GitRefUpdateResult> updateRefs(
        final List<GitRefUpdate> refUpdates, 
        final UUID repositoryId, 
        final String projectId) {

        final UUID locationId = UUID.fromString("2d874a60-a811-4f62-9c9f-963a6ea0a55b"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("repositoryId", repositoryId); //$NON-NLS-1$

        final NameValueCollection queryParameters = new NameValueCollection();
        queryParameters.addIfNotEmpty("projectId", projectId); //$NON-NLS-1$

        final Object httpRequest = super.createRequest(HttpMethod.POST,
                                                       locationId,
                                                       routeValues,
                                                       apiVersion,
                                                       refUpdates,
                                                       APPLICATION_JSON_TYPE,
                                                       queryParameters,
                                                       APPLICATION_JSON_TYPE);

        return super.sendRequest(httpRequest, new TypeReference<List<GitRefUpdateResult>>() {});
    }

    /** 
     * Creates or updates refs with the given information
     * 
     * @param refUpdates 
     *            List of ref updates to attempt to perform
     * @param project 
     *            Project ID or project name
     * @param repositoryId 
     *            The id or friendly name of the repository. To use the friendly name, projectId must also be specified.
     * @param projectId 
     *            The id of the project.
     * @return List<GitRefUpdateResult>
     */
    public List<GitRefUpdateResult> updateRefs(
        final List<GitRefUpdate> refUpdates, 
        final String project, 
        final String repositoryId, 
        final String projectId) {

        final UUID locationId = UUID.fromString("2d874a60-a811-4f62-9c9f-963a6ea0a55b"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("project", project); //$NON-NLS-1$
        routeValues.put("repositoryId", repositoryId); //$NON-NLS-1$

        final NameValueCollection queryParameters = new NameValueCollection();
        queryParameters.addIfNotEmpty("projectId", projectId); //$NON-NLS-1$

        final Object httpRequest = super.createRequest(HttpMethod.POST,
                                                       locationId,
                                                       routeValues,
                                                       apiVersion,
                                                       refUpdates,
                                                       APPLICATION_JSON_TYPE,
                                                       queryParameters,
                                                       APPLICATION_JSON_TYPE);

        return super.sendRequest(httpRequest, new TypeReference<List<GitRefUpdateResult>>() {});
    }

    /** 
     * Creates or updates refs with the given information
     * 
     * @param refUpdates 
     *            List of ref updates to attempt to perform
     * @param project 
     *            Project ID or project name
     * @param repositoryId 
     *            The id or friendly name of the repository. To use the friendly name, projectId must also be specified.
     * @param projectId 
     *            The id of the project.
     * @return List<GitRefUpdateResult>
     */
    public List<GitRefUpdateResult> updateRefs(
        final List<GitRefUpdate> refUpdates, 
        final String project, 
        final UUID repositoryId, 
        final String projectId) {

        final UUID locationId = UUID.fromString("2d874a60-a811-4f62-9c9f-963a6ea0a55b"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("project", project); //$NON-NLS-1$
        routeValues.put("repositoryId", repositoryId); //$NON-NLS-1$

        final NameValueCollection queryParameters = new NameValueCollection();
        queryParameters.addIfNotEmpty("projectId", projectId); //$NON-NLS-1$

        final Object httpRequest = super.createRequest(HttpMethod.POST,
                                                       locationId,
                                                       routeValues,
                                                       apiVersion,
                                                       refUpdates,
                                                       APPLICATION_JSON_TYPE,
                                                       queryParameters,
                                                       APPLICATION_JSON_TYPE);

        return super.sendRequest(httpRequest, new TypeReference<List<GitRefUpdateResult>>() {});
    }

    /** 
     * Creates or updates refs with the given information
     * 
     * @param refUpdates 
     *            List of ref updates to attempt to perform
     * @param project 
     *            Project ID
     * @param repositoryId 
     *            The id or friendly name of the repository. To use the friendly name, projectId must also be specified.
     * @param projectId 
     *            The id of the project.
     * @return List<GitRefUpdateResult>
     */
    public List<GitRefUpdateResult> updateRefs(
        final List<GitRefUpdate> refUpdates, 
        final UUID project, 
        final String repositoryId, 
        final String projectId) {

        final UUID locationId = UUID.fromString("2d874a60-a811-4f62-9c9f-963a6ea0a55b"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("project", project); //$NON-NLS-1$
        routeValues.put("repositoryId", repositoryId); //$NON-NLS-1$

        final NameValueCollection queryParameters = new NameValueCollection();
        queryParameters.addIfNotEmpty("projectId", projectId); //$NON-NLS-1$

        final Object httpRequest = super.createRequest(HttpMethod.POST,
                                                       locationId,
                                                       routeValues,
                                                       apiVersion,
                                                       refUpdates,
                                                       APPLICATION_JSON_TYPE,
                                                       queryParameters,
                                                       APPLICATION_JSON_TYPE);

        return super.sendRequest(httpRequest, new TypeReference<List<GitRefUpdateResult>>() {});
    }

    /** 
     * Creates or updates refs with the given information
     * 
     * @param refUpdates 
     *            List of ref updates to attempt to perform
     * @param project 
     *            Project ID
     * @param repositoryId 
     *            The id or friendly name of the repository. To use the friendly name, projectId must also be specified.
     * @param projectId 
     *            The id of the project.
     * @return List<GitRefUpdateResult>
     */
    public List<GitRefUpdateResult> updateRefs(
        final List<GitRefUpdate> refUpdates, 
        final UUID project, 
        final UUID repositoryId, 
        final String projectId) {

        final UUID locationId = UUID.fromString("2d874a60-a811-4f62-9c9f-963a6ea0a55b"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("project", project); //$NON-NLS-1$
        routeValues.put("repositoryId", repositoryId); //$NON-NLS-1$

        final NameValueCollection queryParameters = new NameValueCollection();
        queryParameters.addIfNotEmpty("projectId", projectId); //$NON-NLS-1$

        final Object httpRequest = super.createRequest(HttpMethod.POST,
                                                       locationId,
                                                       routeValues,
                                                       apiVersion,
                                                       refUpdates,
                                                       APPLICATION_JSON_TYPE,
                                                       queryParameters,
                                                       APPLICATION_JSON_TYPE);

        return super.sendRequest(httpRequest, new TypeReference<List<GitRefUpdateResult>>() {});
    }

    /** 
     * Create a git repository
     * 
     * @param gitRepositoryToCreate 
     *            
     * @return GitRepository
     */
    public GitRepository createRepository(
        final GitRepository gitRepositoryToCreate) {

        final UUID locationId = UUID.fromString("225f7195-f9c7-4d14-ab28-a83f7ff77e1f"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0"); //$NON-NLS-1$

        final Object httpRequest = super.createRequest(HttpMethod.POST,
                                                       locationId,
                                                       apiVersion,
                                                       gitRepositoryToCreate,
                                                       APPLICATION_JSON_TYPE,
                                                       APPLICATION_JSON_TYPE);

        return super.sendRequest(httpRequest, GitRepository.class);
    }

    /** 
     * Create a git repository
     * 
     * @param gitRepositoryToCreate 
     *            
     * @param project 
     *            Project ID or project name
     * @return GitRepository
     */
    public GitRepository createRepository(
        final GitRepository gitRepositoryToCreate, 
        final String project) {

        final UUID locationId = UUID.fromString("225f7195-f9c7-4d14-ab28-a83f7ff77e1f"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("project", project); //$NON-NLS-1$

        final Object httpRequest = super.createRequest(HttpMethod.POST,
                                                       locationId,
                                                       routeValues,
                                                       apiVersion,
                                                       gitRepositoryToCreate,
                                                       APPLICATION_JSON_TYPE,
                                                       APPLICATION_JSON_TYPE);

        return super.sendRequest(httpRequest, GitRepository.class);
    }

    /** 
     * Create a git repository
     * 
     * @param gitRepositoryToCreate 
     *            
     * @param project 
     *            Project ID
     * @return GitRepository
     */
    public GitRepository createRepository(
        final GitRepository gitRepositoryToCreate, 
        final UUID project) {

        final UUID locationId = UUID.fromString("225f7195-f9c7-4d14-ab28-a83f7ff77e1f"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("project", project); //$NON-NLS-1$

        final Object httpRequest = super.createRequest(HttpMethod.POST,
                                                       locationId,
                                                       routeValues,
                                                       apiVersion,
                                                       gitRepositoryToCreate,
                                                       APPLICATION_JSON_TYPE,
                                                       APPLICATION_JSON_TYPE);

        return super.sendRequest(httpRequest, GitRepository.class);
    }

    /** 
     * Delete a git repository
     * 
     * @param repositoryId 
     *            
     */
    public void deleteRepository(
        final UUID repositoryId) {

        final UUID locationId = UUID.fromString("225f7195-f9c7-4d14-ab28-a83f7ff77e1f"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("repositoryId", repositoryId); //$NON-NLS-1$

        final Object httpRequest = super.createRequest(HttpMethod.DELETE,
                                                       locationId,
                                                       routeValues,
                                                       apiVersion,
                                                       APPLICATION_JSON_TYPE);

        super.sendRequest(httpRequest);
    }

    /** 
     * Delete a git repository
     * 
     * @param project 
     *            Project ID or project name
     * @param repositoryId 
     *            
     */
    public void deleteRepository(
        final String project, 
        final UUID repositoryId) {

        final UUID locationId = UUID.fromString("225f7195-f9c7-4d14-ab28-a83f7ff77e1f"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("project", project); //$NON-NLS-1$
        routeValues.put("repositoryId", repositoryId); //$NON-NLS-1$

        final Object httpRequest = super.createRequest(HttpMethod.DELETE,
                                                       locationId,
                                                       routeValues,
                                                       apiVersion,
                                                       APPLICATION_JSON_TYPE);

        super.sendRequest(httpRequest);
    }

    /** 
     * Delete a git repository
     * 
     * @param project 
     *            Project ID
     * @param repositoryId 
     *            
     */
    public void deleteRepository(
        final UUID project, 
        final UUID repositoryId) {

        final UUID locationId = UUID.fromString("225f7195-f9c7-4d14-ab28-a83f7ff77e1f"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("project", project); //$NON-NLS-1$
        routeValues.put("repositoryId", repositoryId); //$NON-NLS-1$

        final Object httpRequest = super.createRequest(HttpMethod.DELETE,
                                                       locationId,
                                                       routeValues,
                                                       apiVersion,
                                                       APPLICATION_JSON_TYPE);

        super.sendRequest(httpRequest);
    }

    /** 
     * Retrieve git repositories.
     * 
     * @param project 
     *            Project ID or project name
     * @param includeLinks 
     *            
     * @return List<GitRepository>
     */
    public List<GitRepository> getRepositories(
        final String project, 
        final Boolean includeLinks) {

        final UUID locationId = UUID.fromString("225f7195-f9c7-4d14-ab28-a83f7ff77e1f"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("project", project); //$NON-NLS-1$

        final NameValueCollection queryParameters = new NameValueCollection();
        queryParameters.addIfNotNull("includeLinks", includeLinks); //$NON-NLS-1$

        final Object httpRequest = super.createRequest(HttpMethod.GET,
                                                       locationId,
                                                       routeValues,
                                                       apiVersion,
                                                       queryParameters,
                                                       APPLICATION_JSON_TYPE);

        return super.sendRequest(httpRequest, new TypeReference<List<GitRepository>>() {});
    }

    /** 
     * Retrieve git repositories.
     * 
     * @param project 
     *            Project ID
     * @param includeLinks 
     *            
     * @return List<GitRepository>
     */
    public List<GitRepository> getRepositories(
        final UUID project, 
        final Boolean includeLinks) {

        final UUID locationId = UUID.fromString("225f7195-f9c7-4d14-ab28-a83f7ff77e1f"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("project", project); //$NON-NLS-1$

        final NameValueCollection queryParameters = new NameValueCollection();
        queryParameters.addIfNotNull("includeLinks", includeLinks); //$NON-NLS-1$

        final Object httpRequest = super.createRequest(HttpMethod.GET,
                                                       locationId,
                                                       routeValues,
                                                       apiVersion,
                                                       queryParameters,
                                                       APPLICATION_JSON_TYPE);

        return super.sendRequest(httpRequest, new TypeReference<List<GitRepository>>() {});
    }

    /** 
     * Retrieve git repositories.
     * 
     * @param includeLinks 
     *            
     * @return List<GitRepository>
     */
    public List<GitRepository> getRepositories(
        final Boolean includeLinks) {

        final UUID locationId = UUID.fromString("225f7195-f9c7-4d14-ab28-a83f7ff77e1f"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0"); //$NON-NLS-1$

        final NameValueCollection queryParameters = new NameValueCollection();
        queryParameters.addIfNotNull("includeLinks", includeLinks); //$NON-NLS-1$

        final Object httpRequest = super.createRequest(HttpMethod.GET,
                                                       locationId,
                                                       apiVersion,
                                                       queryParameters,
                                                       APPLICATION_JSON_TYPE);

        return super.sendRequest(httpRequest, new TypeReference<List<GitRepository>>() {});
    }

    /** 
     * @param repositoryId 
     *            
     * @return GitRepository
     */
    public GitRepository getRepository(
        final String repositoryId) {

        final UUID locationId = UUID.fromString("225f7195-f9c7-4d14-ab28-a83f7ff77e1f"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("repositoryId", repositoryId); //$NON-NLS-1$

        final Object httpRequest = super.createRequest(HttpMethod.GET,
                                                       locationId,
                                                       routeValues,
                                                       apiVersion,
                                                       APPLICATION_JSON_TYPE);

        return super.sendRequest(httpRequest, GitRepository.class);
    }

    /** 
     * @param repositoryId 
     *            
     * @return GitRepository
     */
    public GitRepository getRepository(
        final UUID repositoryId) {

        final UUID locationId = UUID.fromString("225f7195-f9c7-4d14-ab28-a83f7ff77e1f"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("repositoryId", repositoryId); //$NON-NLS-1$

        final Object httpRequest = super.createRequest(HttpMethod.GET,
                                                       locationId,
                                                       routeValues,
                                                       apiVersion,
                                                       APPLICATION_JSON_TYPE);

        return super.sendRequest(httpRequest, GitRepository.class);
    }

    /** 
     * @param project 
     *            Project ID or project name
     * @param repositoryId 
     *            
     * @return GitRepository
     */
    public GitRepository getRepository(
        final String project, 
        final String repositoryId) {

        final UUID locationId = UUID.fromString("225f7195-f9c7-4d14-ab28-a83f7ff77e1f"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("project", project); //$NON-NLS-1$
        routeValues.put("repositoryId", repositoryId); //$NON-NLS-1$

        final Object httpRequest = super.createRequest(HttpMethod.GET,
                                                       locationId,
                                                       routeValues,
                                                       apiVersion,
                                                       APPLICATION_JSON_TYPE);

        return super.sendRequest(httpRequest, GitRepository.class);
    }

    /** 
     * @param project 
     *            Project ID or project name
     * @param repositoryId 
     *            
     * @return GitRepository
     */
    public GitRepository getRepository(
        final String project, 
        final UUID repositoryId) {

        final UUID locationId = UUID.fromString("225f7195-f9c7-4d14-ab28-a83f7ff77e1f"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("project", project); //$NON-NLS-1$
        routeValues.put("repositoryId", repositoryId); //$NON-NLS-1$

        final Object httpRequest = super.createRequest(HttpMethod.GET,
                                                       locationId,
                                                       routeValues,
                                                       apiVersion,
                                                       APPLICATION_JSON_TYPE);

        return super.sendRequest(httpRequest, GitRepository.class);
    }

    /** 
     * @param project 
     *            Project ID
     * @param repositoryId 
     *            
     * @return GitRepository
     */
    public GitRepository getRepository(
        final UUID project, 
        final String repositoryId) {

        final UUID locationId = UUID.fromString("225f7195-f9c7-4d14-ab28-a83f7ff77e1f"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("project", project); //$NON-NLS-1$
        routeValues.put("repositoryId", repositoryId); //$NON-NLS-1$

        final Object httpRequest = super.createRequest(HttpMethod.GET,
                                                       locationId,
                                                       routeValues,
                                                       apiVersion,
                                                       APPLICATION_JSON_TYPE);

        return super.sendRequest(httpRequest, GitRepository.class);
    }

    /** 
     * @param project 
     *            Project ID
     * @param repositoryId 
     *            
     * @return GitRepository
     */
    public GitRepository getRepository(
        final UUID project, 
        final UUID repositoryId) {

        final UUID locationId = UUID.fromString("225f7195-f9c7-4d14-ab28-a83f7ff77e1f"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("project", project); //$NON-NLS-1$
        routeValues.put("repositoryId", repositoryId); //$NON-NLS-1$

        final Object httpRequest = super.createRequest(HttpMethod.GET,
                                                       locationId,
                                                       routeValues,
                                                       apiVersion,
                                                       APPLICATION_JSON_TYPE);

        return super.sendRequest(httpRequest, GitRepository.class);
    }

    /** 
     * Updates the Git repository with the single populated change in the specified repository information.
     * 
     * @param newRepositoryInfo 
     *            
     * @param repositoryId 
     *            
     * @return GitRepository
     */
    public GitRepository updateRepository(
        final GitRepository newRepositoryInfo, 
        final UUID repositoryId) {

        final UUID locationId = UUID.fromString("225f7195-f9c7-4d14-ab28-a83f7ff77e1f"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("repositoryId", repositoryId); //$NON-NLS-1$

        final Object httpRequest = super.createRequest(HttpMethod.PATCH,
                                                       locationId,
                                                       routeValues,
                                                       apiVersion,
                                                       newRepositoryInfo,
                                                       APPLICATION_JSON_TYPE,
                                                       APPLICATION_JSON_TYPE);

        return super.sendRequest(httpRequest, GitRepository.class);
    }

    /** 
     * Updates the Git repository with the single populated change in the specified repository information.
     * 
     * @param newRepositoryInfo 
     *            
     * @param project 
     *            Project ID or project name
     * @param repositoryId 
     *            
     * @return GitRepository
     */
    public GitRepository updateRepository(
        final GitRepository newRepositoryInfo, 
        final String project, 
        final UUID repositoryId) {

        final UUID locationId = UUID.fromString("225f7195-f9c7-4d14-ab28-a83f7ff77e1f"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("project", project); //$NON-NLS-1$
        routeValues.put("repositoryId", repositoryId); //$NON-NLS-1$

        final Object httpRequest = super.createRequest(HttpMethod.PATCH,
                                                       locationId,
                                                       routeValues,
                                                       apiVersion,
                                                       newRepositoryInfo,
                                                       APPLICATION_JSON_TYPE,
                                                       APPLICATION_JSON_TYPE);

        return super.sendRequest(httpRequest, GitRepository.class);
    }

    /** 
     * Updates the Git repository with the single populated change in the specified repository information.
     * 
     * @param newRepositoryInfo 
     *            
     * @param project 
     *            Project ID
     * @param repositoryId 
     *            
     * @return GitRepository
     */
    public GitRepository updateRepository(
        final GitRepository newRepositoryInfo, 
        final UUID project, 
        final UUID repositoryId) {

        final UUID locationId = UUID.fromString("225f7195-f9c7-4d14-ab28-a83f7ff77e1f"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("project", project); //$NON-NLS-1$
        routeValues.put("repositoryId", repositoryId); //$NON-NLS-1$

        final Object httpRequest = super.createRequest(HttpMethod.PATCH,
                                                       locationId,
                                                       routeValues,
                                                       apiVersion,
                                                       newRepositoryInfo,
                                                       APPLICATION_JSON_TYPE,
                                                       APPLICATION_JSON_TYPE);

        return super.sendRequest(httpRequest, GitRepository.class);
    }

    /** 
     * @param project 
     *            Project ID or project name
     * @param repositoryId 
     *            
     * @param sha1 
     *            
     * @param projectId 
     *            
     * @param recursive 
     *            
     * @param fileName 
     *            
     * @return GitTreeRef
     */
    public GitTreeRef getTree(
        final String project, 
        final String repositoryId, 
        final String sha1, 
        final String projectId, 
        final Boolean recursive, 
        final String fileName) {

        final UUID locationId = UUID.fromString("729f6437-6f92-44ec-8bee-273a7111063c"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("project", project); //$NON-NLS-1$
        routeValues.put("repositoryId", repositoryId); //$NON-NLS-1$
        routeValues.put("sha1", sha1); //$NON-NLS-1$

        final NameValueCollection queryParameters = new NameValueCollection();
        queryParameters.addIfNotEmpty("projectId", projectId); //$NON-NLS-1$
        queryParameters.addIfNotNull("recursive", recursive); //$NON-NLS-1$
        queryParameters.addIfNotEmpty("fileName", fileName); //$NON-NLS-1$

        final Object httpRequest = super.createRequest(HttpMethod.GET,
                                                       locationId,
                                                       routeValues,
                                                       apiVersion,
                                                       queryParameters,
                                                       APPLICATION_JSON_TYPE);

        return super.sendRequest(httpRequest, GitTreeRef.class);
    }

    /** 
     * @param project 
     *            Project ID or project name
     * @param repositoryId 
     *            
     * @param sha1 
     *            
     * @param projectId 
     *            
     * @param recursive 
     *            
     * @param fileName 
     *            
     * @return GitTreeRef
     */
    public GitTreeRef getTree(
        final String project, 
        final UUID repositoryId, 
        final String sha1, 
        final String projectId, 
        final Boolean recursive, 
        final String fileName) {

        final UUID locationId = UUID.fromString("729f6437-6f92-44ec-8bee-273a7111063c"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("project", project); //$NON-NLS-1$
        routeValues.put("repositoryId", repositoryId); //$NON-NLS-1$
        routeValues.put("sha1", sha1); //$NON-NLS-1$

        final NameValueCollection queryParameters = new NameValueCollection();
        queryParameters.addIfNotEmpty("projectId", projectId); //$NON-NLS-1$
        queryParameters.addIfNotNull("recursive", recursive); //$NON-NLS-1$
        queryParameters.addIfNotEmpty("fileName", fileName); //$NON-NLS-1$

        final Object httpRequest = super.createRequest(HttpMethod.GET,
                                                       locationId,
                                                       routeValues,
                                                       apiVersion,
                                                       queryParameters,
                                                       APPLICATION_JSON_TYPE);

        return super.sendRequest(httpRequest, GitTreeRef.class);
    }

    /** 
     * @param project 
     *            Project ID
     * @param repositoryId 
     *            
     * @param sha1 
     *            
     * @param projectId 
     *            
     * @param recursive 
     *            
     * @param fileName 
     *            
     * @return GitTreeRef
     */
    public GitTreeRef getTree(
        final UUID project, 
        final String repositoryId, 
        final String sha1, 
        final String projectId, 
        final Boolean recursive, 
        final String fileName) {

        final UUID locationId = UUID.fromString("729f6437-6f92-44ec-8bee-273a7111063c"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("project", project); //$NON-NLS-1$
        routeValues.put("repositoryId", repositoryId); //$NON-NLS-1$
        routeValues.put("sha1", sha1); //$NON-NLS-1$

        final NameValueCollection queryParameters = new NameValueCollection();
        queryParameters.addIfNotEmpty("projectId", projectId); //$NON-NLS-1$
        queryParameters.addIfNotNull("recursive", recursive); //$NON-NLS-1$
        queryParameters.addIfNotEmpty("fileName", fileName); //$NON-NLS-1$

        final Object httpRequest = super.createRequest(HttpMethod.GET,
                                                       locationId,
                                                       routeValues,
                                                       apiVersion,
                                                       queryParameters,
                                                       APPLICATION_JSON_TYPE);

        return super.sendRequest(httpRequest, GitTreeRef.class);
    }

    /** 
     * @param project 
     *            Project ID
     * @param repositoryId 
     *            
     * @param sha1 
     *            
     * @param projectId 
     *            
     * @param recursive 
     *            
     * @param fileName 
     *            
     * @return GitTreeRef
     */
    public GitTreeRef getTree(
        final UUID project, 
        final UUID repositoryId, 
        final String sha1, 
        final String projectId, 
        final Boolean recursive, 
        final String fileName) {

        final UUID locationId = UUID.fromString("729f6437-6f92-44ec-8bee-273a7111063c"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("project", project); //$NON-NLS-1$
        routeValues.put("repositoryId", repositoryId); //$NON-NLS-1$
        routeValues.put("sha1", sha1); //$NON-NLS-1$

        final NameValueCollection queryParameters = new NameValueCollection();
        queryParameters.addIfNotEmpty("projectId", projectId); //$NON-NLS-1$
        queryParameters.addIfNotNull("recursive", recursive); //$NON-NLS-1$
        queryParameters.addIfNotEmpty("fileName", fileName); //$NON-NLS-1$

        final Object httpRequest = super.createRequest(HttpMethod.GET,
                                                       locationId,
                                                       routeValues,
                                                       apiVersion,
                                                       queryParameters,
                                                       APPLICATION_JSON_TYPE);

        return super.sendRequest(httpRequest, GitTreeRef.class);
    }

    /** 
     * @param repositoryId 
     *            
     * @param sha1 
     *            
     * @param projectId 
     *            
     * @param recursive 
     *            
     * @param fileName 
     *            
     * @return GitTreeRef
     */
    public GitTreeRef getTree(
        final String repositoryId, 
        final String sha1, 
        final String projectId, 
        final Boolean recursive, 
        final String fileName) {

        final UUID locationId = UUID.fromString("729f6437-6f92-44ec-8bee-273a7111063c"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("repositoryId", repositoryId); //$NON-NLS-1$
        routeValues.put("sha1", sha1); //$NON-NLS-1$

        final NameValueCollection queryParameters = new NameValueCollection();
        queryParameters.addIfNotEmpty("projectId", projectId); //$NON-NLS-1$
        queryParameters.addIfNotNull("recursive", recursive); //$NON-NLS-1$
        queryParameters.addIfNotEmpty("fileName", fileName); //$NON-NLS-1$

        final Object httpRequest = super.createRequest(HttpMethod.GET,
                                                       locationId,
                                                       routeValues,
                                                       apiVersion,
                                                       queryParameters,
                                                       APPLICATION_JSON_TYPE);

        return super.sendRequest(httpRequest, GitTreeRef.class);
    }

    /** 
     * @param repositoryId 
     *            
     * @param sha1 
     *            
     * @param projectId 
     *            
     * @param recursive 
     *            
     * @param fileName 
     *            
     * @return GitTreeRef
     */
    public GitTreeRef getTree(
        final UUID repositoryId, 
        final String sha1, 
        final String projectId, 
        final Boolean recursive, 
        final String fileName) {

        final UUID locationId = UUID.fromString("729f6437-6f92-44ec-8bee-273a7111063c"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("repositoryId", repositoryId); //$NON-NLS-1$
        routeValues.put("sha1", sha1); //$NON-NLS-1$

        final NameValueCollection queryParameters = new NameValueCollection();
        queryParameters.addIfNotEmpty("projectId", projectId); //$NON-NLS-1$
        queryParameters.addIfNotNull("recursive", recursive); //$NON-NLS-1$
        queryParameters.addIfNotEmpty("fileName", fileName); //$NON-NLS-1$

        final Object httpRequest = super.createRequest(HttpMethod.GET,
                                                       locationId,
                                                       routeValues,
                                                       apiVersion,
                                                       queryParameters,
                                                       APPLICATION_JSON_TYPE);

        return super.sendRequest(httpRequest, GitTreeRef.class);
    }

    /** 
     * @param project 
     *            Project ID or project name
     * @param repositoryId 
     *            
     * @param sha1 
     *            
     * @param projectId 
     *            
     * @param recursive 
     *            
     * @param fileName 
     *            
     * @return InputStream
     */
    public InputStream getTreeZip(
        final String project, 
        final String repositoryId, 
        final String sha1, 
        final String projectId, 
        final Boolean recursive, 
        final String fileName) {

        final UUID locationId = UUID.fromString("729f6437-6f92-44ec-8bee-273a7111063c"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("project", project); //$NON-NLS-1$
        routeValues.put("repositoryId", repositoryId); //$NON-NLS-1$
        routeValues.put("sha1", sha1); //$NON-NLS-1$

        final NameValueCollection queryParameters = new NameValueCollection();
        queryParameters.addIfNotEmpty("projectId", projectId); //$NON-NLS-1$
        queryParameters.addIfNotNull("recursive", recursive); //$NON-NLS-1$
        queryParameters.addIfNotEmpty("fileName", fileName); //$NON-NLS-1$

        final Object httpRequest = super.createRequest(HttpMethod.GET,
                                                       locationId,
                                                       routeValues,
                                                       apiVersion,
                                                       queryParameters,
                                                       APPLICATION_ZIP_TYPE);

        return super.sendRequest(httpRequest, InputStream.class);
    }

    /** 
     * @param project 
     *            Project ID or project name
     * @param repositoryId 
     *            
     * @param sha1 
     *            
     * @param projectId 
     *            
     * @param recursive 
     *            
     * @param fileName 
     *            
     * @return InputStream
     */
    public InputStream getTreeZip(
        final String project, 
        final UUID repositoryId, 
        final String sha1, 
        final String projectId, 
        final Boolean recursive, 
        final String fileName) {

        final UUID locationId = UUID.fromString("729f6437-6f92-44ec-8bee-273a7111063c"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("project", project); //$NON-NLS-1$
        routeValues.put("repositoryId", repositoryId); //$NON-NLS-1$
        routeValues.put("sha1", sha1); //$NON-NLS-1$

        final NameValueCollection queryParameters = new NameValueCollection();
        queryParameters.addIfNotEmpty("projectId", projectId); //$NON-NLS-1$
        queryParameters.addIfNotNull("recursive", recursive); //$NON-NLS-1$
        queryParameters.addIfNotEmpty("fileName", fileName); //$NON-NLS-1$

        final Object httpRequest = super.createRequest(HttpMethod.GET,
                                                       locationId,
                                                       routeValues,
                                                       apiVersion,
                                                       queryParameters,
                                                       APPLICATION_ZIP_TYPE);

        return super.sendRequest(httpRequest, InputStream.class);
    }

    /** 
     * @param project 
     *            Project ID
     * @param repositoryId 
     *            
     * @param sha1 
     *            
     * @param projectId 
     *            
     * @param recursive 
     *            
     * @param fileName 
     *            
     * @return InputStream
     */
    public InputStream getTreeZip(
        final UUID project, 
        final String repositoryId, 
        final String sha1, 
        final String projectId, 
        final Boolean recursive, 
        final String fileName) {

        final UUID locationId = UUID.fromString("729f6437-6f92-44ec-8bee-273a7111063c"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("project", project); //$NON-NLS-1$
        routeValues.put("repositoryId", repositoryId); //$NON-NLS-1$
        routeValues.put("sha1", sha1); //$NON-NLS-1$

        final NameValueCollection queryParameters = new NameValueCollection();
        queryParameters.addIfNotEmpty("projectId", projectId); //$NON-NLS-1$
        queryParameters.addIfNotNull("recursive", recursive); //$NON-NLS-1$
        queryParameters.addIfNotEmpty("fileName", fileName); //$NON-NLS-1$

        final Object httpRequest = super.createRequest(HttpMethod.GET,
                                                       locationId,
                                                       routeValues,
                                                       apiVersion,
                                                       queryParameters,
                                                       APPLICATION_ZIP_TYPE);

        return super.sendRequest(httpRequest, InputStream.class);
    }

    /** 
     * @param project 
     *            Project ID
     * @param repositoryId 
     *            
     * @param sha1 
     *            
     * @param projectId 
     *            
     * @param recursive 
     *            
     * @param fileName 
     *            
     * @return InputStream
     */
    public InputStream getTreeZip(
        final UUID project, 
        final UUID repositoryId, 
        final String sha1, 
        final String projectId, 
        final Boolean recursive, 
        final String fileName) {

        final UUID locationId = UUID.fromString("729f6437-6f92-44ec-8bee-273a7111063c"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("project", project); //$NON-NLS-1$
        routeValues.put("repositoryId", repositoryId); //$NON-NLS-1$
        routeValues.put("sha1", sha1); //$NON-NLS-1$

        final NameValueCollection queryParameters = new NameValueCollection();
        queryParameters.addIfNotEmpty("projectId", projectId); //$NON-NLS-1$
        queryParameters.addIfNotNull("recursive", recursive); //$NON-NLS-1$
        queryParameters.addIfNotEmpty("fileName", fileName); //$NON-NLS-1$

        final Object httpRequest = super.createRequest(HttpMethod.GET,
                                                       locationId,
                                                       routeValues,
                                                       apiVersion,
                                                       queryParameters,
                                                       APPLICATION_ZIP_TYPE);

        return super.sendRequest(httpRequest, InputStream.class);
    }

    /** 
     * @param repositoryId 
     *            
     * @param sha1 
     *            
     * @param projectId 
     *            
     * @param recursive 
     *            
     * @param fileName 
     *            
     * @return InputStream
     */
    public InputStream getTreeZip(
        final String repositoryId, 
        final String sha1, 
        final String projectId, 
        final Boolean recursive, 
        final String fileName) {

        final UUID locationId = UUID.fromString("729f6437-6f92-44ec-8bee-273a7111063c"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("repositoryId", repositoryId); //$NON-NLS-1$
        routeValues.put("sha1", sha1); //$NON-NLS-1$

        final NameValueCollection queryParameters = new NameValueCollection();
        queryParameters.addIfNotEmpty("projectId", projectId); //$NON-NLS-1$
        queryParameters.addIfNotNull("recursive", recursive); //$NON-NLS-1$
        queryParameters.addIfNotEmpty("fileName", fileName); //$NON-NLS-1$

        final Object httpRequest = super.createRequest(HttpMethod.GET,
                                                       locationId,
                                                       routeValues,
                                                       apiVersion,
                                                       queryParameters,
                                                       APPLICATION_ZIP_TYPE);

        return super.sendRequest(httpRequest, InputStream.class);
    }

    /** 
     * @param repositoryId 
     *            
     * @param sha1 
     *            
     * @param projectId 
     *            
     * @param recursive 
     *            
     * @param fileName 
     *            
     * @return InputStream
     */
    public InputStream getTreeZip(
        final UUID repositoryId, 
        final String sha1, 
        final String projectId, 
        final Boolean recursive, 
        final String fileName) {

        final UUID locationId = UUID.fromString("729f6437-6f92-44ec-8bee-273a7111063c"); //$NON-NLS-1$
        final ApiResourceVersion apiVersion = new ApiResourceVersion("2.0"); //$NON-NLS-1$

        final Map<String, Object> routeValues = new HashMap<String, Object>();
        routeValues.put("repositoryId", repositoryId); //$NON-NLS-1$
        routeValues.put("sha1", sha1); //$NON-NLS-1$

        final NameValueCollection queryParameters = new NameValueCollection();
        queryParameters.addIfNotEmpty("projectId", projectId); //$NON-NLS-1$
        queryParameters.addIfNotNull("recursive", recursive); //$NON-NLS-1$
        queryParameters.addIfNotEmpty("fileName", fileName); //$NON-NLS-1$

        final Object httpRequest = super.createRequest(HttpMethod.GET,
                                                       locationId,
                                                       routeValues,
                                                       apiVersion,
                                                       queryParameters,
                                                       APPLICATION_ZIP_TYPE);

        return super.sendRequest(httpRequest, InputStream.class);
    }
}
