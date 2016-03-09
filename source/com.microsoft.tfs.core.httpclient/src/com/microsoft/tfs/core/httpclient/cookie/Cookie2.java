/*
 * $Header:
 * /cvsroot/httpc-cookie2/httpc-cookie2/httpcookie2SVN-patch.082805-2100.diff,v
 * 1.1 2005/08/29 05:01:58 sjain700 Exp $ $Revision:400312 $ $Date:2006-05-06
 * 14:49:41 +0200 (Sat, 06 May 2006) $
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

import java.util.Date;

import com.microsoft.tfs.core.httpclient.Cookie;

/**
 * <p>
 * Cookie class for {@link com.microsoft.tfs.core.httpclient.cookie.RFC2965Spec}
 * cookie specification. It extends {@link Cookie} class and adds newer cookie
 * attributes and functions required for this specification.
 * </p>
 *
 * @author Samit Jain (jain.samit@gmail.com)
 *
 * @since 3.1
 */
public class Cookie2 extends Cookie {
    private static final long serialVersionUID = -1509793557066804646L;

    // string constants for cookie attributes
    public static final String DOMAIN = "domain";
    public static final String PATH = "path";
    public static final String PORT = "port";
    public static final String VERSION = "version";
    public static final String SECURE = "secure";
    public static final String MAXAGE = "max-age";
    public static final String COMMENT = "comment";
    public static final String COMMENTURL = "commenturl";
    public static final String DISCARD = "discard";

    /**
     * Default constructor. Creates a blank cookie
     */
    public Cookie2() {
        super(null, "noname", null, null, null, false);
    }

    /**
     * Creates a cookie with the given name, value and domain attribute.
     *
     * @param name
     *        the cookie name
     * @param value
     *        the cookie value
     * @param domain
     *        the domain this cookie can be sent to
     */
    public Cookie2(final String domain, final String name, final String value) {
        super(domain, name, value);
    }

    /**
     * Creates a cookie with the given name, value, domain attribute, path
     * attribute, expiration attribute, and secure attribute
     *
     * @param name
     *        the cookie name
     * @param value
     *        the cookie value
     * @param domain
     *        the domain this cookie can be sent to
     * @param path
     *        the path prefix for which this cookie can be sent
     * @param expires
     *        the {@link Date} at which this cookie expires, or <tt>null</tt> if
     *        the cookie expires at the end of the session
     * @param secure
     *        if true this cookie can only be sent over secure connections
     * @throws IllegalArgumentException
     *         If cookie name is null or blank, cookie name contains a blank, or
     *         cookie name starts with character $
     *
     */
    public Cookie2(
        final String domain,
        final String name,
        final String value,
        final String path,
        final Date expires,
        final boolean secure) {
        super(domain, name, value, path, expires, secure);
    }

    /**
     * Creates a cookie with the given name, value, domain attribute, path
     * attribute, expiration attribute, secure attribute, and ports attribute.
     *
     * @param name
     *        the cookie name
     * @param value
     *        the cookie value
     * @param domain
     *        the domain this cookie can be sent to
     * @param path
     *        the path prefix for which this cookie can be sent
     * @param expires
     *        the {@link Date} at which this cookie expires, or <tt>null</tt> if
     *        the cookie expires at the end of the session
     * @param secure
     *        if true this cookie can only be sent over secure connections
     * @param ports
     *        the ports for which this cookie can be sent
     * @throws IllegalArgumentException
     *         If cookie name is null or blank, cookie name contains a blank, or
     *         cookie name starts with character $
     *
     */
    public Cookie2(
        final String domain,
        final String name,
        final String value,
        final String path,
        final Date expires,
        final boolean secure,
        final int[] ports) {
        super(domain, name, value, path, expires, secure);
        setPorts(ports);
    }

    /**
     * If a user agent (web browser) presents this cookie to a user, the
     * cookie's purpose will be described by the information at this URL.
     *
     * @see #setCommentURL(String)
     */
    public String getCommentURL() {
        return cookieCommentURL;
    }

    /**
     * If a user agent (web browser) presents this cookie to a user, the
     * cookie's purpose will be described by the information at this URL.
     *
     * @param commentURL
     *
     * @see #getCommentURL()
     */
    public void setCommentURL(final String commentURL) {
        cookieCommentURL = commentURL;
    }

