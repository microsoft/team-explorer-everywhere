// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol;

import java.util.ArrayList;
import java.util.List;

import com.microsoft.tfs.core.clients.security.AccessControlEntry;
import com.microsoft.tfs.core.clients.security.AccessControlEntryDetails;
import com.microsoft.tfs.core.clients.webservices.GroupWellKnownSIDConstants;
import com.microsoft.tfs.core.clients.webservices.IdentityConstants;
import com.microsoft.tfs.core.clients.webservices.IdentityDescriptor;
import com.microsoft.tfs.util.Check;

/**
 * A named collection of {@link AccessControlEntry} objects.
 *
 * @since TEE-SDK-10.1
 * @threadsafety thread-compatible
 */
public class WorkspacePermissionProfile {
    /**
     * The name of the built-in "private" profile.
     */
    public static final String BUILTIN_PROFILE_NAME_PRIVATE = "Private"; //$NON-NLS-1$

    /**
     * The name of the built-in "public limited" profile.
     */
    public static final String BUILTIN_PROFILE_NAME_PUBLIC_LIMITED = "PublicLimited"; //$NON-NLS-1$

    /**
     * The name of the built-in "public" profile.
     */
    public static final String BUILTIN_PROFILE_NAME_PUBLIC = "Public"; //$NON-NLS-1$

    /**
     * The order index of the built-in "private" profile.
     */
    public static final int BUILTIN_PROFILE_INDEX_PRIVATE = 0;

    /**
     * The order index of the built-in "public limited" profile.
     */
    public static final int BUILTIN_PROFILE_INDEX_PUBLIC_LIMITED = 1;

    /**
     * The order index of the built-in "public" profile.
     */
    public static final int BUILTIN_PROFILE_INDEX_PUBLIC = 2;

    /**
     * Holds the built-in profiles. Initialized on first access.
     */
    private static WorkspacePermissionProfile[] BUILTIN_PROFILES;
    private static final Object BUILTIN_PROFILES_LOCK = new Object();

    private final AccessControlEntry[] accessControlEntries;
    private final String name;
    private int builtinIndex = -1;

    /**
     * Creates a permission profile with the given name and access control
     * entires.
     *
     * @param profileName
     *        the name of the profile (must not be <code>null</code> or empty)
     * @param accessControlEntries
     *        the entries (not <code>null</code>, but may be empty)
     */
    public WorkspacePermissionProfile(final String profileName, final AccessControlEntry[] accessControlEntries) {
        Check.notNullOrEmpty(profileName, "profileName"); //$NON-NLS-1$
        Check.notNull(accessControlEntries, "accessControlEntries"); //$NON-NLS-1$

        name = profileName;
        this.accessControlEntries = accessControlEntries;
    }

    /**
     * @return the access control entries for this profile
     */
    public AccessControlEntry[] getAccessControlEntries() {
        return accessControlEntries;
    }

    /**
     * @return the name of this profile
     */
    public String getName() {
        return name;
    }

    /**
     * The index into the BuiltInProfiles array at which this
     * WorkspacePermissionProfile may be found. If this
     * WorkspacePermissionProfile is not a built-in profile, this value will be
     * -1.
     *
     * @return the build-in index of this profile
     */
    public int getBuiltinIndex() {
        return builtinIndex;
    }

    private void setBuiltinIndex(final int value) {
        builtinIndex = value;
    }

    /**
     * @return the built-in profiles that cover private, public limited, and
     *         public scenarios.
     */
    public static WorkspacePermissionProfile[] getBuiltInProfiles() {
        synchronized (BUILTIN_PROFILES_LOCK) {
            if (BUILTIN_PROFILES == null) {
                /*
                 * The everyone group, used in each of the profiles created
                 * here.
                 */
                final IdentityDescriptor everyoneGroup = new IdentityDescriptor(
                    IdentityConstants.TEAM_FOUNDATION_TYPE,
                    GroupWellKnownSIDConstants.EVERYONE_GROUP_SID);

                final List<WorkspacePermissionProfile> profiles = new ArrayList<WorkspacePermissionProfile>();

                /*
                 * Private has no access entries.
                 */
                profiles.add(new WorkspacePermissionProfile(BUILTIN_PROFILE_NAME_PRIVATE, new AccessControlEntry[0]));

                /*
                 * Public limited has one entry that grants read and use for
                 * everyone.
                 */
                final WorkspacePermissions publicLimitedPermissions =
                    WorkspacePermissions.USE.combine(WorkspacePermissions.READ);

                final AccessControlEntryDetails publicLimitedEntry = new AccessControlEntryDetails(
                    everyoneGroup,
                    publicLimitedPermissions.toIntFlags(),
                    WorkspacePermissions.NONE_OR_NOT_SUPPORTED.toIntFlags());

                profiles.add(
                    new WorkspacePermissionProfile(BUILTIN_PROFILE_NAME_PUBLIC_LIMITED, new AccessControlEntry[] {
                        publicLimitedEntry
                }));

                /*
                 * Public has one entry that grants administer, checkin, use,
                 * and read to everyone.
                 */

                final WorkspacePermissions publicPermissions = WorkspacePermissions.ADMINISTER.combine(
                    WorkspacePermissions.CHECK_IN.combine(WorkspacePermissions.USE.combine(WorkspacePermissions.READ)));

                final AccessControlEntryDetails publicEntry = new AccessControlEntryDetails(
                    everyoneGroup,
                    publicPermissions.toIntFlags(),
                    WorkspacePermissions.NONE_OR_NOT_SUPPORTED.toIntFlags());

                profiles.add(new WorkspacePermissionProfile(BUILTIN_PROFILE_NAME_PUBLIC, new AccessControlEntry[] {
                    publicEntry
                }));

                for (int i = 0; i < profiles.size(); i++) {
                    profiles.get(i).setBuiltinIndex(i);
                }

                BUILTIN_PROFILES = profiles.toArray(new WorkspacePermissionProfile[profiles.size()]);
            }

            return BUILTIN_PROFILES;
        }
    }

    public static WorkspacePermissionProfile getPrivateProfile() {
        return getBuiltInProfiles()[WorkspacePermissionProfile.BUILTIN_PROFILE_INDEX_PRIVATE];
    }

    public static WorkspacePermissionProfile getPublicLimitedProfile() {
        return getBuiltInProfiles()[WorkspacePermissionProfile.BUILTIN_PROFILE_INDEX_PUBLIC_LIMITED];
    }

    public static WorkspacePermissionProfile getPublicProfile() {
        return getBuiltInProfiles()[WorkspacePermissionProfile.BUILTIN_PROFILE_INDEX_PUBLIC];
    }
}
