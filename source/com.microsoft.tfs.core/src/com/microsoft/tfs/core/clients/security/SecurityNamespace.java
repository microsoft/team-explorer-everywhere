// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.security;

import com.microsoft.tfs.core.clients.webservices.IdentityDescriptor;

/**
 * Class for managing and enforcing security for a set of
 * AccessControlListDetailss.
 */
public abstract class SecurityNamespace {
    /**
     * @return a copy of the description for this SecurityNamespace.
     */
    public abstract SecurityNamespaceDescription getDescription();

    /**
     * Determines whether or not the supplied descriptor has the requested
     * permissions for the given token.
     *
     * @param token
     *        The security token to perform the check on
     * @param descriptor
     *        The descriptor to perform the security check for
     * @param requestedPermissions
     *        The permissions being requested
     * @param alwaysAllowAdministrators
     *        True if administrators should always be gratned these permissions
     * @return True if the supplied descriptor has the requested permissions for
     *         the given token. False otherwise
     */
    public abstract boolean hasPermission(
        String token,
        IdentityDescriptor descriptor,
        int requestedPermissions,
        boolean alwaysAllowAdministrators);

    /**
     *
     * Determines whether or not the supplied descriptor has the requested
     * permissions for the given tokens
     *
     * @param tokens
     *        The security tokens to perform the check on
     * @param descriptor
     *        The descriptor to perform the security check for
     * @param requestedPermissions
     *        The permissions being requested
     * @param alwaysAllowAdministrators
     *        The permissions being requested
     * @return A collection of booleans where a value of true indicates that the
     *         supplied descriptor has permission to the passed in token. Note
     *         that the returned collection will be the same size and in the
     *         same order as the passed in collection of tokens.
     */
    public abstract boolean[] hasPermission(
        String[] tokens,
        IdentityDescriptor descriptor,
        int requestedPermissions,
        boolean alwaysAllowAdministrators);

    /**
     * Determines whether or not the supplied descriptor have the requested
     * permissions for the given token.
     *
     * @param token
     *        The security token to perform the check on
     * @param descriptor
     *        The descriptor to perform the security check for
     * @param requestedPermissions
     *        The permissions being requested
     * @param alwaysAllowAdministrators
     *        The permissions being requested
     * @return A collection of booleans where a value of true indicates that the
     *         supplied descriptors has permission to the passed in token. Note
     *         that the returned collection will be the same size and in the
     *         same order as the passed in collection of descriptors.
     */
    public abstract boolean[] hasPermission(
        String token,
        IdentityDescriptor[] descriptors,
        int requestedPermissions,
        boolean alwaysAllowAdministrators);

    /**
     * Determines whether or not the supplied descriptor has the requested
     * permissions for the given token.
     *
     * @param token
     *        The security token to perform the check on.
     * @param descriptor
     *        The descriptor to perform the security check for.
     * @param requestedPermissions
     *        The permissions being requested.
     * @param alwaysAllowAdministrators
     *        True if administrators should always be gratned these permissions.
     * @return A collection of booleans where a value of true indicates that the
     *         supplied descriptors has permission to the passed in token. Note
     *         that the returned collection will be the same size and in the
     *         same order as the passed in collection of permissions.
     */
    public abstract boolean[] hasPermission(
        String token,
        IdentityDescriptor descriptor,
        int[] requestedPermissions,
        boolean alwaysAllowAdministrators);

    /**
     * Determines whether or not the current authorized user has permission to
     * change the permissions in permissionsToChange.
     *
     * @param token
     *        The token to check write permissions on.
     * @param permissionsToChange
     *        The permission bits that the authorized user may want to change.
     * @return True if the currently authorized user has permission to change
     *         the permissions in permissionsToChange.
     */
    public abstract boolean hasWritePermission(String token, int permissionsToChange);

    /**
     * Determines whether or not the current authorized user has permission to
     * write the permissions in permissionsToChange.
     *
     * @param token
     *        The token to check write permissions on.
     * @param permissionsToChange
     *        The permission bits that the authorized user may want to change.
     * @return A collection of booleans indicating whether or not the current
     *         authorized user has permission to change the bits passed in. Note
     *         that the returned collection will be the same size and in the
     *         same order as the passed in collection of permissions.
     */
    public abstract boolean[] hasWritePermission(String token, int[] permissionsToChange);

    /**
     * Removes the AccessControlListDetails for the specified token.
     *
     * @param token
     *        The token whose AccessControlListDetails is to be removed from
     *        this SecurityNamespace.
     * @param recurse
     *        True if all of the children below the provided token should also
     *        be removed.
     * @return True if something was removed. False otherwise.
     */
    public abstract boolean removeAccessControlLists(String token, boolean recurse);

