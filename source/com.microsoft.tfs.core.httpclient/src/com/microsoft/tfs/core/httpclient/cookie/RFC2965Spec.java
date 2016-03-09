/*
 * $HeadURL:
 * https://svn.apache.org/repos/asf/jakarta/httpcomponents/oac.hc3x/tags
 * /HTTPCLIENT_3_1
 * /src/java/org/apache/commons/httpclient/cookie/RFC2965Spec.java $ $Revision:
 * 507134 $ $Date: 2007-02-13 19:18:05 +0100 (Tue, 13 Feb 2007) $
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.StringTokenizer;

import com.microsoft.tfs.core.httpclient.Cookie;
import com.microsoft.tfs.core.httpclient.Header;
import com.microsoft.tfs.core.httpclient.HeaderElement;
import com.microsoft.tfs.core.httpclient.NameValuePair;
import com.microsoft.tfs.core.httpclient.util.ParameterFormatter;

/**
 * <p>
 * RFC 2965 specific cookie management functions.
 * </p>
 *
 * @author jain.samit@gmail.com (Samit Jain)
 *
 * @since 3.1
 */
public class RFC2965Spec extends CookieSpecBase implements CookieVersionSupport {

    private static final Comparator<Cookie> PATH_COMPARATOR = new CookiePathComparator();

    /**
     * Cookie Response Header name for cookies processed by this spec.
     */
    public final static String SET_COOKIE2_KEY = "set-cookie2";

    /**
     * used for formatting RFC 2956 style cookies
     */
    private final ParameterFormatter formatter;

    /**
     * Stores the list of attribute handlers
     */
    private final List<CookieAttributeHandler> attribHandlerList;

    /**
     * Stores attribute name -> attribute handler mappings
     */
    private final Map<String, CookieAttributeHandler> attribHandlerMap;

    /**
     * Fallback cookie spec (RFC 2109)
     */
    private final CookieSpec rfc2109;

    /**
     * Default constructor
     */
    public RFC2965Spec() {
        super();
        formatter = new ParameterFormatter();
        formatter.setAlwaysUseQuotes(true);
        attribHandlerMap = new HashMap<String, CookieAttributeHandler>(10);
        attribHandlerList = new ArrayList<CookieAttributeHandler>(10);
        rfc2109 = new RFC2109Spec();

        registerAttribHandler(Cookie2.PATH, new Cookie2PathAttributeHandler());
        registerAttribHandler(Cookie2.DOMAIN, new Cookie2DomainAttributeHandler());
        registerAttribHandler(Cookie2.PORT, new Cookie2PortAttributeHandler());
        registerAttribHandler(Cookie2.MAXAGE, new Cookie2MaxageAttributeHandler());
        registerAttribHandler(Cookie2.SECURE, new CookieSecureAttributeHandler());
        registerAttribHandler(Cookie2.COMMENT, new CookieCommentAttributeHandler());
        registerAttribHandler(Cookie2.COMMENTURL, new CookieCommentUrlAttributeHandler());
        registerAttribHandler(Cookie2.DISCARD, new CookieDiscardAttributeHandler());
        registerAttribHandler(Cookie2.VERSION, new Cookie2VersionAttributeHandler());
    }

    protected void registerAttribHandler(final String name, final CookieAttributeHandler handler) {
        if (name == null) {
            throw new IllegalArgumentException("Attribute name may not be null");
        }
        if (handler == null) {
            throw new IllegalArgumentException("Attribute handler may not be null");
        }
        if (!attribHandlerList.contains(handler)) {
            attribHandlerList.add(handler);
        }
        attribHandlerMap.put(name, handler);
    }

    /**
     * Finds an attribute handler {@link CookieAttributeHandler} for the given
     * attribute. Returns <tt>null</tt> if no attribute handler is found for the
     * specified attribute.
     *
     * @param name
     *        attribute name. e.g. Domain, Path, etc.
     * @return an attribute handler or <tt>null</tt>
     */
    protected CookieAttributeHandler findAttribHandler(final String name) {
        return attribHandlerMap.get(name);
    }

    /**
     * Gets attribute handler {@link CookieAttributeHandler} for the given
     * attribute.
     *
     * @param name
     *        attribute name. e.g. Domain, Path, etc.
     * @throws IllegalStateException
     *         if handler not found for the specified attribute.
     */
    protected CookieAttributeHandler getAttribHandler(final String name) {
        final CookieAttributeHandler handler = findAttribHandler(name);
        if (handler == null) {
            throw new IllegalStateException("Handler not registered for " + name + " attribute.");
        } else {
            return handler;
        }
    }

