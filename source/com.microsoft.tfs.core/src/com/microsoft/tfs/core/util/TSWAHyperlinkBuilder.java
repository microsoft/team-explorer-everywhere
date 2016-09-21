// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.util;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.microsoft.tfs.core.Messages;
import com.microsoft.tfs.core.TFSConfigurationServer;
import com.microsoft.tfs.core.TFSTeamProjectCollection;
import com.microsoft.tfs.core.artifact.ArtifactID;
import com.microsoft.tfs.core.clients.build.IBuildDetail;
import com.microsoft.tfs.core.clients.framework.ServerDataProvider;
import com.microsoft.tfs.core.clients.framework.internal.ServiceInterfaceIdentifiers;
import com.microsoft.tfs.core.clients.framework.internal.ServiceInterfaceNames;
import com.microsoft.tfs.core.clients.framework.location.AccessMapping;
import com.microsoft.tfs.core.clients.framework.location.ILocationService;
import com.microsoft.tfs.core.clients.framework.location.LocationService;
import com.microsoft.tfs.core.clients.framework.location.ServiceDefinition;
import com.microsoft.tfs.core.clients.framework.location.internal.AccessMappingMonikers;
import com.microsoft.tfs.core.clients.registration.RegistrationClient;
import com.microsoft.tfs.core.clients.registration.RegistrationEntry;
import com.microsoft.tfs.core.clients.registration.ServiceInterface;
import com.microsoft.tfs.core.clients.registration.ToolNames;
import com.microsoft.tfs.core.clients.versioncontrol.path.ServerPath;
import com.microsoft.tfs.core.exceptions.NotSupportedException;
import com.microsoft.tfs.core.exceptions.TECoreException;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.GUID;
import com.microsoft.tfs.util.StringUtil;
import com.microsoft.tfs.util.URLEncode;

/**
 * Utility class for building links to TFS resources via web access.
 *
 * @since TEE-SDK-10.1
 */
public class TSWAHyperlinkBuilder {
    private final boolean isCompatibleMode;
    private final boolean displayInEmbeddedBrowser;
    private final RegistrationClient registrationClient;
    private final ILocationService locationService;
    private final ILocationService collectionLocationService;
    private final GUID collectionId;
    private final TFSTeamProjectCollection collection;

    private static final Log log = LogFactory.getLog(TSWAHyperlinkBuilder.class);

    /**
     * Creates a {@link TSWAHyperlinkBuilder} for the given
     * {@link TFSTeamProjectCollection}.
     *
     * @param collection
     *        the collection to build links to resources in (must not be
     *        <code>null</code>)
     */
    public TSWAHyperlinkBuilder(final TFSTeamProjectCollection collection) {
        this(collection, false);
    }

    /**
     * Creates a {@link TSWAHyperlinkBuilder} for the given
     * {@link TFSTeamProjectCollection}.
     *
     * @param collection
     *        the collection to build links to resources in (must not be
     *        <code>null</code>)
     * @param displayInEmbeddedBrowser
     *        true if url will be rendered as hosted content within TEE. When
     *        true, the URL contains a query parameter which instructs web
     *        access to render a page differently with knowledge that it's being
     *        hosted in another app).
     */
    public TSWAHyperlinkBuilder(final TFSTeamProjectCollection collection, final boolean displayInEmbeddedBrowser) {
        Check.notNull(collection, "collection"); //$NON-NLS-1$

        final TFSConfigurationServer configurationServer = collection.getConfigurationServer();
        registrationClient = collection.getRegistrationClient();
        collectionId = collection.getInstanceID();
        if (configurationServer == null) {
            // For Orcas or below, configuration server is null.
            isCompatibleMode = true;
            locationService = null;
        } else {
            isCompatibleMode = false;
            locationService = configurationServer.getLocationService();
        }
        this.collection = collection;
        this.collectionLocationService = collection.getCollectionLocationService();
        this.displayInEmbeddedBrowser = displayInEmbeddedBrowser;
    }

    /**
     * Gets the Web Access home page Url.
     *
     * @return Web Access home page url.
     */
    public URI getHomeURL() {
        return getHomeURL(AccessMappingMonikers.PUBLIC_ACCESS_MAPPING);
    }

