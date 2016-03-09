// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.framework.diagnostics;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class SupportPluginUtils {
    public static void addFileToZip(final File file, final String zipName, final ZipOutputStream zipout)
        throws IOException {
        InputStream in = null;
        try {
            in = new BufferedInputStream(new FileInputStream(file));
            final ZipEntry entry = new ZipEntry(zipName);
            zipout.putNextEntry(entry);
            final byte[] buffer = new byte[2048];
            int length;

            while ((length = in.read(buffer)) != -1) {
                zipout.write(buffer, 0, length);
            }
        } finally {
            if (in != null) {
                in.close();
            }
        }
    }
}
