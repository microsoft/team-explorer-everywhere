// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.controls.vc.checkin;

import org.eclipse.jface.action.IContributionManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

import com.microsoft.tfs.client.common.commands.RestoreConnectionCommand;
import com.microsoft.tfs.client.common.repository.TFSRepository;
import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.controls.generic.compatibility.link.CompatibilityLinkControl;
import com.microsoft.tfs.client.common.ui.controls.generic.compatibility.link.CompatibilityLinkFactory;
import com.microsoft.tfs.client.common.ui.framework.command.UICommandExecutorFactory;
import com.microsoft.tfs.client.common.ui.framework.layout.GridDataBuilder;

public class ConnectionFailureCheckinControl extends AbstractCheckinSubControl {
    private final Label connectionFailureLabel;
    private final CompatibilityLinkControl reconnectLink;

    private TFSRepository repository;

    public ConnectionFailureCheckinControl(
        final Composite parent,
        final int style,
        final CheckinControlOptions options) {
        super(
            parent,
            style,
            Messages.getString("ConnectionFailureCheckinControl.Title"), //$NON-NLS-1$
            CheckinSubControlType.CONNECTION_FAILURE);

        final GridLayout layout = new GridLayout(1, false);
        layout.marginWidth = getHorizontalMargin();
        layout.marginHeight = getVerticalMargin();
        layout.horizontalSpacing = getHorizontalSpacing();
        layout.verticalSpacing = getVerticalSpacing();
        setLayout(layout);

        final Composite composite = new Composite(this, SWT.NONE);
        GridDataBuilder.newInstance().hAlignCenter().hGrab().vAlignCenter().vGrab().applyTo(composite);

        final GridLayout compositeLayout = new GridLayout(1, false);
        compositeLayout.marginWidth = 0;
        compositeLayout.marginHeight = 0;
        compositeLayout.horizontalSpacing = getHorizontalSpacing();
        compositeLayout.verticalSpacing = getVerticalSpacing();
        composite.setLayout(compositeLayout);

        connectionFailureLabel = new Label(composite, SWT.WRAP | SWT.CENTER);
        connectionFailureLabel.setText(Messages.getString("ConnectionFailureCheckinControl.OfflineLabelText")); //$NON-NLS-1$
        GridDataBuilder.newInstance().hAlignCenter().hGrab().applyTo(connectionFailureLabel);

        reconnectLink = CompatibilityLinkFactory.createLink(composite, SWT.NONE);
        reconnectLink.setSimpleText(Messages.getString("ConnectionFailureCheckinControl.ReconnectText")); //$NON-NLS-1$
        reconnectLink.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
                UICommandExecutorFactory.newUICommandExecutor(getShell()).execute(
                    new RestoreConnectionCommand(repository.getConnection()));
            }
        });
        GridDataBuilder.newInstance().hAlignCenter().hGrab().applyTo(reconnectLink.getControl());

        connectionFailureLabel.setFocus();
    }

    public void setRepository(final TFSRepository repository) {
        this.repository = repository;

        reconnectLink.getControl().setVisible(repository != null);
        reconnectLink.getControl().setEnabled(repository != null);
    }

    @Override
    public void addContributions(final IContributionManager contributionManager, final String groupName) {
    }

    @Override
    public void removeContributions(final IContributionManager contributionManager, final String groupname) {
    }
}