    protected Iterator<CookieAttributeHandler> getAttribHandlerIterator() {
        return attribHandlerList.iterator();
    }

    /**
     * Parses the Set-Cookie2 value into an array of <tt>Cookie</tt>s.
     *
     * <P>
     * The syntax for the Set-Cookie2 response header is:
     *
     * <PRE>
     * set-cookie      =    "Set-Cookie2:" cookies
     * cookies         =    1#cookie
     * cookie          =    NAME "=" VALUE * (";" cookie-av)
     * NAME            =    attr
     * VALUE           =    value
     * cookie-av       =    "Comment" "=" value
     *                 |    "CommentURL" "=" <"> http_URL <">
     *                 |    "Discard"
     *                 |    "Domain" "=" value
     *                 |    "Max-Age" "=" value
     *                 |    "Path" "=" value
     *                 |    "Port" [ "=" <"> portlist <"> ]
     *                 |    "Secure"
     *                 |    "Version" "=" 1*DIGIT
     * portlist        =       1#portnum
     * portnum         =       1*DIGIT
     * </PRE>
     *
     * @param host
     *        the host from which the <tt>Set-Cookie2</tt> value was received
     * @param port
     *        the port from which the <tt>Set-Cookie2</tt> value was received
     * @param path
     *        the path from which the <tt>Set-Cookie2</tt> value was received
     * @param secure
     *        <tt>true</tt> when the <tt>Set-Cookie2</tt> value was received
     *        over secure conection
     * @param header
     *        the <tt>Set-Cookie2</tt> <tt>Header</tt> received from the server
     * @return an array of <tt>Cookie</tt>s parsed from the Set-Cookie2 value
     * @throws MalformedCookieException
     *         if an exception occurs during parsing
     */
    @Override
    public Cookie[] parse(
        final String host,
        final int port,
        final String path,
        final boolean secure,
        final Header header) throws MalformedCookieException {
        LOG.trace("enter RFC2965.parse(" + "String, int, String, boolean, Header)");

        if (header == null) {
            throw new IllegalArgumentException("Header may not be null.");
        }
        if (header.getName() == null) {
            throw new IllegalArgumentException("Header name may not be null.");
        }

        if (header.getName().equalsIgnoreCase(SET_COOKIE2_KEY)) {
            // parse cookie2 cookies
            return parse(host, port, path, secure, header.getValue());
        } else if (header.getName().equalsIgnoreCase(RFC2109Spec.SET_COOKIE_KEY)) {
            // delegate parsing of old-style cookies to rfc2109Spec
            return rfc2109.parse(host, port, path, secure, header.getValue());
        } else {
            throw new MalformedCookieException(
                "Header name is not valid. " + "RFC 2965 supports \"set-cookie\" " + "and \"set-cookie2\" headers.");
        }
    }

    /**
     * @see #parse(String, int, String, boolean,
     *      com.microsoft.tfs.core.httpclient.Header)
     */
    @Override
    public Cookie[] parse(String host, final int port, String path, final boolean secure, final String header)
        throws MalformedCookieException {
        LOG.trace("enter RFC2965Spec.parse(" + "String, int, String, boolean, String)");

        // before we do anything, lets check validity of arguments
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
        host = getEffectiveHost(host);

        final HeaderElement[] headerElements = HeaderElement.parseElements(header.toCharArray());

        final List<Cookie> cookies = new LinkedList<Cookie>();
        for (int i = 0; i < headerElements.length; i++) {
            final HeaderElement headerelement = headerElements[i];
            Cookie2 cookie = null;
            try {
                cookie =
                    new Cookie2(host, headerelement.getName(), headerelement.getValue(), path, null, false, new int[] {
                        port
                });
            } catch (final IllegalArgumentException ex) {
                throw new MalformedCookieException(ex.getMessage());
            }
            final NameValuePair[] parameters = headerelement.getParameters();
            // could be null. In case only a header element and no parameters.
            if (parameters != null) {
                // Eliminate duplicate attribues. The first occurence takes
                // precedence
                final Map<String, NameValuePair> attribmap = new HashMap<String, NameValuePair>(parameters.length);
                for (int j = parameters.length - 1; j >= 0; j--) {
                    final NameValuePair param = parameters[j];
                    attribmap.put(param.getName().toLowerCase(), param);
                }
                for (final Iterator<Entry<String, NameValuePair>> it = attribmap.entrySet().iterator(); it.hasNext();) {
                    final Entry<String, NameValuePair> entry = it.next();
                    parseAttribute(entry.getValue(), cookie);
                }
            }
            cookies.add(cookie);
            // cycle through the parameters
        }
        return cookies.toArray(new Cookie[cookies.size()]);
    }

