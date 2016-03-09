// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.prefs;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.SWT;

import com.microsoft.tfs.client.common.ui.TFSCommonUIClientPlugin;
import com.microsoft.tfs.client.common.ui.helpers.WorkItemEditorHelper;

public class UIPreferenceInitializer extends AbstractPreferenceInitializer {
    @Override
    public void initializeDefaultPreferences() {
        final IPreferenceStore prefs = TFSCommonUIClientPlugin.getDefault().getPreferenceStore();

        prefs.setDefault(UIPreferenceConstants.RECONNECT_AT_STARTUP, true);
        prefs.setDefault(UIPreferenceConstants.CONNECT_MAPPED_PROJECTS_AT_IMPORT, true);
        prefs.setDefault(UIPreferenceConstants.ACCEPT_UNTRUSTED_CERTIFICATES, false);
        prefs.setDefault(UIPreferenceConstants.EMBEDDED_WEB_BROWSER_TYPE, SWT.NONE);

        // default lock level for automatic checkout from edits, and default in
        // the checkout dialog
        // default for orcas: lock level is "unchanged"
        prefs.setDefault(
            UIPreferenceConstants.CHECKOUT_LOCK_LEVEL,
            UIPreferenceConstants.CHECKOUT_LOCK_LEVEL_UNCHANGED);

        prefs.setDefault(
            UIPreferenceConstants.WIT_DOUBLE_CLICK_FILE_ATTACHMENT,
            UIPreferenceConstants.WIT_DOUBLE_CLICK_FILE_ATTACHMENT_LAUNCH_BROWSER);

        prefs.setDefault(UIPreferenceConstants.SYNC_DECORATE, true);
        prefs.setDefault(UIPreferenceConstants.SYNC_REFRESH_LOCAL, false);
        prefs.setDefault(UIPreferenceConstants.SYNC_REFRESH_REMOTE, false);
        prefs.setDefault(UIPreferenceConstants.SYNC_REFRESH_REMOTE_INTERVAL, 3600);

        prefs.setDefault(UIPreferenceConstants.LABEL_DECORATION_DECORATE_FOLDERS, false);
        prefs.setDefault(UIPreferenceConstants.LABEL_DECORATION_SHOW_CHANGESET, true);
        prefs.setDefault(UIPreferenceConstants.LABEL_DECORATION_SHOW_SERVER_ITEM, false);
        prefs.setDefault(UIPreferenceConstants.LABEL_DECORATION_SHOW_IGNORED_STATUS, true);

        prefs.setDefault(UIPreferenceConstants.BUILD_NOTIFICATION_SUCCESS, true);
        prefs.setDefault(UIPreferenceConstants.BUILD_NOTIFICATION_PARTIALLY_SUCCEEDED, true);
        prefs.setDefault(UIPreferenceConstants.BUILD_NOTIFICATION_FAILURE, true);

        prefs.setDefault(UIPreferenceConstants.GATED_CONFIRMATION_PRESERVE_PENDING_CHANGES, true);

        prefs.setDefault(UIPreferenceConstants.WORK_ITEM_EDITOR_ID, WorkItemEditorHelper.EMBEDDED_WEB_ACCESS_EDITOR_ID);

        prefs.setDefault(UIPreferenceConstants.DETECT_LOCAL_CHANGES_ON_MANUAL_RECONNECT, true);
        prefs.setDefault(UIPreferenceConstants.DETECT_LOCAL_CHANGES_ON_AUTOMATIC_RECONNECT, true);

        prefs.setDefault(UIPreferenceConstants.RECONNECT_PROJECTS_TO_NEW_REPOSITORIES, false);

        prefs.setDefault(UIPreferenceConstants.CONSOLE_SHOW_ON_NEW_MESSAGE, false);
        prefs.setDefault(UIPreferenceConstants.CONSOLE_SHOW_ON_NEW_WARNING, true);
        prefs.setDefault(UIPreferenceConstants.CONSOLE_SHOW_ON_NEW_ERROR, true);

        prefs.setDefault(UIPreferenceConstants.AUTO_RESOLVE_CONFLICTS, true);
    }
}
