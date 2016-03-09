// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.security;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.microsoft.tfs.core.clients.security.internal.SecurityUtility;
import com.microsoft.tfs.core.clients.security.internal.SecurityUtility.MergePermissionsResult;
import com.microsoft.tfs.core.clients.webservices.ACEExtendedInformation;
import com.microsoft.tfs.core.clients.webservices.IdentityDescriptor;
import com.microsoft.tfs.core.clients.webservices.IdentityDescriptorComparer;
import com.microsoft.tfs.core.internal.wrappers.WebServiceObjectWrapper;
import com.microsoft.tfs.util.Check;

import ms.ws._AccessControlList;

/**
 * The AccessControlList class is meant to associate a set of
 * AccessControlEntries with a security token and its inheritance settings. It
 * is important to note that the AccessControlList class does not talk to a web
 * service when its methods are called. It provides a staging area for changes
 * to the AccessControlEntries for a secured token. Once changes are made to an
 * AccessControlList it can be saved to the web service by passing it into a
 * SecurityNamespace.
 */
public class AccessControlList extends WebServiceObjectWrapper {
    /**
     * Storage of permissions keyed on the identity the permission is for.
     */
    private final Map<IdentityDescriptor, AccessControlEntryDetails> accessControlEntries =
        new TreeMap<IdentityDescriptor, AccessControlEntryDetails>(IdentityDescriptorComparer.INSTANCE);

    /**
     * Keeps track of whether or not this ACL should include extended info when
     * returning ACEs
     */
    private boolean includeExtendedInfoForAces = false;

    public AccessControlList() {
        this(null, false);
    }

    public AccessControlList(final _AccessControlList webServiceObject) {
        super(webServiceObject);
    }

    /**
     * <p>
     * Creates a new AccessControlList
     * </p>
     *
     * @param token
     *        The token that this AccessControlList is for.
     * @param inherit
     *        True if this AccessControlList should inherit permissions from its
     *        parents.
     */
    public AccessControlList(final String token, final boolean inherit) {
        this(token, inherit, null);
    }

    /**
     * <p>
     * Builds an instance of an AccessControlList
     * </p>
     *
     * @param token
     *        The token that this AccessControlList is for.
     * @param inherit
     *        True if this AccessControlList should inherit permissions from its
     *        parents.
     * @param accessControlEntries
     *        The list of AccessControlEntries that apply to this
     *        AccessControlList.
     */
    public AccessControlList(
        final String token,
        final boolean inherit,
        final AccessControlEntryDetails[] accessControlEntries) {
        super(new _AccessControlList(inherit, token));
        setAccessControlEntries(accessControlEntries, false);
    }

    /**
     * <p>
     * Builds an instance of an AccessControlList
     * </p>
     *
     * @param existingList
     *        The AccessControlList to take its data from.
     */
    public AccessControlList(final AccessControlList existingList) {
        this(existingList.getToken(), existingList.isInheritPermissions(), existingList.getAccessControlEntries());
    }

    /**
     * Gets the web service object this class wraps. The returned object should
     * not be modified.
     *
     * @return the web service object this class wraps.
     */
    public _AccessControlList getWebServiceObject() {
        return (_AccessControlList) webServiceObject;
    }

    /**
     * @return True if this AccessControlList inherits permissions from parents.
     */
    public boolean isInheritPermissions() {
        return getWebServiceObject().isInheritPermissions();
    }

    public void setInheritPermissions(final boolean value) {
        getWebServiceObject().setInheritPermissions(value);
    }

    /**
     * @return The token that this AccessControlList is for.
     */
    public String getToken() {
        return getWebServiceObject().getToken();
    }

    public void setToken(final String token) {
        getWebServiceObject().setToken(token);
    }

    /**
     * The list of AccessControlEntries that apply to this AccessControlList.
     */
    public AccessControlEntryDetails[] getAccessControlEntries() {
        final Collection<AccessControlEntryDetails> values = accessControlEntries.values();

        return values.toArray(new AccessControlEntryDetails[values.size()]);
    }

    /**
     * <p>
     * Removes the specified permission bits from the existing allows and denys
     * for this descriptor. If the descriptor is not found, nothing is done and
     * an empty AccessControlEntry is returned.
     * </p>
     *
     * @param descriptor
     *        The descriptor to remove the permissions from.
     * @param permissionsToRemove
     *        The permission bits to remove.
     * @return The updated AccessControlEntry with the permissions removed.
     */
    public AccessControlEntry removePermissions(final IdentityDescriptor descriptor, final int permissionsToRemove) {
        Check.notNull(descriptor, "descriptor"); //$NON-NLS-1$

        AccessControlEntryDetails accessControlEntry;
        if (accessControlEntries.containsKey(descriptor)) {
            accessControlEntry = accessControlEntries.get(descriptor);
        } else {
            return new AccessControlEntryDetails(descriptor, null, 0, 0, null);
        }

        // Actually remove the permissions
        final MergePermissionsResult result = SecurityUtility.mergePermissions(
            accessControlEntry.getAllow(),
            accessControlEntry.getDeny(),
            0,
            0,
            permissionsToRemove);

        accessControlEntry.setAllow(result.updatedAllow);
        accessControlEntry.setDeny(result.updatedDeny);

        // Return a copy of the updated ACE
        return (AccessControlEntryDetails) accessControlEntry.clone();
    }

