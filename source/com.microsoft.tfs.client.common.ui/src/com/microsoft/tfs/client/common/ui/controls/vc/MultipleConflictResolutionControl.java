// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.controls.vc;

import java.text.MessageFormat;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.controls.generic.BaseControl;
import com.microsoft.tfs.client.common.ui.controls.generic.ButtonGroup;
import com.microsoft.tfs.client.common.ui.prefs.ExternalToolPreferenceKey;
import com.microsoft.tfs.core.clients.versioncontrol.conflicts.ConflictCategory;
import com.microsoft.tfs.core.clients.versioncontrol.conflicts.ConflictDescription;
import com.microsoft.tfs.core.clients.versioncontrol.conflicts.ConflictDescriptionFactory;
import com.microsoft.tfs.core.clients.versioncontrol.conflicts.resolutions.ConflictResolution;
import com.microsoft.tfs.core.clients.versioncontrol.conflicts.resolutions.contributors.ConflictResolutionContributor;
import com.microsoft.tfs.core.clients.versioncontrol.conflicts.resolutions.contributors.ExternalConflictResolutionContributor;
import com.microsoft.tfs.core.config.persistence.DefaultPersistenceStoreProvider;
import com.microsoft.tfs.core.externaltools.ExternalToolset;
import com.microsoft.tfs.core.util.MementoRepository;

/**
 * A control which shows a single category of conflicts and all possible ways to
 * resolve them. Used for each conflict category in a
 * MultipleConflictResolutionDialog.
 */
public class MultipleConflictResolutionControl extends BaseControl {
    private final ButtonGroup buttonGroup;
    private final Composite group;
    private final Label resolutionLabel;
    private final Combo resolutionCombo;

    private ConflictCategory conflictCategory;
    private int count;
    private ConflictResolution[] resolutions = new ConflictResolution[0];

    private final ConflictResolutionContributor resolutionContributor;

    public MultipleConflictResolutionControl(final Composite parent, final int style) {
        super(parent, style);

        setLayout(new FillLayout());

        // setup a group for visual distinction of this conflict type. group's
        // title is the name of the type of conflict, plus the number of
        // conflicts, eg "Version Conflicts (3)", "Writable Files (1)"...
        buttonGroup = new ButtonGroup(this, SWT.NONE);
        buttonGroup.setText(""); //$NON-NLS-1$
        group = buttonGroup.getClientComposite();

        final GridLayout groupLayout = new GridLayout(2, false);
        groupLayout.horizontalSpacing = getHorizontalSpacing() * 2;
        groupLayout.verticalSpacing = getVerticalSpacing();
        groupLayout.marginWidth = getHorizontalMargin();
        groupLayout.marginHeight = getVerticalMargin();
        group.setLayout(groupLayout);

        resolutionLabel = new Label(group, SWT.NONE);
        resolutionLabel.setText(Messages.getString("MultipleConflictResolutionControl.ResolutionLabelText")); //$NON-NLS-1$

        final GridData resolutionLabelData = new GridData(SWT.LEFT, SWT.CENTER, false, true);
        resolutionLabel.setLayoutData(resolutionLabelData);

        resolutionCombo = new Combo(group, SWT.READ_ONLY);
        resolutionCombo.setItems(new String[] {
            Messages.getString("MultipleConflictResolutionControl.NotConfiguredComboItemText") //$NON-NLS-1$
        });
        resolutionCombo.select(0);

        final GridData resolutionComboData = new GridData(SWT.FILL, SWT.CENTER, true, true);
        resolutionComboData.grabExcessHorizontalSpace = true;
        resolutionCombo.setLayoutData(resolutionComboData);

        buttonGroup.setSelection(false);
        buttonGroup.setGroupDisabledWithButton(true);

        resolutionContributor = new ExternalConflictResolutionContributor(
            ExternalToolset.loadFromMemento(
                new MementoRepository(DefaultPersistenceStoreProvider.INSTANCE.getConfigurationPersistenceStore()).load(
                    ExternalToolPreferenceKey.MERGE_KEY)));
    }

    public void setConflictCategory(final ConflictCategory conflictCategory) {
        this.conflictCategory = conflictCategory;

        setGroupTitle();
        setResolutionOptions();
    }

    public void setConflictCount(final int count) {
        this.count = count;

        setGroupTitle();
    }

    public void addSelectionListener(final SelectionListener listener) {
        buttonGroup.getButton().addSelectionListener(listener);
    }

    public void removeSelectionListener(final SelectionListener listener) {
        buttonGroup.getButton().removeSelectionListener(listener);
    }

    private void setGroupTitle() {
        String groupTitle = ""; //$NON-NLS-1$

        if (conflictCategory != null) {
            final ConflictDescription categoryDescription =
                ConflictDescriptionFactory.getConflictDescription(conflictCategory);

            if (count == 0) {
                groupTitle = MessageFormat.format(
                    Messages.getString("MultipleConflictResolutionControl.ZeroConflictsGroupTitleFormat"), //$NON-NLS-1$
                    categoryDescription.getName());
            } else if (count == 1) {
                groupTitle = MessageFormat.format(
                    Messages.getString("MultipleConflictResolutionControl.OneConflictGroupTitleFormat"), //$NON-NLS-1$
                    categoryDescription.getName());
            } else {
                groupTitle = MessageFormat.format(
                    //@formatter:off
                    Messages.getString("MultipleConflictResolutionControl.MoreThanOneConflictWithCountGroupTitleFormat"), //$NON-NLS-1$
                    //@formatter:on
                    categoryDescription.getName(),
                    count);
            }
        }

        buttonGroup.setText(groupTitle);
    }

    private void setResolutionOptions() {
        final ConflictDescription categoryDescription =
            ConflictDescriptionFactory.getConflictDescription(conflictCategory);

        resolutions = categoryDescription.getResolutions(resolutionContributor);

        final String[] descriptions = new String[resolutions.length];
        for (int i = 0; i < resolutions.length; i++) {
            descriptions[i] = resolutions[i].getDescription();
        }

        resolutionCombo.setItems(descriptions);
        resolutionCombo.select(0);
    }

    public void updateSelection() {
        resolutionCombo.setEnabled(buttonGroup.getSelection());
    }

    public boolean getSelection() {
        return buttonGroup.getSelection();
    }

    public ConflictResolution getResolution() {
        final int selectionIndex = resolutionCombo.getSelectionIndex();

        if (selectionIndex >= 0) {
            return resolutions[selectionIndex];
        }

        return null;
    }
}
