// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.teamexplorer.internal;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.forms.widgets.SharedScrolledComposite;

import com.microsoft.tfs.client.common.git.utils.GitHelpers;
import com.microsoft.tfs.client.common.server.TFSServer;
import com.microsoft.tfs.client.common.ui.framework.telemetry.ClientTelemetryHelper;
import com.microsoft.tfs.client.common.ui.teamexplorer.TeamExplorerContext;
import com.microsoft.tfs.client.common.ui.views.TeamExplorerPendingChangesView;
import com.microsoft.tfs.core.TFSTeamProjectCollection;
import com.microsoft.tfs.core.clients.teamstore.TeamProjectCollectionTeamStore;
import com.microsoft.tfs.core.clients.versioncontrol.SourceControlCapabilityFlags;
import com.microsoft.tfs.core.clients.versioncontrol.WebServiceLevel;
import com.microsoft.tfs.core.clients.webservices.IIdentityManagementService2;
import com.microsoft.tfs.core.clients.webservices.IdentityManagementException;
import com.microsoft.tfs.core.clients.webservices.IdentityManagementService2;
import com.microsoft.tfs.core.util.ServerURIUtils;
import com.microsoft.tfs.core.util.URIUtils;
import com.microsoft.tfs.jni.RegistryKey;
import com.microsoft.tfs.jni.RegistryValue;
import com.microsoft.tfs.jni.RootKey;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.Platform;
import com.microsoft.tfs.util.StringUtil;

public class TeamExplorerHelpers {
    private static final Log log = LogFactory.getLog(TeamExplorerHelpers.class);

    public static final int MOUSE_RIGHT_BUTTON = 3;

    public static final String PendingChangesViewID = TeamExplorerPendingChangesView.ID;
    public static final String BuildsViewID =
        "com.microsoft.tfs.client.common.ui.teambuild.teamexplorer.views.TeamExplorerBuildView"; //$NON-NLS-1$

    public static final String PendingChangeNavItemID =
        "com.microsoft.tfs.client.common.ui.teamexplorer.items.TeamExplorerPendingChangesNavigationItem"; //$NON-NLS-1$
    public static final String VersionControlNavItemID =
        "com.microsoft.tfs.client.common.ui.vcexplorer.teamexplorer.TeamExplorerVersionControlNavigationItem"; //$NON-NLS-1$
    public static final String WorkItemNavItemID =
        "com.microsoft.tfs.client.common.ui.teamexplorer.items.TeamExplorerWorkItemsNavigationItem"; //$NON-NLS-1$
    public static final String BuildNavItemID =
        "com.microsoft.tfs.client.common.ui.teambuild.teamexplorer.items.TeamExplorerBuildNavigationItem"; //$NON-NLS-1$
    public static final String DocumentsNavItemID =
        "com.microsoft.tfs.client.common.ui.teamexplorer.items.TeamExplorerDocumentsNavigationItem"; //$NON-NLS-1$
    public static final String ReportsNavItemID =
        "com.microsoft.tfs.client.common.ui.teamexplorer.items.TeamExplorerReportsNavigationItem"; //$NON-NLS-1$
    public static final String SettingsNavItemID =
        "com.microsoft.tfs.client.common.ui.teamexplorer.items.TeamExplorerSettingsNavigationItem"; //$NON-NLS-1$

    public static final String EGitRepoViewID = "org.eclipse.egit.ui.RepositoriesView"; //$NON-NLS-1$

    private static final String GIT_TOKEN = "_git"; //$NON-NLS-1$
    private static final String DEFAULT_COLLECTION_TOKEN = "DefaultCollection"; //$NON-NLS-1$

    private final static String PROTOCOL_HANDLER_ARG = "-openuri"; //$NON-NLS-1$
    private final static String PROTOCOL_HANDLER_SCHEME = "vsoi"; //$NON-NLS-1$
    private final static String PROTOCOL_HANDLER_URL_PARAM = "url="; //$NON-NLS-1$
    private final static String PROTOCOL_HANDLER_ENCODING_PARAM = "EncFormat="; //$NON-NLS-1$
    private final static String PROTOCOL_HANDLER_BRANCH_PARAM = "Ref="; //$NON-NLS-1$

