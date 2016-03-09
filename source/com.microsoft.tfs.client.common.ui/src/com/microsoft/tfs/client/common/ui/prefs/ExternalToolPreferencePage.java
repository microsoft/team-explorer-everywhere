// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.prefs;

import java.text.MessageFormat;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableLayout;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.ui.IWorkbench;

import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.framework.helper.ButtonHelper;
import com.microsoft.tfs.client.common.ui.framework.helper.FormHelper;
import com.microsoft.tfs.client.common.ui.framework.helper.TableViewerSorter;
import com.microsoft.tfs.client.common.ui.helpers.AutomationIDHelper;
import com.microsoft.tfs.core.externaltools.ExternalTool;
import com.microsoft.tfs.core.externaltools.ExternalToolAssociation;
import com.microsoft.tfs.core.externaltools.ExternalToolset;

public abstract class ExternalToolPreferencePage extends BasePreferencePage {
    public static final String TOOL_TABLE_ID = "ExternalToolPreferencePage.toolTable"; //$NON-NLS-1$

    public static final String DUPLICATE_BUTTON_ID = "ExternalToolPreferencePage.toolDuplicateButton"; //$NON-NLS-1$
    public static final String REMOVE_BUTTON_ID = "ExternalToolPreferencePage.toolRemoveButton"; //$NON-NLS-1$
    public static final String EDIT_BUTTON_ID = "ExternalToolPreferencePage.toolEditButton"; //$NON-NLS-1$
    public static final String ADD_BUTTON_ID = "ExternalToolPreferencePage.toolAddButton"; //$NON-NLS-1$

    public static final String DIR_REMOVE_BUTTON_ID = "ExternalToolPreferencePage.dirToolRemoveButton"; //$NON-NLS-1$
    public static final String DIR_EDIT_BUTTON_ID = "ExternalToolPreferencePage.dirToolEditButton"; //$NON-NLS-1$

    private class ExternalToolLabelProvider extends LabelProvider implements ITableLabelProvider {
        public ExternalToolLabelProvider() {
        }

        @Override
        public Image getColumnImage(final Object element, final int columnIndex) {
            return null;
        }

        @Override
        public String getColumnText(final Object element, final int columnIndex) {
            if (!(element instanceof ExternalToolAssociation)) {
                return Messages.getString("ExternalToolPreferencePage.UnknownColumnText"); //$NON-NLS-1$
            }

            switch (columnIndex) {
                case 0:
                    final String[] extensions = ((ExternalToolAssociation) element).getExtensions();

                    final StringBuffer sb = new StringBuffer();
                    for (int i = 0; i < extensions.length; i++) {
                        if (i > 0) {
                            sb.append(", "); //$NON-NLS-1$
                        }

                        sb.append(extensions[i]);
                    }

                    return sb.toString();
                case 1:
                    final ExternalTool tool = ((ExternalToolAssociation) element).getTool();
                    return tool.getOriginalCommandAndArguments();
            }

            return Messages.getString("ExternalToolPreferencePage.UnknownColumnText"); //$NON-NLS-1$
        }
    }

    private class ExternalContentProvider implements IStructuredContentProvider {
        /**
         * Returns {@link ExternalToolAssociation} objects.
         *
         * {@inheritDoc}
         */
        @Override
        public Object[] getElements(final Object inputElement) {
            return getToolset().getFileAssociations();
        }

        @Override
        public void dispose() {
        }

        @Override
        public void inputChanged(final Viewer viewer, final Object oldInput, final Object newInput) {
        }
    }

    private Table toolTable;
    private TableViewer toolTableViewer;
    private Button toolAddButton;
    private Button toolEditButton;
    private Button toolRemoveButton;
    private Button toolDuplicateButton;
    private Label dirToolSeparator;
    private Label dirToolLabel;
    private Button dirToolRemoveButton;
    private Button dirToolEditButton;

    public ExternalToolPreferencePage() {
        super();
    }

    public ExternalToolPreferencePage(final String title) {
        super(title);
    }

