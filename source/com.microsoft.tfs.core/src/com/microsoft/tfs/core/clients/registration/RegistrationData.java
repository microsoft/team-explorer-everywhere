// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.registration;

import java.net.URI;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.microsoft.tfs.core.clients.registration.internal.RegistrationDataSerializer;
import com.microsoft.tfs.core.clients.registration.internal.RegistrationUtilities;
import com.microsoft.tfs.core.exceptions.mappers.RegistrationExceptionMapper;
import com.microsoft.tfs.core.internal.wrappers.WrapperUtils;
import com.microsoft.tfs.core.persistence.LockMode;
import com.microsoft.tfs.core.persistence.PersistenceStore;
import com.microsoft.tfs.core.ws.runtime.exceptions.ProxyException;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.GUID;

import ms.tfs.services.registration._03._RegistrationSoap;

/**
 * An immutable holder class for data returned by the registration web service.
 * Supports querying and persistence of the data.
 *
 * @since TEE-SDK-10.1
 * @threadsafety immutable
 */
public class RegistrationData {
    private static final Log log = LogFactory.getLog(RegistrationData.class);
    private static final String INSTANCE_ID_EXTENDED_ATTRIBUTE_NAME = "InstanceId"; //$NON-NLS-1$

    private static final String OBJECT_NAME = "registration.xml"; //$NON-NLS-1$

    public static String makeChildLocationName(final URI serverURI, final String instanceID) {
        Check.notNull(serverURI, "serverURI"); //$NON-NLS-1$
        Check.notNull(instanceID, "instanceID"); //$NON-NLS-1$

        final StringBuffer buffer = new StringBuffer();

        buffer.append(instanceID.toLowerCase());
        buffer.append("_"); //$NON-NLS-1$
        buffer.append(serverURI.getScheme().toLowerCase());

        return buffer.toString();
    }

    public static RegistrationData newFromServer(final _RegistrationSoap webService, final URI serverURI) {
        Check.notNull(webService, "webService"); //$NON-NLS-1$
        Check.notNull(serverURI, "serverURI"); //$NON-NLS-1$

        RegistrationEntry[] entries;
        try {
            entries =
                (RegistrationEntry[]) WrapperUtils.wrap(RegistrationEntry.class, webService.getRegistrationEntries("")); //$NON-NLS-1$
        } catch (final ProxyException e) {
            throw RegistrationExceptionMapper.map(e);
        }

        return new RegistrationData(entries, System.currentTimeMillis(), serverURI.toString());
    }

    public static RegistrationData load(final PersistenceStore cacheStore, final URI serverURI) {
        Check.notNull(cacheStore, "cacheStore"); //$NON-NLS-1$
        Check.notNull(serverURI, "serverURI"); //$NON-NLS-1$

        final ServerMap serverMap = ServerMap.load(cacheStore);
        final String uriString = serverURI.toString();

        final String instanceID = serverMap.getServerID(uriString);
        if (instanceID == null) {
            return null;
        }

        final String childLocationName = RegistrationData.makeChildLocationName(serverURI, instanceID);
        return RegistrationData.load(cacheStore, childLocationName);
    }

    public static RegistrationData load(final PersistenceStore cacheStore, final String childLocationName) {
        Check.notNull(cacheStore, "cacheStore"); //$NON-NLS-1$
        Check.notNull(childLocationName, "childLocationName"); //$NON-NLS-1$

        /*
         * This child location is the server GUID and protocol as a string.
         */
        final PersistenceStore currentStore =
            cacheStore.getChildStore(ServerMap.CHILD_STORE_NAME).getChildStore(childLocationName);

        try {
            if (currentStore.containsItem(OBJECT_NAME) == false) {
                return null;
            }

            return (RegistrationData) currentStore.retrieveItem(
                OBJECT_NAME,
                LockMode.WAIT_FOREVER,
                null,
                new RegistrationDataSerializer());
        } catch (final Exception e) {
            log.warn(MessageFormat.format(
                "unable to load registration data from {0}:{1}", //$NON-NLS-1$
                currentStore.toString(),
                OBJECT_NAME), e);

            return null;
        }
    }