    /**
     * Parse RFC 2965 specific cookie attribute and update the corresponsing
     * {@link com.microsoft.tfs.core.httpclient.Cookie} properties.
     *
     * @param attribute
     *        {@link com.microsoft.tfs.core.httpclient.NameValuePair} cookie
     *        attribute from the <tt>Set-Cookie2</tt> header.
     * @param cookie
     *        {@link com.microsoft.tfs.core.httpclient.Cookie} to be updated
     * @throws MalformedCookieException
     *         if an exception occurs during parsing
     */
    @Override
    public void parseAttribute(final NameValuePair attribute, final Cookie cookie) throws MalformedCookieException {
        if (attribute == null) {
            throw new IllegalArgumentException("Attribute may not be null.");
        }
        if (attribute.getName() == null) {
            throw new IllegalArgumentException("Attribute Name may not be null.");
        }
        if (cookie == null) {
            throw new IllegalArgumentException("Cookie may not be null.");
        }
        final String paramName = attribute.getName().toLowerCase();
        final String paramValue = attribute.getValue();

        final CookieAttributeHandler handler = findAttribHandler(paramName);
        if (handler == null) {
            // ignore unknown attribute-value pairs
            if (LOG.isDebugEnabled()) {
                LOG.debug("Unrecognized cookie attribute: " + attribute.toString());
            }
        } else {
            handler.parse(cookie, paramValue);
        }
    }

    /**
     * Performs RFC 2965 compliant
     * {@link com.microsoft.tfs.core.httpclient.Cookie} validation
     *
     * @param host
     *        the host from which the
     *        {@link com.microsoft.tfs.core.httpclient.Cookie} was received
     * @param port
     *        the port from which the
     *        {@link com.microsoft.tfs.core.httpclient.Cookie} was received
     * @param path
     *        the path from which the
     *        {@link com.microsoft.tfs.core.httpclient.Cookie} was received
     * @param secure
     *        <tt>true</tt> when the
     *        {@link com.microsoft.tfs.core.httpclient.Cookie} was received
     *        using a secure connection
     * @param cookie
     *        The cookie to validate
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

        LOG.trace("enter RFC2965Spec.validate(String, int, String, " + "boolean, Cookie)");

        if (cookie instanceof Cookie2) {
            if (cookie.getName().indexOf(' ') != -1) {
                throw new MalformedCookieException("Cookie name may not contain blanks");
            }
            if (cookie.getName().startsWith("$")) {
                throw new MalformedCookieException("Cookie name may not start with $");
            }
            final CookieOrigin origin = new CookieOrigin(getEffectiveHost(host), port, path, secure);
            for (final Iterator<CookieAttributeHandler> i = getAttribHandlerIterator(); i.hasNext();) {
                final CookieAttributeHandler handler = i.next();
                handler.validate(cookie, origin);
            }
        } else {
            // old-style cookies are validated according to the old rules
            rfc2109.validate(host, port, path, secure, cookie);
        }
    }

    /**
     * Return <tt>true</tt> if the cookie should be submitted with a request
     * with given attributes, <tt>false</tt> otherwise.
     *
     * @param host
     *        the host to which the request is being submitted
     * @param port
     *        the port to which the request is being submitted (ignored)
     * @param path
     *        the path to which the request is being submitted
     * @param secure
     *        <tt>true</tt> if the request is using a secure connection
     * @return true if the cookie matches the criterium
     */
    @Override
    public boolean match(
        final String host,
        final int port,
        final String path,
        final boolean secure,
        final Cookie cookie) {

        LOG.trace("enter RFC2965.match(" + "String, int, String, boolean, Cookie");
        if (cookie == null) {
            throw new IllegalArgumentException("Cookie may not be null");
        }
        if (cookie instanceof Cookie2) {
            // check if cookie has expired
            if (cookie.isPersistent() && cookie.isExpired()) {
                return false;
            }
            final CookieOrigin origin = new CookieOrigin(getEffectiveHost(host), port, path, secure);
            for (final Iterator<CookieAttributeHandler> i = getAttribHandlerIterator(); i.hasNext();) {
                final CookieAttributeHandler handler = i.next();
                if (!handler.match(cookie, origin)) {
                    return false;
                }
            }
            return true;
        } else {
            // old-style cookies are matched according to the old rules
            return rfc2109.match(host, port, path, secure, cookie);
        }
    }

