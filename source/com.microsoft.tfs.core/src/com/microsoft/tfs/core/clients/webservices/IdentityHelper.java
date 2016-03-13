// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.webservices;

import java.util.concurrent.atomic.AtomicReference;

import com.microsoft.tfs.core.clients.security.FrameworkSecurity;
import com.microsoft.tfs.core.clients.versioncontrol.VersionControlConstants;
import com.microsoft.tfs.core.util.TFSUser;
import com.microsoft.tfs.core.util.UserNameUtil;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.StringUtil;

/**
 * Helper class to manage Team Foundation identity descriptors.
 *
 * @since TEE-SDK-10.1
 * @threadsafety thread-compatible
 */
public class IdentityHelper {
    /**
     * Create TFS or Windows descriptor for SID based identities. If the type is
     * already known, using the type specific Create method will be faster. TFS
     * sids start with S-1-9-1551374245. Anything else is treated as Windows.
     * Known forms of Windows SIDs are S-1-5-xxxx, well known sids of the form
     * S-1-0, S-1-0-xx, S-1-2, S-1-3, S-1-3-xx, S-1-4, S-1-5, S-1-5-xxx
     */
    public static IdentityDescriptor createDescriptorFromSID(final String sid) {
        Check.notNullOrEmpty(sid, "sid"); //$NON-NLS-1$

        if (sid.toLowerCase().startsWith(SIDIdentityHelper.TEAM_FOUNDATION_SID_PREFIX.toLowerCase())) {
            return createTeamFoundationDescriptor(sid);
        } else {
            return createWindowsDescriptor(sid);
        }
    }

    /**
     * Create descriptor with identity type of Windows. Note - this does not
     * validate that the SID really is a Windows SID. But if that is not the
     * case, failure will occur later when this descriptor is used in IMS APIs.
     */
    public static IdentityDescriptor createWindowsDescriptor(final String sid) {
        return new IdentityDescriptor(IdentityConstants.WINDOWS_TYPE, sid);
    }

    /**
     * Create descriptor with identity type of TeamFoundation. Note - this does
     * not validate that the SID really is a TeamFoundation SID. But if that is
     * not the case, failure will occur later when this descriptor is used in
     * IMS APIs.
     */
    public static IdentityDescriptor createTeamFoundationDescriptor(final String sid) {
        return new IdentityDescriptor(IdentityConstants.TEAM_FOUNDATION_TYPE, sid);
    }

    /**
     * Returns true if the specified name matches any format of the specified
     * identity.
     */
    public static boolean identityHasName(final TeamFoundationIdentity identity, final String name) {
        Check.notNull(identity, "identity"); //$NON-NLS-1$
        Check.notNull(name, "name"); //$NON-NLS-1$

        if (identity.getUniqueName().equalsIgnoreCase(name)) {
            return true;
        }

        if (identity.getDisplayName().equalsIgnoreCase(name)) {
            return true;
        }

        final String accountName = identity.getAttribute(IdentityAttributeTags.ACCOUNT_NAME, null);
        if (accountName != null && accountName.equalsIgnoreCase(name)) {
            return true;
        }

        final String domainName = identity.getAttribute(IdentityAttributeTags.DOMAIN, null);
        final TFSUser user = new TFSUser(accountName, domainName);
        if (user.toString().equalsIgnoreCase(name)) {
            return true;
        }

        return false;
    }

    /**
     * Returns a unique user name if the specified user name matches any of the
     * various formats of the current authorized user.
     *
     *
     * @param identity
     *        The current authorized user identity from the TfsServer.
     * @param userName
     *        The user name to match.
     * @return A unique user name if the user name matches the current
     *         authorized identity. Otherwise, the input user name is returned.
     */
    public static String getUniqueNameIfCurrentUser(final TeamFoundationIdentity identity, final String userName) {
        Check.notNull(identity, "identity"); //$NON-NLS-1$

        if (StringUtil.isNullOrEmpty(userName)) {
            return userName;
        }

        if (userName.equals(VersionControlConstants.AUTHENTICATED_USER)
            || IdentityHelper.identityHasName(identity, userName)) {
            return identity.getUniqueName();
        }

        return userName;
    }

    /**
     * Get identity name for display.
     **/
    public static String getDomainUserName(final TeamFoundationIdentity identity) {
        Check.notNull(identity, "identity"); //$NON-NLS-1$

        final AtomicReference<String> outResolvableName = new AtomicReference<String>();
        final AtomicReference<String> outDisplayableName = new AtomicReference<String>();

        UserNameUtil.getIdentityName(
            identity.getDescriptor().getIdentityType(),
            identity.getDisplayName(),
            identity.getAttribute(IdentityAttributeTags.DOMAIN, StringUtil.EMPTY),
            identity.getAttribute(IdentityAttributeTags.ACCOUNT_NAME, StringUtil.EMPTY),
            identity.getUniqueUserID(),
            outResolvableName,
            outDisplayableName);

        return outDisplayableName.get();
    }

    /**
     * Security token for an identity that we own (TFS group).
     */
    public static String createSecurityToken(final TeamFoundationIdentity group) {
        // For a TFS group, the project or host URI is stored as the domain of
        // the identity. token is scopeUri\groupId
        return group.getAttribute(IdentityAttributeTags.DOMAIN, "") //$NON-NLS-1$
            + FrameworkSecurity.IDENTITY_SECURITY_PATH_SEPARATOR
            + group.getTeamFoundationID();
    }

    public static void checkDescriptor(final IdentityDescriptor descriptor, final String parameterName) {
        Check.notNull(descriptor, parameterName);
        Check.notNullOrEmpty(descriptor.getIdentityType(), "descriptor.getIdentityType()"); //$NON-NLS-1$
        Check.notNullOrEmpty(descriptor.getIdentifier(), "descriptor.getIdentifier()"); //$NON-NLS-1$
    }
}
