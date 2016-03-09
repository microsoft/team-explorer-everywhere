// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.sdk.samples.checkinpolicy;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;

import com.microsoft.tfs.core.checkinpolicies.PolicyBase;
import com.microsoft.tfs.core.checkinpolicies.PolicyContext;
import com.microsoft.tfs.core.checkinpolicies.PolicyContextKeys;
import com.microsoft.tfs.core.checkinpolicies.PolicyEditArgs;
import com.microsoft.tfs.core.checkinpolicies.PolicyEvaluationCancelledException;
import com.microsoft.tfs.core.checkinpolicies.PolicyFailure;
import com.microsoft.tfs.core.checkinpolicies.PolicyInstance;
import com.microsoft.tfs.core.checkinpolicies.PolicyType;
import com.microsoft.tfs.core.clients.versioncontrol.path.LocalPath;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.PendingChange;
import com.microsoft.tfs.core.memento.Memento;
import com.microsoft.tfs.core.pendingcheckin.PendingCheckin;
import com.microsoft.tfs.util.tasks.TaskMonitor;

/**
 * <p>
 * This sample check-in policy implements the basic features of a TFS check-in
 * policy in Java. All check-in policies must implement {@link PolicyInstance},
 * which is done in this case by extending {@link PolicyBase} (which also
 * provides some event functionality so this class doesn't have to).
 * </p>
 * <p>
 * This sample includes extensive in-line documentation.
 * </p>
 */
public class SamplePolicy extends PolicyBase {
    /**
     * <p>
     * The policy type information can be defined statically because it
     * describes the kind of policy this is. This information is stored in the
     * Team Foundation Server for each configured policy on a Team Project, so
     * other client programs can find and load the appropriate policy
     * implementation. Installation help text is shown to users when an
     * implementation for a configured policy cannot be found.
     * </p>
     * <p>
     * Check-in policies that are Eclipse plug-ins must ensure the policy type
     * ID declared here exactly matches the ID declared in their plugin.xml's
     * <tt>typeID</tt> attribute on the "policy" node, otherwise the
     * implementation will fail to load.
     * </p>
     *
     * @see PolicyType
     */
    private final static PolicyType TYPE =
        new PolicyType(
            "com.microsoft.tfs.sdk.samples.checkinpolicy.SampleCheckinPolicy-1", //$NON-NLS-1$
            "Sample: disallows some file paths", //$NON-NLS-1$
            "Disallows local file paths that contain the configured string", //$NON-NLS-1$
            "A sample policy that demonstrates basic check-in policy implementation by " //$NON-NLS-1$
                + "failing check-ins for source control files with a forbidden string in " //$NON-NLS-1$
                + "the local item's file name (case-insensitive).", //$NON-NLS-1$
            "See the Javadoc in SamplePolicy.java."); //$NON-NLS-1$

    /**
     * This is the string key we use to store and load our settings from the
     * configuration memento object.
     */
    private final static String DISALLOWED_STRING_KEY = "disallowedString"; //$NON-NLS-1$

    /**
     * Files with this extension trigger a special message box during evaluation
     * that does not cause a failure.
     */
    private final static String SPECIAL_WARNING_FILE_EXTENSION = ".moo"; //$NON-NLS-1$

    /**
     * This is the configured string disallowed in file names when the policy
     * runs.
     */
    private String disallowedString = ""; //$NON-NLS-1$

