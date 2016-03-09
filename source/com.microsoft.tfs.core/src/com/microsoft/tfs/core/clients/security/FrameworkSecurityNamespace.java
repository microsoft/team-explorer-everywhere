// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.security;

import java.util.ArrayList;
import java.util.List;

import com.microsoft.tfs.core.TFSConnection;
import com.microsoft.tfs.core.clients.webservices.IdentityDescriptor;
import com.microsoft.tfs.core.internal.wrappers.WrapperUtils;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.GUID;

import ms.ws._AccessControlEntryDetails;
import ms.ws._AccessControlListDetails;
import ms.ws._IdentityDescriptor;
import ms.ws._SecurityWebServiceSoap;

/**
 * @since TEE-SDK-11.0
 */
public class FrameworkSecurityNamespace extends SecurityNamespace {
    /**
     * The description for this namespace
     */
    private final SecurityNamespaceDescription description;

    /**
     * The proxy for talking to the security web service. This is internal for
     * testing purposes.
     */
    private final _SecurityWebServiceSoap securityProxy;

    /**
     * <p>
     * Creates an instance of the SecurityNamespace
     * </p>
     *
     * @param connection
     *        the {@link TFSConnection} to use (must not be <code>null</code>)
     * @param description
     *        The description this object should be built from.
     */
    public FrameworkSecurityNamespace(final TFSConnection connection, final SecurityNamespaceDescription description) {
        Check.notNull(connection, "connection"); //$NON-NLS-1$
        Check.notNull(description.getName(), "description.getName()"); //$NON-NLS-1$
        Check.isTrue(
            description.getNamespaceId() != null && !description.getNamespaceId().equals(GUID.EMPTY),
            "description.getNamespaceId() != null && !description.getNamespaceId().equals(GUID.EMPTY)"); //$NON-NLS-1$

        this.description = description;
        securityProxy = (_SecurityWebServiceSoap) connection.getWebService(_SecurityWebServiceSoap.class);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SecurityNamespaceDescription getDescription() {
        return description.clone();
    }

    @Override
    public boolean hasPermission(
        final String token,
        final IdentityDescriptor descriptor,
        final int requestedPermissions,
        final boolean alwaysAllowAdministrators) {
        Check.notNull(token, "token"); //$NON-NLS-1$
        Check.notNull(descriptor, "descriptor"); //$NON-NLS-1$

        final boolean[] ret = securityProxy.hasPermissionByPermissionsList(
            getDescription().getNamespaceId().getGUIDString(),
            token,
            descriptor.getWebServiceObject(),
            new int[] {
                requestedPermissions
        }, alwaysAllowAdministrators);

        if (ret == null) {
            return false;
        }

        return ret[0];
    }

    @Override
    public boolean[] hasPermission(
        final String[] tokens,
        final IdentityDescriptor descriptor,
        final int requestedPermissions,
        final boolean alwaysAllowAdministrators) {
        Check.notNullOrEmpty(tokens, "tokens"); //$NON-NLS-1$
        Check.notNull(descriptor, "descriptor"); //$NON-NLS-1$

        return securityProxy.hasPermissionByTokenList(
            getDescription().getNamespaceId().getGUIDString(),
            tokens,
            descriptor.getWebServiceObject(),
            requestedPermissions,
            alwaysAllowAdministrators);
    }

    @Override
    public boolean[] hasPermission(
        final String token,
        final IdentityDescriptor[] descriptors,
        final int requestedPermissions,
        final boolean alwaysAllowAdministrators) {
        Check.notNull(token, "token"); //$NON-NLS-1$
        Check.notNullOrEmpty(descriptors, "descriptors"); //$NON-NLS-1$

        return securityProxy.hasPermissionByDescriptorList(
            getDescription().getNamespaceId().getGUIDString(),
            token,
            (_IdentityDescriptor[]) WrapperUtils.unwrap(_IdentityDescriptor.class, descriptors),
            requestedPermissions,
            alwaysAllowAdministrators);
    }

    @Override
    public boolean[] hasPermission(
        final String token,
        final IdentityDescriptor descriptor,
        final int[] requestedPermissions,
        final boolean alwaysAllowAdministrators) {
        Check.notNull(token, "token"); //$NON-NLS-1$
        Check.notNull(descriptor, "descriptor"); //$NON-NLS-1$
        Check.notNull(requestedPermissions, "requestedPermissions"); //$NON-NLS-1$
        Check.isTrue(requestedPermissions.length > 0, "requestedPermissions.length > 0"); //$NON-NLS-1$

        return securityProxy.hasPermissionByPermissionsList(
            getDescription().getNamespaceId().getGUIDString(),
            token,
            descriptor.getWebServiceObject(),
            requestedPermissions,
            alwaysAllowAdministrators);
    }

    @Override
    public boolean hasWritePermission(final String token, final int permissionsToChange) {
        Check.notNull(token, "token"); //$NON-NLS-1$

        final boolean[] ret =
            securityProxy.hasWritePermission(getDescription().getNamespaceId().getGUIDString(), token, new int[] {
                permissionsToChange
        });

        if (ret == null) {
            return false;
        }

        return ret[0];
    }

    @Override
    public boolean[] hasWritePermission(final String token, final int[] permissionsToChange) {
        Check.notNull(token, "token"); //$NON-NLS-1$
        Check.notNull(permissionsToChange, "permissionsToChange"); //$NON-NLS-1$
        Check.isTrue(permissionsToChange.length > 0, "permissionsToChange.length > 0"); //$NON-NLS-1$

        return securityProxy.hasWritePermission(
            getDescription().getNamespaceId().getGUIDString(),
            token,
            permissionsToChange);
    }

    @Override
    public boolean removeAccessControlLists(final String token, final boolean recurse) {
        return removeAccessControlLists(new String[] {
            token
        }, recurse);
    }

    @Override
    public boolean removeAccessControlLists(final String[] tokens, final boolean recurse) {
        Check.notNull(tokens, "tokens"); //$NON-NLS-1$

        return securityProxy.removeAccessControlList(
            getDescription().getNamespaceId().getGUIDString(),
            tokens,
            recurse);
    }

    @Override
    public boolean removeAccessControlEntries(final String token, final IdentityDescriptor[] descriptors) {
        Check.notNull(token, "token"); //$NON-NLS-1$
        Check.notNullOrEmpty(descriptors, "descriptors"); //$NON-NLS-1$

        return securityProxy.removeAccessControlEntries(
            getDescription().getNamespaceId().getGUIDString(),
            token,
            (_IdentityDescriptor[]) WrapperUtils.unwrap(IdentityDescriptor.class, descriptors));
    }

    @Override
    public boolean removeAccessControlEntry(final String token, final IdentityDescriptor descriptor) {
        return removeAccessControlEntries(token, new IdentityDescriptor[] {
            descriptor
        });
    }

    @Override
    public AccessControlEntryDetails removePermissions(
        final String token,
        final IdentityDescriptor descriptor,
        final int permissionsToRemove) {
        Check.notNull(token, "token"); //$NON-NLS-1$
        Check.notNull(descriptor, "descriptor"); //$NON-NLS-1$

        return new AccessControlEntryDetails(
            securityProxy.removePermissions(
                getDescription().getNamespaceId().getGUIDString(),
                token,
                descriptor.getWebServiceObject(),
                permissionsToRemove));
    }

    @Override
    public AccessControlEntryDetails setPermissions(
        final String token,
        final IdentityDescriptor descriptor,
        final int allow,
        final int deny,
        final boolean merge) {
        return setAccessControlEntry(token, new AccessControlEntryDetails(descriptor, allow, deny), merge);
    }

    @Override
    public AccessControlEntryDetails setAccessControlEntry(
        final String token,
        final AccessControlEntryDetails accessControlEntry,
        final boolean merge) {
        Check.notNull(accessControlEntry, "accessControlEntry"); //$NON-NLS-1$
        Check.notNull(token, "token"); //$NON-NLS-1$

        final AccessControlEntryDetails[] ret = setAccessControlEntries(token, new AccessControlEntryDetails[] {
            accessControlEntry
        }, merge);

        if (ret == null) {
            return null;
        }

        return ret[0];
    }

    @Override
    public AccessControlEntryDetails[] setAccessControlEntries(
        final String token,
        final AccessControlEntryDetails[] accessControlEntries,
        final boolean merge) {
        Check.notNullOrEmpty(accessControlEntries, "accessControlEntries"); //$NON-NLS-1$
        Check.notNull(token, "token"); //$NON-NLS-1$

        return (AccessControlEntryDetails[]) WrapperUtils.wrap(
            AccessControlEntryDetails.class,
            securityProxy.setPermissions(
                getDescription().getNamespaceId().getGUIDString(),
                token,
                (_AccessControlEntryDetails[]) WrapperUtils.unwrap(
                    _AccessControlEntryDetails.class,
                    accessControlEntries),
                merge));
    }

    @Override
    public void setAccessControlLists(final AccessControlListDetails[] accessControlLists) {
        Check.notNullOrEmpty(accessControlLists, "accessControlLists"); //$NON-NLS-1$

        securityProxy.setAccessControlList(
            getDescription().getNamespaceId().getGUIDString(),
            (_AccessControlListDetails[]) WrapperUtils.unwrap(_AccessControlListDetails.class, accessControlLists));
    }

    @Override
    public void setAccessControlList(final AccessControlListDetails accessControlList) {
        setAccessControlLists(new AccessControlListDetails[] {
            accessControlList
        });
    }

    @Override
    public AccessControlListDetails[] queryAccessControlLists(
        final String token,
        final IdentityDescriptor[] descriptors,
        final boolean includeExtendedInfo,
        final boolean recurse) {
        Check.notNull(token, "token"); //$NON-NLS-1$

        List<_IdentityDescriptor> identityData = null;
        if (descriptors != null) {
            identityData = new ArrayList<_IdentityDescriptor>();
            for (final IdentityDescriptor descriptor : descriptors) {
                identityData.add(new _IdentityDescriptor(descriptor.getIdentityType(), descriptor.getIdentifier()));
            }
        }

        return (AccessControlListDetails[]) WrapperUtils.wrap(
            AccessControlListDetails.class,
            securityProxy.queryPermissions(
                getDescription().getNamespaceId().getGUIDString(),
                token,
                identityData != null ? identityData.toArray(new _IdentityDescriptor[identityData.size()]) : null,
                includeExtendedInfo,
                recurse));
    }

    @Override
    public AccessControlListDetails queryAccessControlList(
        final String token,
        final IdentityDescriptor[] descriptors,
        final boolean includeExtendedInfo) {
        final AccessControlListDetails[] acl = queryAccessControlLists(token, descriptors, includeExtendedInfo, false);

        if (acl == null || acl.length == 0) {
            // return an empty ACL
            return new AccessControlListDetails(token, true);
        }

        return acl[0];
    }

    @Override
    public int queryEffectivePermissions(final String token, final IdentityDescriptor descriptor) {
        Check.notNull(descriptor, "descriptor"); //$NON-NLS-1$
        Check.notNull(token, "token"); //$NON-NLS-1$

        final IdentityDescriptor identity =
            new IdentityDescriptor(descriptor.getIdentityType(), descriptor.getIdentifier());

        final _AccessControlListDetails[] result = securityProxy.queryPermissions(
            getDescription().getNamespaceId().getGUIDString(),
            token,
            new _IdentityDescriptor[] {
                identity.getWebServiceObject()
        }, true, false);

        final _AccessControlEntryDetails[] data = result[0].getAccessControlEntries();

        if (data.length == 0) {
            return 0;
        }

        return data[0].getExtendedInformation().getEffectiveAllow();
    }

    @Override
    public void setInheritFlag(final String token, final boolean inherit) {
        Check.notNull(token, "token"); //$NON-NLS-1$

        securityProxy.setInheritFlag(getDescription().getNamespaceId().getGUIDString(), token, inherit);
    }
}