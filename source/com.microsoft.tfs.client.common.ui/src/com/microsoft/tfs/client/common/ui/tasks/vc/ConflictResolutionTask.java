// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.tasks.vc;

import java.text.MessageFormat;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

import com.microsoft.tfs.client.common.repository.TFSRepository;
import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.TFSCommonUIClientPlugin;
import com.microsoft.tfs.client.common.ui.dialogs.vc.ConflictDialog;
import com.microsoft.tfs.client.common.ui.editors.ConflictResolutionEditor;
import com.microsoft.tfs.client.common.ui.editors.ConflictResolutionEditorInput;
import com.microsoft.tfs.client.common.ui.tasks.BaseTask;
import com.microsoft.tfs.core.clients.versioncontrol.conflicts.ConflictDescription;
import com.microsoft.tfs.util.Check;

public class ConflictResolutionTask extends BaseTask {
    private final boolean openInEditor = true;

    private final TFSRepository repository;
    private final ConflictDescription[] conflictDescriptions;

    public ConflictResolutionTask(
        final Shell shell,
        final TFSRepository repository,
        final ConflictDescription[] conflictDescriptions) {
        super(shell);

        Check.notNull(repository, "repository"); //$NON-NLS-1$

        this.repository = repository;
        this.conflictDescriptions = conflictDescriptions;
    }

    @Override
    public IStatus run() {
        if (openInEditor) {
            final IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();

            try {
                final ConflictResolutionEditorInput editorInput =
                    new ConflictResolutionEditorInput(repository, conflictDescriptions);

                final ConflictResolutionEditor editor =
                    (ConflictResolutionEditor) page.openEditor(editorInput, ConflictResolutionEditor.ID);

                /*
                 * Note: because we override equals in
                 * ConflictResolutionEditorInput, we need to notify
                 * ConflictResolutionEditor that there's actually new content
                 * directly.
                 */
                editor.setInput(editorInput);
            } catch (final PartInitException e) {
                throw new RuntimeException(e);
            }

            return Status.OK_STATUS;
        } else {
            final ConflictDialog conflictDialog = new ConflictDialog(
                getShell(),
                repository,
                (conflictDescriptions != null ? conflictDescriptions : new ConflictDescription[0]));
            conflictDialog.open();

            if (conflictDialog.getUnresolvedCount() == 0) {
                return Status.OK_STATUS;
            }

            return new Status(
                IStatus.WARNING,
                TFSCommonUIClientPlugin.PLUGIN_ID,
                MessageFormat.format(
                    Messages.getString("ConflictResolutionTask.ConflictsRemainFormat"), //$NON-NLS-1$
                    conflictDialog.getUnresolvedCount()),
                null);
        }
    }
}
