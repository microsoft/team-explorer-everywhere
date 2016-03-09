// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.team;

import java.text.MessageFormat;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicReference;

import com.microsoft.tfs.core.TFSConnection;
import com.microsoft.tfs.core.clients.webservices.IIdentityManagementService2;
import com.microsoft.tfs.core.clients.webservices.IdentityAttributeTags;
import com.microsoft.tfs.core.clients.webservices.IdentityDescriptor;
import com.microsoft.tfs.core.clients.webservices.IdentityPropertyScope;
import com.microsoft.tfs.core.clients.webservices.MembershipQuery;
import com.microsoft.tfs.core.clients.webservices.ReadIdentityOptions;
import com.microsoft.tfs.core.clients.webservices.TeamFoundationIdentity;

/**
 * This is a wrapper class for a Team identity that makes it convenient for
 * working with properties. It also provides helper methods to create, update,
 * query and read Teams on server
 */
public class TeamFoundationTeam {
    private TeamFoundationIdentity identity;

    public TeamFoundationTeam(final TeamFoundationIdentity team) {
        // Validate identity has Team property
        final AtomicReference<Object> val = new AtomicReference<Object>();
        if (!team.tryGetProperty(TeamConstants.TEAM_PROPERTY_NAME, val)) {
            throw new RuntimeException(
                MessageFormat.format(
                    "The TeamFoundationIdentity object is missing the required ''{0}'' property", //$NON-NLS-1$
                    TeamConstants.TEAM_PROPERTY_NAME));
        }

        this.identity = team;
    }

    public TeamFoundationIdentity getIdentity() {
        return identity;
    }

    public void setIdentity(final TeamFoundationIdentity identity) {
        this.identity = identity;
    }

    /**
     * @return Team Project Uri
     */
    public String getProject() {
        return identity.getAttribute(IdentityAttributeTags.DOMAIN, null);
    }

    public String getName() {
        return identity.getAttribute(IdentityAttributeTags.ACCOUNT_NAME, null);
    }

    public void setName(final String name) {
        identity.setAttribute(IdentityAttributeTags.ACCOUNT_NAME, name);
    }

    public String getDescription() {
        return identity.getAttribute(IdentityAttributeTags.DESCRIPTION, null);
    }

    public void setDescription(final String name) {
        identity.setAttribute(IdentityAttributeTags.DESCRIPTION, name);
    }

    /**
     * Property accessor. Will return null if not found.
     */
    public boolean tryGetProperty(final String name, final AtomicReference<Object> value) {
        return identity.tryGetProperty(name, value);
    }

    /**
     * Property accessor. Returns null if not found.
     */
    public Object getProperty(final String name) {
        return identity.getProperty(name);
    }

    /**
     * Remove property, if it exists.
     */
    public void removeProperty(final String name) {
        setProperty(name, null);
    }

    /**
     * Property bag. This could be useful, for example if consumer has to
     * iterate through current properties and modify / remove some based on
     * pattern matching property names.
     */
    public Iterable<Entry<String, Object>> GetProperties() {
        return identity.getProperties();
    }

    /**
     * Sets a property, will overwrite if already set.
     *
     * @param name
     *        Name of the property
     * @param value
     *        Value of the property
     */
    public void setProperty(final String name, final Object value) {
        identity.setProperty(IdentityPropertyScope.LOCAL, name, value);
    }

    /**
     * Get team member identities.
     *
     * @param queryMembership
     *        Member identities
     * @return Member identities
     */
    public TeamFoundationIdentity[] getMembers(final TFSConnection connection, final MembershipQuery queryMembership) {
        final IIdentityManagementService2 identitySvc =
            (IIdentityManagementService2) connection.getClient(IIdentityManagementService2.class);

        final TeamFoundationIdentity teamIdentity = identitySvc.readIdentities(new IdentityDescriptor[] {
            identity.getDescriptor()
        }, queryMembership, ReadIdentityOptions.NONE, null, IdentityPropertyScope.NONE)[0];

        TeamFoundationIdentity[] results;
        if (teamIdentity != null) {
            results =
                identitySvc.readIdentities(teamIdentity.getMembers(), MembershipQuery.NONE, ReadIdentityOptions.NONE);
        } else {
            results = new TeamFoundationIdentity[0];
        }
        return results;
    }
}