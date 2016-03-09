// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.clc.vc.commands;

import java.net.MalformedURLException;
import java.text.MessageFormat;

import com.microsoft.tfs.client.clc.AcceptedOptionSet;
import com.microsoft.tfs.client.clc.Messages;
import com.microsoft.tfs.client.clc.commands.Command;
import com.microsoft.tfs.client.clc.exceptions.ArgumentException;
import com.microsoft.tfs.client.clc.exceptions.CLCException;
import com.microsoft.tfs.client.clc.exceptions.InvalidFreeArgumentException;
import com.microsoft.tfs.client.clc.exceptions.InvalidOptionException;
import com.microsoft.tfs.client.clc.exceptions.InvalidOptionValueException;
import com.microsoft.tfs.client.clc.exceptions.LicenseException;
import com.microsoft.tfs.client.clc.options.Option;
import com.microsoft.tfs.client.clc.options.shared.OptionCollection;
import com.microsoft.tfs.client.clc.options.shared.OptionDelete;
import com.microsoft.tfs.client.clc.options.shared.OptionNew;
import com.microsoft.tfs.client.clc.vc.options.OptionComment;
import com.microsoft.tfs.client.clc.vc.options.OptionComputer;
import com.microsoft.tfs.client.clc.vc.options.OptionFileTime;
import com.microsoft.tfs.client.clc.vc.options.OptionLocation;
import com.microsoft.tfs.client.clc.vc.options.OptionNewName;
import com.microsoft.tfs.client.clc.vc.options.OptionNoPrompt;
import com.microsoft.tfs.client.clc.vc.options.OptionPermission;
import com.microsoft.tfs.client.clc.vc.options.OptionTemplate;
import com.microsoft.tfs.core.TFSTeamProjectCollection;
import com.microsoft.tfs.core.clients.versioncontrol.VersionControlClient;
import com.microsoft.tfs.core.clients.versioncontrol.VersionControlConstants;
import com.microsoft.tfs.core.clients.versioncontrol.WebServiceLevel;
import com.microsoft.tfs.core.clients.versioncontrol.WorkspaceLocation;
import com.microsoft.tfs.core.clients.versioncontrol.WorkspaceOptions;
import com.microsoft.tfs.core.clients.versioncontrol.WorkspacePermissionProfile;
import com.microsoft.tfs.core.clients.versioncontrol.exceptions.FeatureNotSupportedException;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.WorkingFolder;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Workspace;
import com.microsoft.tfs.core.clients.versioncontrol.specs.WorkspaceSpec;
import com.microsoft.tfs.core.clients.versioncontrol.specs.WorkspaceSpecParseException;
import com.microsoft.tfs.core.clients.versioncontrol.workspacecache.WorkspaceInfo;
import com.microsoft.tfs.jni.helpers.LocalHost;
import com.microsoft.tfs.util.Check;

public final class CommandWorkspace extends Command {
    public CommandWorkspace() {
        super();
    }

