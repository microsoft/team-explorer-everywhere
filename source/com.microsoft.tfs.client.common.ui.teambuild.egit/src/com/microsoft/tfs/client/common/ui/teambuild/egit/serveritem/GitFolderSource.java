// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.teambuild.egit.serveritem;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.microsoft.tfs.client.common.framework.command.CommandExecutor;
import com.microsoft.tfs.client.common.framework.command.ICommandExecutor;
import com.microsoft.tfs.client.common.ui.teambuild.egit.repositories.GitBranch;
import com.microsoft.tfs.client.common.ui.teambuild.egit.repositories.GitFolder;
import com.microsoft.tfs.client.common.ui.teambuild.egit.repositories.GitRepositoriesMap;
import com.microsoft.tfs.client.common.ui.teambuild.egit.repositories.GitRepository;
import com.microsoft.tfs.client.common.ui.vc.serveritem.ServerItemSource;
import com.microsoft.tfs.client.common.ui.vc.serveritem.ServerItemType;
import com.microsoft.tfs.client.common.ui.vc.serveritem.TypedServerGitItem;
import com.microsoft.tfs.client.common.ui.vc.serveritem.TypedServerItem;
import com.microsoft.tfs.core.TFSTeamProjectCollection;
import com.microsoft.tfs.core.clients.versioncontrol.path.ServerPath;
import com.microsoft.tfs.util.Check;

public class GitFolderSource extends ServerItemSource {
    private ICommandExecutor commandExecutor = new CommandExecutor();
    private final GitRepositoriesMap repositories;
    private final Map<TypedServerItem, GitRepository> repositoryCache = new HashMap<TypedServerItem, GitRepository>();
    private final Map<TypedServerItem, GitBranch> branchCache = new HashMap<TypedServerItem, GitBranch>();
    private final Map<TypedServerItem, GitFolder> folderCache = new HashMap<TypedServerItem, GitFolder>();

    public GitFolderSource(final TFSTeamProjectCollection connection, final GitRepositoriesMap repositories) {
        super(connection);
        this.repositories = repositories;
    }

    public void setCommandExecutor(final ICommandExecutor commandExecutor) {
        Check.notNull(commandExecutor, "commandExecutor"); //$NON-NLS-1$

        this.commandExecutor = commandExecutor;
    }

    public ICommandExecutor getCommandExecutor() {
        return commandExecutor;
    }

    @Override
    public TypedServerItem[] computeChildren(final TypedServerItem parent) {
        Check.notNull(parent, "parent"); //$NON-NLS-1$

        if (parent.equals(TypedServerItem.ROOT)) {
            return getTypedItemForProject();
        } else if (parent.getType() == ServerItemType.TEAM_PROJECT) {
            return getTypedItemsForRepositories(parent);
        } else if (parent.getType() == ServerItemType.GIT_REPOSITORY) {
            return getTypedItemsForBranches(parent);
        } else if (parent.getType() == ServerItemType.GIT_BRANCH) {
            return getTypedItemsForRootFolder(parent);
        } else {
            return getTypedItemsForSubfolders(parent);
        }
    }

    private TypedServerItem[] getTypedItemForProject() {
        final TypedServerItem[] projectItems = new TypedServerItem[1];
        final String serverPath =
            ServerPath.combine(TypedServerItem.ROOT.getServerPath(), repositories.getProjectName());

        projectItems[0] = new TypedServerGitItem(serverPath, ServerItemType.TEAM_PROJECT);

        return projectItems;
    }

    private TypedServerItem[] getTypedItemsForRepositories(final TypedServerItem parent) {
        final List<TypedServerItem> repositoryItems = new ArrayList<TypedServerItem>();

        for (final GitRepository repository : repositories.getServerRepositories()) {
            final String serverPath = ServerPath.combine(parent.getServerPath(), repository.getName());
            final TypedServerItem item = new TypedServerGitItem(serverPath, ServerItemType.GIT_REPOSITORY);

            repositoryItems.add(item);
            repositoryCache.put(item, repository);
        }

        return repositoryItems.toArray(new TypedServerItem[repositoryItems.size()]);
    }

    private TypedServerItem[] getTypedItemsForBranches(final TypedServerItem parent) {
        final List<TypedServerItem> branchItems = new ArrayList<TypedServerItem>();
        final GitRepository repository = repositoryCache.get(parent);

        for (final GitBranch branch : repository.getBranches()) {
            final String serverPath = ServerPath.combine(parent.getServerPath(), branch.getRemoteName());
            final TypedServerItem item = new TypedServerGitItem(serverPath, ServerItemType.GIT_BRANCH);

            branchItems.add(item);
            branchCache.put(item, branch);
        }

        return branchItems.toArray(new TypedServerItem[branchItems.size()]);
    }

    private TypedServerItem[] getTypedItemsForRootFolder(final TypedServerItem parent) {
        final GitBranch branch = branchCache.get(parent);

        return getTypedItemsForSubfolders(parent, branch.getRootFolder().getChildren());
    }

    private TypedServerItem[] getTypedItemsForSubfolders(final TypedServerItem parent) {
        final GitFolder folder = folderCache.get(parent);

        return getTypedItemsForSubfolders(parent, folder.getChildren());
    }

    private TypedServerItem[] getTypedItemsForSubfolders(final TypedServerItem parent, final List<GitFolder> children) {
        final List<TypedServerItem> folderItems = new ArrayList<TypedServerItem>();

        for (final GitFolder folder : children) {
            final String serverPath = ServerPath.combine(parent.getServerPath(), folder.getName());
            final TypedServerItem item = new TypedServerGitItem(serverPath, ServerItemType.FOLDER);

            folderItems.add(item);
            folderCache.put(item, folder);
        }

        return folderItems.toArray(new TypedServerItem[folderItems.size()]);
    }
}
