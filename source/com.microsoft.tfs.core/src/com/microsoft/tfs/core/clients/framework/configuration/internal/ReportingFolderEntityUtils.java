// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.framework.configuration.internal;

import java.text.MessageFormat;
import java.util.Stack;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.microsoft.tfs.core.clients.framework.configuration.TFSEntity;
import com.microsoft.tfs.core.clients.framework.configuration.entities.ReportingFolderEntity;
import com.microsoft.tfs.core.clients.framework.configuration.entities.ReportingServerEntity;
import com.microsoft.tfs.core.util.URIUtils;
import com.microsoft.tfs.util.Check;

public class ReportingFolderEntityUtils {
    private static final Log log = LogFactory.getLog(ReportingFolderEntityUtils.class);

    private ReportingFolderEntityUtils() {
    }

    public static String getFullItemPath(final ReportingFolderEntity folder) {
        Check.notNull(folder, "folder"); //$NON-NLS-1$

        final Stack<String> path = new Stack<String>();

        TFSEntity facet = folder;

        /* Walk the parentage to build the path to this object */
        while (facet != null) {
            if (facet instanceof ReportingFolderEntity) {
                final String itemPath = ((ReportingFolderEntity) facet).getItemPath();

                if (itemPath != null && itemPath.length() > 0) {
                    path.push(itemPath);
                }

                facet = ((ReportingFolderEntity) facet).getReferencedResource();
            } else if (facet instanceof ReportingServerEntity) {
                /* Stop */
                break;
            } else {
                log.warn(MessageFormat.format(
                    "Invalid type {0} along reporting folder dependency chain", //$NON-NLS-1$
                    facet.getClass().getCanonicalName()));
                break;
            }
        }

        // Construct the full item path.
        String fullItemPath = ""; //$NON-NLS-1$

        while (path.size() > 0) {
            String pathSegment = path.pop();

            pathSegment = pathSegment.trim();

            while (pathSegment.startsWith("/") || pathSegment.startsWith("\\")) //$NON-NLS-1$ //$NON-NLS-2$
            {
                pathSegment = pathSegment.substring(1);
            }

            fullItemPath = URIUtils.combinePaths(fullItemPath, pathSegment);
        }

        // Full item paths always start with a '/'
        if (fullItemPath.startsWith("/")) //$NON-NLS-1$
        {
            return fullItemPath;
        }

        return "/" + fullItemPath; //$NON-NLS-1$

    }
}
