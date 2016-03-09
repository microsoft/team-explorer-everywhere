/*
 * $Header:
 * /home/jerenkrantz/tmp/commons/commons-convert/cvs/home/cvs/jakarta-commons
 * //httpclient
 * /src/java/org/apache/commons/httpclient/cookie/NetscapeDraftSpec.java,v 1.11
 * 2004/05/13 04:02:00 mbecke Exp $ $Revision: 480424 $ $Date: 2006-11-29
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

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.StringTokenizer;

import com.microsoft.tfs.core.httpclient.Cookie;
import com.microsoft.tfs.core.httpclient.HeaderElement;
import com.microsoft.tfs.core.httpclient.NameValuePair;

/**
 * <P>
 * Netscape cookie draft specific cookie management functions
 *
 * @author B.C. Holmes
 * @author <a href="mailto:jericho@thinkfree.com">Park, Sung-Gu</a>
 * @author <a href="mailto:dsale@us.britannica.com">Doug Sale</a>
 * @author Rod Waldhoff
 * @author dIon Gillard
 * @author Sean C. Sullivan
 * @author <a href="mailto:JEvans@Cyveillance.com">John Evans</a>
 * @author Marc A. Saegesser
 * @author <a href="mailto:oleg@ural.ru">Oleg Kalnichevski</a>
 * @author <a href="mailto:mbowler@GargoyleSoftware.com">Mike Bowler</a>
 *
 * @since 2.0
 */

public class NetscapeDraftSpec extends CookieSpecBase {

    /** Default constructor */
    public NetscapeDraftSpec() {
        super();
    }

    /**
     * Parses the Set-Cookie value into an array of <tt>Cookie</tt>s.
     *
     * <p>
     * Syntax of the Set-Cookie HTTP Response Header:
     * </p>
     *
     * <p>
     * This is the format a CGI script would use to add to the HTTP headers a
     * new piece of data which is to be stored by the client for later
     * retrieval.
     * </p>
     *
     * <PRE>
     *  Set-Cookie: NAME=VALUE; expires=DATE; path=PATH; domain=DOMAIN_NAME; secure
     * </PRE>
     *
     * <p>
     * Please note that Netscape draft specification does not fully conform to
     * the HTTP header format. Netscape draft does not specify whether multiple
     * cookies may be sent in one header. Hence, comma character may be present
     * in unquoted cookie value or unquoted parameter value.
     * </p>
     *
     * @link http://wp.netscape.com/newsref/std/cookie_spec.html
     *
     * @param host
     *        the host from which the <tt>Set-Cookie</tt> value was received
     * @param port
     *        the port from which the <tt>Set-Cookie</tt> value was received
     * @param path
     *        the path from which the <tt>Set-Cookie</tt> value was received
     * @param secure
     *        <tt>true</tt> when the <tt>Set-Cookie</tt> value was received over
     *        secure conection
     * @param header
     *        the <tt>Set-Cookie</tt> received from the server
     * @return an array of <tt>Cookie</tt>s parsed from the Set-Cookie value
     * @throws MalformedCookieException
     *         if an exception occurs during parsing
     *
     * @since 3.0
     */
    @Override
    public Cookie[] parse(String host, final int port, String path, final boolean secure, final String header)
        throws MalformedCookieException {

        LOG.trace("enter NetscapeDraftSpec.parse(String, port, path, boolean, Header)");

        if (host == null) {
            throw new IllegalArgumentException("Host of origin may not be null");
        }
        if (host.trim().equals("")) {
            throw new IllegalArgumentException("Host of origin may not be blank");
        }
        if (port < 0) {
            throw new IllegalArgumentException("Invalid port: " + port);
        }
        if (path == null) {
            throw new IllegalArgumentException("Path of origin may not be null.");
        }
        if (header == null) {
            throw new IllegalArgumentException("Header may not be null.");
        }

        if (path.trim().equals("")) {
            path = PATH_DELIM;
        }
        host = host.toLowerCase();

        String defaultPath = path;
        int lastSlashIndex = defaultPath.lastIndexOf(PATH_DELIM);
        if (lastSlashIndex >= 0) {
            if (lastSlashIndex == 0) {
                // Do not remove the very first slash
                lastSlashIndex = 1;
            }
            defaultPath = defaultPath.substring(0, lastSlashIndex);
        }
        final HeaderElement headerelement = new HeaderElement(header.toCharArray());
        final Cookie cookie =
            new Cookie(host, headerelement.getName(), headerelement.getValue(), defaultPath, null, false);
        // cycle through the parameters
        final NameValuePair[] parameters = headerelement.getParameters();
        // could be null. In case only a header element and no parameters.
        if (parameters != null) {
            for (int j = 0; j < parameters.length; j++) {
                parseAttribute(parameters[j], cookie);
            }
        }
        return new Cookie[] {
            cookie
        };
    }

