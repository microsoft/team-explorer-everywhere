// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.ws.runtime.serialization;

import javax.xml.stream.XMLStreamException;

/**
 * Can be read from a single XML attribute in an XML element in a SOAP document.
 */
public interface AttributeDeserializable {
    /**
     * Reads data from the from the given attribute value and writes that data
     * to this object.
     *
     * @param value
     *        the value of the attribute (not null, may be empty)
     * @throws XMLStreamException
     *         if an error occurred reading the XML
     */
    public void readFromAttribute(final String value) throws XMLStreamException;
}
