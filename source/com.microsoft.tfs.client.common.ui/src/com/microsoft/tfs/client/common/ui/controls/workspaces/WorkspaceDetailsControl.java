// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.controls.workspaces;

import java.util.ArrayList;

import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.controls.generic.BaseControl;
import com.microsoft.tfs.client.common.ui.framework.helper.ContentProviderAdapter;
import com.microsoft.tfs.client.common.ui.framework.helper.SWTUtil;
import com.microsoft.tfs.client.common.ui.framework.layout.GridDataBuilder;
import com.microsoft.tfs.client.common.ui.framework.sizing.ControlSize;
import com.microsoft.tfs.core.TFSTeamProjectCollection;
import com.microsoft.tfs.core.clients.versioncontrol.SupportedFeatures;
import com.microsoft.tfs.core.clients.versioncontrol.WorkspaceLocation;
import com.microsoft.tfs.core.clients.versioncontrol.WorkspaceOptions;
import com.microsoft.tfs.core.clients.versioncontrol.WorkspacePermissionProfile;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.StringUtil;
import com.microsoft.tfs.util.valid.Validatable;
import com.microsoft.tfs.util.valid.Validator;
import com.microsoft.tfs.util.valid.ValidatorHelper;

public class WorkspaceDetailsControl extends BaseControl implements Validatable {
    private final ValidatorHelper validator;

    private final Label workspaceNameLabel;
    private final Text workspaceNameText;
    private Label workspaceServerLabel;
    private Text workspaceServerText;
    private Label workspaceOwnerLabel;
    private Text workspaceOwnerText;
    private Label workspaceComputerLabel;
    private Text workspaceComputerText;
    private Label workspaceLocationLabel;
    private Combo workspaceLocationCombo;
    private ComboViewer workspaceLocationComboViewer;
    private Label workspaceFileTimeLabel;
    private Combo workspaceFileTimeCombo;
    private ComboViewer workspaceFileTimeComboViewer;
    private Label workspacePermissionsLabel;
    private Combo workspacePermissionsCombo;
    private ComboViewer workspacePermissionsComboViewer;
    private Label workspacePermissionsDescription;
    private Control workspacePermissionsDescriptionPlaceholder;
    private Label commentLabel;
    private Text commentText;

    private TFSTeamProjectCollection connection;
    private WorkspaceDetails workspaceDetails;

    private boolean respondToWorkspaceNameTextChanges = true;
    private boolean respondToCommentTextChanges = true;
    private boolean respondToLocationComboChanges = true;
    private boolean respondToFileTimeComboChanges = true;
    private boolean respondToPermissionComboChanges = true;

    private boolean advanced = false;

    public static final int PERMISSION_PROFILE_DESCRIPTION_WIDTH = 100;

    public WorkspaceDetailsControl(final Composite parent, final int style) {
        this(parent, style, null);
    }

