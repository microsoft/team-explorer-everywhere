/*
 * $Header:
 * /home/jerenkrantz/tmp/commons/commons-convert/cvs/home/cvs/jakarta-commons
 * //httpclient/src/java/org/apache/commons/httpclient/util/ExceptionUtil.java,v
 * 1.5 2004/10/19 18:09:46 olegk Exp $ $Revision: 480424 $ $Date: 2006-11-29
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
package com.microsoft.tfs.core.httpclient.util;

import java.io.InterruptedIOException;
import java.lang.reflect.Method;
import java.net.SocketTimeoutException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * The home for utility methods that handle various exception-related tasks.
 *
 * @author <a href="mailto:oleg@ural.ru">Oleg Kalnichevski</a>
 * @author <a href="mailto:laura@lwerner.org">Laura Werner</a>
 *
 * @since 3.0
 */
public class ExceptionUtil {

    /** Log object for this class. */
    private static final Log LOG = LogFactory.getLog(ExceptionUtil.class);

    /**
     * A reference to Throwable's initCause method, or null if it's not there in
     * this JVM
     */
    static private final Method INIT_CAUSE_METHOD = getInitCauseMethod();

    /**
     * Returns a <code>Method<code> allowing access to
     * {@link Throwable.initCause(Throwable) initCause} method of {@link Throwable},
     * or <code>null</code> if the method does not exist.
     *
     * @return A <code>Method<code> for <code>Throwable.initCause</code>, or
     *         <code>null</code> if unavailable.
     */
    static private Method getInitCauseMethod() {
        try {
            final Class[] paramsClasses = new Class[] {
                Throwable.class
            };
            return Throwable.class.getMethod("initCause", paramsClasses);
        } catch (final NoSuchMethodException e) {
            return null;
        }
    }

    /**
     * If we're running on JDK 1.4 or later, initialize the cause for the given
     * throwable.
     *
     * @param throwable
     *        The throwable.
     * @param cause
     *        The cause of the throwable.
     */
    public static void initCause(final Throwable throwable, final Throwable cause) {
        if (INIT_CAUSE_METHOD != null) {
            try {
                INIT_CAUSE_METHOD.invoke(throwable, new Object[] {
                    cause
                });
            } catch (final Exception e) {
                LOG.warn("Exception invoking Throwable.initCause", e);
            }
        }
    }

    /**
     * If SocketTimeoutExceptionClass is defined, returns <tt>true</tt> only if
     * the exception is an instance of SocketTimeoutExceptionClass. If
     * SocketTimeoutExceptionClass is undefined, always returns <tt>true</tt>.
     *
     * @param e
     *        an instance of InterruptedIOException class.
     *
     * @return <tt>true</tt> if the exception signals socket timeout,
     *         <tt>false</tt> otherwise.
     */
    public static boolean isSocketTimeoutException(final InterruptedIOException e) {
        return e instanceof SocketTimeoutException;
    }
}
