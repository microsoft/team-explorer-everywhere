// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.jni.internal.console;

import java.io.File;
import java.text.MessageFormat;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.microsoft.tfs.jni.Console;
import com.microsoft.tfs.jni.ExecHelpers;
import com.microsoft.tfs.util.Check;

/**
 * An implementation of the {@link Console} interface via external Unix process
 * execution.
 */
public class UnixExecConsole implements Console {
    private final static Log log = LogFactory.getLog(UnixExecConsole.class);

    public boolean isReadOnly(final String filepath) {
        Check.notNull(filepath, "filepath"); //$NON-NLS-1$

        /*
         * Using Java's File class is faster than spawning an external process.
         *
         * This method for querying writable status fails on Unix when running
         * as root. Many Java implementations return "true" for all files
         * regardless of permissions set on the file when root.
         */
        return new File(filepath).canWrite() == false;
    }

    @Override
    public boolean disableEcho() {
        return false;
    }

    @Override
    public boolean enableEcho() {
        return false;
    }

    public boolean changeCurrentDirectory(final String directory) {
        /*
         * Just can't do this.
         */
        return false;
    }

    @Override
    public int getConsoleColumns() {
        final int[] size = new int[2];

        if (getConsoleSizeViaEnvironment(size)) {
            return size[0];
        }

        return 0;
    }

    @Override
    public int getConsoleRows() {
        final int[] size = new int[2];

        if (getConsoleSizeViaEnvironment(size)) {
            return size[1];
        }

        return 0;
    }

    /**
     * Tries to read the console size via the COLUMNS and LINES environment
     * variables.
     *
     * @param size
     *        Results are written here. size[0] will contain columns (if read),
     *        size[1] will contain rows (if read).
     * @return true if both variables were parsed and stored in size, false
     *         otherwise.
     */
    private boolean getConsoleSizeViaEnvironment(final int[] size) {
        Check.isTrue(size.length == 2, "size array must have length 2"); //$NON-NLS-1$

        boolean readColumns = false;

        /*
         * System.getenv() would make our job much easier, but it was deprecated
         * for Java 1.4, then reprecated for Java 1.5, so we can't rely on it.
         * We have to use a shell instead.
         */

        StringBuffer output = new StringBuffer();

        if (ExecHelpers.exec(new String[] {
            "/bin/sh", //$NON-NLS-1$
            "-c", //$NON-NLS-1$
            "echo $COLUMNS" //$NON-NLS-1$
        }, output) == 0) {
            log.trace(MessageFormat.format(
                "exec for $COLUMNS returned exit code 0, raw output [{0}]", //$NON-NLS-1$
                output.toString()));

            final String columns = output.toString().trim();

            if (columns != null && columns.length() > 0) {
                size[0] = Integer.parseInt(columns);
                log.trace(MessageFormat.format("parsed integer {0} for columns", Integer.toString(size[0]))); //$NON-NLS-1$
                readColumns = true;
            }
        }

        output = new StringBuffer();

        if (ExecHelpers.exec(new String[] {
            "/bin/sh", //$NON-NLS-1$
            "-c", //$NON-NLS-1$
            "echo $LINES" //$NON-NLS-1$
        }, output) == 0) {
            log.trace(
                MessageFormat.format("exec for $LINES returned exit code 0, raw output [{0}]", output.toString())); //$NON-NLS-1$

            final String rows = output.toString().trim();

            if (rows != null && rows.length() > 0) {
                size[1] = Integer.parseInt(rows);
                log.trace(MessageFormat.format("parsed integer {0} for rows", Integer.toString(size[1]))); //$NON-NLS-1$

                if (readColumns) {
                    return true;
                }
            }
        }

        return false;
    }

}
