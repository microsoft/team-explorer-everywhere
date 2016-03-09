// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.controls.vc.history;

import java.text.MessageFormat;
import java.util.Iterator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IPersistableElement;

import com.microsoft.tfs.client.common.commands.vc.QueryHistoryIteratorCommand;
import com.microsoft.tfs.client.common.framework.command.ICommandExecutor;
import com.microsoft.tfs.client.common.repository.TFSRepository;
import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.framework.command.UICommandExecutorFactory;
import com.microsoft.tfs.core.clients.versioncontrol.exceptions.ServerPathFormatException;
import com.microsoft.tfs.core.clients.versioncontrol.path.ItemPath;
import com.microsoft.tfs.core.clients.versioncontrol.path.LocalPath;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Changeset;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.RecursionType;
import com.microsoft.tfs.core.clients.versioncontrol.specs.version.VersionSpec;
import com.microsoft.tfs.util.Check;

/**
 * {@link HistoryInput} is an input to a {@link HistoryCachedTableControl}.
 */
public final class HistoryInput implements IEditorInput {
    private static final Log log = LogFactory.getLog(HistoryInput.class);

    public static final class Builder {
        private final Shell shell;
        private final TFSRepository repository;
        private final String historyItem;
        private final VersionSpec historyItemVersion;
        private int historyItemDeletionId;
        private final RecursionType historyItemRecursionType;
        private String userFilter;
        private VersionSpec versionFrom;
        private VersionSpec versionTo;
        private int maxCount = Integer.MAX_VALUE;
        private boolean slotMode = false;
        private boolean generateDownloadUrls = false;
        private boolean sortAscending = false;

        public Builder(
            final Shell shell,
            final TFSRepository repository,
            final String historyItem,
            final VersionSpec historyItemVersion,
            final RecursionType historyItemRecursionType) {
            Check.notNull(shell, "shell"); //$NON-NLS-1$
            Check.notNull(repository, "repository"); //$NON-NLS-1$
            Check.notNull(historyItem, "historyItem"); //$NON-NLS-1$
            Check.notNull(historyItemVersion, "historyItemVersion"); //$NON-NLS-1$
            Check.notNull(historyItemRecursionType, "historyItemRecursionType"); //$NON-NLS-1$

            this.shell = shell;
            this.repository = repository;
            this.historyItem = historyItem;
            this.historyItemVersion = historyItemVersion;
            this.historyItemRecursionType = historyItemRecursionType;
        }

        public Builder setDeletionID(final int historyItemDeletionId) {
            this.historyItemDeletionId = historyItemDeletionId;
            return this;
        }

        public Builder setUserFilter(final String userFilter) {
            this.userFilter = userFilter;
            return this;
        }

        public Builder setVersionFrom(final VersionSpec versionFrom) {
            this.versionFrom = versionFrom;
            return this;
        }

        public Builder setVersionTo(final VersionSpec versionTo) {
            this.versionTo = versionTo;
            return this;
        }

        public Builder setMaxCount(final int maxCount) {
            this.maxCount = maxCount;
            return this;
        }

        public Builder setSlotMode(final boolean slotMode) {
            this.slotMode = slotMode;
            return this;
        }

        public Builder setGenerateDownloadURLs(final boolean generateDownloadUrls) {
            this.generateDownloadUrls = generateDownloadUrls;
            return this;
        }

        public Builder setSortAscending(final boolean sortAscending) {
            this.sortAscending = sortAscending;
            return this;
        }

        public HistoryInput build() {
            return new HistoryInput(
                shell,
                repository,
                historyItem,
                historyItemVersion,
                historyItemDeletionId,
                historyItemRecursionType,
                userFilter,
                versionFrom,
                versionTo,
                maxCount,
                slotMode,
                generateDownloadUrls,
                sortAscending);
        }
    }

