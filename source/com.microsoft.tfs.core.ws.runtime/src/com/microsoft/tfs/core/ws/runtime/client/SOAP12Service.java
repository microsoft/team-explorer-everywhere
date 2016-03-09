// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.ws.runtime.client;

import java.net.URI;

import javax.xml.namespace.QName;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

import com.microsoft.tfs.core.httpclient.HttpClient;
import com.microsoft.tfs.core.httpclient.MultiThreadedHttpConnectionManager;
import com.microsoft.tfs.core.ws.runtime.Schemas;
import com.microsoft.tfs.core.ws.runtime.exceptions.SOAPFault;
import com.microsoft.tfs.core.ws.runtime.exceptions.SOAPFaultSubCode;
import com.microsoft.tfs.util.xml.DOMUtils;

/**
 * Base class for SOAP 1.2 service implementations.
 */
public abstract class SOAP12Service extends SOAPService {
    /**
     * Create a stub that will use the given HttpClient instance. The client's
     * connection manager <b>must</b> be an instance of
     * {@link MultiThreadedHttpConnectionManager}. The client <b>must</b> also
     * have its client param "http.protocol.expect-continue" set to false.
     *
     * @param client
     *        an HttpClient instance to use (not null).
     * @param endpoint
     *        the complete URI to the SOAP endpoint to use (not null).
     * @param port
     *        the qualified name fo the SOAP port to use (not null).
     */
    public SOAP12Service(final HttpClient client, final URI endpoint, final QName port) {
        super(client, endpoint, port);
    }

    /**
     * Create a stub that will allocate its own HttpClient (with its own
     * HttpConnectionManager).
     *
     * @param endpoint
     *        the complete URI to the SOAP endpoint to use (not null).
     * @param port
     *        the qualified name fo the SOAP port to use (not null).
     */
    public SOAP12Service(final URI endpoint, final QName port) {
        super(endpoint, port);
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * com.microsoft.tfs.core.ws.runtime.client.SOAPService#buildRequestEntity
     * (java.lang .String,
     * com.microsoft.tfs.core.ws.runtime.client.SOAPMethodRequestWriter)
     */
    @Override
    protected SOAPRequestEntity buildRequestEntity(
        final String invokedMethodName,
        final SOAPMethodRequestWriter requestWriter) {
        return new SOAPRequestEntity12(invokedMethodName, getPort().getNamespaceURI(), requestWriter);
    }

    /*
     * (non-Javadoc)
     *
     * @see com.microsoft.tfs.core.ws.runtime.client.SOAPService#
     * getDefaultSOAPNamespace ()
     */
    @Override
    protected String getDefaultSOAPNamespace() {
        return Schemas.SOAP_12;
    }

    /*
     * (non-Javadoc)
     *
     * @seecom.microsoft.tfs.core.ws.runtime.client.SOAPService#
     * examineResponseDOMForFault (org.w3c.dom.Document)
     */
    @Override
    protected void examineResponseDOMForFault(final Document responseDOM) {
        Node node = getChildByName(responseDOM, "soap:Envelope"); //$NON-NLS-1$
        node = getChildByName(node, "soap:Body"); //$NON-NLS-1$

        final Node faultNode = getChildByName(node, "soap:Fault"); //$NON-NLS-1$
        if (faultNode != null) {
            String code = null;
            String subCode = null;
            String message = null;
            String language = null;

            final Node codeNode = getChildByName(faultNode, "soap:Code"); //$NON-NLS-1$
            if (codeNode != null) {
                code = DOMUtils.getText(getChildByName(codeNode, "soap:Value")); //$NON-NLS-1$
                final Node subCodeNode = getChildByName(codeNode, "soap:Subcode"); //$NON-NLS-1$
                if (subCodeNode != null) {
                    subCode = DOMUtils.getText(getChildByName(subCodeNode, "soap:Value")); //$NON-NLS-1$
                }
            }

            final Node reasonNode = getChildByName(faultNode, "soap:Reason"); //$NON-NLS-1$
            if (reasonNode != null) {
                final Node textNode = getChildByName(reasonNode, "soap:Text"); //$NON-NLS-1$

                if (textNode != null) {
                    message = DOMUtils.getText(textNode);
                    language = textNode.getAttributes().getNamedItem("xml:lang").getNodeValue(); //$NON-NLS-1$
                }
            }

            throw new SOAPFault(
                message,
                (code != null) ? new QName(code) : null,
                null,
                language,
                getChildByName(faultNode, "detail"), //$NON-NLS-1$
                (subCode != null) ? new SOAPFaultSubCode(new QName(subCode)) : null,
                null);
        }
    }
}
