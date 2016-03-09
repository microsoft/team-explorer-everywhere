// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.framework.helper;

import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;

import com.microsoft.tfs.client.common.ui.framework.WindowSystem;
import com.microsoft.tfs.client.common.ui.framework.celleditor.accessibility.CellEditorAccessibility;

/**
 * Sets up cell editor keyboard accessibility on supported versions and
 * platforms.
 */
public class CellEditorAccessibilityHelper {
    /*
     * Note: this needs to be kept separate from the CellEditorAccessibility
     * class. The JRE will try to resolve any import statements at class
     * loading, which will fail on old versions of Eclipse.
     */
    public static final void setupAccessibleCellEditors(final TableViewer viewer) {
        /*
         * Accessible cell editing had big bugs in 3.3 (and did not exist
         * previously.) Still has big bugs in Mac OS Carbon.
         */
        if (SWT.getVersion() >= 3400 && !WindowSystem.isCurrentWindowSystem(WindowSystem.CARBON)) {
            CellEditorAccessibility.setupAccessibleCellEditors(viewer);
        }
    }

    /**
     * Attempts to get the index (x, y) of the currently selected cell. Only
     * works if accessible cell editing has been setup on the given table
     * viewer.
     */
    public static final Point getFocusCellIndex(final TableViewer viewer) {
        if (SWT.getVersion() >= 3400 && !WindowSystem.isCurrentWindowSystem(WindowSystem.CARBON)) {
            return CellEditorAccessibility.getFocusCellIndex(viewer);
        }

        return new Point(-1, -1);
    }
}
