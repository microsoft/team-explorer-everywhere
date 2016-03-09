// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.webservices;

import java.text.MessageFormat;

import com.microsoft.tfs.core.Messages;
import com.microsoft.tfs.core.TFSConfigurationServer;
import com.microsoft.tfs.core.TFSConnection;
import com.microsoft.tfs.core.artifact.ArtifactID;
import com.microsoft.tfs.core.clients.registration.RegistrationClient;
import com.microsoft.tfs.core.clients.registration.RegistrationEntry;
import com.microsoft.tfs.core.clients.registration.ToolNames;
import com.microsoft.tfs.core.clients.webservices.internal.TeamFoundationSupportedFeatures;
import com.microsoft.tfs.core.internal.wrappers.WrapperUtils;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.GUID;

import ms.ws._IdentityDescriptor;
import ms.ws._IdentityManagementWebServiceSoap;
import ms.ws._TeamFoundationIdentity;

/**
 * A client for the TFS 2012 {@link _IdentityManagementWebServiceSoap}.
 * <p>
 * Unlike the VS implementation, it does not support pre-TFS 2010 servers.
 *
 * @threadsafety thread-compatible
 * @since TEE-SDK-11.0
 */
public class IdentityManagementService implements IIdentityManagementService {
    private final _IdentityManagementWebServiceSoap proxy;
    private final SecurityIdentifier domainSid;
    private final String domainSidWithWellKnownPrefix;
    private final String domainScope;
    private final boolean serverIsV3;

    // Return the Scope Uri for global Groups in the domain
    public static String getIdentityDomainScope(final GUID hostID) {
        final ArtifactID id = new ArtifactID(ToolNames.FRAMEWORK, "IdentityDomain", hostID.toString()); //$NON-NLS-1$
        return id.encodeURI();
    }

    public IdentityManagementService(final TFSConnection connection) {

        /*
         * The .NET implementation falls back to Group Security Service and
         * Common Structure Service for older servers. TEE only started
         * requiring identity management in TFS 2012, so this back-compatibility
         * is not implemented.
         */

        if (connection instanceof TFSConfigurationServer) {
            serverIsV3 = true;
        } else {
            final RegistrationClient registrationClient = connection.getRegistrationClient();
            final RegistrationEntry frameworkEntry = registrationClient.getRegistrationEntry(ToolNames.FRAMEWORK);

            if (frameworkEntry != null) {
                serverIsV3 = true;
            } else {
                serverIsV3 = false;
            }
        }

        if (!serverIsV3) {
            throw new IdentityManagementException(
                MessageFormat.format(
                    Messages.getString("IdentityManagementService.IdentityManagementServiceUnavailableFormat"), //$NON-NLS-1$
                    connection.getBaseURI()));
        }

        proxy = (_IdentityManagementWebServiceSoap) connection.getWebService(_IdentityManagementWebServiceSoap.class);
        domainSid = SIDIdentityHelper.getDomainSID(connection.getInstanceID());
        domainSidWithWellKnownPrefix = domainSid.getValue() + SIDIdentityHelper.WELL_KNOWN_SID_TYPE;
        domainScope = getIdentityDomainScope(connection.getInstanceID());
    }

    @Override
    public TeamFoundationIdentity[] readIdentities(
        final IdentityDescriptor[] descriptors,
        final MembershipQuery queryMembership,
        final ReadIdentityOptions readOptions) {
        Check.notNull(descriptors, "descriptors"); //$NON-NLS-1$

        for (final IdentityDescriptor descriptor : descriptors) {
            IdentityHelper.checkDescriptor(descriptor, "descriptors element"); //$NON-NLS-1$
        }

        final TeamFoundationIdentity[] identities = (TeamFoundationIdentity[]) WrapperUtils.wrap(
            TeamFoundationIdentity.class,
            proxy.readIdentitiesByDescriptor(
                (_IdentityDescriptor[]) WrapperUtils.unwrap(_TeamFoundationIdentity.class, descriptors),
                queryMembership.getValue(),
                readOptions.toIntFlags(),
                TeamFoundationSupportedFeatures.ALL.getValue(),
                null,
                IdentityPropertyScope.NONE.getValue()));

        return identities;
    }

