// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.vcexplorer.findinsce.actions;

import java.text.MessageFormat;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;

import com.microsoft.tfs.client.common.commands.vc.UndoCommand;
import com.microsoft.tfs.client.common.framework.command.CommandList;
import com.microsoft.tfs.client.common.repository.TFSRepository;
import com.microsoft.tfs.client.common.ui.TFSCommonUIClientPlugin;
import com.microsoft.tfs.client.common.ui.commands.UndoOtherPendingChangeCommand;
import com.microsoft.tfs.client.common.ui.dialogs.vc.ConflictDialog;
import com.microsoft.tfs.client.common.ui.framework.command.UICommandExecutorFactory;
import com.microsoft.tfs.client.common.ui.framework.image.ImageHelper;
import com.microsoft.tfs.client.common.ui.vcexplorer.Messages;
import com.microsoft.tfs.client.common.ui.vcexplorer.versioncontrol.FindInSourceControlEditor;
import com.microsoft.tfs.client.common.ui.vcexplorer.versioncontrol.FindInSourceControlEditorInput;
import com.microsoft.tfs.core.clients.versioncontrol.Workstation;
import com.microsoft.tfs.core.clients.versioncontrol.conflicts.ConflictDescription;
import com.microsoft.tfs.core.clients.versioncontrol.exceptions.MultipleWorkspacesFoundException;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.ItemType;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.PendingChange;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.PendingSet;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.RecursionType;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Workspace;
import com.microsoft.tfs.core.clients.versioncontrol.specs.ItemSpec;
import com.microsoft.tfs.core.clients.versioncontrol.workspacecache.WorkspaceInfo;

public class UndoAction extends Action {
    private final static Log log = LogFactory.getLog(UndoAction.class);

    private final ImageHelper imageHelper = new ImageHelper(TFSCommonUIClientPlugin.PLUGIN_ID);

    private FindInSourceControlEditor editor;

    public UndoAction() {
        setImageDescriptor(imageHelper.getImageDescriptor("images/vc/undo.gif")); //$NON-NLS-1$
        setToolTipText(Messages.getString("UndoAction.ToolTipText")); //$NON-NLS-1$
    }

    public UndoAction(final FindInSourceControlEditor editor) {
        this();

        setActiveEditor(editor);
    }

    public void setActiveEditor(final FindInSourceControlEditor editor) {
        this.editor = editor;

        setEnabled(isEnabled());

        this.editor.addSelectionChangedListener(new ISelectionChangedListener() {
            @Override
            public void selectionChanged(final SelectionChangedEvent event) {
                setEnabled(isEnabled());
                editor.getEditorSite().getActionBars().getToolBarManager().update(true);
            }
        });
    }

    @Override
    public boolean isEnabled() {
        return (editor != null
            && editor.getEditorInput() != null
            && editor.getSelectedPendingChanges() != null
            && !editor.getSelectedPendingChanges().isEmpty()
            && editor.getEditorInput().getQuery().showStatus());
    }

