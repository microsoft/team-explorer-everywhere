// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.commands;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import com.microsoft.tfs.client.common.Messages;
import com.microsoft.tfs.core.checkinpolicies.PolicyContext;
import com.microsoft.tfs.core.checkinpolicies.PolicyEvaluationCancelledException;
import com.microsoft.tfs.core.checkinpolicies.PolicyEvaluator;
import com.microsoft.tfs.core.checkinpolicies.PolicyFailure;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.LocaleUtil;

/**
 * Causes the given {@link PolicyEvaluator} to be re-evaluated with the given
 * {@link PolicyContext}, optionally forcing it to reload policy definitions
 * from the server. This command should be run with UI, so check-in policies can
 * communicate their status.
 *
 * @threadsafety thread-compatible
 */
public class EvaluateCheckinPoliciesCommand extends TFSCommand {
    private final PolicyEvaluator evaluator;
    private final PolicyContext context;
    private final boolean reload;

    private volatile PolicyFailure[] failures;

    public EvaluateCheckinPoliciesCommand(
        final PolicyEvaluator evaluator,
        final PolicyContext context,
        final boolean reload) {
        Check.notNull(evaluator, "evaluator"); //$NON-NLS-1$
        Check.notNull(context, "context"); //$NON-NLS-1$

        this.evaluator = evaluator;
        this.context = context;
        this.reload = reload;

        setCancellable(true);
    }

    @Override
    public String getName() {
        return (Messages.getString("EvaluateCheckinPoliciesCommand.CommandText")); //$NON-NLS-1$
    }

    @Override
    public String getErrorDescription() {
        return (Messages.getString("EvaluateCheckinPoliciesCommand.ErrorText")); //$NON-NLS-1$
    }

    @Override
    public String getLoggingDescription() {
        return (Messages.getString("EvaluateCheckinPoliciesCommand.CommandText", LocaleUtil.ROOT)); //$NON-NLS-1$
    }

    @Override
    protected IStatus doRun(final IProgressMonitor progressMonitor) throws Exception {
        try {
            if (reload) {
                failures = evaluator.reloadAndEvaluate(context);
            } else {
                failures = evaluator.evaluate(context);
            }
            return Status.OK_STATUS;
        } catch (final PolicyEvaluationCancelledException e) {
            return Status.CANCEL_STATUS;
        }
    }

    public PolicyFailure[] getFailures() {
        return failures;
    }
}