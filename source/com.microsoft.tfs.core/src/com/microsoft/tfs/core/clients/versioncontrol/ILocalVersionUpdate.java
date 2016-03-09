// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol;

public interface ILocalVersionUpdate {
    /**
     * Indicates whether or not this ILocalVersionUpdate is eligible to be
     * transformed into a LocalVersionUpdate/ServerItemLocalVersionUpdate object
     * for use in a webmethod call to UpdateLocalVersion. If false, the object
     * is intended to be consumed only by the local workspaces implementation of
     * UpdateLocalVersion.
     */
    boolean isSendToServer();

    /**
     * Committed server item of the item whose local version row should be
     * updated, or the target server item if the item is uncommitted
     * (VersionLocal == 0)
     */
    String getSourceServerItem();

    /**
     * Item ID corresponding to SourceServerItem (optional; but your calls to
     * UpdateLocalVersion on a pre-Dev11 server will fail if this is not
     * provided).
     */
    int getItemID();

    /**
     * The path on the local disk where this item is currently located, or null
     * to remove it from the workspace (delete the local version row).
     */
    String getTargetLocalItem();

    /**
     * The version of the item in the workspace. If zero, the item is
     * uncommitted (a pending add or branch).
     */
    int getVersionLocal();

    /**
     * Shorthand property for VersionLocal != 0
     */
    boolean isCommitted();

}
