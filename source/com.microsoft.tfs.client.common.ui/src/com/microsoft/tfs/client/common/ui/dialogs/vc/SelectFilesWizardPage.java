// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.dialogs.vc;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableLayout;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.Text;

import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.controls.generic.SizeConstrainedComposite;
import com.microsoft.tfs.client.common.ui.framework.helper.GenericElementsContentProvider;
import com.microsoft.tfs.client.common.ui.framework.helper.MultiGlobMatcher;
import com.microsoft.tfs.client.common.ui.framework.helper.SWTUtil;
import com.microsoft.tfs.client.common.ui.framework.layout.GridDataBuilder;
import com.microsoft.tfs.client.common.ui.framework.sizing.ControlSize;
import com.microsoft.tfs.client.common.ui.framework.wizard.ExtendedWizardPage;
import com.microsoft.tfs.client.common.ui.helpers.AutomationIDHelper;
import com.microsoft.tfs.core.clients.versioncontrol.path.LocalPath;

public class SelectFilesWizardPage extends ExtendedWizardPage {
    public static final String PAGE_NAME = "SelectFilesWizardPage"; //$NON-NLS-1$

    private CheckboxTableViewer tableViewer;

    private String excludes;
    private String mappedLocalPath;
    private final Set<File> files = new HashSet<File>();
    private final Set<File> unfilteredFiles = new HashSet<File>();

    public static final String EXCLUDE_TEXT_ID = "SelectFilesWizardPage.excludeText"; //$NON-NLS-1$

    private Label copyNeededLabel;
    private Text serverPathText;

    public SelectFilesWizardPage(final String title) {
        super(PAGE_NAME, title, Messages.getString("SelectFilesWizardPage.SelectFilesDescriptionText")); //$NON-NLS-1$
    }

