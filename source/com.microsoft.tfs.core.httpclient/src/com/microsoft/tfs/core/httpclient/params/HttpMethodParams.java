/*
 * $Header:
 * /home/jerenkrantz/tmp/commons/commons-convert/cvs/home/cvs/jakarta-commons
 * //httpclient
 * /src/java/org/apache/commons/httpclient/params/HttpMethodParams.java,v 1.17
 * 2004/10/06 17:32:04 olegk Exp $ $Revision: 483949 $ $Date: 2006-12-08
 * 12:34:50 +0100 (Fri, 08 Dec 2006) $
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

package com.microsoft.tfs.core.httpclient.params;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.microsoft.tfs.core.httpclient.HttpVersion;
import com.microsoft.tfs.core.httpclient.cookie.CookiePolicy;

/**
 * This class represents a collection of HTTP protocol parameters applicable to
 * {@link com.microsoft.tfs.core.httpclient.HttpMethod HTTP methods}. Protocol
 * parameters may be linked together to form a hierarchy. If a particular
 * parameter value has not been explicitly defined in the collection itself, its
 * value will be drawn from the parent collection of parameters.
 *
 * @author <a href="mailto:oleg@ural.ru">Oleg Kalnichevski</a>
 * @author Christian Kohlschuetter
 *
 * @version $Revision: 483949 $
 *
 * @since 3.0
 */
public class HttpMethodParams extends DefaultHttpParams {

    /** Log object for this class. */
    private static final Log LOG = LogFactory.getLog(HttpMethodParams.class);

    /**
     * Defines the content of the <tt>User-Agent</tt> header used by
     * {@link com.microsoft.tfs.core.httpclient.HttpMethod HTTP methods}.
     * <p>
     * This parameter expects a value of type {@link String}.
     * </p>
     */
    public static final String USER_AGENT = "http.useragent";

    /**
     * Defines the {@link HttpVersion HTTP protocol version} used by
     * {@link com.microsoft.tfs.core.httpclient.HttpMethod HTTP methods} per
     * default.
     * <p>
     * This parameter expects a value of type {@link HttpVersion}.
     * </p>
     */
    public static final String PROTOCOL_VERSION = "http.protocol.version";

    /**
     * Defines whether {@link com.microsoft.tfs.core.httpclient.HttpMethod HTTP
     * methods} should reject ambiguous
     * {@link com.microsoft.tfs.core.httpclient.StatusLine HTTP status line}.
     * <p>
     * This parameter expects a value of type {@link Boolean}.
     * </p>
     */
    public static final String UNAMBIGUOUS_STATUS_LINE = "http.protocol.unambiguous-statusline";

    /**
     * Defines whether {@link com.microsoft.tfs.core.httpclient.Cookie cookies}
     * should be put on a single {@link com.microsoft.tfs.core.httpclient.Header
     * response header}.
     * <p>
     * This parameter expects a value of type {@link Boolean}.
     * </p>
     */
    public static final String SINGLE_COOKIE_HEADER = "http.protocol.single-cookie-header";

    /**
     * Defines whether responses with an invalid <tt>Transfer-Encoding</tt>
     * header should be rejected.
     * <p>
     * This parameter expects a value of type {@link Boolean}.
     * </p>
     */
    public static final String STRICT_TRANSFER_ENCODING = "http.protocol.strict-transfer-encoding";

    /**
     * Defines whether the content body sent in response to
     * {@link com.microsoft.tfs.core.httpclient.methods.HeadMethod} should be
     * rejected.
     * <p>
     * This parameter expects a value of type {@link Boolean}.
     * </p>
     */
    public static final String REJECT_HEAD_BODY = "http.protocol.reject-head-body";

    /**
     * Sets period of time in milliseconds to wait for a content body sent in
     * response to {@link com.microsoft.tfs.core.httpclient.methods.HeadMethod
     * HEAD method} from a non-compliant server. If the parameter is not set or
     * set to <tt>-1</tt> non-compliant response body check is disabled.
     * <p>
     * This parameter expects a value of type {@link Integer}.
     * </p>
     */
    public static final String HEAD_BODY_CHECK_TIMEOUT = "http.protocol.head-body-timeout";

