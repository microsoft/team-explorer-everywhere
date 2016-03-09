// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.checkinpolicies;

import com.microsoft.tfs.core.checkinpolicies.events.PolicyStateChangedEvent;
import com.microsoft.tfs.core.checkinpolicies.events.PolicyStateChangedListener;
import com.microsoft.tfs.core.memento.Memento;
import com.microsoft.tfs.core.pendingcheckin.PendingCheckin;
import com.microsoft.tfs.util.Closable;
import com.microsoft.tfs.util.tasks.TaskMonitor;

/**
 * <p>
 * An instance is a loaded, configured, configurable, and evaluatable policy
 * object ready for use by the framework. Objects that implement this interface
 * are loaded by the framework (see {@link PolicyLoader}) and included in lists
 * of available policies. They may also be configured and evaluated directly
 * from UI code.
 * </p>
 * <p>
 * See {@link PolicyBase} for a ready-to-extend class that implements much of
 * this interface.
 * </p>
 * <p>
 *
 * <h1>Cancellation</h1>
 * <p>
 * Policies that take a long time to run should support cancellation. In your
 * {@link #evaluate(PolicyContext)} implementation, get a {@link TaskMonitor}
 * instance from the {@link PolicyContext}, using the key defined at
 * {@link PolicyContextKeys#TASK_MONITOR}. If the context returns a non-null
 * {@link TaskMonitor}, simply test whether the user wishes to cancel via
 * {@link TaskMonitor#isCanceled()} method. If this method returns true, simply
 * throw {@link PolicyEvaluationCancelledException} to cancel all policy
 * evaluation.
 * </p>
 * <p>
 * The context may return a null value for the
 * {@link PolicyContextKeys#TASK_MONITOR} key if the environment hosting the
 * policy framework did not set a task monitor.
 * </p>
 * <p>
 * Most policies that complete quickly do not need to support cancellation via
 * {@link TaskMonitor}. Its use is never required.
 * </p>
 *
 * <h1>Progress Reporting</h1>
 * <p>
 * Implementations can use the same mechanism for cancellation to report
 * progress: {@link TaskMonitor}. When an implementation wants to use a
 * {@link TaskMonitor} to report on its progress, it <b>must</b> call
 * {@link TaskMonitor#begin(String, int)} before calling
 * {@link TaskMonitor#worked(int)} or
 * {@link TaskMonitor#setCurrentWorkDescription(String)}, then it <b>must</b>
 * call {@link TaskMonitor#done()}.
 * </p>
 * <p>
 * Most policies that complete quickly do not need to interact with a
 * {@link TaskMonitor} object. Its use is never required.
 * </p>
 *
 * <h1>Good UI Behavior</h1>
 * <p>
 * When running in a graphical environment (like Eclipse), the policy framework
 * may invoke {@link #evaluate(PolicyContext)} on a background thread. This
 * allows the user interface to update itself (paint) while a policy evalutes
 * (possibly taking several seconds or even minutes). See the Javadoc on
 * {@link #evaluate(PolicyContext)} for thread marshalling requirements.
 * Multiple policies defined on the same team project will never run
 * concurrently, and the environment will always wait until all policies
 * complete before allowing a check-in to proceed.
 * </p>
 * <p>
 * Because of this threading design, implementations are not required to
 * manually service the user interface's event loop, and may perform long
 * computations directly inside {@link #evaluate(PolicyContext)}.
 * </p>
 *
 * <h1>Thread Policy</h1>
 * </p>
 * <p>
 * Implementations of this class should be fully synchronized to ensure
 * re-entrancy and data visibility. The framework does not schedule calls into
 * an implementation from multiple threads simultaneously, but events fired
 * during an policy's evaluation may cause other threads in the environment
 * (i.e. Eclipse) to read data from the implementation to update status
 * information. These calls require synchronization in the implementation.
 * </p>
 * <p>
 * The policy framework may call constructors,
 * {@link #initialize(PendingCheckin, PolicyContext)}, and
 * {@link #evaluate(PolicyContext)} on different threads for a single instance
 * of this class. For this reason, implementations should not store thread-local
 * information in these methods to be used by other methods.
 * </p>
 *
 * @see PolicyBase
 * @since TEE-SDK-10.1
 * @threadsafety thread-compatible
 */
public interface PolicyInstance extends Closable {
    /*
     * Methods for type information.
     */

