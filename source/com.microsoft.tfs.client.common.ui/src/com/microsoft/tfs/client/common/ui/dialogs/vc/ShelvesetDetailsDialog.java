// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.dialogs.vc;

import java.text.DateFormat;

import org.eclipse.jface.action.IContributionManager;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.HTMLTransfer;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;

import com.microsoft.tfs.client.common.repository.TFSRepository;
import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.TFSCommonUIClientPlugin;
import com.microsoft.tfs.client.common.ui.TFSCommonUIImages;
import com.microsoft.tfs.client.common.ui.controls.vc.changes.ChangeItem;
import com.microsoft.tfs.client.common.ui.controls.vc.changes.ChangeItemProvider;
import com.microsoft.tfs.client.common.ui.controls.vc.changes.QueryShelvesetChangeItemProvider;
import com.microsoft.tfs.client.common.ui.controls.vc.checkin.CheckinControl;
import com.microsoft.tfs.client.common.ui.controls.vc.checkin.CheckinControlOptions;
import com.microsoft.tfs.client.common.ui.controls.vc.checkin.SourceFilesCheckinControl;
import com.microsoft.tfs.client.common.ui.controls.vc.checkin.actions.CompareShelvedChangeWithLatestVersionAction;
import com.microsoft.tfs.client.common.ui.controls.vc.checkin.actions.CompareShelvedChangeWithUnmodifiedVersionAction;
import com.microsoft.tfs.client.common.ui.controls.vc.checkin.actions.ViewPendingChangeAction;
import com.microsoft.tfs.client.common.ui.controls.vc.checkin.actions.ViewVersionType;
import com.microsoft.tfs.client.common.ui.framework.action.StandardActionConstants;
import com.microsoft.tfs.client.common.ui.framework.action.ToolbarPulldownAction;
import com.microsoft.tfs.client.common.ui.framework.command.UICommandExecutorFactory;
import com.microsoft.tfs.client.common.ui.framework.compare.CompareUIType;
import com.microsoft.tfs.client.common.ui.framework.helper.SWTUtil;
import com.microsoft.tfs.client.common.ui.framework.image.ImageHelper;
import com.microsoft.tfs.client.common.ui.framework.layout.GridDataBuilder;
import com.microsoft.tfs.client.common.ui.framework.validation.ButtonValidatorBinding;
import com.microsoft.tfs.client.common.util.DateHelper;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.CheckinNote;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Shelveset;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.WorkItemCheckinInfo;
import com.microsoft.tfs.core.clients.workitem.WorkItemClient;
import com.microsoft.tfs.core.util.TSWAHyperlinkBuilder;
import com.microsoft.tfs.util.Check;

public class ShelvesetDetailsDialog extends AbstractCheckinControlDialog {
    private final Shelveset shelveset;
    private final TFSRepository repository;
    private final boolean allowUnshelve;

    private DateFormat dateFormat;
    private CheckinControl checkinControl;
    private CheckinControlOptions options;
    private Button restoreButton;
    private Button preserveButton;
    private Button copyLinkButton;
    private final ImageHelper imageHelper = new ImageHelper(TFSCommonUIClientPlugin.PLUGIN_ID);

    private MenuManager viewSubMenu;
    private ToolbarPulldownAction viewToolbarAction;
    private ViewPendingChangeAction viewAction;
    private ViewPendingChangeAction viewUnmodifiedAction;
    private ViewPendingChangeAction viewLatestAction;

    private MenuManager compareSubMenu;
    private ToolbarPulldownAction compareToolbarAction;
    private CompareShelvedChangeWithUnmodifiedVersionAction compareWithUnmodifiedAction;
    private CompareShelvedChangeWithLatestVersionAction compareWithLatestAction;

    private boolean preserveShelveset = true;
    private boolean restoreData = true;
    private TSWAHyperlinkBuilder tswaHyperlinkBuilder;

    /**
     * The lazily created clipboard and transferTypes. The two field starts off
     * as <code>null</code>, and allocated if needed.
     */
    private Clipboard clipboard = null;
    private Transfer[] transferTypes = null;

