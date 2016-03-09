/*
 * $HeadURL:https://svn.apache.org/repos/asf/jakarta/commons/proper/httpclient/
 * trunk /src/java/org/apache/commons/httpclient/cookie/CookieOrigin.java $
 * $Revision:400312 $ $Date:2006-05-06 14:49:41 +0200 (Sat, 06 May 2006) $
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

/**
 * CookieOrigin class encapsulates details of an origin server that are relevant
 * when parsing, validating or matching HTTP cookies.
 *
 * @author <a href="mailto:oleg at ural.ru">Oleg Kalnichevski</a>
 *
 * @since 3.1
 */
public final class CookieOrigin {
    private final String host;
    private final int port;
    private final String path;
    private final boolean secure;

    public CookieOrigin(final String host, final int port, final String path, final boolean secure) {
        super();
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
        this.host = host;
        this.port = port;
        this.path = path;
        this.secure = secure;
    }

    public String getHost() {
        return host;
    }

    public String getPath() {
        return path;
    }

    public int getPort() {
        return port;
    }

    public boolean isSecure() {
        return secure;
    }
}
