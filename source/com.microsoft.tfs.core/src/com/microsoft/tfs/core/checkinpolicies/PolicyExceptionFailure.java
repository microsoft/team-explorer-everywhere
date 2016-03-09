// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.checkinpolicies;

import java.text.MessageFormat;

import com.microsoft.tfs.core.Messages;
import com.microsoft.tfs.util.Check;

/**
 * <p>
 * A type of policy evaluation failure caused by an uncaught exception in the
 * framework. Policy implementations should not return these, so the
 * constructors are protected.
 * </p>
 *
 * @since TEE-SDK-10.1
 * @threadsafety immutable
 */
public class PolicyExceptionFailure extends PolicyFailure {
    /**
     * Creates a {@link PolicyExceptionFailure}
     *
     * @param policy
     *        the policy (must not be <code>null</code>)
     * @param context
     *        the context (must not be <code>null</code>)
     * @param exception
     *        the exception (must not be <code>null</code>)
     */
    protected PolicyExceptionFailure(
        final PolicyInstance policy,
        final PolicyContext context,
        final Exception exception) {
        super(makeMessage(policy, exception), policy);

        Check.notNull(context, "context"); //$NON-NLS-1$
    }

    /**
     * Utility for constructor.
     */
    private static String makeMessage(final PolicyInstance policy, final Exception exception) {
        Check.notNull(policy, "policy"); //$NON-NLS-1$
        Check.notNull(exception, "exception"); //$NON-NLS-1$

        return MessageFormat.format(
            Messages.getString("PolicyExceptionFailure.InternalErrorInPolicyFormat"), //$NON-NLS-1$
            policy.getPolicyType().getName(),
            exception.getLocalizedMessage());
    }
}