    /*
     * (non-Javadoc)
     *
     * @see com.microsoft.tfs.client.clc.Command#run()
     */
    @Override
    public void run() throws ArgumentException, MalformedURLException, CLCException, LicenseException {
        final boolean createNew = findOptionType(OptionNew.class) != null;
        final boolean delete = findOptionType(OptionDelete.class) != null;

        if (createNew && delete) {
            throw new InvalidOptionException(Messages.getString("CommandWorkspace.NewOrDeleteButNotBoth")); //$NON-NLS-1$
        }

        String computer = null;
        String template = null;
        String comment = null;
        String newName = null;
        WorkspaceLocation location = null;
        WorkspaceOptions workspaceOptions = null;
        WorkspacePermissionProfile permissionProfile = null;

        Option o = null;

        if ((o = findOptionType(OptionComputer.class)) != null) {
            computer = ((OptionComputer) o).getValue();
        }

        if ((o = findOptionType(OptionTemplate.class)) != null) {
            template = ((OptionTemplate) o).getValue();
        }

        if ((o = findOptionType(OptionComment.class)) != null) {
            comment = ((OptionComment) o).getValue();
        }

        if ((o = findOptionType(OptionNewName.class)) != null) {
            newName = ((OptionNewName) o).getValue();
        }

        if ((o = findOptionType(OptionLocation.class)) != null) {
            final String locationString = ((OptionLocation) o).getValue();

            if (OptionLocation.SERVER.equalsIgnoreCase(locationString)) {
                location = WorkspaceLocation.SERVER;
            } else if (OptionLocation.LOCAL.equalsIgnoreCase(locationString)) {
                location = WorkspaceLocation.LOCAL;
            }
        }

        if ((o = findOptionType(OptionFileTime.class)) != null) {
            workspaceOptions = ((OptionFileTime) o).updateWorkspaceOptions(workspaceOptions);
        }

        if ((o = findOptionType(OptionPermission.class)) != null) {
            final String permissionString = ((OptionPermission) o).getValue();

            if (OptionPermission.PRIVATE.equalsIgnoreCase(permissionString)) {
                permissionProfile = WorkspacePermissionProfile.getPrivateProfile();
            } else if (OptionPermission.PUBLICLIMITED.equalsIgnoreCase(permissionString)) {
                permissionProfile = WorkspacePermissionProfile.getPublicLimitedProfile();
            } else if (OptionPermission.PUBLIC.equalsIgnoreCase(permissionString)) {
                permissionProfile = WorkspacePermissionProfile.getPublicProfile();
            }
        }

        if (createNew) {
            reportBadOptionCombinationIfPresent(OptionNew.class, OptionNewName.class);
            reportBadOptionCombinationIfPresent(OptionNew.class, OptionDelete.class);

            createNewWorkspace(template, computer, comment, location, workspaceOptions, permissionProfile);
        } else if (delete) {
            reportBadOptionCombinationIfPresent(OptionDelete.class, OptionTemplate.class);
            reportBadOptionCombinationIfPresent(OptionDelete.class, OptionComment.class);
            reportBadOptionCombinationIfPresent(OptionDelete.class, OptionNewName.class);
            reportBadOptionCombinationIfPresent(OptionDelete.class, OptionComputer.class);
            reportBadOptionCombinationIfPresent(OptionDelete.class, OptionPermission.class);

            deleteWorkspace();
        } else {
            /*
             * Neither /new or /delete was specified, so we're doing an edit.
             */
            if (computer != null) {
                throw new InvalidOptionException(Messages.getString("CommandWorkspace.ComptuerNotValidWhenEditing")); //$NON-NLS-1$
            }

            if (template != null) {
                throw new InvalidOptionException(Messages.getString("CommandWorkspace.TemplateNotValidWhenEditing")); //$NON-NLS-1$
            }

            editWorkspace(comment, newName, location, workspaceOptions, permissionProfile);
        }
    }

