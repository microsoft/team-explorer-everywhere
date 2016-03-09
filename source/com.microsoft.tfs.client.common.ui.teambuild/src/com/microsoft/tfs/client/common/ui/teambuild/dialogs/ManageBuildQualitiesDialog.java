// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.teambuild.dialogs;

import java.text.Collator;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.HashSet;
import java.util.SortedSet;
import java.util.TreeSet;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import com.microsoft.tfs.client.common.ui.framework.dialog.BaseDialog;
import com.microsoft.tfs.client.common.ui.framework.helper.ButtonHelper;
import com.microsoft.tfs.client.common.ui.framework.helper.SWTUtil;
import com.microsoft.tfs.client.common.ui.framework.layout.GridDataBuilder;
import com.microsoft.tfs.client.common.ui.framework.sizing.ControlSize;
import com.microsoft.tfs.client.common.ui.teambuild.Messages;
import com.microsoft.tfs.core.clients.build.IBuildServer;
import com.microsoft.tfs.core.clients.build.internal.TeamBuildCache;

public class ManageBuildQualitiesDialog extends BaseDialog {

    private final HashSet qualitiesAdded = new HashSet();
    private final HashSet qualitiesRemoved = new HashSet();
    private final SortedSet qualities = new TreeSet(Collator.getInstance());

    private Text qualityToAdd;
    private List qualitiesList;

    public ManageBuildQualitiesDialog(
        final Shell parentShell,
        final IBuildServer buildServer,
        final String teamProject) {
        super(parentShell);
        final String[] qualities = TeamBuildCache.getInstance(buildServer, teamProject).getBuildQualities(true);
        this.qualities.addAll(Arrays.asList(qualities));
    }

    @Override
    protected void hookAddToDialogArea(final Composite dialogArea) {
        final GridLayout layout = SWTUtil.gridLayout(dialogArea, 2);
        layout.marginWidth = getHorizontalMargin();
        layout.marginHeight = getVerticalMargin();
        layout.horizontalSpacing = getHorizontalSpacing();
        layout.verticalSpacing = getVerticalSpacing();

        SWTUtil.createLabel(dialogArea, Messages.getString("ManageBuildQualitiesDialog.QualityNameLabelText")); //$NON-NLS-1$
        SWTUtil.createLabel(dialogArea, ""); //$NON-NLS-1$

        qualityToAdd = new Text(dialogArea, SWT.BORDER);
        GridDataBuilder.newInstance().hFill().hGrab().applyTo(qualityToAdd);

        final Button addButton =
            SWTUtil.createButton(dialogArea, Messages.getString("ManageBuildQualitiesDialog.AddButtonText")); //$NON-NLS-1$
        addButton.setEnabled(false);
        GridDataBuilder.newInstance().hFill().applyTo(addButton);
        addButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
                addButtonPressed();
            }
        });

        qualityToAdd.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(final ModifyEvent e) {
                addButton.setEnabled(qualityToAdd.getText().length() > 0);
            }
        });

        qualitiesList = new List(dialogArea, SWT.BORDER);
        GridDataBuilder.newInstance().fill().grab().applyTo(qualitiesList);
        ControlSize.setCharHeightHint(qualitiesList, 12);
        ControlSize.setCharWidthHint(qualitiesList, 40);
        final Button removeButton =
            SWTUtil.createButton(dialogArea, Messages.getString("ManageBuildQualitiesDialog.RemoveButtonText")); //$NON-NLS-1$
        GridDataBuilder.newInstance().vAlign(SWT.TOP).applyTo(removeButton);
        removeButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
                removeButtonPressed();
            }
        });

        loadQualities();
        qualitiesList.setSelection(0);

        ButtonHelper.setButtonsToButtonBarSize(new Button[] {
            addButton,
            removeButton
        });
    }

    protected void addButtonPressed() {
        final String quality = qualityToAdd.getText();

        qualityToAdd.setFocus();
        qualityToAdd.selectAll();

        if (!isValidBuildQuality(quality)) {
            MessageDialog.openError(
                getShell(),
                Messages.getString("ManageBuildQualitiesDialog.InvalidQualityDialogTitle"), //$NON-NLS-1$
                Messages.getString("ManageBuildQualitiesDialog.InvalidQualityDialogMessage")); //$NON-NLS-1$
            return;
        }

        if (qualities.contains(quality)) {
            final String messageFormat =
                Messages.getString("ManageBuildQualitiesDialog.BuildQualityExistsDialogTextFormat"); //$NON-NLS-1$
            final String message = MessageFormat.format(messageFormat, quality);

            MessageDialog.openError(
                getShell(),
                Messages.getString("ManageBuildQualitiesDialog.BuildQualityExistsDialogTitle"), //$NON-NLS-1$
                message);
            return;
        }

        qualities.add(quality);
        qualitiesList.add(quality);
        qualitiesAdded.add(quality);
        if (qualitiesRemoved.contains(quality)) {
            qualitiesRemoved.remove(quality);
        }

        loadQualities();
    }

    protected void removeButtonPressed() {
        final String[] selection = qualitiesList.getSelection();

        if (selection == null || selection.length == 0) {
            return;
        }

        final String quality = selection[0];
        if (qualities.contains(quality)) {
            qualities.remove(quality);
            qualitiesRemoved.add(quality);
            if (qualitiesAdded.contains(quality)) {
                qualitiesAdded.remove(quality);
            }
            loadQualities();
        }
        qualityToAdd.setText(quality);
        qualityToAdd.selectAll();
        qualityToAdd.setFocus();

    }

    private boolean isValidBuildQuality(final String buildQuality) {
        final char[] chars = buildQuality.toCharArray();
        for (int i = 0; i < chars.length; i++) {
            if (Character.isISOControl(chars[i])) {
                return false;
            }
        }
        return true;
    }

    private void loadQualities() {
        final int selectedIndex = qualitiesList.getSelectionIndex();
        qualitiesList.setItems((String[]) qualities.toArray(new String[qualities.size()]));
        if (selectedIndex >= 0 && selectedIndex <= qualitiesList.getItemCount()) {
            qualitiesList.select(selectedIndex);
        }
    }

    /**
     * @see com.microsoft.tfs.client.common.ui.framework.dialog.BaseDialog#provideDialogTitle()
     */
    @Override
    protected String provideDialogTitle() {
        return Messages.getString("ManageBuildQualitiesDialog.EditQualityDialogTitle"); //$NON-NLS-1$
    }

    public String[] getQualitiesToAdd() {
        return (String[]) qualitiesAdded.toArray(new String[qualitiesAdded.size()]);
    }

    public String[] getQualitiesToRemove() {
        return (String[]) qualitiesRemoved.toArray(new String[qualitiesRemoved.size()]);
    }

}
