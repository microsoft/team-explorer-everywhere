// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.dialogs.vc;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.TFSCommonUIClientPlugin;
import com.microsoft.tfs.client.common.ui.framework.dialog.BaseDialog;
import com.microsoft.tfs.client.common.ui.framework.helper.SWTUtil;
import com.microsoft.tfs.client.common.ui.framework.image.ImageHelper;
import com.microsoft.tfs.client.common.ui.framework.layout.GridDataBuilder;
import com.microsoft.tfs.client.common.ui.helpers.AutomationIDHelper;
import com.microsoft.tfs.client.common.ui.prefs.UIPreferenceConstants;
import com.microsoft.tfs.util.Check;

/**
 * A dialog presented to the user when a check-in is denied because it affects a
 * build definition marked as a gated check-in.
 */
public class GatedCheckinDialog extends BaseDialog {
    public static final String SHELVESET_NAME_TEXT_ID = "GatedCheckinDialog.shelvesetNameLabel"; //$NON-NLS-1$
    public static final String SINGLE_BUILD_DEFINITION_LABEL_ID = "GatedCheckinDialog.buildDefinitionNameLabel"; //$NON-NLS-1$
    public static final String MULTIPLE_BUILD_DEFINITIONS_COMBO_ID = "GatedCheckinDialog.buildDefinitionsCombo"; //$NON-NLS-1$

    public static final String PRESERVE_LOCAL_CHANGES_CHECKBOX_ID = "GatedCheckinDialog.preserveLocalChangesButton"; //$NON-NLS-1$
    public static final String BYPASS_BUILD_VALIDATION_CHECKBOX_ID = "GatedCheckinDialog.bypassBuildValidationButton"; //$NON-NLS-1$

    final private String shelvesetName;
    final private String[] buildDefinitionNames;
    final private String[] buildDefinitionUris;
    final private boolean allowBypassBuild;
    final private boolean allowKeepCheckedOut;

    private final Image image;

    private boolean preserveLocalChanges;
    private boolean bypassBuildValidation;
    private String selectedBuildDefinitionName;
    private String selectedBuildDefinitionUri;

    private Combo buildDefinitionCombo;
    private Button preserveLocalChangesButton;
    private Button bypassBuildValidationButton;

