// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

/**
 * Simple test harness to find EBCDIC files using the method that kicks in on
 * z/OS during pending adds. Lets me run the logic against a large number of
 * files to remove false positives and negatives.
 */
public class LooksLikeEBCDICUtil {
    public static void main(final String[] args) {
        if (args.length == 0) {
            System.err.println("usage: " + LooksLikeEBCDICUtil.class.getName() + " <file> [<file>...]"); //$NON-NLS-1$ //$NON-NLS-2$
            System.exit(-1);
        }

        for (int i = 0; i < args.length; i++) {
            final String filePath = args[i];

            if (new File(filePath).exists() == false) {
                System.err.println("File " + filePath + " does not exit."); //$NON-NLS-1$ //$NON-NLS-2$
                System.exit(1);
            }

            InputStream stream = null;
            try {
                stream = new FileInputStream(filePath);
            } catch (final FileNotFoundException e1) {
                e1.printStackTrace();
                System.exit(2);
            }

            final byte[] buffer = new byte[1024];

            int read = 0;
            try {
                read = stream.read(buffer, 0, buffer.length);
            } catch (final IOException e) {
                e.printStackTrace();
                System.exit(3);
            }

            final boolean looksLikeEBCDIC = FileEncodingDetector.looksLikeEBCDIC(buffer, read);

            System.out.println(filePath + ": " + ((looksLikeEBCDIC) ? " EBCDIC" : "something else")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

            try {
                stream.close();
            } catch (final IOException e) {
                e.printStackTrace();
                System.exit(4);
            }
        }
    }
}
