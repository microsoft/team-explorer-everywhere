// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.memento;

import java.io.InputStream;
import java.io.OutputStream;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.microsoft.tfs.core.ws.runtime.stax.StaxFactoryProvider;
import com.microsoft.tfs.util.Check;

/**
 * <p>
 * {@link XMLMemento} is an implementation of {@link Memento} which serializes
 * its data to/from XML streams. The default encoding for these streams is
 * {@link #DEFAULT_ENCODING} if <code>null</code> is specified for
 * {@link #read(InputStream, String)} or {@link #write(OutputStream, String)}.
 * </p>
 *
 * @since TEE-SDK-10.1
 * @threadsafety thread-safe
 */
public class XMLMemento implements Memento {
    /**
     * The encoding used for {@link #read(InputStream, String)} and
     * {@link #write(OutputStream, String)} when <code>null</code> is specified
     * for the encoding parameter.
     */
    public static final String DEFAULT_ENCODING = "UTF-8"; //$NON-NLS-1$

    private static final Log log = LogFactory.getLog(XMLMemento.class);

    /**
     * This {@link Memento}'s name. Never <code>null</code>. Synchronized on
     * this class instance.
     */
    private final String name;

    /**
     * Holds the special "text" data. May be null. Synchronized on this class
     * instance.
     */
    private String textData;

    /**
     * A map of attribute {@link String} names to {@link String} values.
     * Synchronized on this class instance.
     */
    private final Map attributes = new HashMap();

    /**
     * A collection of child {@link Memento}s. Synchronized on this class
     * instance.
     */
    private final List children = new ArrayList();

    /**
     * Reads an {@link XMLMemento} from the next XML element in the given given
     * {@link InputStream} in the encoding specified as
     * {@link #DEFAULT_ENCODING}.
     *
     * @param inputStream
     *        the {@link InputStream} read to read the {@link XMLMemento} from
     *        (must not be <code>null</code>)
     * @param encoding
     *        the encoding to use when reading the {@link InputStream},
     *        <code>null</code> to use the default encoding (
     *        {@link #DEFAULT_ENCODING})
     * @return a Memento modeled as the first Element in the document.
     * @throws MementoException
     *         if an error prevented the creation of the Memento.
     */
    public static XMLMemento read(final InputStream inputStream, final String encoding) throws MementoException {
        Check.notNull(inputStream, "inputStream"); //$NON-NLS-1$

        try {
            final XMLStreamReader reader = StaxFactoryProvider.getXMLInputFactory(true).createXMLStreamReader(
                inputStream,
                (encoding != null) ? encoding : DEFAULT_ENCODING);

            XMLMemento memento = null;
            String localName;
            int event;

            do {
                event = reader.next();

                if (event == XMLStreamConstants.START_ELEMENT) {
                    localName = reader.getLocalName();

                    memento = new XMLMemento(localName);
                    memento.readFromElement(reader);
                }
            } while (event != XMLStreamConstants.END_ELEMENT && event != XMLStreamConstants.END_DOCUMENT);

            reader.close();

            return memento;
        } catch (final XMLStreamException e) {
            log.error("Error reading", e); //$NON-NLS-1$
            throw new MementoException(e);
        }
    }

    /**
     * Reads the current element from the given reader and returns the
     * {@link XMLMemento} read from its data. The {@link XMLStreamReader} must
     * be positioned at {@link XMLStreamConstants#START_ELEMENT}.
     *
     * @param reader
     *        the reader (must not be <code>null</code>)
     * @return the {@link XMLMemento} read from the stream
     * @throws XMLStreamException
     *         if an error occurred reading from the reader
     */
    private synchronized void readFromElement(final XMLStreamReader reader) throws XMLStreamException {
        Check.notNull(reader, "reader"); //$NON-NLS-1$

        /*
         * Read all the attributes from the current element.
         */
        final int attributeCount = reader.getAttributeCount();
        for (int i = 0; i < attributeCount; i++) {
            putString(reader.getAttributeLocalName(i), reader.getAttributeValue(i));
        }

        /*
         * Process child nodes (which may be text or child Mementos).
         */
        String localName;
        int event;
        do {
            event = reader.next();

            if (event == XMLStreamConstants.START_ELEMENT) {
                localName = reader.getLocalName();

                final XMLMemento child = (XMLMemento) createChild(localName);
                child.readFromElement(reader);
            } else if (event == XMLStreamConstants.CHARACTERS) {
                putTextData(reader.getText());
            }
        } while (event != XMLStreamConstants.END_ELEMENT);
    }

