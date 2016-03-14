// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.build.internal.utils;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.microsoft.tfs.core.Messages;
import com.microsoft.tfs.util.StringUtil;
import com.microsoft.tfs.util.xml.DOMCreateUtils;
import com.microsoft.tfs.util.xml.DOMSerializeUtils;
import com.microsoft.tfs.util.xml.DOMUtils;
import com.microsoft.tfs.util.xml.XMLException;

public class XamlHelper {
    private static final Log log = LogFactory.getLog(XamlHelper.class);

    private static final String XAML_NAMESPACE = "http://schemas.microsoft.com/winfx/2006/xaml"; //$NON-NLS-1$
    private static final String XML_NAMESPACE = "http://www.w3.org/XML/1998/namespace"; //$NON-NLS-1$

    private static final String NEWLINE = System.getProperty("line.separator"); //$NON-NLS-1$

    public static String save(final Properties properties) {
        //
        // Only support parameters of type String for now
        //

        // The result should look something like this:
        //
        // <Dictionary x:TypeArguments="p:String, p:Object"
        // xmlns="clr-namespace:System.Collections.Generic;assembly=mscorlib"
        // xmlns:p="http://schemas.microsoft.com/netfx/2008/xaml/schema"
        // xmlns:x="http://schemas.microsoft.com/winfx/2006/xaml">
        // <p:String x:Key="TestCategory" xml:space="preserve">Test
        // Category</p:String>
        // <p:String x:Key="SymbolStorePath"
        // xml:space="preserve">\\path\to\symbols</p:String>
        // </Dictionary>

        // Note that we're doing this using a string buffer to avoid JAXP
        // implementation issues when adding the clr-namespace declaration that
        // has issues between Sun's Java 1.4 and Java 6

        final StringBuffer xaml = new StringBuffer();
        xaml.append("<Dictionary x:TypeArguments=\"x:String, x:Object\" " //$NON-NLS-1$
            + "xmlns=\"clr-namespace:System.Collections.Generic;assembly=mscorlib\" " //$NON-NLS-1$
            + "xmlns:x=\"" //$NON-NLS-1$
            + XAML_NAMESPACE
            + "\">"); //$NON-NLS-1$
        xaml.append(NEWLINE);

        for (final Iterator it = properties.entrySet().iterator(); it.hasNext();) {
            final Entry entry = (Entry) it.next();
            final String name = escapeValue((String) entry.getKey());
            final String value = escapeValue((String) entry.getValue());

            xaml.append("    <x:String x:Key=\""); //$NON-NLS-1$
            xaml.append(name);
            xaml.append("\" xml:space=\"preserve\">"); //$NON-NLS-1$
            xaml.append(value);
            xaml.append("</x:String>"); //$NON-NLS-1$
            xaml.append(NEWLINE);
        }
        xaml.append("</Dictionary>"); //$NON-NLS-1$

        return xaml.toString();
    }

    /**
     * Escape the value string as per
     * http://www.w3.org/TR/2008/REC-xml-20081126/#syntax
     */
    private static String escapeValue(final String rawString) {
        String escaped = rawString;
        escaped = StringUtil.replace(escaped, "&", "&amp;"); //$NON-NLS-1$ //$NON-NLS-2$
        escaped = StringUtil.replace(escaped, "<", "&lt;"); //$NON-NLS-1$ //$NON-NLS-2$
        escaped = StringUtil.replace(escaped, ">", "&gt;"); //$NON-NLS-1$ //$NON-NLS-2$
        escaped = StringUtil.replace(escaped, "'", "&apos;"); //$NON-NLS-1$ //$NON-NLS-2$
        escaped = StringUtil.replace(escaped, "\"", "&quot;"); //$NON-NLS-1$ //$NON-NLS-2$
        return escaped;
    }

    public static String updateProperties(final String originalXaml, final Properties properties) {
        final ArrayList keys = new ArrayList(properties.keySet());

        final Document document = DOMCreateUtils.parseString(originalXaml);
        final Element root = document.getDocumentElement();

        // first update any properties that we already have
        final NodeList nodes = root.getElementsByTagName("x:String"); //$NON-NLS-1$
        for (int i = 0; i < nodes.getLength(); i++) {
            final Element element = (Element) nodes.item(i);
            final String key = element.getAttribute("x:Key"); //$NON-NLS-1$
            element.getFirstChild().getNodeValue();

            if (properties.containsKey(key)) {
                keys.remove(key);
                element.getFirstChild().setNodeValue(properties.getProperty(key));
            }
        }

        // now add any new properties to the xaml
        for (final Iterator it = keys.iterator(); it.hasNext();) {
            final String key = (String) it.next();
            final Element element = DOMUtils.appendChild(root, "x:String"); //$NON-NLS-1$
            element.setAttributeNS(XAML_NAMESPACE, "x:Key", key); //$NON-NLS-1$
            element.setAttributeNS(XML_NAMESPACE, "xml:space", "preserve"); //$NON-NLS-1$ //$NON-NLS-2$
            DOMUtils.appendText(element, properties.getProperty(key));
        }

        return DOMSerializeUtils.toString(root, DOMSerializeUtils.INDENT).trim();
    }

    public static Properties loadPartial(final String xaml) {
        final Properties properties = new Properties();

        try {
            final Document document = DOMCreateUtils.parseString(xaml);
            final Element root = document.getDocumentElement();

            final NodeList nodes = root.getElementsByTagName("x:String"); //$NON-NLS-1$
            for (int i = 0; i < nodes.getLength(); i++) {
                final Element element = (Element) nodes.item(i);
                final String key = element.getAttribute("x:Key"); //$NON-NLS-1$
                final String value = element.getFirstChild().getNodeValue();
                properties.put(key, value);
            }
        } catch (final XMLException e) {
            log.error(
                MessageFormat.format(Messages.getString("XamlHelper.ExceptionParsingProcessParaemtersFormat"), xaml), //$NON-NLS-1$
                e);
        }

        return properties;
    }
}
