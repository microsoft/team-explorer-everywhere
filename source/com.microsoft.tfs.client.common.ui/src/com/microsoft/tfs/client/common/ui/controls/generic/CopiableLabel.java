// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.controls.generic;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;

import com.microsoft.tfs.client.common.ui.Messages;

/**
 * An extension of the label class that adds a "Copy" feature to the label text.
 * This is due to the fact that it is impossible to add a read-only text box to
 * a tab in Eclipse 3.2 and have it show the gradient of the tab behind it in
 * Windows XP. The SWT team are considering fixing this for 3.3 but we have to
 * hack around it for now.
 *
 * See https://bugs.eclipse.org/bugs/show_bug.cgi?id=152730 or
 * http://dev.eclipse.org/newslists/news.eclipse.platform.swt/msg30312.html for
 * more detail.
 */
public class CopiableLabel extends Label {

    public CopiableLabel(final org.eclipse.swt.widgets.Composite parent, final int style) {
        super(parent, style);

        final Clipboard clipboard = new Clipboard(parent.getDisplay());

        final Menu menu = new Menu(this);
        final Action copyAction = new CopyLabelAction(this, clipboard);
        copyAction.setText(Messages.getString("CopiableLabel.CopyActionText")); //$NON-NLS-1$
        copyAction.setToolTipText(Messages.getString("CopiableLabel.CopyActionTooltip")); //$NON-NLS-1$

        final ActionContributionItem contrib = new ActionContributionItem(copyAction);
        contrib.fill(menu, -1);

        setMenu(menu);

    }

    private class CopyLabelAction extends Action {
        private final Clipboard clipboard;
        private final CopiableLabel label;

        public CopyLabelAction(final CopiableLabel label, final Clipboard clipboard) {
            this.label = label;
            this.clipboard = clipboard;
        }

        @Override
        public void run() {
            clipboard.setContents(new Object[] {
                label.getText()
            }, new Transfer[] {
                TextTransfer.getInstance()
            });
        }
    }

    @Override
    protected void checkSubclass() {
        // Do nothing - this is a hack.
    }

}
