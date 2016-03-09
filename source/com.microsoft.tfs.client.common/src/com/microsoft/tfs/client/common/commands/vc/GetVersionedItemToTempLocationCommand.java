// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.commands.vc;

import java.text.MessageFormat;

import com.microsoft.tfs.client.common.Messages;
import com.microsoft.tfs.client.common.repository.TFSRepository;
import com.microsoft.tfs.core.clients.versioncontrol.path.ServerPath;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Changeset;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.DeletedState;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Item;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.ItemSet;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.ItemType;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.RecursionType;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Workspace;
import com.microsoft.tfs.core.clients.versioncontrol.specs.ItemSpec;
import com.microsoft.tfs.core.clients.versioncontrol.specs.version.LatestVersionSpec;
import com.microsoft.tfs.core.clients.versioncontrol.specs.version.VersionSpec;
import com.microsoft.tfs.util.Check;

public class GetVersionedItemToTempLocationCommand extends AbstractGetToTempLocationCommand {
    private final TFSRepository repository;
    private final String serverPath;
    private final VersionSpec versionSpec;
    private final VersionSpec queryVersionSpec;

    public GetVersionedItemToTempLocationCommand(
        final TFSRepository repository,
        final String localOrServerPath,
        final VersionSpec versionSpec) {
        this(repository, localOrServerPath, versionSpec, versionSpec);
    }

    public GetVersionedItemToTempLocationCommand(
        final TFSRepository repository,
        final String localOrServerPath,
        final VersionSpec versionSpec,
        final VersionSpec queryVersionSpec) {
        super(repository);

        Check.notNull(repository, "repository"); //$NON-NLS-1$
        Check.notNull(localOrServerPath, "localOrServerPath"); //$NON-NLS-1$
        Check.notNull(versionSpec, "versionSpec"); //$NON-NLS-1$

        this.repository = repository;
        serverPath = ServerPath.isServerPath(localOrServerPath) ? localOrServerPath
            : repository.getWorkspace().getMappedServerPath(localOrServerPath);
        this.versionSpec = versionSpec;
        this.queryVersionSpec = queryVersionSpec;

        setLocalFilename(ServerPath.getFileName(serverPath));
    }

    @Override
    public String getFileDescription() {
        return serverPath;
    }

    /**
     * Gets a DownloadURL for the given IResource at the given AVersionSpec in
     * the given TFSRepository. This function appears insanely more complicated
     * than required, but it emulates MSFT behavior, probably with good reason.
     *
     * @return The durl for this object's IResource at the AVersionSpec.
     * @throws Exception
     *         If an error occured in one of the many server queries.
     */
    @Override
    protected String getDownloadURL() throws Exception {
        final Workspace workspace = repository.getWorkspace();

        int itemId;
        int changesetNum;

        // query the item first, then get the durl for this particular
        // changeset. this emulate's microsoft behavior, even though it
        // appears to unnecessarily(?) add a roundtrip... =/
        if (versionSpec instanceof LatestVersionSpec) {
            // Go work out the items id.
            final ItemSet[] items = repository.getVersionControlClient().getItems(new ItemSpec[] {
                new ItemSpec(serverPath, RecursionType.NONE)
            }, queryVersionSpec, DeletedState.NON_DELETED, ItemType.FILE, true);

            if (items == null || items.length == 0 || items[0] == null) {
                final String messageFormat =
                    Messages.getString("GetToTempLocationCommand.ErrorDeterminingLatestFormat"); //$NON-NLS-1$
                final String message = MessageFormat.format(messageFormat, serverPath);
                throw new Exception(message);
            }

            final Item[] subItems = items[0].getItems();

            if (subItems == null || subItems.length == 0 || subItems[0] == null) {
                final String messageFormat =
                    Messages.getString("GetToTempLocationCommand.ErrorDeterminingLatestFormat"); //$NON-NLS-1$
                final String message = MessageFormat.format(messageFormat, serverPath);
                throw new Exception(message);
            }

            itemId = subItems[0].getItemID();
            changesetNum = Changeset.MAX;
        } else {
            // Get a historical version - from that work out the item and
            // changeset number.
            final Changeset[] changeset = workspace.queryHistory(
                serverPath,
                queryVersionSpec,
                0,
                RecursionType.NONE,
                null,
                null,
                versionSpec,
                1,
                true,
                false,
                false,
                false);

            if (changeset == null
                || changeset.length == 0
                || changeset[0].getChanges() == null
                || changeset[0].getChanges().length == 0
                || changeset[0].getChanges()[0] == null) {
                final String messageFormat = Messages.getString("GetToTempLocationCommand.VersionDoesNotExistFormat"); //$NON-NLS-1$
                final String message = MessageFormat.format(messageFormat, serverPath);
                throw new Exception(message);
            }

            if (changeset[0].getChanges()[0].getItem().getItemType() != ItemType.FILE) {
                final String messageFormat = Messages.getString("GetToTempLocationCommand.ItemIsNotAFileMessageFormat"); //$NON-NLS-1$
                final String message = MessageFormat.format(messageFormat, serverPath);
                throw new Exception(message);
            }

            itemId = changeset[0].getChanges()[0].getItem().getItemID();
            changesetNum = changeset[0].getChanges()[0].getItem().getChangeSetID();
        }

        return workspace.getClient().getItem(itemId, changesetNum, true).getDownloadURL();
    }
}
