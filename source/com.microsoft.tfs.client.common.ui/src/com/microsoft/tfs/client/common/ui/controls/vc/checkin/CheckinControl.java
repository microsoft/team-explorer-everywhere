// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.controls.vc.checkin;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.IPostSelectionProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;

import com.microsoft.tfs.checkinpolicies.ExtensionPointPolicyLoader;
import com.microsoft.tfs.client.common.codemarker.CodeMarker;
import com.microsoft.tfs.client.common.codemarker.CodeMarkerDispatch;
import com.microsoft.tfs.client.common.commands.EvaluatePendingCheckinCommand;
import com.microsoft.tfs.client.common.commands.vc.RefreshPendingChangesCommand;
import com.microsoft.tfs.client.common.framework.command.ICommandExecutor;
import com.microsoft.tfs.client.common.repository.TFSRepository;
import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.TFSCommonUIClientPlugin;
import com.microsoft.tfs.client.common.ui.controls.generic.BaseControl;
import com.microsoft.tfs.client.common.ui.controls.generic.Separator;
import com.microsoft.tfs.client.common.ui.controls.vc.changes.ChangeItem;
import com.microsoft.tfs.client.common.ui.controls.vc.changes.ChangeItemProvider;
import com.microsoft.tfs.client.common.ui.controls.vc.changes.ChangeItemProviderEvent;
import com.microsoft.tfs.client.common.ui.controls.vc.changes.ChangeItemProviderListener;
import com.microsoft.tfs.client.common.ui.dialogs.vc.checkinpolicies.OverridePolicyFailuresDialog;
import com.microsoft.tfs.client.common.ui.framework.command.UICommandExecutorFactory;
import com.microsoft.tfs.client.common.ui.framework.helper.UIHelpers;
import com.microsoft.tfs.client.common.ui.framework.image.ImageHelper;
import com.microsoft.tfs.client.common.ui.framework.layout.GridDataBuilder;
import com.microsoft.tfs.client.common.ui.framework.selection.SwitchingSelectionProvider;
import com.microsoft.tfs.client.common.ui.helpers.EditorHelper;
import com.microsoft.tfs.client.common.util.ConnectionHelper;
import com.microsoft.tfs.core.checkinpolicies.PolicyEvaluator;
import com.microsoft.tfs.core.checkinpolicies.PolicyEvaluatorState;
import com.microsoft.tfs.core.checkinpolicies.PolicyFailure;
import com.microsoft.tfs.core.clients.versioncontrol.GatedCheckinUtils;
import com.microsoft.tfs.core.clients.versioncontrol.VersionControlConstants;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Changeset;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.CheckinNote;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.PendingChange;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.WorkItemCheckinInfo;
import com.microsoft.tfs.core.clients.workitem.WorkItem;
import com.microsoft.tfs.core.pendingcheckin.CheckinConflict;
import com.microsoft.tfs.core.pendingcheckin.CheckinEvaluationOptions;
import com.microsoft.tfs.core.pendingcheckin.CheckinEvaluationResult;
import com.microsoft.tfs.core.pendingcheckin.CheckinNoteFailure;
import com.microsoft.tfs.core.pendingcheckin.PendingCheckin;
import com.microsoft.tfs.core.pendingcheckin.StandardPendingCheckin;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.listeners.SingleListenerFacade;

public class CheckinControl extends BaseControl implements IPostSelectionProvider {
    private final static Log log = LogFactory.getLog(CheckinControl.class);

    /**
     * The name of a contribution group ID. If the parent control hosting a
     * {@link CheckinControl} provides an external contribution manager, this
     * group must be present. Sub-control actions will be added to this group.
     */
    public static final String SUBCONTROL_CONTRIBUTION_GROUP_NAME = "subcontrol-contribution-group"; //$NON-NLS-1$

    public static final CodeMarker AFTER_CHECKIN =
        new CodeMarker("com.microsoft.tfs.client.common.ui.controls.vc.checkin.CheckinControl#afterCheckin"); //$NON-NLS-1$

    public static final CodeMarker CHANGE_ITEMS_UPDATED =
        new CodeMarker("com.microsoft.tfs.client.common.ui.controls.vc.checkin.CheckinControl#changeItemsUpdated"); //$NON-NLS-1$

    /**
     * Returned by {@link CheckinControl#validateForCheckin()}.
     *
     * @threadsafety unknown
     */
    public static class ValidationResult {
        private final boolean succeeded;
        private final PolicyFailure[] policyFailures;
        private final String policyOverrideReason;
        private final CheckinConflict[] conflicts;

