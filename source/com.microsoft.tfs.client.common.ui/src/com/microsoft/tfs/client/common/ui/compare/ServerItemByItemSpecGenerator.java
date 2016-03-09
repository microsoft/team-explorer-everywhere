// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.compare;

import java.lang.reflect.InvocationTargetException;
import java.text.MessageFormat;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;

import com.microsoft.tfs.client.common.commands.vc.QueryItemsCommand;
import com.microsoft.tfs.client.common.framework.command.Command;
import com.microsoft.tfs.client.common.framework.command.ThreadedCancellableCommand;
import com.microsoft.tfs.client.common.repository.TFSRepository;
import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.framework.compare.DifferencerInputGenerator;
import com.microsoft.tfs.client.common.ui.vc.ItemSetHandler;
import com.microsoft.tfs.client.common.ui.vc.ItemSetHandler.ItemSetVisitor;
import com.microsoft.tfs.client.common.vc.VersionSpecHelper;
import com.microsoft.tfs.core.clients.versioncontrol.GetItemsOptions;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.DeletedState;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Item;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.ItemType;
import com.microsoft.tfs.core.clients.versioncontrol.specs.ItemSpec;
import com.microsoft.tfs.core.clients.versioncontrol.specs.version.VersionSpec;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.tasks.CanceledException;

public class ServerItemByItemSpecGenerator implements DifferencerInputGenerator {
    private final TFSRepository repository;
    private final ItemSpec itemSpec;
    private final VersionSpec versionSpec;

    public ServerItemByItemSpecGenerator(
        final TFSRepository repository,
        final ItemSpec itemSpec,
        final VersionSpec versionSpec) {
        Check.notNull(repository, "repository"); //$NON-NLS-1$
        Check.notNull(itemSpec, "itemSpec"); //$NON-NLS-1$
        Check.notNull(versionSpec, "versionSpec"); //$NON-NLS-1$

        this.repository = repository;
        this.itemSpec = itemSpec;
        this.versionSpec = versionSpec;
    }

    @Override
    public String getLoggingDescription() {
        return MessageFormat.format(
            "{0} (version {1})", //$NON-NLS-1$
            itemSpec.getItem(),
            VersionSpecHelper.getVersionSpecDescriptionNOLOC(versionSpec));
    }

    @Override
    public Object getInput(final IProgressMonitor inMonitor) throws InvocationTargetException, InterruptedException {
        final IProgressMonitor monitor = inMonitor == null ? new NullProgressMonitor() : inMonitor;

        if (monitor.isCanceled()) {
            throw new InterruptedException();
        }

        try {
            final String messageFormat =
                Messages.getString("ServerItemByItemSpecGenerator.ProgressQueryingServerFormat"); //$NON-NLS-1$
            final String message = MessageFormat.format(messageFormat, itemSpec.getItem());
            monitor.beginTask(message, 2);

            return getSpecificInput(monitor);
        } finally {
            monitor.done();
        }
    }

    private TFSItemNode getSpecificInput(final IProgressMonitor monitor)
        throws InvocationTargetException,
            InterruptedException {
        final QueryItemsCommand command = new QueryItemsCommand(repository, new ItemSpec[] {
            itemSpec
        }, versionSpec, DeletedState.NON_DELETED, ItemType.ANY, GetItemsOptions.DOWNLOAD);

        runCommand(command, monitor);

        /*
         * ItemSets returned can be null if we queried for an invalid
         * versionspec.
         */
        if (command.getItemSets() == null) {
            final String messageFormat = Messages.getString("ServerItemByItemSpecGenerator.NoServerItemFormat"); //$NON-NLS-1$
            final String message = MessageFormat.format(
                messageFormat,
                itemSpec.getItem(),
                VersionSpecHelper.getVersionSpecDescription(versionSpec));
            throw new RuntimeException(message);
        }

        final ItemSetVisitor visitor = new ItemSetVisitor() {
            @Override
            public Object visit(final Item item, final Object parent) throws CanceledException {
                if (monitor.isCanceled()) {
                    throw new CanceledException();
                }

                /*
                 * These items represent pending adds in your workspace. We
                 * don't want to treat them as server items, so we exclude them
                 * here.
                 */
                if (item.getChangeSetID() == 0) {
                    return null;
                }

                final TFSItemNode node = new TFSItemNode(item, repository.getVersionControlClient());

                if (parent != null) {
                    ((TFSItemNode) parent).addChild(node);
                }

                return node;
            }
        };

        try {
            return (TFSItemNode) ItemSetHandler.handleItemSet(command.getItemSets()[0], visitor);
        } catch (final CanceledException e) {
            throw new InterruptedException();
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
            final String messageFormat = Messages.getString("ServerItemByItemSpecGenerator.UnknownFailureFormat"); //$NON-NLS-1$
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

    @Override
    public boolean equals(final Object o) {
        if (o == null) {
            return false;
        }
        if (o == this) {
            return true;
        }
        if (!(o instanceof ServerItemByItemSpecGenerator)) {
            return false;
        }

        final ServerItemByItemSpecGenerator other = (ServerItemByItemSpecGenerator) o;
        if (this.repository.equals(other.repository)
            && this.itemSpec.equals(other.itemSpec)
            && this.versionSpec.equals(other.versionSpec)) {
            return true;
        }
        return false;
    }
}