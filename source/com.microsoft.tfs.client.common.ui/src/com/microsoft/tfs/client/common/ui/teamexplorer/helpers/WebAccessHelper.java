// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.teamexplorer.helpers;

import java.net.URI;
import java.net.URISyntaxException;
import java.text.MessageFormat;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.browser.BrowserFacade;
import com.microsoft.tfs.client.common.ui.browser.BrowserFacade.LaunchMode;
import com.microsoft.tfs.client.common.ui.framework.helper.MessageBoxHelpers;
import com.microsoft.tfs.client.common.ui.framework.helper.ShellUtils;
import com.microsoft.tfs.client.common.ui.teamexplorer.TeamExplorerContext;
import com.microsoft.tfs.client.common.ui.teamexplorer.internal.TeamExplorerHelpers;
import com.microsoft.tfs.core.PreFrameworkServerDataProvider;
import com.microsoft.tfs.core.TFSTeamProjectCollection;
import com.microsoft.tfs.core.clients.commonstructure.ProjectInfo;
import com.microsoft.tfs.core.clients.framework.ServerDataProvider;
import com.microsoft.tfs.core.clients.framework.internal.ServiceInterfaceIdentifiers;
import com.microsoft.tfs.core.clients.framework.internal.ServiceInterfaceNames;
import com.microsoft.tfs.core.clients.framework.location.AccessMapping;
import com.microsoft.tfs.core.clients.framework.location.ServiceDefinition;
import com.microsoft.tfs.core.clients.framework.location.internal.AccessMappingMonikers;
import com.microsoft.tfs.core.clients.workitem.queryhierarchy.QueryFolderUtil;
import com.microsoft.tfs.core.clients.workitem.queryhierarchy.QueryItem;
import com.microsoft.tfs.core.exceptions.NotSupportedException;
import com.microsoft.tfs.core.exceptions.TECoreException;
import com.microsoft.tfs.core.httpclient.util.URIUtil;
import com.microsoft.tfs.core.util.TSWAHyperlinkBuilder;
import com.microsoft.tfs.util.GUID;
import com.microsoft.tfs.util.StringUtil;

public class WebAccessHelper {
    private static final Log log = LogFactory.getLog(WebAccessHelper.class);

    public static boolean hasSecurityService(final TeamExplorerContext context) {
        return hasServiceDefinition(
            context.getServer().getConnection(),
            ServiceInterfaceNames.TSWA_SECURITY_MANAGEMENT,
            ServiceInterfaceIdentifiers.TSWA_SECURITY_MANAGEMENT);
    }

    public static boolean hasWorkItemAreasService(final TeamExplorerContext context) {
        return hasServiceDefinition(
            context.getServer().getConnection(),
            ServiceInterfaceNames.TSWA_AREAS_MANAGEMENT,
            ServiceInterfaceIdentifiers.TSWA_AREAS_MANAGEMENT);
    }

    public static boolean hasProjectAlertsService(final TeamExplorerContext context) {
        return hasServiceDefinition(
            context.getServer().getConnection(),
            ServiceInterfaceNames.TSWA_PROJECT_ALERTS,
            ServiceInterfaceIdentifiers.TSWA_PROJECT_ALERTS);
    }

    public static void openWebAccess(final TeamExplorerContext context) {
        if (!context.isConnectedToCollection()) {
            return;
        } else if (!context.isConnected()) {
            final TSWAHyperlinkBuilder builder = getHyperlinkBuilder(context);
            showURI(builder.getHomeURL());
        } else {
            final URI uri = getCurrentProjectURI(context);

            final TSWAHyperlinkBuilder builder = getHyperlinkBuilder(context);
            showURI(builder.getHomeURL(uri));
        }
    }

    private static URI getCurrentProjectURI(final TeamExplorerContext context) {
        final ProjectInfo project = context.getCurrentProjectInfo();
        if (project == null) {
            return context.getServer().getConnection().getBaseURI();
        }

        final String uriText = project.getURI();
        final URI uri;

        try {
            uri = new URI(uriText);
        } catch (final URISyntaxException e) {
            final String format = Messages.getString("WebAccessHelper.URIErrorFormat"); //$NON-NLS-1$
            throw new TECoreException(MessageFormat.format(format, uriText), e);
        }
        return uri;
    }

    private static String getCurrentProjectName(final TeamExplorerContext context) {
        final ProjectInfo project = context.getCurrentProjectInfo();

        return project != null ? project.getName() : null;
    }

    public static void openVersionControl(final TeamExplorerContext context) {
        final TSWAHyperlinkBuilder builder = getHyperlinkBuilder(context);
        if (TeamExplorerHelpers.supportsTeam(context)) {
            showURI(builder.getSourceExplorerUrl(getCurrentProjectName(context), getCurrentTeamName(context), null));
        } else {
            final URI uri = getCurrentProjectURI(context);
            showURI(builder.getSourceExplorerUrl(uri));
        }
    }

