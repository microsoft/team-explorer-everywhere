// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.controls.vc;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import com.microsoft.tfs.client.common.repository.TFSRepository;
import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.controls.generic.BaseControl;
import com.microsoft.tfs.client.common.ui.controls.vc.VersionPickerControl.VersionDescription;
import com.microsoft.tfs.client.common.ui.dialogs.vc.FindChangesetDialog;
import com.microsoft.tfs.client.common.ui.dialogs.vc.ServerItemPickerDialog;
import com.microsoft.tfs.client.common.ui.framework.layout.GridDataBuilder;
import com.microsoft.tfs.client.common.ui.framework.validation.AbstractTextControlValidator;
import com.microsoft.tfs.client.common.ui.vc.serveritem.ServerItemSource;
import com.microsoft.tfs.client.common.ui.vc.serveritem.WorkspaceItemSource;
import com.microsoft.tfs.core.clients.versioncontrol.path.ServerPath;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Changeset;
import com.microsoft.tfs.core.clients.versioncontrol.specs.version.VersionSpec;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.StringUtil;
import com.microsoft.tfs.util.valid.IValidationMessage;
import com.microsoft.tfs.util.valid.IValidity;
import com.microsoft.tfs.util.valid.MultiValidator;
import com.microsoft.tfs.util.valid.Severity;
import com.microsoft.tfs.util.valid.Validatable;
import com.microsoft.tfs.util.valid.ValidationMessage;
import com.microsoft.tfs.util.valid.Validator;
import com.microsoft.tfs.util.valid.ValidatorHelper;
import com.microsoft.tfs.util.valid.Validity;

public class RollbackItemControl extends BaseControl implements Validatable {

    public static final class RollbackOperationType {
        public static final RollbackOperationType NONE = new RollbackOperationType("NONE"); //$NON-NLS-1$
        public static final RollbackOperationType SINGLE_CHANGESET = new RollbackOperationType("SINGLE_CHANGESET"); //$NON-NLS-1$
        public static final RollbackOperationType CHANGESET_RANGE = new RollbackOperationType("CHANGESET_RANGE"); //$NON-NLS-1$
        public static final RollbackOperationType SPECIFIC_VERSION = new RollbackOperationType("SPECIFIC_VERSION"); //$NON-NLS-1$

        private final String type;

        public RollbackOperationType(final String type) {
            this.type = type;
        }

        @Override
        public String toString() {
            return type;
        }
    }

    private final class ChangesetValidator extends AbstractTextControlValidator {
        protected ChangesetValidator(final Text subject) {
            super(subject);
            validate();
        }

        @Override
        protected IValidity computeValidity(final String text) {
            try {
                Integer.parseInt(text);
            } catch (final NumberFormatException e) {
                final IValidationMessage message =
                    new ValidationMessage(Messages.getString("RollbackItemControl.WrongChangesetID"), Severity.ERROR); //$NON-NLS-1$
                return new Validity(message);
            }

            return Validity.VALID;
        }

        public void setEnabled(final boolean enableValidation) {
            if (enableValidation) {
                resumeValidation();
            } else {
                suspendValidation(true);
            }
        }
    }

    private final TFSRepository repository;

    private String itemPath;
    private String singleChangesetID;
    private String changesetFromID;
    private String changesetToID;

    private RollbackOperationType rollbackOperationType = RollbackOperationType.SINGLE_CHANGESET;

    private Button singleChangesetButton;
    private Button changesetRangeButton;
    private Button specificVersionButton;

    private Text itemText;
    private Text singleChangesetText;
    private Text changesetFromText;
    private Text changesetToText;
    private VersionPickerControl versionPicker;

    private final List<Control> singleChangesetControls = new ArrayList<Control>();
    private final List<Control> changesetRangeControls = new ArrayList<Control>();
    private final List<Control> specificVersionControls = new ArrayList<Control>();

    private final MultiValidator validator;
    private ValidatorHelper textValidator;
    private ChangesetValidator singleChangesetValidator;
    private ChangesetValidator changesetFromValidator;
    private ChangesetValidator changesetToValidator;

