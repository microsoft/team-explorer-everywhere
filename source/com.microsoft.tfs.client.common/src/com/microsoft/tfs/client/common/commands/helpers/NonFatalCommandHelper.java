// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.commands.helpers;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Status;

import com.microsoft.tfs.client.common.Messages;
import com.microsoft.tfs.client.common.TFSCommonClientPlugin;
import com.microsoft.tfs.client.common.repository.TFSRepository;
import com.microsoft.tfs.core.clients.versioncontrol.VersionControlClient;
import com.microsoft.tfs.core.clients.versioncontrol.events.NonFatalErrorEvent;
import com.microsoft.tfs.core.clients.versioncontrol.events.NonFatalErrorListener;
import com.microsoft.tfs.util.Check;

/**
 * This should only be called from within the same thread that contains core
 * origination points.
 *
 * @threadsafety not thread safe.
 */
public class NonFatalCommandHelper {
    private final VersionControlClient vcClient;
    private final NonFatalCommandListener listener = new NonFatalCommandListener();

    private final List<NonFatalErrorEvent> eventList = new ArrayList<NonFatalErrorEvent>();

    public NonFatalCommandHelper(final TFSRepository repository) {
        Check.notNull(repository, "repository"); //$NON-NLS-1$
        vcClient = repository.getVersionControlClient();
    }

    public NonFatalCommandHelper(final VersionControlClient vcClient) {
        Check.notNull(vcClient, "vcClient"); //$NON-NLS-1$
        this.vcClient = vcClient;
    }

    public void hookupListener() {
        vcClient.getEventEngine().addNonFatalErrorListener(listener);
    }

    public void unhookListener() {
        vcClient.getEventEngine().removeNonFatalErrorListener(listener);
    }

    public boolean hasNonFatals() {
        return eventList.size() > 0;
    }

    public NonFatalErrorEvent[] getNonFatalErrors() {
        return eventList.toArray(new NonFatalErrorEvent[eventList.size()]);
    }

    public IStatus getMultiStatus(final int severity, final String description) {
        final IStatus[] statuses = getStatuses(severity);
        return new MultiStatus(TFSCommonClientPlugin.PLUGIN_ID, 0, statuses, description, null);
    }

    /**
     * Gets the best status to use (might be a {@link MultiStatus} to explain
     * the accumulated non-fatal errors. The given format string is used only
     * when multiple errors are present. If only one error happened, then that
     * error's status is returned and the message format string is not used.
     *
     * @param severity
     *        the severity with which to construct the returned status
     * @param errorCount
     *        the count of errors to format the
     *        multipleStatusesDescriptionFormat format string with
     * @param multipleErrorsDescriptionFormat
     *        a format string, {0} is errorCount, to use in the status when
     *        multiple non-fatal errors were encountered (must not be
     *        <code>null</code>)
     * @return
     */
    public IStatus getBestStatus(
        final int severity,
        final int errorCount,
        final String multipleErrorsDescriptionFormat) {
        final IStatus[] statuses = getStatuses(severity);

        if (statuses.length == 0) {
            /* Hopefully never happens, but vague is better than nothing. */
            return new Status(
                severity,
                TFSCommonClientPlugin.PLUGIN_ID,
                0,
                Messages.getString("NonFatalCommandHelper.UnspecifiedErrorMoreInfoInLog"), //$NON-NLS-1$
                null);
        } else if (statuses.length == 1) {
            return statuses[0];
        } else {
            return new MultiStatus(
                TFSCommonClientPlugin.PLUGIN_ID,
                0,
                statuses,
                MessageFormat.format(multipleErrorsDescriptionFormat, errorCount),
                null);
        }
    }

    public IStatus[] getStatuses() {
        return getStatuses(IStatus.WARNING);
    }

    public IStatus[] getStatuses(final int severity) {
        final IStatus[] statusList = new IStatus[eventList.size()];

        for (int i = 0; i < eventList.size(); i++) {
            statusList[i] = getStatusFromNonFatal(eventList.get(i), severity);
        }

        return statusList;
    }

    public static IStatus getStatusFromNonFatal(final NonFatalErrorEvent nonFatal, final int severity) {
        Check.notNull(nonFatal, "nonFatal"); //$NON-NLS-1$

        if (nonFatal.getThrowable() != null) {
            return new Status(severity, TFSCommonClientPlugin.PLUGIN_ID, 0, null, nonFatal.getThrowable());
        } else {
            return new Status(severity, TFSCommonClientPlugin.PLUGIN_ID, 0, nonFatal.getFailure().getMessage(), null);
        }
    }

    private class NonFatalCommandListener implements NonFatalErrorListener {
        @Override
        public void onNonFatalError(final NonFatalErrorEvent e) {
            /* Ignore non fatals that did not originate from this command */
            if (!Thread.currentThread().equals(e.getEventSource().getOriginatingThread())) {
                return;
            }

            /* non fatals can have either a throwable or a failure object */
            eventList.add(e);
        }
    }
}