    private void doFormatCookie2(final Cookie2 cookie, final StringBuffer buffer) {
        final String name = cookie.getName();
        String value = cookie.getValue();
        if (value == null) {
            value = "";
        }
        formatter.format(buffer, new NameValuePair(name, value));
        // format domain attribute
        if (cookie.getDomain() != null && cookie.isDomainAttributeSpecified()) {
            buffer.append("; ");
            formatter.format(buffer, new NameValuePair("$Domain", cookie.getDomain()));
        }
        // format path attribute
        if ((cookie.getPath() != null) && (cookie.isPathAttributeSpecified())) {
            buffer.append("; ");
            formatter.format(buffer, new NameValuePair("$Path", cookie.getPath()));
        }
        // format port attribute
        if (cookie.isPortAttributeSpecified()) {
            String portValue = "";
            if (!cookie.isPortAttributeBlank()) {
                portValue = createPortAttribute(cookie.getPorts());
            }
            buffer.append("; ");
            formatter.format(buffer, new NameValuePair("$Port", portValue));
        }
    }

    /**
     * Return a string suitable for sending in a <tt>"Cookie"</tt> header as
     * defined in RFC 2965
     *
     * @param cookie
     *        a {@link com.microsoft.tfs.core.httpclient.Cookie} to be formatted
     *        as string
     * @return a string suitable for sending in a <tt>"Cookie"</tt> header.
     */
    @Override
    public String formatCookie(final Cookie cookie) {
        LOG.trace("enter RFC2965Spec.formatCookie(Cookie)");

        if (cookie == null) {
            throw new IllegalArgumentException("Cookie may not be null");
        }
        if (cookie instanceof Cookie2) {
            final Cookie2 cookie2 = (Cookie2) cookie;
            final int version = cookie2.getVersion();
            final StringBuffer buffer = new StringBuffer();
            formatter.format(buffer, new NameValuePair("$Version", Integer.toString(version)));
            buffer.append("; ");
            doFormatCookie2(cookie2, buffer);
            return buffer.toString();
        } else {
            // old-style cookies are formatted according to the old rules
            return rfc2109.formatCookie(cookie);
        }
    }

    /**
     * Create a RFC 2965 compliant <tt>"Cookie"</tt> header value containing all
     * {@link com.microsoft.tfs.core.httpclient.Cookie}s suitable for sending in
     * a <tt>"Cookie"</tt> header
     *
     * @param cookies
     *        an array of {@link com.microsoft.tfs.core.httpclient.Cookie}s to
     *        be formatted
     * @return a string suitable for sending in a Cookie header.
     */
    @Override
    public String formatCookies(final Cookie[] cookies) {
        LOG.trace("enter RFC2965Spec.formatCookieHeader(Cookie[])");

        if (cookies == null) {
            throw new IllegalArgumentException("Cookies may not be null");
        }
        // check if cookies array contains a set-cookie (old style) cookie
        boolean hasOldStyleCookie = false;
        int version = -1;
        for (int i = 0; i < cookies.length; i++) {
            final Cookie cookie = cookies[i];
            if (!(cookie instanceof Cookie2)) {
                hasOldStyleCookie = true;
                break;
            }
            if (cookie.getVersion() > version) {
                version = cookie.getVersion();
            }
        }
        if (version < 0) {
            version = 0;
        }
        if (hasOldStyleCookie || version < 1) {
            // delegate old-style cookie formatting to rfc2109Spec
            return rfc2109.formatCookies(cookies);
        }
        // Arrange cookies by path
        Arrays.sort(cookies, PATH_COMPARATOR);

        final StringBuffer buffer = new StringBuffer();
        // format cookie version
        formatter.format(buffer, new NameValuePair("$Version", Integer.toString(version)));
        for (int i = 0; i < cookies.length; i++) {
            buffer.append("; ");
            final Cookie2 cookie = (Cookie2) cookies[i];
            // format cookie attributes
            doFormatCookie2(cookie, buffer);
        }
        return buffer.toString();
    }

    /**
     * Retrieves valid Port attribute value for the given ports array. e.g.
     * "8000,8001,8002"
     *
     * @param ports
     *        int array of ports
     */
    private String createPortAttribute(final int[] ports) {
        final StringBuffer portValue = new StringBuffer();
        for (int i = 0, len = ports.length; i < len; i++) {
            if (i > 0) {
                portValue.append(",");
            }
            portValue.append(ports[i]);
        }
        return portValue.toString();
    }

