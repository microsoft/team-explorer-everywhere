// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.externaltools.internal;

import java.io.StringReader;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * SAX parser for Mac OS X plist files, which are squirreled away in many places
 * on Mac OS filesystems as configuration data.
 */
public class PlistHandler extends DefaultHandler {
    /* plist DTD from http://www.apple.com/DTDs/PropertyList-1.0.dtd */
    private final static String APPLE_PLIST_ID = "http://www.apple.com/DTDs/PropertyList-1.0.dtd"; //$NON-NLS-1$
    private final static String APPLE_PLIST_DTD =
        "<!ENTITY % plistObject \"(array | data | date | dict | real | integer | string | true | false )\" >" //$NON-NLS-1$
            + "<!ELEMENT plist %plistObject;>" //$NON-NLS-1$
            + "<!ATTLIST plist version CDATA \"1.0\" >" //$NON-NLS-1$
            + "<!ELEMENT array (%plistObject;)*>" //$NON-NLS-1$
            + "<!ELEMENT dict (key, %plistObject;)*>" //$NON-NLS-1$
            + "<!ELEMENT key (#PCDATA)>" //$NON-NLS-1$
            + "<!ELEMENT string (#PCDATA)>" //$NON-NLS-1$
            + "<!ELEMENT data (#PCDATA)>" //$NON-NLS-1$
            + "<!ELEMENT date (#PCDATA)>" //$NON-NLS-1$
            + "<!ELEMENT true EMPTY>" //$NON-NLS-1$
            + "<!ELEMENT false EMPTY>" //$NON-NLS-1$
            + "<!ELEMENT real (#PCDATA)>" //$NON-NLS-1$
            + "<!ELEMENT integer (#PCDATA)>"; //$NON-NLS-1$

    private Object plist;
    protected ArrayList currentHierarchy = new ArrayList();

    private String currentTag;
    private StringBuffer currentData;
    private String currentKey;

    @Override
    public void startElement(
        final String namespaceURI,
        final String localName,
        final String qualifiedName,
        final Attributes atts) throws SAXException {
        if (qualifiedName.equals("plist")) //$NON-NLS-1$
        {
            // only one plist entry per plist
            if (plist != null) {
                throw new SAXException("Invalid plist (second plist tag)"); //$NON-NLS-1$
            }

            currentData = null;
        } else if (qualifiedName.equals("dict")) //$NON-NLS-1$
        {
            final Map dict = new HashMap();

            if (plist == null) {
                plist = dict;
            }
            currentHierarchy.add(dict);

            currentData = null;
        } else if (qualifiedName.equals("array")) //$NON-NLS-1$
        {
            final List array = new ArrayList();

            if (plist == null) {
                plist = array;
            }
            currentHierarchy.add(array);

            currentData = null;
        } else if (qualifiedName.equals("key") //$NON-NLS-1$
            || qualifiedName.equals("string") //$NON-NLS-1$
            || qualifiedName.equals("integer") //$NON-NLS-1$
            || qualifiedName.equals("real")) //$NON-NLS-1$
        {
            currentData = new StringBuffer();
        } else if (qualifiedName.equals("true") || qualifiedName.equals("false")) //$NON-NLS-1$ //$NON-NLS-2$
        {
            currentData = null;
        } else {
            throw new SAXException(MessageFormat.format("Unknown type: {0}", qualifiedName)); //$NON-NLS-1$
        }

        currentTag = qualifiedName;
    }

    @Override
    public void endElement(final String namespaceURI, final String localName, final String qualifiedName)
        throws SAXException {
        endElementSanityCheck(qualifiedName);

        if (qualifiedName.equals("plist")) //$NON-NLS-1$
        {
            ;
        } else if (qualifiedName.equals("key")) //$NON-NLS-1$
        {
            currentKey = currentData.toString();
        } else {
            if (qualifiedName.equals("array") || qualifiedName.equals("dict")) //$NON-NLS-1$ //$NON-NLS-2$
            {
                currentHierarchy.remove(currentHierarchy.size() - 1);
            } else if (qualifiedName.equals("true")) //$NON-NLS-1$
            {
                add(Boolean.TRUE);
            } else if (qualifiedName.equals("false")) //$NON-NLS-1$
            {
                add(Boolean.FALSE);
            } else {
                final String data = currentData.toString();

                if (qualifiedName.equals("string")) //$NON-NLS-1$
                {
                    add(data);
                } else if (qualifiedName.equals("integer")) //$NON-NLS-1$
                {
                    add(new Integer(Integer.parseInt(data)));
                } else if (qualifiedName.equals("real")) //$NON-NLS-1$
                {
                    add(new Float(Float.parseFloat(data)));
                }
            }

            currentKey = null;
        }

        currentTag = null;
        currentData = null;
    }

    private void endElementSanityCheck(final String element) throws SAXException {
        if (element.equals("array")) //$NON-NLS-1$
        {
            final Object current = currentHierarchy.get(currentHierarchy.size() - 1);

            if (!(current instanceof List)) {
                throw new SAXException(MessageFormat.format("Unexpected end element: {0}", element)); //$NON-NLS-1$
            }
        } else if (element.equals("dict")) //$NON-NLS-1$
        {
            final Object dict = currentHierarchy.get(currentHierarchy.size() - 1);

            if (!(dict instanceof Map)) {
                throw new SAXException(MessageFormat.format("Unexpected end element: {0}", element)); //$NON-NLS-1$
            }
        } else if (element.equals("key") //$NON-NLS-1$
            || element.equals("string") //$NON-NLS-1$
            || element.equals("integer") //$NON-NLS-1$
            || element.equals("real")) //$NON-NLS-1$
        {
            if (!currentTag.equals(element) || currentData == null) {
                throw new SAXException(MessageFormat.format("Unexpected end element: {0}", element)); //$NON-NLS-1$
            }
        } else if (element.equals("true") || element.equals("false")) //$NON-NLS-1$ //$NON-NLS-2$
        {
            if (!currentTag.equals(element) || currentData != null) {
                throw new SAXException(MessageFormat.format("Unexpected element: {0}", element)); //$NON-NLS-1$
            }
        }
    }

    @Override
    public void characters(final char[] ch, final int start, final int length) throws SAXException {
        if (currentData == null) {
            throw new SAXException(MessageFormat.format("Unexpected character data in {0} tag", currentTag)); //$NON-NLS-1$
        }

        for (int i = start; i < (start + length); i++) {
            currentData.append(ch[i]);
        }
    }

    private void add(final Object obj) throws SAXException {
        if (currentHierarchy.size() > 0) {
            final Object struct = currentHierarchy.get(currentHierarchy.size() - 1);

            if (struct instanceof List) {
                ((List) struct).add(obj);
            } else if (struct instanceof Map) {
                ((Map) struct).put(currentKey, obj);
            } else {
                throw new SAXException(MessageFormat.format("Attempt to add to simple structure: {0}", obj.toString())); //$NON-NLS-1$
            }
        } else {
            plist = obj;
        }
    }

    public Object getPlist() {
        return plist;
    }

    /*
     * Override base class so that we never resolve external entities (ie,
     * www.apple.com's DTD)
     */
    @Override
    public InputSource resolveEntity(final String publicId, final String systemId) throws SAXException {
        if (APPLE_PLIST_ID.equals(systemId)) {
            return new InputSource(new StringReader(APPLE_PLIST_DTD));
        }

        return null;
    }
}