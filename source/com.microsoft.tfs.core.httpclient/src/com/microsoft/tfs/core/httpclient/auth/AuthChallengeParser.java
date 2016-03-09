/*
 * $Header:
 * /home/jerenkrantz/tmp/commons/commons-convert/cvs/home/cvs/jakarta-commons
 * //httpclient
 * /src/java/org/apache/commons/httpclient/auth/AuthChallengeParser.java,v 1.10
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

package com.microsoft.tfs.core.httpclient.auth;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.microsoft.tfs.core.httpclient.Header;
import com.microsoft.tfs.core.httpclient.NameValuePair;
import com.microsoft.tfs.core.httpclient.util.ParameterParser;

/**
 * This class provides utility methods for parsing HTTP www and proxy
 * authentication challenges.
 *
 * @author <a href="mailto:oleg@ural.ru">Oleg Kalnichevski</a>
 *
 * @since 2.0beta1
 */
public final class AuthChallengeParser {
    /**
     * Extracts authentication scheme from the given authentication challenge.
     *
     * @param challengeStr
     *        the authentication challenge string
     * @return authentication scheme
     *
     * @throws MalformedChallengeException
     *         when the authentication challenge string is malformed
     *
     * @since 2.0beta1
     */
    public static String extractScheme(final String challengeStr) throws MalformedChallengeException {
        if (challengeStr == null) {
            throw new IllegalArgumentException("Challenge may not be null");
        }
        final int idx = challengeStr.indexOf(' ');
        String s = null;
        if (idx == -1) {
            s = challengeStr;
        } else {
            s = challengeStr.substring(0, idx);
        }
        if (s.equals("")) {
            throw new MalformedChallengeException("Invalid challenge: " + challengeStr);
        }
        return s.toLowerCase();
    }

    /**
     * Extracts a map of challenge parameters from an authentication challenge.
     * Keys in the map are lower-cased
     *
     * @param challengeStr
     *        the authentication challenge string
     * @return a map of authentication challenge parameters
     * @throws MalformedChallengeException
     *         when the authentication challenge string is malformed
     *
     * @since 2.0beta1
     */
    public static Map<String, String> extractParams(final String challengeStr) throws MalformedChallengeException {
        if (challengeStr == null) {
            throw new IllegalArgumentException("Challenge may not be null");
        }

        final int idx = challengeStr.indexOf(' ');

        if (idx == -1) {
            throw new MalformedChallengeException("Invalid challenge: " + challengeStr);
        }

        final Map<String, String> map = new HashMap<String, String>();
        final ParameterParser parser = new ParameterParser();

        final List<NameValuePair> params = parser.parse(challengeStr.substring(idx + 1, challengeStr.length()), ',');

        for (int i = 0; i < params.size(); i++) {
            final NameValuePair param = params.get(i);

            map.put(param.getName().toLowerCase(), param.getValue());
        }

        return map;
    }

    /**
     * Extracts a map of challenges ordered by authentication scheme name
     *
     * @param headers
     *        the array of authorization challenges
     * @return a map of authorization challenges
     *
     * @throws MalformedChallengeException
     *         if any of challenge strings is malformed
     *
     * @since 3.0
     */
    public static Map<String, String> parseChallenges(final Header[] headers) throws MalformedChallengeException {
        if (headers == null) {
            throw new IllegalArgumentException("Array of challenges may not be null");
        }

        String challenge = null;
        final Map<String, String> challengemap = new HashMap<String, String>(headers.length);

        for (int i = 0; i < headers.length; i++) {
            challenge = headers[i].getValue();
            final String s = AuthChallengeParser.extractScheme(challenge);
            challengemap.put(s, challenge);
        }

        return challengemap;
    }
}