    /**
     * <p>
     * Activates 'Expect: 100-Continue' handshake for the
     * {@link com.microsoft.tfs.core.httpclient.methods.ExpectContinueMethod
     * entity enclosing methods}. The purpose of the 'Expect: 100-Continue'
     * handshake to allow a client that is sending a request message with a
     * request body to determine if the origin server is willing to accept the
     * request (based on the request headers) before the client sends the
     * request body.
     * </p>
     *
     * <p>
     * The use of the 'Expect: 100-continue' handshake can result in noticable
     * peformance improvement for entity enclosing requests (such as POST and
     * PUT) that require the target server's authentication.
     * </p>
     *
     * <p>
     * 'Expect: 100-continue' handshake should be used with caution, as it may
     * cause problems with HTTP servers and proxies that do not support HTTP/1.1
     * protocol.
     * </p>
     *
     * This parameter expects a value of type {@link Boolean}.
     */
    public static final String USE_EXPECT_CONTINUE = "http.protocol.expect-continue";

    /**
     * Defines the charset to be used when encoding
     * {@link com.microsoft.tfs.core.httpclient.Credentials}. If not defined
     * then the {@link #HTTP_ELEMENT_CHARSET} should be used.
     * <p>
     * This parameter expects a value of type {@link String}.
     * </p>
     */
    public static final String CREDENTIAL_CHARSET = "http.protocol.credential-charset";

    /**
     * Defines the charset to be used for encoding HTTP protocol elements.
     * <p>
     * This parameter expects a value of type {@link String}.
     * </p>
     */
    public static final String HTTP_ELEMENT_CHARSET = "http.protocol.element-charset";

    /**
     * Defines the charset to be used for parsing URIs.
     * <p>
     * This parameter expects a value of type {@link String}.
     * </p>
     */
    public static final String HTTP_URI_CHARSET = "http.protocol.uri-charset";

    /**
     * Defines the charset to be used for encoding content body.
     * <p>
     * This parameter expects a value of type {@link String}.
     * </p>
     */
    public static final String HTTP_CONTENT_CHARSET = "http.protocol.content-charset";

    /**
     * Defines {@link CookiePolicy cookie policy} to be used for cookie
     * management.
     * <p>
     * This parameter expects a value of type {@link String}.
     * </p>
     */
    public static final String COOKIE_POLICY = "http.protocol.cookie-policy";

    /**
     * Defines HttpClient's behavior when a response provides more bytes than
     * expected (specified with Content-Length, for example).
     * <p>
     * Such surplus data makes the HTTP connection unreliable for keep-alive
     * requests, as malicious response data (faked headers etc.) can lead to
     * undesired results on the next request using that connection.
     * </p>
     * <p>
     * If this parameter is set to <code>true</code>, any detection of extra
     * input data will generate a warning in the log.
     * </p>
     * <p>
     * This parameter expects a value of type {@link Boolean}.
     * </p>
     */
    public static final String WARN_EXTRA_INPUT = "http.protocol.warn-extra-input";

    /**
     * Defines the maximum number of ignorable lines before we expect a HTTP
     * response's status code.
     * <p>
     * With HTTP/1.1 persistent connections, the problem arises that broken
     * scripts could return a wrong Content-Length (there are more bytes sent
     * than specified).<br />
     * Unfortunately, in some cases, this is not possible after the bad
     * response, but only before the next one. <br />
     * So, HttpClient must be able to skip those surplus lines this way.
     * </p>
     * <p>
     * Set this to 0 to disallow any garbage/empty lines before the status line.
     * <br />
     * To specify no limit, use {@link java.lang.Integer#MAX_VALUE} (default in
     * lenient mode).
     * </p>
     *
     * This parameter expects a value of type {@link Integer}.
     */
    public static final String STATUS_LINE_GARBAGE_LIMIT = "http.protocol.status-line-garbage-limit";

    /**
     * Sets the socket timeout (<tt>SO_TIMEOUT</tt>) in milliseconds to be used
     * when executing the method. A timeout value of zero is interpreted as an
     * infinite timeout.
     * <p>
     * This parameter expects a value of type {@link Integer}.
     * </p>
     *
     * @see java.net.SocketOptions#SO_TIMEOUT
     */
    public static final String SO_TIMEOUT = "http.socket.timeout";

    /**
     * The key used to look up the date patterns used for parsing. The String
     * patterns are stored in a {@link java.util.Collection} and must be
     * compatible with {@link java.text.SimpleDateFormat}.
     * <p>
     * This parameter expects a value of type {@link java.util.Collection}.
     * </p>
     */
    public static final String DATE_PATTERNS = "http.dateparser.patterns";

