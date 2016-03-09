// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.vcexplorer.findinsce.actions;

import org.eclipse.jface.action.Action;

import com.microsoft.tfs.client.common.ui.vcexplorer.Messages;
import com.microsoft.tfs.client.common.ui.vcexplorer.findinsce.tasks.FindInSourceControlTask;
import com.microsoft.tfs.client.common.ui.vcexplorer.versioncontrol.FindInSourceControlEditor;
import com.microsoft.tfs.client.common.ui.vcexplorer.versioncontrol.FindInSourceControlEditorInput;

public class ModifyQueryAction extends Action {
    private FindInSourceControlEditor editor;

    public ModifyQueryAction() {
        setText(Messages.getString("ModifyQueryAction.ActionText")); //$NON-NLS-1$
        setToolTipText(Messages.getString("ModifyQueryAction.ToolTipText")); //$NON-NLS-1$
    }

    public void setActiveEditor(final FindInSourceControlEditor editor) {
        this.editor = editor;
    }

    @Override
    public void run() {
        final FindInSourceControlEditorInput editorInput = editor.getEditorInput();

        if (editorInput == null) {
            return;
        }

        final FindInSourceControlTask findTask =
            new FindInSourceControlTask(editor.getSite().getShell(), editorInput.getRepository());
        findTask.setQuery(editorInput.getQuery());
        findTask.setEditor(editor);
        findTask.run();
    }
}
