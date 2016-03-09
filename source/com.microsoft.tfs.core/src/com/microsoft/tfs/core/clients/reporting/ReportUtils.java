// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.reporting;

import java.text.MessageFormat;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.microsoft.tfs.core.TFSTeamProjectCollection;
import com.microsoft.tfs.core.clients.framework.configuration.entities.ProjectCollectionEntity;
import com.microsoft.tfs.core.clients.framework.configuration.entities.ReportingConfigurationEntity;
import com.microsoft.tfs.core.clients.framework.configuration.entities.ReportingFolderEntity;
import com.microsoft.tfs.core.clients.framework.configuration.entities.ReportingServerEntity;
import com.microsoft.tfs.core.clients.framework.configuration.entities.TeamProjectEntity;
import com.microsoft.tfs.core.clients.reporting.internal.ReportingConstants;
import com.microsoft.tfs.core.util.URIUtils;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.GUID;
import com.microsoft.tfs.util.URLEncode;

/**
 * Utility class to determine common details required when working with SQL
 * Server Reporting Services.
 *
 * @since TEE-SDK-10.1
 */
public final class ReportUtils {
    private static final Log log = LogFactory.getLog(ReportUtils.class);

    private ReportUtils() {
    }

    /**
     * Determine if a reporting instance is defined for the project collection.
     * For a server previous to TFS 2010 this is generally true, however TFS
     * 2010 introduced a basic installation option which means that reporting is
     * optional.
     */
    public static boolean isReportingConfigured(final TFSTeamProjectCollection connection) {
        Check.notNull(connection, "connection"); //$NON-NLS-1$

        /* 2010: query the catalog service */
        final ProjectCollectionEntity projectCollection = connection.getTeamProjectCollectionEntity(false);

        if (projectCollection != null) {
            final ReportingFolderEntity reportingFolder = projectCollection.getReportingFolder();
            final ReportingConfigurationEntity reportingConfiguration = projectCollection.getReportingConfiguration();

            if (reportingFolder != null
                && reportingConfiguration != null
                && reportingConfiguration.getReportingServer() != null) {
                return true;
            }
        }

        return false;
    }

    public static String getReportManagerURL(final TFSTeamProjectCollection connection) {
        Check.notNull(connection, "connection"); //$NON-NLS-1$

        if (isReportingConfigured(connection)) {
            /* 2010: query the catalog service */
            final ProjectCollectionEntity projectCollection = connection.getTeamProjectCollectionEntity(false);

            if (projectCollection != null) {
                /* 2010: use catalog service */
                final ReportingConfigurationEntity reportingConfiguration =
                    projectCollection.getReportingConfiguration();

                if (reportingConfiguration != null) {
                    return reportingConfiguration.getReportingServer().getReportManagerURL();
                }
            }
        }

        return null;
    }

    public static String getReportServiceURL(final TFSTeamProjectCollection connection) {
        Check.notNull(connection, "connection"); //$NON-NLS-1$

        if (isReportingConfigured(connection)) {
            /* 2010: query the catalog service */
            final ProjectCollectionEntity projectCollection = connection.getTeamProjectCollectionEntity(false);

            if (projectCollection != null) {
                /* 2010: use catalog service */
                final ReportingServerEntity reportingServer =
                    projectCollection.getReportingFolder().getReportingServer();

                final String baseUri = reportingServer.getReportWebServiceURL();

                if (baseUri != null) {
                    return URIUtils.combinePaths(baseUri, ReportingConstants.WEB_SERVICE, false);
                }
            }
        }

        return null;
    }

    public static String formatReportManagerPath(final String reportManagerUrl, String itemPath) {
        Check.notNull(reportManagerUrl, "reportManagerUrl"); //$NON-NLS-1$
        Check.notNull(itemPath, "itemPath"); //$NON-NLS-1$

        if (!itemPath.startsWith("/")) //$NON-NLS-1$
        {
            itemPath = "/" + itemPath; //$NON-NLS-1$
        }

        return MessageFormat.format(
            "{0}/Pages/Folder.aspx?ItemPath={1}", //$NON-NLS-1$
            reportManagerUrl,
            URLEncode.encode(itemPath));
    }

    public static String formatReportViewerPath(String reportViewerUrl, String itemPath, final boolean showToolbar) {
        /* remove known web service paths */
        reportViewerUrl = removeKnownWebServerPaths(reportViewerUrl);

        /* trim ending path separator */
        while (reportViewerUrl.endsWith("/")) //$NON-NLS-1$
        {
            reportViewerUrl = reportViewerUrl.substring(0, reportViewerUrl.length() - 1);
        }

        if (!itemPath.startsWith("/")) //$NON-NLS-1$
        {
            itemPath = "/" + itemPath; //$NON-NLS-1$
        }

        final String hideToolbarString = MessageFormat.format("&{0}=false", URLEncode.encode("rc:Toolbar")); //$NON-NLS-1$ //$NON-NLS-2$

        return MessageFormat.format(
            "{0}?{1}{2}", //$NON-NLS-1$
            reportViewerUrl,
            URLEncode.encode(itemPath),
            (showToolbar ? "" : hideToolbarString)); //$NON-NLS-1$
    }

    public static String removeKnownWebServerPaths(String url) {
        Check.notNull(url, "url"); //$NON-NLS-1$

        url = url.trim();

        for (int i = 0; i < ReportingConstants.KNOWN_WEB_SERVICE_PATHS.length; i++) {
            if (url.endsWith(ReportingConstants.KNOWN_WEB_SERVICE_PATHS[i])) {
                url = url.substring(0, url.length() - ReportingConstants.KNOWN_WEB_SERVICE_PATHS[i].length());
            }
        }

        return url;
    }

    public static String getProjectReportFolder(final TFSTeamProjectCollection connection, final GUID projectId) {
        if (isReportingConfigured(connection)) {
            /* 2010: query the catalog service */
            final ProjectCollectionEntity projectCollection = connection.getTeamProjectCollectionEntity(false);

            if (projectCollection != null) {
                final TeamProjectEntity teamProject = projectCollection.getTeamProject(projectId);

                if (teamProject == null) {
                    log.warn(MessageFormat.format("Could not load team project configuration data for {0}", projectId)); //$NON-NLS-1$
                } else {
                    if (teamProject.getReportingFolder() != null) {
                        return teamProject.getReportingFolder().getFullItemPath();
                    }
                }
            }
        }

        return null;
    }
}
