// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.MessageFormat;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.microsoft.tfs.client.common.Messages;

/**
 * <p>
 * Loads and displays license text from a program resource.
 * </p>
 *
 * @threadsafety immutable
 */
public class EULAText {
    private static final String RESOURCE_NAME = "eula.txt"; //$NON-NLS-1$
    private static final Log log = LogFactory.getLog(EULAText.class);
    private static final String NEWLINE = System.getProperty("line.separator"); //$NON-NLS-1$

    public static final String LICENSE_URL = "http://go.microsoft.com/fwlink/?LinkId=184735"; //$NON-NLS-1$

    public static String getEULAText() {
        InputStream in = null;
        BufferedReader bufferedReader = null;
        try {
            final StringBuffer inputData = new StringBuffer();

            in = EULAText.class.getClassLoader().getResourceAsStream(RESOURCE_NAME);

            if (in == null) {
                throw new RuntimeException("unable to load license resource [" //$NON-NLS-1$
                    + RESOURCE_NAME
                    + "] from class loader: " //$NON-NLS-1$
                    + EULAText.class.getClassLoader());
            }

            final InputStreamReader reader = new InputStreamReader(in, "UTF-8"); //$NON-NLS-1$
            bufferedReader = new BufferedReader(reader);
            String line = bufferedReader.readLine();

            while (line != null) {
                inputData.append(line).append(NEWLINE);
                line = bufferedReader.readLine();
            }

            return inputData.toString();
        } catch (final Throwable t) {
            t.printStackTrace();
            log.warn(t);

            final String messageFormat = Messages.getString("EulaText.ErrorLoadingEULAFormat"); //$NON-NLS-1$
            final String message = MessageFormat.format(messageFormat, LICENSE_URL);
            return message;
        } finally {
            if (bufferedReader != null) {
                try {
                    bufferedReader.close();
                } catch (final IOException e) {
                }
            }
        }
    }
}
