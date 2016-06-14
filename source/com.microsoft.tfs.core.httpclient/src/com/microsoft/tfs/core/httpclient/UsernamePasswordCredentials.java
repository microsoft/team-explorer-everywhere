/*
 * $Header:
 * /home/jerenkrantz/tmp/commons/commons-convert/cvs/home/cvs/jakarta-commons
 * //httpclient
 * /src/java/org/apache/commons/httpclient/UsernamePasswordCredentials.java,v
 * 1.14 2004/04/18 23:51:35 jsdever Exp $ $Revision: 480424 $ $Date: 2006-11-29
 * 06:56:49 +0100 (Wed, 29 Nov 2006) $
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

import com.microsoft.tfs.core.httpclient.util.LangUtils;
import com.microsoft.tfs.util.StringUtil;

/**
 * <p>
 * Username and password {@link Credentials}.
 * </p>
 *
 * @author <a href="mailto:remm@apache.org">Remy Maucherat</a>
 * @author Sean C. Sullivan
 * @author <a href="mailto:mbowler@GargoyleSoftware.com">Mike Bowler</a>
 * @author <a href="mailto:oleg@ural.ru">Oleg Kalnichevski</a>
 *
 * @version $Revision: 480424 $ $Date: 2006-11-29 06:56:49 +0100 (Wed, 29 Nov
 *          2006) $
 *
 */
public class UsernamePasswordCredentials extends Credentials {
    /**
     * User name.
     */
    private final String username;

    /**
     * Password, plus synchronization object.
     */
    private String password;
    private final Object passwordLock = new Object();

    /**
     * The constructor with the username and password arguments.
     *
     * @param username
     *        the user name
     * @param password
     *        the password
     */
    public UsernamePasswordCredentials(final String username, final String password) {
        super();

        if (username == null) {
            throw new IllegalArgumentException("Username may not be null");
        }

        this.username = username;
        this.password = password;
    }

    /**
     * User name property getter.
     *
     * @return the userName
     * @see #setUserName(String)
     */
    public String getUsername() {
        return username;
    }

    public void setPassword(final String password) {
        synchronized (passwordLock) {
            this.password = password;
        }
    }

    /**
     * Password property getter.
     *
     * @return the password
     * @see #setPassword(String)
     */
    public String getPassword() {
        synchronized (passwordLock) {
            return password;
        }
    }

    /**
     * Get this object string.
     *
     * @return the username:password formed string
     */
    @Override
    public String toString() {
        final StringBuffer result = new StringBuffer();
        result.append(username);
        result.append(":");
        result.append((password == null) ? "null" : password);
        return result.toString();
    }

    /**
     * Does a hash of both user name and password.
     *
     * @return The hash code including user name and password.
     */
    @Override
    public int hashCode() {
        int hash = LangUtils.HASH_SEED;
        hash = LangUtils.hashCode(hash, username);
        hash = LangUtils.hashCode(hash, password);
        return hash;
    }

    /**
     * These credentials are assumed equal if the username and password are the
     * same.
     *
     * @param o
     *        The other object to compare with.
     *
     * @return <code>true</code> if the object is equivalent.
     */
    @Override
    public boolean equals(final Object o) {
        if (o == null) {
            return false;
        }
        if (this == o) {
            return true;
        }

        if (o instanceof UsernamePasswordCredentials) {
            final UsernamePasswordCredentials that = (UsernamePasswordCredentials) o;

            if (LangUtils.equals(username, that.username) && LangUtils.equals(password, that.password)) {
                return true;
            }
        }
        return false;
    }

    public static class PatCredentials extends UsernamePasswordCredentials {
        public static final String USERNAME_FOR_CODE_ACCESS_PAT = "_VSTS_Code_Access_Token_"; //$NON-NLS-1$
        public static final String TOKEN_DESCRIPTION = "TEE: {0} on: {1}"; //$NON-NLS-1$

        public PatCredentials(final String pat) {
            super(USERNAME_FOR_CODE_ACCESS_PAT, pat == null ? StringUtil.EMPTY : pat);
        }
    }
}
