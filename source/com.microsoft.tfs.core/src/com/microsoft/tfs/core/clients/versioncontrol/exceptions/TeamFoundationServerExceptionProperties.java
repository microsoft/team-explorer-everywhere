// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.exceptions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * A set of properties returned by various TFS exceptions.
 *
 * @since TEE-SDK-10.1
 */
public class TeamFoundationServerExceptionProperties {
    final private Map<String, Object> properties;

    /**
     * Constructor.
     *
     * Creates an empty property set.
     */
    public TeamFoundationServerExceptionProperties() {
        properties = new HashMap<String, Object>();
    }

    /**
     * Constructor.
     *
     * @param exceptionPropertiesElement
     *        An XML DOM element which is the root of a properties list.
     */
    public TeamFoundationServerExceptionProperties(final Element exceptionPropertiesElement) {
        properties = parse(exceptionPropertiesElement);
    }

    /**
     * Test for an object array property with the given name.
     *
     * @param propertyName
     *        The property name.
     * @return True if the property exists and is an object array.
     */
    public boolean hasObjectArrayProperty(final String propertyName) {
        return properties.containsKey(propertyName) && properties.get(propertyName) instanceof Object[];
    }

    /**
     * Test for a integer property with a given name.
     *
     * @param propertyName
     *        The property name.
     * @return True if property exists and and is an integer.
     */
    public boolean hasIntProperty(final String propertyName) {
        return properties.containsKey(propertyName) && properties.get(propertyName) instanceof Integer;
    }

    /**
     * Test for a boolean property with a given name.
     *
     * @param propertyName
     *        The property name.
     * @return True if the property exists and is a boolean.
     */
    public boolean hasBooleanProperty(final String propertyName) {
        return properties.containsKey(propertyName) && properties.get(propertyName) instanceof Boolean;
    }

    /**
     * Test for a string property with a given name.
     *
     * @param propertyName
     *        The property name.
     * @return True if the property exists and is a string.
     */
    public boolean hasStringProperty(final String propertyName) {
        return properties.containsKey(propertyName) && properties.get(propertyName) instanceof String;
    }

    /**
     * Return an object array for the specified property name.
     *
     * @param propertyName
     *        The property name.
     * @return The object array or null if the property does not exist.
     */
    public Object[] getObjectArrayProperty(final String propertyName) {
        if (hasObjectArrayProperty(propertyName)) {
            return (Object[]) properties.get(propertyName);
        } else {
            return null;
        }
    }

    /**
     * Return a boolean value for the specified property name.
     *
     * @param propertyName
     *        The property name.
     * @return The property value as a boolean value or FALSE if the property
     *         does not exist
     */
    public boolean getBooleanProperty(final String propertyName) {
        if (hasBooleanProperty(propertyName)) {
            return ((Boolean) properties.get(propertyName)).booleanValue();
        } else {
            throw new IllegalStateException();
        }
    }

    /**
     * Return an integer value for the specified property name.
     *
     * @param propertyName
     *        The property name.
     * @return The property value as an integer value or 0 if the property does
     *         not exist.
     */
    public int getIntProperty(final String propertyName) {
        if (hasIntProperty(propertyName)) {
            return ((Integer) properties.get(propertyName)).intValue();
        } else {
            return 0;
        }
    }

    /**
     * Return a string value for the specified property name.
     *
     * @param propertyName
     *        The property name.
     * @return The property value as a string or null if there is no such
     *         property with a string value.
     */
    public String getStringProperty(final String propertyName) {
        if (hasStringProperty(propertyName)) {
            return (String) properties.get(propertyName);
        } else {
            return null;
        }
    }

    /**
     * Return a string array value for the specified property name.
     *
     * @param propertyName
     *        The property name.
     * @return The property value as a string array which can be empty.
     */
    public String[] getStringArrayProperty(final String propertyName) {
        final Object[] objects = getObjectArrayProperty(propertyName);
        final ArrayList<String> strings = new ArrayList<String>();

        if (objects != null) {
            for (final Object object : objects) {
                if (object instanceof String) {
                    strings.add((String) object);
                }
            }
        }
        return strings.toArray(new String[strings.size()]);
    }