        /**
         * Creates a {@link ValidationResult}. If succeeded is false,
         * policyFailures and policyOverrideReason should be false. If succeeded
         * is true, policyFailures may be null or non-null. If succeeded is true
         * and policyFailures is non-null and non-empty, then
         * policyOverrideReason must be non-null and non-empty.
         */
        public ValidationResult(
            final boolean succeeded,
            final PolicyFailure[] policyFailures,
            final String policyOverrideReason,
            final CheckinConflict[] conflicts) {
            this.succeeded = succeeded;

            if (succeeded && policyFailures != null && policyFailures.length > 0) {
                Check.notNullOrEmpty(policyOverrideReason, "policyOverrideReason"); //$NON-NLS-1$
            }

            this.policyFailures = policyFailures;
            this.policyOverrideReason = policyOverrideReason;
            this.conflicts = conflicts;
        }

        public boolean getSucceeded() {
            return succeeded;
        }

        public CheckinConflict[] getConflicts() {
            return conflicts;
        }

        public PolicyFailure[] getPolicyFailures() {
            return policyFailures;
        }

        public String getPolicyOverrideReason() {
            return policyOverrideReason;
        }
    }

    private static final String SOURCE_FILES_TOOLBAR_IMAGE = "images/pendingchanges/source_files.gif"; //$NON-NLS-1$
    private static final String POLICY_WARNINGS_TOOLBAR_IMAGE = "images/pendingchanges/policy_warnings.gif"; //$NON-NLS-1$
    private static final String WORK_ITEMS_TOOLBAR_IMAGE = "images/pendingchanges/work_items.gif"; //$NON-NLS-1$
    private static final String CHECKIN_NOTES_TOOLBAR_IMAGE = "images/pendingchanges/checkin_notes.gif"; //$NON-NLS-1$

    private final CheckinControlOptions options;
    private final ImageHelper imageHelper;
    private final ChangeItemProviderListener changeItemProviderListener;
    private final SingleListenerFacade subControlListeners = new SingleListenerFacade(CheckinSubControlListener.class);

    private ToolItem[] subControlToolItems;

    private SourceFilesCheckinControl sourceFilesCheckinControl;
    private WorkItemsCheckinControl workItemsCheckinControl;
    private NotesCheckinControl notesCheckinControl;
    private PolicyWarningsCheckinControl policyWarningsCheckinControl;

    private OfflineCheckinControl offlineCheckinControl;
    private ConnectionFailureCheckinControl connectionFailureCheckinControl;

    private AbstractCheckinSubControl visibleControl;

    /**
     * <p>
     * The {@link PendingCheckin} contains the current changes, work items,
     * notes, and configured policies that make up this control's aggregate
     * checkin data. This model object exists primarily for check-in policy
     * evaluation and pre-check-in validation ({@link #validateForCheckin()})
     * and it is updated through event listeners on subcontrols.
     * </p>
     * <p>
     * Assigned to a new instance when {@link #setRepository(TFSRepository)} is
     * called, and goes <code>null</code> when
     * {@link #setRepository(TFSRepository)} is called with a <code>null</code>
     * {@link TFSRepository}. When non-<code>null</code>, the instance is
     * updated in response to events from this control's sub-controls (check box
     * selection, comment text, check-in note changes, etc.) via
     * {@link policyPendingCheckinUpdater}.
     * </p>
     */
    private PendingCheckin pendingCheckin;

    /**
     * <p>
     * Allocated when {@link #pendingCheckin} is assigned, does the event
     * listenting on controls and updates {@link #pendingCheckin}.
     * </p>
     * <p>
     * Assigned to a new instance when {@link #setRepository(TFSRepository)} is
     * called, and goes <code>null</code> when
     * {@link #setRepository(TFSRepository)} is called with a <code>null</code>
     * {@link TFSRepository}.
     * </p>
     */
    private PendingCheckinUpdater pendingCheckinUpdater;

    /**
     * <p>
     * Performs policy loading and evaluation (a headless task). Evaluation is
     * driven by both user UI events and by events fired by the
     * {@link PendingCheckin} which the {@link PolicyEvaluator} watches for
     * changes. When a {@link PolicyEvaluator} is allocated, it is set on the
     * {@link PolicyWarningsCheckinControl} subcontrol where it replaces any
     * previous {@link PolicyEvaluator}.
     * </p>
     * <p>
     * Assigned to a new instance whenever {@link #setRepository(TFSRepository)}
     * is called and the options permit policy evaluation, and goes
     * <code>null</code> when {@link #setRepository(TFSRepository)} is called
     * with a <code>null</code> {@link TFSRepository}.
     * </p>
     * <p>
     * <b>NOTE:</b> It is important that {@link PolicyEvaluator#close()} is
     * called on this instance (if it is non-<code>null</code>) when this
     * control is disposed. This allows the evaluator to shut down the
     * individual policy implementations, which may be supplied by a third
     * party, and may hold operating system resources until they are disposed.
     * </p>
     */
    private PolicyEvaluator policyEvaluator;

