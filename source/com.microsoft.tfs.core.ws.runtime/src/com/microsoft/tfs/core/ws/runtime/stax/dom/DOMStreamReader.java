/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

/*
 * This implementation comes from the Apache Servicemix project
 * (servicemix.apache.org), and the comments below reference the original Xfire
 * implementation (which has had some bugs fixed in this version).
 */
package com.microsoft.tfs.core.ws.runtime.stax.dom;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.xml.namespace.NamespaceContext;
import javax.xml.namespace.QName;
import javax.xml.stream.Location;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.w3c.dom.Attr;
import org.w3c.dom.CDATASection;
import org.w3c.dom.Comment;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.EntityReference;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.Text;

/**
 * Abstract logic for creating XMLStreamReader from DOM documents. Its works
 * using adapters for Element, Node and Attribute ( @see ElementAdapter }
 *
 * @author <a href="mailto:tsztelak@gmail.com">Tomasz Sztelak</a>
 */
public class DOMStreamReader implements XMLStreamReader {
    public Map properties = new HashMap();

    private final ArrayList frames = new ArrayList();

    private ElementFrame frame;

    private int currentEvent = XMLStreamConstants.START_DOCUMENT;

    private Node content;

    private final Document document;

    private DOMNamespaceContext context;

    /**
     * @param element
     */
    public DOMStreamReader(final Element element) {
        frame = new ElementFrame(element, null);
        frames.add(frame);
        newFrame(frame);
        document = element.getOwnerDocument();
    }

