/*
 * $Header:
 * /home/jerenkrantz/tmp/commons/commons-convert/cvs/home/cvs/jakarta-commons
 * //httpclient
 * /src/java/org/apache/commons/httpclient/auth/AuthSchemeBase.java,v 1.7
 * 2004/04/18 23:51:36 jsdever Exp $ $Revision: 480424 $ $Date: 2006-11-29
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

package com.microsoft.tfs.core.httpclient.auth;

/**
 * <p>
 * Abstract authentication scheme class that implements {@link AuthScheme}
 * interface and provides a default contstructor.
 * </p>
 *
 * @deprecated No longer used
 *
 * @author <a href="mailto:oleg@ural.ru">Oleg Kalnichevski</a>
 */
@Deprecated
public abstract class AuthSchemeBase implements AuthScheme {

    /**
     * Original challenge string as received from the server.
     */
    private String challenge = null;

    /**
     * Constructor for an abstract authetication schemes.
     *
     * @param challenge
     *        authentication challenge
     *
     * @throws MalformedChallengeException
     *         is thrown if the authentication challenge is malformed
     *
     * @deprecated Use parameterless constructor and
     *             {@link AuthScheme#processChallenge(String)} method
     */
    @Deprecated
    public AuthSchemeBase(final String challenge) throws MalformedChallengeException {
        super();
        if (challenge == null) {
            throw new IllegalArgumentException("Challenge may not be null");
        }
        this.challenge = challenge;
    }

    /**
     * @see java.lang.Object#equals(Object)
     */
    @Override
    public boolean equals(final Object obj) {
        if (obj instanceof AuthSchemeBase) {
            return challenge.equals(((AuthSchemeBase) obj).challenge);
        } else {
            return super.equals(obj);
        }
    }

    /**
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        return challenge.hashCode();
    }

    /**
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return challenge;
    }
}