    /**
     * @return the {@link PolicyType} information for this instance.
     */
    public PolicyType getPolicyType();

    /*
     * Methods for run-time configuration of an instance.
     */

    /**
     * @return true if this policy's run-time configuration can be edited, false
     *         if editing should be disallowed by the user interface.
     */
    public boolean canEdit();

    /**
     * <p>
     * Edits the policy's configuration by interacting with the graphical user
     * interface, if appropriate, or by other means. Implementations should
     * obtain references to user interface objects from the
     * {@link PolicyEditArgs} map. This object's configuration is later
     * retrieved by the framework (via {@link #saveConfiguration(Memento)}) and
     * saved in the policy definition on the Team Foundation Server.
     * </p>
     * <p>
     * The policy framework will always invoke this method from the UI thread
     * when running in Eclipse and Explorer.
     * </p>
     * <p>
     * <b>Thread Policy</b>
     * </p>
     * <p>
     * The policy framework <b>always</b> invokes this method on the
     * user-interface thread, if the graphical environment where the framework
     * is hosted requires it. Implementations may use the graphical interface
     * context objects directly, without marhsalling calls to the UI thread.
     * </p>
     *
     * @param policyEditArgs
     *        a map of strings to objects that can be used as context for
     *        interface building.
     * @return true if the user made changes to this policy's configuration,
     *         false if the edit operation was cancelled and no changes were
     *         made.
     */
    public boolean edit(PolicyEditArgs policyEditArgs);

    /**
     * <p>
     * Saves the run-time configuration of this instance to the given (empty
     * except for name) {@link Memento} object, which will be persisted by the
     * framework in the policy definition in the Team Foundation Server.
     * </p>
     * <p>
     * Implementations should not perform user interface work in this method.
     * </p>
     * <p>
     * <b>Memento Notes</b>
     * <p>
     * Implementations <em>should not</em> store text data directly inside the
     * given memento node (via {@link Memento#putTextData(String)}, but instead
     * they should create child nodes to store text data. Any text data stored
     * on the given node will be discarded when it is saved. Implementations are
     * encouraged to set other types of attributes (Integer, String, Boolean,
     * etc.) directly on the given node or any child nodes they create.
     * </p>
     *
     * @param confgurationMemento
     *        the empty (except for name) {@link Memento} to save settings to
     *        (must not be <code>null</code>)
     */
    public void saveConfiguration(Memento confgurationMemento);

    /**
     * <p>
     * Loads run-time configuration information from the given {@link Memento},
     * which was previously build by a call to
     * {@link #saveConfiguration(Memento)}.
     * </p>
     * <p>
     * Implementations should not perform user interface work in this method.
     * </p>
     *
     * @param configurationMemento
     *        the {@link Memento} to load settings from (must not be
     *        <code>null</code>)
     */
    public void loadConfiguration(Memento configurationMemento);

    /*
     * Methods for policy evaluation and events related to evaluation.
     */

    /**
     * Adds a policy state changed listener that receives a
     * {@link PolicyStateChangedEvent} whenever this policy is evaluated and a
     * new set of failures is generated.
     * <p>
     * Policies being evaluated in a graphical context (in an application like
     * the Plug-in for Eclipse, or Explorer) can fire this event to cause the
     * graphical display of their state (including failures) to be updated.
     *
     * @param listener
     *        the listener to add (must not be <code>null</code>)
     */
    public void addPolicyStateChangedListener(PolicyStateChangedListener listener);

    /**
     * Removes a policy state changed listener that was previously added via
     * {@link #addPolicyStateChangedListener(PolicyStateChangedListener)}.
     * changes.
     *
     * @see #addPolicyStateChangedListener(PolicyStateChangedListener)
     *
     * @param listener
     *        the listener to remove (must not be <code>null</code>)
     */
    public void removePolicyStateChangedListener(PolicyStateChangedListener listener);

