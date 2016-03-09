// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.ws.runtime.serialization;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

/**
 * Can be written to a single XML attribute in an XML element in in a SOAP
 * document.
 */
public interface AttributeSerializable {
    /**
     * Writes this item to an XML stream as a single attribute with the given
     * name.
     *
     * @param writer
     *        the XML stream writer (not null)
     * @param name
     *        the name of the attribute to write (not null or empty)
     * @throws XMLStreamException
     *         if an error occurred writing the XML
     */
    public void writeAsAttribute(XMLStreamWriter writer, final String name) throws XMLStreamException;
}
