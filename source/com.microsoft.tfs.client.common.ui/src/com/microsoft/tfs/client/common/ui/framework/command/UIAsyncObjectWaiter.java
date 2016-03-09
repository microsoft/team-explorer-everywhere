// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.framework.command;

import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swt.widgets.Display;

import com.microsoft.tfs.client.common.framework.command.IAsyncObjectWaiter;

/**
 * An {@link IAsyncObjectWaiter} that can be hooked into lower layers (common
 * client) to service the UI event queue while waiting on jobs and threads.
 *
 * @threadsafety thread-safe
 */
public class UIAsyncObjectWaiter implements IAsyncObjectWaiter {
    /**
     * Constructs a {@link UIAsyncObjectWaiter} with no arguments (so it can be
     * contributed as an extension).
     */
    public UIAsyncObjectWaiter() {
    }

    @Override
    public void joinThread(final Thread thread) throws InterruptedException {
        final Display display = Display.getCurrent();

        if (display != null && display.getThread() == Thread.currentThread()) {
            // Thread.join() waits until isAlive() goes false
            while (thread.isAlive()) {
                if (!display.readAndDispatch()) {
                    display.sleep();
                }
            }
        } else {
            // display should only be null if the program is exiting
            thread.join();
        }
    }

    @Override
    public void joinJob(final Job job) throws InterruptedException {
        final Display display = Display.getCurrent();

        if (display != null && display.getThread() == Thread.currentThread()) {
            while (job.getResult() == null) {
                if (!display.readAndDispatch()) {
                    display.sleep();
                }
            }
        } else {
            // display should only be null if the program is exiting
            job.join();
        }
    }

    @Override
    public void waitUntilTrue(final Predicate predicate) throws InterruptedException {
        final Display display = Display.getCurrent();

        if (display != null && display.getThread() == Thread.currentThread()) {
            while (!predicate.isTrue()) {
                if (!display.readAndDispatch()) {
                    display.sleep();
                }
            }
        } else {
            // display should only be null if the program is exiting
            while (!predicate.isTrue()) {
                Thread.sleep(10);
            }
        }
    }
}