    private StackLayout contentAreaStackLayout;
    private Composite contentArea;

    private ChangeItemProvider changeItemProvider;

    private final SwitchingSelectionProvider switchingSelectionProvider = new SwitchingSelectionProvider();

    private TFSRepository repository;

    public CheckinControl(final Composite parent, final int style, final CheckinControlOptions options) {
        super(parent, style);

        Check.notNull(options, "options"); //$NON-NLS-1$

        this.options = new CheckinControlOptions(options);
        imageHelper = new ImageHelper(TFSCommonUIClientPlugin.PLUGIN_ID);
        changeItemProviderListener = new ChangeItemProviderListener() {
            @Override
            public void onChangeItemsUpdated(final ChangeItemProviderEvent event) {
                CheckinControl.this.onChangeItemsUpdated(event);
            }
        };

        final GridLayout layout = new GridLayout(options.isForDialog() ? 2 : 3, false);
        layout.marginHeight = 0;
        layout.marginWidth = 0;
        layout.horizontalSpacing = 0;
        layout.verticalSpacing = 0;
        setLayout(layout);

        final Composite buttonsArea = createSubControlToolBar(this, options);
        GridDataBuilder.newInstance().vGrab().vFill().applyTo(buttonsArea);

        if (!options.isForDialog()) {
            final Separator vSep = new Separator(this, SWT.VERTICAL);
            GridDataBuilder.newInstance().vGrab().vFill().applyTo(vSep);
        }

        final Composite contentArea = createContent(this);
        GridDataBuilder contentAreaDataBuilder = GridDataBuilder.newInstance().grab().fill().vSpan(2);

        if (options.isForDialog()) {
            contentAreaDataBuilder = contentAreaDataBuilder.hIndent(getHorizontalSpacing() * 2);
        }

        contentAreaDataBuilder.applyTo(contentArea);

        addDisposeListener(new DisposeListener() {
            @Override
            public void widgetDisposed(final DisposeEvent e) {
                /*
                 * Close/unhook/whatever the policy evaluation objects, which
                 * closes all policy implementations.
                 */
                disposePendingCheckinAndPolicyEvaluatorFields();

                imageHelper.dispose();
            }
        });

        /*
         * default sub control to show is source files
         */
        showSubControl(CheckinSubControlType.SOURCE_FILES);
    }

    public void setChangeItemProvider(final ChangeItemProvider provider) {
        if (changeItemProvider != null) {
            changeItemProvider.removeListener(changeItemProviderListener);
        }

        changeItemProvider = provider;

        if (changeItemProvider != null) {
            final ChangeItem[] changeItems = changeItemProvider.getChangeItems();
            sourceFilesCheckinControl.getChangesTable().setChangeItems(changeItems, changeItemProvider.getType());

            /*
             * DO NOT FIRE an event here, as we want synchronous updates and
             * we're on the UI thread. Otherwise, we have a race condition with
             * check state enablement by callers in typical usage.
             */
            changeItemProvider.addListener(changeItemProviderListener);
        } else {
            sourceFilesCheckinControl.getChangesTable().setChangeItems(null, null);
        }
    }

    public void showSourceFilesSubControl() {
        showSubControl(CheckinSubControlType.SOURCE_FILES);
    }

    public void showWorkItemSubControl() {
        showSubControl(CheckinSubControlType.WORK_ITEMS);
    }

    public void showCheckinNotesSubControl() {
        showSubControl(CheckinSubControlType.CHECKIN_NOTES);
    }

    public void showCheckinPolicyWarningsSubControl() {
        showSubControl(CheckinSubControlType.POLICY_WARNINGS);
    }

    public SourceFilesCheckinControl getSourceFilesSubControl() {
        return sourceFilesCheckinControl;
    }

    public WorkItemsCheckinControl getWorkItemSubControl() {
        return workItemsCheckinControl;
    }

    public NotesCheckinControl getNotesSubControl() {
        return notesCheckinControl;
    }

    public PolicyWarningsCheckinControl getPolicyWarningsSubControl() {
        return policyWarningsCheckinControl;
    }

    public AbstractCheckinSubControl getVisibleSubControl() {
        return visibleControl;
    }

    public AbstractCheckinSubControl[] getSubControls() {
        final Control[] controls = contentArea.getChildren();
        final AbstractCheckinSubControl[] subControls = new AbstractCheckinSubControl[controls.length];

        for (int i = 0; i < controls.length; i++) {
            subControls[i] = (AbstractCheckinSubControl) controls[i];
        }

        return subControls;
    }

    public void addSubControlListener(final CheckinSubControlListener listener) {
        subControlListeners.addListener(listener);
    }

