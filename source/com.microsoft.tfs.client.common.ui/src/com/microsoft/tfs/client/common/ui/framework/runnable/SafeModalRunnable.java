// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.framework.runnable;

import org.eclipse.swt.widgets.Display;

import com.microsoft.tfs.client.common.ui.framework.helper.ShellUtils;

/**
 * <p>
 * A Runnable implementation that wraps another Runnable. This implementation is
 * intended only to be used as an argument to Display.asyncExec. A
 * SafeModalRunnable guarantees that the run() method of the wrapped Runnable
 * will only be invoked once there are no modal Shells.
 * </p>
 * <p>
 * A SafeModalRunnable is mostly useful for asynchronously raising a modal
 * Shell, such as an error dialog. Unless you use SafeModalRunnable or some
 * other modality checking mechanism, it is possible to create a modal Shell
 * that is a peer of some existing modal Shell. This condition can easily lead
 * to a hung UI.
 * </p>
 * <p>
 * Typical usage looks like:
 * </p>
 *
 * <pre>
 * Display.asyncExec(new SafeModalRunnable(new Runnable() {
 *     public void run() {
 *         MessageDialog.openError(shell, &quot;message&quot;, &quot;title&quot;);
 *     }
 * }));
 * </pre>
 */
public class SafeModalRunnable implements Runnable {
    private final Runnable wrapped;

    public SafeModalRunnable(final Runnable wrapped) {
        this.wrapped = wrapped;
    }

    @Override
    public void run() {
        final Display display = Display.getCurrent();

        if (display == null) {
            throw new IllegalStateException("this Runnable is intended to only be run from a UI thread"); //$NON-NLS-1$
        }

        if (ShellUtils.getFirstModalShell(display) != null) {
            display.asyncExec(this);
        } else {
            wrapped.run();
        }
    }

}
