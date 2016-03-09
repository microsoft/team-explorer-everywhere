// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.dialogs.vc;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

import com.microsoft.tfs.client.common.repository.TFSRepository;
import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.TFSCommonUIClientPlugin;
import com.microsoft.tfs.client.common.ui.controls.generic.AutocompleteCombo;
import com.microsoft.tfs.client.common.ui.controls.vc.changes.ChangeItemProvider;
import com.microsoft.tfs.client.common.ui.controls.vc.changes.RepositoryChangeItemProvider;
import com.microsoft.tfs.client.common.ui.controls.vc.checkin.CheckinControl;
import com.microsoft.tfs.client.common.ui.controls.vc.checkin.CheckinControlOptions;
import com.microsoft.tfs.client.common.ui.controls.vc.checkin.NotesCheckinControl;
import com.microsoft.tfs.client.common.ui.controls.vc.checkin.SourceFilesCheckinControl;
import com.microsoft.tfs.client.common.ui.controls.vc.checkin.WorkItemsCheckinControl;
import com.microsoft.tfs.client.common.ui.framework.layout.GridDataBuilder;
import com.microsoft.tfs.client.common.ui.framework.validation.ButtonValidatorBinding;
import com.microsoft.tfs.client.common.ui.framework.validation.ComboControlValidator;
import com.microsoft.tfs.client.common.ui.helpers.AutomationIDHelper;
import com.microsoft.tfs.client.common.ui.prefs.MRUPreferenceSerializer;
import com.microsoft.tfs.client.common.ui.prefs.UIPreferenceConstants;
import com.microsoft.tfs.client.common.ui.teamexplorer.helpers.PendingChangesHelpers;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.CheckinNote;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.PendingChange;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.WorkItemCheckinInfo;
import com.microsoft.tfs.core.pendingcheckin.PendingCheckin;
import com.microsoft.tfs.util.MRUSet;
import com.microsoft.tfs.util.valid.MultiValidator;
import com.microsoft.tfs.util.valid.Validator;

public class ShelveDialog extends AbstractCheckinControlDialog {
    public static final String SHELVESET_NAME_COMBO_ID = "ShelveDialog.shelvesetNameText"; //$NON-NLS-1$
    public static final String PRESERVE_PENDING_CHANGES_BUTTON_ID = "ShelveDialog.preservePendingChangesButton"; //$NON-NLS-1$
    public static final String EVALUATE_POLICIES_BUTTON_ID = "ShelveDialog.evaluatePoliciesButton"; //$NON-NLS-1$

    public static final int MRU_SHELVESET_NAME_MAX = 10;

    private final TFSRepository repository;
    private final PendingChange[] initialChangesToShelve;
    private final String initialComment;
    private final WorkItemCheckinInfo[] initialCheckedWorkItems;
    private final CheckinNote initialCheckinNotes;

    private boolean preservePendingChanges = true;
    private boolean evaluatePoliciesAndCheckinNotes = false;

    private AutocompleteCombo shelvesetNameCombo;
    private MRUSet shelvesetNameComboMRUSet;
    private Button preservePendingChangesButton;
    private Button evaluatePoliciesButton;

    private volatile PendingCheckin pendingCheckinResult;
    private volatile String shelvesetName;

    public ShelveDialog(
        final Shell parentShell,
        final TFSRepository repository,
        final PendingChange[] initialChangesToShelve,
        final String initialComment,
        final WorkItemCheckinInfo[] initialCheckedWorkItems,
        final CheckinNote initialCheckinNotes) {
        super(parentShell);
        this.repository = repository;
        this.initialChangesToShelve = initialChangesToShelve;
        this.initialComment = initialComment;
        this.initialCheckedWorkItems = initialCheckedWorkItems;
        this.initialCheckinNotes = initialCheckinNotes;
    }

