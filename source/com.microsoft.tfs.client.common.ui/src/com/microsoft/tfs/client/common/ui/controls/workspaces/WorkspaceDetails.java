// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.controls.workspaces;

import java.util.concurrent.atomic.AtomicBoolean;

import com.microsoft.tfs.core.TFSTeamProjectCollection;
import com.microsoft.tfs.core.clients.versioncontrol.SupportedFeatures;
import com.microsoft.tfs.core.clients.versioncontrol.VersionControlClient;
import com.microsoft.tfs.core.clients.versioncontrol.WebServiceLevel;
import com.microsoft.tfs.core.clients.versioncontrol.WorkspaceLocation;
import com.microsoft.tfs.core.clients.versioncontrol.WorkspaceOptions;
import com.microsoft.tfs.core.clients.versioncontrol.WorkspacePermissionProfile;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.ServerSettings;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Workspace;
import com.microsoft.tfs.jni.helpers.LocalHost;
import com.microsoft.tfs.util.Check;

/**
 * Holds details about a workspace. <code>null</code> values for a field mean
 * that detail should not be changed if these values are used for
 * {@link Workspace#update()}.
 */
public class WorkspaceDetails {
    // These fields can be null to signify "no change" during update
    private String name;
    private String server;
    private String owner;
    private String computer;
    private String comment;

    // These fields cannot be null
    private WorkspaceLocation workspaceLocation = WorkspaceLocation.SERVER;
    private final boolean workspaceLocationReadOnly;
    private WorkspaceOptions workspaceOptions = WorkspaceOptions.NONE;
    private final boolean workspaceOptionsReadOnly;
    private WorkspacePermissionProfile workspacePermissionProfile = WorkspacePermissionProfile.getPrivateProfile();
    private final boolean workspacePermissionProfileReadOnly;
    private boolean allowAdminister;

    private WorkspaceDetails(final VersionControlClient client) {
        Check.notNull(client, "client"); //$NON-NLS-1$

        final ServerSettings serverSettings = client.getServerSettingsWithFallback(new AtomicBoolean());

        setWorkspaceLocation(serverSettings.getDefaultWorkspaceLocation());
        setPermissionProfile(WorkspacePermissionProfile.getPrivateProfile());

        // Editable in TFS 2010 and later
        workspacePermissionProfileReadOnly =
            !client.getServerSupportedFeatures().contains(SupportedFeatures.WORKSPACE_PERMISSIONS);

        // Editable in TFS 2012 and later
        final boolean is2012OrLater = client.getServiceLevel().getValue() >= WebServiceLevel.TFS_2012.getValue();
        workspaceLocationReadOnly = !is2012OrLater;
        workspaceOptionsReadOnly = !is2012OrLater;
    }

    /*
     * A constructor for adding a new workspace in the Edit Workspace dialog
     */
    public WorkspaceDetails(final TFSTeamProjectCollection connection, final String defaultWorkspaceName) {
        this(connection != null ? connection.getVersionControlClient() : null);

        allowAdminister = true;
        name = defaultWorkspaceName;
        workspacePermissionProfile = WorkspacePermissionProfile.getPrivateProfile();

        /*
         * This attribute is for cosmetic purposes only - not editable.
         */
        server = connection.getBaseURI().getHost();

        /*
         * These two attributes are not (currently) editable in the UI or
         * settable through Core. They are included here for cosmetic reasons
         * since they are displayed in the UI.
         */
        computer = LocalHost.getShortName();
        if (connection.getVersionControlClient().getServiceLevel().getValue() >= WebServiceLevel.TFS_2012.getValue()) {
            // Use the display name here as it will be available later when
            // editing the workspace
            owner = connection.getAuthorizedIdentity().getDisplayName();
        } else {
            // For TFS 2010 and previous, only the unique name will be available
            // when editing the workspace later, so use it here for consistency.
            // VS also has this behavior.
            owner = connection.getAuthorizedIdentity().getUniqueName();
        }

    }

    /*
     * A constructor for editing an existing workspace in the Edit Workspace
     * dialog
     */
    public WorkspaceDetails(final Workspace workspace) {
        this(workspace != null ? workspace.getClient() : null);

        name = workspace.getName();
        server = workspace.getClient().getConnection().getBaseURI().getHost();
        owner = workspace.getOwnerDisplayName();
        computer = workspace.getComputer();
        comment = workspace.getComment();

        // Editable in TFS 2010 and later
        workspacePermissionProfile = workspace.getPermissionsProfile();

        allowAdminister = workspace.hasAdministerPermission();

        // Editable in TFS 2012 and later
        workspaceLocation = workspace.getLocation();
        workspaceOptions = workspace.getOptions();

        if (comment != null && comment.trim().length() == 0) {
            comment = null;
        }
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public String getServer() {
        return server;
    }

    public String getOwner() {
        return owner;
    }

    public String getComputer() {
        return computer;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(final String comment) {
        this.comment = comment;
    }

    public WorkspaceLocation getWorkspaceLocation() {
        return workspaceLocation;
    }

    public void setWorkspaceLocation(final WorkspaceLocation workspaceLocation) {
        this.workspaceLocation = workspaceLocation;
    }

    public boolean isWorkspaceLocationReadOnly() {
        return workspaceLocationReadOnly;
    }

    public WorkspacePermissionProfile getPermissionProfile() {
        return workspacePermissionProfile;
    }

    public void setPermissionProfile(final WorkspacePermissionProfile workspacePermissionProfile) {
        this.workspacePermissionProfile = workspacePermissionProfile;
    }

    public boolean isWorkspacePermissionProfileReadOnly() {
        return workspacePermissionProfileReadOnly;
    }

    public WorkspaceOptions getWorkspaceOptions() {
        return workspaceOptions;
    }

    public void setWorkspaceOptions(final WorkspaceOptions workspaceOptions) {
        this.workspaceOptions = workspaceOptions;
    }

    public boolean isWorkspaceOptionsReadOnly() {
        return workspaceOptionsReadOnly;
    }

    public boolean isAdministerAllowed() {
        return allowAdminister;
    }
}