    public final static String PROTOCOL_HANDLER_URL_PROPERTY = "com.microsoft.tfs.clone.url"; //$NON-NLS-1$
    public final static String PROTOCOL_HANDLER_ENCODING_PROPERTY = "com.microsoft.tfs.clone.encoding"; //$NON-NLS-1$
    public final static String PROTOCOL_HANDLER_BRANCH_PROPERTY = "com.microsoft.tfs.clone.branch"; //$NON-NLS-1$
    public final static String PROTOCOL_HANDLER_SERVER_PROPERTY = "com.microsoft.tfs.clone.server"; //$NON-NLS-1$
    public final static String PROTOCOL_HANDLER_REPOSITORY_PROPERTY = "com.microsoft.tfs.clone.repository"; //$NON-NLS-1$
    public final static String PROTOCOL_HANDLER_PROJECT_PROPERTY = "com.microsoft.tfs.clone.project"; //$NON-NLS-1$

    public final static String PROTOCOL_HANDLER_LAUNCHER_PROPERTY = "eclipse.launcher"; //$NON-NLS-1$
    public final static String PROTOCOL_HANDLER_REGISTRY_KEY = PROTOCOL_HANDLER_SCHEME + "\\Shell\\Open\\Command"; //$NON-NLS-1$

    public static void relayoutContainingScrolledComposite(Composite composite) {
        SharedScrolledComposite scrolledComposite = null;
        while (composite != null) {
            if (composite instanceof SharedScrolledComposite) {
                scrolledComposite = (SharedScrolledComposite) composite;
                break;
            }

            composite = composite.getParent();
        }

        Check.notNull(scrolledComposite, "scrolledComposite"); //$NON-NLS-1$

        scrolledComposite.layout(true, true);
        scrolledComposite.reflow(true);
    }

    public static void relayoutIfResized(final Composite composite) {
        final Point currentSize = composite.getSize();
        final Point newSize = composite.computeSize(SWT.DEFAULT, SWT.DEFAULT);

        if (!currentSize.equals(newSize)) {
            TeamExplorerHelpers.relayoutContainingScrolledComposite(composite);
        }
    }

    public static void showOrHideSection(final Composite composite, final boolean show) {
        final Section section = getContainingSection(composite);

        if (section != null) {
            final GridData gridData = (GridData) section.getLayoutData();
            gridData.exclude = !show;
            section.setVisible(show);
        }
    }

    public static void updateContainingSectionTitle(final Composite composite, final String title) {
        final Section section = getContainingSection(composite);

        if (section != null) {
            section.setText(title);
        }
    }

    public static void toggleContainingSection(final Composite composite) {
        final Section section = getContainingSection(composite);

        if (section != null) {
            section.setExpanded(false);
            section.setExpanded(true);
        }
    }

    public static void toggleCompositeVisibility(final Composite composite) {
        Check.isTrue(composite.getLayoutData() instanceof GridData, "composite.getLayoutData() instanceof GridData"); //$NON-NLS-1$

        final GridData gridData = (GridData) composite.getLayoutData();

        if (gridData.exclude) {
            gridData.exclude = false;
            composite.setVisible(true);
        } else {
            gridData.exclude = true;
            composite.setVisible(false);
        }

        // reflowContainingScrolledComposite(composite);
    }

    public static Color getDropCompositeForeground(final Composite parent) {
        if (Platform.isCurrentPlatform(Platform.WINDOWS)) {
            return parent.getDisplay().getSystemColor(SWT.COLOR_TITLE_FOREGROUND);
        } else {
            return parent.getDisplay().getSystemColor(SWT.COLOR_WIDGET_FOREGROUND);
        }
    }

    public static Color getDropCompositeBackground(final Composite parent) {
        if (Platform.isCurrentPlatform(Platform.WINDOWS)) {
            return parent.getDisplay().getSystemColor(SWT.COLOR_TITLE_BACKGROUND);
        } else {
            return parent.getDisplay().getSystemColor(SWT.COLOR_TITLE_INACTIVE_BACKGROUND);
        }
    }

