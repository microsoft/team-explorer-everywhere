// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.framework.table.tooltip;

/**
 * A simple interface for table-row tooltip text declaration. This is suitable
 * for use with a {@link TableTooltipLabelManager}.
 */
public interface TableTooltipLabelProvider {
    public String getTooltipText(Object element, int columnIndex);
}