    /**
     * Gets the Web Access home page Url.
     *
     * @param accessMappingMoniker
     *        A moniker for the desired access mapping.
     * @return Web Access home page url.
     */
    public URI getHomeURL(final String accessMappingMoniker) {
        if (isCompatibleMode) {
            // TfsAdminUtil in Orcas SP1 does not register root TSWA location.
            // TfsAdminUtil appends work item editor url as "/wi.aspx" to the
            // root location and registers it.

            String result = buildURL(ServiceInterfaceNames.TSWA_WORK_ITEM_EDITOR, accessMappingMoniker).toString();
            final int pos = result.lastIndexOf('/');

            if (pos > 0) {
                result = result.substring(0, pos);
            }

            try {
                return new URI(result);
            } catch (final URISyntaxException e) {
                throw new TECoreException(MessageFormat.format("Unable to create URI from \"{0}\"", result), e); //$NON-NLS-1$
            }
        } else {
            return buildURL(ServiceInterfaceNames.TSWA_HOME, accessMappingMoniker);
        }
    }

    /**
     * Gets Team Web Access home url for a specified project.
     *
     * @param projectUri
     *        Uri of the project to be selected in the home page of Team Web
     *        Access.
     * @return Team Web Access home url for a specified project.
     */
    public URI getHomeURL(final URI projectUri) {
        return getHomeUrl(projectUri, AccessMappingMonikers.PUBLIC_ACCESS_MAPPING);
    }

    /**
     * Gets Team Web Access home url for a specified project.
     *
     * @param projectUri
     *        Uri of the project to be selected in the home page of Team Web
     *        Access.
     * @param accessMappingMoniker
     *        A moniker for the desired access mapping.
     * @return Team Web Access home url for a specified project.
     */
    public URI getHomeUrl(final URI projectUri, final String accessMappingMoniker) {
        Check.notNull(projectUri, "projectUri"); //$NON-NLS-1$
        return getPageURL(accessMappingMoniker, "index.aspx", formatQueryString(projectUri.toString(), new String[0])); //$NON-NLS-1$
    }

    /**
     * Gets a shelveset details url.
     *
     *
     * @param shelvesetName
     *        A shelveset name.
     * @param shelvesetOwner
     *        A shelveset owner.
     * @return A shelveset detail url.
     */
    public URI getShelvesetDetailsURL(final String shelvesetName, final String shelvesetOwner) {
        return getShelvesetDetailsURL(shelvesetName, shelvesetOwner, AccessMappingMonikers.PUBLIC_ACCESS_MAPPING);
    }

    /**
     * Gets a shelveset details url.
     *
     *
     * @param shelvesetName
     *        A shelveset name.
     * @param shelvesetOwner
     *        A shelveset owner.
     * @param accessMappingMoniker
     *        A moniker for the desired access mapping
     * @return A shelveset detail url.
     */
    public URI getShelvesetDetailsURL(
        final String shelvesetName,
        final String shelvesetOwner,
        final String accessMappingMoniker) {
        Check.notNullOrEmpty(shelvesetName, "shelvesetName"); //$NON-NLS-1$
        Check.notNullOrEmpty(shelvesetOwner, "shelvesetOwner"); //$NON-NLS-1$

        if (isCompatibleMode) {
            return getPageURL(
                accessMappingMoniker,
                "ss.aspx", //$NON-NLS-1$
                formatQueryString("ss", shelvesetName + ";" + shelvesetOwner)); //$NON-NLS-1$ //$NON-NLS-2$
        }

        return buildURL(ServiceInterfaceNames.TSWA_VIEW_SHELVESET_DETAILS, accessMappingMoniker, new String[] {
            "shelvesetName", //$NON-NLS-1$
            shelvesetName,
            "shelvesetOwner", //$NON-NLS-1$
            shelvesetOwner
        }, null);
    }

    /**
     * Gets a New Work Item URL.
     *
     *
     * @param projectUri
     *        The team project for the new work item.
     * @param workItemType
     *        The type of the new work item.
     * @return A New Work Item URL.
     */
    public URI getNewWorkItemURL(final String projectUri, final String workItemType, final int titleID) {
        return getNewWorkItemURL(projectUri, workItemType, titleID, AccessMappingMonikers.PUBLIC_ACCESS_MAPPING);
    }

