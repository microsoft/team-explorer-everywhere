// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.commands.helpers;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Status;

import com.microsoft.tfs.client.common.TFSCommonClientPlugin;
import com.microsoft.tfs.core.clients.versioncontrol.events.NonFatalErrorEvent;
import com.microsoft.tfs.core.clients.versioncontrol.events.NonFatalErrorListener;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Workspace;
import com.microsoft.tfs.util.Check;

/*
 * NonFatalErrorHelper is a simple listener for NonFatalErrors produced by core.
 * It will listen for non-fatals occurring on the calling thread (whichever
 * thread instantiates this) and provides several helpful methods for retrieving
 * them.
 *
 * Clients MUST call destroy() when complete. It is safe to call destroy()
 * multiple times.
 *
 * Note: at some point, it may be beneficial to allow this to listen on multiple
 * workspaces (for example: Plugin may become multi-AWorkspace aware.) This is
 * not covered, but it should be relatively easy to make this happen.
 */
public class NonFatalErrorHelper {
    /*
     * The workspace we're listening on (strictly speaking, the VCClient)
     */
    private final Workspace workspace;

    /*
     * List of non-fatals received
     */
    private final List nonFatalList = new ArrayList();

    /* Remember calling thread away to ensure we get the right events */
    private final Thread originatingThread = Thread.currentThread();

    /*
     * Lock for listener configuration
     */
    private final Object listenerLock = new Object();

    /*
     * Our NFE listener
     */
    private NonFatalErrorHelperListener listener = new NonFatalErrorHelperListener();

    public NonFatalErrorHelper(final Workspace workspace) {
        Check.notNull(workspace, "workspace"); //$NON-NLS-1$

        this.workspace = workspace;

        workspace.getClient().getEventEngine().addNonFatalErrorListener(listener);
    }

    public boolean hasNonFatalErrors() {
        return (getNonFatalMessageCount() > 0);
    }

    public int getNonFatalMessageCount() {
        synchronized (nonFatalList) {
            return nonFatalList.size();
        }
    }

    public String[] getNonFatalErrorMessages() {
        synchronized (nonFatalList) {
            return (String[]) nonFatalList.toArray(new String[nonFatalList.size()]);
        }
    }

    public IStatus[] getNonFatalStatus() {
        return getNonFatalStatus(IStatus.WARNING);
    }

    public IStatus[] getNonFatalStatus(final int severity) {
        synchronized (nonFatalList) {
            final IStatus[] status = new IStatus[nonFatalList.size()];

            for (int i = 0; i < nonFatalList.size(); i++) {
                status[i] = new Status(severity, getPluginID(), 0, nonFatalList.get(i).toString(), null);
            }

            return status;
        }
    }

    public MultiStatus getNonFatalMultiStatus(final String message) {
        return getNonFatalMultiStatus(IStatus.WARNING, message);
    }

    public MultiStatus getNonFatalMultiStatus(final int severity, final String message) {
        return new MultiStatus(getPluginID(), 0, getNonFatalStatus(severity), message, null);
    }

    /*
     * TODO: this should come from a proper source. See also
     * CommandExceptionHandler.getPluginIdForCommand()
     */
    private String getPluginID() {
        return TFSCommonClientPlugin.PLUGIN_ID;
    }

    public void destroy() {
        synchronized (listenerLock) {
            if (listener == null) {
                return;
            }

            workspace.getClient().getEventEngine().removeNonFatalErrorListener(listener);
            listener = null;
        }
    }

    private class NonFatalErrorHelperListener implements NonFatalErrorListener {
        @Override
        public void onNonFatalError(final NonFatalErrorEvent e) {
            /* Only pay attention to non-fatals which occur on our thread */
            if (!originatingThread.equals(Thread.currentThread())) {
                return;
            }

            synchronized (nonFatalList) {
                nonFatalList.add(e.getMessage());
            }
        }
    }
}