    @Override
    protected void hookAddToDialogArea(final Composite dialogArea) {
        final GridLayout layout = new GridLayout(1, false);
        layout.marginWidth = getHorizontalMargin();
        layout.marginHeight = getVerticalMargin();
        layout.horizontalSpacing = getHorizontalSpacing();
        layout.verticalSpacing = getVerticalSpacing();
        dialogArea.setLayout(layout);

        final Label label = new Label(dialogArea, SWT.NONE);
        label.setText(Messages.getString("ShelveDialog.NameLabelText")); //$NON-NLS-1$

        shelvesetNameCombo = new AutocompleteCombo(dialogArea, SWT.BORDER);
        shelvesetNameCombo.setCaseSensitive(true);
        shelvesetNameCombo.setTextLimit(64);
        GridDataBuilder.newInstance().hGrab().hFill().applyTo(shelvesetNameCombo);
        AutomationIDHelper.setWidgetID(shelvesetNameCombo, SHELVESET_NAME_COMBO_ID);

        final CheckinControlOptions options = new CheckinControlOptions();
        options.setForDialog(true);
        options.setSourceFilesCheckboxes(true);
        options.setSourceFilesCommentReadOnly(false);
        options.setWorkItemInitialQuery(initialCheckedWorkItems == null || initialCheckedWorkItems.length == 0);
        // options.setCheckinNotesDeferUpdates(true);
        options.setPolicyEvaluationEnabled(true);
        options.setPolicyDisplayed(false);

        final CheckinControl checkinControl = new CheckinControl(dialogArea, SWT.NONE, options);
        GridDataBuilder.newInstance().grab().fill().applyTo(checkinControl);

        final ChangeItemProvider changeItemProvider = new RepositoryChangeItemProvider(repository);
        checkinControl.setChangeItemProvider(changeItemProvider);
        dialogArea.addDisposeListener(new DisposeListener() {
            @Override
            public void widgetDisposed(final DisposeEvent e) {
                changeItemProvider.dispose();
            }
        });

        /*
         * Set the checkin control on the superclass.
         */
        setCheckinControl(checkinControl);

        getCheckinControl().setRepository(repository);

        /* Setup the source files tab */
        final SourceFilesCheckinControl sourceFilesSubControl = getCheckinControl().getSourceFilesSubControl();

        sourceFilesSubControl.setComment(initialComment);

        if (initialChangesToShelve != null) {
            sourceFilesSubControl.getChangesTable().setCheckedElements(
                RepositoryChangeItemProvider.getChangeItemsFromPendingChanges(repository, initialChangesToShelve));
        } else {
            sourceFilesSubControl.getChangesTable().checkAll();
        }

        /* Setup the work items tab */
        final WorkItemsCheckinControl workItemsSubControl = getCheckinControl().getWorkItemSubControl();

        if (initialCheckedWorkItems != null) {
            workItemsSubControl.getWorkItemTable().setWorkItems(initialCheckedWorkItems);
            workItemsSubControl.getWorkItemTable().setCheckedWorkItems(initialCheckedWorkItems);
        }

        /* Setup the check-in notes tab */
        final NotesCheckinControl notesSubControl = getCheckinControl().getNotesSubControl();

        if (initialCheckinNotes != null) {
            notesSubControl.setCheckinNote(initialCheckinNotes);
        }

        // notesSubControl.setDeferUpdates(false);

        shelvesetNameComboMRUSet =
            new MRUPreferenceSerializer(TFSCommonUIClientPlugin.getDefault().getPreferenceStore()).read(
                MRU_SHELVESET_NAME_MAX,
                UIPreferenceConstants.SHELVE_DIALOG_NAME_MRU_PREFIX);

        /*
         * The MRUSet keeps most recent items at the end, but it's nice if
         * they're first in the drop-down, so reverse.
         */
        final List<String> mruItemsList = new ArrayList<String>(shelvesetNameComboMRUSet);
        Collections.reverse(mruItemsList);
        shelvesetNameCombo.setItems(mruItemsList.toArray(new String[mruItemsList.size()]));

        /*
         * Set the focus on the text box.
         */
        shelvesetNameCombo.setFocus();

        pendingCheckinResult = null;
    }

    @Override
    protected void hookAfterButtonsCreated() {
        final Button button = getButton(IDialogConstants.OK_ID);
        button.setText(Messages.getString("ShelveDialog.ShelveButtonText")); //$NON-NLS-1$
        setButtonLayoutData(button);

        final SourceFilesCheckinControl sourceFilesSubControl = getCheckinControl().getSourceFilesSubControl();
        final Validator checkValidator = sourceFilesSubControl.getChangesTable().getCheckboxValidator();
        final Validator nameValidator = new ComboControlValidator(shelvesetNameCombo);

        final MultiValidator multiValidator = new MultiValidator(button);
        multiValidator.addValidator(checkValidator);
        multiValidator.addValidator(nameValidator);
        new ButtonValidatorBinding(button).bind(multiValidator);
    }

