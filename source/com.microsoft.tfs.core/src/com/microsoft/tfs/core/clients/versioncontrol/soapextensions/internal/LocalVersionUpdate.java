// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.soapextensions.internal;

import com.microsoft.tfs.core.clients.versioncontrol.ClientLocalVersionUpdate;
import com.microsoft.tfs.core.clients.versioncontrol.ILocalVersionUpdate;
import com.microsoft.tfs.core.clients.versioncontrol.path.LocalPath;
import com.microsoft.tfs.core.internal.wrappers.WebServiceObjectWrapper;
import com.microsoft.tfs.util.Check;

import ms.tfs.versioncontrol.clientservices._03._LocalVersionUpdate;

/**
 * Contains version information about local working folder files sent to a TFS
 * <= 2010 server during and after a "get" operation.
 *
 * This class was public API in TFS 10.1, but was made internal in 11.0 because
 * {@link ClientLocalVersionUpdate} is used instead.
 *
 * @threadsafety thread-safe
 */
public final class LocalVersionUpdate extends WebServiceObjectWrapper {
    public LocalVersionUpdate(final ILocalVersionUpdate sourceUpdate) {
        super(new _LocalVersionUpdate());

        Check.isTrue(0 != sourceUpdate.getItemID(), "0 != sourceUpdate.getItemID()"); //$NON-NLS-1$
        setItemID(sourceUpdate.getItemID());
        setVersionLocal(sourceUpdate.getVersionLocal());
        setTargetLocalItem(sourceUpdate.getTargetLocalItem());
    }

    /**
     * Gets the web service object this class wraps. The returned object should
     * not be modified.
     *
     * @return the web service object this class wraps.
     */
    public _LocalVersionUpdate getWebServiceObject() {
        return (_LocalVersionUpdate) webServiceObject;
    }

    /**
     * @return the numeric ID of the item.
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
    public synchronized int getVersionLocal() {
        return getWebServiceObject().getLver();
    }

    /**
     * @param version
     *        the local version of the item.
     */
    public synchronized void setVersionLocal(final int version) {
        getWebServiceObject().setLver(version);
    }
}
