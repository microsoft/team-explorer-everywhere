// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.reporting;

import java.net.URI;
import java.net.URISyntaxException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.microsoft.tfs.core.TFSTeamProjectCollection;
import com.microsoft.tfs.core.clients.commonstructure.ProjectInfo;
import com.microsoft.tfs.core.clients.framework.configuration.entities.ProjectCollectionEntity;
import com.microsoft.tfs.core.clients.framework.configuration.entities.ReportingFolderEntity;
import com.microsoft.tfs.core.clients.framework.configuration.entities.TeamProjectEntity;
import com.microsoft.tfs.core.clients.framework.internal.ServiceInterfaceNames;
import com.microsoft.tfs.core.clients.registration.ToolNames;
import com.microsoft.tfs.core.clients.registration.exceptions.RegistrationException;
import com.microsoft.tfs.core.clients.sharepoint.WSSClient;
import com.microsoft.tfs.core.ws.runtime.client.SOAPService;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.GUID;

import ms.sql.reporting.reportingservices._CatalogItem;
import ms.sql.reporting.reportingservices._ReportingService2005Soap;

/**
 * A client to talk to the SQL Server Reporting Services web service.
 *
 * @since TEE-SDK-10.1
 */
public class ReportingClient {
    private static final Log log = LogFactory.getLog(ReportingClient.class);

    private final _ReportingService2005Soap webService;
    private final TFSTeamProjectCollection connection;

    private boolean is2010Server = false;

    public ReportingClient(final TFSTeamProjectCollection connection, final _ReportingService2005Soap webService) {
        this.connection = connection;
        this.webService = webService;
    }

    public _ReportingService2005Soap getProxy() {
        return webService;
    }

    /**
     * <p>
     * TEE will automatically correct the endpoints registered URL when creating
     * the web service, however we must provide a mechansim to correct fully
     * qualified URI's provided as additional URI from the same webservice.
     * </p>
     * <p>
     * We compare the passed uri with the registered web service endpoint, if
     * they share the same root (i.e. http://TFSERVER) then we correct the
     * passed uri to be the same as the corrected web service enpoint (i.e.
     * http://tfsserver.mycompany.com)
     * </p>
     *
     * @see WSSClient#getFixedURI(String)
     */
    public String getFixedURI(final String uri) {
        Check.notNull(uri, "uri"); //$NON-NLS-1$

        try {
            // Get what the server thinks the url is.
            String url = connection.getRegistrationClient().getServiceInterfaceURL(
                ToolNames.WAREHOUSE,
                ServiceInterfaceNames.REPORTING);

            if (url == null || url.length() == 0) {
                // Might be a Rosario server
                url = connection.getRegistrationClient().getServiceInterfaceURL(
                    ToolNames.WAREHOUSE,
                    ServiceInterfaceNames.REPORTING_WEB_SERVICE_URL);
                if (url == null || url.length() == 0) {
                    // Couldn't figure this out - just give up and return what
                    // we
                    // were passed.
                    return uri;
                }
                is2010Server = true;
            }

            final URI registeredEndpointUri = new URI(url);

            final URI passedUri = new URI(uri);

            if (passedUri.getScheme().equals(registeredEndpointUri.getScheme())
                && passedUri.getHost().equals(registeredEndpointUri.getHost())
                && passedUri.getPort() == registeredEndpointUri.getPort()) {
                final URI endpointUri = ((SOAPService) getProxy()).getEndpoint();
                final URI fixedUri = new URI(
                    endpointUri.getScheme(),
                    endpointUri.getHost(),
                    passedUri.getPath(),
                    passedUri.getQuery(),
                    passedUri.getFragment());
                return fixedUri.toASCIIString();
            }
        } catch (final URISyntaxException e) {
            // ignore;
        }
        return uri;
    }

    public List getReports(final ProjectInfo projectInfo, final boolean refresh) {
        final String item = getFullItemPath(projectInfo);

        final _CatalogItem[] items = getProxy().listChildren(item, true);

        if (items == null) {
            return new ArrayList<ReportFolder>(0);
        }

        final ReportFolder root =
            populateChildren(projectInfo.getName(), new ReportFolder(projectInfo.getName(), item + "/"), items, false); //$NON-NLS-1$

        return Arrays.asList(root.getChildren());
    }