    public static void openGitRepo(
        final TeamExplorerContext context,
        final String projName,
        final String repoName,
        final String branchName) {
        final TSWAHyperlinkBuilder builder = getHyperlinkBuilder(context);
        if (TeamExplorerHelpers.supportsGit(context)) {
            showURI(builder.getGitRepoURL(projName, repoName, branchName));
        }
    }

    public static String getGitRepoURL(
        final TeamExplorerContext context,
        final String projName,
        final String repoName,
        final String branchName) {
        final TSWAHyperlinkBuilder builder = getHyperlinkBuilder(context);
        if (TeamExplorerHelpers.supportsGit(context)) {
            return builder.getGitRepoURL(projName, repoName, branchName).toString();
        } else {
            return null;
        }
    }

    public static void openWorkItems(final TeamExplorerContext context) {
        final TSWAHyperlinkBuilder builder = getHyperlinkBuilder(context);
        if (TeamExplorerHelpers.supportsTeam(context)) {
            showURI(builder.getWorkItemPageUrl(getCurrentProjectName(context), getCurrentTeamName(context)));
        } else {
            final URI uri = getCurrentProjectURI(context);
            showURI(builder.getWorkItemPageUrl(uri));
        }
    }

    public static void openBuilds(final TeamExplorerContext context) {
        final TSWAHyperlinkBuilder builder = getHyperlinkBuilder(context);
        if (TeamExplorerHelpers.supportsTeam(context)) {
            showURI(builder.getBuildsPageUrl(getCurrentProjectName(context), getCurrentTeamName(context)));
        } else {
            final URI uri = getCurrentProjectURI(context);
            showURI(builder.getBuildsPageUrl(uri));
        }
    }

    public static void openSettings(final TeamExplorerContext context) {
        final TSWAHyperlinkBuilder builder = getHyperlinkBuilder(context);
        if (TeamExplorerHelpers.supportsTeam(context)) {
            showURI(builder.getSettingsPageUrl(getCurrentProjectName(context), getCurrentTeamName(context)));
        } else {
            final URI uri = getCurrentProjectURI(context);
            showURI(builder.getSettingsPageUrl(uri));
        }
    }

    private static TSWAHyperlinkBuilder getHyperlinkBuilder(final TeamExplorerContext context) {
        final TSWAHyperlinkBuilder builder = new TSWAHyperlinkBuilder(context.getServer().getConnection());
        return builder;
    }

    private static String getCurrentTeamName(final TeamExplorerContext context) {
        if (context == null || context.getCurrentTeam() == null) {
            return null;
        }
        return context.getCurrentTeam().getTeamName();
    }

    public static void openProjectSecurity(final TeamExplorerContext context) {
        showPage(
            context.getServer().getConnection(),
            getCurrentProjectName(context),
            ServiceInterfaceNames.TSWA_SECURITY_MANAGEMENT,
            ServiceInterfaceIdentifiers.TSWA_SECURITY_MANAGEMENT);
    }

    public static void openProjectGroupMembership(final TeamExplorerContext context) {
        showPage(
            context.getServer().getConnection(),
            getCurrentProjectName(context),
            ServiceInterfaceNames.TSWA_IDENTITY_MANAGEMENT,
            ServiceInterfaceIdentifiers.TSWA_IDENTITY_MANAGEMENT);
    }

    public static void openProjectWorkItemAreas(final TeamExplorerContext context) {
        showPage(
            context.getServer().getConnection(),
            getCurrentProjectName(context),
            ServiceInterfaceNames.TSWA_AREAS_MANAGEMENT,
            ServiceInterfaceIdentifiers.TSWA_AREAS_MANAGEMENT);
    }

    public static void openProjectWorkItemIterations(final TeamExplorerContext context) {
        showPage(
            context.getServer().getConnection(),
            getCurrentProjectName(context),
            ServiceInterfaceNames.TSWA_ITERATIONS_MANAGEMENT,
            ServiceInterfaceIdentifiers.TSWA_ITERATIONS_MANAGEMENT);
    }

    public static void openProjectAlerts(final TeamExplorerContext context) {
        showPage(
            context.getServer().getConnection(),
            getCurrentProjectName(context),
            ServiceInterfaceNames.TSWA_PROJECT_ALERTS,
            ServiceInterfaceIdentifiers.TSWA_PROJECT_ALERTS);
    }

    public static void openCollectionSecurity(final TeamExplorerContext context) {
        showPage(
            context.getServer().getConnection(),
            null,
            ServiceInterfaceNames.TSWA_SECURITY_MANAGEMENT,
            ServiceInterfaceIdentifiers.TSWA_SECURITY_MANAGEMENT);
    }

