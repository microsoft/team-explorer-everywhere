// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.eclipse.ui.wizard.importwizard;

import java.io.File;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkingSet;
import org.eclipse.ui.IWorkingSetManager;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.IWorkingSetSelectionDialog;

import com.microsoft.tfs.client.common.codemarker.CodeMarker;
import com.microsoft.tfs.client.common.codemarker.CodeMarkerDispatch;
import com.microsoft.tfs.client.common.ui.TFSCommonUIClientPlugin;
import com.microsoft.tfs.client.common.ui.controls.vc.ServerItemTreeControl;
import com.microsoft.tfs.client.common.ui.framework.helper.ButtonHelper;
import com.microsoft.tfs.client.common.ui.framework.helper.MessageBoxHelpers;
import com.microsoft.tfs.client.common.ui.framework.layout.GridDataBuilder;
import com.microsoft.tfs.client.common.ui.framework.sizing.ControlSize;
import com.microsoft.tfs.client.common.ui.framework.wizard.ExtendedWizardPage;
import com.microsoft.tfs.client.common.ui.helpers.AutomationIDHelper;
import com.microsoft.tfs.client.common.ui.helpers.WorkingSetHelper;
import com.microsoft.tfs.client.common.ui.vc.serveritem.ServerItemLabelProvider;
import com.microsoft.tfs.client.common.ui.vc.serveritem.ServerItemType;
import com.microsoft.tfs.client.common.ui.vc.serveritem.TypedServerItem;
import com.microsoft.tfs.client.common.ui.vc.serveritem.VersionedItemSource;
import com.microsoft.tfs.client.common.ui.wizard.connectwizard.ConnectWizard;
import com.microsoft.tfs.client.eclipse.ui.Messages;
import com.microsoft.tfs.client.eclipse.ui.wizard.importwizard.support.ImportFolderCollection;
import com.microsoft.tfs.client.eclipse.ui.wizard.importwizard.support.ImportFolderValidation;
import com.microsoft.tfs.client.eclipse.ui.wizard.importwizard.support.ImportFolderValidation.ImportFolderValidationFlag;
import com.microsoft.tfs.client.eclipse.ui.wizard.importwizard.support.ImportFolderValidation.ImportFolderValidationStatus;
import com.microsoft.tfs.client.eclipse.ui.wizard.importwizard.support.ImportItemCollectionBase;
import com.microsoft.tfs.client.eclipse.ui.wizard.importwizard.support.ImportOptions;
import com.microsoft.tfs.core.TFSTeamProjectCollection;
import com.microsoft.tfs.core.clients.commonstructure.ProjectInfo;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.StringUtil;

public class TfsImportWizardTreePage extends ExtendedWizardPage {
    public static final String PAGE_NAME = "TfsImportWizardTreePage"; //$NON-NLS-1$

    public static final CodeMarker CODEMARKER_REFRESH_COMPLETE =
        new CodeMarker("com.microsoft.tfs.client.eclipse.ui.wizard.importwizard.ImportWizardTreePage#refreshComplete"); //$NON-NLS-1$

    private static final Log log = LogFactory.getLog(TfsImportWizardTreePage.class);

    public static final String FORCE_BUTTON_ID = "ImportWizardTreePage.forceButton"; //$NON-NLS-1$

    private IWorkingSet[] workingSets;
    private String[] workingSetNames;

    private ServerItemTreeControl folderControl;
    private Label statusLabel;
    private Button forceButton;
    private Button newProjectButton;
    private Button workingSetButton;
    private Button workingSetSelectButton;
    private Combo workingSetCombo;

    private ImportFolderCollection itemCollection;

    ImportOptions options;
    TFSTeamProjectCollection connection;
    VersionedItemSource itemSource;

    public TfsImportWizardTreePage() {
        super(PAGE_NAME, null, null);
    }

    @Override
    public String getTitle() {
        return Messages.getString("ImportWizardTreePage.TfsPageTitle"); //$NON-NLS-1$
    }

    @Override
    public String getDescription() {
        return Messages.getString("ImportWizardTreePage.TfsPageDescription"); //$NON-NLS-1$
    }

    private ImportWizard getImportWizard() {
        return (ImportWizard) getExtendedWizard();
    }

