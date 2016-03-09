// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.dialogs.vc;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import com.microsoft.tfs.client.common.commands.vc.QueryItemsCommand;
import com.microsoft.tfs.client.common.framework.command.CommandExecutor;
import com.microsoft.tfs.client.common.repository.TFSRepository;
import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.TFSCommonUIClientPlugin;
import com.microsoft.tfs.client.common.ui.controls.generic.ProgressMonitorControl;
import com.microsoft.tfs.client.common.ui.controls.vc.LabelItemTable;
import com.microsoft.tfs.client.common.ui.controls.vc.LabelItemTable.LabelItem;
import com.microsoft.tfs.client.common.ui.controls.vc.LabelItemTable.LabelItemStatus;
import com.microsoft.tfs.client.common.ui.controls.vc.VersionPickerControl.VersionDescription;
import com.microsoft.tfs.client.common.ui.framework.dialog.ExtendedButtonDialog;
import com.microsoft.tfs.client.common.ui.framework.layout.GridDataBuilder;
import com.microsoft.tfs.client.common.ui.framework.sizing.ControlSize;
import com.microsoft.tfs.client.common.ui.vc.serveritem.ServerItemType;
import com.microsoft.tfs.client.common.ui.vc.serveritem.VersionedItemSource;
import com.microsoft.tfs.client.common.util.DateHelper;
import com.microsoft.tfs.client.common.vc.VersionSpecHelper;
import com.microsoft.tfs.core.clients.versioncontrol.GetItemsOptions;
import com.microsoft.tfs.core.clients.versioncontrol.VersionControlConstants;
import com.microsoft.tfs.core.clients.versioncontrol.path.ServerPath;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.DeletedState;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Item;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.ItemSet;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.ItemType;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.RecursionType;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.VersionControlLabel;
import com.microsoft.tfs.core.clients.versioncontrol.specs.ItemSpec;
import com.microsoft.tfs.core.clients.versioncontrol.specs.LabelItemSpec;
import com.microsoft.tfs.core.clients.versioncontrol.specs.version.ChangesetVersionSpec;
import com.microsoft.tfs.core.clients.versioncontrol.specs.version.DateVersionSpec;
import com.microsoft.tfs.core.clients.versioncontrol.specs.version.LatestVersionSpec;
import com.microsoft.tfs.core.clients.versioncontrol.specs.version.VersionSpec;
import com.microsoft.tfs.core.exceptions.TECoreException;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.CollatorFactory;

public class EditLabelDialog extends ExtendedButtonDialog {
    private static final Log log = LogFactory.getLog(EditLabelDialog.class);

    private final TFSRepository repository;
    private final VersionControlLabel initialLabel;
    private String initialName;
    private String initialComment;
    private final String initialServerPath;
    private final RecursionType initialRecursionType;
    private final VersionSpec initialVersionSpec;

    private final LabelContentsTree labelContentsTree = new LabelContentsTree();

    private Text nameText;
    private Text commentText;
    private LabelItemTable labelItemsTable;
    private ProgressMonitorControl progressMonitorControl;

    private boolean ignoreCommentModifyEvents = false;

    private boolean labelModified = false;
    private EditLabelResults editLabelResults;

    private final static int BUTTON_ADD_ID = IDialogConstants.CLIENT_ID + 1;
    private final static int BUTTON_REMOVE_ID = IDialogConstants.CLIENT_ID + 2;

    /**
     * Constructs an edit label dialog given an existing
     * {@link VersionControlLabel}.
     *
     * @param parentShell
     *        the shell to parent from (not null)
     * @param repository
     *        the repository to edit the label in (not null)
     * @param initialLabel
     *        the version control label to edit (not null)
     */
    public EditLabelDialog(
        final Shell parentShell,
        final TFSRepository repository,
        final VersionControlLabel initialLabel) {
        this(parentShell, repository, initialLabel, null, null, null);

        Check.notNull(repository, "repository"); //$NON-NLS-1$
        Check.notNull(initialLabel, "initialLabel"); //$NON-NLS-1$
    }

    /**
     * Constructs an edit label dialog with no existing label entries, suitable
     * for creating a new label. The given initialServerPath will be used as the
     * default server path for browsing new label items.
     *
     * @param parentShell
     *        the shell to parent from (not null)
     * @param repository
     *        the repository to edit the label in (not null)
     * @param initialServerPath
     *        the initial server path to use when browsing new label items (not
     *        null)
     */
    public EditLabelDialog(final Shell parentShell, final TFSRepository repository, final String initialServerPath) {
        this(parentShell, repository, null, initialServerPath, null, null);

        Check.notNull(repository, "repository"); //$NON-NLS-1$
        Check.notNull(initialServerPath, "initialServerPath"); //$NON-NLS-1$
    }

