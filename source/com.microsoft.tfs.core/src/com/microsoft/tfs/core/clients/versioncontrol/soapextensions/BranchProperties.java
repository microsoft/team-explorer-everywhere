// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.soapextensions;

import com.microsoft.tfs.core.internal.wrappers.WebServiceObjectWrapper;
import com.microsoft.tfs.core.internal.wrappers.WrapperUtils;
import com.microsoft.tfs.util.Check;

import ms.tfs.versioncontrol.clientservices._03._BranchProperties;
import ms.tfs.versioncontrol.clientservices._03._ItemIdentifier;

/**
 * Describes properties of a branch object.
 *
 * @since TEE-SDK-10.1
 */
public class BranchProperties extends WebServiceObjectWrapper {
    public BranchProperties(final _BranchProperties webServiceObject) {
        super(webServiceObject);

        // Older servers will not pass down the display name so just set it to
        // owner.
        final String displayName = webServiceObject.getOwnerDisplayName();
        if (displayName == null || displayName.length() == 0) {
            setOwnerDisplayName(webServiceObject.getOwner());
        }
    }

    /**
     * Gets the web service object this class wraps. The returned object should
     * not be modified.
     *
     * @return the web service object this class wraps.
     */
    public _BranchProperties getWebServiceObject() {
        return (_BranchProperties) webServiceObject;
    }

    public Mapping[] getBranchMappings() {
        return (Mapping[]) WrapperUtils.wrap(Mapping.class, getWebServiceObject().getBranchMappings());
    }

    public String getDescription() {
        return getWebServiceObject().getDescription();
    }

    public String getOwner() {
        return getWebServiceObject().getOwner();
    }

    public String getOwnerDisplayName() {
        return getWebServiceObject().getOwnerDisplayName();
    }

    public ItemIdentifier getParentBranch() {
        if (getWebServiceObject().getParentBranch() == null) {
            return null;
        }
        return new ItemIdentifier(getWebServiceObject().getParentBranch());
    }

    public ItemIdentifier getRootItem() {
        return new ItemIdentifier(getWebServiceObject().getRootItem());
    }

    public void setDescription(final String desc) {
        getWebServiceObject().setDescription(desc);
    }

    public void setOwner(final String owner) {
        getWebServiceObject().setOwner(owner);
    }

    public void setOwnerDisplayName(final String owner) {
        getWebServiceObject().setOwnerDisplayName(owner);
    }

    public static BranchProperties from(
        final ItemIdentifier item,
        final String desc,
        final String owner,
        final String ownerDisplayName,
        final ItemIdentifier parent) {
        Check.notNull(item, "item"); //$NON-NLS-1$
        _ItemIdentifier parentItem = null;
        if (parent != null) {
            parentItem = parent.getWebServiceObject();
        }

        return new BranchProperties(
            new _BranchProperties(item.getWebServiceObject(), desc, owner, ownerDisplayName, owner, parentItem, null));
    }

    public static BranchProperties from(final BranchProperties prop, final BranchObject parent) {
        return from(
            prop.getRootItem(),
            prop.getDescription(),
            prop.getOwner(),
            prop.getOwnerDisplayName(),
            parent.getProperties().getRootItem());
    }
}
