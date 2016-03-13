// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.dialogs.vc;

import java.text.MessageFormat;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;

import com.microsoft.tfs.client.common.repository.TFSRepository;
import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.TFSCommonUIClientPlugin;
import com.microsoft.tfs.client.common.ui.controls.vc.RollbackItemControl;
import com.microsoft.tfs.client.common.ui.controls.vc.RollbackItemControl.RollbackOperationType;
import com.microsoft.tfs.client.common.ui.framework.dialog.ExtendedButtonDialog;
import com.microsoft.tfs.client.common.ui.framework.helper.MessageBoxHelpers;
import com.microsoft.tfs.client.common.ui.framework.layout.GridDataBuilder;
import com.microsoft.tfs.client.common.ui.framework.validation.ButtonValidatorBinding;
import com.microsoft.tfs.client.common.ui.prefs.UIPreferenceConstants;
import com.microsoft.tfs.core.clients.versioncontrol.RollbackOptions;
import com.microsoft.tfs.core.clients.versioncontrol.VersionControlConstants;
import com.microsoft.tfs.core.clients.versioncontrol.path.ServerPath;
import com.microsoft.tfs.core.clients.versioncontrol.specs.version.ChangesetVersionSpec;
import com.microsoft.tfs.core.clients.versioncontrol.specs.version.LatestVersionSpec;
import com.microsoft.tfs.core.clients.versioncontrol.specs.version.VersionSpec;
import com.microsoft.tfs.util.StringUtil;

/**
 * Prompts the user to choose an item to rollback and a rollback mode: - a
 * single changeset - a range of changesets - to a specific version defined as
 * either -- a changeset, or -- a date, or -- a label, or -- a workspace version
 */
public class RollbackItemDialog extends ExtendedButtonDialog {
    @SuppressWarnings("unused")
    private static final Log log = LogFactory.getLog(RollbackItemDialog.class);

    private final TFSRepository repository;
    private String item;
    private RollbackOperationType rollbackOperationType = RollbackOperationType.NONE;
    private VersionSpec fromVersion;
    private VersionSpec toVersion;
    private RollbackOptions rollbackOptions;

    private RollbackItemControl rollbackItemControl;

    public RollbackItemDialog(final Shell parentShell, final String initialItem, final TFSRepository repository) {
        this(parentShell, initialItem, repository, null, null);
    }

