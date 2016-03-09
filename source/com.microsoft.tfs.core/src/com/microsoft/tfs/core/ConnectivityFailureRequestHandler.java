// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core;

import java.util.concurrent.atomic.AtomicBoolean;

import com.microsoft.tfs.core.ws.runtime.client.SOAPRequest;
import com.microsoft.tfs.core.ws.runtime.client.SOAPService;
import com.microsoft.tfs.core.ws.runtime.client.TransportRequestHandler;
import com.microsoft.tfs.core.ws.runtime.exceptions.TransportException;
import com.microsoft.tfs.core.ws.runtime.exceptions.TransportRequestHandlerCanceledException;
import com.microsoft.tfs.util.Check;

/**
 * A transport request handler that listens for success / failures from
 * {@link SOAPService}s and updates the {@link TFSConnection}'s error state
 * accordingly. This handler should be invoked last in the list of handlers.
 *
 * @threadsafety unknown
 */
public class ConnectivityFailureRequestHandler implements TransportRequestHandler {
    private final TFSConnection connection;

    public ConnectivityFailureRequestHandler(final TFSConnection connection) {
        Check.notNull(connection, "connection"); //$NON-NLS-1$
        this.connection = connection;
    }

    @Override
    public Status prepareRequest(final SOAPService service, final SOAPRequest request, final AtomicBoolean cancel) {
        /*
         * A previous handler canceled the prepare process (the user may have
         * refused to build valid credentials). Mark the connection failed in
         * this case but leave it alone in other cases.
         */
        if (cancel.get()) {
            connection.setConnectivityFailureOnLastWebServiceCall(true);
        }

        return Status.CONTINUE;
    }

    @Override
    public Status handleException(
        final SOAPService service,
        final SOAPRequest request,
        final Exception exception,
        final AtomicBoolean cancel) {
        /*
         * Set to failed for transport or auth exceptions.
         */
        if (exception instanceof TransportException || exception instanceof TransportRequestHandlerCanceledException) {
            connection.setConnectivityFailureOnLastWebServiceCall(true);
        }

        return Status.CONTINUE;
    }

    @Override
    public Status handleSuccess(final SOAPService service, final SOAPRequest request) {
        connection.setConnectivityFailureOnLastWebServiceCall(false);

        return Status.CONTINUE;
    }
}
