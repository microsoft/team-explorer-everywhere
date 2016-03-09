// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.teambuild.prefs;

import org.eclipse.core.runtime.Preferences;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import com.microsoft.tfs.client.common.TFSCommonClientPlugin;
import com.microsoft.tfs.client.common.prefs.PreferenceConstants;
import com.microsoft.tfs.client.common.server.TFSServer;
import com.microsoft.tfs.client.common.ui.TFSCommonUIClientPlugin;
import com.microsoft.tfs.client.common.ui.framework.layout.GridDataBuilder;
import com.microsoft.tfs.client.common.ui.helpers.AutomationIDHelper;
import com.microsoft.tfs.client.common.ui.prefs.BasePreferencePage;
import com.microsoft.tfs.client.common.ui.prefs.UIPreferenceConstants;
import com.microsoft.tfs.client.common.ui.teambuild.Messages;

public class BuildNotificationPreferencePage extends BasePreferencePage {
    public static final String REFRESH_INTERVAL_TEXT_ID = "WatchedBuildsPreferencePage.refreshInterval"; //$NON-NLS-1$
    public static final String NOTIFY_BUILD_SUCCESS_BUTTON_ID = "WatchedBuildsPreferencePage.notifyBuildSuccess"; //$NON-NLS-1$
    public static final String NOTIFY_BUILD_PARTIALLY_SUCCEEDED_BUTTON_ID =
        "WatchedBuildsPreferencePage.notifyBuildPartiallySucceeded"; //$NON-NLS-1$
    public static final String NOTIFY_BUILD_FAILURE_BUTTON_ID = "WatchedBuildsPreferencePage.notifyBuildFailure"; //$NON-NLS-1$

    private Text refreshTimeText;

    private Button notifyBuildSuccessButton;
    private Button notifyBuildPartiallySucceededButton;
    private Button notifyBuildFailureButton;

    public BuildNotificationPreferencePage() {
        super();
    }

    public BuildNotificationPreferencePage(final String title) {
        super(title);
    }

    public BuildNotificationPreferencePage(final String title, final ImageDescriptor image) {
        super(title, image);
    }

    // get preference settings from the preference store
    private void initializeValues() {
        /*
         * Note: The interval preference is from the non-UI common client
         * plug-in because BuildStatusManager is in that plug-in and uses the
         * pref directly.
         *
         * TODO Use a non-deprecated method to access the preferences in the
         * non-UI common client plug-in or refactor TFSServer and
         * BuildStatusManager not to use a pref (pass in the value).
         */
        final Preferences nonUIPrefs = TFSCommonClientPlugin.getDefault().getPluginPreferences();

        int refreshIntervalMillis = nonUIPrefs.getInt(PreferenceConstants.BUILD_STATUS_REFRESH_INTERVAL);

        if (refreshIntervalMillis <= 0) {
            refreshIntervalMillis = nonUIPrefs.getDefaultInt(PreferenceConstants.BUILD_STATUS_REFRESH_INTERVAL);
        }

        int refreshIntervalMins = 0;
        if (refreshIntervalMillis > 0) {
            refreshIntervalMins = refreshIntervalMillis / (60 * 1000);
        }
        if (refreshIntervalMins <= 0) {
            refreshIntervalMins = 5;
        }

        refreshTimeText.setText(Integer.toString(refreshIntervalMins));

        /*
         * These prefs are in the UI client with most other prefs.
         */

        final IPreferenceStore uiPrefs = TFSCommonUIClientPlugin.getDefault().getPreferenceStore();

        final boolean notifySuccess = uiPrefs.contains(UIPreferenceConstants.BUILD_NOTIFICATION_SUCCESS)
            ? uiPrefs.getBoolean(UIPreferenceConstants.BUILD_NOTIFICATION_SUCCESS)
            : uiPrefs.getDefaultBoolean(UIPreferenceConstants.BUILD_NOTIFICATION_SUCCESS);

        final boolean notifyPartiallySucceeded =
            uiPrefs.contains(UIPreferenceConstants.BUILD_NOTIFICATION_PARTIALLY_SUCCEEDED)
                ? uiPrefs.getBoolean(UIPreferenceConstants.BUILD_NOTIFICATION_PARTIALLY_SUCCEEDED)
                : uiPrefs.getDefaultBoolean(UIPreferenceConstants.BUILD_NOTIFICATION_PARTIALLY_SUCCEEDED);

        final boolean notifyFailure = uiPrefs.contains(UIPreferenceConstants.BUILD_NOTIFICATION_FAILURE)
            ? uiPrefs.getBoolean(UIPreferenceConstants.BUILD_NOTIFICATION_FAILURE)
            : uiPrefs.getDefaultBoolean(UIPreferenceConstants.BUILD_NOTIFICATION_FAILURE);

        notifyBuildSuccessButton.setSelection(notifySuccess);
        notifyBuildPartiallySucceededButton.setSelection(notifyPartiallySucceeded);
        notifyBuildFailureButton.setSelection(notifyFailure);
    }

