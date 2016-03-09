// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.teambuild.controls.builddefinition;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.viewers.ComboBoxCellEditor;
import org.eclipse.jface.viewers.ICellModifier;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Item;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.TableItem;

import com.microsoft.tfs.client.common.ui.framework.helper.TableViewerSorter;
import com.microsoft.tfs.client.common.ui.framework.helper.TableViewerSorter.SortDirection;
import com.microsoft.tfs.client.common.ui.framework.table.EqualSizeTableLayout;
import com.microsoft.tfs.client.common.ui.framework.table.TableColumnData;
import com.microsoft.tfs.client.common.ui.framework.table.TableControl;
import com.microsoft.tfs.client.common.ui.teambuild.Messages;
import com.microsoft.tfs.client.common.ui.teambuild.TeamBuildImageHelper;
import com.microsoft.tfs.client.common.ui.teambuild.dialogs.RetentionPolicyDeleteDialog;
import com.microsoft.tfs.core.clients.build.IBuildDefinition;
import com.microsoft.tfs.core.clients.build.IBuildServer;
import com.microsoft.tfs.core.clients.build.IRetentionPolicy;
import com.microsoft.tfs.core.clients.build.flags.BuildReason;
import com.microsoft.tfs.core.clients.build.flags.BuildStatus;
import com.microsoft.tfs.core.clients.build.flags.DeleteOptions;

/**
 */
public class RetentionPolicyTableControl extends TableControl {

    private final IBuildServer buildServer;

    private static final String KEEP_ALL_TEXT = Messages.getString("RetentionPolicyTableControl.KeepAllText"); //$NON-NLS-1$
    private static final String KEEP_NONE_TEXT = Messages.getString("RetentionPolicyTableControl.KeepNoneText"); //$NON-NLS-1$
    private static final String KEEP_LATEST_TEXT = Messages.getString("RetentionPolicyTableControl.KeepLatestText"); //$NON-NLS-1$
    private static final String KEEP_CUSTOM_TEXT = Messages.getString("RetentionPolicyTableControl.KeepCustomText"); //$NON-NLS-1$

    private static final String DELETE_CUSTOM_TEXT = Messages.getString("RetentionPolicyTableControl.DeleteCustomText"); //$NON-NLS-1$

    static final String[] RETENTION_POLICY_STANDARD_TEXTS = new String[] {
        KEEP_ALL_TEXT,
        KEEP_NONE_TEXT,
        KEEP_LATEST_TEXT,
        Messages.getString("RetentionPolicyTableControl.KeepTwoLatestText"), //$NON-NLS-1$
        Messages.getString("RetentionPolicyTableControl.KeepFiveLatestText"), //$NON-NLS-1$
        Messages.getString("RetentionPolicyTableControl.KeepSevenLatestText"), //$NON-NLS-1$
        Messages.getString("RetentionPolicyTableControl.KeepTenLatestText"), //$NON-NLS-1$
        KEEP_CUSTOM_TEXT
    };

    static final int[] RETENTION_POLICY_STANDARD_VALUES = new int[] {
        Integer.MAX_VALUE,
        0,
        1,
        2,
        5,
        7,
        10,
        -1
    };

    static final String[] DELETE_ITEMS_STANDARD_TEXTS = new String[] {
        Messages.getString("RetentionPolicyTableControl.DeleteAllText"), //$NON-NLS-1$
        Messages.getString("RetentionPolicyTableControl.DeleteDetailDropLabelSymbolsText"), //$NON-NLS-1$
        Messages.getString("RetentionPolicyTableControl.DeleteDetailDropTestText"), //$NON-NLS-1$
        Messages.getString("RetentionPolicyTableControl.DeleteDetailDropLabelText"), //$NON-NLS-1$
        Messages.getString("RetentionPolicyTableControl.DeleteDetailDropText"), //$NON-NLS-1$
        Messages.getString("RetentionPolicyTableControl.DeleteDetailText"), //$NON-NLS-1$
        DELETE_CUSTOM_TEXT
    };

