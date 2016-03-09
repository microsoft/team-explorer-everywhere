/*
 * $Header:
 * /home/jerenkrantz/tmp/commons/commons-convert/cvs/home/cvs/jakarta-commons
 * //httpclient/src/java/org/apache/commons/httpclient/HttpState.java,v 1.38
 * 2004/12/20 11:50:54 olegk Exp $ $Revision: 561099 $ $Date: 2007-07-30
 * 21:41:17 +0200 (Mon, 30 Jul 2007) $
 *
 * ====================================================================
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many individuals on
 * behalf of the Apache Software Foundation. For more information on the Apache
 * Software Foundation, please see <http://www.apache.org/>.
 */

package com.microsoft.tfs.core.httpclient;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.microsoft.tfs.core.httpclient.auth.AuthScope;

/**
 * <p>
 * A container for HTTP attributes that may persist from request to request,
 * such as {@link Cookie cookies} and authentication {@link Credentials
 * credentials}.
 * </p>
 *
 * @author <a href="mailto:remm@apache.org">Remy Maucherat</a>
 * @author Rodney Waldhoff
 * @author <a href="mailto:jsdever@apache.org">Jeff Dever</a>
 * @author Sean C. Sullivan
 * @author <a href="mailto:becke@u.washington.edu">Michael Becke</a>
 * @author <a href="mailto:oleg@ural.ru">Oleg Kalnichevski</a>
 * @author <a href="mailto:mbowler@GargoyleSoftware.com">Mike Bowler</a>
 * @author <a href="mailto:adrian@intencha.com">Adrian Sutton</a>
 *
 * @version $Revision: 561099 $ $Date: 2007-07-30 21:41:17 +0200 (Mon, 30 Jul
 *          2007) $
 *
 */
public class HttpState {

    // ----------------------------------------------------- Instance Variables

    /**
     * Map of {@link Credentials credentials} by realm that this HTTP state
     * contains.
     */
    protected HashMap<AuthScope, Credentials> credMap = new HashMap<AuthScope, Credentials>();

    /**
     * Map of {@link Credentials proxy credentials} by realm that this HTTP
     * state contains
     */
    protected HashMap<AuthScope, Credentials> proxyCred = new HashMap<AuthScope, Credentials>();

    /**
     * Array of {@link Cookie cookies} that this HTTP state contains.
     */
    protected ArrayList<Cookie> cookies = new ArrayList<Cookie>();

    // -------------------------------------------------------- Class Variables

    /** Log object for this class. */
    private static final Log LOG = LogFactory.getLog(HttpState.class);

    /**
     * Default constructor.
     */
    public HttpState() {
        super();
    }

    // ------------------------------------------------------------- Properties

    /**
     * Adds an {@link Cookie HTTP cookie}, replacing any existing equivalent
     * cookies. If the given cookie has already expired it will not be added,
     * but existing values will still be removed.
     *
     * @param cookie
     *        the {@link Cookie cookie} to be added
     *
     * @see #addCookies(Cookie[])
     *
     */
    public synchronized void addCookie(final Cookie cookie) {
        LOG.trace("enter HttpState.addCookie(Cookie)");

        if (cookie != null) {
            // first remove any old cookie that is equivalent
            for (final Iterator<Cookie> it = cookies.iterator(); it.hasNext();) {
                final Cookie tmp = it.next();
                if (cookie.equals(tmp)) {
                    it.remove();
                    break;
                }
            }
            if (!cookie.isExpired()) {
                cookies.add(cookie);
            }
        }
    }

    /**
     * Adds an array of {@link Cookie HTTP cookies}. Cookies are added
     * individually and in the given array order. If any of the given cookies
     * has already expired it will not be added, but existing values will still
     * be removed.
     *
     * @param cookies
     *        the {@link Cookie cookies} to be added
     *
     * @see #addCookie(Cookie)
     *
     *
     */
    public synchronized void addCookies(final Cookie[] cookies) {
        LOG.trace("enter HttpState.addCookies(Cookie[])");

        if (cookies != null) {
            for (int i = 0; i < cookies.length; i++) {
                LOG.trace(cookies[i]);
                addCookie(cookies[i]);
            }
        }
        LOG.trace("leave HttpState.addCookies(Cookie[])");
    }

