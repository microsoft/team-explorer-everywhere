// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.checkinpolicies;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.microsoft.tfs.core.Messages;
import com.microsoft.tfs.core.checkinpolicies.events.PolicyEvaluatorStateChangedEvent;
import com.microsoft.tfs.core.checkinpolicies.events.PolicyEvaluatorStateChangedListener;
import com.microsoft.tfs.core.checkinpolicies.events.PolicyLoadErrorEvent;
import com.microsoft.tfs.core.checkinpolicies.events.PolicyLoadErrorListener;
import com.microsoft.tfs.core.checkinpolicies.events.PolicyStateChangedEvent;
import com.microsoft.tfs.core.checkinpolicies.events.PolicyStateChangedListener;
import com.microsoft.tfs.core.checkinpolicies.internal.LoadErrorPolicy;
import com.microsoft.tfs.core.checkinpolicies.internal.PolicyEvaluationStatusComparator;
import com.microsoft.tfs.core.clients.CoreClientEvent;
import com.microsoft.tfs.core.clients.versioncontrol.VersionControlClient;
import com.microsoft.tfs.core.clients.versioncontrol.events.EventSource;
import com.microsoft.tfs.core.exceptions.TECoreException;
import com.microsoft.tfs.core.pendingcheckin.PendingCheckin;
import com.microsoft.tfs.core.pendingcheckin.events.AffectedTeamProjectsChangedEvent;
import com.microsoft.tfs.core.pendingcheckin.events.AffectedTeamProjectsChangedListener;
import com.microsoft.tfs.core.pendingcheckin.events.CheckedPendingChangesChangedEvent;
import com.microsoft.tfs.core.pendingcheckin.events.CheckedPendingChangesChangedListener;
import com.microsoft.tfs.core.pendingcheckin.events.CheckedWorkItemsChangedEvent;
import com.microsoft.tfs.core.pendingcheckin.events.CheckedWorkItemsChangedListener;
import com.microsoft.tfs.core.pendingcheckin.events.CommentChangedEvent;
import com.microsoft.tfs.core.pendingcheckin.events.CommentChangedListener;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.Closable;
import com.microsoft.tfs.util.listeners.ListenerList;
import com.microsoft.tfs.util.listeners.ListenerRunnable;
import com.microsoft.tfs.util.listeners.StandardListenerList;
import com.microsoft.tfs.util.tasks.TaskMonitor;
import com.microsoft.tfs.util.tasks.TaskMonitorService;

/**
 * <p>
 * A long-lived object that manages evaluation of check-in policies. Most UI
 * controls will use a {@link PolicyEvaluator} to drive policy evaluation (and
 * user-initiated re-evaluation). Clients like the command-line client will use
 * one (for a short time) for evaluation.
 * </p>
 * <p>
 * After a {@link PolicyEvaluator} is constructed, it is initialized with a
 * pending checkin via {@link #setPendingCheckin(PendingCheckin)}.
 * </p>
 * <p>
 * Then, evaluation is done via the public {@link #evaluate(PolicyContext)}
 * method, or {@link #reloadAndEvaluate(PolicyContext)} if the policy
 * definitions and implementations should be re-loaded from the server. Policy
 * failures can be retrieved after evaluation with {@link #getFailures()}, or by
 * attaching listeners with
 * {@link #addPolicyStateChangedListener(PolicyStateChangedListener)}.
 * </p>
 * <p>
 * The general status of the object can be watched through events (
 * {@link #addPolicyEvaluatorStateChangedListener(PolicyEvaluatorStateChangedListener)}
 * ) or with {@link #getPolicyEvaluatorState()}. See
 * {@link PolicyEvaluatorState} for information on possible states.
 * </p>
 * <p>
 * A new pending check-in can be assigned at any time with
 * {@link #setPendingCheckin(PendingCheckin)}. The evaluator resets its state,
 * and is ready to evaluate again.
 * </p>
 * <p>
 * Call the {@link #close()} method when done with the evaluator, so it can
 * close its policy implementations (which may have large objects to release or
 * events to unhook).
 * </p>
 * <p>
 * <b>Context Notes</b>
 * </p>
 * <p>
 * Callers don't need to supply a {@link TaskMonitor} in the policy context they
 * pass to {@link #reloadAndEvaluate(PolicyContext)} or
 * {@link #evaluate(PolicyContext)}. The {@link PolicyEvaluator} will put one in
 * the context when it runs the policy instances.
 * </p>
 * <p>
 * <b>Handling Load Errors</b>
 * </p>
 * <p>
 * The evaluator doesn't normally throw exceptions during
 * {@link #evaluate(PolicyContext)} or {@link #reloadAndEvaluate(PolicyContext)}
 * if a configured policy could not be loaded from disk. Instead, the
 * evaluator's state is set to {@link PolicyEvaluatorState#POLICIES_LOAD_ERROR}
 * and the caller can test for this state with
 * {@link #getPolicyEvaluatorState()}. If the caller is interested in the load
 * error details (including exception data), it can register an event listener
 * via {@link #addPolicyLoadErrorListener(PolicyLoadErrorListener)} which is
 * fired as the load error happens.
 * </p>
 *
 * @see PolicyEvaluatorState
 * @since TEE-SDK-10.1
 * @threadsafety thread-safe
 */
