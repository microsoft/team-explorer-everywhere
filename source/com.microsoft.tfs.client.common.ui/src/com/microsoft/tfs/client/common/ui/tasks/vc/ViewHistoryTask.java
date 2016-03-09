// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.tasks.vc;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

import com.microsoft.tfs.client.common.repository.TFSRepository;
import com.microsoft.tfs.client.common.ui.controls.vc.history.HistoryInput;
import com.microsoft.tfs.client.common.ui.editors.HistoryEditor;
import com.microsoft.tfs.client.common.ui.tasks.BaseTask;
import com.microsoft.tfs.client.common.vc.HistoryManager;
import com.microsoft.tfs.core.clients.versioncontrol.path.ServerPath;
import com.microsoft.tfs.core.clients.versioncontrol.specs.ItemSpec;
import com.microsoft.tfs.core.clients.versioncontrol.specs.version.LatestVersionSpec;
import com.microsoft.tfs.core.clients.versioncontrol.specs.version.VersionSpec;
import com.microsoft.tfs.core.clients.versioncontrol.specs.version.WorkspaceVersionSpec;
import com.microsoft.tfs.util.Check;

public class ViewHistoryTask extends BaseTask {
    private final TFSRepository repository;
    private final ItemSpec item;

    public ViewHistoryTask(final Shell shell, final TFSRepository repository, final ItemSpec item) {
        super(shell);

        Check.notNull(repository, "repository"); //$NON-NLS-1$
        Check.notNull(item, "item"); //$NON-NLS-1$

        this.repository = repository;
        this.item = item;
    }

    @Override
    public IStatus run() {
        /*
         * Use a workspace version spec if the path is local and it is mapped,
         * otherwise latest (covers server paths).
         */
        VersionSpec version = LatestVersionSpec.INSTANCE;

        if (ServerPath.isServerPath(item.getItem()) == false
            && repository.getWorkspace().translateLocalPathToServerPath(item.getItem()) != null) {
            version = new WorkspaceVersionSpec(repository.getWorkspace());
        }

        final HistoryInput.Builder builder =
            new HistoryInput.Builder(getShell(), repository, item.getItem(), version, item.getRecursionType());
        builder.setSlotMode(HistoryManager.extensionsSupported(repository));

        final HistoryInput editorInput = builder.build();

        final IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
        try {
            final HistoryEditor editor = (HistoryEditor) page.openEditor(editorInput, HistoryEditor.ID);
            editor.run();
        } catch (final PartInitException e) {
            throw new RuntimeException(e);
        }

        return Status.OK_STATUS;
    }
}