// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.framework.location.internal;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import com.microsoft.tfs.core.clients.framework.location.AccessMapping;
import com.microsoft.tfs.core.clients.framework.location.LocationMapping;
import com.microsoft.tfs.core.clients.framework.location.ServiceDefinition;
import com.microsoft.tfs.core.internal.persistence.StaxSerializer;

import ms.ws._AccessMapping;
import ms.ws._LocationMapping;
import ms.ws._ServiceDefinition;

/**
 * Serializes location data to/from a stream using the StAX API.
 *
 * @threadsafety immutable
 */
public class LocationDataSerializer extends StaxSerializer {
    private static final int SCHEMA_VERSION = 1;

    // XML element names.
    private static final String LOCATION_DATA_ELEMENT_NAME = "LocationServiceConfiguration"; //$NON-NLS-1$
    private static final String LAST_CHANGE_ELEMENT_NAME = "LastChangeId"; //$NON-NLS-1$
    private static final String DEFAULT_MAPPING_MONIKER_ELEMENT_NAME = "DefaultAccessMappingMoniker"; //$NON-NLS-1$
    private static final String VIRTUAL_DIRECTORY_ELEMENT_NAME = "VirtualDirectory"; //$NON-NLS-1$
    private static final String ACCESS_MAPPINGS_ELEMENT_NAME = "AcessMappings"; //$NON-NLS-1$
    private static final String ACCESS_MAPPING_ELEMENT_NAME = "AcessMapping"; //$NON-NLS-1$
    private static final String MONIKER_ELEMENT_NAME = "Moniker"; //$NON-NLS-1$
    private static final String ACCESS_POINT_ELEMENT_NAME = "AccessPoint"; //$NON-NLS-1$
    private static final String DISPLAY_NAME_ELEMENT_NAME = "DisplayName"; //$NON-NLS-1$
    private static final String DESCRIPTION_ELEMENT_NAME = "Description"; //$NON-NLS-1$
    private static final String SERVICES_ELEMENT_NAME = "Services"; //$NON-NLS-1$
    private static final String SERVICE_DEFINITION_ELEMENT_NAME = "ServiceDefinition"; //$NON-NLS-1$
    private static final String SERVICE_TYPE_ELEMENT_NAME = "ServiceType"; //$NON-NLS-1$
    private static final String IDENTIFIER_ELEMENT_NAME = "Identifier"; //$NON-NLS-1$
    private static final String TOOL_ID_ELEMENT_NAME = "ToolId"; //$NON-NLS-1$
    private static final String RELATIVE_PATH_ELEMENT_NAME = "RelativePath"; //$NON-NLS-1$
    private static final String LOCATION_MAPPINGS_ELEMENT_NAME = "LocationMappings"; //$NON-NLS-1$
    private static final String LOCATION_MAPPING_ELEMENT_NAME = "LocationMapping"; //$NON-NLS-1$
    private static final String LOCATION_ELEMENT_NAME = "Location"; //$NON-NLS-1$
    private static final String ACCESS_MAPPING_MONIKER_ELEMENT_NAME = "AcessMappingMoniker"; //$NON-NLS-1$

    // XML attribute names.
    private static final String VERSION_ATTRIBUTE_NAME = "version"; //$NON-NLS-1$
    private static final String RELATIVE_TO_ATTRIBUTE_NAME = "relativeTo"; //$NON-NLS-1$