    public WorkspaceDetailsControl(final Composite parent, final int style, final TFSTeamProjectCollection connection) {
        super(parent, style);
        this.connection = connection;

        validator = new ValidatorHelper(this);

        final GridLayout layout = new GridLayout(2, false);
        layout.marginWidth = 0;
        layout.marginHeight = 0;
        layout.horizontalSpacing = getHorizontalSpacing();
        layout.verticalSpacing = getVerticalSpacing();
        setLayout(layout);

        workspaceNameLabel = SWTUtil.createLabel(this, Messages.getString("WorkspaceDetailsControl.NameLabelText")); //$NON-NLS-1$
        GridDataBuilder.newInstance().hAlignPrompt().applyTo(workspaceNameLabel);

        workspaceNameText = new Text(this, SWT.BORDER);
        GridDataBuilder.newInstance().hGrab().hFill().applyTo(workspaceNameText);
        workspaceNameText.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(final ModifyEvent e) {
                workspaceNameTextModified(e);
            }
        });

        // setAdvanced(true);

        validator.setInvalid();
    }

    public void setConnection(final TFSTeamProjectCollection connection) {
        this.connection = connection;
    }

    public void setAdvanced(final boolean advanced) {
        if (this.advanced == advanced) {
            return;
        }

        this.advanced = advanced;

        if (advanced) {
            workspaceServerLabel =
                SWTUtil.createLabel(this, Messages.getString("WorkspaceDetailsControl.ServerLabelText")); //$NON-NLS-1$
            GridDataBuilder.newInstance().hAlignPrompt().applyTo(workspaceServerLabel);

            workspaceServerText = new Text(this, SWT.BORDER);
            GridDataBuilder.newInstance().hGrab().hFill().applyTo(workspaceServerText);
            workspaceServerText.setEnabled(false);

            workspaceOwnerLabel =
                SWTUtil.createLabel(this, Messages.getString("WorkspaceDetailsControl.OwnerLabelText")); //$NON-NLS-1$
            GridDataBuilder.newInstance().hAlignPrompt().applyTo(workspaceOwnerLabel);

            workspaceOwnerText = new Text(this, SWT.BORDER | SWT.READ_ONLY);
            GridDataBuilder.newInstance().hGrab().hFill().applyTo(workspaceOwnerText);
            workspaceOwnerText.setEnabled(false);

            workspaceComputerLabel =
                SWTUtil.createLabel(this, Messages.getString("WorkspaceDetailsControl.ComputerLabelText")); //$NON-NLS-1$
            GridDataBuilder.newInstance().hAlignPrompt().applyTo(workspaceComputerLabel);

            workspaceComputerText = new Text(this, SWT.BORDER | SWT.READ_ONLY);
            GridDataBuilder.newInstance().hGrab().hFill().applyTo(workspaceComputerText);
            workspaceComputerText.setEnabled(false);

            workspaceLocationLabel =
                SWTUtil.createLabel(this, Messages.getString("WorkspaceDetailsControl.LocationText")); //$NON-NLS-1$
            GridDataBuilder.newInstance().hAlignPrompt().applyTo(workspaceLocationLabel);

            workspaceLocationCombo = new Combo(this, SWT.READ_ONLY);
            GridDataBuilder.newInstance().hGrab().hFill().applyTo(workspaceLocationCombo);

            workspaceLocationComboViewer = new ComboViewer(workspaceLocationCombo);
            workspaceLocationComboViewer.setLabelProvider(new LabelProvider() {
                @Override
                public String getText(final Object element) {
                    if (WorkspaceLocation.LOCAL.equals(element)) {
                        return Messages.getString("WorkspaceDetailsControl.LocationLocalText"); //$NON-NLS-1$
                    }

                    return Messages.getString("WorkspaceDetailsControl.LocationServerText"); //$NON-NLS-1$
                }
            });
            workspaceLocationComboViewer.setContentProvider(new ContentProviderAdapter() {
                @Override
                public Object[] getElements(final Object input) {
                    return (WorkspaceLocation[]) input;
                }
            });
            workspaceLocationComboViewer.addSelectionChangedListener(new ISelectionChangedListener() {
                @Override
                public void selectionChanged(final SelectionChangedEvent event) {
                    locationComboSelected(event);
                }
            });
            workspaceLocationComboViewer.setInput(new WorkspaceLocation[] {
                WorkspaceLocation.SERVER,
                WorkspaceLocation.LOCAL
            });

            workspaceFileTimeLabel =
                SWTUtil.createLabel(this, Messages.getString("WorkspaceDetailsControl.FileTimeText")); //$NON-NLS-1$
            GridDataBuilder.newInstance().hAlignPrompt().applyTo(workspaceFileTimeLabel);

            workspaceFileTimeCombo = new Combo(this, SWT.READ_ONLY);
            GridDataBuilder.newInstance().hGrab().hFill().applyTo(workspaceFileTimeCombo);

            workspaceFileTimeComboViewer = new ComboViewer(workspaceFileTimeCombo);
            workspaceFileTimeComboViewer.setLabelProvider(new LabelProvider() {
                @Override
                public String getText(final Object element) {
                    if (((WorkspaceOptions) element).contains(WorkspaceOptions.SET_FILE_TO_CHECKIN)) {
                        return Messages.getString("WorkspaceDetailsControl.FileTimeCheckinText"); //$NON-NLS-1$
                    }

                    return Messages.getString("WorkspaceDetailsControl.FileTimeCurrentText"); //$NON-NLS-1$
                }
            });
            workspaceFileTimeComboViewer.setContentProvider(new ContentProviderAdapter() {
                @Override
                public Object[] getElements(final Object input) {
                    return (WorkspaceOptions[]) input;
                }
            });
            workspaceFileTimeComboViewer.addSelectionChangedListener(new ISelectionChangedListener() {
                @Override
                public void selectionChanged(final SelectionChangedEvent event) {
                    fileTimeComboSelected(event);
                }
            });
            workspaceFileTimeComboViewer.setInput(new WorkspaceOptions[] {
                WorkspaceOptions.NONE,
                WorkspaceOptions.SET_FILE_TO_CHECKIN
            });

            workspacePermissionsLabel =
                SWTUtil.createLabel(this, Messages.getString("WorkspaceDetailsControl.PermissionsLabel")); //$NON-NLS-1$
            GridDataBuilder.newInstance().hAlignPrompt().applyTo(workspacePermissionsLabel);

            workspacePermissionsCombo = new Combo(this, SWT.READ_ONLY);
            GridDataBuilder.newInstance().hGrab().hFill().applyTo(workspacePermissionsCombo);

            workspacePermissionsComboViewer = new ComboViewer(workspacePermissionsCombo);
            workspacePermissionsComboViewer.setLabelProvider(new LabelProvider() {
                @Override
                public String getText(final Object element) {
                    return permissionProfileName(((WorkspacePermissionProfile) element).getBuiltinIndex());
                }
            });
            workspacePermissionsComboViewer.setContentProvider(new ContentProviderAdapter() {
                @Override
                public Object[] getElements(final Object input) {
                    return (WorkspacePermissionProfile[]) input;
                }
            });
            workspacePermissionsComboViewer.addSelectionChangedListener(new ISelectionChangedListener() {
                @Override
                public void selectionChanged(final SelectionChangedEvent event) {
                    permissionComboSelected(event);
                }
            });
            workspacePermissionsComboViewer.setInput(WorkspacePermissionProfile.getBuiltInProfiles());

            workspacePermissionsDescriptionPlaceholder = SWTUtil.createHorizontalGridLayoutSpacer(this, 1);

            workspacePermissionsDescription = new Label(this, SWT.WRAP);
            final Point labelSize = permissionProfileDescriptionLabelSize(PERMISSION_PROFILE_DESCRIPTION_WIDTH);
            ControlSize.setCharSizeHints(workspacePermissionsDescription, labelSize.x, labelSize.y);
            GridDataBuilder.newInstance().hGrab().cHint(
                workspacePermissionsDescription,
                labelSize.x,
                labelSize.y).hAlignLeft().applyTo(workspacePermissionsDescription);

            commentLabel = SWTUtil.createLabel(this, Messages.getString("WorkspaceDetailsControl.CommentLabelText")); //$NON-NLS-1$
            GridDataBuilder.newInstance().hSpan(2).vIndent(getVerticalSpacing()).applyTo(commentLabel);

            commentText = new Text(this, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL | SWT.WRAP);
            GridDataBuilder.newInstance().hGrab().hFill().hSpan(2).applyTo(commentText);
            ControlSize.setCharHeightHint(commentText, 3);
            commentText.addModifyListener(new ModifyListener() {
                @Override
                public void modifyText(final ModifyEvent e) {
                    commentTextModified(e);
                }
            });
        } else {
            workspaceServerLabel.dispose();
            workspaceServerLabel = null;

            workspaceServerText.dispose();
            workspaceServerText = null;

            workspaceOwnerLabel.dispose();
            workspaceOwnerLabel = null;

            workspaceOwnerText.dispose();
            workspaceOwnerText = null;

            workspaceComputerLabel.dispose();
            workspaceComputerLabel = null;

            workspaceComputerText.dispose();
            workspaceComputerText = null;

            workspaceLocationLabel.dispose();
            workspaceLocationLabel = null;

            workspaceLocationCombo.dispose();
            workspaceLocationCombo = null;

            workspaceFileTimeLabel.dispose();
            workspaceFileTimeLabel = null;

            workspaceFileTimeCombo.dispose();
            workspaceFileTimeCombo = null;

            workspacePermissionsLabel.dispose();
            workspacePermissionsLabel = null;

            workspacePermissionsCombo.dispose();
            workspacePermissionsCombo = null;

            workspacePermissionsDescriptionPlaceholder.dispose();
            workspacePermissionsDescriptionPlaceholder = null;

            workspacePermissionsDescription.dispose();
            workspacePermissionsDescription = null;

            commentLabel.dispose();
            commentLabel = null;

            commentText.dispose();
            commentText = null;
        }

        setWorkspaceDetails(workspaceDetails);
    }

    public void setImmutable(final boolean immutable) {
        workspaceNameText.setEditable(!immutable);
        workspaceNameText.setEnabled(!immutable);
    }

    public WorkspaceDetails getWorkspaceDetails() {
        return workspaceDetails;
    }

    public void setWorkspaceDetails(final WorkspaceDetails newWorkspaceMetadata) {
        Check.notNull(newWorkspaceMetadata, "newWorkspaceMetadata"); //$NON-NLS-1$

        workspaceDetails = newWorkspaceMetadata;

        boolean valid = true;

        if (workspaceDetails.getName() != null) {
            try {
                respondToWorkspaceNameTextChanges = false;
                workspaceNameText.setText(workspaceDetails.getName());
            } finally {
                respondToWorkspaceNameTextChanges = true;
            }
            workspaceNameText.selectAll();
        } else {
            workspaceNameText.setText(""); //$NON-NLS-1$
            valid = false;
        }

        if (advanced) {
            if (workspaceDetails.getServer() != null) {
                workspaceServerText.setText(workspaceDetails.getServer());
            } else {
                workspaceServerText.setText(""); //$NON-NLS-1$
            }

            if (workspaceDetails.getOwner() != null) {
                workspaceOwnerText.setText(workspaceDetails.getOwner());
            } else {
                workspaceOwnerText.setText(""); //$NON-NLS-1$
            }

            if (workspaceDetails.getComputer() != null) {
                workspaceComputerText.setText(workspaceDetails.getComputer());
            } else {
                workspaceComputerText.setText(""); //$NON-NLS-1$
            }

            if (workspaceDetails.getComment() != null) {
                try {
                    respondToCommentTextChanges = false;
                    commentText.setText(workspaceDetails.getComment());
                } finally {
                    respondToCommentTextChanges = true;
                }
            } else {
                commentText.setText(""); //$NON-NLS-1$
            }

            try {
                respondToLocationComboChanges = false;
                workspaceLocationComboViewer.setSelection(
                    new StructuredSelection(workspaceDetails.getWorkspaceLocation()));
                workspaceLocationCombo.setEnabled(!workspaceDetails.isWorkspaceLocationReadOnly());
            } finally {
                respondToLocationComboChanges = true;
            }

            try {
                respondToFileTimeComboChanges = false;
                workspaceFileTimeComboViewer.setSelection(
                    new StructuredSelection(workspaceDetails.getWorkspaceOptions()));
                workspaceFileTimeCombo.setEnabled(!workspaceDetails.isWorkspaceOptionsReadOnly());
            } finally {
                respondToFileTimeComboChanges = true;
            }

            if (connection.getVersionControlClient().getServerSupportedFeatures().contains(
                SupportedFeatures.WORKSPACE_PERMISSIONS)) {
                try {
                    if (workspaceDetails.getPermissionProfile() != null) {
                        respondToPermissionComboChanges = false;

                        if (workspaceDetails.getPermissionProfile().getBuiltinIndex() < 0) {
                            workspacePermissionsComboViewer.add(workspaceDetails.getPermissionProfile());
                        }

                        workspacePermissionsComboViewer.setSelection(
                            new StructuredSelection(workspaceDetails.getPermissionProfile()));
                        workspacePermissionsCombo.setEnabled(!workspaceDetails.isWorkspacePermissionProfileReadOnly());
                    }
                } finally {
                    respondToPermissionComboChanges = true;
                }

                /*
                 * if the user does not have permission to edit the workspace
                 * (e.g not an owner accesses a PublicLimited workspace), make
                 * the form read-only
                 */
                if (!workspaceDetails.isAdministerAllowed()) {
                    workspaceNameText.setEditable(false);
                    workspaceOwnerText.setEditable(false);
                    workspaceComputerText.setEditable(false);
                    workspaceServerText.setEditable(false);
                    commentText.setEditable(false);

                    workspaceFileTimeCombo.setEnabled(false);
                    workspaceLocationCombo.setEnabled(false);
                    workspacePermissionsCombo.setEnabled(false);

                    /*
                     * TODO: Make the working folder table read-only. By now we
                     * expect that either the server or the
                     * VersionControlClient#updateWorkspace will throw an error
                     * with a reasonable message
                     */
                }
            } else {
                workspacePermissionsComboViewer.setSelection(
                    new StructuredSelection(WorkspacePermissionProfile.getPrivateProfile()));
                workspacePermissionsCombo.setEnabled(false);
                workspacePermissionsDescription.setText(
                    Messages.getString("WorkspaceDetailsControl.WorkspacePermissionUnsupported")); //$NON-NLS-1$
            }
        }

        workspaceNameText.setFocus();
        validator.setValid(valid);
    }

    private void commentTextModified(final ModifyEvent e) {
        if (!respondToCommentTextChanges) {
            return;
        }

        final String value = ((Text) (e.widget)).getText();

        /*
         * A null comment in the details leaves the existing value when the
         * workspace is updated, so use an empty string for blank.
         */
        workspaceDetails.setComment(value);
    }

    private void workspaceNameTextModified(final ModifyEvent e) {
        if (!respondToWorkspaceNameTextChanges) {
            return;
        }

        final String value = ((Text) (e.widget)).getText();
        workspaceDetails.setName(value);
        validator.setValid(value.length() > 0);
    }

    private void locationComboSelected(final SelectionChangedEvent event) {
        if (!respondToLocationComboChanges || !(event.getSelection() instanceof IStructuredSelection)) {
            return;
        }

        final IStructuredSelection selection = (IStructuredSelection) event.getSelection();
        final WorkspaceLocation workspaceLocation = (WorkspaceLocation) selection.getFirstElement();

        workspaceDetails.setWorkspaceLocation(workspaceLocation);
    }

    private void fileTimeComboSelected(final SelectionChangedEvent event) {
        if (!respondToFileTimeComboChanges || !(event.getSelection() instanceof IStructuredSelection)) {
            return;
        }

        final IStructuredSelection selection = (IStructuredSelection) event.getSelection();
        final WorkspaceOptions workspaceOptions = (WorkspaceOptions) selection.getFirstElement();

        workspaceDetails.setWorkspaceOptions(workspaceOptions);
    }

    private void permissionComboSelected(final SelectionChangedEvent event) {
        if (!(event.getSelection() instanceof IStructuredSelection)) {
            return;
        }

        final IStructuredSelection selection = (IStructuredSelection) event.getSelection();
        final WorkspacePermissionProfile permissionProfile = (WorkspacePermissionProfile) selection.getFirstElement();
        final String profileDescription = permissionProfileDescription(permissionProfile.getBuiltinIndex());

        workspacePermissionsDescription.setText(profileDescription);

        if (respondToPermissionComboChanges) {
            workspaceDetails.setPermissionProfile(permissionProfile);
        }
    }

    @Override
    public Validator getValidator() {
        return validator;
    }

    private String permissionProfileName(final int permissionProfileBuiltinIndex) {
        switch (permissionProfileBuiltinIndex) {
            case WorkspacePermissionProfile.BUILTIN_PROFILE_INDEX_PRIVATE:
                return Messages.getString("WorkspaceDetailsControl.PrivateWorkspace"); //$NON-NLS-1$
            case WorkspacePermissionProfile.BUILTIN_PROFILE_INDEX_PUBLIC_LIMITED:
                return Messages.getString("WorkspaceDetailsControl.PublicLimitedWorkspace"); //$NON-NLS-1$
            case WorkspacePermissionProfile.BUILTIN_PROFILE_INDEX_PUBLIC:
                return Messages.getString("WorkspaceDetailsControl.PublicWorkspace"); //$NON-NLS-1$
            default:
                return Messages.getString("WorkspaceDetailsControl.CustomWorkspace"); //$NON-NLS-1$
        }
    }

    private String permissionProfileDescription(final int permissionProfileBuiltinIndex) {
        switch (permissionProfileBuiltinIndex) {
            case WorkspacePermissionProfile.BUILTIN_PROFILE_INDEX_PRIVATE:
                return Messages.getString("WorkspaceDetailsControl.PermissionPrivateDescription"); //$NON-NLS-1$
            case WorkspacePermissionProfile.BUILTIN_PROFILE_INDEX_PUBLIC_LIMITED:
                return Messages.getString("WorkspaceDetailsControl.PermissionPublicLimitedDescription"); //$NON-NLS-1$
            case WorkspacePermissionProfile.BUILTIN_PROFILE_INDEX_PUBLIC:
                return Messages.getString("WorkspaceDetailsControl.PermissionPublicDescription"); //$NON-NLS-1$
            default:
                return Messages.getString("WorkspaceDetailsControl.PermissionCustomDescription"); //$NON-NLS-1$
        }
    }

    private Point permissionProfileDescriptionLabelSize(final int maxWidth) {
        int x = 0;
        int y = 0;

        for (int i = -1; i < WorkspacePermissionProfile.getBuiltInProfiles().length; i++) {
            final String description = permissionProfileDescription(i);
            final String[] wrappedDescription = wrapString(description, maxWidth);

            for (int j = 0; j < wrappedDescription.length; j++) {
                x = Math.max(x, wrappedDescription[j].length());
            }

            y = Math.max(y, wrappedDescription.length);
        }

        return new Point(Math.max(x, maxWidth), y);
    }

    private String[] wrapString(final String s, final int maxWidth) {
        final ArrayList<String> result = new ArrayList<String>();

        final String[] words = s.split(" "); //$NON-NLS-1$
        String t = StringUtil.EMPTY;

        for (int i = 0; i < words.length; i++) {
            final String word = words[i];

            if (t.length() + word.length() + 1 >= maxWidth) {
                if (t.length() > 0) {
                    result.add(t);
                }

                t = word;
            }

            t += " " + word; //$NON-NLS-1$
        }

        if (t.length() > 0) {
            result.add(t);
        }

        return result.toArray(new String[result.size()]);
    }
}
