// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.console;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.microsoft.tfs.client.common.server.ServerManager;
import com.microsoft.tfs.client.common.server.ServerManagerAdapter;
import com.microsoft.tfs.client.common.server.ServerManagerEvent;
import com.microsoft.tfs.client.common.server.ServerManagerListener;
import com.microsoft.tfs.client.common.server.TFSServer;
import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.console.Message.MessageType;
import com.microsoft.tfs.core.clients.versioncontrol.ProcessType;
import com.microsoft.tfs.core.clients.versioncontrol.conflicts.resolutions.ConflictResolutionHelper;
import com.microsoft.tfs.core.clients.versioncontrol.events.BeforeShelveListener;
import com.microsoft.tfs.core.clients.versioncontrol.events.CheckinEvent;
import com.microsoft.tfs.core.clients.versioncontrol.events.CheckinListener;
import com.microsoft.tfs.core.clients.versioncontrol.events.ConflictEvent;
import com.microsoft.tfs.core.clients.versioncontrol.events.ConflictListener;
import com.microsoft.tfs.core.clients.versioncontrol.events.ConflictResolvedEvent;
import com.microsoft.tfs.core.clients.versioncontrol.events.ConflictResolvedListener;
import com.microsoft.tfs.core.clients.versioncontrol.events.GetEvent;
import com.microsoft.tfs.core.clients.versioncontrol.events.GetListener;
import com.microsoft.tfs.core.clients.versioncontrol.events.NonFatalErrorEvent;
import com.microsoft.tfs.core.clients.versioncontrol.events.NonFatalErrorListener;
import com.microsoft.tfs.core.clients.versioncontrol.events.OperationCompletedEvent;
import com.microsoft.tfs.core.clients.versioncontrol.events.OperationCompletedListener;
import com.microsoft.tfs.core.clients.versioncontrol.events.OperationStartedEvent;
import com.microsoft.tfs.core.clients.versioncontrol.events.OperationStartedListener;
import com.microsoft.tfs.core.clients.versioncontrol.events.PendingChangeEvent;
import com.microsoft.tfs.core.clients.versioncontrol.events.VersionControlEventEngine;

public class ConsoleCoreEventListener {
    private static final Log log = LogFactory.getLog(ConsoleCoreEventListener.class);

    /*
     * To see all of the places we should be writing to the console, search for
     * usages of:
     * Microsoft.VisualStudio.TeamFoundation.VersionControl.VsOutputPane
     * .WriteLine
     */
    private final TFSConsole console;
    private final ServerManagerListener serverManagerListener;
    private final CoreListener coreListener;
    private final Set<ServerManager> attachedServerManagers = new HashSet<ServerManager>();

    private final ConcurrentMap<Long, List<OperationStartedEvent>> operationsByThread =
        new ConcurrentHashMap<Long, List<OperationStartedEvent>>();

    private final Object lock = new Object();

    public ConsoleCoreEventListener(final TFSConsole console) {
        this.console = console;
        serverManagerListener = new ConsoleCoreServerManagerListener();
        coreListener = new CoreListener();
    }

    public void attach(final ServerManager serverManager) {
        synchronized (lock) {
            serverManager.addListener(serverManagerListener);

            final TFSServer defaultServer = serverManager.getDefaultServer();

            if (defaultServer != null) {
                attachToCoreEvents(defaultServer);
            }

            attachedServerManagers.add(serverManager);
        }
    }

    public void detach(final ServerManager serverManager) {
        synchronized (lock) {
            serverManager.removeListener(serverManagerListener);

            final TFSServer defaultServer = serverManager.getDefaultServer();

            if (defaultServer != null) {
                detachFromCoreEvents(defaultServer);
            }

            attachedServerManagers.remove(serverManager);
        }
    }

    public void detachAll() {
        synchronized (lock) {
            for (final Iterator<ServerManager> iterator = attachedServerManagers.iterator(); iterator.hasNext();) {
                final ServerManager serverManager = iterator.next();

                final TFSServer defaultServer = serverManager.getDefaultServer();

                if (defaultServer != null) {
                    detachFromCoreEvents(defaultServer);
                }
            }

            attachedServerManagers.clear();
        }
    }

    private void attachToCoreEvents(final TFSServer server) {
        synchronized (lock) {
            final VersionControlEventEngine eventEngine =
                server.getConnection().getVersionControlClient().getEventEngine();

            eventEngine.addOperationStartedListener(coreListener);
            eventEngine.addOperationCompletedListener(coreListener);
            eventEngine.addGetListener(coreListener);
            eventEngine.addNonFatalErrorListener(coreListener);
            eventEngine.addCheckinListener(coreListener);
            eventEngine.addBeforeShelveListener(coreListener);
            eventEngine.addConflictListener(coreListener);
            eventEngine.addConflictResolvedListener(coreListener);
        }
    }

    private void detachFromCoreEvents(final TFSServer server) {
        synchronized (lock) {
            final VersionControlEventEngine eventEngine =
                server.getConnection().getVersionControlClient().getEventEngine();

            eventEngine.removeOperationStartedListener(coreListener);
            eventEngine.removeOperationCompletedListener(coreListener);
            eventEngine.removeGetListener(coreListener);
            eventEngine.removeNonFatalErrorListener(coreListener);
            eventEngine.removeCheckinListener(coreListener);
            eventEngine.removeBeforeShelveListener(coreListener);
            eventEngine.removeConflictListener(coreListener);
            eventEngine.removeConflictResolvedListener(coreListener);

            operationsByThread.clear();
        }
    }

    private void onOperationStarted(final OperationStartedEvent e) {
        final long threadId = Thread.currentThread().getId();
        List<OperationStartedEvent> operationsForThread = operationsByThread.get(threadId);

        if (operationsForThread == null) {
            operationsForThread = new ArrayList<OperationStartedEvent>();
            operationsByThread.put(threadId, operationsForThread);
        }

        operationsForThread.add(e);
    }

