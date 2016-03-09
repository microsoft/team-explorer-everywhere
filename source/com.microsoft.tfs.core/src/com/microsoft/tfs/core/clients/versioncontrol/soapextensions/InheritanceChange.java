// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.soapextensions;

import ms.tfs.versioncontrol.clientservices._03._InheritanceChange;

/**
 * @since TEE-SDK-10.1
 */
public abstract class InheritanceChange extends SecurityChange {
    /**
     * Constructs an {@link InheritanceChange} for the given item.
     *
     * @param item
     *        the item being changed (must not be <code>null</code> or empty)
     * @param inherit
     *        true if the item will now inherit permissions, false if it will
     *        not
     */
    public InheritanceChange(final String item, final boolean inherit) {
        super(new _InheritanceChange(item, inherit));
    }

    public InheritanceChange(final _InheritanceChange change) {
        super(change);
    }

    /**
     * Gets the web service object this class wraps. The returned object should
     * not be modified.
     *
     * @return the web service object this class wraps.
     */
    public _InheritanceChange getWebServiceObject() {
        return (_InheritanceChange) webServiceObject;
    }

    /**
     * @return true if the changed item now inherits permissions, false if it
     *         does not inherit permissions
     */
    public boolean isInherit() {
        return getWebServiceObject().isInherit();
    }

}
