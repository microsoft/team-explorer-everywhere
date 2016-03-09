// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.checkinpolicies;

import java.text.MessageFormat;

import com.microsoft.tfs.core.Messages;
import com.microsoft.tfs.util.TypesafeEnum;

/**
 * <p>
 * Represents the state of an object that can perform checkin policy
 * evaluations.
 * </p>
 *
 * @since TEE-SDK-10.1
 * @threadsafety immutable
 */
public class PolicyEvaluatorState extends TypesafeEnum {
    /**
     * Policies could not be loaded.
     */
    public final static PolicyEvaluatorState POLICIES_LOAD_ERROR = new PolicyEvaluatorState(0);

    /**
     * Evaluator has been constructed, and policies may have been loaded, but
     * they have not been evaluated.
     */
    public final static PolicyEvaluatorState UNEVALUATED = new PolicyEvaluatorState(1);

    /**
     * Policies have been evaluated. There may be failures to retrieve.
     */
    public final static PolicyEvaluatorState EVALUATED = new PolicyEvaluatorState(2);

    /**
     * Policies evaluation was cancelled by the user.
     */
    public final static PolicyEvaluatorState CANCELLED = new PolicyEvaluatorState(3);

    private PolicyEvaluatorState(final int value) {
        super(value);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        if (this == POLICIES_LOAD_ERROR) {
            return Messages.getString("PolicyEvaluatorState.LoadError"); //$NON-NLS-1$
        } else if (this == UNEVALUATED) {
            return Messages.getString("PolicyEvaluatorState.Unevaluated"); //$NON-NLS-1$
        } else if (this == EVALUATED) {
            return Messages.getString("PolicyEvaluatorState.Evaluated"); //$NON-NLS-1$
        } else if (this == CANCELLED) {
            return Messages.getString("PolicyEvaluatorState.Cancelled"); //$NON-NLS-1$
        } else {
            throw new RuntimeException(
                MessageFormat.format("No name known for enumeration value {0}", Integer.toString(getValue()))); //$NON-NLS-1$
        }
    }
}
