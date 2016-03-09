// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.webservices;

import java.text.MessageFormat;
import java.util.Set;

import com.microsoft.tfs.core.Messages;
import com.microsoft.tfs.core.TFSConnection;
import com.microsoft.tfs.core.clients.webservices.internal.TeamFoundationSupportedFeatures;
import com.microsoft.tfs.core.internal.wrappers.WrapperUtils;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.GUID;

import ms.ws._IdentityDescriptor;
import ms.ws._IdentityManagementWebService2Soap;
import ms.ws._PropertyValue;
import ms.ws._TeamFoundationIdentity;

/**
 * A client for the TFS 2012 IdentityManagementService2.
 *
 * @threadsafety thread-compatible
 * @since TEE-SDK-11.0
 */
public class IdentityManagementService2 extends IdentityManagementService implements IIdentityManagementService2 {
    private final _IdentityManagementWebService2Soap proxy2;

    public IdentityManagementService2(final TFSConnection connection) {
        super(connection);
        this.proxy2 =
            (_IdentityManagementWebService2Soap) connection.getWebService(_IdentityManagementWebService2Soap.class);
    }

    public boolean isSupported() {
        return this.proxy2 != null;
    }

    @Override
    public TeamFoundationIdentity[] getMostRecentlyUsedUsers() {
        final TeamFoundationIdentity[] identities = (TeamFoundationIdentity[]) WrapperUtils.wrap(
            TeamFoundationIdentity.class,
            proxy2.getMostRecentlyUsedUsers(TeamFoundationSupportedFeatures.ALL.getValue()));

        return identities;
    }

    @Override
    public void addRecentUser(final TeamFoundationIdentity identity) {
        proxy2.addRecentUser(identity.getTeamFoundationID().getGUIDString());
    }

    @Override
    public FilteredIdentitiesList readFilteredIdentities(
        final String expression,
        final int suggestedPageSize,
        final String lastSearchResult,
        final boolean lookForward,
        final int queryMembership) {
        final FilteredIdentitiesList list = FilteredIdentitiesList.fromWebServiceObject(
            proxy2.readFilteredIdentities(
                expression,
                suggestedPageSize,
                lastSearchResult,
                lookForward,
                queryMembership,
                TeamFoundationSupportedFeatures.ALL.getValue()));

        return list;
    }

    @Override
    public TeamFoundationIdentity readIdentity(final String generalSearchValue) {
        return readIdentity(
            IdentitySearchFactor.GENERAL,
            generalSearchValue,
            MembershipQuery.NONE,
            ReadIdentityOptions.NONE);
    }

    @Override
    public TeamFoundationIdentity[] listApplicationGroups(
        final String scopeId,
        final ReadIdentityOptions readOptions,
        final String[] propertyNameFilters,
        final IdentityPropertyScope propertyScope) {
        final TeamFoundationIdentity[] groups = (TeamFoundationIdentity[]) WrapperUtils.wrap(
            TeamFoundationIdentity.class,
            proxy2.listApplicationGroups(
                scopeId,
                readOptions.toIntFlags(),
                TeamFoundationSupportedFeatures.ALL.getValue(),
                propertyNameFilters,
                propertyScope.getValue()));

        return groups;
    }

    @Override
    public void setCustomDisplayName(final String customDisplayName) {
        Check.notNullOrEmpty(customDisplayName, "customDisplayName"); //$NON-NLS-1$

        proxy2.setCustomDisplayName(customDisplayName);
    }

    @Override
    public void clearCustomDisplayName() {
        proxy2.setCustomDisplayName(null);
    }

    @Override
    public TeamFoundationIdentity[] readIdentities(
        final IdentityDescriptor[] descriptors,
        final MembershipQuery queryMembership,
        final ReadIdentityOptions readOptions,
        final String[] propertyNameFilters,
        final IdentityPropertyScope propertyScope) {
        Check.notNull(descriptors, "descriptors"); //$NON-NLS-1$

        for (final IdentityDescriptor descriptor : descriptors) {
            IdentityHelper.checkDescriptor(descriptor, "descriptors element"); //$NON-NLS-1$
        }

        final TeamFoundationIdentity[] identities = (TeamFoundationIdentity[]) WrapperUtils.wrap(
            TeamFoundationIdentity.class,
            proxy2.readIdentitiesByDescriptor(
                (_IdentityDescriptor[]) WrapperUtils.unwrap(_IdentityDescriptor.class, descriptors),
                queryMembership.getValue(),
                readOptions.toIntFlags(),
                TeamFoundationSupportedFeatures.ALL.getValue(),
                propertyNameFilters,
                propertyScope.getValue()));

        return identities;
    }

