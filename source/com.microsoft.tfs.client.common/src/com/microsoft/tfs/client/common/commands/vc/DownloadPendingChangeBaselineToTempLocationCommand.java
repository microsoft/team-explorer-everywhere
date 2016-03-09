// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.commands.vc;

import java.io.File;
import java.text.MessageFormat;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import com.microsoft.tfs.client.common.Messages;
import com.microsoft.tfs.client.common.commands.TFSConnectedCommand;
import com.microsoft.tfs.client.common.repository.TFSRepository;
import com.microsoft.tfs.core.clients.versioncontrol.path.LocalPath;
import com.microsoft.tfs.core.clients.versioncontrol.path.ServerPath;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.PendingChange;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.LocaleUtil;

/**
 * This is currently an "online" command ({@link TFSConnectedCommand}) because
 * we don't know if the core will be able to fetch a cached baseline file vs.
 * hitting the server. In the future it would be nice to not force a connection
 * (if offline) to run this command.
 */
public class DownloadPendingChangeBaselineToTempLocationCommand extends TFSConnectedCommand {
    private final TFSRepository repository;
    private final PendingChange change;
    private final String localFilename;

    private volatile File tempFile;

    public DownloadPendingChangeBaselineToTempLocationCommand(
        final TFSRepository repository,
        final PendingChange change) {
        this(repository, change, computeLocalFilename(change));
    }

    public DownloadPendingChangeBaselineToTempLocationCommand(
        final TFSRepository repository,
        final PendingChange change,
        final String localFilename) {
        Check.notNull(repository, "repository"); //$NON-NLS-1$
        Check.notNull(change, "change"); //$NON-NLS-1$
        Check.notNullOrEmpty(localFilename, "localFilename"); //$NON-NLS-1$

        this.repository = repository;
        this.change = change;
        this.localFilename = localFilename;

        setConnection(repository.getConnection());
    }

    private static String computeLocalFilename(final PendingChange change) {
        final String localItem = change.getLocalItem();

        if (localItem != null && localItem.length() > 0) {
            return LocalPath.getFileName(localItem);
        }

        return ServerPath.getFileName(change.getServerItem());
    }

    @Override
    public String getName() {
        return MessageFormat.format(
            Messages.getString("DownloadPendingChangeBaselineToTempLocationCommand.CommandTextFormat"), //$NON-NLS-1$
            localFilename);
    }

    @Override
    public String getErrorDescription() {
        return MessageFormat.format(
            Messages.getString("DownloadPendingChangeBaselineToTempLocationCommand.ErrorTextFormat"), //$NON-NLS-1$
            localFilename);
    }

    @Override
    public String getLoggingDescription() {
        return MessageFormat.format(
            Messages.getString("DownloadPendingChangeBaselineToTempLocationCommand.CommandTextFormat", LocaleUtil.ROOT), //$NON-NLS-1$
            localFilename);
    }

    @Override
    protected IStatus doRun(final IProgressMonitor progressMonitor) throws Exception {
        tempFile = change.downloadBaseFileToTempLocation(repository.getVersionControlClient(), localFilename);

        return Status.OK_STATUS;
    }

    public File getTempFile() {
        return tempFile;
    }
}