    /**
     * <p>
     * Removes the AccessControlEntry from this AccessControlList that applies
     * to the given descriptor.
     * </p>
     *
     * @param descriptor
     *        The descriptor for for the AccessControlEntry to remove.
     * @return True if something was removed.
     */
    public boolean removeAccessControlEntry(final IdentityDescriptor descriptor) {
        Check.notNull(descriptor, "descriptor"); //$NON-NLS-1$

        return accessControlEntries.remove(descriptor) != null;
    }

    /**
     * <p>
     * Sets a permission for the descriptor on this AccessControlList.
     * </p>
     *
     * @param descriptor
     *        The descriptor to set the permissions for.
     * @param allow
     *        The allowed permissions to set.
     * @param deny
     *        The denied permissions to set.
     * @param merge
     *        If merge is true and a preexisting AccessControlEntry for the
     *        descriptor is found the two AccessControlEntries will be merged.
     *        When merging permissions in AccessControlEntries, if there is a
     *        conflict, the new permissions will take precedence over the old
     *        permissions. If merge is false and a preexisting
     *        AccessControlEntry for the descriptor is found it will be dropped
     *        and the passed in AccessControlEntry will be the only
     *        AccessControlEntry that remains for this descriptor on this
     *        AccessControlList.
     * @return The new or updated AccessControlEnty that was set in the
     *         AccessControlList.
     */
    public AccessControlEntryDetails setPermissions(
        final IdentityDescriptor descriptor,
        final int allow,
        final int deny,
        final Boolean merge) {
        Check.notNull(descriptor, "descriptor"); //$NON-NLS-1$

        return setAccessControlEntry(new AccessControlEntryDetails(descriptor, allow, deny), merge);
    }

    /**
     * <p>
     * Sets the provided AccessControlEntry in this AccessControlList.
     * </p>
     *
     * @param accessControlEntry
     *        The AccessControlEntry to set in the AccessControlList.
     * @param merge
     *        If merge is true and a preexisting AccessControlEntry for the
     *        descriptor is found the two AccessControlEntries will be merged.
     *        When merging permissions in AccessControlEntries, if there is a
     *        conflict, the new permissions will take precedence over the old
     *        permissions. If merge is false and a preexisting
     *        AccessControlEntry for the descriptor is found it will be dropped
     *        and the passed in AccessControlEntry will be the only
     *        AccessControlEntry that remains for this descriptor on this
     *        AccessControlList.
     * @return The new or updated AccessControlEntry that was set in the
     *         AccessControlList.
     */
    public AccessControlEntryDetails setAccessControlEntry(
        final AccessControlEntryDetails accessControlEntry,
        final boolean merge) {
        return setAccessControlEntries(new AccessControlEntryDetails[] {
            accessControlEntry
        }, merge)[0];
    }

    /**
     * <p>
     * Sets the provided AccessControlEntry in this AccessControlList.
     * </p>
     *
     * @param accessControlEntryDetailsList
     *        The AccessControlEntries to set in the SecurityNamespace.
     * @param merge
     *        If merge is true and a preexisting AccessControlEntry for the
     *        descriptor is found the two AccessControlEntries will be merged.
     *        When merging permissions in AccessControlEntries, if there is a
     *        conflict, the new permissions will take precedence over the old
     *        permissions. If merge is false and a preexisting
     *        AccessControlEntry for the descriptor is found it will be dropped
     *        and the passed in AccessControlEntry will be the only
     *        AccessControlEntry that remains for this descriptor on this
     *        AccessControlList.
     * @return The new or updated permissions that were set in the
     *         AccessControlList.
     */
    public AccessControlEntryDetails[] setAccessControlEntries(
        final AccessControlEntryDetails[] accessControlEntryDetailsList,
        final boolean merge) {
        Check.notNull(accessControlEntryDetailsList, "accessControlEntryDetailsList"); //$NON-NLS-1$

        // Keep track of changed entries
        final List<AccessControlEntryDetails> changedEntries = new ArrayList<AccessControlEntryDetails>();

        for (final AccessControlEntryDetails newAccessControlEntry : accessControlEntryDetailsList) {
            // Check to see if this user already has an entry
            if (accessControlEntries.containsKey(newAccessControlEntry.getSerializableDescriptor())) {
                final AccessControlEntryDetails existingEntry =
                    accessControlEntries.get(newAccessControlEntry.getSerializableDescriptor());

                // update the existing entry
                if (merge) {
                    // Unfortunately we have to create these extra variables
                    // because a property cannot be passed as an out parameter
                    final MergePermissionsResult result = SecurityUtility.mergePermissions(
                        existingEntry.getAllow(),
                        existingEntry.getDeny(),
                        newAccessControlEntry.getAllow(),
                        newAccessControlEntry.getDeny(),
                        0);

                    // Update the value of the allows and denys. Update the
                    // newAccessControlEntry so that
                    // we can just return that object.
                    existingEntry.setAllow(result.updatedAllow);
                    existingEntry.setDeny(result.updatedDeny);
                    newAccessControlEntry.setAllow(result.updatedAllow);
                    newAccessControlEntry.setDeny(result.updatedDeny);
                } else {
                    existingEntry.setAllow(newAccessControlEntry.getAllow() & ~newAccessControlEntry.getDeny());
                    existingEntry.setDeny(newAccessControlEntry.getDeny());
                    newAccessControlEntry.setAllow(existingEntry.getAllow());
                }
            } else {
                // Create a copied AccessControlEntry for it
                final AccessControlEntryDetails copiedAccessControlEntry = new AccessControlEntryDetails(
                    newAccessControlEntry.getSerializableDescriptor(),
                    newAccessControlEntry.getAllow() & ~newAccessControlEntry.getDeny(),
                    newAccessControlEntry.getDeny());

                final ACEExtendedInformation extendedInformation = newAccessControlEntry.getExtendedInformation();
                if (extendedInformation != null) {
                    copiedAccessControlEntry.setExtendedInformation(extendedInformation);
                }

                accessControlEntries.put(newAccessControlEntry.getSerializableDescriptor(), copiedAccessControlEntry);

                // Update the allow value in case there was a conflict in the
                // allows and denies set.
                newAccessControlEntry.setAllow(copiedAccessControlEntry.getAllow());
            }

            changedEntries.add(newAccessControlEntry);
        }

        return changedEntries.toArray(new AccessControlEntryDetails[changedEntries.size()]);
    }

