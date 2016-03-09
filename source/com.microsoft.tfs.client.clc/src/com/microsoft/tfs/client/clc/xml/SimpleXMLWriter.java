// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.clc.xml;

import java.text.DateFormat;
import java.text.SimpleDateFormat;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamResult;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import com.microsoft.tfs.console.display.Display;
import com.microsoft.tfs.util.Check;

/**
 * A very simple SAX-like XML writer for use in the CLC. Takes a {@link Display}
 * and builds the right SAX classes to write to it, and exposes many SAX
 * {@link TransformerHandler} methods so the writer can be used directly.
 */
public class SimpleXMLWriter {
    /**
     * An ISO 8601 formatter for XML output. Not used directly by this class,
     * but provided so users of this class don't have to construct their own.
     */
    public final static DateFormat ISO_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ"); //$NON-NLS-1$

    final TransformerHandler transformerHandler;
    final Transformer transformer;

    /**
     * Builds a {@link SimpleXMLWriter} for the given display.
     *
     * @param display
     *        the display to build the writer for (not null).
     * @throws TransformerConfigurationException
     *         if for some reason the TransformerHandler cannot be created.
     */
    public SimpleXMLWriter(final Display display) throws TransformerConfigurationException {
        Check.notNull(display, "display"); //$NON-NLS-1$

        final SAXTransformerFactory transformerFactory = (SAXTransformerFactory) SAXTransformerFactory.newInstance();
        transformerHandler = transformerFactory.newTransformerHandler();
        transformer = transformerHandler.getTransformer();

        transformer.setOutputProperty(OutputKeys.ENCODING, "utf-8"); //$NON-NLS-1$
        transformer.setOutputProperty(OutputKeys.INDENT, "yes"); //$NON-NLS-1$
        transformer.setOutputProperty(OutputKeys.METHOD, "xml"); //$NON-NLS-1$
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2"); //$NON-NLS-1$ //$NON-NLS-2$

        // TODO Reference DTD or schema.

        transformerHandler.setResult(new StreamResult(display.getPrintStream()));
    }

    /*
     * The following methods delegate to the TransformerHandler.
     */

    public void startDocument() throws SAXException {
        transformerHandler.startDocument();
    }

    public void endDocument() throws SAXException {
        transformerHandler.endDocument();
    }

    public void characters(final char[] ch, final int start, final int length) throws SAXException {
        transformerHandler.characters(ch, start, length);
    }

    public void comment(final char[] ch, final int start, final int length) throws SAXException {
        transformerHandler.comment(ch, start, length);
    }

    public void endCDATA() throws SAXException {
        transformerHandler.endCDATA();
    }

    public void endDTD() throws SAXException {
        transformerHandler.endDTD();
    }

    public void endElement(final String uri, final String localName, final String name) throws SAXException {
        transformerHandler.endElement(uri, localName, name);
    }

    public void startCDATA() throws SAXException {
        transformerHandler.startCDATA();
    }

    public void startDTD(final String name, final String publicId, final String systemId) throws SAXException {
        transformerHandler.startDTD(name, publicId, systemId);
    }

    public void startElement(final String uri, final String localName, final String name, final Attributes atts)
        throws SAXException {
        transformerHandler.startElement(uri, localName, name, atts);
    }

    public void startEntity(final String name) throws SAXException {
        transformerHandler.startEntity(name);
    }
}
