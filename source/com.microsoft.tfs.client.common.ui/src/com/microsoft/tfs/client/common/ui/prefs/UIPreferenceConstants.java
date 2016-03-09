// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.prefs;

/**
 * Contains definitions of all common UI client preference keys (and some
 * values) for storage via <b>this plug-in's</b> Eclipse preference store.
 */
public class UIPreferenceConstants {
    // Whether to prompt before checkin from the pending changes view
    public static final String PROMPT_BEFORE_CHECKIN = "com.microsoft.tfs.vc.promptBeforeCheckin"; //$NON-NLS-1$

    // Whether to prompt before checking out a file when the user starts to edit
    // it
    public static final String PROMPT_BEFORE_CHECKOUT = "com.microsoft.tfs.vc.promptBeforeCheckout"; //$NON-NLS-1$

    // Default lock level for checkout
    public static final String CHECKOUT_LOCK_LEVEL = "com.microsoft.tfs.vc.checkoutLockLevel"; //$NON-NLS-1$
    public static final String CHECKOUT_LOCK_LEVEL_CHECKOUT = "checkout"; //$NON-NLS-1$
    public static final String CHECKOUT_LOCK_LEVEL_CHECKIN = "checkin"; //$NON-NLS-1$
    public static final String CHECKOUT_LOCK_LEVEL_UNCHANGED = "unchanged"; //$NON-NLS-1$

    // Whether to automatically get the latest version on checkout
    public static final String GET_LATEST_ON_CHECKOUT = "com.microsoft.tfs.vc.getLatestOnCheckout"; //$NON-NLS-1$

    // Automerge conflicts on get and checkin
    public static final String AUTO_RESOLVE_CONFLICTS = "com.microsoft.tfs.vc.autoResolveConflicts"; //$NON-NLS-1$

    public static final String CHECKOUT_SYNCHRONOUS = "com.microsoft.tfs.vc.checkoutSynchronous"; //$NON-NLS-1$
    public static final String CHECKOUT_FOREGROUND = "com.microsoft.tfs.vc.checkoutInForeground"; //$NON-NLS-1$

    // Whether to hide the "All files up to date" message after a "Get"
    public static final String HIDE_ALL_FILES_UP_TO_DATE_MESSAGE = "com.microsoft.tfs.vc.hideAllFilesUpToDatePrompt"; //$NON-NLS-1$

    // Whether to show deleted items in VC Explorer
    public static final String SHOW_DELETED_ITEMS = "com.microsoft.tfs.vc.showDeletedItems"; //$NON-NLS-1$

    // Whether to reconnect automatically at startup
    public static final String RECONNECT_AT_STARTUP = "com.microsoft.tfs.reconnectAtStartup"; //$NON-NLS-1$

    // Whether to automatically connect (set ourselves as repository provider)
    // projects beneath mapped folders
    public static final String CONNECT_MAPPED_PROJECTS_AT_IMPORT = "com.microsoft.tfs.connectMappedProjects"; //$NON-NLS-1$

    // Whether to accept untrusted SSL certificates
    public static final String ACCEPT_UNTRUSTED_CERTIFICATES = "com.microsoft.tfs.acceptUntrustedCertificates"; //$NON-NLS-1$

    // The type of web browser to specify when using the SWT.Browser class. The
    // value is an integer: SWT.NONE (to use the default for this platform, IE
    // on Windows), SWT.MOZILLA, or SWT.WEBKIT
    public static final String EMBEDDED_WEB_BROWSER_TYPE = "com.microsoft.tfs.ui.browser.type"; //$NON-NLS-1$

    // Last used connection profile
    public static final String LAST_SERVER_URI = "com.microsoft.tfs.lastServerURI"; //$NON-NLS-1$

    // Last used project collection for a given configuration server
    public static final String LAST_PROJECT_COLLECTION_ID = "com.microsoft.tfs.lastProjectCollectionID"; //$NON-NLS-1$
    public static final String LAST_PROJECT_COLLECTION_SEPARATOR = ":"; //$NON-NLS-1$

    // Last Explorer workspace and separator (URL follows separator in value)
    public static final String LAST_WORKSPACE_NAME = "com.microsoft.tfs.vc.lastWorkspaceName"; //$NON-NLS-1$
    public static final String LAST_WORKSPACE_SEPARATOR = ";"; //$NON-NLS-1$

    // Whether the Workspace edit dialog is expanded by default
    public static final String WORKSPACE_DIALOG_EXPANDED = "com.microsoft.tfs.workspaceDialogExpanded"; //$NON-NLS-1$

    // Whether to show text (Check In, Shelve, etc.) in the toolbar buttons in
    // the pending changes view
    public static final String HIDE_TEXT_IN_PENDING_CHANGE_VIEW_BUTTONS =
        "com.microsoft.tfs.vc.hideTextInPendingChangeViewButtons"; //$NON-NLS-1$

    // How to handle wit file attachments
    public static final String WIT_DOUBLE_CLICK_FILE_ATTACHMENT = "com.microsoft.tfs.wit.doubleClickFileAttachment"; //$NON-NLS-1$
    public static final String WIT_DOUBLE_CLICK_FILE_ATTACHMENT_LAUNCH_LOCAL = "launchLocal"; //$NON-NLS-1$
    public static final String WIT_DOUBLE_CLICK_FILE_ATTACHMENT_LAUNCH_BROWSER = "launchBrowser"; //$NON-NLS-1$