    @Override
    protected Object deserialize(final XMLStreamReader reader)
        throws XMLStreamException,
            IOException,
            InterruptedException {
        int event;

        int lastChangeId = -1;
        String defaultMappingMoniker = ""; //$NON-NLS-1$
        String virtualDirectoryName = ""; //$NON-NLS-1$

        // Is non-null only when an access mapping is being parsed.
        _AccessMapping accessMapping = null;

        // Is non-null only when a service definition is being parsed.
        _ServiceDefinition serviceDefinition = null;

        // Is non-null only when a location mappings is being parsed.
        _LocationMapping locationMapping = null;

        // Contains the parsed AccessMappings in the cache.
        final List<AccessMapping> accessMappings = new ArrayList<AccessMapping>();

        // Contains all parsed ServiceDefinitions in the cache.
        final List<ServiceDefinition> serviceDefinitions = new ArrayList<ServiceDefinition>();

        // Contains all parsed _LocationMappings. Location mappings are in
        // the context of a ServiceDefinition. When the end tag for a
        // ServiceDefinition is encountered, this list will be assigned to
        // the just completed service definition and the array cleared.
        final List<_LocationMapping> locationMappings = new ArrayList<_LocationMapping>();

        try {
            while ((event = reader.next()) != XMLStreamConstants.END_DOCUMENT) {
                if (event == XMLStreamConstants.START_ELEMENT) {
                    final String localName = reader.getLocalName();
                    if (localName.equals(LAST_CHANGE_ELEMENT_NAME)) {
                        lastChangeId = Integer.valueOf(readTextToElementEnd(reader)).intValue();
                    } else if (localName.equals(DEFAULT_MAPPING_MONIKER_ELEMENT_NAME)) {
                        defaultMappingMoniker = readTextToElementEnd(reader);
                    } else if (localName.equals(VIRTUAL_DIRECTORY_ELEMENT_NAME)) {
                        virtualDirectoryName = readTextToElementEnd(reader);
                    } else if (localName.equals(MONIKER_ELEMENT_NAME)) {
                        accessMapping.setMoniker(readTextToElementEnd(reader));
                    } else if (localName.equals(ACCESS_POINT_ELEMENT_NAME)) {
                        accessMapping.setAccessPoint(readTextToElementEnd(reader));
                    } else if (localName.equals(DISPLAY_NAME_ELEMENT_NAME)) {
                        if (accessMapping != null) {
                            accessMapping.setDisplayName(readTextToElementEnd(reader));
                        } else {
                            serviceDefinition.setDisplayName(readTextToElementEnd(reader));
                        }
                    } else if (localName.equals(DESCRIPTION_ELEMENT_NAME)) {
                        serviceDefinition.setDescription(readTextToElementEnd(reader));
                    } else if (localName.equals(SERVICE_TYPE_ELEMENT_NAME)) {
                        serviceDefinition.setServiceType(readTextToElementEnd(reader));
                    } else if (localName.equals(IDENTIFIER_ELEMENT_NAME)) {
                        serviceDefinition.setIdentifier(readTextToElementEnd(reader));
                    } else if (localName.equals(TOOL_ID_ELEMENT_NAME)) {
                        serviceDefinition.setToolId(readTextToElementEnd(reader));
                    } else if (localName.equals(RELATIVE_PATH_ELEMENT_NAME)) {
                        final String relativeTo = reader.getAttributeValue(null, RELATIVE_TO_ATTRIBUTE_NAME);
                        serviceDefinition.setRelativeToSetting(Integer.valueOf(relativeTo).intValue());
                        serviceDefinition.setRelativePath(readTextToElementEnd(reader));
                    } else if (localName.equals(LOCATION_ELEMENT_NAME)) {
                        locationMapping.setLocation(readTextToElementEnd(reader));
                    } else if (localName.equals(ACCESS_MAPPING_MONIKER_ELEMENT_NAME)) {
                        locationMapping.setAccessMappingMoniker(readTextToElementEnd(reader));
                    } else if (localName.equals(ACCESS_MAPPING_ELEMENT_NAME)) {
                        accessMapping = new _AccessMapping();
                    } else if (localName.equals(SERVICE_DEFINITION_ELEMENT_NAME)) {
                        serviceDefinition = new _ServiceDefinition();
                    } else if (localName.equals(LOCATION_MAPPING_ELEMENT_NAME)) {
                        locationMapping = new _LocationMapping();
                    }
                } else if (event == XMLStreamConstants.END_ELEMENT) {
                    final String localName = reader.getLocalName();
                    if (localName.equals(ACCESS_MAPPING_ELEMENT_NAME)) {
                        accessMappings.add(new AccessMapping(accessMapping));
                        accessMapping = null;
                    } else if (localName.equals(SERVICE_DEFINITION_ELEMENT_NAME)) {
                        final _LocationMapping[] mappings = locationMappings.toArray(new _LocationMapping[0]);
                        locationMappings.clear();

                        serviceDefinition.setLocationMappings(mappings);
                        serviceDefinitions.add(new ServiceDefinition(serviceDefinition));
                        serviceDefinition = null;
                    } else if (localName.equals(LOCATION_MAPPING_ELEMENT_NAME)) {
                        locationMappings.add(locationMapping);
                        locationMapping = null;
                    }
                }
            }
        } catch (final Exception e) {
            return null;
        }

        return new LocationServiceCacheData(
            lastChangeId,
            defaultMappingMoniker,
            virtualDirectoryName,
            accessMappings.toArray(new AccessMapping[accessMappings.size()]),
            serviceDefinitions.toArray(new ServiceDefinition[serviceDefinitions.size()]));
    }

