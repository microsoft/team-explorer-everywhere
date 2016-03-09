// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.teambuild.egit.repositories;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.egit.core.Activator;
import org.eclipse.egit.core.RepositoryUtil;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.RepositoryBuilder;
import org.eclipse.jgit.lib.RepositoryCache.FileKey;
import org.eclipse.jgit.lib.StoredConfig;
import org.eclipse.jgit.util.FS;

import com.microsoft.tfs.client.common.framework.command.CommandExecutor;
import com.microsoft.tfs.client.common.framework.command.ICommandExecutor;
import com.microsoft.tfs.client.common.git.commands.QueryGitRepositoriesCommand;
import com.microsoft.tfs.client.common.git.json.TfsGitRepositoryJson;
import com.microsoft.tfs.core.clients.versioncontrol.VersionControlClient;
import com.microsoft.tfs.core.httpclient.URIException;
import com.microsoft.tfs.core.httpclient.util.URIUtil;
import com.microsoft.tfs.util.Check;

public class GitRepositoriesMap {
    private static final Log log = LogFactory.getLog(GitRepositoriesMap.class);

    private static final String REMOTES_SECTION_NAME = "remote"; //$NON-NLS-1$
    private static final String BRANCHES_SECTION_NAME = "branch"; //$NON-NLS-1$
    private static final String URL_VALUE_NAME = "url"; //$NON-NLS-1$
    private static final String MERGE_VALUE_NAME = "merge"; //$NON-NLS-1$
    private static final String REMOTE_VALUE_NAME = "remote"; //$NON-NLS-1$

    private final List<Repository> registeredRepositories;
    private final List<GitRepository> serverRepositories;
    private final List<GitRepository> clonedRepositories;
    private final VersionControlClient vcClient;
    private final String projectName;

    public GitRepositoriesMap(final VersionControlClient vcClient, final String projectName) {
        Check.notNull(vcClient, "vcClient"); //$NON-NLS-1$
        Check.notNullOrEmpty(projectName, "projectName"); //$NON-NLS-1$

        this.vcClient = vcClient;
        this.projectName = projectName;
        this.serverRepositories = new ArrayList<GitRepository>();
        this.clonedRepositories = new ArrayList<GitRepository>();

        final ICommandExecutor commandExecutor = new CommandExecutor();

        registeredRepositories = findRegisteredRepositories();

        final QueryGitRepositoriesCommand queryCommand = new QueryGitRepositoriesCommand(vcClient, projectName);
        final IStatus status = commandExecutor.execute(queryCommand);

        if (status.isOK()) {
            final List<TfsGitRepositoryJson> serverRepositoryInfos = queryCommand.getRepositories();

            for (final TfsGitRepositoryJson serverRepositoryInfo : serverRepositoryInfos) {
                final GitRepository repository = findClonedRepository(serverRepositoryInfo);
                final GitRepository repositoryToAdd =
                    repository != null ? repository : new GitRepository(vcClient, serverRepositoryInfo);

                if (repository != null) {
                    clonedRepositories.add(repositoryToAdd);
                }
                serverRepositories.add(repositoryToAdd);
            }
        }
    }

    private List<Repository> findRegisteredRepositories() {
        final RepositoryUtil util = Activator.getDefault().getRepositoryUtil();
        final List<String> repositoryFolders = util.getConfiguredRepositories();

        final List<Repository> repositories = new ArrayList<Repository>();

        for (final String repositoryFolder : repositoryFolders) {
            final File folder = new File(repositoryFolder);
            if (!folder.exists() || !folder.isDirectory()) {
                continue;
            }

            if (!folder.getName().equals(Constants.DOT_GIT) || !FileKey.isGitRepository(folder, FS.DETECTED)) {
                continue;
            }

            final RepositoryBuilder rb = new RepositoryBuilder().setGitDir(folder).setMustExist(true);

            try {
                repositories.add(rb.build());
            } catch (final Exception e) {
                log.error("Error loading Git repository " + repositoryFolder, e); //$NON-NLS-1$
                continue;
            }
        }

        return repositories;
    }

    private GitRepository findClonedRepository(final TfsGitRepositoryJson serverRepository) {
        for (final Repository localRepository : registeredRepositories) {
            final Map<String, String> mappedBranches = getMappedBranches(serverRepository, localRepository);

            if (mappedBranches != null && mappedBranches.size() > 0) {
                return new GitRepository(vcClient, serverRepository, localRepository, mappedBranches);
            }
        }

        return null;
    }

    private Map<String, String> getMappedBranches(
        final TfsGitRepositoryJson serverRepository,
        final Repository localRepository) {
        Map<String, String> mappedBranches = null;
        String upstreamURL = serverRepository.getRemoteUrl();
        try {
            upstreamURL = URIUtil.encodePath(serverRepository.getRemoteUrl());
        } catch (final URIException e) {
            log.error("Error encoding repository URL " + upstreamURL, e); //$NON-NLS-1$
        }

        final StoredConfig repositoryConfig = localRepository.getConfig();
        final Set<String> remotes = repositoryConfig.getSubsections(REMOTES_SECTION_NAME);

        for (final String remoteName : remotes) {
            final String remoteURL = repositoryConfig.getString(REMOTES_SECTION_NAME, remoteName, URL_VALUE_NAME);

            if (remoteURL != null && remoteURL.equalsIgnoreCase(upstreamURL)) {
                if (mappedBranches == null) {
                    mappedBranches = new HashMap<String, String>();
                }

                final Set<String> branches = repositoryConfig.getSubsections(BRANCHES_SECTION_NAME);

                for (final String branch : branches) {
                    final String fullBranchName = Constants.R_HEADS + branch;

                    final String[] remoteNames =
                        repositoryConfig.getStringList(BRANCHES_SECTION_NAME, branch, REMOTE_VALUE_NAME);
                    final String[] mappedBrancheNames =
                        repositoryConfig.getStringList(BRANCHES_SECTION_NAME, branch, MERGE_VALUE_NAME);

                    for (int k = 0; k < remoteNames.length; k++) {
                        if (remoteNames[k].equals(remoteName)) {
                            final String remoteBranchName = mappedBrancheNames[k];

                            if (!mappedBranches.containsKey(remoteBranchName)) {
                                mappedBranches.put(remoteBranchName, fullBranchName);
                            }

                            break;
                        }
                    }
                }

                break;
            }
        }

        return mappedBranches;
    }

    public String getProjectName() {
        return projectName;
    }

    public List<GitRepository> getServerRepositories() {
        return serverRepositories;
    }

    public List<Repository> getRegisteredRepositories() {
        return registeredRepositories;
    }

    public List<GitRepository> getClonedRepositories() {
        return clonedRepositories;
    }

    public List<GitRepository> getMappedRepositories() {
        final List<GitRepository> mappedRepositories = new ArrayList<GitRepository>();

        for (final GitRepository repository : clonedRepositories) {
            if (repository.hasMappedBranches()) {
                mappedRepositories.add(repository);
            }
        }

        return mappedRepositories;
    }
}
