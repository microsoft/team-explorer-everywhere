// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.dialogs.vc;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.FontMetrics;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import com.microsoft.tfs.client.common.commands.vc.QueryItemsCommand;
import com.microsoft.tfs.client.common.commands.vc.QueryLabelsCommand;
import com.microsoft.tfs.client.common.framework.command.CommandList;
import com.microsoft.tfs.client.common.repository.TFSRepository;
import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.controls.generic.menubutton.MenuButton;
import com.microsoft.tfs.client.common.ui.controls.generic.menubutton.MenuButtonFactory;
import com.microsoft.tfs.client.common.ui.controls.vc.VersionPickerControl;
import com.microsoft.tfs.client.common.ui.controls.vc.VersionPickerControl.VersionDescription;
import com.microsoft.tfs.client.common.ui.framework.WindowSystemProperties;
import com.microsoft.tfs.client.common.ui.framework.command.UICommandExecutorFactory;
import com.microsoft.tfs.client.common.ui.framework.dialog.BaseDialog;
import com.microsoft.tfs.client.common.ui.framework.layout.GridDataBuilder;
import com.microsoft.tfs.client.common.ui.framework.sizing.ControlSize;
import com.microsoft.tfs.client.common.ui.vc.serveritem.ServerItemType;
import com.microsoft.tfs.client.common.ui.vc.serveritem.VersionedItemSource;
import com.microsoft.tfs.client.common.vc.VersionSpecHelper;
import com.microsoft.tfs.core.clients.versioncontrol.GetItemsOptions;
import com.microsoft.tfs.core.clients.versioncontrol.path.ServerPath;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.DeletedState;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.ItemSet;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.ItemType;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.RecursionType;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.VersionControlLabel;
import com.microsoft.tfs.core.clients.versioncontrol.specs.ItemSpec;
import com.microsoft.tfs.core.clients.versioncontrol.specs.version.ChangesetVersionSpec;
import com.microsoft.tfs.core.clients.versioncontrol.specs.version.LatestVersionSpec;
import com.microsoft.tfs.core.clients.versioncontrol.specs.version.VersionSpec;
import com.microsoft.tfs.util.Check;

public class ApplyLabelDialog extends BaseDialog {
    public final static int NAME_MAX_LENGTH = 63;
    public final static int COMMENT_MAX_LENGTH = 2048;

    final static char[] INVALID_NAME_CHARS = new char[] {
        '"',
        '/',
        ':',
        '<',
        '>',
        '\\',
        '|',
        '*',
        '?',
        '@'
    };

    private final TFSRepository repository;
    private final String initialServerPath;

    private Text nameText;
    private Text commentText;
    private Text pathText;
    private VersionPickerControl versionControl;
    private MenuButton okButton;

    private String labelName;
    private String comment;
    private String serverPath;
    private VersionSpec versionSpec;
    private RecursionType recursionType;
    private boolean editLabel = false;
    private VersionControlLabel[] deleteExisting = new VersionControlLabel[0];

    public ApplyLabelDialog(final Shell parentShell, final TFSRepository repository, final String serverPath) {
        super(parentShell);

        Check.notNull(repository, "repository"); //$NON-NLS-1$
        Check.notNull(serverPath, "serverPath"); //$NON-NLS-1$

        this.repository = repository;
        initialServerPath = serverPath;

        setOptionResizableDirections(SWT.HORIZONTAL);
    }

    @Override
    protected String provideDialogTitle() {
        return Messages.getString("ApplyLabelDialog.DialogTitle"); //$NON-NLS-1$
    }

