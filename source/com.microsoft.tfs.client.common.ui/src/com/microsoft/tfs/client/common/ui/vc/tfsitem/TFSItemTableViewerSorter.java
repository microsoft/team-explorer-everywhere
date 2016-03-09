// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.vc.tfsitem;

import org.eclipse.jface.viewers.TableViewer;

import com.microsoft.tfs.client.common.ui.framework.helper.TableViewerSorter;

public class TFSItemTableViewerSorter extends TableViewerSorter {
    public TFSItemTableViewerSorter(final TableViewer viewer) {
        super(viewer);
    }

    @Override
    public int category(final Object element) {
        if (element instanceof TFSFile) {
            return 2;
        }
        if (element instanceof TFSFolder) {
            return 1;
        }

        return super.category(element);
    }
}