    @Override
    protected void serialize(final Object component, final XMLStreamWriter writer)
        throws XMLStreamException,
            IOException,
            InterruptedException {
        final LocationServiceCacheData data = (LocationServiceCacheData) component;

        writer.writeStartElement(LOCATION_DATA_ELEMENT_NAME);
        writer.writeAttribute(VERSION_ATTRIBUTE_NAME, String.valueOf(SCHEMA_VERSION));

        createChildTextElement(writer, LAST_CHANGE_ELEMENT_NAME, String.valueOf(data.getLastChangeID()));
        createChildTextElement(writer, DEFAULT_MAPPING_MONIKER_ELEMENT_NAME, data.getDefaultMappingMoniker());

        if (data.getVirtualDirectory() != null) {
            createChildTextElement(writer, VIRTUAL_DIRECTORY_ELEMENT_NAME, data.getVirtualDirectory());
        }

        // Write access mappings.
        writer.writeStartElement(ACCESS_MAPPINGS_ELEMENT_NAME);
        final AccessMapping[] accessMappings = data.getAccessMappings();

        for (int i = 0; i < accessMappings.length; i++) {
            final AccessMapping mapping = accessMappings[i];
            writer.writeStartElement(ACCESS_MAPPING_ELEMENT_NAME);

            createChildTextElement(writer, MONIKER_ELEMENT_NAME, mapping.getMoniker());
            createChildTextElement(writer, ACCESS_POINT_ELEMENT_NAME, mapping.getAccessPoint());
            createChildTextElement(writer, DISPLAY_NAME_ELEMENT_NAME, mapping.getDisplayName());

            writer.writeEndElement(); // AccessMapping
        }
        writer.writeEndElement(); // AccessMappings

        // Write service definitions
        writer.writeStartElement(SERVICES_ELEMENT_NAME);
        final ServiceDefinition[] serviceDefinitions = data.getServiceDefinitions();

        for (int i = 0; i < serviceDefinitions.length; i++) {
            final ServiceDefinition definition = serviceDefinitions[i];
            writer.writeStartElement(SERVICE_DEFINITION_ELEMENT_NAME);

            createChildTextElement(writer, SERVICE_TYPE_ELEMENT_NAME, definition.getServiceType());
            createChildTextElement(writer, IDENTIFIER_ELEMENT_NAME, definition.getIdentifier().getGUIDString());

            if (definition.getDisplayName() != null) {
                createChildTextElement(writer, DISPLAY_NAME_ELEMENT_NAME, definition.getDisplayName());
            }
            if (definition.getDescription() != null) {
                createChildTextElement(writer, DESCRIPTION_ELEMENT_NAME, definition.getDescription());
            }
            if (definition.getToolID() != null) {
                createChildTextElement(writer, TOOL_ID_ELEMENT_NAME, definition.getToolID());
            }

            writer.writeStartElement(RELATIVE_PATH_ELEMENT_NAME);
            writer.writeAttribute(
                RELATIVE_TO_ATTRIBUTE_NAME,
                String.valueOf(definition.getRelativeToSetting().toInt()));
            if (definition.getRelativePath() != null) {
                writer.writeCharacters(definition.getRelativePath());
            }
            writer.writeEndElement(); // RelativePath

            // Write location mappings
            writer.writeStartElement(LOCATION_MAPPINGS_ELEMENT_NAME);
            final LocationMapping[] locationMappings = definition.getLocationMappings();

            for (int j = 0; j < locationMappings.length; j++) {
                final LocationMapping mapping = locationMappings[j];
                writer.writeStartElement(LOCATION_MAPPING_ELEMENT_NAME);

                createChildTextElement(writer, ACCESS_MAPPING_MONIKER_ELEMENT_NAME, mapping.getAccessMappingMoniker());
                createChildTextElement(writer, LOCATION_ELEMENT_NAME, mapping.getLocation());

                writer.writeEndElement(); // LocationMapping
            }
            writer.writeEndElement(); // LocationMappings
            writer.writeEndElement(); // ServiceDefinition
        }
        writer.writeEndElement(); // Services
    }
}
