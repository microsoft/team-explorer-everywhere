// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.wizard.merge;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import com.microsoft.tfs.client.common.codemarker.CodeMarker;
import com.microsoft.tfs.client.common.codemarker.CodeMarkerDispatch;
import com.microsoft.tfs.client.common.framework.command.Command;
import com.microsoft.tfs.client.common.framework.command.ICommand;
import com.microsoft.tfs.client.common.framework.command.ICommandExecutor;
import com.microsoft.tfs.client.common.framework.command.ICommandFinishedCallback;
import com.microsoft.tfs.client.common.item.ServerItemPath;
import com.microsoft.tfs.client.common.repository.TFSRepository;
import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.TFSCommonUIClientPlugin;
import com.microsoft.tfs.client.common.ui.dialogs.vc.ServerItemTreeDialog;
import com.microsoft.tfs.client.common.ui.framework.command.UICommandExecutorFactory;
import com.microsoft.tfs.client.common.ui.framework.helper.FormHelper;
import com.microsoft.tfs.client.common.ui.framework.helper.MessageBoxHelpers;
import com.microsoft.tfs.client.common.ui.framework.helper.UIHelpers;
import com.microsoft.tfs.client.common.ui.framework.image.ImageHelper;
import com.microsoft.tfs.client.common.ui.framework.sizing.ControlSize;
import com.microsoft.tfs.client.common.ui.vc.serveritem.ServerItemType;
import com.microsoft.tfs.client.common.ui.vc.serveritem.VersionedItemSource;
import com.microsoft.tfs.client.common.ui.vc.tfsitem.TFSFolder;
import com.microsoft.tfs.client.common.ui.vc.tfsitem.TFSItem;
import com.microsoft.tfs.client.common.ui.vc.tfsitem.TFSItemFactory;
import com.microsoft.tfs.core.clients.versioncontrol.BranchHistory;
import com.microsoft.tfs.core.clients.versioncontrol.BranchHistoryTreeItem;
import com.microsoft.tfs.core.clients.versioncontrol.MergeFlags;
import com.microsoft.tfs.core.clients.versioncontrol.WebServiceLevel;
import com.microsoft.tfs.core.clients.versioncontrol.path.ServerPath;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Item;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.ItemIdentifier;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.RecursionType;
import com.microsoft.tfs.core.clients.versioncontrol.specs.ItemSpec;
import com.microsoft.tfs.core.clients.versioncontrol.specs.version.LatestVersionSpec;
import com.microsoft.tfs.util.LocaleUtil;

public class SelectMergeSourceTargetWizardPage extends WizardPage {
    private Combo targetCombo;
    private Text sourceText;
    private String sourcePath;
    private String targetPath;
    private String targetsLoadedFor;
    private MergeFlags mergeFlags = MergeFlags.NONE;
    private Button selectedChangesetsButton;
    private Button allChangesButton;
    private Label baselessWarningImageLabel;
    private Label baselessWarningLabel;

    private final ImageHelper imageHelper;
    private final TFSRepository repository;

    public static final String NAME = "SelectMergeSourceTargetWizardPage"; //$NON-NLS-1$

    private final Log log = LogFactory.getLog(SelectMergeSourceTargetWizardPage.class);

    public static final CodeMarker CODEMARKER_MERGESOURCETARGETPAGELOAD_COMPLETE = new CodeMarker(
        "com.microsoft.tfs.client.common.ui.wizard.merge.SelectMergeSourceTargetWizardPage#sourceTargetPageLoadComplete"); //$NON-NLS-1$

    /**
     * Create the wizard
     */
    public SelectMergeSourceTargetWizardPage(
        final TFSRepository repository,
        final String sourcePath,
        final ImageHelper imageHelper) {
        super(NAME);

        setTitle(Messages.getString("SelectMergeSourceTargetWizardPage.PageTitle")); //$NON-NLS-1$
        setDescription(Messages.getString("SelectMergeSourceTargetWizardPage.PageDescription")); //$NON-NLS-1$

        this.sourcePath = sourcePath;
        this.imageHelper = imageHelper;
        this.repository = repository;
    }