    public RollbackItemControl(
        final Composite parent,
        final int style,
        final TFSRepository repository,
        final String itemServerPath) {
        this(parent, style, repository, itemServerPath, null, null);
    }

    public RollbackItemControl(
        final Composite parent,
        final int style,
        final TFSRepository repository,
        final String itemServerPath,
        final VersionSpec versionFrom,
        final VersionSpec versionTo) {
        super(parent, style);

        Check.notNull(repository, "repository"); //$NON-NLS-1$

        this.repository = repository;
        this.itemPath = (itemServerPath != null && itemServerPath.trim().length() > 0) ? itemServerPath : null;

        validator = new MultiValidator(this);

        createControls(this);
    }

    public String getItem() {
        return itemPath;
    }

    public void setInitialValues(final RollbackOperationType rollbackOperationType) {
        rollbackOperationTypeChanged(rollbackOperationType);
        singleChangesetButton.setSelection(rollbackOperationType == RollbackOperationType.SINGLE_CHANGESET);
        changesetRangeButton.setSelection(rollbackOperationType == RollbackOperationType.CHANGESET_RANGE);
        specificVersionButton.setSelection(rollbackOperationType == RollbackOperationType.SPECIFIC_VERSION);
    }

    public void setInitialValues(final RollbackOperationType rollbackOperationType, final String singleChangesetID) {
        setInitialValues(rollbackOperationType);
        singleChangesetText.setText(singleChangesetID);
    }

    public void setInitialValues(
        final RollbackOperationType rollbackOperationType,
        final String changesetFromID,
        final String changesetToID) {

        setInitialValues(rollbackOperationType);
        changesetFromText.setText(changesetFromID);
        changesetToText.setText(changesetToID);
    }

    public void setInitialValues(final RollbackOperationType rollbackOperationType, final VersionSpec specificVersion) {
        setInitialValues(rollbackOperationType);
        versionPicker.setVersionSpec(specificVersion);
    }

    private void setItem(final String path) {
        itemPath = path;
        if (itemPath != null && itemPath.trim().length() == 0) {
            itemPath = null;
        }
        itemText.setText(itemPath != null ? itemPath : ""); //$NON-NLS-1$
    }

    public String getSingleChangesetID() {
        return singleChangesetID;
    }

    public String getChangesetFromID() {
        return changesetFromID;
    }

    public String getChangesetToID() {
        return changesetToID;
    }

    public VersionSpec getVersionSpec() {
        return versionPicker.getVersionSpec();
    }

    public RollbackOperationType getRollbackOperationType() {
        return rollbackOperationType;
    }

    private void setChangesetID(final Text chagesetText, final Changeset chageset) {
        chagesetText.setText(chageset != null ? Integer.toString(chageset.getChangesetID()) : ""); //$NON-NLS-1$
    }

    private void createControls(final Composite composite) {
        final GridLayout layout = new GridLayout();
        layout.marginWidth = 0;
        layout.marginHeight = 0;
        layout.horizontalSpacing = getHorizontalSpacing();
        layout.verticalSpacing = getVerticalSpacing();
        setLayout(layout);

        final Control itemsToRollbackArea = createItemsToRollbackArea(composite);
        GridDataBuilder.newInstance().hGrab().hFill().applyTo(itemsToRollbackArea);

        final Control typeOfRollbackArea = createTypeOfRollbackArea(composite);
        GridDataBuilder.newInstance().hGrab().hFill().applyTo(typeOfRollbackArea);

        rollbackOperationTypeChanged(rollbackOperationType);
    }

