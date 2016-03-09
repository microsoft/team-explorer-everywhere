// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.eclipse.ui.egit.teamexplorer;

import org.eclipse.jface.action.Action;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.widgets.Display;

import com.microsoft.tfs.util.URLEncode;

/**
 *
 * An easy reusable action that copies content to clipboard
 */
public class CopyAction extends Action {
    private Clipboard clipboard = null;
    private final String content;

    public CopyAction(final String name, final Display display, final String content) {
        this(name, display, content, false);
    }

    public CopyAction(final String name, final Display display, final String content, final boolean encode) {
        if (encode) {
            this.content = URLEncode.encode(content);
        } else {
            this.content = content;
        }

        this.clipboard = new Clipboard(display);
        setText(name);
    }

    @Override
    public void run() {
        final Transfer[] transferTypes = new Transfer[] {
            TextTransfer.getInstance()
        };

        final String[] transferData = new String[] {
            content
        };
        clipboard.setContents(transferData, transferTypes);
    }
}