    public ExternalToolPreferencePage(final String title, final ImageDescriptor image) {
        super(title, image);
    }

    abstract protected String getName();

    /*
     * (non-Javadoc)
     *
     * @see
     * org.eclipse.ui.IWorkbenchPreferencePage#init(org.eclipse.ui.IWorkbench)
     */
    @Override
    public void init(final IWorkbench workbench) {
        noDefaultAndApplyButton();
    }

    /**
     * @return the toolset that this preference page is configuring.
     */
    abstract protected ExternalToolset getToolset();

    @Override
    protected Control createContents(final Composite parent) {
        final Composite container = new Composite(parent, SWT.NONE);

        final FormLayout containerLayout = new FormLayout();
        containerLayout.spacing = 10;
        container.setLayout(containerLayout);

        final Group toolGroup = new Group(container, SWT.NONE);

        final String messageFormat = Messages.getString("ExternalToolPreferencePage.ToolGroupTextFormat"); //$NON-NLS-1$
        final String message = MessageFormat.format(messageFormat, getName());
        toolGroup.setText(message);

        final FormLayout toolGroupLayout = new FormLayout();
        toolGroupLayout.marginHeight = 5;
        toolGroupLayout.marginWidth = 5;
        toolGroupLayout.spacing = 5;
        toolGroup.setLayout(toolGroupLayout);

        final FormData mergeGroupData = new FormData();
        mergeGroupData.top = new FormAttachment(0, 0);
        mergeGroupData.left = new FormAttachment(0, 0);
        mergeGroupData.right = new FormAttachment(100, 0);
        mergeGroupData.bottom = new FormAttachment(100, 0);
        toolGroup.setLayoutData(mergeGroupData);

        toolTable = new Table(toolGroup, SWT.FULL_SELECTION);
        AutomationIDHelper.setWidgetID(toolTable, TOOL_TABLE_ID);

        toolTableViewer = new TableViewer(toolTable);

        toolAddButton = new Button(toolGroup, SWT.NONE);
        AutomationIDHelper.setWidgetID(toolAddButton, ADD_BUTTON_ID);

        toolEditButton = new Button(toolGroup, SWT.NONE);
        AutomationIDHelper.setWidgetID(toolEditButton, EDIT_BUTTON_ID);

        toolRemoveButton = new Button(toolGroup, SWT.NONE);
        AutomationIDHelper.setWidgetID(toolRemoveButton, REMOVE_BUTTON_ID);

        toolDuplicateButton = new Button(toolGroup, SWT.NONE);
        AutomationIDHelper.setWidgetID(toolDuplicateButton, DUPLICATE_BUTTON_ID);

        dirToolSeparator = new Label(toolGroup, SWT.SEPARATOR | SWT.HORIZONTAL);
        dirToolLabel = new Label(toolGroup, SWT.NONE);

        dirToolRemoveButton = new Button(toolGroup, SWT.NONE);
        AutomationIDHelper.setWidgetID(dirToolRemoveButton, DIR_REMOVE_BUTTON_ID);

        dirToolEditButton = new Button(toolGroup, SWT.NONE);
        AutomationIDHelper.setWidgetID(dirToolEditButton, DIR_EDIT_BUTTON_ID);

        // external compare checkbox
        final FormData toolTableData = new FormData();
        toolTableData.top = new FormAttachment(0, 0);
        toolTableData.left = new FormAttachment(0, 0);
        toolTableData.right = new FormAttachment(100, 0);
        toolTableData.bottom = new FormAttachment(toolAddButton, 0, SWT.TOP);
        toolTable.setLayoutData(toolTableData);
        toolTable.setLinesVisible(true);
        toolTable.setHeaderVisible(true);

        final TableLayout mergeTableLayout = new TableLayout();
        toolTable.setLayout(mergeTableLayout);

        mergeTableLayout.addColumnData(new ColumnWeightData(20, 40, true));
        final TableColumn typeColumn = new TableColumn(toolTable, SWT.NONE);
        typeColumn.setText(Messages.getString("ExternalToolPreferencePage.ColumnNameType")); //$NON-NLS-1$

        mergeTableLayout.addColumnData(new ColumnWeightData(80, 160, true));
        final TableColumn commandColumn = new TableColumn(toolTable, SWT.NONE);
        commandColumn.setText(Messages.getString("ExternalToolPreferencePage.ColumnNameCommand")); //$NON-NLS-1$

        toolTableViewer.setLabelProvider(new ExternalToolLabelProvider());
        toolTableViewer.setContentProvider(new ExternalContentProvider());
        toolTableViewer.setInput(new Object());

        final TableViewerSorter sorter = new TableViewerSorter(toolTableViewer);
        toolTableViewer.setSorter(sorter);
        sorter.sort(0);

        toolTableViewer.addSelectionChangedListener(new ISelectionChangedListener() {
            @Override
            public void selectionChanged(final SelectionChangedEvent event) {
                setEnabled();
            }
        });

        // external tool add
        final FormData toolAddData = new FormData();
        toolAddData.right = new FormAttachment(100, 0);
        toolAddData.bottom =
            supportsDirectories() ? new FormAttachment(dirToolSeparator, -1, SWT.TOP) : new FormAttachment(100, 0);
        toolAddButton.setLayoutData(toolAddData);
        toolAddButton.setText(Messages.getString("ExternalToolPreferencePage.AddButtonText")); //$NON-NLS-1$
        toolAddButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
                if (addPressed()) {
                    refresh();
                }
            }
        });

        // external tool edit
        final FormData toolEditData = new FormData();
        toolEditData.right = new FormAttachment(toolAddButton, 0, SWT.LEFT);
        toolEditData.bottom = new FormAttachment(toolAddButton, 0, SWT.BOTTOM);
        toolEditButton.setLayoutData(toolEditData);
        toolEditButton.setText(Messages.getString("ExternalToolPreferencePage.EditButtonText")); //$NON-NLS-1$
        toolEditButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
                if (editPressed()) {
                    refresh();
                }
            }
        });

        // external tool remove
        final FormData toolRemoveData = new FormData();
        toolRemoveData.right = new FormAttachment(toolEditButton, 0, SWT.LEFT);
        toolRemoveData.bottom = new FormAttachment(toolEditButton, 0, SWT.BOTTOM);
        toolRemoveButton.setLayoutData(toolRemoveData);
        toolRemoveButton.setText(Messages.getString("ExternalToolPreferencePage.RemoveButtonText")); //$NON-NLS-1$
        toolRemoveButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
                if (removePressed()) {
                    refresh();
                }
            }
        });

        // duplicate tool settings
        final FormData toolDuplicateData = new FormData();
        toolDuplicateData.right = new FormAttachment(toolRemoveButton, 0, SWT.LEFT);
        toolDuplicateData.bottom = new FormAttachment(toolRemoveButton, 0, SWT.BOTTOM);
        toolDuplicateButton.setLayoutData(toolDuplicateData);
        toolDuplicateButton.setText(Messages.getString("ExternalToolPreferencePage.DuplicateButtonText")); //$NON-NLS-1$
        toolDuplicateButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
                if (duplicatePressed()) {
                    refresh();
                }
            }
        });

        // external tools for directories
        if (supportsDirectories()) {
            final FormData dirToolSeparatorData = new FormData();
            dirToolSeparatorData.left = new FormAttachment(0, 0);
            dirToolSeparatorData.right = new FormAttachment(100, 0);
            dirToolSeparatorData.bottom = new FormAttachment(dirToolEditButton, -1, SWT.TOP);
            dirToolSeparator.setLayoutData(dirToolSeparatorData);

            final FormData dirToolData = new FormData();
            dirToolData.left = new FormAttachment(0, 0);
            dirToolData.right = new FormAttachment(dirToolRemoveButton, -10, SWT.LEFT);
            dirToolData.top = new FormAttachment(
                dirToolEditButton,
                FormHelper.VerticalOffset(dirToolLabel, dirToolEditButton),
                SWT.TOP);
            dirToolLabel.setLayoutData(dirToolData);
            dirToolLabel.setAlignment(SWT.RIGHT);

            // remove directory tool
            final FormData dirToolRemoveData = new FormData();
            dirToolRemoveData.right = new FormAttachment(dirToolEditButton, 0, SWT.LEFT);
            dirToolRemoveData.top = new FormAttachment(dirToolEditButton, 0, SWT.TOP);
            dirToolRemoveButton.setLayoutData(dirToolRemoveData);
            dirToolRemoveButton.setText(Messages.getString("ExternalToolPreferencePage.DirRemoveButtonText")); //$NON-NLS-1$
            dirToolRemoveButton.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(final SelectionEvent e) {
                    dirRemovePressed();
                    refresh();
                }
            });

            // change/add directory tool
            final FormData dirToolEditData = new FormData();
            dirToolEditData.right = new FormAttachment(100, 0);
            dirToolEditData.bottom = new FormAttachment(100, 0);
            dirToolEditButton.setLayoutData(dirToolEditData);
            dirToolEditButton.setText(Messages.getString("ExternalToolPreferencePage.DirChangeButtonText")); //$NON-NLS-1$
            dirToolEditButton.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(final SelectionEvent e) {
                    if (getToolset().findToolForDirectory() != null) {
                        dirChangePressed();
                    } else {
                        dirAddPressed();
                    }

                    refresh();
                }
            });
        }

        /* Resize button sizes */
        ButtonHelper.resizeButtons(new Button[] {
            toolAddButton,
            toolEditButton,
            toolRemoveButton,
            toolDuplicateButton,
            dirToolRemoveButton,
            dirToolEditButton
        });

        if (toolTable.getItemCount() > 0) {
            toolTable.setSelection(0);
        }

        setEnabled();

        refresh();

        return container;
    }

    protected ExternalToolAssociation getSelection() {
        final Object selection = ((IStructuredSelection) toolTableViewer.getSelection()).getFirstElement();

        // one (and only one) label may be selected, let's get it
        if (selection == null || !(selection instanceof ExternalToolAssociation)) {
            return null;
        }

        return (ExternalToolAssociation) selection;
    }

    protected boolean supportsDirectories() {
        return false;
    }

    abstract protected boolean addPressed();

    abstract protected boolean editPressed();

    abstract protected boolean removePressed();

    abstract protected boolean duplicatePressed();

    protected boolean dirAddPressed() {
        return false;
    }

    protected boolean dirChangePressed() {
        return false;
    }

    protected boolean dirRemovePressed() {
        return false;
    }

    private void refresh() {
        // retain selection
        final TableItem[] selection = toolTable.getSelection();

        getToolset().getFileAssociations();
        final ExternalToolAssociation directoryEntry = getToolset().getDirectoryAssociation();

        toolTableViewer.refresh();

        toolTable.setSelection(selection);

        setEnabled();

        // change directory label
        if (supportsDirectories()) {
            if (directoryEntry != null) {
                final String args = directoryEntry.getTool().getOriginalCommandAndArguments();
                final String messageFormat = Messages.getString("ExternalToolPreferencePage.DirToolLabelTextFormat"); //$NON-NLS-1$
                final String message = MessageFormat.format(messageFormat, args);

                dirToolLabel.setText(message);
                dirToolRemoveButton.setEnabled(true);
                dirToolEditButton.setText(Messages.getString("ExternalToolPreferencePage.DirEditButtonText")); //$NON-NLS-1$
            } else {
                dirToolLabel.setText(Messages.getString("ExternalToolPreferencePage.NotConfiguredDirToolLabelText")); //$NON-NLS-1$
                dirToolRemoveButton.setEnabled(false);
                dirToolEditButton.setText(Messages.getString("ExternalToolPreferencePage.DirAddButtonText")); //$NON-NLS-1$
            }
        }
    }

    private void setEnabled() {
        final boolean enable = toolTable.getSelectionCount() > 0;
        toolEditButton.setEnabled(enable);
        toolRemoveButton.setEnabled(enable);
        toolDuplicateButton.setEnabled(enable);
    }
}