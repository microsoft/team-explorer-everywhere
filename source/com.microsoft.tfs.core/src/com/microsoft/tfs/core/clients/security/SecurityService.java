// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.security;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import com.microsoft.tfs.core.Messages;
import com.microsoft.tfs.core.TFSConnection;
import com.microsoft.tfs.core.TFSTeamProjectCollection;
import com.microsoft.tfs.core.clients.registration.RegistrationClient;
import com.microsoft.tfs.core.clients.registration.RegistrationEntry;
import com.microsoft.tfs.core.clients.registration.ToolNames;
import com.microsoft.tfs.core.clients.security.exceptions.SecurityServiceException;
import com.microsoft.tfs.util.GUID;

import ms.ws._SecurityNamespaceDescription;
import ms.ws._SecurityWebServiceSoap;

/**
 * @since TEE-SDK-11.0
 */
public class SecurityService implements ISecurityService {
    /**
     * The server proxy we are talking to.
     */
    private final TFSConnection connection;

    /**
     * Dictionary used for the storage of the ISecurityNamespaces.
     */
    private final Map<GUID, SecurityNamespace> namespaces = new HashMap<GUID, SecurityNamespace>();
    private volatile boolean namespacesLoaded;

    /**
     * The lock used to control access to the m_namespaces dictionary
     */
    private final ReadWriteLock accessLock = new ReentrantReadWriteLock();

    /**
     * The proxy for talking to the security web service.
     */
    private final _SecurityWebServiceSoap m_securityProxy;

    public SecurityService(final TFSConnection connection) {
        this.connection = connection;

        if (connection instanceof TFSTeamProjectCollection) {
            // Determine if we are talking to a new or old server.
            final RegistrationClient registrationService =
                (RegistrationClient) connection.getClient(RegistrationClient.class);
            final RegistrationEntry frameworkEntry = registrationService.getRegistrationEntry(ToolNames.FRAMEWORK);

            if (frameworkEntry != null) {
                // This is Rosario or later.
                m_securityProxy = (_SecurityWebServiceSoap) connection.getWebService(_SecurityWebServiceSoap.class);
            } else {
                throw new SecurityServiceException(
                    Messages.getString("SecurityService.ServiceNotSupportedPreFramework")); //$NON-NLS-1$
            }
        } else {
            throw new SecurityServiceException(Messages.getString("SecurityService.ServiceNotSupportedPreFramework")); //$NON-NLS-1$
        }
    }

    @Override
    public SecurityNamespace createSecurityNamespace(final SecurityNamespaceDescription description) {
        if (m_securityProxy == null) {
            throw new SecurityServiceException(Messages.getString("SecurityService.OperationNotSuportedPreFramework")); //$NON-NLS-1$
        }

        boolean locked = false;
        try {
            ensureNamespacesLoaded();

            // Create the SecurityNamespace through the Web service first. This
            // will
            // throw an exception if they don't have permission to do this.
            m_securityProxy.createSecurityNamespace(description.getWebServiceObject());

            // The web service call must have succeeded so go ahead and create
            // the client portion.
            accessLock.writeLock().lock();
            locked = true;

            final SecurityNamespace securityNamespace = new FrameworkSecurityNamespace(connection, description);
            namespaces.put(securityNamespace.getDescription().getNamespaceId(), securityNamespace);

            return securityNamespace;
        } finally {
            if (locked) {
                accessLock.writeLock().unlock();
            }
        }
    }

    @Override
    public boolean deleteSecurityNamespace(final GUID namespaceId) {
        if (m_securityProxy == null) {
            throw new SecurityServiceException(Messages.getString("SecurityService.OperationNotSuportedPreFramework")); //$NON-NLS-1$
        }

        boolean locked = false;
        try {
            ensureNamespacesLoaded();

            // Try to do this over the web service first to see if it succeeds.
            m_securityProxy.deleteSecurityNamespace(namespaceId.getGUIDString());

            accessLock.writeLock().lock();
            locked = true;

            if (namespaces.remove(namespaceId) != null) {
                accessLock.writeLock().unlock();
                locked = false;

                m_securityProxy.deleteSecurityNamespace(namespaceId.getGUIDString());

                return true;
            }

            return false;
        } finally {
            if (locked) {
                accessLock.writeLock().unlock();
            }
        }
    }

    @Override
    public SecurityNamespace getSecurityNamespace(final GUID namespaceId) {
        boolean locked = false;
        try {
            ensureNamespacesLoaded();

            accessLock.readLock().lock();
            locked = true;

            final SecurityNamespace securityNamespace = namespaces.get(namespaceId);

            return securityNamespace;
        } finally {
            if (locked) {
                accessLock.readLock().unlock();
            }
        }
    }

    @Override
    public SecurityNamespace[] getSecurityNamespaces() {
        boolean locked = false;
        try {
            ensureNamespacesLoaded();

            accessLock.readLock().lock();
            locked = true;

            // return a copy of the values to prevent race conditions while
            // iterating
            final Collection<SecurityNamespace> values = namespaces.values();
            return values.toArray(new SecurityNamespace[values.size()]);
        } finally {
            if (locked) {
                accessLock.readLock().unlock();
            }
        }
    }

    // / <summary>
    // / THIS FUNCTION IS FOR TEST PURPOSES ONLY!!!!!
    // / </summary>
    // / <param name="simulateVersionTwoServer"></param>
    // internal void SetServerSimulationMode(
    // Boolean simulateVersionTwoServer)
    // {
    // m_namespaces = null;
    //
    // if (simulateVersionTwoServer)
    // {
    // m_securityProxy = null;
    // }
    // else
    // {
    // m_securityProxy = new SecurityWebService(m_server);
    // }
    // }

    /**
     * Ensures that all of the namespaces that exist in the system are loaded.
     */
    private void ensureNamespacesLoaded() {
        if (namespacesLoaded) {
            return;
        }

        boolean locked = false;
        try {
            accessLock.writeLock().lock();
            locked = true;

            if (m_securityProxy != null) {
                final _SecurityNamespaceDescription[] descriptions =
                    m_securityProxy.querySecurityNamespaces(GUID.EMPTY.getGUIDString());

                for (final _SecurityNamespaceDescription description : descriptions) {
                    final SecurityNamespace securityNamespace =
                        new FrameworkSecurityNamespace(connection, new SecurityNamespaceDescription(description));
                    namespaces.put(securityNamespace.getDescription().getNamespaceId(), securityNamespace);
                }

                namespacesLoaded = true;
            } else if (connection instanceof TFSTeamProjectCollection) {
                throw new SecurityServiceException(
                    Messages.getString("SecurityService.OperationNotSuportedPreFramework")); //$NON-NLS-1$
            }
        } finally {
            if (locked) {
                accessLock.writeLock().unlock();
            }
        }
    }
}
