// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.soapextensions;

import java.util.Calendar;

import com.microsoft.tfs.core.internal.wrappers.WebServiceObjectWrapper;
import com.microsoft.tfs.core.internal.wrappers.WrapperUtils;

import ms.tfs.versioncontrol.clientservices._03._Item;
import ms.tfs.versioncontrol.clientservices._03._VersionControlLabel;

/**
 * Represents a label applied to a source code control item.
 *
 * @threadsafety thread-compatible
 * @since TEE-SDK-10.1
 */
public final class VersionControlLabel extends WebServiceObjectWrapper {
    public VersionControlLabel() {
        this(new _VersionControlLabel());
    }

    public VersionControlLabel(final _VersionControlLabel label) {
        super(label);

        final String displayName = label.getOwnerdisp();
        if (displayName == null || displayName.length() == 0) {
            label.setOwnerdisp(label.getOwner());
        }
    }

    /**
     * Constructs a version control label to be sent to the server. A version
     * control label contains a collection of items, but those are sent to the
     * server separately during label creation, so they are not included in this
     * constructor. That field (and others) is only used to return results of
     * label queries performed against the server.
     *
     * @param name
     *        the name of the label (must not be <code>null</code>)
     * @param owner
     *        the owner of the label.
     * @param scope
     *        the scope of the label.
     * @param comment
     *        a comment to attach to the label.
     */
    public VersionControlLabel(
        final String name,
        final String owner,
        final String ownerDisplayName,
        final String scope,
        final String comment) {
        super(
            new _VersionControlLabel(
                Calendar.getInstance(),
                name,
                owner,
                ownerDisplayName,
                owner,
                scope,
                0,
                comment,
                new _Item[0]));
    }

    /**
     * Gets the web service object this class wraps. The returned object should
     * not be modified.
     *
     * @return the web service object this class wraps.
     */
    public _VersionControlLabel getWebServiceObject() {
        return (_VersionControlLabel) webServiceObject;
    }

    public String getName() {
        return getWebServiceObject().getName();
    }

    public String getOwner() {
        return getWebServiceObject().getOwner();
    }

    public void setOwner(final String owner) {
        getWebServiceObject().setOwner(owner);
    }

    public String getOwnerDisplayName() {
        return getWebServiceObject().getOwnerdisp();
    }

    public String getScope() {
        return getWebServiceObject().getScope();
    }

    public String getComment() {
        return getWebServiceObject().getComment();
    }

    public Calendar getDate() {
        return getWebServiceObject().getDate();

    }

    public Item[] getItems() {
        if (getWebServiceObject().getItems() == null) {
            return null;
        }

        return (Item[]) WrapperUtils.wrap(Item.class, getWebServiceObject().getItems());
    }
}