    private void onOperationCompleted(final OperationCompletedEvent e) {
        final long threadId = Thread.currentThread().getId();
        final List<OperationStartedEvent> operationsForThread = operationsByThread.get(threadId);

        if (operationsForThread == null) {
            log.warn(
                MessageFormat.format(
                    "Unbalanced operation start/completed events - ignoring operation completed for thread {0}", //$NON-NLS-1$
                    Long.toString(threadId)));

            return;
        }

        if (operationsForThread.size() > 0) {
            operationsForThread.remove(operationsForThread.size() - 1);
        } else {
            log.warn(
                MessageFormat.format(
                    "Unbalanced operation start/completed events - ignoring operation completed for thread {0}", //$NON-NLS-1$
                    Long.toString(threadId)));
        }

        if (operationsForThread.size() == 0) {
            operationsByThread.remove(threadId);
        }
    }

    private void onGet(final GetEvent e) {
        e.getStatus();

        final AtomicReference<String> errorHolder = new AtomicReference<String>();
        final String messageString = e.getMessage(null, errorHolder);

        Message message;
        if (errorHolder.get() != null) {
            message = new Message(MessageType.ERROR, errorHolder.get());
        } else {
            message = new Message(MessageType.INFO, messageString);
        }

        printMessageToConsole(message);
    }

    private void onNonFatalError(final NonFatalErrorEvent e) {
        final Message message = NonFatalErrorEventFormatter.getMessage(e);

        printMessageToConsole(message);
    }

    private void onCheckin(final CheckinEvent e) {
        Message message;

        if (e.getChangesetID() != 0) {
            message = new Message(
                MessageType.INFO,
                MessageFormat.format(
                    Messages.getString("ConsoleCoreEventListener.ChagesetCheckedInFormat"), //$NON-NLS-1$
                    Integer.toString(e.getChangesetID())));
        } else {
            message = new Message(MessageType.INFO, Messages.getString("ConsoleCoreEventListener.NoChangesToCheckin")); //$NON-NLS-1$
        }

        printMessageToConsole(message);
    }

    private void onBeforeShelve(final PendingChangeEvent e) {
        final Message message = new Message(
            MessageType.INFO,
            MessageFormat.format(
                Messages.getString("ConsoleCoreEventListener.ShelvingChangesFormat"), //$NON-NLS-1$
                e.getPendingChange().getServerItem()));

        printMessageToConsole(message);
    }

    private void onConflict(final ConflictEvent e) {
        final Message message = new Message(MessageType.INFO, e.getMessage());
        printMessageToConsole(message);
    }

    private void onConflictResolved(final ConflictResolvedEvent e) {
        /*
         * We only care to report conflicts that were *automatically* resolved.
         */
        final long threadId = Thread.currentThread().getId();
        final List<OperationStartedEvent> operationsForThread = operationsByThread.get(threadId);
        boolean autoResolving = false;

        if (operationsForThread == null) {
            return;
        }

        for (final OperationStartedEvent operation : operationsForThread) {
            if (operation.getProcessType() != ProcessType.NONE) {
                autoResolving = true;
                break;
            }
        }

        if (!autoResolving) {
            return;
        }

        final Message message = new Message(
            MessageType.INFO,
            MessageFormat.format(
                Messages.getString("ConsoleCoreEventListener.AutoResolvedConflictFormat"), //$NON-NLS-1$
                e.getConflict().getDetailedMessage(false),
                ConflictResolutionHelper.getResolutionString(e.getConflict().getResolution())));

        printMessageToConsole(message);
    }

    private void printMessageToConsole(final Message message) {
        if (MessageType.ERROR == message.getType()) {
            console.printErrorMessage(message.getText());
        } else if (MessageType.WARNING == message.getType()) {
            console.printWarning(message.getText());
        } else {
            console.printMessage(message.getText());
        }
    }

    private class ConsoleCoreServerManagerListener extends ServerManagerAdapter {
        @Override
        public void onServerAdded(final ServerManagerEvent event) {
            attachToCoreEvents(event.getServer());
        }

        @Override
        public void onServerRemoved(final ServerManagerEvent event) {
            detachFromCoreEvents(event.getServer());
        }
    }

    private class CoreListener
        implements OperationStartedListener, OperationCompletedListener, GetListener, NonFatalErrorListener,
        CheckinListener, BeforeShelveListener, ConflictListener, ConflictResolvedListener {
        @Override
        public void onOperationStarted(final OperationStartedEvent e) {
            ConsoleCoreEventListener.this.onOperationStarted(e);
        }

        @Override
        public void onOperationCompleted(final OperationCompletedEvent e) {
            ConsoleCoreEventListener.this.onOperationCompleted(e);
        }

        @Override
        public void onGet(final GetEvent e) {
            ConsoleCoreEventListener.this.onGet(e);
        }

        @Override
        public void onNonFatalError(final NonFatalErrorEvent e) {
            ConsoleCoreEventListener.this.onNonFatalError(e);
        }

        @Override
        public void onCheckin(final CheckinEvent e) {
            ConsoleCoreEventListener.this.onCheckin(e);
        }

        @Override
        public void onBeforeShelve(final PendingChangeEvent e) {
            ConsoleCoreEventListener.this.onBeforeShelve(e);
        }

        @Override
        public void onConflict(final ConflictEvent e) {
            ConsoleCoreEventListener.this.onConflict(e);
        }

        @Override
        public void onConflictResolved(final ConflictResolvedEvent e) {
            ConsoleCoreEventListener.this.onConflictResolved(e);
        }
    }
}
