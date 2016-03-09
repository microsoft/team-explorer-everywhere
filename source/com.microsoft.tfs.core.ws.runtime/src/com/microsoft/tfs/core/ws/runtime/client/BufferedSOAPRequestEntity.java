// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.ws.runtime.client;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.MessageFormat;

import com.microsoft.tfs.core.httpclient.methods.RequestEntity;

/**
 * An HttpClient request entity implementation suited for SOAP request use which
 * buffers its entire request into memory so that its content length may be
 * measured. The actual request body construction is delegated to a
 * SOAPRequestEntity (@see {@link SOAPRequestEntity}).
 */
public class BufferedSOAPRequestEntity extends Object implements RequestEntity {
    /**
     * Our byte-array backed buffer of the entire request.
     */
    private ByteArrayOutputStream buffer;

    /**
     * The delegate request entity that we wrap, which actually does all the
     * serialization.
     */
    private final SOAPRequestEntity delegate;

    /**
     * Construct a buffered request entity that wraps the given entity.
     *
     * @param entity
     *        the entity to wrap (not null).
     */
    public BufferedSOAPRequestEntity(final SOAPRequestEntity entity) {
        delegate = entity;
    }

    public void setSOAPHeaderProvider(final SOAPHeaderProvider soapHeaderProvider) {
        delegate.setSOAPHeaderProvider(soapHeaderProvider);
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * org.apache.commons.httpclient.methods.RequestEntity#getContentLength()
     */
    @Override
    public long getContentLength() {
        try {
            ensureRequestIsBuffered();
        } catch (final IOException e) {
            final String messageFormat = "Content length not available because buffering of request body failed: {0}"; //$NON-NLS-1$
            final String message = MessageFormat.format(messageFormat, e.getMessage());
            throw new RuntimeException(message);
        }

        return buffer.size();
    }

    /*
     * (non-Javadoc)
     *
     * @see org.apache.commons.httpclient.methods.RequestEntity#getContentType()
     */
    @Override
    public String getContentType() {
        return delegate.getContentType();
    }

    /*
     * (non-Javadoc)
     *
     * @see org.apache.commons.httpclient.methods.RequestEntity#isRepeatable()
     */
    @Override
    public boolean isRepeatable() {
        /*
         * This class is always repeatable since we buffer the entire request.
         */
        return true;
    }

    /**
     * @return the SOAP method (action) associated with this request.
     */
    public String getMethodName() {
        return delegate.getMethodName();
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * org.apache.commons.httpclient.methods.RequestEntity#writeRequest(java
     * .io.OutputStream)
     */
    @Override
    public void writeRequest(final OutputStream out) throws IOException {
        ensureRequestIsBuffered();

        buffer.writeTo(out);
    }

    /**
     * Invokes the delegate to write its request into the byte array buffer.
     */
    private synchronized void ensureRequestIsBuffered() throws IOException {
        if (buffer == null) {
            buffer = new ByteArrayOutputStream();
            delegate.writeRequest(buffer);
        }
    }
}
