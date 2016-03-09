// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.framework.table.tooltip;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.custom.StyleRange;

public class TableTooltipStyledTextInfo {
    // The text to display in the tooltip. This will contain tabs and newlines
    // in the string.
    private String text;

    // The length of the longest text item in the first column. This is used to
    // estimate where tab stops should be placed to allow for alignment of the
    // second column. This is really inteded for tool tips that have two columns
    // of value (name, value) and we get alignment be placing tabs between the
    // two values.
    private int tabWidth;

    // Style ranges to determine which areas of the text should be bold (usually
    // the second column when displaying key/value pairs)
    private final List<StyleRange> ranges = new ArrayList<StyleRange>();

    public String getText() {
        return text;
    }

    public int getTabWidth() {
        return tabWidth;
    }

    public StyleRange[] getStyleRanges() {
        return ranges.toArray(new StyleRange[ranges.size()]);
    }

    public void setText(final String text) {
        this.text = text;
    }

    public void setTabWidth(final int tabWidth) {
        this.tabWidth = tabWidth;
    }

    public void addStyleRange(final StyleRange styleRange) {
        ranges.add(styleRange);
    }
}