    /**
     * Sets the method retry handler parameter.
     * <p>
     * This parameter expects a value of type
     * {@link com.microsoft.tfs.core.httpclient.HttpMethodRetryHandler}.
     * </p>
     */
    public static final String RETRY_HANDLER = "http.method.retry-handler";

    /**
     * Sets the maximum buffered response size (in bytes) that triggers no
     * warning. Buffered responses exceeding this size will trigger a warning in
     * the log.
     * <p>
     * This parameter expects a value if type {@link Integer}.
     * </p>
     */
    public static final String BUFFER_WARN_TRIGGER_LIMIT = "http.method.response.buffer.warnlimit";

    /**
     * Defines the virtual host name.
     * <p>
     * This parameter expects a value of type {@link java.lang.String}.
     * </p>
     */
    public static final String VIRTUAL_HOST = "http.virtual-host";

    /**
     * Sets the value to use as the multipart boundary.
     * <p>
     * This parameter expects a value if type {@link String}.
     * </p>
     *
     * @see com.microsoft.tfs.core.httpclient.methods.multipart.
     *      MultipartRequestEntity
     */
    public static final String MULTIPART_BOUNDARY = "http.method.multipart.boundary";

    /**
     * Creates a new collection of parameters with the collection returned by
     * {@link #getDefaultParams()} as a parent. The collection will defer to its
     * parent for a default value if a particular parameter is not explicitly
     * set in the collection itself.
     *
     * @see #getDefaultParams()
     */
    public HttpMethodParams() {
        super(getDefaultParams());
    }

    /**
     * Creates a new collection of parameters with the given parent. The
     * collection will defer to its parent for a default value if a particular
     * parameter is not explicitly set in the collection itself.
     *
     * @param defaults
     *        the parent collection to defer to, if a parameter is not explictly
     *        set in the collection itself.
     *
     * @see #getDefaultParams()
     */
    public HttpMethodParams(final HttpParams defaults) {
        super(defaults);
    }

    /**
     * Returns the charset to be used for writing HTTP headers.
     *
     * @return The charset
     */
    public String getHttpElementCharset() {
        String charset = (String) getParameter(HTTP_ELEMENT_CHARSET);
        if (charset == null) {
            LOG.warn("HTTP element charset not configured, using US-ASCII");
            charset = "US-ASCII";
        }
        return charset;
    }

    /**
     * Sets the charset to be used for writing HTTP headers.
     *
     * @param charset
     *        The charset
     */
    public void setHttpElementCharset(final String charset) {
        setParameter(HTTP_ELEMENT_CHARSET, charset);
    }

    /**
     * Returns the default charset to be used for writing content body, when no
     * charset explicitly specified.
     *
     * @return The charset
     */
    public String getContentCharset() {
        String charset = (String) getParameter(HTTP_CONTENT_CHARSET);
        if (charset == null) {
            LOG.warn("Default content charset not configured, using ISO-8859-1");
            charset = "ISO-8859-1";
        }
        return charset;
    }

    /**
     * Sets the charset to be used for parsing URIs.
     *
     * @param charset
     *        The charset
     */
    public void setUriCharset(final String charset) {
        setParameter(HTTP_URI_CHARSET, charset);
    }

    /**
     * Returns the charset to be used for parsing URIs.
     *
     * @return The charset
     */
    public String getUriCharset() {
        String charset = (String) getParameter(HTTP_URI_CHARSET);
        if (charset == null) {
            charset = "UTF-8";
        }
        return charset;
    }

    /**
     * Sets the default charset to be used for writing content body, when no
     * charset explicitly specified.
     *
     * @param charset
     *        The charset
     */
    public void setContentCharset(final String charset) {
        setParameter(HTTP_CONTENT_CHARSET, charset);
    }

    /**
     * Returns the charset to be used for
     * {@link com.microsoft.tfs.core.httpclient.Credentials}. If not configured
     * the {@link #HTTP_ELEMENT_CHARSET HTTP element charset} is used.
     *
     * @return The charset
     */
    public String getCredentialCharset() {
        String charset = (String) getParameter(CREDENTIAL_CHARSET);
        if (charset == null) {
            LOG.debug("Credential charset not configured, using HTTP element charset");
            charset = getHttpElementCharset();
        }
        return charset;
    }

    /**
     * Sets the charset to be used for writing HTTP headers.
     *
     * @param charset
     *        The charset
     */
    public void setCredentialCharset(final String charset) {
        setParameter(CREDENTIAL_CHARSET, charset);
    }

