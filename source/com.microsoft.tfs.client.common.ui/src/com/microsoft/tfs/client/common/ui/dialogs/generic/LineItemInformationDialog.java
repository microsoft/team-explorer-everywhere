// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.dialogs.generic;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import com.microsoft.tfs.client.common.ui.framework.dialog.BaseDialog;
import com.microsoft.tfs.client.common.ui.framework.helper.SWTUtil;
import com.microsoft.tfs.client.common.ui.framework.layout.GridDataBuilder;
import com.microsoft.tfs.util.Check;

public class LineItemInformationDialog extends BaseDialog {
    /*
     * Max value width in characters before wrapping
     */
    private static final int MAX_VALUE_WIDTH = 80;

    private final String title;
    private final String groupBoxTitle;
    private final List lines = new ArrayList();

    public LineItemInformationDialog(final Shell parentShell, final String title, final String groupBoxTitle) {
        super(parentShell);

        Check.notNull(title, "title"); //$NON-NLS-1$

        this.title = title;
        this.groupBoxTitle = groupBoxTitle;
    }

    public void addLine(final String label, final String text) {
        Check.notNull(label, "label"); //$NON-NLS-1$

        lines.add(new String[] {
            label,
            text
        });
    }

    @Override
    protected void createButtonsForButtonBar(final Composite parent) {
        createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
    }

    @Override
    protected void hookAddToDialogArea(final Composite dialogArea) {
        Composite composite = dialogArea;

        if (groupBoxTitle != null) {
            SWTUtil.fillLayout(dialogArea, SWT.NONE, getHorizontalMargin(), getVerticalMargin(), getSpacing());

            final Group group = new Group(dialogArea, SWT.NONE);
            group.setText(groupBoxTitle);
            composite = group;
        }

        final GridLayout layout = new GridLayout(2, false);
        layout.marginWidth = getHorizontalMargin() / 2;
        layout.marginHeight = getVerticalMargin();
        layout.verticalSpacing = getVerticalSpacing() * 2;
        layout.horizontalSpacing = getHorizontalSpacing();
        composite.setLayout(layout);

        for (final Iterator it = lines.iterator(); it.hasNext();) {
            final String[] line = (String[]) it.next();
            addLineWidgets(line[0], line[1], composite);
        }
    }

    private void addLineWidgets(final String label, String text, final Composite parent) {
        final int textWidthHint = convertWidthInCharsToPixels(MAX_VALUE_WIDTH);

        final Label labelWidget = new Label(parent, SWT.NONE);
        labelWidget.setText(label);

        final Text textWidget = new Text(parent, SWT.WRAP | SWT.READ_ONLY);
        GridDataBuilder.newInstance().hGrab().hFill().wHint(textWidthHint).applyTo(textWidget);
        textWidget.setBackground(textWidget.getDisplay().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));

        if (text == null) {
            text = ""; //$NON-NLS-1$
        }
        textWidget.setText(text);
    }

    @Override
    protected String provideDialogTitle() {
        return title;
    }
}
