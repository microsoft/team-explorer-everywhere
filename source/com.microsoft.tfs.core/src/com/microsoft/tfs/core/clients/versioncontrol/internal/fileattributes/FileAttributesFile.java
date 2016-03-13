// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.internal.fileattributes;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.MessageFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.PatternSyntaxException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.StringUtil;

/**
 * A file attributes file is a text file consisting of lines, each a
 * serialized-to-string {@link FileAttributesEntry}. One of these files can be
 * stored in the TFS repository in the directory where files listed in
 * {@link FileAttributesEntry}s will be updated during get operations by TEE
 * clients.
 *
 * When a file is loaded via load(), lines beginning with # (ignoring leading
 * whitespace) are ignored.
 */
public final class FileAttributesFile {
    private final static Log log = LogFactory.getLog(FileAttributesFile.class);

    /**
     * The default name for files that contain data this class can load.
     */
    public static final String DEFAULT_FILENAME = ".tpattributes"; //$NON-NLS-1$

    /**
     * Lines beginning with this character (ignoring leading whitespace) are
     * ignored.
     */
    public static final char COMMENT_CHAR = '#';

    private interface AttributesLineTransformation {
        String transform(String input);
    }

    /**
     * Reads the given global attributes file and parses each line into a
     * {@link FileAttributesEntry}. Malformed lines are ignored and logged.
     * Lines beginning with # are ignored.
     *
     * @param localPath
     *        the local file path to load (must not be <code>null</code> or
     *        empty).
     * @return a list containing all the {@link FileAttributesEntry}s that were
     *         loaded in their original order, or null if the file was not found
     *         or an error occurred reading the file.
     */
    public static List<FileAttributesEntry> loadGlobalFile(final String localPath) {
        Check.notNullOrEmpty(localPath, "localPath"); //$NON-NLS-1$

        return loadFile(localPath, new AttributesLineTransformation() {
            @Override
            public String transform(final String line) {
                if (line.startsWith("$/")) //$NON-NLS-1$
                {
                    return "\\" + line; //$NON-NLS-1$
                }

                return line;
            }
        });
    }

    /**
     * Reads the given file and parses each line into a
     * {@link FileAttributesEntry}. Malformed lines are ignored and logged.
     * Lines beginning with # are ignored.
     *
     * @param localPath
     *        the local file path to load (must not be <code>null</code> or
     *        empty).
     * @return a list containing all the {@link FileAttributesEntry}s that were
     *         loaded in their original order, or null if the file was not found
     *         or an error occurred reading the file.
     */
    public static List<FileAttributesEntry> loadAttributesFile(final String localPath) {
        Check.notNullOrEmpty(localPath, "localPath"); //$NON-NLS-1$

        return loadFile(localPath, null);
    }

    private static List<FileAttributesEntry> loadFile(
        final String localPath,
        final AttributesLineTransformation lineTransformation) {
        final File file = new File(localPath);

        if (file.exists() == false) {
            return null;
        }

        final List<FileAttributesEntry> entries = new ArrayList<FileAttributesEntry>();

        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(file));

            String line = null;

            int lineNumber = 0;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                line = StringUtil.trimBegin(line);

                // Skip empty lines and comment lines.
                if (line.length() == 0 || line.startsWith("" + COMMENT_CHAR)) //$NON-NLS-1$
                {
                    continue;
                }

                if (lineTransformation != null) {
                    line = lineTransformation.transform(line);

                    if (StringUtil.isNullOrEmpty(line)) {
                        continue;
                    }
                }

                try {
                    final FileAttributesEntry e = FileAttributesEntry.parse(line);
                    entries.add(e);
                } catch (final PatternSyntaxException e) {
                    log.warn(
                        MessageFormat.format(
                            "Ignoring entry for invalid regular expression at line {0} of file {1}", //$NON-NLS-1$
                            lineNumber,
                            localPath),
                        e);
                } catch (final ParseException e) {
                    log.warn(MessageFormat.format(
                        "Ignoring malformed file attributes entry at line {0} of file {1}", //$NON-NLS-1$
                        lineNumber,
                        localPath));
                }
            }
        } catch (final IOException e) {
            log.error(MessageFormat.format("Error reading attributes file {0}: {1}", localPath, e.getMessage())); //$NON-NLS-1$
            return null;
        } finally {
            try {
                if (reader != null) {
                    reader.close();
                }
            } catch (final IOException e) {
            }
        }

        return entries;
    }
}
