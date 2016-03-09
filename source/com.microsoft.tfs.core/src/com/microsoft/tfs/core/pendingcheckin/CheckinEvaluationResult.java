// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.pendingcheckin;

import com.microsoft.tfs.core.checkinpolicies.PolicyEvaluator;
import com.microsoft.tfs.core.checkinpolicies.PolicyEvaluatorState;
import com.microsoft.tfs.core.checkinpolicies.PolicyFailure;
import com.microsoft.tfs.util.Check;

/**
 * <p>
 * Contains the results of a checkin evaluation.
 * </p>
 *
 * @since TEE-SDK-10.1
 * @threadsafety conditionally thread-safe
 */
public class CheckinEvaluationResult {
    private final CheckinConflict[] conflicts;
    private final CheckinNoteFailure[] noteFailures;
    private final PolicyFailure[] policyFailures;
    private final PolicyEvaluatorState policyEvaluatorState;
    private final Exception policyEvaluationException;

    /**
     * @param conflicts
     *        the conflicts detected in the evaluation (must not be
     *        <code>null</code>)
     * @param noteFailures
     *        checkin notes that were not supplied correctly (must not be
     *        <code>null</code>)
     * @param policyFailures
     *        checkin policy evaluation failures (must not be <code>null</code>)
     * @param policyEvaluatorState
     *        the final state of the policy evaluator after evaluation (may be
     *        <code>null</code> if policies were not evaluated)
     * @param policyEvaluationException
     *        an exception generated during checkin policy evaluation (may be
     *        <code>null</code>).
     */
    public CheckinEvaluationResult(
        final CheckinConflict[] conflicts,
        final CheckinNoteFailure[] noteFailures,
        final PolicyFailure[] policyFailures,
        final PolicyEvaluatorState policyEvaluatorState,
        final Exception policyEvaluationException) {
        Check.notNull(conflicts, "conflicts"); //$NON-NLS-1$
        Check.notNull(noteFailures, "noteFailures"); //$NON-NLS-1$
        Check.notNull(policyFailures, "policyFailures"); //$NON-NLS-1$

        this.conflicts = conflicts;
        this.noteFailures = noteFailures;
        this.policyFailures = policyFailures;
        this.policyEvaluatorState = policyEvaluatorState;
        this.policyEvaluationException = policyEvaluationException;
    }

    /**
     * @return the conflicts found during evaluation. Do not modify the returned
     *         objects to ensure thread-safety.
     */
    public CheckinConflict[] getConflicts() {
        return conflicts;
    }

    /**
     * @return the check-in note failures found during evaluation. Do not modify
     *         the returned objects to ensure thread-safety.
     */
    public CheckinNoteFailure[] getNoteFailures() {
        return noteFailures;
    }

    /**
     * @return the check-in policy failures found during evaluation. Do not
     *         modify the returned objects to ensure thread-safety.
     */
    public PolicyFailure[] getPolicyFailures() {
        return policyFailures;
    }

    /**
     * @return the state of the {@link PolicyEvaluator} after policy evaluation,
     *         which may be <code>null</code> if check-in policies were not
     *         evaluated.
     */
    public PolicyEvaluatorState getPolicyEvaluatorState() {
        return policyEvaluatorState;
    }

    /**
     * @return the {@link Exception} that occurred during policy evaluation, if
     *         there was one (otherwise null).
     */
    public Exception getPolicyEvaluationException() {
        return policyEvaluationException;
    }
}