    private Control createItemsToRollbackArea(final Composite parent) {
        final Composite composite = new Composite(parent, SWT.NONE);

        final GridLayout layout = new GridLayout(2, false);
        layout.marginWidth = getHorizontalMargin();
        layout.marginHeight = getVerticalMargin();
        layout.horizontalSpacing = getHorizontalSpacing();
        layout.verticalSpacing = getVerticalSpacing();
        composite.setLayout(layout);

        final Label itemsToRollbackLabel = new Label(composite, SWT.NONE);
        itemsToRollbackLabel.setText(Messages.getString("RollbackItemControl.ItemsToRollback")); //$NON-NLS-1$
        GridDataBuilder.newInstance().hSpan(layout).applyTo(itemsToRollbackLabel);

        itemText = new Text(composite, SWT.BORDER);
        GridDataBuilder.newInstance().hGrab().hFill().applyTo(itemText);
        if (itemPath != null) {
            itemText.setText(itemPath);
        }

        itemText.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(final ModifyEvent e) {
                RollbackItemControl.this.modifyText(e);
            }
        });

        textValidator = new ValidatorHelper(itemText);
        validator.addValidator(textValidator);

        final Button browseButton = new Button(composite, SWT.NONE);
        browseButton.setText(Messages.getString("RollbackItemControl.Browse")); //$NON-NLS-1$
        browseButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
                browsePressed();
            }
        });

        return composite;
    }

    private Control createTypeOfRollbackArea(final Composite parent) {
        final Composite composite = new Composite(parent, SWT.NONE);

        final GridLayout layout = new GridLayout();
        layout.marginWidth = getHorizontalMargin();
        layout.marginHeight = getVerticalMargin();
        layout.horizontalSpacing = getHorizontalSpacing();
        layout.verticalSpacing = getVerticalSpacing();
        composite.setLayout(layout);

        final Label itemsToRollbackLabel = new Label(composite, SWT.NONE);
        itemsToRollbackLabel.setText(Messages.getString("RollbackItemControl.ChooseOperationType")); //$NON-NLS-1$
        GridDataBuilder.newInstance().hSpan(layout).applyTo(itemsToRollbackLabel);

        createSingleChangesetOption(composite);
        createChangesetRangeOption(composite);
        createSpecificVersionOption(composite);

        return composite;
    }

    private void createSingleChangesetOption(final Composite composite) {
        final GridLayout layout = (GridLayout) composite.getLayout();

        singleChangesetButton = new Button(composite, SWT.RADIO);
        singleChangesetButton.setText(Messages.getString("RollbackItemControl.SingleChangeset")); //$NON-NLS-1$
        GridDataBuilder.newInstance().hSpan(layout).applyTo(singleChangesetButton);

        singleChangesetButton.setSelection(RollbackOperationType.SINGLE_CHANGESET == rollbackOperationType);
        singleChangesetButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
                if (singleChangesetButton.getSelection()) {
                    rollbackOperationTypeChanged(RollbackOperationType.SINGLE_CHANGESET);
                }
            }
        });

        final Composite singleChangesetComposite = new Composite(composite, SWT.NONE);

        final GridLayout singleChangesetLayout = new GridLayout(3, false);
        singleChangesetLayout.marginWidth = 0;
        singleChangesetLayout.marginHeight = 0;
        singleChangesetLayout.horizontalSpacing = getHorizontalSpacing();
        singleChangesetLayout.verticalSpacing = getVerticalSpacing();
        singleChangesetComposite.setLayout(singleChangesetLayout);

        GridDataBuilder.newInstance().hSpan(layout).hGrab().hFill().hIndent(getHorizontalSpacing() * 4).applyTo(
            singleChangesetComposite);

        final Label changesetLabel = new Label(singleChangesetComposite, SWT.NONE);
        changesetLabel.setText(Messages.getString("RollbackItemControl.Changeset")); //$NON-NLS-1$
        singleChangesetControls.add(changesetLabel);

        singleChangesetText = new Text(singleChangesetComposite, SWT.BORDER);
        GridDataBuilder.newInstance().wCHint(singleChangesetText, 10).applyTo(singleChangesetText);
        singleChangesetControls.add(singleChangesetText);

        singleChangesetText.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(final ModifyEvent e) {
                singleChangesetID = ((Text) e.widget).getText();
            }
        });

        singleChangesetValidator = new ChangesetValidator(singleChangesetText);
        validator.addValidator(singleChangesetValidator);

        final Button findButton = new Button(singleChangesetComposite, SWT.NONE);
        findButton.setText(Messages.getString("RollbackItemControl.FindButtonTitle")); //$NON-NLS-1$
        singleChangesetControls.add(findButton);

        findButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
                findChangesetPressed(singleChangesetText);
            }
        });
    }

    private void createChangesetRangeOption(final Composite composite) {
        final GridLayout layout = (GridLayout) composite.getLayout();

        changesetRangeButton = new Button(composite, SWT.RADIO);
        changesetRangeButton.setText(Messages.getString("RollbackItemControl.ChangesetRange")); //$NON-NLS-1$
        GridDataBuilder.newInstance().hSpan(layout).applyTo(changesetRangeButton);

        changesetRangeButton.setSelection(RollbackOperationType.CHANGESET_RANGE == rollbackOperationType);
        changesetRangeButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
                if (changesetRangeButton.getSelection()) {
                    rollbackOperationTypeChanged(RollbackOperationType.CHANGESET_RANGE);
                }
            }
        });

        final Composite changesetRangeComposite = new Composite(composite, SWT.NONE);

        final GridLayout changesetRangeLayout = new GridLayout(6, false);
        changesetRangeLayout.marginWidth = 0;
        changesetRangeLayout.marginHeight = 0;
        changesetRangeLayout.horizontalSpacing = getHorizontalSpacing();
        changesetRangeLayout.verticalSpacing = getVerticalSpacing();
        changesetRangeComposite.setLayout(changesetRangeLayout);

        GridDataBuilder.newInstance().hSpan(layout).hGrab().hFill().hIndent(getHorizontalSpacing() * 4).applyTo(
            changesetRangeComposite);

        final Label changesetFromLabel = new Label(changesetRangeComposite, SWT.NONE);
        changesetFromLabel.setText(Messages.getString("RollbackItemControl.ChangestFrom")); //$NON-NLS-1$
        changesetRangeControls.add(changesetFromLabel);

        changesetFromText = new Text(changesetRangeComposite, SWT.BORDER);
        GridDataBuilder.newInstance().wCHint(changesetFromText, 10).applyTo(changesetFromText);
        changesetRangeControls.add(changesetFromText);

        changesetFromText.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(final ModifyEvent e) {
                changesetFromID = ((Text) e.widget).getText();
            }
        });

        changesetFromValidator = new ChangesetValidator(changesetFromText);
        validator.addValidator(changesetFromValidator);

        final Button findFromButton = new Button(changesetRangeComposite, SWT.NONE);
        findFromButton.setText(Messages.getString("RollbackItemControl.FindButtonTitle")); //$NON-NLS-1$
        changesetRangeControls.add(findFromButton);

        findFromButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
                findChangesetPressed(changesetFromText);
            }
        });

        final Label changesetToLabel = new Label(changesetRangeComposite, SWT.NONE);
        changesetToLabel.setText(Messages.getString("RollbackItemControl.ChangesetTo")); //$NON-NLS-1$
        changesetRangeControls.add(changesetToLabel);

        changesetToText = new Text(changesetRangeComposite, SWT.BORDER);
        GridDataBuilder.newInstance().wCHint(changesetToText, 10).applyTo(changesetToText);
        changesetRangeControls.add(changesetToText);

        changesetToText.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(final ModifyEvent e) {
                changesetToID = ((Text) e.widget).getText();
            }
        });

        changesetToValidator = new ChangesetValidator(changesetToText);
        validator.addValidator(changesetToValidator);

        final Button findToButton = new Button(changesetRangeComposite, SWT.NONE);
        findToButton.setText(Messages.getString("RollbackItemControl.FindButtonTitle")); //$NON-NLS-1$
        changesetRangeControls.add(findToButton);

        findToButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
                findChangesetPressed(changesetToText);
            }
        });
    }

    private void createSpecificVersionOption(final Composite composite) {
        final GridLayout layout = (GridLayout) composite.getLayout();

        specificVersionButton = new Button(composite, SWT.RADIO);
        specificVersionButton.setText(Messages.getString("RollbackItemControl.SpecificVersion")); //$NON-NLS-1$
        GridDataBuilder.newInstance().hSpan(layout).applyTo(specificVersionButton);

        specificVersionButton.setSelection(RollbackOperationType.SPECIFIC_VERSION == rollbackOperationType);
        specificVersionButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
                if (specificVersionButton.getSelection()) {
                    rollbackOperationTypeChanged(RollbackOperationType.SPECIFIC_VERSION);
                }
            }
        });

        final Composite specificVersionComposite = new Composite(composite, SWT.NONE);

        final GridLayout specificVersionLayout = new GridLayout(6, false);
        specificVersionLayout.marginWidth = 0;
        specificVersionLayout.marginHeight = 0;
        specificVersionLayout.horizontalSpacing = getHorizontalSpacing();
        specificVersionLayout.verticalSpacing = getVerticalSpacing();
        specificVersionComposite.setLayout(specificVersionLayout);

        GridDataBuilder.newInstance().hSpan(layout).hGrab().hFill().hIndent(getHorizontalSpacing() * 4).applyTo(
            specificVersionComposite);

        versionPicker = new VersionPickerControl(
            specificVersionComposite,
            SWT.NONE,
            Messages.getString("RollbackItemControl.ValidationMessage"), //$NON-NLS-1$
            new VersionDescription[] {
                VersionDescription.CHANGESET,
                VersionDescription.DATE,
                VersionDescription.LABEL,
                VersionDescription.WORKSPACE
        }, VersionDescription.CHANGESET);
        versionPicker.setText(Messages.getString("RollbackItemControl.VersionSpecType")); //$NON-NLS-1$
        versionPicker.setRepository(repository);
        versionPicker.setPath(itemPath);
        GridDataBuilder.newInstance().hGrab().hFill().applyTo(versionPicker);
        specificVersionControls.add(versionPicker);

        validator.addValidatable(versionPicker);
    }

    private void findChangesetPressed(final Text changesetText) {
        final FindChangesetDialog dialog = new FindChangesetDialog(getShell(), repository);
        dialog.setCloseOnlyMode(false);
        dialog.setPath(itemPath);

        if (IDialogConstants.OK_ID != dialog.open()) {
            return;
        }

        setChangesetID(changesetText, dialog.getSelectedChangeset());
    }

    private void browsePressed() {
        final ServerItemSource serverItemSource = new WorkspaceItemSource(repository.getWorkspace());

        String initialItem = ServerPath.ROOT;
        if (!StringUtil.isNullOrEmpty(itemPath) && ServerPath.isServerPath(itemPath)) {
            initialItem = itemPath;
        }

        final ServerItemPickerDialog dialog =
            new ServerItemPickerDialog(
                getShell(),
                Messages.getString("RollbackItemControl.ItemPickerDialogTitle"), //$NON-NLS-1$
                initialItem,
                serverItemSource);

        if (IDialogConstants.OK_ID != dialog.open()) {
            return;
        }

        setItem(dialog.getSelectedServerPath());
    }

    private void rollbackOperationTypeChanged(final RollbackOperationType rollbackOperationType) {
        this.rollbackOperationType = rollbackOperationType;

        enableControls(singleChangesetControls, RollbackOperationType.SINGLE_CHANGESET == rollbackOperationType);
        enableControls(changesetRangeControls, RollbackOperationType.CHANGESET_RANGE == rollbackOperationType);
        enableControls(specificVersionControls, RollbackOperationType.SPECIFIC_VERSION == rollbackOperationType);

        setValidation(rollbackOperationType);
    }

    private void setValidation(final RollbackOperationType rollbackOperationType) {
        singleChangesetValidator.setEnabled(RollbackOperationType.SINGLE_CHANGESET == rollbackOperationType);
        changesetFromValidator.setEnabled(RollbackOperationType.CHANGESET_RANGE == rollbackOperationType);
        changesetToValidator.setEnabled(RollbackOperationType.CHANGESET_RANGE == rollbackOperationType);
    }

    private void enableControls(final List<Control> controls, final boolean enable) {
        for (final Iterator<Control> it = controls.iterator(); it.hasNext();) {
            final Control control = it.next();
            control.setEnabled(enable);
        }
    }

    private void modifyText(final ModifyEvent e) {
        itemPath = ((Text) e.widget).getText().trim();
        if (itemPath.length() == 0) {
            itemPath = null;
        }

        if (itemPath == null) {
            textValidator.setInvalid(Messages.getString("RollbackItemControl.ItemValidationMessage")); //$NON-NLS-1$
        } else {
            textValidator.setValid();
        }
    }

    @Override
    public Validator getValidator() {
        return validator;
    }

}
