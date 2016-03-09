// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.prefs;

import org.eclipse.jface.dialogs.MessageDialogWithToggle;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
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
import com.microsoft.tfs.client.common.ui.framework.layout.GridDataBuilder;
import com.microsoft.tfs.client.common.ui.helpers.AutomationIDHelper;

/**
 */
public class SourceControlPreferencePage extends BasePreferencePage {
    public static final String GET_LATEST_ON_CHECKOUT_CHECKBOX_ID = "SourceControlPreferencePage.autoGetButton"; //$NON-NLS-1$
    public static final String SHOW_DELETED_CHECKBOX_ID = "SourceControlPreferencePage.showDeletedItemsButton"; //$NON-NLS-1$
    public static final String AUTO_RESOLVE_CHECKBOX_ID = "SourceControlPreferencePage.autoResolveButton"; //$NON-NLS-1$

    public static final String LOCK_LEVEL_UNCHANGED_RADIO_ID = "SourceControlPreferencePage.lockLevelUnchangedButton"; //$NON-NLS-1$
    public static final String LOCK_LEVEL_CHECK_OUT_RADIO_ID = "SourceControlPreferencePage.lockLevelCheckOutButton"; //$NON-NLS-1$
    public static final String LOCK_LEVEL_CHECK_IN_RADIO_ID = "SourceControlPreferencePage.lockLevelCheckInButton"; //$NON-NLS-1$

    public static final String HIDE_ALL_FILES_UP_TO_DATE_MESSAGE_CHECKBOX_ID =
        "SourceControlPreferencePage.notifyButton"; //$NON-NLS-1$
    public static final String CONFIRM_CHECK_IN_CHECKBOX_ID = "SourceControlPreferencePage.checkinMessageButton"; //$NON-NLS-1$

    public static final String CHECK_OUT_BACKGROUND_RADIO_ID = "SourceControlPreferencePage.checkoutBackgroundButton"; //$NON-NLS-1$
    public static final String CHECK_OUT_FOREGROUND_RADIO_ID = "SourceControlPreferencePage.checkoutForegroundButton"; //$NON-NLS-1$
    public static final String CHECK_OUT_PROMPT_RADIO_ID = "SourceControlPreferencePage.checkoutPromptButton"; //$NON-NLS-1$

    private Button autoGetButton;
    private Button showDeletedItemsButton;
    private Button autoResolveButton;

    private Button lockLevelUnchangedButton;
    private Button lockLevelCheckOutButton;
    private Button lockLevelCheckInButton;

    private Button notifyButton;
    private Button checkinMessageButton;

    private Button checkoutBackgroundButton;
    private Button checkoutForegroundButton;
    private Button checkoutPromptButton;

    public SourceControlPreferencePage() {
        super();
    }

    public SourceControlPreferencePage(final String title) {
        super(title);
    }

    public SourceControlPreferencePage(final String title, final ImageDescriptor image) {
        super(title, image);
    }

    // get preference settings from the preference store
    private void initializeValues() {
        final IPreferenceStore store = getPreferenceStore();

        autoGetButton.setSelection(store.getBoolean(UIPreferenceConstants.GET_LATEST_ON_CHECKOUT));
        showDeletedItemsButton.setSelection(store.getBoolean(UIPreferenceConstants.SHOW_DELETED_ITEMS));
        autoResolveButton.setSelection(store.getBoolean(UIPreferenceConstants.AUTO_RESOLVE_CONFLICTS));

        if (UIPreferenceConstants.CHECKOUT_LOCK_LEVEL_CHECKOUT.equals(
            store.getString(UIPreferenceConstants.CHECKOUT_LOCK_LEVEL))) {
            lockLevelCheckOutButton.setSelection(true);
            lockLevelCheckInButton.setSelection(false);
            lockLevelUnchangedButton.setSelection(false);
        } else if (UIPreferenceConstants.CHECKOUT_LOCK_LEVEL_CHECKIN.equals(
            store.getString(UIPreferenceConstants.CHECKOUT_LOCK_LEVEL))) {
            lockLevelCheckOutButton.setSelection(false);
            lockLevelCheckInButton.setSelection(true);
            lockLevelUnchangedButton.setSelection(false);
        } else {
            lockLevelCheckOutButton.setSelection(false);
            lockLevelCheckInButton.setSelection(false);
            lockLevelUnchangedButton.setSelection(true);
        }

        notifyButton.setSelection(
            MessageDialogWithToggle.ALWAYS.equals(
                store.getString(UIPreferenceConstants.HIDE_ALL_FILES_UP_TO_DATE_MESSAGE)));
        checkinMessageButton.setSelection(
            !MessageDialogWithToggle.ALWAYS.equals(store.getString(UIPreferenceConstants.PROMPT_BEFORE_CHECKIN)));

        checkoutBackgroundButton.setSelection(
            !store.getBoolean(UIPreferenceConstants.CHECKOUT_FOREGROUND)
                && !store.getBoolean(UIPreferenceConstants.CHECKOUT_SYNCHRONOUS)
                && !store.getBoolean(UIPreferenceConstants.PROMPT_BEFORE_CHECKOUT));
        checkoutForegroundButton.setSelection(store.getBoolean(UIPreferenceConstants.CHECKOUT_FOREGROUND));
        checkoutPromptButton.setSelection(store.getBoolean(UIPreferenceConstants.PROMPT_BEFORE_CHECKOUT));
    }