    @Override
    public void run() {
        if (editor == null || editor.getEditorInput() == null) {
            return;
        }

        final FindInSourceControlEditorInput editorInput = editor.getEditorInput();

        final TFSRepository repository = editorInput.getRepository();
        final Map<PendingChange, PendingSet> pendingChangeMap = editor.getSelectedPendingChanges();

        if (pendingChangeMap == null || pendingChangeMap.size() == 0) {
            return;
        }

        /*
         * Build a composite undo command for each pending change. Stop
         * processing only on cancel. If we're undoing a pending change in the
         * local workspace, keep that command so we can query conflicts with it.
         */
        UndoCommand localWorkspaceCommand = null;
        final CommandList undoCommands = new CommandList(
            Messages.getString("UndoAction.UndoCommandName"), //$NON-NLS-1$
            Messages.getString("UndoAction.UndoCommandError"), //$NON-NLS-1$
            new int[] {
                IStatus.OK,
                IStatus.INFO,
                IStatus.WARNING,
                IStatus.ERROR
        });

        for (final Entry<PendingChange, PendingSet> entry : pendingChangeMap.entrySet()) {
            final PendingChange pendingChange = entry.getKey();
            final PendingSet pendingSet = entry.getValue();

            final ItemSpec[] itemSpecs = new ItemSpec[] {
                new ItemSpec(
                    pendingChange.getServerItem(),
                    pendingChange.getItemType() == ItemType.FILE ? RecursionType.NONE : RecursionType.FULL)
            };

            /*
             * This is the current workspace.
             */
            if (Workspace.matchOwner(pendingSet.getOwnerName(), repository.getWorkspace().getOwnerName())
                && Workspace.matchComputer(pendingSet.getComputer(), repository.getWorkspace().getComputer())
                && Workspace.matchName(pendingSet.getName(), repository.getWorkspace().getName())) {
                localWorkspaceCommand = new UndoCommand(repository, itemSpecs);
                undoCommands.addCommand(localWorkspaceCommand);

                continue;
            }

            /*
             * Load up the workspace cache so that we can use local (on this
             * computer) workspaces that are not the current workspace.
             */
            final Workstation workstation =
                Workstation.getCurrent(repository.getConnection().getPersistenceStoreProvider());

            try {
                final WorkspaceInfo info = workstation.getLocalWorkspaceInfo(
                    repository.getVersionControlClient(),
                    pendingSet.getName(),
                    pendingSet.getOwnerName());

                if (info == null) {
                    /*
                     * Warn, fall through to non-local behavior which will not
                     * do a get.
                     */
                    log.warn(
                        MessageFormat.format(
                            "Could not find cached workspace with name {0}, will not update disk during undo", //$NON-NLS-1$
                            pendingSet.getName()));
                } else {
                    final Workspace workspace = repository.getVersionControlClient().getWorkspace(info);
                    undoCommands.addCommand(new UndoOtherPendingChangeCommand(workspace, itemSpecs));
                    continue;
                }

            } catch (final MultipleWorkspacesFoundException e) {
                /*
                 * Warn, fall through to non-local behavior which will not do a
                 * get.
                 */
                log.warn(
                    MessageFormat.format(
                        "Multiple workspaces found in workspace cache with name {0}, will not update disk during undo", //$NON-NLS-1$
                        pendingSet.getName()));
            }

            /*
             * Non-local workspace, simply tell VersionControlClient to do the
             * undo, we don't need to worry about GetOperations coming back.
             */
            undoCommands.addCommand(
                new UndoOtherPendingChangeCommand(
                    repository.getVersionControlClient(),
                    pendingSet.getName(),
                    pendingSet.getOwnerName(),
                    itemSpecs));

        }

        /* Prompt for confirmation. */
        final String title, prompt;

        if (pendingChangeMap.entrySet().size() == 1) {
            title = Messages.getString("UndoAction.UndoSingleConfirmTitle"); //$NON-NLS-1$
            prompt = Messages.getString("UndoAction.UndoSingleConfirmPrompt"); //$NON-NLS-1$
        } else {
            title = Messages.getString("UndoAction.UndoMultipleConfirmTitle"); //$NON-NLS-1$
            prompt = Messages.getString("UndoAction.UndoMultipleConfirmPrompt"); //$NON-NLS-1$
        }

        if (!MessageDialog.openQuestion(editor.getSite().getShell(), title, prompt)) {
            return;
        }

        /*
         * Execute all the commands - if we had a pending change in the local
         * workspace and it has conflicts, prompt to resolve.
         */
        UICommandExecutorFactory.newUICommandExecutor(editor.getSite().getShell()).execute(undoCommands);

        if (localWorkspaceCommand != null && localWorkspaceCommand.hasConflicts()) {
            final ConflictDescription[] conflicts = localWorkspaceCommand.getConflictDescriptions();

            final ConflictDialog conflictDialog =
                new ConflictDialog(editor.getSite().getShell(), repository, conflicts);
            conflictDialog.open();
        }

        editor.run();
    }
}
