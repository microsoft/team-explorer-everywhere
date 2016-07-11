// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.ws.runtime.client;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.SocketException;
import java.net.URI;
import java.net.URLDecoder;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.zip.GZIPInputStream;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.microsoft.tfs.core.httpclient.Credentials;
import com.microsoft.tfs.core.httpclient.Header;
import com.microsoft.tfs.core.httpclient.HttpClient;
import com.microsoft.tfs.core.httpclient.HttpMethod;
import com.microsoft.tfs.core.httpclient.HttpStatus;
import com.microsoft.tfs.core.httpclient.MultiThreadedHttpConnectionManager;
import com.microsoft.tfs.core.httpclient.StatusLine;
import com.microsoft.tfs.core.httpclient.URIException;
import com.microsoft.tfs.core.httpclient.auth.AuthScope;
import com.microsoft.tfs.core.httpclient.methods.PostMethod;
import com.microsoft.tfs.core.httpclient.params.HttpClientParams;
import com.microsoft.tfs.core.httpclient.util.EncodingUtil;
import com.microsoft.tfs.core.ws.runtime.Messages;
import com.microsoft.tfs.core.ws.runtime.client.TransportRequestHandler.Status;
import com.microsoft.tfs.core.ws.runtime.exceptions.EndpointNotFoundException;
import com.microsoft.tfs.core.ws.runtime.exceptions.FederatedAuthException;
import com.microsoft.tfs.core.ws.runtime.exceptions.FederatedAuthFailedException;
import com.microsoft.tfs.core.ws.runtime.exceptions.InvalidServerResponseException;
import com.microsoft.tfs.core.ws.runtime.exceptions.ProxyUnauthorizedException;
import com.microsoft.tfs.core.ws.runtime.exceptions.SOAPFault;
import com.microsoft.tfs.core.ws.runtime.exceptions.ServiceErrorException;
import com.microsoft.tfs.core.ws.runtime.exceptions.TransportException;
import com.microsoft.tfs.core.ws.runtime.exceptions.TransportRequestHandlerCanceledException;
import com.microsoft.tfs.core.ws.runtime.exceptions.UnauthorizedException;
import com.microsoft.tfs.core.ws.runtime.stax.StaxFactoryProvider;
import com.microsoft.tfs.core.ws.runtime.xml.XMLStreamReaderHelper;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.GUID;
import com.microsoft.tfs.util.LocaleUtil;
import com.microsoft.tfs.util.tasks.CanceledException;
import com.microsoft.tfs.util.tasks.TaskMonitorService;
import com.microsoft.tfs.util.xml.DOMCreateUtils;
import com.microsoft.tfs.util.xml.DOMSerializeUtils;

/**
 * Base class for SOAP service implementations. One of {@link SOAP11Service} or
 * {@link SOAP12Service} is extended to provide a stub implementation.
 */
public abstract class SOAPService {
    private final static Log log = LogFactory.getLog(SOAPService.class);
    private final static Log base64log = LogFactory.getLog("base64." + SOAPService.class.getName()); //$NON-NLS-1$

    private static final String NEWLINE = System.getProperty("line.separator"); //$NON-NLS-1$

    protected static final int RESPONSE_MAX_SIZE_FOR_DEBUG_LOGGING = 1024 * 1024;

    /**
     * If the system property "teamexplorer.soap.disable-gzip=true" then we do
     * not tell the server that we support gzip compression in our SOAP
     * requests, therefore the responses will come back uncompressed. The
     * default is to say that we support compression which means that the server
     * has the option of compressing our responses before transmitting over the
     * wire - however some responses will not be compressed - it is dependant on
     * the server.
     */
    protected static final boolean ALLOW_RESPONSE_COMPRESSION = !Boolean.getBoolean("teamexplorer.soap.disable-gzip"); //$NON-NLS-1$

    /**
     * The HTTP client used by this stub for all network operations. Must be
     * configured with a {@link MultiThreadedHttpConnectionManager}.
     */
    private final HttpClient client;

    /**
     * The complete URI to the SOAP endpoint this stub will use.
     */
    private final URI endpoint;

    /**
     * The qualified name of the SOAP port this stub will use.
     */
    private final QName port;

    /**
     * The SOAPHeader provider, if any. May be <code>null</code>.
     */
    private volatile SOAPHeaderProvider soapHeaderProvider;

    /**
     * Optional exception handler for transport configuration and authentication
     * exceptions. May be <code>null</code>.
     */
    private final List<TransportRequestHandler> transportRequestHandlers = new ArrayList<TransportRequestHandler>();

    /**
     * The Accept-Langauge HTTP request header value, if any. May be
     * <code>null</code>.
     */
    private volatile String acceptLanguageHeaderValue;

    private volatile boolean promptForCredentials = true;

