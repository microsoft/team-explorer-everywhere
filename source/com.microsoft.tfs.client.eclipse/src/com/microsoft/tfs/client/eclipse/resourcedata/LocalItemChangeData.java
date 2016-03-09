// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.eclipse.resourcedata;

import java.text.MessageFormat;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.resources.IResource;

import com.microsoft.tfs.client.common.framework.resources.ResourceType;
import com.microsoft.tfs.client.common.framework.resources.Resources;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.ItemType;
import com.microsoft.tfs.util.Check;

/**
 * Describes some change to a local item that's mapped to TFS.
 *
 * @threadsafety thread-safe
 */
class LocalItemChangeData {
    private static final Log log = LogFactory.getLog(LocalItemChangeData.class);

    private final String localItem;
    private final ItemType itemType;
    private final String serverItem;
    private final int version;

    /**
     * Creates a {@link LocalItemChangeData}.
     *
     * @param localItem
     *        the local path which changed (must not be <code>null</code>)
     * @param itemType
     *        the type of the local item which changed (must not be
     *        <code>null</code> and must not be {@link ItemType#ANY})
     * @param serverItem
     *        the server path that corresponds to the local item (
     *        <code>null</code> to indicate it no longer exists on the server)
     * @param version
     *        the version of the item now (0 means it no longer exists on the
     *        server)
     */
    protected LocalItemChangeData(
        final String localItem,
        final ItemType itemType,
        final String serverItem,
        final int version) {
        Check.notNull(localItem, "localItem"); //$NON-NLS-1$
        Check.notNull(itemType, "itemType"); //$NON-NLS-1$
        Check.isTrue(itemType != ItemType.ANY, "ItemType.ANY is not allowed"); //$NON-NLS-1$

        this.localItem = localItem;
        this.itemType = itemType;
        this.serverItem = serverItem;
        this.version = version;
    }

    public String getLocalItem() {
        return localItem;
    }

    public ItemType getItemType() {
        return itemType;
    }

    public String getServerItem() {
        return serverItem;
    }

    public int getVersion() {
        return version;
    }

    public ResourceDataUpdate createResourceDataUpdate() {
        final ResourceType resourceType = (itemType == ItemType.FILE) ? ResourceType.FILE : ResourceType.CONTAINER;

        /*
         * Note that we pass "false" for mustExist: we want to get resources
         * that were recently deleted (as part of the get operation that queued
         * this update), so these resources may not actually exist on disk in
         * the workspace location.
         */
        final IResource[] resources = Resources.getAllResourcesForLocation(getLocalItem(), resourceType, false);

        /*
         * It's possible the path isn't in a managed project yet, or is in some
         * other state. Ignore it.
         */
        if (resources == null || resources.length == 0) {
            log.trace(
                MessageFormat.format(
                    "Could not find a resource (even inaccessible ones) for local path {0}, ignoring", //$NON-NLS-1$
                    localItem));
            return null;
        }

        final boolean notInServer = serverItem == null || version == 0;
        return new ResourceDataUpdate(resources, (notInServer) ? null : new ResourceData(serverItem, version));
    }
}