// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.eclipse.sync.resourcestore;

import org.eclipse.core.resources.IResource;

import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.GetOperation;
import com.microsoft.tfs.util.Check;

/**
 * This is a subclass of ResourceStore that exists solely to detect remote
 * renames in GetOperations. (See setOperation)
 */
public class GetOperationResourceStore extends ResourceStore<GetOperation> {
    /**
     * This sets the AGetOperation for a particular resource, with intelligent
     * handling for remote renames (wherein we get two AGetOperations for the
     * source of the rename.) Returns true if this operation was stored in the
     * resource store, returns false if it already exists OR if this operation
     * would clobber an AGetOperation with more information about what happened
     * to this resource on the remote end.
     *
     * @param resource
     *        the resource affected by the AGetOperation
     * @param operation
     *        the AGetOperation in question
     * @return true if the operation was stored, false if not
     */
    @Override
    public boolean addOperation(final IResource resource, final GetOperation operation) {
        Check.notNull(resource, "resource"); //$NON-NLS-1$
        Check.notNull(operation, "operation"); //$NON-NLS-1$

        final GetOperation oldGetOperation = getOperation(resource);

        if (oldGetOperation == null) {
            return super.addOperation(resource, operation);
        }

        if (oldGetOperation.equals(operation)) {
            return false;
        }

        /*
         * Pre-schema change servers may return two different GetOperations for
         * this resource on a rename. (one with source item = this, target item
         * = null; the other with source item = this, target item = that). We
         * only want to keep one, and that's the one with both source and target
         * item data. This allows us to resolve renames better.
         */
        if (operation.getCurrentLocalItem() != null
            && oldGetOperation.getCurrentLocalItem() != null
            && operation.getCurrentLocalItem().equals(oldGetOperation.getCurrentLocalItem())) {
            // the old operation in the cache actually has more data
            if (oldGetOperation.getTargetLocalItem() != null) {
                return false;
            }

            // this new contains the rename information, keep it...
            return super.addOperation(resource, operation);
        }

        // okay, it wasn't the source of a rename... i guess just pass it up...
        return super.addOperation(resource, operation);
    }
}
