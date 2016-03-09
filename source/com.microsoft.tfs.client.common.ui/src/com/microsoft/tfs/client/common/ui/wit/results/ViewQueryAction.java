// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.wit.results;

import org.eclipse.jface.action.Action;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.widgets.Display;

import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.TFSCommonUIClientPlugin;
import com.microsoft.tfs.client.common.ui.framework.image.ImageHelper;

public class ViewQueryAction extends Action {
    private QueryResultsEditor editor;
    private final ImageHelper imageHelper = new ImageHelper(TFSCommonUIClientPlugin.PLUGIN_ID);

    public ViewQueryAction() {
        this(null);
    }

    public ViewQueryAction(final QueryResultsEditor editor) {
        this.editor = editor;

        setToolTipText(Messages.getString("ViewQueryAction.ActionTooltip")); //$NON-NLS-1$
        setImageDescriptor(imageHelper.getImageDescriptor("images/wit/query_view.gif")); //$NON-NLS-1$
    }

    public void setActiveEditor(final QueryResultsEditor editor) {
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
                editor.viewQuery();
            }
        });
    }
}