    /**
     * Parses the given Port attribute value (e.g. "8000,8001,8002") into an
     * array of ports.
     *
     * @param portValue
     *        port attribute value
     * @return parsed array of ports
     * @throws MalformedCookieException
     *         if there is a problem in parsing due to invalid portValue.
     */
    private int[] parsePortAttribute(final String portValue) throws MalformedCookieException {
        final StringTokenizer st = new StringTokenizer(portValue, ",");
        final int[] ports = new int[st.countTokens()];
        try {
            int i = 0;
            while (st.hasMoreTokens()) {
                ports[i] = Integer.parseInt(st.nextToken().trim());
                if (ports[i] < 0) {
                    throw new MalformedCookieException("Invalid Port attribute.");
                }
                ++i;
            }
        } catch (final NumberFormatException e) {
            throw new MalformedCookieException("Invalid Port " + "attribute: " + e.getMessage());
        }
        return ports;
    }

    /**
     * Gets 'effective host name' as defined in RFC 2965.
     * <p>
     * If a host name contains no dots, the effective host name is that name
     * with the string .local appended to it. Otherwise the effective host name
     * is the same as the host name. Note that all effective host names contain
     * at least one dot.
     *
     * @param host
     *        host name where cookie is received from or being sent to.
     * @return
     */
    private static String getEffectiveHost(final String host) {
        String effectiveHost = host.toLowerCase();
        if (host.indexOf('.') < 0) {
            effectiveHost += ".local";
        }
        return effectiveHost;
    }

    /**
     * Performs domain-match as defined by the RFC2965.
     * <p>
     * Host A's name domain-matches host B's if
     * <ol>
     * <ul>
     * their host name strings string-compare equal; or
     * </ul>
     * <ul>
     * A is a HDN string and has the form NB, where N is a non-empty name
     * string, B has the form .B', and B' is a HDN string. (So, x.y.com
     * domain-matches .Y.com but not Y.com.)
     * </ul>
     * </ol>
     *
     * @param host
     *        host name where cookie is received from or being sent to.
     * @param domain
     *        The cookie domain attribute.
     * @return true if the specified host matches the given domain.
     */
    @Override
    public boolean domainMatch(final String host, final String domain) {
        final boolean match = host.equals(domain) || (domain.startsWith(".") && host.endsWith(domain));

        return match;
    }

    /**
     * Returns <tt>true</tt> if the given port exists in the given ports list.
     *
     * @param port
     *        port of host where cookie was received from or being sent to.
     * @param ports
     *        port list
     * @return true returns <tt>true</tt> if the given port exists in the given
     *         ports list; <tt>false</tt> otherwise.
     */
    private boolean portMatch(final int port, final int[] ports) {
        boolean portInList = false;
        for (int i = 0, len = ports.length; i < len; i++) {
            if (port == ports[i]) {
                portInList = true;
                break;
            }
        }
        return portInList;
    }

    /**
     * <tt>"Path"</tt> attribute handler for RFC 2965 cookie spec.
     */
    private class Cookie2PathAttributeHandler implements CookieAttributeHandler {

        /**
         * Parse cookie path attribute.
         */
        @Override
        public void parse(final Cookie cookie, final String path) throws MalformedCookieException {
            if (cookie == null) {
                throw new IllegalArgumentException("Cookie may not be null");
            }
            if (path == null) {
                throw new MalformedCookieException("Missing value for path attribute");
            }
            if (path.trim().equals("")) {
                throw new MalformedCookieException("Blank value for path attribute");
            }
            cookie.setPath(path);
            cookie.setPathAttributeSpecified(true);
        }

        /**
         * Validate cookie path attribute. The value for the Path attribute must
         * be a prefix of the request-URI (case-sensitive matching).
         */
        @Override
        public void validate(final Cookie cookie, final CookieOrigin origin) throws MalformedCookieException {
            if (cookie == null) {
                throw new IllegalArgumentException("Cookie may not be null");
            }
            if (origin == null) {
                throw new IllegalArgumentException("Cookie origin may not be null");
            }
            String path = origin.getPath();
            if (path == null) {
                throw new IllegalArgumentException("Path of origin host may not be null.");
            }
            if (cookie.getPath() == null) {
                throw new MalformedCookieException("Invalid cookie state: " + "path attribute is null.");
            }
            if (path.trim().equals("")) {
                path = PATH_DELIM;
            }

            if (!pathMatch(path, cookie.getPath())) {
                throw new MalformedCookieException(
                    "Illegal path attribute \"" + cookie.getPath() + "\". Path of origin: \"" + path + "\"");
            }
        }