    /**
     * Gets a New Work Item URL.
     *
     *
     * @param projectUri
     *        The team project for the new work item.
     * @param workItemType
     *        The type of the new work item.
     * @param accessMappingMoniker
     *        A moniker for the desired access mapping.
     * @return A New Work Item URL.
     */
    public URI getNewWorkItemURL(
        final String projectUri,
        final String workItemType,
        final int titleID,
        final String accessMappingMoniker) {
        final String[] replacementArgs = new String[] {
            "projectUri", //$NON-NLS-1$
            projectUri,
            "workItemType", //$NON-NLS-1$
            workItemType,
        };

        final List<String> additionalQueryParameters = new ArrayList<String>();
        if (titleID > 0) {
            additionalQueryParameters.add(MessageFormat.format("titleId={0}", titleID)); //$NON-NLS-1$
        }

        return buildURL(
            ServiceInterfaceNames.TSWA_CREATE_WORK_ITEM,
            accessMappingMoniker,
            replacementArgs,
            additionalQueryParameters);
    }

    /**
     * Gets a Work Item Editor Url.
     *
     *
     * @param workItemId
     *        A workitem id.
     * @return A Work Item Editor url.
     */
    public URI getWorkItemEditorURL(final int workItemId) {
        return getWorkItemEditorURL(workItemId, AccessMappingMonikers.PUBLIC_ACCESS_MAPPING);
    }

    /**
     * Gets a Work Item Editor Url.
     *
     * @param workItemId
     *        A workitem id.
     * @param accessMappingMoniker
     *        A moniker for the desired access mapping.
     * @return A Work Item Editor url.
     */
    public URI getWorkItemEditorURL(final int workItemId, final String accessMappingMoniker) {
        if (workItemId < 1) {
            throw new IndexOutOfBoundsException(MessageFormat.format(
                "The value {0} is outside of the allowed range.", //$NON-NLS-1$
                Integer.toString(workItemId)));
        }

        return buildURL(
            ServiceInterfaceNames.TSWA_OPEN_WORK_ITEM,
            accessMappingMoniker,
            "workItemId", //$NON-NLS-1$
            Integer.toString(workItemId),
            null);
    }

    /**
     * Gets a changeset URL.
     *
     * @param changesetId
     *        A changeset id.
     * @return a changeset URL
     */
    public URI getChangesetURL(final int changesetId) {
        return getChangesetURL(changesetId, AccessMappingMonikers.PUBLIC_ACCESS_MAPPING);
    }

    /**
     * Gets a changeset URL.
     *
     * @param changesetId
     *        A changeset id.
     * @param accessMappingMoniker
     *        A moniker for the desired access mapping.
     * @return a changeset URL
     */
    public URI getChangesetURL(final int changesetId, final String accessMappingMoniker) {
        if (changesetId < 1) {
            throw new IndexOutOfBoundsException(MessageFormat.format(
                "The value {0} is outside of the allowed range.", //$NON-NLS-1$
                Integer.toString(changesetId)));
        }

        return buildURL(
            ServiceInterfaceNames.TSWA_VIEW_CHANGESET_DETAILS,
            accessMappingMoniker,
            "changesetId", //$NON-NLS-1$
            Integer.toString(changesetId),
            null);
    }

    /**
     * Gets a Work Item Query Results Url.
     *
     * @param projectUri
     *        URI of the project that this query is in
     * @param queryPath
     *        Path of the query to run (e.g. ProjectName/My Queries/Query1).
     * @return A Query Results url.
     */
    public URI getWorkItemQueryResultsURL(final String projectUri, final String queryPath) {
        return getWorkItemQueryResultsURL(projectUri, queryPath, AccessMappingMonikers.PUBLIC_ACCESS_MAPPING);
    }

    /**
     * Gets a Work Item Query Results Url.
     *
     * @param projectUri
     *        URI of the project that this query is in
     * @param queryPath
     *        Path of the query to run (e.g. ProjectName/My Queries/Query1).
     * @param accessMappingMoniker
     *        A moniker for the desired access mapping.
     * @return A Work Item Query Results url.
     */
    public URI getWorkItemQueryResultsURL(
        final String projectUri,
        final String queryPath,
        final String accessMappingMoniker) {
        if (queryPath == null || queryPath.length() == 0) {
            throw new IllegalArgumentException("The queryPath cannot be null or empty."); //$NON-NLS-1$
        }

        if (isCompatibleMode || projectUri == null) {
            final String query = formatQueryString(projectUri, "path", queryPath); //$NON-NLS-1$
            return getPageURL(accessMappingMoniker, "qr.aspx", query); //$NON-NLS-1$
        } else {
            return buildURL(ServiceInterfaceNames.TSWA_VIEW_SERVER_QUERY_RESULTS, accessMappingMoniker, new String[] {
                "projectUri", //$NON-NLS-1$
                projectUri,
                "storedQueryPath", //$NON-NLS-1$
                queryPath
            }, null);
        }
    }

