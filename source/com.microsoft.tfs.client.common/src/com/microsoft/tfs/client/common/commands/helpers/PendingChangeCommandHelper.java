// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.commands.helpers;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Status;

import com.microsoft.tfs.client.common.Messages;
import com.microsoft.tfs.client.common.TFSCommonClientPlugin;
import com.microsoft.tfs.client.common.repository.TFSRepository;
import com.microsoft.tfs.core.clients.versioncontrol.OperationStatus;
import com.microsoft.tfs.core.clients.versioncontrol.VersionControlClient;
import com.microsoft.tfs.core.clients.versioncontrol.events.NewPendingChangeListener;
import com.microsoft.tfs.core.clients.versioncontrol.events.PendingChangeEvent;
import com.microsoft.tfs.util.Check;

/**
 * This should only be called from within the same thread that contains core
 * origination points.
 *
 * @threadsafety not thread safe.
 */
public class PendingChangeCommandHelper {
    private final VersionControlClient vcClient;
    private final PendingChangeCommandListener listener = new PendingChangeCommandListener();

    private boolean hasConflicts = false;
    private final List pendingChangeList = new ArrayList();

    private final List warningList = new ArrayList();

    public PendingChangeCommandHelper(final TFSRepository repository) {
        Check.notNull(repository, "repository"); //$NON-NLS-1$
        vcClient = repository.getVersionControlClient();
    }

    public PendingChangeCommandHelper(final VersionControlClient vcClient) {
        Check.notNull(vcClient, "vcClient"); //$NON-NLS-1$
        this.vcClient = vcClient;
    }

    public void hookupListener() {
        vcClient.getEventEngine().addNewPendingChangeListener(listener);
    }

    public void unhookListener() {
        vcClient.getEventEngine().removeNewPendingChangeListener(listener);
    }

    public boolean hasConflicts() {
        return hasConflicts;
    }

    public boolean hasWarnings() {
        return warningList.size() > 0;
    }

    public String[] getWarnings() {
        return (String[]) warningList.toArray(new String[warningList.size()]);
    }

    public IStatus getMultiStatus(final int severity, final String description) {
        final MultiStatus status = new MultiStatus(TFSCommonClientPlugin.PLUGIN_ID, 0, description, null);

        for (final Iterator i = warningList.iterator(); i.hasNext();) {
            status.add(new Status(severity, TFSCommonClientPlugin.PLUGIN_ID, 0, (String) i.next(), null));
        }

        return status;
    }

    private class PendingChangeCommandListener implements NewPendingChangeListener {
        @Override
        public void onNewPendingChange(final PendingChangeEvent e) {
            /* Ignore non fatals that did not originate from this command */
            if (!Thread.currentThread().equals(e.getEventSource().getOriginatingThread())) {
                return;
            }

            String warning = null;

            if (e.getOperationStatus() == OperationStatus.CONFLICT
                || e.getOperationStatus() == OperationStatus.TARGET_WRITABLE
                || e.getOperationStatus() == OperationStatus.SOURCE_WRITABLE) {
                hasConflicts = true;
            } else if (e.getOperationStatus() == OperationStatus.TARGET_IS_DIRECTORY) {
                final String target = e.getPendingChange().getLocalItem() != null ? e.getPendingChange().getLocalItem()
                    : e.getPendingChange().getServerItem();

                final String warningFormat = Messages.getString("PendingChangeCommandHelper.WarningFormat"); //$NON-NLS-1$
                warning = MessageFormat.format(warningFormat, target);
            }

            if (warning != null) {
                synchronized (warningList) {
                    warningList.add(warning);
                }
            }

            pendingChangeList.add(e.getPendingChange());
        }
    }
}
