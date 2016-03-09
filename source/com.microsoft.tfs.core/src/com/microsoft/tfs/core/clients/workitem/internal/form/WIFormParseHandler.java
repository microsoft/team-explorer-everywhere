// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.internal.form;

import java.io.StringReader;
import java.lang.reflect.Constructor;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import com.microsoft.tfs.core.clients.workitem.form.WIFormElement;
import com.microsoft.tfs.core.clients.workitem.form.WIFormLinksControlExternalLinkFilters;
import com.microsoft.tfs.core.clients.workitem.form.WIFormLinksControlWILinkFilters;
import com.microsoft.tfs.core.clients.workitem.form.WIFormLinksControlWITypeFilters;

public class WIFormParseHandler extends DefaultHandler {
    /**
     * Warning log.
     */
    private static final Log log = LogFactory.getLog(WIFormParseHandler.class);

    /**
     * A map of names to associated object model class constructors for XML
     * elements in a WIT type definition. This table is used when a start tag is
     * parsed to instantiate the corresponding object model instance.
     */
    private static HashMap elementNameMap;

    /**
     * Static initializer for the element name to object model class constructor
     * map.
     */
    static {
        elementNameMap = new HashMap();
        final Class[] emptyArgs = new Class[0];

        try {
            elementNameMap.put(
                WIFormParseConstants.ELEMENT_NAME_FORM.toLowerCase(),
                WIFormDescriptionImpl.class.getConstructor(emptyArgs));
            elementNameMap.put(
                WIFormParseConstants.ELEMENT_NAME_GROUP.toLowerCase(),
                WIFormGroupImpl.class.getConstructor(emptyArgs));
            elementNameMap.put(
                WIFormParseConstants.ELEMENT_NAME_CONTROL.toLowerCase(),
                WIFormControlImpl.class.getConstructor(emptyArgs));
            elementNameMap.put(
                WIFormParseConstants.ELEMENT_NAME_TAB_GROUP.toLowerCase(),
                WIFormTabGroupImpl.class.getConstructor(emptyArgs));
            elementNameMap.put(
                WIFormParseConstants.ELEMENT_NAME_SPLITTER.toLowerCase(),
                WIFormSplitterImpl.class.getConstructor(emptyArgs));
            elementNameMap.put(
                WIFormParseConstants.ELEMENT_NAME_LAYOUT.toLowerCase(),
                WIFormLayoutImpl.class.getConstructor(emptyArgs));
            elementNameMap.put(
                WIFormParseConstants.ELEMENT_NAME_TAB.toLowerCase(),
                WIFormTabImpl.class.getConstructor(emptyArgs));
            elementNameMap.put(
                WIFormParseConstants.ELEMENT_NAME_COLUMN.toLowerCase(),
                WIFormColumnImpl.class.getConstructor(emptyArgs));
            elementNameMap.put(
                WIFormParseConstants.ELEMENT_NAME_LABELTEXT.toLowerCase(),
                WIFormLabelTextImpl.class.getConstructor(emptyArgs));
            elementNameMap.put(
                WIFormParseConstants.ELEMENT_NAME_LINK.toLowerCase(),
                WIFormLinkImpl.class.getConstructor(emptyArgs));
            elementNameMap.put(
                WIFormParseConstants.ELEMENT_NAME_PARAM.toLowerCase(),
                WIFormParamImpl.class.getConstructor(emptyArgs));
            elementNameMap.put(
                WIFormParseConstants.ELEMENT_NAME_TEXT.toLowerCase(),
                WIFormTextImpl.class.getConstructor(emptyArgs));
            elementNameMap.put(
                WIFormParseConstants.ELEMENT_NAME_LINKCOLUMN.toLowerCase(),
                WIFormLinkColumnImpl.class.getConstructor(emptyArgs));
            elementNameMap.put(
                WIFormParseConstants.ELEMENT_NAME_LINKSCONTROLOPTIONS.toLowerCase(),
                WIFormLinksControlOptionsImpl.class.getConstructor(emptyArgs));
            elementNameMap.put(
                WIFormParseConstants.ELEMENT_NAME_LINKCOLUMNS.toLowerCase(),
                WIFormLinkColumnsImpl.class.getConstructor(emptyArgs));
            elementNameMap.put(
                WIFormParseConstants.ELEMENT_NAME_WORKITEMLINKFILTERS.toLowerCase(),
                WIFormLinksControlWILinkFiltersImpl.class.getConstructor(emptyArgs));
            elementNameMap.put(
                WIFormParseConstants.ELEMENT_NAME_WORKITEMTYPEFILTERS.toLowerCase(),
                WIFormLinksControlWITypeFiltersImpl.class.getConstructor(emptyArgs));
            elementNameMap.put(
                WIFormParseConstants.ELEMENT_NAME_EXTERNALLINKFILTERS.toLowerCase(),
                WIFormLinksControlExternalLinkFiltersImpl.class.getConstructor(emptyArgs));
            elementNameMap.put(
                WIFormParseConstants.ELEMENT_NAME_WEBPAGECONTROLOPTIONS.toLowerCase(),
                WIFormWebPageControlOptionsImpl.class.getConstructor(emptyArgs));
            elementNameMap.put(
                WIFormParseConstants.ELEMENT_NAME_CONTENT.toLowerCase(),
                WIFormContentImpl.class.getConstructor(emptyArgs));
        } catch (final Exception e) {
            log.warn(MessageFormat.format("Element constructor not found: {0}", e.getMessage())); //$NON-NLS-1$
        }
    }