    /**
     * Gets a Query Editor Url.
     *
     * @param projectUri
     *        URI of the project that this query is in
     * @param queryPath
     *        Path of the query to run (e.g. ProjectName/My Queries/Query1).
     * @return A Query Editor url.
     */
    public URI getWorkItemQueryEditorURL(final String projectUri, final String queryPath) {
        return getWorkItemQueryEditorURL(projectUri, queryPath, AccessMappingMonikers.PUBLIC_ACCESS_MAPPING);
    }

    /**
     * Gets a Query Editor Url.
     *
     * @param projectUri
     *        URI of the project that this query is in
     * @param queryPath
     *        Path of the query to run (e.g. ProjectName/My Queries/Query1).
     * @param accessMappingMoniker
     *        A moniker for the desired access mapping.
     * @return A Query Editor url.
     */
    public URI getWorkItemQueryEditorURL(
        final String projectUri,
        final String queryPath,
        final String accessMappingMoniker) {
        if (queryPath == null || queryPath.length() == 0) {
            throw new IllegalArgumentException("The queryPath cannot be null or empty."); //$NON-NLS-1$
        }

        return getPageURL(accessMappingMoniker, "qe.aspx", formatQueryString(projectUri, "path", queryPath)); //$NON-NLS-1$ //$NON-NLS-2$
    }

    /**
     * Gets a view build details url.
     *
     * @param buildUri
     *        Build uri
     */
    public URI getViewBuildDetailsURL(final String buildUri) {
        return getViewBuildDetailsURI(buildUri, AccessMappingMonikers.PUBLIC_ACCESS_MAPPING);
    }

    public URI getViewBuildDetailsURI(final String buildUri, final String accessMappingMoniker) {
        Check.notNullOrEmpty(buildUri, "buildUri"); //$NON-NLS-1$

        return buildURL(
            ServiceInterfaceNames.TSWA_VIEW_BUILD_DETAILS,
            accessMappingMoniker,
            "buildUri", //$NON-NLS-1$
            buildUri,
            null);
    }

    public URI getBuildDefinitionVNextURI(final String projectName, final String action) {
        return getBuildDefinitionVNextURI(projectName, action, 0);
    }

    public URI getBuildDefinitionVNextURI(final String projectName, final String action, final int definitionId) {
        Check.notNullOrEmpty(projectName, "projectName"); //$NON-NLS-1$
        Check.notNullOrEmpty(action, "action"); //$NON-NLS-1$

        final List<String> queryParameters = new ArrayList<String>();

        queryParameters.add("projectname=" + projectName); //$NON-NLS-1$
        queryParameters.add("definitionid=" + definitionId); //$NON-NLS-1$
        queryParameters.add("action=" + action); //$NON-NLS-1$

        /*
         * Current Location Service does not have any entry for vNext build
         * definition services. We have to reuse the one for ViewBuildDetails
         * that returns the template like
         * 'https://tee.visualstudio.com/web/build.aspx?pcguid={
         * projectCollectionGuid}&builduri={buildUri}'.
         *
         * But we do not pass replacement parameters to the call to 'buildURL',
         * and thus the 'builduri={buildUri}' parameter template will be removed
         * from the returned URI.
         */
        return buildURL(
            ServiceInterfaceNames.TSWA_VIEW_BUILD_DETAILS,
            AccessMappingMonikers.PUBLIC_ACCESS_MAPPING,
            null,
            queryParameters);
    }

    public URI getBuildDefinitionVNextURI(
        final String projectName,
        final String action,
        final String definitionTemplateId) {
        Check.notNullOrEmpty(projectName, "projectName"); //$NON-NLS-1$
        Check.notNullOrEmpty(action, "action"); //$NON-NLS-1$

        final List<String> queryParameters = new ArrayList<String>();

        queryParameters.add("projectname=" + projectName); //$NON-NLS-1$
        queryParameters.add("definitionid=0"); //$NON-NLS-1$
        queryParameters.add("templateid=" + definitionTemplateId); //$NON-NLS-1$
        queryParameters.add("action=" + action); //$NON-NLS-1$

        /*
         * Current Location Service does not have any entry for vNext build
         * definition services. We have to reuse the one for ViewBuildDetails
         * that returns the template like
         * 'https://tee.visualstudio.com/web/build.aspx?pcguid={
         * projectCollectionGuid}&builduri={buildUri}'.
         *
         * But we do not pass replacement parameters to the call to 'buildURL',
         * and thus the 'builduri={buildUri}' parameter template will be removed
         * from the returned URI.
         */
        return buildURL(
            ServiceInterfaceNames.TSWA_VIEW_BUILD_DETAILS,
            AccessMappingMonikers.PUBLIC_ACCESS_MAPPING,
            null,
            queryParameters);
    }

