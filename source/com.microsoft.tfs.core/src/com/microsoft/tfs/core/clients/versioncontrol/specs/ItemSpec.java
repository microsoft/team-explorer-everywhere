// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.specs;

import java.text.MessageFormat;

import com.microsoft.tfs.core.Messages;
import com.microsoft.tfs.core.clients.versioncontrol.path.ItemPath;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.PendingChange;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.RecursionType;
import com.microsoft.tfs.core.internal.wrappers.WebServiceObjectWrapper;
import com.microsoft.tfs.util.Check;

import ms.tfs.versioncontrol.clientservices._03._ItemSpec;

/**
 * Describes a repository object, using either a local or server path, including
 * an optional deletion ID identifier and a recursion type. It is not generally
 * converted to/from string representation.
 *
 * @since TEE-SDK-10.1
 */
public class ItemSpec extends WebServiceObjectWrapper {
    public ItemSpec() {
        super(new _ItemSpec());
    }

    public ItemSpec(final _ItemSpec spec) {
        super(spec);
    }

    /**
     * Constructs an instance with the following parameters.
     *
     * @param item
     *        a local or server path.
     * @param recurse
     *        the recursion type.
     * @param did
     *        a deletion ID.
     */
    public ItemSpec(final String item, final RecursionType recurse, final int did) {
        super(
            new _ItemSpec(
                ItemPath.smartNativeToTFS(item),
                recurse == null ? null : recurse.getWebServiceObject(),
                did));
    }

    /**
     * Constructs an instance with the following parameters.
     *
     * @param item
     *        a local or server path.
     * @param recurse
     *        the recursion type.
     */
    public ItemSpec(final String item, final RecursionType recurse) {
        this(item, recurse, 0);
    }

    /**
     * Convenience constructor to build an {@link ItemSpec} from a
     * {@link PendingChange}.
     *
     * @param pendingChange
     *        the {@link PendingChange} to build the {@link ItemSpec} from (must
     *        not be <code>null</code>)
     */
    public ItemSpec(final PendingChange pendingChange) {
        this(pendingChange.getServerItem(), RecursionType.NONE, pendingChange.getDeletionID());
    }

    /**
     * Gets the web service object this class wraps. The returned object should
     * not be modified.
     *
     * @return the web service object this class wraps.
     */
    public _ItemSpec getWebServiceObject() {
        return (_ItemSpec) webServiceObject;
    }

    /**
     * Create item specs from strings. Item paths and deletion IDs are read from
     * the strings, the given recursion type is applied to all specs. Version
     * specs (other than deletion) encountered parsing the strings will result
     * in a run-time exception.
     *
     * @param localOrServerItems
     *        the local or server paths to use (must not be <code>null</code>)
     * @param recursion
     *        the type of recursion to use for each item spec (must not be
     *        <code>null</code>)
     * @return the item specs created from the given paths.
     */
    public static ItemSpec[] fromStrings(final String[] localOrServerItems, final RecursionType recursion) {
        Check.notNull(localOrServerItems, "localOrServerItems"); //$NON-NLS-1$
        Check.notNull(recursion, "recursion"); //$NON-NLS-1$

        final ItemSpec[] ret = new ItemSpec[localOrServerItems.length];
        for (int i = 0; i < localOrServerItems.length; i++) {
            /*
             * We use AVersionedFileSpec because it will handle parsing
             * deletions for us. It will also parse other version specs out,
             * which we'll error on if we find.
             */
            VersionedFileSpec vfs = null;

            // The username "bogus" will not be used.
            vfs = VersionedFileSpec.parse(localOrServerItems[i], "bogus", false); //$NON-NLS-1$

            if (vfs == null) {
                throw new ItemSpecParseException(
                    MessageFormat.format(
                        Messages.getString("ItemSpec.ErrorParsingStringAsItemSpecFormat"), //$NON-NLS-1$
                        localOrServerItems[i]));
            }

            if (vfs.getVersions() != null && vfs.getVersions().length > 0) {
                throw new ItemSpecParseException(
                    Messages.getString("ItemSpec.VersionsMayNotBeSpecifiedInSimpleItemSpecs")); //$NON-NLS-1$
            }

            final int did = (vfs.getDeletionVersionSpec() != null) ? vfs.getDeletionVersionSpec().getDeletionID() : 0;

            ret[i] = new ItemSpec(vfs.getItem(), recursion, did);
        }

        return ret;
    }

    public int getDeletionID() {
        return getWebServiceObject().getDid();
    }

    public String getItem() {
        return ItemPath.smartTFSToNative(getWebServiceObject().getItem());
    }

    public RecursionType getRecursionType() {
        return RecursionType.fromWebServiceObject(getWebServiceObject().getRecurse());
    }

    public void setDeletionID(final int did) {
        getWebServiceObject().setDid(did);
    }

    public void setItem(final String item) {
        getWebServiceObject().setItem(ItemPath.smartNativeToTFS(item));
    }

    public void setRecursionType(final RecursionType recurse) {
        getWebServiceObject().setRecurse(recurse.getWebServiceObject());
    }

    @Override
    public String toString() {
        return MessageFormat.format(
            "{0}:{1}", //$NON-NLS-1$
            getWebServiceObject().getItem(),
            (getWebServiceObject().getRecurse() != null ? getWebServiceObject().getRecurse().getName() : "null")); //$NON-NLS-1$
    }
}
