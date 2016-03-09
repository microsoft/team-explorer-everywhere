// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.teambuild.dialogs;

import java.text.MessageFormat;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

import com.microsoft.tfs.client.common.codemarker.CodeMarker;
import com.microsoft.tfs.client.common.codemarker.CodeMarkerDispatch;
import com.microsoft.tfs.client.common.repository.TFSRepository;
import com.microsoft.tfs.client.common.ui.TFSCommonUIClientPlugin;
import com.microsoft.tfs.client.common.ui.browser.BrowserFacade.LaunchMode;
import com.microsoft.tfs.client.common.ui.controls.generic.compatibility.link.CompatibilityLinkControl;
import com.microsoft.tfs.client.common.ui.controls.generic.compatibility.link.CompatibilityLinkFactory;
import com.microsoft.tfs.client.common.ui.framework.dialog.BaseDialog;
import com.microsoft.tfs.client.common.ui.framework.layout.GridDataBuilder;
import com.microsoft.tfs.client.common.ui.helpers.SystemColor;
import com.microsoft.tfs.client.common.ui.tasks.ViewBuildReportTask;
import com.microsoft.tfs.client.common.ui.tasks.vc.ReconcileGatedCheckinTask;
import com.microsoft.tfs.client.common.ui.tasks.vc.UnshelveBuiltShelvesetTask;
import com.microsoft.tfs.client.common.ui.tasks.vc.ViewChangesetDetailsTask;
import com.microsoft.tfs.client.common.ui.teambuild.Messages;
import com.microsoft.tfs.client.common.ui.teambuild.TeamBuildConstants;
import com.microsoft.tfs.core.TFSTeamProjectCollection;
import com.microsoft.tfs.core.clients.build.IBuildDetail;
import com.microsoft.tfs.core.clients.build.IQueuedBuild;
import com.microsoft.tfs.core.clients.build.InformationNodeConverters;
import com.microsoft.tfs.core.clients.build.flags.BuildReason;
import com.microsoft.tfs.core.clients.build.flags.BuildStatus;
import com.microsoft.tfs.util.Check;

/**
 * Provides build status notification to users, and allows users to unshelve or
 * reconcile completed gated checkin builds. Returns
 * {@link IDialogConstants#OK_ID} if action was successful (and the
 */
public class BuildStatusNotificationDialog extends BaseDialog {
    public static final CodeMarker CODEMARKER_NOTIFICATION_DIALOG_OPEN =
        new CodeMarker("com.microsoft.tfs.client.common.ui.teambuild.dialogs.BuildStatusNotificationDialog#open"); //$NON-NLS-1$

    public static int RECONCILE_BUTTON_ID = IDialogConstants.CLIENT_ID;
    public static int UNSHELVE_BUTTON_ID = IDialogConstants.CLIENT_ID + 1;
    public static int IGNORE_BUTTON_ID = IDialogConstants.OK_ID;

    private TFSTeamProjectCollection connection;
    private IQueuedBuild queuedBuild;
    private IBuildDetail buildDetail;

    private boolean neverShowToggle;

    public BuildStatusNotificationDialog(final Shell parentShell) {
        super(null);

        // Disable standard OK/Cancel buttons.
        setOptionIncludeDefaultButtons(false);
    }

    public void setConnection(final TFSTeamProjectCollection connection) {
        this.connection = connection;
    }

    public void setQueuedBuild(final IQueuedBuild queuedBuild) {
        this.queuedBuild = queuedBuild;
        buildDetail = queuedBuild != null ? queuedBuild.getBuild() : null;
    }

    public void setBuildDetail(final IBuildDetail buildDetail) {
        this.buildDetail = buildDetail;
    }

