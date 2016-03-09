// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.ws.runtime.client;

import java.io.IOException;
import java.io.InputStream;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

/**
 * Reads the XML elements that make up the entire SOAP method response. For use
 * by generated stubs which want to read their results as a stream.
 */
public interface SOAPMethodResponseReader {
    /**
     * Read the response information for a SOAP method to the given output
     * stream using the already-configured XML stream writer. The SOAP envelope
     * and header elements have already been read by the XML reader, so this
     * call does not need to handle those events.
     * <p>
     * Do not close the given XML stream reader or input stream!
     *
     * @param reader
     *        the XML stream reader already configured to read from the given
     *        {@link InputStream}
     * @param in
     *        the input stream to read the request from (if you need raw stream
     *        access for some reason; make sure anything you do with this stream
     *        is safe with the {@link XMLStreamReader}
     * @throws XMLStreamException
     *         if an error occured parsing XML from the input stream
     * @throws IOException
     *         if an error occurred reading from the output stream
     */
    public void readSOAPResponse(final XMLStreamReader reader, final InputStream in)
        throws XMLStreamException,
            IOException;
}