        /**
         * Match cookie path attribute. The value for the Path attribute must be
         * a prefix of the request-URI (case-sensitive matching).
         */
        @Override
        public boolean match(final Cookie cookie, final CookieOrigin origin) {
            if (cookie == null) {
                throw new IllegalArgumentException("Cookie may not be null");
            }
            if (origin == null) {
                throw new IllegalArgumentException("Cookie origin may not be null");
            }
            String path = origin.getPath();
            if (cookie.getPath() == null) {
                LOG.warn("Invalid cookie state: path attribute is null.");
                return false;
            }
            if (path.trim().equals("")) {
                path = PATH_DELIM;
            }

            if (!pathMatch(path, cookie.getPath())) {
                return false;
            }
            return true;
        }
    }

    /**
     * <tt>"Domain"</tt> cookie attribute handler for RFC 2965 cookie spec.
     */
    private class Cookie2DomainAttributeHandler implements CookieAttributeHandler {

        /**
         * Parse cookie domain attribute.
         */
        @Override
        public void parse(final Cookie cookie, String domain) throws MalformedCookieException {
            if (cookie == null) {
                throw new IllegalArgumentException("Cookie may not be null");
            }
            if (domain == null) {
                throw new MalformedCookieException("Missing value for domain attribute");
            }
            if (domain.trim().equals("")) {
                throw new MalformedCookieException("Blank value for domain attribute");
            }
            domain = domain.toLowerCase();
            if (!domain.startsWith(".")) {
                // Per RFC 2965 section 3.2.2
                // "... If an explicitly specified value does not start with
                // a dot, the user agent supplies a leading dot ..."
                // That effectively implies that the domain attribute
                // MAY NOT be an IP address of a host name
                domain = "." + domain;
            }
            cookie.setDomain(domain);
            cookie.setDomainAttributeSpecified(true);
        }

        /**
         * Validate cookie domain attribute.
         */
        @Override
        public void validate(final Cookie cookie, final CookieOrigin origin) throws MalformedCookieException {
            if (cookie == null) {
                throw new IllegalArgumentException("Cookie may not be null");
            }
            if (origin == null) {
                throw new IllegalArgumentException("Cookie origin may not be null");
            }
            final String host = origin.getHost().toLowerCase();
            if (cookie.getDomain() == null) {
                throw new MalformedCookieException("Invalid cookie state: " + "domain not specified");
            }
            final String cookieDomain = cookie.getDomain().toLowerCase();

            if (cookie.isDomainAttributeSpecified()) {
                // Domain attribute must start with a dot
                if (!cookieDomain.startsWith(".")) {
                    throw new MalformedCookieException(
                        "Domain attribute \""
                            + cookie.getDomain()
                            + "\" violates RFC 2109: domain must start with a dot");
                }

                // Domain attribute must contain atleast one embedded dot,
                // or the value must be equal to .local.
                final int dotIndex = cookieDomain.indexOf('.', 1);
                if (((dotIndex < 0) || (dotIndex == cookieDomain.length() - 1)) && (!cookieDomain.equals(".local"))) {
                    throw new MalformedCookieException(
                        "Domain attribute \""
                            + cookie.getDomain()
                            + "\" violates RFC 2965: the value contains no embedded dots "
                            + "and the value is not .local");
                }

                // The effective host name must domain-match domain attribute.
                if (!domainMatch(host, cookieDomain)) {
                    throw new MalformedCookieException("Domain attribute \""
                        + cookie.getDomain()
                        + "\" violates RFC 2965: effective host name does not "
                        + "domain-match domain attribute.");
                }

                // effective host name minus domain must not contain any dots
                final String effectiveHostWithoutDomain = host.substring(0, host.length() - cookieDomain.length());
                if (effectiveHostWithoutDomain.indexOf('.') != -1) {
                    throw new MalformedCookieException(
                        "Domain attribute \""
                            + cookie.getDomain()
                            + "\" violates RFC 2965: "
                            + "effective host minus domain may not contain any dots");
                }
            } else {
                // Domain was not specified in header. In this case, domain must
                // string match request host (case-insensitive).
                if (!cookie.getDomain().equals(host)) {
                    throw new MalformedCookieException(
                        "Illegal domain attribute: \""
                            + cookie.getDomain()
                            + "\"."
                            + "Domain of origin: \""
                            + host
                            + "\"");
                }
            }
        }

        /**
         * Match cookie domain attribute.
         */
        @Override
        public boolean match(final Cookie cookie, final CookieOrigin origin) {
            if (cookie == null) {
                throw new IllegalArgumentException("Cookie may not be null");
            }
            if (origin == null) {
                throw new IllegalArgumentException("Cookie origin may not be null");
            }
            final String host = origin.getHost().toLowerCase();
            final String cookieDomain = cookie.getDomain();

            // The effective host name MUST domain-match the Domain
            // attribute of the cookie.
            if (!domainMatch(host, cookieDomain)) {
                return false;
            }
            // effective host name minus domain must not contain any dots
            final String effectiveHostWithoutDomain = host.substring(0, host.length() - cookieDomain.length());
            if (effectiveHostWithoutDomain.indexOf('.') != -1) {
                return false;
            }
            return true;
        }

    }

