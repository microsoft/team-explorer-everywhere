// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.framework.location.internal;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import com.microsoft.tfs.core.clients.framework.location.AccessMapping;
import com.microsoft.tfs.core.clients.framework.location.LocationMapping;
import com.microsoft.tfs.core.clients.framework.location.RelativeToSetting;
import com.microsoft.tfs.core.clients.framework.location.ServiceDefinition;
import com.microsoft.tfs.core.ws.runtime.stax.StaxFactoryProvider;

import junit.framework.TestCase;
import ms.ws._AccessMapping;
import ms.ws._LocationMapping;
import ms.ws._ServiceDefinition;

public class LocationDataSerializerTest extends TestCase {
    public void testLocationDataSerialize0am0sd() throws Exception {
        final LocationServiceCacheData dataIn = new LocationServiceCacheData(
            3,
            "dmm", //$NON-NLS-1$
            "v1", //$NON-NLS-1$
            createAccessMappings(0),
            createServiceDefinitions(0));
        final ByteArrayOutputStream out = serializeCacheData(dataIn);
        final LocationServiceCacheData dataOut = deserializeCacheData(new ByteArrayInputStream(out.toByteArray()));
        compareCacheData(dataIn, dataOut);
    }

    public void testLocationDataSerialize0am1sd() throws Exception {
        final LocationServiceCacheData dataIn = new LocationServiceCacheData(
            3,
            "dmm", //$NON-NLS-1$
            "v1", //$NON-NLS-1$
            createAccessMappings(0),
            createServiceDefinitions(1));
        final ByteArrayOutputStream out = serializeCacheData(dataIn);
        final LocationServiceCacheData dataOut = deserializeCacheData(new ByteArrayInputStream(out.toByteArray()));
        compareCacheData(dataIn, dataOut);
    }

    public void testLocationDataSerialize1am0sd() throws Exception {
        final LocationServiceCacheData dataIn = new LocationServiceCacheData(
            3,
            "dmm", //$NON-NLS-1$
            "v1", //$NON-NLS-1$
            createAccessMappings(1),
            createServiceDefinitions(0));
        final ByteArrayOutputStream out = serializeCacheData(dataIn);
        final LocationServiceCacheData dataOut = deserializeCacheData(new ByteArrayInputStream(out.toByteArray()));
        compareCacheData(dataIn, dataOut);
    }

    public void testLocationDataSerialize1am1sd() throws Exception {
        final LocationServiceCacheData dataIn = new LocationServiceCacheData(
            3,
            "dmm", //$NON-NLS-1$
            "v1", //$NON-NLS-1$
            createAccessMappings(1),
            createServiceDefinitions(1));
        final ByteArrayOutputStream out = serializeCacheData(dataIn);
        final LocationServiceCacheData dataOut = deserializeCacheData(new ByteArrayInputStream(out.toByteArray()));
        compareCacheData(dataIn, dataOut);
    }

    public void testLocationDataSerialize1am2sd() throws Exception {
        final LocationServiceCacheData dataIn = new LocationServiceCacheData(
            3,
            "dmm", //$NON-NLS-1$
            "v1", //$NON-NLS-1$
            createAccessMappings(1),
            createServiceDefinitions(2));
        final ByteArrayOutputStream out = serializeCacheData(dataIn);
        final LocationServiceCacheData dataOut = deserializeCacheData(new ByteArrayInputStream(out.toByteArray()));
        compareCacheData(dataIn, dataOut);
    }

    public void testLocationDataSerialize2am1sd() throws Exception {
        final LocationServiceCacheData dataIn = new LocationServiceCacheData(
            3,
            "dmm", //$NON-NLS-1$
            "v1", //$NON-NLS-1$
            createAccessMappings(2),
            createServiceDefinitions(1));
        final ByteArrayOutputStream out = serializeCacheData(dataIn);
        final LocationServiceCacheData dataOut = deserializeCacheData(new ByteArrayInputStream(out.toByteArray()));
        compareCacheData(dataIn, dataOut);
    }

    public void testLocationDataSerialize2am2sd() throws Exception {
        final LocationServiceCacheData dataIn = new LocationServiceCacheData(
            3,
            "dmm", //$NON-NLS-1$
            "v1", //$NON-NLS-1$
            createAccessMappings(2),
            createServiceDefinitions(2));
        final ByteArrayOutputStream out = serializeCacheData(dataIn);
        final LocationServiceCacheData dataOut = deserializeCacheData(new ByteArrayInputStream(out.toByteArray()));
        compareCacheData(dataIn, dataOut);
    }

    private AccessMapping[] createAccessMappings(final int count) {
        final AccessMapping[] accessMappings = new AccessMapping[count];

        for (int i = 0; i < count; i++) {
            final _AccessMapping am = new _AccessMapping();
            am.setAccessPoint("am-ap" + i); //$NON-NLS-1$
            am.setDisplayName("am-dn" + i); //$NON-NLS-1$
            am.setMoniker("am-m" + i); //$NON-NLS-1$
            accessMappings[i] = new AccessMapping(am);
        }

        return accessMappings;
    }

