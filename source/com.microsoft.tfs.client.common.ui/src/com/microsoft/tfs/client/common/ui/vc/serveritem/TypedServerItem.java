// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.vc.serveritem;

import java.text.MessageFormat;

import org.eclipse.core.resources.IResource;

import com.microsoft.tfs.client.common.repository.TFSRepository;
import com.microsoft.tfs.client.common.ui.vc.tfsitem.TFSFolder;
import com.microsoft.tfs.client.common.ui.vc.tfsitem.TFSItem;
import com.microsoft.tfs.core.clients.versioncontrol.path.ServerPath;
import com.microsoft.tfs.util.Check;

public class TypedServerItem implements Comparable<TypedServerItem> {
    public static final TypedServerItem ROOT = new TypedServerItem(ServerPath.ROOT, ServerItemType.ROOT);

    private final String serverPath;
    private final ServerItemType type;
    private final boolean isBranch;

    // only if available, expect null otherwise
    private String localPath;

    public TypedServerItem(final String serverPath, final ServerItemType type) {
        this(serverPath, type, false);
    }

    public TypedServerItem(final String serverPath, final ServerItemType type, final boolean isBranch) {
        Check.notNull(serverPath, "serverItem"); //$NON-NLS-1$
        Check.notNull(type, "type"); //$NON-NLS-1$

        this.serverPath = ServerPath.canonicalize(serverPath);

        ServerItemType detectedType = null;

        if (ServerPath.equals(serverPath, ServerPath.ROOT)) {
            detectedType = ServerItemType.ROOT;
        } else if (ServerPath.equals(ServerPath.getParent(serverPath), ServerPath.ROOT)) {
            detectedType = ServerItemType.TEAM_PROJECT;
        }

        /*
         * Always override type for root and team project for sane equality.
         */
        if (detectedType == ServerItemType.ROOT || detectedType == ServerItemType.TEAM_PROJECT) {
            this.type = detectedType;
        } else {
            this.type = type;
        }

        this.isBranch = isBranch;
    }

    public TypedServerItem(final String serverPath, final ServerItemType type, final String localPath) {
        this(serverPath, type, false, localPath);
    }

    public TypedServerItem(
        final String serverPath,
        final ServerItemType type,
        final boolean isBranch,
        final String localPath) {
        this(serverPath, type, isBranch);
        this.localPath = localPath;
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof TypedServerItem)) {
            return false;
        }

        final TypedServerItem other = (TypedServerItem) obj;

        return type.equals(other.type) && ServerPath.equals(serverPath, other.serverPath);
    }

    @Override
    public int hashCode() {
        int hash = 17;
        hash = 37 * hash + type.hashCode();
        hash = 37 * hash + serverPath.toLowerCase().hashCode();
        return hash;
    }

    @Override
    public int compareTo(final TypedServerItem other) {
        final String[] segments = ServerPath.split(serverPath);
        final String[] otherSegments = ServerPath.split(other.serverPath);

        int c = segments.length - otherSegments.length;
        if (c != 0) {
            return c;
        }

        for (int i = 0; i < segments.length; i++) {
            if (i == segments.length - 1) {
                c = type.compareTo(other.type);
                if (c != 0) {
                    return c;
                }
            }

            c = segments[i].compareToIgnoreCase(otherSegments[i]);
            if (c != 0) {
                return c;
            }
        }

        return 0;
    }

    @Override
    public String toString() {
        final String messageFormat = "{0} ({1})"; //$NON-NLS-1$
        return MessageFormat.format(messageFormat, serverPath, type);
    }

    public TypedServerItem getParent() {
        final String parentServerPath = ServerPath.getParent(serverPath);
        if (parentServerPath == null) {
            return null;
        }
        return new TypedServerItem(parentServerPath, ServerItemType.FOLDER);
    }

    public TypedServerItem[] getHierarchy() {
        final String[] hierarchyPaths = ServerPath.getHierarchy(serverPath);
        final TypedServerItem[] hierarchy = new TypedServerItem[hierarchyPaths.length];

        for (int i = 0; i < hierarchyPaths.length; i++) {
            ServerItemType currentType;
            if (i < hierarchyPaths.length - 1) {
                currentType = ServerItemType.FOLDER;
            } else {
                currentType = type;
            }
            hierarchy[i] = new TypedServerItem(hierarchyPaths[i], currentType);
        }

        return hierarchy;
    }

    public String getServerPath() {
        return serverPath;
    }

    public String getLocalPath() {
        return localPath;
    }

    public ServerItemType getType() {
        return type;
    }

    public boolean isBranch() {
        return isBranch;
    }

    public String getName() {
        if (type == ServerItemType.ROOT) {
            return ServerPath.ROOT;
        }

        return ServerPath.getFileName(serverPath);
    }

    public static TypedServerItem[] getTypedServerItemFromServerPaths(
        final String[] serverPaths,
        final ServerItemType type) {
        Check.notNull(serverPaths, "serverPaths"); //$NON-NLS-1$
        Check.notNull(type, "type"); //$NON-NLS-1$

        final TypedServerItem[] serverItems = new TypedServerItem[serverPaths.length];

        for (int i = 0; i < serverPaths.length; i++) {
            serverItems[i] = new TypedServerItem(serverPaths[i], type);
        }

        return serverItems;
    }

    public static TypedServerItem[] getTypedServerItemFromTFSItem(final TFSItem[] tfsItems) {
        Check.notNull(tfsItems, "tfsItems"); //$NON-NLS-1$

        final TypedServerItem[] serverItems = new TypedServerItem[tfsItems.length];

        for (int i = 0; i < tfsItems.length; i++) {
            final ServerItemType type =
                (tfsItems[i] instanceof TFSFolder) ? ServerItemType.FOLDER : ServerItemType.FILE;
            serverItems[i] = new TypedServerItem(tfsItems[i].getFullPath(), type, tfsItems[i].getLocalPath());
        }

        return serverItems;
    }

    public static TypedServerItem[] getTypedServerItemFromResource(
        final TFSRepository repository,
        final IResource[] resources) {
        Check.notNull(repository, "repository"); //$NON-NLS-1$
        Check.notNull(resources, "resources"); //$NON-NLS-1$

        final TypedServerItem[] serverItems = new TypedServerItem[resources.length];

        for (int i = 0; i < resources.length; i++) {
            final String localPath = resources[i].getLocation().toOSString();
            final String serverPath = repository.getWorkspace().getMappedServerPath(localPath);
            final ServerItemType type = (resources[i].getType() == IResource.FILE || resources[i].isLinked())
                ? ServerItemType.FILE : ServerItemType.FOLDER;

            serverItems[i] = new TypedServerItem(serverPath, type, localPath);
        }

        return serverItems;
    }
}
