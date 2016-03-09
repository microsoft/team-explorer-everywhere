// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.pid;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public final class ProductIDPlugin {
    /* Static methods only */
    private ProductIDPlugin() {
    }

    /* Java resource used for pre-pidding */
    private static final String RESOURCE_NAME = "com.microsoft.tfs.client.common.pid.txt"; //$NON-NLS-1$

    /**
     * Clients should call to ensure that the product id plugin exists and is
     * complete. This is simply a noop, but exists so that clients will fail
     * with a classloading exception when we do not exist.
     */
    public static void initialize() {
    }

    /**
     * Returns any pre-pidded product id for this product.
     *
     * @return A string representing the pre-pid data, or null if none exists.
     */
    public static String getProductID() {

        BufferedReader bufferedReader = null;
        try {
            final InputStream resourceStream =
                ProductIDPlugin.class.getClassLoader().getResourceAsStream(RESOURCE_NAME);

            if (resourceStream == null) {
                return null;
            }

            final InputStreamReader reader = new InputStreamReader(resourceStream);
            bufferedReader = new BufferedReader(reader);
            String line = bufferedReader.readLine();

            if (line == null) {
                return null;
            }

            line = line.trim();

            if (line.length() > 0) {
                return line;
            }
        } catch (final Exception e) {
            /* Ignore */
        } finally {
            if (bufferedReader != null) {
                try {
                    bufferedReader.close();
                } catch (final IOException e) {
                }
            }
        }

        return null;
    }
}