/*
 * $Header:
 * /home/jerenkrantz/tmp/commons/commons-convert/cvs/home/cvs/jakarta-commons
 * //httpclient
 * /src/java/org/apache/commons/httpclient/cookie/IgnoreCookiesSpec.java,v 1.6
 * 2004/09/14 20:11:31 olegk Exp $ $Revision: 480424 $ $Date: 2006-11-29
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

package com.microsoft.tfs.core.httpclient.cookie;

import java.net.URI;
import java.text.SimpleDateFormat;
import java.util.Collection;

import com.microsoft.tfs.core.httpclient.Cookie;
import com.microsoft.tfs.core.httpclient.Header;
import com.microsoft.tfs.core.httpclient.NameValuePair;

/**
 * A cookie spec that does nothing. Cookies are neither parsed, formatted nor
 * matched. It can be used to effectively disable cookies altogether.
 *
 * @since 3.0
 */
public class IgnoreCookiesSpec implements CookieSpec {

    /**
     *
     */
    public IgnoreCookiesSpec() {
        super();
    }

    /**
     * Returns an empty {@link Cookie cookie} array. All parameters are ignored.
     */
    @Override
    public Cookie[] parse(
        final String host,
        final int port,
        final String path,
        final boolean secure,
        final String header) throws MalformedCookieException {
        return new Cookie[0];
    }

    /**
     * @return <code>null</code>
     */
    @Override
    public Collection<SimpleDateFormat> getValidDateFormats() {
        return null;
    }

    /**
     * Does nothing.
     */
    @Override
    public void setValidDateFormats(final Collection<SimpleDateFormat> datepatterns) {
    }

    /**
     * @return <code>null</code>
     */
    @Override
    public String formatCookie(final Cookie cookie) {
        return null;
    }

    /**
     * @return <code>null</code>
     */
    @Override
    public Header formatCookieHeader(final Cookie cookie) throws IllegalArgumentException {
        return null;
    }

    /**
     * @return <code>null</code>
     */
    @Override
    public Header formatCookieHeader(final Cookie[] cookies) throws IllegalArgumentException {
        return null;
    }

    /**
     * @return <code>null</code>
     */
    @Override
    public String formatCookies(final Cookie[] cookies) throws IllegalArgumentException {
        return null;
    }

    /**
     * @return <code>false</code>
     */
    @Override
    public boolean match(
        final String host,
        final int port,
        final String path,
        final boolean secure,
        final Cookie cookie) {
        return false;
    }

    /**
     * Returns an empty {@link Cookie cookie} array. All parameters are ignored.
     */
    @Override
    public Cookie[] match(
        final String host,
        final int port,
        final String path,
        final boolean secure,
        final Cookie[] cookies) {
        return new Cookie[0];
    }

    /**
     * Returns an empty {@link Cookie cookie} array. All parameters are ignored.
     */
    @Override
    public Cookie[] parse(final URI uri, final Header header)
        throws MalformedCookieException,
            IllegalArgumentException {
        return new Cookie[0];
    }

    /**
     * Returns an empty {@link Cookie cookie} array. All parameters are ignored.
     */
    @Override
    public Cookie[] parse(final URI uri, final String header)
        throws MalformedCookieException,
            IllegalArgumentException {
        return new Cookie[0];
    }

    /**
     * Returns an empty {@link Cookie cookie} array. All parameters are ignored.
     */
    @Override
    public Cookie[] parse(
        final String host,
        final int port,
        final String path,
        final boolean secure,
        final Header header) throws MalformedCookieException, IllegalArgumentException {
        return new Cookie[0];
    }

    /**
     * Does nothing.
     */
    @Override
    public void parseAttribute(final NameValuePair attribute, final Cookie cookie)
        throws MalformedCookieException,
            IllegalArgumentException {
    }

    /**
     * Does nothing.
     */
    @Override
    public void validate(
        final String host,
        final int port,
        final String path,
        final boolean secure,
        final Cookie cookie) throws MalformedCookieException, IllegalArgumentException {
    }

    /**
     * @return <code>false</code>
     */
    @Override
    public boolean domainMatch(final String host, final String domain) {
        return false;
    }

    /**
     * @return <code>false</code>
     */
    @Override
    public boolean pathMatch(final String path, final String topmostPath) {
        return false;
    }

}
