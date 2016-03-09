// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.controls.vc.checkin;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IContributionManager;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;

import com.microsoft.tfs.client.common.commands.EvaluateCheckinPoliciesCommand;
import com.microsoft.tfs.client.common.framework.command.ICommandExecutor;
import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.TFSCommonUIClientPlugin;
import com.microsoft.tfs.client.common.ui.controls.vc.checkinpolicies.PolicyFailureData;
import com.microsoft.tfs.client.common.ui.controls.vc.checkinpolicies.PolicyFailureTable;
import com.microsoft.tfs.client.common.ui.framework.command.UICommandExecutorFactory;
import com.microsoft.tfs.client.common.ui.framework.helper.SWTUtil;
import com.microsoft.tfs.client.common.ui.framework.helper.UIHelpers;
import com.microsoft.tfs.client.common.ui.framework.layout.GridDataBuilder;
import com.microsoft.tfs.core.checkinpolicies.PolicyContext;
import com.microsoft.tfs.core.checkinpolicies.PolicyContextKeys;
import com.microsoft.tfs.core.checkinpolicies.PolicyEvaluator;
import com.microsoft.tfs.core.checkinpolicies.PolicyEvaluatorState;
import com.microsoft.tfs.core.checkinpolicies.PolicyFailure;
import com.microsoft.tfs.core.checkinpolicies.PolicyInstance;
import com.microsoft.tfs.core.checkinpolicies.events.PolicyEvaluatorStateChangedEvent;
import com.microsoft.tfs.core.checkinpolicies.events.PolicyEvaluatorStateChangedListener;
import com.microsoft.tfs.core.checkinpolicies.events.PolicyLoadErrorEvent;
import com.microsoft.tfs.core.checkinpolicies.events.PolicyLoadErrorListener;
import com.microsoft.tfs.core.checkinpolicies.events.PolicyStateChangedEvent;
import com.microsoft.tfs.core.checkinpolicies.events.PolicyStateChangedListener;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.PolicyFailureInfo;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.PolicyOverrideInfo;
import com.microsoft.tfs.core.product.ProductInformation;
import com.microsoft.tfs.core.product.ProductName;
import com.microsoft.tfs.util.Check;

public class PolicyWarningsCheckinControl extends AbstractCheckinSubControl {
    private static final Log log = LogFactory.getLog(PolicyWarningsCheckinControl.class);

    private final CheckinControlOptions options;
    private final PolicyFailureTable policyFailureTable;
    private final CLabel statusLabel;
    private final Image warningImage;
    private final Text overrideReasonText;
    private final SashForm sashForm;

    private IAction evaluateAction;
    private IAction helpAction;

    private PolicyEvaluator evaluator;
    private PolicyEvaluatorStateChangedListener policyEvaluatorStateChangedListener;
    private PolicyStateChangedListener policyStateChangedListener;
    private PolicyLoadErrorListener policyLoadErrorListener;