    /**
     * Removes the AccessControlListDetails for the specified tokens.
     *
     * @param tokens
     *        The tokens whose AccessControlListDetailss are to be removed from
     *        this SecurityNamespace.
     * @param recurse
     *        True if all of the children below the provided tokens should also
     *        be removed.
     * @return True if something was removed. False otherwise.
     */
    public abstract boolean removeAccessControlLists(String[] tokens, boolean recurse);

    /**
     * Removes all of the AccessControlEntries for the given descriptors that
     * exist on the AccessControlListDetails that is associated with the
     * provided token. This function will not throw an exception if either the
     * token or descriptor cannot be found.
     *
     * @param token
     *        The token for the AccessControlListDetails on which to remove the
     *        AccessControlEntries.
     * @param descriptors
     *        Descriptors for the identities that should have their
     *        AccessControlEntryDetails removed.
     * @return True if something was removed.
     */
    public abstract boolean removeAccessControlEntries(String token, IdentityDescriptor[] descriptors);

    /**
     * Removes the AccessControlEntryDetails for the given descriptor that
     * exists on the AccessControlListDetails that is associated with the
     * provided token. This function will not throw an exception if either the
     * token or descriptor cannot be found.
     *
     * @param token
     *        The token for the AccessControlListDetails on which to remove the
     *        AccessControlEntryDetails.
     * @param descriptor
     *        Descriptor for the identity that should have its
     *        AccessControlEntryDetails removed.
     * @return True if something was removed.
     */
    public abstract boolean removeAccessControlEntry(String token, IdentityDescriptor descriptor);

    /**
     * Removes the specified permission bits from the existing allows and denys
     * for this descriptor. If no existing AccessControlEntryDetails is found
     * for this descriptor then nothing is done and an empty
     * AccessControlListDetails is returned. This function will not throw an
     * exception if either the token or descriptor cannot be found.
     *
     * @param token
     *        The token for the AccessControlListDetails to remove the
     *        permissions from.
     * @param descriptor
     *        The descriptor to remove the permissions for.
     * @param permissionsToRemove
     *        The permission bits to remove.
     * @return The updated AccessControlEntryDetails after removing the
     *         permissions.
     */
    public abstract AccessControlEntryDetails removePermissions(
        String token,
        IdentityDescriptor descriptor,
        int permissionsToRemove);

    /**
     * Sets a permission for the descriptor in this SecurityNamespace.
     *
     * @param token
     *        The token for the AccessControlListDetails to set the permissions
     *        on.
     * @param descriptor
     *        The descriptor to set the permissions for.
     * @param allow
     *        The allowed permissions to set.
     * @param deny
     *        The denied permissions to set.
     * @param merge
     *        If merge is true and a preexisting AccessControlEntryDetails for
     *        the descriptor is found the two permissions will be merged. When
     *        merging permissions, if there is a conflict, the new permissions
     *        will take precedence over the old permissions. If merge is false
     *        and a preexisting AccessControlEntryDetails for the descriptor is
     *        found it will be dropped and the passed in permissions will be the
     *        only permissions that remain for this descriptor on this
     *        AccessControlListDetails.
     * @return The new or updated AccessControlEnty that was set in the
     *         SecurityNamespace.
     */
    public abstract AccessControlEntryDetails setPermissions(
        String token,
        IdentityDescriptor descriptor,
        int allow,
        int deny,
        boolean merge);

    /**
     * Sets the provided AccessControlEntryDetails in this SecurityNamespace.
     *
     * @param token
     *        The token for the AccessControlListDetails to set the permissions
     *        on.
     * @param AccessControlEntryDetails
     *        The AccessControlEntryDetails to set in the SecurityNamespace.
     * @param merge
     *        If merge is true and a preexisting AccessControlEntryDetails for
     *        the descriptor is found the two permissions will be merged. When
     *        merging permissions, if there is a conflict, the new permissions
     *        will take precedence over the old permissions. If merge is false
     *        and a preexisting AccessControlEntryDetails for the descriptor is
     *        found it will be dropped and the passed in permissions will be the
     *        only permissions that remain for this descriptor on this
     *        AccessControlListDetails.
     * @return The new or updated permission that was set in the
     *         SecurityNamespace.
     */
    public abstract AccessControlEntryDetails setAccessControlEntry(
        String token,
        AccessControlEntryDetails AccessControlEntryDetails,
        boolean merge);

