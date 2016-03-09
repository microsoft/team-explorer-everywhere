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
import com.microsoft.tfs.core.clients.versioncontrol.MergeFlags;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.MergeCandidate;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.RecursionType;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.LocaleUtil;

public class GetMergeCandidatesCommand extends TFSConnectedCommand {
    private final TFSRepository repository;
    private final String sourcePath;
    private final String targetPath;
    private final RecursionType recursionType;
    private final MergeFlags mergeFlags;

    private MergeCandidate[] mergeCandidates;

    public GetMergeCandidatesCommand(
        final TFSRepository repository,
        final String sourcePath,
        final String targetPath,
        final RecursionType recursionType,
        final MergeFlags mergeFlags) {
        Check.notNull(repository, "repository"); //$NON-NLS-1$
        Check.notNull(sourcePath, "sourcePath"); //$NON-NLS-1$
        Check.notNull(targetPath, "targetPath"); //$NON-NLS-1$
        Check.notNull(recursionType, "recursionType"); //$NON-NLS-1$
        Check.notNull(mergeFlags, "mergeFlags"); //$NON-NLS-1$

        this.repository = repository;
        this.sourcePath = sourcePath;
        this.targetPath = targetPath;
        this.recursionType = recursionType;
        this.mergeFlags = mergeFlags;

        setConnection(repository.getConnection());
    }

    @Override
    public String getName() {
        final String messageFormat = Messages.getString("GetMergeCandidatesCommand.CommandTextFormat"); //$NON-NLS-1$
        return MessageFormat.format(messageFormat, sourcePath);
    }

    @Override
    public String getErrorDescription() {
        final String messageFormat = Messages.getString("GetMergeCandidatesCommand.ErrorTextFormat"); //$NON-NLS-1$
        return MessageFormat.format(messageFormat, sourcePath);
    }

    @Override
    public String getLoggingDescription() {
        final String messageFormat = Messages.getString("GetMergeCandidatesCommand.CommandTextFormat", LocaleUtil.ROOT); //$NON-NLS-1$
        return MessageFormat.format(messageFormat, sourcePath);
    }

    @Override
    protected IStatus doRun(final IProgressMonitor progressMonitor) throws Exception {
        mergeCandidates =
            repository.getWorkspace().getMergeCandidates(sourcePath, targetPath, recursionType, mergeFlags);

        return Status.OK_STATUS;
    }

    /**
     * @return the mergeCandidates
     */
    public MergeCandidate[] getMergeCandidates() {
        return mergeCandidates;
    }
}
