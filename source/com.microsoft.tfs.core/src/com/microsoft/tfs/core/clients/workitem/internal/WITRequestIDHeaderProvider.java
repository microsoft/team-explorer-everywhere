// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.internal;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.microsoft.tfs.core.ws.runtime.client.SOAPHeaderProvider;
import com.microsoft.tfs.util.GUID;
import com.microsoft.tfs.util.xml.DOMCreateUtils;
import com.microsoft.tfs.util.xml.DOMUtils;

/**
 * <p>
 * Provides the SOAP RequestHeader header required for work item tracking web
 * services.
 * </p>
 *
 * @threadsafety immutable
 */
public class WITRequestIDHeaderProvider implements SOAPHeaderProvider {
    private static final String NAMESPACE_URI =
        "http://schemas.microsoft.com/TeamFoundation/2005/06/WorkItemTracking/ClientServices/03"; //$NON-NLS-1$

    public WITRequestIDHeaderProvider() {
    }

    @Override
    public Element getSOAPHeader() {
        /*
         * The client generates a new GUID with each SOAP request to the work
         * item tracking webservice.
         *
         * Right now, this isn't being used for anything by our client. The
         * server may include it in some logs. The server also includes some
         * support for cancelling a long-running request by passing that
         * request's generated ID. TEE does not currently exploit this feature.
         */

        final String generatedRequestId = "uuid:" + GUID.newGUIDString(); //$NON-NLS-1$

        final Document document = DOMCreateUtils.newDocumentNS(NAMESPACE_URI, "RequestHeader"); //$NON-NLS-1$
        final Element root = document.getDocumentElement();
        DOMUtils.appendChildWithTextNS(root, NAMESPACE_URI, "Id", generatedRequestId); //$NON-NLS-1$

        return root;
    }
}