    public void removeSubControlListener(final CheckinSubControlListener listener) {
        subControlListeners.removeListener(listener);
    }

    @Override
    public boolean setFocus() {
        if (getVisibleSubControl() != null) {
            return getVisibleSubControl().setFocus();
        }
        return super.setFocus();
    }

    public void setRepository(final TFSRepository repository) {
        this.repository = repository;

        /*
         * Clear out any existing pending checkin data and evaluators.
         */
        disposePendingCheckinAndPolicyEvaluatorFields();

        /* Repository may be null (when we're disconnected from a server) */
        if (repository == null) {
            policyEvaluator = null;
            pendingCheckin = null;
            pendingCheckinUpdater = null;

            sourceFilesCheckinControl.setRepository(repository);
            policyWarningsCheckinControl.setEvaluator(policyEvaluator);
            workItemsCheckinControl.setRepository(repository);
            notesCheckinControl.setPendingCheckin(repository, pendingCheckin);
            connectionFailureCheckinControl.setRepository(repository);

            showSubControl(CheckinSubControlType.OFFLINE);

            /*
             * Turn off all other toolbar items so that the control cannot be
             * changed from the offline control.
             */
            for (int i = 0; i < subControlToolItems.length; i++) {
                subControlToolItems[i].setEnabled(false);
            }

            return;
        }

        /* Turn on sub control items so that controls may be changed. */
        for (int i = 0; i < subControlToolItems.length; i++) {
            subControlToolItems[i].setEnabled(true);
        }

        /*
         * Create an evaluator that uses extension points to load policies.
         */
        if (options.isPolicyEvaluationEnabled()) {
            policyEvaluator =
                new PolicyEvaluator(repository.getVersionControlClient(), new ExtensionPointPolicyLoader());
        }

        /*
         * Create a pending checkin which will be updated in this class in
         * response to sub-control change events. This is done even if
         * evaluation is not desired, so users can get modified data from this
         * control via the pending checkin. Use a null
         */
        pendingCheckin =
            new StandardPendingCheckin(
                repository.getWorkspace(),
                new PendingChange[0],
                new PendingChange[0],
                "", //$NON-NLS-1$
                new CheckinNote(),
                new WorkItemCheckinInfo[0],
                policyEvaluator);

        if (options.isPolicyEvaluationEnabled()) {
            /*
             * Set the pending checkin on the evaluator. The evaluator
             * subscribes to some update events from the pending checkin.
             */
            policyEvaluator.setPendingCheckin(pendingCheckin);
        }

        /*
         * Instead of hooking up event handlers for each subcontrol that might
         * change the PendingCheckin, use the handy updater class to do the
         * updating. It registers for the events on construction and updates the
         * PendingCheckin as they fire, until it is unhooked.
         *
         * Only allocate an updater if check boxes are visible and policy
         * evaluation is desired. Check boxes are disabled on the control when
         * it's used for historical display (showing Change items, not
         * PendingChange items), and we don't need to do policy evaluation
         * anyway.
         */
        if (options.isSourceFilesCheckboxes() && options.isPolicyEvaluationEnabled()) {
            pendingCheckinUpdater = new PendingCheckinUpdater(pendingCheckin, this);
        }

        /*
         * Set the evaluator on the subcontrols. The evaluator may may have gone
         * null (which causes the control to stop evaluating).
         */

        showSubControl(CheckinSubControlType.SOURCE_FILES);

        sourceFilesCheckinControl.setRepository(repository);
        policyWarningsCheckinControl.setEvaluator(policyEvaluator);
        workItemsCheckinControl.setRepository(repository);
        notesCheckinControl.setPendingCheckin(repository, pendingCheckin);
        connectionFailureCheckinControl.setRepository(repository);
    }

    public TFSRepository getRepository() {
        return repository;
    }

    public void refreshConnection() {
        /*
         * Update the subcontrols so that those that are offline are not
         * visible.
         */
        showSubControl(getVisibleSubControl().getSubControlType());
    }

    /*
     * BEGIN: IPostSelectionProvider methods
     */

    @Override
    public void addPostSelectionChangedListener(final ISelectionChangedListener listener) {
        switchingSelectionProvider.addPostSelectionChangedListener(listener);
    }

    @Override
    public void removePostSelectionChangedListener(final ISelectionChangedListener listener) {
        switchingSelectionProvider.removePostSelectionChangedListener(listener);
    }

    @Override
    public void addSelectionChangedListener(final ISelectionChangedListener listener) {
        switchingSelectionProvider.addSelectionChangedListener(listener);
    }

    @Override
    public void removeSelectionChangedListener(final ISelectionChangedListener listener) {
        switchingSelectionProvider.removeSelectionChangedListener(listener);
    }

    @Override
    public ISelection getSelection() {
        return switchingSelectionProvider.getSelection();
    }