    /**
     * Turn on coalescing so text processing is easier. This must remain enabled
     * or the generated web service classes must be updated.
     */
    private final static XMLInputFactory xmlInputFactory = StaxFactoryProvider.getXMLInputFactory(true);

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
    public SOAPService(final HttpClient client, final URI endpoint, final QName port) {
        Check.notNull(client, "client"); //$NON-NLS-1$
        Check.notNull(endpoint, "endpoint"); //$NON-NLS-1$
        Check.notNull(port, "port"); //$NON-NLS-1$

        this.client = client;
        this.endpoint = endpoint;
        this.port = port;
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
    public SOAPService(final URI endpoint, final QName port) {
        Check.notNull(endpoint, "endpoint"); //$NON-NLS-1$
        Check.notNull(port, "port"); //$NON-NLS-1$

        this.endpoint = endpoint;
        this.port = port;

        client = new HttpClient(new MultiThreadedHttpConnectionManager());
        final HttpClientParams params = new HttpClientParams();
        params.setBooleanParameter("http.protocol.expect-continue", false); //$NON-NLS-1$
        client.setParams(params);
    }

    /**
     * Sets the {@link SOAPHeaderProvider} for this stub.
     *
     * @param soapHeaderProvider
     *        the header provider (may be <code>null</code>)
     */
    public void setSOAPHeaderProvider(final SOAPHeaderProvider soapHeaderProvider) {
        this.soapHeaderProvider = soapHeaderProvider;
    }

    /**
     * Sets the {@link Locale} used to set the Accept-Language HTTP header for
     * every HTTP request. The {@link Locale} is tranformed into the header
     * value using {@link LocaleUtil#localeToRFC5646LanguageTag(Locale)}.
     *
     * @param locale
     *        the {@link Locale} to use to set the header. Pass
     *        <code>null</code> to disable sending this header.
     */
    public void setAcceptLanguage(final Locale locale) {
        if (locale != null) {
            try {
                acceptLanguageHeaderValue = LocaleUtil.localeToRFC5646LanguageTag(locale);
            } catch (final IllegalArgumentException e) {
                log.error("Couldn't turn Locale into Accept-Language header", e); //$NON-NLS-1$
                acceptLanguageHeaderValue = null;
            }
        } else {
            acceptLanguageHeaderValue = null;
        }

        log.debug(MessageFormat.format("Accept-Language header set to: {0}", acceptLanguageHeaderValue)); //$NON-NLS-1$
    }

    /**
     * @return <code>true</code> if the application may prompt the user for
     *         credentials if there was an authentication failure using this
     *         service, <code>false</code> if the application should not prompt
     *         the user
     */
    public boolean isPromptForCredentials() {
        return promptForCredentials;
    }

    /**
     * Sets whether the application should prompt for credentials after an
     * authentication failure using this service.
     *
     * @param promptForCredentials
     *        <code>true</code> if the application may prompt,
     *        <code>false</code> if it should not
     */
    public void setPromptForCredentials(final boolean promptForCredentials) {
        this.promptForCredentials = promptForCredentials;
    }

    /**
     * Sets this as a {@link TransportRequestHandler} for this stub. Users may
     * configure multiple transport request handlers, they will be called in the
     * order they are added.
     *
     * @param transportAuthHandler
     *        the request handler (not <code>null</code>)
     */
    public void addTransportRequestHandler(final TransportRequestHandler transportRequestHandler) {
        Check.notNull(transportRequestHandler, "transportRequestHandler"); //$NON-NLS-1$

        synchronized (transportRequestHandlers) {
            transportRequestHandlers.add(transportRequestHandler);
        }
    }

    /**
     * Removes this as a {@link TransportRequestHandler} for this stub. Users
     * may configure multiple transport request handlers, they will be called in
     * the order they are added.
     *
     * @param transportAuthHandler
     *        the request handler (not <code>null</code>)
     */
    public void removeTransportRequestHandler(final TransportRequestHandler transportRequestHandler) {
        Check.notNull(transportRequestHandler, "transportRequestHandler"); //$NON-NLS-1$

        synchronized (transportRequestHandlers) {
            transportRequestHandlers.remove(transportRequestHandler);
        }
    }

    /**
     * Sets any required HTTP request headers on the given method, which is
     * already initialized.
     *
     * @param method
     *        the HTTP method on which to set any request headers required by
     *        this stub.
     * @param invokedMethod
     *        the SOAP method being invoked.
     */
    protected void setRequestHeaders(final HttpMethod method, final String invokedMethod) {
        if (ALLOW_RESPONSE_COMPRESSION) {
            // If debugging then do not want the response compressed - makes it
            // easier to see what the
            // server is doing.
            method.addRequestHeader("Accept-Encoding", "gzip"); //$NON-NLS-1$ //$NON-NLS-2$
        }

        if (acceptLanguageHeaderValue != null) {
            /*
             * TFS uses this header to return error messages and other web
             * content in the correct locale.
             */
            method.addRequestHeader("Accept-Language", acceptLanguageHeaderValue); //$NON-NLS-1$
        }
    }

    /**
     * Builds the SOAPRequestEntity used to perform a SOAP request.
     *
     * @return a new SOAPRequestEntity.
     */
    protected abstract SOAPRequestEntity buildRequestEntity(
        String invokedMethodName,
        SOAPMethodRequestWriter requestWriter);

    /**
     * @return the default SOAP namespace used for this stub.
     */
    protected abstract String getDefaultSOAPNamespace();

    /**
     * Create a SOAP request for the given method name. The writing of the
     * request body is delegated to the given request writer.
     *
     * @param methodName
     *        the method name to invoke (not null or emtpy).
     * @param requestWriter
     *        the request writer that will do the work of writing the request
     *        body (except the SOAP envelope). If null, no request stream writer
     *        is invoked (an empty request is sent).
     * @return the SOAP request that was created.
     */
    protected SOAPRequest createSOAPRequest(final String methodName, final SOAPMethodRequestWriter requestWriter) {
        final PostMethod method = new PostMethod(endpoint.toString());
        method.setContentChunked(false);

        // Let the extending classes insert their headers, if required (SOAP 1.1
        // needs the SOAPAction header).
        setRequestHeaders(method, methodName);

        /*
         * The content type doesn't need to be set here, because the request
         * entity declares it and HttpClient will set the header from the
         * declared value.
         */

        // The SOAP request entity does all the work of composing the message.
        final SOAPRequestEntity requestEntity = buildRequestEntity(methodName, requestWriter);

        requestEntity.setSOAPHeaderProvider(soapHeaderProvider);

        /*
         * We must wrap the request entity in a buffered version so its content
         * length can be known (the request body is buffered in memory before it
         * is sent). This also makes the NTLM authentication process more
         * efficient since repeated calls to the request entity to construct its
         * body can be done from the buffer.
         */
        final BufferedSOAPRequestEntity bufferedEntity = new BufferedSOAPRequestEntity(requestEntity);

        // Use the buffered wrapper.
        method.setRequestEntity(bufferedEntity);

        return new SOAPRequest(method, requestEntity);
    }

    /**
     * Execute a SOAP request that was built via
     * {@link #createSOAPRequest(String, SOAPMethodRequestWriter)}
     *
     * @param request
     *        the request to execute (not null).
     * @param responseName
     *        the name of the SOAP response message for this request (not null)
     * @param responseReader
     *        the response reader that will do the work of reading the response
     *        (except the SOAP envelope). If null, no response stream reader is
     *        invoked (no response data is read except for the SOAP envelope and
     *        body elements).
     * @throws SOAPFault
     *         if a SOAP fault was returned by the server.
     * @throws UnauthorizedException
     *         if the client could not contact the server because of an
     *         authorization error (HTTP 401).
     * @throws ProxyUnauthorizedException
     *         if the client could not authenticate to the HTTP proxy
     * @throws FederatedAuthException
     *         if the client could not contact the server because it lacks the
     *         proper federated authentication (ACS) cookies and the federated
     *         authentication handler (set by
     *         {@link #setTransportAuthHandler(TransportAuthHandler)} ) did not
     *         handle the exception. The caller is expected to obtain the
     *         cookies and resubmit.
     * @throws InvalidServerResponseException
     *         if the server returned data that could not be parsed as XML or
     *         SOAP.
     * @throws EndpointNotFoundException
     *         if the server returned HTTP 404 when the request was executed.
     * @throws TransportException
     *         if some other an IO error occurred.
     * @throws TransportRequestHandlerCanceledException
     *         if the user cancelled the prompt for credentials
     */
    protected void executeSOAPRequest(
        final SOAPRequest request,
        final String responseName,
        final SOAPMethodResponseReader responseReader)
        throws SOAPFault,
            UnauthorizedException,
            ProxyUnauthorizedException,
            FederatedAuthException,
            InvalidServerResponseException,
            EndpointNotFoundException,
            TransportException,
            TransportRequestHandlerCanceledException {
        /*
         * Duplicate the transport request handler map so we needn't keep a lock
         * and so that we have a consistent set throughout execution.
         */
        final List<TransportRequestHandler> requestHandlers = new ArrayList<TransportRequestHandler>();

        synchronized (transportRequestHandlers) {
            requestHandlers.addAll(transportRequestHandlers);
        }

        /*
         * Allow the transport authentication handler to process initial
         * credentials. This can happen if we're lazily authenticating and we do
         * not yet have a full set of credentials.
         */
        final AtomicBoolean cancel = new AtomicBoolean(false);

        for (final TransportRequestHandler requestHandler : requestHandlers) {
            // cancel doesn't stop us from invoking handlers
            if (requestHandler.prepareRequest(this, request, cancel) == Status.COMPLETE) {
                break;
            }
        }

        if (cancel.get()) {
            throw new TransportRequestHandlerCanceledException();
        }

        /*
         * Execute this method in a retry loop. On exceptions, we can delegate
         * to a user configured exception handler, which may modify the method
         * and allow us to resubmit.
         *
         * The typical use case for this is ACS authentication - it can expire
         * in the middle of a call and we want to prompt the user to
         * reauthenticate.
         */

        RuntimeException failure = null;
        do {
            try {
                executeSOAPRequestInternal(request, responseName, responseReader);
                break;
            } catch (final RuntimeException e) {
                // Give the handlers a chance to handle/correct/cancel this
                // exception

                boolean exceptionHandled = false;
                cancel.set(false);

                for (final TransportRequestHandler requestHandler : requestHandlers) {
                    // cancel doesn't stop us from invoking handlers
                    if (requestHandler.handleException(this, request, e, cancel) == Status.COMPLETE) {
                        /*
                         * This handler handled the exception - defer all others
                         * from attempting to handle it and reset the auth
                         * state.
                         */
                        request.getPostMethod().getHostAuthState().invalidate();

                        failure = null;
                        exceptionHandled = true;
                        break;
                    }

                    // Status was CONTINUE, continue with next handler
                }

                // Wasn't handled, prepare to throw it
                if (!exceptionHandled) {
                    // The user wants to cancel, convert to a cancel
                    if (cancel.get()) {
                        failure = new TransportRequestHandlerCanceledException();
                    } else {
                        failure = e;
                    }
                    break;
                }

                // Exception handled, loop to retry
            }
        } while (true);

        if (failure != null) {
            throw failure;
        }

        for (final TransportRequestHandler requestHandler : requestHandlers) {
            requestHandler.handleSuccess(this, request);
        }
    }

    private void executeSOAPRequestInternal(
        final SOAPRequest request,
        final String responseName,
        final SOAPMethodResponseReader responseReader)
        throws SOAPFault,
            UnauthorizedException,
            ProxyUnauthorizedException,
            InvalidServerResponseException,
            EndpointNotFoundException,
            TransportException,
            CanceledException {
        final PostMethod method = request.getPostMethod();

        final long start = System.currentTimeMillis();
        long serverExecute = -1;
        long contentLength = -1;
        int response = -1;
        boolean isCompressed = false;

        IOException ioException = null;
        byte[] responseBytes = null;
        TraceInputStream responseStream = null;

        try {
            /*
             * Our implementation aims to be tolerant of connection resets
             * caused by half-open sockets. It detects them and retries the
             * operation once.
             *
             * Here's the problem: sometimes IIS's ASP.NET worker process is
             * recycled (this can happen because of an application pool time
             * threshold, number of requests served threshold, memory usage
             * threshold, etc.). When the process is recycled, most sockets that
             * were connected to it (including client sockets) will continue to
             * work fine once IIS builds a new worker. Sometimes, however, bad
             * things happen: those connected sockets will be reset by the
             * server the first time they're used.
             *
             * Since the default TFS configuration (as of RC) is to recycle the
             * application pool's worker every 29 hours, a user is likely to
             * have a half-open TCP socket if he leaves his client running
             * through the night. The client may work correctly, but in the case
             * that it receives a reset, it is pretty safe to retry the
             * operation once.
             *
             * Some JREs use the string "Connection reset by peer", others use
             * "Connection reset". We will match both.
             */
            final long serverStart = System.currentTimeMillis();
            try {
                response = client.executeMethod(method);
            } catch (final SocketException e) {
                /*
                 * If the user cancelled the current task, we might get a
                 * "socket closed" exception if the HTTPConnectionCanceller
                 * closed the socket after timing out waiting for voluntary
                 * cancel.
                 */
                if (TaskMonitorService.getTaskMonitor().isCanceled() && (e.getMessage().startsWith("Socket closed") //$NON-NLS-1$
                    || e.getMessage().startsWith("Stream closed"))) //$NON-NLS-1$
                {
                    throw new CanceledException();
                }

                /*
                 * If this fault was not a TCP connection reset, rethrow it.
                 */
                if (e.getMessage().startsWith("Connection reset") == false) //$NON-NLS-1$
                {
                    throw e;
                }

                log.warn("Retrying invoke after a connection reset", e); //$NON-NLS-1$

                /*
                 * Give it one more try on the user's behalf.
                 */
                response = client.executeMethod(method);
            }
            serverExecute = System.currentTimeMillis() - serverStart;

            responseStream = getResponseStream(method);
            isCompressed = responseStream.isCompressed();

            switch (response) {
                case HttpStatus.SC_OK:
                    XMLStreamReader reader = null;

                    try {

                        reader = SOAPService.xmlInputFactory.createXMLStreamReader(
                            responseStream,
                            SOAPRequestEntity.SOAP_ENCODING);

                        /*
                         * Read as far as the SOAP body from the stream.
                         */
                        final QName envelopeQName = new QName(getDefaultSOAPNamespace(), "Envelope", "soap"); //$NON-NLS-1$ //$NON-NLS-2$
                        final QName headerQName = new QName(getDefaultSOAPNamespace(), "Header", "soap"); //$NON-NLS-1$ //$NON-NLS-2$
                        final QName bodyQName = new QName(getDefaultSOAPNamespace(), "Body", "soap"); //$NON-NLS-1$ //$NON-NLS-2$

                        // Read the envelope.
                        if (reader.nextTag() == XMLStreamConstants.START_ELEMENT
                            && reader.getName().equals(envelopeQName)) {
                            while (reader.nextTag() == XMLStreamConstants.START_ELEMENT) {
                                if (reader.getName().equals(headerQName)) {
                                    // Ignore headers for now.
                                    XMLStreamReaderHelper.readUntilElementEnd(reader);
                                } else if (reader.getName().equals(bodyQName)) {
                                    /*
                                     * The first element in the body should be
                                     * the desired response element, which we
                                     * must find (and read into) before we
                                     * delegate to the reader (if there is one).
                                     */
                                    if (reader.nextTag() == XMLStreamConstants.START_ELEMENT
                                        && reader.getName().getLocalPart().equals(responseName)) {
                                        try {
                                            if (responseReader != null) {
                                                responseReader.readSOAPResponse(reader, responseStream);
                                            }
                                        } catch (final XMLStreamException e) {
                                            throw new InvalidServerResponseException(e);
                                        }

                                        return;
                                    }
                                }
                            }
                        }

                        /*
                         * If we got here, some error happened (we couldn't find
                         * our envelope and body tags).
                         */
                        throw new InvalidServerResponseException(
                            "The server's response does not seem to be a SOAP message."); //$NON-NLS-1$
                    } catch (final XMLStreamException e) {
                        final String messageFormat = "The server's response could not be parsed as XML: {0}"; //$NON-NLS-1$
                        final String message = MessageFormat.format(messageFormat, e.getMessage());
                        throw new InvalidServerResponseException(message);
                    } finally {
                        if (reader != null) {
                            try {
                                reader.close();
                            } catch (final XMLStreamException e) {
                            }
                        }
                    }
                case HttpStatus.SC_UNAUTHORIZED:
                case HttpStatus.SC_MOVED_TEMPORARILY:
                    /*
                     * This may be an ACS or on-premises authentication failure,
                     * examine the headers.
                     */
                    examineHeadersForFederatedAuthURL(method);
                case HttpStatus.SC_PROXY_AUTHENTICATION_REQUIRED:
                    throw new ProxyUnauthorizedException(
                        client.getHostConfiguration().getProxyHost(),
                        client.getHostConfiguration().getProxyPort(),
                        client.getState().getProxyCredentials(AuthScope.ANY));
                case HttpStatus.SC_SERVICE_UNAVAILABLE:
                    /*
                     * An error message may be inside the response, in the
                     * headers.
                     */
                    examineHeadersForErrorMessage(method);
                case HttpStatus.SC_INTERNAL_SERVER_ERROR:
                    /*
                     * A SOAP fault may be inside the response.
                     */
                    examineBodyForFault(method);
                default:
                    final String messageFormat = "The SOAP endpoint {0} could not be contacted.  HTTP status: {1}"; //$NON-NLS-1$
                    final String message =
                        MessageFormat.format(messageFormat, method.getURI().toString(), Integer.toString(response));
                    throw new EndpointNotFoundException(message, response);
            }
        } catch (final IOException e) {
            ioException = e;
            throw new TransportException(e.getMessage(), e);
        } finally {
            final long total = System.currentTimeMillis() - start;

            if (responseStream != null) {
                try {
                    responseStream.close();
                } catch (final IOException e) {
                    ioException = e;
                }
                responseBytes = responseStream.getBytes();
                contentLength = responseStream.getTotalBytes();
            }
            /*
             * perform logging
             */
            try {
                if (log.isDebugEnabled()) {
                    logExtended(method, serverExecute, total, contentLength, isCompressed, responseBytes, ioException);
                } else {
                    log.info(makeNormalLogEntry(method, serverExecute, total, contentLength, isCompressed));
                }
            } catch (final Throwable t) {
                /*
                 * don't propogate any errors raised while logging
                 */
                log.warn("Error logging SOAP call", t); //$NON-NLS-1$
            }

            method.releaseConnection();
        }
    }

    protected TraceInputStream getResponseStream(final PostMethod method) throws IOException {
        boolean isCompressed = false;
        InputStream responseStream;
        final Header encoding = method.getResponseHeader("Content-Encoding"); //$NON-NLS-1$
        if (encoding != null && encoding.getValue().equalsIgnoreCase("gzip")) //$NON-NLS-1$
        {
            responseStream = new GZIPInputStream(method.getResponseBodyAsStream());
            isCompressed = true;
        } else {
            responseStream = method.getResponseBodyAsStream();
        }

        // Calculate if we want to store read bytes (useful for debugging)
        int storeBytes = -1;
        if (log.isTraceEnabled()
            || (log.isDebugEnabled() && method.getResponseContentLength() <= RESPONSE_MAX_SIZE_FOR_DEBUG_LOGGING)) {
            storeBytes = RESPONSE_MAX_SIZE_FOR_DEBUG_LOGGING;
        }

        return new TraceInputStream(responseStream, storeBytes, isCompressed);
    }

    private void logExtended(
        final PostMethod method,
        final long serverExecuteMs,
        final long totalMs,
        final long contentLength,
        final boolean isCompressed,
        final byte[] responseBytes,
        final Throwable t) throws IOException {
        final StringBuffer sb = new StringBuffer();
        final String newline = System.getProperty("line.separator"); //$NON-NLS-1$

        /*
         * the first line of the trace log message is the same as a normal log
         * message
         */
        sb.append(makeNormalLogEntry(method, serverExecuteMs, totalMs, contentLength, isCompressed)).append(newline);

        /*
         * append request name, path, and headers
         */
        sb.append(method.getName() + " " + method.getPath()).append(newline); //$NON-NLS-1$
        sb.append(newline);

        final Header[] requestHeaders = method.getRequestHeaders();
        for (int i = 0; i < requestHeaders.length; i++) {
            sb.append(requestHeaders[i].getName() + ": " + requestHeaders[i].getValue()).append(newline); //$NON-NLS-1$
        }
        sb.append(newline);

        /*
         * append request body
         */
        final ByteArrayOutputStream requestBodyByteStream = new ByteArrayOutputStream();

        /*
         * this line makes the assumption that we're using
         * BufferedSoapRequestEntity
         */
        method.getRequestEntity().writeRequest(requestBodyByteStream);

        final String requestBodyString = requestBodyByteStream.toString(SOAPRequestEntity.SOAP_ENCODING);
        sb.append(requestBodyString).append(newline);

        /*
         * separate request and response portions of the trace log entry
         */
        sb.append(newline);

        /*
         * In the case of multiple failed attempts at method execution, the
         * PostMethod may come back to us with a null status line, which will
         * cause NullPointerExceptions when invoking some of its methods (like
         * getStatusCode()). We can prevent the exceptions by checking for this
         * internal state.
         */
        final StatusLine statusLine = method.getStatusLine();

        /*
         * append response code and headers
         */
        sb.append(((statusLine != null) ? method.getStatusCode() : -1)
            + " " //$NON-NLS-1$
            + ((statusLine != null) ? method.getStatusText() : "<no status line>")).append(newline); //$NON-NLS-1$
        sb.append(newline);

        final Header[] responseHeaders = method.getResponseHeaders();
        for (int i = 0; i < responseHeaders.length; i++) {
            sb.append(responseHeaders[i].getName() + ": " + responseHeaders[i].getValue()).append(newline); //$NON-NLS-1$
        }
        sb.append(newline);

        String responseBodyAsString = null;
        if (responseBytes != null) {
            responseBodyAsString = EncodingUtil.getString(responseBytes, method.getResponseCharSet());
        }

        StringBuffer base64buffer = null;

        /*
         * If we have a response body, and the base64log is enabled or we
         * couldn't convert the response body to a string, we then log a base64
         * representation of the response body to the base64log.
         */
        if (responseBytes != null && (base64log.isDebugEnabled() || responseBodyAsString == null)) {
            final String base64guid = GUID.newGUIDString();

            final String base64EncodedResponse = getFormattedBase64Encoding(responseBytes);

            base64buffer = new StringBuffer();
            base64buffer.append("-- " //$NON-NLS-1$
                + base64guid
                + " base64 encoded response: " //$NON-NLS-1$
                + responseBytes.length
                + " byte(s) --"); //$NON-NLS-1$
            base64buffer.append(newline);
            base64buffer.append(base64EncodedResponse).append(newline);
            base64buffer.append("-- end base64 encoded response --").append(newline); //$NON-NLS-1$

            sb.append("-- base64 response key: " + base64guid).append(newline); //$NON-NLS-1$
            sb.append(newline);
        }

        sb.append(responseBodyAsString != null ? responseBodyAsString : "-- RESPONSE UNAVAILABLE --"); //$NON-NLS-1$
        sb.append(newline);

        /*
         * if an error occurred during the low-level HTTP call, log it
         */
        if (t != null) {
            sb.append(newline);
            final StringWriter sw = new StringWriter();
            final PrintWriter pw = new PrintWriter(sw);
            t.printStackTrace(pw);
            pw.flush();
            sb.append("ERROR: " + t.getMessage()).append(newline); //$NON-NLS-1$
            sb.append(sw.toString());
        }

        /*
         * finish off the log entry with a separator
         */
        sb.append("--------------------------------------------------------------------------------"); //$NON-NLS-1$
        // String end = sb.toString().substring(sb.length() > 200 ? sb.length()
        // - 200 : 0);
        log.debug(sb.toString());

        /*
         * write the base64 log last - the ensures that if both logs are being
         * appended to the same file, the regular log entry appears first
         */
        if (base64buffer != null) {
            base64log.debug(base64buffer.toString());
        }
    }

    protected String getFormattedBase64Encoding(final byte[] responseBytes) {
        String unformatted;
        try {
            unformatted = new String(Base64.encodeBase64(responseBytes), "US-ASCII"); //$NON-NLS-1$
        } catch (final UnsupportedEncodingException e) {
            /*
             * should never happen: every Java implementation must support
             * US-ASCII encoding:
             * http://java.sun.com/j2se/1.4.2/docs/api/java/nio
             * /charset/Charset.html
             */
            throw new RuntimeException(e);
        }
        final String newline = System.getProperty("line.separator"); //$NON-NLS-1$

        final StringBuffer sb = new StringBuffer();

        int ix = 0;
        while (ix < unformatted.length()) {
            final int newIx = Math.min(ix + 80, unformatted.length());
            sb.append(unformatted.substring(ix, newIx));
            if (newIx < unformatted.length()) {
                sb.append(newline);
            }
            ix = newIx;
        }

        return sb.toString();
    }

    private String makeNormalLogEntry(
        final PostMethod method,
        final long serverExecuteMs,
        final long totalMs,
        final long contentLength,
        final boolean isCompressed) {
        final String methodName = ((BufferedSOAPRequestEntity) method.getRequestEntity()).getMethodName();

        /*
         * In the case of multiple failed attempts at method execution, the
         * PostMethod may come back to us with a null status line, which will
         * cause NullPointerExceptions when invoking some of its methods (like
         * getStatusCode()). We can prevent the exceptions by checking for this
         * internal state.
         */
        final StatusLine statusLine = method.getStatusLine();

        return "SOAP method=\'" //$NON-NLS-1$
            + methodName
            + "\', status=" //$NON-NLS-1$
            + ((statusLine != null) ? method.getStatusCode() : -1)
            + ", content-length=" //$NON-NLS-1$
            + contentLength
            + ", server-wait=" //$NON-NLS-1$
            + serverExecuteMs
            + " ms, parse=" //$NON-NLS-1$
            + (totalMs - serverExecuteMs)
            + " ms, total=" //$NON-NLS-1$
            + totalMs
            + " ms, throughput=" //$NON-NLS-1$
            + Math.round(contentLength / Math.max(totalMs, 1F) * 1000F)
            + " B/s" //$NON-NLS-1$
            + (isCompressed ? ", gzip" : ", uncompressed"); //$NON-NLS-1$ //$NON-NLS-2$
    }

    protected void finishSOAPRequest(final SOAPRequest request) {
        request.getPostMethod().releaseConnection();
    }

    /**
     * Look at the message body for an XML document that might contain a SOAP
     * fault. If one is found, {@link SOAPFault} is thrown; else
     * {@link InvalidServerResponseException} is thrown.
     *
     * @param method
     *        the method containing the (as of yet) unread response body.
     * @throws SOAPFault
     *         if a SOAP fault is found in the body.
     * @throws InvalidServerResponseException
     *         if no fault was found.
     */
    public void examineBodyForFault(final PostMethod method) throws SOAPFault, InvalidServerResponseException {
        Document doc;

        byte[] responseBody = null;
        InputStream responseBodyStream = null;
        if (method.getResponseContentLength() != -1
            && method.getResponseContentLength() <= RESPONSE_MAX_SIZE_FOR_DEBUG_LOGGING) {
            try {
                responseBody = method.getResponseBody();
                responseBodyStream = new ByteArrayInputStream(responseBody);
            } catch (final IOException ex) {
                // ignore
            }
        }

        if (responseBodyStream == null) {
            try {
                responseBodyStream = method.getResponseBodyAsStream();
            } catch (final IOException e) {
                throw new InvalidServerResponseException("Unable to parse obtain the server's response.", e); //$NON-NLS-1$
            }
        }

        try {
            doc = DOMCreateUtils.parseStream(method.getResponseBodyAsStream(), null);
        } catch (final Exception e) {
            if (responseBody != null) {
                final String base64Body = getFormattedBase64Encoding(responseBody);
                final String messageFormat = "unable to parse server's response body:{0}{1}"; //$NON-NLS-1$
                final String message = MessageFormat.format(messageFormat, NEWLINE, base64Body);
                log.warn(message);
            }

            throw new InvalidServerResponseException(
                "The server's error could not be parsed as XML.  No SOAP fault found.", //$NON-NLS-1$
                e);
        }

        /*
         * this subclass method will throw a SOAPFault if the correct soap fault
         * elements are present in the DOM
         */
        examineResponseDOMForFault(doc);

        /*
         * Couldn't find SOAP fault elements in the body.
         */

        final String messageFormat = "no soap fault found in server response from 500: {0}{1}"; //$NON-NLS-1$
        final String message = MessageFormat.format(messageFormat, NEWLINE, DOMSerializeUtils.toString(doc));
        log.warn(message);

        throw new InvalidServerResponseException(
            "The server's error could not be parsed as XML.  No SOAP fault found."); //$NON-NLS-1$
    }

    private void examineHeadersForFederatedAuthURL(final PostMethod method)
        throws URIException,
            FederatedAuthException {
        final Header locationHeader = method.getResponseHeader("Location"); //$NON-NLS-1$
        final Header fedAuthRedirectHeader = method.getResponseHeader("X-TFS-FedAuthRedirect"); //$NON-NLS-1$

        final Header fedAuthIssuerHeader = method.getResponseHeader("X-TFS-FedAuthIssuer"); //$NON-NLS-1$
        final Header fedAuthRealmHeader = method.getResponseHeader("X-TFS-FedAuthRealm"); //$NON-NLS-1$
        final Header fedServerErrorHeader = method.getResponseHeader("X-TFS-ServiceError"); //$NON-NLS-1$
        final Header[] authenticateHeaders = method.getResponseHeaders("WWW-Authenticate"); //$NON-NLS-1$

        final Credentials credentials = client.getState().getCredentials(AuthScope.ANY);
        final String uri = method.getURI().toString();
        final int statusCode = method.getStatusCode();

        final Header authenticationUrlHeader = fedAuthRedirectHeader != null ? fedAuthRedirectHeader : locationHeader;

        if (authenticationUrlHeader != null && fedAuthIssuerHeader != null && fedAuthRealmHeader != null) {
            String authenticationUrl = null;
            String fedAuthIssuer = null;
            String fedAuthRealm = null;
            String fedServerError = null;
            String[] mechanisms = new String[0];

            try {
                authenticationUrl = URLDecoder.decode(authenticationUrlHeader.getValue(), "UTF-8"); //$NON-NLS-1$
                fedAuthIssuer = URLDecoder.decode(fedAuthIssuerHeader.getValue(), "UTF-8"); //$NON-NLS-1$
                fedAuthRealm = URLDecoder.decode(fedAuthRealmHeader.getValue(), "UTF-8"); //$NON-NLS-1$
                fedServerError = URLDecoder.decode(fedServerErrorHeader.getValue(), "UTF-8"); //$NON-NLS-1$

                if (authenticateHeaders != null) {
                    mechanisms = new String[authenticateHeaders.length];

                    for (int i = 0; i < authenticateHeaders.length; i++) {
                        mechanisms[i] = URLDecoder.decode(authenticateHeaders[i].getValue(), "UTF-8"); //$NON-NLS-1$
                    }
                }
            } catch (final Exception e) {
                log.warn("Could not decode federated authentication URL as UTF-8", e); //$NON-NLS-1$
            }

            if (authenticationUrl != null && fedAuthRealm != null) {
                if (statusCode == HttpStatus.SC_MOVED_TEMPORARILY) {
                    /* Throw a FederatedAuthException so that issuers know. */
                    throw new FederatedAuthException(
                        uri,
                        authenticationUrl,
                        fedAuthIssuer,
                        fedAuthRealm,
                        mechanisms,
                        credentials,
                        fedServerError);
                } else {
                    throw new FederatedAuthFailedException(fedServerError, fedAuthRealm);
                }
            }
        }

        /*
         * No ACS URL or Realm found, it could be a 401 response from
         * on-premises server or another error.
         */
        if (statusCode == HttpStatus.SC_UNAUTHORIZED) {
            throw new UnauthorizedException(uri, credentials);
        } else {
            final String messageFormat = Messages.getString("SOAPService.SoapEndpointCouldNotBeContactedFormat"); //$NON-NLS-1$
            final String message = MessageFormat.format(messageFormat, uri, Integer.toString(statusCode));

            throw new EndpointNotFoundException(message, statusCode);
        }
    }

    /**
     * Look at the message headers for an appropriate service error. If one is
     * found, {@link ServiceErrorException} is thrown; else
     * {@link EndpointNotFoundException} is thrown.
     *
     * @param method
     *        the method containing the (as of yet) unread response body.
     * @throws URIException
     * @throws ServiceErrorException
     *         if a SOAP fault is found in the body.
     * @throws EndpointNotFoundException
     *         if no fault was found.
     * @throws URIException
     *         if no fault was found and the uri was invalid
     */
    public void examineHeadersForErrorMessage(final PostMethod method)
        throws ServiceErrorException,
            EndpointNotFoundException,
            URIException {
        final String errorMessage = getServerErrorMessage(method);

        if (errorMessage != null) {
            throw new ServiceErrorException(errorMessage);
        }

        final String messageFormat = Messages.getString("SOAPService.SoapEndpointCouldNotBeContactedFormat"); //$NON-NLS-1$
        final String message =
            MessageFormat.format(messageFormat, method.getURI().toString(), Integer.toString(method.getStatusCode()));
        throw new EndpointNotFoundException(message, method.getStatusCode());
    }

    private String getServerErrorMessage(final PostMethod method) {
        final Header serviceErrorHeader = method.getResponseHeader("X-TFS-ServiceError"); //$NON-NLS-1$

        if (serviceErrorHeader != null) {
            try {
                return URLDecoder.decode(serviceErrorHeader.getValue(), "UTF-8"); //$NON-NLS-1$
            } catch (final Exception e) {
                log.warn("Could not decode service error message as UTF-8", e); //$NON-NLS-1$
            }

        }

        return null;
    }

    /**
     * Examines the given response body DOM for elements representing a SOAP
     * fault. If such elements are found, throw a new SOAPFault. If such
     * elements are not found, do nothing.
     *
     * @param responseDOM
     *        the parsed reponse body as a DOM
     */
    protected abstract void examineResponseDOMForFault(Document responseDOM);

    /**
     * Given a Node, gets its child node by name. If no matching child is found,
     * returns null.
     *
     * @param node
     *        the node to search. If null, null is returned.
     * @param childName
     *        the child name to search for (not null).
     * @return the child node that matched the given childName, null if none
     *         found or given node is null.
     */
    protected Node getChildByName(final Node node, final String childName) {
        Check.notNull(childName, "childName"); //$NON-NLS-1$

        if (node == null) {
            return null;
        }

        final NodeList children = node.getChildNodes();

        for (int i = 0; i < children.getLength(); i++) {
            final Node child = children.item(i);

            if (child != null && child.getNodeName().equalsIgnoreCase(childName)) {
                return child;
            }
        }

        return null;
    }

    /**
     * Gets the HttpClient in use by this stub so that it may be configured or
     * examined.
     *
     * @return the HttpClient used by this stub.
     */
    public final HttpClient getHTTPClient() {
        return client;
    }

    /**
     * Gets the HttpClient this stub is using for its operations. Will not be
     * null.
     *
     * @return the HttpClient this stub is using (not null).
     */
    public HttpClient getClient() {
        return client;
    }

    /**
     * @return the SOAP endpoint in use by this stub.
     */
    public URI getEndpoint() {
        return endpoint;
    }

    /**
     * @return the SOAP port in use by this stub.
     */
    public QName getPort() {
        return port;
    }
}
