// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.wit.qe;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;

import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.framework.dialog.BaseDialog;
import com.microsoft.tfs.client.common.ui.framework.helper.MessageBoxHelpers;
import com.microsoft.tfs.core.clients.workitem.fields.FieldDefinitionCollection;
import com.microsoft.tfs.core.clients.workitem.query.qe.ResultOptions;

public class ResultOptionsDialog extends BaseDialog {
    private final FieldDefinitionCollection fieldDefinitions;
    private final ResultOptions resultOptions;

    public ResultOptionsDialog(
        final Shell parentShell,
        final FieldDefinitionCollection fieldDefinitions,
        final ResultOptions resultOptions) {
        super(parentShell);
        this.fieldDefinitions = fieldDefinitions;
        this.resultOptions = resultOptions;
    }

    @Override
    protected void hookAddToDialogArea(final Composite dialogArea) {
        final FillLayout layout = new FillLayout();
        layout.marginWidth = getHorizontalMargin();
        layout.marginHeight = getVerticalMargin();

        dialogArea.setLayout(layout);

        final TabFolder tabFolder = new TabFolder(dialogArea, SWT.NONE);

        final FillLayout tabLayout = new FillLayout();
        tabLayout.marginWidth = getHorizontalMargin();
        tabLayout.marginHeight = getVerticalMargin();

        TabItem tab = new TabItem(tabFolder, SWT.NONE);
        tab.setText(Messages.getString("ResultOptionsDialog.FieldsTabText")); //$NON-NLS-1$

        final Composite displayFieldsComposite = new Composite(tabFolder, SWT.NONE);
        displayFieldsComposite.setLayout(tabLayout);

        new DisplayFieldsResultOptionsControl(displayFieldsComposite, SWT.NONE, fieldDefinitions, resultOptions);
        tab.setControl(displayFieldsComposite);

        tab = new TabItem(tabFolder, SWT.NONE);
        tab.setText(Messages.getString("ResultOptionsDialog.SortingTabText")); //$NON-NLS-1$

        final Composite sortFieldsComposite = new Composite(tabFolder, SWT.NONE);
        sortFieldsComposite.setLayout(tabLayout);

        new SortFieldsResultOptionsControl(sortFieldsComposite, SWT.NONE, fieldDefinitions, resultOptions);
        tab.setControl(sortFieldsComposite);
    }

    @Override
    protected Point defaultComputeInitialSize() {
        return new Point(500, 400);
    }

    @Override
    protected String provideDialogTitle() {
        return Messages.getString("ResultOptionsDialog.DialogTitle"); //$NON-NLS-1$
    }

    @Override
    protected void buttonPressed(final int buttonId) {
        if (IDialogConstants.OK_ID == buttonId) {
            if (resultOptions.getDisplayFields().getCount() == 0) {
                MessageBoxHelpers.errorMessageBox(
                    getShell(),
                    Messages.getString("ResultOptionsDialog.ErrorDialogTitle"), //$NON-NLS-1$
                    Messages.getString("ResultOptionsDialog.ErrorDialogText")); //$NON-NLS-1$
                return;
            }
        }
        super.buttonPressed(buttonId);
    }
}
