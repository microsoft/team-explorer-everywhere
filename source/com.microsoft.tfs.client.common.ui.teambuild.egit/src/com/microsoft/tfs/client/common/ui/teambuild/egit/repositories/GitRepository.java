// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.teambuild.egit.repositories;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jgit.lib.Repository;

import com.microsoft.tfs.client.common.framework.command.CommandExecutor;
import com.microsoft.tfs.client.common.framework.command.ICommandExecutor;
import com.microsoft.tfs.client.common.git.commands.QueryGitRepositoryBranchesCommand;
import com.microsoft.tfs.client.common.git.json.TfsGitBranchJson;
import com.microsoft.tfs.client.common.git.json.TfsGitBranchesJson;
import com.microsoft.tfs.client.common.git.json.TfsGitRepositoryJson;
import com.microsoft.tfs.client.common.ui.teambuild.egit.Messages;
import com.microsoft.tfs.core.clients.versioncontrol.VersionControlClient;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.GUID;
import com.microsoft.tfs.util.StringUtil;

public class GitRepository implements Comparable<GitRepository> {
    private static final Log log = LogFactory.getLog(GitRepositoriesMap.class);

    private final VersionControlClient vcClient;
    private final String name;
    private final GUID ID;
    private final GUID projectID;
    private final Repository localRepository;
    private final boolean hasMappedBranches;

    private final List<GitBranch> branches;
    private final String defaultBranchName;

    public GitRepository(final VersionControlClient vcClient, final TfsGitRepositoryJson serverRepository) {
        this(vcClient, serverRepository, null, new HashMap<String, String>());
    }

    public GitRepository(
        final VersionControlClient vcClient,
        final TfsGitRepositoryJson serverRepository,
        final Repository localRepository,
        final Map<String, String> mappedBranches) {
        this.vcClient = vcClient;
        this.localRepository = localRepository;
        this.name = serverRepository.getName();
        this.ID = new GUID(serverRepository.getId());
        this.projectID = new GUID(serverRepository.getTeamProject().getId());
        this.branches = new ArrayList<GitBranch>();
        this.defaultBranchName = serverRepository.getDefaultBranch();

        this.hasMappedBranches = !mappedBranches.isEmpty();
        queryServerBranches(serverRepository, mappedBranches);

    }

    private void queryServerBranches(
        final TfsGitRepositoryJson serverRepository,
        final Map<String, String> mappedBranches) {
        final ICommandExecutor commandExecutor = new CommandExecutor();
        final QueryGitRepositoryBranchesCommand queryBranchCommand =
            new QueryGitRepositoryBranchesCommand(vcClient, new GUID(serverRepository.getId()));
        final IStatus status = commandExecutor.execute(queryBranchCommand);

        if (status.isOK()) {
            final TfsGitBranchesJson remoteBranches = queryBranchCommand.getRepositoryBranches().get(ID);

            for (final TfsGitBranchJson remoteBranch : remoteBranches.getBranches()) {
                final String remoteFullName = remoteBranch.getFullName();
                final GitBranch branch = new GitBranch(
                    vcClient,
                    this,
                    remoteBranch.getObjectId(),
                    mappedBranches.get(remoteFullName),
                    remoteFullName);

                branches.add(branch);
            }
        }
    }

    public String getName() {
        return name;
    }

    public GUID getID() {
        return ID;
    }

    public GUID getProjectID() {
        return projectID;
    }

    public boolean hasMappedBranches() {
        return hasMappedBranches;
    }

    public List<GitBranch> getBranches() {
        return branches;
    }

    public GitBranch getDefaultBranch() {
        for (final GitBranch branch : branches) {
            if (branch.getRemoteFullName().equals(defaultBranchName)) {
                return branch;
            }
        }

        return null;
    }

    public GitBranch getRemoteBranch(final String branchName) {
        Check.notNullOrEmpty(branchName, "branchName"); //$NON-NLS-1$

        for (final GitBranch branch : branches) {
            if (branchName.equals(branch.getRemoteFullName()) || branchName.equals(branch.getRemoteName())) {
                return branch;
            }
        }

        return null;
    }

    public GitBranch getLocalBranch(final String branchName) {
        Check.notNullOrEmpty(branchName, "branchName"); //$NON-NLS-1$

        for (final GitBranch branch : branches) {
            if (branchName.equals(branch.getLocalFullName()) || branchName.equals(branch.getLocalName())) {
                return branch;
            }
        }

        return null;
    }

    public GitBranch getCurrentBranch() {
        if (localRepository == null) {
            return null;
        }

        try {
            final String currentBranchName = localRepository.getBranch();

            if (StringUtil.isNullOrEmpty(currentBranchName)) {
                return null;
            } else {
                for (final GitBranch branch : branches) {
                    if (currentBranchName.equals(branch.getLocalName())) {
                        return branch;
                    }
                }
            }
        } catch (final IOException e) {
            log.error("Error reading branch information", e); //$NON-NLS-1$
        }

        return null;
    }

    public String getWorkingDirectoryPath() {
        if (localRepository != null) {
            try {
                return localRepository.getWorkTree().getCanonicalPath();
            } catch (final Exception e) {
                log.warn("Working directory path is not avaqilable", e); //$NON-NLS-1$
                return Messages.getString("GitRepositoriesMap.PathIsNotAvailable"); //$NON-NLS-1$
            }
        } else {
            return Messages.getString("GitRepositoriesMap.NotClonedRepository"); //$NON-NLS-1$
        }
    }

    public String getRepositoryPath() {
        if (localRepository != null) {
            try {
                return localRepository.getDirectory().getCanonicalPath();
            } catch (final Exception e) {
                log.warn("Working directory path is not avaqilable", e); //$NON-NLS-1$
                return Messages.getString("GitRepositoriesMap.PathIsNotAvailable"); //$NON-NLS-1$
            }
        } else {
            return Messages.getString("GitRepositoriesMap.NotClonedRepository"); //$NON-NLS-1$
        }
    }

    public Repository getLocalRepository() {
        return localRepository;
    }

    @Override
    public int compareTo(final GitRepository o) {
        if (o == null || o.name == null) {
            return -1;
        } else {
            return o.name.compareToIgnoreCase(name);
        }
    }
}
