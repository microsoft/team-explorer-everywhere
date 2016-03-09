// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.framework.table.tooltip;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;

import com.microsoft.tfs.util.Check;

public class TableTooltipStyledTextManager extends TableTooltipManager {
    private final TableTooltipStyledTextProvider labelProvider;

    public TableTooltipStyledTextManager(final Table table, final TableTooltipStyledTextProvider labelProvider) {
        super(table);

        Check.notNull(table, "table"); //$NON-NLS-1$
        Check.notNull(labelProvider, "labelProvider"); //$NON-NLS-1$

        this.labelProvider = labelProvider;
    }

    @Override
    protected boolean shouldReplaceTooltip(
        final TableItem newHoverItem,
        final int newHoverColumnIndex,
        final TableItem oldHoverItem,
        final int oldHoverColumnIndex) {
        return true;
    }

    @Override
    protected boolean createTooltip(final Shell shell, final TableItem tableItem, final int columnIndex) {
        if (tableItem == null || tableItem.getData() == null) {
            return false;
        }

        final TableTooltipStyledTextInfo info = labelProvider.getTooltipStyledTextInfo(tableItem.getData());
        if (info == null) {
            return false;
        }

        final FillLayout layout = new FillLayout();
        layout.marginWidth = 4;
        layout.marginHeight = 4;
        shell.setLayout(layout);

        final StyledText tooltipLabel = new StyledText(shell, SWT.MULTI | SWT.READ_ONLY | SWT.WRAP);
        tooltipLabel.setForeground(shell.getDisplay().getSystemColor(SWT.COLOR_INFO_FOREGROUND));
        tooltipLabel.setBackground(shell.getDisplay().getSystemColor(SWT.COLOR_INFO_BACKGROUND));
        tooltipLabel.setData("_TABLEITEM", tableItem); //$NON-NLS-1$

        // Set the tab stops and style ranges. The ranges must be set after the
        // text. The argument to setTabs is an character width, which is
        // returned int he tooltip info. However, the tab stops are set at pixel
        // boundaries based on the the width of spaces. We really want the first
        // tab stop to be to the right of the largest text element, so I'm
        // doubling the size of the tabStop to account for 'n' number of the
        // wider characters. We may need to fudge this number better.
        tooltipLabel.setTabs(info.getTabWidth() * 2);
        tooltipLabel.setText(info.getText());
        tooltipLabel.setStyleRanges(info.getStyleRanges());

        return true;
    }
}
