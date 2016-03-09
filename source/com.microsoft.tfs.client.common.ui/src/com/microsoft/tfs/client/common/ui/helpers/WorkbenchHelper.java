// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.helpers;

import java.lang.reflect.Method;
import java.text.MessageFormat;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.internal.jobs.JobManager;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.internal.Workbench;

import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.TFSCommonUIClientPlugin;
import com.microsoft.tfs.util.Check;

/**
 * Provides an assurance that {@link Runnable}s will be executed after the
 * {@link Workbench} is started.
 *
 * It is not safe to assume that the {@link JobManager} executing your jobs
 * implies that the Workbench is started. Nor is it safe to assume that
 * {@link Display#asyncExec(Runnable)} executing a {@link Runnable} implies that
 * the Workbench is started, despite the javadoc's assurances. (When Eclipse is
 * restarted after a hard crash, it seems to run jobs in the {@link JobManager}
 * and pump the readAndDispatch loop before {@link Workbench} is actually
 * started.
 *
 * @threadsafety unknown
 */
public class WorkbenchHelper {
    private static final Log log = LogFactory.getLog(WorkbenchHelper.class);

    private WorkbenchHelper() {
    }

    public static void runOnWorkbenchStartup(String name, final Runnable runnable) {
        Check.notNull(runnable, "runnable"); //$NON-NLS-1$

        if (name == null) {
            name = Messages.getString("WorkbenchHelper.WorkbenchStartupJobName"); //$NON-NLS-1$
        }

        final Job startupJob = new WorkbenchStartupJob(name, runnable);
        startupJob.setPriority(Job.INTERACTIVE);
        startupJob.setSystem(true);
        startupJob.schedule();
    }

    private static final class WorkbenchStartupJob extends Job {
        private final Runnable runnable;

        public WorkbenchStartupJob(final String name, final Runnable runnable) {
            super(name);

            Check.notNull(runnable, "runnable"); //$NON-NLS-1$
            this.runnable = runnable;
        }

        @Override
        protected IStatus run(final IProgressMonitor monitor) {
            try {
                if (getWorkbench() == null) {
                    log.info(MessageFormat.format("Job '{0}' is waiting for workbench creation", getName())); //$NON-NLS-1$

                    for (int slept = 0; getWorkbench() == null; slept++) {
                        if (slept > 0 && (slept % 5) == 0) {
                            log.warn(
                                MessageFormat.format(
                                    "Job ''{0}'' is waiting for workbench creation after {1} seconds", //$NON-NLS-1$
                                    getName(),
                                    Integer.toString(slept)));
                        }

                        Thread.sleep(1000);
                    }
                }

                if (isWorkbenchStarting()) {
                    log.info(MessageFormat.format("Job '{0}' is waiting for workbench startup", getName())); //$NON-NLS-1$

                    for (int slept = 0; isWorkbenchStarting(); slept++) {
                        if (slept > 0 && (slept % 5) == 0) {
                            log.warn(
                                MessageFormat.format(
                                    "Job ''{0}'' is waiting for workbench startup after {1} seconds", //$NON-NLS-1$
                                    getName(),
                                    Integer.toString(slept)));
                        }

                        Thread.sleep(1000);
                    }
                }

                runnable.run();
                return Status.OK_STATUS;
            } catch (final Exception e) {
                return new Status(Status.ERROR, TFSCommonUIClientPlugin.PLUGIN_ID, 0, null, e);
            }
        }

        private final IWorkbench getWorkbench() {
            /*
             * Will throw if the Workbench has not yet been created.
             */
            try {
                return PlatformUI.getWorkbench();
            } catch (final IllegalStateException e) {
                return null;
            }
        }

        private final boolean isWorkbenchStarting() {
            final IWorkbench workbench = getWorkbench();

            if (workbench == null) {
                return true;
            }

            /*
             * IWorkbench.isStarting() didn't exist until 3.5. Unfortunate.
             */
            if (SWT.getVersion() < 3500) {
                return false;
            }

            /*
             * Use some fun reflection to call IWorkbench.isStarting() so that
             * we can compile on 3.4.
             */
            try {
                final Method isStartingMethod = IWorkbench.class.getMethod("isStarting", new Class[0]); //$NON-NLS-1$
                final Boolean isStarting = (Boolean) isStartingMethod.invoke(workbench, new Object[0]);

                return isStarting;
            } catch (final Exception e) {
                log.warn("Could not determine if workbench is starting", e); //$NON-NLS-1$
                return false;
            }
        }
    }
}
