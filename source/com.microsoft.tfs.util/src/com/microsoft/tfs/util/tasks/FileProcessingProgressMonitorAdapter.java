// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.util.tasks;

import java.text.DecimalFormat;
import java.text.MessageFormat;

public class FileProcessingProgressMonitorAdapter implements TaskMonitor {
    private static final long MONITORING_TIME_INTERVAL = 5 * 1000;
    private static String PERCENT_MESSAGE_FORMAT = "{0}/{1} ({2,number,percent})"; //$NON-NLS-1$

    private final long alreadyProcessed;
    private final long totalLength;
    private final String messageFormat;
    private final TaskMonitor parentMonitor;

    private long currentProgress;
    private long lastDisplayTime;
    private final long startTime;
    private boolean monitoringRequested = false;

    private static final DecimalFormat[] sizeFormat = new DecimalFormat[] {
        new DecimalFormat("#0.00"), //$NON-NLS-1$
        new DecimalFormat("#0.0"), //$NON-NLS-1$
        new DecimalFormat("#0"), //$NON-NLS-1$
    };

    private static final String[] formats = new String[] {
        "{0}", //$NON-NLS-1$
        "{0}KB", //$NON-NLS-1$
        "{0}MB", //$NON-NLS-1$
        "{0}GB", //$NON-NLS-1$
        "{0}TB", //$NON-NLS-1$
        "{0}PB", //$NON-NLS-1$
    };

    public FileProcessingProgressMonitorAdapter(final TaskMonitor parentMonitor, final long totalLength) {
        this(parentMonitor, 0, totalLength, "{0}"); //$NON-NLS-1$
    }

    public FileProcessingProgressMonitorAdapter(
        final TaskMonitor parentMonitor,
        final long totalLength,
        final String messageFormat) {
        this(parentMonitor, 0, totalLength, messageFormat);
    }

    public FileProcessingProgressMonitorAdapter(
        final TaskMonitor parentMonitor,
        final long alreadyProcessed,
        final long totalLength) {
        this(parentMonitor, alreadyProcessed, totalLength, "{0}"); //$NON-NLS-1$
    }

    public FileProcessingProgressMonitorAdapter(
        final TaskMonitor parentMonitor,
        final long alreadyProcessed,
        final long totalLength,
        final String messageFormat) {
        this.parentMonitor = parentMonitor;
        this.alreadyProcessed = alreadyProcessed;
        this.totalLength = totalLength;
        this.messageFormat = messageFormat;

        this.currentProgress = 0;
        this.lastDisplayTime = System.currentTimeMillis();
        this.startTime = System.currentTimeMillis();
        this.monitoringRequested = totalLength > 0;
    }

    @Override
    public void worked(final int processed) {
        worked(processed, false);
    }

    private void worked(final int processed, final boolean forced) {
        if (monitoringRequested) {
            currentProgress += processed;

            final long now = System.currentTimeMillis();
            if (forced || now - lastDisplayTime > MONITORING_TIME_INTERVAL) {
                lastDisplayTime = now;

                final String percent = MessageFormat.format(
                    PERCENT_MESSAGE_FORMAT,
                    size(alreadyProcessed + currentProgress),
                    size(totalLength),
                    (float) (alreadyProcessed + currentProgress) / totalLength);

                synchronized (parentMonitor) {
                    parentMonitor.setCurrentWorkDescription(MessageFormat.format(messageFormat, percent));
                }
            }
        }
    }

    private String size(final long amount) {
        int idx = 0;
        float value = amount;
        final DecimalFormat format;

        while (value > 1024 && idx < formats.length - 1) {
            value /= 1024;
            idx++;
        }

        if (idx > 0 && value < 10) {
            format = sizeFormat[0];
        } else if (idx > 0 && value < 100) {
            format = sizeFormat[1];
        } else {
            format = sizeFormat[2];
        }

        return MessageFormat.format(formats[idx], format.format(value));
    }

    @Override
    public boolean isCanceled() {
        return parentMonitor.isCanceled();
    }

    @Override
    public void begin(final String taskName, final int totalWork) {
        currentProgress = 0;
        lastDisplayTime = System.currentTimeMillis();
    }

    @Override
    public void beginWithUnknownTotalWork(final String taskName) {
        // ignore
    }

    @Override
    public void done() {
        /*
         * If we already have displayed progress for this file at least once,
         * let's show that we've finished its upload for 100%
         */
        worked(0, lastDisplayTime - startTime > MONITORING_TIME_INTERVAL);
    }

    @Override
    public void setCanceled() {
        parentMonitor.setCanceled();

    }

    @Override
    public void setTaskName(final String taskName) {
        // ignore
    }

    @Override
    public void setCurrentWorkDescription(final String description) {
        // ignore
    }

    @Override
    public TaskMonitor newSubTaskMonitor(final int amount) {
        synchronized (parentMonitor) {
            return parentMonitor.newSubTaskMonitor(amount);
        }
    }
}
