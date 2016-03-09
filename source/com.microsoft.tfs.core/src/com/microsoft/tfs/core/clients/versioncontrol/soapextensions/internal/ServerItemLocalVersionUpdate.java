// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.soapextensions.internal;

import com.microsoft.tfs.core.clients.versioncontrol.ILocalVersionUpdate;
import com.microsoft.tfs.core.clients.versioncontrol.path.LocalPath;
import com.microsoft.tfs.core.clients.versioncontrol.path.ServerPath;
import com.microsoft.tfs.core.internal.wrappers.WebServiceObjectWrapper;
import com.microsoft.tfs.util.Check;

import ms.tfs.versioncontrol.clientservices._03._ServerItemLocalVersionUpdate;

/**
 * Contains version information about local working folder files sent to a TFS
 * >= 2012 server during and after a "get" operation. Just like
 * {@link LocalVersionUpdate} but adds the source server item field.
 *
 * When connected to a TFS 2012 server only this class is used to communicate
 * version updates. {@link LocalVersionUpdate} is not used.
 *
 * @threadsafety thread-safe
 */
public final class ServerItemLocalVersionUpdate extends WebServiceObjectWrapper {
    public ServerItemLocalVersionUpdate() {
        this(new _ServerItemLocalVersionUpdate());
    }

    public ServerItemLocalVersionUpdate(final _ServerItemLocalVersionUpdate update) {
        super(update);
    }

    public ServerItemLocalVersionUpdate(final ILocalVersionUpdate sourceUpdate) {
        super(new _ServerItemLocalVersionUpdate());

        /*
         * Ensure the validation/conversion happens.
         */
        Check.notNullOrEmpty(sourceUpdate.getSourceServerItem(), "sourceUpdate.getSourceServerItem()"); //$NON-NLS-1$
        setSourceServerItem(sourceUpdate.getSourceServerItem());

        setLocalVersion(sourceUpdate.getVersionLocal());
        setTargetLocalItem(sourceUpdate.getTargetLocalItem());
    }

    /**
     * Gets the web service object this class wraps. The returned object should
     * not be modified.
     *
     * @return the web service object this class wraps.
     */
    public _ServerItemLocalVersionUpdate getWebServiceObject() {
        return (_ServerItemLocalVersionUpdate) webServiceObject;
    }

    /**
     * Test if the item has been committed to the server.
     *
     * @return Returns true if the item has been committed to the server.
     */
    public boolean isCommitted() {
        return getLocalVersion() != 0;
    }

    /**
     * @return the ID of the item being updated. This is sent from the server to
     *         the client when Reconcile is called. It is not used for calling
     *         UpdateLocalVersion.
     */
    public synchronized int getItemID() {
        return getWebServiceObject().getItemid();
    }

    /**
     * @param id
     *        the numeric ID of the item.
     */
    public synchronized void setItemID(final int id) {
        getWebServiceObject().setItemid(id);
    }

    /**
     * @return the path to the target local item.
     */
    public synchronized String getTargetLocalItem() {
        return LocalPath.tfsToNative(getWebServiceObject().getTlocal());
    }

    /**
     * @param item
     *        the path to the target local item.
     */
    public synchronized void setTargetLocalItem(final String item) {
        getWebServiceObject().setTlocal(LocalPath.nativeToTFS(item));
    }

    /**
     * @return the local version of the item.
     */
    public synchronized int getLocalVersion() {
        return getWebServiceObject().getLver();
    }

    /**
     * @param version
     *        the local version of the item.
     */
    public synchronized void setLocalVersion(final int version) {
        getWebServiceObject().setLver(version);
    }

    /**
     * For a committed item, this is the item in committed space, otherwise it
     * is the item in pending space.
     *
     * @return the path to the source server item.
     */
    public synchronized String getSourceServerItem() {
        return getWebServiceObject().getSitem();
    }

    /**
     * For a committed item, this is the item in committed space, otherwise it
     * is the item in pending space.
     *
     * @param item
     *        the path to the source server item.
     */
    public synchronized void setSourceServerItem(final String item) {
        getWebServiceObject().setSitem(item);
    }

    @Override
    public boolean equals(final Object o) {
        if (o == null || (o instanceof ServerItemLocalVersionUpdate) == false) {
            return false;
        }

        final ServerItemLocalVersionUpdate other = (ServerItemLocalVersionUpdate) o;
        if (isCommitted() == other.isCommitted()
            && ServerPath.equals(getSourceServerItem(), other.getSourceServerItem(), true)) {
            return true;
        }

        return false;
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }
}