    @Override
    protected void hookAddToDialogArea(final Composite dialogArea) {
        final GridLayout layout = new GridLayout(3, false);
        layout.marginWidth = getHorizontalMargin();
        layout.marginHeight = getVerticalMargin();
        layout.horizontalSpacing = getHorizontalSpacing();
        layout.verticalSpacing = getVerticalSpacing();
        dialogArea.setLayout(layout);

        final Label nameLabel = new Label(dialogArea, SWT.NONE);
        nameLabel.setText(Messages.getString("ApplyLabelDialog.NameLabelText")); //$NON-NLS-1$

        nameText = new Text(dialogArea, SWT.BORDER);
        GridDataBuilder.newInstance().hSpan(2).hGrab().hFill().applyTo(nameText);
        nameText.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(final ModifyEvent e) {
                validate();
            }
        });
        nameText.setFocus();
        nameText.setTextLimit(NAME_MAX_LENGTH);

        final Label commentLabel = new Label(dialogArea, SWT.NONE);
        commentLabel.setText(Messages.getString("ApplyLabelDialog.CommentLabelText")); //$NON-NLS-1$
        GridDataBuilder.newInstance().vAlign(SWT.TOP).applyTo(commentLabel);

        commentText = new Text(dialogArea, SWT.MULTI | SWT.BORDER);
        GridDataBuilder.newInstance().hSpan(2).hGrab().hFill().applyTo(commentText);
        ControlSize.setCharHeightHint(commentText, 3);
        commentText.setTextLimit(COMMENT_MAX_LENGTH);

        final Label pathLabel = new Label(dialogArea, SWT.NONE);
        pathLabel.setText(Messages.getString("ApplyLabelDialog.PathLabelText")); //$NON-NLS-1$

        pathText = new Text(dialogArea, SWT.BORDER);
        pathText.setText(initialServerPath);
        pathText.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(final ModifyEvent e) {
                versionControl.setPath(pathText.getText());

                validate();
            }
        });
        GridDataBuilder.newInstance().hGrab().hFill().applyTo(pathText);

        final Button browseButton = new Button(dialogArea, SWT.PUSH);
        browseButton.setText(Messages.getString("ApplyLabelDialog.BrowseButtonText")); //$NON-NLS-1$
        browseButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
                final ServerItemTreeDialog itemDialog =
                    new ServerItemTreeDialog(
                        getShell(),
                        Messages.getString("ApplyLabelDialog.ItemDialogTitle"), //$NON-NLS-1$
                        pathText.getText(),
                        new VersionedItemSource(repository, LatestVersionSpec.INSTANCE),
                        ServerItemType.ALL);

                if (itemDialog.open() != IDialogConstants.OK_ID) {
                    return;
                }

                pathText.setText(itemDialog.getSelectedServerPath());
            }
        });

        final Label versionLabel = new Label(dialogArea, SWT.NONE);
        versionLabel.setText(Messages.getString("ApplyLabelDialog.VersionLabelText")); //$NON-NLS-1$

        versionControl = new VersionPickerControl(dialogArea, VersionPickerControl.NO_PROMPT);
        versionControl.setRepository(repository);
        versionControl.setPath(initialServerPath);
        GridDataBuilder.newInstance().hSpan(2).hGrab().hFill().applyTo(versionControl);
        ControlSize.setCharWidthHint(versionControl, 80);
    }

    /**
     * Override createButtonBar to use drop-down button.
     *
     * {@inheritDoc}
     */
    @Override
    protected Control createButtonBar(final Composite parent) {
        final Composite buttonBar = new Composite(parent, SWT.NONE);

        final GridLayout buttonBarLayout = new GridLayout(1, true);
        buttonBarLayout.marginWidth = getHorizontalMargin();
        buttonBarLayout.marginHeight = getVerticalMargin();
        buttonBarLayout.horizontalSpacing = getHorizontalSpacing();
        buttonBarLayout.verticalSpacing = getVerticalSpacing();
        buttonBar.setLayout(buttonBarLayout);

        GridDataBuilder.newInstance().hAlignRight().vAlignCenter().applyTo(buttonBar);

        if (WindowSystemProperties.getDefaultButton() == IDialogConstants.OK_ID) {
            createCancelButton(buttonBar);
            createOkButton(buttonBar);
        } else {
            createOkButton(buttonBar);
            createCancelButton(buttonBar);
        }

        return buttonBar;
    }

    private void createCancelButton(final Composite parent) {
        createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
    }

    private void createOkButton(final Composite parent) {
        okButton = MenuButtonFactory.getMenuButton(parent, SWT.NONE);
        okButton.setText(Messages.getString("ApplyLabelDialog.OkButtonText")); //$NON-NLS-1$

        final GridData data = new GridData(GridData.HORIZONTAL_ALIGN_FILL);
        okButton.setLayoutData(data);

        /* Set to default size */
        final GC gc = new GC(okButton);
        final FontMetrics fm = gc.getFontMetrics();
        gc.dispose();

        final int widthHint = Dialog.convertHorizontalDLUsToPixels(fm, IDialogConstants.BUTTON_WIDTH);
        final Point okSize = okButton.computeSize(SWT.DEFAULT, SWT.DEFAULT, true);
        okSize.x = Math.max(widthHint, okSize.x);
        ControlSize.setSizeHints(okButton, okSize);

        /* Create menu */
        okButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
                editLabel = false;
                okPressed();
            }
        });

        final IAction createLabelAction = new Action() {
            @Override
            public void run() {
                editLabel = false;
                okPressed();
            }
        };
        createLabelAction.setText(Messages.getString("ApplyLabelDialog.CreateLabelActionText")); //$NON-NLS-1$

        final IAction createAndEditLabelAction = new Action() {
            @Override
            public void run() {
                editLabel = true;
                okPressed();
            }
        };
        createAndEditLabelAction.setText(Messages.getString("ApplyLabelDialog.EditLabelActionText")); //$NON-NLS-1$

        okButton.addMenuListener(new IMenuListener() {
            @Override
            public void menuAboutToShow(final IMenuManager manager) {
                manager.add(createLabelAction);
                manager.add(createAndEditLabelAction);
            }
        });

        getShell().setDefaultButton(okButton.getButton());

        validate();
    }

    private void validate() {
        okButton.setEnabled(nameText.getText().length() > 0 && pathText.getText().length() > 0);
    }

    @Override
    protected void okPressed() {
        labelName = nameText.getText().trim();
        comment = commentText.getText().trim();
        serverPath = pathText.getText();
        versionSpec = versionControl.getVersionSpec();

        final String scope =
            ServerPath.equals(serverPath, ServerPath.ROOT) ? ServerPath.ROOT : ServerPath.getTeamProject(serverPath);

        if (versionSpec == null
            || (versionSpec instanceof ChangesetVersionSpec
                && ((ChangesetVersionSpec) versionSpec).getChangeset() == 0)) {
            String errorMessage = Messages.getString("ApplyLabelDialog.InvalidVersionError"); //$NON-NLS-1$

            if (versionControl.getVersionType() == VersionDescription.DATE) {
                errorMessage = Messages.getString("ApplyLabelDialog.InvalidDateError"); //$NON-NLS-1$
            } else if (versionControl.getVersionType() == VersionDescription.CHANGESET) {
                errorMessage = Messages.getString("ApplyLabelDialog.InvalidChangesetError"); //$NON-NLS-1$
            }

            MessageDialog.openError(
                getShell(),
                Messages.getString("ApplyLabelDialog.InvalidVersionDialogTitle"), //$NON-NLS-1$
                errorMessage);
            return;
        }

        if (labelName.length() == 0) {
            MessageDialog.openError(
                getShell(),
                Messages.getString("ApplyLabelDialog.InvalidNameDialogTitle"), //$NON-NLS-1$
                Messages.getString("ApplyLabelDialog.LabelRequiresName")); //$NON-NLS-1$
            return;
        }

        if (labelName.length() > NAME_MAX_LENGTH) {
            final String title = Messages.getString("ApplyLabelDialog.InvalidNameDialogTitle"); //$NON-NLS-1$
            final String messageFormat = Messages.getString("ApplyLabelDialog.LabelTooLongFormat"); //$NON-NLS-1$
            final String message = MessageFormat.format(messageFormat, (NAME_MAX_LENGTH + 1));

            MessageDialog.openError(getShell(), title, message);
            return;
        }

        if (comment.length() > COMMENT_MAX_LENGTH) {
            final String title = Messages.getString("ApplyLabelDialog.CommentTooLongDialogTitle"); //$NON-NLS-1$
            final String messageFormat = Messages.getString("ApplyLabelDialog.CommentTooLongFormat"); //$NON-NLS-1$
            final String message = MessageFormat.format(messageFormat, ApplyLabelDialog.COMMENT_MAX_LENGTH);

            MessageDialog.openError(getShell(), title, message);
            return;
        }

        for (int i = 0; i < INVALID_NAME_CHARS.length; i++) {
            if (labelName.indexOf(INVALID_NAME_CHARS[i]) >= 0) {
                final String title = Messages.getString("ApplyLabelDialog.InvalidNameDialogTitle"); //$NON-NLS-1$
                final String messageFormat = Messages.getString("ApplyLabelDialog.InvalidCharInLabelFormat"); //$NON-NLS-1$
                final String message = MessageFormat.format(messageFormat, INVALID_NAME_CHARS[i]);

                MessageDialog.openError(getShell(), title, message);
                return;
            }
        }

        if (serverPath.length() == 0 || !ServerPath.isServerPath(serverPath)) {
            final String title = Messages.getString("ApplyLabelDialog.InvalidPathDialogTitle"); //$NON-NLS-1$
            final String message = Messages.getString("ApplyLabelDialog.NotValidServerPath"); //$NON-NLS-1$

            MessageDialog.openError(getShell(), title, message);
            return;
        }

        final QueryItemsCommand queryItemsCommand = new QueryItemsCommand(repository, new ItemSpec[] {
            new ItemSpec(serverPath, RecursionType.NONE)
        }, versionSpec, DeletedState.NON_DELETED, ItemType.ANY, GetItemsOptions.UNSORTED);

        final QueryLabelsCommand queryLabelsCommand = new QueryLabelsCommand(repository, labelName, scope, null);

        final CommandList queryAllCommand =
            new CommandList(
                Messages.getString("ApplyLabelDialog.QueryLabelCommandText"), //$NON-NLS-1$
                Messages.getString("ApplyLabelDialog.QueryLabelErrorText")); //$NON-NLS-1$
        queryAllCommand.addCommand(queryItemsCommand);
        queryAllCommand.addCommand(queryLabelsCommand);

        final IStatus queryStatus = UICommandExecutorFactory.newUICommandExecutor(getShell()).execute(queryAllCommand);

        if (!queryStatus.isOK()) {
            return;
        }

        /* Make sure the item exists at that version */
        final ItemSet[] querySets = queryItemsCommand.getItemSets();

        if (querySets.length != 1 || querySets[0].getItems().length != 1) {
            final String spec = VersionSpecHelper.getVersionSpecDescription(versionSpec);
            final String messageFormat = Messages.getString("ApplyLabelDialog.ItemNotOnServerFormat"); //$NON-NLS-1$
            final String message = MessageFormat.format(messageFormat, serverPath, spec);

            MessageDialog.openError(getShell(), Messages.getString("ApplyLabelDialog.InvalidPathDialogTitle"), message); //$NON-NLS-1$
            return;
        }

        recursionType =
            querySets[0].getItems()[0].getItemType() == ItemType.FOLDER ? RecursionType.FULL : RecursionType.NONE;

        /*
         * See if there exists an existing label with that name. Note that the
         * user may have entered a wildcard in the Name field, which will cause
         * us to get multiple results back. Thus, examine each and do a proper
         * name comparison.
         */
        final VersionControlLabel[] existingLabels = queryLabelsCommand.getLabels();
        final List existingLabelList = new ArrayList();

        for (int i = 0; i < existingLabels.length; i++) {
            if (existingLabels[i].getName().equalsIgnoreCase(labelName)) {
                existingLabelList.add(existingLabels[i]);
            }
        }

        if (existingLabelList.size() > 0) {
            String message;
            if (existingLabelList.size() == 1) {
                message = Messages.getString("ApplyLabelDialog.SingleLabelExists"); //$NON-NLS-1$
            } else {
                final String messageFormat = Messages.getString("ApplyLabelDialog.MultiLabelsExistFormat"); //$NON-NLS-1$
                message = MessageFormat.format(messageFormat, existingLabelList.size());
            }

            if (!MessageDialog.openQuestion(
                getShell(),
                Messages.getString("ApplyLabelDialog.ConfirmOverriteDialogTitle"), //$NON-NLS-1$
                message)) {
                return;
            }

            deleteExisting =
                (VersionControlLabel[]) existingLabelList.toArray(new VersionControlLabel[existingLabelList.size()]);
        }

        super.okPressed();
    }

    public String getName() {
        return labelName;
    }

    public String getComment() {
        return comment;
    }

    public String getServerItem() {
        return serverPath;
    }

    public VersionSpec getVersionSpec() {
        return versionSpec;
    }

    public RecursionType getRecursionType() {
        return recursionType;
    }

    public boolean isEditLabel() {
        return editLabel;
    }

    public VersionControlLabel[] getDeleteExisting() {
        return deleteExisting;
    }
}