    private ReportFolder populateChildren(

        final String projectName,
        ReportFolder parentNode,
        final _CatalogItem[] items,
        final boolean showHidden) {
        /*
         * This loop needs improvement - I should be able to figure out a way of
         * doing this with only one pass over the array but couldn't see the
         * answer straight away. Hope to come back to this (at the time of
         * writing it is May 17 2006 - let's see how long it is before this code
         * is re-written...
         */

        // Get the sub folders
        parentNode = populateChildren(projectName, parentNode, items, ReportNodeType.FOLDER, showHidden);

        // recursively call for each child folder
        final Object[] subFolders = parentNode.getChildren();
        for (int i = 0; i < subFolders.length; i++) {
            subFolders[i] = populateChildren(projectName, (ReportFolder) subFolders[i], items, showHidden);
        }

        // now add reports
        parentNode = populateChildren(projectName, parentNode, items, ReportNodeType.REPORT, showHidden);

        return parentNode;
    }

    private ReportFolder populateChildren(
        final String projectName,
        final ReportFolder parentNode,
        final _CatalogItem[] items,
        final String type,
        final boolean showHidden) {

        for (int i = 0; i < items.length; i++) {
            if (isChildPath(parentNode.getPath(), items[i].getPath())
                && type.equals(items[i].getType().toString())
                && (showHidden || !items[i].isHidden())) {
                ReportNode node;
                if (items[i].getType().toString().equals(ReportNodeType.FOLDER)) {
                    node = new ReportFolder(projectName, items[i]);
                } else {
                    node = new Report(projectName, items[i]);
                }
                node.setParent(parentNode);
                parentNode.addChild(node);

            }
        }

        return parentNode;
    }

    public boolean isChildPath(String parentPath, final String evalPath) {
        if (parentPath == null || evalPath == null || evalPath.length() < parentPath.length()) {
            return false;
        }
        if (!parentPath.endsWith("/")) //$NON-NLS-1$
        {
            parentPath += "/"; //$NON-NLS-1$
        }

        final String subPath = evalPath.substring(0, Math.min(evalPath.length(), parentPath.length()));
        return subPath.equalsIgnoreCase(parentPath) && (evalPath.indexOf('/', parentPath.length()) < 0);
    }

    private String getFullItemPath(final ProjectInfo projectInfo) {
        String itemPath = null;

        /*
         * For 2010 servers, look at the project collection's reporting
         * configuration in the catalog
         */
        final ProjectCollectionEntity projectCollection = connection.getTeamProjectCollectionEntity(false);

        if (projectCollection != null) {
            final TeamProjectEntity project = projectCollection.getTeamProject(new GUID(projectInfo.getGUID()));

            if (project == null) {
                log.warn(MessageFormat.format("Could not locate project catalog data for {0}", projectInfo.getGUID())); //$NON-NLS-1$
            } else {
                final ReportingFolderEntity collectionReportingFolder = projectCollection.getReportingFolder();
                final ReportingFolderEntity projectReportingFolder = project.getReportingFolder();

                if (collectionReportingFolder == null || projectReportingFolder == null) {
                    log.info("Could not locate project reporting folder catalog data"); //$NON-NLS-1$
                } else {
                    itemPath = MessageFormat.format(
                        "{0}/{1}", //$NON-NLS-1$
                        collectionReportingFolder.getItemPath(),
                        projectReportingFolder.getItemPath());
                }
            }
        } else {
            if (is2010Server) {
                itemPath = connection.getRegistrationClient().getRosarioURLForTeamProject(
                    "ReportFolder", //$NON-NLS-1$
                    projectInfo.getName());
                // TODO: Consider removing when TFS 2010 is released.
                if (itemPath == null) {
                    throw new RegistrationException(
                        "Unable to determine report folder for TFS 2010 server. Possibly a pre TFS 2010 Beta 2 instance?"); //$NON-NLS-1$
                }
            }

            if (itemPath == null || itemPath.length() == 0) {
                itemPath = "/" + projectInfo.getName(); //$NON-NLS-1$
            }
        }

        return itemPath;
    }

    protected boolean isTFS2010Server() {
        return is2010Server;
    }

}
