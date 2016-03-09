// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.registration.internal;

import com.microsoft.tfs.core.clients.registration.ArtifactType;
import com.microsoft.tfs.core.clients.registration.Database;
import com.microsoft.tfs.core.clients.registration.EventType;
import com.microsoft.tfs.core.clients.registration.OutboundLinkType;
import com.microsoft.tfs.core.clients.registration.RegistrationEntry;
import com.microsoft.tfs.core.clients.registration.RegistrationExtendedAttribute;
import com.microsoft.tfs.core.clients.registration.ServiceInterface;
import com.microsoft.tfs.util.Check;

/**
 * Static utility methods for validating and copying many registration types.
 * These are used by the registration data cacheing logic to enhance
 * performance.
 *
 * @threadsafety thread-safe
 */
public abstract class RegistrationUtilities {
    // AKA isToolTypeWellFormed
    public static boolean isToolType(final String toolId) {
        if (toolId == null) {
            return false;
        }

        if (toolId.trim().length() == 0) {
            return false;
        }

        if (toolId.indexOf('/') != -1) {
            return false;
        }

        if (toolId.indexOf('\\') != -1) {
            return false;
        }

        if (toolId.indexOf('.') != -1) {
            return false;
        }

        return true;
    }

    public static RegistrationEntry[] copy(final RegistrationEntry[] entries) {
        Check.notNull(entries, "entries"); //$NON-NLS-1$

        final RegistrationEntry[] copy = new RegistrationEntry[entries.length];

        for (int i = 0; i < entries.length; i++) {
            copy[i] = copy(entries[i]);
        }

        return copy;
    }

    public static RegistrationEntry copy(final RegistrationEntry entry) {
        Check.notNull(entry, "entry"); //$NON-NLS-1$

        ServiceInterface[] serviceInterfaces = entry.getServiceInterfaces();
        if (serviceInterfaces != null) {
            serviceInterfaces = copy(serviceInterfaces);
        }

        Database[] databases = entry.getDatabases();
        if (databases != null) {
            databases = copy(databases);
        }

        EventType[] eventTypes = entry.getEventTypes();
        if (eventTypes != null) {
            eventTypes = copy(eventTypes);
        }

        ArtifactType[] artifactTypes = entry.getArtifactTypes();
        if (artifactTypes != null) {
            artifactTypes = copy(artifactTypes);
        }

        RegistrationExtendedAttribute[] registrationExtendedAttributes = entry.getRegistrationExtendedAttributes();
        if (registrationExtendedAttributes != null) {
            registrationExtendedAttributes = copy(registrationExtendedAttributes);
        }

        return new RegistrationEntry(
            entry.getType(),
            serviceInterfaces,
            databases,
            eventTypes,
            artifactTypes,
            registrationExtendedAttributes);
    }

    public static ServiceInterface[] copy(final ServiceInterface[] serviceInterfaces) {
        Check.notNull(serviceInterfaces, "serviceInterfaces"); //$NON-NLS-1$

        final ServiceInterface[] copy = new ServiceInterface[serviceInterfaces.length];

        for (int i = 0; i < serviceInterfaces.length; i++) {
            copy[i] = copy(serviceInterfaces[i]);
        }

        return copy;
    }

    public static ServiceInterface copy(final ServiceInterface serviceInterface) {
        Check.notNull(serviceInterface, "serviceInterface"); //$NON-NLS-1$

        return new ServiceInterface(serviceInterface.getName(), serviceInterface.getURL());
    }

    public static Database[] copy(final Database[] databases) {
        Check.notNull(databases, "databases"); //$NON-NLS-1$

        final Database[] copy = new Database[databases.length];

        for (int i = 0; i < databases.length; i++) {
            copy[i] = copy(databases[i]);
        }

        return copy;
    }

    public static Database copy(final Database database) {
        Check.notNull(database, "database"); //$NON-NLS-1$

        return new Database(
            database.getName(),
            database.getDatabaseName(),
            database.getSQLServerName(),
            database.getConnectionString(),
            database.isExcludeFromBackup());
    }

    public static EventType[] copy(final EventType[] eventTypes) {
        Check.notNull(eventTypes, "eventTypes"); //$NON-NLS-1$

        final EventType[] copy = new EventType[eventTypes.length];

        for (int i = 0; i < eventTypes.length; i++) {
            copy[i] = copy(eventTypes[i]);
        }

        return copy;
    }

    public static EventType copy(final EventType eventType) {
        Check.notNull(eventType, "eventType"); //$NON-NLS-1$

        return new EventType(eventType.getName(), eventType.getSchema());
    }

    public static ArtifactType[] copy(final ArtifactType[] artifactTypes) {
        Check.notNull(artifactTypes, "artifactTypes"); //$NON-NLS-1$

        final ArtifactType[] copy = new ArtifactType[artifactTypes.length];

        for (int i = 0; i < artifactTypes.length; i++) {
            copy[i] = copy(artifactTypes[i]);
        }

        return copy;
    }

    public static ArtifactType copy(final ArtifactType artifactType) {
        Check.notNull(artifactType, "artifactType"); //$NON-NLS-1$

        OutboundLinkType[] outboundLinkTypes = artifactType.getOutboundLinkTypes();
        if (outboundLinkTypes != null) {
            outboundLinkTypes = copy(outboundLinkTypes);
        }

        return new ArtifactType(artifactType.getName(), outboundLinkTypes);
    }

    public static OutboundLinkType[] copy(final OutboundLinkType[] outboundLinkTypes) {
        Check.notNull(outboundLinkTypes, "outboundLinkTypes"); //$NON-NLS-1$

        final OutboundLinkType[] copy = new OutboundLinkType[outboundLinkTypes.length];

        for (int i = 0; i < outboundLinkTypes.length; i++) {
            copy[i] = copy(outboundLinkTypes[i]);
        }

        return copy;
    }

    public static OutboundLinkType copy(final OutboundLinkType outboundLinkType) {
        Check.notNull(outboundLinkType, "outboundLinkType"); //$NON-NLS-1$

        return new OutboundLinkType(
            outboundLinkType.getName(),
            outboundLinkType.getTargetArtifactTypeTool(),
            outboundLinkType.getTargetArtifactTypeName());
    }

    public static RegistrationExtendedAttribute[] copy(
        final RegistrationExtendedAttribute[] registrationExtendedAttributes) {
        Check.notNull(registrationExtendedAttributes, "registrationExtendedAttributes"); //$NON-NLS-1$

        final RegistrationExtendedAttribute[] copy =
            new RegistrationExtendedAttribute[registrationExtendedAttributes.length];

        for (int i = 0; i < registrationExtendedAttributes.length; i++) {
            copy[i] = copy(registrationExtendedAttributes[i]);
        }

        return copy;
    }

    public static RegistrationExtendedAttribute copy(
        final RegistrationExtendedAttribute registrationExtendedAttribute) {
        Check.notNull(registrationExtendedAttribute, "registrationExtendedAttribute"); //$NON-NLS-1$

        return new RegistrationExtendedAttribute(
            registrationExtendedAttribute.getName(),
            registrationExtendedAttribute.getValue());
    }
}
