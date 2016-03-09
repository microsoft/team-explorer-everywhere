// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.framework.command;

import org.eclipse.core.runtime.IStatus;

/**
 * A helper class used to compose together {@link ICommandFinishedCallback}
 * instances.
 */
public class MultiCommandFinishedCallback implements ICommandFinishedCallback {
    /**
     * Combines the two {@link ICommandFinishedCallback}s into a single
     * {@link ICommandFinishedCallback} that invokes them both. The arguments to
     * this method are unchanged. If either argument is <code>null</code> the
     * other argument is returned (if both arguments are <code>null</code>,
     * <code>null</code> is returned).
     *
     * @param c1
     *        the first {@link ICommandFinishedCallback} to compose
     * @param c2
     *        the second {@link ICommandFinishedCallback} to compose
     * @return a {@link ICommandFinishedCallback} that composes the two
     *         arguments, or <code>null</code>
     */
    public static ICommandFinishedCallback combine(
        final ICommandFinishedCallback c1,
        final ICommandFinishedCallback c2) {
        if (c1 == null) {
            return c2;
        }

        if (c2 == null) {
            return c1;
        }

        final ICommandFinishedCallback[] callbacks1 = getCallbacks(c1);
        final ICommandFinishedCallback[] callbacks2 = getCallbacks(c2);

        final ICommandFinishedCallback[] callbacks =
            new ICommandFinishedCallback[callbacks1.length + callbacks2.length];

        System.arraycopy(callbacks1, 0, callbacks, 0, callbacks1.length);
        System.arraycopy(callbacks2, 0, callbacks, callbacks1.length, callbacks2.length);

        return new MultiCommandFinishedCallback(callbacks);
    }

    private static ICommandFinishedCallback[] getCallbacks(final ICommandFinishedCallback callback) {
        if (callback instanceof MultiCommandFinishedCallback) {
            return ((MultiCommandFinishedCallback) callback).callbacks;
        }
        return new ICommandFinishedCallback[] {
            callback
        };
    }

    private final ICommandFinishedCallback[] callbacks;

    private MultiCommandFinishedCallback(final ICommandFinishedCallback[] callbacks) {
        this.callbacks = callbacks;
    }

    @Override
    public void onCommandFinished(final ICommand command, final IStatus status) {
        for (int i = 0; i < callbacks.length; i++) {
            callbacks[i].onCommandFinished(command, status);
        }
    }
}
