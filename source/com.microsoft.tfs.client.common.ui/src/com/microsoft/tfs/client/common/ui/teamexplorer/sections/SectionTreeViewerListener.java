// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.teamexplorer.sections;

import org.eclipse.jface.viewers.ITreeViewerListener;
import org.eclipse.jface.viewers.TreeExpansionEvent;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.widgets.Section;

public class SectionTreeViewerListener implements ITreeViewerListener {
    @Override
    public void treeCollapsed(final TreeExpansionEvent event) {
        reflow(event.getTreeViewer().getControl().getParent());
    }

    @Override
    public void treeExpanded(final TreeExpansionEvent event) {
        reflow(event.getTreeViewer().getControl().getParent());
    }

    private void reflow(final Composite treeComposite) {
        // Get the parent section and scrolled composite
        Section section = null;
        ScrolledComposite scrolled = null;

        Composite parent = treeComposite;
        while (parent != null) {
            if (parent instanceof ScrolledComposite) {
                scrolled = (ScrolledComposite) parent;
                break;
            }
            if (parent instanceof Section) {
                section = (Section) parent;
            }
            parent = parent.getParent();
        }

        if (section != null) {
            final ScrolledComposite scrolledFinal = scrolled;
            final Section sectionFinal = section;

            // Collapsed and expanded are called before the control has
            // actually expanded or collapsed. We need to reflow after the
            // controls have resized, so we queue a runnable which will run
            // after the resize has occurred.
            sectionFinal.getDisplay().asyncExec(new Runnable() {
                @Override
                public void run() {
                    final Point p = scrolledFinal.getOrigin();
                    sectionFinal.setExpanded(false);
                    sectionFinal.setExpanded(true);
                    scrolledFinal.setOrigin(p);
                }
            });
        }
    }
}
