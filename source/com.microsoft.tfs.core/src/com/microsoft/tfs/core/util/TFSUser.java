// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.util;

import com.microsoft.tfs.core.Messages;
import com.microsoft.tfs.util.Check;

/**
 * Identifies a user by a username and Windows domain name, and provides methods
 * for parsing these from and printing these to strings.
 *
 * @since TEE-SDK-10.1
 * @threadsafety immutable
 */
public final class TFSUser implements Comparable {
    private final String username;
    private final String domain;

    /**
     * <p>
     * Constructs a TFSUser from the given username (and optionally, domain)
     * string. These styles are supported:
     * </p>
     * <p>
     * <ul>
     * <li>username</li>
     * <li>username@domain</li>
     * <li>domain\\username</li>
     * </ul>
     * </p>
     * <p>
     * If a separator is found at the beginning or end of the input, the
     * username or domain will be considered empty, respectively. If the
     * username is empty, the construction throws
     * {@link TFSUsernameParseException}.
     * </p>
     *
     * @param usernameAndOptionalDomain
     *        the username and optional domain string (must not be
     *        <code>null</code>)
     * @throws TFSUsernameParseException
     *         when the string could not be parsed.
     */
    public TFSUser(final String usernameAndOptionalDomain) throws TFSUsernameParseException {
        if (usernameAndOptionalDomain == null) {
            throw new TFSUsernameParseException(Messages.getString("TFSUser.TheUsernameStringIsRequired")); //$NON-NLS-1$
        }

        final int indexOfBackslash = usernameAndOptionalDomain.indexOf('\\');
        final int indexOfAtSign = usernameAndOptionalDomain.indexOf('@');

        if (indexOfBackslash >= 0 && indexOfAtSign >= 0) {
            throw new TFSUsernameParseException(
                Messages.getString("TFSUser.UsernameAndDomainMustContainOnlyOneSeparator")); //$NON-NLS-1$
        }

        if (indexOfBackslash >= 0) {
            domain = usernameAndOptionalDomain.substring(0, indexOfBackslash);
            username = usernameAndOptionalDomain.substring(indexOfBackslash + 1);
        } else if (indexOfAtSign >= 0) {
            username = usernameAndOptionalDomain.substring(0, indexOfAtSign);
            domain = usernameAndOptionalDomain.substring(indexOfAtSign + 1);
        } else {
            // No separator found, so the whole string must be the username.
            username = usernameAndOptionalDomain;
            domain = ""; //$NON-NLS-1$
        }
    }

    /**
     * Constructs a TFSUser from the given username (and optionally, domain)
     * string.
     *
     * @param username
     *        the username string to assign (must not be <code>null</code>)
     * @param domain
     *        the domain string to assign (may be null).
     * @throws TFSUsernameParseException
     *         when the inputs were not invalid
     */
    public TFSUser(final String username, final String domain) throws TFSUsernameParseException {
        Check.notNull(username, "username"); //$NON-NLS-1$

        this.username = username;
        this.domain = domain;
    }

    /**
     * Constructs a TFSUser from the given TFSUser class.
     *
     * @param user
     *        the instance to initialize the new instance's values from.
     */
    public TFSUser(final TFSUser user) {
        username = user.username;
        domain = user.domain;
    }

    /**
     * @return the username (never null).
     */
    public String getUsername() {
        return username;
    }

    /**
     * @return the domain (null if null during construction).
     */
    public String getDomain() {
        return domain;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(final Object obj) {
        if (obj == null) {
            return false;
        }
        if (obj instanceof TFSUser == false) {
            return false;
        }
        if (obj == this) {
            return true;
        }

        final TFSUser other = (TFSUser) obj;

        if (domain != null && other.domain != null) {
            if (domain.equalsIgnoreCase(other.domain) == false) {
                return false;
            }
        } else if (domain != null ^ other.domain != null) {
            /*
             * XOR of the domains is true when one, but not both, of the domains
             * is null.
             */
            return false;
        }

        /*
         * Domains are both null, so just test username.
         */

        return username.equalsIgnoreCase(other.username);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        int result = 17;

        result = result * 37 + ((domain == null) ? 0 : domain.toLowerCase().hashCode());
        result = result * 37 + ((username == null) ? 0 : username.toLowerCase().hashCode());

        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        if (domain != null && domain.length() > 0) {
            return domain + "\\" + username; //$NON-NLS-1$
        } else {
            return username;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int compareTo(final Object o) {
        final TFSUser other = (TFSUser) o;

        int res = 0;

        if (domain != null && other.domain != null) {
            res = domain.compareToIgnoreCase(other.domain);
            if (res != 0) {
                return res;
            }
        } else if (domain != null ^ other.domain != null) {
            /*
             * XOR of the domains is true when one, but not both, of the domains
             * is null.
             */
            return -1;
        }

        /*
         * Domains are both null, so just compare username.
         */

        return username.compareToIgnoreCase(other.username);
    }
}
