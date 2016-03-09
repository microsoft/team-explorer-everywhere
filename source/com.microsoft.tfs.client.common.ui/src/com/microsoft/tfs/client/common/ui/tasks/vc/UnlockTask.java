// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.tasks.vc;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.swt.widgets.Shell;

import com.microsoft.tfs.client.common.commands.vc.LockCommand;
import com.microsoft.tfs.client.common.framework.resources.command.ResourceChangingCommand;
import com.microsoft.tfs.client.common.repository.TFSRepository;
import com.microsoft.tfs.client.common.ui.tasks.BaseTask;
import com.microsoft.tfs.client.common.vc.TypedItemSpec;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.LockLevel;
import com.microsoft.tfs.util.Check;

/**
 * Unlocks {@link ItemSpecs}s. No dialog is currently raised; all item specs are
 * just unlocked.
 *
 * @threadsafety unknown
 */
public class UnlockTask extends BaseTask {
    private final TFSRepository repository;
    private final TypedItemSpec[] itemSpecs;

    /**
     * Creates a {@link UnlockTask}.
     *
     * @param shell
     *        the shell (must not be <code>null</code>)
     * @param repository
     *        the repository (must not be <code>null</code>)
     * @param itemSpecs
     *        the items to unlock (must not be <code>null</code>)
     */
    public UnlockTask(final Shell shell, final TFSRepository repository, final TypedItemSpec[] itemSpecs) {
        super(shell);

        Check.notNull(repository, "repository"); //$NON-NLS-1$
        Check.notNull(itemSpecs, "itemSpecs"); //$NON-NLS-1$

        this.repository = repository;
        this.itemSpecs = itemSpecs;
    }

    @Override
    public IStatus run() {
        final LockCommand command = new LockCommand(repository, itemSpecs, LockLevel.NONE);

        return getCommandExecutor().execute(new ResourceChangingCommand(command));
    }
}