    public static WIFormElement parse(final String xml) {
        final WIFormParseHandler handler = new WIFormParseHandler();

        try {
            final SAXParser parser = SAXParserFactory.newInstance().newSAXParser();
            parser.parse(new InputSource(new StringReader(xml)), handler);
        } catch (final SAXException ex) {
            ex.initCause(ex.getException());
            throw new RuntimeException(ex);
        } catch (final Exception ex) {
            throw new RuntimeException(ex);
        }

        return handler.root;
    }

    private final List elementStack = new ArrayList();
    private final Set handledElementNames = new HashSet();
    private WIFormElementImpl root;

    private void push(final WIFormElementImpl element) {
        elementStack.add(0, element);
    }

    private WIFormElementImpl pop() {
        return (WIFormElementImpl) elementStack.remove(0);
    }

    private WIFormElementImpl peek() {
        if (elementStack.size() > 0) {
            return (WIFormElementImpl) elementStack.get(0);
        } else {
            return null;
        }
    }

    private WIFormElementImpl addChildElement(final WIFormElementImpl element) {
        if (peek() != null) {
            peek().addChildElement(element);
        }
        return element;
    }

    public WIFormElementImpl getRoot() {
        return root;
    }

    @Override
    public void startElement(final String uri, final String localName, final String qName, final Attributes attributes)
        throws SAXException {
        final String lowerElementName = qName.toLowerCase();
        handledElementNames.add(lowerElementName);

        if (elementNameMap.containsKey(lowerElementName)) {
            final Constructor c = (Constructor) elementNameMap.get(lowerElementName);
            if (c != null) {
                try {
                    final WIFormElementImpl impl = (WIFormElementImpl) c.newInstance((Object[]) null);
                    impl.startLoading(attributes);
                    push(addChildElement(impl));
                    return;
                } catch (final Exception e) {
                    log.warn(MessageFormat.format("exception for element ({0}): {1}", qName, e.getMessage())); //$NON-NLS-1$
                }
            }
        } else if (lowerElementName.equalsIgnoreCase(WIFormParseConstants.ELEMENT_NAME_FILTER)) {
            // The "Filter" element has three different forms depending on the
            // context where it appears.
            // We can't use the elementNameMap to handle the three separate
            // cases. Handle the directly.
            final WIFormElementImpl parent = peek();
            WIFormElementImpl filter = null;

            // Peek at the parent to see which type of object model instance
            // should be created.
            if (parent instanceof WIFormLinksControlWITypeFilters) {
                filter = new WIFormLinksControlWITypeFilterImpl();
            } else if (parent instanceof WIFormLinksControlWILinkFilters) {
                filter = new WIFormLinksControlWILinkFilterImpl();
            } else if (parent instanceof WIFormLinksControlExternalLinkFilters) {
                filter = new WIFormLinksControlExternalLinkFilterImpl();
            }

            // Process the attributes and and push the element.
            if (filter != null) {
                filter.startLoading(attributes);
                push(addChildElement(filter));
                return;
            }
        }

        handledElementNames.remove(lowerElementName);
        log.warn(MessageFormat.format("unhandled element: {0}", qName)); //$NON-NLS-1$
    }

    @Override
    public void endElement(final String uri, final String localName, final String qName) throws SAXException {
        final String lowerElementName = qName.toLowerCase();
        if (handledElementNames.contains(lowerElementName)) {
            final WIFormElementImpl x = pop();
            x.endLoading();

            if (elementStack.size() == 0) {
                root = x;
            }
        }
    }

    @Override
    public void characters(final char buf[], final int offset, final int len) throws SAXException {
        final WIFormElementImpl element = peek();
        if (element != null) {
            if (element instanceof WIFormTextImpl) {
                final WIFormTextImpl textElement = (WIFormTextImpl) element;
                textElement.appendInnerText(new String(buf, offset, len));
            } else if (element instanceof WIFormContentImpl) {
                final WIFormContentImpl contentElement = (WIFormContentImpl) element;
                contentElement.setContent(new String(buf, offset, len));
            }
        }
    }

    public static Integer readIntegerValue(final Attributes attributes, final String attributeName) {
        final String value = attributes.getValue(attributeName);
        if (value != null) {
            return new Integer(value);
        } else {
            return null;
        }
    }

    public static WIFormSizeAttributeImpl readSizeAttribute(final Attributes attributes, final String attributeName) {
        final String value = attributes.getValue(attributeName);
        if (value != null) {
            return new WIFormSizeAttributeImpl(value);
        } else {
            return null;
        }
    }

    public static WIFormPaddingAttributeImpl readPaddingAttribute(
        final Attributes attributes,
        final String attributeName) {
        final String value = attributes.getValue(attributeName);
        if (value != null) {
            return new WIFormPaddingAttributeImpl(value);
        } else {
            return null;
        }
    }

    public static String readStringValue(final Attributes attributes, final String attributeName) {
        for (int i = 0; i < attributes.getLength(); i++) {
            final String name = attributes.getQName(i);
            if (name != null && name.equalsIgnoreCase(attributeName)) {
                return attributes.getValue(i);
            }
        }

        return null;
    }

    public static boolean readBooleanValue(
        final Attributes attributes,
        final String attributeName,
        final boolean defaultValue) {
        final String value = attributes.getValue(attributeName);

        if (value != null) {
            return Boolean.valueOf(value).booleanValue();
        }

        return defaultValue;
    }

}