    /**
     * @param location
     *        the workspace location or <code>null</code> to use the server's
     *        default
     * @param workspaceOptions
     *        the workspace options or <code>null</code> to use the default
     */
    private void createNewWorkspace(
        final String template,
        String computer,
        String comment,
        final WorkspaceLocation location,
        final WorkspaceOptions workspaceOptions,
        final WorkspacePermissionProfile permissionProfile)
            throws ArgumentException,
                CLCException,
                MalformedURLException,
                LicenseException {
        if (getFreeArguments().length > 1) {
            throw new InvalidFreeArgumentException(Messages.getString("CommandWorkspace.OnlyOneNameWhenCreating")); //$NON-NLS-1$
        }

        final TFSTeamProjectCollection connection = createConnection(true);
        final VersionControlClient client = connection.getVersionControlClient();
        initializeClient(client);

        if (workspaceOptions != null
            && workspaceOptions.contains(WorkspaceOptions.SET_FILE_TO_CHECKIN)
            && client.getServiceLevel().getValue() < WebServiceLevel.TFS_2012.getValue()) {
            throw new FeatureNotSupportedException(Messages.getString("CommandWorkspace.SetFileTimeNotSupported")); //$NON-NLS-1$
        }

        if (permissionProfile != null
            && !permissionProfile.equals(WorkspacePermissionProfile.getPrivateProfile())
            && client.getServiceLevel().getValue() < WebServiceLevel.TFS_2010.getValue()) {
            throw new FeatureNotSupportedException(Messages.getString("CommandWorkspace.PublicWorkspacesNotSupported")); //$NON-NLS-1$
        }

        WorkingFolder[] workingFolders = null;

        /*
         * If a template was supplied, use it to gather the comment and working
         * folders, else use some reasonable defaults.
         */
        if (template != null) {
            /*
             * Get the template workspace to use as default values for the new
             * workspace.
             */

            WorkspaceSpec spec;
            try {
                spec = WorkspaceSpec.parse(template, connection.getAuthorizedTFSUser().toString());
            } catch (final WorkspaceSpecParseException e) {
                throw new InvalidOptionValueException(e.getMessage());
            }

            final Workspace[] ws = client.queryWorkspaces(spec.getName(), spec.getOwner(), null);

            if (ws == null || ws.length == 0) {
                final String messageFormat =
                    Messages.getString("CommandWorkspace.WorkspaceTemplateCouldNotBeFoundFormat"); //$NON-NLS-1$
                final String message = MessageFormat.format(messageFormat, spec.getName());

                throw new InvalidOptionValueException(message);
            }

            Check.isTrue(ws.length == 1, "ws.length == 1"); //$NON-NLS-1$

            /*
             * If the user is supplying a comment, it should override the
             * template's comment.
             */
            if (comment == null) {
                comment = ws[0].getComment();
            }

            workingFolders = ws[0].getFolders();
        } else {
            // No initial working folders.
            workingFolders = new WorkingFolder[0];
        }

        /*
         * Figure out what name the user wanted.
         */
        String name = null;
        String ownerDisplay = null;
        String ownerUnique;

        if (getFreeArguments().length == 1) {
            final String specString = getFreeArguments()[0];
            Check.notNull(specString, "specString"); //$NON-NLS-1$

            WorkspaceSpec spec;

            try {
                spec = WorkspaceSpec.parse(specString, connection.getAuthorizedTFSUser().toString());
            } catch (final WorkspaceSpecParseException e) {
                throw new InvalidOptionValueException(e.getMessage());
            }

            name = spec.getName();
            ownerDisplay = spec.getOwner();
            ownerUnique = ownerDisplay;
        } else {
            /*
             * No name was specified, so use the default, which is this
             * machine's short name.
             */
            name = LocalHost.getShortName();

            if (comment == null) {
                comment = ""; //$NON-NLS-1$
            }

            ownerDisplay = client.getConnection().getAuthorizedIdentity().getDisplayName();
            ownerUnique = client.getConnection().getAuthorizedAccountName();
        }

        if (comment == null) {
            comment = ""; //$NON-NLS-1$
        }

        if (computer == null) {
            computer = LocalHost.getShortName();
        }

        final Workspace result = client.createWorkspace(
            workingFolders,
            name,
            ownerUnique,
            ownerDisplay,
            comment,
            location,
            workspaceOptions,
            permissionProfile);
        Check.notNull(result, "result"); //$NON-NLS-1$

        getDisplay().printLine(
            MessageFormat.format(Messages.getString("CommandWorkspace.WorkspaceCreatedFormat"), result.getName())); //$NON-NLS-1$
    }