    /**
     * Parse the properties from the specified properties element.
     *
     * @param propertiesElement
     *        The DOM element at the root of the properties list.
     * @return A map of property name and value pairs.
     */
    private Map<String, Object> parse(final Element propertiesElement) {
        final HashMap<String, Object> propertiesMap = new HashMap<String, Object>();
        final NodeList propertyNodes = propertiesElement.getElementsByTagName("property"); //$NON-NLS-1$

        if (propertyNodes != null) {
            for (int i = 0; i < propertyNodes.getLength(); i++) {
                final Element propertyElement = (Element) propertyNodes.item(i);
                if (propertyElement != null) {
                    final String propertyName = propertyElement.getAttribute("name"); //$NON-NLS-1$
                    final Object propertyValue = getPropertyValue(propertyElement);
                    propertiesMap.put(propertyName, propertyValue);
                }
            }
        }

        return propertiesMap;
    }

    /**
     * Helper to retrieve the first child of a specifed parent with a specific
     * name.
     *
     * @param parentNode
     *        THe parent node.
     * @param childElementName
     *        The name of the desired child node.
     * @return The element node of a child with the specified name or null.
     */
    private static Element getChildElement(final Node parentNode, final String childElementName) {
        if (parentNode.getNodeType() == Node.ELEMENT_NODE) {
            final Element parentElement = (Element) parentNode;
            final NodeList nodeList = parentElement.getElementsByTagName(childElementName);

            if (nodeList != null && nodeList.getLength() > 0) {
                return (Element) nodeList.item(0);
            }
        }
        return null;
    }

    /**
     * Parse a property value from the given property element. The value may be
     * an array or a simple type.
     *
     * @param propertyElement
     *        A DOM element which represents a property value in an server
     *        exceptions properties list.
     * @return
     */
    private static Object getPropertyValue(final Element propertyElement) {
        final Element propertyValueElement = getChildElement(propertyElement, "value"); //$NON-NLS-1$
        if (propertyValueElement != null) {
            final String valueType = propertyValueElement.getAttribute("xsi:type"); //$NON-NLS-1$
            if (valueType.equals("ArrayOfAnyType")) //$NON-NLS-1$
            {
                final ArrayList list = new ArrayList();
                final NodeList anyTypeNodes = propertyValueElement.getElementsByTagName("anyType"); //$NON-NLS-1$
                for (int i = 0; i < anyTypeNodes.getLength(); i++) {
                    final Element anyTypeElement = (Element) anyTypeNodes.item(i);
                    final String type = anyTypeElement.getAttribute("xsi:type"); //$NON-NLS-1$
                    final String content = anyTypeElement.getTextContent();

                    final Object propertyValue = getValue(type, content);
                    if (propertyValue != null) {
                        list.add(propertyValue);
                    }
                }
                return list.toArray(new Object[list.size()]);
            } else {
                return getValue(valueType, propertyValueElement.getTextContent());
            }
        }
        return null;
    }

    /**
     * Convert the specified string value to the specified primitive type.
     *
     * @param type
     *        The type to convert to.
     * @param value
     *        The string value to be converted.
     * @return The converted value of the desired type.
     */
    private static Object getValue(final String type, final String value) {
        if (type.equals("xsd:string")) //$NON-NLS-1$
        {
            return value;
        } else if (type.equals("xsd:boolean")) //$NON-NLS-1$
        {
            return Boolean.valueOf(value);
        } else if (type.equals("xsd:int")) //$NON-NLS-1$
        {
            try {
                return Integer.valueOf(value);
            } catch (final NumberFormatException e) {
                return null;
            }
        } else {
            return null;
        }
    }
}