    /**
     * Constructs an edit label dialog and populates label items from a query on
     * the given server path at the given version spec. Suitable for creating
     * new labels after prompting for a first label entry. The query will occur
     * when the dialog is opened.
     *
     * @param parentShell
     *        the shell to parent from (not null)
     * @param repository
     *        the repository to edit the label in (not null)
     * @param initialServerPath
     *        the initial server path used to populate the label (not null)
     * @param recursionType
     *        the recursion type to query the given server path on (not null)
     * @param versionSpec
     *        the version spec to add the given server item with (not null)
     */
    public EditLabelDialog(
        final Shell parentShell,
        final TFSRepository repository,
        final String initialServerPath,
        final RecursionType recursionType,
        final VersionSpec versionSpec) {
        this(parentShell, repository, null, initialServerPath, recursionType, versionSpec);

        Check.notNull(repository, "repository"); //$NON-NLS-1$
        Check.notNull(initialServerPath, "initialServerPath"); //$NON-NLS-1$
        Check.notNull(recursionType, "recursionType"); //$NON-NLS-1$
        Check.notNull(versionSpec, "versionSpec"); //$NON-NLS-1$
    }

    private EditLabelDialog(
        final Shell parentShell,
        final TFSRepository repository,
        final VersionControlLabel initialLabel,
        final String initialServerPath,
        final RecursionType recursionType,
        final VersionSpec versionSpec) {
        super(parentShell);

        Check.notNull(repository, "repository"); //$NON-NLS-1$

        this.repository = repository;
        this.initialLabel = initialLabel;
        this.initialServerPath = initialServerPath;
        initialRecursionType = recursionType;
        initialVersionSpec = versionSpec;

        addExtendedButtonDescription(BUTTON_ADD_ID, Messages.getString("EditLabelDialog.AddButtonText"), false); //$NON-NLS-1$
        addExtendedButtonDescription(BUTTON_REMOVE_ID, Messages.getString("EditLabelDialog.RemoveButtonText"), false); //$NON-NLS-1$

        setOptionPersistGeometry(false);
    }

    @Override
    protected String provideDialogTitle() {
        if (initialLabel == null) {
            return Messages.getString("EditLabelDialog.NewLabelDialogTitle"); //$NON-NLS-1$
        } else {
            String scope = ServerPath.getTeamProjectName(initialLabel.getScope());

            if (scope == null) {
                scope = initialLabel.getScope();
            }

            final String messageFormat = Messages.getString("EditLabelDialog.DialogTitleFormat"); //$NON-NLS-1$
            return MessageFormat.format(messageFormat, scope);
        }
    }

    public void setName(final String initialName) {
        this.initialName = initialName;
    }

    public void setComment(final String initialComment) {
        this.initialComment = initialComment;
    }

