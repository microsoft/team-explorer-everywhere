// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.helpers;

import java.net.URI;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

import com.microsoft.tfs.client.common.codemarker.CodeMarker;
import com.microsoft.tfs.client.common.codemarker.CodeMarkerDispatch;
import com.microsoft.tfs.client.common.commands.wit.GetWorkItemByIDCommand;
import com.microsoft.tfs.client.common.framework.command.ICommandExecutor;
import com.microsoft.tfs.client.common.server.TFSServer;
import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.TFSCommonUIClientPlugin;
import com.microsoft.tfs.client.common.ui.browser.BrowserFacade;
import com.microsoft.tfs.client.common.ui.browser.BrowserFacade.LaunchMode;
import com.microsoft.tfs.client.common.ui.framework.command.UICommandExecutorFactory;
import com.microsoft.tfs.client.common.ui.framework.helper.MessageBoxHelpers;
import com.microsoft.tfs.client.common.ui.framework.helper.ShellUtils;
import com.microsoft.tfs.client.common.ui.prefs.UIPreferenceConstants;
import com.microsoft.tfs.client.common.ui.teamexplorer.helpers.WorkItemHelpers;
import com.microsoft.tfs.client.common.ui.webaccessintegration.editors.WebAccessWorkItemEditorInput;
import com.microsoft.tfs.client.common.ui.wit.form.WorkItemEditorInfo;
import com.microsoft.tfs.client.common.ui.wit.form.WorkItemEditorInput;
import com.microsoft.tfs.client.common.ui.wit.query.UIQueryUtils;
import com.microsoft.tfs.core.clients.workitem.WorkItem;
import com.microsoft.tfs.core.clients.workitem.WorkItemClient;
import com.microsoft.tfs.core.exceptions.NotSupportedException;
import com.microsoft.tfs.core.util.TSWAHyperlinkBuilder;
import com.microsoft.tfs.util.NewlineUtils;

public final class WorkItemEditorHelper {
    // The logger.
    private static final Log log = LogFactory.getLog(WorkItemEditorHelper.class);

    // The work item editor extension point ID.
    public static final String WORK_ITEM_EDITORS_EXTENSION_POINT_ID =
        "com.microsoft.tfs.client.common.ui.workItemEditors"; //$NON-NLS-1$

    // Internal work item editors IDs.
    public static final String EXTERNAL_WEB_ACCESS_EDITOR_ID = "com.microsoft.tfs.client.common.ui.teamwebaccess"; //$NON-NLS-1$
    public static final String EMBEDDED_WEB_ACCESS_EDITOR_ID =
        "com.microsoft.tfs.client.common.ui.webaccessintegration.editors.WebAccessWorkItemEditor"; //$NON-NLS-1$

    // Define code markers.
    public static final CodeMarker CODEMARKER_OPEN_COMPLETE =
        new CodeMarker("com.microsoft.tfs.client.common.ui.wit.form.WorkItemEditor#openComplete"); //$NON-NLS-1$

    // Incrementing counter used to generate unique names for new work items.
    private static int nextDocumentNumber = 0;
    private static Object documentNumberLock = new Object();

    /**
     * Get the registered work item editor display names and workbench editor
     * IDs in an order where the out of box editors appear first.
     *
     *
     * @return A list containing the display name and editor ID for all
     *         registered work item editors.
     */
    public static List<WorkItemEditorInfo> getWorkItemEditors() {
        final IExtensionRegistry registry = Platform.getExtensionRegistry();
        final IExtensionPoint extensionPoint = registry.getExtensionPoint(WORK_ITEM_EDITORS_EXTENSION_POINT_ID);
        final IConfigurationElement[] elements = extensionPoint.getConfigurationElements();

        final List<WorkItemEditorInfo> editorInfos = new ArrayList<WorkItemEditorInfo>();

        WorkItemEditorInfo embeddedEditor = null;
        WorkItemEditorInfo externalBrowser = null;

        // Retrieve the work item editor extensions and created an ordered list
        // of editors. The 2012 embedded web access editor will appear first in
        // the list, the SWT editor second, and the external web access third.
        // All others will appear after these three in the order they are read
        // from the configuration.
        for (final IConfigurationElement element : elements) {
            final String id = element.getAttribute("id"); //$NON-NLS-1$
            final String displayName = element.getAttribute("displayName"); //$NON-NLS-1$
            final WorkItemEditorInfo editorInfo = new WorkItemEditorInfo(id, displayName);

            // Set aside the known out of box editors so they can be inserted at
            // the head of the list in the order Embedded, SWT, External.
            if (id.equals(EMBEDDED_WEB_ACCESS_EDITOR_ID)) {
                embeddedEditor = editorInfo;
            } else if (id.equals(EXTERNAL_WEB_ACCESS_EDITOR_ID)) {
                externalBrowser = editorInfo;
            } else {
                editorInfos.add(editorInfo);
            }
        }

        // External browser is second in the list.
        if (externalBrowser != null) {
            editorInfos.add(0, externalBrowser);
        }

        // Embedded is first in the list.
        if (embeddedEditor != null) {
            editorInfos.add(0, embeddedEditor);
        }

        return editorInfos;
    }