    @Override
    protected void performDefaults() {
        // TODO see TODO in initializeValues()

        final Preferences nonUIPrefs = TFSCommonClientPlugin.getDefault().getPluginPreferences();

        final int refreshIntervalMillis = nonUIPrefs.getDefaultInt(PreferenceConstants.BUILD_STATUS_REFRESH_INTERVAL);

        int refreshIntervalMins = 0;
        if (refreshIntervalMillis > 0) {
            refreshIntervalMins = refreshIntervalMillis / (60 * 1000);
        }
        if (refreshIntervalMins == 0) {
            refreshIntervalMins = 5;
        }

        refreshTimeText.setText(Integer.toString(refreshIntervalMins));

        // Back to normal UI prefs

        final IPreferenceStore uiPrefs = TFSCommonUIClientPlugin.getDefault().getPreferenceStore();

        notifyBuildSuccessButton.setSelection(
            uiPrefs.getDefaultBoolean(UIPreferenceConstants.BUILD_NOTIFICATION_SUCCESS));
        notifyBuildPartiallySucceededButton.setSelection(
            uiPrefs.getDefaultBoolean(UIPreferenceConstants.BUILD_NOTIFICATION_PARTIALLY_SUCCEEDED));
        notifyBuildFailureButton.setSelection(
            uiPrefs.getDefaultBoolean(UIPreferenceConstants.BUILD_NOTIFICATION_FAILURE));

        super.performDefaults();
    }

    private int getRefreshInterval() {
        int refreshIntervalMins = -1;

        try {
            refreshIntervalMins = Integer.parseInt(refreshTimeText.getText());
        } catch (final Exception e) {
        }

        if (refreshIntervalMins < 1) {
            MessageDialog.openError(
                getShell(),
                Messages.getString("BuildNotificationPreferencePage.InvalidTimeTitle"), //$NON-NLS-1$
                Messages.getString("BuildNotificationPreferencePage.InvalidTimeMessage")); //$NON-NLS-1$
            return -1;
        }

        return (refreshIntervalMins * 60 * 1000);
    }

