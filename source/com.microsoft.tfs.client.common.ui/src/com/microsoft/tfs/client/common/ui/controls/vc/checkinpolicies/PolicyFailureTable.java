// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.controls.vc.checkinpolicies;

import org.eclipse.swt.widgets.Composite;

import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.framework.table.FillLastColumnLayout;
import com.microsoft.tfs.client.common.ui.framework.table.TableColumnData;
import com.microsoft.tfs.client.common.ui.framework.table.TableControl;

public class PolicyFailureTable extends TableControl {
    public PolicyFailureTable(final Composite parent, final int style) {
        this(parent, style, null);
    }

    public PolicyFailureTable(final Composite parent, final int style, final String viewDataKey) {
        super(parent, style, PolicyFailureData.class, viewDataKey);

        final TableColumnData[] columnData = new TableColumnData[] {
            new TableColumnData(Messages.getString("PolicyFailureTable.ColumnNameDescription"), 200, "description") //$NON-NLS-1$ //$NON-NLS-2$
        };

        setupTable(true, false, columnData);

        setUseViewerDefaults();

        // Make the last column take up the full width.
        getViewer().getTable().setLayout(new FillLastColumnLayout());
    }

    public void setPolicyFailures(final PolicyFailureData[] policyFailures) {
        setElements(policyFailures);
    }

    public PolicyFailureData[] getPolicyFailures() {
        return (PolicyFailureData[]) getElements();
    }

    public void setSelectedPolicyFailures(final PolicyFailureData[] policyFailures) {
        setSelectedElements(policyFailures);
    }

    public void setSelectedPolicyFailure(final PolicyFailureData policyFailure) {
        setSelectedElement(policyFailure);
    }

    public PolicyFailureData[] getSelectedPolicyFailures() {
        return (PolicyFailureData[]) getSelectedElements();
    }

    public PolicyFailureData getSelectedPolicyFailure() {
        return (PolicyFailureData) getSelectedElement();
    }

    @Override
    protected String getColumnText(final Object element, final int columnIndex) {
        final PolicyFailureData failure = (PolicyFailureData) element;
        return failure.getMessage();
    }
}
