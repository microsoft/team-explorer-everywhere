// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.framework.command;

public interface ICancellableCommand extends ICommand {
    /**
     * Adds a cancellable changed listener that will be notified when the
     * cancellability of a command changes.
     *
     * @param listener
     *        The {@link ICommandCancellableListener} that is no longer notified
     *        of cancellability changes (not <code>null</code>)
     */
    void addCancellableChangedListener(CommandCancellableListener listener);

    /**
     * Removes a cancellable changed listener.
     *
     * @param listener
     *        The {@link ICommandCancellableListener} that is no longer notified
     *        of cancellability changes (not <code>null</code>)
     */
    void removeCancellableChangedListener(CommandCancellableListener listener);
}
