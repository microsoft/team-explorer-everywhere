// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.ws.runtime.stax;

import java.text.MessageFormat;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.StringUtil;

public abstract class StaxUtils {
    /**
     * Gets the element text or null if the element has no text. The standard
     * {@link XMLStreamReader#getElementText()} implementation never returns
     * null, only the empty string, even when there is no text (empty element
     * &lt;fun/&gt; or &lt;fun&gt;&lt;/fun&gt;). This method returns null when
     * there are no {@link XMLStreamConstants#CHARACTERS} events before the next
     * {@link XMLStreamConstants#END_ELEMENT} event.
     *
     * @param reader
     *        the reader (not null)
     * @return the text in the element, null if there was no text before the end
     *         of the element
     */
    public static String getElementTextOrNull(final XMLStreamReader reader) throws XMLStreamException {
        /*
         * This implementation is mostly the one recommended in the Javadoc
         * comments on the getElementText() method in the standard API interface
         * itself. However, if no character, cdata, space or entity reference is
         * encountered, the return value is null (not empty string).
         */
        if (reader.getEventType() != XMLStreamConstants.START_ELEMENT) {
            throw new XMLStreamException("parser must be on START_ELEMENT to read next text", reader.getLocation()); //$NON-NLS-1$
        }

        int eventType = reader.next();
        StringBuffer content = null;
        while (eventType != XMLStreamConstants.END_ELEMENT) {
            if (eventType == XMLStreamConstants.CHARACTERS
                || eventType == XMLStreamConstants.CDATA
                || eventType == XMLStreamConstants.SPACE
                || eventType == XMLStreamConstants.ENTITY_REFERENCE) {
                if (content == null) {
                    content = new StringBuffer();
                }
                content.append(reader.getText());
            } else if (eventType == XMLStreamConstants.PROCESSING_INSTRUCTION
                || eventType == XMLStreamConstants.COMMENT) {
                // skipping
            } else if (eventType == XMLStreamConstants.END_DOCUMENT) {
                throw new XMLStreamException(
                    "unexpected end of document when reading element text content", //$NON-NLS-1$
                    reader.getLocation());
            } else if (eventType == XMLStreamConstants.START_ELEMENT) {
                throw new XMLStreamException(
                    "element text content may not contain START_ELEMENT", //$NON-NLS-1$
                    reader.getLocation());
            } else if (eventType == XMLStreamConstants.ATTRIBUTE) {
                // Skip
            } else {
                final String messageFormat = "Unexpected event type {0}"; //$NON-NLS-1$
                final String message = MessageFormat.format(messageFormat, eventType);
                throw new XMLStreamException(message, reader.getLocation());
            }
            eventType = reader.next();
        }

        return (content == null) ? null : content.toString();
    }

    /**
     * Copies the current element (and all its children) in the given
     * {@link XMLStreamReader} to the given writer.
     *
     * @param reader
     *        the reader (not null)
     * @param writer
     *        the writer (not null)
     * @throws XMLStreamException
     */
    public static void copyCurrentElement(final XMLStreamReader reader, final XMLStreamWriter writer)
        throws XMLStreamException {
        Check.notNull(reader, "reader"); //$NON-NLS-1$
        Check.notNull(writer, "writer"); //$NON-NLS-1$

        int event = reader.getEventType();

        Check.isTrue(event == XMLStreamConstants.START_ELEMENT, "event == XMLStreamConstants.START_ELEMENT"); //$NON-NLS-1$

        /*
         * Start element depth at 1, increment when an element is started (not
         * including the element that we start with), decrement when an element
         * is ended, and when it goes to 0 we've read the end of the original
         * reader's element.
         */
        int elementDepth = 1;

        boolean firstTime = true;
        while (true) {
            switch (event) {
                case XMLStreamConstants.START_ELEMENT:
                    writer.writeStartElement(reader.getPrefix(), reader.getLocalName(), reader.getNamespaceURI());

                    final int attributeCount = reader.getAttributeCount();
                    for (int i = 0; i < attributeCount; i++) {
                        final String prefix = reader.getAttributePrefix(i);
                        final String nameSpace = reader.getAttributeNamespace(i);
                        final String localName = reader.getAttributeLocalName(i);
                        final String value = reader.getAttributeValue(i);

                        writer.writeAttribute(
                            StringUtil.isNullOrEmpty(prefix) ? StringUtil.EMPTY : prefix,
                            StringUtil.isNullOrEmpty(nameSpace) ? StringUtil.EMPTY : nameSpace,
                            StringUtil.isNullOrEmpty(localName) ? StringUtil.EMPTY : localName,
                            StringUtil.isNullOrEmpty(value) ? StringUtil.EMPTY : value);
                    }

                    /*
                     * Don't increment depth the first time through, because the
                     * caller opened the element.
                     */
                    if (firstTime) {
                        firstTime = false;
                    } else {
                        elementDepth++;
                    }

                    break;
                case XMLStreamConstants.END_ELEMENT:
                    writer.writeEndElement();
                    elementDepth--;

                    if (elementDepth < 1) {
                        /*
                         * We just wrote the end element for the original
                         * element.
                         */
                        return;
                    }

                    break;
                case XMLStreamConstants.PROCESSING_INSTRUCTION:
                    writer.writeProcessingInstruction(reader.getPITarget(), reader.getPIData());
                    break;
                case XMLStreamConstants.CHARACTERS:
                    writer.writeCharacters(reader.getText());
                    break;
                case XMLStreamConstants.COMMENT:
                    writer.writeComment(reader.getText());
                    break;
                case XMLStreamConstants.SPACE:
                    // Ignore.
                    break;
                case XMLStreamConstants.START_DOCUMENT:
                    throw new RuntimeException("XMLStreamConstants.START_DOCUMENT shouldn't happen in element copy"); //$NON-NLS-1$
                case XMLStreamConstants.END_DOCUMENT:
                    throw new RuntimeException("XMLStreamConstants.END_DOCUMENT shouldn't happen in element copy"); //$NON-NLS-1$
                case XMLStreamConstants.ENTITY_REFERENCE:
                    writer.writeEntityRef(reader.getLocalName());
                    break;
                case XMLStreamConstants.ATTRIBUTE:
                    throw new RuntimeException("XMLStreamConstants.ATTRIBUTE shouldn't happen in element copy"); //$NON-NLS-1$
                case XMLStreamConstants.DTD:
                    writer.writeDTD(reader.getText());
                    break;
                case XMLStreamConstants.CDATA:
                    writer.writeCData(reader.getText());
                    break;
                case XMLStreamConstants.NAMESPACE:
                    writer.writeNamespace(reader.getNamespacePrefix(0), reader.getNamespaceURI(0));
                    break;
                case XMLStreamConstants.NOTATION_DECLARATION:
                    throw new RuntimeException("Can't handle XMLStreamConstants.NOTATION_DECLARATION"); //$NON-NLS-1$
                case XMLStreamConstants.ENTITY_DECLARATION:
                    throw new RuntimeException("Can't handle XMLStreamConstants.ENTITY_DECLARATION"); //$NON-NLS-1$
                default:
                    final String messageFormat = "Unhandled XMLStreamReader event {0}"; //$NON-NLS-1$
                    final String message = MessageFormat.format(messageFormat, reader.getEventType());
                    throw new RuntimeException(message);
            }

            event = reader.next();
        }
    }
}
