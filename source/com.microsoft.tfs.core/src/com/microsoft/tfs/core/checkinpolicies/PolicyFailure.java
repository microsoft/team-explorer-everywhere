// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.checkinpolicies;

import com.microsoft.tfs.core.Messages;
import com.microsoft.tfs.util.Check;

/**
 * <p>
 * Contains information about a single failure detected during checkin policy
 * evaluation ({@link PolicyInstance#evaluate(PolicyContext)}). Once produced, a
 * failure may be examined by UI code, sent with events, queried for help text,
 * or used in other ways.
 * </p>
 * <p>
 * Implementations should be immutable or thread-safe.
 * </p>
 *
 * @since TEE-SDK-10.1
 * @threadsafety immutable, thread-safe
 */
public class PolicyFailure {
    /**
     * The text that explains the failure to the end user.
     */
    private final String message;

    /**
     * The {@link PolicyInstance} object that detected and created the failure.
     */
    private final PolicyInstance policy;

    /**
     * Creates a {@link PolicyFailure} with the given message for the given
     * policy.
     *
     * @param message
     *        the message that describes the failure. If null, a generic message
     *        is displayed.
     * @param policy
     *        the policy that detected and created the failure (must not be
     *        <code>null</code>)
     */
    public PolicyFailure(final String message, final PolicyInstance policy) {
        Check.notNull(policy, "policy"); //$NON-NLS-1$

        if (message == null) {
            this.message = Messages.getString("PolicyFailure.PolicyFailedButDidNotProvideFailureMessage"); //$NON-NLS-1$
        } else {
            this.message = message;
        }

        this.policy = policy;
    }

    /**
     * @return the text that describes the failure (may be null).
     */
    public String getMessage() {
        return message;
    }

    /**
     * @return the policy that detected and created this failure.
     */
    public PolicyInstance getPolicy() {
        return policy;
    }
}