    protected ElementFrame getCurrentFrame() {
        return frame;
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.xml.stream.XMLStreamReader#getProperty(java.lang.String)
     */
    @Override
    public Object getProperty(final String key) throws IllegalArgumentException {
        return properties.get(key);
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.xml.stream.XMLStreamReader#next()
     */
    @Override
    public int next() throws XMLStreamException {
        if (frame.ended) {
            frames.remove(frames.size() - 1);
            if (!frames.isEmpty()) {
                frame = (ElementFrame) frames.get(frames.size() - 1);
            } else {
                currentEvent = END_DOCUMENT;
                return currentEvent;
            }
        }

        if (!frame.started) {
            frame.started = true;
            currentEvent = START_ELEMENT;
        } else if (frame.currentAttribute < getAttributeCount() - 1) {
            frame.currentAttribute++;
            currentEvent = ATTRIBUTE;
        } else if (frame.currentNamespace < getNamespaceCount() - 1) {
            frame.currentNamespace++;
            currentEvent = NAMESPACE;
        } else if (frame.currentChild < getChildCount() - 1) {
            frame.currentChild++;

            currentEvent = moveToChild(frame.currentChild);

            if (currentEvent == START_ELEMENT) {
                final ElementFrame newFrame = getChildFrame(frame.currentChild);
                newFrame.started = true;
                frame = newFrame;
                frames.add(frame);
                currentEvent = START_ELEMENT;

                newFrame(newFrame);
            }
        } else {
            frame.ended = true;
            currentEvent = END_ELEMENT;
            endElement();
        }
        return currentEvent;
    }

    protected void skipFrame() {
        frame.ended = true;
        currentEvent = END_ELEMENT;
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.xml.stream.XMLStreamReader#require(int, java.lang.String,
     * java.lang.String)
     */
    @Override
    public void require(final int arg0, final String arg1, final String arg2) throws XMLStreamException {
        throw new UnsupportedOperationException();
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.xml.stream.XMLStreamReader#nextTag()
     */
    @Override
    public int nextTag() throws XMLStreamException {
        while (hasNext()) {
            final int e = next();
            if (e == START_ELEMENT || e == END_ELEMENT) {
                return e;
            }
        }

        return currentEvent;
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.xml.stream.XMLStreamReader#hasNext()
     */
    @Override
    public boolean hasNext() throws XMLStreamException {
        return !(frames.size() == 0 && frame.ended);

    }

    /*
     * (non-Javadoc)
     *
     * @see javax.xml.stream.XMLStreamReader#close()
     */
    @Override
    public void close() throws XMLStreamException {
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.xml.stream.XMLStreamReader#isStartElement()
     */
    @Override
    public boolean isStartElement() {
        return (currentEvent == START_ELEMENT);
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.xml.stream.XMLStreamReader#isEndElement()
     */
    @Override
    public boolean isEndElement() {
        return (currentEvent == END_ELEMENT);
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.xml.stream.XMLStreamReader#isCharacters()
     */
    @Override
    public boolean isCharacters() {
        return (currentEvent == CHARACTERS);
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.xml.stream.XMLStreamReader#isWhiteSpace()
     */
    @Override
    public boolean isWhiteSpace() {
        return (currentEvent == SPACE);
    }

    @Override
    public int getEventType() {
        return currentEvent;
    }

    @Override
    public int getTextCharacters(final int sourceStart, final char[] target, final int targetStart, int length)
        throws XMLStreamException {
        final char[] src = getText().toCharArray();

        if (sourceStart + length >= src.length) {
            length = src.length - sourceStart;
        }

        for (int i = 0; i < length; i++) {
            target[targetStart + i] = src[i + sourceStart];
        }

        return length;
    }

    @Override
    public boolean hasText() {
        return (currentEvent == CHARACTERS
            || currentEvent == DTD
            || currentEvent == ENTITY_REFERENCE
            || currentEvent == COMMENT
            || currentEvent == SPACE);
    }

    @Override
    public Location getLocation() {
        return new Location() {

            @Override
            public int getCharacterOffset() {
                return 0;
            }

            @Override
            public int getColumnNumber() {
                return 0;
            }

            @Override
            public int getLineNumber() {
                return 0;
            }

            @Override
            public String getPublicId() {
                return null;
            }

            @Override
            public String getSystemId() {
                return null;
            }

        };
    }

    @Override
    public boolean hasName() {
        return (currentEvent == START_ELEMENT || currentEvent == END_ELEMENT);
    }

    @Override
    public String getVersion() {
        return null;
    }

    @Override
    public boolean isStandalone() {
        return false;
    }

    @Override
    public boolean standaloneSet() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public String getCharacterEncodingScheme() {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * Get the document associated with this stream.
     *
     * @return
     */
    public Document getDocument() {
        return document;
    }

    /**
     * Find name spaces declaration in attributes and move them to separate
     * collection.
     */
    protected void newFrame(final ElementFrame frame) {
        final Element element = getCurrentElement();
        frame.uris = new ArrayList();
        frame.prefixes = new ArrayList();
        frame.attributes = new ArrayList();

        if (context == null) {
            context = new DOMNamespaceContext();
        }

        context.setElement(element);

        final NamedNodeMap nodes = element.getAttributes();

        String ePrefix = element.getPrefix();
        if (ePrefix == null) {
            ePrefix = ""; //$NON-NLS-1$
        }

        for (int i = 0; i < nodes.getLength(); i++) {
            final Node node = nodes.item(i);
            String prefix = node.getPrefix();
            final String localName = node.getLocalName();
            final String value = node.getNodeValue();
            final String name = node.getNodeName();

            if (prefix == null) {
                prefix = ""; //$NON-NLS-1$
            }

            if (name != null && name.equals("xmlns")) //$NON-NLS-1$
            {
                frame.uris.add(value);
                frame.prefixes.add(""); //$NON-NLS-1$
            } else if (prefix.length() > 0 && prefix.equals("xmlns")) //$NON-NLS-1$
            {
                frame.uris.add(value);
                frame.prefixes.add(localName);
            } else if (name.startsWith("xmlns:")) //$NON-NLS-1$
            {
                prefix = name.substring(6);
                frame.uris.add(value);
                frame.prefixes.add(prefix);
            } else {
                frame.attributes.add(node);
            }
        }
    }

    protected void endElement() {
    }

    protected Element getCurrentElement() {
        return (Element) getCurrentFrame().element;
    }

    public Element skipElement() {
        final Element e = (Element) getCurrentFrame().element;
        skipFrame();
        return e;
    }

    protected ElementFrame getChildFrame(final int currentChild) {
        return new ElementFrame(getCurrentElement().getChildNodes().item(currentChild), getCurrentFrame());
    }

    protected int getChildCount() {
        return getCurrentElement().getChildNodes().getLength();
    }

    protected int moveToChild(final int currentChild) {
        content = getCurrentElement().getChildNodes().item(currentChild);

        if (content instanceof Text) {
            return CHARACTERS;
        } else if (content instanceof Element) {
            return START_ELEMENT;
        } else if (content instanceof CDATASection) {
            return CDATA;
        } else if (content instanceof Comment) {
            return CHARACTERS;
        } else if (content instanceof EntityReference) {
            return ENTITY_REFERENCE;
        }

        throw new IllegalStateException();
    }

    @Override
    public String getElementText() throws XMLStreamException {
        return getText();
    }

    @Override
    public String getNamespaceURI(final String prefix) {
        ElementFrame frame = getCurrentFrame();

        while (null != frame) {
            final int index = frame.prefixes.indexOf(prefix);
            if (index != -1) {
                return (String) frame.uris.get(index);
            }

            frame = frame.parent;
        }

        return null;
    }

    @Override
    public String getAttributeValue(final String ns, final String local) {
        if (ns == null || ns.equals("")) //$NON-NLS-1$
        {
            return getCurrentElement().getAttribute(local);
        } else {
            return getCurrentElement().getAttributeNS(ns, local);
        }
    }

    @Override
    public int getAttributeCount() {
        return getCurrentFrame().attributes.size();
    }

    Attr getAttribute(final int i) {
        return (Attr) getCurrentFrame().attributes.get(i);
    }

    private String getLocalName(final Attr attr) {

        String name = attr.getLocalName();
        if (name == null) {
            name = attr.getNodeName();
        }
        return name;
    }

    @Override
    public QName getAttributeName(final int i) {
        final Attr at = getAttribute(i);

        final String prefix = at.getPrefix();
        final String ln = getLocalName(at);
        // at.getNodeName();
        final String ns = at.getNamespaceURI();

        if (prefix == null) {
            return new QName(ns, ln);
        } else {
            return new QName(ns, ln, prefix);
        }
    }

    @Override
    public String getAttributeNamespace(final int i) {
        return getAttribute(i).getNamespaceURI();
    }

    @Override
    public String getAttributeLocalName(final int i) {
        final Attr attr = getAttribute(i);
        final String name = getLocalName(attr);
        return name;
    }

    @Override
    public String getAttributePrefix(final int i) {
        return getAttribute(i).getPrefix();
    }

    @Override
    public String getAttributeType(final int i) {
        return toStaxType(getAttribute(i).getNodeType());
    }

    public static String toStaxType(final short jdom) {
        switch (jdom) {
            default:
                return null;
        }
    }

    @Override
    public String getAttributeValue(final int i) {
        return getAttribute(i).getValue();
    }

    @Override
    public boolean isAttributeSpecified(final int i) {
        return getAttribute(i).getValue() != null;
    }

    @Override
    public int getNamespaceCount() {
        return getCurrentFrame().prefixes.size();
    }

    @Override
    public String getNamespacePrefix(final int i) {
        return (String) getCurrentFrame().prefixes.get(i);
    }

    @Override
    public String getNamespaceURI(final int i) {
        return (String) getCurrentFrame().uris.get(i);
    }

    @Override
    public NamespaceContext getNamespaceContext() {
        return context;
    }

    @Override
    public String getText() {
        final Node node = getCurrentElement().getChildNodes().item(getCurrentFrame().currentChild);
        return node.getNodeValue();
    }

    @Override
    public char[] getTextCharacters() {
        return getText().toCharArray();
    }

    @Override
    public int getTextStart() {
        return 0;
    }

    @Override
    public int getTextLength() {
        return getText().length();
    }

    @Override
    public String getEncoding() {
        return null;
    }

    @Override
    public QName getName() {
        final Element el = getCurrentElement();

        final String prefix = getPrefix();
        final String ln = getLocalName();

        if (prefix == null) {
            return new QName(el.getNamespaceURI(), ln);
        } else {
            return new QName(el.getNamespaceURI(), ln, prefix);
        }
    }

    @Override
    public String getLocalName() {
        String name = getCurrentElement().getLocalName();
        // When the element has no namespaces, null is returned
        if (name == null) {
            name = getCurrentElement().getNodeName();
        }
        return name;
    }

    @Override
    public String getNamespaceURI() {
        return getCurrentElement().getNamespaceURI();
    }

    @Override
    public String getPrefix() {
        String prefix = getCurrentElement().getPrefix();
        if (prefix == null) {
            prefix = ""; //$NON-NLS-1$
        }
        return prefix;
    }

    @Override
    public String getPITarget() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getPIData() {
        throw new UnsupportedOperationException();
    }

    public class DOMNamespaceContext implements NamespaceContext {
        private Element currentNode;

        @Override
        public String getNamespaceURI(final String prefix) {
            String name = prefix;
            if (name.length() == 0) {
                name = "xmlns"; //$NON-NLS-1$
            } else {
                name = "xmlns:" + prefix; //$NON-NLS-1$
            }

            return getNamespaceURI(currentNode, name);
        }

        private String getNamespaceURI(final Element e, final String name) {
            final Attr attr = e.getAttributeNode(name);
            if (attr == null) {
                final Node n = e.getParentNode();
                if (n instanceof Element && n != e) {
                    return getNamespaceURI((Element) n, name);
                }
            } else {
                return attr.getValue();
            }

            return null;
        }

        @Override
        public String getPrefix(final String uri) {
            return getPrefix(currentNode, uri);
        }

        private String getPrefix(final Element e, final String uri) {
            final NamedNodeMap attributes = e.getAttributes();
            for (int i = 0; i < attributes.getLength(); i++) {
                final Attr a = (Attr) attributes.item(i);

                final String val = a.getValue();
                if (val != null && val.equals(uri)) {
                    final String name = a.getNodeName();
                    if (name.equals("xmlns")) //$NON-NLS-1$
                    {
                        return ""; //$NON-NLS-1$
                    } else {
                        return name.substring(6);
                    }
                }
            }

            final Node n = e.getParentNode();
            if (n instanceof Element && n != e) {
                return getPrefix((Element) n, uri);
            }

            return null;
        }

        @Override
        public Iterator getPrefixes(final String uri) {
            final List prefixes = new ArrayList();

            final String prefix = getPrefix(uri);
            if (prefix != null) {
                prefixes.add(prefix);
            }

            return prefixes.iterator();
        }

        public Element getElement() {
            return currentNode;
        }

        public void setElement(final Element currentNode) {
            this.currentNode = currentNode;
        }
    }

    public static class ElementFrame {
        public ElementFrame(final Object element, final ElementFrame parent) {
            this.element = element;
            this.parent = parent;
        }

        final Object element;
        final ElementFrame parent;
        boolean started = false;
        boolean ended = false;
        int currentChild = -1;
        int currentAttribute = -1;
        int currentNamespace = -1;
        int currentElement = -1;
        List uris;
        List prefixes;
        List attributes;
        List allAttributes;
    }

}
