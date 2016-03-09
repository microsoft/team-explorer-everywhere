// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.eclipse.resourcechange;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IResource;

import com.microsoft.tfs.core.util.FileEncoding;
import com.microsoft.tfs.util.Check;

public final class TFSResourceChangeSet {
    private final List<String> pathList = new ArrayList<String>();
    private final Map<String, IResource> pathToResourceMap = new HashMap<String, IResource>();
    private final Map<String, FileEncoding> pathToEncodingMap = new HashMap<String, FileEncoding>();

    public TFSResourceChangeSet() {
    }

    public void add(final String localPath, final FileEncoding fileEncoding, final IResource resource) {
        Check.notNull(localPath, "localPath"); //$NON-NLS-1$
        Check.notNull(fileEncoding, "fileEncoding"); //$NON-NLS-1$

        pathList.add(localPath);
        pathToEncodingMap.put(localPath, fileEncoding);
        pathToResourceMap.put(localPath, resource);
    }

    public List<String> getPathList() {
        return pathList;
    }

    public Map<String, FileEncoding> getPathToEncodingMap() {
        return pathToEncodingMap;
    }

    public Map<String, IResource> getPathToResourceMap() {
        return pathToResourceMap;
    }
}