    public RollbackItemDialog(
        final Shell parentShell,
        final String initialItem,
        final TFSRepository repository,
        final VersionSpec fromVersion,
        final VersionSpec toVersion) {
        super(parentShell);

        this.item = initialItem;
        this.repository = repository;
        this.fromVersion = fromVersion;
        this.toVersion = toVersion;

        setOptionIncludeDefaultButtons(false);

        addButtonDescription(IDialogConstants.OK_ID, Messages.getString("RollbackItemDialog.RollbackButton"), false); //$NON-NLS-1$
        addButtonDescription(IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
    }

    public String getItem() {
        return item;
    }

    public VersionSpec getFromVersion() {
        return fromVersion;
    }

    public VersionSpec getToVersion() {
        return toVersion;
    }

    public RollbackOptions getRollbackOptions() {
        return rollbackOptions;
    }

    @Override
    protected void hookAddToDialogArea(final Composite dialogArea) {
        final GridLayout layout = new GridLayout();
        layout.marginWidth = getHorizontalMargin();
        layout.marginHeight = getVerticalMargin();
        layout.horizontalSpacing = getHorizontalSpacing();
        layout.verticalSpacing = getVerticalSpacing();
        dialogArea.setLayout(layout);

        rollbackItemControl = new RollbackItemControl(dialogArea, SWT.NONE, repository, item);
        GridDataBuilder.newInstance().grab().fill().applyTo(rollbackItemControl);

        if (fromVersion != null) {
            if (fromVersion instanceof ChangesetVersionSpec) {
                if (toVersion != null && toVersion instanceof ChangesetVersionSpec) {
                    rollbackItemControl.setInitialValues(
                        RollbackOperationType.CHANGESET_RANGE,
                        String.valueOf(((ChangesetVersionSpec) fromVersion).getChangeset()),
                        String.valueOf(((ChangesetVersionSpec) toVersion).getChangeset()));
                } else {
                    rollbackItemControl.setInitialValues(
                        RollbackOperationType.SINGLE_CHANGESET,
                        String.valueOf(((ChangesetVersionSpec) fromVersion).getChangeset()));
                }
            } else {
                rollbackItemControl.setInitialValues(RollbackOperationType.SPECIFIC_VERSION, fromVersion);
            }
        } else {
            rollbackItemControl.setInitialValues(RollbackOperationType.SINGLE_CHANGESET);
        }
        super.setOptionResizableDirections(SWT.HORIZONTAL);
    }

    @Override
    protected String provideDialogTitle() {
        return Messages.getString("RollbackItemDialog.RollbackTitle"); //$NON-NLS-1$
    }

    @Override
    protected void hookAfterButtonsCreated() {
        new ButtonValidatorBinding(getButton(IDialogConstants.OK_ID)).bind(rollbackItemControl.getValidator());
    }

    @Override
    protected void okPressed() {
        try {
            item = rollbackItemControl.getItem();

            if (!ServerPath.isServerPath(item)) {
                final String serverPath = repository.getWorkspace().getMappedServerPath(item);
                if (StringUtil.isNullOrEmpty(serverPath)) {
                    final String noMappingExistsFormat = Messages.getString("RollbackItemDialog.NoMappingExistsFormat"); //$NON-NLS-1$
                    MessageBoxHelpers.errorMessageBox(
                        getShell(),
                        Messages.getString("RollbackItemDialog.ErrorBoxTitle"), //$NON-NLS-1$
                        MessageFormat.format(noMappingExistsFormat, item));
                    return;
                }

                item = serverPath;
            }

            item = ServerPath.canonicalize(item);

            rollbackOperationType = rollbackItemControl.getRollbackOperationType();

            rollbackOptions = RollbackOptions.NONE;
            final IPreferenceStore preferences = TFSCommonUIClientPlugin.getDefault().getPreferenceStore();
            if (!preferences.getBoolean(UIPreferenceConstants.AUTO_RESOLVE_CONFLICTS)) {
                rollbackOptions = rollbackOptions.combine(RollbackOptions.NO_AUTO_RESOLVE);
            }

            if (rollbackOperationType == RollbackOperationType.SPECIFIC_VERSION) {
                fromVersion = rollbackItemControl.getVersionSpec();
                toVersion = LatestVersionSpec.INSTANCE;
                rollbackOptions = rollbackOptions.combine(RollbackOptions.TO_VERSION);
            } else if (rollbackOperationType == RollbackOperationType.SINGLE_CHANGESET) {
                fromVersion = VersionSpec.parseSingleVersionFromSpec(
                    rollbackItemControl.getSingleChangesetID(),
                    VersionControlConstants.AUTHENTICATED_USER);
                toVersion = fromVersion;
            } else if (rollbackOperationType == RollbackOperationType.CHANGESET_RANGE) {
                final int changesetFromID = Integer.parseInt(rollbackItemControl.getChangesetFromID());
                final int changesetToID = Integer.parseInt(rollbackItemControl.getChangesetToID());

                fromVersion = VersionSpec.parseSingleVersionFromSpec(
                    String.valueOf(Math.min(changesetFromID, changesetToID)),
                    VersionControlConstants.AUTHENTICATED_USER);
                toVersion = VersionSpec.parseSingleVersionFromSpec(
                    String.valueOf(Math.max(changesetFromID, changesetToID)),
                    VersionControlConstants.AUTHENTICATED_USER);
            }
        } catch (final Exception e) {
            MessageBoxHelpers.errorMessageBox(
                getShell(),
                Messages.getString("RollbackItemDialog.ErrorBoxTitle"), //$NON-NLS-1$
                e.getMessage());
            return;
        }

        super.okPressed();
    }

}