    /**
     * Returns {@link HttpVersion HTTP protocol version} to be used by the
     * {@link com.microsoft.tfs.core.httpclient.HttpMethod HTTP methods} that
     * this collection of parameters applies to.
     *
     * @return {@link HttpVersion HTTP protocol version}
     */
    public HttpVersion getVersion() {
        final Object param = getParameter(PROTOCOL_VERSION);
        if (param == null) {
            return HttpVersion.HTTP_1_1;
        }
        return (HttpVersion) param;
    }

    /**
     * Assigns the {@link HttpVersion HTTP protocol version} to be used by the
     * {@link com.microsoft.tfs.core.httpclient.HttpMethod HTTP methods} that
     * this collection of parameters applies to.
     *
     * @param version
     *        the {@link HttpVersion HTTP protocol version}
     */
    public void setVersion(final HttpVersion version) {
        setParameter(PROTOCOL_VERSION, version);
    }

    /**
     * Returns {@link CookiePolicy cookie policy} to be used by the
     * {@link com.microsoft.tfs.core.httpclient.HttpMethod HTTP methods} this
     * collection of parameters applies to.
     *
     * @return {@link CookiePolicy cookie policy}
     */
    public String getCookiePolicy() {
        final Object param = getParameter(COOKIE_POLICY);
        if (param == null) {
            return CookiePolicy.DEFAULT;
        }
        return (String) param;
    }

    /**
     * Assigns the {@link CookiePolicy cookie policy} to be used by the
     * {@link com.microsoft.tfs.core.httpclient.HttpMethod HTTP methods} this
     * collection of parameters applies to.
     *
     * @param policy
     *        the {@link CookiePolicy cookie policy}
     */
    public void setCookiePolicy(final String policy) {
        setParameter(COOKIE_POLICY, policy);
    }

    /**
     * Returns the default socket timeout (<tt>SO_TIMEOUT</tt>) in milliseconds
     * which is the timeout for waiting for data. A timeout value of zero is
     * interpreted as an infinite timeout.
     *
     * @return timeout in milliseconds
     */
    public int getSoTimeout() {
        return getIntParameter(SO_TIMEOUT, 0);
    }

    /**
     * Sets the default socket timeout (<tt>SO_TIMEOUT</tt>) in milliseconds
     * which is the timeout for waiting for data. A timeout value of zero is
     * interpreted as an infinite timeout.
     *
     * @param timeout
     *        Timeout in milliseconds
     */
    public void setSoTimeout(final int timeout) {
        setIntParameter(SO_TIMEOUT, timeout);
    }

    /**
     * Sets the virtual host name.
     *
     * @param hostname
     *        The host name
     */
    public void setVirtualHost(final String hostname) {
        setParameter(VIRTUAL_HOST, hostname);
    }

    /**
     * Returns the virtual host name.
     *
     * @return The virtual host name
     */
    public String getVirtualHost() {
        return (String) getParameter(VIRTUAL_HOST);
    }

    private static final String[] PROTOCOL_STRICTNESS_PARAMETERS = {
        UNAMBIGUOUS_STATUS_LINE,
        SINGLE_COOKIE_HEADER,
        STRICT_TRANSFER_ENCODING,
        REJECT_HEAD_BODY,
        WARN_EXTRA_INPUT
    };

    /**
     * Makes the {@link com.microsoft.tfs.core.httpclient.HttpMethod HTTP
     * methods} strictly follow the HTTP protocol specification (RFC 2616 and
     * other relevant RFCs). It must be noted that popular HTTP agents have
     * different degree of HTTP protocol compliance and some HTTP serves are
     * programmed to expect the behaviour that does not strictly adhere to the
     * HTTP specification.
     */
    public void makeStrict() {
        setParameters(PROTOCOL_STRICTNESS_PARAMETERS, Boolean.TRUE);
        setIntParameter(STATUS_LINE_GARBAGE_LIMIT, 0);
    }

    /**
     * Makes the {@link com.microsoft.tfs.core.httpclient.HttpMethod HTTP
     * methods} attempt to mimic the exact behaviour of commonly used HTTP
     * agents, which many HTTP servers expect, even though such behaviour may
     * violate the HTTP protocol specification (RFC 2616 and other relevant
     * RFCs).
     */
    public void makeLenient() {
        setParameters(PROTOCOL_STRICTNESS_PARAMETERS, Boolean.FALSE);
        setIntParameter(STATUS_LINE_GARBAGE_LIMIT, Integer.MAX_VALUE);
    }

}
