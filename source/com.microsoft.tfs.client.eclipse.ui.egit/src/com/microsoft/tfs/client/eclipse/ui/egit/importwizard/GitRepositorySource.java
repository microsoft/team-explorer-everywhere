// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.eclipse.ui.egit.importwizard;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.IStatus;

import com.microsoft.tfs.client.common.framework.command.CommandExecutor;
import com.microsoft.tfs.client.common.framework.command.ICommandExecutor;
import com.microsoft.tfs.client.common.git.commands.QueryGitRepositoriesCommand;
import com.microsoft.tfs.client.common.git.json.TfsGitRepositoryJson;
import com.microsoft.tfs.client.common.ui.vc.serveritem.ServerItemSource;
import com.microsoft.tfs.client.common.ui.vc.serveritem.ServerItemType;
import com.microsoft.tfs.client.common.ui.vc.serveritem.TypedServerGitRepository;
import com.microsoft.tfs.client.common.ui.vc.serveritem.TypedServerItem;
import com.microsoft.tfs.core.TFSTeamProjectCollection;
import com.microsoft.tfs.core.clients.commonstructure.ProjectInfo;
import com.microsoft.tfs.core.clients.versioncontrol.VersionControlClient;
import com.microsoft.tfs.core.clients.versioncontrol.path.ServerPath;
import com.microsoft.tfs.util.Check;

public class GitRepositorySource extends ServerItemSource {
    private final static TypedServerItem[] NO_CHILDREN = new TypedServerItem[0];

    private ICommandExecutor commandExecutor = new CommandExecutor();
    private ProjectInfo[] projects = null;
    private Map<String, List<TfsGitRepositoryJson>> projectRepositories = null;

    public GitRepositorySource(final TFSTeamProjectCollection connection, final ProjectInfo[] projects) {
        super(connection);
        this.projects = projects;
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
            readRepositoriesInfo();
            return getChildItems(projects, parent);
        }

        final List<TfsGitRepositoryJson> gitRepositories = projectRepositories.get(parent.getName());
        return getChildItems(gitRepositories, parent);
    }

    private void readRepositoriesInfo() {
        if (projectRepositories == null) {
            projectRepositories = new HashMap<String, List<TfsGitRepositoryJson>>();

            if (projects != null && projects.length > 0) {
                final VersionControlClient vcClient = getConnection().getVersionControlClient();
                final QueryGitRepositoriesCommand queryCommand = new QueryGitRepositoriesCommand(vcClient);

                final IStatus status = commandExecutor.execute(queryCommand);

                if (status.isOK()) {
                    final List<TfsGitRepositoryJson> gitRepositories = queryCommand.getRepositories();

                    for (final TfsGitRepositoryJson gitRepository : gitRepositories) {
                        final String projectName = gitRepository.getTeamProject().getName();
                        List<TfsGitRepositoryJson> list = projectRepositories.get(projectName);

                        if (list == null) {
                            list = new ArrayList<TfsGitRepositoryJson>();
                            projectRepositories.put(projectName, list);
                        }

                        list.add(gitRepository);
                    }
                }
            }
        }
    }

    private TypedServerItem[] getChildItems(final ProjectInfo[] projects, final TypedServerItem parent) {
        if (projects == null || projects.length == 0) {
            return NO_CHILDREN;
        }

        final int n = projects.length;
        final TypedServerItem[] items = new TypedServerItem[n];

        for (int i = 0; i < n; i++) {
            final TypedServerItem child = getTypedItemForProject(projects[i]);
            items[i] = child;
        }

        return items;
    }

    private TypedServerItem getTypedItemForProject(final ProjectInfo project) {
        final String serverPath = ServerPath.combine(TypedServerItem.ROOT.getServerPath(), project.getName());
        return new TypedServerItem(serverPath, ServerItemType.TEAM_PROJECT);
    }

    private TypedServerItem[] getChildItems(
        final List<TfsGitRepositoryJson> gitRepositories,
        final TypedServerItem parent) {
        if (gitRepositories == null) {
            return NO_CHILDREN;
        }

        final TypedServerItem[] children = new TypedServerItem[gitRepositories.size()];

        int idx = 0;
        for (final TfsGitRepositoryJson repo : gitRepositories) {
            final String serverPath = ServerPath.combine(parent.getServerPath(), repo.getName());
            final TypedServerItem child = new TypedServerGitRepository(serverPath, repo);
            children[idx++] = child;
        }

        return children;
    }

    public List<TypedServerItem> getRepositoriesForRoot() {
        final List<TypedServerItem> list = new ArrayList<TypedServerItem>();

        for (final TypedServerItem project : computeChildren(TypedServerItem.ROOT)) {
            list.addAll(getRepositoriesForProject(project));
        }

        return list;
    }

    public List<TypedServerItem> getRepositoriesForProject(final TypedServerItem project) {
        Check.isTrue(
            project.getType() == ServerItemType.TEAM_PROJECT,
            "project.getType() == ServerItemType.TEAM_PROJECT"); //$NON-NLS-1$

        final TypedServerItem[] children = computeChildren(project);
        return Arrays.asList(children);
    }
}