    @Override
    protected String getBaseTitle() {
        return Messages.getString("ShelveDialog.DialogBaseTitle"); //$NON-NLS-1$
    }

    @Override
    protected Control createButtonBar(final Composite parent) {
        final Composite composite = new Composite(parent, SWT.NONE);
        GridDataBuilder.newInstance().hGrab().hFill().applyTo(composite);

        final GridLayout layout = new GridLayout(2, false);
        layout.marginWidth = 0;
        layout.marginHeight = 0;
        layout.horizontalSpacing = 0;
        layout.verticalSpacing = 0;
        composite.setLayout(layout);

        final Composite extraControlsArea = createButtonBarExtraControls(composite);
        GridDataBuilder.newInstance().hGrab().hFill().applyTo(extraControlsArea);

        final Composite buttonsArea = new Composite(composite, SWT.NONE);
        GridDataBuilder.newInstance().vAlignBottom().applyTo(buttonsArea);

        final GridLayout buttonsAreaLayout = new GridLayout(1, false);
        buttonsAreaLayout.marginWidth = 0;
        buttonsAreaLayout.marginHeight = 0;
        buttonsAreaLayout.horizontalSpacing = 0;
        buttonsAreaLayout.verticalSpacing = 0;
        buttonsArea.setLayout(buttonsAreaLayout);

        super.createButtonBar(buttonsArea);

        return composite;
    }

    private Composite createButtonBarExtraControls(final Composite parent) {
        final Composite composite = new Composite(parent, SWT.NONE);

        final GridLayout layout = new GridLayout(1, false);

        if (SWT.getVersion() > 3100) {
            layout.marginBottom = getVerticalMargin();
        }

        layout.marginWidth = getHorizontalMargin();
        layout.marginHeight = getVerticalMargin();
        layout.horizontalSpacing = getHorizontalSpacing();
        layout.verticalSpacing = getVerticalSpacing();
        composite.setLayout(layout);

        preservePendingChangesButton = new Button(composite, SWT.CHECK);
        preservePendingChangesButton.setText(Messages.getString("ShelveDialog.PreserveButtonText")); //$NON-NLS-1$
        preservePendingChangesButton.setSelection(preservePendingChanges);
        preservePendingChangesButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
                preservePendingChanges = preservePendingChangesButton.getSelection();
            }
        });
        AutomationIDHelper.setWidgetID(preservePendingChangesButton, PRESERVE_PENDING_CHANGES_BUTTON_ID);

        evaluatePoliciesButton = new Button(composite, SWT.CHECK);
        evaluatePoliciesButton.setText(Messages.getString("ShelveDialog.EvaluatePoliciesButtonText")); //$NON-NLS-1$
        evaluatePoliciesButton.setSelection(evaluatePoliciesAndCheckinNotes);
        evaluatePoliciesButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
                evaluatePoliciesAndCheckinNotes = evaluatePoliciesButton.getSelection();
            }
        });
        AutomationIDHelper.setWidgetID(evaluatePoliciesButton, EVALUATE_POLICIES_BUTTON_ID);

        return composite;
    }

    @Override
    public void okPressed() {
        if (evaluatePoliciesAndCheckinNotes) {
            /*
             * Validate the pending change (checked work items, notes, policies,
             * etc.). The control's validation method may raise dialogs.
             */
            if (getCheckinControl().validateForCheckin().getSucceeded() == false) {
                return;
            }
        }

        if (!PendingChangesHelpers.confirmShelvesetCanBeWritten(getShell(), repository, shelvesetNameCombo.getText())) {
            return;
        }

        pendingCheckinResult = getCheckinControl().getPendingCheckin();
        this.shelvesetName = shelvesetNameCombo.getText();

        if (shelvesetNameComboMRUSet.add(this.shelvesetName)) {
            new MRUPreferenceSerializer(TFSCommonUIClientPlugin.getDefault().getPreferenceStore()).write(
                shelvesetNameComboMRUSet,
                UIPreferenceConstants.SHELVE_DIALOG_NAME_MRU_PREFIX);
        }

        super.okPressed();
    }

    public PendingCheckin getPendingCheckin() {
        return pendingCheckinResult;
    }

    public String getShelvesetName() {
        return shelvesetName;
    }

    public boolean isPreservePendingChanges() {
        return preservePendingChanges;
    }
}