    @Override
    public TeamFoundationIdentity readIdentity(
        final IdentityDescriptor descriptor,
        final MembershipQuery queryMembership,
        final ReadIdentityOptions readOptions) {
        return readIdentities(new IdentityDescriptor[] {
            descriptor
        }, queryMembership, readOptions)[0];
    }

    @Override
    public TeamFoundationIdentity[] readIdentities(
        final GUID[] teamFoundationIds,
        final MembershipQuery queryMembership) {
        Check.notNull(teamFoundationIds, "teamFoundationIds"); //$NON-NLS-1$

        for (int i = 0; i < teamFoundationIds.length; i++) {
            Check.notNull(teamFoundationIds[i], "teamFoundationIds[i]"); //$NON-NLS-1$
        }

        final String[] ids = new String[teamFoundationIds.length];
        for (int i = 0; i < teamFoundationIds.length; i++) {
            ids[i] = teamFoundationIds[i].getGUIDString();
        }

        final TeamFoundationIdentity[] identities = (TeamFoundationIdentity[]) WrapperUtils.wrap(
            TeamFoundationIdentity.class,
            proxy.readIdentitiesById(
                ids,
                queryMembership.getValue(),
                TeamFoundationSupportedFeatures.ALL.getValue(),
                ReadIdentityOptions.NONE.toIntFlags(),
                null,
                IdentityPropertyScope.NONE.getValue()));

        return identities;
    }

    @Override
    public TeamFoundationIdentity[][] readIdentities(
        final IdentitySearchFactor searchFactor,
        final String[] factorValues,
        final MembershipQuery queryMembership,
        final ReadIdentityOptions readOptions) {
        Check.notNull(factorValues, "searchFactorValues"); //$NON-NLS-1$

        for (final String factorValue : factorValues) {
            Check.notNullOrEmpty(factorValue, "factorValue"); //$NON-NLS-1$
        }

        final _TeamFoundationIdentity[][] wso = proxy.readIdentities(
            searchFactor.getValue(),
            factorValues,
            queryMembership.getValue(),
            readOptions.toIntFlags(),
            TeamFoundationSupportedFeatures.ALL.getValue(),
            null,
            IdentityPropertyScope.NONE.getValue());

        final TeamFoundationIdentity[][] results = new TeamFoundationIdentity[wso.length][];
        for (int i = 0; i < results.length; i++) {
            results[i] = (TeamFoundationIdentity[]) WrapperUtils.wrap(TeamFoundationIdentity.class, wso[i]);
        }
        return results;
    }

    @Override
    public TeamFoundationIdentity readIdentity(
        final IdentitySearchFactor searchFactor,
        final String factorValue,
        final MembershipQuery queryMembership,
        final ReadIdentityOptions readOptions) {
        final TeamFoundationIdentity[] results = readIdentities(searchFactor, new String[] {
            factorValue
        }, queryMembership, readOptions)[0];

        final int resultCount = results.length;
        if (resultCount > 1) {
            int activeMatches = 0;
            TeamFoundationIdentity match = null;
            for (final TeamFoundationIdentity result : results) {
                if (result.isActive()) {
                    match = result;
                    activeMatches++;
                }
            }

            if (activeMatches != 1) {
                throw new IdentityManagementException(
                    MessageFormat.format(
                        Messages.getString("IdentityManagementService.MultipleIdentitiesFoundFormat"), //$NON-NLS-1$
                        factorValue));
            }

            return match;
        } else if (resultCount == 1) {
            return results[0];
        } else {
            return null;
        }
    }

    @Override
    public IdentityDescriptor createApplicationGroup(
        final String projectUri,
        final String groupName,
        final String groupDescription) {
        return new IdentityDescriptor(proxy.createApplicationGroup(projectUri, groupName, groupDescription));
    }

    @Override
    public TeamFoundationIdentity[] listApplicationGroups(
        final String projectUri,
        final ReadIdentityOptions readOptions) {
        final TeamFoundationIdentity[] groups = (TeamFoundationIdentity[]) WrapperUtils.wrap(
            TeamFoundationIdentity.class,
            proxy.listApplicationGroups(
                projectUri,
                readOptions.toIntFlags(),
                TeamFoundationSupportedFeatures.ALL.getValue(),
                null,
                IdentityPropertyScope.NONE.getValue()));

        return groups;
    }

