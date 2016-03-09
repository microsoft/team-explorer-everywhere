// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.controls.vc;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import com.microsoft.tfs.client.common.commands.vc.QueryLabelsCommand;
import com.microsoft.tfs.client.common.repository.TFSRepository;
import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.controls.generic.BaseControl;
import com.microsoft.tfs.client.common.ui.framework.command.UICommandExecutorFactory;
import com.microsoft.tfs.client.common.ui.framework.layout.GridDataBuilder;
import com.microsoft.tfs.client.common.ui.framework.sizing.ControlSize;
import com.microsoft.tfs.client.common.ui.helpers.ComboHelper;
import com.microsoft.tfs.core.clients.versioncontrol.path.ServerPath;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.DeletedState;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Item;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.ItemSet;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.ItemType;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.RecursionType;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.VersionControlLabel;
import com.microsoft.tfs.core.clients.versioncontrol.specs.ItemSpec;
import com.microsoft.tfs.core.clients.versioncontrol.specs.version.LatestVersionSpec;
import com.microsoft.tfs.core.clients.webservices.IdentityHelper;
import com.microsoft.tfs.util.Check;

public class FindLabelControl extends BaseControl {
    private final TFSRepository repository;
    private final String teamProjectServerPath;

    private Text nameText;
    private Combo projectCombo;
    private Text ownerText;
    private final LabelsTable labelsTable;

    /* Search results, cached */
    private VersionControlLabel[] labels;

    /* Last search, cached */
    private String lastLabelName;
    private String lastLabelScope;
    private String lastOwnerName;

    public FindLabelControl(
        final Composite parent,
        final int style,
        final TFSRepository repository,
        final String teamProjectServerPath) {
        this(parent, style, repository, teamProjectServerPath, null);
    }

    public FindLabelControl(
        final Composite parent,
        final int style,
        final TFSRepository repository,
        String teamProjectServerPath,
        final String viewDataKey) {
        super(parent, style);

        Check.notNull(repository, "repository"); //$NON-NLS-1$
        this.repository = repository;

        if (teamProjectServerPath != null && teamProjectServerPath.trim().length() == 0) {
            teamProjectServerPath = null;
        }
        this.teamProjectServerPath = teamProjectServerPath;

        final GridLayout layout = new GridLayout();
        layout.marginWidth = 0;
        layout.marginHeight = 0;
        layout.horizontalSpacing = getHorizontalSpacing();
        layout.verticalSpacing = getVerticalSpacing();
        setLayout(layout);

        final Control findOptionsArea = createFindOptionsArea(this);
        GridDataBuilder.newInstance().hGrab().hFill().applyTo(findOptionsArea);

        labelsTable = new LabelsTable(this, this.repository, SWT.FULL_SELECTION, viewDataKey);
        labelsTable.setText(Messages.getString("FindLabelControl.ResultsTableText")); //$NON-NLS-1$
        GridDataBuilder.newInstance().grab().fill().applyTo(labelsTable);
        ControlSize.setCharHeightHint(labelsTable, 10);
    }

    public LabelsTable getLabelsTable() {
        return labelsTable;
    }

    @Override
    public boolean setFocus() {
        return nameText.setFocus();
    }