    /**
     * Open the work item with the specified ID in the preferred editor.
     *
     *
     * @param server
     *        The TFS server.
     * @param workItemID
     *        The ID of the work item to be opened.
     */
    public static void openEditor(final TFSServer server, final int workItemID) {
        final Shell shell = ShellUtils.getWorkbenchShell();
        final WorkItemClient workItemClient = server.getConnection().getWorkItemClient();

        // Get the work item object for the specified ID.
        final GetWorkItemByIDCommand command = new GetWorkItemByIDCommand(workItemClient, workItemID);
        final ICommandExecutor executor = UICommandExecutorFactory.newUICommandExecutor(shell);
        final IStatus status = executor.execute(command);

        // Bail if the command failed.
        if (!status.isOK()) {
            return;
        }

        // Get the work item from the command result.
        final WorkItem workItem = command.getWorkItem();
        if (workItem == null) {
            WorkItemHelpers.showWorkItemDoesNotExistError(shell, workItemID);
            return;
        }

        // Open the specified work item in the preferred editor.
        WorkItemEditorHelper.openEditor(server, workItem);
    }

    /**
     * Open the specified work item in the preferred editor.
     *
     *
     * @param server
     *        The server hosting the work item.
     * @param workItem
     *        The work item to open.
     */
    public static void openEditor(final TFSServer server, final WorkItem workItem) {
        openEditor(server, workItem, null);
    }

