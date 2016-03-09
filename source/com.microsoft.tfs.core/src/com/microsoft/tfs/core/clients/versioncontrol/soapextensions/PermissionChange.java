// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.soapextensions;

import com.microsoft.tfs.core.Messages;
import com.microsoft.tfs.util.Check;

import ms.tfs.versioncontrol.clientservices._03._PermissionChange;

/**
 * Represents a change of permissions on a source control item.
 *
 * @threadsafety thread-compatible
 * @since TEE-SDK-10.1
 */
public abstract class PermissionChange extends SecurityChange {
    /*
     * The global and item permission strings must match TFS's values.
     */

    public static final String GLOBAL_PERMISSION_ADMIN_CONFIGURATION = "AdminConfiguration"; //$NON-NLS-1$
    public static final String GLOBAL_PERMISSION_ADMIN_CONNECTIONS = "AdminConnections"; //$NON-NLS-1$
    public static final String GLOBAL_PERMISSION_ADMIN_SHELVESETS = "AdminShelvesets"; //$NON-NLS-1$
    public static final String GLOBAL_PERMISSION_ADMIN_WORKSPACES = "AdminWorkspaces"; //$NON-NLS-1$
    public static final String GLOBAL_PERMISSION_CREATE_WORKSPACE = "CreateWorkspace"; //$NON-NLS-1$

    public static final String ITEM_PERMISSION_ADMIN_PROJECT_RIGHTS = "AdminProjectRights"; //$NON-NLS-1$
    public static final String ITEM_PERMISSION_CHECKIN = "Checkin"; //$NON-NLS-1$
    public static final String ITEM_PERMISSION_CHECKIN_OTHER = "CheckinOther"; //$NON-NLS-1$
    public static final String ITEM_PERMISSION_LABEL = "Label"; //$NON-NLS-1$
    public static final String ITEM_PERMISSION_LABEL_OTHER = "LabelOther"; //$NON-NLS-1$
    public static final String ITEM_PERMISSION_LOCK = "Lock"; //$NON-NLS-1$
    public static final String ITEM_PERMISSION_MANAGE_BRANCH = "ManageBranch"; //$NON-NLS-1$
    public static final String ITEM_PERMISSION_MERGE = "Merge"; //$NON-NLS-1$
    public static final String ITEM_PERMISSION_PEND_CHANGE = "PendChange"; //$NON-NLS-1$
    public static final String ITEM_PERMISSION_READ = "Read"; //$NON-NLS-1$
    public static final String ITEM_PERMISSION_REVISE_OTHER = "ReviseOther"; //$NON-NLS-1$
    public static final String ITEM_PERMISSION_UNDO_OTHER = "UndoOther"; //$NON-NLS-1$
    public static final String ITEM_PERMISSION_UNLOCK_OTHER = "UnlockOther"; //$NON-NLS-1$

    public final static String[] getAllGlobalPermissions() {
        return new String[] {
            GLOBAL_PERMISSION_CREATE_WORKSPACE,
            GLOBAL_PERMISSION_ADMIN_WORKSPACES,
            GLOBAL_PERMISSION_ADMIN_SHELVESETS,
            GLOBAL_PERMISSION_ADMIN_CONNECTIONS,
            GLOBAL_PERMISSION_ADMIN_CONFIGURATION
        };
    }

    public final static String[] getAllGlobalLocalizedPermissions() {
        return new String[] {
            Messages.getString("PermissionChange.CreateAWorkspace"), //$NON-NLS-1$
            Messages.getString("PermissionChange.AdministerWorkspaces"), //$NON-NLS-1$
            Messages.getString("PermissionChange.AdministerShelvedChanges"), //$NON-NLS-1$
            Messages.getString("PermissionChange.AdministerSourceControlConnections"), //$NON-NLS-1$
            Messages.getString("PermissionChange.AdministerSourceControlConfigurations") //$NON-NLS-1$
        };
    }

