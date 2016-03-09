// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.commands;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import com.microsoft.tfs.client.common.Messages;
import com.microsoft.tfs.core.checkinpolicies.PolicyContext;
import com.microsoft.tfs.core.checkinpolicies.PolicyEvaluationCancelledException;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Workspace;
import com.microsoft.tfs.core.pendingcheckin.CheckinEvaluationOptions;
import com.microsoft.tfs.core.pendingcheckin.CheckinEvaluationResult;
import com.microsoft.tfs.core.pendingcheckin.PendingCheckin;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.LocaleUtil;

/**
 * Evalutes an entire {@link PendingCheckin} object before it is checked in. In
 * general, dialogs aren't raised for errors (the caller gets the
 * {@link CheckinEvaluationResult} and raises dialogs there), but check-in
 * policies are given a task monitor which will interact with the command's
 * progress monitor (if run as a UI job, which is desired).
 *
 * @threadsafety thread-compatible
 */
public class EvaluatePendingCheckinCommand extends TFSConnectedCommand {
    private final Workspace workspace;
    private final CheckinEvaluationOptions options;
    private final PendingCheckin pendingCheckin;
    private final PolicyContext context;

    private volatile CheckinEvaluationResult result;

    public EvaluatePendingCheckinCommand(
        final Workspace workspace,
        final CheckinEvaluationOptions options,
        final PendingCheckin pendingCheckin,
        final PolicyContext context) {
        Check.notNull(workspace, "workspace"); //$NON-NLS-1$
        Check.notNull(options, "options"); //$NON-NLS-1$
        Check.notNull(pendingCheckin, "pendingCheckin"); //$NON-NLS-1$
        Check.notNull(context, "context"); //$NON-NLS-1$

        this.workspace = workspace;
        this.options = options;
        this.pendingCheckin = pendingCheckin;
        this.context = context;

        setCancellable(true);
        setConnection(workspace.getClient().getConnection());
    }

    @Override
    public String getName() {
        return (Messages.getString("EvaluatePendingCheckinCommand.CommandText")); //$NON-NLS-1$
    }

    @Override
    public String getErrorDescription() {
        return (Messages.getString("EvaluatePendingCheckinCommand.ErrorText")); //$NON-NLS-1$
    }

    @Override
    public String getLoggingDescription() {
        return (Messages.getString("EvaluatePendingCheckinCommand.CommandText", LocaleUtil.ROOT)); //$NON-NLS-1$
    }

    @Override
    protected IStatus doRun(final IProgressMonitor progressMonitor) throws Exception {
        try {
            result = workspace.evaluateCheckIn(options, pendingCheckin, context);
            return Status.OK_STATUS;
        } catch (final PolicyEvaluationCancelledException e) {
            return Status.CANCEL_STATUS;
        }
    }

    public CheckinEvaluationResult getEvaluationResult() {
        return result;
    }
}