    @Override
    public TeamFoundationIdentity readIdentity(
        final IdentityDescriptor descriptor,
        final MembershipQuery queryMembership,
        final ReadIdentityOptions readOptions,
        final String[] propertyNameFilters,
        final IdentityPropertyScope propertyScope) {
        return readIdentities(new IdentityDescriptor[] {
            descriptor
        }, queryMembership, readOptions, propertyNameFilters, propertyScope)[0];
    }

    @Override
    public TeamFoundationIdentity[] readIdentities(
        final GUID[] teamFoundationIds,
        final MembershipQuery queryMembership,
        final ReadIdentityOptions readOptions,
        final String[] propertyNameFilters,
        final IdentityPropertyScope propertyScope) {
        Check.notNull(teamFoundationIds, "teamFoundationIds"); //$NON-NLS-1$

        final String[] idStrings = new String[teamFoundationIds.length];
        for (int i = 0; i < teamFoundationIds.length; i++) {
            Check.notNull(teamFoundationIds[i], "teamFoundationIds[i]"); //$NON-NLS-1$
            idStrings[i] = teamFoundationIds[i].getGUIDString();
        }

        final TeamFoundationIdentity[] identities = (TeamFoundationIdentity[]) WrapperUtils.wrap(
            TeamFoundationIdentity.class,
            proxy2.readIdentitiesById(
                idStrings,
                queryMembership.getValue(),
                TeamFoundationSupportedFeatures.ALL.getValue(),
                readOptions.toIntFlags(),
                propertyNameFilters,
                propertyScope.getValue()));

        return identities;
    }

    @Override
    public TeamFoundationIdentity[][] readIdentities(
        final IdentitySearchFactor searchFactor,
        final String[] searchFactorValues,
        final MembershipQuery queryMembership,
        final ReadIdentityOptions readOptions,
        final String[] propertyNameFilters,
        final IdentityPropertyScope propertyScope) {
        Check.notNull(searchFactorValues, "searchFactorValues"); //$NON-NLS-1$
        for (final String factorValue : searchFactorValues) {
            Check.notNullOrEmpty(factorValue, "factorValue"); //$NON-NLS-1$
        }

        final _TeamFoundationIdentity[][] wso = proxy2.readIdentities(
            searchFactor.getValue(),
            searchFactorValues,
            queryMembership.getValue(),
            readOptions.toIntFlags(),
            TeamFoundationSupportedFeatures.ALL.getValue(),
            propertyNameFilters,
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
        final String searchFactorValue,
        final MembershipQuery queryMembership,
        final ReadIdentityOptions readOptions,
        final String[] propertyNameFilters,
        final IdentityPropertyScope propertyScope) {
        final TeamFoundationIdentity[] results = readIdentities(searchFactor, new String[] {
            searchFactorValue
        }, queryMembership, readOptions, propertyNameFilters, propertyScope)[0];

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
                        searchFactorValue));
            }

            return match;
        } else if (resultCount == 1) {
            return results[0];
        } else {
            return null;
        }
    }

    @Override
    public void updateExtendedProperties(final TeamFoundationIdentity identity) {
        final Set<String> modifiedPropertiesLog = identity.getModifiedPropertiesLog(IdentityPropertyScope.GLOBAL);
        final Set<String> modifiedLocalPropertiesLog = identity.getModifiedPropertiesLog(IdentityPropertyScope.LOCAL);

        if ((modifiedPropertiesLog != null && modifiedPropertiesLog.size() > 0)
            || (modifiedLocalPropertiesLog != null && modifiedLocalPropertiesLog.size() > 0)) {
            final _PropertyValue[] modifiedProperties =
                buildModifiedProperties(IdentityPropertyScope.GLOBAL, identity, modifiedPropertiesLog);

            final _PropertyValue[] modifiedLocalProperties =
                buildModifiedProperties(IdentityPropertyScope.LOCAL, identity, modifiedLocalPropertiesLog);

            proxy2.updateIdentityExtendedProperties(
                identity.getDescriptor().getWebServiceObject(),
                modifiedProperties,
                modifiedLocalProperties);

            identity.resetModifiedProperties();
        }
    }

    private _PropertyValue[] buildModifiedProperties(
        final IdentityPropertyScope propertyScope,
        final TeamFoundationIdentity identity,
        final Set<String> modifiedPropertiesLog) {
        final _PropertyValue[] modifiedProperties =
            new _PropertyValue[modifiedPropertiesLog != null ? modifiedPropertiesLog.size() : 0];
        if (modifiedPropertiesLog != null) {
            int i = 0;
            for (final String propertyName : modifiedPropertiesLog) {
                modifiedProperties[i++] =
                    new _PropertyValue(propertyName, identity.getProperty(propertyScope, propertyName));
            }
        }

        return modifiedProperties;
    }
}
