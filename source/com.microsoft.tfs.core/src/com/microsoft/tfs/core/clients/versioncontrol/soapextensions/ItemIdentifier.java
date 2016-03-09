// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.soapextensions;

import com.microsoft.tfs.core.clients.versioncontrol.path.ServerPath;
import com.microsoft.tfs.core.clients.versioncontrol.specs.version.VersionSpec;
import com.microsoft.tfs.core.internal.wrappers.WebServiceObjectWrapper;

import ms.tfs.versioncontrol.clientservices._03._ChangeType;
import ms.tfs.versioncontrol.clientservices._03._ChangesetVersionSpec;
import ms.tfs.versioncontrol.clientservices._03._ItemIdentifier;

/**
 * <p>
 * Identifies a server item at a given version, with an optional associated
 * change (which is not strictly part of the identity).
 * </p>
 * <p>
 * {@link #equals(Object)} and {@link #hashCode()} do not consider this item's
 * change type (accessible from {@link #getChangeType()}). This behavior makes
 * this class useful for comparing against other {@link ItemIdentifier}s queried
 * from the server at different times: the change type may change, but the same
 * item is being identified.
 * </p>
 *
 * @threadsafety immutable
 * @since TEE-SDK-10.1
 */
public class ItemIdentifier extends WebServiceObjectWrapper {
    /**
     * Creates a {@link ItemIdentifier} from a web service object.
     *
     * @param webServiceObject
     *        the web service object (must not be <code>null</code>)
     */
    public ItemIdentifier(final _ItemIdentifier webServiceObject) {
        super(webServiceObject);
    }

    /**
     * Gets the web service object this class wraps. The returned object should
     * not be modified.
     *
     * @return the web service object this class wraps.
     */
    public _ItemIdentifier getWebServiceObject() {
        return (_ItemIdentifier) webServiceObject;
    }

    /**
     * Creates a {@link ItemIdentifier} from a {@link Change}. All fields will
     * be initialized from the given change (including version and change type).
     *
     * @param change
     *        the change to create an identifier for (must not be
     *        <code>null</code>)
     */
    public ItemIdentifier(final Change change) {
        /*
         * The web service object only stores an integer for change type, which
         * contains the normal change type fields as well as the extended ones.
         * I'm not sure if Visual Studio's design only needs to store the
         * extended ones, but we store them all here.
         */
        this(
            new _ItemIdentifier(
                change.getItem().getServerItem(),
                change.getItem().getDeletionID(),
                change.getChangeType().toIntFlags(),
                new _ChangesetVersionSpec(change.getItem().getChangeSetID())));
    }

    /**
     * Creates a {@link ItemIdentifier} for the given server path. The version
     * and change type will be null.
     *
     * @param serverPath
     *        the server path (must not be <code>null</code> or empty)
     */
    public ItemIdentifier(final String serverPath) {
        this(serverPath, 0);
    }

    /**
     * Creates a {@link ItemIdentifier} for the given server path and deletion
     * ID. The version and change type will be null.
     *
     * @param serverPath
     *        the server path (must not be <code>null</code> or empty)
     * @param deletionID
     *        the deletion ID (0 for none)
     */
    public ItemIdentifier(final String serverPath, final int deletionID) {
        this(serverPath, null, deletionID);
    }

    /**
     * Creates a {@link ItemIdentifier} for the given server path at the given
     * version with the given deletion ID. The change type will be null.
     *
     * @param serverPath
     *        the server path (must not be <code>null</code> or empty)
     * @param version
     *        the version of the item (may be null)
     * @param deletionID
     *        the deletion ID (0 for none)
     */
    public ItemIdentifier(final String serverPath, final VersionSpec version, final int deletionID) {
        this(
            new _ItemIdentifier(
                serverPath,
                deletionID,
                ChangeType.NONE.toIntFlags(),
                version == null ? null : version.getWebServiceObject()));
    }

    /**
     * @return the server path of this item, never <code>null</code> or empty
     */
    public String getItem() {
        return getWebServiceObject().getIt();
    }

    /**
     * @return the version of the item, may be null
     */
    public VersionSpec getVersion() {
        return VersionSpec.fromWebServiceObject(getWebServiceObject().getVersion());
    }

    /**
     * @return the deletion ID for this item
     */
    public int getDeletionID() {
        return getWebServiceObject().getDi();
    }

    /**
     * @return the change type for this item, may be null
     */
    public ChangeType getChangeType() {
        /*
         * The web service object stores all the change type flags, not just the
         * extended ones, so we can just use the constructor for ChangeType
         * which ORs the values together to get the correct change type
         * (including non-extended flags).
         */
        return new ChangeType(new _ChangeType(), getWebServiceObject().getCtype());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(final Object o) {
        if (o == this) {
            return true;
        }

        if (o == null) {
            return false;
        }

        if (o instanceof ItemIdentifier == false) {
            return false;
        }

        final ItemIdentifier other = (ItemIdentifier) o;

        if (ServerPath.equals(getItem(), other.getItem()) == false) {
            return false;
        }

        if (getDeletionID() != other.getDeletionID()) {
            return false;
        }

        final VersionSpec thisVersion = getVersion();
        final VersionSpec otherVersion = other.getVersion();

        if (thisVersion != null || otherVersion != null) {
            if (thisVersion == null || otherVersion == null) {
                return false;
            }

            /*
             * Visual Studio's code compares by string, which seems odd.
             */
            if (thisVersion.toString().equals(otherVersion.toString()) == false) {
                return false;
            }
        }

        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        int result = 17;

        result = result * 37 + getItem().hashCode();
        result = result * 37 + getDeletionID();
        result = result * 37 + ((getVersion() == null) ? 0 : getVersion().hashCode());

        /*
         * Don't include changetype (see class Javadoc).
         */

        return result;
    }
}
