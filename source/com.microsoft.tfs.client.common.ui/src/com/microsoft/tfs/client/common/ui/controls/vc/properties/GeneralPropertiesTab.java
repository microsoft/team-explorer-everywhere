// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.controls.vc.properties;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import com.microsoft.tfs.client.common.commands.vc.QueryItemsExtendedCommand;
import com.microsoft.tfs.client.common.repository.TFSRepository;
import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.controls.generic.BaseControl;
import com.microsoft.tfs.client.common.ui.dialogs.vc.SetEncodingDialog;
import com.microsoft.tfs.client.common.ui.framework.command.UICommandExecutorFactory;
import com.microsoft.tfs.client.common.ui.framework.layout.GridDataBuilder;
import com.microsoft.tfs.client.common.ui.tasks.vc.ChangeUnixExecutablePropertyTask;
import com.microsoft.tfs.client.common.ui.tasks.vc.CheckoutTask;
import com.microsoft.tfs.client.common.ui.vc.tfsitem.TFSFolder;
import com.microsoft.tfs.client.common.ui.vc.tfsitem.TFSItem;
import com.microsoft.tfs.core.clients.versioncontrol.GetItemsOptions;
import com.microsoft.tfs.core.clients.versioncontrol.PendChangesOptions;
import com.microsoft.tfs.core.clients.versioncontrol.PropertyConstants;
import com.microsoft.tfs.core.clients.versioncontrol.PropertyUtils;
import com.microsoft.tfs.core.clients.versioncontrol.WebServiceLevel;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.ChangeType;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.DeletedState;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.ExtendedItem;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.ItemIdentifier;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.ItemType;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.LockLevel;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.PendingChange;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.PropertyValue;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.RecursionType;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Workspace;
import com.microsoft.tfs.core.clients.versioncontrol.specs.ItemSpec;
import com.microsoft.tfs.core.util.FileEncoding;
import com.microsoft.tfs.jni.FileSystemUtils;
import com.microsoft.tfs.util.Check;

public class GeneralPropertiesTab implements PropertiesTab {
    private TFSRepository repository;
    private FileEncoding encoding;
    private boolean currentExecutable;
    private boolean newExecutable;
    private String localPath;

    private GeneralPropertiesControl generalControl;

    private Text serverNameValue;
    private Text localNameValue;
    private Text serverVersionValue;
    private Text localVersionValue;
    private Text encodingValue;
    private Text tfsServerValue;
    private Text tfsWorkspaceValue;
    protected Text tfsUserValue;

    private Button encodingButton;
    private Button executableButton;

    protected Label serverNameLabel;
    protected Label tfsUserLabel;
    protected Label localVersionLabel;

    private static final String NA = Messages.getString("GeneralPropertiesTab.NotApplicable"); //$NON-NLS-1$

