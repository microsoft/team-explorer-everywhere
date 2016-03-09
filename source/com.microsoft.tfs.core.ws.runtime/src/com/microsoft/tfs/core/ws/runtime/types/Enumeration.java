// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.ws.runtime.types;

import java.text.MessageFormat;
import java.util.Map;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import com.microsoft.tfs.core.ws.runtime.exceptions.SOAPSerializationException;
import com.microsoft.tfs.core.ws.runtime.serialization.AttributeSerializable;
import com.microsoft.tfs.core.ws.runtime.serialization.ElementSerializable;

public abstract class Enumeration implements ElementSerializable, AttributeSerializable {
    protected final String name;

    protected Enumeration(final String name, final Map valuesToInstances) {
        this.name = name;

        valuesToInstances.put(name, this);
    }

    /**
     * @return the name of the enumeration value.
     */
    public String getName() {
        return name;
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(final Object obj) {
        /*
         * Enumeration instances are always singletons so we only need to test
         * for object equality.
         */
        return obj == this;
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        /*
         * Since enumerations are singletons, and only object identity counts
         * (names can be duplicated across instances) we can use the default
         * Object.hashCode().
         */
        return super.hashCode();
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return name;
    }

    /**
     * Gets the apprpriate enumeration instance for the given string value. This
     * method is provided because {@link #readFromAttribute(String)} and
     * {@link #readFromElement(XMLStreamReader)} can't be implemented because
     * they modify an instance of {@link Enumeration}, but instances are
     * restricted and immutable.
     *
     * @param value
     *        the string value (not null)
     * @param valuesToInstances
     *        a map to use to find the instance for the given value
     * @return the appropriate enumeration instance.
     * @throws SOAPSerializationException
     *         if there was no matching enumeration instance.
     */
    public static Enumeration fromString(final String value, final Map valuesToInstances)
        throws SOAPSerializationException {
        final Enumeration ret = (Enumeration) valuesToInstances.get(value);

        if (ret == null) {
            final String messageFormat = "No enumeration matches the attribute value {0}"; //$NON-NLS-1$
            final String message = MessageFormat.format(messageFormat, value);
            throw new SOAPSerializationException(message);
        }

        return ret;
    }

    /*
     * (non-Javadoc)
     *
     * @seecom.microsoft.tfs.core.ws.runtime.types.SOAPAttributeSerializable#
     * readFromAttribute (java.lang.String)
     */
    public void readFromAttribute(final String value) throws XMLStreamException {
        /*
         * Because we're immutable, we can't any data into ourselves.
         */
        final String messageFormat =
            "{0} does not implement readFromAttribute because of its instance-restricted design"; //$NON-NLS-1$
        final String message = MessageFormat.format(messageFormat, Enumeration.class.getName());
        throw new XMLStreamException(message);
    }

    /*
     * (non-Javadoc)
     *
     * @seecom.microsoft.tfs.core.ws.runtime.types.SOAPElementSerializable#
     * readFromElement (javax.xml.stream.XMLStreamReader)
     */
    public void readFromElement(final XMLStreamReader reader) throws XMLStreamException {
        /*
         * Because we're immutable, we can't any data into ourselves.
         */
        final String messageFormat = "{0} does not implement readFromElement because of its instance-restricted design"; //$NON-NLS-1$
        final String message = MessageFormat.format(messageFormat, Enumeration.class.getName());
        throw new XMLStreamException(message);
    }

    /*
     * (non-Javadoc)
     *
     * @seecom.microsoft.tfs.core.ws.runtime.types.SOAPAttributeSerializable#
     * writeAsAttribute (javax.xml.stream.XMLStreamWriter, java.lang.String)
     */
    @Override
    public void writeAsAttribute(final XMLStreamWriter writer, final String name) throws XMLStreamException {
        writer.writeAttribute(name, toString());
    }

    /*
     * (non-Javadoc)
     *
     * @seecom.microsoft.tfs.core.ws.runtime.types.SOAPElementSerializable#
     * writeAsElement (javax.xml.stream.XMLStreamWriter, java.lang.String)
     */
    @Override
    public void writeAsElement(final XMLStreamWriter writer, final String name) throws XMLStreamException {
        writer.writeStartElement(name);
        writer.writeCharacters(toString());
        writer.writeEndElement();
    }
}