    static final DeleteOptions[] DELETE_ITEMS_STANDARD_VALUES = new DeleteOptions[] {
        DeleteOptions.ALL,
        DeleteOptions.ALL.remove(DeleteOptions.TEST_RESULTS),
        DeleteOptions.DETAILS.combine(DeleteOptions.DROP_LOCATION).combine(DeleteOptions.TEST_RESULTS),
        DeleteOptions.DETAILS.combine(DeleteOptions.DROP_LOCATION).combine(DeleteOptions.LABEL),
        DeleteOptions.DETAILS.combine(DeleteOptions.DROP_LOCATION),
        DeleteOptions.DETAILS,
        null
    };

    private static final String KEEP_COL = "keepCount"; //$NON-NLS-1$

    private static final String DELETE_COL = "deleteItems"; //$NON-NLS-1$

    private final TeamBuildImageHelper imageHelper = new TeamBuildImageHelper();

    private final ComboBoxCellEditor keepCountCellEditor;
    private final ComboBoxCellEditor deleteItemCellEditor;
    private final CellModifier cellModifier;

    private String currentDisplayText;

    /**
     * @param parent
     * @param style
     * @param elementType
     * @param viewDataKey
     */
    public RetentionPolicyTableControl(final Composite parent, final int style, final IBuildServer buildServer) {
        super(parent, style, IRetentionPolicy.class, null);
        this.buildServer = buildServer;

        final TableColumnData[] columnData = new TableColumnData[] {
            new TableColumnData(
                Messages.getString("RetentionPolicyTableControl.BuildOutcomeColumnText"), //$NON-NLS-1$
                60,
                "statusName"), //$NON-NLS-1$
            new TableColumnData(
                Messages.getString("RetentionPolicyTableControl.RetentionPolicyColumnText"), //$NON-NLS-1$
                50,
                KEEP_COL),
            new TableColumnData(
                Messages.getString("RetentionPolicyTableControl.WhatToDeleteColumnText"), //$NON-NLS-1$
                50,
                DELETE_COL)
        };

        setupTable(
            true, // header visible
            false, // lines invisible
            columnData);

        setUseDefaultLabelProvider();
        setUseDefaultContentProvider();
        getViewer().setSorter(createSorter());

        keepCountCellEditor =
            new ComboBoxCellEditor(getViewer().getTable(), RETENTION_POLICY_STANDARD_TEXTS, SWT.READ_ONLY);
        deleteItemCellEditor =
            new ComboBoxCellEditor(getViewer().getTable(), DELETE_ITEMS_STANDARD_TEXTS, SWT.READ_ONLY);
        cellModifier = new CellModifier(getViewer(), RETENTION_POLICY_STANDARD_TEXTS, DELETE_ITEMS_STANDARD_TEXTS);

        // Add hook to ComboCellEditor to listen for a custom keep count being
        // requested.
        ((CCombo) keepCountCellEditor.getControl()).addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
                if (KEEP_CUSTOM_TEXT.equals(((CCombo) e.getSource()).getText())) {
                    promptForCustomKeepCount();
                }
            }
        });

        ((CCombo) deleteItemCellEditor.getControl()).addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(final FocusEvent e) {
                currentDisplayText = ((CCombo) e.getSource()).getText();
            }
        });

        ((CCombo) deleteItemCellEditor.getControl()).addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
                if (DELETE_CUSTOM_TEXT.equals(((CCombo) e.getSource()).getText())) {
                    promptForCustomDeleteItems(currentDisplayText);
                } else {
                    currentDisplayText = ((CCombo) e.getSource()).getText();
                    final IRetentionPolicy policy = getSelectedRetentionPolicy();
                    policy.setDeleteOptions(getDeleteOptions(currentDisplayText));
                    getViewer().update(policy, null);
                }
            }
        });

        setCellEditor(KEEP_COL, keepCountCellEditor);
        setCellEditor(DELETE_COL, deleteItemCellEditor);
        getViewer().setCellModifier(cellModifier);

        getViewer().getTable().setLayout(new EqualSizeTableLayout());

        if (SWT.getVersion() >= 3200) {
            // The custom paint methods used here only available in Eclipse 3.2
            // and up.
            addImages();
        }
    }

    private void addImages() {
        final Image image = imageHelper.getStatusImage(BuildStatus.SUCCEEDED);

        // Add custom image paint logic to table to allow for our "indent"
        // effect along with icons
        final Listener paintListener = new Listener() {
            @Override
            public void handleEvent(final Event event) {
                switch (event.type) {
                    case SWT.MeasureItem: {
                        final Rectangle rect = image.getBounds();
                        event.height = Math.max(event.height, rect.height + 6);
                        break;
                    }
                    case SWT.PaintItem: {
                        final IRetentionPolicy policy = (IRetentionPolicy) ((TableItem) event.item).getData();
                        if (policy.getBuildStatus() != null && event.x < 16) {
                            final int x = event.x + 5;
                            final Rectangle rect = image.getBounds();
                            final int offset = Math.max(0, (event.height - rect.height) / 2);
                            event.gc.drawImage(
                                imageHelper.getStatusImage(policy.getBuildStatus()),
                                x,
                                event.y + offset);
                        }
                        break;
                    }
                }
            }
        };
        getViewer().getTable().addListener(SWT.MeasureItem, paintListener);
        getViewer().getTable().addListener(SWT.PaintItem, paintListener);
    }

    protected void promptForCustomKeepCount() {
        final IInputValidator validator = new IInputValidator() {

            @Override
            public String isValid(final String newText) {
                int newValue = -1;
                try {
                    newValue = Integer.parseInt(newText);
                } catch (final NumberFormatException e) {
                    // Ignore - we will throw an error soon.
                }

                if (newValue < 0) {
                    final String messageFormat =
                        Messages.getString("RetentionPolicyTableControl.ErrorCountNotInRangeFormat"); //$NON-NLS-1$
                    final String message = MessageFormat.format(messageFormat, Integer.MAX_VALUE);
                    return message;
                }

                return null;
            }
        };

        final InputDialog dialog =
            new InputDialog(
                getShell(),
                Messages.getString("RetentionPolicyTableControl.ConfigureDialogTitle"), //$NON-NLS-1$
                Messages.getString("RetentionPolicyTableControl.ConfigureDialogPrompt"), //$NON-NLS-1$
                "12", //$NON-NLS-1$
                validator);

        if (dialog.open() == IDialogConstants.OK_ID) {
            final IRetentionPolicy policy = getSelectedRetentionPolicy();
            policy.setNumberToKeep(Integer.parseInt(dialog.getValue()));
            getViewer().update(policy, null);
        }

        keepCountCellEditor.deactivate();
    }

    protected void promptForCustomDeleteItems(final String text) {
        final IRetentionPolicy policy = getSelectedRetentionPolicy();
        final RetentionPolicyDeleteDialog dialog = new RetentionPolicyDeleteDialog(
            getShell(),
            buildServer.getDisplayText(policy.getBuildReason()),
            buildServer.getDisplayText(policy.getBuildStatus()),
            getDeleteOptions(text));
        if (dialog.open() == IDialogConstants.OK_ID) {
            policy.setDeleteOptions(dialog.getDeleteOption());
            getViewer().update(policy, null);
        }
        deleteItemCellEditor.deactivate();
    }

    public class CellModifier implements ICellModifier {
        private String[] items;
        private int[] values;
        private String[] deleteItems;
        private DeleteOptions[] deleteValues;
        private final TableViewer viewer;

        public CellModifier(final TableViewer viewer, final String[] items, final String[] deleteItems) {
            this.viewer = viewer;
            this.items = items;
            this.deleteItems = deleteItems;
        }

        @Override
        public boolean canModify(final Object element, final String property) {
            if (KEEP_COL.equals(property)) {
                // We are about to modify the retention policy keep column.
                // If we have a custom count then this should be available
                // in the drop-down, otherwise we should just have the default
                // descriptions.

                IRetentionPolicy policy;
                if (element instanceof Item) {
                    policy = (IRetentionPolicy) ((Item) element).getData();
                } else {
                    policy = (IRetentionPolicy) element;
                }

                if (policy.getBuildStatus() == null) {
                    // This is a dummy header policy - do not edit
                    return false;
                }

                final String selectedDescription = getRetentionPolicyText(policy.getNumberToKeep());

                // Look to see if is one of the standard ones.
                boolean isOneOfStandardValues = false;
                for (int i = 0; i < RETENTION_POLICY_STANDARD_TEXTS.length; i++) {
                    if (RETENTION_POLICY_STANDARD_TEXTS[i].equals(selectedDescription)) {
                        isOneOfStandardValues = true;
                    }
                }

                String[] keepDescriptions;
                int[] keepValues;

                if (isOneOfStandardValues) {
                    keepDescriptions = RETENTION_POLICY_STANDARD_TEXTS;
                    keepValues = RETENTION_POLICY_STANDARD_VALUES;
                } else {
                    // Add the one we have to the defaults.
                    keepDescriptions = new String[RETENTION_POLICY_STANDARD_TEXTS.length + 1];
                    keepDescriptions[0] = selectedDescription;
                    System.arraycopy(
                        RETENTION_POLICY_STANDARD_TEXTS,
                        0,
                        keepDescriptions,
                        1,
                        RETENTION_POLICY_STANDARD_TEXTS.length);
                    keepValues = new int[RETENTION_POLICY_STANDARD_VALUES.length + 1];
                    keepValues[0] = policy.getNumberToKeep();
                    System.arraycopy(
                        RETENTION_POLICY_STANDARD_VALUES,
                        0,
                        keepValues,
                        1,
                        RETENTION_POLICY_STANDARD_VALUES.length);
                }

                keepCountCellEditor.setItems(keepDescriptions);
                setItems(keepDescriptions);
                setValues(keepValues);

                return true;
            } else if (DELETE_COL.equals(property)) {
                if (buildServer.getBuildServerVersion().isLessThanV3()) {
                    return false;
                }

                IRetentionPolicy policy;
                if (element instanceof Item) {
                    policy = (IRetentionPolicy) ((Item) element).getData();
                } else {
                    policy = (IRetentionPolicy) element;
                }

                if (policy.getBuildStatus() == null) {
                    return false;
                }

                final String selectedDescription = getWhatToDeleteText(policy.getDeleteOptions());

                // Look to see if is one of the standard ones.
                boolean isOneOfStandardValues = false;
                for (int i = 0; i < DELETE_ITEMS_STANDARD_TEXTS.length; i++) {
                    if (DELETE_ITEMS_STANDARD_TEXTS[i].equals(selectedDescription)) {
                        isOneOfStandardValues = true;
                    }
                }

                String[] deleteDescriptions;
                DeleteOptions[] deleteValues;

                if (isOneOfStandardValues) {
                    deleteDescriptions = DELETE_ITEMS_STANDARD_TEXTS;
                    deleteValues = DELETE_ITEMS_STANDARD_VALUES;
                } else {
                    // Add the one we have to the defaults.
                    deleteDescriptions = new String[DELETE_ITEMS_STANDARD_TEXTS.length + 1];
                    deleteDescriptions[0] = selectedDescription;
                    System.arraycopy(
                        DELETE_ITEMS_STANDARD_TEXTS,
                        0,
                        deleteDescriptions,
                        1,
                        DELETE_ITEMS_STANDARD_TEXTS.length);
                    deleteValues = new DeleteOptions[DELETE_ITEMS_STANDARD_VALUES.length + 1];
                    deleteValues[0] = policy.getDeleteOptions();
                    System.arraycopy(
                        DELETE_ITEMS_STANDARD_VALUES,
                        0,
                        deleteValues,
                        1,
                        DELETE_ITEMS_STANDARD_VALUES.length);
                }

                deleteItemCellEditor.setItems(deleteDescriptions);
                setDeleteItems(deleteDescriptions);
                setDeleteOptions(deleteValues);
                return true;
            }

            return false;
        }

        @Override
        public Object getValue(final Object element, final String property) {
            if (KEEP_COL.equals(property)) {
                final int keepCount = ((IRetentionPolicy) element).getNumberToKeep();
                final String keepText = getRetentionPolicyText(keepCount);
                for (int i = 0; i < items.length; i++) {
                    if (items[i].equals(keepText)) {
                        return new Integer(i);
                    }
                }
            } else if (DELETE_COL.equals(property)) {
                final DeleteOptions option = ((IRetentionPolicy) element).getDeleteOptions();
                final String text = getWhatToDeleteText(option);
                for (int i = 0; i < deleteItems.length; i++) {
                    if (deleteItems[i].equals(text)) {
                        return new Integer(i);
                    }
                }
            }
            return null;
        }

        @Override
        public void modify(final Object element, final String property, final Object value) {
            if (KEEP_COL.equals(property) || DELETE_COL.equals(property)) {
                IRetentionPolicy policy;
                if (element instanceof Item) {
                    policy = (IRetentionPolicy) ((Item) element).getData();
                } else {
                    policy = (IRetentionPolicy) element;
                }

                final int selectedIndex = ((Integer) value).intValue();

                if (KEEP_COL.equals(property)) {
                    if (selectedIndex < 0 || values[selectedIndex] < 0) {
                        return;
                    }
                    policy.setNumberToKeep(values[selectedIndex]);
                } else {
                    if (selectedIndex < 0 || deleteValues[selectedIndex] == null) {
                        return;
                    }
                    policy.setDeleteOptions((deleteValues[selectedIndex]));
                }
                viewer.update(policy, null);
            }
        }

        public void setItems(final String[] items) {
            this.items = items;
        }

        public void setValues(final int[] values) {
            this.values = values;
        }

        private void setDeleteItems(final String[] deleteDescriptions) {
            this.deleteItems = deleteDescriptions;
        }

        private void setDeleteOptions(final DeleteOptions[] deleteValues) {
            this.deleteValues = deleteValues;
        }
    }

    private ViewerSorter createSorter() {
        final TableViewerSorter sorter = new TableViewerSorter(getViewer());

        sorter.setComparator(0, new RPC());

        sorter.sort(0, SortDirection.ASCENDING);
        return sorter;
    }

    @Override
    protected Image getColumnImage(final Object element, final int columnIndex) {
        if (columnIndex == 0) {
            final BuildStatus status = ((IRetentionPolicy) element).getBuildStatus();
            if (status != null) {
                // return imageHelper.getStatusImageIdented(status);
            }
        }
        return null;
    }

    @Override
    protected String getColumnText(final Object element, final int columnIndex) {
        final IRetentionPolicy retentionPolicy = (IRetentionPolicy) element;

        final BuildStatus status = retentionPolicy.getBuildStatus();
        switch (columnIndex) {
            case 0:
                if (status == null) {
                    return buildServer.getDisplayText(retentionPolicy.getBuildReason());
                }
                return "          " + buildServer.getDisplayText(status); //$NON-NLS-1$

            case 1:
                if (status == null) {
                    return ""; //$NON-NLS-1$
                }
                return getRetentionPolicyText(retentionPolicy.getNumberToKeep());

            case 2:
                if (status == null) {
                    return ""; //$NON-NLS-1$
                }
                return getWhatToDeleteText(retentionPolicy.getDeleteOptions());
            default:
                return ""; //$NON-NLS-1$
        }
    }

    public String getRetentionPolicyText(final int numberToKeep) {
        if (numberToKeep == 0) {
            return KEEP_NONE_TEXT;
        }
        if (numberToKeep == 1) {
            return KEEP_LATEST_TEXT;
        }
        if (numberToKeep == Integer.MAX_VALUE) {
            return KEEP_ALL_TEXT;
        }

        final String messageFormat = Messages.getString("RetentionPolicyTableControl.KeepSpecifiedLatestFormat"); //$NON-NLS-1$
        final String message = MessageFormat.format(messageFormat, numberToKeep);
        return message;
    }

    private String getWhatToDeleteText(final DeleteOptions deleteOptions) {
        return buildServer.getDisplayText(deleteOptions);
    }

    public void setRetentionPolicies(final IRetentionPolicy[] retentionPolicies) {
        final List<IRetentionPolicy> policies = new ArrayList<IRetentionPolicy>();

        // Add Dummy retention policies for summary Levels
        Arrays.sort(retentionPolicies, new RPC());
        BuildReason lastBuildReason = BuildReason.NONE;

        for (int i = 0; i < retentionPolicies.length; i++) {
            if (!lastBuildReason.equals(retentionPolicies[i].getBuildReason())) {
                // Not seen this build reason before - add a dummy retention
                // policy so that we display the header.
                lastBuildReason = retentionPolicies[i].getBuildReason();
                policies.add(new DRP(lastBuildReason));
            }
            policies.add(retentionPolicies[i]);
        }

        setElements(policies.toArray(new IRetentionPolicy[policies.size()]));
        getViewer().getTable().getColumn(0).pack();
    }

    private DeleteOptions getDeleteOptions(final String displayText) {
        DeleteOptions deleteOption = DeleteOptions.NONE;
        if (displayText.equals(Messages.getString("RetentionPolicyTableControl.DeleteAllText"))) //$NON-NLS-1$
        {
            return DeleteOptions.ALL;
        } else {
            final String[] options = displayText.split(","); //$NON-NLS-1$
            for (final String option : options) {
                final String text = option.trim();
                if (text.equals(Messages.getString("RetentionPolicyTableControl.DetailsText"))) //$NON-NLS-1$
                {
                    deleteOption = deleteOption.combine(DeleteOptions.DETAILS);
                }
                if (text.equals(Messages.getString("RetentionPolicyTableControl.DropText"))) //$NON-NLS-1$
                {
                    deleteOption = deleteOption.combine(DeleteOptions.DROP_LOCATION);
                }
                if (text.equals(Messages.getString("RetentionPolicyTableControl.LabelText"))) //$NON-NLS-1$
                {
                    deleteOption = deleteOption.combine(DeleteOptions.LABEL);
                }
                if (text.equals(Messages.getString("RetentionPolicyTableControl.SymbolsText"))) //$NON-NLS-1$
                {
                    deleteOption = deleteOption.combine(DeleteOptions.SYMBOLS);
                }
                if (text.equals(Messages.getString("RetentionPolicyTableControl.TestResultsText"))) //$NON-NLS-1$
                {
                    deleteOption = deleteOption.combine(DeleteOptions.TEST_RESULTS);
                }
            }
            return deleteOption;
        }
    }

    public IRetentionPolicy[] getRetentionPolicies() {
        final List<IRetentionPolicy> policies = new ArrayList<IRetentionPolicy>();
        final IRetentionPolicy[] policyArray = (IRetentionPolicy[]) getElements();
        for (int i = 0; i < policyArray.length; i++) {
            if (policyArray[i].getBuildStatus() != null) {
                policies.add(policyArray[i]);
            }
        }

        return policies.toArray(new IRetentionPolicy[policies.size()]);
    }

    public void setSelectedRetentionPolicies(final IRetentionPolicy[] retentionPolicies) {
        setSelectedElements(retentionPolicies);
    }

    public void setSelectedRetentionPolicies(final IRetentionPolicy retentionPolicy) {
        setSelectedElement(retentionPolicy);
    }

    public IRetentionPolicy[] getSelectedRetentionPolicies() {
        return (IRetentionPolicy[]) getSelectedElements();
    }

    public IRetentionPolicy getSelectedRetentionPolicy() {
        return (IRetentionPolicy) getSelectedElement();
    }

    /**
     * Should be called RetentionPolicyComparator but that would be too long to
     * build on Windows.
     */
    private class RPC implements Comparator<IRetentionPolicy> {
        @Override
        public int compare(final IRetentionPolicy o1, final IRetentionPolicy o2) {
            final int statusPos1 = getSortPos(o1);
            final int statusPos2 = getSortPos(o2);

            return statusPos1 - statusPos2;
        }

        private int getSortPos(final IRetentionPolicy retentionPolicy) {
            int sortPos = 0;

            final BuildReason reason = retentionPolicy.getBuildReason();
            if (reason.contains(BuildReason.MANUAL)) {
                sortPos = 10;
            } else if (reason.contains(BuildReason.ALL)) {
                sortPos = 100;
            } else if (reason.contains(BuildReason.INDIVIDUAL_CI)) {
                sortPos = 20;
            } else if (reason.contains(BuildReason.BATCHED_CI)) {
                sortPos = 30;
            } else if (reason.contains(BuildReason.SCHEDULE)) {
                sortPos = 40;
            } else if (reason.contains(BuildReason.SCHEDULE_FORCED)) {
                sortPos = 50;
            } else if (reason.contains(BuildReason.USER_CREATED)) {
                sortPos = 60;
            } else if (reason.contains(BuildReason.VALIDATE_SHELVESET)) {
                sortPos = 70;
            } else if (reason.contains(BuildReason.CHECK_IN_SHELVESET)) {
                sortPos = 80;
            } else if (reason.contains(BuildReason.TRIGGERED)) {
                sortPos = 90;
            } else if (reason.contains(BuildReason.ALL)) {
                sortPos = 100;
            }

            final BuildStatus status = retentionPolicy.getBuildStatus();

            // Show in the order VS client does.
            if (status != null) {
                if (status.contains(BuildStatus.STOPPED)) {
                    return sortPos + 1;
                }
                if (status.contains(BuildStatus.FAILED)) {
                    return sortPos + 2;
                }
                if (status.contains(BuildStatus.PARTIALLY_SUCCEEDED)) {
                    return sortPos + 3;
                }
                if (status.contains(BuildStatus.SUCCEEDED)) {
                    return sortPos + 4;
                }
            }

            return sortPos;
        }
    }

    /**
     * Should be called DummyRetentionPolicy but that would be too long to build
     * on Windows.
     */
    private class DRP implements IRetentionPolicy {
        private final BuildReason buildReason;

        public DRP(final BuildReason buildReason) {
            this.buildReason = buildReason;
        }

        @Override
        public IBuildDefinition getBuildDefinition() {
            return null;
        }

        @Override
        public BuildReason getBuildReason() {
            return buildReason;
        }

        @Override
        public BuildStatus getBuildStatus() {
            return null;
        }

        @Override
        public DeleteOptions getDeleteOptions() {
            return null;
        }

        @Override
        public int getNumberToKeep() {
            return 0;
        }

        @Override
        public void setBuildReason(final BuildReason reason) {
        }

        @Override
        public void setDeleteOptions(final DeleteOptions options) {
        }

        @Override
        public void setNumberToKeep(final int builds) {
        }

        @Override
        public void setBuildStatus(final BuildStatus value) {
        }

    }
}