public class PolicyEvaluator implements Closable {
    private static final Log log = LogFactory.getLog(PolicyEvaluator.class);

    private static final PolicyEvaluationStatusComparator policyStatusComparator =
        new PolicyEvaluationStatusComparator();

    /**
     * Contains {@link PolicyEvaluationStatus} objects, which contain the actual
     * instances we evaluate.
     */
    private List<PolicyEvaluationStatus> policyEvaluationStatuses = new ArrayList<PolicyEvaluationStatus>();

    private final VersionControlClient client;
    private final PolicyLoader policyLoader;
    private final ListenerList evaluatorStateChangedEventListeners = new StandardListenerList();
    private final ListenerList policyStateChangedEventListeners = new StandardListenerList();
    private final ListenerList policyLoadErrorEventListeners = new StandardListenerList();

    private PendingCheckin pendingCheckin;

    private final Object pendingCheckinLock = new Object();

    private final Object evaluatorLock = new Object();

    /**
     * Invoked when a policy status object we have changes its state.
     */
    private final PolicyStateChangedListener savedPolicyStateChangedEventListener = new PolicyStateChangedListener() {
        @Override
        public void onPolicyStateChanged(final PolicyStateChangedEvent e) {
            PolicyEvaluator.this.onPolicyStateChanged(e);
        }
    };

    /**
     * Invoked when the checked pending changes in our pending checkin changes.
     */
    private final CheckedPendingChangesChangedListener savedCheckedPendingChangesChangedListener =
        new CheckedPendingChangesChangedListener() {
            @Override
            public void onCheckedPendingChangesChanged(final CheckedPendingChangesChangedEvent e) {
                PolicyEvaluator.this.onPolicyEvaluatorStateChanged(e);
            }
        };

    /**
     * Invoked when a change to the pending checkin we watch causes the set of
     * affected pending changes to change.
     */
    private final AffectedTeamProjectsChangedListener savedAffectedTeamProjectsChangedListener =
        new AffectedTeamProjectsChangedListener() {
            @Override
            public void onAffectedTeamProjectsChanged(final AffectedTeamProjectsChangedEvent e) {
                PolicyEvaluator.this.onPolicyEvaluatorStateChanged(e);
            }
        };

    /**
     * Invoked when the check-in comment changes.
     */
    private final CommentChangedListener savedCommentChangedListener = new CommentChangedListener() {
        @Override
        public void onCommentChanged(final CommentChangedEvent e) {
            PolicyEvaluator.this.onPolicyEvaluatorStateChanged(e);
        }
    };

    private final CheckedWorkItemsChangedListener savedWorkItemsChagnedListener =
        new CheckedWorkItemsChangedListener() {
            @Override
            public void onCheckedWorkItemsChangesChanged(final CheckedWorkItemsChangedEvent e) {
                PolicyEvaluator.this.onPolicyEvaluatorStateChanged(e);
            }
        };

    /**
     * State of this evaluator.
     */
    private PolicyEvaluatorState evaluatorState = PolicyEvaluatorState.UNEVALUATED;

    /**
     * Builds a policy evaluator for the given {@link VersionControlClient}. No
     * checkin policies are automatically loaded or evaluated during
     * construction.
     */
    public PolicyEvaluator(final VersionControlClient client, final PolicyLoader policyLoader) {
        Check.notNull(client, "client"); //$NON-NLS-1$
        Check.notNull(policyLoader, "policyLoader"); //$NON-NLS-1$

        this.client = client;
        this.policyLoader = policyLoader;
    }

    /**
     * Add a listener for the event fired when the state of this evaluator
     * changes (perhaps because it has been given a new pending checkin or team
     * project to run policies for).
     *
     * @param listener
     *        the listener to add (must not be <code>null</code>)
     */
    public void addPolicyEvaluatorStateChangedListener(final PolicyEvaluatorStateChangedListener listener) {
        evaluatorStateChangedEventListeners.addListener(listener);
    }

    /**
     * Remove a listener for the event fired when the state of this evaluator
     * changes (perhaps because it has been given a new pending checkin or team
     * project to run policies for).
     *
     * @param listener
     *        the listener to remove (must not be <code>null</code>)
     */
    public void removePolicyEvaluatorStateChangedListener(final PolicyEvaluatorStateChangedListener listener) {
        evaluatorStateChangedEventListeners.removeListener(listener);
    }

