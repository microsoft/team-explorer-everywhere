/*
 * $Header:
 * /home/jerenkrantz/tmp/commons/commons-convert/cvs/home/cvs/jakarta-commons
 * //httpclient
 * /src/java/org/apache/commons/httpclient/auth/InvalidCredentialsException
 * .java,v 1.4 2004/05/13 04:02:00 mbecke Exp $ $Revision: 480424 $ $Date:
 * 2006-11-29 06:56:49 +0100 (Wed, 29 Nov 2006) $
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
 * Authentication credentials required to respond to a authentication challenge
 * are invalid
 *
 * @author <a href="mailto:oleg@ural.ru">Oleg Kalnichevski</a>
 *
 * @since 3.0
 */
public class InvalidCredentialsException extends AuthenticationException {
    /**
     * Creates a new InvalidCredentialsException with a <tt>null</tt> detail
     * message.
     */
    public InvalidCredentialsException() {
        super();
    }

    /**
     * Creates a new InvalidCredentialsException with the specified message.
     *
     * @param message
     *        the exception detail message
     */
    public InvalidCredentialsException(final String message) {
        super(message);
    }

    /**
     * Creates a new InvalidCredentialsException with the specified detail
     * message and cause.
     *
     * @param message
     *        the exception detail message
     * @param cause
     *        the <tt>Throwable</tt> that caused this exception, or
     *        <tt>null</tt> if the cause is unavailable, unknown, or not a
     *        <tt>Throwable</tt>
     */
    public InvalidCredentialsException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
