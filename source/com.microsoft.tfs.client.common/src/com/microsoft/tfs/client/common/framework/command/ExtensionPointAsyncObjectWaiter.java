// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.framework.command;

import java.text.MessageFormat;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.runtime.jobs.Job;

import com.microsoft.tfs.client.common.util.ExtensionLoader;

/**
 * An {@link IAsyncObjectWaiter} that will proxy to an implementation provided
 * by an extension point, or if there is no contribution, falls back to a simple
 * implementation.
 * <p>
 * Prefer using this class to waiting on objects directly, so extensions can
 * perform work like processing UI events.
 * <p>
 * This class is for use inside this plug-in only.
 *
 * @threadsafety thread-safe
 */
public class ExtensionPointAsyncObjectWaiter implements IAsyncObjectWaiter {
    public static final String EXTENSION_POINT_ID = "com.microsoft.tfs.client.common.asyncObjectWaiter"; //$NON-NLS-1$

    private static final Log log = LogFactory.getLog(ExtensionPointAsyncObjectWaiter.class);

    private static final Object extensionLock = new Object();
    private static IAsyncObjectWaiter extension;

    public ExtensionPointAsyncObjectWaiter() {
    }

    @Override
    public void joinThread(final Thread thread) throws InterruptedException {
        final IAsyncObjectWaiter e = getExtension();
        if (e != null) {
            e.joinThread(thread);
        } else {
            thread.join();
        }
    }

    @Override
    public void joinJob(final Job job) throws InterruptedException {
        final IAsyncObjectWaiter e = getExtension();
        if (e != null) {
            e.joinJob(job);
        } else {
            job.join();
        }
    }

    @Override
    public void waitUntilTrue(final Predicate predicate) throws InterruptedException {
        final IAsyncObjectWaiter e = getExtension();
        if (e != null) {
            e.waitUntilTrue(predicate);
        } else {
            while (!predicate.isTrue()) {
                Thread.sleep(10);
            }
        }
    }

    private static IAsyncObjectWaiter getExtension() {
        synchronized (extensionLock) {
            if (extension == null) {
                extension = (IAsyncObjectWaiter) ExtensionLoader.loadSingleExtensionClass(EXTENSION_POINT_ID, false);

                if (extension == null) {
                    log.debug(
                        MessageFormat.format(
                            "No IAsyncObjectWaiter at extension point {0}, using simple implementation", //$NON-NLS-1$
                            EXTENSION_POINT_ID));
                }
            }

            return extension;
        }
    }
}