    /**
     * <p>
     * Returns the AccessControlEntry for the descriptor provided. If no
     * AccessControlEntry exists for the provided descriptor in this
     * AccessControlList then an empty AccessControlEntry will be returned.
     * </p>
     *
     * @param descriptor
     *        The descriptor to query the AccessControlEntry for. This cannot be
     *        null.
     * @return The AccessControlEntry for the descriptor provided. If no
     *         AccessControlEntry exists for the provided descriptor in this
     *         AccessControlList then an empty AccessControlEntry will be
     *         returned.
     */
    public AccessControlEntryDetails queryAccessControlEntry(final IdentityDescriptor descriptor) {
        Check.notNull(descriptor, "descriptor"); //$NON-NLS-1$

        return queryAccessControlEntries(new IdentityDescriptor[] {
            descriptor
        })[0];
    }

    /**
     * <p>
     * Returns the AccessControlEntries for the descriptors provided.
     * </p>
     *
     * @param descriptors
     *        The descriptors to query AccessControlEntries for. If null is
     *        passed in for this, AccessControlEntries for all descriptors will
     *        be returned.
     * @return The AccessControlEntries for the descriptors provided. If no
     *         AccessControlEntry exists for a given descriptor in this
     *         AccessControlList then an empty AccessControlEntry will be
     *         returned. The AccessControlEntries are retuned in the same order
     *         that the descriptors are passed in.
     */
    public AccessControlEntryDetails[] queryAccessControlEntries(IdentityDescriptor[] descriptors) {
        final List<AccessControlEntry> accessControlEntryList = new ArrayList<AccessControlEntry>();

        // If null was passed in for identities that means that all of them
        // should be returned.
        if (descriptors == null) {
            final List<IdentityDescriptor> allDescriptors = new ArrayList<IdentityDescriptor>();
            for (final AccessControlEntryDetails entry : accessControlEntries.values()) {
                allDescriptors.add(entry.getSerializableDescriptor());
            }

            descriptors = allDescriptors.toArray(new IdentityDescriptor[allDescriptors.size()]);
        }

        for (final IdentityDescriptor descriptor : descriptors) {
            AccessControlEntryDetails returnableAccessControlEntry;

            if (accessControlEntries.containsKey(descriptor)) {
                final AccessControlEntryDetails storedAccessControlEntry = accessControlEntries.get(descriptor);
                returnableAccessControlEntry = (AccessControlEntryDetails) storedAccessControlEntry.clone();
            } else {
                // Create a new empty permission object that we can return
                final ACEExtendedInformation extendedInfo =
                    (includeExtendedInfoForAces) ? new ACEExtendedInformation(0, 0, 0, 0) : null;
                returnableAccessControlEntry = new AccessControlEntryDetails(descriptor, null, 0, 0, extendedInfo);
            }

            accessControlEntryList.add(returnableAccessControlEntry);
        }

        return accessControlEntryList.toArray(new AccessControlEntryDetails[accessControlEntryList.size()]);
    }

    public void loadAce(final AccessControlEntryDetails ace) {
        accessControlEntries.put(ace.getSerializableDescriptor(), ace);
    }

    public boolean isIncludeExtendedInfoForAces() {
        return includeExtendedInfoForAces;
    }

    public void setIncludeExtendedInfoForAces(final boolean value) {
        includeExtendedInfoForAces = value;
    }
}