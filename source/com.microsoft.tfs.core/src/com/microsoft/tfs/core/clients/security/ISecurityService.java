// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.security;

import com.microsoft.tfs.core.clients.security.exceptions.InvalidSecurityNamespaceDescriptionException;
import com.microsoft.tfs.core.clients.security.exceptions.SecurityNamespaceAlreadyExistsException;
import com.microsoft.tfs.util.GUID;

/**
 * An interface for managing collections of security namespaces.
 */
public interface ISecurityService {
    /**
     * Creates a {@link SecurityNamespace} that is based off of the provided
     * information.
     *
     *
     * @param description
     *        The description to create the namespace from.
     * @return the {@link SecurityNamespace} that was created
     * @throws InvalidSecurityNamespaceDescriptionException
     *         if the {@link SecurityNamespaceDescription} does not pass
     *         validation for some reason
     * @throws SecurityNamespaceAlreadyExistsException
     *         if a {@link SecurityNamespace} already exists with id of the
     *         description passed in
     */
    SecurityNamespace createSecurityNamespace(SecurityNamespaceDescription description)
        throws InvalidSecurityNamespaceDescriptionException,
            SecurityNamespaceAlreadyExistsException;

    /**
     *
     * Deletes the SecurityNamespace from the collection of
     * {@link SecurityNamespace}s.
     *
     * @param namespaceId
     *        The id of the {@link SecurityNamespace} to delete
     * @return True if something was deleted
     */
    boolean deleteSecurityNamespace(GUID namespaceId);

    /**
     * Returns the {@link SecurityNamespace} associated with this id.
     * <code>null</code> is returned if a SecurityNamespace with this id does
     * not exist.
     *
     * @param namespaceId
     *        The id for the {@link SecurityNamespace} desired
     * @return {@link SecurityNamespace} associated with this id.
     *         <code>null</code> is returned if a {@link SecurityNamespace} with
     *         this id does not exist
     */
    SecurityNamespace getSecurityNamespace(GUID namespaceId);

    /**
     * @return An enumeration of all of the {@link SecurityNamespace}s that
     *         exist as a part of this Security Service.
     */
    SecurityNamespace[] getSecurityNamespaces();
}