    /**
     * Returns an array of {@link Cookie cookies} that this HTTP state currently
     * contains.
     *
     * @return an array of {@link Cookie cookies}.
     *
     * @see #getCookies(String, int, String, boolean)
     *
     */
    public synchronized Cookie[] getCookies() {
        LOG.trace("enter HttpState.getCookies()");
        return cookies.toArray(new Cookie[cookies.size()]);
    }

    /**
     * Removes all of {@link Cookie cookies} in this HTTP state that have
     * expired according to the current system time.
     *
     * @see #purgeExpiredCookies(java.util.Date)
     *
     */
    public synchronized boolean purgeExpiredCookies() {
        LOG.trace("enter HttpState.purgeExpiredCookies()");
        return purgeExpiredCookies(new Date());
    }

    /**
     * Removes all of {@link Cookie cookies} in this HTTP state that have
     * expired by the specified {@link java.util.Date date}.
     *
     * @param date
     *        The {@link java.util.Date date} to compare against.
     *
     * @return true if any cookies were purged.
     *
     * @see Cookie#isExpired(java.util.Date)
     *
     * @see #purgeExpiredCookies()
     */
    public synchronized boolean purgeExpiredCookies(final Date date) {
        LOG.trace("enter HttpState.purgeExpiredCookies(Date)");
        boolean removed = false;
        final Iterator<Cookie> it = cookies.iterator();
        while (it.hasNext()) {
            if (it.next().isExpired(date)) {
                it.remove();
                removed = true;
            }
        }
        return removed;
    }

    /**
     * Sets the {@link Credentials credentials} for the given authentication
     * scope. Any previous credentials for the given scope will be overwritten.
     *
     * @param authscope
     *        the {@link AuthScope authentication scope}
     * @param credentials
     *        the authentication {@link Credentials credentials} for the given
     *        scope.
     *
     * @see #getCredentials(AuthScope)
     * @see #setProxyCredentials(AuthScope, Credentials)
     *
     * @since 3.0
     */
    public synchronized void setCredentials(final AuthScope authscope, final Credentials credentials) {
        if (authscope == null) {
            throw new IllegalArgumentException("Authentication scope may not be null");
        }
        LOG.trace("enter HttpState.setCredentials(AuthScope, Credentials)");
        credMap.put(authscope, credentials);
    }

    /**
     * Find matching {@link Credentials credentials} for the given
     * authentication scope.
     *
     * @param map
     *        the credentials hash map
     * @param token
     *        the {@link AuthScope authentication scope}
     * @return the credentials
     *
     */
    private static Credentials matchCredentials(final HashMap<AuthScope, Credentials> map, final AuthScope authscope) {
        // see if we get a direct hit
        Credentials creds = map.get(authscope);

        if (creds == null) {
            // Nope.
            // Do a full scan
            int bestMatchFactor = -1;
            AuthScope bestMatch = null;
            final Iterator<AuthScope> items = map.keySet().iterator();
            while (items.hasNext()) {
                final AuthScope current = items.next();
                final int factor = authscope.match(current);
                if (factor > bestMatchFactor) {
                    bestMatchFactor = factor;
                    bestMatch = current;
                }
            }
            if (bestMatch != null) {
                creds = map.get(bestMatch);
            }
        }
        return creds;
    }

    /**
     * Get the {@link Credentials credentials} for the given authentication
     * scope.
     *
     * @param authscope
     *        the {@link AuthScope authentication scope}
     * @return the credentials
     *
     * @see #setCredentials(AuthScope, Credentials)
     *
     * @since 3.0
     */
    public synchronized Credentials getCredentials(final AuthScope authscope) {
        if (authscope == null) {
            throw new IllegalArgumentException("Authentication scope may not be null");
        }
        LOG.trace("enter HttpState.getCredentials(AuthScope)");
        return matchCredentials(credMap, authscope);
    }