    public PolicyWarningsCheckinControl(
        final Composite parent,
        final int style,
        final CheckinControlOptions options,
        final CheckinControl parentCheckinControl) {
        super(
            parent,
            style,
            Messages.getString("PolicyWarningsCheckinControl.Title"), //$NON-NLS-1$
            CheckinSubControlType.POLICY_WARNINGS);

        Check.notNull(parentCheckinControl, "parentCheckinControl"); //$NON-NLS-1$
        Check.notNull(options, "options"); //$NON-NLS-1$

        this.options = new CheckinControlOptions(options);

        SWTUtil.gridLayout(this);

        statusLabel = new CLabel(this, SWT.LEFT | SWT.BORDER);
        GridDataBuilder.newInstance().hGrab().hFill().applyTo(statusLabel);

        sashForm = new SashForm(this, SWT.VERTICAL);
        GridDataBuilder.newInstance().grab().fill().applyTo(sashForm);

        policyFailureTable = new PolicyFailureTable(sashForm, SWT.FULL_SELECTION);

        final Composite composite = SWTUtil.createComposite(sashForm);
        SWTUtil.gridLayout(composite, 1, true, 0, 0);

        SWTUtil.createLabel(composite, Messages.getString("PolicyWarningsCheckinControl.OverrideReasonLabelText")); //$NON-NLS-1$
        overrideReasonText = new Text(composite, SWT.MULTI | SWT.BORDER | SWT.V_SCROLL | SWT.WRAP);
        overrideReasonText.setEditable(false);
        GridDataBuilder.newInstance().grab().fill().applyTo(overrideReasonText);

        sashForm.setMaximizedControl(policyFailureTable);

        policyFailureTable.getContextMenu().addMenuListener(new IMenuListener() {
            @Override
            public void menuAboutToShow(final IMenuManager manager) {
                PolicyWarningsCheckinControl.this.fillContextMenu(manager);
            }
        });

        policyFailureTable.addSelectionChangedListener(new ISelectionChangedListener() {
            @Override
            public void selectionChanged(final SelectionChangedEvent event) {
                helpAction.setEnabled(policyFailureTable.getSelectionCount() == 1);
            }
        });

        policyFailureTable.addDoubleClickListener(new IDoubleClickListener() {
            @Override
            public void doubleClick(final DoubleClickEvent event) {
                PolicyWarningsCheckinControl.this.doubleClick();
            }
        });

        setSelectionProvider(policyFailureTable);

        addDisposeListener(new DisposeListener() {
            @Override
            public void widgetDisposed(final DisposeEvent e) {
                PolicyWarningsCheckinControl.this.widgetDisposed(e);
            }
        });

        warningImage = PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJS_WARN_TSK);

        final Color backgroundColor = getDisplay().getSystemColor(SWT.COLOR_INFO_BACKGROUND);
        statusLabel.setBackground(backgroundColor);

