// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.dialogs.vc;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;

import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.controls.vc.changes.ChangeItem;
import com.microsoft.tfs.client.common.ui.controls.vc.changes.ChangeItemType;
import com.microsoft.tfs.client.common.ui.controls.vc.changes.ChangesTable;
import com.microsoft.tfs.client.common.ui.framework.dialog.BaseDialog;
import com.microsoft.tfs.client.common.ui.framework.helper.SWTUtil;
import com.microsoft.tfs.client.common.ui.framework.layout.GridDataBuilder;
import com.microsoft.tfs.client.common.ui.helpers.AutomationIDHelper;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.PendingChange;
import com.microsoft.tfs.util.Check;

public abstract class AbstractUndoPendingChangesDialog extends BaseDialog {
    private final ChangeItem[] changeItems;

    private ChangesTable table;
    private String labelText = ""; //$NON-NLS-1$

    public static final String CHANGES_TABLE_ID =
        "com.microsoft.tfs.client.common.ui.dialogs.vc.AbstractUndoPendingChangesDialog.changesTable"; //$NON-NLS-1$

    public AbstractUndoPendingChangesDialog(final Shell parentShell, final ChangeItem[] changeItems) {
        super(parentShell);
        this.changeItems = changeItems;
    }

    protected ChangesTable getChangesTable() {
        return table;
    }

    public void setLabelText(final String labelText) {
        Check.notNull(labelText, "labelText"); //$NON-NLS-1$
        this.labelText = labelText;
    }

    public PendingChange[] getCheckedPendingChanges() {
        return ChangeItem.getPendingChanges(table.getCheckedChangeItems());
    }

    @Override
    protected void hookAddToDialogArea(final Composite dialogArea) {
        final GridLayout layout = SWTUtil.gridLayout(dialogArea);
        layout.marginWidth = getHorizontalMargin();
        layout.marginHeight = getVerticalMargin();
        layout.verticalSpacing = 10;

        final Composite labelComposite = SWTUtil.createComposite(dialogArea);
        SWTUtil.fillLayout(labelComposite);
        GridDataBuilder.newInstance().hFill().hGrab().wHint(getMinimumMessageAreaWidth()).applyTo(labelComposite);

        SWTUtil.createLabel(labelComposite, SWT.WRAP, labelText);

        final Composite tableComposite = SWTUtil.createComposite(dialogArea);
        SWTUtil.gridLayout(tableComposite, 1, false, 0, 0);
        GridDataBuilder.newInstance().grab().fill().applyTo(tableComposite);

        SWTUtil.createLabel(tableComposite, Messages.getString("AbstractUndoPendingChangesDialog.FilesLabelText")); //$NON-NLS-1$

        table = new ChangesTable(tableComposite, SWT.CHECK, "UndoDialog"); //$NON-NLS-1$
        AutomationIDHelper.setWidgetID(table, CHANGES_TABLE_ID);
        GridDataBuilder.newInstance().grab().fill().hCHint(table, 10).wHint(getMinimumMessageAreaWidth()).applyTo(
            table);

        table.setChangeItems(changeItems, ChangeItemType.PENDING);
        table.checkAll();
    }
}
