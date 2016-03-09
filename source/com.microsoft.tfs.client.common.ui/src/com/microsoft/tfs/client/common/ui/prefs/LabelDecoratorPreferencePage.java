// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.prefs;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;

import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.TFSCommonUIClientPlugin;
import com.microsoft.tfs.client.common.ui.framework.helper.SWTUtil;
import com.microsoft.tfs.client.common.ui.framework.layout.GridDataBuilder;
import com.microsoft.tfs.client.common.ui.helpers.AutomationIDHelper;

public class LabelDecoratorPreferencePage extends BasePreferencePage {
    public static final String DECORATE_FOLDERS_CHECKBOX_ID = "LabelDecoratorPreferencePage.decorateFolders"; //$NON-NLS-1$
    public static final String DECORATE_WITH_CHANGESET_CHECKBOX_ID =
        "LabelDecoratorPreferencePage.decorateWithChangeset"; //$NON-NLS-1$
    public static final String DECORATE_WITH_SERVER_ITEM_CHECKBOX_ID =
        "LabelDecoratorPreferencePage.decorateWithServerItem"; //$NON-NLS-1$
    public static final String DECORATE_WITH_IGNORED_STATUS_CHECKBOX_ID =
        "LabelDecoratorPreferencePage.decorateWithIgnoredStatus"; //$NON-NLS-1$

    private Button decorateFolders;

    private Button decorateWithChangeset;
    private Button decorateWithServerItem;
    private Button decorateWithIgnoredStatus;

    public LabelDecoratorPreferencePage() {
        super();
    }

    public LabelDecoratorPreferencePage(final String title) {
        super(title);
    }

    public LabelDecoratorPreferencePage(final String title, final ImageDescriptor image) {
        super(title, image);
    }

    // get preference settings from the preference store
    private void initializeValues() {
        final IPreferenceStore prefs = TFSCommonUIClientPlugin.getDefault().getPreferenceStore();

        decorateFolders.setSelection(prefs.getBoolean(UIPreferenceConstants.LABEL_DECORATION_DECORATE_FOLDERS));

        decorateWithChangeset.setSelection(prefs.getBoolean(UIPreferenceConstants.LABEL_DECORATION_SHOW_CHANGESET));
        decorateWithServerItem.setSelection(prefs.getBoolean(UIPreferenceConstants.LABEL_DECORATION_SHOW_SERVER_ITEM));
        decorateWithIgnoredStatus.setSelection(
            prefs.getBoolean(UIPreferenceConstants.LABEL_DECORATION_SHOW_IGNORED_STATUS));

    }

    @Override
    protected void performDefaults() {
        final IPreferenceStore prefs = TFSCommonUIClientPlugin.getDefault().getPreferenceStore();

        decorateFolders.setSelection(prefs.getDefaultBoolean(UIPreferenceConstants.LABEL_DECORATION_DECORATE_FOLDERS));

        decorateWithChangeset.setSelection(
            prefs.getDefaultBoolean(UIPreferenceConstants.LABEL_DECORATION_SHOW_CHANGESET));
        decorateWithServerItem.setSelection(
            prefs.getDefaultBoolean(UIPreferenceConstants.LABEL_DECORATION_SHOW_SERVER_ITEM));
        decorateWithIgnoredStatus.setSelection(
            prefs.getDefaultBoolean(UIPreferenceConstants.LABEL_DECORATION_SHOW_IGNORED_STATUS));

        super.performDefaults();
    }

    @Override
    public boolean performOk() {
        final IPreferenceStore prefs = TFSCommonUIClientPlugin.getDefault().getPreferenceStore();

        prefs.setValue(UIPreferenceConstants.LABEL_DECORATION_DECORATE_FOLDERS, decorateFolders.getSelection());

        prefs.setValue(UIPreferenceConstants.LABEL_DECORATION_SHOW_CHANGESET, decorateWithChangeset.getSelection());
        prefs.setValue(UIPreferenceConstants.LABEL_DECORATION_SHOW_SERVER_ITEM, decorateWithServerItem.getSelection());
        prefs.setValue(
            UIPreferenceConstants.LABEL_DECORATION_SHOW_IGNORED_STATUS,
            decorateWithIgnoredStatus.getSelection());

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

        final Label label = SWTUtil.createLabel(
            container,
            SWT.WRAP,
            Messages.getString("LabelDecoratorPreferencePage.SummaryLabelText")); //$NON-NLS-1$
        GridDataBuilder.newInstance().hGrab().hFill().wCHint(label, 20).applyTo(label);

        decorateWithChangeset = new Button(container, SWT.CHECK);
        AutomationIDHelper.setWidgetID(decorateWithChangeset, DECORATE_WITH_CHANGESET_CHECKBOX_ID);
        decorateWithChangeset.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, true, false, 1, 1));
        decorateWithChangeset.setText(
            Messages.getString("LabelDecoratorPreferencePage.DecorateWithChangesetButtonText")); //$NON-NLS-1$

        decorateWithServerItem = new Button(container, SWT.CHECK);
        AutomationIDHelper.setWidgetID(decorateWithServerItem, DECORATE_WITH_SERVER_ITEM_CHECKBOX_ID);
        decorateWithServerItem.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, true, false, 1, 1));
        decorateWithServerItem.setText(
            Messages.getString("LabelDecoratorPreferencePage.DecorateWithServerItemButtonText")); //$NON-NLS-1$

        decorateFolders = new Button(container, SWT.CHECK);
        AutomationIDHelper.setWidgetID(decorateFolders, DECORATE_FOLDERS_CHECKBOX_ID);
        decorateFolders.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, true, false, 1, 1));
        decorateFolders.setText(Messages.getString("LabelDecoratorPreferencePage.DecorateFoldersButtonText")); //$NON-NLS-1$

        decorateWithIgnoredStatus = new Button(container, SWT.CHECK);
        AutomationIDHelper.setWidgetID(decorateWithIgnoredStatus, DECORATE_WITH_IGNORED_STATUS_CHECKBOX_ID);
        decorateWithIgnoredStatus.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, true, false, 1, 1));
        decorateWithIgnoredStatus.setText(
            Messages.getString("LabelDecoratorPreferencePage.DecorateWithIgnoredStatusButtonText")); //$NON-NLS-1$

        initializeValues();

        return container;
    }
}
