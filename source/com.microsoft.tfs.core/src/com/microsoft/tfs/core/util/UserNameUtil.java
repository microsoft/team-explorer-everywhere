// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.util;

import java.text.MessageFormat;
import java.util.Comparator;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.microsoft.tfs.core.clients.versioncontrol.VersionControlConstants;
import com.microsoft.tfs.core.httpclient.DefaultNTCredentials;
import com.microsoft.tfs.core.httpclient.auth.NTLMScheme;
import com.microsoft.tfs.core.httpclient.auth.NegotiateScheme;
import com.microsoft.tfs.jni.PlatformMiscUtils;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.Platform;

/**
 * <p>
 * {@link UserNameUtil} contains static utility methods for working with TFS /
 * Windows user names.
 * </p>
 * <p>
 * Equivalent to: Microsoft.TeamFoundation.Common.UserNameUtil
 * </p>
 *
 * @since TEE-SDK-10.1
 * @threadsafety thread-safe
 */
public class UserNameUtil {
    private static final Log log = LogFactory.getLog(UserNameUtil.class);

    public static final char DOMAIN_SEPARATOR = '\\';

    private static Object currentUserLock = new Object();
    private static boolean currentUserInitialized = false;
    private static String currentUserName;
    private static String currentUserDomain;

    /**
     * Obtains the current user name for this process, if available.
     *
     * @return the current user name, or <code>null</code> if not available
     */
    public static String getCurrentUserName() {
        synchronized (currentUserLock) {
            ensureCurrentUserInitialized();
            return currentUserName;
        }
    }

    /**
     * Obtains the current user domain for this process, if available.
     *
     * @return the current user domain, or <code>null</code> if not available
     */
    public static String getCurrentUserDomain() {
        synchronized (currentUserLock) {
            ensureCurrentUserInitialized();
            return currentUserDomain;
        }
    }

    private static void ensureCurrentUserInitialized() {
        synchronized (currentUserLock) {
            if (currentUserInitialized) {
                return;
            }

            String defaultCredentials = null;

            /*
             * Try to look up our current credentials. (Is only likely to
             * succeed against Unix kerberos implementations.
             */
            if (NegotiateScheme.supportsCredentials(DefaultNTCredentials.class)) {
                defaultCredentials = NegotiateScheme.getDefaultCredentials();
            }

            /* Try again with default credentials. (Is unlikely to succeed.) */
            if (defaultCredentials == null && NTLMScheme.supportsCredentials(DefaultNTCredentials.class)) {
                defaultCredentials = NTLMScheme.getDefaultCredentials();
            }

            if (defaultCredentials != null) {
                /* Native credentials are in the form user@DOMAIN. */
                final int separatorIdx = defaultCredentials.indexOf("@"); //$NON-NLS-1$

                if (separatorIdx >= 0) {
                    currentUserName = defaultCredentials.substring(0, separatorIdx);
                    currentUserDomain = defaultCredentials.substring(separatorIdx + 1);
                } else {
                    currentUserName = defaultCredentials;
                }
            } else if (Platform.isCurrentPlatform(Platform.WINDOWS)) {
                final NTUserInfo info = lookUpNTUserInfo();

                if (info != null) {
                    currentUserName = info.getUserName();
                    currentUserDomain = info.getDomain();
                } else {
                    currentUserDomain = PlatformMiscUtils.getInstance().getEnvironmentVariable("USERDOMAIN"); //$NON-NLS-1$
                    currentUserName = PlatformMiscUtils.getInstance().getEnvironmentVariable("USERNAME"); //$NON-NLS-1$
                }
            }

            if (currentUserName == null) {
                currentUserName = System.getProperty("user.name"); //$NON-NLS-1$
            }

            currentUserInitialized = true;
        }
    }

    /**
     * Compares two usernames / domains for equality. The strings are simply
     * compared case-insensitively.
     *
     * @param username1
     *        the first username to compare (must not be <code>null</code>)
     * @param username2
     *        the second username to compare (must not be <code>null</code>)
     * @return <code>true</code> if the two usernames are equal
     */
    public static boolean equals(final String username1, final String username2) {
        Check.notNull(username1, "username1"); //$NON-NLS-1$
        Check.notNull(username2, "username2"); //$NON-NLS-1$

        return username1.equalsIgnoreCase(username2);
    }

    /**
     * Does a compare of two usernames / domains.
     *
     * @param username1
     *        the first username (must not be <code>null</code>)
     * @param username2
     *        the second username (must not be <code>null</code>)
     * @return the compare value
     */
    public static int compare(final String username1, final String username2) {
        final ParsedUserName p1 = parse(username1);
        final ParsedUserName p2 = parse(username2);

        final int val = p1.getName().compareToIgnoreCase(p2.getName());

        if (val != 0) {
            return val;
        }

        if (p1.getDomain() == null && p2.getDomain() == null) {
            return 0;
        }

        if (p1.getDomain() == null) {
            return -1;
        }

        if (p2.getDomain() == null) {
            return 1;
        }

        return p1.getDomain().compareToIgnoreCase(p2.getDomain());
    }