    /**
     * This dialog warns the user of a denied check-in due to changes that
     * affect a build definition marked as a gated check-in. The user chooses
     * the target build definition (if there are multiple) and chooses to
     * re-issue the check-in operation after successfully building the chagnes.
     * An option to override the gated check-in is offered if the user has
     * permissions.
     *
     * @param parentShell
     *        The parent for this dialog.
     * @param shelvesetName
     *        The shelveset name created by the server for the denied check-in.
     * @param buildDefinitionNames
     *        The build definition names that are affected by the attempted
     *        check-in.
     * @param buildDefinitionUris
     *        The build uris that are affected by the attempted check-in.
     * @param allowBypassBuild
     *        True if the user is allowed to override the gated check-in.
     * @param allowKeepCheckedOut
     *        <code>true</code> if the user is allowed to keep the files checked
     *        out.
     */
    public GatedCheckinDialog(
        final Shell parentShell,
        final String shelvesetName,
        final String[] buildDefinitionNames,
        final String[] buildDefinitionUris,
        final boolean allowBypassBuild,
        final boolean allowKeepCheckedOut) {
        super(parentShell);

        Check.notNull(shelvesetName, "shelvesetName"); //$NON-NLS-1$
        Check.notNull(buildDefinitionNames, "buildDefinitions"); //$NON-NLS-1$

        this.shelvesetName = shelvesetName;
        this.buildDefinitionNames = buildDefinitionNames;
        this.buildDefinitionUris = buildDefinitionUris;
        this.allowBypassBuild = allowBypassBuild;
        this.allowKeepCheckedOut = allowKeepCheckedOut;

        final ImageHelper imageHelper = new ImageHelper(TFSCommonUIClientPlugin.PLUGIN_ID);
        image = imageHelper.getImage("images/vc/gated.gif"); //$NON-NLS-1$ );

        setOptionIncludeDefaultButtons(false);
        addButtonDescription(
            IDialogConstants.OK_ID,
            Messages.getString("GatedCheckinDialog.BuildChangesButtonText"), //$NON-NLS-1$
            true);
        addButtonDescription(IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
    }

    @Override
    protected void hookAddToDialogArea(final Composite dialogArea) {
        GridLayout layout = SWTUtil.gridLayout(dialogArea, 2);
        layout.marginWidth = getHorizontalMargin();
        layout.marginHeight = getVerticalMargin();
        layout.horizontalSpacing = getHorizontalSpacing();
        layout.verticalSpacing = getVerticalSpacing();

        final Label imageLabel = new Label(dialogArea, SWT.NULL);
        image.setBackground(imageLabel.getBackground());
        imageLabel.setImage(image);
        GridDataBuilder.newInstance().fill().vSpan(2).align(SWT.CENTER, SWT.BEGINNING).applyTo(imageLabel);

        // Create the image label
        final Label header = new Label(dialogArea, SWT.NONE);
        header.setText(Messages.getString("GatedCheckinDialog.SummaryText")); //$NON-NLS-1$
        GridDataBuilder.newInstance().fill().hGrab(true).applyTo(header);

        // Composite for the grouped layout to right of the image.
        final Composite summaryComposite = new Composite(dialogArea, SWT.NONE);
        GridDataBuilder.newInstance().fill().hGrab(true).applyTo(summaryComposite);
        layout = SWTUtil.gridLayout(summaryComposite, 2);
        layout.marginHeight = 20;
        layout.marginWidth = 0;

        // Create summary label.
        final Label label = new Label(summaryComposite, SWT.NONE);
        label.setText(Messages.getString("GatedCheckinDialog.WillBeBuiltAsFollows")); //$NON-NLS-1$
        GridDataBuilder.newInstance().fill().hGrab(true).hSpan(2).applyTo(label);

        // Create some vertical space between labels.
        final Label spacerLabel = new Label(summaryComposite, SWT.NONE);
        spacerLabel.setText(" "); //$NON-NLS-1$
        GridDataBuilder.newInstance().fill().hGrab(true).hSpan(2).applyTo(spacerLabel);

        // Create the shelveset label
        final Label shelvesetLabel = new Label(summaryComposite, SWT.NONE);
        shelvesetLabel.setText(Messages.getString("GatedCheckinDialog.ShelvesetLabelText")); //$NON-NLS-1$

        // The shelveset name as a read-only textbox to allow copy/paste.
        final Text shelvesetNameLabel = new Text(summaryComposite, SWT.READ_ONLY);
        AutomationIDHelper.setWidgetID(shelvesetNameLabel, SHELVESET_NAME_TEXT_ID);
        shelvesetNameLabel.setText(shelvesetName);

        // Create the build definition label.
        final Label buildDefinitionLabel = new Label(summaryComposite, SWT.NONE);
        buildDefinitionLabel.setText(Messages.getString("GatedCheckinDialog.BuildDefinitionsLabelText")); //$NON-NLS-1$

        // Multiple build definitions are shown in a combo. A single build
        // definition is shown as a label.
        if (buildDefinitionNames.length > 1) {
            buildDefinitionCombo = new Combo(summaryComposite, SWT.READ_ONLY);
            AutomationIDHelper.setWidgetID(buildDefinitionCombo, MULTIPLE_BUILD_DEFINITIONS_COMBO_ID);
            for (final String buildDefinitionName : buildDefinitionNames) {
                buildDefinitionCombo.add(buildDefinitionName);
            }
            buildDefinitionCombo.select(0);
            buildDefinitionCombo.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(final SelectionEvent e) {
                    buildDefinitionSelectionChanged();
                }
            });
        } else if (buildDefinitionNames.length == 1) {
            // Create the build definition label
            final Label buildDefinitionNameLabel = new Label(summaryComposite, SWT.NONE);
            AutomationIDHelper.setWidgetID(buildDefinitionNameLabel, SINGLE_BUILD_DEFINITION_LABEL_ID);
            buildDefinitionNameLabel.setText(buildDefinitionNames[0]);
        }

        selectedBuildDefinitionName = buildDefinitionNames[0];
        selectedBuildDefinitionUri = buildDefinitionUris[0];

