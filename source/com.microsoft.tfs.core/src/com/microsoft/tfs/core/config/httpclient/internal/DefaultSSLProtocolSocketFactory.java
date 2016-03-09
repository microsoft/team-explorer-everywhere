// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.config.httpclient.internal;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.microsoft.tfs.core.httpclient.ConnectTimeoutException;
import com.microsoft.tfs.core.httpclient.params.HttpConnectionParams;
import com.microsoft.tfs.core.httpclient.protocol.SecureProtocolSocketFactory;
import com.microsoft.tfs.util.Check;

/**
 * An SSL socket factory for HTTPClient that tweaks certificate validation,
 * either providing the ability to accept self-signed certificates, or one that
 * includes additional certificates (notably newer well-trusted certificates
 * like the Microsoft Internet Authority certificate, vital for connecting to
 * Azure-hosted TFS servers on platforms that lack the updated Microsoft
 * Internet Authority cert.)
 */
public class DefaultSSLProtocolSocketFactory implements SecureProtocolSocketFactory {
    public static final String ACCEPT_UNTRUSTED_CERTIFICATES_PARAMETER =
        "DefaultSSLProtocolSocketFactory.acceptUntrustedCertificates"; //$NON-NLS-1$

    /*
     * Name of the Java system property used to disable modifying the {@link
     * SSLSocketFactory} from the default. If this is set (to any value), we
     * will always use the standard system {@link SSLSocketFactory}.
     */
    public static final String DISABLE_PROPERTY_NAME =
        "com.microsoft.tfs.core.config.httpclient.sslsocketfactory.disable"; //$NON-NLS-1$

    private static final Log log = LogFactory.getLog(DefaultSSLProtocolSocketFactory.class);

    private static final Object lock = new Object();

    private static SSLSocketFactory standardSocketFactory;
    private static SSLSocketFactory selfSignedSocketFactory;

    /**
     * {@inheritDoc}
     */
    @Override
    public Socket createSocket(
        final String host,
        final int port,
        final InetAddress localAddress,
        final int localPort,
        final HttpConnectionParams params) throws IOException, UnknownHostException, ConnectTimeoutException {
        Check.notNull(params, "params"); //$NON-NLS-1$

        final int timeout = params.getConnectionTimeout();

        final Socket socket = getSocketFactory(params).createSocket();
        socket.bind(new InetSocketAddress(localAddress, localPort));
        socket.connect(new InetSocketAddress(host, port), timeout);
        return socket;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Socket createSocket(
        final Socket socket,
        final String host,
        final int port,
        final HttpConnectionParams params,
        final boolean autoClose) throws IOException, UnknownHostException {
        return getSocketFactory(params).createSocket(socket, host, port, autoClose);
    }

    private SSLSocketFactory getSocketFactory(final HttpConnectionParams params) {
        if (isEnabled()) {
            try {
                SSLSocketFactory socketFactory;

                if (params.getBooleanParameter(ACCEPT_UNTRUSTED_CERTIFICATES_PARAMETER, false)) {
                    socketFactory = getSelfSignedSocketFactory(params);
                } else {
                    socketFactory = getStandardSocketFactory(params);
                }

                if (socketFactory != null) {
                    return socketFactory;
                }
            } catch (final Exception e) {
                log.warn("Could not create SSL socket factory, falling back to default", e); //$NON-NLS-1$
            }
        } else {
            log.info("SSLSocketFactory is disabled, falling back to system"); //$NON-NLS-1$
        }

        return (SSLSocketFactory) SSLSocketFactory.getDefault();
    }

    private boolean isEnabled() {
        return (System.getProperty(DISABLE_PROPERTY_NAME) == null);
    }

    /**
     * Create a new SSL socket factory that is tolerant of self-signed
     * certificates.
     *
     * @throws IOException
     * @throws CertificateException
     */
    private SSLSocketFactory getStandardSocketFactory(final HttpConnectionParams params)
        throws NoSuchAlgorithmException,
            KeyManagementException,
            KeyStoreException,
            CertificateException,
            IOException {
        synchronized (lock) {
            if (standardSocketFactory == null) {
                final SSLContext context = SSLContext.getInstance("SSL"); //$NON-NLS-1$

                /* Use the self-signed x509 trust manager. */
                context.init(null, new TrustManager[] {
                    new DefaultX509TrustManager(null)
                }, null);

                standardSocketFactory = context.getSocketFactory();
            }

            return standardSocketFactory;
        }
    }

    /**
     * Create a new SSL socket factory that is tolerant of self-signed
     * certificates.
     *
     * @throws NoSuchAlgorithmException
     * @throws KeyStoreException
     * @throws KeyManagementException
     */
    private SSLSocketFactory getSelfSignedSocketFactory(final HttpConnectionParams params)
        throws NoSuchAlgorithmException,
            KeyManagementException,
            KeyStoreException {
        synchronized (lock) {
            if (selfSignedSocketFactory == null) {
                final SSLContext context = SSLContext.getInstance("SSL"); //$NON-NLS-1$

                /* Use the self-signed x509 trust manager. */
                context.init(null, new TrustManager[] {
                    new SelfSignedX509TrustManager(null)
                }, null);

                selfSignedSocketFactory = context.getSocketFactory();
            }

            return selfSignedSocketFactory;
        }
    }
}