    private Control createFindOptionsArea(final Composite parent) {
        final Group composite = new Group(parent, SWT.NONE);
        composite.setText(Messages.getString("FindLabelControl.FindOptionsGroupText")); //$NON-NLS-1$

        final GridLayout layout = new GridLayout(2, false);
        layout.marginWidth = getHorizontalMargin();
        layout.marginHeight = getVerticalMargin();
        layout.horizontalSpacing = getHorizontalSpacing();
        layout.verticalSpacing = getVerticalSpacing();
        composite.setLayout(layout);

        Label label = new Label(composite, SWT.NONE);
        label.setText(Messages.getString("FindLabelControl.NameLabelText")); //$NON-NLS-1$

        nameText = new Text(composite, SWT.BORDER);
        GridDataBuilder.newInstance().hGrab().hFill().applyTo(nameText);

        label = new Label(composite, SWT.NONE);
        label.setText(Messages.getString("FindLabelControl.ProjectLabelText")); //$NON-NLS-1$

        projectCombo = new Combo(composite, SWT.NONE);
        GridDataBuilder.newInstance().hGrab().hFill().applyTo(projectCombo);

        populateProjectCombo();

        label = new Label(composite, SWT.NONE);
        label.setText(Messages.getString("FindLabelControl.OwnerLabelText")); //$NON-NLS-1$

        ownerText = new Text(composite, SWT.BORDER);
        GridDataBuilder.newInstance().hGrab().hFill().applyTo(ownerText);

        final Button button = new Button(composite, SWT.NONE);
        button.setText(Messages.getString("FindLabelControl.FindButtonText")); //$NON-NLS-1$
        GridDataBuilder.newInstance().hSpan(layout).applyTo(button);

        // Make this the default button.
        getShell().setDefaultButton(button);

        button.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
                find();
            }
        });

        return composite;
    }

    public void deleteLabel(final VersionControlLabel deleteLabel) {
        boolean matched = false;

        if (labels.length == 0) {
            return;
        }

        final VersionControlLabel[] newLabels = new VersionControlLabel[labels.length - 1];

        for (int i = 0, j = 0; i < labels.length; i++) {
            if (labels[i].equals(deleteLabel)) {
                matched = true;
            } else {
                newLabels[j++] = labels[i];
            }
        }

        if (matched) {
            labels = newLabels;
            labelsTable.setLabels(labels);
        } else {
            /* Couldn't update, redo the search */
            find(lastLabelName, lastLabelScope, lastOwnerName);
        }
    }

    public void updateLabel(final VersionControlLabel oldLabel, final VersionControlLabel newLabel) {
        boolean labelUpdated = false;

        if (oldLabel != null && newLabel != null) {
            for (int i = 0; i < labels.length; i++) {
                if (labels[i].equals(oldLabel)) {
                    labels[i] = newLabel;
                    labelUpdated = true;
                }
            }
        }

        if (labelUpdated) {
            labelsTable.getViewer().refresh(true);

            final StructuredSelection newSelection = new StructuredSelection(newLabel);
            labelsTable.getViewer().setSelection(newSelection, true);
        } else {
            /* Couldn't update, redo the search */
            find(lastLabelName, lastLabelScope, lastOwnerName);
        }
    }

    private void find() {
        String labelScope;

        if (teamProjectServerPath != null) {
            labelScope = teamProjectServerPath;
        } else if (projectCombo.getSelectionIndex() != 0) {
            labelScope = ServerPath.ROOT + projectCombo.getText();
        } else {
            labelScope = ServerPath.ROOT;
        }

        String labelName = nameText.getText();
        if (labelName.trim().length() == 0) {
            labelName = null;
        }

        String ownerName = ownerText.getText();
        if (ownerName.trim().length() == 0) {
            ownerName = null;
        }

        ownerName =
            IdentityHelper.getUniqueNameIfCurrentUser(repository.getConnection().getAuthorizedIdentity(), ownerName);

        find(labelName, labelScope, ownerName);
    }

    /*
     * TODO: don't block the ui
     */
    private void find(final String labelName, final String labelScope, final String ownerName) {
        final QueryLabelsCommand queryCommand = new QueryLabelsCommand(repository, labelName, labelScope, ownerName);
        final IStatus queryStatus = UICommandExecutorFactory.newUICommandExecutor(getShell()).execute(queryCommand);

        VersionControlLabel[] labels;
        if (!queryStatus.isOK()) {
            labels = new VersionControlLabel[0];
        } else {
            labels = queryCommand.getLabels();
        }

        lastLabelName = labelName;
        lastLabelScope = labelScope;
        lastOwnerName = ownerName;

        this.labels = labels;
        labelsTable.setLabels(labels);
    }

    private void populateProjectCombo() {
        if (teamProjectServerPath != null) {
            projectCombo.add(teamProjectServerPath);
            projectCombo.select(0);
            projectCombo.setEnabled(false);
            return;
        }

        projectCombo.add(Messages.getString("FindLabelControl.AllProjectsComboItemText")); //$NON-NLS-1$

        final ItemSpec[] itemSpecs = new ItemSpec[] {
            new ItemSpec(ServerPath.ROOT, RecursionType.ONE_LEVEL)
        };

        final ItemSet itemSet = repository.getVersionControlClient().getItems(
            itemSpecs,
            LatestVersionSpec.INSTANCE,
            DeletedState.NON_DELETED,
            ItemType.FOLDER,
            false)[0];

        final Item[] items = itemSet.getItems();
        for (int i = 0; i < items.length; i++) {
            if (ServerPath.equals(ServerPath.ROOT, items[i].getServerItem())) {
                continue;
            }

            final String teamProjectName = ServerPath.getFileName(items[i].getServerItem());
            projectCombo.add(teamProjectName);
        }

        projectCombo.select(0);
        ComboHelper.setVisibleItemCount(projectCombo);
    }
}
