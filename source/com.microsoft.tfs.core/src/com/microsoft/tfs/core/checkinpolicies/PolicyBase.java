// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.checkinpolicies;

import com.microsoft.tfs.core.Messages;
import com.microsoft.tfs.core.checkinpolicies.events.PolicyStateChangedEvent;
import com.microsoft.tfs.core.checkinpolicies.events.PolicyStateChangedListener;
import com.microsoft.tfs.core.clients.versioncontrol.events.EventSource;
import com.microsoft.tfs.core.memento.Memento;
import com.microsoft.tfs.core.pendingcheckin.PendingCheckin;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.listeners.ListenerList;
import com.microsoft.tfs.util.listeners.ListenerRunnable;
import com.microsoft.tfs.util.listeners.StandardListenerList;

/**
 * <p>
 * A convenient base class for checkin policy implementers.
 * </p>
 * <h1>Events</h1>
 * <p>
 * Policy implementations can fire an event ({@link PolicyStateChangedEvent}) to
 * indicate to upper layers in the framework that that policy has re-evaluated
 * itself, and has built a new set of failures (perhaps an empty set).
 * <p>
 * Simple policies that do not respond to external events like timers or events
 * from other components do not need to fire this event. More complicated
 * policies might re-evaluate their rules because of events fired by other
 * components , so methods to register listeners and fire the event have been
 * implemented for your convenience.
 * </p>
 * <p>
 * To fire the event, simply invoke
 * {@link #firePolicyStateChangedEvent(PolicyFailure[])} with the new array of
 * failures (an empty array signifies no failures).
 * </p>
 * <h2>Event Example</h2>
 * <p>
 * A policy for the Plug-in for Eclipse wants to return policy failures if unit
 * tests for the pending checkin do not pass. In this example, the unit tests
 * are managed by a long-running component that fires events when the user runs
 * the tests.
 * <ol>
 *
 * <li>Eclipse starts (no unit tests have been run) and the user views the
 * Check-in Policies results view.</li>
 *
 * <li>The framework loads the policy and calls
 * {@link #initialize(PendingCheckin, PolicyContext)} with the current pending
 * check-in.</li>
 *
 * <li>{@link #initialize(PendingCheckin, PolicyContext)} finds the running unit
 * test component, and registers itself to handle test pass/failure events.</li>
 *
 * <li>The policy immediately fires the {@link PolicyStateChangedEvent} with
 * failures describing the current test state ("Unit tests have not completed").
 * These failures prevent prevent a check-in.</li>
 *
 * <li>If the user attempts to check-in his changes, the policy framework
 * invokes {@link #evaluate(PolicyContext)} on the policy, and it re-queries the
 * test component for results, returning the appropriate failures.</li>
 *
 * <li>Some time later, when the user runs the unit tests, the pass/fail event
 * fires, and the policy handles this event by firing its own
 * {@link PolicyStateChangedEvent} with a new set of failures (or an empty array
 * when all tests pass).</li>
 *
 * </ol>
 * </p>
 * <p>
 * Later, when {@link #close()} is called, event handlers are removed from the
 * unit test component.
 * </p>
 * <h1>Declaring Editable</h1>
 * <p>
 * Make sure to override {@link #canEdit()} to return false if your policy does
 * not support user configuration (i.e. it has no settings to configure). If
 * {@link #canEdit()} returns true (the default), the "Edit" button or UI
 * element is enabled when a policy is displayed in user interfaces. If it
 * returns false, the user is prevented from editing the policy definition (
 * {@link #edit(PolicyEditArgs)} will not be invoked).
 * </p>
 *
 * <h1>Thread Safety</h1>
 * <p>
 * This class is thread-safe, but implementations are not required to be. The
 * policy framework will not invoke policy methods from multiple threads
 * concurrently. Policies that run in the Eclipse environment and interact with
 * other components or Eclipse plug-ins with events should ensure their error
 * handlers interact with policy data in a thread-safe way.
 * </p>
 * <p>
 * The framework does not guarantee which thread will be used to invoke
 * {@link #evaluate(PolicyContext)}. Implementations must marshall any
 * thread-sensitive work in this method (for example, user interface work in
 * SWT/Eclipse) to the correct thread manually. See the thread policy notice in
 * {@link PolicyInstance} for important information.
 * </p>
 *
 * @since TEE-SDK-10.1
 * @threadsafety thread-compatible
 */
public abstract class PolicyBase implements PolicyInstance {
    /**
     * The checkin we are evaluating.
     */
    private volatile PendingCheckin pendingCheckin;

    /**
     * The event listeners invoked when
     * {@link #firePolicyStateChangedEvent(PolicyStateChangedEvent)} is called.
     */
    private final ListenerList stateChangedEventListeners = new StandardListenerList();

