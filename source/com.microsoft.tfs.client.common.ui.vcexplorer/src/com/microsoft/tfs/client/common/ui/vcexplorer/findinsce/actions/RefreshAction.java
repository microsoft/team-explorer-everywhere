// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.vcexplorer.findinsce.actions;

import org.eclipse.jface.action.Action;

import com.microsoft.tfs.client.common.ui.TFSCommonUIClientPlugin;
import com.microsoft.tfs.client.common.ui.framework.image.ImageHelper;
import com.microsoft.tfs.client.common.ui.vcexplorer.Messages;
import com.microsoft.tfs.client.common.ui.vcexplorer.versioncontrol.FindInSourceControlEditor;

public class RefreshAction extends Action {
    private final ImageHelper imageHelper = new ImageHelper(TFSCommonUIClientPlugin.PLUGIN_ID);

    private FindInSourceControlEditor editor;

    public RefreshAction() {
        setImageDescriptor(imageHelper.getImageDescriptor("images/common/refresh.gif")); //$NON-NLS-1$
        setToolTipText(Messages.getString("RefreshAction.ToolTipText")); //$NON-NLS-1$
    }

    public void setActiveEditor(final FindInSourceControlEditor editor) {
        this.editor = editor;
    }

    @Override
    public void run() {
        if (editor == null) {
            return;
        }

        editor.run();
    }
}
