// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.eclipse.tpignore;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Status;

import com.microsoft.tfs.client.eclipse.Messages;
import com.microsoft.tfs.client.eclipse.TFSEclipseClientPlugin;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.FileHelpers;
import com.microsoft.tfs.util.LocaleUtil;

/**
 * <p>
 * This class implements a parser for <code>.tpignore</code> files. It reads the
 * <code>.tpignore</code> file from disk and parses each line into a Java
 * regular expression so we can match potential Eclipse project resources
 * against these for exclusion from the auto-pend-add behavior of the plug-in.
 * There is only one <code>.tpignore</code> file per Eclipse project.
 * </p>
 *
 * <p>
 * When a file is loaded via load(), lines beginning with # (ignoring leading
 * whitespace) are ignored.
 * </p>
 */
final class TPIgnoreFileParser {
    private final static Log log = LogFactory.getLog(TPIgnoreFileParser.class);

    /**
     * Lines beginning with this character (ignoring leading whitespace) are
     * ignored.
     */
    public final static char COMMENT_CHAR = '#';

    /**
     * The flags used during regular expression complilation to enable/disable
     * case sensitivity.
     */
    private static int compileFlags = -1000;

    /**
     * Gets the flags appropriate for this platform's filesystem's
     * case-sensitivity.
     */
    private synchronized static int getCompileFlags() {
        if (compileFlags == -1000) {
            if (FileHelpers.doesFileSystemIgnoreCase()) {
                compileFlags = Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE;
            } else {
                compileFlags = 0;
            }
        }

        return compileFlags;
    }

    /**
     * Loads all the exclusion patterns from the .tpignore file for the given
     * project if the file on disk exists and can be read. Malformed lines are
     * ignored and logged. Lines beginning with # are ignored.
     *
     * @param ignoreFile
     *        the .tpignore file to load (not null).
     * @return an array of the regular expression patterns loaded from the
     *         .tpignore file for the given project, or null if none were loaded
     *         because the file did not exist or an error occurred reading it.
     */
    public static Pattern[] load(final IFile ignoreFile) {
        Check.notNull(ignoreFile, "file"); //$NON-NLS-1$

        if (ignoreFile.exists() == false) {
            return null;
        }

        final ArrayList<Pattern> patterns = new ArrayList<Pattern>();

        BufferedReader reader = null;

        try {
            reader = new BufferedReader(
                new InputStreamReader(
                    ignoreFile.getContents(),
                    ignoreFile.getCharset() != null ? ignoreFile.getCharset() : ResourcesPlugin.getEncoding()));

            String line = null;

            int lineNumber = 0;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                line = line.trim();

                // Skip empty lines and comment lines.
                if (line.length() == 0 || line.startsWith("" + COMMENT_CHAR)) //$NON-NLS-1$
                {
                    continue;
                }

                try {
                    patterns.add(Pattern.compile(line, getCompileFlags()));
                } catch (final PatternSyntaxException e) {
                    log.warn(MessageFormat.format(
                        Messages.getString("TPIgnoreFileParser.CouldNotParseIgnorePatternFormat", LocaleUtil.ROOT), //$NON-NLS-1$
                        line,
                        ignoreFile.getLocation().toOSString(),
                        Integer.toString(lineNumber)), e);

                    TFSEclipseClientPlugin.getDefault().getLog().log(
                        new Status(
                            Status.ERROR,
                            TFSEclipseClientPlugin.PLUGIN_ID,
                            0,
                            MessageFormat.format(
                                Messages.getString("TPIgnoreFileParser.CouldNotParseIgnorePatternFormat"), //$NON-NLS-1$
                                line,
                                ignoreFile.getLocation().toOSString(),
                                Integer.toString(lineNumber)),
                            e));
                }
            }
        } catch (final CoreException e) {
            log.error(MessageFormat.format("Resource error reading resource exclusion file {0}", ignoreFile), e); //$NON-NLS-1$
            return null;
        } catch (final IOException e) {
            log.error(MessageFormat.format("Error reading resource exclusion file {0}", ignoreFile), e); //$NON-NLS-1$
            return null;
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (final IOException e) {
                }
            }
        }

        return patterns.toArray(new Pattern[patterns.size()]);
    }
}
