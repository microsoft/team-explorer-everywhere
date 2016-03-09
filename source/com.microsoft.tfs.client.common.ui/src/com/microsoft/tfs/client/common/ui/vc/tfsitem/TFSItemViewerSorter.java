// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.vc.tfsitem;

import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerSorter;

import com.microsoft.tfs.client.common.ui.controls.vc.file.FileControlLabelProvider;

/**
 * An extension of ViewerSorter for use when sorting TFSItems. This
 * implementation separates TFSFiles and TFSFolders into categories, with the
 * folder category sorting higher than the file category. Within each category,
 * the default sort is performed, which sorts on the label provided by the
 * LabelProvider (see ViewerSorter for details on this).
 */
public class TFSItemViewerSorter extends ViewerSorter {
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

    int sortColumn1 = 0;
    int sortColumn2 = 1;
    int sortDirection1 = 1;
    int sortDirection2 = 1;

    @Override
    public int compare(final Viewer viewer, final Object e1, final Object e2) {
        if (category(e1) != category(e2)) {
            return super.compare(viewer, e1, e2);
        }
        if (viewer instanceof TableViewer && e1 instanceof TFSItem && e2 instanceof TFSItem) {
            final FileControlLabelProvider labeler =
                (FileControlLabelProvider) ((TableViewer) viewer).getLabelProvider();

            String str1 = labeler.getColumnText(e1, sortColumn1);
            String str2 = labeler.getColumnText(e2, sortColumn1);

            if (str1 == null) {
                str1 = ""; //$NON-NLS-1$
            }
            if (str2 == null) {
                str2 = ""; //$NON-NLS-1$
            }
            int compare1 = str1.compareToIgnoreCase(str2) * sortDirection1;

            if (compare1 == 0) {
                String str1sub = labeler.getColumnText(e1, sortColumn2);
                String str2sub = labeler.getColumnText(e2, sortColumn2);
                if (str1sub == null) {
                    str1sub = ""; //$NON-NLS-1$
                }
                if (str2sub == null) {
                    str2sub = ""; //$NON-NLS-1$
                }
                if (str1sub != null && str2sub != null) {
                    compare1 = str1sub.compareToIgnoreCase(str2sub) * sortDirection2;
                }
            }
            return compare1;
        } else {
            return super.compare(viewer, e1, e2);
        }
    }

    public void setSortColumn(final int column) {
        if (sortColumn1 == column) {
            sortDirection1 *= -1;
        } else {
            sortColumn2 = sortColumn1;
            sortColumn1 = column;
            sortDirection2 = sortDirection1;
            sortDirection1 = 1;
        }
    }
}