    public static void openCollectionGroupMembership(final TeamExplorerContext context) {
        showPage(
            context.getServer().getConnection(),
            null,
            ServiceInterfaceNames.TSWA_IDENTITY_MANAGEMENT,
            ServiceInterfaceIdentifiers.TSWA_IDENTITY_MANAGEMENT);
    }

    public static URI getWebAccessQueryEditorURI(final TSWAHyperlinkBuilder tswaBuilder, final QueryItem queryItem) {
        final String queryPath = StringUtil.replace(
            QueryFolderUtil.getHierarchicalPath(queryItem.getParent()),
            QueryFolderUtil.PATH_HIERARCHY_SEPARATOR,
            "/") //$NON-NLS-1$
            + "/" //$NON-NLS-1$
            + queryItem.getName();
        return tswaBuilder.getWorkItemQueryEditorURL(queryItem.getProject().getURI(), queryPath);
    }

    public static URI getWebAccessQueryResultURI(final TSWAHyperlinkBuilder tswaBuilder, final QueryItem queryItem) {
        final String queryPath = StringUtil.replace(
            QueryFolderUtil.getHierarchicalPath(queryItem.getParent()),
            QueryFolderUtil.PATH_HIERARCHY_SEPARATOR,
            "/") //$NON-NLS-1$
            + "/" //$NON-NLS-1$
            + queryItem.getName();
        return tswaBuilder.getWorkItemQueryResultsURL(queryItem.getProject().getURI(), queryPath);
    }

    private static void showPage(
        final TFSTeamProjectCollection collection,
        final String projectName,
        final String serviceType,
        final GUID serviceIdentifier) {
        final URI uri;

        try {
            final ServiceDefinition serviceDefinition =
                getServiceDefinition(collection, serviceType, serviceIdentifier);

            if (serviceDefinition == null) {
                final String format = Messages.getString("WebAccessHelper.ServiceNotSupportedFormat"); //$NON-NLS-1$
                throw new NotSupportedException(MessageFormat.format(format, serviceType));
            }

            final ServerDataProvider provider = collection.getServerDataProvider();
            final AccessMapping accessMapping = provider.getAccessMapping(AccessMappingMonikers.PUBLIC_ACCESS_MAPPING);
            String uriText = provider.locationForAccessMapping(serviceDefinition, accessMapping, false);

            if (uriText.indexOf("/{projectName}") > 0) //$NON-NLS-1$
            {
                if (projectName == null) {
                    uriText = StringUtil.replace(uriText, "/{projectName}", ""); //$NON-NLS-1$ //$NON-NLS-2$
                } else {
                    uriText = StringUtil.replace(uriText, "{projectName}", projectName); //$NON-NLS-1$
                }
            }

            try {
                uri = new URI(URIUtil.encodePathQuery(uriText));
            } catch (final Exception e) {
                final String format = Messages.getString("WebAccessHelper.URIErrorFormat"); //$NON-NLS-1$
                throw new TECoreException(MessageFormat.format(format, uriText), e);
            }
        } catch (final TECoreException e) {
            final String title = Messages.getString("WebAccessHelper.ErrorDialogTitle"); //$NON-NLS-1$
            final String format = Messages.getString("WebAccessHelper.LauchWebAccessErrorFormat"); //$NON-NLS-1$
            final String message = MessageFormat.format(format, e.getLocalizedMessage());

            log.error("Caught exception while creating a Web Access URL", e); //$NON-NLS-1$
            MessageBoxHelpers.errorMessageBox(ShellUtils.getWorkbenchShell(), title, message);
            return;
        }

        showURI(uri);
    }

    private static void showURI(final URI uri) {
        // When calling web access we want to be explicitly external
        BrowserFacade.launchURL(uri, null, null, null, LaunchMode.EXTERNAL);
    }

    private static boolean hasServiceDefinition(
        final TFSTeamProjectCollection collection,
        final String serviceType,
        final GUID serviceIdentifier) {
        try {
            return getServiceDefinition(collection, serviceType, serviceIdentifier) != null;
        } catch (final TECoreException e) {
            final String title = Messages.getString("WebAccessHelper.ErrorDialogTitle"); //$NON-NLS-1$
            final String format = Messages.getString("WebAccessHelper.LauchWebAccessErrorFormat"); //$NON-NLS-1$
            final String message = MessageFormat.format(format, e.getLocalizedMessage());

            log.error("Caught exception while checking a Web Access service", e); //$NON-NLS-1$
            MessageBoxHelpers.errorMessageBox(ShellUtils.getWorkbenchShell(), title, message);
            return false;
        }
    }

    private static ServiceDefinition getServiceDefinition(
        final TFSTeamProjectCollection collection,
        final String serviceType,
        final GUID serviceIdentifier) {
        final ServerDataProvider provider = collection.getServerDataProvider();
        if (provider == null || provider instanceof PreFrameworkServerDataProvider) {
            return null;
        }

        return provider.findServiceDefinition(serviceType, serviceIdentifier);
    }
}