    /**
     * Create contents of the wizard
     *
     * @param parent
     */
    @Override
    public void createControl(final Composite parent) {
        final Composite container = new Composite(parent, SWT.NULL);

        final FormLayout formLayout = new FormLayout();
        formLayout.spacing = FormHelper.Spacing();
        formLayout.marginHeight = FormHelper.MarginHeight();
        formLayout.marginWidth = FormHelper.MarginWidth();
        container.setLayout(formLayout);
        setControl(container);

        createMergeSourceControls(container);
        createMergeSelectionControls(container);
        createMergeTargeControls(container);

        calculateTargetPaths(true, sourcePath);
    }

    private void createMergeSourceControls(final Composite container) {
        final Label selectTheBranchLabel = new Label(container, SWT.NONE);
        final FormData selectTheBranchLabelData = new FormData();
        selectTheBranchLabelData.top = new FormAttachment(0, 0);
        selectTheBranchLabelData.left = new FormAttachment(0, 0);
        selectTheBranchLabel.setLayoutData(selectTheBranchLabelData);
        selectTheBranchLabel.setText(Messages.getString("SelectMergeSourceTargetWizardPage.SelectLabelText")); //$NON-NLS-1$

        final Label sourceBranchLabel = new Label(container, SWT.NONE);
        final FormData sourceBranchLabelData = new FormData();
        sourceBranchLabelData.top = new FormAttachment(selectTheBranchLabel, 5, SWT.BOTTOM);
        sourceBranchLabelData.left = new FormAttachment(0, 0);
        sourceBranchLabel.setLayoutData(sourceBranchLabelData);
        sourceBranchLabel.setText(Messages.getString("SelectMergeSourceTargetWizardPage.SourceBranchLabelText")); //$NON-NLS-1$

        final Button browseSourceButton = new Button(container, SWT.NONE);
        final FormData browseSourceButtonData = new FormData();
        browseSourceButtonData.top = new FormAttachment(sourceBranchLabel, 0, SWT.BOTTOM);
        browseSourceButtonData.right = new FormAttachment(100, 0);
        browseSourceButton.setLayoutData(browseSourceButtonData);
        browseSourceButton.setText(Messages.getString("SelectMergeSourceTargetWizardPage.BrowseButtonText")); //$NON-NLS-1$

        browseSourceButton.addSelectionListener(new SelectionListener() {
            @Override
            public void widgetDefaultSelected(final SelectionEvent e) {
                browseSourceClicked();
            }

            @Override
            public void widgetSelected(final SelectionEvent e) {
                browseSourceClicked();
            }
        });

        sourceText = new Text(container, SWT.BORDER);
        final FormData sourceTextData = new FormData();
        sourceTextData.top =
            new FormAttachment(browseSourceButton, FormHelper.VerticalOffset(sourceText, browseSourceButton), SWT.TOP);
        sourceTextData.left = new FormAttachment(0, 0);
        sourceTextData.right = new FormAttachment(browseSourceButton, 0, SWT.LEFT);
        sourceText.setLayoutData(sourceTextData);
        sourceText.setText(sourcePath);

        sourceText.addFocusListener(new FocusListener() {
            @Override
            public void focusGained(final FocusEvent e) {
                updatePageComplete();
            }

            @Override
            public void focusLost(final FocusEvent e) {
                calculateTargetPaths(false, sourcePath);
            }
        });

        sourceText.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(final ModifyEvent e) {
                updatePageComplete();
            }
        });
    }

    private void createMergeSelectionControls(final Composite container) {
        final Label selectTheSourceLabel = new Label(container, SWT.NONE);
        final FormData selectTheSourceLabelData = new FormData();
        selectTheSourceLabelData.top = new FormAttachment(sourceText, 5, SWT.BOTTOM);
        selectTheSourceLabelData.left = new FormAttachment(0, 0);
        selectTheSourceLabel.setLayoutData(selectTheSourceLabelData);
        selectTheSourceLabel.setText(Messages.getString("SelectMergeSourceTargetWizardPage.SelectSourceLabelText")); //$NON-NLS-1$

        allChangesButton = new Button(container, SWT.RADIO);
        final FormData allChangesButtonData = new FormData();
        allChangesButtonData.top = new FormAttachment(selectTheSourceLabel, 0, SWT.BOTTOM);
        allChangesButtonData.left = new FormAttachment(0, 0);
        allChangesButton.setLayoutData(allChangesButtonData);
        allChangesButton.setText(Messages.getString("SelectMergeSourceTargetWizardPage.AllChangesButtonText")); //$NON-NLS-1$
        allChangesButton.setSelection(true);

        allChangesButton.addSelectionListener(new SelectionListener() {
            @Override
            public void widgetDefaultSelected(final SelectionEvent e) {
                wizardCanFinish(isPageComplete());
            }

            @Override
            public void widgetSelected(final SelectionEvent e) {
                wizardCanFinish(isPageComplete());
            }
        });

        selectedChangesetsButton = new Button(container, SWT.RADIO);
        final FormData selectedChangesetsButtonData = new FormData();
        selectedChangesetsButtonData.top = new FormAttachment(allChangesButton, 0, SWT.BOTTOM);
        selectedChangesetsButtonData.left = new FormAttachment(0, 0);
        selectedChangesetsButton.setLayoutData(selectedChangesetsButtonData);
        selectedChangesetsButton.setText(
            Messages.getString("SelectMergeSourceTargetWizardPage.SelectedChangesetsButtonText")); //$NON-NLS-1$

        selectedChangesetsButton.addSelectionListener(new SelectionListener() {
            @Override
            public void widgetDefaultSelected(final SelectionEvent e) {
                wizardCanFinish(false);
            }

            @Override
            public void widgetSelected(final SelectionEvent e) {
                wizardCanFinish(false);
            }
        });
    }

    private void createMergeTargeControls(final Composite container) {
        final Label selectTheTargetLabel = new Label(container, SWT.WRAP);
        final FormData selectTheTargetLabelData = new FormData();
        selectTheTargetLabelData.top = new FormAttachment(selectedChangesetsButton, 5, SWT.BOTTOM);
        selectTheTargetLabelData.left = new FormAttachment(0, 0);
        selectTheTargetLabel.setLayoutData(selectTheTargetLabelData);
        selectTheTargetLabel.setText(Messages.getString("SelectMergeSourceTargetWizardPage.SelectTargetLabelText")); //$NON-NLS-1$
        ControlSize.setCharWidthHint(selectTheTargetLabel, MergeWizard.TEXT_CHARACTER_WIDTH);

        final Label targetBranchLabel = new Label(container, SWT.NONE);
        final FormData targetBranchLabelData = new FormData();
        targetBranchLabelData.top = new FormAttachment(selectTheTargetLabel, 5, SWT.BOTTOM);
        targetBranchLabelData.left = new FormAttachment(0, 0);
        targetBranchLabel.setLayoutData(targetBranchLabelData);
        targetBranchLabel.setText(Messages.getString("SelectMergeSourceTargetWizardPage.TargetBranchLabelText")); //$NON-NLS-1$

        final Button browseTargetButton = new Button(container, SWT.NONE);
        final FormData browseTargetButtonData = new FormData();
        browseTargetButtonData.top = new FormAttachment(targetBranchLabel, 0, SWT.BOTTOM);
        browseTargetButtonData.right = new FormAttachment(100, 0);
        browseTargetButton.setLayoutData(browseTargetButtonData);
        browseTargetButton.setText(Messages.getString("SelectMergeSourceTargetWizardPage.BrowseButtonText")); //$NON-NLS-1$

        browseTargetButton.addSelectionListener(new SelectionListener() {
            @Override
            public void widgetDefaultSelected(final SelectionEvent e) {
                browseTargetClicked();
            }

            @Override
            public void widgetSelected(final SelectionEvent e) {
                browseTargetClicked();
            }
        });

        targetCombo = new Combo(container, SWT.BORDER);
        final FormData targetComboData = new FormData();
        final int targetComboTopOffset = FormHelper.VerticalOffset(targetCombo, browseTargetButton);
        targetComboData.top = new FormAttachment(browseTargetButton, targetComboTopOffset, SWT.TOP);
        targetComboData.left = new FormAttachment(0, 0);
        targetComboData.right = new FormAttachment(browseTargetButton, 0, SWT.LEFT);
        targetCombo.setLayoutData(targetComboData);

        targetCombo.addSelectionListener(new SelectionListener() {
            @Override
            public void widgetDefaultSelected(final SelectionEvent e) {
                targetPathChanged();
            }

            @Override
            public void widgetSelected(final SelectionEvent e) {
                targetPathChanged();
            }
        });

        targetCombo.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(final ModifyEvent e) {
                targetPathChanged();
            }
        });

        targetCombo.addFocusListener(new FocusListener() {
            @Override
            public void focusGained(final FocusEvent e) {
                updatePageComplete();
            }

            @Override
            public void focusLost(final FocusEvent e) {
                updatePageComplete();
            }
        });

        createBaselessWarningMessage(container, targetBranchLabel);
    }

    private void createBaselessWarningMessage(final Composite container, final Control imageNeighbour) {
        final Image baselessWarningImage = imageHelper.getImage("images/common/warning.gif"); //$NON-NLS-1$
        baselessWarningImageLabel = new Label(container, SWT.NONE);
        baselessWarningImageLabel.setImage(baselessWarningImage);
        baselessWarningImageLabel.setToolTipText(
            Messages.getString("SelectMergeSourceTargetWizardPage.BaselessWarningText")); //$NON-NLS-1$
        final FormData baselessWarningImageLabelData = new FormData();
        final int warningImageTopOffset = FormHelper.VerticalOffset(baselessWarningImageLabel, imageNeighbour);
        baselessWarningImageLabelData.top = new FormAttachment(imageNeighbour, warningImageTopOffset, SWT.TOP);
        baselessWarningImageLabelData.left = new FormAttachment(imageNeighbour, 2, SWT.RIGHT);
        baselessWarningImageLabel.setLayoutData(baselessWarningImageLabelData);

        baselessWarningLabel = new Label(container, SWT.NONE);
        final FormData baselessWarningLabelData = new FormData();
        baselessWarningLabelData.top = new FormAttachment(targetCombo, 5, SWT.BOTTOM);
        baselessWarningLabelData.left = new FormAttachment(0, 0);
        baselessWarningLabel.setLayoutData(baselessWarningLabelData);
        baselessWarningLabel.setText(Messages.getString("SelectMergeSourceTargetWizardPage.BaselessWarningText")); //$NON-NLS-1$

        setBaselessWarningVisibility(false);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean canFlipToNextPage() {
        return isComplete();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IWizardPage getNextPage() {
        boolean canNavigate = true;

        try {
            final String itemDoesNotExistFormat =
                Messages.getString("SelectMergeSourceTargetWizardPage.ItemDoesNotExistFormat"); //$NON-NLS-1$

            TFSItem sourceItem;
            TFSItem targetItem;

            if ((sourceItem = TFSItemFactory.getItemAtPath(repository, sourcePath)) == null) {
                MessageBoxHelpers.errorMessageBox(
                    getShell(),
                    Messages.getString("SelectMergeSourceTargetWizardPage.ErrorDialogTitle"), //$NON-NLS-1$
                    MessageFormat.format(itemDoesNotExistFormat, sourcePath));

                canNavigate = false;
            } else if ((targetItem = TFSItemFactory.getItemAtPath(repository, targetPath)) == null) {
                MessageBoxHelpers.errorMessageBox(
                    getShell(),
                    Messages.getString("SelectMergeSourceTargetWizardPage.ErrorDialogTitle"), //$NON-NLS-1$
                    MessageFormat.format(itemDoesNotExistFormat, targetPath));

                canNavigate = false;
            } else if (repository.getWorkspace().getMappedLocalPath(targetPath) == null) {
                // The target item should always be mapped
                MessageBoxHelpers.errorMessageBox(
                    getShell(),
                    Messages.getString("SelectMergeSourceTargetWizardPage.ErrorDialogTitle"), //$NON-NLS-1$
                    Messages.getString("SelectMergeSourceTargetWizardPage.TargetIsNotMapped")); //$NON-NLS-1$

                canNavigate = false;
            } else if (isBaseless()) {
                if ((sourceItem instanceof TFSFolder) != (targetItem instanceof TFSFolder)) {
                    // The target item and the source item should be either
                    // both folders or both files
                    MessageBoxHelpers.errorMessageBox(
                        getShell(),
                        Messages.getString("SelectMergeSourceTargetWizardPage.ErrorDialogTitle"), //$NON-NLS-1$
                        Messages.getString("SelectMergeSourceTargetWizardPage.ItemsAreOfDifferentType")); //$NON-NLS-1$

                    canNavigate = false;
                }
            }
        } catch (final Exception e) {
            log.trace(e);

            MessageBoxHelpers.errorMessageBox(
                getShell(),
                Messages.getString("SelectMergeSourceTargetWizardPage.ErrorDialogTitle"), //$NON-NLS-1$
                e.getLocalizedMessage());

            canNavigate = false;
        }

        if (canNavigate) {
            return super.getNextPage();
        } else {
            return this;
        }
    }

    private void targetPathChanged() {
        updatePageComplete();

        final boolean isBaseless = this.isBaseless();

        // Cannot query merge candidates for baseless merges before Tfs2012 web
        // service level
        if (repository.getVersionControlClient().getServiceLevel().getValue() < WebServiceLevel.TFS_2010.getValue()) {
            selectedChangesetsButton.setEnabled(!isBaseless);

            if (isBaseless) {
                allChangesButton.setSelection(true);
            }
        }

        setBaselessWarningVisibility(isBaseless);
    }

    private void browseSourceClicked() {
        final ServerItemTreeDialog browseDialog = openBrowseServerDialog(
            sourceText.getText(),
            sourcePath,
            Messages.getString("SelectMergeSourceTargetWizardPage.BrowseSourceDialogTitle")); //$NON-NLS-1$

        if (browseDialog != null && browseDialog.getSelectedItem() != null) {
            sourceText.setText(browseDialog.getSelectedServerPath());
            calculateTargetPaths(false, sourceText.getText());
        }
    }

    private void browseTargetClicked() {
        final ServerItemTreeDialog browseDialog = openBrowseServerDialog(
            targetCombo.getText(),
            targetPath,
            Messages.getString("SelectMergeSourceTargetWizardPage.BrowseTargetDialogTitle")); //$NON-NLS-1$

        if (browseDialog != null && browseDialog.getSelectedItem() != null) {
            targetCombo.setText(browseDialog.getSelectedServerPath());
        }
    }

    private ServerItemTreeDialog openBrowseServerDialog(
        final String serverPath,
        final String defaultPath,
        final String dialogTitle) {
        String initialPath;

        if (!isEmpty(serverPath)) {
            initialPath = serverPath;
        } else {
            initialPath = defaultPath;
        }

        final ServerItemTreeDialog browseDialog = new ServerItemTreeDialog(
            getShell(),
            dialogTitle,
            initialPath,
            new VersionedItemSource(repository),
            ServerItemType.ALL);

        if (browseDialog.open() != ServerItemTreeDialog.OK) {
            return null;
        }

        return browseDialog;
    }

    private void setBaselessWarningVisibility(final boolean isVisible) {
        baselessWarningImageLabel.setVisible(isVisible);
        baselessWarningLabel.setVisible(isVisible);
    }

    private void wizardCanFinish(final boolean canFinish) {
        ((MergeWizard) getWizard()).setComplete(canFinish);
    }

    private boolean isValidServerPath(final String path) {
        if (!isEmpty(path)) {
            if (!ServerPath.isServerPath(path)) {
                MessageBoxHelpers.errorMessageBox(
                    getShell(),
                    Messages.getString("SelectMergeSourceTargetWizardPage.ErrorDialogTitle"), //$NON-NLS-1$
                    Messages.getString("SelectMergeSourceTargetWizardPage.RequiresServerPath")); //$NON-NLS-1$
                return false;
            }

            if (ServerPath.isWildcard(path)) {
                MessageBoxHelpers.errorMessageBox(
                    getShell(),
                    Messages.getString("SelectMergeSourceTargetWizardPage.ErrorDialogTitle"), //$NON-NLS-1$
                    Messages.getString("SelectMergeSourceTargetWizardPage.WildcardNotAllowed")); //$NON-NLS-1$
                return false;
            }
        }

        return true;
    }

    private boolean isComplete() {
        sourcePath = sourceText.getText().trim();
        targetPath = targetCombo.getText().trim();

        if (isEmpty(sourcePath) || isEmpty(targetPath)) {
            return false;
        } else if (!ServerPath.isServerPath(sourcePath) || !ServerPath.isServerPath(targetPath)) {
            return false;
        } else if (ServerPath.isWildcard(sourcePath) || ServerPath.isWildcard(targetPath)) {
            return false;
        } else {
            return true;
        }
    }

    private void updatePageComplete() {
        setPageComplete(isComplete());
    }

    /**
     * @return the sourcePath
     */
    public String getSourcePath() {
        return sourcePath;
    }

    public String getTargetPath() {
        return targetPath;
    }

    public MergeFlags getMergeFlags() {
        return mergeFlags;
    }

    public boolean isSelectChangesetsSelected() {
        return selectedChangesetsButton.getSelection();
    }

    private boolean isBaseless() {
        mergeFlags = MergeFlags.NONE;

        final String target = targetPath.trim();

        try {
            final String targetFullPath = new ServerItemPath(target).getFullPath();

            for (final String targetComboItem : targetCombo.getItems()) {
                if (targetComboItem.equalsIgnoreCase(targetFullPath)) {
                    return false;
                }
            }

            mergeFlags = MergeFlags.BASELESS;
            return true;
        } catch (final Exception e) {
        }

        return false;
    }

    private boolean isEmpty(final String s) {
        return s == null || s.length() == 0;
    }

    private void calculateTargetPaths(final boolean force, final String sourcePath) {
        if (force || targetsLoadedFor == null || !targetsLoadedFor.equals(sourcePath)) {
            targetsLoadedFor = sourcePath;

            targetCombo.remove(0, targetCombo.getItemCount() - 1);

            if (isEmpty(sourcePath) || !isValidServerPath(sourcePath)) {
                return;
            }

            asyncLoadTargetCandidates(sourcePath);
        }

        updatePageComplete();
    }

    private void asyncLoadTargetCandidates(final String sourcePath) {
        final Shell shell = getShell();

        shell.getDisplay().asyncExec(new Runnable() {
            @Override
            public void run() {
                if (shell != null && !shell.isDisposed()) {
                    final QueryTargetCandidatesCommand loadTargetsCommand = new QueryTargetCandidatesCommand(shell);

                    final ICommandExecutor commandExecutor =
                        UICommandExecutorFactory.newWizardCommandExecutor(getContainer());
                    commandExecutor.setCommandFinishedCallback(loadTargetsCommand);

                    final IStatus status = commandExecutor.execute(loadTargetsCommand);

                    if (status.getSeverity() == IStatus.ERROR) {
                        ErrorDialog.openError(
                            shell,
                            Messages.getString("SelectMergeSourceTargetWizardPage.ErrorDialogTitle"), //$NON-NLS-1$
                            null,
                            status);
                    }
                }
            };
        });
    }

    private List<String> getTargets(
        final TFSRepository repository,
        final String sourcePath,
        final IProgressMonitor progressMonitor) throws Exception {
        final ArrayList<String> targets = new ArrayList<String>();
        final ItemSpec itemSpec = new ItemSpec(sourcePath, RecursionType.NONE);

        try {
            if (repository.getVersionControlClient().getServiceLevel().getValue() >= WebServiceLevel.TFS_2010.getValue()) {
                final ItemIdentifier[] items = repository.getVersionControlClient().queryMergeRelationships(sourcePath);
                if (items != null) {
                    for (int i = 0; i < items.length; i++) {
                        if (progressMonitor.isCanceled()) {
                            return null;
                        }

                        if (items[i].getDeletionID() == 0
                            && items[i].getItem() != null
                            && items[i].getItem().length() > 0) {
                            targets.add(items[i].getItem());
                        }
                    }
                }
            } else {
                final BranchHistory branchHistory =
                    repository.getWorkspace().getBranchHistory(itemSpec, LatestVersionSpec.INSTANCE);
                if (branchHistory != null && branchHistory.getRequestedItem() != null) {
                    final BranchHistoryTreeItem requestedItem = branchHistory.getRequestedItem();

                    // Add parent history item.
                    if (requestedItem.getParentBranchHistoryTreeItem() != null
                        && requestedItem.getParentBranchHistoryTreeItem().getItem() != null
                        && requestedItem.getParentBranchHistoryTreeItem().getItem().getDeletionID() == 0
                        && requestedItem.getParentBranchHistoryTreeItem().getItem().getServerItem() != null) {
                        targets.add(requestedItem.getParentBranchHistoryTreeItem().getItem().getServerItem());
                    }

                    // Add child history items
                    if (requestedItem.hasChildren()) {
                        for (final Iterator<BranchHistoryTreeItem> children =
                            requestedItem.getChildrenAsList().iterator(); children.hasNext();) {
                            if (progressMonitor.isCanceled()) {
                                return null;
                            }

                            final Item item = children.next().getItem();
                            if (item != null && item.getDeletionID() == 0 && item.getServerItem() != null) {
                                targets.add(item.getServerItem());
                            }
                        }
                    }
                }
            }
        }

        catch (final Exception e) {
            log.error("Could not determine branch targets", e); //$NON-NLS-1$
            throw e;
        }

        return targets;
    }

    private class QueryTargetCandidatesCommand extends Command implements ICommandFinishedCallback {
        List<String> targets;
        final Shell shell;

        public QueryTargetCandidatesCommand(final Shell shell) {
            this.shell = shell;
        }

        @Override
        public String getName() {
            return Messages.getString("SelectMergeSourceTargetWizardPage.LoadingTargetCandidatesName"); //$NON-NLS-1$
        }

        @Override
        public String getErrorDescription() {
            return Messages.getString("SelectMergeSourceTargetWizardPage.LoadingTargetCandidatesError"); //$NON-NLS-1$
        }

        @Override
        public String getLoggingDescription() {
            return Messages.getString("SelectMergeSourceTargetWizardPage.LoadingTargetCandidatesName", LocaleUtil.ROOT); //$NON-NLS-1$
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean isCancellable() {
            return true;
        }

        @Override
        protected IStatus doRun(final IProgressMonitor progressMonitor) throws Exception {
            targets = null;

            if (shell == null || shell.isDisposed()) {
                return Status.CANCEL_STATUS;
            }

            progressMonitor.beginTask(getName(), IProgressMonitor.UNKNOWN);

            try {
                targets = getTargets(repository, sourcePath, progressMonitor);
            } catch (final Exception e) {
                return new Status(IStatus.ERROR, TFSCommonUIClientPlugin.PLUGIN_ID, getErrorDescription(), e);
            } finally {
                progressMonitor.done();
            }

            return Status.OK_STATUS;
        }

        @Override
        public void onCommandFinished(final ICommand command, final IStatus status) {
            if (status.isOK()) {
                final QueryTargetCandidatesCommand queryTargetCandidatesCommand =
                    (QueryTargetCandidatesCommand) command;

                if (queryTargetCandidatesCommand.targets != null
                    && queryTargetCandidatesCommand.targets.size() > 0
                    && !queryTargetCandidatesCommand.shell.isDisposed()) {
                    // run on the UI thread to avoid contention
                    UIHelpers.runOnUIThread(shell, false, new Runnable() {
                        @Override
                        public void run() {
                            if (shell.isDisposed() || targetCombo.isDisposed()) {
                                return;
                            }

                            Collections.sort(targets, String.CASE_INSENSITIVE_ORDER);

                            int selectionIndex = 0;
                            for (final String target : targets) {
                                if (target.equalsIgnoreCase(targetPath)) {
                                    selectionIndex = targetCombo.getItemCount();
                                }
                                targetCombo.add(target);
                            }

                            targetCombo.select(selectionIndex);
                        }
                    });

                    CodeMarkerDispatch.dispatch(CODEMARKER_MERGESOURCETARGETPAGELOAD_COMPLETE);
                }
            }
        }
    }
}