    /**
     * Writes this {@link XMLMemento} as an XML element (with child elements for
     * child {@link Memento}s) to the specified {@link OutputStream}.
     *
     * @param outputStream
     *        the output stream to write the document to
     * @param encoding
     *        the encoding to use when writing the {@link OutputStream},
     *        <code>null</code> to use the default encoding (
     *        {@link #DEFAULT_ENCODING}).
     * @throws MementoException
     *         if there is a problem serializing the document to the stream.
     */
    public synchronized void write(final OutputStream outputStream, final String encoding) throws MementoException {
        Check.notNull(outputStream, "outputStream"); //$NON-NLS-1$

        try {
            final XMLStreamWriter writer = StaxFactoryProvider.getXMLOutputFactory().createXMLStreamWriter(
                outputStream,
                (encoding != null) ? encoding : DEFAULT_ENCODING);

            writeAsElement(writer);

            writer.close();
        } catch (final XMLStreamException e) {
            log.error("Error saving", e); //$NON-NLS-1$
            throw new MementoException(e);
        }
    }

    /**
     * Saves this {@link Memento} as an XML Element in the given writer
     *
     * @param writer
     *        the writer (must not be <code>null</code>)
     */
    private void writeAsElement(final XMLStreamWriter writer) throws XMLStreamException {
        writer.writeStartElement(name);

        /*
         * Write all attributes as XML attributes.
         */
        for (final Iterator iterator = attributes.entrySet().iterator(); iterator.hasNext();) {
            final Entry entry = (Entry) iterator.next();
            writer.writeAttribute((String) entry.getKey(), (String) entry.getValue());
        }

        /*
         * Write the text node if there is one.
         */
        if (textData != null) {
            writer.writeCharacters(textData);
        }

        /*
         * Write all children as elements.
         */
        for (final Iterator iterator = children.iterator(); iterator.hasNext();) {
            final XMLMemento child = (XMLMemento) iterator.next();
            child.writeAsElement(writer);
        }

        /*
         * Done with the element.
         */
        writer.writeEndElement();
    }

