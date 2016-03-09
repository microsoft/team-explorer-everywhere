// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.ws.runtime.types;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import javax.xml.XMLConstants;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;

import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.xml.DOMCreateUtils;

/**
 * An implementation of {@link AnyContentType} where the elements are modeled as
 * DOM {@link Element}s. This is probably the simplest interface for users of
 * the web service runtime to access {@link AnyContentType}, but the entire
 * document is stored in memory, and for large documents a different
 * implementation may be preferred.
 * <p>
 * The {@link Iterator} returned by {@link #getElementIterator()} provides
 * {@link Element}s.
 */
public class DOMAnyContentType implements AnyContentType {
    private Element[] elements = new Element[0];

    public DOMAnyContentType() {
    }

    /**
     * Creates a {@link DOMAnyContentType} with the given elements.
     *
     * @param elements
     *        the initial elements (not null, but may be empty)
     */
    public DOMAnyContentType(final Element[] elements) {
        setElements(elements);
    }

    /**
     * Sets the element data.
     *
     * @param elements
     *        the elements to set (not null, but may be empty)
     */
    public void setElements(final Element[] elements) {
        Check.notNull(elements, "elements"); //$NON-NLS-1$
        this.elements = elements;
    }

    /**
     * @return the {@link Element}s that contain the data (never null)
     */
    public Element[] getElements() {
        return elements;
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(final Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (obj instanceof DOMAnyContentType == false) {
            return false;
        }

        return Arrays.equals(elements, ((DOMAnyContentType) obj).elements);
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        int ret = 1;
        for (int i = 0; i < elements.length; i++) {
            if (elements[i] != null) {
                ret += elements[i].hashCode();
            }
        }

        return ret;
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
        /*
         * The helper method processes the element end event for the currently
         * open element (that's how it knows it's done reading the children).
         */
        elements = readElements(reader);
    }

    /*
     * (non-Javadoc)
     *
     * @seecom.microsoft.tfs.core.ws.runtime.serialization.ElementSerializable#
     * writeAsElement (javax.xml.stream.XMLStreamWriter, java.lang.String)
     */
    @Override
    public void writeAsElement(final XMLStreamWriter writer, final String name) throws XMLStreamException {
        writer.writeStartElement(name);

        writeElements(writer, elements);

        writer.writeEndElement();
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
        return new ArrayList(Arrays.asList(elements)).iterator();
    }

    /*
     * (non-Javadoc)
     *
     * @see com.microsoft.tfs.core.ws.runtime.types.AnyContentType#dispose()
     */
    @Override
    public void dispose() {
        elements = new Element[0];
    }

    public final static Element[] readElements(final XMLStreamReader reader) throws XMLStreamException {
        final List elements = new ArrayList();
        int event;
        do {
            event = reader.next();
            if (event == javax.xml.stream.XMLStreamConstants.START_ELEMENT) {
                elements.add(readElement(reader));
            }
        } while (event != javax.xml.stream.XMLStreamConstants.END_ELEMENT);

        return (Element[]) elements.toArray(new Element[] {});
    }

    public final static Element readElement(final XMLStreamReader reader) throws XMLStreamException {
        int event = reader.getEventType();

        Document document = null;
        Element currentElement = null;

        while (true) {
            if (event == XMLStreamConstants.START_ELEMENT) {
                /*
                 * We need to create a new DOM element node to correspond to the
                 * element the stax parser started.
                 */

                if (document == null) {
                    /*
                     * special case: this is the first element we haven't create
                     * the DOM document yet
                     */

                    document = DOMCreateUtils.newDocument(reader.getLocalName());
                    currentElement = document.getDocumentElement();
                } else {
                    /*
                     * normal case: create a new child element
                     */

                    final Element newElement = document.createElement(reader.getLocalName());
                    currentElement.appendChild(newElement);
                    currentElement = newElement;
                }

                /*
                 * set attributes on the new element
                 */
                final int attributeCount = reader.getAttributeCount();
                for (int i = 0; i < attributeCount; i++) {
                    currentElement.setAttribute(reader.getAttributeLocalName(i), reader.getAttributeValue(i));
                }
            } else if (event == XMLStreamConstants.END_ELEMENT) {
                if (currentElement == document.getDocumentElement()) {
                    /*
                     * special case: if the current element is the document's
                     * root element, we're done
                     */

                    break;
                }

                currentElement = (Element) currentElement.getParentNode();
            } else if (event == XMLStreamConstants.CHARACTERS) {
                final Text textNode = document.createTextNode(reader.getText());
                currentElement.appendChild(textNode);
            }

            event = reader.next();
        }

        return currentElement;
    }

    public static void writeElements(final XMLStreamWriter writer, final Element[] elements) throws XMLStreamException {
        for (int i = 0; i < elements.length; i++) {
            writeElement(writer, elements[i]);
        }
    }

    public static void writeElement(final XMLStreamWriter writer, final Element element) throws XMLStreamException {
        if (element.getPrefix() != null) {
            writer.writeStartElement(element.getPrefix(), element.getLocalName(), element.getNamespaceURI());
            writer.writeNamespace(element.getPrefix(), element.getNamespaceURI());
        } else {
            String elementNamspaceUri = element.getNamespaceURI();

            final String elementName = (elementNamspaceUri == null ? element.getNodeName() : element.getLocalName());

            writer.writeStartElement(elementName);

            if (elementNamspaceUri != null && elementNamspaceUri.length() == 0) {
                elementNamspaceUri = null;
            }
            final String currentNamespaceUri =
                writer.getNamespaceContext().getNamespaceURI(XMLConstants.DEFAULT_NS_PREFIX);

            final boolean equalToCurrentNamspaceURI = (elementNamspaceUri == null ? currentNamespaceUri == null
                : elementNamspaceUri.equals(currentNamespaceUri));

            if (!equalToCurrentNamspaceURI) {
                writer.writeDefaultNamespace(elementNamspaceUri);
                writer.setDefaultNamespace(elementNamspaceUri);
            }
        }

        final NamedNodeMap attributes = element.getAttributes();

        for (int i = 0; i < attributes.getLength(); i++) {
            final Node attributeNode = attributes.item(i);
            writer.writeAttribute(attributeNode.getNodeName(), attributeNode.getNodeValue());
        }

        final NodeList children = element.getChildNodes();

        for (int i = 0; i < children.getLength(); i++) {
            final Node child = children.item(i);
            if (child.getNodeType() == Node.ELEMENT_NODE) {
                writeElement(writer, (Element) child);
            } else if (child.getNodeType() == Node.TEXT_NODE) {
                String value = child.getNodeValue();
                if (value == null) {
                    value = ""; //$NON-NLS-1$
                }
                writer.writeCharacters(value);
            } else if (child.getNodeType() == Node.CDATA_SECTION_NODE) {
                String value = child.getNodeValue();
                if (value == null) {
                    value = ""; //$NON-NLS-1$
                }
                writer.writeCData(value);
            }
        }

        writer.writeEndElement();
    }
}