    public ShelvesetDetailsDialog(
        final Shell parentShell,
        final Shelveset shelveset,
        final TFSRepository repository,
        final boolean allowUnshelve) {
        super(parentShell);
        this.shelveset = shelveset;
        this.repository = repository;
        dateFormat = DateHelper.getDefaultDateTimeFormat();
        this.allowUnshelve = allowUnshelve;

        setOptionIncludeDefaultButtons(false);

        if (allowUnshelve) {
            addButtonDescription(
                IDialogConstants.OK_ID,
                Messages.getString("ShelvesetDetailsDialog.UnshelveButtonText"), //$NON-NLS-1$
                true);
            addButtonDescription(IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
        } else {
            addButtonDescription(IDialogConstants.CANCEL_ID, IDialogConstants.CLOSE_LABEL, true);
        }
    }

    public void setDateFormat(final DateFormat dateFormat) {
        Check.notNull(dateFormat, "dateFormat"); //$NON-NLS-1$
        this.dateFormat = dateFormat;
    }

    /**
     * @return the preserveShelveset
     */
    public boolean isPreserveShelveset() {
        return preserveShelveset;
    }

    /**
     * @param preserveShelveset
     *        the preserveShelveset to set
     */
    public void setPreserveShelveset(final boolean preserveShelveset) {
        this.preserveShelveset = preserveShelveset;
    }

    /**
     * @return the restoreData
     */
    public boolean isRestoreData() {
        return restoreData;
    }

    /**
     * @param restoreData
     *        the restoreData to set
     */
    public void setRestoreData(final boolean restoreData) {
        this.restoreData = restoreData;
    }

    @Override
    protected void hookAddToDialogArea(final Composite dialogArea) {
        final GridLayout layout = new GridLayout(2, false);
        layout.marginWidth = getHorizontalMargin();
        layout.marginHeight = getVerticalMargin();
        layout.horizontalSpacing = getHorizontalSpacing();
        layout.verticalSpacing = getVerticalSpacing();
        dialogArea.setLayout(layout);

        Label label = new Label(dialogArea, SWT.NONE);
        label.setText(Messages.getString("ShelvesetDetailsDialog.NameLabelText")); //$NON-NLS-1$

        final Composite composite = new Composite(dialogArea, SWT.NONE);
        SWTUtil.gridLayout(composite, 2, false, 0, 0);
        GridDataBuilder.newInstance().hFill().hGrab().applyTo(composite);
        Text text = new Text(composite, SWT.BORDER | SWT.READ_ONLY);
        text.setText(shelveset.getName());
        GridDataBuilder.newInstance().hFill().hGrab().applyTo(text);

        copyLinkButton = new Button(composite, SWT.PUSH);
        copyLinkButton.setToolTipText(Messages.getString("ShelvesetDetailsDialog.CopyLinkButtonTooptip")); //$NON-NLS-1$
        copyLinkButton.setImage(imageHelper.getImage("images/htmleditor/link.gif")); //$NON-NLS-1$
        GridDataBuilder.newInstance().applyTo(copyLinkButton);

        copyLinkButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
                transferTypes = getTransferTypes();
                final Object[] transferData = new Object[transferTypes.length];
                for (int i = 0; i < transferTypes.length; i++) {
                    transferData[i] = getTransferData(transferTypes[i]);
                }
                getClipboard(dialogArea).setContents(transferData, transferTypes);
            }
        });

        label = new Label(dialogArea, SWT.NONE);
        label.setText(Messages.getString("ShelvesetDetailsDialog.OwnerLabelText")); //$NON-NLS-1$

        text = new Text(dialogArea, SWT.BORDER | SWT.READ_ONLY);
        text.setText(shelveset.getOwnerDisplayName());
        GridDataBuilder.newInstance().hGrab().hFill().applyTo(text);

        label = new Label(dialogArea, SWT.NONE);
        label.setText(Messages.getString("ShelvesetDetailsDialog.DateLabelText")); //$NON-NLS-1$

        text = new Text(dialogArea, SWT.BORDER | SWT.READ_ONLY);
        text.setText(dateFormat.format(shelveset.getCreationDate().getTime()));
        GridDataBuilder.newInstance().hGrab().hFill().applyTo(text);

        options = new CheckinControlOptions();
        options.setForDialog(true);
        options.setForShelveset(true);
        options.setSourceFilesCheckboxes(allowUnshelve);
        options.setSourceFilesCommentReadOnly(true);
        options.setPolicyEvaluationEnabled(false);
        options.setPolicyDisplayed(false);
        options.setWorkItemSearchEnabled(false);
        options.setWorkItemReadOnly(true);
        options.setWorkItemShowAction(true);
        options.setCheckinNotesReadOnly(true);
        options.setChangesText(Messages.getString("ShelvesetDetailsDialog.ShelvedChangesText")); //$NON-NLS-1$

        checkinControl = new CheckinControl(dialogArea, SWT.NONE, options);
        GridDataBuilder.newInstance().grab().fill().hSpan(layout).vIndent(getVerticalSpacing() * 2).applyTo(
            checkinControl);

        final ChangeItemProvider changeItemProvider = new QueryShelvesetChangeItemProvider(
            repository,
            UICommandExecutorFactory.newUICommandExecutor(getShell()),
            shelveset);

        checkinControl.setChangeItemProvider(changeItemProvider);

        if (options.isSourceFilesCheckboxes()) {
            checkinControl.getSourceFilesSubControl().getChangesTable().setCheckedChangeItems(
                changeItemProvider.getChangeItems());
        }

        dialogArea.addDisposeListener(new DisposeListener() {
            @Override
            public void widgetDisposed(final DisposeEvent e) {
                changeItemProvider.dispose();
                imageHelper.dispose();
            }
        });

        setCheckinControl(checkinControl, false);

        /* Set up source files tab */
        checkinControl.setRepository(repository);
        checkinControl.getSourceFilesSubControl().setComment(shelveset.getComment());

        /* Set up work items tab */
        final WorkItemClient workItemClient = repository.getWorkspace().getClient().getConnection().getWorkItemClient();

        if (workItemClient != null) {
            final WorkItemCheckinInfo[] workItems = shelveset.getWorkItemInfo(workItemClient);

            if (workItems != null && workItems.length > 0) {
                checkinControl.getWorkItemSubControl().getWorkItemTable().setWorkItems(workItems);
            }
        }

        /* Set up checkin notes tab */
        final CheckinNote checkinNote = shelveset.getCheckinNote();

        if (checkinNote != null) {
            checkinControl.getNotesSubControl().setCheckinNote(checkinNote);
        }

        createActions();
        contributeActions();

        if (allowUnshelve) {
            restoreButton = new Button(dialogArea, SWT.CHECK);
            restoreButton.setSelection(restoreData);
            restoreButton.setText(Messages.getString("ShelvesetDetailsDialog.RestoreButtonText")); //$NON-NLS-1$
            GridDataBuilder.newInstance().hGrab().hFill().hSpan(2).vIndent(getVerticalSpacing() * 2).applyTo(
                restoreButton);

            preserveButton = new Button(dialogArea, SWT.CHECK);
            preserveButton.setSelection(preserveShelveset);
            preserveButton.setText(Messages.getString("ShelvesetDetailsDialog.PreserveButtonText")); //$NON-NLS-1$
            GridDataBuilder.newInstance().hGrab().hFill().hSpan(2).applyTo(preserveButton);
        }
    }

    @Override
    protected void hookAfterButtonsCreated() {
        if (allowUnshelve) {
            final Button button = getButton(IDialogConstants.OK_ID);

            final SourceFilesCheckinControl sourceFilesSubControl = getCheckinControl().getSourceFilesSubControl();
            new ButtonValidatorBinding(button).bind(sourceFilesSubControl.getChangesTable().getCheckboxValidator());
        }
    }

    @Override
    protected void hookDialogAboutToClose() {
        if (allowUnshelve) {
            restoreData = restoreButton.getSelection();
            preserveShelveset = preserveButton.getSelection();
        }
    }

    private void contributeActions() {
        final SourceFilesCheckinControl sourceFilesSubControl = getCheckinControl().getSourceFilesSubControl();

        sourceFilesSubControl.getContextMenu().addMenuListener(new IMenuListener() {
            @Override
            public void menuAboutToShow(final IMenuManager manager) {
                final String groupId = StandardActionConstants.HOSTING_CONTROL_CONTRIBUTIONS;

                manager.appendToGroup(groupId, viewSubMenu);
                manager.appendToGroup(groupId, new Separator());
                manager.appendToGroup(groupId, compareSubMenu);
            }
        });

        sourceFilesSubControl.getChangesTable().addDoubleClickListener(new IDoubleClickListener() {
            @Override
            public void doubleClick(final DoubleClickEvent event) {
                if (viewAction.isEnabled()) {
                    viewAction.run();
                } else if (viewUnmodifiedAction.isEnabled()) {
                    viewUnmodifiedAction.run();
                }
            }
        });

        final IContributionManager contributionManager = sourceFilesSubControl.getContributionManager();

        contributionManager.add(new Separator());
        contributionManager.add(compareToolbarAction);
        contributionManager.add(viewToolbarAction);

        contributionManager.update(false);
    }

    private void createActions() {
        final SourceFilesCheckinControl sourceFilesSubControl = getCheckinControl().getSourceFilesSubControl();
        final ISelectionProvider sourceFilesSelectionProvider = sourceFilesSubControl.getSelectionProvider();

        viewSubMenu = new MenuManager(Messages.getString("ShelvesetDetailsDialog.ViewSubMenuText")); //$NON-NLS-1$

        viewToolbarAction = new ToolbarPulldownAction();
        viewToolbarAction.setImageDescriptor(
            PlatformUI.getWorkbench().getSharedImages().getImageDescriptor(ISharedImages.IMG_OBJ_FOLDER));

        viewAction =
            new ViewPendingChangeAction(sourceFilesSelectionProvider, repository, true, ViewVersionType.SHELVED);
        viewSubMenu.add(viewAction);
        viewToolbarAction.addSubAction(viewAction);
        viewToolbarAction.setDefaultSubAction(viewAction);

        viewUnmodifiedAction =
            new ViewPendingChangeAction(sourceFilesSelectionProvider, repository, true, ViewVersionType.UNMODIFIED);
        viewSubMenu.add(viewUnmodifiedAction);
        viewToolbarAction.addSubAction(viewUnmodifiedAction);

        viewLatestAction =
            new ViewPendingChangeAction(sourceFilesSelectionProvider, repository, true, ViewVersionType.LATEST);
        viewSubMenu.add(viewLatestAction);
        viewToolbarAction.addSubAction(viewLatestAction);

        compareSubMenu = new MenuManager(Messages.getString("ShelvesetDetailsDialog.CompareSubMenuText")); //$NON-NLS-1$

        compareToolbarAction = new ToolbarPulldownAction();
        compareToolbarAction.setImageDescriptor(TFSCommonUIImages.getImageDescriptor(TFSCommonUIImages.IMG_COMPARE));

        compareWithUnmodifiedAction = new CompareShelvedChangeWithUnmodifiedVersionAction(
            sourceFilesSelectionProvider,
            repository,
            CompareUIType.DIALOG,
            shelveset.getName(),
            shelveset.getOwnerName(),
            getShell());
        compareSubMenu.add(compareWithUnmodifiedAction);
        compareToolbarAction.addSubAction(compareWithUnmodifiedAction);

        compareWithLatestAction = new CompareShelvedChangeWithLatestVersionAction(
            sourceFilesSelectionProvider,
            repository,
            CompareUIType.DIALOG,
            shelveset.getName(),
            shelveset.getOwnerName(),
            getShell());
        compareSubMenu.add(compareWithLatestAction);
        compareToolbarAction.addSubAction(compareWithLatestAction);
        compareToolbarAction.setDefaultSubAction(compareWithLatestAction);
    }

    @Override
    protected String getBaseTitle() {
        return Messages.getString("ShelvesetDetailsDialog.DialogBaseTitle"); //$NON-NLS-1$
    }

    /**
     * @return the change items the user checked, or <code>null</code> to
     *         indicate all items were checked (saves bandwidth when sending as
     *         part of the unshelve web method call)
     */
    public ChangeItem[] getCheckedChangeItems() {
        if (!options.isSourceFilesCheckboxes()) {
            return null;
        }

        if (checkinControl.getSourceFilesSubControl().getChangesTable().getCheckedProjectsCount() == checkinControl.getSourceFilesSubControl().getChangesTable().getCount()) {
            return null;
        }

        return checkinControl.getSourceFilesSubControl().getChangesTable().getCheckedChangeItems();
    }

    /**
     * Generate hyperlink for this shelveset
     *
     * @param transferType
     * @return
     */
    private Object getTransferData(final Transfer transferType) {
        if (tswaHyperlinkBuilder == null) {
            tswaHyperlinkBuilder = new TSWAHyperlinkBuilder(repository.getVersionControlClient().getConnection());
        }
        final StringBuffer sb = new StringBuffer();
        if (transferType.getClass().getName().equals("org.eclipse.swt.dnd.HTMLTransfer") //$NON-NLS-1$
            && tswaHyperlinkBuilder != null) {
            // Create HTML to copy
            sb.append("<a href=\""); //$NON-NLS-1$
            sb.append(
                tswaHyperlinkBuilder.getShelvesetDetailsURL(shelveset.getName(), shelveset.getOwnerName()).toString());
            sb.append("\">"); //$NON-NLS-1$
            sb.append(shelveset.getName());
            sb.append(";"); //$NON-NLS-1$
            sb.append(shelveset.getOwnerName());
            sb.append("</a>"); //$NON-NLS-1$
        } else {
            // Assume text transfer type
            sb.append(shelveset.getName());
            sb.append(";"); //$NON-NLS-1$
            sb.append(shelveset.getOwnerName());
        }
        return sb.toString();
    }

    public final Clipboard getClipboard(final Composite composite) {
        if (clipboard == null) {
            clipboard = new Clipboard(composite.getDisplay());
        }

        return clipboard;
    }

    /**
     * Get transferTypes, if null, create them
     *
     * @return
     */
    private Transfer[] getTransferTypes() {
        if (transferTypes == null) {
            transferTypes = new Transfer[] {
                TextTransfer.getInstance(),
                HTMLTransfer.getInstance()
            };
        }

        return transferTypes;
    }
}