    /**
     * All policy implementations must include a zero-argument constructor, so
     * they can be dynamically created by the policy framework.
     */
    public SamplePolicy() {
        super();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean canEdit() {
        /*
         * Returning true means the user TEE interface can enable the "edit"
         * button (or similar control), and the edit() method on this class will
         * be called if the user activates that UI.
         *
         * If this method returns false, edit() is never called by the
         * framework.
         */
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean edit(final PolicyEditArgs policyEditArgs) {
        /*
         * Returning true means the user completed editing this policy and
         * configuration changes should be saved. Returning false means the
         * changes made to this policy should be discarded (not saved to the
         * server).
         *
         * If canEdit() returns false for this class, this method will never be
         * called by the framework.
         */

        final Shell shell = (Shell) policyEditArgs.getContext().getProperty(PolicyContextKeys.SWT_SHELL);

        /*
         * Shell is not provided only if this policy is being edited from
         * outside the Eclipse environment (for example, from the command-line
         * client). This is unlikely for this sample because it is deployed as
         * an Eclipse plug-in, but it's a good idea to validate the objects
         * returned from the edit args in all cases.
         */
        if (shell == null) {
            return false;
        }

        final InputDialog dialog = new InputDialog(
            shell,
            "Policy Configuration", //$NON-NLS-1$
            "Enter the string that will be disallowed in file names:", //$NON-NLS-1$
            disallowedString,
            null);

        if (dialog.open() != IDialogConstants.OK_ID) {
            return false;
        }

        /*
         * Update this policy's configuration and return true to let the
         * framework save our configuration to the server.
         */
        disallowedString = dialog.getValue();
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PolicyFailure[] evaluate(final PolicyContext context) throws PolicyEvaluationCancelledException {
        final PendingCheckin pc = getPendingCheckin();

        /*
         * The pending check-in object describes the following parts of a
         * check-in: Check-in Notes, Pending Changes, Work Items, Check-in
         * Policies. This policy is only interested in the pending changes the
         * user has "checked" in the user interface.
         */
        final PendingChange[] checkedChanges = pc.getPendingChanges().getCheckedPendingChanges();

        /*
         * Policy failures are collected in this list.
         */
        final List<PolicyFailure> failures = new ArrayList<PolicyFailure>();

        /*
         * This policy uses a task monitor, if present, to report progress to
         * the user and detect cancellation. Using a task monitor in this method
         * is optional, and cancellation requests may be ignored.
         */
        final TaskMonitor taskMonitor = (TaskMonitor) context.getProperty(PolicyContextKeys.TASK_MONITOR);

        try {
            if (taskMonitor != null) {
                /*
                 * Read TaskMonitor's documentation before using it. There are
                 * strict requirements around work calculation, subtask
                 * allocation, and finish.
                 */
                taskMonitor.begin("Checking pending changes for disallowed strings", checkedChanges.length); //$NON-NLS-1$
            }

            for (final PendingChange change : checkedChanges) {
                /*
                 * Only check changes with local items, but ensure we complete
                 * each iteration and update the task monitor's work count.
                 */
                if (change.getLocalItem() != null) {
                    if (taskMonitor != null) {
                        /*
                         * Policies that can run for a long time should check
                         * often for user cancellation.
                         */
                        if (taskMonitor.isCanceled()) {
                            throw new PolicyEvaluationCancelledException();
                        }

                        taskMonitor.setCurrentWorkDescription(MessageFormat.format(
                            "Checking: {0}", //$NON-NLS-1$
                            change.getLocalItem()));
                    }

                    /*
                     * Lowercase both strings for case-insensitive match. This
                     * is a simplistic test and may not work in all locales.
                     */
                    if (change.getLocalItem().toLowerCase().indexOf(disallowedString.toLowerCase()) != -1) {
                        failures.add(
                            new PolicyFailure(
                                MessageFormat.format(
                                    "The checked pending change ''{0}'' contains the disallowed string ''{1}''", //$NON-NLS-1$
                                    change.getLocalItem(),
                                    disallowedString),
                                this));
                    }

                    /*
                     * Simulate a long-running policy. Sleeping here makes it
                     * easier to test cancellation and see progress updates.
                     */
                    try {
                        Thread.sleep(150);
                    } catch (final InterruptedException e) {
                    }

                    /*
                     * Test for a special extension and pop up a message box.
                     */
                    if (LocalPath.getFileExtension(change.getLocalItem()).equalsIgnoreCase(
                        SPECIAL_WARNING_FILE_EXTENSION)) {
                        /*
                         * An example of user-interface work. Evaluate may be
                         * called on any thread, so all UI work must be run on
                         * the correct thread.
                         */

                        final Shell shell = (Shell) context.getProperty(PolicyContextKeys.SWT_SHELL);

                        /*
                         * Always check the returned value for null, because
                         * some properties are not set in some environments (for
                         * example, command-line client never sets the SWT
                         * shell).
                         */
                        if (shell != null) {
                            /*
                             * Run the UI work on the UI thread. Synchronous
                             * execution blocks the policy evaluation from
                             * completing.
                             */
                            shell.getDisplay().syncExec(new Runnable() {
                                @Override
                                public void run() {
                                    final MessageBox specialWarning = new MessageBox(shell, SWT.ICON_INFORMATION);
                                    specialWarning.setMessage(
                                        MessageFormat.format(
                                            "A pending change that ends in ''{0}'' was detected.  " //$NON-NLS-1$
                                                + "This message box was raised as an example.  " //$NON-NLS-1$
                                                + "No failure was created.", //$NON-NLS-1$
                                            SPECIAL_WARNING_FILE_EXTENSION));
                                    specialWarning.open();
                                }
                            });
                        }
                    }
                }

                if (taskMonitor != null) {
                    taskMonitor.worked(1);
                }
            }
        } finally {
            /*
             * We must guarantee TaskMonitor.done() is called if we called
             * begin().
             */
            if (taskMonitor != null) {
                taskMonitor.done();
            }
        }

        /*
         * Always return a non-null array.
         */
        return failures.toArray(new PolicyFailure[failures.size()]);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PolicyType getPolicyType() {
        /*
         * This class statically defines a type which is always appropriate.
         */
        return SamplePolicy.TYPE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void loadConfiguration(final Memento configurationMemento) {
        /*
         * Our only setting is a string, which we can load from an attribute.
         */
        disallowedString = configurationMemento.getString(DISALLOWED_STRING_KEY);

        if (disallowedString == null) {
            disallowedString = ""; //$NON-NLS-1$
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void saveConfiguration(final Memento configurationMemento) {
        /*
         * Save our only setting as an attribute.
         */
        configurationMemento.putString(DISALLOWED_STRING_KEY, disallowedString);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void displayHelp(final PolicyFailure failure, final PolicyContext context) {
        /*
         * This method is invoked when the user wants to see help about a
         * failure in the user interface.
         */
        final Shell shell = (Shell) context.getProperty(PolicyContextKeys.SWT_SHELL);

        if (shell == null) {
            return;
        }

        /*
         * displayHelp() is called on the UI thread, so we don't have to
         * explicitly run our work on the UI thread.
         */
        final MessageBox helpMessageBox = new MessageBox(shell, SWT.ICON_INFORMATION);
        helpMessageBox.setText("Sample Help"); //$NON-NLS-1$
        helpMessageBox.setMessage("Help text would go here."); //$NON-NLS-1$
        helpMessageBox.open();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void activate(final PolicyFailure failure, final PolicyContext context) {
        /*
         * This method is invoked when the user activates a policy failure
         * produced by this policy (usually by double-clicking the failure).
         * Implementations may offer detailed failure information, offer a
         * solution, or do nothing.
         */
        final Shell shell = (Shell) context.getProperty(PolicyContextKeys.SWT_SHELL);

        if (shell == null) {
            return;
        }

        /*
         * activate() is called on the UI thread, so we don't have to explicitly
         * run our work on the UI thread.
         */
        final MessageBox helpMessageBox = new MessageBox(shell, SWT.ICON_INFORMATION);
        helpMessageBox.setText("Activation Dialog"); //$NON-NLS-1$
        helpMessageBox.setMessage("Policy activated."); //$NON-NLS-1$
        helpMessageBox.open();
    }
}
