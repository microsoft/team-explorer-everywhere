// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.clc.vc.diff.launch;

import java.io.IOException;
import java.text.MessageFormat;

import com.microsoft.tfs.client.clc.Messages;
import com.microsoft.tfs.client.clc.exceptions.CLCException;
import com.microsoft.tfs.core.clients.versioncontrol.VersionControlClient;
import com.microsoft.tfs.core.clients.versioncontrol.path.LocalPath;
import com.microsoft.tfs.core.clients.versioncontrol.path.ServerPath;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.ChangeType;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Item;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.PendingChange;
import com.microsoft.tfs.core.clients.versioncontrol.specs.version.ChangesetVersionSpec;
import com.microsoft.tfs.core.clients.versioncontrol.specs.version.VersionSpec;
import com.microsoft.tfs.util.Check;

public class PendingChangeDiffLaunchItem extends AbstractDiffLaunchItem {
    private final PendingChange pendingChange;
    private final VersionControlClient client;

    private String filePath;
    private int encoding;
    private String label;

    public PendingChangeDiffLaunchItem(final PendingChange pendingChange, final VersionControlClient client) {
        this.pendingChange = pendingChange;
        this.client = client;
    }

    @Override
    public int getEncoding() throws CLCException {
        if (encoding == 0) {
            if (pendingChange.getChangeType().contains(ChangeType.ENCODING) && pendingChange.getVersion() != 0) {
                final Item item = client.getItem(pendingChange.getItemID(), pendingChange.getVersion(), false);
                Check.notNull(item, "item"); //$NON-NLS-1$
                encoding = item.getEncoding().getCodePage();
            } else {
                encoding = pendingChange.getEncoding();
            }
        }

        return encoding;
    }

    @Override
    public String getFilePath() throws CLCException {
        if (filePath == null) {
            if (pendingChange.getChangeType().contains(ChangeType.ADD)) {
                try {
                    filePath =
                        getVersionedTempFileFullPath(pendingChange.getServerItem(), pendingChange.getItemType(), null);
                    createEmptyTempFile(filePath);
                } catch (final IOException e) {
                    throw new CLCException(
                        Messages.getString("PendingChangeDiffLaunchItem.ErrorCreatingTempForAdd"), //$NON-NLS-1$
                        e);
                }
            } else {
                final VersionSpec versionSpec =
                    pendingChange.getVersion() == 0 ? null : new ChangesetVersionSpec(pendingChange.getVersion());

                filePath = getVersionedTempFileFullPath(
                    pendingChange.getServerItem(),
                    pendingChange.getItemType(),
                    versionSpec);
                pendingChange.downloadBaseFile(client, filePath);
            }
        }

        return filePath;
    }

    @Override
    public boolean isTemporary() {
        return true;
    }

    @Override
    public String getLabel() throws CLCException {
        if (label == null || label.length() == 0) {
            final String creationDateString =
                SHORT_DATE_TIME_FORMATTER.format(pendingChange.getCreationDate().getTime());

            if (pendingChange.getVersion() == 0) {
                if (pendingChange.getChangeType().contains(ChangeType.ADD)) {
                    label = Messages.getString("PendingChangeDiffLaunchItem.NoFileLabel"); //$NON-NLS-1$
                } else {
                    final String messageFormat =
                        Messages.getString("PendingChangeDiffLaunchItem.PendingChangeServerItemLabelFormat"); //$NON-NLS-1$
                    final String message = MessageFormat.format(
                        messageFormat,
                        ServerPath.getFileName(pendingChange.getSourceServerItem()),
                        creationDateString);

                    label = message;
                }
            } else if (pendingChange.getLocalItem() != null) {
                final String messageFormat =
                    Messages.getString("PendingChangeDiffLaunchItem.PendingChangeLocalItemLabelFormat"); //$NON-NLS-1$
                final String message = MessageFormat.format(
                    messageFormat,
                    LocalPath.makeRelative(pendingChange.getLocalItem(), LocalPath.getCurrentWorkingDirectory()),
                    new ChangesetVersionSpec(pendingChange.getVersion()).toString(),
                    creationDateString);

                label = message;
            } else {
                final String messageFormat =
                    Messages.getString("PendingChangeDiffLaunchItem.PendingChangeLocalItemLabelFormat"); //$NON-NLS-1$
                final String message = MessageFormat.format(
                    messageFormat,
                    pendingChange.getServerItem(),
                    new ChangesetVersionSpec(pendingChange.getVersion()).toString(),
                    creationDateString);

                label = message;
            }
        }

        return label;
    }

    @Override
    public void setLabel(final String label) {
        this.label = label;
    }
}
