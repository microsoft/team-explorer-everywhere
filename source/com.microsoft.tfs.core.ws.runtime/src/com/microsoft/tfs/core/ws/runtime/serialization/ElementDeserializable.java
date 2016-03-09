// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.ws.runtime.serialization;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

/**
 * Can be read from a single XML element in a SOAP document.
 */
public interface ElementDeserializable {
    /**
     * Reads data from the from the open element in the given XML stream and
     * writes that data to this object. The element is always read until its
     * end.
     * <p>
     * The stream must be positioned with the correct element selected (already
     * open). This method must read and process the entire element, child
     * elements, and end element event (which the caller will not see).
     *
     * @param reader
     *        the XML stream reader (not null)
     * @throws XMLStreamException
     *         if an error occurred reading the XML
     */
    public void readFromElement(XMLStreamReader reader) throws XMLStreamException;
}