    private void deleteWorkspace() throws ArgumentException, MalformedURLException, CLCException, LicenseException {
        if (getFreeArguments().length != 1) {
            throw new InvalidFreeArgumentException(
                Messages.getString("CommandWorkspace.OneWorkspaceNameRequiredForDelete")); //$NON-NLS-1$
        }

        final TFSTeamProjectCollection connection = createConnection(true);
        final VersionControlClient client = connection.getVersionControlClient();
        initializeClient(client);

        /*
         * Parse out the workspace spec string.
         */
        final String specString = getFreeArguments()[0];
        Check.notNull(specString, "specString"); //$NON-NLS-1$

        WorkspaceSpec spec;
        try {
            spec = WorkspaceSpec.parse(specString, VersionControlConstants.AUTHENTICATED_USER);
        } catch (final WorkspaceSpecParseException e) {
            throw new InvalidOptionValueException(e.getMessage());
        }

        /*
         * TODO If there are multiple workspaces with the name given by the
         * user, don't pick one based on the default workspace.
         */

        final Workspace[] workspaces = client.queryWorkspaces(spec.getName(), spec.getOwner(), null);

        if (workspaces == null || workspaces.length == 0) {
            final String messageFormat = Messages.getString("CommandWorkspace.WorkspaceCouldNotBeFoundFormat"); //$NON-NLS-1$
            final String message = MessageFormat.format(messageFormat, spec.getName());

            throw new InvalidOptionValueException(message);
        }

        if (workspaces.length > 1) {
            final String messageFormat = Messages.getString("CommandWorkspace.MultipleWorkspacesMatchSpecFormat"); //$NON-NLS-1$
            final String message = MessageFormat.format(messageFormat, specString);

            throw new CLCException(message);
        }

        client.deleteWorkspace(workspaces[0]);

        final String messageFormat = Messages.getString("CommandWorkspace.WorkspaceDeletedFormat"); //$NON-NLS-1$
        final String message = MessageFormat.format(messageFormat, workspaces[0].getName());

        getDisplay().printLine(message);
    }