    private String formatQueryString(final String parameterName, final String parameterValue) {
        return formatQueryString(null, new String[] {
            parameterName,
            parameterValue
        });
    }

    private String formatQueryString(final String projectUri, final String parameterName, final String parameterValue) {
        return formatQueryString(projectUri, new String[] {
            parameterName,
            parameterValue
        });
    }

    private String formatQueryString(final String projectUri, final String[] args) {
        final StringBuffer result = new StringBuffer();

        if (projectUri != null) {
            if (isCompatibleMode) {
                result.append("puri="); //$NON-NLS-1$
                result.append(URLEncode.encode(projectUri.toString()));
            } else {
                final ArtifactID artifactID = new ArtifactID(projectUri);
                result.append("pguid="); //$NON-NLS-1$
                result.append(URLEncode.encode(artifactID.getToolSpecificID()));
            }
        } else if (!isCompatibleMode) {
            result.append("pcguid="); //$NON-NLS-1$
            result.append(URLEncode.encode(collectionId.toString()));
        }

        for (int i = 0; i < args.length - 1; i += 2) {
            final String name = args[i];
            final String value = args[i + 1];

            if (name != null) {
                if (result.length() > 0) {
                    result.append('&');
                }

                result.append(URLEncode.encode(name));
            }

            if (value != null) {
                if (name != null) {
                    result.append('=');
                } else if (result.length() > 0) {
                    result.append('&');
                }

                result.append(URLEncode.encode(value));
            }
        }

        return result.toString();
    }

    private URI getPageURL(final String accessMappingMoniker, final String relativePath, final String queryString) {
        final String url = URIUtils.combinePaths(getHomeURL(accessMappingMoniker).toString(), relativePath);
        return URI.create(url + "?" + queryString); //$NON-NLS-1$
    }

    private URI buildURL(final String serviceType, final String accessMappingMoniker) {
        return buildURL(serviceType, accessMappingMoniker, null, null);
    }

    private URI buildURL(
        final String serviceType,
        final String accessMappingMoniker,
        final String replaceToken,
        final String replaceWith,
        final List<String> additionalQueryParameters) {
        return buildURL(serviceType, accessMappingMoniker, new String[] {
            replaceToken,
            replaceWith
        }, additionalQueryParameters);
    }

    /**
     * Get a URI from the specified service type and replacement arguments
     *
     * @param serviceType
     * @param accessMappingMoniker
     * @param replacementArgs
     *        Odd entries are the replaceTokens, Even entries are the values to
     *        replace them with
     * @return
     */
    private URI buildURL(
        final String serviceType,
        final String accessMappingMoniker,
        final String[] replacementArgs,
        final List<String> additionalQueryParams) {
        URI uri = null;

        String urlText = getURL(serviceType, accessMappingMoniker);

        if (urlText == null) {
            throw new NotSupportedException(
                MessageFormat.format(
                    "The Location Service Manager cannot find the service definition \"{0}\" among the TSWebAccess tool's services. Please verify that Web Access is configured correctly.", //$NON-NLS-1$
                    serviceType));
        }

        if (replacementArgs == null) {
            final int idx = urlText.indexOf('&');
            if (idx > 0) {
                urlText = urlText.substring(0, idx);
            }
        }

        uri = formatURL(urlText, replacementArgs, additionalQueryParams);
        return uri;
    }