    private ServiceDefinition[] createServiceDefinitions(final int count) {
        final ServiceDefinition[] serviceDefinitions = new ServiceDefinition[count];

        for (int i = 0; i < count; i++) {
            final _ServiceDefinition sd = new _ServiceDefinition();
            sd.setDisplayName("sd-dn" + i); //$NON-NLS-1$
            sd.setDescription("sd-d" + i); //$NON-NLS-1$
            sd.setIdentifier("00000000-0000-0000-0000-000000000001"); //$NON-NLS-1$
            sd.setRelativePath("sd-rp" + i); //$NON-NLS-1$
            sd.setServiceType("sd-st" + i); //$NON-NLS-1$
            sd.setRelativeToSetting(RelativeToSetting.AUTHORITY.toInt());
            sd.setToolId("sd-ti" + i); //$NON-NLS-1$

            final _LocationMapping[] locationMappings = new _LocationMapping[i];
            for (int j = 0; j < locationMappings.length; j++) {
                final _LocationMapping lm = new _LocationMapping();
                lm.setAccessMappingMoniker("lm-amm" + j); //$NON-NLS-1$
                lm.setLocation("lm-l" + j); //$NON-NLS-1$
                locationMappings[j] = lm;
            }
            sd.setLocationMappings(locationMappings);
            serviceDefinitions[i] = new ServiceDefinition(sd);
        }

        return serviceDefinitions;
    }

    private ByteArrayOutputStream serializeCacheData(final LocationServiceCacheData data)
        throws IOException,
            InterruptedException,
            XMLStreamException {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();

        final XMLStreamWriter writer = StaxFactoryProvider.getXMLOutputFactory().createXMLStreamWriter(out, "UTF-8"); //$NON-NLS-1$
        writer.writeStartDocument();

        final LocationDataSerializer serializer = new LocationDataSerializer();
        serializer.serialize(data, writer);

        writer.writeEndDocument();
        writer.flush();

        return out;
    }

    private LocationServiceCacheData deserializeCacheData(final ByteArrayInputStream bytesIn)
        throws IOException,
            InterruptedException,
            XMLStreamException {
        final XMLInputFactory factory = StaxFactoryProvider.getXMLInputFactory(true);
        factory.setProperty(XMLInputFactory.IS_COALESCING, Boolean.TRUE);

        final LocationDataSerializer serializer = new LocationDataSerializer();
        final XMLStreamReader reader = factory.createXMLStreamReader(bytesIn, "UTF-8"); //$NON-NLS-1$
        return (LocationServiceCacheData) serializer.deserialize(reader);
    }

    private void compareCacheData(
        final LocationServiceCacheData original,
        final LocationServiceCacheData deserialized) {
        assertEquals(original.getLastChangeID(), deserialized.getLastChangeID());
        assertEquals(original.getDefaultMappingMoniker(), deserialized.getDefaultMappingMoniker());
        assertEquals(original.getVirtualDirectory(), deserialized.getVirtualDirectory());

        assertEquals(original.getAccessMappings().length, deserialized.getAccessMappings().length);
        assertEquals(original.getServiceDefinitions().length, deserialized.getServiceDefinitions().length);

        for (int i = 0; i < original.getAccessMappings().length; i++) {
            compareAccessMapping(original.getAccessMappings()[i], deserialized.getAccessMappings()[i]);
        }

        for (int i = 0; i < original.getServiceDefinitions().length; i++) {
            compareServiceDefinition(original.getServiceDefinitions()[i], deserialized.getServiceDefinitions()[i]);
        }
    }

    private void compareAccessMapping(final AccessMapping original, final AccessMapping deserialized) {
        assertEquals(original.getAccessPoint(), deserialized.getAccessPoint());
        assertEquals(original.getDisplayName(), deserialized.getDisplayName());
        assertEquals(original.getMoniker(), deserialized.getMoniker());
    }

    private void compareServiceDefinition(final ServiceDefinition original, final ServiceDefinition deserialized) {
        assertEquals(original.getDescription(), deserialized.getDescription());
        assertEquals(original.getDisplayName(), deserialized.getDisplayName());
        assertEquals(original.getRelativePath(), deserialized.getRelativePath());
        assertEquals(original.getServiceType(), deserialized.getServiceType());
        assertEquals(original.getToolID(), deserialized.getToolID());
        assertEquals(original.getIdentifier().getGUIDString(), deserialized.getIdentifier().getGUIDString());
        assertEquals(original.getRelativeToSetting(), deserialized.getRelativeToSetting());

        assertEquals(original.getLocationMappings().length, deserialized.getLocationMappings().length);

        for (int i = 0; i < original.getLocationMappings().length; i++) {
            compareLocationMapping(original.getLocationMappings()[i], deserialized.getLocationMappings()[i]);
        }
    }

    private void compareLocationMapping(final LocationMapping original, final LocationMapping deserialized) {
        assertEquals(original.getAccessMappingMoniker(), deserialized.getAccessMappingMoniker());
        assertEquals(original.getLocation(), deserialized.getLocation());
    }
}
