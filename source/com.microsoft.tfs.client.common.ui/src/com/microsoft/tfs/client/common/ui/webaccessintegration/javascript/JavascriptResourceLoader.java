// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.webaccessintegration.javascript;

import java.io.IOException;
import java.io.InputStream;
import java.text.MessageFormat;
import java.util.MissingResourceException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.IOUtils;

public class JavascriptResourceLoader {
    private static final Log log = LogFactory.getLog(JavascriptResourceLoader.class);

    public static String loadJavascriptFile(final String resourceID) throws IOException {
        return loadJavascriptFile(resourceID, "UTF-8"); //$NON-NLS-1$
    }

    public static String loadJavascriptFile(final String resourceID, final String charsetName) throws IOException {
        Check.notNull(resourceID, "resourceID"); //$NON-NLS-1$
        Check.notNull(charsetName, "charsetName"); //$NON-NLS-1$

        final InputStream stream = JavascriptResourceLoader.class.getResourceAsStream(resourceID);

        if (stream == null) {
            throw new MissingResourceException(
                MessageFormat.format("Could not load resource {0}", resourceID), //$NON-NLS-1$
                resourceID,
                ""); //$NON-NLS-1$
        }

        /*
         * Read the resource as a string and return it.
         */
        try {
            try {
                return IOUtils.toString(stream, charsetName);
            } catch (final IOException e) {
                log.error("Error reading from stream", e); //$NON-NLS-1$
                throw e;
            }
        } finally {
            try {
                stream.close();
            } catch (final IOException e) {
                log.error("Error closing resource stream", e); //$NON-NLS-1$
            }
        }
    }
}