        // Create the preserve local changes checkbox.
        preserveLocalChanges = allowKeepCheckedOut;
        preserveLocalChangesButton = new Button(dialogArea, SWT.CHECK);
        AutomationIDHelper.setWidgetID(preserveLocalChangesButton, PRESERVE_LOCAL_CHANGES_CHECKBOX_ID);
        GridDataBuilder.newInstance().fill().hGrab(true).hSpan(2).applyTo(preserveLocalChangesButton);
        preserveLocalChangesButton.setText(Messages.getString("GatedCheckinDialog.PreserveLocalChangesButtonText")); //$NON-NLS-1$
        preserveLocalChangesButton.setSelection(preserveLocalChanges);
        preserveLocalChangesButton.setEnabled(allowKeepCheckedOut);
        preserveLocalChangesButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
                preserveLocalChanges = preserveLocalChangesButton.getSelection();
            }
        });

        // Create the override gated check-in checkbox.
        bypassBuildValidation = false;
        bypassBuildValidationButton = new Button(dialogArea, SWT.CHECK);
        AutomationIDHelper.setWidgetID(bypassBuildValidationButton, BYPASS_BUILD_VALIDATION_CHECKBOX_ID);
        GridDataBuilder.newInstance().fill().hGrab(true).hSpan(2).applyTo(bypassBuildValidationButton);
        bypassBuildValidationButton.setText(Messages.getString("GatedCheckinDialog.BypassBuildButtonText")); //$NON-NLS-1$
        bypassBuildValidationButton.setEnabled(allowBypassBuild);
        bypassBuildValidationButton.setSelection(bypassBuildValidation);
        bypassBuildValidationButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
                final Button okButton = getButton(IDialogConstants.OK_ID);
                Check.notNull(okButton, "okButton"); //$NON-NLS-1$

                // Change the button text to relect a build operation or a
                // check-in operation depending on the state of the checkbox.
                bypassBuildValidation = bypassBuildValidationButton.getSelection();
                if (bypassBuildValidation) {
                    okButton.setText(Messages.getString("GatedCheckinDialog.CheckinButtonText")); //$NON-NLS-1$
                    preserveLocalChangesButton.setSelection(false);
                } else {
                    okButton.setText(Messages.getString("GatedCheckinDialog.BuildChangesButtonText")); //$NON-NLS-1$
                    preserveLocalChangesButton.setSelection(preserveLocalChanges);
                }

                // Disable other controls when bypass is chosen.
                preserveLocalChangesButton.setEnabled(!bypassBuildValidation && allowKeepCheckedOut);
                if (buildDefinitionCombo != null) {
                    buildDefinitionCombo.setEnabled(!bypassBuildValidation);
                }
            }
        });

        // Load the user settings and reset selection of the affected controls.
        loadUserSettings();
    }

    @Override
    protected void okPressed() {
        saveUserSettings();
        super.okPressed();
    }

    @Override
    protected void hookAfterButtonsCreated() {
        // Set the initial focus on the OK button.
        getButton(IDialogConstants.OK_ID).setFocus();
    }

    public String getSelectedBuildDefinitionURI() {
        return selectedBuildDefinitionUri;
    }

    public String getSelectedBuildDefinitionName() {
        return selectedBuildDefinitionName;
    }

    public boolean getPreserveLocalChanges() {
        return preserveLocalChanges;
    }

    public boolean getBypassBuild() {
        return bypassBuildValidation;
    }

    @Override
    protected String provideDialogTitle() {
        return Messages.getString("GatedCheckinDialog.DialogTitle"); //$NON-NLS-1$
    }

    private void buildDefinitionSelectionChanged() {
        final int index = buildDefinitionCombo.getSelectionIndex();
        selectedBuildDefinitionName = buildDefinitionNames[index];
        selectedBuildDefinitionUri = buildDefinitionUris[index];
    }

    private void loadUserSettings() {
        final IPreferenceStore prefs = TFSCommonUIClientPlugin.getDefault().getPreferenceStore();

        if (allowKeepCheckedOut) {
            final boolean flag = prefs.getBoolean(UIPreferenceConstants.GATED_CONFIRMATION_PRESERVE_PENDING_CHANGES);
            preserveLocalChangesButton.setSelection(flag);
            preserveLocalChanges = flag;
        }

        if (buildDefinitionCombo != null) {
            final String uri = prefs.getString(UIPreferenceConstants.GATED_CONFIRMATION_LAST_BUILD_DEFINITION);
            for (int i = 0; i < buildDefinitionUris.length; i++) {
                if (buildDefinitionUris[i].equals(uri)) {
                    buildDefinitionCombo.select(i);
                    buildDefinitionSelectionChanged();
                    break;
                }
            }
        }
    }

    private void saveUserSettings() {
        final IPreferenceStore prefs = TFSCommonUIClientPlugin.getDefault().getPreferenceStore();
        prefs.setValue(UIPreferenceConstants.GATED_CONFIRMATION_LAST_BUILD_DEFINITION, selectedBuildDefinitionUri);

        /*
         * Only set the preserve local changes button default if it's enabled.
         */
        if (allowKeepCheckedOut) {
            prefs.setValue(UIPreferenceConstants.GATED_CONFIRMATION_PRESERVE_PENDING_CHANGES, preserveLocalChanges);
        }
    }
}
