// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.vc.serveritem;

import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.ItemType;
import com.microsoft.tfs.util.Check;

public class ServerItemType implements Comparable<ServerItemType> {
    public static final ServerItemType ROOT = new ServerItemType("ROOT", 1); //$NON-NLS-1$
    public static final ServerItemType TEAM_PROJECT = new ServerItemType("TEAM_PROJECT", 2); //$NON-NLS-1$
    public static final ServerItemType FOLDER = new ServerItemType("FOLDER", 4); //$NON-NLS-1$
    public static final ServerItemType FILE = new ServerItemType("FILE", 8); //$NON-NLS-1$
    public static final ServerItemType GIT_REPOSITORY = new ServerItemType("GIT_REPOSITORY", 16); //$NON-NLS-1$
    public static final ServerItemType GIT_BRANCH = new ServerItemType("GIT_BRANCH", 32); //$NON-NLS-1$

    public static final ServerItemType[] ALL = new ServerItemType[] {
        ROOT,
        TEAM_PROJECT,
        FOLDER,
        FILE
    };

    public static final ServerItemType[] ALL_FOLDERS = new ServerItemType[] {
        ROOT,
        TEAM_PROJECT,
        FOLDER
    };

    public static final ServerItemType[] ALL_FOLDERS_AND_GIT = new ServerItemType[] {
        ROOT,
        TEAM_PROJECT,
        FOLDER,
        GIT_REPOSITORY,
        GIT_BRANCH
    };

    public static boolean isFile(final ServerItemType type) {
        return type.equals(FILE);
    }

    public static boolean isGitRepository(final ServerItemType type) {
        return type.equals(GIT_REPOSITORY);
    }

    public static boolean isFolder(final ServerItemType type) {
        for (int i = 0; i < ALL_FOLDERS.length; i++) {
            if (type.equals(ALL_FOLDERS[i])) {
                return true;
            }
        }

        return false;
    }

    public static ServerItemType getTypeFromItemType(final ItemType itemType) {
        Check.notNull(itemType, "itemType"); //$NON-NLS-1$

        if (itemType == ItemType.FILE) {
            return FILE;
        }

        if (itemType == ItemType.FOLDER) {
            return FOLDER;
        }

        throw new IllegalArgumentException(
            "ItemType.ANY is not supported. itemType must be ItemType.FILE or ItemType.FOLDER"); //$NON-NLS-1$
    }

    private final String type;
    private final int flag;

    private ServerItemType(final String type, final int flag) {
        this.type = type;
        this.flag = flag;
    }

    @Override
    public String toString() {
        return type;
    }

    public int getFlag() {
        return flag;
    }

    @Override
    public int compareTo(final ServerItemType other) {
        if (flag < other.flag) {
            return -1;
        } else if (flag > other.flag) {
            return 1;
        }
        return 0;
    }
}