    private URI formatURL(String urlText, final String[] replacementArgs, final List<String> additionalQueryParams) {
        URI uri = null;
        urlText = StringUtil.replace(urlText, "tfs={tfsUrl}&", ""); //$NON-NLS-1$ //$NON-NLS-2$
        if (urlText.indexOf("{projectCollectionGuid}") > 0) //$NON-NLS-1$
        {
            if (collectionId == GUID.EMPTY) {
                throw new TECoreException("Collection ID is not initialized"); //$NON-NLS-1$
            } else {
                urlText = StringUtil.replace(
                    urlText,
                    "{projectCollectionGuid}", //$NON-NLS-1$
                    URLEncode.encode(collectionId.getGUIDString()));
            }
        }

        if (replacementArgs != null) {
            for (int i = 0; i < replacementArgs.length - 1; i += 2) {
                urlText = StringUtil.replace(
                    urlText,
                    "{" + replacementArgs[i] + "}", //$NON-NLS-1$ //$NON-NLS-2$
                    URLEncode.encode(replacementArgs[i + 1]).replace("&", "%26")); //$NON-NLS-1$ //$NON-NLS-2$
            }
        }

        if (additionalQueryParams != null) {
            for (int i = 0; i < additionalQueryParams.size(); i++) {
                urlText += (urlText.indexOf('?') == -1) ? "?" : "&"; //$NON-NLS-1$//$NON-NLS-2$
                urlText += URLEncode.encode(additionalQueryParams.get(i));
            }
        }

        if (displayInEmbeddedBrowser) {
            if (urlText.indexOf('?') != -1) {
                urlText += "&clienthost=tee"; //$NON-NLS-1$
            } else {
                urlText += "?clienthost=tee"; //$NON-NLS-1$
            }
        }

        try {
            uri = new URI(urlText);
        } catch (final URISyntaxException e) {
            throw new TECoreException(MessageFormat.format("Unable to create URI from \"{0}\"", urlText), e); //$NON-NLS-1$
        }

        return uri;
    }

    private String getURL(final String serviceType, final String accessMappingMoniker) {
        return getURL(serviceType, accessMappingMoniker, this.locationService, ToolNames.TS_WEB_ACCESS);
    }

    /**
     * Get the template for the location of the specified Tswa service.
     *
     * @param serviceType
     *        A Tswa service type.
     * @param accessMappingMoniker
     *        The moniker for the desired access mapping.
     * @param locationService
     *        The location service used to get URL.
     * @param toolName
     *        The toolName used, e.g. ToolNames.TS_WEB_ACCESS, ToolNames.GIT
     * @return The template for the location of the specified Tswa serviceType.
     */
    private String getURL(
        final String serviceType,
        final String accessMappingMoniker,
        final ILocationService locationService,
        final String toolName) {
        Check.notNullOrEmpty(serviceType, "serviceType"); //$NON-NLS-1$

        String url = null;
        if (isCompatibleMode) {
            // Using Orcas server or earlier
            final ServiceInterface serviceInterface = getServiceInterface(serviceType);

            if (serviceInterface != null) {
                url = serviceInterface.getURL();
            }
        } else {
            // Rosario server or later
            final ServiceDefinition serviceDefinition = getServiceDefinition(serviceType, locationService, toolName);

            if (serviceDefinition != null) {
                AccessMapping accessMapping = null;

                if (accessMappingMoniker != null && accessMappingMoniker.length() > 0) {
                    accessMapping = locationService.getAccessMapping(accessMappingMoniker);
                }

                return locationService.locationForAccessMapping(serviceDefinition, accessMapping, false);
            }
        }

        return url;
    }

    private ServiceDefinition getServiceDefinition(
        final String serviceType,
        final ILocationService locationService,
        final String toolName) {
        final ServiceDefinition[] definitions = locationService.findServiceDefinitionsByToolType(toolName);
        if (definitions != null) {
            for (int i = 0; i < definitions.length; i++) {
                if (definitions[i].getServiceType().equalsIgnoreCase(serviceType)) {
                    return definitions[i];
                }
            }
        }
        return null;
    }

    private ServiceInterface getServiceInterface(final String serviceType) {
        final RegistrationEntry entry = registrationClient.getRegistrationEntry(ToolNames.TS_WEB_ACCESS);
        if (entry != null) {
            final ServiceInterface[] interfaces = entry.getServiceInterfaces();
            for (int i = 0; i < interfaces.length; i++) {
                if (interfaces[i].getName().equalsIgnoreCase(serviceType)) {
                    return interfaces[i];
                }
            }
        }

        return null;
    }

    public URI getGitCommitURL(final String projectName, final String repositoryID, final String commitID) {
        Check.notNullOrEmpty(projectName, "projectName"); //$NON-NLS-1$
        Check.notNullOrEmpty(repositoryID, "repositoryID"); //$NON-NLS-1$
        Check.notNullOrEmpty(commitID, "commitID"); //$NON-NLS-1$

        URI uri = null;
        String urlText = getURL(
            ServiceInterfaceNames.GIT_VIEW_COMMIT_DETAILS,
            AccessMappingMonikers.PUBLIC_ACCESS_MAPPING,
            this.collectionLocationService,
            ToolNames.GIT);

        if (urlText == null) {
            urlText = collection.getBaseURI() + "{projectName}/_git/repoId/commit/{commitId}?repoId={repositoryId}"; //$NON-NLS-1$
        }

        uri = formatURL(urlText, new String[] {
            "projectName", //$NON-NLS-1$
            projectName,
            "commitId", //$NON-NLS-1$
            commitID,
            "repositoryId", //$NON-NLS-1$
            repositoryID
        }, null);

        return uri;
    }

