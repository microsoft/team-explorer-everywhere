// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.commands.vc;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import com.microsoft.tfs.client.common.Messages;
import com.microsoft.tfs.client.common.commands.TFSCommand;
import com.microsoft.tfs.client.common.repository.TFSRepository;
import com.microsoft.tfs.core.clients.versioncontrol.GetOptions;
import com.microsoft.tfs.core.clients.versioncontrol.PendChangesOptions;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.LockLevel;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.PendingChange;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.RecursionType;
import com.microsoft.tfs.core.util.FileEncoding;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.LocaleUtil;
import com.microsoft.tfs.util.StringUtil;

public class PromoteCandidateChangesCommand extends TFSCommand {
    private final TFSRepository repository;
    private final PendingChange[] candidates;

    private int deletesPended;
    private int addsPended;

    public PromoteCandidateChangesCommand(final TFSRepository repository, final PendingChange[] candidates) {
        Check.notNull(repository, "repository"); //$NON-NLS-1$
        Check.notNull(candidates, "candidates"); //$NON-NLS-1$

        this.repository = repository;
        this.candidates = candidates;
    }

    @Override
    public String getName() {
        return Messages.getString("PromoteCandidateChangesCommand.CommandText"); //$NON-NLS-1$
    }

    @Override
    public String getErrorDescription() {
        return Messages.getString("PromoteCandidateChangesCommand.ErrorText"); //$NON-NLS-1$
    }

    @Override
    public String getLoggingDescription() {
        return Messages.getString("PromoteCandidateChangesCommand.CommandText", LocaleUtil.ROOT); //$NON-NLS-1$
    }

    @Override
    public IStatus doRun(final IProgressMonitor progressMonitor) throws Exception {
        final List<String> candidateAdds = new ArrayList<String>();
        final List<String> candidateDeletes = new ArrayList<String>();

        for (final PendingChange change : candidates) {
            if (!StringUtil.isNullOrEmpty(change.getLocalItem())) {
                if (change.isAdd()) {
                    candidateAdds.add(change.getLocalItem());
                } else if (change.isDelete()) {
                    candidateDeletes.add(change.getLocalItem());
                }
            }
        }

        if (candidateAdds.size() > 0) {
            addsPended = repository.getWorkspace().pendAdd(
                candidateAdds.toArray(new String[candidateAdds.size()]),
                false /* recursive */,
                FileEncoding.UTF_8,
                LockLevel.UNCHANGED,
                GetOptions.NONE,
                PendChangesOptions.APPLY_LOCAL_ITEM_EXCLUSIONS);
        }

        if (candidateDeletes.size() > 0) {
            deletesPended = repository.getWorkspace().pendDelete(
                candidateDeletes.toArray(new String[candidateDeletes.size()]),
                RecursionType.NONE,
                LockLevel.UNCHANGED,
                GetOptions.NONE,
                PendChangesOptions.NONE);
        }

        return Status.OK_STATUS;
    }

    public int getAddsPended() {
        return addsPended;
    }

    public int getDeletesPended() {
        return deletesPended;
    }
}
