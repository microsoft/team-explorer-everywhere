// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.teambuild.actions;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IEditorPart;

import com.microsoft.tfs.client.common.ui.helpers.EditorHelper;
import com.microsoft.tfs.client.common.ui.teambuild.editors.BuildExplorer;

public class EditBuildQualityAction extends BuildDetailAction {
    @Override
    protected void onSelectionChanged(final IAction action, final ISelection selection) {
        if (action.isEnabled() == false) {
            return;
        }

        super.onSelectionChanged(action, selection);

        if (getEditor() == null) {
            action.setEnabled(false);
        }
    }

    /**
     * @see org.eclipse.ui.IActionDelegate#run(org.eclipse.jface.action.IAction)
     */
    @Override
    public void run(final IAction action) {
        final IEditorPart editor = getEditor();

        if (editor != null) {
            /* open the build explorer control editor. */
            EditorHelper.focusEditor(editor);

            ((BuildExplorer) editor).getBuildEditorPage().getBuildsTableControl().editQuality(getSelectedBuildDetail());
        }
    }
}