    public URI getGitRepoURL(final String projectName, final String repoName, final String branchName) {
        Check.notNullOrEmpty(projectName, "projectName"); //$NON-NLS-1$
        Check.notNullOrEmpty(projectName, "repoName"); //$NON-NLS-1$

        String urlText = getURL(
            ServiceInterfaceNames.GIT_VIEW_REF_DETAILS,
            AccessMappingMonikers.PUBLIC_ACCESS_MAPPING,
            this.collectionLocationService,
            ToolNames.GIT);

        if (urlText == null) {
            urlText = collection.getBaseURI() + "{projectNameOrId}/_git/{repositoryId}#version={encodedRef}"; //$NON-NLS-1$
        }

        final String encodedRef;
        if (StringUtil.isNullOrEmpty(branchName)) {
            encodedRef = StringUtil.EMPTY;
        } else if (branchName.startsWith("refs/heads/")) { //$NON-NLS-1$
            encodedRef = "GB" + branchName.substring("refs/heads/".length()); //$NON-NLS-1$ //$NON-NLS-2$
        } else {
            encodedRef = "GB" + branchName; //$NON-NLS-1$
        }

        final URI uri = formatURL(urlText, new String[] {
            "projectNameOrId", //$NON-NLS-1$
            projectName,
            "repositoryId", //$NON-NLS-1$
            repoName,
            "encodedRef", //$NON-NLS-1$
            encodedRef,
        }, null);

        return uri;
    }

    public URI getSourceExplorerUrl(final String projectName, final String teamName, final String serverItemPath) {
        String serverItem = serverItemPath;
        if (StringUtil.isNullOrEmpty(serverItem)) {
            serverItem = ServerPath.ROOT;
        }

        URI uri = null;

        if (isCompatibleMode) {
            uri = getPageURL(
                AccessMappingMonikers.PUBLIC_ACCESS_MAPPING,
                "scc.aspx", //$NON-NLS-1$
                formatQueryString("path", serverItem)); //$NON-NLS-1$
            return uri;
        }

        String urlText = getURL(
            ServiceInterfaceNames.TF_VIEW_SOURCE_CONTROL,
            AccessMappingMonikers.PUBLIC_ACCESS_MAPPING,
            locationService,
            ToolNames.FRAMEWORK);

        if (urlText == null) {
            urlText = getFallbackURL("_versionControl/?path={sourceControlPath}", projectName, teamName); //$NON-NLS-1$
        }

        uri = formatURL(urlText, new String[] {
            "projectName", //$NON-NLS-1$
            projectName,
            "teamName", //$NON-NLS-1$
            teamName,
            "sourceControlPath", //$NON-NLS-1$
            serverItem,
        }, null);

        return uri;
    }

    public URI getSourceExplorerUrl(final URI projectUri) {
        Check.notNull(projectUri, "projectUri"); //$NON-NLS-1$
        return getPageURL(
            AccessMappingMonikers.PUBLIC_ACCESS_MAPPING,
            "UI/Pages/Scc/Explorer.aspx?", //$NON-NLS-1$
            formatQueryString(projectUri.toString(), new String[0]));
    }

    public URI getWorkItemPageUrl(final String projectName, final String teamName) {
        Check.notNullOrEmpty(projectName, "projectName"); //$NON-NLS-1$

        String urlText = getURL(
            ServiceInterfaceNames.TF_VIEW_WORK_ITEMS,
            AccessMappingMonikers.PUBLIC_ACCESS_MAPPING,
            locationService,
            ToolNames.FRAMEWORK);

        if (urlText == null) {
            urlText = getFallbackURL("_workitems", projectName, teamName); //$NON-NLS-1$
        }

        final URI uri = formatURL(urlText, new String[] {
            "projectName", //$NON-NLS-1$
            projectName,
            "teamName", //$NON-NLS-1$
            teamName,
        }, null);

        return uri;
    }

    public URI getWorkItemPageUrl(final URI projectUri) {
        Check.notNull(projectUri, "projectUri"); //$NON-NLS-1$
        return getPageURL(
            AccessMappingMonikers.PUBLIC_ACCESS_MAPPING,
            "UI/Pages/WorkItems/QueryExplorer.aspx?", //$NON-NLS-1$
            formatQueryString(projectUri.toString(), new String[0]));
    }

