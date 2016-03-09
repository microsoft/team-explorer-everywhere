// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.ws.runtime.client;

import java.io.IOException;
import java.io.OutputStream;

import javax.xml.stream.XMLOutputFactory;

import com.microsoft.tfs.core.httpclient.methods.RequestEntity;
import com.microsoft.tfs.core.ws.runtime.stax.StaxFactoryProvider;
import com.microsoft.tfs.util.Check;

/**
 * An HttpClient request entity implementation suited for SOAP request use. The
 * writing of the body of the soap request is done partially by this class and
 * is partially delegated to the SOAPRequestStreamWriter given during
 * construction.
 */
public abstract class SOAPRequestEntity extends Object implements RequestEntity {
    /**
     * The encoding we will use when composing our SOAP messages via HTTP.
     */
    public static final String SOAP_ENCODING = "utf-8"; //$NON-NLS-1$

    /**
     * A cached factory used to construct new XMLOutputWriters quickly.
     */
    private static final XMLOutputFactory xmlOutputFactory = StaxFactoryProvider.getXMLOutputFactory();

    private final String methodName;
    private final String defaultNamespace;
    private final SOAPMethodRequestWriter requestWriter;

    private SOAPHeaderProvider soapHeaderProvider;

    public SOAPRequestEntity(
        final String methodName,
        final String defaultNamespace,
        final SOAPMethodRequestWriter requestWriter) {
        Check.notNull(methodName, "methodName"); //$NON-NLS-1$
        Check.notNull(defaultNamespace, "defaultNamespace"); //$NON-NLS-1$
        Check.notNull(requestWriter, "requestWriter"); //$NON-NLS-1$

        this.methodName = methodName;
        this.defaultNamespace = defaultNamespace;
        this.requestWriter = requestWriter;
    }

    public static XMLOutputFactory getXMLOutputFactory() {
        return xmlOutputFactory;
    }

    public String getDefaultNamespace() {
        return defaultNamespace;
    }

    public SOAPMethodRequestWriter getRequestWriter() {
        return requestWriter;
    }

    public void setSOAPHeaderProvider(final SOAPHeaderProvider soapHeaderProvider) {
        this.soapHeaderProvider = soapHeaderProvider;
    }

    public SOAPHeaderProvider getSoapHeaderProvider() {
        return soapHeaderProvider;
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * org.apache.commons.httpclient.methods.RequestEntity#getContentLength()
     */
    @Override
    public long getContentLength() {
        // We don't know how long the content will be.
        return -1;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.apache.commons.httpclient.methods.RequestEntity#getContentType()
     */
    @Override
    public abstract String getContentType();

    /*
     * (non-Javadoc)
     *
     * @see org.apache.commons.httpclient.methods.RequestEntity#isRepeatable()
     */
    @Override
    public boolean isRepeatable() {
        return true;
    }

    /**
     * @return the SOAP method (action) associated with this request.
     */
    public String getMethodName() {
        return methodName;
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * org.apache.commons.httpclient.methods.RequestEntity#writeRequest(java
     * .io.OutputStream)
     */
    @Override
    public abstract void writeRequest(OutputStream out) throws IOException;
}