    private final Shell shell;
    private final TFSRepository repository;
    private final String historyItem;
    private final VersionSpec historyItemVersion;
    private final int historyItemDeletionId;
    private final RecursionType historyItemRecursionType;
    private final String userFilter;
    private final VersionSpec versionFrom;
    private final VersionSpec versionTo;
    private final int maxCount;
    private final boolean slotMode;
    private final boolean generateDownloadUrls;
    private final boolean sortAscending;

    public HistoryInput(
        final Shell shell,
        final TFSRepository repository,
        final String historyItem,
        final VersionSpec historyItemVersion,
        final int historyItemDeletionId,
        final RecursionType historyItemRecursionType,
        final String userFilter,
        final VersionSpec versionFrom,
        final VersionSpec versionTo,
        final int maxCount,
        final boolean slotMode,
        final boolean generateDownloadUrls,
        final boolean sortAscending) {
        Check.notNull(shell, "shell"); //$NON-NLS-1$
        Check.notNull(repository, "repository"); //$NON-NLS-1$
        Check.notNull(historyItem, "historyItem"); //$NON-NLS-1$
        Check.notNull(historyItemVersion, "historyItemVersion"); //$NON-NLS-1$
        Check.notNull(historyItemRecursionType, "historyItemRecursionType"); //$NON-NLS-1$

        this.shell = shell;
        this.repository = repository;
        this.historyItem = historyItem;
        this.historyItemVersion = historyItemVersion;
        this.historyItemDeletionId = historyItemDeletionId;
        this.historyItemRecursionType = historyItemRecursionType;
        this.userFilter = userFilter;
        this.versionFrom = versionFrom;
        this.versionTo = versionTo;
        this.maxCount = maxCount;
        this.slotMode = slotMode;
        this.generateDownloadUrls = generateDownloadUrls;
        this.sortAscending = sortAscending;
    }

    @Override
    public boolean equals(final Object otherObject) {
        if (!(otherObject instanceof HistoryInput)) {
            return false;
        }
        final HistoryInput otherHistory = (HistoryInput) otherObject;
        return ItemPath.equals(this.historyItem, otherHistory.getHistoryItem());
    }

    @Override
    public int hashCode() {
        return ItemPath.hashcode(this.historyItem);
    }

    public TFSRepository getRepository() {
        return repository;
    }

    public String getHistoryItem() {
        return historyItem;
    }

    public boolean isSlotMode() {
        return slotMode;
    }

    public boolean isSingleItem() {
        /*
         * see:Microsoft.TeamFoundation.VersionControl.Controls.ControlHistory.
         * IsSingleItem : Boolean
         */

        if (historyItemRecursionType != RecursionType.NONE) {
            return false;
        }

        if (historyItem.indexOf('*') != -1 || historyItem.indexOf('?') != -1) {
            return false;
        }

        return true;
    }

    public Iterator<Changeset> queryHistory() {
        try {
            log.info(MessageFormat.format("Querying history for {0}", historyItem)); //$NON-NLS-1$

            final ICommandExecutor executor = UICommandExecutorFactory.newUICommandExecutor(shell);

            final QueryHistoryIteratorCommand command = new QueryHistoryIteratorCommand(
                repository,
                historyItem,
                historyItemVersion,
                historyItemDeletionId,
                historyItemRecursionType,
                userFilter,
                versionFrom,
                versionTo,
                maxCount,
                isSingleItem(),
                slotMode,
                generateDownloadUrls,
                sortAscending);

            executor.execute(command);

            // The command's iterator is always non-null (empty on error)
            return command.getIterator();
        } catch (final ServerPathFormatException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object getAdapter(final Class adapter) {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean exists() {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ImageDescriptor getImageDescriptor() {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return LocalPath.getFileName(historyItem);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IPersistableElement getPersistable() {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getToolTipText() {
        return MessageFormat.format(
            Messages.getString("HistoryInput.ToolTipFormat"), //$NON-NLS-1$
            LocalPath.getFileName(historyItem));
    }
}
