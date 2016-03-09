/*
 * $Header:
 * /home/jerenkrantz/tmp/commons/commons-convert/cvs/home/cvs/jakarta-commons
 * //httpclient/src/java/org/apache/commons/httpclient/HttpConstants.java,v 1.15
 * 2004/04/18 23:51:35 jsdever Exp $ $Revision: 480424 $ $Date: 2006-11-29
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

import java.io.UnsupportedEncodingException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * HTTP content conversion routines.
 *
 * @author Oleg Kalnichevski
 * @author <a href="mailto:mbowler@GargoyleSoftware.com">Mike Bowler</a>
 *
 * @deprecated use EncodingUtil class
 */
@Deprecated
public class HttpConstants {

    /** Character set used to encode HTTP protocol elements */
    public static final String HTTP_ELEMENT_CHARSET = "US-ASCII";

    /** Default content encoding chatset */
    public static final String DEFAULT_CONTENT_CHARSET = "ISO-8859-1";

    /** Log object for this class. */
    private static final Log LOG = LogFactory.getLog(HttpConstants.class);

    /**
     * Converts the specified string to a byte array of HTTP element characters.
     * This method is to be used when encoding content of HTTP elements (such as
     * request headers)
     *
     * @param data
     *        the string to be encoded
     * @return The resulting byte array.
     */
    public static byte[] getBytes(final String data) {
        if (data == null) {
            throw new IllegalArgumentException("Parameter may not be null");
        }

        try {
            return data.getBytes(HTTP_ELEMENT_CHARSET);
        } catch (final UnsupportedEncodingException e) {

            if (LOG.isWarnEnabled()) {
                LOG.warn("Unsupported encoding: " + HTTP_ELEMENT_CHARSET + ". System default encoding used");
            }

            return data.getBytes();
        }
    }

    /**
     * Converts the byte array of HTTP element characters to a string This
     * method is to be used when decoding content of HTTP elements (such as
     * response headers)
     *
     * @param data
     *        the byte array to be encoded
     * @param offset
     *        the index of the first byte to encode
     * @param length
     *        the number of bytes to encode
     * @return The resulting string.
     */
    public static String getString(final byte[] data, final int offset, final int length) {

        if (data == null) {
            throw new IllegalArgumentException("Parameter may not be null");
        }

        try {
            return new String(data, offset, length, HTTP_ELEMENT_CHARSET);
        } catch (final UnsupportedEncodingException e) {

            if (LOG.isWarnEnabled()) {
                LOG.warn("Unsupported encoding: " + HTTP_ELEMENT_CHARSET + ". System default encoding used");
            }

            return new String(data, offset, length);
        }
    }

    /**
     * Converts the byte array of HTTP element characters to a string This
     * method is to be used when decoding content of HTTP elements (such as
     * response headers)
     *
     * @param data
     *        the byte array to be encoded
     * @return The resulting string.
     */
    public static String getString(final byte[] data) {
        return getString(data, 0, data.length);
    }

    /**
     * Converts the specified string to a byte array of HTTP content charachetrs
     * This method is to be used when encoding content of HTTP request/response
     * If the specified charset is not supported, default HTTP content encoding
     * (ISO-8859-1) is applied
     *
     * @param data
     *        the string to be encoded
     * @param charset
     *        the desired character encoding
     * @return The resulting byte array.
     */
    public static byte[] getContentBytes(final String data, String charset) {

        if (data == null) {
            throw new IllegalArgumentException("Parameter may not be null");
        }

        if ((charset == null) || (charset.equals(""))) {
            charset = DEFAULT_CONTENT_CHARSET;
        }

        try {
            return data.getBytes(charset);
        } catch (final UnsupportedEncodingException e) {

            if (LOG.isWarnEnabled()) {
                LOG.warn("Unsupported encoding: " + charset + ". HTTP default encoding used");
            }

            try {
                return data.getBytes(DEFAULT_CONTENT_CHARSET);
            } catch (final UnsupportedEncodingException e2) {

                if (LOG.isWarnEnabled()) {
                    LOG.warn("Unsupported encoding: " + DEFAULT_CONTENT_CHARSET + ". System encoding used");
                }

                return data.getBytes();
            }
        }
    }

