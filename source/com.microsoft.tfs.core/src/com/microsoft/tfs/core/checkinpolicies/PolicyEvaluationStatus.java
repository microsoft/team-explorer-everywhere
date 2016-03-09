// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.checkinpolicies;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.microsoft.tfs.core.checkinpolicies.events.PolicyStateChangedEvent;
import com.microsoft.tfs.core.checkinpolicies.events.PolicyStateChangedListener;
import com.microsoft.tfs.core.memento.Memento;
import com.microsoft.tfs.core.pendingcheckin.PendingCheckin;
import com.microsoft.tfs.core.pendingcheckin.filters.FilterPendingCheckin;
import com.microsoft.tfs.core.pendingcheckin.filters.ScopeFilter;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.Closable;
import com.microsoft.tfs.util.listeners.ListenerList;
import com.microsoft.tfs.util.listeners.ListenerRunnable;
import com.microsoft.tfs.util.listeners.StandardListenerList;

/**
 * <p>
 * Contains status about a policy that is ready to be evaluated (or possibly
 * already has). These objects are used exclusively by {@link PolicyEvaluator}
 * to evaluate policies and report status.
 * </p>
 * <p>
 * This class is thread-safe, but caller threads must synchronize access to
 * calls of {@link #initialize(PendingCheckin, PolicyContext)},
 * {@link #evaluate(PolicyContext)}, and {@link #getFailures()} if they want
 * meaningful failure information.
 * </p>
 *
 * @since TEE-SDK-10.1
 * @threadsafety conditionally thread-safe
 */
public class PolicyEvaluationStatus implements Closable {
    private static final Log log = LogFactory.getLog(PolicyEvaluationStatus.class);

    private PolicyInstance policy;
    private PolicyFailure[] failures = new PolicyFailure[0];
    private String[] scopeExpressions;
    private int priority;

    private final ListenerList policyStateChangedEventListeners = new StandardListenerList();

    private final PolicyStateChangedListener savedPolicyStateChangedListener = new PolicyStateChangedListener() {
        @Override
        public void onPolicyStateChanged(final PolicyStateChangedEvent e) {
            PolicyEvaluationStatus.this.onPolicyStateChanged(e);
        }
    };

    /**
     * Creates a {@link PolicyEvaluationStatus} for the given policy. The policy
     * is not initialized or evaluated automatically.
     *
     * @param policy
     *        the policy to wrap (must not be <code>null</code>)
     * @param priority
     *        the evaluation priority of this status (lower numbers evaluate
     *        before higher numbers).
     * @param scopeExpressions
     *        an array of strings that are regular expressions that define the
     *        scope of this policy (the server paths it affects). An empty array
     *        means all paths.
     */
    public PolicyEvaluationStatus(final PolicyInstance policy, final int priority, final String[] scopeExpressions) {
        Check.notNull(policy, "policy"); //$NON-NLS-1$
        Check.notNull(scopeExpressions, "scopeExpressions"); //$NON-NLS-1$

        this.policy = policy;
        this.priority = priority;
        this.scopeExpressions = scopeExpressions;
        this.policy.addPolicyStateChangedListener(savedPolicyStateChangedListener);
    }

    /**
     * Adds a listener of the {@link PolicyStateChangedEvent}, which is fired
     * when the {@link PolicyInstance} in this status object changes its state.
     *
     * @param listener
     *        the listener to add (must not be <code>null</code>)
     */
    public void addPolicyStateChangedEventListener(final PolicyStateChangedListener listener) {
        policyStateChangedEventListeners.addListener(listener);
    }

    /**
     * Removes a listener previously added via
     * {@link #addPolicyStateChangedEventListener(PolicyStateChangedListener)}.
     *
     * @param listener
     *        the listener to remove (must not be <code>null</code>)
     */
    public void removePolicyStateChangedEventListener(final PolicyStateChangedListener listener) {
        policyStateChangedEventListeners.removeListener(listener);
    }

    /**
     * Called by {@link #savedPolicyStateChangedListener} to handle the actual
     * event.
     *
     * @param e
     *        the event arguments
     */
    private synchronized void onPolicyStateChanged(final PolicyStateChangedEvent e) {
        /*
         * Save a copy of the failures and reflect the event up through our
         * event. We send the copy because the policy instance may make changes
         * to its array.
         */
        final PolicyFailure[] newFailures = e.getFailures();
        failures = new PolicyFailure[newFailures.length];
        for (int i = 0; i < failures.length; i++) {
            failures[i] = newFailures[i];
        }

        policyStateChangedEventListeners.foreachListener(new ListenerRunnable() {
            @Override
            public boolean run(final Object listener) throws Exception {
                ((PolicyStateChangedListener) listener).onPolicyStateChanged(
                    new PolicyStateChangedEvent(e.getEventSource(), newFailures, e.getPolicy()));
                return true;
            }
        });
    }

