// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.dialogs.vc;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.controls.vc.ConflictEncodingSelectionControl;
import com.microsoft.tfs.client.common.ui.framework.dialog.BaseDialog;
import com.microsoft.tfs.client.common.ui.framework.layout.GridDataBuilder;
import com.microsoft.tfs.core.clients.versioncontrol.conflicts.ConflictDescription;
import com.microsoft.tfs.core.util.FileEncoding;
import com.microsoft.tfs.util.Check;

public class ConflictResolutionEncodingDialog extends BaseDialog {
    private ConflictEncodingSelectionControl encodingSelectionControl;

    private ConflictDescription conflictDescription;
    private FileEncoding selectedEncoding = null;

    public ConflictResolutionEncodingDialog(final Shell parentShell) {
        super(parentShell);

        setOptionResizableDirections(SWT.HORIZONTAL);
    }

    @Override
    protected String provideDialogTitle() {
        return Messages.getString("ConflictResolutionEncodingDialog.ConflictDialogTitle"); //$NON-NLS-1$
    }

    public void setConflictDescription(final ConflictDescription conflictDescription) {
        Check.notNull(conflictDescription, "conflictDescription"); //$NON-NLS-1$

        this.conflictDescription = conflictDescription;

        if (conflictDescription != null && encodingSelectionControl != null && !encodingSelectionControl.isDisposed()) {
            encodingSelectionControl.setConflictDescription(conflictDescription);
        }
    }

    @Override
    protected void hookAddToDialogArea(final Composite dialogArea) {
        final GridLayout layout = new GridLayout(1, true);
        layout.marginWidth = getHorizontalMargin();
        layout.marginHeight = getVerticalMargin();
        layout.horizontalSpacing = getHorizontalSpacing();
        layout.verticalSpacing = getVerticalSpacing();
        dialogArea.setLayout(layout);

        final Label descriptionLabel = new Label(dialogArea, SWT.NONE);
        descriptionLabel.setText(Messages.getString("ConflictResolutionEncodingDialog.DescriptionLabelText")); //$NON-NLS-1$

        encodingSelectionControl = new ConflictEncodingSelectionControl(dialogArea, SWT.NONE);
        GridDataBuilder.newInstance().hGrab().hFill().applyTo(encodingSelectionControl);

        if (conflictDescription != null) {
            encodingSelectionControl.setConflictDescription(conflictDescription);
        }
    }

    @Override
    protected void hookDialogAboutToClose() {
        selectedEncoding = encodingSelectionControl.getFileEncoding();
    }

    public FileEncoding getFileEncoding() {
        return selectedEncoding;
    }
}
