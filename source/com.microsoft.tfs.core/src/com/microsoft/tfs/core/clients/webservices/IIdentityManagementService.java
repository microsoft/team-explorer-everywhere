// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.webservices;

import com.microsoft.tfs.util.GUID;

/**
 * @since TEE-SDK-11.0
 */
public interface IIdentityManagementService {
    /**
     * Read identities for given descriptors. First try IMS store. If not found,
     * optionally try source like AD. Note - performance will be fastest when no
     * membership information is requested.
     *
     * @param descriptors
     *        descriptors (descriptor is identity type + identifier)
     * @param queryMembership
     *        none, direct or expanded membership information
     * @param readOptions
     *        read options, such as reading from source
     * @return Array of identities, corresponding 1 to 1 with input descriptor
     *         array.
     */
    TeamFoundationIdentity[] readIdentities(
        IdentityDescriptor[] descriptors,
        MembershipQuery queryMembership,
        ReadIdentityOptions readOptions);

    /**
     * Overload that takes a single descriptor. Read identity for given
     * descriptor. First try IMS store. If not found, optionally try source like
     * AD. Note - performance will be fastest when no membership information is
     * requested.
     *
     * @param descriptor
     *        identity type + identifier
     * @param queryMembership
     *        none, direct or expanded membership information
     * @param readOptions
     *        read options, such as reading from source
     * @return identity if found, else null
     */
    TeamFoundationIdentity readIdentity(
        IdentityDescriptor descriptor,
        MembershipQuery queryMembership,
        ReadIdentityOptions readOptions);

    /**
     * Read identities by Team Foundation Id. Note - performance will be fastest
     * when no membership information is requested.
     *
     * @param teamFoundationIds
     *        identity ids
     * @param queryMembership
     *        none, direct or expanded membership information
     * @return Array of identities, corresponding 1 to 1 with input array
     */
    TeamFoundationIdentity[] readIdentities(GUID[] teamFoundationIds, MembershipQuery queryMembership);

    /**
     * Read identities based on search factor. First read from IMS store, then
     * (optionally) read from source like AD.
     *
     * @param searchFactor
     *        how search is specified (by account name, etc.)
     * @param searchFactorValues
     *        actual search strings (account names, etc.)
     * @param queryMembership
     *        none, direct or expanded membership information
     * @param readOptions
     *        readOptions, such as reading from source
     * @return Arrays of identities. Inner array corresponds 1 to 1 with search
     *         factor values
     */
    TeamFoundationIdentity[][] readIdentities(
        IdentitySearchFactor searchFactor,
        String[] searchFactorValues,
        MembershipQuery queryMembership,
        ReadIdentityOptions readOptions);

    /**
     * Overload that takes a single search factor and returns match following
     * this order. 1. With multiple matches, active identity if exists, else
     * first match. 2. When there is a single match, the match. 3. When there is
     * no match, null.
     *
     * Read identity based on search factor. First read from IMS store, then
     * (optionally) read from source like AD.
     *
     * @param searchFactor
     *        how search is specified (by account name, etc.)
     * @param searchFactorValue
     *        actual search string (account name, etc.)
     * @param queryMembership
     *        none, direct or expanded membership information
     * @param readOptions
     *        readOptions, such as reading from source
     * @return Array of matching identities
     */
    TeamFoundationIdentity readIdentity(
        IdentitySearchFactor searchFactor,
        String searchFactorValue,
        MembershipQuery queryMembership,
        ReadIdentityOptions readOptions);

    /**
     * Creates a TFS application group
     *
     * @param projectUri
     *        Scope Uri, specifying whether group scope is project level or
     *        global to this host. Null or empty value signifies global scope
     * @param groupName
     *        name
     * @param groupDescription
     *        description. can be null
     * @return IdentityDescriptor of the created group
     */
    IdentityDescriptor createApplicationGroup(String projectUri, String groupName, String groupDescription);

    /**
     * Lists all TFS application groups within the specified scope
     *
     * @param projectUri
     *        Scope Uri, specifying whether group scope is project level or
     *        global to this host. Null or empty value signifies global scope
     * @param readOptions
     *        read options
     * @return Application groups as an array of identities
     */
    TeamFoundationIdentity[] listApplicationGroups(String projectUri, ReadIdentityOptions readOptions);

    /**
     * Updates a property of a TFS application group
     *
     * @param groupDescriptor
     * @param groupProperty
     *        which property to update
     * @param newValue
     *        the new value for the property
     */
    void updateApplicationGroup(IdentityDescriptor groupDescriptor, GroupProperty groupProperty, String newValue);

    /**
     * Deletes a TFS application group
     *
     * @param groupDescriptor
     *        groupDescriptor
     *
     */
    void deleteApplicationGroup(IdentityDescriptor groupDescriptor);

    /**
     * Add member to TFS Group. </summary>
     *
     * @param groupDescriptor
     *        groupDescriptor
     * @param descriptor
     *        member
     */
    void addMemberToApplicationGroup(IdentityDescriptor groupDescriptor, IdentityDescriptor descriptor);

    /**
     * Remove member from TFS Group. </summary>
     *
     * @param groupDescriptor
     *        groupDescriptor
     * @param descriptor
     *        member
     */
    void removeMemberFromApplicationGroup(IdentityDescriptor groupDescriptor, IdentityDescriptor descriptor);

    /**
     * Expanded membership query for direct or nested member. </summary>
     *
     * @param groupDescriptor
     *        group
     * @param descriptor
     *        member
     */
    boolean isMember(IdentityDescriptor groupDescriptor, IdentityDescriptor descriptor);

    /**
     * Refresh identity properties from provider now. This identity must already
     * be in the IMS store. If identity is a group, its DIRECT members and their
     * properties will also be refreshed (asynchronously).
     * <p>
     * Identity properties, such as display name, are synced from the provider
     * over a 24h cylce, by default. Use this API to sync now.
     *
     * @param descriptor
     *        identity specification. Null implies caller
     * @return True if identity is in IMS (in which case will be refreshed now),
     *         else false.
     */
    boolean refreshIdentity(IdentityDescriptor descriptor);

    /**
     * Gets the scope name for the provided scope id.
     *
     * @param scopeId
     *        scope id, which is the project or domain Uri
     * @return The scope name.
     */
    String getScopeName(String scopeId);

    /**
     * Check if the given descriptor is of TeamFoundation type, and belongs to
     * this IMS host. This does not confirm that such a group actually exists,
     * just that the Sid pattern belongs to this host.
     *
     * @param descriptor
     * @return True if owner, else false.
     */
    boolean isOwner(IdentityDescriptor descriptor);

    /**
     * Check if the given descriptor is of TeamFoundation type, belongs to this
     * IMS host and is a well-known group. This does not confirm that such a
     * group actually exists, just that the Sid pattern meets these
     * requirements.
     *
     * @param descriptor
     * @return True if owned and is well-known group.
     */
    boolean isOwnedWellKnownGroup(IdentityDescriptor descriptor);

    /**
     * @return Return the Scope Uri for global Groups in the domain
     */
    String getIdentityDomainScope();
}