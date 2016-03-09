// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.dialogs.vc;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

import com.microsoft.tfs.client.common.commands.ResolveConflictsCommand;
import com.microsoft.tfs.client.common.commands.TFSCommand;
import com.microsoft.tfs.client.common.commands.vc.QueryConflictsCommand;
import com.microsoft.tfs.client.common.commands.vc.RefreshPendingChangesCommand;
import com.microsoft.tfs.client.common.framework.command.ICommandExecutor;
import com.microsoft.tfs.client.common.framework.resources.command.ResourceChangingCommand;
import com.microsoft.tfs.client.common.repository.TFSRepository;
import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.conflicts.resolutions.EclipseMergeConflictResolution;
import com.microsoft.tfs.client.common.ui.controls.vc.ConflictTable;
import com.microsoft.tfs.client.common.ui.framework.command.UICommandExecutorFactory;
import com.microsoft.tfs.client.common.ui.framework.dialog.BaseDialog;
import com.microsoft.tfs.client.common.ui.framework.helper.ShellUtils;
import com.microsoft.tfs.client.common.ui.framework.helper.UIHelpers;
import com.microsoft.tfs.client.common.ui.framework.sizing.ControlSize;
import com.microsoft.tfs.client.common.ui.helpers.AutomationIDHelper;
import com.microsoft.tfs.client.common.ui.helpers.ConflictHelpers;
import com.microsoft.tfs.core.clients.versioncontrol.conflicts.ConflictDescription;
import com.microsoft.tfs.core.clients.versioncontrol.conflicts.resolutions.ConflictResolution;
import com.microsoft.tfs.core.clients.versioncontrol.conflicts.resolutions.ConflictResolutionOptions;
import com.microsoft.tfs.core.clients.versioncontrol.conflicts.resolutions.ConflictResolutionStatus;
import com.microsoft.tfs.core.clients.versioncontrol.conflicts.resolutions.ConflictResolutionStatusListener;
import com.microsoft.tfs.core.clients.versioncontrol.conflicts.resolutions.CoreConflictResolution;
import com.microsoft.tfs.core.clients.versioncontrol.conflicts.resolutions.ExternalConflictResolution;
import com.microsoft.tfs.core.clients.versioncontrol.conflicts.resolutions.ExternalConflictResolution.ExternalConflictResolver;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Conflict;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Resolution;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Workspace;
import com.microsoft.tfs.core.clients.versioncontrol.specs.ItemSpec;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.LocaleUtil;

/**
 * ConflictDialog holds a list of conflicts and allows them to be resolved
 * singularly or en masse.
 *
 * TODO: this needs to be hooked up to the plugin's conflictManager. Will
 * explorer get a conflict manager? oy.
 */
public class ConflictDialog extends BaseDialog implements ConflictResolutionStatusListener {
    public static final String CONFLICTS_TABLE_ID = "ConflictDialog.ConflictTable"; //$NON-NLS-1$

    private final Shell parentShell;
    private final TFSRepository repository;

    private ConflictTable conflictTable;
    private Label countLabel;

    private int resolveCount = 0;

    private final List<ConflictDescription> descriptionList = new ArrayList<ConflictDescription>();
    private final List<ConflictResolution> runningResolutionList = new ArrayList<ConflictResolution>();

    private static final int AUTOMERGE = IDialogConstants.CLIENT_ID;

    public ConflictDialog(
        final Shell parentShell,
        final TFSRepository repository,
        final ConflictDescription[] descriptions) {
        super(parentShell);

        this.parentShell = ShellUtils.getBestParent(parentShell);

        Check.notNull(repository, "repository"); //$NON-NLS-1$
        this.repository = repository;

        for (int i = 0; i < descriptions.length; i++) {
            descriptionList.add(descriptions[i]);
        }
    }

