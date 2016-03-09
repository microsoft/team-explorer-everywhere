/*
 * $Header:
 * /home/jerenkrantz/tmp/commons/commons-convert/cvs/home/cvs/jakarta-commons
 * //httpclient/src/java/org/apache/commons/httpclient/StatusLine.java,v 1.14
 * 2004/07/19 20:24:21 olegk Exp $ $Revision: 480424 $ $Date: 2006-11-29
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

/**
 * Represents a Status-Line as returned from a HTTP server.
 *
 * <a href="http://www.ietf.org/rfc/rfc2616.txt">RFC2616</a> states the
 * following regarding the Status-Line:
 *
 * <pre>
 * 6.1 Status-Line
 *
 *  The first line of a Response message is the Status-Line, consisting
 *  of the protocol version followed by a numeric status code and its
 *  associated textual phrase, with each element separated by SP
 *  characters. No CR or LF is allowed except in the final CRLF sequence.
 *
 *      Status-Line = HTTP-Version SP Status-Code SP Reason-Phrase CRLF
 * </pre>
 * <p>
 * This class is immutable and is inherently thread safe.
 *
 * @see HttpStatus
 * @author <a href="mailto:jsdever@apache.org">Jeff Dever</a>
 * @author <a href="mailto:mbowler@GargoyleSoftware.com">Mike Bowler</a>
 * @version $Id: StatusLine.java 480424 2006-11-29 05:56:49Z bayard $
 * @since 2.0
 */
public class StatusLine {

    // ----------------------------------------------------- Instance Variables

    /** The original Status-Line. */
    private final String statusLine;

    /** The HTTP-Version. */
    private final String httpVersion;

    /** The Status-Code. */
    private final int statusCode;

    /** The Reason-Phrase. */
    private final String reasonPhrase;

    // ----------------------------------------------------------- Constructors

    /**
     * Default constructor.
     *
     * @param statusLine
     *        the status line returned from the HTTP server
     * @throws HttpException
     *         if the status line is invalid
     */
    public StatusLine(final String statusLine) throws HttpException {

        final int length = statusLine.length();
        int at = 0;
        int start = 0;
        try {
            while (Character.isWhitespace(statusLine.charAt(at))) {
                ++at;
                ++start;
            }
            if (!"HTTP".equals(statusLine.substring(at, at += 4))) {
                throw new HttpException("Status-Line '" + statusLine + "' does not start with HTTP");
            }
            // handle the HTTP-Version
            at = statusLine.indexOf(" ", at);
            if (at <= 0) {
                throw new ProtocolException("Unable to parse HTTP-Version from the status line: '" + statusLine + "'");
            }
            httpVersion = (statusLine.substring(start, at)).toUpperCase();

            // advance through spaces
            while (statusLine.charAt(at) == ' ') {
                at++;
            }

            // handle the Status-Code
            int to = statusLine.indexOf(" ", at);
            if (to < 0) {
                to = length;
            }
            try {
                int tmpCode = Integer.parseInt(statusLine.substring(at, to));
                if (tmpCode == HttpStatus.SC_NON_AUTHORITATIVE_INFORMATION) {
                    tmpCode = HttpStatus.SC_OK;
                }

                statusCode = tmpCode;
            } catch (final NumberFormatException e) {
                throw new ProtocolException("Unable to parse status code from status line: '" + statusLine + "'");
            }
            // handle the Reason-Phrase
            at = to + 1;
            if (at < length) {
                reasonPhrase = statusLine.substring(at).trim();
            } else {
                reasonPhrase = "";
            }
        } catch (final StringIndexOutOfBoundsException e) {
            throw new HttpException("Status-Line '" + statusLine + "' is not valid");
        }
        // save the original Status-Line
        this.statusLine = statusLine;
    }

    // --------------------------------------------------------- Public Methods

    /**
     * @return the Status-Code
     */
    public final int getStatusCode() {
        return statusCode;
    }

    /**
     * @return the HTTP-Version
     */
    public final String getHttpVersion() {
        return httpVersion;
    }

    /**
     * @return the Reason-Phrase
     */
    public final String getReasonPhrase() {
        return reasonPhrase;
    }

    /**
     * Return a string representation of this object.
     *
     * @return a string represenation of this object.
     */
    @Override
    public final String toString() {
        return statusLine;
    }

    /**
     * Tests if the string starts with 'HTTP' signature.
     *
     * @param s
     *        string to test
     * @return <tt>true</tt> if the line starts with 'HTTP' signature,
     *         <tt>false</tt> otherwise.
     */
    public static boolean startsWithHTTP(final String s) {
        try {
            int at = 0;
            while (Character.isWhitespace(s.charAt(at))) {
                ++at;
            }
            return ("HTTP".equals(s.substring(at, at + 4)));
        } catch (final StringIndexOutOfBoundsException e) {
            return false;
        }
    }
}