    public class GeneralPropertiesControl extends BaseControl {
        public GeneralPropertiesControl(final Composite parent, final int style) {
            super(parent, style);

            final GridLayout gridLayout = new GridLayout(2, false);
            gridLayout.horizontalSpacing = getHorizontalSpacing();
            gridLayout.verticalSpacing = getVerticalSpacing();
            gridLayout.marginWidth = 0;
            gridLayout.marginHeight = 0;
            setLayout(gridLayout);

            /* Server path */
            serverNameLabel = new Label(this, SWT.NONE);
            GridDataBuilder.newInstance().vAlignTop().hAlignPrompt().applyTo(serverNameLabel);
            serverNameLabel.setText(Messages.getString("GeneralPropertiesTab.ServerNameLabelText")); //$NON-NLS-1$

            serverNameValue = new Text(this, SWT.READ_ONLY | SWT.WRAP);
            GridDataBuilder.newInstance().hGrab().hFill().wHint(getMinimumMessageAreaWidth()).applyTo(serverNameValue);

            /* Local path */
            final Label localPathLabel = new Label(this, SWT.NONE);
            GridDataBuilder.newInstance().vAlignTop().hAlignPrompt().applyTo(localPathLabel);
            localPathLabel.setText(Messages.getString("GeneralPropertiesTab.LocalPathLabelText")); //$NON-NLS-1$

            localNameValue = new Text(this, SWT.READ_ONLY | SWT.WRAP);
            GridDataBuilder.newInstance().hGrab().hFill().wHint(getMinimumMessageAreaWidth()).applyTo(localNameValue);

            /* Server version */
            final Label serverVersionLabel = new Label(this, SWT.NONE);
            GridDataBuilder.newInstance().vAlignTop().hAlignPrompt().applyTo(serverVersionLabel);
            serverVersionLabel.setText(Messages.getString("GeneralPropertiesTab.LatestVersionLabelText")); //$NON-NLS-1$

            serverVersionValue = new Text(this, SWT.READ_ONLY);
            GridDataBuilder.newInstance().hGrab().hFill().wHint(getMinimumMessageAreaWidth()).applyTo(
                serverVersionValue);

            /* Local version */
            localVersionLabel = new Label(this, SWT.NONE);
            GridDataBuilder.newInstance().vAlignTop().hAlignPrompt().applyTo(localVersionLabel);
            localVersionLabel.setText(Messages.getString("GeneralPropertiesTab.WorkspaceVersionLabelText")); //$NON-NLS-1$

            localVersionValue = new Text(this, SWT.READ_ONLY);
            GridDataBuilder.newInstance().hGrab().hFill().wHint(getMinimumMessageAreaWidth()).applyTo(
                localVersionValue);

            /* Encoding */
            final Label encodingLabel = new Label(this, SWT.NONE);
            GridDataBuilder.newInstance().vAlignTop().hAlignPrompt().applyTo(encodingLabel);
            encodingLabel.setText(Messages.getString("GeneralPropertiesTab.EncodingLabelText")); //$NON-NLS-1$

            encodingValue = new Text(this, SWT.READ_ONLY);
            GridDataBuilder.newInstance().hGrab().hFill().wHint(getMinimumMessageAreaWidth()).applyTo(encodingValue);

            final Label encodingSpacerLabel = new Label(this, SWT.NONE);
            GridDataBuilder.newInstance().vAlignCenter().applyTo(encodingSpacerLabel);

            encodingButton = new Button(this, SWT.NONE);
            encodingButton.setText(Messages.getString("GeneralPropertiesTab.SetEncodingButtonText")); //$NON-NLS-1$
            encodingButton.setEnabled(false);
            encodingButton.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(final SelectionEvent e) {
                    setEncoding();
                }
            });

            // Executable