    /**
     * <p>
     * All policy implementations must include a zero-argument constructor, so
     * they can be dynamically created by the policy framework.
     * </p>
     * <p>
     * Policies should prefer to hook up evaluation-specific events to other
     * objects in {@link #initialize(PendingCheckin, PolicyContext)} instead of
     * in the constructor, and unhook them in {@link #close}. This is because
     * policy instances are constructed during configuration, when no evaluation
     * will be performed. Policies should also assure that events are only
     * hooked up as many times as needed in
     * {@link #initialize(PendingCheckin, PolicyContext)}, since it may be
     * called multiple times during a policy instance's lifetime.
     * </p>
     */
    public PolicyBase() {
        super();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addPolicyStateChangedListener(final PolicyStateChangedListener listener) {
        Check.notNull(listener, "listener"); //$NON-NLS-1$
        stateChangedEventListeners.addListener(listener);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void removePolicyStateChangedListener(final PolicyStateChangedListener listener) {
        Check.notNull(listener, "listener"); //$NON-NLS-1$
        stateChangedEventListeners.removeListener(listener);
    }

    /**
     * Fires the {@link PolicyStateChangedEvent} with the given event failures,
     * filling in the other {@link PolicyStateChangedEvent} fields
     * automatically.
     * <p>
     * This is the preferred way to fire the {@link PolicyStateChangedEvent}.
     *
     * @param failures
     *        the failures to include with other event information (may be null:
     *        will be converted to an empty failure array).
     */
    protected void firePolicyStateChangedEvent(final PolicyFailure[] failures) {
        // Convert null failures to empty array.
        final PolicyFailure[] newFailures = (failures == null) ? new PolicyFailure[0] : failures;

        firePolicyStateChangedEvent(new PolicyStateChangedEvent(EventSource.newFromHere(), newFailures, this));
    }

    /**
     * Fires the given {@link PolicyStateChangedEvent}, which must be
     * constructed manually.
     * <p>
     * {@link #firePolicyStateChangedEvent(PolicyFailure[])} fills in some
     * fields automatically, and is preferred to this method.
     *
     * @param event
     *        the event to fire (must not be <code>null</code>)
     * @see #firePolicyStateChangedEvent(PolicyFailure[])
     */
    protected void firePolicyStateChangedEvent(final PolicyStateChangedEvent event) {
        Check.notNull(event, "event"); //$NON-NLS-1$

        stateChangedEventListeners.foreachListener(new ListenerRunnable() {
            @Override
            public boolean run(final Object listener) throws Exception {
                ((PolicyStateChangedListener) listener).onPolicyStateChanged(event);
                return true;
            }
        });
    }

    /**
     * @return the pending checkin this {@link PolicyInstance} was configured
     *         with. Never returns null.
     * @throws IllegalStateException
     *         if {@link #initialize(PendingCheckin, PolicyContext)} has not yet
     *         been called on this object.
     */
    protected PendingCheckin getPendingCheckin() {
        if (pendingCheckin == null) {
            throw new IllegalStateException(Messages.getString("PolicyBase.PolicyInstanceHasNotBeenInitialized")); //$NON-NLS-1$
        }

        return pendingCheckin;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean canEdit() {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void activate(final PolicyFailure failure, final PolicyContext context) {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void displayHelp(final PolicyFailure failure, final PolicyContext context) {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void initialize(final PendingCheckin pendingCheckin, final PolicyContext context) {
        Check.notNull(pendingCheckin, "pendingCheckin"); //$NON-NLS-1$
        this.pendingCheckin = pendingCheckin;
    }

    /**
     * Removes all event listeners from this policy instance. Do not use this
     * object after calling {@link #close()}. Extending classes should make sure
     * to call super.close() if they override this method.
     */
    @Override
    public void close() {
        synchronized (stateChangedEventListeners) {
            stateChangedEventListeners.clear();
        }
    }

    /*
     * Derived classes must implement this methods.
     */

    /**
     * {@inheritDoc}
     */
    @Override
    public abstract void loadConfiguration(Memento configurationMemento);

    /**
     * {@inheritDoc}
     */
    @Override
    public abstract void saveConfiguration(Memento configurationMemento);

    /**
     * {@inheritDoc}
     */
    @Override
    public abstract boolean edit(PolicyEditArgs policyEditArgs);

    /**
     * {@inheritDoc}
     */
    @Override
    public abstract PolicyType getPolicyType();

    /**
     * {@inheritDoc}
     */
    @Override
    public abstract PolicyFailure[] evaluate(PolicyContext context) throws PolicyEvaluationCancelledException;
}
