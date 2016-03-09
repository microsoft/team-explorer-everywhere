// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.eclipse.ui.prefs;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;

import com.microsoft.tfs.client.common.ui.framework.helper.SWTUtil;
import com.microsoft.tfs.client.common.ui.framework.layout.GridDataBuilder;
import com.microsoft.tfs.client.common.ui.helpers.AutomationIDHelper;
import com.microsoft.tfs.client.common.ui.prefs.BasePreferencePage;
import com.microsoft.tfs.client.common.ui.prefs.UIPreferenceConstants;
import com.microsoft.tfs.client.eclipse.ui.Messages;

/**
 * Preference page for offline / return online behavior.
 *
 * @threadsafety unknown
 */
public class OfflinePreferencePage extends BasePreferencePage {
    public static final String AUTOMATICALLY_RETURN_ONLINE_CHECKBOX_ID =
        "OfflinePreferencePage.reconnectAutomaticallyButton";//$NON-NLS-1$

    public static final String DETECT_WHEN_MANUALLY_RETURNING_ONLINE_CHECKBOX_ID =
        "OfflinePreferencePage.detectChangesOnManualReconnectButton"; //$NON-NLS-1$
    public static final String DETECT_WHEN_AUTOMATICALLY_RETURNIN_ONLINE_CHECKBOX_ID =
        "OfflinePreferencePage.detectChangesOnAutomaticReconnectButton"; //$NON-NLS-1$

    private Button detectChangesOnAutomaticReconnectButton;
    private Button detectChangesOnManualReconnectButton;

    private Button reconnectAutomaticallyButton;

    /**
     * {@inheritDoc}
     */
    @Override
    protected Control createContents(final Composite parent) {
        final Composite container = new Composite(parent, SWT.NONE);

        final GridLayout layout = new GridLayout();
        layout.marginWidth = 0;
        layout.marginHeight = 0;
        layout.horizontalSpacing = getHorizontalSpacing();
        layout.verticalSpacing = getVerticalSpacing();

        container.setLayout(layout);

        final Label summaryLabel = new Label(container, SWT.WRAP);
        summaryLabel.setText(Messages.getString("OfflinePreferencePage.SummaryLabelText")); //$NON-NLS-1$
        GridDataBuilder.newInstance().hGrab().hFill().wCHint(summaryLabel, 20).applyTo(summaryLabel);

        SWTUtil.createGridLayoutSpacer(container);

        reconnectAutomaticallyButton = new Button(container, SWT.CHECK);
        AutomationIDHelper.setWidgetID(reconnectAutomaticallyButton, AUTOMATICALLY_RETURN_ONLINE_CHECKBOX_ID);
        reconnectAutomaticallyButton.setText(Messages.getString("OfflinePreferencePage.ReconnectAutomaticallyMessage")); //$NON-NLS-1$

        SWTUtil.createGridLayoutSpacer(container);

        final Label detectChangesLabel = new Label(container, SWT.WRAP);
        detectChangesLabel.setText(Messages.getString("OfflinePreferencePage.DetectChangesPrompt")); //$NON-NLS-1$
        GridDataBuilder.newInstance().hGrab().hFill().wCHint(detectChangesLabel, 20).applyTo(detectChangesLabel);

        detectChangesOnManualReconnectButton = new Button(container, SWT.CHECK);
        AutomationIDHelper.setWidgetID(
            detectChangesOnManualReconnectButton,
            DETECT_WHEN_MANUALLY_RETURNING_ONLINE_CHECKBOX_ID);
        detectChangesOnManualReconnectButton.setText(
            Messages.getString("OfflinePreferencePage.DetectChangesOnManualConnectionMessage")); //$NON-NLS-1$

        detectChangesOnAutomaticReconnectButton = new Button(container, SWT.CHECK);
        AutomationIDHelper.setWidgetID(
            detectChangesOnAutomaticReconnectButton,
            DETECT_WHEN_AUTOMATICALLY_RETURNIN_ONLINE_CHECKBOX_ID);
        detectChangesOnAutomaticReconnectButton.setText(
            Messages.getString("OfflinePreferencePage.DetectChangesOnAutomaticConnectionMessage")); //$NON-NLS-1$

        initializeValues();

        return container;
    }

    // get preference settings from the preference store
    private void initializeValues() {
        final IPreferenceStore store = getPreferenceStore();

        detectChangesOnManualReconnectButton.setSelection(
            store.getBoolean(UIPreferenceConstants.DETECT_LOCAL_CHANGES_ON_MANUAL_RECONNECT));
        detectChangesOnAutomaticReconnectButton.setSelection(
            store.getBoolean(UIPreferenceConstants.DETECT_LOCAL_CHANGES_ON_AUTOMATIC_RECONNECT));

        reconnectAutomaticallyButton.setSelection(
            store.getBoolean(UIPreferenceConstants.RECONNECT_PROJECTS_TO_NEW_REPOSITORIES));
    }

    @Override
    protected void performDefaults() {
        final IPreferenceStore store = getPreferenceStore();

        detectChangesOnManualReconnectButton.setSelection(
            store.getDefaultBoolean(UIPreferenceConstants.DETECT_LOCAL_CHANGES_ON_MANUAL_RECONNECT));
        detectChangesOnAutomaticReconnectButton.setSelection(
            store.getDefaultBoolean(UIPreferenceConstants.DETECT_LOCAL_CHANGES_ON_AUTOMATIC_RECONNECT));

        reconnectAutomaticallyButton.setSelection(
            store.getDefaultBoolean(UIPreferenceConstants.RECONNECT_PROJECTS_TO_NEW_REPOSITORIES));
    }

    @Override
    public boolean performOk() {
        final IPreferenceStore store = getPreferenceStore();

        store.setValue(
            UIPreferenceConstants.DETECT_LOCAL_CHANGES_ON_MANUAL_RECONNECT,
            detectChangesOnManualReconnectButton.getSelection());
        store.setValue(
            UIPreferenceConstants.DETECT_LOCAL_CHANGES_ON_AUTOMATIC_RECONNECT,
            detectChangesOnAutomaticReconnectButton.getSelection());

        store.setValue(
            UIPreferenceConstants.RECONNECT_PROJECTS_TO_NEW_REPOSITORIES,
            reconnectAutomaticallyButton.getSelection());

        return true;
    }
}