    /**
     *
     *
     *
     * @param server
     * @param workItem
     * @param openWithEditorID
     */
    public static void openEditor(final TFSServer server, final WorkItem workItem, final String openWithEditorID) {
        if (log.isDebugEnabled()) {
            final String messageFormat = "openEditor({0})"; //$NON-NLS-1$
            final String message = MessageFormat.format(messageFormat, workItem);
            log.debug(message);
        }

        if (!UIQueryUtils.verifyAccessToWorkItem(workItem)) {
            return;
        }

        IEditorInput input = null;
        String editorID = (openWithEditorID == null) ? getPreferredWorkItemEditorID() : openWithEditorID;

        int documentNumber = -1;

        if (workItem.getFields().getID() == 0) {
            synchronized (documentNumberLock) {
                documentNumber = ++nextDocumentNumber;
            }
        }

        if (editorID.equals(EMBEDDED_WEB_ACCESS_EDITOR_ID)) {
            input = new WebAccessWorkItemEditorInput(server, workItem, documentNumber);
        } else {
            input = new WorkItemEditorInput(server, workItem, documentNumber);
        }

        final IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();

        // Launch TSWA if the preferred editor is web access. Fall through to
        // the internal editor if there are any problems launching the web
        // access browser.
        if (editorID.equals(EXTERNAL_WEB_ACCESS_EDITOR_ID)) {
            try {
                final Shell shell = Display.getCurrent().getActiveShell();
                openWorkItemInExternalBrowser(shell, (WorkItemEditorInput) input);
                return;
            } catch (final RuntimeException e) {
                // fall through and open in the default built-in editor
                editorID = EMBEDDED_WEB_ACCESS_EDITOR_ID;
            }
        }

        // Open the work item in the specified Eclipse work item editor.
        try {
            page.openEditor(input, editorID);
            CodeMarkerDispatch.dispatch(CODEMARKER_OPEN_COMPLETE);
        } catch (final PartInitException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Retrieve the preferred editor ID from the preferences store and verify
     * the editor is still contributed. The internal work item editor ID is
     * returned as a fallback if there are any errors.
     *
     *
     * @return The editor ID of the preferred editor or the internal work item
     *         editor if the preferred editor is no longer contributed.
     */
    private static String getPreferredWorkItemEditorID() {
        // Find the preferred work item editor.
        final IPreferenceStore preferences = TFSCommonUIClientPlugin.getDefault().getPreferenceStore();
        final String prefValue = preferences.getString(UIPreferenceConstants.WORK_ITEM_EDITOR_ID);

        // Check that the preferred work item editor still exists.
        final IExtensionRegistry registry = Platform.getExtensionRegistry();
        final IExtensionPoint extensionPoint = registry.getExtensionPoint(WORK_ITEM_EDITORS_EXTENSION_POINT_ID);
        final IConfigurationElement[] elements = extensionPoint.getConfigurationElements();

        // Check all contributed editors for the preference value.
        for (final IConfigurationElement element : elements) {
            final String id = element.getAttribute("id"); //$NON-NLS-1$
            if (id.equals(prefValue)) {
                // Use the preferred contributed editor.
                return id;
            }
        }

        // Default to embedded Web Access editor
        return EMBEDDED_WEB_ACCESS_EDITOR_ID;
    }

    /**
     * Launch the web access editor for for the given WorkItemEditorInput.
     * Eclipse will have no knowledge of this edit session regardless of whether
     * the edit session is hosted in an eclipse browser.
     *
     *
     * @param shell
     *        A SWT shell.
     * @param input
     *        The work item editor input.
     */
    private static void openWorkItemInExternalBrowser(final Shell shell, final WorkItemEditorInput input) {
        try {
            // Build a web access URL for the given work item and connection.
            final WorkItem workItem = input.getWorkItem();
            final URI uri =
                workItemToWebAccessURI(input.getServer(), input.getWorkItem(), input.getDocumentNumber(), false);

            // Display the web access editor in Eclipse's preferred browser.
            shell.getDisplay().asyncExec(new Runnable() {
                @Override
                public void run() {
                    // Force the browser to launch externally as we are doing
                    // "Open in Browser"
                    BrowserFacade.launchURL(
                        uri,
                        input.getName(),
                        input.getName(),
                        String.valueOf(workItem.getID()),
                        LaunchMode.EXTERNAL);
                }
            });
        } catch (final NotSupportedException e) {
            shell.getDisplay().asyncExec(new Runnable() {
                @Override
                public void run() {
                    MessageBoxHelpers.errorMessageBox(
                        shell,
                        Messages.getString("BaseQueryWebAccessAction.ErrorDialogTitle"), //$NON-NLS-1$
                        Messages.getString("BaseQueryWebAccessAction.ErrorDialogText")); //$NON-NLS-1$
                }
            });
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Generate a team web access URL for the specified server and work item.
     *
     *
     * @param server
     *        The current TFS server.
     * @param workItem
     *        The target work item.
     * @param hosted
     *        True if the resulting URI content will be hosted in an editor.
     * @return A team web access URI for the specified work item.
     */
    public static URI workItemToWebAccessURI(
        final TFSServer server,
        final WorkItem workItem,
        final int titleId,
        final boolean hosted) {
        final TSWAHyperlinkBuilder tswaBuilder = new TSWAHyperlinkBuilder(server.getConnection(), hosted);

        final URI uri;
        if (workItem.getID() == 0) {
            // Construct a URL to create a new work item.
            final String projectURI = workItem.getProject().getURI();
            final String workItemType = workItem.getType().getName();

            uri = tswaBuilder.getNewWorkItemURL(projectURI, workItemType, titleId);
        } else {
            // Construct a URL for an existing work item.
            final int workItemID = workItem.getID();
            uri = tswaBuilder.getWorkItemEditorURL(workItemID);
        }

        return uri;
    }

    public static String createGitCommitWorkItemsLink(final WorkItem[] workItems) {
        final StringBuilder sb = new StringBuilder();

        for (final WorkItem workItem : workItems) {
            sb.append("#"); //$NON-NLS-1$
            sb.append(String.valueOf(workItem.getID()));
            sb.append("\t-\t"); //$NON-NLS-1$
            sb.append(workItem.getTitle());
            sb.append(NewlineUtils.PLATFORM_NEWLINE);
        }

        return sb.toString();
    }
}
