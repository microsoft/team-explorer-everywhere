// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.tasks.vc;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.swt.widgets.Shell;

import com.microsoft.tfs.client.common.repository.TFSRepository;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.LockLevel;
import com.microsoft.tfs.core.clients.versioncontrol.specs.ItemSpec;
import com.microsoft.tfs.core.util.FileEncoding;
import com.microsoft.tfs.util.Check;

/**
 * Checks out the given {@link ItemSpec}s. Does not raise any UI.
 *
 * @threadsafety unknown
 */
public class CheckoutTask extends AbstractCheckoutTask {
    private final ItemSpec[] itemSpecs;
    private final LockLevel lockLevel;
    private final FileEncoding fileEncoding;

    /**
     * Creates a {@link CheckoutTask}.
     *
     * @param shell
     *        the shell (must not be <code>null</code>)
     * @param repository
     *        the repository (must not be <code>null</code>)
     * @param itemSpecs
     *        the items to check out (must not be <code>null</code>)
     * @param lockLevel
     *        the lock level to check out with (must not be <code>null</code>)
     * @param fileEncoding
     *        the file encoding to use (or <code>null</code> for unchanged)
     */
    public CheckoutTask(
        final Shell shell,
        final TFSRepository repository,
        final ItemSpec[] itemSpecs,
        final LockLevel lockLevel,
        final FileEncoding fileEncoding) {
        super(shell, repository);

        Check.notNull(itemSpecs, "itemSpecs"); //$NON-NLS-1$
        Check.notNull(lockLevel, "lockLevel"); //$NON-NLS-1$

        this.itemSpecs = itemSpecs;
        this.lockLevel = lockLevel;
        this.fileEncoding = fileEncoding;
    }

    @Override
    public IStatus run() {
        return checkout(itemSpecs, lockLevel, fileEncoding);
    }
}
