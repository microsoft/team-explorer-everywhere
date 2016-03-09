/*
 * $HeadURL:
 * https://svn.apache.org/repos/asf/jakarta/httpcomponents/oac.hc3x/tags
 * /HTTPCLIENT_3_1
 * /src/java/org/apache/commons/httpclient/methods/FileRequestEntity.java $
 * $Revision: 486665 $ $Date: 2006-12-13 15:19:07 +0100 (Wed, 13 Dec 2006) $
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
package com.microsoft.tfs.core.httpclient.methods;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * A RequestEntity that represents a File.
 *
 * @since 3.1
 */
public class FileRequestEntity implements RequestEntity {

    final File file;
    final String contentType;

    public FileRequestEntity(final File file, final String contentType) {
        super();
        if (file == null) {
            throw new IllegalArgumentException("File may not be null");
        }
        this.file = file;
        this.contentType = contentType;
    }

    @Override
    public long getContentLength() {
        return file.length();
    }

    @Override
    public String getContentType() {
        return contentType;
    }

    @Override
    public boolean isRepeatable() {
        return true;
    }

    @Override
    public void writeRequest(final OutputStream out) throws IOException {
        final byte[] tmp = new byte[4096];
        int i = 0;
        final InputStream instream = new FileInputStream(file);
        try {
            while ((i = instream.read(tmp)) >= 0) {
                out.write(tmp, 0, i);
            }
        } finally {
            instream.close();
        }
    }

}