    /**
     * Fires the given {@link PolicyEvaluatorStateChangedEvent}, which is
     * constructed automatically.
     */
    private void firePolicyEvaluatorStateChangedEvent() {
        final PolicyEvaluator evaluator = this;

        evaluatorStateChangedEventListeners.foreachListener(new ListenerRunnable() {
            @Override
            public boolean run(final Object listener) throws Exception {
                ((PolicyEvaluatorStateChangedListener) listener).onPolicyEvaluatorStateChanged(
                    new PolicyEvaluatorStateChangedEvent(EventSource.newFromHere(), evaluator));
                return true;
            }
        });
    }

    /**
     * @see PolicyInstance#addPolicyStateChangedListener(PolicyStateChangedListener)
     */
    public void addPolicyStateChangedListener(final PolicyStateChangedListener listener) {
        policyStateChangedEventListeners.addListener(listener);
    }

    /**
     * @see PolicyInstance#removePolicyStateChangedListener(PolicyStateChangedListener)
     */
    public void removePolicyStateChangedListener(final PolicyStateChangedListener listener) {
        policyStateChangedEventListeners.removeListener(listener);
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
    private void firePolicyStateChangedEvent(final PolicyStateChangedEvent event) {
        Check.notNull(event, "event"); //$NON-NLS-1$

        policyStateChangedEventListeners.foreachListener(new ListenerRunnable() {
            @Override
            public boolean run(final Object listener) throws Exception {
                ((PolicyStateChangedListener) listener).onPolicyStateChanged(event);
                return true;
            }
        });
    }

    /**
     * Adds a listener for the {@link PolicyLoadErrorEvent}, which is fired when
     * a policy implementation fails to load during an
     * {@link #evaluate(PolicyContext)} or
     * {@link #reloadAndEvaluate(PolicyContext)} call.
     *
     * @param listener
     *        the listener to add (must not be <code>null</code>)
     */
    public void addPolicyLoadErrorListener(final PolicyLoadErrorListener listener) {
        policyLoadErrorEventListeners.addListener(listener);
    }

    /**
     * Removes a listener for the {@link PolicyLoadErrorEvent}, which is fired
     * when a policy implementation fails to load during an
     * {@link #evaluate(PolicyContext)} or
     * {@link #reloadAndEvaluate(PolicyContext)} call.
     *
     * @param listener
     *        the listener to remove (must not be <code>null</code>)
     */
    public void removePolicyLoadErrorListener(final PolicyLoadErrorListener listener) {
        policyLoadErrorEventListeners.removeListener(listener);
    }

    /**
     * Fires the given {@link PolicyLoadErrorEvent}, which must be constructed
     * manually.
     *
     * @param event
     *        the event to fire (must not be <code>null</code>)
     */
    private void firePolicyLoadErrorEvent(final PolicyLoadErrorEvent event) {
        Check.notNull(event, "event"); //$NON-NLS-1$

        policyLoadErrorEventListeners.foreachListener(new ListenerRunnable() {
            @Override
            public boolean run(final Object listener) throws Exception {
                ((PolicyLoadErrorListener) listener).onPolicyLoadError(event);
                return true;
            }
        });
    }

    /**
     * Sets the pending checkin used for policy evaluations. Resets the state of
     * the evaluator to {@link PolicyEvaluatorState#UNEVALUATED}.
     *
     * @param pendingCheckin
     *        the pending checkin used for policy evaluations (must not be
     *        <code>null</code>)
     */
    public void setPendingCheckin(final PendingCheckin pendingCheckin) {
        Check.notNull(pendingCheckin, "pendingCheckin"); //$NON-NLS-1$

        synchronized (pendingCheckinLock) {
            /*
             * Remove any listeners on the existing pending checkin (if there is
             * one).
             */
            removePendingCheckinEventListeners();

            this.pendingCheckin = pendingCheckin;

            /*
             * Add the new listeners. Removed on another call to this method, or
             * to close().
             */
            this.pendingCheckin.getPendingChanges().addAffectedTeamProjectsChangedListener(
                savedAffectedTeamProjectsChangedListener);

            this.pendingCheckin.getPendingChanges().addCheckedPendingChangesChangedListener(
                savedCheckedPendingChangesChangedListener);

            this.pendingCheckin.getPendingChanges().addCommentChangedListener(savedCommentChangedListener);

            this.pendingCheckin.getWorkItems().addCheckedWorkItemsChangedListener(savedWorkItemsChagnedListener);
        }

        synchronized (evaluatorLock) {
            evaluatorState = PolicyEvaluatorState.UNEVALUATED;
        }

        /*
         * Must happen outside synchronized to prevent deadlock.
         */
        firePolicyEvaluatorStateChangedEvent();
    }

    /**
     * Loads the policy definitions from the server appropriate for the pending
     * checkin previously passed to {@link #setPendingCheckin(PendingCheckin)}.
     * If the pending checkin that was supplied was null, this evaluator's
     * loaded policies are cleared.
     * <p>
     * Policy <b>implementations</b> are not necessarily reloaded when this
     * method is run. An implementation for a given policy type ID may be reused
     * between definition loads.
     * <p>
     * If some policies fail to load, the state of the evaluator will become
     * {@link PolicyEvaluatorState#POLICIES_LOAD_ERROR}. Details about the
     * problems can be retrieved as failures. This method tries hard to capture
     * all policy-related problems (including exceptions) and report them as
     * failures, so they can be presented to the user in the usual way and are
     * available to be sent to the server with an override comment.
     * <p>
     * <b>Event Warning</b>
     * <p>
     * This method does not invoke
     * {@link #firePolicyEvaluatorStateChangedEvent()} even though it changes
     * the evaluator state. The caller must fire the event after it invokes this
     * method.
     *
     * @param policyContext
     *        contextual settings that may include information about the user
     *        interface, etc. (must not be <code>null</code>)
     */
    private void loadPolicies(final PolicyContext policyContext) {
        Check.notNull(policyContext, "policyContext"); //$NON-NLS-1$

        final PendingCheckin currentPendingCheckin;

        synchronized (pendingCheckinLock) {
            currentPendingCheckin = pendingCheckin;
        }

        /*
         * Make a copy of the old policies so they can be disposed.
         */
        final List<PolicyEvaluationStatus> oldPolicies = policyEvaluationStatuses;

        /*
         * Use a new list for this evaluator's statuses.
         */
        policyEvaluationStatuses = new ArrayList<PolicyEvaluationStatus>();

        evaluatorState = PolicyEvaluatorState.UNEVALUATED;

        if (currentPendingCheckin == null) {
            return;
        }

        try {
            final String[] affectedTeamProjectServerPaths =
                currentPendingCheckin.getPendingChanges().getAffectedTeamProjectPaths();
            if (affectedTeamProjectServerPaths.length > 0) {
                // Get the definitions for all the paths from the server (TFS
                // annotation).
                final PolicyDefinition[] definitions =
                    client.getCheckinPoliciesForServerPaths(affectedTeamProjectServerPaths);

                // For each definition...
                for (int i = 0; i < definitions.length; i++) {
                    final PolicyDefinition definition = definitions[i];

                    if (definition.isEnabled()) {
                        /*
                         * See if we previously loaded a policy (from
                         * oldPolicies) loaded for the given type, and if so,
                         * don't reload it (but do set new priority, etc.). Skip
                         * implementations that are LoadErrorPolicy because
                         * they're generated each time this method runs.
                         */
                        final int oldPoliciesOriginalSize = oldPolicies.size();
                        int j;
                        for (j = 0; j < oldPolicies.size(); j++) {
                            final PolicyEvaluationStatus oldStatus = oldPolicies.get(j);

                            if ((oldStatus.getPolicy() instanceof LoadErrorPolicy == false)
                                && oldStatus.getPolicyType().equals(definition.getType())) {
                                // Copy to the list of keepers.
                                policyEvaluationStatuses.add(oldStatus);

                                /*
                                 * Remove the policy from the old list so it
                                 * doesn't get disposed.
                                 */
                                oldPolicies.remove(j);

                                /*
                                 * Make sure to update the priority with our new
                                 * configuration.
                                 */
                                oldStatus.update(
                                    definition.getPriority(),
                                    definition.getScopeExpressions(),
                                    definition.getConfigurationMemento());

                                break;
                            }
                        }

                        /*
                         * If we searched the whole list and didn't find an old
                         * one, load a new implementation.
                         */
                        if (j == oldPoliciesOriginalSize) {
                            final PolicyLoader loader = getPolicyLoader();
                            PolicyInstance instance = null;

                            try {
                                /*
                                 * The loader can signal errors in two ways:
                                 * PolicyLoaderException, and a null return
                                 * value. A null return simply means the policy
                                 * wasn't found. Other (bigger) problems are
                                 * exceptions.
                                 */
                                instance = loader.load(definition.getType().getID());

                                if (instance == null) {
                                    /*
                                     * Add a surrogate policy implementation so
                                     * the failure appears in the UI for
                                     * failures and can also be sent to TFS
                                     * (which requires a failure when an
                                     * override comment is supplied).
                                     */
                                    log.warn(
                                        MessageFormat.format(
                                            Messages.getString("PolicyEvaluator.CouldNotLoadImplementationFormat"), //$NON-NLS-1$
                                            definition.getType().toString()));

                                    instance = new LoadErrorPolicy(
                                        MessageFormat.format(
                                            Messages.getString("PolicyEvaluator.NoImplementationFoundFormat"), //$NON-NLS-1$
                                            definition.getType().getID()),
                                        definition.getType());

                                    /*
                                     * Turning on the load error lets users of
                                     * this PolicyEvaluator know to raise
                                     * warnings, prevent check-ins, etc.
                                     */
                                    evaluatorState = PolicyEvaluatorState.POLICIES_LOAD_ERROR;
                                }
                            } catch (final PolicyLoaderException e) {
                                log.warn(MessageFormat.format(
                                    "Exception loading check-in policy {0}", //$NON-NLS-1$
                                    definition.toString()), e);
                                evaluatorState = PolicyEvaluatorState.POLICIES_LOAD_ERROR;

                                instance =
                                    new LoadErrorPolicy(
                                        MessageFormat.format(
                                            Messages.getString("PolicyEvaluator.ExceptionLoadingPolicyFormat"), //$NON-NLS-1$
                                            definition.getType().getID(),
                                            e.getLocalizedMessage()),
                                        definition.getType());
                            }

                            /*
                             * We must configure the instance with the
                             * definition.
                             */
                            instance.loadConfiguration(definition.getConfigurationMemento());

                            /*
                             * Put a reference into the new list.
                             */
                            policyEvaluationStatuses.add(
                                new PolicyEvaluationStatus(
                                    instance,
                                    definition.getPriority(),
                                    definition.getScopeExpressions()));
                        }

                    }
                }

                /*
                 * Sort the list by their internal priorities and save the new
                 * list.
                 */
                Collections.sort(policyEvaluationStatuses, policyStatusComparator);

                /*
                 * Initialize each policy. This happens to all loaded policies
                 * (even old ones) every time through this method.
                 *
                 * If initialize() throws (probably an implementation error),
                 * the status is replaced with a new one holding a
                 * LoadErrorPolicy (to better report the errors).
                 */
                for (int i = 0; i < policyEvaluationStatuses.size(); i++) {
                    final PolicyEvaluationStatus status = policyEvaluationStatuses.get(i);

                    try {
                        status.initialize(currentPendingCheckin, policyContext);
                        status.addPolicyStateChangedEventListener(savedPolicyStateChangedEventListener);
                    } catch (final Exception e) {
                        log.warn(MessageFormat.format(
                            "Exception initializing check-in policy {0}", //$NON-NLS-1$
                            status.getPolicyType().getName()), e);

                        evaluatorState = PolicyEvaluatorState.POLICIES_LOAD_ERROR;

                        policyEvaluationStatuses.set(
                            i,
                            new PolicyEvaluationStatus(
                                new LoadErrorPolicy(
                                    MessageFormat.format(
                                        Messages.getString("PolicyEvaluator.ExceptionInitializingPolicyFormat"), //$NON-NLS-1$
                                        status.getPolicyType().getID(),
                                        e.getLocalizedMessage()),
                                    status.getPolicyType()),
                                status.getPriority(),
                                status.getScopeExpressions()));

                        // Call close last so we can use methods on status to
                        // build the replacement.
                        status.close();
                    }
                }

            }
        } catch (final Throwable t) {
            log.error("Generic error loading policies", t); //$NON-NLS-1$
            evaluatorState = PolicyEvaluatorState.POLICIES_LOAD_ERROR;

            /*
             * Clear our all our new statuses that we may have loaded before the
             * failure. The finally block will get the old ones.
             */
            closePolicyStatuses(
                policyEvaluationStatuses.toArray(new PolicyEvaluationStatus[policyEvaluationStatuses.size()]));
            policyEvaluationStatuses = new ArrayList<PolicyEvaluationStatus>();

            /*
             * Rethrow the exception because this is probably a programming
             * error.
             */
            throw new TECoreException(Messages.getString("PolicyEvaluator.ErrorLoadingCheckinPolicies"), t); //$NON-NLS-1$
        } finally {
            /*
             * Close out the old statuses we no longer use.
             */
            closePolicyStatuses(oldPolicies.toArray(new PolicyEvaluationStatus[oldPolicies.size()]));
        }

        /*
         * A quick performance check for no policies found.
         */
        if (evaluatorState == PolicyEvaluatorState.UNEVALUATED && policyEvaluationStatuses.size() == 0) {
            evaluatorState = PolicyEvaluatorState.EVALUATED;
        }
    }

    /**
     * Reloads all policy definitions from the server and evaluates all policies
     * via {@link #evaluate(PolicyContext)}.
     *
     * @return the value returned by {@link #evaluate(PolicyContext)};
     * @throws PolicyEvaluationCancelledException
     *         if the user cancelled the policy evaluation.
     */
    public PolicyFailure[] reloadAndEvaluate(final PolicyContext policyContext)
        throws PolicyEvaluationCancelledException {
        Check.notNull(policyContext, "policyContext"); //$NON-NLS-1$

        synchronized (evaluatorLock) {
            // This triggers a reload of the definitions when evaluate()
            // runs. It will always fire the state changed event for us.
            evaluatorState = PolicyEvaluatorState.UNEVALUATED;

            return evaluate(policyContext);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void close() {
        synchronized (pendingCheckinLock) {
            removePendingCheckinEventListeners();
        }

        synchronized (evaluatorLock) {
            evaluatorStateChangedEventListeners.clear();

            closePolicyStatuses(
                policyEvaluationStatuses.toArray(new PolicyEvaluationStatus[policyEvaluationStatuses.size()]));

            policyEvaluationStatuses.clear();
        }
    }

    /**
     * Closes the given statuses, removing this class's saved policy state
     * changed even listener in the process.
     */
    private void closePolicyStatuses(final PolicyEvaluationStatus[] statuses) {
        Check.notNull(statuses, "statuses"); //$NON-NLS-1$

        for (int i = 0; i < statuses.length; i++) {
            final PolicyEvaluationStatus s = statuses[i];

            if (s != null) {
                log.trace(MessageFormat.format(
                    "closing status for no-longer-needed policy type {0}", //$NON-NLS-1$
                    s.getPolicyType().getID()));

                /*
                 * Stop listening to events from the status, because policies
                 * can fire events at will (from timers, other threads, etc.).
                 */
                s.removePolicyStateChangedEventListener(savedPolicyStateChangedEventListener);

                /*
                 * Let the policy status do its own clean-up.
                 */
                try {
                    s.close();
                } catch (final Exception e) {
                    /*
                     * Log and continue.
                     */

                    log.error("Error closing policy status, continuing closing others", e); //$NON-NLS-1$
                }
            }
        }
    }

    /**
     * Removes the listeners we added to the pending checkin during
     * {@link #setPendingCheckin(PendingCheckin)}. Shared by
     * {@link #setPendingCheckin(PendingCheckin)} and {@link #close()}.
     */
    private void removePendingCheckinEventListeners() {
        if (pendingCheckin != null && pendingCheckin.getPendingChanges() != null) {
            pendingCheckin.getPendingChanges().removeAffectedTeamProjectsChangedListener(
                savedAffectedTeamProjectsChangedListener);

            pendingCheckin.getPendingChanges().removeCheckedPendingChangesChangedListener(
                savedCheckedPendingChangesChangedListener);
        }
    }

    /**
     * Evaluates checkin policies.
     *
     * @param policyContext
     *        contextual settings that may include information about the user
     *        interface, etc. (must not be <code>null</code>)
     * @return any failures detected.
     * @throws PolicyEvaluationCancelledException
     *         if the user canceled the policy evaluation.
     */
    public PolicyFailure[] evaluate(final PolicyContext policyContext) throws PolicyEvaluationCancelledException {
        Check.notNull(policyContext, "policyContext"); //$NON-NLS-1$

        log.trace("evaluate called"); //$NON-NLS-1$

        PolicyFailure[] failures = new PolicyFailure[0];

        final TaskMonitor taskMonitor = TaskMonitorService.getTaskMonitor();

        /*
         * Holds a loader exception so we can fire an event later (outside the
         * synchronized block).
         */
        Throwable loaderThrowable = null;

        try {
            synchronized (evaluatorLock) {
                try {
                    /*
                     * Load or reload policies if needed. We must be very
                     * careful not to fire the evaluator state changed until we
                     * have set the correct status, otherwise the event may tell
                     * a control to re-evaluate (which calls this method, which
                     * fires the event, etc.). If the state is correctly set,
                     * the control can know not to evaluate.
                     */
                    if (evaluatorState == PolicyEvaluatorState.UNEVALUATED
                        || evaluatorState == PolicyEvaluatorState.POLICIES_LOAD_ERROR
                        || evaluatorState == PolicyEvaluatorState.CANCELLED) {
                        try {
                            loadPolicies(policyContext);
                        } catch (final Throwable t) {
                            loaderThrowable = t;
                        }
                    }

                    /*
                     * The load may have failed, but the failures are converted
                     * to statuses each with a failure message. However, we must
                     * evaluate these load failure implementations to calculate
                     * their messages (and store the failures in the statuses).
                     */
                    boolean preserveLoadErrorState = false;
                    if (evaluatorState == PolicyEvaluatorState.POLICIES_LOAD_ERROR) {
                        preserveLoadErrorState = true;
                    }

                    /*
                     * Allocate one work unit for each policy status. 1 is big
                     * enough, since policy instances can begin new subtasks
                     * with as many work units as they desire, and those units
                     * will be scaled into 1 of these work units.
                     */
                    taskMonitor.begin(
                        Messages.getString("PolicyEvaluator.EvaluatingCheckinPolicies"), //$NON-NLS-1$
                        policyEvaluationStatuses.size());

                    if (policyEvaluationStatuses.size() == 0) {
                        /*
                         * If there are no statuses (no policies configured for
                         * the current pending checkin), then become evaluated.
                         * Of course, don't set to evaluated if we're preserving
                         * an error.
                         */
                        if (preserveLoadErrorState == false) {
                            evaluatorState = PolicyEvaluatorState.EVALUATED;
                        }
                    } else {
                        /*
                         * Evaluate all the statuses.
                         */

                        for (int i = 0; i < policyEvaluationStatuses.size(); i++) {
                            if (taskMonitor.isCanceled()) {
                                throw new PolicyEvaluationCancelledException();
                            }

                            final PolicyEvaluationStatus status = policyEvaluationStatuses.get(i);

                            taskMonitor.setCurrentWorkDescription(
                                MessageFormat.format(
                                    Messages.getString("PolicyEvaluator.EvaluatingFormat"), //$NON-NLS-1$
                                    status.getPolicyType().getName()));

                            TaskMonitor subTaskMonitor = null;

                            try {
                                subTaskMonitor = taskMonitor.newSubTaskMonitor(1);
                                policyContext.addProperty(PolicyContextKeys.TASK_MONITOR, subTaskMonitor);
                                status.evaluate(policyContext);
                            } finally {
                                if (subTaskMonitor != null) {
                                    subTaskMonitor.done();
                                }
                            }

                            /*
                             * Only change state to Evaluated if there wasn't a
                             * load error state before the evaluation that we
                             * must preserve.
                             */
                            if (preserveLoadErrorState == false) {
                                evaluatorState = PolicyEvaluatorState.EVALUATED;
                            }
                        }

                        /*
                         * Get all the failures from all of the statuses we
                         * evaluated.
                         */
                        failures = getFailures();
                    }
                } catch (final PolicyEvaluationCancelledException e) {
                    evaluatorState = PolicyEvaluatorState.CANCELLED;
                    throw e;
                } catch (final Exception e) {
                    /*
                     * These are probably programming errors, since load
                     * exceptions are handled in loadPolicies(), and evaluation
                     * failures are handled as failures.
                     */
                    log.error("Unhandled policy evaluation exception", e); //$NON-NLS-1$
                    evaluatorState = PolicyEvaluatorState.POLICIES_LOAD_ERROR;
                    failures = new PolicyFailure[0];

                    firePolicyLoadErrorEvent(new PolicyLoadErrorEvent(EventSource.newFromHere(), this, e));
                }
            }
        } finally {
            taskMonitor.done();

            /*
             * Fire an event for any exception we encountered.
             */
            if (loaderThrowable != null) {
                firePolicyLoadErrorEvent(new PolicyLoadErrorEvent(EventSource.newFromHere(), this, loaderThrowable));
            }

            /*
             * Also fire an event for any failures that were load error
             * failures. This pattern lets us report multiple failures whereas
             * we could only ever handle one exception.
             */
            for (int i = 0; i < failures.length; i++) {
                final PolicyFailure policyFailure = failures[i];

                if (policyFailure.getPolicy() instanceof LoadErrorPolicy) {
                    firePolicyLoadErrorEvent(
                        new PolicyLoadErrorEvent(EventSource.newFromHere(), this, new PolicyLoaderException(
                            policyFailure.getMessage(),
                            policyFailure.getPolicy().getPolicyType())));
                }
            }

            /*
             * Fire once for all the conditions that change state. This must
             * happen outside the synchronized block to prevent deadlock (event
             * handlers will often call back into this class).
             */
            firePolicyEvaluatorStateChangedEvent();
        }

        return failures;
    }

    /**
     * @return the current state of this evaluator
     */
    public PolicyEvaluatorState getPolicyEvaluatorState() {
        synchronized (evaluatorLock) {
            return evaluatorState;
        }
    }

    /**
     * @return the count of policy definitions loaded. Can be 0 if
     *         {@link #loadPolicies(PolicyContext)} and
     *         {@link #setPendingCheckin(PendingCheckin)} were not called, or if
     *         no policies were defined or loaded for the current pending
     *         checkin.
     */
    public int getPolicyCount() {
        synchronized (evaluatorLock) {
            return policyEvaluationStatuses.size();
        }
    }

    /**
     * @return shallow copy of all the failures from all the policies evaluated
     *         by this evaluator. Do not modify the objects returned.
     */
    public PolicyFailure[] getFailures() {
        /*
         * TODO Introduce a cache here if needed. It can be updated in the
         * onPolicyStateChanged handler with the new failures.
         */

        final List<PolicyFailure> failures = new ArrayList<PolicyFailure>();
        synchronized (evaluatorLock) {
            for (final Iterator<PolicyEvaluationStatus> i = policyEvaluationStatuses.iterator(); i.hasNext();) {
                final PolicyEvaluationStatus status = i.next();

                final PolicyFailure[] theseFailures = status.getFailures();

                if (theseFailures != null) {
                    for (int j = 0; j < theseFailures.length; j++) {
                        failures.add(theseFailures[j]);
                    }
                }
            }
        }

        return failures.toArray(new PolicyFailure[failures.size()]);
    }

    /**
     * @return the {@link PolicyLoader} in use by this evaluator.
     */
    public PolicyLoader getPolicyLoader() {
        return policyLoader;
    }

    /**
     * Handles generic policy evaluator state events by settings the state to
     * {@link PolicyEvaluatorState#UNEVALUATED} and firing the event.
     */
    private void onPolicyEvaluatorStateChanged(final CoreClientEvent e) {
        synchronized (evaluatorLock) {
            evaluatorState = PolicyEvaluatorState.UNEVALUATED;
        }

        firePolicyEvaluatorStateChangedEvent();
    }

    /**
     * @see PolicyStateChangedListener
     */
    private void onPolicyStateChanged(final PolicyStateChangedEvent e) {
        /*
         * This method is invoked when one of our loaded PolicyEvaluationStatus
         * object fires its policy state changed event, which is actually
         * reflected up from the status's PolicyInstance object.
         */

        firePolicyStateChangedEvent(e);
    }

    /**
     * @return the pending checkin that was previously set via
     *         {@link #setPendingCheckin(PendingCheckin)}.
     */
    public PendingCheckin getPendingCheckin() {
        synchronized (pendingCheckinLock) {
            return pendingCheckin;
        }
    }

    /**
     * Returns a text error message (with newlines) suitable for printing in a
     * console window, log file, or other text area that describes a problem
     * loading a check-in policy implementation so the user can fix the problem.
     * Things like policy type ID, installation instructions, and sometimes
     * stack traces are formatted into the message.
     *
     * @param throwable
     *        the problem that caused the load failure, usually these are
     *        {@link PolicyLoaderException}, but they can be any kind of
     *        {@link Throwable} and the error message will be as descriptive as
     *        possible (must not be <code>null</code>)
     * @return the formatted error text.
     */
    public static String makeTextErrorForLoadException(final Throwable throwable) {
        Check.notNull(throwable, "throwable"); //$NON-NLS-1$

        final StringBuffer sb = new StringBuffer();

        if (throwable instanceof PolicyLoaderException && ((PolicyLoaderException) throwable).getPolicyType() != null) {
            /*
             * Additional details for policy loader exceptions with policy type
             * information.
             */

            sb.append(Messages.getString("PolicyEvaluator.RequiredCheckinPolicyFailedToLoad")); //$NON-NLS-1$

            final PolicyLoaderException ple = (PolicyLoaderException) throwable;
            sb.append(Messages.getString("PolicyEvaluator.NameColon") + ple.getPolicyType().getName() + "\n"); //$NON-NLS-1$ //$NON-NLS-2$
            sb.append(Messages.getString("PolicyEvaluator.IDColon") + ple.getPolicyType().getID() + "\n"); //$NON-NLS-1$ //$NON-NLS-2$
            sb.append(Messages.getString("PolicyEvaluator.InstallationInstructionsColon") //$NON-NLS-1$
                + ple.getPolicyType().getInstallationInstructions()
                + "\n"); //$NON-NLS-1$

            sb.append(Messages.getString("PolicyEvaluator.ErrorColon") + throwable.getLocalizedMessage() + "\n\n"); //$NON-NLS-1$ //$NON-NLS-2$
        } else {
            /*
             * Run-time errors and other problems not handled during policy
             * loading are wrapped in TECoreException, but some other exception
             * types may come through (very rare).
             */

            sb.append(Messages.getString("PolicyEvaluator.AnErrorOccurredInThePolicyFramework")); //$NON-NLS-1$

            final StringWriter sw = new StringWriter();
            final PrintWriter pw = new PrintWriter(sw, true);
            throwable.printStackTrace(pw);
            pw.flush();
            sw.flush();

            sb.append(Messages.getString("PolicyEvaluator.ErrorColon") + sw.toString() + "\n"); //$NON-NLS-1$ //$NON-NLS-2$
        }

        sb.append(Messages.getString("PolicyEvaluator.MoreDetailsMayBeAvailableInPlatformLogs")); //$NON-NLS-1$

        return sb.toString();
    }
}