            executableButton = new Button(this, SWT.CHECK);
            executableButton.setText(Messages.getString("GeneralPropertiesTab.ExecutableCheckboxText")); //$NON-NLS-1$
            executableButton.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(final SelectionEvent e) {
                    newExecutable = executableButton.getSelection();
                }
            });
            configureExecutableButton(ItemType.ANY, false, false);

            final Label spacerLabel = new Label(this, SWT.NONE);
            GridDataBuilder.newInstance().hSpan(2).applyTo(spacerLabel);

            /* TFS Server */
            final Label tfsServerLabel = new Label(this, SWT.NONE);
            GridDataBuilder.newInstance().vAlignTop().hAlignPrompt().applyTo(tfsServerLabel);
            tfsServerLabel.setText(Messages.getString("GeneralPropertiesTab.ServerLabelText")); //$NON-NLS-1$

            tfsServerValue = new Text(this, SWT.READ_ONLY | SWT.WRAP);
            GridDataBuilder.newInstance().hGrab().hFill().wHint(getMinimumMessageAreaWidth()).applyTo(tfsServerValue);

            /* TFS Workspace */
            final Label tfsWorkspaceLabel = new Label(this, SWT.NONE);
            GridDataBuilder.newInstance().vAlignTop().hAlignPrompt().applyTo(tfsWorkspaceLabel);
            tfsWorkspaceLabel.setText(Messages.getString("GeneralPropertiesTab.WorkspaceLabelText")); //$NON-NLS-1$

            tfsWorkspaceValue = new Text(this, SWT.READ_ONLY | SWT.WRAP);
            GridDataBuilder.newInstance().hGrab().hFill().wHint(getMinimumMessageAreaWidth()).applyTo(
                tfsWorkspaceValue);

            /* TFS User */
            tfsUserLabel = new Label(this, SWT.NONE);
            GridDataBuilder.newInstance().vAlignTop().hAlignPrompt().applyTo(tfsUserLabel);
            tfsUserLabel.setText(Messages.getString("GeneralPropertiesTab.UserLabelText")); //$NON-NLS-1$

            tfsUserValue = new Text(this, SWT.READ_ONLY | SWT.WRAP);
            GridDataBuilder.newInstance().hGrab().hFill().wHint(getMinimumMessageAreaWidth()).applyTo(tfsUserValue);
        }

        private void setEncoding() {
            final SetEncodingDialog encodingDialog = new SetEncodingDialog(getShell(), localPath, encoding);

            if (encodingDialog.open() != IDialogConstants.OK_ID
                || encodingDialog.getFileEncoding().getCodePage() == encoding.getCodePage()) {
                return;
            }

            final FileEncoding newEncoding = encodingDialog.getFileEncoding();

            final CheckoutTask checkoutTask = new CheckoutTask(getShell(), repository, new ItemSpec[] {
                new ItemSpec(localPath, RecursionType.NONE)
            }, LockLevel.UNCHANGED, newEncoding);

            final IStatus checkoutStatus = checkoutTask.run();

            if (checkoutStatus.getSeverity() != IStatus.ERROR) {
                encoding = newEncoding;
                encodingValue.setText(newEncoding.getName());
            }
        }

        /**
         * Set the enabled state, checked state, and tooltip text on the
         * "executable" check box according to the version of the service we're
         * connected to and the type of item we're viewing properties on.
         *
         * @param itemType
         *        the item type, which may be {@link ItemType#ANY} (must not be
         *        <code>null</code>)
         * @param <code>true</code>
         *        if the item already has the executable property set on it,
         *        <code>false</code> if it does not
         * @param <code>true</code>
         *        if the item has the symbolic link property set on it,
         *        <code>false</code> if it does not
         */
        public void configureExecutableButton(
            final ItemType itemType,
            final boolean initialExecutable,
            final boolean isSymlink) {
            /*
             * If you make changes to this method, see the duplicate in
             * BaseGeneralPropertyPage (for Resources and Pending Changes).
             */

            Check.notNull(itemType, "itemType"); //$NON-NLS-1$

            if (itemType != ItemType.FILE
                || repository == null
                || repository.getVersionControlClient().getServiceLevel().getValue() < WebServiceLevel.TFS_2012_2.getValue()
                || localPath == null
                || !repository.getWorkspace().isLocalPathMapped(localPath)
                || isSymlink) {
                executableButton.setEnabled(false);
                executableButton.setSelection(false);
                executableButton.setToolTipText(null);
            } else {
                executableButton.setEnabled(true);
                executableButton.setSelection(initialExecutable);
                executableButton.setToolTipText(
                    Messages.getString("GeneralPropertiesTab.ExecutableCheckboxTooltipText")); //$NON-NLS-1$
            }
        }
    }

    @Override
    public void populate(final TFSRepository repository, final TFSItem item) {
        this.repository = repository;
        encoding = null;
        localPath = null;

        if (repository != null && item != null) {
            localPath = item.getLocalPath();

            if (item.getExtendedItem() != null) {
                serverNameValue.setText(item.getExtendedItem().getSourceServerItem());
                serverVersionValue.setText(String.valueOf(item.getRemoteVersion()));
            } else {
                serverNameValue.setText(""); //$NON-NLS-1$
                serverVersionValue.setText("0"); //$NON-NLS-1$
            }

            if (item.getLocalPath() != null) {
                localNameValue.setText(item.getLocalPath());
                localVersionValue.setText(String.valueOf(item.getLocalVersion()));
            } else {
                localNameValue.setText(""); //$NON-NLS-1$
                localVersionValue.setText("0"); //$NON-NLS-1$
            }

            if (item instanceof TFSFolder) {
                encodingValue.setText(NA);
            } else {
                // Encoding

                encoding = item.getEncoding();

                if (localPath != null) {
                    final PendingChange existingChange =
                        repository.getPendingChangeCache().getPendingChangeByLocalPath(localPath);

                    if (existingChange != null
                        && existingChange.getChangeType().contains(ChangeType.ENCODING)
                        && !existingChange.getChangeType().contains(ChangeType.ADD)) {
                        encoding = new FileEncoding(existingChange.getEncoding());
                    }
                }

                if (encoding == null) {
                    encodingValue.setText(Messages.getString("GeneralPropertiesTab.UnknownEncodingValue")); //$NON-NLS-1$
                } else {
                    encodingValue.setText(encoding.getName());
                }

                // Only query for properties if the server supports them
                String[] itemPropertyFilters = null;
                if (repository.getVersionControlClient().getServiceLevel().getValue() >= WebServiceLevel.TFS_2012_2.getValue()) {
                    itemPropertyFilters = new String[] {
                        PropertyConstants.EXECUTABLE_KEY,
                        PropertyConstants.SYMBOLIC_KEY
                    };
                }

                final QueryItemsExtendedCommand command = new QueryItemsExtendedCommand(repository, new ItemSpec[] {
                    new ItemSpec(item.getFullPath(), RecursionType.NONE)
                }, DeletedState.NON_DELETED, ItemType.FILE, GetItemsOptions.NONE, itemPropertyFilters);

                final IStatus status =
                    UICommandExecutorFactory.newUICommandExecutor(generalControl.getShell()).execute(command);

                ExtendedItem extendedItem = item.getExtendedItem();
                if (status.isOK()) {
                    final ExtendedItem[][] results = command.getItems();

                    if (results != null && results.length > 0 && results[0] != null && results[0].length > 0) {
                        extendedItem = results[0][0];
                    }
                }

                currentExecutable = containsExecutableProperty(extendedItem.getPropertyValues());
                final boolean isSymLink = isSymLink(extendedItem);
                newExecutable = currentExecutable;
                generalControl.configureExecutableButton(ItemType.FILE, currentExecutable, isSymLink);
            }
        }

        if (repository != null && repository.getWorkspace() != null) {
            final Workspace workspace = repository.getWorkspace();
            tfsWorkspaceValue.setText(workspace.getName());
            tfsServerValue.setText(workspace.getServerName());
            tfsUserValue.setText(workspace.getOwnerDisplayName());
        } else {
            tfsWorkspaceValue.setText(NA);
            tfsServerValue.setText(NA);
            tfsUserValue.setText(NA);
        }

        if (encodingButton != null) {
            encodingButton.setEnabled(repository != null && encoding != null && localPath != null);
        }
    }

    @Override
    public void populate(final TFSRepository repository, final ItemIdentifier item) {
        this.repository = repository;
        encoding = null;
        localPath = null;

        if (item != null) {
            serverNameValue.setText(item.getItem());
            serverVersionValue.setText(item.getVersion().toString());

            localNameValue.setText(""); //$NON-NLS-1$
            localVersionValue.setText("0"); //$NON-NLS-1$

            encodingValue.setText(NA);
        }

        if (repository != null && repository.getWorkspace() != null) {
            final Workspace workspace = repository.getWorkspace();
            tfsWorkspaceValue.setText(workspace.getName());
            tfsServerValue.setText(workspace.getServerName());
            tfsUserValue.setText(workspace.getOwnerDisplayName());
        } else {
            tfsWorkspaceValue.setText(NA);
            tfsServerValue.setText(NA);
            tfsUserValue.setText(NA);
        }

        if (encodingButton != null) {
            encodingButton.setEnabled(false);
        }
    }

    @Override
    public String getTabItemText() {
        return Messages.getString("GeneralPropertiesTab.TabItemText"); //$NON-NLS-1$
    }

    @Override
    public Control setupTabItemControl(final Composite parent) {
        if (generalControl == null) {
            generalControl = new GeneralPropertiesControl(parent, SWT.NONE);
        }

        return generalControl;
    }

    @Override
    public boolean okPressed() {
        // Bits can only differ if the item was a file (not directory)
        if (newExecutable != currentExecutable) {
            final ChangeUnixExecutablePropertyTask task =
                new ChangeUnixExecutablePropertyTask(generalControl.getShell(), repository, new String[] {
                    localPath
                }, newExecutable, LockLevel.UNCHANGED, PendChangesOptions.NONE);

            if (!task.run().isOK()) {
                return false;
            }
        }

        return true;
    }

    private boolean containsExecutableProperty(final PropertyValue[] propertyValues) {
        return PropertyConstants.EXECUTABLE_ENABLED_VALUE.equals(
            PropertyUtils.selectMatching(propertyValues, PropertyConstants.EXECUTABLE_KEY));
    }

    private boolean containsSymLinkProperty(final PropertyValue[] propertyValues) {
        return PropertyConstants.IS_SYMLINK.equals(
            PropertyUtils.selectMatching(propertyValues, PropertyConstants.SYMBOLIC_KEY));
    }

    private boolean isSymLink(final ExtendedItem extendedItem) {
        return (localPath != null && FileSystemUtils.getInstance().getAttributes(localPath).isSymbolicLink())
            || containsSymLinkProperty(extendedItem.getPropertyValues());
    }
}