    @Override
    public void setSelection(final ISelection selection) {
        switchingSelectionProvider.setSelection(selection);
    }

    /*
     * END: IPostSelectionProvider methods
     */

    private void onChangeItemsUpdated(final ChangeItemProviderEvent event) {
        UIHelpers.runOnUIThread(getDisplay(), true, new Runnable() {
            @Override
            public void run() {
                final ChangeItem[] changeItems = changeItemProvider.getChangeItems();
                sourceFilesCheckinControl.getChangesTable().setChangeItems(changeItems, changeItemProvider.getType());

                CodeMarkerDispatch.dispatch(CHANGE_ITEMS_UPDATED);
            }
        });
    }

    private Composite createSubControlToolBar(final Composite parent, final CheckinControlOptions options) {
        final Composite composite = new Composite(parent, options.isForDialog() ? SWT.BORDER : SWT.NONE);
        final GridLayout compositeLayout = new GridLayout(1, false);
        compositeLayout.marginWidth = 0;
        compositeLayout.marginHeight = 0;
        compositeLayout.horizontalSpacing = 0;
        compositeLayout.verticalSpacing = 0;

        composite.setLayout(compositeLayout);

        final ToolBar toolBar = new ToolBar(composite, SWT.VERTICAL | SWT.FLAT);
        GridDataBuilder.newInstance().grab().fill().applyTo(toolBar);

        final List<ToolItem> subControlToolItemList = new ArrayList<ToolItem>();

        /*
         * Note: we do not add the offline subcontrol to the toolbar. It should
         * only ever be shown by us and not available to users.
         *
         * Further note that we optionally do not show policy warnings
         * subcontrol in the toolbar. Shelve and shelveset details dialogs
         * should not show policy warnings.
         */

        subControlToolItemList.add(
            createSubControlToolItem(
                toolBar,
                SOURCE_FILES_TOOLBAR_IMAGE,
                Messages.getString("CheckinControl.SourceFilesChannelTitle"), //$NON-NLS-1$
                CheckinSubControlType.SOURCE_FILES));

        subControlToolItemList.add(
            createSubControlToolItem(
                toolBar,
                WORK_ITEMS_TOOLBAR_IMAGE,
                Messages.getString("CheckinControl.WorkItemsChannelTitle"), //$NON-NLS-1$
                CheckinSubControlType.WORK_ITEMS));

        subControlToolItemList.add(
            createSubControlToolItem(
                toolBar,
                CHECKIN_NOTES_TOOLBAR_IMAGE,
                Messages.getString("CheckinControl.CheckinNotesChannelTitle"), //$NON-NLS-1$
                CheckinSubControlType.CHECKIN_NOTES));

        if (options.isPolicyDisplayed() == true) {
            subControlToolItemList.add(
                createSubControlToolItem(
                    toolBar,
                    POLICY_WARNINGS_TOOLBAR_IMAGE,
                    Messages.getString("CheckinControl.PolicyWarningsChannelTitle"), //$NON-NLS-1$
                    CheckinSubControlType.POLICY_WARNINGS));
        }

        subControlToolItems = subControlToolItemList.toArray(new ToolItem[subControlToolItemList.size()]);

        return composite;
    }

