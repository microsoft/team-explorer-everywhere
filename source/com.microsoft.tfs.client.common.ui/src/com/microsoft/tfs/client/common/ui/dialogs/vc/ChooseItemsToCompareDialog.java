// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.dialogs.vc;

import java.text.MessageFormat;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Shell;

import com.microsoft.tfs.client.common.repository.TFSRepository;
import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.controls.vc.ItemAndVersionPicker;
import com.microsoft.tfs.client.common.ui.framework.dialog.BaseDialog;
import com.microsoft.tfs.client.common.ui.framework.helper.MessageBoxHelpers;
import com.microsoft.tfs.client.common.ui.framework.layout.GridDataBuilder;
import com.microsoft.tfs.client.common.ui.framework.validation.ButtonValidatorBinding;
import com.microsoft.tfs.client.common.ui.vc.ItemAndVersionResult;
import com.microsoft.tfs.client.common.ui.vc.ItemAndVersionResult.ValidationException;
import com.microsoft.tfs.core.clients.versioncontrol.specs.version.VersionSpec;
import com.microsoft.tfs.util.valid.MultiValidator;

/**
 * Prompts the user to choose an item to compare with some initially selected
 * item. This class uses the Eclipse compare terms "modified" and "original" to
 * mean "source" and "target" ("modified" = "source" because the source item is
 * usually a workspace item which could be edited).
 */
public class ChooseItemsToCompareDialog extends BaseDialog {
    private static final Log log = LogFactory.getLog(ChooseItemsToCompareDialog.class);

    private final String initialModifiedItem;
    private final String initialOriginalItem;
    private final TFSRepository repository;
    private final boolean isDirectory;

    private ItemAndVersionPicker modifiedItemAndVersionPicker;
    private ItemAndVersionPicker originalItemAndVersionPicker;

    private ItemAndVersionResult modifiedResult;
    private ItemAndVersionResult originalResult;

    public ChooseItemsToCompareDialog(
        final Shell parentShell,
        final String initialLeftItem,
        final String initialRightItem,
        final boolean isDirectory,
        final TFSRepository repository) {
        super(parentShell);

        initialModifiedItem = initialLeftItem;
        initialOriginalItem = initialRightItem;
        this.isDirectory = isDirectory;
        this.repository = repository;
    }

    public String getModifiedItem() {
        return modifiedItemAndVersionPicker.getItem();
    }

    public VersionSpec getModifiedVersion() {
        return modifiedItemAndVersionPicker.getVersionSpec();
    }

    public String getOriginalItem() {
        return originalItemAndVersionPicker.getItem();
    }

    public VersionSpec getOriginalVersion() {
        return originalItemAndVersionPicker.getVersionSpec();
    }

    public ItemAndVersionResult getModifiedResult() {
        return modifiedResult;
    }

    public ItemAndVersionResult getOriginalResult() {
        return originalResult;
    }

    @Override
    protected void hookAddToDialogArea(final Composite dialogArea) {
        final GridLayout layout = new GridLayout(1, false);
        layout.marginWidth = getHorizontalMargin();
        layout.marginHeight = getVerticalMargin();
        layout.horizontalSpacing = getHorizontalSpacing();
        layout.verticalSpacing = getVerticalSpacing();
        dialogArea.setLayout(layout);

        modifiedItemAndVersionPicker =
            createItemAndVersionPicker(
                dialogArea,
                Messages.getString("ChooseItemsToCompareDialog.SourcePickerTitler"), //$NON-NLS-1$
                repository,
                initialModifiedItem);
        modifiedItemAndVersionPicker.setIsDirectory(isDirectory);

        originalItemAndVersionPicker =
            createItemAndVersionPicker(
                dialogArea,
                Messages.getString("ChooseItemsToCompareDialog.TargetPickerTitle"), //$NON-NLS-1$
                repository,
                initialOriginalItem);
        originalItemAndVersionPicker.setIsDirectory(isDirectory);
        super.setOptionResizableDirections(SWT.HORIZONTAL);
    }

    @Override
    protected String provideDialogTitle() {
        return Messages.getString("ChooseItemsToCompareDialog.DialogTitle"); //$NON-NLS-1$
    }

    @Override
    protected void hookAfterButtonsCreated() {
        final MultiValidator validator = new MultiValidator(this);
        validator.addValidatable(modifiedItemAndVersionPicker);
        validator.addValidatable(originalItemAndVersionPicker);
        new ButtonValidatorBinding(getButton(IDialogConstants.OK_ID)).bind(validator);
    }

    @Override
    protected void okPressed() {
        try {
            modifiedResult = ItemAndVersionResult.newResult(
                getModifiedItem(),
                getModifiedVersion(),
                repository.getVersionControlClient());

            originalResult = ItemAndVersionResult.newResult(
                getOriginalItem(),
                getOriginalVersion(),
                repository.getVersionControlClient());
        } catch (final ValidationException e) {
            MessageBoxHelpers.errorMessageBox(getShell(), null, e.getMessage());
            return;
        }

        if (modifiedResult.getItemType() != originalResult.getItemType()) {
            final String modifiedType = modifiedResult.getItemType().toUIString();
            final String originalType = originalResult.getItemType().toUIString();
            final String messageFormat = Messages.getString("ChooseItemsToCompareDialog.ErrorDialogTextFormat"); //$NON-NLS-1$
            final String message = MessageFormat.format(messageFormat, modifiedType, originalType);

            MessageBoxHelpers.errorMessageBox(getShell(), null, message);
            return;
        }

        super.okPressed();
    }

    private ItemAndVersionPicker createItemAndVersionPicker(
        final Composite parent,
        final String groupBoxLabel,
        final TFSRepository repository,
        final String initialItem) {
        final Group composite = new Group(parent, SWT.NONE);
        composite.setText(groupBoxLabel);
        final FillLayout fillLayout = new FillLayout();
        fillLayout.spacing = getSpacing();
        fillLayout.marginHeight = getVerticalMargin();
        fillLayout.marginWidth = getHorizontalMargin();
        composite.setLayout(fillLayout);
        GridDataBuilder.newInstance().hGrab().hFill().applyTo(composite);

        final ItemAndVersionPicker itemAndVersionPicker = new ItemAndVersionPicker(composite, SWT.NONE);
        itemAndVersionPicker.setRepository(repository);
        itemAndVersionPicker.setItem(initialItem);

        return itemAndVersionPicker;
    }
}
