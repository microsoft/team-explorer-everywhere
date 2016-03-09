// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.groupsecurity;

import java.text.MessageFormat;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.microsoft.tfs.core.TFSTeamProjectCollection;
import com.microsoft.tfs.core.clients.authorization.Identity;
import com.microsoft.tfs.core.clients.authorization.IdentityType;
import com.microsoft.tfs.core.clients.authorization.QueryMembership;
import com.microsoft.tfs.core.clients.authorization.SearchFactor;
import com.microsoft.tfs.core.clients.commonstructure.CommonStructureClient;
import com.microsoft.tfs.core.clients.commonstructure.ProjectInfo;
import com.microsoft.tfs.core.clients.webservices.IdentityAttributeTags;
import com.microsoft.tfs.core.clients.webservices.IdentityConstants;
import com.microsoft.tfs.core.clients.webservices.IdentityDescriptor;
import com.microsoft.tfs.core.clients.webservices.IdentityHelper;
import com.microsoft.tfs.core.clients.webservices.TeamFoundationIdentity;
import com.microsoft.tfs.core.exceptions.mappers.GroupSecurityExceptionMapper;
import com.microsoft.tfs.core.ws.runtime.exceptions.ProxyException;
import com.microsoft.tfs.util.Check;

import ms.tfs.services.groupsecurity._03._GroupSecurityServiceSoap;

/**
 * Accesses TFS groups security services.
 *
 * @threadsafety thread-safe
 * @since TEE-SDK-10.1
 */
public class GroupSecurityClient {
    private final static Log log = LogFactory.getLog(GroupSecurityClient.class);

    private final TFSTeamProjectCollection connection;
    private final _GroupSecurityServiceSoap webService;

    public GroupSecurityClient(final TFSTeamProjectCollection connection, final _GroupSecurityServiceSoap webService) {
        Check.notNull(connection, "connection"); //$NON-NLS-1$
        Check.notNull(webService, "webService"); //$NON-NLS-1$

        this.connection = connection;
        this.webService = webService;
    }

    public Identity readIdentity(
        final SearchFactor factor,
        final String factorValue,
        final QueryMembership queryMembership) {
        try {
            return new Identity(
                webService.readIdentity(
                    factor.getWebServiceObject(),
                    factorValue,
                    queryMembership.getWebServiceObject()));
        } catch (final ProxyException e) {
            throw GroupSecurityExceptionMapper.map(e);
        }
    }

    /**
     * Convert method for backward compat scenarios that involve a Rosario
     * client connecting to an Orcas or Whidbey server.
     */
    public TeamFoundationIdentity convert(final Identity identity) {
        if (identity == null) {
            return null;
        }

        final IdentityDescriptor descriptor = IdentityHelper.createDescriptorFromSID(identity.getSID());

        IdentityDescriptor[] members = null;
        if (identity.getMembers() != null) {
            members = new IdentityDescriptor[identity.getMembers().length];

            for (int i = 0; i < identity.getMembers().length; i++) {
                members[i] = (IdentityHelper.createDescriptorFromSID(identity.getMembers()[i]));
            }
        }

        IdentityDescriptor[] memberOf = null;
        if (identity.getMemberOf() != null) {
            memberOf = new IdentityDescriptor[identity.getMemberOf().length];

            for (int i = 0; i < identity.getMemberOf().length; i++) {
                memberOf[i] = (IdentityHelper.createDescriptorFromSID(identity.getMemberOf()[i]));
            }
        }

        final TeamFoundationIdentity result =
            new TeamFoundationIdentity(descriptor, identity.getDisplayName(), !identity.isDeleted(), members, memberOf);

        if ((identity.getType() == IdentityType.APPLICATION_GROUP)
            || (identity.getType() == IdentityType.WINDOWS_GROUP)) {
            result.setAttribute(IdentityAttributeTags.SCHEMA_CLASS_NAME, IdentityConstants.SCHEMA_CLASS_GROUP);
        } else {
            result.setAttribute(IdentityAttributeTags.SCHEMA_CLASS_NAME, IdentityConstants.SCHEMA_CLASS_USER);
        }

        result.setAttribute(IdentityAttributeTags.DESCRIPTION, identity.getDescription());
        result.setAttribute(IdentityAttributeTags.DOMAIN, identity.getDomain());

        if (identity.getType() == IdentityType.APPLICATION_GROUP) {
            result.setAttribute(IdentityAttributeTags.ACCOUNT_NAME, identity.getDisplayName());

            if (identity.getDomain() == null || identity.getDomain().length() == 0) {
                result.setAttribute(IdentityAttributeTags.GLOBAL_SCOPE, IdentityAttributeTags.GLOBAL_SCOPE);
            } else {
                final String projectName = getProjectName(identity.getDomain());
                result.setAttribute(IdentityAttributeTags.SCOPE_NAME, projectName);
                result.setDisplayName("[" + projectName + "]\\" + identity.getDisplayName()); //$NON-NLS-1$ //$NON-NLS-2$
            }
        } else {
            result.setAttribute(IdentityAttributeTags.ACCOUNT_NAME, identity.getAccountName());
        }

        result.setAttribute(
            IdentityAttributeTags.DISAMBIGUATION,
            identity.getDomain() + "\\" + identity.getAccountName()); //$NON-NLS-1$
        result.setAttribute(IdentityAttributeTags.DISTINGUISHED_NAME, identity.getDistinguishedName());
        result.setAttribute(IdentityAttributeTags.MAIL_ADDRESS, identity.getMailAddress());
        result.setAttribute(IdentityAttributeTags.SECURITY_GROUP, IdentityAttributeTags.SECURITY_GROUP);
        result.setAttribute(IdentityAttributeTags.SPECIAL_TYPE, identity.getSpecialType().toString());

        return result;
    }

    private String getProjectName(final String scopeId) {
        String projectName = scopeId;

        final CommonStructureClient commonStructureClient = connection.getCommonStructureClient();

        if (commonStructureClient != null) {
            try {
                final ProjectInfo pi = commonStructureClient.getProject(scopeId);
                projectName = pi == null ? "" : pi.getName(); //$NON-NLS-1$
            } catch (final Exception e) {
                log.info(
                    MessageFormat.format(
                        "Error resolving scopeID {0} to project name via common structure service, returning scopeId as name", //$NON-NLS-1$
                        scopeId),
                    e);

                // if we have an error getting the project, use the projectUri
                // we were given.
            }
        }

        return projectName;
    }
    // TODO implement more methods as required
}
