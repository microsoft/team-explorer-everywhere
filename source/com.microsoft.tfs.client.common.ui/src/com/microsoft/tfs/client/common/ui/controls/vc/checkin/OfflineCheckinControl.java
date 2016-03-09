// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.controls.vc.checkin;

import java.util.Set;

import org.eclipse.jface.action.IContributionManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

import com.microsoft.tfs.client.common.framework.background.BackgroundTaskEvent;
import com.microsoft.tfs.client.common.framework.background.BackgroundTaskListener;
import com.microsoft.tfs.client.common.framework.background.IBackgroundTask;
import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.TFSCommonUIClientPlugin;
import com.microsoft.tfs.client.common.ui.framework.helper.UIHelpers;
import com.microsoft.tfs.client.common.ui.framework.layout.GridDataBuilder;

public class OfflineCheckinControl extends AbstractCheckinSubControl {
    private final Label offlineLabel;

    public OfflineCheckinControl(final Composite parent, final int style, final CheckinControlOptions options) {
        super(parent, style, Messages.getString("OfflineCheckinControl.Title"), CheckinSubControlType.OFFLINE); //$NON-NLS-1$

        final GridLayout layout = new GridLayout(1, false);
        layout.marginWidth = getHorizontalMargin();
        layout.marginHeight = getVerticalMargin();
        layout.horizontalSpacing = getHorizontalSpacing();
        layout.verticalSpacing = getVerticalSpacing();

        setLayout(layout);

        offlineLabel = new Label(this, SWT.WRAP | SWT.CENTER);
        offlineLabel.setText(Messages.getString("OfflineCheckinControl.OfflineLabelText")); //$NON-NLS-1$
        GridDataBuilder.newInstance().hAlignCenter().hGrab().vAlignCenter().vGrab().applyTo(offlineLabel);

        /*
         * Hook up a listener to the Team Explorer manager that allows us to
         * know when connections are being built so that we can update our label
         * accordingly.
         */
        TFSCommonUIClientPlugin.getDefault().getProductPlugin().getServerManager().addBackgroundConnectionTaskListener(
            new BackgroundTaskListener() {
                @Override
                public void onBackgroundTaskStarted(final BackgroundTaskEvent event) {
                    updateLabel();
                }

                @Override
                public void onBackgroundTaskFinished(final BackgroundTaskEvent event) {
                    updateLabel();
                }
            });
    }

    private void updateLabel() {
        final Set<IBackgroundTask> backgroundTasks =
            TFSCommonUIClientPlugin.getDefault().getProductPlugin().getServerManager().getBackgroundConnectionTasks();

        UIHelpers.runOnUIThread(true, new Runnable() {
            @Override
            public void run() {
                if (isDisposed()) {
                    return;
                }

                if (backgroundTasks.size() == 0) {
                    offlineLabel.setText(Messages.getString("OfflineCheckinControl.OfflineLabelText")); //$NON-NLS-1$
                } else if (backgroundTasks.size() == 1) {
                    final String connectString = backgroundTasks.iterator().next().getName() + " ... "; //$NON-NLS-1$

                    offlineLabel.setText(connectString);
                } else {
                    offlineLabel.setText(Messages.getString("OfflineCheckinControl.ConnectingLabelText")); //$NON-NLS-1$
                }

                /*
                 * Label width changes; layout to recenter it.
                 */
                layout();
            }
        });
    }

    @Override
    public void addContributions(final IContributionManager contributionManager, final String groupName) {
    }

    @Override
    public void removeContributions(final IContributionManager contributionManager, final String groupname) {

    }
}
