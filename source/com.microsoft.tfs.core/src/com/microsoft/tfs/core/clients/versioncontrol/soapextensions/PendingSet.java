// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.soapextensions;

import com.microsoft.tfs.core.clients.versioncontrol.OwnershipState;
import com.microsoft.tfs.core.internal.wrappers.WebServiceObjectWrapper;
import com.microsoft.tfs.core.internal.wrappers.WrapperUtils;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.GUID;

import ms.tfs.versioncontrol.clientservices._03._PendingChange;
import ms.tfs.versioncontrol.clientservices._03._PendingSet;

/**
 * Set of {@link PendingChange}s.
 *
 * @since TEE-SDK-10.1
 */
public final class PendingSet extends WebServiceObjectWrapper {
    /**
     * Contains the change array directly. These are cached in the wrapper for
     * speed and to enable setting fields in {@link PendingChange} objects once
     * (instead of each time {@link #getPendingChanges()} is called).
     *
     * {@link #getWebServiceObject()} will always update the wrapped web service
     * object before it returns it (it will be up-to-date).
     *
     * Constructors must initialize this field from available data.
     */
    private PendingChange[] pendingChanges;

    private PendingChange[] candidatePendingChanges;

    public PendingSet(final _PendingSet pendingSet) {
        super(pendingSet);

        final String displayName = pendingSet.getOwnerdisp();
        if (displayName == null || displayName.length() == 0) {
            pendingSet.setOwnerdisp(pendingSet.getOwner());
        }

        if (pendingSet.getPendingChanges() != null) {
            this.pendingChanges =
                (PendingChange[]) WrapperUtils.wrap(PendingChange.class, pendingSet.getPendingChanges());
        }

        if (pendingSet.getSignature() == null) {
            pendingSet.setSignature(GUID.EMPTY.getGUIDString());
        }
    }

    public PendingSet(
        final String name,
        final String ownerName,
        final String ownerDisplayName,
        final OwnershipState ownership,
        final String computer,
        final PendingSetType type,
        final PendingChange[] pendingChanges) {
        this(name, ownerName, ownerDisplayName, ownership, computer, type, pendingChanges, null);
    }

    public PendingSet(
        final String name,
        final String ownerName,
        final String ownerDisplayName,
        final OwnershipState ownership,
        final String computer,
        final PendingSetType type,
        final PendingChange[] pendingChanges,
        final PendingChange[] candidatePendingChanges) {
        this(
            new _PendingSet(
                computer,
                ownerName,
                ownerDisplayName,
                ownerName,
                ownership.getValue(),
                name,
                type.getWebServiceObject(),
                GUID.EMPTY.getGUIDString(),
                null /* pendingChanges (don't double set) */));

        Check.notNull(pendingChanges, "pendingChanges"); //$NON-NLS-1$

        this.pendingChanges = pendingChanges.clone();

        this.candidatePendingChanges = candidatePendingChanges;
    }

    /**
     * Gets the web service object this class wraps. The returned object should
     * not be modified.
     *
     * @return the web service object this class wraps.
     */
    public _PendingSet getWebServiceObject() {
        /*
         * Update the changes in the wrapped object.
         */
        getWebServiceObjectInternal().setPendingChanges(
            (_PendingChange[]) WrapperUtils.unwrap(_PendingChange.class, pendingChanges));

        return getWebServiceObjectInternal();
    }

    /**
     * Gets the web service object but does not update it.
     *
     * Prefer this method internally.
     */
    private _PendingSet getWebServiceObjectInternal() {
        return (_PendingSet) webServiceObject;
    }

    public synchronized String getComputer() {
        return getWebServiceObjectInternal().getComputer();
    }

    public synchronized String getName() {
        return getWebServiceObjectInternal().getName();
    }

    public synchronized String getOwnerName() {
        return getWebServiceObjectInternal().getOwner();
    }

    public synchronized String getOwnerDisplayName() {
        return getWebServiceObjectInternal().getOwnerdisp();
    }

    public synchronized PendingSetType getType() {
        return PendingSetType.fromWebServiceObject(getWebServiceObjectInternal().getType());
    }

    public synchronized PendingChange[] getPendingChanges() {
        /*
         * Return the data from the cache field.
         */

        return pendingChanges;
    }

    public synchronized GUID getPendingChangeSignature() {
        return new GUID(getWebServiceObjectInternal().getSignature());
    }

    public synchronized PendingChange[] getCandidatePendingChanges() {
        return candidatePendingChanges;
    }

    /**
     * Sets the pending set name, owner and type on each of the pending changes
     */
    public synchronized void setPendingSetDetails() {
        // We need to tag PendingChange objects with PendingSetName and
        // PendingSetOwner
        for (final PendingChange pc : pendingChanges) {
            pc.setPendingSetName(getName());
            pc.setPendingSetOwner(getOwnerName());
            pc.setPendingSetOwnerDisplay(getOwnerDisplayName());
            pc.setInShelveset(getType() == PendingSetType.SHELVESET);
        }

        if (candidatePendingChanges != null) {
            for (final PendingChange pc : candidatePendingChanges) {
                pc.setPendingSetName(getName());
                pc.setPendingSetOwner(getOwnerName());
                pc.setPendingSetOwnerDisplay(getOwnerDisplayName());
                pc.setInShelveset(getType() == PendingSetType.SHELVESET);
            }
        }
    }
}
