/*
 * $Header:
 * /home/jerenkrantz/tmp/commons/commons-convert/cvs/home/cvs/jakarta-commons
 * //httpclient
 * /src/java/org/apache/commons/httpclient/auth/CredentialsProvider.java,v 1.6
 * 2004/07/05 22:46:59 olegk Exp $ $Revision: 480424 $ $Date: 2006-11-29
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

import com.microsoft.tfs.core.httpclient.Credentials;

/**
 * <p>
 * Credentials provider interface can be used to provide
 * {@link com.microsoft.tfs.core.httpclient.HttpMethod HTTP method} with a means
 * to request authentication credentials if no credentials have been given or
 * given credentials are incorrect.
 * </p>
 * <p>
 * HttpClient makes no provisions to check whether the same credentials have
 * been tried already. It is a responsibility of the custom credentials provider
 * to keep track of authentication attempts and to ensure that credentials known
 * to be invalid are not retried. HttpClient will simply store the set of
 * credentials returned by the custom credentials provider in the
 * {@link com.microsoft.tfs.core.httpclient.HttpState http state} object and
 * will attempt to use these credentials for all subsequent requests with the
 * given authentication scope.
 * </p>
 * <p>
 * Classes implementing this interface must synchronize access to shared data as
 * methods of this interfrace may be executed from multiple threads
 * </p>
 *
 *
 * @author Ortwin Glueck
 * @author <a href="mailto:oleg@ural.ru">Oleg Kalnichevski</a>
 *
 * @since 3.0
 */
public interface CredentialsProvider {

    /**
     * Sets the credentials provider parameter.
     * <p>
     * This parameter expects a value of type {@link CredentialsProvider}.
     * </p>
     */
    public static final String PROVIDER = "http.authentication.credential-provider";

    /**
     * Requests additional {@link Credentials authentication credentials}.
     *
     * @param scheme
     *        the {@link AuthScheme authentication scheme}
     * @param host
     *        the authentication host
     * @param port
     *        the port of the authentication host
     * @param proxy
     *        <tt>true</tt> if authenticating with a proxy, <tt>false</tt>
     *        otherwise
     */
    public Credentials getCredentials(final AuthScheme scheme, final String host, int port, boolean proxy)
        throws CredentialsNotAvailableException;

}
