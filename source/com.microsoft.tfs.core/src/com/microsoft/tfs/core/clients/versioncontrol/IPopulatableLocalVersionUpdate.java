// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol;

import com.microsoft.tfs.core.clients.versioncontrol.internal.localworkspace.WorkspaceLocalItem;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Item;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.PendingChange;

/**
 *
 *
 *
 * @threadsafety unknown
 */
public interface IPopulatableLocalVersionUpdate extends ILocalVersionUpdate {
    /**
     * The baseline file GUID for the baseline for this update
     */
    byte[] getBaselineFileGUID();

    /**
     * The MD5 hash value of the baseline for this update
     */
    byte[] getBaselineHashValue();

    /**
     * The URL where the baseline for this local version entry can be retrieved
     */
    String getDownloadURL();

    void setDownloadURL(final String value);

    /**
     * If non-null, EnsureUpdatesFullyPopulated may use QueryPendingChanges as a
     * preferred data source for populating missing baseline information. The
     * pending change to be queried will be the one on this target server item.
     */
    String getPendingChangeTargetServerItem();

    /**
     * Indicates whether this IPopulatableLocalVersionUpdate has all the fields
     * populated that are necessary to call UpdateLocalVersion for a local
     * workspace.
     */
    boolean isFullyPopulated(boolean requireVersionLocalDate);

    /**
     * Updates the populatable fields of this IPopulatableLocalVersionUpdate
     * from the data in the provided Item object.
     *
     *
     * @param item
     *        Data source to update from
     */
    void updateFrom(Item item);

    /**
     * Updates the populatable fields of this IPopulatableLocalVersionUpdate
     * from the data in the provided WorkspaceLocalItem object.
     *
     *
     * @param lvExisting
     *        Data source to update from
     */
    void updateFrom(WorkspaceLocalItem lvExisting);

    /**
     * Updates the populatable fields of this IPopulatableLocalVersionUpdate
     * from the data in the provided PendingChange object.
     *
     *
     * @param pendingChange
     *        Data source to update from
     */
    void updateFrom(PendingChange pendingChange);

}
