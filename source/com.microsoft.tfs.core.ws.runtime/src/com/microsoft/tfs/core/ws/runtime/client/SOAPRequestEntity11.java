// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.ws.runtime.client;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import com.microsoft.tfs.core.ws.runtime.Schemas;
import com.microsoft.tfs.core.ws.runtime.types.DOMAnyContentType;

/**
 * An HttpClient request entity implementation suited for SOAP 1.1 request use.
 */
public class SOAPRequestEntity11 extends SOAPRequestEntity {
    public SOAPRequestEntity11(
        final String methodName,
        final String defaultNamespace,
        final SOAPMethodRequestWriter requestWriter) {
        super(methodName, defaultNamespace, requestWriter);
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * com.microsoft.tfs.core.ws.runtime.client.SOAPRequestEntity#getContentType
     * ()
     */
    @Override
    public String getContentType() {
        return "text/xml; charset=" + SOAP_ENCODING; //$NON-NLS-1$
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * com.microsoft.tfs.core.ws.runtime.client.SOAPRequestEntity#writeRequest
     * (java.io .OutputStream)
     */
    @Override
    public void writeRequest(final OutputStream out) throws IOException {
        XMLStreamWriter writer;

        try {
            writer = getXMLOutputFactory().createXMLStreamWriter(
                new BufferedOutputStream(out),
                SOAPRequestEntity.SOAP_ENCODING);

            writer.writeStartDocument();
            writer.writeStartElement("soap", "Envelope", Schemas.SOAP_11); //$NON-NLS-1$ //$NON-NLS-2$
            writer.writeNamespace("soap", Schemas.SOAP_11); //$NON-NLS-1$
            writer.writeNamespace("xsi", Schemas.XSI); //$NON-NLS-1$
            writer.writeNamespace("xsd", Schemas.XSD); //$NON-NLS-1$

            if (getSoapHeaderProvider() != null) {
                writer.writeStartElement("soap", "Header", Schemas.SOAP_11); //$NON-NLS-1$ //$NON-NLS-2$

                // TODO make the soap header serializable
                DOMAnyContentType.writeElement(writer, getSoapHeaderProvider().getSOAPHeader());

                writer.writeEndElement();
            }

            writer.writeStartElement("soap", "Body", Schemas.SOAP_11); //$NON-NLS-1$ //$NON-NLS-2$
            writer.setDefaultNamespace(getDefaultNamespace());
            writer.writeDefaultNamespace(getDefaultNamespace());

            /*
             * Do not write the method name, since the requestWriter handles
             * that (and the rest of the request), if it was supplied.
             */
            try {
                if (getRequestWriter() != null) {
                    getRequestWriter().writeSOAPRequest(writer, out);
                }
            } catch (final XMLStreamException e) {
                throw new IOException(e.getMessage());
            }

            /*
             * StAX lets us omit these writeEndElement calls and just call
             * writeEndDocument, which will close any open elements. We don't do
             * this because we'd like to catch any mismatch in parity between
             * delegated start and end elements, instead of silently correcting
             * those problems (and possibly hiding others).
             */

            // Body
            writer.writeEndElement();

            // Envelope
            writer.writeEndElement();

            writer.writeEndDocument();
            writer.close();
        } catch (final XMLStreamException e) {
            throw new IOException(e.getMessage());
        }
    }
}
