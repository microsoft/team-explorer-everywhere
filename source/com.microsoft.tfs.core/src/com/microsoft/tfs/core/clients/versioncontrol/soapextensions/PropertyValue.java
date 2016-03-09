// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.soapextensions;

import java.text.MessageFormat;
import java.util.Arrays;

import com.microsoft.tfs.core.internal.wrappers.WebServiceObjectWrapper;
import com.microsoft.tfs.core.ws.runtime.xml.XMLStreamWriterHelper;
import com.microsoft.tfs.util.LocaleInvariantStringHelpers;

import ms.tfs.versioncontrol.clientservices._03._PropertyValue;

/**
 * Holds additional information about a version control item or changeset.
 * Property value names should be compared case-insensitive.
 *
 * @threadsafety thread-compatible
 * @since TEE-SDK-10.1
 */
public class PropertyValue extends WebServiceObjectWrapper {
    /**
     * Tracks whether the value stored in this property has been modified. Must
     * be set to true by any constructor which assigns a value and by any other
     * method which changes the stored value.
     */
    private final boolean valueDirty;

    /**
     * Creates a {@link PropertyValue} with the given name and {@link Object}
     * value. The value type must be serializable via the informal XSD/XSI
     * serializer used by
     * {@link XMLStreamWriterHelper#writeElement(javax.xml.stream.XMLStreamWriter, String, Object)}
     * .
     *
     * @param name
     *        the property name (must not be <code>null</code> or empty)
     * @param value
     *        the value object (may be <code>null</code>)
     */
    public PropertyValue(final String name, final Object value) {
        this(new _PropertyValue(name, value, null, null));
    }

    /**
     * Creates a {@link PropertyValue} from a web service object.
     *
     * @param webServiceObject
     *        the web service object (must not be <code>null</code>)
     */
    public PropertyValue(final _PropertyValue webServiceObject) {
        super(webServiceObject);

        /*
         * This constructor does assign a value object, so set the dirty flag.
         */
        valueDirty = true;
    }

    /**
     * Tests whether this {@link PropertyValue}'s name matches the specified
     * name. Names are compared case-insensitive.
     *
     * @param propertyName
     *        the name to match (may be <code>null</code>)
     * @return <code>true</code> if the names match, <code>false</code> if the
     *         names do not match (possibly because the specified name was
     *         <code>null</code>)
     */
    public boolean matchesName(final String propertyName) {
        return comparePropertyNames(getPropertyName(), propertyName) == 0;
    }

    public static int comparePropertyNames(final String name1, final String name2) {
        return String.CASE_INSENSITIVE_ORDER.compare(name1, name2);
    }

    @Override
    public boolean equals(final Object o) {
        if (o == this) {
            return true;
        }
        if (o instanceof PropertyValue == false) {
            return false;
        }

        final PropertyValue other = (PropertyValue) o;

        // Name is easy (ignore case)
        if (!getWebServiceObject().getPname().equalsIgnoreCase(other.getWebServiceObject().getPname())) {
            return false;
        }

        // Value can take many specialized types
        final Object thisValue = getWebServiceObject().getVal();
        final Object otherValue = other.getWebServiceObject().getVal();

        if (thisValue == otherValue) {
            // Both are same object or both null
            return true;
        }

        if (thisValue == null || otherValue == null) {
            // One item is null
            return false;
        }

        // Both non-null
        if (thisValue instanceof byte[] && otherValue instanceof byte[]) {
            return Arrays.equals((byte[]) thisValue, (byte[]) otherValue);
        }

        // Handles Integer, Double, Calendar, String, etc.
        return thisValue.equals(otherValue);
    }

    @Override
    public int hashCode() {
        int result = 17;

        result = result * 37
            + (getWebServiceObject().getPname() == null ? 0
                : LocaleInvariantStringHelpers.caseInsensitiveHashCode(getWebServiceObject().getPname()));
        result = result * 37 + (getWebServiceObject().getVal() == null ? 0 : getWebServiceObject().getVal().hashCode());

        return result;
    }

    /**
     * Gets the web service object this class wraps. The returned object should
     * not be modified.
     *
     * @return the web service object this class wraps.
     */
    public _PropertyValue getWebServiceObject() {
        return (_PropertyValue) webServiceObject;
    }

    /**
     * @return the name of this property
     */
    public String getPropertyName() {
        return getWebServiceObject().getPname();
    }

    /**
     * @return the object stored as the value for this property
     */
    public Object getPropertyValue() {
        return getWebServiceObject().getVal();
    }

    /**
     * @return the type of the property value
     */
    public Class<?> getPropertyType() {
        final Object value = getPropertyValue();

        if (value != null) {
            return value.getClass();
        }

        return null;
    }

    /**
     * @return true if the property value was set during construction, false if
     *         it has not been modified
     */
    public boolean isDirty() {
        return valueDirty;
    }

    @Override
    public String toString() {
        return MessageFormat.format(
            "PropertyValue [valueDirty={0}, getPropertyName()={1}, getPropertyValue()={2}, getPropertyType()={3}]", //$NON-NLS-1$
            valueDirty,
            getPropertyName(),
            getPropertyValue(),
            getPropertyType());
    }
}