    public final static String[] getAllItemPermissions() {
        return new String[] {
            ITEM_PERMISSION_READ,
            ITEM_PERMISSION_PEND_CHANGE,
            ITEM_PERMISSION_CHECKIN,
            ITEM_PERMISSION_LABEL,
            ITEM_PERMISSION_LOCK,
            ITEM_PERMISSION_REVISE_OTHER,
            ITEM_PERMISSION_UNLOCK_OTHER,
            ITEM_PERMISSION_UNDO_OTHER,
            ITEM_PERMISSION_LABEL_OTHER,
            ITEM_PERMISSION_ADMIN_PROJECT_RIGHTS,
            ITEM_PERMISSION_CHECKIN_OTHER,
            ITEM_PERMISSION_MERGE,
            ITEM_PERMISSION_MANAGE_BRANCH
        };
    }

    public final static String[] getAllLocalizedItemPermissions() {
        return new String[] {
            Messages.getString("PermissionChange.Read"), //$NON-NLS-1$
            Messages.getString("PermissionChange.CheckOut"), //$NON-NLS-1$
            Messages.getString("PermissionChange.CheckIn"), //$NON-NLS-1$
            Messages.getString("PermissionChange.Label"), //$NON-NLS-1$
            Messages.getString("PermissionChange.Lock"), //$NON-NLS-1$
            Messages.getString("PermissionChange.ReviseOtherUsersChanges"), //$NON-NLS-1$
            Messages.getString("PermissionChange.UnlockOtherUsersChanges"), //$NON-NLS-1$
            Messages.getString("PermissionChange.UndoOtherUsersChanges"), //$NON-NLS-1$
            Messages.getString("PermissionChange.AdministerLabels"), //$NON-NLS-1$
            Messages.getString("PermissionChange.ManipulateSecuritySettings"), //$NON-NLS-1$
            Messages.getString("PermissionChange.CheckInOtherUsersChanges"), //$NON-NLS-1$
            Messages.getString("PermissionChange.Merge"), //$NON-NLS-1$
            Messages.getString("PermissionChange.ManageBranch") //$NON-NLS-1$
        };
    }

    /**
     * Constructs a {@link PermissionChange} for the given item with the given
     * permissions.
     *
     * @param item
     *        the item whose permissions are being changed (must not be
     *        <code>null</code> or empty)
     */
    public PermissionChange(
        final String item,
        final String identity,
        final String displayName,
        final String[] allows,
        final String[] denies,
        final String[] removes) {
        super(new _PermissionChange(item, identity, displayName, allows, denies, removes));

        Check.notNullOrEmpty(identity, "identity"); //$NON-NLS-1$
    }

    public PermissionChange(final _PermissionChange change) {
        super(change);
    }

    /**
     * Gets the web service object this class wraps. The returned object should
     * not be modified.
     *
     * @return the web service object this class wraps.
     */
    public _PermissionChange getWebServiceObject() {
        return (_PermissionChange) webServiceObject;
    }

    public String getIdentityName() {
        return getWebServiceObject().getIdent();
    }

    public void setIdentityName(final String identityName) {
        Check.notNull(identityName, "identityName"); //$NON-NLS-1$
        getWebServiceObject().setIdent(identityName);
    }

    public String[] getAllow() {
        return getWebServiceObject().getAllow().clone();
    }

    public void setAllow(final String[] allow) {
        Check.notNull(allow, "allow"); //$NON-NLS-1$
        getWebServiceObject().setAllow(allow);
    }

    public String[] getDeny() {
        return getWebServiceObject().getDeny().clone();
    }

    public void setDeny(final String[] deny) {
        Check.notNull(deny, "deny"); //$NON-NLS-1$
        getWebServiceObject().setDeny(deny);
    }

    public String[] getRemove() {
        return getWebServiceObject().getRemove().clone();
    }

    public void setRemove(final String[] remove) {
        Check.notNull(remove, "remove"); //$NON-NLS-1$
        getWebServiceObject().setRemove(remove);
    }
}
