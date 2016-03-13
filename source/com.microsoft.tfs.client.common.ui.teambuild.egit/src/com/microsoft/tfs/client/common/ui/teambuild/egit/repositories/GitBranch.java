// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.teambuild.egit.repositories;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jgit.lib.Constants;

import com.microsoft.tfs.client.common.framework.command.CommandExecutor;
import com.microsoft.tfs.client.common.framework.command.ICommandExecutor;
import com.microsoft.tfs.client.common.git.commands.QueryGitItemsCommand;
import com.microsoft.tfs.client.common.git.json.TfsGitItemJson;
import com.microsoft.tfs.core.clients.versioncontrol.VersionControlClient;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.StringUtil;

public class GitBranch implements Comparable<GitBranch> {
    private static final Log log = LogFactory.getLog(GitBranch.class);

    private final VersionControlClient vcClient;
    private final GitRepository repository;
    private final String remoteName;
    private final String remoteFullName;
    private final String localName;
    private final String localFullName;
    private GitFolder rootFolder;

    public GitBranch(
        final VersionControlClient vcClient,
        final GitRepository repository,
        final String objectId,
        final String localFullName,
        final String remoteFullName) {
        this.vcClient = vcClient;
        this.repository = repository;
        this.localFullName = localFullName;
        this.remoteFullName = remoteFullName;
        this.localName = StringUtil.isNullOrEmpty(localFullName) ? null : localFullName.startsWith(Constants.R_HEADS)
            ? localFullName.substring(Constants.R_HEADS.length()) : localFullName;
        this.remoteName =
            StringUtil.isNullOrEmpty(remoteFullName) ? null : remoteFullName.startsWith(Constants.R_HEADS)
                ? remoteFullName.substring(Constants.R_HEADS.length()) : remoteFullName;
    }

    public String getRemoteName() {
        return remoteName;
    }

    public String getRemoteFullName() {
        return remoteFullName;
    }

    public String getLocalName() {
        return localName;
    }

    public String getLocalFullName() {
        return localFullName;
    }

    public GitFolder getRootFolder() {
        if (rootFolder == null) {
            final ICommandExecutor commandExecutor = new CommandExecutor();
            final QueryGitItemsCommand command = new QueryGitItemsCommand(vcClient, repository.getID(), remoteName);

            final IStatus status = commandExecutor.execute(command);

            if (status.isOK()) {
                final List<TfsGitItemJson> items = command.getRepositoryItems();

                Check.notNull(items, "items"); //$NON-NLS-1$
                Check.isTrue(items.size() == 1, "Wrong number of items received: " + items.size()); //$NON-NLS-1$

                rootFolder = new GitFolder(
                    vcClient,
                    repository,
                    (GitFolder) null,
                    items.get(0).getObjectId(),
                    items.get(0).getPath());
            } else {
                log.error(status.getMessage(), status.getException());
            }
        }

        return rootFolder;
    }

    @Override
    public int compareTo(final GitBranch o) {
        if (o == null || o.remoteName == null) {
            return -1;
        } else {
            return o.remoteName.compareTo(remoteName);
        }
    }
}