    @Override
    protected void performDefaults() {
        final IPreferenceStore store = getPreferenceStore();

        autoGetButton.setSelection(store.getDefaultBoolean(UIPreferenceConstants.GET_LATEST_ON_CHECKOUT));
        showDeletedItemsButton.setSelection(store.getDefaultBoolean(UIPreferenceConstants.SHOW_DELETED_ITEMS));
        autoResolveButton.setSelection(store.getDefaultBoolean(UIPreferenceConstants.AUTO_RESOLVE_CONFLICTS));

        if (UIPreferenceConstants.CHECKOUT_LOCK_LEVEL_CHECKOUT.equals(
            store.getDefaultString(UIPreferenceConstants.CHECKOUT_LOCK_LEVEL))) {
            lockLevelCheckOutButton.setSelection(true);
            lockLevelCheckInButton.setSelection(false);
            lockLevelUnchangedButton.setSelection(false);
        } else if (UIPreferenceConstants.CHECKOUT_LOCK_LEVEL_CHECKIN.equals(
            store.getDefaultString(UIPreferenceConstants.CHECKOUT_LOCK_LEVEL))) {
            lockLevelCheckOutButton.setSelection(false);
            lockLevelCheckInButton.setSelection(true);
            lockLevelUnchangedButton.setSelection(false);
        } else {
            lockLevelCheckOutButton.setSelection(false);
            lockLevelCheckInButton.setSelection(false);
            lockLevelUnchangedButton.setSelection(true);
        }

        notifyButton.setSelection(
            MessageDialogWithToggle.ALWAYS.equals(
                store.getDefaultString(UIPreferenceConstants.HIDE_ALL_FILES_UP_TO_DATE_MESSAGE)));
        checkinMessageButton.setSelection(
            !MessageDialogWithToggle.ALWAYS.equals(
                store.getDefaultString(UIPreferenceConstants.PROMPT_BEFORE_CHECKIN)));

        checkoutBackgroundButton.setSelection(
            !store.getDefaultBoolean(UIPreferenceConstants.CHECKOUT_FOREGROUND)
                && !store.getDefaultBoolean(UIPreferenceConstants.CHECKOUT_SYNCHRONOUS)
                && !store.getDefaultBoolean(UIPreferenceConstants.PROMPT_BEFORE_CHECKOUT));
        checkoutForegroundButton.setSelection(store.getDefaultBoolean(UIPreferenceConstants.CHECKOUT_FOREGROUND));
        checkoutPromptButton.setSelection(store.getDefaultBoolean(UIPreferenceConstants.PROMPT_BEFORE_CHECKOUT));

        super.performDefaults();
    }

