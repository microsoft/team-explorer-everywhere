// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.commands.vc;

import java.text.MessageFormat;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import com.microsoft.tfs.client.common.Messages;
import com.microsoft.tfs.client.common.commands.TFSConnectedCommand;
import com.microsoft.tfs.client.common.repository.TFSRepository;
import com.microsoft.tfs.core.checkinpolicies.PolicyFailure;
import com.microsoft.tfs.core.clients.versioncontrol.VersionControlConstants;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.PolicyOverrideInfo;
import com.microsoft.tfs.core.clients.versioncontrol.specs.version.VersionSpec;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.LocaleUtil;

public class BranchBranchCommand extends TFSConnectedCommand {
    private final TFSRepository repository;
    private final String sourceServerPath;
    private final String targetServerPath;
    private final VersionSpec version;
    private final String description;

    private int changeset;

    public BranchBranchCommand(
        final TFSRepository repository,
        final String sourceServerPath,
        final String targetServerPath,
        final VersionSpec version,
        final String description) {
        Check.notNull(repository, "repository"); //$NON-NLS-1$
        Check.notNull(sourceServerPath, "sourceServerPath"); //$NON-NLS-1$
        Check.notNull(targetServerPath, "targetServerPath"); //$NON-NLS-1$
        Check.notNull(version, "version"); //$NON-NLS-1$
        Check.notNull(description, "description"); //$NON-NLS-1$

        this.repository = repository;
        this.sourceServerPath = sourceServerPath;
        this.targetServerPath = targetServerPath;
        this.version = version;
        this.description = description;

        setConnection(repository.getConnection());
    }

    @Override
    public String getName() {
        final String messageFormat = Messages.getString("BranchBranchCommand.CommandTextFormat"); //$NON-NLS-1$
        return MessageFormat.format(messageFormat, targetServerPath);
    }

    @Override
    public String getErrorDescription() {
        final String messageFormat = Messages.getString("BranchBranchCommand.ErrorTextFormat"); //$NON-NLS-1$
        return MessageFormat.format(messageFormat, targetServerPath);
    }

    @Override
    public String getLoggingDescription() {
        final String messageFormat = Messages.getString("BranchBranchCommand.CommandTextFormat", LocaleUtil.ROOT); //$NON-NLS-1$
        return MessageFormat.format(messageFormat, targetServerPath);
    }

    @Override
    protected IStatus doRun(final IProgressMonitor progressMonitor) throws Exception {
        changeset = repository.getVersionControlClient().createBranch(
            sourceServerPath,
            targetServerPath,
            version,
            VersionControlConstants.AUTHENTICATED_USER,
            description,
            null,
            new PolicyOverrideInfo("", new PolicyFailure[0]), //$NON-NLS-1$
            null);

        return Status.OK_STATUS;
    }

    public int getChangeset() {
        return changeset;
    }

}
