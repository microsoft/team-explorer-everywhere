// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.prefs;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;

import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.diagnostics.SupportUtils;
import com.microsoft.tfs.client.common.ui.framework.WindowSystem;
import com.microsoft.tfs.client.common.ui.framework.helper.MessageBoxHelpers;
import com.microsoft.tfs.client.common.ui.framework.helper.SWTUtil;
import com.microsoft.tfs.client.common.ui.framework.layout.GridDataBuilder;
import com.microsoft.tfs.client.common.ui.helpers.AutomationIDHelper;

public class MainPreferencePage extends BasePreferencePage {
    public static final String SUPPORT_BUTTON_ID = "MainPreferencePage.supportButton"; //$NON-NLS-1$
    public static final String RECONNECT_CHECKBOX_ID = "MainPreferencePage.reconnectButton"; //$NON-NLS-1$
    public static final String CONNECT_AT_IMPORT_CHECKBOX_ID = "MainPreferencePage.connectAtImportButton"; //$NON-NLS-1$
    public static final String ACCEPT_UNTRUSTED_CERTIFICATES_CHECKBOX_ID =
        "MainPreferencePage.acceptUntrustedCertificates"; //$NON-NLS-1$

    /**
     * This constant is since SWT 3.7, so we duplicate it here.
     */
    public static final int WEBKIT = 1 << 16;

    private Button reconnectButton;
    private Button connectAtImportButton;
    private Button acceptUntrustedCertificatesButton;

    private Button embeddedBrowserDefault;
    private Button embeddedBrowserMozilla;
    private Button embeddedBrowserWebkit;