    /**
     * <tt>"Port"</tt> cookie attribute handler for RFC 2965 cookie spec.
     */
    private class Cookie2PortAttributeHandler implements CookieAttributeHandler {

        /**
         * Parse cookie port attribute.
         */
        @Override
        public void parse(final Cookie cookie, final String portValue) throws MalformedCookieException {
            if (cookie == null) {
                throw new IllegalArgumentException("Cookie may not be null");
            }
            if (cookie instanceof Cookie2) {
                final Cookie2 cookie2 = (Cookie2) cookie;
                if ((portValue == null) || (portValue.trim().equals(""))) {
                    // If the Port attribute is present but has no value, the
                    // cookie can only be sent to the request-port.
                    // Since the default port list contains only request-port,
                    // we don't
                    // need to do anything here.
                    cookie2.setPortAttributeBlank(true);
                } else {
                    final int[] ports = parsePortAttribute(portValue);
                    cookie2.setPorts(ports);
                }
                cookie2.setPortAttributeSpecified(true);
            }
        }

        /**
         * Validate cookie port attribute. If the Port attribute was specified
         * in header, the request port must be in cookie's port list.
         */
        @Override
        public void validate(final Cookie cookie, final CookieOrigin origin) throws MalformedCookieException {
            if (cookie == null) {
                throw new IllegalArgumentException("Cookie may not be null");
            }
            if (origin == null) {
                throw new IllegalArgumentException("Cookie origin may not be null");
            }
            if (cookie instanceof Cookie2) {
                final Cookie2 cookie2 = (Cookie2) cookie;
                final int port = origin.getPort();
                if (cookie2.isPortAttributeSpecified()) {
                    if (!portMatch(port, cookie2.getPorts())) {
                        throw new MalformedCookieException(
                            "Port attribute violates RFC 2965: " + "Request port not found in cookie's port list.");
                    }
                }
            }
        }

        /**
         * Match cookie port attribute. If the Port attribute is not specified
         * in header, the cookie can be sent to any port. Otherwise, the request
         * port must be in the cookie's port list.
         */
        @Override
        public boolean match(final Cookie cookie, final CookieOrigin origin) {
            if (cookie == null) {
                throw new IllegalArgumentException("Cookie may not be null");
            }
            if (origin == null) {
                throw new IllegalArgumentException("Cookie origin may not be null");
            }
            if (cookie instanceof Cookie2) {
                final Cookie2 cookie2 = (Cookie2) cookie;
                final int port = origin.getPort();
                if (cookie2.isPortAttributeSpecified()) {
                    if (cookie2.getPorts() == null) {
                        LOG.warn("Invalid cookie state: port not specified");
                        return false;
                    }
                    if (!portMatch(port, cookie2.getPorts())) {
                        return false;
                    }
                }
                return true;
            } else {
                return false;
            }
        }
    }

    /**
     * <tt>"Max-age"</tt> cookie attribute handler for RFC 2965 cookie spec.
     */
    private class Cookie2MaxageAttributeHandler implements CookieAttributeHandler {

        /**
         * Parse cookie max-age attribute.
         */
        @Override
        public void parse(final Cookie cookie, final String value) throws MalformedCookieException {
            if (cookie == null) {
                throw new IllegalArgumentException("Cookie may not be null");
            }
            if (value == null) {
                throw new MalformedCookieException("Missing value for max-age attribute");
            }
            int age = -1;
            try {
                age = Integer.parseInt(value);
            } catch (final NumberFormatException e) {
                age = -1;
            }
            if (age < 0) {
                throw new MalformedCookieException("Invalid max-age attribute.");
            }
            cookie.setExpiryDate(new Date(System.currentTimeMillis() + age * 1000L));
        }

        /**
         * validate cookie max-age attribute.
         */
        @Override
        public void validate(final Cookie cookie, final CookieOrigin origin) {
        }

        /**
         * @see CookieAttributeHandler#match(com.microsoft.tfs.core.httpclient.Cookie,
         *      String)
         */
        @Override
        public boolean match(final Cookie cookie, final CookieOrigin origin) {
            return true;
        }

    }

    /**
     * <tt>"Secure"</tt> cookie attribute handler for RFC 2965 cookie spec.
     */
    private class CookieSecureAttributeHandler implements CookieAttributeHandler {

