// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.webservices;

import com.microsoft.tfs.util.GUID;

/**
 * @since TEE-SDK-11.0
 */
public interface IIdentityManagementService2 extends IIdentityManagementService {
    /**
     * Get the set of identities which have been recently accessed by the
     * current user
     */
    TeamFoundationIdentity[] getMostRecentlyUsedUsers();

    /**
     * Adds the specified identity to MRU list of users for the current user.
     */
    void addRecentUser(TeamFoundationIdentity identity);

    /**
     * Read an identity using the General search factor.
     * <p>
     * Equivalent to ReadIdentity(IdentitySearchFactor.General,
     * generalSearchValue, MembershipQuery.None, ReadIdentityOptions.None).
     * <p>
     * You can use this to find an identity by one of the following properties:
     * <ul>
     * <li>Display name</li>
     * <li>account name</li>
     * <li>UniqueName</li>
     * </ul>
     * UniqueName may be easier to type than display name. It can also be used
     * to find a single identity when two or more identities share the same
     * display name (e.g. "John Smith")
     *
     * @param generalSearchValue
     *        The search string
     * @return
     */
    TeamFoundationIdentity readIdentity(String generalSearchValue);

    /**
     * ReadFilteredIdentities is used to retrieve a set of identities based on
     * an expression. The expression is a syntax that resembles a SQL WHERE
     * clause. For full details on the expressions capabilities see
     * documentation on the QueryExpression class.
     *
     * @param expression
     * @param suggestedPageSize
     * @param lastSearchResult
     * @param lookForward
     * @param queryMembership
     * @return
     */
    FilteredIdentitiesList readFilteredIdentities(
        String expression,
        int suggestedPageSize,
        String lastSearchResult,
        boolean lookForward,
        int queryMembership);

    /**
     * Sets the display name for the current user in a sticky manner, overriding
     * any display name returned by an external identity provider (Active
     * Directory, Live, etc).
     * <p>
     * TFS 2010 would automatically disambiguate users with the same display
     * name by appending the domain and account name. TFS 2012 does not
     * disambiguate display names. SetCustomDisplayName can be used instead to
     * make a display name unique.
     * <p>
     * "John Q. Smith, Sr."
     * <p>
     * "John Smith (Contoso, Human Resources)"
     *
     * @param customDisplayName
     *        The new display name
     */
    void setCustomDisplayName(String customDisplayName);

    /**
     * Clears the custom display name for the current user, returning to using
     * the display name from the external identity provider (Active Directory,
     * Live, etc).
     */
    void clearCustomDisplayName();

    // new overloads for updating and querying extended properties.

    /**
     * Save changes to extended properties.
     *
     * @param identity
     *        Identity with extended property changes
     */
    void updateExtendedProperties(TeamFoundationIdentity identity);

    /**
     * Read identities for given descriptors. First try IMS store. If not found,
     * optionally try source like AD. Note - performance will be fastest when no
     * membership information is requested.
     *
     *
     * @param descriptors
     *        descriptors (descriptor is identity type + identifier)
     * @param queryMembership
     *        none, direct or expanded membership information
     * @param readOptions
     *        read options, such as reading from source
     * @param propertyNameFilters
     * @param propertyScope
     * @return Array of identities, corresponding 1 to 1 with input descriptor
     *         array
     */
    TeamFoundationIdentity[] readIdentities(
        IdentityDescriptor[] descriptors,
        MembershipQuery queryMembership,
        ReadIdentityOptions readOptions,
        String[] propertyNameFilters,
        IdentityPropertyScope propertyScope);

    /**
     * Overload that takes a single descriptor. Read identity for given
     * descriptor. First try IMS store. If not found, optionally try source like
     * AD. Note - performance will be fastest when no membership information is
     * requested.
     *
     *
     * @param descriptor
     *        identity type + identifier
     * @param queryMembership
     *        none, direct or expanded membership information
     * @param readOptions
     *        read options, such as reading from source
     * @param propertyNameFilters
     * @param propertyScope
     * @return identity if found, else null
     */
    TeamFoundationIdentity readIdentity(
        IdentityDescriptor descriptor,
        MembershipQuery queryMembership,
        ReadIdentityOptions readOptions,
        String[] propertyNameFilters,
        IdentityPropertyScope propertyScope);

    /**
     * Read identities by Team Foundation Id. Note - performance will be fastest
     * when no membership information is requested.
     *
     * @param teamFoundationIds
     *        identity ids
     * @param queryMembership
     *        none, direct or expanded membership information
     * @param readOptions
     * @param propertyNameFilters
     * @param propertyScope
     * @return Array of identities, corresponding 1 to 1 with input array
     */
    TeamFoundationIdentity[] readIdentities(
        GUID[] teamFoundationIds,
        MembershipQuery queryMembership,
        ReadIdentityOptions readOptions,
        String[] propertyNameFilters,
        IdentityPropertyScope propertyScope);

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
     *
     */
    TeamFoundationIdentity[][] readIdentities(
        IdentitySearchFactor searchFactor,
        String[] searchFactorValues,
        MembershipQuery queryMembership,
        ReadIdentityOptions readOptions,
        String[] propertyNameFilters,
        IdentityPropertyScope propertyScope);

    /**
     * Overload that takes a single search factor and returns match following
     * this order.
     * <ol>
     * <li>With multiple matches, active identity if exists, else first match.
     * </li>
     * <li>When there is a single match, the match.</li>
     * <li>When there is no match, null.</li>
     * </ol>
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
        ReadIdentityOptions readOptions,
        String[] propertyNameFilters,
        IdentityPropertyScope propertyScope);

    /**
     * Lists all TFS application groups within the specified scope
     *
     * @param scopeId
     *        Scope Uri, specifying whether group scope is project level or
     *        global to this host. Null or empty value signifies global scope
     * @param readOptions
     *        read options
     * @param propertyNameFilters
     *        extended properties to retrieve with application groups
     * @param propertyScope
     *        indicates where to read extended properties from
     * @return Application groups as an array of identities
     */
    TeamFoundationIdentity[] listApplicationGroups(
        String scopeId,
        ReadIdentityOptions readOptions,
        String[] propertyNameFilters,
        IdentityPropertyScope propertyScope);
}