    /**
     * Sets the provided AccessControlEntries in this SecurityNamespace.
     *
     * @param token
     *        The token for the AccessControlListDetails to set the
     *        AccessControlEntryDetails on.
     * @param accessControlEntries
     *        The AccessControlEntries to set in the SecurityNamespace.
     * @param merge
     *        If merge is true and a preexisting AccessControlEntryDetails for
     *        the descriptor is found the two AccessControlEntries will be
     *        merged. When merging AccessControlEntries, if there is a conflict
     *        in permissions, the new permissions will take precedence over the
     *        old permissions. If merge is false and a preexisting
     *        AccessControlEntryDetails for the descriptor is found it will be
     *        dropped and the passed in AccessControlEntryDetails will be the
     *        only AccessControlEntryDetails that remain for this descriptor on
     *        this AccessControlListDetails.
     * @return The new or updated AccessControlEntires that were set in the
     *         SecurityNamespace.
     */
    public abstract AccessControlEntryDetails[] setAccessControlEntries(
        String token,
        AccessControlEntryDetails[] accessControlEntries,
        boolean merge);

    /**
     * Sets the AccessControlListDetails specified in the SecurityNamespace.
     * Setting an AccessControlListDetails will always overwrite an existing
     * AccessControlListDetails if one exists.
     *
     * @param AccessControlListDetails
     *        The AccessControlListDetails to set in the SecurityNamespace.
     */
    public abstract void setAccessControlList(AccessControlListDetails AccessControlListDetails);

    /**
     * Sets the AccessControlListDetailss specified in the SecurityNamespace.
     * Setting an AccessControlListDetails will always overwrite an existing
     * AccessControlListDetails if one exists.
     *
     * @param AccessControlListDetailss
     *        The AccessControlListDetailss to set in the SecurityNamespace.
     */
    public abstract void setAccessControlLists(AccessControlListDetails[] AccessControlListDetailss);

    /**
     * In all cases: This method will query the AccessControlListDetails for the
     * token specified. It will return AccessControlEntryDetails information for
     * the descriptors that are supplied or all descriptors if null is supplied
     * for the descriptors parameter.
     *
     * @param token
     *        The token for the AccessControlListDetails to query permissions
     *        for.
     * @param descriptors
     *        The descriptors that are to have permission information retrieved
     *        about. If this is left null, all descriptors will be considered.
     * @param includeExtendedInfo
     *        If includeExtendedInfo is false: All of the ExtendedInfo
     *        properties for the returned AccessControlEntryDetails objects will
     *        be null. If includeExtendedInfo is true: All of the ExtendedInfo
     *        properties for the returned AccessControlEntryDetails objects will
     *        contain references to valid AceExtendedInformation objects. If the
     *        descriptors parameter is null, this function will return
     *        AccessControlEntries for all descriptors that have explicit or
     *        inherited permissions on them.
     * @param recurse
     *        If recurse is true and this is a hierarchical namespace:
     *        Information about the tokens that exist below the specified token
     *        passed in the SecurityNamespace will be returned as well.
     * @return AccessControlListDetailss for the information passed in.
     */
    public abstract AccessControlListDetails[] queryAccessControlLists(
        String token,
        IdentityDescriptor[] descriptors,
        boolean includeExtendedInfo,
        boolean recurse);

    /**
     * In all cases: This method will query the AccessControlListDetails for the
     * token specified. It will return AccessControlEntryDetails information on
     * the descriptors that are supplied or all descriptors if null is supplied
     * for the descriptors parameter.
     *
     * @param token
     *        The token for the AccessControlListDetails to query permissions
     *        for.
     * @param descriptors
     *        The descriptors that are to have permission information retrieved
     *        about. If this is left null, all descriptors will be considered.
     * @param includeExtendedInfo
     *        If includeExtendedInfo is false: All of the ExtendedInfo
     *        properties for the returned AccessControlEntryDetails objects will
     *        be null. If includeExtendedInfo is true: All of the ExtendedInfo
     *        properties for the returned AccessControlEntryDetails objects will
     *        contain references to valid AceExtendedInformation objects. If the
     *        descriptors parameter is null, this function will return
     *        AccessControlEntries for all descriptors that have explicit or
     *        inherited permissions on them.
     * @return AccessControlListDetailss for the information passed in.
     */
    public abstract AccessControlListDetails queryAccessControlList(
        String token,
        IdentityDescriptor[] descriptors,
        boolean includeExtendedInfo);

    /**
     * Returns the effective allowed permissions for the given descriptor.
     *
     * @param token
     *        The token for the AccessControlListDetails we are querying
     *        permissions on.
     * @param descriptor
     *        The descriptor to query permissions for.
     * @return The effective allowed permissions for the descriptor.
     */
    public abstract int queryEffectivePermissions(String token, IdentityDescriptor descriptor);

    /**
     * Sets whether or not an AccessControlListDetails should inherit
     * permissions from its parents.
     *
     * @param token
     *        The token for the AccessControlListDetails to set the inherit flag
     *        on.
     * @param inherit
     *        True if it should inherit permissions.
     */
    public abstract void setInheritFlag(String token, boolean inherit);
}