    /**
     * @return true if we're connected to a server that supports teams
     *         (introduced in TFS 2012), false if we're not connected or the
     *         server does not support teams
     */
    public static boolean supportsTeam(final TeamExplorerContext context) {
        /*
         * Avoid using the ServerProjectCache if we're not connected, because it
         * would bring us online or throw exceptions if the server is
         * unreachable.
         */
        if (!context.isConnectedToCollection()) {
            return false;
        }

        final TFSServer server = context.getServer();

        if (server == null) {
            return false;
        }

        if (context.isConnected()) {
            return server.getProjectCache().supportsTeam();
        } else {
            return ((TeamProjectCollectionTeamStore) server.getConnection().getClient(
                TeamProjectCollectionTeamStore.class)).supportsTeam();
        }
    }

    public static boolean supportsGit(final TeamExplorerContext context) {
        return supportsSourceControlFlag(context, SourceControlCapabilityFlags.GIT);
    }

    public static boolean supportsTfvc(final TeamExplorerContext context) {
        return supportsSourceControlFlag(context, SourceControlCapabilityFlags.TFS);
    }

    private static boolean supportsSourceControlFlag(
        final TeamExplorerContext context,
        final SourceControlCapabilityFlags flag) {
        if (!context.isConnected()) {
            return false;
        }

        final SourceControlCapabilityFlags flags = context.getSourceControlCapability();
        return flags.contains(flag);
    }

    /**
     * @return true if we're connected to a server that supports
     *         IdentityService2 (introduced in TFS 2010), false if we're not
     *         connected or the server does not support teams
     */
    public static boolean supportsIdentityService2(final TFSTeamProjectCollection tpc) {
        try {
            return ((IdentityManagementService2) tpc.getClient(IIdentityManagementService2.class)).isSupported();
        } catch (final IdentityManagementException e) {
            return false;
        }
    }

    public static boolean supportsMyFavorites(final TFSTeamProjectCollection tpc) {
        if (tpc == null) {
            return false;
        }

        return supportsIdentityService2(tpc);
    }

    public static boolean supportsTeamFavorites(final TFSTeamProjectCollection tpc) {
        if (tpc == null) {
            return false;
        }

        return ((TeamProjectCollectionTeamStore) tpc.getClient(TeamProjectCollectionTeamStore.class)).supportsTeam();
    }

    public static boolean isVersion2010OrGreater(final TeamExplorerContext context) {
        if (!context.isConnectedToCollection()) {
            return false;
        }

        final WebServiceLevel level = context.getServer().getConnection().getVersionControlClient().getServiceLevel();
        return level.getValue() >= WebServiceLevel.TFS_2010.getValue();
    }

    public static boolean isVersion2012OrGreater(final TeamExplorerContext context) {
        if (!context.isConnected()) {
            return false;
        }

        final WebServiceLevel level = context.getServer().getConnection().getVersionControlClient().getServiceLevel();
        return level.getValue() >= WebServiceLevel.TFS_2012_2.getValue();
    }

    private static Section getContainingSection(Composite composite) {
        Section section = null;
        while (composite != null) {
            if (composite instanceof Section) {
                section = (Section) composite;
                break;
            }

            composite = composite.getParent();
        }

        return section;
    }

    /**
     * Helper method to check whether a TE view is open
     *
     * @param viewID
     * @return
     */
    public static boolean isViewUndocked(final String viewID) {
        final IViewPart view = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().findView(viewID);

        return view != null;
    }

    /**
     * Helper method to show a Team Explorer view based on viewID
     *
     * @param viewID
     */
    public static IViewPart showView(final String viewID) {
        ClientTelemetryHelper.sendTeamExplorerPageView(viewID);

        IViewPart view = null;
        try {
            view = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().showView(viewID);
        } catch (final PartInitException e) {
            log.error(e);
        }

        return view;
    }