    @Override
    public String getScopeName(final String scopeId) {
        Check.notNullOrEmpty(scopeId, "scopeId"); //$NON-NLS-1$

        return proxy.getScopeName(scopeId);
    }

    @Override
    public void updateApplicationGroup(
        final IdentityDescriptor groupDescriptor,
        final GroupProperty property,
        final String newValue) {
        IdentityHelper.checkDescriptor(groupDescriptor, "groupDescriptor"); //$NON-NLS-1$

        proxy.updateApplicationGroup(groupDescriptor.getWebServiceObject(), property.getValue(), newValue);
    }

    @Override
    public void deleteApplicationGroup(final IdentityDescriptor groupDescriptor) {
        IdentityHelper.checkDescriptor(groupDescriptor, "groupDescriptor"); //$NON-NLS-1$

        proxy.deleteApplicationGroup(groupDescriptor.getWebServiceObject());
    }

    @Override
    public void addMemberToApplicationGroup(
        final IdentityDescriptor groupDescriptor,
        final IdentityDescriptor descriptor) {
        IdentityHelper.checkDescriptor(groupDescriptor, "groupDescriptor"); //$NON-NLS-1$
        IdentityHelper.checkDescriptor(descriptor, "descriptor"); //$NON-NLS-1$

        proxy.addMemberToApplicationGroup(groupDescriptor.getWebServiceObject(), descriptor.getWebServiceObject());
    }

    @Override
    public void removeMemberFromApplicationGroup(
        final IdentityDescriptor groupDescriptor,
        final IdentityDescriptor descriptor) {
        IdentityHelper.checkDescriptor(groupDescriptor, "groupDescriptor"); //$NON-NLS-1$
        IdentityHelper.checkDescriptor(descriptor, "descriptor"); //$NON-NLS-1$

        proxy.removeMemberFromApplicationGroup(groupDescriptor.getWebServiceObject(), descriptor.getWebServiceObject());
    }

    @Override
    public boolean isMember(final IdentityDescriptor groupDescriptor, final IdentityDescriptor descriptor) {
        IdentityHelper.checkDescriptor(groupDescriptor, "groupDescriptor"); //$NON-NLS-1$
        IdentityHelper.checkDescriptor(descriptor, "descriptor"); //$NON-NLS-1$

        return proxy.isMember(groupDescriptor.getWebServiceObject(), descriptor.getWebServiceObject());
    }

    @Override
    public boolean refreshIdentity(final IdentityDescriptor descriptor) {
        IdentityHelper.checkDescriptor(descriptor, "descriptor"); //$NON-NLS-1$

        return proxy.refreshIdentity(descriptor.getWebServiceObject());
    }

    @Override
    public boolean isOwner(final IdentityDescriptor descriptor) {
        IdentityHelper.checkDescriptor(descriptor, "descriptor"); //$NON-NLS-1$

        return IdentityConstants.TEAM_FOUNDATION_TYPE.equalsIgnoreCase(descriptor.getIdentityType())
            && (descriptor.getIdentifier().toLowerCase().startsWith(domainSid.getValue())
                || descriptor.getIdentifier().toLowerCase().startsWith(SIDIdentityHelper.WELL_KNOWN_SID_PREFIX));
    }

    @Override
    public boolean isOwnedWellKnownGroup(final IdentityDescriptor descriptor) {
        IdentityHelper.checkDescriptor(descriptor, "descriptor"); //$NON-NLS-1$

        // This comparison works for both Dev10 and earlier servers because
        // well-known groups from earlier servers will start with
        // the well-known sid prefix.

        return IdentityConstants.TEAM_FOUNDATION_TYPE.equalsIgnoreCase(descriptor.getIdentityType())
            && (descriptor.getIdentifier().toLowerCase().startsWith(domainSidWithWellKnownPrefix)
                || descriptor.getIdentifier().toLowerCase().startsWith(SIDIdentityHelper.WELL_KNOWN_SID_PREFIX));
    }

    @Override
    public String getIdentityDomainScope() {
        return domainScope;
    }
}