    // Synchronize preferences
    public static final String SYNC_REFRESH_LOCAL = "com.microsoft.tfs.sync.refreshLocal"; //$NON-NLS-1$
    public static final String SYNC_REFRESH_REMOTE = "com.microsoft.tfs.sync.refreshRemote"; //$NON-NLS-1$
    public static final String SYNC_REFRESH_REMOTE_INTERVAL = "com.microsoft.tfs.sync.refreshRemoteTime"; //$NON-NLS-1$
    public static final String SYNC_DECORATE = "com.microsoft.tfs.sync.decorateLabels"; //$NON-NLS-1$

    // Label decoration preferences (prefix stored separately so event handlers
    // can filter changes to prefs to limit refreshes).
    public static final String LABEL_DECORATION_PREF_PREFIX = "com.microsoft.tfs.decorate."; //$NON-NLS-1$
    public static final String LABEL_DECORATION_DECORATE_FOLDERS = LABEL_DECORATION_PREF_PREFIX + "decorateFolders"; //$NON-NLS-1$
    public static final String LABEL_DECORATION_SHOW_CHANGESET = LABEL_DECORATION_PREF_PREFIX + "showChangeset"; //$NON-NLS-1$
    public static final String LABEL_DECORATION_SHOW_SERVER_ITEM = LABEL_DECORATION_PREF_PREFIX + "showServerItem"; //$NON-NLS-1$
    public static final String LABEL_DECORATION_SHOW_IGNORED_STATUS =
        LABEL_DECORATION_PREF_PREFIX + "showIgnoredStatus"; //$NON-NLS-1$

    // Pending changes control
    public static final String PENDING_CHANGES_CONTROL_COMMENT_ON_TOP =
        "com.microsoft.tfs.vc.pendingChanges.commentTop"; //$NON-NLS-1$

    // Username formatting
    public static final String TFS_USERNAME_FORMAT_OPTIONS = "com.microsoft.tfs.ui.usernameFormatType"; //$NON-NLS-1$
    public static final String TFS_USERNAME_FORMAT_OPTIONS_KEEP_DOMAINS = "keepDomains"; //$NON-NLS-1$
    public static final String TFS_USERNAME_FORMAT_OPTIONS_REMOVE_DOMAINS = "removeDomains"; //$NON-NLS-1$
    public static final String TFS_USERNAME_FORMAT_OPTIONS_RELATIVE_DOMAINS = "relativeDomains"; //$NON-NLS-1$

    // Shelveset details as a dialog or view
    public static final String SHELVESET_DETAILS_UI_TYPE = "com.microsoft.tfs.ui.shelvesetDetails.uiType"; //$NON-NLS-1$
    public static final String SHELVESET_DETAILS_UI_TYPE_VIEW = "view"; //$NON-NLS-1$
    public static final String SHELVESET_DETAILS_UI_TYPE_DIALOG = "dialog"; //$NON-NLS-1$

    // Build result notifications
    public static final String BUILD_NOTIFICATION_SUCCESS = "com.microsoft.tfs.build.notification.succeededBuilds"; //$NON-NLS-1$
    public static final String BUILD_NOTIFICATION_PARTIALLY_SUCCEEDED =
        "com.microsoft.tfs.build.notification.partiallySucceededBuilds"; //$NON-NLS-1$
    public static final String BUILD_NOTIFICATION_FAILURE = "com.microsoft.tfs.build.notification.failedBuilds"; //$NON-NLS-1$

    // Gated check-in confirmation
    public static final String GATED_CONFIRMATION_LAST_BUILD_DEFINITION =
        "com.microsoft.tfs.build.gatedcheckin.lastBuildDefinition"; //$NON-NLS-1$
    public static final String GATED_CONFIRMATION_PRESERVE_PENDING_CHANGES =
        "com.microsoft.tfs.build.gatedcheckin.preservePendingChanges"; //$NON-NLS-1$

    // Shelve/Unshelve dialog MRU
    public static final String SHELVE_DIALOG_NAME_MRU_PREFIX = "com.microsoft.tfs.ui.shelveDialog.name.mru"; //$NON-NLS-1$
    public static final String SHELVESET_SEARCH_CONTROL_OWNER_MRU_PREFIX =
        "com.microsoft.tfs.ui.shelvesetSearchControl.owner.mru"; //$NON-NLS-1$

    // The default work item editor
    public static final String WORK_ITEM_EDITOR_ID = "com.microsoft.tfs.ui.workItemEditor.id"; //$NON-NLS-1$

    // Detect local changes on reconnection
    public static final String DETECT_LOCAL_CHANGES_ON_MANUAL_RECONNECT =
        "com.microsoft.tfs.vc.detectLocalChangesOnReconnect.manual"; //$NON-NLS-1$
    public static final String DETECT_LOCAL_CHANGES_ON_AUTOMATIC_RECONNECT =
        "com.microsoft.tfs.vc.detectLocalChangesOnReconnect.automatic"; //$NON-NLS-1$

    // Reconnect IProjects automatically when a new connection is available
    public static final String RECONNECT_PROJECTS_TO_NEW_REPOSITORIES =
        "com.microsoft.tfs.vc.reconnectProjectsToNewRepositories"; //$NON-NLS-1$

    // Raise the console automatically
    public static final String CONSOLE_SHOW_ON_NEW_MESSAGE = "com.microsoft.tfs.ui.console.raiseOnMessage"; //$NON-NLS-1$
    public static final String CONSOLE_SHOW_ON_NEW_WARNING = "com.microsoft.tfs.ui.console.raiseOnWarning"; //$NON-NLS-1$
    public static final String CONSOLE_SHOW_ON_NEW_ERROR = "com.microsoft.tfs.ui.console.raiseOnError"; //$NON-NLS-1$

    // Team explorer
    public static final String TEAM_EXPLORER_RECENT_SEARCHES =
        "com.microsoft.tfs.ui.teamExplorerControl.recentSearches"; //$NON-NLS-1$
}