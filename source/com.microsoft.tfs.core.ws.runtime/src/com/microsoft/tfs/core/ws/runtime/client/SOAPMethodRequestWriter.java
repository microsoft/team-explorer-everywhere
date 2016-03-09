// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.ws.runtime.client;

import java.io.IOException;
import java.io.OutputStream;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

/**
 * Writes the XML elements that make up an entire SOAP method request. For use
 * by generated stubs which want to stream their requests into the SOAP body to
 * avoid storing them in memory before the request is sent.
 */
public interface SOAPMethodRequestWriter {
    /**
     * Write the request information for a SOAP method to the given output
     * stream using the already-configured XML stream writer. The SOAP envelope
     * is already open and will be closed by the caller, so this method doesn't
     * need to do those things. This method must write the start and end
     * elements for the method.
     * <p>
     * Do not close the given XML stream writer or output stream!
     *
     * @param writer
     *        the XML stream writer already configured to write to the given
     *        {@link OutputStream}
     * @param out
     *        the output stream to write the request to (if you need raw stream
     *        access for some reason; make sure anything you do with this stream
     *        is safe with the {@link XMLStreamWriter}
     * @throws XMLStreamException
     *         if an error occured writing XML to the output stream
     * @throws IOException
     *         if an error occurred writing to the output stream
     */
    public void writeSOAPRequest(final XMLStreamWriter writer, final OutputStream out)
        throws XMLStreamException,
            IOException;
}
