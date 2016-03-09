// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.registration.internal;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import com.microsoft.tfs.core.clients.registration.RegistrationData;
import com.microsoft.tfs.core.clients.registration.RegistrationEntry;
import com.microsoft.tfs.core.internal.persistence.StaxSerializer;
import com.microsoft.tfs.core.internal.wrappers.WrapperUtils;

import ms.tfs.services.registration._03._FrameworkRegistrationEntry;

/**
 * Serializes registration data to/from a stream using the StAX API.
 *
 * @threadsafety immutable
 */
public class RegistrationDataSerializer extends StaxSerializer {
    private static final int SCHEMA_VERSION = 1;

    private static final String REGISTRATION_ELEMENT_NAME = "registration"; //$NON-NLS-1$
    private static final String VERSION_ATTRIBUTE_NAME = "version"; //$NON-NLS-1$
    private static final String LAST_REFRESH_TIME_ELMENT_NAME = "last-refresh-time"; //$NON-NLS-1$
    private static final String SERVER_URI_ELEMENT_NAME = "server-uri"; //$NON-NLS-1$
    private static final String REGISTRATION_ENTRIES_ELEMENT_NAME = "RegistrationEntries"; //$NON-NLS-1$
    private static final String REGISTRATION_ENTRY_ELEMENT_NAME = "RegistrationEntry"; //$NON-NLS-1$

    @Override
    protected void serialize(final Object component, final XMLStreamWriter writer)
        throws XMLStreamException,
            IOException,
            InterruptedException {
        final RegistrationData registrationData = (RegistrationData) component;

        writer.writeStartElement(REGISTRATION_ELEMENT_NAME);
        writer.writeAttribute(VERSION_ATTRIBUTE_NAME, String.valueOf(SCHEMA_VERSION));

        createChildTextElement(
            writer,
            LAST_REFRESH_TIME_ELMENT_NAME,
            String.valueOf(registrationData.getLastRefreshTimeMillis()));

        createChildTextElement(writer, SERVER_URI_ELEMENT_NAME, registrationData.getServerURI());

        final RegistrationEntry[] entries = registrationData.getRegistrationEntries(false);
        if (entries.length > 0) {
            writer.writeStartElement(REGISTRATION_ENTRIES_ELEMENT_NAME);

            for (int i = 0; i < entries.length; i++) {
                entries[i].getWebServiceObject().writeAsElement(writer, REGISTRATION_ENTRY_ELEMENT_NAME);
            }

            writer.writeEndElement();
        }

        writer.writeEndElement(); // REGISTRATION_ELEMENT_NAME
    }

    @Override
    protected Object deserialize(final XMLStreamReader reader)
        throws XMLStreamException,
            IOException,
            InterruptedException {
        final List registrationEntries = new ArrayList();
        String tfsUri = null;
        long lastRefreshTimeMillis = -1;

        if (reader.nextTag() != XMLStreamConstants.START_ELEMENT) {
            throw new IllegalStateException();
        }

        if (!REGISTRATION_ELEMENT_NAME.equals(reader.getLocalName())) {
            throw new RuntimeException(MessageFormat.format("unexpected root element: [{0}]", reader.getLocalName())); //$NON-NLS-1$
        }

        int event;

        while ((event = reader.next()) != XMLStreamConstants.END_ELEMENT) {
            if (event == XMLStreamConstants.START_ELEMENT) {
                final String localName = reader.getLocalName();

                if (LAST_REFRESH_TIME_ELMENT_NAME.equals(localName)) {
                    final String s = readTextToElementEnd(reader);
                    try {
                        lastRefreshTimeMillis = Long.parseLong(s);
                    } catch (final NumberFormatException ex) {
                        return null;
                    }
                } else if (SERVER_URI_ELEMENT_NAME.equals(localName)) {
                    tfsUri = readTextToElementEnd(reader);
                    if (tfsUri.trim().length() == 0) {
                        tfsUri = null;
                    }
                } else if (REGISTRATION_ENTRIES_ELEMENT_NAME.equals(localName)) {
                    while ((reader.next()) != XMLStreamConstants.END_ELEMENT) {
                        if (event == XMLStreamConstants.START_ELEMENT) {
                            if (REGISTRATION_ENTRY_ELEMENT_NAME.equals(reader.getLocalName())) {
                                final _FrameworkRegistrationEntry entry = new _FrameworkRegistrationEntry();
                                entry.readFromElement(reader);
                                registrationEntries.add(entry);
                            } else {
                                readToElementEnd(reader);
                            }
                        }
                    }
                }
            }
        }

        final _FrameworkRegistrationEntry[] registrationEntryArray =
            (_FrameworkRegistrationEntry[]) registrationEntries.toArray(
                new _FrameworkRegistrationEntry[registrationEntries.size()]);

        if (registrationEntryArray.length == 0) {
            return null;
        }

        return new RegistrationData(
            (RegistrationEntry[]) WrapperUtils.wrap(RegistrationEntry.class, registrationEntryArray),
            lastRefreshTimeMillis,
            tfsUri);
    }
}
