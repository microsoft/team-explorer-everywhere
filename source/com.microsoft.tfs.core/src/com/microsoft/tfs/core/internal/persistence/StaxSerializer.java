// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.internal.persistence;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import com.microsoft.tfs.core.persistence.ObjectSerializer;
import com.microsoft.tfs.core.ws.runtime.stax.StaxFactoryProvider;

/**
 * {@link StaxSerializer} is a {@link ObjectSerializer} implementation that
 * serves as a base class for serializers that use the StAX API. Subclasses
 * implement methods for writing to an {@link XMLStreamWriter} and reading from
 * an {@link XMLStreamReader}.
 *
 * @see ObjectSerializer
 */
public abstract class StaxSerializer implements ObjectSerializer {
    private static final String ENCODING = "UTF-8"; //$NON-NLS-1$

    private static final Object xmlInputFactoryLock = new Object();
    private static XMLInputFactory xmlInputFactory;

    private static final Object xmlOutputFactoryLock = new Object();
    private static XMLOutputFactory xmlOutputFactory;

    private static XMLOutputFactory getXMLOutputFactory() {
        synchronized (xmlOutputFactoryLock) {
            if (xmlOutputFactory == null) {
                xmlOutputFactory = StaxFactoryProvider.getXMLOutputFactory();
            }

            return xmlOutputFactory;
        }
    }

    private static XMLInputFactory getXMLInputFactory() {
        synchronized (xmlInputFactoryLock) {
            if (xmlInputFactory == null) {
                /*
                 * Turn on text coalescing because it makes the parsing easier
                 * and the text is never huge.
                 */
                xmlInputFactory = StaxFactoryProvider.getXMLInputFactory(true);
            }
            return xmlInputFactory;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object deserialize(final InputStream inputStream) throws IOException, InterruptedException {
        try {
            /*
             * Coalescing means text nodes are returned as one event (one
             * String), instead of multiple events.
             */
            final XMLInputFactory factory = getXMLInputFactory();
            factory.setProperty(XMLInputFactory.IS_COALESCING, Boolean.TRUE);

            final XMLStreamReader reader = factory.createXMLStreamReader(inputStream, ENCODING);

            return deserialize(reader);
        } catch (final XMLStreamException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void serialize(final Object object, final OutputStream outputStream)
        throws IOException,
            InterruptedException {
        try {
            final XMLStreamWriter writer = getXMLOutputFactory().createXMLStreamWriter(outputStream, ENCODING);
            writer.writeStartDocument();

            serialize(object, writer);

            writer.writeEndDocument();
            writer.flush();
        } catch (final XMLStreamException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Serialize the specified component to the {@link XMLStreamWriter}.
     *
     * @param object
     *        the component to serialize (must not be <code>null</code>)
     * @param writer
     *        the {@link XMLStreamWriter} (must not be <code>null</code>)
     */
    protected abstract void serialize(Object object, XMLStreamWriter writer)
        throws XMLStreamException,
            IOException,
            InterruptedException;

    /**
     * Reads events off of the {@link XMLStreamReader} to deserialize a
     * component.
     *
     * @param reader
     *        the {@link XMLStreamReader} (must not be <code>null</code>)
     * @return the deserialized component, or <code>null</code> if the component
     *         could not be deserialized
     */
    protected abstract Object deserialize(XMLStreamReader reader)
        throws XMLStreamException,
            IOException,
            InterruptedException;

    protected final void readToElementEnd(final XMLStreamReader reader) throws XMLStreamException {
        int depth = 0;
        int event;
        while ((event = reader.next()) != XMLStreamConstants.END_ELEMENT || depth > 0) {
            if (event == XMLStreamConstants.START_ELEMENT) {
                ++depth;
            } else if (event == XMLStreamConstants.END_ELEMENT) {
                --depth;
            }
        }
    }

    protected final String readTextToElementEnd(final XMLStreamReader reader) throws XMLStreamException {
        final StringBuffer buffer = new StringBuffer();
        int depth = 0;
        int event;
        while ((event = reader.next()) != XMLStreamConstants.END_ELEMENT || depth > 0) {
            if (event == XMLStreamConstants.START_ELEMENT) {
                ++depth;
            } else if (event == XMLStreamConstants.END_ELEMENT) {
                --depth;
            }
            if (event == XMLStreamConstants.CHARACTERS && depth == 0) {
                buffer.append(reader.getText());
            }
        }
        return buffer.toString();
    }

    protected final void createChildTextElement(
        final XMLStreamWriter writer,
        final String elementName,
        final String text) throws XMLStreamException {
        writer.writeStartElement(elementName);
        writer.writeCharacters(text);
        writer.writeEndElement();
    }
}