    public static boolean tryParseProtocolHandlerArguments(final String[] applicationArgs) {

        if (applicationArgs == null) {
            return false;
        }

        boolean found = false;

        for (final String arg : applicationArgs) {
            if (found) {
                final URI uri = URIUtils.newURI(arg);

                if (!uri.getScheme().equalsIgnoreCase(PROTOCOL_HANDLER_SCHEME)) {
                    found = false;
                    continue;
                }

                boolean repoUrlAvailable = false;
                final String[] queryItems = uri.getQuery().split("&"); //$NON-NLS-1$

                for (final String queryItem : queryItems) {
                    if (StringUtil.startsWithIgnoreCase(queryItem, PROTOCOL_HANDLER_URL_PARAM)) {
                        final String value = queryItem.substring(PROTOCOL_HANDLER_URL_PARAM.length());
                        System.setProperty(PROTOCOL_HANDLER_URL_PROPERTY, value);
                        repoUrlAvailable = tryParseGitRepoUrl(value);
                    } else if (StringUtil.startsWithIgnoreCase(queryItem, PROTOCOL_HANDLER_ENCODING_PARAM)) {
                        final String value = queryItem.substring(PROTOCOL_HANDLER_ENCODING_PARAM.length());
                        System.setProperty(PROTOCOL_HANDLER_ENCODING_PROPERTY, value);
                    } else if (StringUtil.startsWithIgnoreCase(queryItem, PROTOCOL_HANDLER_BRANCH_PARAM)) {
                        final String value = queryItem.substring(PROTOCOL_HANDLER_BRANCH_PARAM.length());
                        System.setProperty(PROTOCOL_HANDLER_BRANCH_PROPERTY, value);
                    }
                }

                if (repoUrlAvailable) {
                    return true;
                } else {
                    found = false;
                }
            } else if (arg.equalsIgnoreCase(PROTOCOL_HANDLER_ARG)) {
                found = true;
            }
        }

        return false;
    }

    public static boolean hasProtocolHandlerRequest() {
        final String url = System.getProperty(PROTOCOL_HANDLER_URL_PROPERTY, null);
        return !StringUtil.isNullOrEmpty(url);
    }

    public static String getProtocolHandlerServer() {
        return System.getProperty(PROTOCOL_HANDLER_SERVER_PROPERTY, StringUtil.EMPTY);
    }

    public static String getProtocolHandlerProject() {
        return System.getProperty(PROTOCOL_HANDLER_PROJECT_PROPERTY, StringUtil.EMPTY);
    }

    public static String getProtocolHandlerBranch() {
        return System.getProperty(PROTOCOL_HANDLER_BRANCH_PROPERTY, StringUtil.EMPTY);
    }

    public static String getProtocolHandlerRepository() {
        return System.getProperty(PROTOCOL_HANDLER_REPOSITORY_PROPERTY, StringUtil.EMPTY);
    }

    public static String getProtocolHandlerUrl() {
        return System.getProperty(PROTOCOL_HANDLER_URL_PROPERTY, StringUtil.EMPTY);
    }

    public static String getProtocolHandlerEncodedUrl() {
        return URIUtils.newURI(System.getProperty(PROTOCOL_HANDLER_URL_PROPERTY, StringUtil.EMPTY)).toASCIIString();
    }

    private static boolean tryParseGitRepoUrl(final String repoUrl) {
        final URI uri = URIUtils.newURI(repoUrl);
        final String[] pathItems = uri.getPath().split("/"); //$NON-NLS-1$
        int n = pathItems.length;

        if (n < 2 || !GIT_TOKEN.equals(pathItems[n - 2])) {
            return false;
        }

        final String repository = pathItems[n - 1];
        String path;
        String project;

        // Note that because uri.getPath() is absolute, pathItems[0] is always
        // an empty string.
        if (ServerURIUtils.isHosted(uri)) {
            /*
             * @formatter off            
             * Possible URLs are:
             * https://account.visualstudio.com/_git/repository                           (n=3)
             * https://account.visualstudio.com/project/_git/repository                   (n=4) 
             * https://account.visualstudio.com/DefaultCollection/_git/repository         (n=4) 
             * https://account.visualstudio.com/DefaultCollection/project/_git/repository (n=5) 
             * in the future, "DefaultCollection" will be replaced with "Organization"
             * @formatter:on          
             */
            switch (n) {
                case 5:
                    project = pathItems[2];
                    path = '/' + pathItems[1];
                    break;
                case 4:
                    if (DEFAULT_COLLECTION_TOKEN.equalsIgnoreCase(pathItems[1])) {
                        project = repository;
                        path = '/' + pathItems[1];
                    } else {
                        project = pathItems[1];
                        path = null;
                    }
                    break;
                case 3:
                    project = repository;
                    path = null;
                    break;
                default:
                    return false;
            }
        } else {
            /*
             * @formatter off            
             * Possible URLs are:
             * https://server:port/path/collection/_git/repository                           (n=5)
             * https://server:port/path/collection/project/_git/repository                   (n=6) 
             * @formatter:on          
             */
            switch (n) {
                case 6:
                    project = pathItems[3];
                    path = '/' + pathItems[1] + '/' + pathItems[2];
                    break;
                case 5:
                    project = repository;
                    path = '/' + pathItems[1] + '/' + pathItems[2];
                    break;
                default:
                    return false;
            }
        }

        System.setProperty(
            PROTOCOL_HANDLER_SERVER_PROPERTY,
            URIUtils.newURI(uri.getScheme(), uri.getAuthority(), path, null, null).toString());
        System.setProperty(PROTOCOL_HANDLER_REPOSITORY_PROPERTY, repository);
        System.setProperty(PROTOCOL_HANDLER_PROJECT_PROPERTY, project);

        return true;
    }

