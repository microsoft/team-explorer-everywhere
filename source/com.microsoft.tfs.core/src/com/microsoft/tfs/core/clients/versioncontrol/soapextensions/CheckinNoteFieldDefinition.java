// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.soapextensions;

import com.microsoft.tfs.core.internal.wrappers.WebServiceObjectWrapper;

import ms.tfs.versioncontrol.clientservices._03._CheckinNoteFieldDefinition;

/**
 * Defines a checkin note field (which has a name, can be required, and has a
 * display order).
 *
 * @since TEE-SDK-10.1
 */
public class CheckinNoteFieldDefinition extends WebServiceObjectWrapper implements Comparable {
    public CheckinNoteFieldDefinition() {
        super(new _CheckinNoteFieldDefinition());
    }

    public CheckinNoteFieldDefinition(final String name, final boolean required, final int displayOrder) {
        this(null, name, required, displayOrder);
    }

    public CheckinNoteFieldDefinition(final _CheckinNoteFieldDefinition definition) {
        super(definition);
    }

    /**
     * @param serverItem
     *        path to server item that this definition corresponds - usually the
     *        path of the project in source control.
     * @param name
     *        Name of the field
     * @param required
     *        Indicator defining if providing a value is mandatory
     * @param displayOrder
     *        integer representing the order in which the item should be
     *        presented to the user, low numbers first.
     */
    public CheckinNoteFieldDefinition(
        final String serverItem,
        final String name,
        final boolean required,
        final int displayOrder) {
        super(new _CheckinNoteFieldDefinition(serverItem, name, required, displayOrder));
    }

    /**
     * Gets the web service object this class wraps. The returned object should
     * not be modified.
     *
     * @return the web service object this class wraps.
     */
    public _CheckinNoteFieldDefinition getWebServiceObject() {
        return (_CheckinNoteFieldDefinition) webServiceObject;
    }

    @Override
    public boolean equals(final Object otherObject) {
        if (otherObject == this) {
            return true;
        }
        if (otherObject == null) {
            return false;
        }
        if ((otherObject instanceof CheckinNoteFieldDefinition) == false) {
            return false;
        }

        final CheckinNoteFieldDefinition other = (CheckinNoteFieldDefinition) otherObject;

        return isRequired() == other.isRequired()
            && getDisplayOrder() == other.getDisplayOrder()
            && (getName() != null && getName().equals(other.getName()))
            && (getServerItem() != null && getServerItem().equals(other.getServerItem()));
    }

    @Override
    public int hashCode() {
        int result = 17;

        result = 37 * result + ((getServerItem() == null) ? 0 : getServerItem().hashCode());
        result = 37 * result + ((getName() == null) ? 0 : getName().hashCode());
        result = 37 * result + ((isRequired()) ? 1 : 0);
        result = 37 * result + getDisplayOrder();

        return result;
    }

    /**
     * IComparable implementation. Sort by displayOrder then by name. However,
     * if two field defnitions have the same name, they are considered equal.
     *
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    @Override
    public int compareTo(final Object otherObject) {
        final CheckinNoteFieldDefinition other = (CheckinNoteFieldDefinition) otherObject;

        final int compare = getName().compareTo(other.getName());

        // Display order
        if (compare != 0) {
            if (getDisplayOrder() > other.getDisplayOrder()) {
                return 1;
            } else if (getDisplayOrder() < other.getDisplayOrder()) {
                return -1;
            }
        }

        return compare;
    }

    public String getServerItem() {
        return getWebServiceObject().getAi();
    }

    public void setServerItem(final String serverItem) {
        getWebServiceObject().setAi(serverItem);
    }

    public String getName() {
        return getWebServiceObject().getName();
    }

    public void setName(final String name) {
        getWebServiceObject().setName(name);
    }

    public boolean isRequired() {
        return getWebServiceObject().isReq();
    }

    public void setRequired(final boolean required) {
        getWebServiceObject().setReq(required);
    }

    public int getDisplayOrder() {
        return getWebServiceObject().get_do();
    }

    public void setDisplayOrder(final int displayOrder) {
        getWebServiceObject().set_do(displayOrder);
    }

}
