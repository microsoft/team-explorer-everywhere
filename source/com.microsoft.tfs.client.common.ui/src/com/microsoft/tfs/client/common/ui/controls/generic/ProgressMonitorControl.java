// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.controls.generic;

import java.lang.reflect.Method;
import java.text.MessageFormat;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IProgressMonitorWithBlocking;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.ProgressIndicator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.framework.layout.GridDataBuilder;

public class ProgressMonitorControl extends BaseControl {
    private static final Log log = LogFactory.getLog(ProgressMonitorControl.class);

    private final Label statusLabel;
    private final ProgressIndicator progressIndicator;

    private final ProgressMonitor progressMonitor = new ProgressMonitor();

    public ProgressMonitorControl(final Composite parent, final int style) {
        super(parent, style);

        final GridLayout layout = new GridLayout(1, true);
        layout.marginWidth = 0;
        layout.marginHeight = 0;
        layout.horizontalSpacing = getHorizontalSpacing();
        layout.verticalSpacing = getVerticalSpacing() / 2;
        setLayout(layout);

        statusLabel = new Label(this, SWT.NONE);
        GridDataBuilder.newInstance().hGrab().hFill().applyTo(statusLabel);

        progressIndicator = new ProgressIndicator(this);
        GridDataBuilder.newInstance().hGrab().hFill().applyTo(progressIndicator);
    }

    public IProgressMonitor getProgressMonitor() {
        return progressMonitor;
    }

    private class ProgressMonitor implements IProgressMonitorWithBlocking {
        private String taskName = null;
        private String subTaskName = null;

        private boolean canceled = false;

        @Override
        public void beginTask(final String name, final int totalWork) {
            if (isDisposed()) {
                return;
            }

            setTaskName(name);
            updateStatusText();

            if (totalWork == IProgressMonitor.UNKNOWN) {
                progressIndicator.beginAnimatedTask();
            } else {
                progressIndicator.beginTask(totalWork);
            }
        }

        private void updateStatusText() {
            if (isDisposed()) {
                return;
            }

            String message;
            if (taskName == null) {
                if (subTaskName == null) {
                    message = Messages.getString("ProgressMonitorControl.Working"); //$NON-NLS-1$
                } else {
                    message = MessageFormat.format(
                        Messages.getString("ProgressMonitorControl.WorkingAndSubtaskFormat"), //$NON-NLS-1$
                        subTaskName);
                }
            } else {
                if (subTaskName == null) {
                    message =
                        MessageFormat.format(Messages.getString("ProgressMonitorControl.TaskNameFormat"), taskName); //$NON-NLS-1$
                } else {
                    message =
                        MessageFormat.format(
                            Messages.getString("ProgressMonitorControl.TaskNameAndSubtaskNameFormat"), //$NON-NLS-1$
                            taskName,
                            subTaskName);
                }
            }

            message = Dialog.shortenText(message, statusLabel);
            statusLabel.setText(message);
        }

        @Override
        public void setBlocked(final IStatus reason) {
            if (isDisposed()) {
                return;
            }

            statusLabel.setText(Dialog.shortenText(reason.getMessage(), statusLabel));

            try {
                final Method showPausedMethod = progressIndicator.getClass().getMethod("showPaused", new Class[0]); //$NON-NLS-1$

                if (showPausedMethod != null) {
                    showPausedMethod.invoke(progressIndicator, new Object[0]);
                }
            } catch (final Exception e) {
                log.debug("Could not show progress monitor control as paused", e); //$NON-NLS-1$
            }
        }

        @Override
        public void clearBlocked() {
            if (isDisposed()) {
                return;
            }

            updateStatusText();

            try {
                final Method showNormalMethod = progressIndicator.getClass().getMethod("showNormal", new Class[0]); //$NON-NLS-1$

                if (showNormalMethod != null) {
                    showNormalMethod.invoke(progressIndicator, new Object[0]);
                }
            } catch (final Exception e) {
                log.debug("Could not show progress monitor control as normal", e); //$NON-NLS-1$
            }
        }

        @Override
        public void done() {
            if (isDisposed()) {
                return;
            }

            statusLabel.setText(""); //$NON-NLS-1$
            progressIndicator.sendRemainingWork();
            progressIndicator.done();
        }

        @Override
        public void setCanceled(final boolean value) {
            canceled = value;
        }

        @Override
        public boolean isCanceled() {
            return canceled;
        }

        @Override
        public void setTaskName(final String taskName) {
            this.taskName = taskName;
        }

        @Override
        public void subTask(final String subTaskName) {
            this.subTaskName = subTaskName;
        }

        @Override
        public void worked(final int work) {
            if (isDisposed()) {
                return;
            }

            progressIndicator.worked(work);
        }

        @Override
        public void internalWorked(final double work) {
            if (isDisposed()) {
                return;
            }

            progressIndicator.worked(work);
        }
    }
}