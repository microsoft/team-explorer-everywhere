// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.compare;

import java.lang.reflect.InvocationTargetException;
import java.text.MessageFormat;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;

import com.microsoft.tfs.client.common.commands.vc.QueryItemAtVersionCommand;
import com.microsoft.tfs.client.common.framework.command.Command;
import com.microsoft.tfs.client.common.framework.command.ThreadedCancellableCommand;
import com.microsoft.tfs.client.common.repository.TFSRepository;
import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.framework.compare.DifferencerInputGenerator;
import com.microsoft.tfs.client.common.vc.VersionSpecHelper;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.RecursionType;
import com.microsoft.tfs.core.clients.versioncontrol.specs.ItemSpec;
import com.microsoft.tfs.core.clients.versioncontrol.specs.version.VersionSpec;
import com.microsoft.tfs.util.Check;

public class ServerItemByItemVersionGenerator implements DifferencerInputGenerator {
    private final TFSRepository repository;
    private final ItemSpec itemSpec;
    private final VersionSpec itemVersion;
    private final VersionSpec requestedVersion;

    public ServerItemByItemVersionGenerator(
        final TFSRepository repository,
        final ItemSpec itemSpec,
        final VersionSpec itemVersion,
        final VersionSpec requestedVersion) {
        Check.notNull(repository, "repository"); //$NON-NLS-1$
        Check.notNull(itemSpec, "itemSpec"); //$NON-NLS-1$
        Check.notNull(itemVersion, "itemVersion"); //$NON-NLS-1$
        Check.notNull(requestedVersion, "requestedVersion"); //$NON-NLS-1$

        this.repository = repository;
        this.itemSpec = itemSpec;
        this.itemVersion = itemVersion;
        this.requestedVersion = requestedVersion;
    }

    public ServerItemByItemVersionGenerator(
        final TFSRepository repository,
        final String itemPath,
        final VersionSpec itemVersion,
        final VersionSpec requestedVersion) {
        this(repository, new ItemSpec(itemPath, RecursionType.NONE), itemVersion, requestedVersion);
    }

    @Override
    public String getLoggingDescription() {
        return MessageFormat.format(
            "{0} (version {1})", //$NON-NLS-1$
            itemSpec.getItem(),
            VersionSpecHelper.getVersionSpecDescriptionNOLOC(requestedVersion));
    }

    @Override
    public Object getInput(final IProgressMonitor inMonitor) throws InvocationTargetException, InterruptedException {
        final IProgressMonitor monitor = inMonitor == null ? new NullProgressMonitor() : inMonitor;
        monitor.beginTask(MessageFormat.format(
            Messages.getString("ServerItemByItemVersionGenerator.TaskNameFormat"), //$NON-NLS-1$
            itemSpec.getItem()), 2);

        if (monitor.isCanceled()) {
            throw new InterruptedException();
        }

        try {
            /*
             * If we wanted a recursive query, directly hand over to
             * ServerItemByItemSpecGenerator, since it knows how to handle
             * recursion.
             */
            if (!itemSpec.getRecursionType().equals(RecursionType.NONE)) {
                final ServerItemByItemSpecGenerator itemSpecGenerator =
                    new ServerItemByItemSpecGenerator(repository, itemSpec, requestedVersion);
                return itemSpecGenerator.getInput(new SubProgressMonitor(monitor, 1));
            }

            String messageFormat = Messages.getString("ServerItemByItemVersionGenerator.ProgressQueryingServerFormat"); //$NON-NLS-1$
            String message = MessageFormat.format(messageFormat, itemSpec.getItem());
            monitor.beginTask(message, 2);

            /*
             * Never do a full recursive QueryItemAtVersionCommand: this command
             * uses QueryHistory to determine the current server path of the
             * given local path. QueryHistory in recursive mode will return the
             * history beneath the given folder, while we want the history for
             * the actual folder itself.
             */
            final ItemSpec queryItemSpec = new ItemSpec(itemSpec.getItem(), RecursionType.NONE);

            final QueryItemAtVersionCommand command =
                new QueryItemAtVersionCommand(repository, queryItemSpec, itemVersion, requestedVersion);

            runCommand(command, new SubProgressMonitor(monitor, 1));

            /* Sanity check to prevent NPE */
            if (command.getItem() == null) {
                messageFormat = Messages.getString("ServerItemByItemVersionGenerator.NoServerItemFormat"); //$NON-NLS-1$
                message = MessageFormat.format(
                    messageFormat,
                    itemSpec.getItem(),
                    VersionSpecHelper.getVersionSpecDescription(requestedVersion));

                throw new RuntimeException(message);
            }

            /*
             * If our query is just for this item, we can simply return a
             * TFSItemNode based on it.
             */
            return new TFSItemNode(command.getItem(), repository.getVersionControlClient());

        } finally {
            monitor.done();
        }
    }

    private void runCommand(final Command command, final IProgressMonitor monitor)
        throws InvocationTargetException,
            InterruptedException {
        final ThreadedCancellableCommand cancelableCommand = new ThreadedCancellableCommand(command);
        IStatus status;
        try {
            status = cancelableCommand.run(monitor);
        } catch (final Exception e) {
            throw new InvocationTargetException(e);
        }

        if (status == null) {
            final String messageFormat = Messages.getString("ServerItemByItemVersionGenerator.UnknownErrorFormat"); //$NON-NLS-1$
            final String message = MessageFormat.format(messageFormat, command.getName());
            throw new RuntimeException(message);
        } else if (status.getSeverity() == IStatus.CANCEL) {
            throw new InterruptedException();
        } else if (status.getSeverity() == IStatus.ERROR) {
            throw new RuntimeException(status.getMessage());
        }

        monitor.worked(1);
        if (monitor.isCanceled()) {
            throw new InterruptedException();
        }
    }
}
