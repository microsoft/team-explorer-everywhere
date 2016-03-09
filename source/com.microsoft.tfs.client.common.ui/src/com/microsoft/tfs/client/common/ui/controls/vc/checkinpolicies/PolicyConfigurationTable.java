// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.controls.vc.checkinpolicies;

import java.util.Comparator;

import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.ICellModifier;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Item;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;

import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.framework.helper.TableViewerSorter;
import com.microsoft.tfs.client.common.ui.framework.table.TableColumnData;
import com.microsoft.tfs.client.common.ui.framework.table.TableControl;

public class PolicyConfigurationTable extends TableControl {
    private static final String ENABLED_COLUMN_NAME = "enabled"; //$NON-NLS-1$
    private static final String POLICY_TYPE_COLUMN_NAME = "policyType"; //$NON-NLS-1$
    private static final String DESCRIPTION_COLUMN_NAME = "description"; //$NON-NLS-1$
    private static final String PRIORITY_COLUMN_NAME = "priority"; //$NON-NLS-1$

    public PolicyConfigurationTable(final Composite parent, final int style) {
        this(parent, style, null);
    }

    public PolicyConfigurationTable(final Composite parent, final int style, final String viewDataKey) {
        super(parent, style | SWT.CHECK, PolicyConfiguration.class, viewDataKey);

        final TableColumnData[] columnData = new TableColumnData[] {
            new TableColumnData(
                Messages.getString("PolicyConfigurationTable.ColumnNameEnabled"), //$NON-NLS-1$
                50,
                ENABLED_COLUMN_NAME),
            new TableColumnData(
                Messages.getString("PolicyConfigurationTable.ColumnNamePolicyType"), //$NON-NLS-1$
                100,
                POLICY_TYPE_COLUMN_NAME),
            new TableColumnData(
                Messages.getString("PolicyConfigurationTable.ColumnNameDescription"), //$NON-NLS-1$
                250,
                DESCRIPTION_COLUMN_NAME),
            new TableColumnData(
                Messages.getString("PolicyConfigurationTable.ColumnNamePriority"), //$NON-NLS-1$
                100,
                PRIORITY_COLUMN_NAME)
        };

        setupTable(true, false, columnData);

        setUseViewerDefaults();
        setEnableTooltips(true);

        final TableViewerSorter sorter = (TableViewerSorter) getViewer().getSorter();
        sorter.setComparator(PRIORITY_COLUMN_NAME, new Comparator() {
            @Override
            public int compare(final Object o1, final Object o2) {
                final PolicyConfiguration policyConfiguration1 = (PolicyConfiguration) o1;
                final PolicyConfiguration policyConfiguration2 = (PolicyConfiguration) o2;
                if (policyConfiguration1.getPriority() < policyConfiguration2.getPriority()) {
                    return -1;
                }
                if (policyConfiguration1.getPriority() > policyConfiguration2.getPriority()) {
                    return 1;
                }
                return 0;
            }
        });

        setCellEditor(PRIORITY_COLUMN_NAME, new TextCellEditor(getTable()));
        getViewer().setCellModifier(new CellModifier(getViewer()));

        addCheckStateListener(new ICheckStateListener() {
            @Override
            public void checkStateChanged(final CheckStateChangedEvent event) {
                final PolicyConfiguration policyConfiguration = (PolicyConfiguration) event.getElement();
                policyConfiguration.setEnabled(event.getChecked());
            }
        });
    }

    public void setPolicyConfigurations(final PolicyConfiguration[] policyConfigurations) {
        setElements(policyConfigurations);

        for (int i = 0; i < policyConfigurations.length; i++) {
            if (policyConfigurations[i].isEnabled()) {
                setChecked(policyConfigurations[i]);
            }
        }
    }

    public PolicyConfiguration[] getPolicyConfigurations() {
        return (PolicyConfiguration[]) getElements();
    }

    public void setSelectedPolicyConfigurations(final PolicyConfiguration[] policyConfigurations) {
        setSelectedElements(policyConfigurations);
    }

    public void setSelectedPolicyConfiguration(final PolicyConfiguration policyConfiguration) {
        setSelectedElement(policyConfiguration);
    }

    public PolicyConfiguration[] getSelectedPolicyConfigurations() {
        return (PolicyConfiguration[]) getSelectedElements();
    }

    public PolicyConfiguration getSelectedPolicyConfiguration() {
        return (PolicyConfiguration) getSelectedElement();
    }

    public void setCheckedPolicyConfigurations(final PolicyConfiguration[] policyConfigurations) {
        setCheckedElements(policyConfigurations);
    }

    public PolicyConfiguration[] getCheckedPolicyConfigurations() {
        return (PolicyConfiguration[]) getCheckedElements();
    }

    @Override
    public String getTooltipText(final Object element, final int columnIndex) {
        final PolicyConfiguration policyConfiguration = (PolicyConfiguration) element;
        if (!policyConfiguration.hasInstance()) {
            return Messages.getString("PolicyConfigurationTable.TooltipText"); //$NON-NLS-1$
        }
        return null;
    }

    @Override
    protected Image getColumnImage(final Object element, final String columnPropertyName) {
        final PolicyConfiguration policyConfiguration = (PolicyConfiguration) element;

        if (ENABLED_COLUMN_NAME.equals(columnPropertyName)) {
            if (!policyConfiguration.hasInstance()) {
                return PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJS_ERROR_TSK);
            }
        }

        return null;
    }

    @Override
    protected String getColumnText(final Object element, final String columnPropertyName) {
        final PolicyConfiguration policyConfiguration = (PolicyConfiguration) element;

        if (POLICY_TYPE_COLUMN_NAME.equals(columnPropertyName)) {
            return policyConfiguration.getType().getName();
        }

        if (DESCRIPTION_COLUMN_NAME.equals(columnPropertyName)) {
            return policyConfiguration.getType().getShortDescription();
        }

        if (PRIORITY_COLUMN_NAME.equals(columnPropertyName)) {
            return String.valueOf(policyConfiguration.getPriority());
        }

        return null;
    }

    private static class CellModifier implements ICellModifier {
        private final StructuredViewer viewer;

        public CellModifier(final StructuredViewer viewer) {
            this.viewer = viewer;
        }

        @Override
        public boolean canModify(final Object element, final String property) {
            return PRIORITY_COLUMN_NAME.equals(property);
        }

        @Override
        public Object getValue(final Object element, final String property) {
            final PolicyConfiguration policyConfiguration = (PolicyConfiguration) element;
            return String.valueOf(policyConfiguration.getPriority());
        }

        @Override
        public void modify(final Object element, final String property, final Object value) {
            final PolicyConfiguration policyConfiguration =
                (PolicyConfiguration) (element instanceof Item ? ((Item) element).getData() : element);

            final String sValue = (String) value;
            int priority;
            try {
                priority = Integer.parseInt(sValue);
            } catch (final NumberFormatException e) {
                return;
            }
            policyConfiguration.setPriority(priority);

            viewer.update(policyConfiguration, new String[] {
                property
            });
        }
    }
}