    /**
     * Initializes (or re-initializes) the evaluatable policy instance. Runtime
     * exceptions from the policy instance's
     * {@link PolicyInstance#initialize(PendingCheckin, PolicyContext)} method
     * are thrown from here.
     *
     * @throws IllegalStateException
     *         if {@link #close()} has been called.
     * @throws Throwable
     *         if
     *         {@link PolicyInstance#initialize(PendingCheckin, PolicyContext)}
     *         threw
     */
    public synchronized void initialize(PendingCheckin pendingCheckin, final PolicyContext context) {
        Check.notNull(pendingCheckin, "pendingCheckin"); //$NON-NLS-1$
        Check.notNull(context, "context"); //$NON-NLS-1$

        if (policy == null) {
            throw new IllegalStateException("This PolicyEvaluationStatus object has been closed."); //$NON-NLS-1$
        }

        /*
         * If the policy's configuration requires filtering paths by scope,
         * build a wrapper pending checkin and pass that into the policy. The
         * wrapper only interferes with getting the checked pending changes, and
         * all event add/remove methods are simply delegated to the original
         * object.
         *
         * This makes scoped changes transparent to the policy, but preserves
         * the lifecycle interaction with the original pending checkin.
         */

        if (scopeExpressions.length > 0) {
            pendingCheckin = new FilterPendingCheckin(pendingCheckin, new ScopeFilter(scopeExpressions));
        }

        // Initialization errors can be thrown from here.
        policy.initialize(pendingCheckin, context);
    }

    /**
     * Evaluates the policy instance in this status. Call {@link #getFailures()}
     * when this method returns to obtain any failures detected.
     *
     * @throws PolicyEvaluationCancelledException
     *         if the user canceled the evaluation of the policy.
     * @param policyContext
     *        contextual settings that may include information about the user
     *        interface, etc. (must not be <code>null</code>)
     * @throws IllegalStateException
     *         if {@link #close()} has been called.
     */
    public synchronized void evaluate(final PolicyContext policyContext) throws PolicyEvaluationCancelledException {
        Check.notNull(policyContext, "policyContext"); //$NON-NLS-1$

        if (policy == null) {
            throw new IllegalStateException("This PolicyEvaluationStatus object has been closed."); //$NON-NLS-1$
        }

        try {
            final PolicyFailure[] failures = policy.evaluate(policyContext);

            this.failures = (failures != null) ? failures : new PolicyFailure[0];

            /*
             * Make sure the policy didn't sneak in any null items that might
             * upset our users.
             */
            for (int i = 0; i < this.failures.length; i++) {
                if (this.failures[i] == null) {
                    this.failures[i] = new PolicyFailure(
                        "Policy Internal Error: Policy returned a null failure", //$NON-NLS-1$
                        policy);
                }
            }
        } catch (final PolicyEvaluationCancelledException e) {
            // Rethrow.
            throw e;
        } catch (final Exception e) {
            /*
             * These can be any exceptions thrown by the policy implementation.
             * Convert them into a proper failure.
             */
            log.error("Error in checkin policy", e); //$NON-NLS-1$

            failures = new PolicyFailure[] {
                new PolicyExceptionFailure(policy, policyContext, e)
            };
        }
    }

    /**
     * @return any failures returned by this policy, if it has been run.
     */
    public synchronized PolicyFailure[] getFailures() {
        return failures;
    }

    /**
     * Gets the type of policy this status is for. Throws if {@link #close()}
     * has been called.
     *
     * @return the type of policy this status wraps.
     *
     * @throws IllegalStateException
     *         if {@link #close()} has been called.
     */
    public synchronized PolicyType getPolicyType() {
        if (policy == null) {
            throw new IllegalStateException("This PolicyEvaluationStatus object has been closed."); //$NON-NLS-1$
        }

        return policy.getPolicyType();
    }

    /**
     * Once a {@link PolicyEvaluationStatus} is closed, you cannot invoke
     * {@link #getPolicy()}, {@link #initialize(PendingCheckin, PolicyContext)}
     * or {@link #evaluate(PolicyContext)} on it.
     *
     * @see com.microsoft.tfs.util.Closable#close()
     */
    @Override
    public synchronized void close() {
        if (policy != null) {
            /*
             * Stop listening to events fired by the policy.
             */
            policy.removePolicyStateChangedListener(savedPolicyStateChangedListener);

            policy.close();
            policy = null;
        }
    }

    /**
     * Updates this status for re-use with a new configuration. This happens
     * every time {@link PolicyEvaluator} reloads policies.
     *
     * @param priority
     *        the new priority.
     * @param scopeExpressions
     *        the regular expression strings that define the server items this
     *        policy evaluates (an empty array means all paths) (must not be
     *        <code>null</code>)
     * @param configurationMemento
     *        the policy definition's configuration memento.
     */
    public synchronized void update(
        final int priority,
        final String[] scopeExpressions,
        final Memento configurationMemento) {
        if (policy == null) {
            throw new IllegalStateException("This PolicyEvaluationStatus object has been closed."); //$NON-NLS-1$
        }

        this.priority = priority;
        this.scopeExpressions = scopeExpressions;

        policy.loadConfiguration(configurationMemento);
    }

    /**
     * @return the policy this status wraps.
     */
    public synchronized PolicyInstance getPolicy() {
        if (policy == null) {
            throw new IllegalStateException("This PolicyEvaluationStatus object has been closed."); //$NON-NLS-1$
        }

        return policy;
    }

    /**
     * @return the numeric priority by which this policy is evaluated (lower
     *         numbers evaluate before higher numbers).
     */
    public synchronized int getPriority() {
        return priority;
    }

    /**
     * @return the regular expression strings that define the server items this
     *         policy evaluates (an empty array means all paths).
     */
    public String[] getScopeExpressions() {
        return scopeExpressions;
    }
}
