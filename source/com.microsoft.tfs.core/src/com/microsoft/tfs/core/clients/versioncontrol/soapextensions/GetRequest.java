// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.soapextensions;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.microsoft.tfs.core.clients.versioncontrol.VersionControlClient;
import com.microsoft.tfs.core.clients.versioncontrol.Workstation;
import com.microsoft.tfs.core.clients.versioncontrol.path.LocalPath;
import com.microsoft.tfs.core.clients.versioncontrol.specs.ItemSpec;
import com.microsoft.tfs.core.clients.versioncontrol.specs.version.VersionSpec;
import com.microsoft.tfs.core.internal.wrappers.WebServiceObjectWrapper;
import com.microsoft.tfs.util.Check;

import ms.tfs.versioncontrol.clientservices._03._GetRequest;

/**
 * An item at a speicific version that the user wishes to get from the server.
 *
 * @since TEE-SDK-10.1
 */
public final class GetRequest extends WebServiceObjectWrapper {
    /**
     * @param itemSpec
     *        the item to get (may be null to support the case of getting all
     *        items in the current workspace).
     * @param versionSpec
     *        the version of the item to get.
     */
    public GetRequest(final ItemSpec itemSpec, final VersionSpec versionSpec) {
        super(
            new _GetRequest(
                (itemSpec != null) ? itemSpec.getWebServiceObject() : null,
                versionSpec.getWebServiceObject()));
    }

    /**
     * Gets the web service object this class wraps. The returned object should
     * not be modified.
     *
     * @return the web service object this class wraps.
     */
    public _GetRequest getWebServiceObject() {
        return (_GetRequest) webServiceObject;
    }

    public ItemSpec getItemSpec() {
        if (getWebServiceObject().getItemSpec() == null) {
            return null;
        } else {
            return new ItemSpec(getWebServiceObject().getItemSpec());
        }
    }

    public void setItemSpec(final ItemSpec itemSpec) {
        Check.notNull(itemSpec, "itemSpec"); //$NON-NLS-1$

        getWebServiceObject().setItemSpec(itemSpec.getWebServiceObject());
    }

    public VersionSpec getVersionSpec() {
        return VersionSpec.fromWebServiceObject(getWebServiceObject().getVersionSpec());
    }

    public void setVersionSpec(final VersionSpec versionSpec) {
        Check.notNull(versionSpec, "versionSpec"); //$NON-NLS-1$

        getWebServiceObject().setVersionSpec(versionSpec.getWebServiceObject());
    }

    @Override
    public String toString() {
        final _GetRequest request = getWebServiceObject();

        final String item = ((request.getItemSpec() != null) ? request.getItemSpec().getItem() : "<null>"); //$NON-NLS-1$
        final String recursionType = ((request.getItemSpec() != null && request.getItemSpec().getRecurse() != null)
            ? request.getItemSpec().getRecurse().getName() : "null"); //$NON-NLS-1$

        return MessageFormat.format("{0}: {1}", item, recursionType); //$NON-NLS-1$
    }

    /**
     * Take a list of file paths (for which no recursion is desired) and try to
     * determine the optimal set of parent directories that would contain those
     * paths if the directories were recursed. Note that this mechanism also has
     * the advantage of tracking renames if the source and target are in the
     * same folder.
     *
     * @param fileSpecs
     *        the local item paths to get (must not be <code>null</code>)
     * @param version
     *        the version of the items to get (must not be <code>null</code>)
     * @return the list of {@link GetRequest} objects to pass to the client get
     *         API
     */
    public static GetRequest[] createOptimizedRequests(
        final VersionControlClient client,
        final String[] fileSpecs,
        final VersionSpec version) {
        Check.notNull(client, "client"); //$NON-NLS-1$
        Check.notNull(fileSpecs, "fileSpecs"); //$NON-NLS-1$
        Check.notNull(version, "version"); //$NON-NLS-1$

        final List<GetRequest> retval = new ArrayList<GetRequest>(fileSpecs.length);
        if (fileSpecs.length > 0) {
            /*
             * Sort array in top down fashion This will throw if any of the
             * names are invalid; however, they should already be canonicalized
             * local paths.
             */
            final String[] localFilePaths = new String[fileSpecs.length];

            System.arraycopy(fileSpecs, 0, localFilePaths, 0, fileSpecs.length);
            Arrays.sort(localFilePaths, LocalPath.TOP_DOWN_COMPARATOR);

            /*
             * Search top down and come up with minimum set of parent
             * directories to recurse
             */
            String currentParent = null;
            int folderDepthOfLastItem = -1;
            GetRequest lastRequestAdded = null;
            for (final String filepath : localFilePaths) {
                final int folderDepth = LocalPath.getFolderDepth(filepath);
                if ((currentParent == null) || !LocalPath.isChild(currentParent, filepath)) {
                    // ADD NEW ELEMENT TO OUTPUT LIST:
                    GetRequest request;

                    // currentParent can be null if the filepath has no parent
                    // (drive or filesystem root)
                    currentParent = LocalPath.getParent(filepath);

                    if (currentParent != null
                        && Workstation.getCurrent(client.getConnection().getPersistenceStoreProvider()).isMapped(
                            currentParent)) {
                        // The parent is mapped, so we add it
                        request = new GetRequest(new ItemSpec(currentParent, RecursionType.ONE_LEVEL), version);
                    } else {
                        /*
                         * The parent is not mapped. Add this item rather than
                         * its parent. Reset current parent to null, so that we
                         * reenter the "ADD NEW ELEMENT TO OUTPUT LIST" block
                         * and bypass the "CHECK FOR NEED FOR FULL RECURSION" on
                         * the next iteration
                         */
                        request = new GetRequest(new ItemSpec(filepath, RecursionType.NONE), version);
                        currentParent = null;
                    }
                    lastRequestAdded = request;
                    retval.add(request);
                } else {
                    if (folderDepth != folderDepthOfLastItem) {
                        /*
                         * CHECK FOR NEED FOR FULL RECURSION: Differing folder
                         * depths for items that are children of the same
                         * request means that we need to recurse more than one
                         * level
                         */
                        lastRequestAdded.getItemSpec().setRecursionType(RecursionType.FULL);
                    }
                }
                folderDepthOfLastItem = folderDepth;
            }
        }

        return retval.toArray(new GetRequest[retval.size()]);
    }
}