    /**
     * Converts the byte array of HTTP content characters to a string This
     * method is to be used when decoding content of HTTP request/response If
     * the specified charset is not supported, default HTTP content encoding
     * (ISO-8859-1) is applied
     *
     * @param data
     *        the byte array to be encoded
     * @param offset
     *        the index of the first byte to encode
     * @param length
     *        the number of bytes to encode
     * @param charset
     *        the desired character encoding
     * @return The result of the conversion.
     */
    public static String getContentString(final byte[] data, final int offset, final int length, String charset) {

        if (data == null) {
            throw new IllegalArgumentException("Parameter may not be null");
        }

        if ((charset == null) || (charset.equals(""))) {
            charset = DEFAULT_CONTENT_CHARSET;
        }

        try {
            return new String(data, offset, length, charset);
        } catch (final UnsupportedEncodingException e) {

            if (LOG.isWarnEnabled()) {
                LOG.warn("Unsupported encoding: " + charset + ". Default HTTP encoding used");
            }

            try {
                return new String(data, offset, length, DEFAULT_CONTENT_CHARSET);
            } catch (final UnsupportedEncodingException e2) {

                if (LOG.isWarnEnabled()) {
                    LOG.warn("Unsupported encoding: " + DEFAULT_CONTENT_CHARSET + ". System encoding used");
                }

                return new String(data, offset, length);
            }
        }
    }

    /**
     * Converts the byte array of HTTP content characters to a string This
     * method is to be used when decoding content of HTTP request/response If
     * the specified charset is not supported, default HTTP content encoding
     * (ISO-8859-1) is applied
     *
     * @param data
     *        the byte array to be encoded
     * @param charset
     *        the desired character encoding
     * @return The result of the conversion.
     */
    public static String getContentString(final byte[] data, final String charset) {
        return getContentString(data, 0, data.length, charset);
    }

    /**
     * Converts the specified string to a byte array of HTTP content characters
     * using default HTTP content encoding (ISO-8859-1) This method is to be
     * used when encoding content of HTTP request/response
     *
     * @param data
     *        the string to be encoded
     * @return The byte array as above.
     */
    public static byte[] getContentBytes(final String data) {
        return getContentBytes(data, null);
    }

    /**
     * Converts the byte array of HTTP content characters to a string using
     * default HTTP content encoding (ISO-8859-1) This method is to be used when
     * decoding content of HTTP request/response
     *
     * @param data
     *        the byte array to be encoded
     * @param offset
     *        the index of the first byte to encode
     * @param length
     *        the number of bytes to encode
     * @return The string representation of the byte array.
     */
    public static String getContentString(final byte[] data, final int offset, final int length) {
        return getContentString(data, offset, length, null);
    }

    /**
     * Converts the byte array of HTTP content characters to a string using
     * default HTTP content encoding (ISO-8859-1) This method is to be used when
     * decoding content of HTTP request/response
     *
     * @param data
     *        the byte array to be encoded
     * @return The string representation of the byte array.
     */
    public static String getContentString(final byte[] data) {
        return getContentString(data, null);
    }

    /**
     * Converts the specified string to byte array of ASCII characters.
     *
     * @param data
     *        the string to be encoded
     * @return The string as a byte array.
     */
    public static byte[] getAsciiBytes(final String data) {

        if (data == null) {
            throw new IllegalArgumentException("Parameter may not be null");
        }

        try {
            return data.getBytes("US-ASCII");
        } catch (final UnsupportedEncodingException e) {
            throw new RuntimeException("HttpClient requires ASCII support");
        }
    }

    /**
     * Converts the byte array of ASCII characters to a string. This method is
     * to be used when decoding content of HTTP elements (such as response
     * headers)
     *
     * @param data
     *        the byte array to be encoded
     * @param offset
     *        the index of the first byte to encode
     * @param length
     *        the number of bytes to encode
     * @return The string representation of the byte array
     */
    public static String getAsciiString(final byte[] data, final int offset, final int length) {

        if (data == null) {
            throw new IllegalArgumentException("Parameter may not be null");
        }

        try {
            return new String(data, offset, length, "US-ASCII");
        } catch (final UnsupportedEncodingException e) {
            throw new RuntimeException("HttpClient requires ASCII support");
        }
    }

    /**
     * Converts the byte array of ASCII characters to a string. This method is
     * to be used when decoding content of HTTP elements (such as response
     * headers)
     *
     * @param data
     *        the byte array to be encoded
     * @return The string representation of the byte array
     */
    public static String getAsciiString(final byte[] data) {
        return getAsciiString(data, 0, data.length);
    }
}