    /*
     * (non-Javadoc)
     *
     * @seecom.microsoft.tfs.client.common.ui.shared.dialog.BaseDialog#
     * hookAddToDialogArea(org.eclipse. swt.widgets.Composite)
     */
    @Override
    protected void hookAddToDialogArea(final Composite dialogArea) {
        final GridLayout dialogLayout = new GridLayout(1, false);

        dialogLayout.marginWidth = convertHorizontalDLUsToPixels(IDialogConstants.HORIZONTAL_MARGIN);
        dialogLayout.marginHeight = convertVerticalDLUsToPixels(IDialogConstants.VERTICAL_MARGIN);

        dialogArea.setLayout(dialogLayout);

        final Label descriptionLabel = new Label(dialogArea, SWT.NONE);
        descriptionLabel.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, true, false));
        descriptionLabel.setText(Messages.getString("ConflictDialog.DescriptionLabelText")); //$NON-NLS-1$

        /* Make the label larger so that there's spacing beneath it. */
        ControlSize.setCharHeightHint(descriptionLabel, 2);

        final Label conflictLabel = new Label(dialogArea, SWT.NONE);
        AutomationIDHelper.setWidgetID(conflictLabel, CONFLICTS_TABLE_ID);
        conflictLabel.setLayoutData(new GridData(SWT.LEFT, SWT.BOTTOM, true, false));
        conflictLabel.setText(Messages.getString("ConflictDialog.ConflictLabelText")); //$NON-NLS-1$

        // set up the conflict table
        conflictTable = new ConflictTable(dialogArea, SWT.MULTI | SWT.FULL_SELECTION | SWT.BORDER);

        final GridData conflictTableData = new GridData(SWT.FILL, SWT.FILL, true, true);
        conflictTableData.grabExcessHorizontalSpace = true;
        conflictTableData.grabExcessVerticalSpace = true;
        conflictTable.setLayoutData(conflictTableData);

        conflictTable.addSelectionChangedListener(new ISelectionChangedListener() {
            @Override
            public void selectionChanged(final SelectionChangedEvent event) {
                if (conflictTable.getSelectedElements().length == 0) {
                    getButton(IDialogConstants.OK_ID).setEnabled(false);
                } else {
                    getButton(IDialogConstants.OK_ID).setEnabled(true);
                }
            }
        });
        conflictTable.addDoubleClickListener(new IDoubleClickListener() {
            @Override
            public void doubleClick(final DoubleClickEvent event) {
                resolveSelection();
            }
        });
        conflictTable.setFocus();

        ControlSize.setCharHeightHint(conflictTable, 12);

        // add an auto merge all button
        addButtonDescription(AUTOMERGE, Messages.getString("ConflictDialog.AutoMergeAllButtonText"), false); //$NON-NLS-1$
    }

    /*
     * override Dialog.createButtonBar so that we can add a label to the left of
     * the buttons which shows resolution progress, a la MSFT
     *
     * (non-Javadoc)
     *
     * @see
     * org.eclipse.jface.dialogs.Dialog#createButtonBar(org.eclipse.swt.widgets
     * .Composite)
     */
    @Override
    protected Control createButtonBar(final Composite parent) {
        final Composite buttonBar = new Composite(parent, SWT.NONE);

        final GridLayout layout = new GridLayout();
        layout.numColumns = 2;
        layout.makeColumnsEqualWidth = false;
        layout.horizontalSpacing = convertHorizontalDLUsToPixels(IDialogConstants.HORIZONTAL_SPACING);
        buttonBar.setLayout(layout);

        final GridData data = new GridData(SWT.FILL, SWT.BOTTOM, true, false);
        data.grabExcessHorizontalSpace = true;
        data.grabExcessVerticalSpace = false;
        buttonBar.setLayoutData(data);

        buttonBar.setFont(parent.getFont());

        // this is our label (eg "2 remaining, 0 resolved")
        countLabel = new Label(buttonBar, SWT.NONE);
        final GridData countLabelData = new GridData(SWT.LEFT, SWT.CENTER, true, true);
        countLabelData.grabExcessHorizontalSpace = true;
        countLabelData.horizontalIndent = convertHorizontalDLUsToPixels(IDialogConstants.HORIZONTAL_MARGIN);
        countLabel.setLayoutData(countLabelData);

        ControlSize.setCharWidthHint(countLabel, 36);

        // add the dialog's button bar to the right
        final Control buttonControl = super.createButtonBar(buttonBar);
        buttonControl.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, true, false));

        return buttonBar;
    }

    /*
     * (non-Javadoc)
     *
     * @seecom.microsoft.tfs.client.common.ui.shared.dialog.BaseDialog#
     * hookAfterButtonsCreated()
     */
    @Override
    protected void hookAfterButtonsCreated() {
        getButton(IDialogConstants.CANCEL_ID).setText(Messages.getString("ConflictDialog.CloseButtonTExt")); //$NON-NLS-1$
        getButton(IDialogConstants.OK_ID).setText(Messages.getString("ConflictDialog.ResolveButtonText")); //$NON-NLS-1$

        refreshLabels();
    }

    /**
     * Refresh the conflict list and the conflict table, performed after a
     * conflict has been resolved (successfully or no). This will refresh the
     * list of conflicts, as this may have changed during conflict resolution.
     */
    private void refresh() {
        final Set<ItemSpec> itemSpecSet = new HashSet<ItemSpec>();

        for (final Iterator i = descriptionList.iterator(); i.hasNext();) {
            final ConflictDescription description = (ConflictDescription) i.next();

            final ItemSpec[] descriptionSpecs = description.getConflictItemSpecs();

            if (descriptionSpecs != null) {
                for (int j = 0; j < descriptionSpecs.length; j++) {
                    itemSpecSet.add(descriptionSpecs[j]);
                }
            }
        }

        final ItemSpec[] requerySpecs = itemSpecSet.toArray(new ItemSpec[itemSpecSet.size()]);

        final QueryConflictsCommand requeryCommand = new QueryConflictsCommand(repository, requerySpecs);
        final IStatus requeryStatus = UICommandExecutorFactory.newUICommandExecutor(getShell()).execute(requeryCommand);

        if (requeryStatus.isOK()) {
            descriptionList.clear();
            descriptionList.addAll(Arrays.asList(requeryCommand.getConflictDescriptions()));
        }

        refreshLabels();
    }

    /**
     * Refresh the conflict table with the internal list of conflict
     * descriptions, and update any count or status labels.
     */
    private void refreshLabels() {
        conflictTable.setConflictDescriptions(descriptionList.toArray(new ConflictDescription[descriptionList.size()]));
        conflictTable.refresh();

        final int conflictCount = descriptionList.size();
        if (conflictCount > 0) {
            conflictTable.setSelection(0);
            getButton(IDialogConstants.OK_ID).setEnabled(true);
            getButton(AUTOMERGE).setEnabled(true);
        } else {
            getButton(IDialogConstants.OK_ID).setEnabled(false);
            getButton(AUTOMERGE).setEnabled(false);
        }

        if (conflictCount == 0) {
            countLabel.setText(Messages.getString("ConflictDialog.NoConflictsRemaining")); //$NON-NLS-1$
        } else if (resolveCount == 0) {
            if (conflictCount == 1) {
                countLabel.setText(Messages.getString("ConflictDialog.OneConflict")); //$NON-NLS-1$
            } else {
                final String messageFormat = Messages.getString("ConflictDialog.MultiConflictsFormat"); //$NON-NLS-1$
                countLabel.setText(MessageFormat.format(messageFormat, conflictCount));
            }
        } else {
            if (conflictCount == 1) {
                final String messageFormat = Messages.getString("ConflictDialog.OneRemainingMultiResolvedFormat"); //$NON-NLS-1$
                countLabel.setText(MessageFormat.format(messageFormat, resolveCount));
            } else {
                final String messageFormat = Messages.getString("ConflictDialog.MultiRemainingMultiResolvedFormat"); //$NON-NLS-1$
                countLabel.setText(MessageFormat.format(messageFormat, conflictCount, resolveCount));

            }
        }
    }

    /*
     * (non-Javadoc)
     *
     * @seecom.microsoft.tfs.client.common.ui.shared.dialog.BaseDialog#
     * provideDialogTitle()
     */
    @Override
    protected String provideDialogTitle() {
        return Messages.getString("ConflictDialog.DialogTitle"); //$NON-NLS-1$
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * com.microsoft.tfs.client.common.ui.shared.dialog.BaseDialog#buttonPressed
     * (int)
     */
    @Override
    protected void buttonPressed(final int buttonId) {
        // TODO; should this really be *auto merge all*? this seems non-
        // intuitive with the selection? maybe it should be "auto merge
        // selected" unless there's none selected, in which case it should
        // be "auto merge all"?
        if (buttonId == AUTOMERGE) {
            automergeAll();
        } else if (buttonId == IDialogConstants.OK_ID) {
            resolveSelection();
        } else if (buttonId == IDialogConstants.CANCEL_ID && cancelResolution()) {
            super.cancelPressed();

            /* Refresh pending changes */
            final RefreshPendingChangesCommand pendingChangesCommand = new RefreshPendingChangesCommand(repository);
            UICommandExecutorFactory.newUICommandExecutor(parentShell).execute(pendingChangesCommand);
        }
    }

    /**
     * Prompt the user to cancel running resolutions. (If there are any.)
     *
     * @return true to exit, false otherwise
     */
    private boolean cancelResolution() {
        synchronized (runningResolutionList) {
            if (runningResolutionList.size() == 0) {
                return true;
            }

            String message;
            if (runningResolutionList.size() == 1) {
                message = Messages.getString("ConflictDialog.ConfirmSingleConflictDialogText"); //$NON-NLS-1$
            } else {
                message = Messages.getString("ConflictDialog.ConfirmMultiConflictsDialogText"); //$NON-NLS-1$
            }

            final boolean cancelling =
                MessageDialog.openQuestion(
                    getShell(),
                    Messages.getString("ConflictDialog.ConfirmCancelDialogTitle"), //$NON-NLS-1$
                    message);

            if (!cancelling) {
                return false;
            }

            for (final Iterator i = runningResolutionList.iterator(); i.hasNext();) {
                final ConflictResolution resolution = (ConflictResolution) i.next();

                resolution.removeStatusListener(this);
                resolution.cancel();
            }
        }

        return true;
    }

    /**
     * Auto merge all conflicts. Attempt to create a new CoreConflict with
     * Resolution.AcceptMerge.
     */
    private void automergeAll() {
        final List<ConflictResolution> resolutionList = new ArrayList<ConflictResolution>();

        // Remove any running resolutions
        for (final Iterator<ConflictDescription> i = descriptionList.iterator(); i.hasNext();) {
            final ConflictDescription description = i.next();

            if (!isResolving(description)) {
                resolutionList.add(
                    new CoreConflictResolution(
                        description,
                        Messages.getString("ConflictDialog.ResolutionDescription"), //$NON-NLS-1$
                        Messages.getString("ConflictDialog.ResolutionHelpText"), //$NON-NLS-1$
                        ConflictResolutionOptions.NONE,
                        Resolution.ACCEPT_MERGE));
            }
        }

        final ConflictResolution[] resolutions = resolutionList.toArray(new ConflictResolution[resolutionList.size()]);

        final ResolveConflictsCommand resolver = new ResolveConflictsCommand(repository, resolutions);
        UICommandExecutorFactory.newUICommandExecutor(getShell()).execute(new ResourceChangingCommand(resolver));

        resolutionFinished(resolutions, resolver.getStatuses());
    }

    /**
     * Queries if an asynchronous conflict resolution is running for the given
     * ConflictDescription.
     *
     * @param description
     *        ConflictDescription to query resolution state for
     * @return true if there's an asynchronous resolution for the given
     *         ConflictDescription
     */
    public boolean isResolving(final ConflictDescription description) {
        synchronized (runningResolutionList) {
            for (final Iterator<ConflictResolution> i = runningResolutionList.iterator(); i.hasNext();) {
                final ConflictResolution resolution = i.next();

                if (resolution.getConflictDescription().equals(description)) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Resolves the selected conflict(s).
     */
    private void resolveSelection() {
        final ConflictDescription[] descriptions = conflictTable.getSelectedElements();

        if (descriptions.length == 1) {
            resolveConflict(descriptions[0]);
        } else if (descriptions.length > 1) {
            resolveConflicts(descriptions);
        }
    }

    /**
     * Raise the dialog prompting the user for conflict resolution, and then
     * attempt to resolve the conflict with that given resolution.
     *
     * @param description
     *        ConflictDescription to resolve
     */
    private void resolveConflict(final ConflictDescription description) {
        final ConflictResolutionDialog resolveDialog = new ConflictResolutionDialog(getShell(), description);

        /*
         * TODO: don't clear this analysis here, at least for the plugin. This
         * will only get stale in the ConflictView, so let it hook up a listener
         * to the filemodificationvalidator, and let it dump conflict analyses
         * when they become stale. (but do leave this here for explorer.)
         */

        /*
         * clear any analysis that's associated with this description, it may be
         * stale
         */
        description.clearAnalysis();

        if (resolveDialog.open() != IDialogConstants.OK_ID) {
            return;
        }

        final ConflictResolution resolution = resolveDialog.getResolution();

        // add this as a status listener so that we can handle async resolutions
        // (eg, ExternalConflictResolution)
        resolution.addStatusListener(this);

        // add this to the running resolution list *now*, before resolution
        // begins, so that we don't deadlock the ui thread with other callbacks
        synchronized (runningResolutionList) {
            runningResolutionList.add(resolution);
        }

        /* Run us in a workspace command. */
        if (resolution instanceof ExternalConflictResolution) {
            ((ExternalConflictResolution) resolution).setConflictResolver(
                new ResourceChangingConflictResolver(getShell()));
        }

        // resolve the conflict
        final ResolveConflictsCommand resolver = new ResolveConflictsCommand(repository, resolution);

        final ICommandExecutor commandExecutor;

        /*
         * TODO: this is quick and dirty for SP1, replace with a more elegant
         * solution.
         *
         * When running a command executor with a delay, the internal merge tool
         * will typically pop up parented on the conflict resolution dialog,
         * then the command executor dialog can pop up the "Resolving..."
         * progress dialog parented on the same shell after the delay. This can
         * lead to UI shell parenting deadlocks. Thus, using a command executor
         * with no delay will guarantee that the merge dialog parents itself off
         * the best parent (the progress dialog.)
         */
        if (resolution instanceof EclipseMergeConflictResolution) {
            commandExecutor = UICommandExecutorFactory.newUICommandExecutor(getShell(), 0);
        } else {
            commandExecutor = UICommandExecutorFactory.newUICommandExecutor(getShell());
        }

        commandExecutor.execute(new ResourceChangingCommand(resolver));

        // the status listener will handle notification, etc
    }

    /**
     * Raise the dialog prompting the user for conflict resolutions, and then
     * attempt to resolve the conflicts with that given resolution(s).
     *
     * @param descriptions
     *        ConflictDescriptions to resolve
     */
    private void resolveConflicts(final ConflictDescription[] descriptions) {
        final List<ConflictResolution> resolutionList = new ArrayList<ConflictResolution>();

        final MultipleConflictResolutionDialog resolveDialog =
            new MultipleConflictResolutionDialog(getShell(), descriptions);

        if (resolveDialog.open() != IDialogConstants.OK_ID) {
            return;
        }

        for (int i = 0; i < descriptions.length; i++) {
            final ConflictResolution resolution = resolveDialog.getResolution(descriptions[i]);

            if (resolution != null) {
                resolutionList.add(resolution);
            }
        }

        final ConflictResolution[] resolutions = resolutionList.toArray(new ConflictResolution[resolutionList.size()]);

        final ResolveConflictsCommand resolver = new ResolveConflictsCommand(repository, resolutions);
        UICommandExecutorFactory.newUICommandExecutor(getShell()).execute(new ResourceChangingCommand(resolver));

        resolutionFinished(resolutions, resolver.getStatuses());
    }

    /*
     * We'll get a ConflictResolutionStatus on resolution status changes. This
     * will allow us to do any completion or notification on conflicts which
     * were being resolved asynchronously.
     *
     * (non-Javadoc)
     *
     * @seecom.microsoft.tfs.core.vc.conflicts.resolutions.
     * ConflictResolutionStatusListener #
     * statusChanged(com.microsoft.tfs.core.vc
     * .conflicts.resolutions.ConflictResolution ,
     * com.microsoft.tfs.core.vc.conflicts.resolutions.ConflictResolutionStatus)
     */
    @Override
    public void statusChanged(final ConflictResolution resolution, final ConflictResolutionStatus newStatus) {
        // we only care about finished statuses for resolutions that were
        // running asynchronously
        synchronized (runningResolutionList) {
            if (!runningResolutionList.contains(resolution)
                || newStatus == ConflictResolutionStatus.NOT_STARTED
                || newStatus == ConflictResolutionStatus.RUNNING) {
                return;
            }

            runningResolutionList.remove(resolution);
        }

        UIHelpers.asyncExec(new Runnable() {
            @Override
            public void run() {
                resolutionFinished(resolution, newStatus);
                refreshLabels();
            }
        });
    }

    /**
     * Complete any resolution for a single conflict. (Raise errors, remove from
     * the list(s), etc.)
     *
     * TODO: this needs to fire to the conflict manager for the plugin.
     *
     * @param resolution
     *        Conflict Resolution that finished
     * @param status
     *        The ConflictResolutionStatus that resolution completed with
     */
    private void resolutionFinished(final ConflictResolution resolution, final ConflictResolutionStatus status) {
        if (status == ConflictResolutionStatus.SUCCESS || status == ConflictResolutionStatus.SUCCEEDED_WITH_CONFLICTS) {
            resolveCount++;
        }

        ConflictHelpers.showConflictError(getShell(), resolution, status);

        refresh();
    }

    /**
     * Complete any resolution for multiple conflicts. (Raise errors, remove
     * from the list(s), etc.)
     *
     * TODO: this needs to fire to the conflict manager for the plugin.
     *
     * @param resolutions
     *        Conflict Resolutions that finished
     * @param statuses
     *        The ConflictResolutionStatus for each resolution
     */
    private void resolutionFinished(final ConflictResolution[] resolutions, final ConflictResolutionStatus[] statuses) {

        for (int i = 0; i < statuses.length; i++) {
            if (statuses[i] == ConflictResolutionStatus.SUCCESS
                || statuses[i] == ConflictResolutionStatus.SUCCEEDED_WITH_CONFLICTS) {
                resolveCount++;
            }
        }

        ConflictHelpers.showConflictErrors(getShell(), resolutions, statuses);

        refresh();
    }

    public int getUnresolvedCount() {
        return descriptionList.size();
    }

    public int getResolvedCount() {
        return resolveCount;
    }

    private static class ResourceChangingConflictResolver extends ExternalConflictResolver {
        private final Shell parentShell;

        public ResourceChangingConflictResolver(final Shell parentShell) {
            Check.notNull(parentShell, "parentShell"); //$NON-NLS-1$

            this.parentShell = parentShell;
        }

        @Override
        public boolean resolveConflict(final Workspace workspace, final Conflict conflict) {
            final ConflictResolutionCommand resolveCommand = new ConflictResolutionCommand(workspace, conflict);
            final IStatus resolveStatus = UICommandExecutorFactory.newUICommandExecutor(parentShell).execute(
                new ResourceChangingCommand(resolveCommand));

            return (resolveStatus.isOK());
        }
    }

    private static class ConflictResolutionCommand extends TFSCommand {
        private final Workspace workspace;
        private final Conflict conflict;

        public ConflictResolutionCommand(final Workspace workspace, final Conflict conflict) {
            this.workspace = workspace;
            this.conflict = conflict;
        }

        @Override
        public String getName() {
            return Messages.getString("ConflictDialog.ResolveCommandText"); //$NON-NLS-1$
        }

        @Override
        public String getErrorDescription() {
            return Messages.getString("ConflictDialog.ResolveCommandErrorText"); //$NON-NLS-1$
        }

        @Override
        public String getLoggingDescription() {
            return Messages.getString("ConflictDialog.ResolveCommandText", LocaleUtil.ROOT); //$NON-NLS-1$
        }

        @Override
        protected IStatus doRun(final IProgressMonitor progressMonitor) throws Exception {
            workspace.resolveConflict(conflict);

            return Status.OK_STATUS;
        }
    }
}
