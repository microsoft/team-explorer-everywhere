// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.util;

import java.text.MessageFormat;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import com.microsoft.tfs.core.Messages;
import com.microsoft.tfs.util.Platform;

public class FileTypeDescription {
    private static final String MESSAGES_PREFIX = "FileTypeDescription.DYNAMIC."; //$NON-NLS-1$

    private static final String WINDOWS_SUFFIX = ".Windows"; //$NON-NLS-1$
    private static final String UNIX_SUFFIX = ".Unix"; //$NON-NLS-1$

    private static Map<String, String> fileTypeMap;

    static {
        fileTypeMap = new HashMap<String, String>();

        for (final Enumeration<String> keys = Messages.getResourceBundle().getKeys(); keys.hasMoreElements();) {
            final String key = keys.nextElement();

            if (!key.startsWith(MESSAGES_PREFIX)) {
                continue;
            }

            String extension = key.substring(MESSAGES_PREFIX.length());
            String description = Messages.getString(key);

            if (extension.endsWith(WINDOWS_SUFFIX)) {
                extension = extension.substring(0, extension.length() - WINDOWS_SUFFIX.length());

                if (Platform.isCurrentPlatform(Platform.GENERIC_UNIX)) {
                    description =
                        MessageFormat.format(
                            Messages.getString("FileTypeDescription.WindowsDescriptionFormat"), //$NON-NLS-1$
                            description);
                }
            } else if (extension.endsWith(UNIX_SUFFIX)) {
                extension = extension.substring(0, extension.length() - UNIX_SUFFIX.length());

                if (Platform.isCurrentPlatform(Platform.WINDOWS)) {
                    description = MessageFormat.format(
                        Messages.getString("FileTypeDescription.UnixDescriptionFormat"), //$NON-NLS-1$
                        description);
                }
            }

            fileTypeMap.put(extension, description);
        }
    }

    private FileTypeDescription() {
    }

    public static String getDescription(final String filename) {
        int pos;

        if ((pos = filename.lastIndexOf('.')) >= 0 && pos < filename.length() - 1) {
            /*
             * Always upper case in english locale to avoid .ini file problems
             * in Turkey.
             */
            final String extension = filename.substring(pos + 1).toUpperCase(Locale.ENGLISH);

            if (fileTypeMap.containsKey(extension)) {
                return fileTypeMap.get(extension);
            }

            return MessageFormat.format(
                Messages.getString("FileTypeDescription.TypeFormat"), //$NON-NLS-1$
                extension);
        }

        return Messages.getString("FileTypeDescription.GenericFile"); //$NON-NLS-1$
    }
}
