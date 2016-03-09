// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.wit.qe;

import org.eclipse.jface.action.Action;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.widgets.Display;

import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.TFSCommonUIClientPlugin;
import com.microsoft.tfs.client.common.ui.framework.image.ImageHelper;

public class RunQueryAction extends Action {
    private QueryEditor editor;
    private final ImageHelper imageHelper = new ImageHelper(TFSCommonUIClientPlugin.PLUGIN_ID);

    public RunQueryAction() {
        this(null);
    }

    public RunQueryAction(final QueryEditor editor) {
        this.editor = editor;

        setToolTipText(Messages.getString("RunQueryAction.ActionTooltip")); //$NON-NLS-1$
        setImageDescriptor(imageHelper.getImageDescriptor("images/wit/run_query.gif")); //$NON-NLS-1$
    }

    public void setActiveEditor(final QueryEditor editor) {
        this.editor = editor;
    }

    @Override
    public void run() {
        if (editor == null) {
            return;
        }

        BusyIndicator.showWhile(Display.getDefault(), new Runnable() {
            @Override
            public void run() {
                editor.runQuery();
            }
        });
    }
}
