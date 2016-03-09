// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.clc.vc.diff.launch;

import java.io.IOException;
import java.text.MessageFormat;

import com.microsoft.tfs.client.clc.Messages;
import com.microsoft.tfs.client.clc.exceptions.CLCException;
import com.microsoft.tfs.core.clients.versioncontrol.VersionControlClient;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.ChangeType;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.PendingChange;
import com.microsoft.tfs.core.clients.versioncontrol.specs.version.ChangesetVersionSpec;
import com.microsoft.tfs.util.Check;

public class ShelvedChangeDiffLaunchItem extends AbstractDiffLaunchItem {
    private final String shelvesetName;
    private final PendingChange pendingChange;
    private final VersionControlClient client;

    private String filePath;
    private String label;

    /**
     * Creates a {@link ShelvedChangeDiffLaunchItem} from the given shelveset
     * name and pending change.
     *
     * @param shelvesetName
     *        the name of the shelveset (not null)
     * @param pendingChange
     *        the pending change (not null)
     * @param a
     *        {@link VersionControlClient} used for downloads (not null)
     */
    public ShelvedChangeDiffLaunchItem(
        final String shelvesetName,
        final PendingChange pendingChange,
        final VersionControlClient client) {
        Check.notNull(shelvesetName, "shelvesetName"); //$NON-NLS-1$
        Check.notNull(pendingChange, "pendingChange"); //$NON-NLS-1$
        Check.notNull(client, "client"); //$NON-NLS-1$

        this.shelvesetName = shelvesetName;
        this.pendingChange = pendingChange;
        this.client = client;
    }

    @Override
    public int getEncoding() throws CLCException {
        return pendingChange.getEncoding();
    }

    @Override
    public String getFilePath() throws CLCException {
        if (filePath == null) {
            if (pendingChange.getChangeType().contains(ChangeType.EDIT)) {
                filePath =
                    getVersionedTempFileFullPath(pendingChange.getServerItem(), pendingChange.getItemType(), null);
                pendingChange.downloadShelvedFile(client, filePath);
            } else if (pendingChange.getChangeType().contains(ChangeType.DELETE)) {
                try {
                    /*
                     * Use an empty file to simulate the delete.
                     */
                    filePath = getVersionedTempFileFullPath(
                        pendingChange.getServerItem(),
                        pendingChange.getItemType(),
                        new ChangesetVersionSpec(pendingChange.getVersion()));
                    createEmptyTempFile(filePath);
                } catch (final IOException e) {
                    throw new CLCException(
                        Messages.getString("ShelvedChangeDiffLaunchItem.ErrorCreatingTempFileForDelete"), //$NON-NLS-1$
                        e);
                }
            } else {
                filePath =
                    getVersionedTempFileFullPath(pendingChange.getServerItem(), pendingChange.getItemType(), null);
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
            final String messageFormat = Messages.getString("ShelvedChangeDiffLaunchItem.ShelvedChangeLabelFormat"); //$NON-NLS-1$
            final String message = MessageFormat.format(messageFormat, pendingChange.getServerItem(), shelvesetName);

            label = message;
        }

        return label;
    }

    @Override
    public void setLabel(final String label) {
        this.label = label;
    }
}
