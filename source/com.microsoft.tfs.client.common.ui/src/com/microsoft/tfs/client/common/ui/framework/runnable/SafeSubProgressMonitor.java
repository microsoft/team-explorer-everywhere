// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.framework.runnable;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;

/**
 * <p>
 * {@link SafeSubProgressMonitor} is a safer alternative to
 * {@link SubProgressMonitor}. <b>Note:</b> if you can take a dependency on
 * Eclipse 3.3, you should use SubMonitor instead of this class.
 * </p>
 *
 * <p>
 * {@link SubProgressMonitor} requires {@link #beginTask(String, int)} to be
 * called - if it is not, no work is reported to the parent progress monitor
 * when {@link #done()} is called. This requires that you either call
 * {@link #beginTask(String, int)} yourself before calling worker code, or
 * assume that worker code will call {@link #beginTask(String, int)}. Neither of
 * these options are very good, especially when calling foreign worker code
 * (i.e., as part of a framework).
 * </p>
 *
 * <p>
 * {@link SafeSubProgressMonitor} does not have the same requirement. If
 * {@link #beginTask(String, int)} is not called, {@link #done()} will still
 * report the proper amount of work to the parent.
 * </p>
 *
 * <p>
 * In addition, {@link SafeSubProgressMonitor} will set the main task name
 * passed in {@link #beginTask(String, int)} as the parent's subtask name. In
 * contrast, {@link SubProgressMonitor} does not do anything with the main task
 * name. If this behavior is not desired, create a
 * {@link SafeSubProgressMonitor} with the style bit
 * {@link SubProgressMonitor#SUPPRESS_SUBTASK_LABEL}.
 * </p>
 */
public class SafeSubProgressMonitor extends SubProgressMonitor {
    private boolean beginTaskCalled = false;
    private boolean doneCalled = false;

    public SafeSubProgressMonitor(final IProgressMonitor monitor, final int ticks, final int style) {
        super(monitor, ticks, style);
    }

    public SafeSubProgressMonitor(final IProgressMonitor monitor, final int ticks) {
        super(monitor, ticks);
    }

    @Override
    public void beginTask(final String name, final int totalWork) {
        beginTaskCalled = true;
        super.beginTask(name, totalWork);
        subTask(name);
    }

    @Override
    public void done() {
        if (!beginTaskCalled && !doneCalled) {
            beginTask("", IProgressMonitor.UNKNOWN); //$NON-NLS-1$
        }
        doneCalled = true;
        super.done();
    }
}
