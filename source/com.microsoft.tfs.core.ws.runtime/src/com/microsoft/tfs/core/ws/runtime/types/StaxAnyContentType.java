// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.ws.runtime.types;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.microsoft.tfs.core.ws.runtime.client.SOAPRequestEntity;
import com.microsoft.tfs.core.ws.runtime.stax.StaxFactoryProvider;
import com.microsoft.tfs.core.ws.runtime.stax.StaxUtils;
import com.microsoft.tfs.util.Closable;
import com.microsoft.tfs.util.temp.FastTempOutputStream;

/**
 * An implementation of {@link AnyContentType} where the XML data can be
 * accessed via {@link XMLStreamReader}.
 * <p>
 * When the deserialized data is small, the source XML is kept in memory, but
 * when it exceeds a threshold it is saved into temporary files on disk. This
 * behavior is transparent to users, but {@link #dispose()} must always be
 * called to free any temporary files.
 * <p>
 * The {@link Iterator} returned by {@link #getElementIterator()} provides
 * {@link XMLStreamReader}s.
 */
public class StaxAnyContentType implements AnyContentType {
    private final static Log log = LogFactory.getLog(StaxAnyContentType.class);

    /**
     * Wraps a {@link File} {@link Iterator} and provides
     * {@link XMLStreamReader}s for those files via {@link #next()}.
     */
    private static class XMLStreamReaderIterator implements Iterator, Closable {
        private final Iterator outputStreamIterator;
        private final XMLInputFactory inputFactory;
        private final List<InputStream> tempInputStreams;

        public XMLStreamReaderIterator(final Iterator fileIterator, final XMLInputFactory inputFactory) {
            outputStreamIterator = fileIterator;
            this.inputFactory = inputFactory;
            this.tempInputStreams = new ArrayList<InputStream>();
        }

        @Override
        public boolean hasNext() {
            return outputStreamIterator.hasNext();
        }

        @Override
        public Object next() {
            final FastTempOutputStream ftos = (FastTempOutputStream) outputStreamIterator.next();

            try {
                final InputStream inputStream = ftos.getInputStream();
                tempInputStreams.add(inputStream);

                final XMLStreamReader reader =
                    inputFactory.createXMLStreamReader(inputStream, SOAPRequestEntity.SOAP_ENCODING);

                return reader;
            } catch (final IOException e) {
                throw new RuntimeException(e);
            } catch (final XMLStreamException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void remove() {
            outputStreamIterator.remove();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void close() {
            for (final InputStream inputStream : tempInputStreams) {
                try {
                    inputStream.close();
                } catch (final IOException e) {
                    log.error("Cannot close input stream.", e); //$NON-NLS-1$
                }
            }
        }
    }

    private final List tempOutputStreams = new ArrayList();

    /**
     * @see FastTempOutputStream#FastTempOutputStream(int, int)
     */
    private int initialHeapStorageSizeBytes = -1;

    /**
     * @see FastTempOutputStream#FastTempOutputStream(int, int)
     */
    private int heapStorageLimitBytes = -1;

    /**
     * Creates a {@link StaxAnyContentType} with default temp storage sizes.
     */
    public StaxAnyContentType() {
    }

    /**
     * Creates a {@link StaxAnyContentType} with the given heap storage limits.
     *
     *
     * @param heapStorageLimitBytes
     *        the (approximate) limit on heap storage to use (per element
     *        deserialzied) before file storage is used instead
     * @param initialHeapStorageSizeBytes
     *        the amount of heap storage to initially allocate (per element
     *        deserialized). Using numbers near the ultimate size of the content
     *        written helps reduce heap allocations and fragmentation (at the
     *        expense of more initial memory used)
     */
    public StaxAnyContentType(final int heapStorageLimitBytes, final int initialHeapStorageSizeBytes) {
        this.heapStorageLimitBytes = heapStorageLimitBytes;
        this.initialHeapStorageSizeBytes = initialHeapStorageSizeBytes;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.microsoft.tfs.core.ws.runtime.types.AnyContentType#dispose()
     */
    @Override
    public void dispose() {
        for (final Iterator i = tempOutputStreams.iterator(); i.hasNext();) {
            final FastTempOutputStream outputStream = (FastTempOutputStream) i.next();

            try {
                outputStream.dispose();
            } catch (final IOException e) {
                throw new RuntimeException("Error storing temporary Stax content", e); //$NON-NLS-1$
            }
        }

        tempOutputStreams.clear();
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * com.microsoft.tfs.core.ws.runtime.types.AnyContentType#getElementIterator
     * ()
     */
    @Override
    public Iterator getElementIterator() {
        return new XMLStreamReaderIterator(tempOutputStreams.iterator(), StaxFactoryProvider.getXMLInputFactory(true));
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * com.microsoft.tfs.core.ws.runtime.serialization.ElementDeserializable
     * #readFromElement (javax.xml.stream.XMLStreamReader)
     */
    @Override
    public void readFromElement(final XMLStreamReader reader) throws XMLStreamException {
        FastTempOutputStream ftos = null;
        XMLStreamWriter writer = null;

        /*
         * When this method is called, the writer is positioned at the element
         * that contains the "any" content. Process the child elements until the
         * container is done.
         */

        /*
         * Advance one event to get the first child.
         */
        int event = reader.next();

        do {
            if (event == XMLStreamConstants.START_ELEMENT) {
                /*
                 * Parse the child element into its own temp file. The copier
                 * will read the child's end element.
                 */
                try {
                    /*
                     * Get a new fast temp output stream.
                     */
                    ftos = new FastTempOutputStream(heapStorageLimitBytes, initialHeapStorageSizeBytes);

                    tempOutputStreams.add(ftos);

                    /*
                     * Create a writer.
                     */
                    writer = StaxFactoryProvider.getXMLOutputFactory().createXMLStreamWriter(
                        ftos,
                        SOAPRequestEntity.SOAP_ENCODING);
                    writer.writeStartDocument();

                    StaxUtils.copyCurrentElement(reader, writer);

                    /*
                     * Make sure to finish off the document.
                     */
                    writer.writeEndDocument();
                } finally {
                    if (writer != null) {
                        writer.close();
                    }

                    /*
                     * Closing writers does not close the underlying stream, so
                     * close that manually. This is required so the temp stream
                     * can be read from.
                     */
                    if (ftos != null) {
                        try {
                            ftos.close();
                        } catch (final IOException e) {
                            log.error(e);
                        }
                    }

                }
            }
        } while ((event = reader.next()) != XMLStreamConstants.END_ELEMENT);
    }

    /*
     * (non-Javadoc)
     *
     * @seecom.microsoft.tfs.core.ws.runtime.serialization.ElementSerializable#
     * writeAsElement (javax.xml.stream.XMLStreamWriter, java.lang.String)
     */
    @Override
    public void writeAsElement(final XMLStreamWriter writer, final String name) throws XMLStreamException {
        final Iterator i = getElementIterator();

        /*
         * Use the regular public iterator for reader access.
         */
        for (; i.hasNext();) {
            final XMLStreamReader reader = (XMLStreamReader) i.next();

            /*
             * Advance one event, beyond the start document, to get the first
             * element.
             */
            reader.next();

            StaxUtils.copyCurrentElement(reader, writer);

            reader.close();
        }

        if (i instanceof Closable) {
            ((Closable) i).close();
        }
    }
}