    /**
     * <p>
     * Prepares a {@link PolicyInstance} for policy evaluation. The framework
     * tries to keep {@link PolicyInstance} instances around as long as
     * possible, for performance reasons, so it will invoke this method
     * repeatedly to re-configure the object for a new pending checkin.
     * </p>
     * <p>
     * If you allocate resources in this method, {@link Closable#close()} is a
     * good place to release them.
     * </p>
     * <p>
     * Implementations should not save the {@link PolicyContext} object for
     * later use, because the framework may pass a different context to
     * {@link #evaluate(PolicyContext)}.
     * </p>
     * <p>
     * Implementations should not perform user interface work in this method
     * because the framework tries to initialize a policy as lazily as it can,
     * but also must call initialize many times as the checkin data changes.
     * </p>
     *
     * @param pendingCheckin
     *        the pending changes that will be evaluated by this
     *        {@link PolicyInstance} (must not be <code>null</code>)
     * @param context
     *        contextual settings that may include information about the user
     *        interface, etc. (must not be <code>null</code>)
     */
    public void initialize(PendingCheckin pendingCheckin, PolicyContext context);

    /**
     * <p>
     * Evaluates the pending checkin for policy compliance and returns any
     * failures encountered. Called by the checkin policy framework at various
     * times during the life of the {@link PolicyInstance} object, but always
     * after {@link #initialize(PendingCheckin, PolicyContext)} was called with
     * a pending checkin and contextual information.
     * </p>
     * <p>
     * Implementations should not save the {@link PolicyContext} object for
     * later use, becuase the objects inside it are not guaranteed to persist
     * after {@link #evaluate(PolicyContext)} returns. This restriction means
     * that policies which call their own {@link #evaluate(PolicyContext)}
     * method (as a result of an external event or some other design decision)
     * must create their own {@link PolicyContext} instances for this call. They
     * can re-use the values from the original, framework-called
     * {@link #evaluate(PolicyContext)} at their own risk--these objects may
     * become stale or invalid.
     * </p>
     * <p>
     * <b>Thread Policy</b>
     * </p>
     * <p>
     * Implementations must not assume this method will be invoked on any
     * specific thread. More specifically, if the framework is running in an
     * application with a graphical user interface, any user-interface work done
     * in the policy (through objects obtained in the context, for example) must
     * be marshalled to the correct thread, if required by the interface toolkit
     * in use. For Eclipse plug-ins, this means access to all user-interface
     * objects should be done by by org.eclipse.swt.widgets.Display's
     * asyncExec(Runnable) or syncExec(Runnable) methods.
     * </p>
     *
     * @return any failures encounterd by this policy, or an empty
     *         {@link PolicyFailure} array if none encountered.
     * @param context
     *        contextual settings that may include information about the user
     *        interface, etc. (must not be <code>null</code>)
     * @throws PolicyEvaluationCancelledException
     *         if the user cancelled the policy evaluation.
     */
    public PolicyFailure[] evaluate(PolicyContext context) throws PolicyEvaluationCancelledException;

    /**
     * <p>
     * Called when the user activates (by double-clicking or some other user
     * interface) a failure generated by a previous call to
     * {@link #evaluate(PolicyContext)}. Implementations should further explain
     * the failure, perhaps presenting additional information or proposing a
     * solution.
     * </p>
     * <p>
     * Always called after the framework has called
     * {@link #initialize(PendingCheckin, PolicyContext)}.
     * <p>
     * <b>Thread Policy</b>
     * </p>
     * <p>
     * The policy framework <b>always</b> invokes this method on the
     * user-interface thread, if the graphical environment where the framework
     * is hosted requires it. Implementations may use the graphical interface
     * context objects directly, without marhsalling calls to the UI thread.
     * </p>
     *
     * @param failure
     *        the failure to activate or display (must not be <code>null</code>)
     * @param context
     *        contextual settings that may include information about the user
     *        interface, etc. (must not be <code>null</code>)
     */
    public void activate(PolicyFailure failure, PolicyContext context);

    /**
     * <p>
     * Shows help about the given failure if it was selected for help in the
     * user interface.
     * </p>
     * <p>
     * Always called after the framework has called
     * {@link #initialize(PendingCheckin, PolicyContext)}.
     * <p>
     * <b>Thread Policy</b>
     * </p>
     * <p>
     * The policy framework <b>always</b> invokes this method on the
     * user-interface thread, if the graphical environment where the framework
     * is hosted requires it. Implementations may use the graphical interface
     * context objects directly, without marhsalling calls to the UI thread.
     * </p>
     *
     * @param failure
     *        the failure to display help for (must not be <code>null</code>)
     * @param context
     *        contextual settings that may include information about the user
     *        interface, etc. (must not be <code>null</code>)
     */
    public void displayHelp(PolicyFailure failure, PolicyContext context);
}