    @Override
    protected void hookAddToDialogArea(final Composite dialogArea) {
        super.hookAddToDialogArea(dialogArea);

        final GridLayout layout = new GridLayout(2, false);
        layout.marginWidth = getHorizontalMargin();
        layout.marginHeight = getVerticalMargin();
        layout.horizontalSpacing = getHorizontalSpacing();
        layout.verticalSpacing = getVerticalSpacing();
        dialogArea.setLayout(layout);

        final Label nameLabel = new Label(dialogArea, SWT.NONE);
        nameLabel.setText(Messages.getString("EditLabelDialog.NameLabelText")); //$NON-NLS-1$

        nameText = new Text(dialogArea, SWT.BORDER);
        GridDataBuilder.newInstance().hGrab().hFill().applyTo(nameText);
        nameText.setTextLimit(ApplyLabelDialog.NAME_MAX_LENGTH);

        final Label commentLabel = new Label(dialogArea, SWT.NONE);
        commentLabel.setText(Messages.getString("EditLabelDialog.CommentLabelText")); //$NON-NLS-1$
        GridDataBuilder.newInstance().hSpan(2).vIndent(getVerticalSpacing()).applyTo(commentLabel);

        commentText = new Text(dialogArea, SWT.MULTI | SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL);
        GridDataBuilder.newInstance().hSpan(2).hGrab().hFill().applyTo(commentText);
        ControlSize.setCharHeightHint(commentText, 5);
        ControlSize.setCharWidthHint(commentText, 60);
        commentText.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(final ModifyEvent e) {
                if (ignoreCommentModifyEvents == false) {
                    labelModified = true;
                }
            }
        });
        commentText.setTextLimit(ApplyLabelDialog.COMMENT_MAX_LENGTH);

        final Label itemsLabel = new Label(dialogArea, SWT.NONE);
        itemsLabel.setText(Messages.getString("EditLabelDialog.ItemsLabelText")); //$NON-NLS-1$
        GridDataBuilder.newInstance().hSpan(2).applyTo(itemsLabel);

        labelItemsTable = new LabelItemTable(dialogArea, SWT.MULTI | SWT.FULL_SELECTION);
        GridDataBuilder.newInstance().hSpan(2).hGrab().hFill().vGrab().vFill().hCHint(labelItemsTable, 10).wHint(
            getMinimumMessageAreaWidth()).applyTo(labelItemsTable);

        progressMonitorControl = new ProgressMonitorControl(dialogArea, SWT.NONE);
        GridDataBuilder.newInstance().hSpan(2).hGrab().hFill().vIndent(getVerticalSpacing()).applyTo(
            progressMonitorControl);

        if (initialLabel != null) {
            nameText.setText(initialLabel.getName());
            nameText.setEditable(false);
            nameText.setEnabled(false);

            ignoreCommentModifyEvents = true;
            commentText.setText(initialLabel.getComment());
            ignoreCommentModifyEvents = false;
        }

        /*
         * When we come through the ApplyLabelDialog, it checks to ensure that
         * the label does not already exist. Thus the name is not editable.
         */
        if (initialName != null) {
            nameText.setText(initialName);
            nameText.setEditable(false);
        }

        if (initialComment != null) {
            commentText.setText(initialComment);
        }

        nameText.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(final ModifyEvent e) {
                getButton(IDialogConstants.OK_ID).setEnabled(nameText.getText().length() > 0);
            }
        });

        labelItemsTable.addSelectionChangedListener(new ISelectionChangedListener() {
            @Override
            public void selectionChanged(final SelectionChangedEvent event) {
                getButton(BUTTON_REMOVE_ID).setEnabled(!event.getSelection().isEmpty());
            }
        });
    }

    @Override
    protected void hookCustomButtonPressed(final int buttonId) {
        if (buttonId == BUTTON_ADD_ID) {
            addPressed();
        } else if (buttonId == BUTTON_REMOVE_ID) {
            removePressed();
        }
    }

    @Override
    protected void hookDialogIsOpen() {
        if (!(initialLabel != null
            || (initialServerPath != null && initialRecursionType != null && initialVersionSpec != null))) {
            getButton(IDialogConstants.OK_ID).setEnabled(nameText.getText().length() > 0);
            return;
        }

        getButton(BUTTON_ADD_ID).setEnabled(false);
        getButton(BUTTON_REMOVE_ID).setEnabled(false);
        nameText.setEnabled(false);
        commentText.setEnabled(false);
        labelItemsTable.setEnabled(false);
        getButton(IDialogConstants.OK_ID).setEnabled(false);

        Runnable runnable;

        if (initialLabel != null) {
            runnable = new Runnable() {
                @Override
                public void run() {
                    addFromLabel(initialLabel);
                }
            };
        } else if (initialServerPath != null && initialRecursionType != null && initialVersionSpec != null) {
            labelModified = true;

            runnable = new Runnable() {
                @Override
                public void run() {
                    addFromQuery(initialServerPath, initialRecursionType, initialVersionSpec, new Runnable() {
                        @Override
                        public void run() {
                            if (initialLabel == null && nameText.getText().length() == 0) {
                                nameText.setFocus();
                            }
                        }
                    });
                }
            };
        } else {
            return;
        }

        new Thread(runnable).start();
    }

    private void addPressed() {
        final ServerItemVersionDialog chooseVersionDialog =
            new ServerItemVersionDialog(
                getShell(),
                repository,
                Messages.getString("EditLabelDialog.SelectItemToLabel"), //$NON-NLS-1$
                initialServerPath,
                new VersionedItemSource(repository, LatestVersionSpec.INSTANCE),
                ServerItemType.ALL,
                LatestVersionSpec.INSTANCE);

        if (chooseVersionDialog.open() != IDialogConstants.OK_ID) {
            return;
        }

        labelModified = true;

        final ItemSpec addedItemSpec = chooseVersionDialog.getItemSpec();
        final VersionSpec addedVersionSpec = chooseVersionDialog.getVersionSpec();

        if (addedVersionSpec == null
            || (addedVersionSpec instanceof ChangesetVersionSpec
                && ((ChangesetVersionSpec) addedVersionSpec).getChangeset() == 0)) {
            String errorMessage = Messages.getString("EditLabelDialog.InvalidVersionDialogText"); //$NON-NLS-1$

            if (chooseVersionDialog.getVersionType() == VersionDescription.DATE) {
                errorMessage = Messages.getString("EditLabelDialog.InvalidDateDialogText"); //$NON-NLS-1$
            } else if (chooseVersionDialog.getVersionType() == VersionDescription.CHANGESET) {
                errorMessage = Messages.getString("EditLabelDialog.InvalidChangesetDialogText"); //$NON-NLS-1$
            }

            MessageDialog.openError(
                getShell(),
                Messages.getString("EditLabelDialog.InvalidVersionDialogTitle"), //$NON-NLS-1$
                errorMessage);
            return;
        }

        getButton(BUTTON_ADD_ID).setEnabled(false);
        getButton(BUTTON_REMOVE_ID).setEnabled(false);
        nameText.setEnabled(false);
        commentText.setEnabled(false);
        labelItemsTable.setEnabled(false);
        getButton(IDialogConstants.OK_ID).setEnabled(false);

        new Thread() {
            @Override
            public void run() {
                addFromQuery(addedItemSpec.getItem(), addedItemSpec.getRecursionType(), addedVersionSpec, null);
            }
        }.start();
    }

    private void addFromLabel(final VersionControlLabel label) {
        final List<LabelItem> labelItemList = new ArrayList<LabelItem>();

        final IProgressMonitor progressMonitor = progressMonitorControl.getProgressMonitor();

        final Shell shell = getShell();

        if (shell == null) {
            return;
        }

        final Display display = shell.getDisplay();

        display.syncExec(new Runnable() {
            @Override
            public void run() {
                progressMonitor.beginTask(
                    Messages.getString("EditLabelDialog.ProgressBuildLabeling"), //$NON-NLS-1$
                    IProgressMonitor.UNKNOWN);
            }
        });

        try {
            synchronized (labelContentsTree) {
                final Item[] items = label.getItems();

                for (int i = 0; i < items.length; i++) {
                    final LabelItem labelItem = new LabelItem(
                        items[i].getServerItem(),
                        RecursionType.NONE,
                        new ChangesetVersionSpec(items[i].getChangeSetID()),
                        items[i].getDeletionID(),
                        LabelItemStatus.EXISTS);

                    labelContentsTree.put(labelItem);
                    labelItemList.add(labelItem);
                }
            }

            final LabelItem[] items = labelItemList.toArray(new LabelItem[labelItemList.size()]);

            display.syncExec(new Runnable() {
                @Override
                public void run() {
                    if (labelItemsTable != null && !labelItemsTable.isDisposed()) {
                        labelItemsTable.setItems(items);
                    }
                }
            });

        } finally {
            display.syncExec(new Runnable() {
                @Override
                public void run() {
                    progressMonitor.done();
                }
            });
        }

        addFinished(Status.OK_STATUS, null);
    }

    private void addFromQuery(
        final String serverPath,
        final RecursionType recursionType,
        final VersionSpec versionSpec,
        final Runnable finishedHandler) {
        final ItemSpec[] queryItemSpecs = new ItemSpec[] {
            new ItemSpec(serverPath, recursionType)
        };

        final QueryItemsCommand queryCommand = new QueryItemsCommand(
            repository,
            queryItemSpecs,
            versionSpec,
            DeletedState.ANY,
            ItemType.ANY,
            GetItemsOptions.UNSORTED);

        final IProgressMonitor progressMonitor = progressMonitorControl.getProgressMonitor();

        final Shell shell = getShell();

        if (shell == null) {
            return;
        }

        final Display display = shell.getDisplay();

        display.syncExec(new Runnable() {
            @Override
            public void run() {
                final String messageFormat = Messages.getString("EditLabelDialog.AddingLabelProgressFormat"); //$NON-NLS-1$
                final String message = MessageFormat.format(messageFormat, serverPath);

                progressMonitor.beginTask(message, IProgressMonitor.UNKNOWN);
                progressMonitor.subTask(Messages.getString("EditLabelDialog.ProgressSubtask")); //$NON-NLS-1$
            }
        });

        try {
            IStatus queryStatus = new CommandExecutor().execute(queryCommand);

            if (!queryStatus.isOK()) {
                if (queryStatus.getException() != null
                    && queryStatus.getException() instanceof TECoreException
                    && queryStatus.getException().getMessage().startsWith("TF14021: ")) //$NON-NLS-1$
                {
                    final Date date = ((DateVersionSpec) versionSpec).getDate().getTime();
                    final String messageFormat = Messages.getString("EditLabelDialog.InvalidDateFormat"); //$NON-NLS-1$
                    final String message =
                        MessageFormat.format(messageFormat, DateHelper.getDefaultDateTimeFormat().format(date));

                    queryStatus = new Status(IStatus.ERROR, TFSCommonUIClientPlugin.PLUGIN_ID, 0, message, null);
                }

                addFinished(queryStatus, finishedHandler);
                return;
            }

            final ItemSet[] itemSet = queryCommand.getItemSets();

            if (itemSet == null || itemSet.length != 1 || itemSet[0].getItems().length < 1) {
                final String messageFormat = Messages.getString("EditLabelDialog.ServerDoesNotContainItemFormat"); //$NON-NLS-1$
                final String message = MessageFormat.format(
                    messageFormat,
                    serverPath,
                    VersionSpecHelper.getVersionSpecDescription(versionSpec));

                addFinished(
                    new Status(IStatus.ERROR, TFSCommonUIClientPlugin.PLUGIN_ID, 0, message, null),
                    finishedHandler);

                return;
            }

            display.syncExec(new Runnable() {
                @Override
                public void run() {
                    progressMonitor.subTask(Messages.getString("EditLabelDialog.ProgressSubtaskLabing")); //$NON-NLS-1$
                }
            });

            final LabelItem[] labelItems;

            synchronized (labelContentsTree) {
                final Item[] items = itemSet[0].getItems();

                for (int i = 0; i < items.length; i++) {
                    final String itemServerItem = items[i].getServerItem();

                    /*
                     * The first element return from query items is the item we
                     * queried for -- which we are actually adding to the label.
                     * Any additional items are children, and are "implicit"
                     * adds, merely drawn in the table control but not sent to
                     * the server.
                     */
                    final LabelItemStatus itemStatus = (i == 0) ? LabelItemStatus.ADD : LabelItemStatus.ADD_IMPLICIT;
                    final RecursionType itemRecursionType =
                        (items[i].getItemType() == ItemType.FOLDER) ? RecursionType.FULL : RecursionType.NONE;
                    final LabelItem labelItem = new LabelItem(
                        itemServerItem,
                        itemRecursionType,
                        versionSpec,
                        items[i].getDeletionID(),
                        itemStatus);

                    labelContentsTree.put(labelItem);
                }

                labelItems = labelContentsTree.getLabelItems();
            }

            display.syncExec(new Runnable() {
                @Override
                public void run() {
                    if (labelItemsTable != null && !labelItemsTable.isDisposed()) {
                        labelItemsTable.setItems(labelItems);
                    }
                }
            });
        } finally {
            display.syncExec(new Runnable() {
                @Override
                public void run() {
                    progressMonitor.done();
                }
            });
        }

        addFinished(Status.OK_STATUS, finishedHandler);

        return;
    }

    private void addFinished(final IStatus status, final Runnable finishedHandler) {
        final Shell shell = getShell();

        if (shell == null) {
            return;
        }

        shell.getDisplay().syncExec(new Runnable() {
            @Override
            public void run() {
                if (!status.isOK()) {
                    ErrorDialog.openError(
                        shell,
                        Messages.getString("EditLabelDialog.CantQueryDialogTitle"), //$NON-NLS-1$
                        null,
                        status);
                }

                if (nameText.isDisposed()) {
                    return;
                }

                nameText.setEnabled(initialLabel == null);
                commentText.setEnabled(true);
                labelItemsTable.setEnabled(true);
                getButton(BUTTON_ADD_ID).setEnabled(true);
                getButton(BUTTON_REMOVE_ID).setEnabled(!labelItemsTable.getSelection().isEmpty());
                getButton(IDialogConstants.OK_ID).setEnabled(nameText.getText().length() > 0);

                if (finishedHandler != null) {
                    finishedHandler.run();
                }
            }
        });
    }

    private void removePressed() {
        final LabelItem[] selectedItems = labelItemsTable.getSelectedItems();

        if (selectedItems.length == 0) {
            return;
        }

        labelModified = true;

        final LabelContentsTreeTransaction removeTransaction = new LabelContentsTreeTransaction();

        synchronized (labelContentsTree) {
            for (int i = 0; i < selectedItems.length; i++) {
                labelContentsTree.remove(removeTransaction, selectedItems[i].getServerItem());
            }
        }

        final IStatus removeStatus = removeTransaction.getStatus();

        if (!removeStatus.isOK()) {
            ErrorDialog.openError(
                getShell(),
                Messages.getString("EditLabelDialog.CantRemoveDialogTitle"), //$NON-NLS-1$
                null,
                removeStatus);
        }

        final Shell shell = getShell();

        BusyIndicator.showWhile(shell.getDisplay(), new Runnable() {
            @Override
            public void run() {
                shell.getDisplay().syncExec(new Runnable() {
                    @Override
                    public void run() {
                        labelItemsTable.removeItems(removeTransaction.getAffectedLabelItems());
                    }
                });
            }
        });
    }

    @Override
    protected void okPressed() {
        if (!labelModified) {
            editLabelResults = new EditLabelResults(null, null, null);
            setReturnCode(IDialogConstants.CANCEL_ID);
            close();
            return;
        }

        if (initialLabel == null) {
            final String labelName = nameText.getText().trim();

            if (labelName.length() == 0) {
                final String title = Messages.getString("EditLabelDialog.InvalidNameDialogTitle"); //$NON-NLS-1$
                final String message = Messages.getString("EditLabelDialog.InvalidNameDialogText"); //$NON-NLS-1$

                MessageDialog.openError(getShell(), title, message);
                return;
            }

            if (labelName.length() >= ApplyLabelDialog.NAME_MAX_LENGTH) {
                final String title = Messages.getString("EditLabelDialog.NametooLongDialogTitle"); //$NON-NLS-1$
                final String messageFormat = Messages.getString("EditLabelDialog.NameTooLongFormat"); //$NON-NLS-1$
                final String message = MessageFormat.format(messageFormat, (ApplyLabelDialog.NAME_MAX_LENGTH + 1));

                MessageDialog.openError(getShell(), title, message);
                return;
            }

            for (int i = 0; i < ApplyLabelDialog.INVALID_NAME_CHARS.length; i++) {
                if (labelName.indexOf(ApplyLabelDialog.INVALID_NAME_CHARS[i]) >= 0) {
                    final String title = Messages.getString("EditLabelDialog.InvalidNameDialogTitle"); //$NON-NLS-1$
                    final String messageFormat = Messages.getString("EditLabelDialog.InvalidCharInLabelFormat"); //$NON-NLS-1$
                    final String message = MessageFormat.format(messageFormat, ApplyLabelDialog.INVALID_NAME_CHARS[i]);

                    MessageDialog.openError(getShell(), title, message);
                    return;
                }
            }
        }

        if (commentText.getText().trim().length() > ApplyLabelDialog.COMMENT_MAX_LENGTH) {
            final String title = Messages.getString("EditLabelDialog.CommentTooLongDialogTitle"); //$NON-NLS-1$
            final String messageFormat = Messages.getString("EditLabelDialog.CommentTooLongFormat"); //$NON-NLS-1$
            final String message = MessageFormat.format(messageFormat, ApplyLabelDialog.COMMENT_MAX_LENGTH);

            MessageDialog.openError(getShell(), title, message);
            return;
        }

        getButton(BUTTON_ADD_ID).setEnabled(false);
        getButton(BUTTON_REMOVE_ID).setEnabled(false);
        nameText.setEnabled(false);
        commentText.setEnabled(false);
        labelItemsTable.setEnabled(false);
        getButton(IDialogConstants.OK_ID).setEnabled(false);

        final IProgressMonitor progressMonitor = progressMonitorControl.getProgressMonitor();
        progressMonitor.beginTask(
            Messages.getString("EditLabelDialog.ProgressAnalyzingLabel"), //$NON-NLS-1$
            IProgressMonitor.UNKNOWN);

        new Thread(new Runnable() {
            @Override
            public void run() {
                final boolean isDelete;
                final ItemSpec[] deletes;
                final LabelItemSpec[] adds;

                synchronized (labelContentsTree) {
                    final LabelContentsTreeDelta delta = labelContentsTree.getDelta();

                    isDelete = (initialLabel != null && delta.getExistsCount() == 0 && delta.getAdds().length == 0);
                    deletes = delta.getDeletes();
                    adds = delta.getAdds();
                }

                final Shell shell = getShell();

                if (shell == null) {
                    return;
                }

                shell.getDisplay().syncExec(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            if (isDelete
                                && !MessageDialog.openQuestion(
                                    getShell(),
                                    Messages.getString("EditLabelDialog.RemoveLabelDialogTitle"), //$NON-NLS-1$
                                    Messages.getString("EditLabelDialog.RemoveLabelDialogText"))) //$NON-NLS-1$
                            {
                                progressMonitor.done();

                                nameText.setEnabled(initialLabel == null);
                                commentText.setEnabled(true);
                                labelItemsTable.setEnabled(true);
                                getButton(BUTTON_ADD_ID).setEnabled(true);
                                getButton(BUTTON_REMOVE_ID).setEnabled(!labelItemsTable.getSelection().isEmpty());
                                getButton(IDialogConstants.OK_ID).setEnabled(nameText.getText().length() > 0);

                                return;
                            }

                            VersionControlLabel newLabel;

                            if (isDelete) {
                                newLabel = null;
                            } else {
                                final String name =
                                    initialLabel != null ? initialLabel.getName() : nameText.getText().trim();
                                final String owner = initialLabel != null ? initialLabel.getOwner()
                                    : VersionControlConstants.AUTHENTICATED_USER;
                                final String ownerDisplayName =
                                    initialLabel != null ? initialLabel.getOwnerDisplayName() : null;
                                final String scope = initialLabel != null ? initialLabel.getScope() : null;
                                final String comment = commentText.getText().trim();

                                newLabel = new VersionControlLabel(name, owner, ownerDisplayName, scope, comment);
                            }

                            editLabelResults = new EditLabelResults(newLabel, deletes, adds);

                            EditLabelDialog.super.okPressed();
                        } catch (final Throwable e) {
                            progressMonitor.done();

                            MessageDialog.openError(
                                getShell(),
                                Messages.getString("EditLabelDialog.LabelErrorDialogTitle"), //$NON-NLS-1$
                                e.getMessage());

                            nameText.setEnabled(initialLabel == null);
                            commentText.setEnabled(true);
                            labelItemsTable.setEnabled(true);
                            getButton(BUTTON_ADD_ID).setEnabled(true);
                            getButton(BUTTON_REMOVE_ID).setEnabled(!labelItemsTable.getSelection().isEmpty());
                            getButton(IDialogConstants.OK_ID).setEnabled(nameText.getText().length() > 0);

                            return;
                        }
                    }
                });
            }
        }).start();
    }

    public EditLabelResults getEditLabelResults() {
        return editLabelResults;
    }

    public static class EditLabelResults {
        private final VersionControlLabel label;
        private final ItemSpec[] deletes;
        private final LabelItemSpec[] adds;

        public EditLabelResults(final VersionControlLabel label, final ItemSpec[] deletes, final LabelItemSpec[] adds) {
            this.label = label;
            this.deletes = deletes;
            this.adds = adds;
        }

        public VersionControlLabel getLabel() {
            return label;
        }

        public ItemSpec[] getDeletes() {
            return deletes;
        }

        public LabelItemSpec[] getAdds() {
            return adds;
        }
    }

    private static class LabelContentsTree {
        /**
         * Case-insensitive server item string maps to label contents tree node.
         */
        private final Map<String, LabelContentsTreeNode> labelItemMap =
            new TreeMap<String, LabelContentsTreeNode>(CollatorFactory.getCaseInsensitiveCollator());

        public LabelContentsTree() {
        }

        public void put(final LabelItem labelItem) {
            final LabelContentsTreeNode node = getOrCreateNode(labelItem.getServerItem());
            node.setLabelItem(labelItem);
        }

        private LabelContentsTreeNode getOrCreateNode(final String serverItem) {
            /* See if there's an existing entry in the tree */
            LabelContentsTreeNode node = labelItemMap.get(serverItem);

            if (node == null) {
                /* No existing node, create a placeholder. */
                node = new LabelContentsTreeNode(
                    new LabelItem(
                        serverItem,
                        RecursionType.NONE,
                        new ChangesetVersionSpec(0),
                        0,
                        LabelItemStatus.NONE));

                labelItemMap.put(serverItem, node);

                /* If we're not the root, find our parent */
                if (!ServerPath.equals(ServerPath.ROOT, serverItem)) {
                    String parentPath = ServerPath.getParent(serverItem);

                    if (parentPath.equals("$")) //$NON-NLS-1$
                    {
                        parentPath = ServerPath.ROOT;
                    }

                    final LabelContentsTreeNode parentNode = getOrCreateNode(parentPath);

                    parentNode.addChild(node);
                }
            }

            return node;
        }

        public LabelItem[] getLabelItems() {
            final List<LabelItem> labelItemList = new ArrayList<LabelItem>();

            for (final Iterator<LabelContentsTreeNode> i = labelItemMap.values().iterator(); i.hasNext();) {
                final LabelItem labelItem = i.next().getLabelItem();
                final LabelItemStatus status = labelItem.getItemStatus();

                if (status == LabelItemStatus.ADD
                    || status == LabelItemStatus.ADD_IMPLICIT
                    || status == LabelItemStatus.EXISTS) {
                    labelItemList.add(labelItem);
                }
            }

            return labelItemList.toArray(new LabelItem[labelItemList.size()]);
        }

        public void remove(final LabelContentsTreeTransaction transaction, final String serverItem) {
            final LabelContentsTreeNode removeNode = labelItemMap.get(serverItem);

            if (removeNode == null) {
                String messageFormat = "The item {0} does not exist in the label tree."; //$NON-NLS-1$
                String message = MessageFormat.format(messageFormat, serverItem);
                log.error(message);

                messageFormat = Messages.getString("EditLabelDialog.ItemNotInLabelFormat"); //$NON-NLS-1$
                message = MessageFormat.format(messageFormat, serverItem);
                transaction.addError(message);
                return;
            }

            /*
             * This was previously removed as part of a recursive folder delete.
             * Ignore.
             */
            if (transaction.isAffectedNode(removeNode)) {
                return;
            }

            LabelItemStatus newStatus;

            /*
             * This file or folder was explicitly added to this label by the
             * user. We should simply not add this.
             */
            if (removeNode.getLabelItem().getItemStatus() == LabelItemStatus.ADD) {
                newStatus = LabelItemStatus.NONE;
            }
            /*
             * This file or folder was implicitly added due to recursion. We
             * should now exclude this.
             */
            else if (removeNode.getLabelItem().getItemStatus() == LabelItemStatus.ADD_IMPLICIT) {
                newStatus = LabelItemStatus.EXCLUDE;
            }
            /*
             * This file or folder exists in the existing label. We need to
             * remove this.
             */
            else if (removeNode.getLabelItem().getItemStatus() == LabelItemStatus.EXISTS) {
                newStatus = LabelItemStatus.REMOVE;
            } else {
                final String status = removeNode.getLabelItem().getItemStatus().toString();
                String messageFormat = "Could not remove item {0} from the label, it is in state {1}"; //$NON-NLS-1$
                String message = MessageFormat.format(messageFormat, serverItem, status);
                log.error(message);

                messageFormat = Messages.getString("EditLabelDialog.ItemInconsistentStateFormat"); //$NON-NLS-1$
                message = MessageFormat.format(messageFormat, serverItem);
                transaction.addError(message);
                return;
            }

            setStatusRecursive(transaction, removeNode, newStatus);
        }

        private void setStatusRecursive(
            final LabelContentsTreeTransaction transaction,
            final LabelContentsTreeNode node,
            final LabelItemStatus newStatus) {
            Check.notNull(node, "node"); //$NON-NLS-1$

            node.getLabelItem().setItemStatus(newStatus);
            transaction.addAffectedNode(node);

            /* Apply to children */
            for (final Iterator<LabelContentsTreeNode> i = node.getChildren().iterator(); i.hasNext();) {
                final LabelContentsTreeNode child = i.next();

                setStatusRecursive(transaction, child, newStatus);
            }
        }

        public LabelContentsTreeDelta getDelta() {
            final List<ItemSpec> removeList = new ArrayList<ItemSpec>();
            final List<LabelItemSpec> addList = new ArrayList<LabelItemSpec>();
            int existsCount = 0;

            for (final Iterator<LabelContentsTreeNode> i = labelItemMap.values().iterator(); i.hasNext();) {
                final LabelContentsTreeNode treeNode = i.next();
                final LabelItem labelItem = treeNode.getLabelItem();
                final LabelItemStatus labelItemStatus = labelItem.getItemStatus();

                final ItemSpec itemSpec = new ItemSpec(labelItem.getServerItem(), labelItem.getRecursionType());

                if (labelItemStatus == LabelItemStatus.ADD) {
                    addList.add(new LabelItemSpec(itemSpec, labelItem.getVersionSpec(), false));
                } else if (labelItemStatus == LabelItemStatus.EXCLUDE) {
                    addList.add(new LabelItemSpec(itemSpec, labelItem.getVersionSpec(), true));
                } else if (labelItemStatus == LabelItemStatus.REMOVE) {
                    removeList.add(itemSpec);
                } else if (labelItemStatus == LabelItemStatus.EXISTS) {
                    existsCount++;
                }

                /*
                 * Ignore placeholders, existing files in the label or files
                 * that are implicitly added.
                 */
            }

            final ItemSpec[] removes = removeList.toArray(new ItemSpec[removeList.size()]);
            final LabelItemSpec[] adds = addList.toArray(new LabelItemSpec[addList.size()]);

            return new LabelContentsTreeDelta(existsCount, removes, adds);
        }
    }

    private static class LabelContentsTreeTransaction {
        private final List<IStatus> errorList = new ArrayList<IStatus>();
        private final Map<LabelContentsTreeNode, LabelItem> affectedNodes =
            new HashMap<LabelContentsTreeNode, LabelItem>();

        public void addError(final String errorMessage) {
            addError(new Status(IStatus.ERROR, TFSCommonUIClientPlugin.PLUGIN_ID, 0, errorMessage, null));
        }

        public void addError(final IStatus status) {
            errorList.add(status);
        }

        public IStatus getStatus() {
            if (errorList.size() == 0) {
                return Status.OK_STATUS;
            } else if (errorList.size() == 1) {
                return errorList.get(0);
            } else {
                final IStatus[] errors = errorList.toArray(new IStatus[errorList.size()]);
                return new MultiStatus(
                    TFSCommonUIClientPlugin.PLUGIN_ID,
                    0,
                    errors,
                    Messages.getString("EditLabelDialog.LabelingErrors"), //$NON-NLS-1$
                    null);
            }
        }

        public void addAffectedNode(final LabelContentsTreeNode node) {
            affectedNodes.put(node, node.getLabelItem());
        }

        public boolean isAffectedNode(final LabelContentsTreeNode node) {
            return affectedNodes.containsKey(node);
        }

        public LabelItem[] getAffectedLabelItems() {
            return affectedNodes.values().toArray(new LabelItem[affectedNodes.values().size()]);
        }
    }

    private static class LabelContentsTreeNode {
        private final List<LabelContentsTreeNode> children = new ArrayList<LabelContentsTreeNode>();

        private LabelItem labelItem;

        public LabelContentsTreeNode(final LabelItem labelItem) {
            this.labelItem = labelItem;
        }

        public void addChild(final LabelContentsTreeNode child) {
            children.add(child);
        }

        public List<LabelContentsTreeNode> getChildren() {
            return children;
        }

        public void setLabelItem(final LabelItem labelItem) {
            this.labelItem = labelItem;
        }

        public LabelItem getLabelItem() {
            return labelItem;
        }
    }

    private static class LabelContentsTreeDelta {
        private final int existsCount;
        private final ItemSpec[] deletes;
        private final LabelItemSpec[] adds;

        public LabelContentsTreeDelta(final int existsCount, final ItemSpec[] deletes, final LabelItemSpec[] adds) {
            this.existsCount = existsCount;
            this.deletes = deletes;
            this.adds = adds;
        }

        public int getExistsCount() {
            return existsCount;
        }

        public ItemSpec[] getDeletes() {
            return deletes;
        }

        public LabelItemSpec[] getAdds() {
            return adds;
        }
    }
}