    /**
     * Parse the cookie attribute and update the corresponsing {@link Cookie}
     * properties as defined by the Netscape draft specification
     *
     * @param attribute
     *        {@link NameValuePair} cookie attribute from the
     *        <tt>Set- Cookie</tt>
     * @param cookie
     *        {@link Cookie} to be updated
     * @throws MalformedCookieException
     *         if an exception occurs during parsing
     */
    @Override
    public void parseAttribute(final NameValuePair attribute, final Cookie cookie) throws MalformedCookieException {

        if (attribute == null) {
            throw new IllegalArgumentException("Attribute may not be null.");
        }
        if (cookie == null) {
            throw new IllegalArgumentException("Cookie may not be null.");
        }
        final String paramName = attribute.getName().toLowerCase();
        final String paramValue = attribute.getValue();

        if (paramName.equals("expires")) {

            if (paramValue == null) {
                throw new MalformedCookieException("Missing value for expires attribute");
            }
            try {
                final DateFormat expiryFormat = new SimpleDateFormat("EEE, dd-MMM-yyyy HH:mm:ss z", Locale.US);
                final Date date = expiryFormat.parse(paramValue);
                cookie.setExpiryDate(date);
            } catch (final ParseException e) {
                throw new MalformedCookieException("Invalid expires " + "attribute: " + e.getMessage());
            }
        } else {
            super.parseAttribute(attribute, cookie);
        }
    }

    /**
     * Performs domain-match as described in the Netscape draft.
     *
     * @param host
     *        The target host.
     * @param domain
     *        The cookie domain attribute.
     * @return true if the specified host matches the given domain.
     */
    @Override
    public boolean domainMatch(final String host, final String domain) {
        return host.endsWith(domain);
    }

    /**
     * Performs Netscape draft compliant {@link Cookie} validation
     *
     * @param host
     *        the host from which the {@link Cookie} was received
     * @param port
     *        the port from which the {@link Cookie} was received
     * @param path
     *        the path from which the {@link Cookie} was received
     * @param secure
     *        <tt>true</tt> when the {@link Cookie} was received using a secure
     *        connection
     * @param cookie
     *        The cookie to validate.
     * @throws MalformedCookieException
     *         if an exception occurs during validation
     */
    @Override
    public void validate(
        final String host,
        final int port,
        final String path,
        final boolean secure,
        final Cookie cookie) throws MalformedCookieException {

        LOG.trace("enterNetscapeDraftCookieProcessor " + "RCF2109CookieProcessor.validate(Cookie)");
        // Perform generic validation
        super.validate(host, port, path, secure, cookie);
        // Perform Netscape Cookie draft specific validation
        if (host.indexOf(".") >= 0) {
            final int domainParts = new StringTokenizer(cookie.getDomain(), ".").countTokens();

            if (isSpecialDomain(cookie.getDomain())) {
                if (domainParts < 2) {
                    throw new MalformedCookieException(
                        "Domain attribute \""
                            + cookie.getDomain()
                            + "\" violates the Netscape cookie specification for "
                            + "special domains");
                }
            } else {
                if (domainParts < 3) {
                    throw new MalformedCookieException(
                        "Domain attribute \"" + cookie.getDomain() + "\" violates the Netscape cookie specification");
                }
            }
        }
    }

    /**
     * Checks if the given domain is in one of the seven special top level
     * domains defined by the Netscape cookie specification.
     *
     * @param domain
     *        The domain.
     * @return True if the specified domain is "special"
     */
    private static boolean isSpecialDomain(final String domain) {
        final String ucDomain = domain.toUpperCase();
        if (ucDomain.endsWith(".COM")
            || ucDomain.endsWith(".EDU")
            || ucDomain.endsWith(".NET")
            || ucDomain.endsWith(".GOV")
            || ucDomain.endsWith(".MIL")
            || ucDomain.endsWith(".ORG")
            || ucDomain.endsWith(".INT")) {
            return true;
        }
        return false;
    }
}
