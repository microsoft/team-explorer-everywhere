// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.dialogs.wit;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.framework.dialog.BaseDialog;
import com.microsoft.tfs.client.common.ui.framework.layout.GridDataBuilder;
import com.microsoft.tfs.client.common.ui.framework.sizing.ControlSize;
import com.microsoft.tfs.client.common.ui.helpers.AutomationIDHelper;

public class InputWorkItemIDDialog extends BaseDialog {
    public static final String TEXT_ID = "InputWorkItemIdDialog.text"; //$NON-NLS-1$

    private Text text;
    private int[] ids;

    public InputWorkItemIDDialog(final Shell parentShell, final int[] ids) {
        super(parentShell);
        this.ids = ids;
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
        label.setText(Messages.getString("InputWorkItemIdDialog.EnterIdsLabelText")); //$NON-NLS-1$

        text = new Text(dialogArea, SWT.BORDER | SWT.MULTI | SWT.WRAP);
        GridDataBuilder.newInstance().hGrab().hFill().vGrab().vFill().applyTo(text);
        AutomationIDHelper.setWidgetID(text, TEXT_ID);

        initialPopulate();

        ControlSize.setCharWidthHint(text, 80);
        ControlSize.setCharHeightHint(text, 10);
    }

    private void initialPopulate() {
        if (ids == null) {
            return;
        }

        final StringBuffer sb = new StringBuffer();
        for (int i = 0; i < ids.length; i++) {
            sb.append(ids[i]);
            if (i < ids.length - 1) {
                sb.append(","); //$NON-NLS-1$
            }
        }

        text.setText(sb.toString());

        text.selectAll();
    }

    @Override
    protected void okPressed() {
        parseIDs();
        super.okPressed();
    }

    public int[] getIDs() {
        return ids;
    }

    private void parseIDs() {
        final List integers = new ArrayList();
        final String[] inputs = text.getText().split("[, \n\r]"); //$NON-NLS-1$
        for (int i = 0; i < inputs.length; i++) {
            try {
                final Integer integer = Integer.valueOf(inputs[i]);
                if (!integers.contains(integer)) {
                    integers.add(integer);
                }
            } catch (final NumberFormatException ex) {
                // ignore unparsable textf
            }
        }

        ids = new int[integers.size()];
        for (int i = 0; i < ids.length; i++) {
            ids[i] = ((Integer) integers.get(i)).intValue();
        }
    }

    @Override
    protected String provideDialogTitle() {
        return Messages.getString("InputWorkItemIdDialog.EnterIdsDialogTitle"); //$NON-NLS-1$
    }
}