    private ToolItem createSubControlToolItem(
        final ToolBar toolBar,
        final String imageFilePath,
        final String text,
        final CheckinSubControlType subControlType) {
        final ToolItem toolItem = new ToolItem(toolBar, SWT.CHECK);

        toolItem.setData(subControlType);

        toolItem.setImage(imageHelper.getImage(imageFilePath));
        toolItem.setToolTipText(text);
        if (options.isForDialog()) {
            toolItem.setText(text);
        }

        toolItem.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
                final CheckinSubControlType subControlType = (CheckinSubControlType) e.widget.getData();
                showSubControl(subControlType);
            }
        });

        return toolItem;
    }

    private void showSubControl(final CheckinSubControlType subControlType) {
        /*
         * Ensure that the correct sub-control toolbar button is selected, and
         * no other toolbar buttons are selected.
         */
        for (int i = 0; i < subControlToolItems.length; i++) {
            subControlToolItems[i].setSelection(subControlToolItems[i].getData() == subControlType);
        }

        final AbstractCheckinSubControl currentSubControl = visibleControl;
        AbstractCheckinSubControl newSubControl;

        if (CheckinSubControlType.SOURCE_FILES == subControlType) {
            newSubControl = sourceFilesCheckinControl;
        } else if (CheckinSubControlType.WORK_ITEMS == subControlType) {
            newSubControl = workItemsCheckinControl;
        } else if (CheckinSubControlType.CHECKIN_NOTES == subControlType) {
            newSubControl = notesCheckinControl;
        } else if (CheckinSubControlType.POLICY_WARNINGS == subControlType) {
            newSubControl = policyWarningsCheckinControl;
        } else if (CheckinSubControlType.OFFLINE == subControlType) {
            newSubControl = offlineCheckinControl;
        } else {
            newSubControl = null;
        }

        final CheckinSubControlListener listener = (CheckinSubControlListener) subControlListeners.getListener();

        /*
         * remove the old sub-control's contributions from the contribution
         * manager (if there is one)
         */
        if (currentSubControl != null) {
            if (options.getExternalContributionManager() != null) {
                currentSubControl.removeContributions(
                    options.getExternalContributionManager(),
                    SUBCONTROL_CONTRIBUTION_GROUP_NAME);
                options.getExternalContributionManager().update(false);
            }

            /*
             * set the content area's top control to null - this will ensure
             * that sub control listeners will see null for the currently
             * visible sub control during the "on hidden" event
             */
            visibleControl = null;
            listener.onSubControlHidden(new CheckinSubControlEvent(currentSubControl));
        }

        /*
         * make the new sub control visible.
         */
        visibleControl = newSubControl;
        contentAreaStackLayout.topControl =
            shouldShowConnectionFailure(newSubControl) ? connectionFailureCheckinControl : newSubControl;
        contentArea.layout();

        /*
         * add the new sub-control's contributions to the contribution manager
         * (if there is one)
         */
        if (newSubControl != null) {
            if (options.getExternalContributionManager() != null) {
                newSubControl.addContributions(
                    options.getExternalContributionManager(),
                    SUBCONTROL_CONTRIBUTION_GROUP_NAME);
                options.getExternalContributionManager().update(false);
            }

            newSubControl.setFocus();

            listener.onSubControlVisible(new CheckinSubControlEvent(newSubControl));
        }

        /*
         * switch the selection provider to the new sub-control's selection
         * provider, if any
         */
        final ISelectionProvider newSelectionProvider =
            (newSubControl == null ? null : newSubControl.getSelectionProvider());
        switchingSelectionProvider.setSelectionProvider(newSelectionProvider);
    }

    private boolean shouldShowConnectionFailure(final AbstractCheckinSubControl control) {
        /* Always show change items. */
        if (control == sourceFilesCheckinControl) {
            return false;
        }

        return (repository != null && !ConnectionHelper.isConnected(repository.getConnection()));
    }

    private Composite createContent(final Composite parent) {
        contentArea = new Composite(parent, SWT.NONE);
        contentAreaStackLayout = new StackLayout();
        contentArea.setLayout(contentAreaStackLayout);

        sourceFilesCheckinControl = new SourceFilesCheckinControl(contentArea, SWT.NONE, options);
        workItemsCheckinControl = new WorkItemsCheckinControl(contentArea, SWT.NONE, options);
        notesCheckinControl = new NotesCheckinControl(contentArea, SWT.NONE, options);
        policyWarningsCheckinControl = new PolicyWarningsCheckinControl(contentArea, SWT.NONE, options, this);

        offlineCheckinControl = new OfflineCheckinControl(contentArea, SWT.NONE, options);
        connectionFailureCheckinControl = new ConnectionFailureCheckinControl(contentArea, SWT.NONE, options);

        return contentArea;
    }

    /**
     * Correctly closes, unhooks, or otherwise disposes of the
     * {@link #policyEvaluator}, {@link #pendingCheckinUpdater}, and
     * {@link #pendingCheckin} fields, setting them to <code>null</code>.
     */
    private void disposePendingCheckinAndPolicyEvaluatorFields() {
        if (policyEvaluator != null) {
            policyEvaluator.close();
            policyEvaluator = null;
        }

        if (pendingCheckinUpdater != null) {
            pendingCheckinUpdater.unhookListeners();
            pendingCheckinUpdater = null;
        }

        pendingCheckin = null;
    }

    /**
     * Validates this control's {@link PendingCheckin} (notes are checked, work
     * item states are validated, policies are evaluated), which a caller
     * usually does before a check-in. May prompt the user to save some dirty
     * editors. Raises dialogs to notify the user of invalid data. The caller
     * should check the return value to see if the validation was ultimately a
     * success, and if it was, whether there is policy override information that
     * must be sent for the check-in.
     *
     * @return a {@link ValidationResult} containing the results of the
     *         validation
     */
    public ValidationResult validateForCheckin() {
        /*
         * Have the work item control validate the checkin. We must call this
         * before calling saveDirtyEditors(), since the work item control has
         * special handling for dirty work item editors.
         */
        if (getWorkItemSubControl().validateForCheckin() == false) {
            return new ValidationResult(false, null, null, null);
        }

        /*
         * Save any dirty editors which may be editing files or work items to be
         * included in this check-in. All other dirty editors are excluded.
         */
        final PendingCheckinSaveableFilter filter = new PendingCheckinSaveableFilter(pendingCheckin);
        if (EditorHelper.saveAllDirtyEditors(filter) == false) {
            return new ValidationResult(false, null, null, null);
        }

        /*
         * Use core's evaluation (via Workspace), run as a command.
         */
        final EvaluatePendingCheckinCommand command = new EvaluatePendingCheckinCommand(
            getRepository().getWorkspace(),
            CheckinEvaluationOptions.ALL,
            pendingCheckin,
            getPolicyWarningsSubControl().createPolicyContext());

        final ICommandExecutor executor = UICommandExecutorFactory.newUICommandExecutor(getShell());

        /*
         * Non-OK status means the user cancelled some part of evaluation.
         */
        if (executor.execute(command) != Status.OK_STATUS) {
            return new ValidationResult(false, null, null, null);
        }

        /*
         * Get the result of the evaluation.
         */
        final CheckinEvaluationResult evaluationResult = command.getEvaluationResult();

        /*
         * See if checkin conflicts contain an error from the server (from the
         * checkPendingChanges SOAP call)
         */
        if (validateCheckinConflicts(evaluationResult) == false) {
            return new ValidationResult(false, null, null, evaluationResult.getConflicts());
        }

        /*
         * See if checkin notes evaluated correctly. If there were failures,
         * return the
         */
        if (validateEvaluationResultNotes(evaluationResult) == false) {
            return new ValidationResult(false, null, null, evaluationResult.getConflicts());
        }

        /*
         * Test policies (last), which returns a complete ValidationResult
         * containing the user's override comment (if required).
         */
        return validateEvaluationResultPolicies(evaluationResult);
    }

    /**
     * Validates just the checkin conflicts that were raised by the server in
     * the checkPendingChanges SOAP call, notifying the user of errors via
     * dialog.
     *
     * @param evaluationResult
     *        the core evaluation result (must not be <code>null</code>)
     * @return true if there were no checkin conflict problems, false if the
     *         server raised an error
     */
    private boolean validateCheckinConflicts(final CheckinEvaluationResult evaluationResult) {
        final CheckinConflict[] conflicts = evaluationResult.getConflicts();

        if (conflicts.length == 0) {
            return true;
        }

        /*
         * Search for an ITEM_NOT_CHECKED_OUT_EXCEPTION code. This is the only
         * error code we wish to validate. Other checkin exceptions should
         * properly be handled by the conflict resolution mechanism.
         */
        for (int i = 0; i < conflicts.length; i++) {
            if (VersionControlConstants.ITEM_NOT_CHECKED_OUT_EXCEPTION.equals(conflicts[i].getCode())) {
                showSourceFilesSubControl();

                final RefreshPendingChangesCommand refreshCommand = new RefreshPendingChangesCommand(repository);
                final ICommandExecutor executor = UICommandExecutorFactory.newUICommandExecutor(getShell());

                executor.execute(refreshCommand);

                MessageDialog.openError(
                    getShell(),
                    Messages.getString("CheckinControl.CheckinConflictsDialogTitle"), //$NON-NLS-1$
                    Messages.getString("CheckinControl.PendingChangesOutdated")); //$NON-NLS-1$

                return false;
            }
        }

        return true;
    }

    /**
     * Validates just the checkin note part of a {@link CheckinEvaluationResult}
     * notifying the user of missing notes via dialog. .
     *
     * @param evaluationResult
     *        the core evaluation result (must not be <code>null</code>)
     * @return true if there were no note problems, false if some required notes
     *         were missing
     */
    private boolean validateEvaluationResultNotes(final CheckinEvaluationResult evaluationResult) {
        final CheckinNoteFailure[] failures = evaluationResult.getNoteFailures();
        final IStatus[] failureStatus = new IStatus[failures.length];

        if (failures.length == 0) {
            return true;
        }

        for (int i = 0; i < failures.length; i++) {
            failureStatus[i] = new Status(
                Status.ERROR,
                TFSCommonUIClientPlugin.PLUGIN_ID,
                0,
                MessageFormat.format(
                    Messages.getString("CheckinControl.CheckinNoteIsRequiredFormat"), //$NON-NLS-1$
                    failures[i].getDefinition().getName()),
                null);
        }

        IStatus displayStatus;
        if (failures.length == 1) {
            displayStatus = failureStatus[0];
        } else {
            displayStatus = new MultiStatus(
                TFSCommonUIClientPlugin.PLUGIN_ID,
                0,
                failureStatus,
                Messages.getString("CheckinControl.SomeCheckinNotesAreMissingRequiredValues"), //$NON-NLS-1$
                null);
        }

        showCheckinNotesSubControl();
        ErrorDialog.openError(
            getShell(),
            Messages.getString("CheckinControl.CheckinNotesAreRequiredDialogTitle"), //$NON-NLS-1$
            null,
            displayStatus);

        return false;
    }

    /**
     * Validates just the checkin policy part of a
     * {@link CheckinEvaluationResult}, prompting for an override.
     *
     * @param evaluationResult
     *        the core evaluation result (must not be <code>null</code>)
     * @return a {@link ValidationResult} with a success of true if there were
     *         no policy failures or if there were failures but the user
     *         supplied an override comment, or a success of false if there were
     *         failures and no override comment was supplied
     */
    private ValidationResult validateEvaluationResultPolicies(final CheckinEvaluationResult evaluationResult) {
        Check.notNull(evaluationResult, "evaluationResult"); //$NON-NLS-1$

        /*
         * Check policy failures.
         */
        final PolicyEvaluatorState evaluatorState = evaluationResult.getPolicyEvaluatorState();
        final PolicyFailure[] failures = evaluationResult.getPolicyFailures();

        if (evaluatorState == PolicyEvaluatorState.POLICIES_LOAD_ERROR || failures.length > 0) {
            String customMessage = null;

            if (evaluatorState == PolicyEvaluatorState.POLICIES_LOAD_ERROR) {
                customMessage = Messages.getString("CheckinControl.PoliciesLoadErrorMultilineText"); //$NON-NLS-1$

            }

            // Passing a null message uses the default message.
            final OverridePolicyFailuresDialog dialog = new OverridePolicyFailuresDialog(getShell(), customMessage);

            if (IDialogConstants.OK_ID != dialog.open()) {
                return new ValidationResult(false, null, null, evaluationResult.getConflicts());
            }

            /*
             * Return with override comment.
             */
            return new ValidationResult(true, failures, dialog.getOverrideComment(), evaluationResult.getConflicts());
        }

        /*
         * No failures.
         */
        return new ValidationResult(true, null, null, evaluationResult.getConflicts());
    }

    public void afterCheckin() {
        getSourceFilesSubControl().afterCheckin();
        getWorkItemSubControl().afterCheckin();
        getNotesSubControl().afterCheckin();

        CodeMarkerDispatch.dispatch(AFTER_CHECKIN);
    }

    public void afterShelve() {
        /* Treat a shelve where all are undone as a checkin */
        if (getSourceFilesSubControl().getChangesTable().getChangeItems().length == 0) {
            afterCheckin();
        }
    }

    /**
     * Call after a gated build changeset has been reconciled (unmodified local
     * changes undone and new items fetched). Clears the comment, associated
     * work items, and notes if and only if the committed changeset and
     * associated items exactly matches the control's current state.
     *
     * @param changeset
     *        the committed changeset (must not be <code>null</code>)
     * @param associatedWorkItems
     *        the associated work items for the changeset (must not be
     *        <code>null</code>)
     */
    public void afterReconcileGatedCheckin(final Changeset changeset, final WorkItem[] associatedWorkItems) {
        Check.notNull(changeset, "changeset"); //$NON-NLS-1$
        Check.notNull(associatedWorkItems, "associatedWorkItems"); //$NON-NLS-1$

        final boolean commentsMatch = GatedCheckinUtils.gatedCheckinCommentsMatch(
            changeset.getComment(),
            getSourceFilesSubControl().getComment());

        final boolean checkinNotesMatch =
            GatedCheckinUtils.gatedCheckinNotesMatch(changeset.getCheckinNote(), getNotesSubControl().getCheckinNote());

        final int[] committedWorkItemIds = WorkItemsCheckinControl.getIDsForWorkItems(associatedWorkItems);

        final WorkItemCheckinInfo[] checkedWorkItemInfos =
            getWorkItemSubControl().getWorkItemTable().getCheckedWorkItems();
        final int[] pendingWorkItemIds = WorkItemsCheckinControl.getIDsForWorkItemCheckinInfos(checkedWorkItemInfos);

        final boolean workItemsMatch =
            GatedCheckinUtils.gatedCheckinWorkItemsMatch(committedWorkItemIds, pendingWorkItemIds);

        if (commentsMatch && checkinNotesMatch && workItemsMatch) {
            // Resets the comments, notes, and work items
            getSourceFilesSubControl().afterCheckin();
            getWorkItemSubControl().afterCheckin();
            getNotesSubControl().afterCheckin();
        }
    }

    /**
     * Do not make changes to the {@link PendingCheckin} object returned, except
     * to add or remove event listeners
     *
     * @return the {@link PendingCheckin} this control is updating, which will
     *         be <code>null</code> if no {@link TFSRepository} was set via
     *         {@link #setRepository(TFSRepository)}
     */
    public PendingCheckin getPendingCheckin() {
        return pendingCheckin;
    }
}
