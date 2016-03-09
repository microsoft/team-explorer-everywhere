// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.ws.runtime.client;

import java.util.concurrent.atomic.AtomicBoolean;

import com.microsoft.tfs.core.httpclient.HttpClient;
import com.microsoft.tfs.core.ws.runtime.exceptions.TransportAuthException;
import com.microsoft.tfs.core.ws.runtime.exceptions.TransportRequestHandlerCanceledException;

/**
 * Interface for providing information to {@link SOAPService} or handling
 * request processing errors. Allows for updating information (such as
 * credentials) before requests occur, or handling
 * {@link TransportAuthException}s encountered when {@link SOAPService} is
 * processing a request. This exception contains information about the
 * authentication or authorization failure, and in the case of federated
 * authentication it contains the information required to complete it via
 * third-party services TFS is configured to support.
 *
 * @threadsafety unknown
 */
public interface TransportRequestHandler {
    /**
     * Indicates the completion status of a {@link TransportRequestHandler}
     * method.
     */
    public static enum Status {
        /**
         * The method succeeded. The caller should not invoke more handlers.
         */
        COMPLETE,

        /**
         * The handler may have performed some work but more handlers should be
         * invoked.
         */
        CONTINUE,
    }

    /**
     * Called by {@link SOAPService} before a {@link SOAPRequest} is submitted
     * to the server. The handler may do things like check that the credentials
     * are valid (and maybe prompt the user to complete them).
     *
     * @param service
     *        the service that was processing the request (must not be
     *        <code>null</code>)
     * @param request
     *        the request being processed (must not be <code>null</code>)
     * @param cancel
     *        a handler sets this to <code>true</code> to cause
     *        {@link SOAPService} to not send the {@link SOAPRequest} and throw
     *        a {@link TransportRequestHandlerCanceledException} after the
     *        remaining handlers are invoked (according to the return
     *        {@link Status})
     * @return a {@link Status} indicating the result of the method
     */
    Status prepareRequest(SOAPService service, SOAPRequest request, AtomicBoolean cancel);

    /**
     * Called when there was an exception executing a {@link SOAPRequest}.
     * <p>
     * The handler may do things like notify the user and/or make changes to the
     * service, or request, or some other part of the program so the request can
     * succeed in the future. Authentication exceptions are commonly handled in
     * this way (for example, by prompting for new credentials and updating the
     * {@link HttpClient}).
     * <p>
     * The return value controls how the {@link SOAPService} behaves after the
     * handler finishes.
     *
     * @param service
     *        the service that was processing the request (must not be
     *        <code>null</code>)
     * @param request
     *        the request being processed (must not be <code>null</code>)
     * @param exception
     *        the exception encountered (must not be <code>null</code>)
     * @param cancel
     *        a handler sets this to <code>true</code> to cause
     *        {@link SOAPService} to not retry the {@link SOAPRequest} and throw
     *        a {@link TransportRequestHandlerCanceledException} after the
     *        remaining handlers are invoked (according to the return
     *        {@link Status})
     * @return a {@link Status} indicating the result of the method; when
     *         {@link Status#COMPLETE} the handler took action to correct the
     *         problem and the request can be retried; when
     *         {@link Status#CONTINUE} the other handlers should be invoked
     */
    Status handleException(SOAPService service, SOAPRequest request, Exception exception, AtomicBoolean cancel);

    /**
     * Called when a {@link SOAPRequest} executed normally.
     *
     * @param service
     *        the service that was processing the request (must not be
     *        <code>null</code>)
     * @param request
     *        the request being processed (must not be <code>null</code>)
     * @return a {@link Status} indicating the result of the method (
     *         {@link SOAPService} continues to invoke all handlers regardless
     *         of the returned value)
     */
    Status handleSuccess(SOAPService service, SOAPRequest request);
}
