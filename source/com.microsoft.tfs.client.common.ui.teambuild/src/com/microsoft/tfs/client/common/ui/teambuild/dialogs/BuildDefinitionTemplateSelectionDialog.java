// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.teambuild.dialogs;

import java.util.List;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

import com.microsoft.alm.teamfoundation.build.webapi.BuildDefinitionTemplate;
import com.microsoft.tfs.client.common.ui.framework.dialog.BaseDialog;
import com.microsoft.tfs.client.common.ui.framework.layout.GridDataBuilder;
import com.microsoft.tfs.client.common.ui.teambuild.Messages;
import com.microsoft.tfs.client.common.ui.teambuild.controls.BuildDefinitionTemplatesTable;

public class BuildDefinitionTemplateSelectionDialog extends BaseDialog {
    private final List<BuildDefinitionTemplate> templates;
    private BuildDefinitionTemplatesTable templatesTable;

    public BuildDefinitionTemplateSelectionDialog(
        final Shell parentShell,
        final List<BuildDefinitionTemplate> templates) {
        super(parentShell);
        this.templates = templates;
        setOptionIncludeDefaultButtons(false);
        setOptionEnforceMinimumSize(false);
    }

    @Override
    protected String provideDialogTitle() {
        return Messages.getString("BuildDefinitionTemplateSelectionDialog.DialogTitle"); //$NON-NLS-1$
    }

    @Override
    protected Point defaultComputeInitialSize() {
        final Rectangle parentBounds = getParentShell().getMonitor().getClientArea();

        final int width = Math.min((int) (parentBounds.width * 0.75), 616);
        final int height = Math.min((int) (parentBounds.height * 0.75), 488);
        return new Point(width, height);
    }

    @Override
    protected void hookAddToDialogArea(final Composite dialogArea) {
        final GridLayout layout = new GridLayout(1, true);

        layout.horizontalSpacing = convertHorizontalDLUsToPixels(IDialogConstants.HORIZONTAL_SPACING);
        layout.verticalSpacing = convertVerticalDLUsToPixels(IDialogConstants.VERTICAL_SPACING);
        layout.marginWidth = convertHorizontalDLUsToPixels(IDialogConstants.HORIZONTAL_MARGIN);
        layout.marginHeight = convertVerticalDLUsToPixels(IDialogConstants.VERTICAL_MARGIN);

        dialogArea.setLayout(layout);

        final Label explanationLabel = new Label(dialogArea, SWT.WRAP);
        explanationLabel.setText(Messages.getString("BuildDefinitionTemplateSelectionDialog.ExplanationLabel")); //$NON-NLS-1$
        GridDataBuilder.newInstance().hAlignLeft().vAlignTop().hGrab().hFill().applyTo(explanationLabel);

        templatesTable = new BuildDefinitionTemplatesTable(dialogArea, SWT.NONE);
        templatesTable.setTemplates(templates);
        GridDataBuilder.newInstance().hGrab().hFill().vGrab().cHint(templatesTable, SWT.DEFAULT, 256).applyTo(
            templatesTable);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void hookDialogIsOpen() {
        templatesTable.setColumnWidth();
    }

    @Override
    protected void createButtonsForButtonBar(final Composite parent) {
        createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
        createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void buttonPressed(final int buttonId) {
        super.setReturnCode(buttonId);
        close();
    }

    public BuildDefinitionTemplate getSelectedTemplate() {
        return templatesTable.getSelectedTemplate();
    }
}