    @Override
    protected void hookAddToDialogArea(final Composite dialogArea) {
        final GridLayout dialogLayout = new GridLayout(2, false);
        dialogLayout.marginWidth = getHorizontalMargin();
        dialogLayout.marginHeight = getVerticalMargin();
        dialogLayout.horizontalSpacing = getHorizontalSpacing() * 2;
        dialogLayout.verticalSpacing = getVerticalSpacing();
        dialogArea.setLayout(dialogLayout);

        final Image image = getBuildDetailImage();

        if (image != null) {
            final Label imageLabel = new Label(dialogArea, SWT.NONE);
            imageLabel.setImage(image);
            GridDataBuilder.newInstance().vAlignTop().applyTo(imageLabel);
        }

        /* Create a composite for the many labels we're displaying. */
        final Composite messageComposite = new Composite(dialogArea, SWT.NONE);
        GridDataBuilder.newInstance().hSpan(image != null ? 1 : 2).hAlignFill().vAlignTop().hFill().applyTo(
            messageComposite);

        final GridLayout messageCompositeLayout = new GridLayout(1, true);
        messageCompositeLayout.marginWidth = 0;
        messageCompositeLayout.marginHeight = 0;
        messageCompositeLayout.horizontalSpacing = getHorizontalSpacing();
        messageCompositeLayout.verticalSpacing = getVerticalSpacing();
        messageComposite.setLayout(messageCompositeLayout);

        final Label titleLabel = new Label(messageComposite, SWT.WRAP);
        titleLabel.setText(getBuildDetailTitle());
        titleLabel.setFont(JFaceResources.getFontRegistry().getBold(JFaceResources.DEFAULT_FONT));
        GridDataBuilder.newInstance().hAlignFill().vAlignTop().hFill().wHint(getMinimumMessageAreaWidth()).applyTo(
            titleLabel);

        if (buildDetail != null) {
            final Composite buildDetailComposite = createBuildDetailMessages(messageComposite);
            GridDataBuilder.newInstance().hAlignFill().vAlignTop().hFill().applyTo(buildDetailComposite);

            /*
             * Provide an informative label about their options for gated
             * checkins
             */
            final String helpMessage = getBuildHelpMessage();
            if (helpMessage != null) {
                final Label helpLabel = new Label(messageComposite, SWT.WRAP);
                helpLabel.setText(helpMessage);
                GridDataBuilder.newInstance().hAlignFill().vAlignTop().hFill().wHint(
                    getMinimumMessageAreaWidth()).applyTo(helpLabel);
            }
        }

        /* Provide some space between the button area and the message area */
        final Label spacerLabel = new Label(dialogArea, SWT.NONE);
        GridDataBuilder.newInstance().hSpan(2).applyTo(spacerLabel);

        final Button neverShowButton = new Button(dialogArea, SWT.CHECK | SWT.LEFT);
        neverShowButton.setText(Messages.getString("BuildStatusNotificationDialog.NeverShowNotificationToggleMessage")); //$NON-NLS-1$
        neverShowButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
                neverShowToggle = neverShowButton.getSelection();
            }
        });
        GridDataBuilder.newInstance().hSpan(2).applyTo(neverShowButton);

        /*
         * Can only unshelve failed (or partially successful) builds
         */
        final boolean unshelve = (buildDetail != null
            && buildDetail.isBuildFinished()
            && (buildDetail.getStatus().contains(BuildStatus.FAILED)
                || buildDetail.getStatus().contains(BuildStatus.PARTIALLY_SUCCEEDED))
            && buildDetail.getReason().contains(BuildReason.CHECK_IN_SHELVESET));

        /*
         * Can only reconcile successfully (or partially successful) builds that
         * were checked in (ie, have a non-negative changeset id)
         */
        final boolean reconcile = (buildDetail != null
            && buildDetail.isBuildFinished()
            && (buildDetail.getStatus().contains(BuildStatus.SUCCEEDED)
                || buildDetail.getStatus().contains(BuildStatus.PARTIALLY_SUCCEEDED))
            && buildDetail.getReason().contains(BuildReason.CHECK_IN_SHELVESET)
            && InformationNodeConverters.getChangesetID(buildDetail.getInformation()) > 0);

        /*
         * If we're not offering reconcile or unshelve, this is simply a build
         * completion alert. In this case, only offer an okay button.
         */
        if (unshelve || reconcile) {
            if (reconcile) {
                /*
                 * Reconcile should be in the OK position (the default button)
                 */
                addButtonDescription(
                    RECONCILE_BUTTON_ID,
                    Messages.getString("BuildStatusNotificationDialog.ReconcileButtonLabel"), //$NON-NLS-1$
                    true);
            }

            if (unshelve) {
                /*
                 * Unshelve should be in the OK position (the default button)
                 * unless there's a reconcile
                 */
                addButtonDescription(
                    UNSHELVE_BUTTON_ID,
                    Messages.getString("BuildStatusNotificationDialog.UnshelveButtonLabel"), //$NON-NLS-1$
                    (!reconcile));
            }

            /*
             * Note: all non-action buttons (ie, any buttons that are not
             * unshelve or reconcile) always return cancel id, indicating that
             * no action was performed and the build should still be considered
             * "watched".
             */
            addButtonDescription(
                IGNORE_BUTTON_ID,
                Messages.getString("BuildStatusNotificationDialog.IgnoreButtonLabel"), //$NON-NLS-1$
                false);
        } else {
            /*
             * Note: all non-action buttons (ie, any buttons that are not
             * unshelve or reconcile) always return cancel id, indicating that
             * no action was performed and the build should still be considered
             * "watched".
             */
            addButtonDescription(IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
        }
    }

    @Override
    protected void hookDialogIsOpen() {
        /*
         * The dialog is now open and the buttons have been layed out. Now we
         * can change the default button without changing their order. (SWT
         * changes the order of the default button on Mac OS to be the far right
         * button for platform sanity.
         */
        final Button okButton = getButton(IDialogConstants.OK_ID);

        getShell().setDefaultButton(okButton);
        okButton.setFocus();

        CodeMarkerDispatch.dispatch(CODEMARKER_NOTIFICATION_DIALOG_OPEN);
    }

    private Image getBuildDetailImage() {
        if (buildDetail == null || buildDetail.getStatus() == null) {
            return null;
        }

        if (buildDetail.getStatus().contains(BuildStatus.FAILED)) {
            return getShell().getDisplay().getSystemImage(SWT.ICON_ERROR);
        } else if (buildDetail.getStatus().contains(BuildStatus.SUCCEEDED)) {
            return getShell().getDisplay().getSystemImage(SWT.ICON_INFORMATION);
        } else if (buildDetail.getStatus().contains(BuildStatus.PARTIALLY_SUCCEEDED)) {
            return getShell().getDisplay().getSystemImage(SWT.ICON_WARNING);
        }

        return null;
    }

    private String getBuildDetailTitle() {
        if (buildDetail == null || buildDetail.getStatus() == null) {
            return Messages.getString("BuildStatusNotificationDialog.BuildStatusUnknownTitle"); //$NON-NLS-1$
        }

        if (buildDetail.getStatus().contains(BuildStatus.FAILED)
            && buildDetail.getReason().contains(BuildReason.CHECK_IN_SHELVESET)) {
            return Messages.getString("BuildStatusNotificationDialog.BuildStatusGatedFailedTitle"); //$NON-NLS-1$
        }
        if (buildDetail.getStatus().contains(BuildStatus.FAILED)) {
            return Messages.getString("BuildStatusNotificationDialog.BuildStatusFailedTitle"); //$NON-NLS-1$
        }

        if (buildDetail.getStatus().contains(BuildStatus.SUCCEEDED)
            && buildDetail.getReason().contains(BuildReason.CHECK_IN_SHELVESET)) {
            return Messages.getString("BuildStatusNotificationDialog.BuildStatusGatedSucceededTitle"); //$NON-NLS-1$
        }
        if (buildDetail.getStatus().contains(BuildStatus.SUCCEEDED)) {
            return Messages.getString("BuildStatusNotificationDialog.BuildStatusSucceededTitle"); //$NON-NLS-1$
        }

        if (buildDetail.getStatus().contains(BuildStatus.PARTIALLY_SUCCEEDED)
            && buildDetail.getReason().contains(BuildReason.CHECK_IN_SHELVESET)) {
            return Messages.getString("BuildStatusNotificationDialog.BuildStatusGatedPartiallySucceededTitle"); //$NON-NLS-1$
        }
        if (buildDetail.getStatus().contains(BuildStatus.PARTIALLY_SUCCEEDED)) {
            return Messages.getString("BuildStatusNotificationDialog.BuildStatusPartiallySucceededTitle"); //$NON-NLS-1$
        }

        return Messages.getString("BuildStatusNotificationDialog.BuildStatusUnknownTitle"); //$NON-NLS-1$
    }

    private String getBuildHelpMessage() {
        if (buildDetail == null || !buildDetail.getReason().contains(BuildReason.CHECK_IN_SHELVESET)) {
            return null;
        }

        /*
         * Always offer the reconcile message when a change was committed,
         * regardless of status
         */
        if (InformationNodeConverters.getChangesetID(buildDetail.getInformation()) > 0) {
            return Messages.getString("BuildStatusNotificationDialog.HelpReconcileMessage"); //$NON-NLS-1$
        } else {
            return Messages.getString("BuildStatusNotificationDialog.HelpUnshelveMessage"); //$NON-NLS-1$
        }
    }

    protected Composite createBuildDetailMessages(final Composite parent) {
        Check.notNull(buildDetail, "buildDetail"); //$NON-NLS-1$

        final Composite detailComposite = new Composite(parent, SWT.NONE);

        /*
         * Use no vert spacing so that it appears that the line is a linebreak
         * away from the previous (instead of a paragraph break)
         */
        final GridLayout detailCompositeLayout = new GridLayout(1, true);
        detailCompositeLayout.marginWidth = 0;
        detailCompositeLayout.marginHeight = 0;
        detailCompositeLayout.horizontalSpacing = getHorizontalSpacing();
        detailCompositeLayout.verticalSpacing = 0;
        detailComposite.setLayout(detailCompositeLayout);

        if (connection != null && buildDetail != null) {
            final int changeset = InformationNodeConverters.getChangesetID(buildDetail.getInformation());
            final String linkText;

            if (changeset > 0) {
                linkText =
                    MessageFormat.format(
                        Messages.getString("BuildStatusNotificationDialog.LinkChangesetMessageFormat"), //$NON-NLS-1$
                        Integer.toString(changeset));
            } else {
                final String status = buildDetail.getStatus().contains(BuildStatus.PARTIALLY_SUCCEEDED)
                    ? Messages.getString("BuildStatusNotificationDialog.LinkFailureMessagePartiallySucceeded") : //$NON-NLS-1$
                    Messages.getString("BuildStatusNotificationDialog.LinkFailureMessageFailure"); //$NON-NLS-1$

                linkText =
                    MessageFormat.format(
                        Messages.getString("BuildStatusNotificationDialog.LinkFailureMessageFormat"), //$NON-NLS-1$
                        buildDetail.getBuildNumber(),
                        status);
            }

            final CompatibilityLinkControl linkControl = CompatibilityLinkFactory.createLink(detailComposite, SWT.NONE);
            linkControl.setSimpleText(linkText);
            linkControl.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(final SelectionEvent e) {
                    if (changeset > 0) {
                        final TFSRepository repository =
                            TFSCommonUIClientPlugin.getDefault().getProductPlugin().getRepositoryManager().getDefaultRepository();
                        new ViewChangesetDetailsTask(getShell(), repository, changeset).run();
                    } else {
                        // Use External browser because we're in a modal dialog
                        new ViewBuildReportTask(
                            getShell(),
                            connection.getBuildServer(),
                            buildDetail.getURI(),
                            buildDetail.getBuildNumber(),
                            LaunchMode.EXTERNAL).run();
                    }
                }
            });
            GridDataBuilder.newInstance().hFill().hGrab().wHint(getMinimumMessageAreaWidth()).applyTo(
                linkControl.getControl());
        }

        if (queuedBuild != null) {
            final Label submissionLabel = new Label(detailComposite, SWT.NONE);
            submissionLabel.setText(
                MessageFormat.format(
                    Messages.getString("BuildStatusNotificationDialog.SubmissionMessageFormat"), //$NON-NLS-1$
                    queuedBuild.getRequestedFor(),
                    TeamBuildConstants.DATE_FORMAT.format(queuedBuild.getQueueTime().getTime())));

            final Color submissionLabelColor = SystemColor.getDimmedWidgetForegroundColor(getShell().getDisplay());

            if (submissionLabelColor != null) {
                submissionLabel.setForeground(submissionLabelColor);
            }

            GridDataBuilder.newInstance().hFill().hGrab().wHint(getMinimumMessageAreaWidth()).applyTo(submissionLabel);
        }

        return detailComposite;
    }

    @Override
    protected String provideDialogTitle() {
        if (buildDetail != null && buildDetail.getReason().contains(BuildReason.CHECK_IN_SHELVESET)) {
            return Messages.getString("BuildStatusNotificationDialog.DialogTitleGated"); //$NON-NLS-1$
        }

        return Messages.getString("BuildStatusNotificationDialog.DialogTitle"); //$NON-NLS-1$
    }

    @Override
    protected void buttonPressed(final int buttonId) {
        if (buttonId == UNSHELVE_BUTTON_ID) {
            final UnshelveBuiltShelvesetTask unshelveTask = new UnshelveBuiltShelvesetTask(
                getShell(),
                TFSCommonUIClientPlugin.getDefault().getProductPlugin().getRepositoryManager().getDefaultRepository(),
                buildDetail,
                true);

            final IStatus unshelveStatus = unshelveTask.run();

            /* On error or cancel, keep this dialog open. */
            if (unshelveStatus.matches(IStatus.ERROR | IStatus.CANCEL)) {
                return;
            }

            setReturnCode(IDialogConstants.OK_ID);
            close();
        } else if (buttonId == RECONCILE_BUTTON_ID) {
            final ReconcileGatedCheckinTask reconcileTask = new ReconcileGatedCheckinTask(
                getShell(),
                TFSCommonUIClientPlugin.getDefault().getProductPlugin().getRepositoryManager().getDefaultRepository(),
                buildDetail);

            final IStatus reconcileStatus = reconcileTask.run();

            /* On error or cancel, keep this dialog open. */
            if (reconcileStatus.matches(IStatus.ERROR | IStatus.CANCEL)) {
                return;
            }

            setReturnCode(IDialogConstants.OK_ID);
            close();
        } else {
            /*
             * Always return cancel when no action is performed. This allows
             * callers to know when a build should no longer be "watched"
             * (OK_ID) vs no action was performed (CANCEL_ID).
             */
            setReturnCode(IDialogConstants.CANCEL_ID);
            close();
        }
    }

    public boolean getNeverShow() {
        return neverShowToggle;
    }
}