    private void editWorkspace(
        final String comment,
        final String newName,
        final WorkspaceLocation newLocation,
        final WorkspaceOptions workspaceOptions,
        final WorkspacePermissionProfile permissionProfile)
            throws ArgumentException,
                MalformedURLException,
                CLCException,
                LicenseException {
        if (getFreeArguments().length > 1) {
            throw new InvalidFreeArgumentException(Messages.getString("CommandWorkspace.OnlyOneNameWhenEditing")); //$NON-NLS-1$
        }

        /*
         * If there's a workspace spec as a free argument, parse it, otherwise
         * attempt to figure it out the normal way.
         */

        String workspaceName = null;
        String workspaceOwner = null;

        if (getFreeArguments().length == 1) {
            /*
             * Use the named workspace.
             */
            final String specString = getFreeArguments()[0];

            WorkspaceSpec spec;
            try {
                spec = WorkspaceSpec.parse(specString, VersionControlConstants.AUTHENTICATED_USER);
            } catch (final WorkspaceSpecParseException e) {
                throw new InvalidOptionValueException(e.getMessage());
            }
            workspaceName = spec.getName();
            workspaceOwner = spec.getOwner();
        } else {
            /*
             * Use the cached workspace.
             */
            final WorkspaceInfo cw = determineCachedWorkspace();
            workspaceName = cw.getName();
            workspaceOwner = cw.getOwnerName();
        }

        /*
         * At least one kind of edit must be specified.
         */
        if (findOptionType(OptionNewName.class) == null
            && findOptionType(OptionComment.class) == null
            && findOptionType(OptionFileTime.class) == null
            && findOptionType(OptionPermission.class) == null) {
            throw new InvalidOptionException(
                Messages.getString("CommandWorkspace.SpecifyAtLeastNewnameOrCommentOrFileTime")); //$NON-NLS-1$
        }

        final TFSTeamProjectCollection connection = createConnection(true);
        final VersionControlClient client = connection.getVersionControlClient();
        initializeClient(client);

        if (workspaceOptions != null
            && workspaceOptions.contains(WorkspaceOptions.SET_FILE_TO_CHECKIN)
            && client.getServiceLevel().getValue() < WebServiceLevel.TFS_2012.getValue()) {
            throw new FeatureNotSupportedException(Messages.getString("CommandWorkspace.SetFileTimeNotSupported")); //$NON-NLS-1$
        }

        if (permissionProfile != null
            && !permissionProfile.equals(WorkspacePermissionProfile.getPrivateProfile())
            && client.getServiceLevel().getValue() < WebServiceLevel.TFS_2010.getValue()) {
            throw new FeatureNotSupportedException(Messages.getString("CommandWorkspace.PublicWorkspacesNotSupported")); //$NON-NLS-1$
        }

        final Workspace[] workspaces = client.queryWorkspaces(workspaceName, workspaceOwner, LocalHost.getShortName());

        if (workspaces == null || workspaces.length == 0) {
            final String messageFormat = Messages.getString("CommandWorkspace.WorkspaceCouldNotBeFoundFormat"); //$NON-NLS-1$
            final String message = MessageFormat.format(messageFormat, workspaceName);

            throw new InvalidOptionValueException(message);
        }

        if (workspaces.length > 1) {
            final String messageFormat = Messages.getString("CommandWorkspace.MultipleWorkspacesMatchSpecFormat"); //$NON-NLS-1$
            final String message = MessageFormat.format(messageFormat, getFreeArguments()[0]);

            throw new CLCException(message);
        }

        final WorkspaceOptions oldOptions = workspaces[0].getOptions();

        /*
         * We have one workspace to edit. If the new values are null, the
         * existing values will persist.
         */

        workspaces[0].update(
            newName,
            null,
            comment,
            null,
            null,
            permissionProfile,
            false,
            workspaceOptions,
            newLocation);

        getDisplay().printLine(MessageFormat.format(
            Messages.getString("CommandWorkspace.WorkspaceUpdatedFormat"), //$NON-NLS-1$
            workspaces[0].getName()));

        if (!oldOptions.contains(WorkspaceOptions.SET_FILE_TO_CHECKIN)
            && workspaces[0].getOptions().contains(WorkspaceOptions.SET_FILE_TO_CHECKIN)) {
            getDisplay().printLine(""); //$NON-NLS-1$
            getDisplay().printLine(Messages.getString("CommandWorkspace.SetFileTimeToCheckinGetWarning")); //$NON-NLS-1$
        }
    }

    @Override
    public AcceptedOptionSet[] getSupportedOptionSets() {
        final AcceptedOptionSet[] optionSets = new AcceptedOptionSet[3];

        // This optionSet includes a required option (new).
        optionSets[0] = new AcceptedOptionSet(new Class[] {
            OptionNoPrompt.class,
            OptionTemplate.class,
            OptionComputer.class,
            OptionComment.class,
            OptionCollection.class,
            OptionLocation.class,
            OptionFileTime.class,
            OptionPermission.class
        }, "[<workspacename;[workspaceowner]>]", new Class[] //$NON-NLS-1$
        {
            OptionNew.class
        });

        // This optionSet includes a required option (delete).
        optionSets[1] = new AcceptedOptionSet(new Class[] {
            OptionCollection.class
        }, "[<workspacename;[workspaceowner]>]", new Class[] //$NON-NLS-1$
        {
            OptionDelete.class
        });

        // No required option (edit)
        optionSets[2] = new AcceptedOptionSet(new Class[] {
            OptionCollection.class,
            OptionComment.class,
            OptionNewName.class,
            OptionFileTime.class,
            OptionPermission.class
        }, "[<workspacename;[workspaceowner]>]"); //$NON-NLS-1$

        return optionSets;
    }

    @Override
    public String[] getCommandHelpText() {
        return new String[] {
            Messages.getString("CommandWorkspace.HelpText1") //$NON-NLS-1$
        };
    }
}