    private final Map regEntryCache = new HashMap();
    private final long lastRefreshTimeMillis;
    private final String serverURI;

    public RegistrationData(
        final RegistrationEntry[] entries,
        final long lastRefreshTimeMillis,
        final String serverURI) {
        Check.notNull(entries, "entries"); //$NON-NLS-1$
        Check.notNull(serverURI, "serverURI"); //$NON-NLS-1$

        this.lastRefreshTimeMillis = lastRefreshTimeMillis;
        this.serverURI = serverURI;

        for (int i = 0; i < entries.length; i++) {
            regEntryCache.put(entries[i].getType().toLowerCase(), entries[i]);
        }
    }

    public void save(final PersistenceStore baseStore, final String childLocationName) {
        Check.notNull(baseStore, "baseStore"); //$NON-NLS-1$
        Check.notNull(childLocationName, "childLocationName"); //$NON-NLS-1$

        /*
         * This child location is the server GUID and protocol as a string. Use
         * the same child store as ServerMap, since we always live under it.
         */
        final PersistenceStore currentStore =
            baseStore.getChildStore(ServerMap.CHILD_STORE_NAME).getChildStore(childLocationName);

        try {
            currentStore.storeItem(OBJECT_NAME, this, LockMode.WAIT_FOREVER, null, new RegistrationDataSerializer());
        } catch (final Exception e) {
            log.warn(MessageFormat.format(
                "unable to save registration data to {0}:{1}", //$NON-NLS-1$
                currentStore.toString(),
                OBJECT_NAME), e);
        }
    }

    public boolean isDataStale(final long refreshIntervalMillis) {
        final long nextRefreshTimeMillis = lastRefreshTimeMillis + refreshIntervalMillis;
        final long currentTimeMillis = System.currentTimeMillis();
        return currentTimeMillis >= nextRefreshTimeMillis;
    }

    public RegistrationEntry[] getRegistrationEntries(final boolean makeCopy) {
        RegistrationEntry[] entries =
            (RegistrationEntry[]) regEntryCache.values().toArray(new RegistrationEntry[regEntryCache.size()]);

        if (makeCopy) {
            entries = RegistrationUtilities.copy(entries);
        }

        return entries;
    }

    public RegistrationEntry getRegistrationEntry(final String toolID, final boolean makeCopy) {
        if (!RegistrationUtilities.isToolType(toolID)) {
            throw new IllegalArgumentException(MessageFormat.format("illegal tool id: [{0}]", toolID)); //$NON-NLS-1$
        }

        RegistrationEntry entry = (RegistrationEntry) regEntryCache.get(toolID.toLowerCase());

        if (makeCopy && entry != null) {
            entry = RegistrationUtilities.copy(entry);
        }

        return entry;
    }

    public ServiceInterface[] getServiceInterfaces(final String toolID, final boolean makeCopy) {
        final RegistrationEntry entry = getRegistrationEntry(toolID, false);

        if (entry == null) {
            return null;
        }

        ServiceInterface[] serviceInterfaces = entry.getServiceInterfaces();

        if (serviceInterfaces != null && makeCopy) {
            serviceInterfaces = RegistrationUtilities.copy(serviceInterfaces);
        }

        return serviceInterfaces;
    }

    public ServiceInterface getServiceInterface(
        final String toolID,
        final String serviceInterfaceName,
        final boolean makeCopy) {
        Check.notNull(serviceInterfaceName, "serviceInterfaceName"); //$NON-NLS-1$

        final ServiceInterface[] serviceInterfaces = getServiceInterfaces(toolID, false);

        if (serviceInterfaces == null) {
            return null;
        }

        for (int i = 0; i < serviceInterfaces.length; i++) {
            if (serviceInterfaceName.equalsIgnoreCase(serviceInterfaces[i].getName())) {
                if (makeCopy) {
                    return RegistrationUtilities.copy(serviceInterfaces[i]);
                } else {
                    return serviceInterfaces[i];
                }
            }
        }

        return null;
    }

