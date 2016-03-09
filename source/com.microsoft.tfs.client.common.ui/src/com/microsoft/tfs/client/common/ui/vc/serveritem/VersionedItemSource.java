// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.vc.serveritem;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IStatus;

import com.microsoft.tfs.client.common.commands.vc.QueryItemsCommand;
import com.microsoft.tfs.client.common.framework.command.CommandExecutor;
import com.microsoft.tfs.client.common.framework.command.ICommand;
import com.microsoft.tfs.client.common.framework.command.ICommandExecutor;
import com.microsoft.tfs.client.common.repository.TFSRepository;
import com.microsoft.tfs.client.common.server.TFSServer;
import com.microsoft.tfs.core.TFSTeamProjectCollection;
import com.microsoft.tfs.core.clients.commonstructure.ProjectInfo;
import com.microsoft.tfs.core.clients.versioncontrol.GetItemsOptions;
import com.microsoft.tfs.core.clients.versioncontrol.VersionControlClient;
import com.microsoft.tfs.core.clients.versioncontrol.path.ServerPath;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.DeletedState;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Item;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.ItemSet;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.ItemType;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.RecursionType;
import com.microsoft.tfs.core.clients.versioncontrol.specs.ItemSpec;
import com.microsoft.tfs.core.clients.versioncontrol.specs.version.LatestVersionSpec;
import com.microsoft.tfs.core.clients.versioncontrol.specs.version.VersionSpec;
import com.microsoft.tfs.util.Check;

public class VersionedItemSource extends ServerItemSource {
    private final static TypedServerItem[] NO_CHILDREN = new TypedServerItem[0];

    private final VersionSpec version;
    private ICommandExecutor commandExecutor = new CommandExecutor();
    private ProjectInfo[] projects = null;

    public VersionedItemSource(final TFSRepository repository) {
        this(repository.getVersionControlClient().getConnection());
    }

    public VersionedItemSource(final TFSRepository repository, final VersionSpec versionSpec) {
        this(repository.getVersionControlClient().getConnection(), versionSpec);
    }

    public VersionedItemSource(final TFSServer server) {
        this(server.getConnection());
    }

    public VersionedItemSource(final TFSTeamProjectCollection connection) {
        super(connection);
        this.version = LatestVersionSpec.INSTANCE;
    }

    public VersionedItemSource(final TFSTeamProjectCollection connection, final ProjectInfo[] projects) {
        super(connection);
        this.version = LatestVersionSpec.INSTANCE;
        this.projects = projects;
    }

    public VersionedItemSource(final TFSTeamProjectCollection connection, final VersionSpec version) {
        super(connection);
        if (version == null) {
            this.version = LatestVersionSpec.INSTANCE;
        } else {
            this.version = version;
        }
    }

    public void setCommandExecutor(final ICommandExecutor commandExecutor) {
        Check.notNull(commandExecutor, "commandExecutor"); //$NON-NLS-1$

        this.commandExecutor = commandExecutor;
    }

    public ICommandExecutor getCommandExecutor() {
        return commandExecutor;
    }

    @Override
    protected TypedServerItem[] computeChildren(final TypedServerItem parent) {
        Check.notNull(parent, "parent"); //$NON-NLS-1$

        if (projects != null && projects.length > 0 && parent.equals(TypedServerItem.ROOT)) {
            return getChildItems(projects, parent);
        }

        final VersionControlClient vcClient = getConnection().getVersionControlClient();
        final ItemSpec[] itemSpecs = new ItemSpec[] {
            new ItemSpec(parent.getServerPath(), RecursionType.ONE_LEVEL)
        };

        final ICommand queryCommand = new QueryItemsCommand(
            vcClient,
            itemSpecs,
            version,
            DeletedState.NON_DELETED,
            ItemType.ANY,
            GetItemsOptions.INCLUDE_BRANCH_INFO);

        final IStatus status = commandExecutor.execute(queryCommand);

        if (!status.isOK()) {
            return NO_CHILDREN;
        }

        final ItemSet itemSet = ((QueryItemsCommand) queryCommand).getItemSets()[0];
        return getChildItems(itemSet, parent);
    }

    private TypedServerItem[] getChildItems(final ProjectInfo[] projects, final TypedServerItem parent) {
        if (projects == null || projects.length == 0) {
            return NO_CHILDREN;
        }

        final int n = projects.length;
        final TypedServerItem[] items = new TypedServerItem[n];

        for (int i = 0; i < n; i++) {
            final ProjectInfo project = projects[i];
            final String serverPath = ServerPath.combine(parent.getServerPath(), project.getName());
            final TypedServerItem child = new TypedServerItem(serverPath, ServerItemType.TEAM_PROJECT);

            items[i] = child;
        }

        return items;
    }

    private TypedServerItem[] getChildItems(final ItemSet itemSet, final TypedServerItem parent) {
        if (itemSet == null) {
            return NO_CHILDREN;
        }

        final Item[] items = itemSet.getItems();

        if (items == null) {
            return NO_CHILDREN;
        }

        final List<TypedServerItem> list = new ArrayList<TypedServerItem>(items.length);

        for (int i = 0; i < items.length; i++) {
            final ServerItemType type = ServerItemType.getTypeFromItemType(items[i].getItemType());
            final boolean isBranch = items[i].isBranch();
            final TypedServerItem child = new TypedServerItem(items[i].getServerItem(), type, isBranch);

            if (!parent.equals(child)) {
                list.add(child);
            }
        }

        return list.toArray(new TypedServerItem[list.size()]);
    }
}
