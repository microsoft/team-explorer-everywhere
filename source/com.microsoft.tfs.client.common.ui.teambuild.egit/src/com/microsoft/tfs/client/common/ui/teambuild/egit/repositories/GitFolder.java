// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.teambuild.egit.repositories;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.runtime.IStatus;

import com.microsoft.tfs.client.common.framework.command.CommandExecutor;
import com.microsoft.tfs.client.common.framework.command.ICommandExecutor;
import com.microsoft.tfs.client.common.git.commands.QueryGitTreeEntriesCommand;
import com.microsoft.tfs.client.common.git.json.TfsGitTreeEntryJson;
import com.microsoft.tfs.core.clients.versioncontrol.VersionControlClient;

public class GitFolder implements Comparable<GitFolder> {
    private static final Log log = LogFactory.getLog(GitFolder.class);

    private final VersionControlClient vcClient;
    private final GitRepository repository;
    private final GitFolder parent;
    private final String objectId;
    private final String name;

    private List<GitFolder> children;

    public GitRepository getRepository() {
        return repository;
    }

    public GitFolder(
        final VersionControlClient vcClient,
        final GitRepository repository,
        final GitFolder parent,
        final String objectId,
        final String name) {
        this.vcClient = vcClient;
        this.repository = repository;
        this.parent = parent;
        this.objectId = objectId;
        this.name = name;
    }

    public GitFolder getParent() {
        return parent;
    }

    public String getObjectId() {
        return objectId;
    }

    public String getName() {
        return name;
    }

    public List<GitFolder> getChildren() {
        if (children == null) {
            children = new ArrayList<GitFolder>();

            final ICommandExecutor commandExecutor = new CommandExecutor();
            final QueryGitTreeEntriesCommand command =
                new QueryGitTreeEntriesCommand(vcClient, repository.getID(), objectId);

            final IStatus status = commandExecutor.execute(command);

            if (status.isOK()) {
                for (final TfsGitTreeEntryJson treeEntry : command.getTreeEntries()) {
                    if (treeEntry.getGitObjectType().equals("tree")) //$NON-NLS-1$
                    {
                        children.add(
                            new GitFolder(
                                vcClient,
                                repository,
                                this,
                                treeEntry.getObjectId(),
                                treeEntry.getRelativePath()));
                    }
                }
            } else {
                log.error(status.getMessage(), status.getException());
            }
        }

        return children;
    }

    @Override
    public int compareTo(final GitFolder o) {
        if (o == null || o.name == null) {
            return -1;
        } else {
            return o.name.compareTo(name);
        }
    }
}