    /**
     * Get the Port attribute. It restricts the ports to which a cookie may be
     * returned in a Cookie request header.
     *
     * @see #setPorts(int[])
     */
    public int[] getPorts() {
        return cookiePorts;
    }

    /**
     * Set the Port attribute. It restricts the ports to which a cookie may be
     * returned in a Cookie request header.
     *
     * @param ports
     *
     * @see #getPorts()
     */
    public void setPorts(final int[] ports) {
        cookiePorts = ports;
    }

    /**
     * Set the Discard attribute.
     *
     * Note: <tt>Discard</tt> attribute overrides <tt>Max-age</tt>.
     *
     * @see #isPersistent()
     */
    public void setDiscard(final boolean toDiscard) {
        discard = toDiscard;
    }

    /**
     * Returns <tt>false</tt> if the cookie should be discarded at the end of
     * the "session"; <tt>true</tt> otherwise.
     *
     * @return <tt>false</tt> if the cookie should be discarded at the end of
     *         the "session"; <tt>true</tt> otherwise
     */
    @Override
    public boolean isPersistent() {
        return (null != getExpiryDate()) && !discard;
    }

    /**
     * Indicates whether the cookie had a port attribute specified in the
     * <tt>Set-Cookie2</tt> response header.
     *
     * @param value
     *        <tt>true</tt> if port attribute is specified in response header.
     *
     * @see #isPortAttributeSpecified
     */
    public void setPortAttributeSpecified(final boolean value) {
        hasPortAttribute = value;
    }

    /**
     * @return <tt>true</tt> if cookie port attribute was specified in the
     *         <tt>Set-Cookie2</tt> header.
     *
     * @see #setPortAttributeSpecified
     */
    public boolean isPortAttributeSpecified() {
        return hasPortAttribute;
    }

    /**
     * Indicates whether the Port attribute in <tt>Set-Cookie2</tt> header
     * contains no value (is of the form Port="").
     * <p>
     * This value is required for generating the <tt>Cookie</tt> request header
     * because the specification requires that if <tt>Set-Cookie2</tt> header
     * contains a blank value for port attribute, the <tt>Cookie</tt> header
     * should also contain a port attribute with no value.
     *
     * @param value
     *        <tt>true</tt> if port attribute is specified as blank in response
     *        header.
     *
     * @see #isPortAttributeBlank
     */
    public void setPortAttributeBlank(final boolean value) {
        isPortAttributeBlank = value;
    }

    /**
     * @return <tt>true</tt> if the port attribute in <tt>Set-Cookie2</tt>
     *         header had no value (was of the form Port="").
     *
     * @see #setPortAttributeBlank
     */
    public boolean isPortAttributeBlank() {
        return isPortAttributeBlank;
    }

    /**
     * Indicates whether the cookie had a version attribute specified in the
     * <tt>Set-Cookie2</tt> response header.
     *
     * @param value
     *        <tt>true</tt> if version attribute is specified in response
     *        header.
     * @see #isVersionAttributeSpecified()
     */
    public void setVersionAttributeSpecified(final boolean value) {
        hasVersionAttribute = value;
    }

    /**
     * @return <tt>true</tt> if cookie version attribute was specified in the
     *         <tt>Set-Cookie2</tt> header.
     *
     * @see #setVersionAttributeSpecified
     */
    public boolean isVersionAttributeSpecified() {
        return hasVersionAttribute;
    }

    /**
     * Return a textual representation of the cookie.
     *
     * @return string.
     */
    @Override
    public String toExternalForm() {
        final CookieSpec spec = CookiePolicy.getCookieSpec(CookiePolicy.RFC_2965);
        return spec.formatCookie(this);
    }

    /**
     * Comment URL attribute
     */
    private String cookieCommentURL;

    /**
     * Port attribute.
     */
    private int[] cookiePorts;

    /**
     * Discard attribute.
     */
    private boolean discard = false;

    /**
     * Indicates if the set-cookie2 header included a Port attribute for this
     * cookie
     */
    private boolean hasPortAttribute = false;

    /**
     * Indicates if the set-cookie2 header's Port attribute did not have any
     * value.
     */
    private boolean isPortAttributeBlank = false;

    /**
     * Indicates if the set-cookie2 header included a Version attribute
     */
    private boolean hasVersionAttribute = false;

}
