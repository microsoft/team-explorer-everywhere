// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.vcexplorer.findinsce.tasks;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

import com.microsoft.tfs.client.common.repository.TFSRepository;
import com.microsoft.tfs.client.common.ui.FindInSourceControlQuery;
import com.microsoft.tfs.client.common.ui.dialogs.vc.FindInSourceControlDialog;
import com.microsoft.tfs.client.common.ui.tasks.BaseTask;
import com.microsoft.tfs.client.common.ui.vcexplorer.versioncontrol.FindInSourceControlEditor;
import com.microsoft.tfs.client.common.ui.vcexplorer.versioncontrol.FindInSourceControlEditorInput;
import com.microsoft.tfs.util.Check;

public class FindInSourceControlTask extends BaseTask {
    private final TFSRepository repository;

    private FindInSourceControlEditor editor;
    private FindInSourceControlQuery query = new FindInSourceControlQuery();

    public FindInSourceControlTask(final Shell shell, final TFSRepository repository) {
        super(shell);

        Check.notNull(repository, "repository"); //$NON-NLS-1$

        this.repository = repository;
    }

    public void setEditor(final FindInSourceControlEditor editor) {
        this.editor = editor;
    }

    public void setQuery(final FindInSourceControlQuery query) {
        Check.notNull(query, "query"); //$NON-NLS-1$

        this.query = query;
    }

    @Override
    public IStatus run() {
        final FindInSourceControlDialog findDialog = new FindInSourceControlDialog(getShell(), repository);
        findDialog.setQuery(query);

        if (findDialog.open() != IDialogConstants.OK_ID) {
            return Status.CANCEL_STATUS;
        }

        final FindInSourceControlQuery query = findDialog.getQuery();

        Check.notNull(query, "query"); //$NON-NLS-1$

        final FindInSourceControlEditorInput editorInput = new FindInSourceControlEditorInput(repository, query);

        /* Use the existing editor */
        if (editor != null) {
            editor.setInput(editorInput);
            editor.run();
        }
        /* Build a new editor */
        else {
            final IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
            try {
                final FindInSourceControlEditor editor =
                    (FindInSourceControlEditor) page.openEditor(editorInput, FindInSourceControlEditor.ID);
                editor.run();
            } catch (final PartInitException e) {
                throw new RuntimeException(e);
            }
        }

        return Status.OK_STATUS;
    }
}
