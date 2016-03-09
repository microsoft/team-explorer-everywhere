// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.framework.configuration.internal;

import java.util.Stack;

import com.microsoft.tfs.core.clients.framework.configuration.TFSEntity;
import com.microsoft.tfs.core.clients.framework.configuration.TFSWebSiteEntity;
import com.microsoft.tfs.core.clients.framework.configuration.TFSWebSiteEntity.TFSAbsoluteWebSiteEntity;
import com.microsoft.tfs.core.clients.framework.configuration.TFSWebSiteEntity.TFSRelativeWebSiteEntity;
import com.microsoft.tfs.core.util.URIUtils;
import com.microsoft.tfs.util.Check;

public class TFSWebSiteEntityUtils {
    public static String getFullItemPath(final TFSWebSiteEntity entity) {
        final Stack<String> pathStack = new Stack<String>();

        String url = null;
        TFSWebSiteEntity facet = entity;

        while (facet != null) {
            if (facet instanceof TFSRelativeWebSiteEntity) {
                final TFSRelativeWebSiteEntity relative = (TFSRelativeWebSiteEntity) facet;

                if (relative.getRelativePath() != null && relative.getRelativePath().length() > 0) {
                    pathStack.push(relative.getRelativePath());
                }

                final TFSEntity parent = relative.getReferencedResource();

                if (parent == null) {
                    facet = null;
                } else {
                    Check.isTrue(parent instanceof TFSWebSiteEntity, "parent instanceof TFSWebSiteEntity"); //$NON-NLS-1$
                    facet = (TFSWebSiteEntity) parent;
                }
            } else if (facet instanceof TFSAbsoluteWebSiteEntity) {
                final TFSAbsoluteWebSiteEntity absolute = (TFSAbsoluteWebSiteEntity) facet;

                url = absolute.getBaseURL();
                Check.notNull(url, "url"); //$NON-NLS-1$

                break;
            } else {
                throw new RuntimeException("Invalid type along site path dependency chain"); //$NON-NLS-1$
            }
        }

        // Construct the path.
        while (pathStack.size() > 0) {
            url = URIUtils.combinePartiallyEncodedPaths(url, pathStack.pop());
        }

        return url;
    }
}
