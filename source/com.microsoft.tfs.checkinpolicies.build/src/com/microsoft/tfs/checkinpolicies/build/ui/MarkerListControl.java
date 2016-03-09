// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.checkinpolicies.build.ui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;

import com.microsoft.tfs.checkinpolicies.build.Messages;
import com.microsoft.tfs.checkinpolicies.build.settings.MarkerMatch;
import com.microsoft.tfs.client.common.ui.framework.helper.SWTUtil;
import com.microsoft.tfs.client.common.ui.framework.layout.GridDataBuilder;
import com.microsoft.tfs.client.common.ui.framework.validation.ButtonValidatorBinding;
import com.microsoft.tfs.client.common.ui.framework.validation.NumericConstraint;
import com.microsoft.tfs.client.common.ui.framework.validation.SelectionProviderValidator;
import com.microsoft.tfs.util.Check;

public class MarkerListControl extends org.eclipse.swt.widgets.Composite {
    private final List markerMatches = new ArrayList();

    private final MarkerMatchTable markerTable;

    private final Button addButton;
    private final Button deleteButton;

    public MarkerListControl(final Composite parent, final int style) {
        super(parent, style);

        SWTUtil.gridLayout(this, 2);

        // Table.

        markerTable = new MarkerMatchTable(this, SWT.SINGLE | SWT.FULL_SELECTION);
        GridDataBuilder.newInstance().vSpan(3).grab().fill().applyTo(markerTable);

        // Buttons.

        addButton = SWTUtil.createButton(this, Messages.getString("MarkerListControl.AddButtonText")); //$NON-NLS-1$
        GridDataBuilder.newInstance().vAlign(SWT.TOP).hFill().applyTo(addButton);
        addButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
                addClicked();
            }
        });

        deleteButton = SWTUtil.createButton(this, Messages.getString("MarkerListControl.DeleteButtonText")); //$NON-NLS-1$
        GridDataBuilder.newInstance().vAlign(SWT.TOP).hFill().applyTo(deleteButton);
        deleteButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
                deleteClicked();
            }
        });

        new ButtonValidatorBinding(deleteButton).bind(
            new SelectionProviderValidator(markerTable, NumericConstraint.ONE_OR_MORE));

        refreshTable();
    }

    @Override
    public void setEnabled(final boolean enabled) {
        super.setEnabled(enabled);

        markerTable.setEnabled(enabled);
        addButton.setEnabled(enabled);
        deleteButton.setEnabled(enabled);
    }

    public MarkerMatchTable getTable() {
        return markerTable;
    }

    public void setMarkers(final MarkerMatch[] markers) {
        Check.notNull(markers, "markers"); //$NON-NLS-1$

        markerMatches.clear();
        markerMatches.addAll(Arrays.asList(markers));
        refreshTable();
    }

    public MarkerMatch[] getMarkers() {
        return (MarkerMatch[]) markerMatches.toArray(new MarkerMatch[markerMatches.size()]);
    }

    private void deleteClicked() {
        final MarkerMatch[] selectedMarkers = markerTable.getSelectedMarkers();
        markerMatches.removeAll(Arrays.asList(selectedMarkers));
        refreshTable();
    }

    private void addClicked() {
        final MarkerBrowseDialog browseDialog = new MarkerBrowseDialog(getShell());

        if (browseDialog.open() != IDialogConstants.OK_ID) {
            return;
        }

        if (browseDialog.getMarker().trim().length() == 0) {
            return;
        }

        final MarkerMatch newMarker = new MarkerMatch(
            browseDialog.getMarker(),
            true,
            "", //$NON-NLS-1$
            true,
            false,
            false,
            false,
            false,
            false);

        markerMatches.add(newMarker);

        refreshTable();

        markerTable.setSelectedElement(newMarker);
    }

    public void refreshTable() {
        markerTable.setMarkers(getMarkers());
    }
}
