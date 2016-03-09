// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.framework.celleditor.accessibility;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.jface.viewers.ColumnViewerEditorActivationEvent;
import org.eclipse.jface.viewers.ColumnViewerEditorActivationStrategy;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerEditor;
import org.eclipse.jface.viewers.TableViewerFocusCellManager;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.TableItem;

import com.microsoft.tfs.util.Check;

/**
 * Turns on cell editor accessibility in TableViewers on Eclipse >= 3.3.
 */
public class CellEditorAccessibility {
    private static final Log log = LogFactory.getLog(CellEditorAccessibility.class);

    public static void setupAccessibleCellEditors(final TableViewer viewer) {
        setupAccessibleCellEditors(
            viewer,
            TableViewerEditor.TABBING_MOVE_TO_ROW_NEIGHBOR
                | TableViewerEditor.TABBING_VERTICAL
                | TableViewerEditor.TABBING_HORIZONTAL
                | TableViewerEditor.KEYBOARD_ACTIVATION);
    }

    public static void setupAccessibleCellEditors(final TableViewer viewer, final int flags) {
        Check.notNull(viewer, "viewer"); //$NON-NLS-1$

        try {
            final MultiRowHighlighter cellHighlighter = new MultiRowHighlighter(viewer);

            final TableViewerFocusCellManager focusCellManager =
                new TableViewerFocusCellManager(viewer, cellHighlighter);

            final ColumnViewerEditorActivationStrategy activationStrategy =
                new ColumnViewerEditorActivationStrategy(viewer) {
                    @Override
                    protected boolean isEditorActivationEvent(final ColumnViewerEditorActivationEvent event) {
                        /*
                         * Deny any cell editor activation if there are multiple
                         * rows selected.
                         */
                        if (getViewer().getSelection() != null
                            && getViewer().getSelection() instanceof StructuredSelection
                            && ((StructuredSelection) getViewer().getSelection()).size() > 1) {
                            return false;
                        }

                        if (event.eventType == ColumnViewerEditorActivationEvent.KEY_PRESSED
                            && event.keyCode == SWT.CR) {
                            return true;
                        }

                        return super.isEditorActivationEvent(event);
                    }
                };

            TableViewerEditor.create(viewer, focusCellManager, activationStrategy, flags);
        } catch (final Exception e) {
            log.warn("Could not configure cell editor accessibility", e); //$NON-NLS-1$
        }
    }

    public static final Point getFocusCellIndex(final TableViewer viewer) {
        try {
            if (viewer.getColumnViewerEditor() != null) {
                final ViewerCell focusCell = viewer.getColumnViewerEditor().getFocusCell();

                if (focusCell != null) {
                    final int x = focusCell.getColumnIndex();
                    final int y = viewer.getTable().indexOf((TableItem) focusCell.getItem());

                    return new Point(x, y);
                }
            }
        } catch (final Exception e) {
            log.warn("Could not determine cell index", e); //$NON-NLS-1$
        }

        return new Point(-1, -1);
    }
}