    public URI getBuildsPageUrl(final String projectName, final String teamName) {
        Check.notNullOrEmpty(projectName, "projectName"); //$NON-NLS-1$

        String urlText = getURL(
            ServiceInterfaceNames.TF_VIEW_BUILDS,
            AccessMappingMonikers.PUBLIC_ACCESS_MAPPING,
            locationService,
            ToolNames.FRAMEWORK);

        if (urlText == null) {
            urlText = getFallbackURL("_build", projectName, teamName); //$NON-NLS-1$
        }

        final URI uri = formatURL(urlText, new String[] {
            "projectName", //$NON-NLS-1$
            projectName,
            "teamName", //$NON-NLS-1$
            teamName,
        }, null);

        return uri;
    }

    public URI getBuildsPageUrl(final URI projectUri) {
        Check.notNull(projectUri, "projectUri"); //$NON-NLS-1$
        return getPageURL(
            AccessMappingMonikers.PUBLIC_ACCESS_MAPPING,
            "UI/Pages/Build/Explorer.aspx", //$NON-NLS-1$
            formatQueryString(projectUri.toString(), new String[0]));
    }

    public URI getSettingsPageUrl(final String projectName, final String teamName) {
        String urlText = getURL(
            ServiceInterfaceNames.TF_VIEW_SETTINGS,
            AccessMappingMonikers.PUBLIC_ACCESS_MAPPING,
            locationService,
            ToolNames.FRAMEWORK);

        if (urlText == null) {
            urlText = getFallbackURL("_admin", projectName, teamName); //$NON-NLS-1$
        }

        final URI uri = formatURL(urlText, new String[] {
            "projectName", //$NON-NLS-1$
            projectName,
            "teamName", //$NON-NLS-1$
            teamName,
        }, null);

        return uri;
    }

    public URI getSettingsPageUrl(final URI projectUri) {
        Check.notNull(projectUri, "projectUri"); //$NON-NLS-1$
        return getPageURL(
            AccessMappingMonikers.PUBLIC_ACCESS_MAPPING,
            "UI/Pages/Options.aspx", //$NON-NLS-1$
            formatQueryString(projectUri.toString(), new String[0]));
    }

    public static URL getFileContainerURL(final IBuildDetail build) {
        final TFSTeamProjectCollection collection = build.getBuildServer().getConnection();

        final ServerDataProvider provider = collection.getServerDataProvider();
        final AccessMapping accessMapping = provider.getAccessMapping(AccessMappingMonikers.PUBLIC_ACCESS_MAPPING);

        final LocationService locationService = new LocationService(collection);
        final String containerServiceURI = locationService.locationForAccessMapping(
            ServiceInterfaceNames.FILE_CONTAINER_SERVICE,
            ServiceInterfaceIdentifiers.FILE_CONTAINER_SERVICE,
            accessMapping);

        final String dropLocation = build.getDropLocation();
        final String[] containerParts = dropLocation.split("/"); //$NON-NLS-1$
        if (containerParts == null || containerParts.length != 3) {
            log.error(
                MessageFormat.format(Messages.getString("BuildDropLocation.InvalidContainerPathFormat"), dropLocation)); //$NON-NLS-1$
            return null;
        }
        final String containID = containerParts[1];
        final String containerPath = containerParts[2];

        final String url = MessageFormat.format(
            "{0}/{1}?itemPath={2}", //$NON-NLS-1$
            containerServiceURI,
            containID,
            containerPath);

        URL dropURL = null;
        try {
            dropURL = new URL(url);
        } catch (final MalformedURLException e) {
            log.error(
                MessageFormat.format(Messages.getString("BuildDropLocation.InvalidContainerPathFormat"), dropLocation), //$NON-NLS-1$
                e);
            return null;
        }

        return dropURL;
    }

    private String getFallbackURL(final String toolName, final String projectName, final String teamName) {
        if (StringUtil.isNullOrEmpty(projectName)) {
            return collection.getBaseURI() + toolName;
        }
        if (StringUtil.isNullOrEmpty(teamName)) {
            return collection.getBaseURI() + "{projectName}/" + toolName; //$NON-NLS-1$
        } else {
            return collection.getBaseURI() + "{projectName}/{teamName}/" + toolName; //$NON-NLS-1$
        }
    }

}