    @Override
    protected Control createContents(final Composite parent) {
        final Composite composite = new Composite(parent, SWT.NONE);

        final GridLayout layout = new GridLayout(1, false);
        layout.horizontalSpacing = getHorizontalSpacing();
        layout.verticalSpacing = getVerticalSpacing();
        layout.marginWidth = 0;
        layout.marginHeight = 0;
        composite.setLayout(layout);

        final Label generalLabel = new Label(composite, SWT.WRAP);
        generalLabel.setText(Messages.getString("MainPreferencePage.GeneralPreferencesText")); //$NON-NLS-1$

        reconnectButton = new Button(composite, SWT.CHECK);
        AutomationIDHelper.setWidgetID(reconnectButton, RECONNECT_CHECKBOX_ID);
        reconnectButton.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false, 1, 1));
        reconnectButton.setText(Messages.getString("MainPreferencePage.ReconnectButtonText")); //$NON-NLS-1$

        connectAtImportButton = new Button(composite, SWT.CHECK);
        AutomationIDHelper.setWidgetID(connectAtImportButton, CONNECT_AT_IMPORT_CHECKBOX_ID);
        connectAtImportButton.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false, 1, 1));
        connectAtImportButton.setText(Messages.getString("MainPreferencePage.ConnectAtImportButtonText")); //$NON-NLS-1$

        acceptUntrustedCertificatesButton = new Button(composite, SWT.CHECK);
        AutomationIDHelper.setWidgetID(acceptUntrustedCertificatesButton, ACCEPT_UNTRUSTED_CERTIFICATES_CHECKBOX_ID);
        acceptUntrustedCertificatesButton.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false, 1, 1));
        acceptUntrustedCertificatesButton.setText(
            Messages.getString("MainPreferencePage.AcceptUntrustedCertificatesText")); //$NON-NLS-1$

        acceptUntrustedCertificatesButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
                // Confirm the user really wants to do this
                if (acceptUntrustedCertificatesButton.getSelection()) {
                    final boolean answer = MessageBoxHelpers.dialogYesNoPrompt(
                        getShell(),
                        Messages.getString("MainPreferencePage.AcceptUntrustedCertificatesDialogTitle"), //$NON-NLS-1$
                        Messages.getString("MainPreferencePage.AcceptUntrustedCertificatesDialogText")); //$NON-NLS-1$

                    acceptUntrustedCertificatesButton.setSelection(answer);
                }
            }
        });

        if (showBrowserPreference()) {
            final Group browserGroup = new Group(composite, SWT.NONE);
            browserGroup.setText(Messages.getString("MainPreferencePage.EmbeddedBrowserGroupTitle")); //$NON-NLS-1$
            browserGroup.setLayout(new GridLayout(2, false));
            GridDataBuilder.newInstance().hGrab().hFill().vIndent(getVerticalSpacing()).applyTo(browserGroup);

            embeddedBrowserDefault = new Button(browserGroup, SWT.RADIO);
            embeddedBrowserDefault.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false, 2, 1));
            embeddedBrowserDefault.setText(Messages.getString("MainPreferencePage.EmbeddedBrowserButtonDefault")); //$NON-NLS-1$
            embeddedBrowserDefault.setToolTipText(
                Messages.getString("MainPreferencePage.EmbeddedBrowserTooltipDefault")); //$NON-NLS-1$

            embeddedBrowserMozilla = new Button(browserGroup, SWT.RADIO);
            embeddedBrowserMozilla.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false, 2, 1));
            embeddedBrowserMozilla.setText(Messages.getString("MainPreferencePage.EmbeddedBrowserButtonMozilla")); //$NON-NLS-1$
            embeddedBrowserMozilla.setToolTipText(
                Messages.getString("MainPreferencePage.EmbeddedBrowserTooltipMozilla")); //$NON-NLS-1$

            embeddedBrowserWebkit = new Button(browserGroup, SWT.RADIO);
            embeddedBrowserWebkit.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false, 2, 1));
            embeddedBrowserWebkit.setText(Messages.getString("MainPreferencePage.EmbeddedBrowserButtonWebkit")); //$NON-NLS-1$
            embeddedBrowserWebkit.setToolTipText(Messages.getString("MainPreferencePage.EmbeddedBrowserTooltipWebkit")); //$NON-NLS-1$

            addNoteControl(browserGroup, Messages.getString("MainPreferencePage.EmbeddedBrowserNoteText")); //$NON-NLS-1$
        }

        SWTUtil.createGridLayoutSpacer(composite);

        final Button supportButton =
            SWTUtil.createButton(composite, Messages.getString("MainPreferencePage.SupportButtonText")); //$NON-NLS-1$
        GridDataBuilder.newInstance().hGrab().applyTo(supportButton);
        AutomationIDHelper.setWidgetID(supportButton, SUPPORT_BUTTON_ID);

        supportButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
                SupportUtils.openSupportDialog(((Button) e.widget).getShell(), getClass().getClassLoader());
            }
        });

        initializeValues();

        return composite;
    }

    // get preference settings from the preference store
    private void initializeValues() {
        final IPreferenceStore store = getPreferenceStore();

        reconnectButton.setSelection(store.getBoolean(UIPreferenceConstants.RECONNECT_AT_STARTUP));
        connectAtImportButton.setSelection(store.getBoolean(UIPreferenceConstants.CONNECT_MAPPED_PROJECTS_AT_IMPORT));
        acceptUntrustedCertificatesButton.setSelection(
            store.getBoolean(UIPreferenceConstants.ACCEPT_UNTRUSTED_CERTIFICATES));

        if (showBrowserPreference()) {
            setEmbeddedBrowserType(store.getInt(UIPreferenceConstants.EMBEDDED_WEB_BROWSER_TYPE));
        }
    }

    @Override
    protected void performDefaults() {
        final IPreferenceStore store = getPreferenceStore();

        reconnectButton.setSelection(store.getDefaultBoolean(UIPreferenceConstants.RECONNECT_AT_STARTUP));
        connectAtImportButton.setSelection(
            store.getDefaultBoolean(UIPreferenceConstants.CONNECT_MAPPED_PROJECTS_AT_IMPORT));
        acceptUntrustedCertificatesButton.setSelection(
            store.getDefaultBoolean(UIPreferenceConstants.ACCEPT_UNTRUSTED_CERTIFICATES));

        if (showBrowserPreference()) {
            setEmbeddedBrowserType(store.getDefaultInt(UIPreferenceConstants.EMBEDDED_WEB_BROWSER_TYPE));
        }

        super.performDefaults();
    }

    @Override
    public boolean performOk() {
        final IPreferenceStore store = getPreferenceStore();

        store.setValue(UIPreferenceConstants.RECONNECT_AT_STARTUP, reconnectButton.getSelection());
        store.setValue(UIPreferenceConstants.CONNECT_MAPPED_PROJECTS_AT_IMPORT, connectAtImportButton.getSelection());
        store.setValue(
            UIPreferenceConstants.ACCEPT_UNTRUSTED_CERTIFICATES,
            acceptUntrustedCertificatesButton.getSelection());

        if (showBrowserPreference()) {
            store.setValue(UIPreferenceConstants.EMBEDDED_WEB_BROWSER_TYPE, getEmbeddedBrowserType());
        }

        return super.performOk();
    }

    private void addNoteControl(final Composite gridLayoutParent, final String text) {
        final Label noteLabel = new Label(gridLayoutParent, SWT.NONE);
        GridDataBuilder.newInstance().align(SWT.LEFT, SWT.TOP).applyTo(noteLabel);

        final FontData labelFontData = noteLabel.getFont().getFontData()[0];
        labelFontData.setStyle(SWT.BOLD);

        final Font boldFont = new Font(null, labelFontData);

        noteLabel.setFont(boldFont);
        noteLabel.setText(Messages.getString("MainPreferencePage.NoteText")); //$NON-NLS-1$

        final Label noteTextLabel = new Label(gridLayoutParent, SWT.WRAP);
        GridDataBuilder.newInstance().hGrab().hFill().wCHint(noteTextLabel, 30).vAlign(SWT.TOP).applyTo(noteTextLabel);
        noteTextLabel.setText(text);
    }

    private boolean showBrowserPreference() {
        // Choosing a browser doesn't make sense on Windows and Mac OS X
        return !WindowSystem.isCurrentWindowSystem(WindowSystem.WINDOWS)
            && !WindowSystem.isCurrentWindowSystem(WindowSystem.AQUA);
    }

    /**
     * Sets the radio buttons from an {@link SWT} constant
     *
     * @param browserType
     *        the {@link SWT} constant to set the radio buttons with
     */
    private void setEmbeddedBrowserType(final int browserType) {
        embeddedBrowserDefault.setSelection(false);
        embeddedBrowserMozilla.setSelection(false);
        embeddedBrowserWebkit.setSelection(false);

        switch (browserType) {
            case SWT.MOZILLA:
                embeddedBrowserMozilla.setSelection(true);
                break;
            case WEBKIT:
                embeddedBrowserWebkit.setSelection(true);
                break;
            default:
                // Handles SWT.NONE
                embeddedBrowserDefault.setSelection(true);
                break;
        }
    }

    /**
     * @return the {@link SWT} constant that represents the radio button state.
     */
    private int getEmbeddedBrowserType() {
        if (embeddedBrowserMozilla.getSelection()) {
            return SWT.MOZILLA;
        } else if (embeddedBrowserWebkit.getSelection()) {
            return WEBKIT;
        } else {
            return SWT.NONE;
        }
    }
}