    /**
     * Sets the {@link Credentials proxy credentials} for the given
     * authentication realm. Any previous credentials for the given realm will
     * be overwritten.
     *
     * @param authscope
     *        the {@link AuthScope authentication scope}
     * @param credentials
     *        the authentication {@link Credentials credentials} for the given
     *        realm.
     *
     * @see #getProxyCredentials(AuthScope)
     * @see #setCredentials(AuthScope, Credentials)
     *
     * @since 3.0
     */
    public synchronized void setProxyCredentials(final AuthScope authscope, final Credentials credentials) {
        if (authscope == null) {
            throw new IllegalArgumentException("Authentication scope may not be null");
        }
        LOG.trace("enter HttpState.setProxyCredentials(AuthScope, Credentials)");
        proxyCred.put(authscope, credentials);
    }

    /**
     * Get the {@link Credentials proxy credentials} for the given
     * authentication scope.
     *
     * @param authscope
     *        the {@link AuthScope authentication scope}
     * @return the credentials
     *
     * @see #setProxyCredentials(AuthScope, Credentials)
     *
     * @since 3.0
     */
    public synchronized Credentials getProxyCredentials(final AuthScope authscope) {
        if (authscope == null) {
            throw new IllegalArgumentException("Authentication scope may not be null");
        }
        LOG.trace("enter HttpState.getProxyCredentials(AuthScope)");
        return matchCredentials(proxyCred, authscope);
    }

    /**
     * Returns a string representation of this HTTP state.
     *
     * @return The string representation of the HTTP state.
     *
     * @see java.lang.Object#toString()
     */
    @Override
    public synchronized String toString() {
        final StringBuffer sbResult = new StringBuffer();

        sbResult.append("[");
        sbResult.append(getCredentialsStringRepresentation(proxyCred));
        sbResult.append(" | ");
        sbResult.append(getCredentialsStringRepresentation(credMap));
        sbResult.append(" | ");
        sbResult.append(getCookiesStringRepresentation(cookies));
        sbResult.append("]");

        final String strResult = sbResult.toString();

        return strResult;
    }

    /**
     * Returns a string representation of the credentials.
     *
     * @param credMap
     *        The credentials.
     * @return The string representation.
     */
    private static String getCredentialsStringRepresentation(final Map<AuthScope, Credentials> credMap) {
        final StringBuffer sbResult = new StringBuffer();
        final Iterator<AuthScope> iter = credMap.keySet().iterator();
        while (iter.hasNext()) {
            final Object key = iter.next();
            final Credentials cred = credMap.get(key);
            if (sbResult.length() > 0) {
                sbResult.append(", ");
            }
            sbResult.append(key);
            sbResult.append("#");
            sbResult.append(cred.toString());
        }
        return sbResult.toString();
    }

    /**
     * Returns a string representation of the cookies.
     *
     * @param cookies
     *        The cookies
     * @return The string representation.
     */
    private static String getCookiesStringRepresentation(final List<Cookie> cookies) {
        final StringBuffer sbResult = new StringBuffer();
        final Iterator<Cookie> iter = cookies.iterator();
        while (iter.hasNext()) {
            final Cookie ck = iter.next();
            if (sbResult.length() > 0) {
                sbResult.append("#");
            }
            sbResult.append(ck.toExternalForm());
        }
        return sbResult.toString();
    }

    /**
     * Clears all credentials.
     */
    public void clearCredentials() {
        credMap.clear();
    }

    /**
     * Clears all proxy credentials.
     */
    public void clearProxyCredentials() {
        proxyCred.clear();
    }

    /**
     * Clears all cookies.
     */
    public synchronized void clearCookies() {
        cookies.clear();
    }

    /**
     * Clears the state information (all cookies, credentials and proxy
     * credentials).
     */
    public void clear() {
        clearCookies();
        clearCredentials();
        clearProxyCredentials();
    }
}