        createActions();
    }

    /**
     * <p>
     * Sets policy override information, which usually comes from a historical
     * changeset. This override info contains a comment as well as failures,
     * which will be displayed in the failures table as if they were retrieved
     * from a {@link PolicyEvaluator}.
     * </p>
     * <p>
     * Calling this method removes the existing {@link PolicyEvaluator}
     * (equivalent to calling setEvaluator(<code>null</code>)), which means the
     * control will no longer update itself from the evaluator's events. Call
     * {@link #setEvaluator(PolicyEvaluator)} with a non-null PolicyEvaluator to
     * restore the self-updating behavior.
     * </p>
     *
     * @param policyOverrideInfo
     *        the override information (must not be <code>null</code>)
     */
    public void setHistoricPolicyOverrideInfo(final PolicyOverrideInfo policyOverrideInfo) {
        Check.notNull(policyOverrideInfo, "policyOverrideInfo"); //$NON-NLS-1$

        /*
         * Disable evaluation.
         */

        evaluator = null;

        /*
         * Some of the failure reporting logic changed after TFS 2010 with the
         * introduction of gated check-ins on the server, which doesn't
         * perfectly pass along policy failure information from the client. A
         * new configuration of the PolicyOverrideInfo is that it has no
         * failures, yet has an override message which should be displayed.
         */

        final PolicyFailureInfo[] policyFailures = policyOverrideInfo.getPolicyFailures();

        // Always display all failure rows.

        final PolicyFailureData[] failureData = new PolicyFailureData[policyFailures.length];
        for (int i = 0; i < policyFailures.length; i++) {
            failureData[i] = PolicyFailureData.fromPolicyFailureInfo(policyFailures[i]);
        }

        policyFailureTable.setPolicyFailures(failureData);

        /*
         * Always set the override comment if there is one. The control may be
         * made invisible depending on the failure count (below).
         */

        overrideReasonText.setText(policyOverrideInfo.getComment() != null ? policyOverrideInfo.getComment() : ""); //$NON-NLS-1$

        /*
         * Decide on a message and icon based on failure count and comment
         * presence.
         */

        if (failureData.length == 0) {
            if (policyOverrideInfo.getComment() != null) {
                /*
                 * This is the case where a shelveset was committed through
                 * gated checkin, and there were checkin policy failures. The
                 * override comment was tagged to the shelveset, but not the
                 * policy failures. So now we have a committed changeset with an
                 * override comment but no data on what was overridden. See
                 * Dev10 bug 671965.
                 */

                statusLabel.setText(Messages.getString("PolicyWarningsCheckinControl.PolicyViolationsUnknown")); //$NON-NLS-1$
                statusLabel.setImage(warningImage);
                showOverrideCommentText();
            } else {
                statusLabel.setText(Messages.getString("PolicyWarningsCheckinControl.SatisfiedStatusLabelText")); //$NON-NLS-1$
                statusLabel.setImage(null);
                hideOverrideCommentText();
            }
        } else {
            statusLabel.setText(Messages.getString("PolicyWarningsCheckinControl.NotSatisfiedStatusLabelText")); //$NON-NLS-1$
            statusLabel.setImage(warningImage);
            showOverrideCommentText();
        }
    }

    /**
     * Configures the sash form so the override comment text box is visible.
     */
    private void showOverrideCommentText() {
        sashForm.setMaximizedControl(null);
        sashForm.setWeights(new int[] {
            70,
            30
        });
    }

    /**
     * Configures the sash form so the override comment text box is not visible.
     */
    private void hideOverrideCommentText() {
        sashForm.setMaximizedControl(policyFailureTable);
    }

    private void doubleClick() {
        final PolicyFailureData policyFailure = policyFailureTable.getSelectedPolicyFailure();
        policyFailure.activate();
    }

    private void createActions() {
        evaluateAction = new Action() {
            @Override
            public void run() {
                evaluate();
            }
        };
        evaluateAction.setText(Messages.getString("PolicyWarningsCheckinControl.EvaluateActionText")); //$NON-NLS-1$

        helpAction = new Action() {
            @Override
            public void run() {
                final PolicyFailureData policyFailure = policyFailureTable.getSelectedPolicyFailure();

                if (policyFailure != null) {
                    policyFailure.displayHelp();
                }
            }
        };
        helpAction.setText(Messages.getString("PolicyWarningsCheckinControl.HelpActionText")); //$NON-NLS-1$
    }

    private void fillContextMenu(final IMenuManager manager) {
        if (evaluator == null) {
            return;
        }

        manager.add(evaluateAction);
        manager.add(new Separator());
        manager.add(helpAction);
    }

    /**
     * <p>
     * Sets the {@link PolicyEvaluator} that drives this control. The control
     * subscribes to events fired by the {@link PolicyEvaluator} in order to
     * refresh its contents.
     * </p>
     * <p>
     * Give <code>null</code> to disable event-driven control updating.
     * </p>
     *
     * @param evaluator
     *        the {@link PolicyEvaluator} that drives this control, or null to
     *        disable event-driven update of this control from a
     *        {@link PolicyEvaluator}
     */
    public void setEvaluator(final PolicyEvaluator evaluator) {
        /*
         * If we already have an evaluator, unhook events from it.
         */
        unhookEvaluatorEvents();

        this.evaluator = evaluator;

        if (evaluator != null) {
            /*
             * Hook up new listeners.
             *
             * These listeners can be fired by unknown threads (perhaps one the
             * policy implementation created). Marshall to the UI thread before
             * updating this control.
             */

            policyEvaluatorStateChangedListener = new PolicyEvaluatorStateChangedListener() {
                @Override
                public void onPolicyEvaluatorStateChanged(final PolicyEvaluatorStateChangedEvent e) {
                    PolicyWarningsCheckinControl.this.onPolicyEvaluatorStateChanged(e);
                }
            };

            policyStateChangedListener = new PolicyStateChangedListener() {
                @Override
                public void onPolicyStateChanged(final PolicyStateChangedEvent e) {
                    PolicyWarningsCheckinControl.this.onPolicyStateChanged(e);
                }
            };

            policyLoadErrorListener = new PolicyLoadErrorListener() {
                @Override
                public void onPolicyLoadError(final PolicyLoadErrorEvent e) {
                    PolicyWarningsCheckinControl.this.onPolicyLoadErrorEvent(e);
                }
            };

            evaluator.addPolicyEvaluatorStateChangedListener(policyEvaluatorStateChangedListener);
            evaluator.addPolicyStateChangedListener(policyStateChangedListener);
            evaluator.addPolicyLoadErrorListener(policyLoadErrorListener);

            evaluateIfVisibleAndNotEvaluated();
        }
    }

    @Override
    public void setVisible(final boolean visible) {
        super.setVisible(visible);

        if (visible) {
            log.trace("made visible"); //$NON-NLS-1$
        }

        evaluateIfVisibleAndNotEvaluated();
    }

    private void evaluateIfVisibleAndNotEvaluated() {
        if (getVisible()
            && evaluator != null
            && PolicyEvaluatorState.EVALUATED != evaluator.getPolicyEvaluatorState()) {
            log.trace("visible and needs evaluating, evaluating"); //$NON-NLS-1$
            evaluate();
        }
    }

    /**
     * Handles the dispose event for this control. Unhooks events from the
     * current evaluator, if there is one, and sets the evaluator to null. This
     * prevents the control from being updated after dispose.
     */
    private void widgetDisposed(final DisposeEvent e) {
        unhookEvaluatorEvents();
        evaluator = null;
    }

    /**
     * Unhooks all the event listeners from the current {@link PolicyEvaluator},
     * if there is one.
     */
    private void unhookEvaluatorEvents() {
        if (evaluator != null) {
            if (policyEvaluatorStateChangedListener != null) {
                evaluator.removePolicyEvaluatorStateChangedListener(policyEvaluatorStateChangedListener);
                policyEvaluatorStateChangedListener = null;
            }

            if (policyStateChangedListener != null) {
                evaluator.removePolicyStateChangedListener(policyStateChangedListener);
                policyStateChangedListener = null;
            }

            if (policyLoadErrorListener != null) {
                evaluator.removePolicyLoadErrorListener(policyLoadErrorListener);
                policyLoadErrorListener = null;
            }
        }
    }

    /**
     * Handles the event fired by the {@link PolicyEvaluator} when the state of
     * a single {@link PolicyInstance} object has changed (because of
     * re-evaluation, etc.).
     */
    private void onPolicyStateChanged(final PolicyStateChangedEvent e) {
        log.trace("a policy state has changed, updating failures table"); //$NON-NLS-1$

        final PolicyEvaluatorState state = evaluator.getPolicyEvaluatorState();
        final PolicyFailure[] failures = evaluator.getFailures();

        UIHelpers.runOnUIThread(getDisplay(), true, new Runnable() {
            @Override
            public void run() {
                if (isDisposed()) {
                    return;
                }

                updateFailuresTableAndSummary(state, failures);
            }
        });
    }

    /**
     * Handles the event fired by the {@link PolicyEvaluator} when a policy
     * instance fails to load.
     */
    private void onPolicyLoadErrorEvent(final PolicyLoadErrorEvent event) {
        if (isDisposed()) {
            return;
        }

        TFSCommonUIClientPlugin.getDefault().getConsole().printErrorMessage(
            PolicyEvaluator.makeTextErrorForLoadException(event.getError()));
    }

    /**
     * Creates a {@link PolicyContext} for use during evaluation, pre-filled
     * with the important context keys.
     *
     * @return a policy context object for use with check-in policies,
     *         initialized with the current product, SWT, and the given task
     *         monitor.
     */
    public PolicyContext createPolicyContext() {
        final PolicyContext context = new PolicyContext();

        context.addProperty(PolicyContextKeys.SWT_SHELL, getShell());
        context.addProperty(
            PolicyContextKeys.TFS_TEAM_PROJECT_COLLECTION,
            TFSCommonUIClientPlugin.getDefault().getProductPlugin().getServerManager().getDefaultServer().getConnection());

        final ProductName product = ProductInformation.getCurrent();
        if (product.equals(ProductName.PLUGIN)) {
            context.addProperty(PolicyContextKeys.RUNNING_PRODUCT_ECLIPSE_PLUGIN, new Object());
        }

        return context;
    }

    /**
     * This control's basic private evaluation method.
     */
    private void evaluate() {
        log.trace("evaluating"); //$NON-NLS-1$

        final EvaluateCheckinPoliciesCommand command =
            new EvaluateCheckinPoliciesCommand(evaluator, createPolicyContext(), false);

        final ICommandExecutor executor = UICommandExecutorFactory.newUICommandExecutor(getShell());

        final IStatus status = executor.execute(command);

        if (status.isOK()) {
            updateFailuresTableAndSummary(evaluator.getPolicyEvaluatorState(), command.getFailures());
        }
    }

    /**
     * Updates the failures table and summary information with the given state
     * and failures.
     */
    private void updateFailuresTableAndSummary(
        final PolicyEvaluatorState policyEvaluatorState,
        final PolicyFailure[] policyFailures) {
        /*
         * This triggers evaluator events to fire, which will eventually lead to
         * a refresh of our failure table control.
         */

        final PolicyContext context = createPolicyContext();

        final PolicyFailureData[] data = new PolicyFailureData[policyFailures.length];
        for (int i = 0; i < policyFailures.length; i++) {
            data[i] = PolicyFailureData.fromPolicyFailure(policyFailures[i], context);
        }
        policyFailureTable.setPolicyFailures(data);

        /*
         * Update the summary information.
         */

        if (evaluator == null) {
            /*
             * Shouldn't happen because this method is usually called after live
             * policy evaluation (or in response to one of the evaluator's
             * events), which doesn't happen when there isn't an evaluator.
             */
            return;
        }

        if (PolicyEvaluatorState.CANCELLED == policyEvaluatorState) {
            statusLabel.setText(Messages.getString("PolicyWarningsCheckinControl.CanceledStatusLabelText")); //$NON-NLS-1$
            statusLabel.setImage(warningImage);
            return;
        }

        if (PolicyEvaluatorState.POLICIES_LOAD_ERROR == policyEvaluatorState) {
            statusLabel.setText(Messages.getString("PolicyWarningsCheckinControl.LoadFailedStatusLabelText")); //$NON-NLS-1$
            statusLabel.setImage(warningImage);
            return;
        }

        if (PolicyEvaluatorState.UNEVALUATED == policyEvaluatorState) {
            statusLabel.setText(
                Messages.getString("PolicyWarningsCheckinControl.PoliciesNotYetEvaluatedStatusLabelText")); //$NON-NLS-1$
            statusLabel.setImage(null);
            return;
        }

        if (PolicyEvaluatorState.EVALUATED == policyEvaluatorState) {
            if (policyFailures.length > 0) {
                statusLabel.setText(
                    Messages.getString("PolicyWarningsCheckinControl.FollowingNotEvulatedStatusLabelText")); //$NON-NLS-1$
                statusLabel.setImage(warningImage);
                return;
            } else {
                statusLabel.setText(
                    Messages.getString("PolicyWarningsCheckinControl.CheckinPolicyStatisfiedStatusLabelText")); //$NON-NLS-1$
                statusLabel.setImage(null);
                return;
            }
        }
    }

    /**
     * Handles the event fired by the {@link PolicyEvaluator} when the overal
     * state of the evaluator changes (becomes unevaluated after policy
     * configuration changes, is finished evaluating, has a load error, etc.).
     */
    private void onPolicyEvaluatorStateChanged(final PolicyEvaluatorStateChangedEvent e) {
        /*
         * Post a runnable to the UI thread, which will happen some time after
         * this method runs if this method is running on the UI thread, or might
         * happen very quickly if this method is running on a different thread
         * (modal context).
         *
         * We can post multiple runnables for each evaluation state changed, and
         * the first one that gets run will trigger the evaluation, changing the
         * state of the evaluator to Evaluated, causing the subsequent runnables
         * to skip the evaluation and just update the failures table.
         */
        UIHelpers.runOnUIThread(getDisplay(), true, new Runnable() {
            @Override
            public void run() {
                if (isDisposed()) {
                    return;
                }

                final PolicyEvaluatorState evaluatorState = e.getEvaluator().getPolicyEvaluatorState();

                if (PolicyEvaluatorState.UNEVALUATED == evaluatorState && isVisible()) {
                    log.trace("control is visible and state is unevaluated, evaluating"); //$NON-NLS-1$
                    evaluate();
                } else {
                    log.trace(
                        "control is invisible or state is not unevaluated or cancelled, updating table and summary from existing failures"); //$NON-NLS-1$
                    updateFailuresTableAndSummary(evaluatorState, e.getEvaluator().getFailures());
                }
            }
        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addContributions(final IContributionManager contributionManager, final String groupName) {
        // No actions to add for this control.
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void removeContributions(final IContributionManager contributionManager, final String groupname) {
        // No actions to add for this control.
    }
}