    public String getServiceInterfaceURL(
        final String toolID,
        final String serviceInterfaceName,
        final boolean relative) {
        final ServiceInterface serviceInterface = getServiceInterface(toolID, serviceInterfaceName, false);

        if (serviceInterface == null) {
            return null;
        }

        return relative ? serviceInterface.getRelativeURL() : serviceInterface.getURL();
    }

    public ArtifactType[] getArtifactTypes(final String toolID, final boolean makeCopy) {
        final RegistrationEntry entry = getRegistrationEntry(toolID, false);

        if (entry == null) {
            return null;
        }

        ArtifactType[] artifactTypes = entry.getArtifactTypes();

        if (artifactTypes != null && makeCopy) {
            artifactTypes = RegistrationUtilities.copy(artifactTypes);
        }

        return artifactTypes;
    }

    public ArtifactType getArtifactType(final String toolID, final String artifactTypeName, final boolean makeCopy) {
        Check.notNull(artifactTypeName, "artifactTypeName"); //$NON-NLS-1$

        final ArtifactType[] artifactTypes = getArtifactTypes(toolID, false);

        if (artifactTypes == null) {
            return null;
        }

        for (int i = 0; i < artifactTypes.length; i++) {
            if (artifactTypeName.equalsIgnoreCase(artifactTypes[i].getName())) {
                if (makeCopy) {
                    return RegistrationUtilities.copy(artifactTypes[i]);
                } else {
                    return artifactTypes[i];
                }
            }
        }

        return null;
    }

    public OutboundLinkType[] getOutboundLinkTypes(
        final String toolID,
        final String artifactTypeName,
        final boolean makeCopy) {
        final ArtifactType artifactType = getArtifactType(toolID, artifactTypeName, false);

        if (artifactType == null) {
            return null;
        }

        OutboundLinkType[] outboundLinkTypes = artifactType.getOutboundLinkTypes();

        if (outboundLinkTypes != null && makeCopy) {
            outboundLinkTypes = RegistrationUtilities.copy(outboundLinkTypes);
        }

        return outboundLinkTypes;
    }

    public RegistrationExtendedAttribute[] getExtendedAttributes(final String toolID, final boolean makeCopy) {
        final RegistrationEntry entry = getRegistrationEntry(toolID, false);

        if (entry == null) {
            return null;
        }

        RegistrationExtendedAttribute[] extendedAttributes = entry.getRegistrationExtendedAttributes();

        if (extendedAttributes != null && makeCopy) {
            extendedAttributes = RegistrationUtilities.copy(extendedAttributes);
        }

        return extendedAttributes;
    }

    public RegistrationExtendedAttribute getExtendedAttribute(
        final String toolID,
        final String attributeName,
        final boolean makeCopy) {
        Check.notNull(attributeName, "attributeName"); //$NON-NLS-1$

        final RegistrationExtendedAttribute[] attributes = getExtendedAttributes(toolID, false);

        if (attributes == null) {
            return null;
        }

        for (int i = 0; i < attributes.length; i++) {
            if (attributeName.equals(attributes[i].getName())) {
                if (makeCopy) {
                    return RegistrationUtilities.copy(attributes[i]);
                } else {
                    return attributes[i];
                }
            }
        }

        return null;
    }

    public String getExtendedAttributeValue(final String toolID, final String attributeName) {
        final RegistrationExtendedAttribute attribute = getExtendedAttribute(toolID, attributeName, false);

        if (attribute == null) {
            return null;
        }

        return attribute.getValue();
    }

    public GUID getInstanceIDExtendedAttributeValue() {
        final String value = getExtendedAttributeValue(ToolNames.CORE_SERVICES, INSTANCE_ID_EXTENDED_ATTRIBUTE_NAME);

        if (value == null || value.trim().length() == 0) {
            return null;
        }

        return new GUID(value.trim());
    }

    public long getLastRefreshTimeMillis() {
        return lastRefreshTimeMillis;
    }

    public String getServerURI() {
        return serverURI;
    }
}
