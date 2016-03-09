// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol;

import java.text.MessageFormat;

import com.microsoft.tfs.core.Messages;
import com.microsoft.tfs.core.checkinpolicies.PolicyDefinition;
import com.microsoft.tfs.core.clients.versioncontrol.path.ServerPath;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Failure;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Item;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.ItemType;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.SeverityType;
import com.microsoft.tfs.util.Check;

/**
 * Used by check-in policies, describes the version control item at the root of
 * a team project.
 *
 * @threadsafety thread-compatible
 * @since TEE-SDK-10.1
 */
public class TeamProject {
    private final Item item;
    private final VersionControlClient client;

    /**
     * Creates a {@link TeamProject} for an item.
     *
     * @param item
     *        the item that describes the team project (must not be
     *        <code>null</code>)
     */
    public TeamProject(final Item item, final VersionControlClient client) {
        Check.notNull(item, "item"); //$NON-NLS-1$
        Check.notNull(client, "client"); //$NON-NLS-1$

        this.item = item;
        this.client = client;
    }

    /**
     * Creates new {@link TeamProject} objects for the given items.
     *
     * @param items
     *        the items to wrap with {@link TeamProject}s (must not be
     *        <code>null</code>)
     * @return an array of team projects, one for each given item.
     */
    public static TeamProject[] fromItems(final Item[] items, final VersionControlClient client) {
        Check.notNull(items, "items"); //$NON-NLS-1$
        Check.notNull(client, "client"); //$NON-NLS-1$

        final TeamProject[] ret = new TeamProject[items.length];

        for (int i = 0; i < items.length; i++) {
            ret[i] = new TeamProject(items[i], client);
        }

        return ret;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(final Object obj) {
        if (obj == null) {
            return false;
        }
        if (obj instanceof TeamProject == false) {
            return false;
        }
        if (obj == this) {
            return true;
        }

        final TeamProject other = (TeamProject) obj;

        if (getVersionControlClient().getServerGUID().equals(other.getVersionControlClient().getServerGUID())) {
            return getName().equals(other.getName());
        }

        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        int result = 17;

        result = result * 37 + getVersionControlClient().getServerGUID().hashCode();
        result = result * 37 + getName().hashCode();

        return result;
    }

    /**
     * @return the server path where this team project is located.
     */
    public String getServerItem() {
        return item.getServerItem();
    }

    /**
     * @return the name of this team project.
     */
    public String getName() {
        return ServerPath.getTeamProjectName(getServerItem());
    }

    /**
     * @return the {@link VersionControlClient} that can access this team
     *         project (may be <code>null</code>)
     */
    public VersionControlClient getVersionControlClient() {
        return client;
    }

    /**
     * @return the ID number for this team project's item.
     */
    public int getItemID() {
        return item.getItemID();
    }

    /**
     * Gets the policies defined for this team project.
     *
     * @return the policies that are defined on this team project.
     */
    public PolicyDefinition[] getCheckinPolicies() {
        return getVersionControlClient().getCheckinPoliciesForServerPaths(new String[] {
            getServerItem()
        });
    }

    /**
     * Validates if given server items is a root or team project path. If so,
     * creates apropriate failure. Does not validate if item represents existing
     * team project or child of existing team project.
     *
     * @param serverItem
     *        the server item where the change is being made (must not be
     *        <code>null</code> or empty)
     * @param type
     *        the type of the server item where the change is being made (must
     *        not be <code>null</code>)
     * @return <code>null</code> if the operation is valid, a {@link Failure} if
     *         the serverItem is root or team project folder or some other error
     *         condition
     */
    public static Failure validateChange(final String serverItem, final ItemType type) {
        Check.notNullOrEmpty(serverItem, "serverItem"); //$NON-NLS-1$
        Check.notNull(type, "type"); //$NON-NLS-1$

        if (ServerPath.isRootFolder(serverItem)) {
            return new Failure(
                Messages.getString("TeamProject.CanNotChangeRootFolderException"), //$NON-NLS-1$
                FailureCodes.CANNOT_CHANGE_ROOT_FOLDER_EXCEPTION,
                SeverityType.ERROR,
                serverItem);
        }

        if (ServerPath.isTeamProject(serverItem)) {
            // We do not allow the creation of files in the root folder.
            if (type == ItemType.FILE) {
                return new Failure(
                    Messages.getString("TeamProject.CannotCreateFilesInRootException"), //$NON-NLS-1$
                    FailureCodes.CANNOT_CREATE_FILES_IN_ROOT_EXCEPTION,
                    SeverityType.ERROR,
                    serverItem);
            } else {
                return new Failure(
                    MessageFormat.format(
                        Messages.getString("TeamProject.InvalidProjectPendingChangeExceptionFormat"), //$NON-NLS-1$
                        serverItem),
                    FailureCodes.INVALID_PROJECT_PENDING_CHANGE_EXCEPTION,
                    SeverityType.ERROR,
                    serverItem);
            }
        }

        return null;
    }
}