    public static void removeProtocolHandlerArguments() {
        System.getProperties().remove(PROTOCOL_HANDLER_URL_PROPERTY);
        System.getProperties().remove(PROTOCOL_HANDLER_SERVER_PROPERTY);
        System.getProperties().remove(PROTOCOL_HANDLER_BRANCH_PROPERTY);
        System.getProperties().remove(PROTOCOL_HANDLER_REPOSITORY_PROPERTY);
        System.getProperties().remove(PROTOCOL_HANDLER_PROJECT_PROPERTY);
        System.getProperties().remove(PROTOCOL_HANDLER_ENCODING_PROPERTY);
    }

    public static void registerProtocolHandler() {
        if (!GitHelpers.isEGitInstalled(false)) {
            return;
        }

        if (Platform.isCurrentPlatform(Platform.WINDOWS)) {
            try {
                final String launcher = System.getProperty(PROTOCOL_HANDLER_LAUNCHER_PROPERTY, null);
                final String command = "\"" + launcher + "\" \"%1\""; //$NON-NLS-1$ //$NON-NLS-2$

                final RegistryKey handlerKey =
                    new RegistryKey(RootKey.HKEY_CLASSES_ROOT, PROTOCOL_HANDLER_REGISTRY_KEY);

                if (handlerKey.exists()) {
                    final RegistryValue value = handlerKey.getDefaultValue();
                    if (value != null && command.equalsIgnoreCase(value.getStringValue())) {
                        return;
                    }
                }

                final File script = createRegeditFile(launcher);

                final Process cmd = Runtime.getRuntime().exec("cmd.exe /C regedit.exe /s \"" + script.getPath() + "\""); //$NON-NLS-1$ //$NON-NLS-2$
                int rc = cmd.waitFor();
                script.delete();

                log.info("rc = " + rc); //$NON-NLS-1$
            } catch (final IOException e) {
                log.error("Error accessing Windows registry:", e); //$NON-NLS-1$
            } catch (final InterruptedException e) {
                log.warn("Protocol handler registration has been cancelled."); //$NON-NLS-1$
            }
        }
    }

    private static File createRegeditFile(final String launcher) throws IOException {
        final File script = File.createTempFile("CreateKeys", ".reg"); //$NON-NLS-1$ //$NON-NLS-2$
        final FileWriter fileWriter = new FileWriter(script);
        final BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
        try {
            bufferedWriter.write("Windows Registry Editor Version 5.00\r\n" + //$NON-NLS-1$
                "[-HKEY_CLASSES_ROOT\\vsoe]\r\n" + //$NON-NLS-1$
                "[HKEY_CLASSES_ROOT\\" + PROTOCOL_HANDLER_REGISTRY_KEY + "]\r\n" + //$NON-NLS-1$ //$NON-NLS-2$
                "@=\"\\\"" + launcher.replace("\\", "\\\\") + "\\\" \\\"%1\\\"\""); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
        } finally {
            bufferedWriter.close();
            fileWriter.close();
        }
        return script;
    }

}