    @Override
    public boolean performOk() {
        final int refreshTime = getRefreshInterval();

        if (refreshTime < 1) {
            return false;
        }

        // TODO see TODO in initializeValues()

        final Preferences nonUIPrefs = TFSCommonClientPlugin.getDefault().getPluginPreferences();

        nonUIPrefs.setValue(PreferenceConstants.BUILD_STATUS_REFRESH_INTERVAL, refreshTime);

        TFSCommonClientPlugin.getDefault().savePluginPreferences();

        // Back to normal UI prefs

        final IPreferenceStore uiPrefs = TFSCommonUIClientPlugin.getDefault().getPreferenceStore();

        uiPrefs.setValue(UIPreferenceConstants.BUILD_NOTIFICATION_SUCCESS, notifyBuildSuccessButton.getSelection());
        uiPrefs.setValue(
            UIPreferenceConstants.BUILD_NOTIFICATION_PARTIALLY_SUCCEEDED,
            notifyBuildPartiallySucceededButton.getSelection());
        uiPrefs.setValue(UIPreferenceConstants.BUILD_NOTIFICATION_FAILURE, notifyBuildFailureButton.getSelection());

        final TFSServer currentServer =
            TFSCommonUIClientPlugin.getDefault().getProductPlugin().getServerManager().getDefaultServer();

        if (currentServer != null) {
            currentServer.getBuildStatusManager().setRefreshInterval(refreshTime);
        }

        return super.performOk();
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * org.eclipse.jface.preference.PreferencePage#createContents(org.eclipse
     * .swt.widgets.Composite)
     */
    @Override
    protected Control createContents(final Composite parent) {
        final Composite container = new Composite(parent, SWT.NONE);

        final GridLayout layout = new GridLayout(3, false);
        layout.marginWidth = 0;
        layout.marginHeight = 0;
        layout.horizontalSpacing = getHorizontalSpacing();
        layout.verticalSpacing = getVerticalSpacing();

        container.setLayout(layout);

        final Label refreshDescriptionLabel = new Label(container, SWT.NONE);
        refreshDescriptionLabel.setText(Messages.getString("BuildNotificationPreferencePage.RefreshPromptMessage")); //$NON-NLS-1$

        refreshTimeText = new Text(container, SWT.BORDER);
        refreshTimeText.setTextLimit(3);
        GridDataBuilder.newInstance().wCHint(refreshTimeText, 5).applyTo(refreshTimeText);
        AutomationIDHelper.setWidgetID(refreshTimeText, REFRESH_INTERVAL_TEXT_ID);
        refreshTimeText.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(final ModifyEvent e) {
                setValid(refreshTimeText.getText().length() > 0);
            }
        });

        final Label refreshUnitsLabel = new Label(container, SWT.NONE);
        refreshUnitsLabel.setText(Messages.getString("BuildNotificationPreferencePage.RefreshUnitsMessage")); //$NON-NLS-1$

        final Label spacerLabel = new Label(container, SWT.NONE);
        GridDataBuilder.newInstance().hSpan(3).applyTo(spacerLabel);

        final Label notifyDescriptionLabel = new Label(container, SWT.WRAP);
        notifyDescriptionLabel.setText(Messages.getString("BuildNotificationPreferencePage.NotifyPromptMessage")); //$NON-NLS-1$
        GridDataBuilder.newInstance().hSpan(3).applyTo(notifyDescriptionLabel);

        notifyBuildSuccessButton = new Button(container, SWT.CHECK);
        AutomationIDHelper.setWidgetID(notifyBuildSuccessButton, NOTIFY_BUILD_SUCCESS_BUTTON_ID);
        notifyBuildSuccessButton.setText(Messages.getString("BuildNotificationPreferencePage.NotifySucceededLabel")); //$NON-NLS-1$
        GridDataBuilder.newInstance().hSpan(3).applyTo(notifyBuildSuccessButton);

        notifyBuildPartiallySucceededButton = new Button(container, SWT.CHECK);
        AutomationIDHelper.setWidgetID(notifyBuildPartiallySucceededButton, NOTIFY_BUILD_PARTIALLY_SUCCEEDED_BUTTON_ID);
        notifyBuildPartiallySucceededButton.setText(
            Messages.getString("BuildNotificationPreferencePage.NotifyPartiallySucceededButtonLabel")); //$NON-NLS-1$
        GridDataBuilder.newInstance().hSpan(3).applyTo(notifyBuildPartiallySucceededButton);

        notifyBuildFailureButton = new Button(container, SWT.CHECK);
        AutomationIDHelper.setWidgetID(notifyBuildFailureButton, NOTIFY_BUILD_FAILURE_BUTTON_ID);
        notifyBuildFailureButton.setText(Messages.getString("BuildNotificationPreferencePage.NotifyFailedButtonLabel")); //$NON-NLS-1$
        GridDataBuilder.newInstance().hSpan(3).applyTo(notifyBuildFailureButton);

        initializeValues();

        return container;
    }
}