    /**
     * Creates an {@link XMLMemento} with the given name and an empty set of
     * attributes and children.
     *
     * @param name
     *        the name (must not be <code>null</code> or empty)
     */
    public XMLMemento(final String name) {
        Check.notNullOrEmpty(name, "name"); //$NON-NLS-1$
        this.name = name;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized String getName() {
        return name;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized Memento createChild(final String name) {
        Check.notNullOrEmpty(name, "name"); //$NON-NLS-1$

        final Memento child = new XMLMemento(name);
        children.add(child);
        return child;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized Memento getChild(final String name) {
        Check.notNullOrEmpty(name, "name"); //$NON-NLS-1$

        for (final Iterator iterator = children.iterator(); iterator.hasNext();) {
            final Memento child = (Memento) iterator.next();

            if (child.getName().equals(name)) {
                return child;
            }
        }

        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized Memento[] getChildren(final String name) {
        Check.notNullOrEmpty(name, "name"); //$NON-NLS-1$

        final List results = new ArrayList();

        for (final Iterator iterator = children.iterator(); iterator.hasNext();) {
            final Memento child = (Memento) iterator.next();

            if (child.getName().equals(name)) {
                results.add(child);
            }
        }

        return (Memento[]) results.toArray(new Memento[results.size()]);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized Memento[] getAllChildren() {
        return (Memento[]) children.toArray(new Memento[children.size()]);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized Memento[] removeChildren(final String name) {
        Check.notNullOrEmpty(name, "name"); //$NON-NLS-1$

        final Memento[] matches = getChildren(name);

        children.removeAll(Arrays.asList(matches));

        return matches;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized boolean removeChild(final Memento memento) {
        Check.notNull(memento, "memento"); //$NON-NLS-1$

        return children.remove(memento);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized Map getAllAttributes() {
        /*
         * Return a copy to preserve thread-safety.
         */
        return new HashMap(attributes);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized Double getDouble(final String key) {
        Check.notNullOrEmpty(key, "key"); //$NON-NLS-1$
        final String value = (String) attributes.get(key);

        if (value == null) {
            return null;
        }

        try {
            return new Double(value);
        } catch (final NumberFormatException e) {
            log.warn(MessageFormat.format("Invalid double for key: {0} value: {1}", key, value), e); //$NON-NLS-1$
            return null;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized Float getFloat(final String key) {
        Check.notNullOrEmpty(key, "key"); //$NON-NLS-1$
        final String value = (String) attributes.get(key);

        if (value == null) {
            return null;
        }

        try {
            return new Float(value);
        } catch (final NumberFormatException e) {
            log.warn(MessageFormat.format("Invalid float for key: {0} value: {1}", key, value), e); //$NON-NLS-1$
            return null;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized Integer getInteger(final String key) {
        Check.notNullOrEmpty(key, "key"); //$NON-NLS-1$
        final String value = (String) attributes.get(key);

        if (value == null) {
            return null;
        }

        try {
            return new Integer(value);
        } catch (final NumberFormatException e) {
            log.warn(MessageFormat.format("Invalid integer for key: {0} value: {1}", key, value), e); //$NON-NLS-1$
            return null;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized Long getLong(final String key) {
        Check.notNullOrEmpty(key, "key"); //$NON-NLS-1$
        final String value = (String) attributes.get(key);

        if (value == null) {
            return null;
        }

        try {
            return new Long(value);
        } catch (final NumberFormatException e) {
            log.warn(MessageFormat.format("Invalid long integer for key: {0} value: {1}", key, value), e); //$NON-NLS-1$
            return null;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized String getString(final String key) {
        Check.notNullOrEmpty(key, "key"); //$NON-NLS-1$
        return (String) attributes.get(key);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized Boolean getBoolean(final String key) {
        Check.notNullOrEmpty(key, "key"); //$NON-NLS-1$
        final String value = (String) attributes.get(key);

        if (value == null) {
            return null;
        }

        return Boolean.valueOf(value);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized String getTextData() {
        return textData;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized void putDouble(final String key, final double value) {
        Check.notNullOrEmpty(key, "key"); //$NON-NLS-1$
        attributes.put(key, String.valueOf(value));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized void putFloat(final String key, final float value) {
        Check.notNullOrEmpty(key, "key"); //$NON-NLS-1$
        attributes.put(key, String.valueOf(value));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized void putInteger(final String key, final int value) {
        Check.notNullOrEmpty(key, "key"); //$NON-NLS-1$
        attributes.put(key, String.valueOf(value));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized void putLong(final String key, final long value) {
        Check.notNullOrEmpty(key, "key"); //$NON-NLS-1$
        attributes.put(key, String.valueOf(value));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized void putString(final String key, final String value) {
        Check.notNullOrEmpty(key, "key"); //$NON-NLS-1$
        attributes.put(key, value);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized void putBoolean(final String key, final boolean value) {
        Check.notNullOrEmpty(key, "key"); //$NON-NLS-1$
        attributes.put(key, String.valueOf(value));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized void putTextData(final String data) {
        textData = data;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized void putMemento(final Memento memento) {
        Check.notNull(memento, "memento"); //$NON-NLS-1$

        /*
         * Copy the text node.
         */
        final String otherText = memento.getTextData();
        if (otherText != null) {
            putTextData(memento.getTextData());
        }

        /*
         * Copy all attributes.
         */
        attributes.putAll(memento.getAllAttributes());

        /*
         * Copy all children (and their children, etc.).
         */
        final Memento[] allChildren = memento.getAllChildren();
        for (int i = 0; i < allChildren.length; i++) {
            final Memento child = createChild(allChildren[i].getName());
            child.putMemento(allChildren[i]);
        }
    }
}
