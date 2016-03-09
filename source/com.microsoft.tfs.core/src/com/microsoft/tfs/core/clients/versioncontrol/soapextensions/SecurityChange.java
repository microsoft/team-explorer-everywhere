// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.soapextensions;

import com.microsoft.tfs.core.clients.versioncontrol.path.ItemPath;
import com.microsoft.tfs.core.internal.wrappers.WebServiceObjectWrapper;
import com.microsoft.tfs.util.Check;

import ms.tfs.versioncontrol.clientservices._03._SecurityChange;

/**
 * Represents a security change.
 *
 * @since TEE-SDK-10.1
 */
public abstract class SecurityChange extends WebServiceObjectWrapper {
    public SecurityChange(final _SecurityChange change) {
        super(change);

        Check.notNullOrEmpty(change.getItem(), "change.getItem()"); //$NON-NLS-1$
        change.setItem(ItemPath.canonicalize(change.getItem()));
    }

    /**
     * Gets the web service object this class wraps. The returned object should
     * not be modified.
     *
     * @return the web service object this class wraps.
     */
    private _SecurityChange getWebServiceObject() {
        return (_SecurityChange) webServiceObject;
    }

    /**
     * @return the path of the item whose security was changed
     */
    public String getItem() {
        return getWebServiceObject().getItem();
    }
}
