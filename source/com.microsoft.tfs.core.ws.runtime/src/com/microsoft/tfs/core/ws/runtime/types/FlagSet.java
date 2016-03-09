// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.ws.runtime.types;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import com.microsoft.tfs.core.ws.runtime.exceptions.SOAPSerializationException;
import com.microsoft.tfs.core.ws.runtime.serialization.AttributeSerializable;
import com.microsoft.tfs.core.ws.runtime.serialization.ElementSerializable;
import com.microsoft.tfs.core.ws.runtime.xml.XMLStreamReaderHelper;
import com.microsoft.tfs.util.Check;

/**
 * A {@link Set} of {@link Flag}s. Not all the {@link Set} methods are
 * overridden with {@link Flag}-specific types to minimize maintenance and
 * because no specialized action is required. A few extras are provided (
 * {@link #containsOnly(Flag)}, {@link #containsAny(FlagSet)}).
 */
public abstract class FlagSet extends HashSet implements AttributeSerializable, ElementSerializable {
    /**
     * Constructs a {@link FlagSet} with the no flags initially set.
     */
    public FlagSet() {
        super();
    }

    /**
     * Constructs a {@link FlagSet} with the given flags set.
     *
     * @param values
     *        the flags initially contained in this set (not null and elements
     *        not null)
     */
    protected FlagSet(final Flag[] flags) {
        super();

        for (int i = 0; i < flags.length; i++) {
            add(flags[i]);
        }
    }

    /**
     * Constructs a {@link FlagSet} with the given flags set.
     *
     * @param values
     *        the flags initially contained in this set (not null and elements
     *        not null)
     */
    protected FlagSet(final String[] flagStrings) {
        super();

        Check.notNull(flagStrings, "flagStrings"); //$NON-NLS-1$

        for (int i = 0; i < flagStrings.length; i++) {
            add(findFlagInstance(flagStrings[i].trim()));
        }
    }

    /*
     * (non-Javadoc)
     *
     * @seecom.microsoft.tfs.core.ws.runtime.types.SOAPAttributeSerializable#
     * readFromAttribute (java.lang.String)
     */
    public void readFromAttribute(final String value) throws XMLStreamException {
        if (value.length() == 0) {
            throw new XMLStreamException("Can't read a flag set from empty attribute value"); //$NON-NLS-1$
        }

        /*
         * Remove existing flags.
         */
        clear();

        final String[] flagStrings = value.split(" "); //$NON-NLS-1$

        for (int i = 0; i < flagStrings.length; i++) {
            add(findFlagInstance(flagStrings[i].trim()));
        }
    }

    /*
     * (non-Javadoc)
     *
     * @seecom.microsoft.tfs.core.ws.runtime.types.SOAPElementSerializable#
     * readFromElement (javax.xml.stream.XMLStreamReader)
     */
    public void readFromElement(final XMLStreamReader reader) throws XMLStreamException {
        /*
         * Our contents are small enough to read into memory and process like an
         * attribute.
         */
        readFromAttribute(reader.getText());

        XMLStreamReaderHelper.readUntilElementEnd(reader);
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

    /*
     * (non-Javadoc)
     *
     * @see java.util.AbstractCollection#toString()
     */
    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer();

        for (final Iterator i = iterator(); i.hasNext();) {
            if (sb.length() > 0) {
                sb.append(' ');
            }

            sb.append(((Flag) i.next()).name);
        }

        return sb.toString();
    }

    /*
     * Convenience methods for set manipulation.
     */

    /**
     * Finds the flag instance for the given name. This work is delegated to the
     * derived class so the base class doesn't have to interact directly with
     * the mapping.
     *
     * @param value
     *        the flag name
     * @return the instance found
     * @throws SOAPSerializationException
     *         if the flag value does not map to an instance
     */
    protected abstract Flag findFlagInstance(final String value) throws SOAPSerializationException;

    /**
     * Tests whether the given {@link Flag} is the only {@link Flag} set in this
     * {@link FlagSet}.
     *
     * @param flag
     *        the {@link Flag} to test for inclusion in this {@link FlagSet}
     *        (not null)
     * @return true if the given {@link Flag}s is the only flag set in this
     *         {@link FlagSet}, false otherwise
     */
    public boolean containsOnly(final Flag flag) {
        return size() == 1 && super.contains(flag);
    }

    /**
     * Tests whether any of the {@link Flag}s in the given {@link FlagSet} are
     * also set in this {@link FlagSet}.
     *
     * @param other
     *        the other {@link FlagSet}, whose {@link Flag}s are tested against
     *        this {@link FlagSet} (not null)
     * @return true if any of the {@link Flag}s in the given {@link FlagSet} are
     *         also set in this {@link FlagSet}, false if none of the other
     *         type's flags are set in this {@link FlagSet}
     */
    public boolean containsAny(final FlagSet other) {
        for (final Iterator iterator = iterator(); iterator.hasNext();) {
            if (other.contains(iterator.next())) {
                return true;
            }
        }

        return false;
    }
}