    @Override
    public void doCreateControl(final Composite parent, final IDialogSettings dialogSettings) {
        final SizeConstrainedComposite container = new SizeConstrainedComposite(parent, SWT.NONE);
        container.setDefaultSize(SWT.DEFAULT, SWT.DEFAULT);
        setControl(container);

        final GridLayout layout = new GridLayout(2, false);
        layout.marginWidth = getHorizontalMargin();
        layout.marginHeight = getVerticalMargin();
        layout.horizontalSpacing = getHorizontalSpacing();
        layout.verticalSpacing = getVerticalSpacing() - 2;
        container.setLayout(layout);

        setControl(container);

        copyNeededLabel = new Label(container, SWT.WRAP);
        GridDataBuilder.newInstance().hSpan(2).hFill().hGrab().wHint(
            IDialogConstants.MINIMUM_MESSAGE_AREA_WIDTH).applyTo(copyNeededLabel);

        final Label addSelectedFilesLabel = new Label(container, SWT.WRAP);
        addSelectedFilesLabel.setText(Messages.getString("SelectFilesWizardPage.ExcludeItemsLabelText")); //$NON-NLS-1$
        GridDataBuilder.newInstance().hSpan(2).hFill().hGrab().wHint(
            IDialogConstants.MINIMUM_MESSAGE_AREA_WIDTH).applyTo(addSelectedFilesLabel);

        final Table table = new Table(container, SWT.BORDER | SWT.CHECK | SWT.FULL_SELECTION | SWT.MULTI);
        final TableLayout tableLayout = new TableLayout();
        table.setLayout(tableLayout);
        GridDataBuilder.newInstance().hFill().hHint(200).hGrab().hSpan(2).applyTo(table);

        tableLayout.addColumnData(new ColumnWeightData(40, 20, true));
        final TableColumn nameTableColumn = new TableColumn(table, SWT.NONE);
        nameTableColumn.setText(Messages.getString("SelectFilesWizardPage.ColumnNameName")); //$NON-NLS-1$

        tableLayout.addColumnData(new ColumnWeightData(60, 30, true));
        final TableColumn folderTableColumn = new TableColumn(table, SWT.NONE);
        folderTableColumn.setText(Messages.getString("SelectFilesWizardPage.ColumnNameFolder")); //$NON-NLS-1$

        table.setLinesVisible(true);
        table.setHeaderVisible(true);

        tableViewer = new CheckboxTableViewer(table);
        tableViewer.setContentProvider(new GenericElementsContentProvider());
        tableViewer.setLabelProvider(new AddFilesDialogLabelProvider());
        tableViewer.setSorter(new ViewerSorter());
        tableViewer.setInput(files);

        // If files are pre-populated, then select all items
        if (files.size() > 0) {
            tableViewer.setCheckedElements(files.toArray());
        }

        tableViewer.addSelectionChangedListener(new ISelectionChangedListener() {
            @Override
            public void selectionChanged(final SelectionChangedEvent event) {
                toggleFinishState();
            }
        });

        ControlSize.setCharSizeHints(table, 15, 10);

        final Label excludeLabel = new Label(container, SWT.NONE);
        excludeLabel.setText(Messages.getString("SelectFilesWizardPage.ExcludeLabelText")); //$NON-NLS-1$
        GridDataBuilder.newInstance().applyTo(excludeLabel);

        final Text excludeText = new Text(container, SWT.BORDER);
        AutomationIDHelper.setWidgetID(excludeText, EXCLUDE_TEXT_ID);
        GridDataBuilder.newInstance().hFill().applyTo(excludeText);

        SWTUtil.createGridLayoutSpacer(container);

        final Label exampleLabel = new Label(container, SWT.NONE);
        exampleLabel.setText(Messages.getString("SelectFilesWizardPage.ExampleLabelText")); //$NON-NLS-1$
        GridDataBuilder.newInstance().applyTo(exampleLabel);

        excludeText.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(final ModifyEvent e) {
                excludes = excludeText.getText();
                updateTable();
            }
        });

        final Label label = new Label(container, SWT.WRAP);
        label.setText(Messages.getString("SelectFilesWizardPage.DestinationServerPathText")); //$NON-NLS-1$
        GridDataBuilder.newInstance().applyTo(label);

        serverPathText = new Text(container, SWT.BORDER);
        serverPathText.setEditable(false);
        GridDataBuilder.newInstance().hFill().hGrab().applyTo(serverPathText);
    }

    @Override
    public void refresh() {
        unfilteredFiles.clear();
        files.clear();
        serverPathText.setText((String) getExtendedWizard().getPageData(AddFilesWizard.SERVER_PATH));
        mappedLocalPath = (String) getExtendedWizard().getPageData(AddFilesWizard.LOCAL_PATH);
        final File[] initialFiles = (File[]) getExtendedWizard().getPageData(AddFilesWizard.SELECTED_FILES);
        final String uploadPath = (String) getExtendedWizard().getPageData(AddFilesWizard.UPLOAD_PATH);
        checkCopyNeeded(mappedLocalPath, uploadPath);
        processFiles(initialFiles);
        updateTable();
    }

    @Override
    protected boolean onPageFinished() {
        final Object[] checkedItems = tableViewer.getCheckedElements();
        final File[] checkedFiles = new File[checkedItems.length];

        for (int i = 0; i < checkedItems.length; i++) {
            checkedFiles[i] = (File) checkedItems[i];
        }

        getExtendedWizard().setPageData(AddFilesWizard.SELECTED_FILES, checkedFiles);

        return true;
    }

    public String getExcludes() {
        return excludes;
    }

    public void setExcludes(final String excludes) {
        this.excludes = excludes;
    }

    private void checkCopyNeeded(final String localPath, final String uploadPath) {
        if (!LocalPath.equals(localPath, uploadPath)) {
            copyNeededLabel.setText(Messages.getString("SelectFilesWizardPage.CopyNeededLabel")); //$NON-NLS-1$
        }
    }

    private void processFiles(final File[] potentialFiles) {
        for (int i = 0; i < potentialFiles.length; i++) {
            final File current = potentialFiles[i];
            unfilteredFiles.add(current);
            files.add(current);
        }
        tableViewer.setInput(files);
        tableViewer.setCheckedElements(files.toArray());
    }

    private void toggleFinishState() {
        final Object[] checkedElements = tableViewer.getCheckedElements();
        if (checkedElements != null && checkedElements.length > 0) {
            setPageComplete(true);
        } else {
            setPageComplete(false);
        }
    }

    private void updateTable() {
        /* create a matcher to use if the excludes text has been specified */
        final MultiGlobMatcher matcher = MultiGlobMatcher.fromMultiPattern(excludes, ";"); //$NON-NLS-1$
        final List<File> addedFiles = new ArrayList<File>();

        if (matcher == null) {
            for (final File f : unfilteredFiles) {
                if (!files.contains(f)) {
                    files.add(f);
                    addedFiles.add(f);
                }
            }
        } else {
            for (final File f : unfilteredFiles) {
                if (!matcher.matches(f.getName())) {
                    if (!files.contains(f)) {
                        files.add(f);
                        addedFiles.add(f);
                    }
                } else {
                    files.remove(f);
                }
            }
        }

        final Object[] currentlyChecked = tableViewer.getCheckedElements();
        final List newChecked = new ArrayList(addedFiles);
        newChecked.addAll(Arrays.asList(currentlyChecked));

        tableViewer.setInput(files);
        tableViewer.setCheckedElements(newChecked.toArray());
        tableViewer.refresh();
        toggleFinishState();
    }

    private class AddFilesDialogLabelProvider extends LabelProvider implements ITableLabelProvider {
        public AddFilesDialogLabelProvider() {
        }

        @Override
        public Image getColumnImage(final Object element, final int columnIndex) {
            return null;
        }

        @Override
        public String getColumnText(final Object element, final int columnIndex) {
            final File file = (File) element;
            if (columnIndex == 0) // name
            {
                return file.getName();
            } else if (columnIndex == 1) // folder
            {
                return file.getParent();
            }
            return null;
        }

        // this method supports sorting
        @Override
        public String getText(final Object element) {
            return ((File) element).getName();
        }
    }

}