    @Override
    public boolean performOk() {
        final IPreferenceStore store = getPreferenceStore();

        store.setValue(UIPreferenceConstants.GET_LATEST_ON_CHECKOUT, autoGetButton.getSelection());

        store.setValue(UIPreferenceConstants.SHOW_DELETED_ITEMS, showDeletedItemsButton.getSelection());

        store.setValue(UIPreferenceConstants.AUTO_RESOLVE_CONFLICTS, autoResolveButton.getSelection());

        /* Unchanged is default, we need not set the value here */
        if (lockLevelCheckOutButton.getSelection()) {
            store.setValue(
                UIPreferenceConstants.CHECKOUT_LOCK_LEVEL,
                UIPreferenceConstants.CHECKOUT_LOCK_LEVEL_CHECKOUT);
        } else if (lockLevelCheckInButton.getSelection()) {
            store.setValue(
                UIPreferenceConstants.CHECKOUT_LOCK_LEVEL,
                UIPreferenceConstants.CHECKOUT_LOCK_LEVEL_CHECKIN);
        } else {
            store.setValue(
                UIPreferenceConstants.CHECKOUT_LOCK_LEVEL,
                UIPreferenceConstants.CHECKOUT_LOCK_LEVEL_UNCHANGED);
        }

        store.setValue(
            UIPreferenceConstants.HIDE_ALL_FILES_UP_TO_DATE_MESSAGE,
            notifyButton.getSelection() ? MessageDialogWithToggle.ALWAYS : MessageDialogWithToggle.NEVER);

        store.setValue(
            UIPreferenceConstants.PROMPT_BEFORE_CHECKIN,
            checkinMessageButton.getSelection() ? MessageDialogWithToggle.NEVER : MessageDialogWithToggle.ALWAYS);

        store.setValue(UIPreferenceConstants.PROMPT_BEFORE_CHECKOUT, checkoutPromptButton.getSelection());
        store.setValue(UIPreferenceConstants.CHECKOUT_FOREGROUND, checkoutForegroundButton.getSelection());

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

        final GridLayout layout = new GridLayout();
        layout.marginWidth = 0;
        layout.marginHeight = 0;
        layout.horizontalSpacing = getHorizontalSpacing();
        layout.verticalSpacing = getVerticalSpacing();

        container.setLayout(layout);

        final Group optionsGroup = new Group(container, SWT.NONE);
        optionsGroup.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false, 1, 1));
        optionsGroup.setText(Messages.getString("SourceControlPreferencePage.OptionsGroupText")); //$NON-NLS-1$
        optionsGroup.setLayout(new GridLayout(1, true));

        autoGetButton = new Button(optionsGroup, SWT.CHECK);
        AutomationIDHelper.setWidgetID(autoGetButton, GET_LATEST_ON_CHECKOUT_CHECKBOX_ID);
        autoGetButton.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false, 1, 1));
        autoGetButton.setText(Messages.getString("SourceControlPreferencePage.AutoGetButtonText")); //$NON-NLS-1$

        showDeletedItemsButton = new Button(optionsGroup, SWT.CHECK);
        AutomationIDHelper.setWidgetID(showDeletedItemsButton, SHOW_DELETED_CHECKBOX_ID);
        showDeletedItemsButton.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false, 1, 1));
        showDeletedItemsButton.setText(Messages.getString("SourceControlPreferencePage.ShowDeletedButtonText")); //$NON-NLS-1$

        autoResolveButton = new Button(optionsGroup, SWT.CHECK);
        AutomationIDHelper.setWidgetID(autoResolveButton, AUTO_RESOLVE_CHECKBOX_ID);
        autoResolveButton.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false, 1, 1));
        autoResolveButton.setText(Messages.getString("SourceControlPreferencePage.AutoResolveButtonText")); //$NON-NLS-1$

        final Group lockLevelGroup = new Group(container, SWT.NONE);
        lockLevelGroup.setText(Messages.getString("SourceControlPreferencePage.LockLevelGroupText")); //$NON-NLS-1$
        lockLevelGroup.setLayout(new GridLayout(2, false));
        GridDataBuilder.newInstance().hGrab().hFill().vIndent(getVerticalSpacing()).applyTo(lockLevelGroup);

        lockLevelUnchangedButton = new Button(lockLevelGroup, SWT.RADIO);
        AutomationIDHelper.setWidgetID(lockLevelUnchangedButton, LOCK_LEVEL_UNCHANGED_RADIO_ID);
        lockLevelUnchangedButton.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false, 2, 1));
        lockLevelUnchangedButton.setText(Messages.getString("SourceControlPreferencePage.LevelUnchangedButtonText")); //$NON-NLS-1$

        lockLevelCheckOutButton = new Button(lockLevelGroup, SWT.RADIO);
        AutomationIDHelper.setWidgetID(lockLevelCheckOutButton, LOCK_LEVEL_CHECK_OUT_RADIO_ID);
        lockLevelCheckOutButton.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false, 2, 1));
        lockLevelCheckOutButton.setText(Messages.getString("SourceControlPreferencePage.LevelCheckoutButtonText")); //$NON-NLS-1$

        lockLevelCheckInButton = new Button(lockLevelGroup, SWT.RADIO);
        AutomationIDHelper.setWidgetID(lockLevelCheckInButton, LOCK_LEVEL_CHECK_IN_RADIO_ID);
        lockLevelCheckInButton.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false, 2, 1));
        lockLevelCheckInButton.setText(Messages.getString("SourceControlPreferencePage.LevelCheckinButtonText")); //$NON-NLS-1$

        final Label noteLabel = new Label(lockLevelGroup, SWT.NONE);
        GridDataBuilder.newInstance().align(SWT.LEFT, SWT.CENTER).hIndent(10).applyTo(noteLabel);
        noteLabel.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1));

        final FontData labelFontData = noteLabel.getFont().getFontData()[0];
        labelFontData.setStyle(SWT.BOLD);

        final Font boldFont = new Font(null, labelFontData);

        noteLabel.setFont(boldFont);
        noteLabel.setText(Messages.getString("SourceControlPreferencePage.NoteLabelText")); //$NON-NLS-1$

        final Label noteTextLabel = new Label(lockLevelGroup, SWT.NONE);
        noteTextLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
        noteTextLabel.setText(Messages.getString("SourceControlPreferencePage.NoteTextLabelText")); //$NON-NLS-1$

        final Group promptsGroup = new Group(container, SWT.NONE);
        promptsGroup.setText(Messages.getString("SourceControlPreferencePage.PromptsGroupText")); //$NON-NLS-1$
        promptsGroup.setLayout(new GridLayout(1, true));
        GridDataBuilder.newInstance().hGrab().hFill().vIndent(getVerticalSpacing()).applyTo(promptsGroup);

        notifyButton = new Button(promptsGroup, SWT.CHECK);
        AutomationIDHelper.setWidgetID(notifyButton, HIDE_ALL_FILES_UP_TO_DATE_MESSAGE_CHECKBOX_ID);
        notifyButton.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false, 1, 1));
        notifyButton.setText(Messages.getString("SourceControlPreferencePage.NotifyButtonText")); //$NON-NLS-1$

        checkinMessageButton = new Button(promptsGroup, SWT.CHECK);
        AutomationIDHelper.setWidgetID(checkinMessageButton, CONFIRM_CHECK_IN_CHECKBOX_ID);
        checkinMessageButton.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false, 1, 1));
        checkinMessageButton.setText(Messages.getString("SourceControlPreferencePage.CheckinMessageButtonText")); //$NON-NLS-1$

        final Group editingGroup = new Group(container, SWT.NONE);
        editingGroup.setText(Messages.getString("SourceControlPreferencePage.EditingGroupText")); //$NON-NLS-1$
        editingGroup.setLayout(new GridLayout(2, false));
        GridDataBuilder.newInstance().hGrab().hFill().vIndent(getVerticalSpacing()).applyTo(editingGroup);

        checkoutBackgroundButton = new Button(editingGroup, SWT.RADIO);
        AutomationIDHelper.setWidgetID(checkoutBackgroundButton, CHECK_OUT_BACKGROUND_RADIO_ID);
        checkoutBackgroundButton.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false, 2, 1));
        checkoutBackgroundButton.setText(
            Messages.getString("SourceControlPreferencePage.CheckoutBackgroundButtonText")); //$NON-NLS-1$

        checkoutForegroundButton = new Button(editingGroup, SWT.RADIO);
        AutomationIDHelper.setWidgetID(checkoutForegroundButton, CHECK_OUT_FOREGROUND_RADIO_ID);
        checkoutForegroundButton.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false, 2, 1));
        checkoutForegroundButton.setText(
            Messages.getString("SourceControlPreferencePage.CheckoutForegroundButtonText")); //$NON-NLS-1$

        checkoutPromptButton = new Button(editingGroup, SWT.RADIO);
        AutomationIDHelper.setWidgetID(checkoutPromptButton, CHECK_OUT_PROMPT_RADIO_ID);
        checkoutPromptButton.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false, 2, 1));
        checkoutPromptButton.setText(Messages.getString("SourceControlPreferencePage.CheckoutPromptButtonText")); //$NON-NLS-1$

        final Label checkoutNoteLabel = new Label(editingGroup, SWT.NONE);
        GridDataBuilder.newInstance().align(SWT.LEFT, SWT.CENTER).hIndent(10).applyTo(checkoutNoteLabel);
        checkoutNoteLabel.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1));

        checkoutNoteLabel.setFont(boldFont);
        checkoutNoteLabel.setText(Messages.getString("SourceControlPreferencePage.CheckoutNoteLabel")); //$NON-NLS-1$

        final Label checkoutNoteTextLabel = new Label(editingGroup, SWT.NONE);
        checkoutNoteTextLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
        checkoutNoteTextLabel.setText(Messages.getString("SourceControlPreferencePage.CheckoutNoteTextLabel")); //$NON-NLS-1$

        initializeValues();

        container.addDisposeListener(new DisposeListener() {
            @Override
            public void widgetDisposed(final DisposeEvent arg0) {
                boldFont.dispose();
            }
        });

        return container;
    }
}