    /**
     * @return a new {@link Comparator} that uses the
     *         {@link #compare(String, String)} method to sort usernames /
     *         domains (never <code>null</code>)
     */
    public static Comparator<String> newUsernameComparator() {
        return new Comparator<String>() {
            @Override
            public int compare(final String username1, final String username2) {
                return UserNameUtil.compare(username1, username2);
            }
        };
    }

    /**
     * Tests whether the given user name contains a domain.
     *
     * @param userName
     *        the user name to test (must not be <code>null</code>)
     * @return <code>true</code> if the specified user name contains a domain
     */
    public static boolean hasDomain(final String userName) {
        final String domain = getDomain(userName);
        return domain != null && domain.length() > 0;
    }

    /**
     * Gets the domain portion of the specified user name.
     *
     * @param userName
     *        the user name to get the domain for (must not be <code>null</code>
     *        )
     * @return the domain portion or <code>null</code> if there is no domain
     *         portion
     */
    public static String getDomain(final String userName) {
        return parse(userName).getDomain();
    }

    /**
     * Gets the name-only (no domain) portion of the specified user name.
     *
     * @param userName
     *        the user name to get the name for (must not be <code>null</code>)
     * @return the name portion (never <code>null</code>)
     */
    public static String getName(final String userName) {
        return parse(userName).getName();
    }

    public static void getIdentityName(
        final String identityType,
        final String displayName,
        final String attribute,
        final String attribute2,
        final int uniqueUserID,
        final AtomicReference<String> outResolvableName,
        final AtomicReference<String> outDisplayableName) {
        // TODO: NYI
    }

    /**
     * Tests whether the given user name is complete (contains a domain
     * separator or equals the authenticated user constant).
     *
     * @param userName
     *        the user name to test (must not be <code>null</code> or empty)
     * @return true if the user name is complete, false if it is not.
     */
    public static boolean isComplete(final String userName) {
        Check.notNullOrEmpty(userName, "userName"); //$NON-NLS-1$

        if (userName.indexOf(DOMAIN_SEPARATOR) < 0) {
            return userName.equals(VersionControlConstants.AUTHENTICATED_USER);
        }

        return true;
    }

    /**
     * Completes the specified user name. Completing a user name works the
     * following way:
     * <ol>
     * <li>If the specified user name contains a domain, the specified user name
     * is returned</li>
     * <li>If the specified user name is equal to the "authenticated user"
     * constant (<code>.</code>) and <code>allowAuthenticatedUserConstant</code>
     * is <code>true</code>, the specified user name is returned</li>
     * <li>If a domain can be determined, the specified username is combined
     * with that domain and the result is returned</li>
     * <li>Otherwise, the specified user name is returned</li>
     * </ol>
     * A domain is determined in the following way:
     * <ol>
     * <li>If the specified <code>relative</code> argument is non-
     * <code>null</code>, the domain is either the <code>relative</code>
     * argument or the domain portion of the <code>relative</code> argument</li>
     * <li>If the current user domain can be determined (
     * {@link #getCurrentUserDomain()}), that is used as the domain</li>
     * <li>Otherwise, no domain can be determined</li>
     * </ol>
     *
     * @param userName
     *        the user name to complete (must not be <code>null</code> or empty)
     * @param relative
     *        the relative argument (can be <code>null</code>)
     * @param allowAuthenticatedUserConstant
     *        <code>true</code> to allow usernames that are equal to the
     *        authenticated user constant
     * @return the completed user name (never <code>null</code>)
     */
    public static String makeComplete(
        final String userName,
        final String relative,
        final boolean allowAuthenticatedUserConstant) {
        Check.notNullOrEmpty(userName, "userName"); //$NON-NLS-1$

        if (hasDomain(userName)) {
            return userName;
        }

        if (allowAuthenticatedUserConstant && VersionControlConstants.AUTHENTICATED_USER.equals(userName)) {
            return userName;
        }

        String defaultDomain;

        if (relative == null) {
            defaultDomain = getCurrentUserDomain();
        } else if (relative.indexOf(DOMAIN_SEPARATOR) != -1) {
            defaultDomain = getDomain(relative);
        } else {
            defaultDomain = relative;
        }

        if (defaultDomain == null) {
            return userName;
        }

        return defaultDomain + DOMAIN_SEPARATOR + userName;
    }

