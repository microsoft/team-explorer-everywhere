// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.internal.node;

import java.text.MessageFormat;

public class NodePathUtils {
    private static final String PATH_SEPARATOR = "\\"; //$NON-NLS-1$
    private static final String PATH_SEPARATOR_REGEX = "\\\\"; //$NON-NLS-1$

    public static String[] splitPathIntoSegments(String path) {
        if (path == null) {
            throw new IllegalArgumentException("path must not be null"); //$NON-NLS-1$
        }

        path = path.trim();

        while (path.startsWith(PATH_SEPARATOR)) {
            path = path.substring(1);
        }

        while (path.endsWith(PATH_SEPARATOR)) {
            path = path.substring(0, path.length() - 1);
        }

        final String[] segments = path.split(PATH_SEPARATOR_REGEX);

        for (int i = 0; i < segments.length; i++) {
            segments[i] = segments[i].trim();
        }

        return segments;
    }

    public static String createPathFromSegments(final String[] segments, final int beginIndex) {
        if (segments == null) {
            throw new IllegalArgumentException("segments must not be null"); //$NON-NLS-1$
        }

        if (beginIndex < 0 || beginIndex >= segments.length) {
            throw new IllegalArgumentException(MessageFormat.format(
                "beginIndex {0} is out of range (0,{1})", //$NON-NLS-1$
                Integer.toString(beginIndex),
                Integer.toString(segments.length)));
        }

        final StringBuffer sb = new StringBuffer();

        final int endIndex = segments.length;

        for (int i = beginIndex; i < endIndex; i++) {
            sb.append((segments[i] == null ? "" : segments[i])); //$NON-NLS-1$
            if (i < endIndex - 1) {
                sb.append(PATH_SEPARATOR);
            }
        }

        return sb.toString();
    }
}