    @Override
    protected void doCreateControl(final Composite parent, final IDialogSettings dialogSettings) {
        final ImportOptions options = (ImportOptions) getImportWizard().getPageData(ImportOptions.class);

        final Composite container = new Composite(parent, SWT.NONE);
        setControl(container);

        final GridLayout layout = new GridLayout(3, false);
        layout.marginWidth = getHorizontalMargin();
        layout.marginHeight = getVerticalMargin();
        layout.horizontalSpacing = getHorizontalSpacing();
        layout.verticalSpacing = getVerticalSpacing();
        container.setLayout(layout);

        /*
         * Build a composite to hold the prompt label and the folder control
         * with lessened spacing
         */

        final Composite folderContainer = new Composite(container, SWT.NONE);

        final GridLayout folderLayout = new GridLayout();
        folderLayout.marginWidth = 0;
        folderLayout.marginHeight = 0;
        folderLayout.horizontalSpacing = getHorizontalSpacing();
        folderLayout.verticalSpacing = 0;
        folderContainer.setLayout(folderLayout);

        final Label folderLabel = new Label(folderContainer, SWT.NONE);
        folderLabel.setText(Messages.getString("ImportWizardTreePage.FolderLabelText")); //$NON-NLS-1$
        GridDataBuilder.newInstance().hGrab().hFill().applyTo(folderLabel);

        final ServerItemType[] visibleItemTypes = ServerItemType.ALL_FOLDERS;

        folderControl = new ServerItemTreeControl(folderContainer, SWT.MULTI);
        folderControl.setVisibleServerItemTypes(visibleItemTypes);
        folderControl.setLabelProvider(new ImportWizardTreeLabelProvider(options));
        folderControl.addSelectionChangedListener(new ImportWizardSelectionListener());
        GridDataBuilder.newInstance().grab().fill().applyTo(folderControl);
        ControlSize.setCharSizeHints(folderControl, 40, 15);

        GridDataBuilder.newInstance().hSpan(3).grab().fill().applyTo(folderContainer);

        statusLabel = new Label(container, SWT.NONE);
        GridDataBuilder.newInstance().hGrab().hFill().applyTo(statusLabel);

        final List<Button> buttons = new ArrayList<Button>(3);

        /*
         * Add load/save plan buttons
         */

        final Button loadButton = new Button(container, SWT.PUSH);
        loadButton.setText(Messages.getString("TfsImportWizardTreePage.LoadButtonText")); //$NON-NLS-1$
        loadButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
                loadSelections();
                handleSelection();
            }
        });
        buttons.add(loadButton);

        final Button saveButton = new Button(container, SWT.PUSH);
        saveButton.setText(Messages.getString("TfsImportWizardTreePage.SaveButtonText")); //$NON-NLS-1$
        saveButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
                saveSelections();
            }
        });
        buttons.add(saveButton);

        /*
         * Options buttons
         */
        final Composite optionsContainer = new Composite(container, SWT.NONE);
        GridDataBuilder.newInstance().hGrab().hFill().hSpan(3).vIndent(getVerticalSpacing() * 2).applyTo(
            optionsContainer);

        final GridLayout optionsLayout = new GridLayout(3, false);
        optionsLayout.marginWidth = 0;
        optionsLayout.marginHeight = 0;
        optionsLayout.horizontalSpacing = getHorizontalSpacing();
        optionsLayout.verticalSpacing = 0;
        optionsContainer.setLayout(optionsLayout);

        workingSetButton = new Button(optionsContainer, SWT.CHECK);
        workingSetButton.setText(Messages.getString("ImportWizardTreePage.WorkingSetButtonText")); //$NON-NLS-1$
        workingSetButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
                /*
                 * No working set: simply open the dialog instead of enabling a
                 * useless combo.
                 */
                if (workingSets.length == 0) {
                    selectWorkingSet();

                    /* Still no working set. (Dialog canceled.) Abort. */
                    if (workingSets.length == 0) {
                        workingSetButton.setSelection(false);
                        return;
                    }
                }

                workingSetCombo.setEnabled(workingSetButton.getSelection());
                workingSetSelectButton.setEnabled(workingSetButton.getSelection());
            }
        });

        workingSetCombo = new Combo(optionsContainer, SWT.READ_ONLY);
        workingSetCombo.setEnabled(false);
        GridDataBuilder.newInstance().hGrab().hFill().applyTo(workingSetCombo);

        workingSetSelectButton = new Button(optionsContainer, SWT.NONE);
        workingSetSelectButton.setText(Messages.getString("ImportWizardTreePage.WorkingSetSelectButtonText")); //$NON-NLS-1$
        workingSetSelectButton.setEnabled(false);
        workingSetSelectButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
                selectWorkingSet();
            }
        });
        buttons.add(workingSetSelectButton);

        ButtonHelper.resizeButtons(buttons.toArray(new Button[buttons.size()]));

        newProjectButton = new Button(optionsContainer, SWT.CHECK);
        newProjectButton.setText(Messages.getString("ImportWizardTreePage.NewProjectButtonText")); //$NON-NLS-1$
        GridDataBuilder.newInstance().hGrab().hFill().hSpan(3).applyTo(newProjectButton);

        forceButton = new Button(optionsContainer, SWT.CHECK);
        forceButton.setText(Messages.getString("ImportWizardTreePage.ForceButtonText")); //$NON-NLS-1$
        AutomationIDHelper.setWidgetID(forceButton, FORCE_BUTTON_ID);
        GridDataBuilder.newInstance().hGrab().hFill().hSpan(3).vIndent(getVerticalSpacing()).applyTo(forceButton);

        computeWorkingSets();

        if (options.getWorkingSet() != null) {
            workingSetButton.setSelection(true);
            selectWorkingSet(options.getWorkingSet());
        }
    }

    /*
     * Loads from an XML file a list of server paths for folders to import from
     * TFS and selects appropriate nodes in the folder selection control
     */
    private void loadSelections() {
        final FileDialog fileDialog = new FileDialog(getShell(), SWT.OPEN);

        fileDialog.setFilterExtensions(new String[] {
            "*.xml", //$NON-NLS-1$
            "*.*" //$NON-NLS-1$
        });
        fileDialog.setFilterNames(new String[] {
            Messages.getString("TfsImportWizardTreePage.XmlFilesFilterLabel"), //$NON-NLS-1$
            Messages.getString("TfsImportWizardTreePage.AllFilesFilterLabel") //$NON-NLS-1$
        });

        final String filePath = fileDialog.open();

        if (!StringUtil.isNullOrEmpty(filePath)) {
            final ImportFolderCollection loadedItemCollection = new ImportFolderCollection(options);

            try {
                loadedItemCollection.fromFile(new File(filePath));
            } catch (final Exception e) {
                String messageFormat = "error loading selections from {0}"; //$NON-NLS-1$
                String message = MessageFormat.format(messageFormat, filePath);
                log.error(message, e);

                messageFormat = Messages.getString("TfsImportWizardTreePage.ErrorDialogTextFormat"); //$NON-NLS-1$
                message = MessageFormat.format(messageFormat, e.getMessage());

                MessageBoxHelpers.errorMessageBox(
                    getShell(),
                    Messages.getString("TfsImportWizardTreePage.LoadingErrorDialogTitle"), //$NON-NLS-1$
                    message);
                return;
            }

            final String[] folderPaths = loadedItemCollection.getFolders();
            setSelectedFolders(folderPaths);
        }
    }

    private void saveSelections() {
        final FileDialog fileDialog = new FileDialog(getShell(), SWT.SAVE);

        fileDialog.setFilterExtensions(new String[] {
            "*.xml", //$NON-NLS-1$
            "*.*" //$NON-NLS-1$
        });
        fileDialog.setFilterNames(new String[] {
            Messages.getString("TfsImportWizardTreePage.XmlFilesFilterLabel"), //$NON-NLS-1$
            Messages.getString("TfsImportWizardTreePage.AllFilesFilterLabel") //$NON-NLS-1$
        });

        fileDialog.setFileName("importplan.xml"); //$NON-NLS-1$
        final String filePath = fileDialog.open();

        if (filePath != null) {
            try {
                itemCollection.toFile(new File(filePath));
            } catch (final Exception e) {
                String messageFormat = "error saving selections to {0}"; //$NON-NLS-1$
                String message = MessageFormat.format(messageFormat, filePath);
                log.error(message, e);

                messageFormat = Messages.getString("TfsImportWizardTreePage.ErrorDialogTextFormat"); //$NON-NLS-1$
                message = MessageFormat.format(messageFormat, e.getMessage());

                MessageBoxHelpers.errorMessageBox(
                    getShell(),
                    Messages.getString("TfsImportWizardTreePage.SavingErrorDialogTitle"), //$NON-NLS-1$
                    message);
            }
        }
    }

    /*
     * Saves to an XML file a list of server paths for currently selected
     * folders.This list could be used by team members to simplify selection.
     */
    private void setSelectedFolders(final String[] serverPaths) {
        if (serverPaths != null && serverPaths.length > 0) {
            final TypedServerItem[] serverItems = new TypedServerItem[serverPaths.length];

            for (int i = 0; i < serverPaths.length; i++) {
                serverItems[i] = new TypedServerItem(serverPaths[i], ServerItemType.FOLDER);
            }

            folderControl.setSelectedItems(serverItems);
        }
    }

    private void computeWorkingSets() {
        computeWorkingSets(null);
    }

    /**
     * Rebuilds the internal list of IWorkingSets that are available. Optionally
     * includes the given <code>newWorkingSet</code>. This is useful for
     * including an AggregateWorkingSet that is not included in the list of
     * working sets managed by the {@link IWorkbench}'s
     * {@link IWorkingSetManager}.
     *
     * @param newWorkingSet
     *        An IWorkingSet to include in the list. (may be <code>null</code>).
     */
    private void computeWorkingSets(final IWorkingSet newWorkingSet) {
        if (newWorkingSet == null) {
            workingSets = PlatformUI.getWorkbench().getWorkingSetManager().getWorkingSets();
        } else {
            final List<IWorkingSet> workingSetList = new ArrayList<IWorkingSet>();

            workingSetList.addAll(Arrays.asList(PlatformUI.getWorkbench().getWorkingSetManager().getWorkingSets()));

            if (!workingSetList.contains(newWorkingSet)) {
                workingSetList.add(newWorkingSet);
            }

            workingSets = workingSetList.toArray(new IWorkingSet[workingSetList.size()]);
        }

        workingSetNames = new String[workingSets.length];
        for (int i = 0; i < workingSets.length; i++) {
            workingSetNames[i] = WorkingSetHelper.getLabel(workingSets[i]);
        }

        workingSetCombo.setItems(workingSetNames);

        if (workingSetCombo.getSelectionIndex() == -1) {
            workingSetCombo.select(0);
        }
    }

    private void selectWorkingSet(final IWorkingSet selection) {
        for (int i = 0; i < workingSets.length; i++) {
            if (workingSets[i].getName().equals(selection.getName())) {
                workingSetCombo.select(i);
                return;
            }
        }
    }

    @Override
    public void refresh() {
        final ImportWizard wizard = getImportWizard();

        wizard.removePageData(ImportItemCollectionBase.class);

        options = (ImportOptions) wizard.getPageData(ImportOptions.class);
        connection = (TFSTeamProjectCollection) wizard.getPageData(TFSTeamProjectCollection.class);

        final List<ProjectInfo> projects;

        if (wizard.hasPageData(ConnectWizard.SELECTED_TEAM_PROJECTS)) {
            projects = Arrays.asList((ProjectInfo[]) wizard.getPageData(ConnectWizard.SELECTED_TEAM_PROJECTS));
        } else {
            projects = wizard.getInitialTeamProjectList();
        }

        itemSource = new VersionedItemSource(connection, projects.toArray(new ProjectInfo[projects.size()]));
        itemSource.setCommandExecutor(getCommandExecutor());
        folderControl.setServerItemSource(itemSource);

        if (folderControl.getSelectedItems().length == 0) {
            /*
             * None previously selected (first time in). Look to see if we were
             * created with paths in the TFVC Source Control Explorer view
             */
            final String[] folderPaths = options.getImportFolders();
            setSelectedFolders(folderPaths);
        } else {
            /*
             * We're back to the page with some items selected. Let's make sure
             * that all selected items are visible, i.e. their parents are
             * expanded.
             */
            folderControl.setSelectedItems(folderControl.getSelectedItems());
        }

        handleSelection();

        computeWorkingSets();

        CodeMarkerDispatch.dispatch(CODEMARKER_REFRESH_COMPLETE);
    }

    private void handleSelection() {
        itemCollection = null;

        final TypedServerItem[] items = folderControl.getSelectedItems();
        if (items.length == 0) {
            statusLabel.setText(Messages.getString("ImportWizardTreePage.NoProjectsStatusLabelText")); //$NON-NLS-1$
            setPageComplete(false);
            setErrorMessage(null);
            return;
        } else if (items.length == 1) {
            statusLabel.setText(Messages.getString("ImportWizardTreePage.OneProjectSelectedStatusText")); //$NON-NLS-1$
        } else {
            final String message = MessageFormat.format(
                Messages.getString("ImportWizardTreePage.MultiProjectsSelectedStatusLabelTextFormat"), //$NON-NLS-1$
                items.length);
            statusLabel.setText(message);
        }

        itemCollection = new ImportFolderCollection(options, Arrays.asList(items));

        if (itemCollection.isValid()) {
            setPageComplete(true);
            setErrorMessage(null);
        } else {
            setPageComplete(false);
            setErrorMessage(itemCollection.getInvalidMessage());
        }
    }

    private void selectWorkingSet() {
        final IWorkingSetSelectionDialog workingSetDialog =
            PlatformUI.getWorkbench().getWorkingSetManager().createWorkingSetSelectionDialog(getShell(), false);

        if (workingSetDialog.open() != IDialogConstants.OK_ID) {
            return;
        }

        final IWorkingSet[] selection = workingSetDialog.getSelection();
        final IWorkingSet set = selection.length == 0 ? null : selection[0];

        computeWorkingSets(set);

        if (selection.length > 0) {
            selectWorkingSet(set);
        }
    }

    @Override
    protected boolean onPageFinished() {
        if (!itemCollection.isValid()) {
            MessageBoxHelpers.errorMessageBox(
                getShell(),
                Messages.getString("TfsImportWizardTreePage.InvalidSelectionErrorDialogTitle"), //$NON-NLS-1$
                itemCollection.getInvalidMessage());
            return false;
        }

        final ImportWizard wizard = (ImportWizard) getExtendedWizard();
        wizard.setPageData(ImportItemCollectionBase.class, itemCollection);

        options.setUseNewProjectWizard(newProjectButton.getSelection());
        options.setForceGet(forceButton.getSelection());

        if (workingSetButton.getSelection() && workingSets.length > 0) {
            options.setWorkingSet(workingSets[workingSetCombo.getSelectionIndex()]);
        }

        return true;
    }

    private class ImportWizardSelectionListener implements ISelectionChangedListener {
        @Override
        public void selectionChanged(final SelectionChangedEvent event) {
            handleSelection();
        }
    }

    private class ImportWizardTreeLabelProvider extends ServerItemLabelProvider {
        private final ImportOptions options;

        public ImportWizardTreeLabelProvider(final ImportOptions options) {
            Check.notNull(options, "options"); //$NON-NLS-1$

            this.options = options;
        }

        @Override
        public Image getImage(final Object element) {
            final TypedServerItem node = (TypedServerItem) element;

            final ImportFolderValidation validation = options.getFolderValidator().validate(node.getServerPath());

            if (validation.getStatus() == ImportFolderValidationStatus.ERROR
                && !validation.hasFlag(ImportFolderValidationFlag.NO_VISUAL_ERROR)) {
                return getImageHelper().getImage(TFSCommonUIClientPlugin.PLUGIN_ID, "images/common/warning.gif"); //$NON-NLS-1$
            } else if (validation.getStatus() == ImportFolderValidationStatus.CLOAKED) {
                return getImageHelper().getImage(TFSCommonUIClientPlugin.PLUGIN_ID, "images/vc/folder_cloaked.gif"); //$NON-NLS-1$
            } else if (validation.getStatus() == ImportFolderValidationStatus.ALREADY_EXISTS) {
                return getImageHelper().getImage(TFSCommonUIClientPlugin.PLUGIN_ID, "images/vc/folder_disabled.gif"); //$NON-NLS-1$
            } else if (validation.hasFlag(ImportFolderValidationFlag.EXISTING_MAPPING)) {
                return getImageHelper().getImage(TFSCommonUIClientPlugin.PLUGIN_ID, "images/vc/folder_mapped.gif"); //$NON-NLS-1$
            }

            return super.getImage(element);
        }
    }
}