    /**
     * Formats the specified user account name and domain name. The result is a
     * full NT4 style user spec (<code>DOMAIN\\user</code>).
     *
     * @param username
     *        the user account portion (must not be <code>null</code>)
     * @param domain
     *        the domain portion (may be <code>null</code>)
     * @return the full NT4 style username
     */
    public static String format(final String username, final String domain) {
        Check.notNull(username, "username"); //$NON-NLS-1$

        final StringBuffer buffer = new StringBuffer();

        if (domain != null && domain.length() > 0) {
            buffer.append(domain);
            buffer.append(DOMAIN_SEPARATOR);
        }

        buffer.append(username);

        return buffer.toString();
    }

    /**
     * Removes the domain portion from the specified user name, if present. If
     * no domain portion is present, the specified user name is returned.
     *
     * @param userName
     *        user name to parse (must not be <code>null</code>)
     * @return the specified user name with any domain portion removed
     */
    public static String removeDomain(final String userName) {
        final ParsedUserName parsedUserName = parse(userName);

        return parsedUserName.getName();
    }

    /**
     * Parses the specified user name, returning a {@link ParsedUserName} object
     * that contains the user and domain portions.
     *
     * @param userName
     *        the user name to parse (must not be <code>null</code>)
     * @return the parsed user name (never <code>null</code>)
     */
    public static ParsedUserName parse(final String userName) {
        Check.notNull(userName, "userName"); //$NON-NLS-1$

        final int ix = userName.indexOf(DOMAIN_SEPARATOR);

        if (ix == -1) {
            return new ParsedUserName(null, userName);
        }

        if (ix == userName.length() - 1) {
            throw new IllegalArgumentException(MessageFormat.format("Invalid user name: [{0}]", userName)); //$NON-NLS-1$
        }

        final String domain = userName.substring(0, ix);
        final String name = userName.substring(ix + 1);

        return new ParsedUserName(domain, name);
    }

    /**
     * Represents a parsed user name's two components: domain and account name.
     */
    public static class ParsedUserName {
        private final String domain;
        private final String name;

        /**
         * Creates a new {@link ParsedUserName}.
         *
         * @param domain
         *        the domain (may be <code>null</code>)
         * @param name
         *        the account name (must not be <code>null</code>)
         */
        public ParsedUserName(final String domain, final String name) {
            Check.notNull(name, "name"); //$NON-NLS-1$

            this.domain = domain;
            this.name = name;
        }

        /**
         * @return the domain portion of the parsed user name, or
         *         <code>null</code> if there is no domain portion
         */
        public String getDomain() {
            return domain;
        }

        /**
         * @return the name portion of the parsed user name, or
         *         <code>null</code> if there is no name portion
         */
        public String getName() {
            return name;
        }
    }

    private static NTUserInfo lookUpNTUserInfo() {
        try {
            final Class c = Class.forName("com.sun.security.auth.module.NTSystem"); //$NON-NLS-1$
            final Object instance = c.newInstance();

            final String userName = (String) c.getMethod("getName", (Class[]) null).invoke(instance, (Object[]) null); //$NON-NLS-1$
            final String domain = (String) c.getMethod("getDomain", (Class[]) null).invoke(instance, (Object[]) null); //$NON-NLS-1$
            final String domainSID =
                (String) c.getMethod("getDomainSID", (Class[]) null).invoke(instance, (Object[]) null); //$NON-NLS-1$
            final String userSID = (String) c.getMethod("getUserSID", (Class[]) null).invoke(instance, (Object[]) null); //$NON-NLS-1$
            final String primaryGroupID = (String) c.getMethod("getPrimaryGroupID", (Class[]) null).invoke( //$NON-NLS-1$
                instance,
                (Object[]) null);
            final String[] groupIDs =
                (String[]) c.getMethod("getGroupIDs", (Class[]) null).invoke(instance, (Object[]) null); //$NON-NLS-1$
            final long impersonationToken = ((Long) c.getMethod("getImpersonationToken", (Class[]) null).invoke( //$NON-NLS-1$
                instance,
                (Object[]) null)).longValue();

            return new NTUserInfo(userName, domain, domainSID, userSID, primaryGroupID, groupIDs, impersonationToken);
        } catch (final Throwable t) {
            if (log.isDebugEnabled()) {
                log.debug("Unable to get NT user info", t); //$NON-NLS-1$
            }

            return null;
        }
    }

    private static class NTUserInfo {
        private final String userName;
        private final String domain;

        public NTUserInfo(
            final String userName,
            final String domain,
            final String domainSID,
            final String userSID,
            final String primaryGroupID,
            final String[] groupIDs,
            final long impersonationToken) {
            this.userName = userName;
            this.domain = domain;
        }

        public String getUserName() {
            return userName;
        }

        public String getDomain() {
            return domain;
        }
    }
}
