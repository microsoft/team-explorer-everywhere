// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.credentials;

import java.net.URI;

import com.microsoft.tfs.core.httpclient.Cookie;
import com.microsoft.tfs.core.httpclient.CookieCredentials;
import com.microsoft.tfs.core.httpclient.Credentials;
import com.microsoft.tfs.core.httpclient.PreemptiveUsernamePasswordCredentials;
import com.microsoft.tfs.core.httpclient.UsernamePasswordCredentials;
import com.microsoft.tfs.core.httpclient.UsernamePasswordCredentials.PatCredentials;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.StringUtil;

/**
 * Represents credentials used to authenticate to TFS or other network resources
 * (HTTP proxies) that can be saved on the client.
 *
 * @threadsafety thread-safe
 */
public class CachedCredentials {
    private final URI uri;
    private final String username;
    private final String password;
    private final Cookie[] cookies;

    /**
     * Creates {@link CachedCredentials} from an existing {@link Credentials}
     * object.
     *
     * @param uri
     *        the {@link URI} the credentials are for (must not be
     *        <code>null</code>)
     * @param credentials
     *        the credentials (must not be <code>null</code>)
     */
    public CachedCredentials(final URI uri, final Credentials credentials) {
        Check.notNull(uri, "uri"); //$NON-NLS-1$
        Check.notNull(credentials, "credentials"); //$NON-NLS-1$
        this.uri = uri;

        if (credentials instanceof UsernamePasswordCredentials) {
            Check.notNull(
                ((UsernamePasswordCredentials) credentials).getUsername(),
                "((UsernamePasswordCredentials) credentials).getUsername()"); //$NON-NLS-1$

            this.username = ((UsernamePasswordCredentials) credentials).getUsername();
            this.password = ((UsernamePasswordCredentials) credentials).getPassword();
            this.cookies = null;
        } else if (credentials instanceof CookieCredentials) {
            this.username = null;
            this.password = null;
            this.cookies = ((CookieCredentials) credentials).getCookies();
        } else {
            this.username = null;
            this.password = null;
            this.cookies = null;
        }
    }

    /**
     * Creates username/password based {@link CachedCredentials} for the given
     * {@link URI}.
     *
     * @param uri
     *        the {@link URI} (must not be <code>null</code>)
     * @param username
     *        the username including domain (may be <code>null</code> or empty)
     * @param password
     *        the password (may be <code>null</code> or empty)
     */
    public CachedCredentials(final URI uri, final String username, final String password) {
        Check.notNull(uri, "uri"); //$NON-NLS-1$

        this.uri = uri;
        this.username = username;
        this.password = password;
        this.cookies = null;
    }

    /**
     * Creates PAT based {@link CachedCredentials} for the given {@link URI}.
     *
     * @param uri
     *        the {@link URI} (must not be <code>null</code>)
     * @param username
     *        the username including domain (may be <code>null</code> or empty)
     * @param password
     *        the password (may be <code>null</code> or empty)
     */
    public CachedCredentials(final URI uri, final String pat) {
        Check.notNull(uri, "uri"); //$NON-NLS-1$

        this.uri = uri;
        this.username = PatCredentials.USERNAME_FOR_CODE_ACCESS_PAT;
        this.password = pat == null ? StringUtil.EMPTY : pat;
        this.cookies = null;
    }

    /**
     * Creates cookie based {@link CachedCredentials} for the given {@link URI}.
     *
     * @param uri
     *        the {@link URI} (must not be <code>null</code>)
     * @param cookies
     *        the cookies for Federated Authentication)
     */
    public CachedCredentials(final URI uri, final Cookie[] cookies) {
        Check.notNull(uri, "uri"); //$NON-NLS-1$

        this.uri = uri;
        this.username = null;
        this.password = null;
        this.cookies = cookies;
    }

    /**
     * @return the URI these credentials are for (never <code>null</code>)
     */
    public URI getURI() {
        return uri;
    }

    /**
     * @return the username with domain (may be <code>null</code>)
     */
    public String getUsername() {
        return username;
    }

    /**
     * @return the password (may be <code>null</code>)
     */
    public String getPassword() {
        return password;
    }

    /**
     * @return the cookies (may be <code>null</code>)
     */
    public Cookie[] getCookies() {
        return cookies;
    }

    public boolean isNtlmCredentials() {
        return cookies == null && username == null && password == null;
    }

    public boolean isCookieCredentials() {
        return cookies != null;
    }

    public boolean isPatCredentials() {
        return PatCredentials.USERNAME_FOR_CODE_ACCESS_PAT.equals(username);
    }

    public boolean isUsernamePasswordCredentials() {
        return !isCookieCredentials() && !isPatCredentials() && username != null;
    }

    public Credentials toCredentials() {
        if (isCookieCredentials()) {
            return new CookieCredentials(cookies);
        } else if (isPatCredentials()) {
            return new PatCredentials(password);
        } else {
            final String u = (username != null) ? username : ""; //$NON-NLS-1$
            return new UsernamePasswordCredentials(u, password);
        }
    }

    public Credentials toPreemptiveCredentials() {
        if (isCookieCredentials()) {
            return new CookieCredentials(cookies);
        } else if (isPatCredentials()) {
            return PreemptiveUsernamePasswordCredentials.newFrom(new PatCredentials(password));
        } else {
            final String u = (username != null) ? username : ""; //$NON-NLS-1$
            return new PreemptiveUsernamePasswordCredentials(u, password);
        }
    }

    @Override
    public int hashCode() {
        int result = 17;

        result = result * 37 + uri.hashCode();
        result = result * 37 + (username != null ? username.hashCode() : 0);
        result = result * 37 + (password != null ? password.hashCode() : 0);
        result = result * 37 + (cookies != null ? cookies.hashCode() : 0);

        return result;
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj == this) {
            return true;
        }

        if (obj instanceof CachedCredentials == false) {
            return false;
        }

        final CachedCredentials other = (CachedCredentials) obj;

        if (cookies == null && other.cookies == null) {
            return uri.equals(other.uri)
                && (username == other.username || username != null && username.equals(other.username))
                && (password == other.password || password != null && password.equals(other.password));
        } else if (cookies != null && other.cookies != null) {
            if (cookies.length != other.cookies.length) {
                return false;
            }

            for (int k = 0; k < cookies.length; k++) {
                if (!cookies[k].equals(other.cookies[k])) {
                    return false;
                }
            }

            return true;
        } else {
            return false;
        }
    }
}
