// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.commands.vc;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import com.microsoft.tfs.client.common.Messages;
import com.microsoft.tfs.client.common.commands.TFSConnectedCommand;
import com.microsoft.tfs.client.common.repository.TFSRepository;
import com.microsoft.tfs.core.clients.versioncontrol.specs.DownloadSpec;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.GUID;
import com.microsoft.tfs.util.LocaleUtil;
import com.microsoft.tfs.util.tasks.CanceledException;

public abstract class AbstractGetToTempLocationCommand extends TFSConnectedCommand {
    private final TFSRepository repository;
    private String localFilename;

    private String tempLocation;

    protected AbstractGetToTempLocationCommand(final TFSRepository repository) {
        this(repository, GUID.newGUID().getGUIDString());
    }

    protected AbstractGetToTempLocationCommand(final TFSRepository repository, final String localFilename) {
        Check.notNull(repository, "repository"); //$NON-NLS-1$
        Check.notNull(localFilename, "localFilename"); //$NON-NLS-1$

        this.repository = repository;
        this.localFilename = localFilename;

        setConnection(repository.getConnection());
    }

    public void setLocalFilename(final String localFilename) {
        Check.notNull(localFilename, "localFilename"); //$NON-NLS-1$

        this.localFilename = localFilename;
    }

    @Override
    public String getName() {
        return (Messages.getString("AbstractGetToTempLocationCommand.CommandText")); //$NON-NLS-1$
    }

    @Override
    public String getErrorDescription() {
        return (Messages.getString("AbstractGetToTempLocationCommand.ErrorText")); //$NON-NLS-1$
    }

    @Override
    public String getLoggingDescription() {
        return (Messages.getString("AbstractGetToTempLocationCommand.CommandText", LocaleUtil.ROOT)); //$NON-NLS-1$
    }

    public abstract String getFileDescription();

    protected abstract String getDownloadURL() throws Exception;

    @Override
    protected final IStatus doRun(final IProgressMonitor progressMonitor) throws Exception {
        final String downloadUrl = getDownloadURL();

        try {
            tempLocation = repository.getVersionControlClient().downloadFileToTempLocation(
                new DownloadSpec(downloadUrl),
                localFilename).getAbsolutePath();
        } catch (final CanceledException e) {
            return Status.CANCEL_STATUS;
        }

        return Status.OK_STATUS;
    }

    public String getTempLocation() {
        return tempLocation;
    }
}