        @Override
        public void parse(final Cookie cookie, final String secure) throws MalformedCookieException {
            cookie.setSecure(true);
        }

        @Override
        public void validate(final Cookie cookie, final CookieOrigin origin) throws MalformedCookieException {
        }

        @Override
        public boolean match(final Cookie cookie, final CookieOrigin origin) {
            if (cookie == null) {
                throw new IllegalArgumentException("Cookie may not be null");
            }
            if (origin == null) {
                throw new IllegalArgumentException("Cookie origin may not be null");
            }
            return cookie.getSecure() == origin.isSecure();
        }

    }

    /**
     * <tt>"Commant"</tt> cookie attribute handler for RFC 2965 cookie spec.
     */
    private class CookieCommentAttributeHandler implements CookieAttributeHandler {

        @Override
        public void parse(final Cookie cookie, final String comment) throws MalformedCookieException {
            cookie.setComment(comment);
        }

        @Override
        public void validate(final Cookie cookie, final CookieOrigin origin) throws MalformedCookieException {
        }

        @Override
        public boolean match(final Cookie cookie, final CookieOrigin origin) {
            return true;
        }

    }

    /**
     * <tt>"CommantURL"</tt> cookie attribute handler for RFC 2965 cookie spec.
     */
    private class CookieCommentUrlAttributeHandler implements CookieAttributeHandler {

        @Override
        public void parse(final Cookie cookie, final String commenturl) throws MalformedCookieException {
            if (cookie instanceof Cookie2) {
                final Cookie2 cookie2 = (Cookie2) cookie;
                cookie2.setCommentURL(commenturl);
            }
        }

        @Override
        public void validate(final Cookie cookie, final CookieOrigin origin) throws MalformedCookieException {
        }

        @Override
        public boolean match(final Cookie cookie, final CookieOrigin origin) {
            return true;
        }

    }

    /**
     * <tt>"Discard"</tt> cookie attribute handler for RFC 2965 cookie spec.
     */
    private class CookieDiscardAttributeHandler implements CookieAttributeHandler {

        @Override
        public void parse(final Cookie cookie, final String commenturl) throws MalformedCookieException {
            if (cookie instanceof Cookie2) {
                final Cookie2 cookie2 = (Cookie2) cookie;
                cookie2.setDiscard(true);
            }
        }

        @Override
        public void validate(final Cookie cookie, final CookieOrigin origin) throws MalformedCookieException {
        }

        @Override
        public boolean match(final Cookie cookie, final CookieOrigin origin) {
            return true;
        }

    }

    /**
     * <tt>"Version"</tt> cookie attribute handler for RFC 2965 cookie spec.
     */
    private class Cookie2VersionAttributeHandler implements CookieAttributeHandler {

        /**
         * Parse cookie version attribute.
         */
        @Override
        public void parse(final Cookie cookie, final String value) throws MalformedCookieException {
            if (cookie == null) {
                throw new IllegalArgumentException("Cookie may not be null");
            }
            if (cookie instanceof Cookie2) {
                final Cookie2 cookie2 = (Cookie2) cookie;
                if (value == null) {
                    throw new MalformedCookieException("Missing value for version attribute");
                }
                int version = -1;
                try {
                    version = Integer.parseInt(value);
                } catch (final NumberFormatException e) {
                    version = -1;
                }
                if (version < 0) {
                    throw new MalformedCookieException("Invalid cookie version.");
                }
                cookie2.setVersion(version);
                cookie2.setVersionAttributeSpecified(true);
            }
        }

        /**
         * validate cookie version attribute. Version attribute is REQUIRED.
         */
        @Override
        public void validate(final Cookie cookie, final CookieOrigin origin) throws MalformedCookieException {
            if (cookie == null) {
                throw new IllegalArgumentException("Cookie may not be null");
            }
            if (cookie instanceof Cookie2) {
                final Cookie2 cookie2 = (Cookie2) cookie;
                if (!cookie2.isVersionAttributeSpecified()) {
                    throw new MalformedCookieException("Violates RFC 2965. Version attribute is required.");
                }
            }
        }

        @Override
        public boolean match(final Cookie cookie, final CookieOrigin origin) {
            return true;
        }

    }

    @Override
    public int getVersion() {
        return 1;
    }

    @Override
    public Header getVersionHeader() {
        final ParameterFormatter formatter = new ParameterFormatter();
        final StringBuffer buffer = new StringBuffer();
        formatter.format(buffer, new NameValuePair("$Version", Integer.toString(getVersion())));
        return new Header("Cookie2", buffer.toString(), true);
    }

}
