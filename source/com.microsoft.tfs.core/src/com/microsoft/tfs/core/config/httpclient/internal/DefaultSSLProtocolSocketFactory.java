// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.config.httpclient.internal;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.microsoft.tfs.core.config.EnvironmentVariables;
import com.microsoft.tfs.core.httpclient.ConnectTimeoutException;
import com.microsoft.tfs.core.httpclient.params.HttpConnectionParams;
import com.microsoft.tfs.core.httpclient.protocol.SecureProtocolSocketFactory;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.StringUtil;

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

    /**
     * Name of the Java system property used to disable modifying the
     * {@link SSLSocketFactory} from the default. If this is set (to any value),
     * we will always use the standard system {@link SSLSocketFactory}.
     */
    public static final String DISABLE_PROPERTY_NAME =
        "com.microsoft.tfs.core.config.httpclient.sslsocketfactory.disable"; //$NON-NLS-1$

    /**
     * Name of the SSL protocol to be used to create the
     * {@link SSLSocketFactory}. Valid protocol names: SSL, SSLv3, TLS, TLSv1,
     * TLSv1.1, TLSv1.2
     */
    public static final String SSL_PROTOCOL_PROPERTY_NAME =
        "com.microsoft.tfs.core.config.httpclient.sslsocketfactory.sslprotocol"; //$NON-NLS-1$

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
        final HttpConnectionParams params)
        throws IOException,
            UnknownHostException,
            ConnectTimeoutException {
        Check.notNull(params, "params"); //$NON-NLS-1$

        final int timeout = params.getConnectionTimeout();

        final Socket socket = getSocketFactory(params).createSocket();
        configureSNI(socket, host);
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
        final boolean autoClose)
        throws IOException,
            UnknownHostException {
        Check.notNull(params, "params"); //$NON-NLS-1$

        final Socket ssocket = getSocketFactory(params).createSocket(socket, host, port, autoClose);
        configureSNI(ssocket, host);
        return ssocket;
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

    private SSLContext getSSLContext() throws NoSuchAlgorithmException {

        final String requestedProtocol = getRequestedProtocol();

        try {
            return SSLContext.getInstance(requestedProtocol);
        } catch (final NoSuchAlgorithmException e) {
            log.error("Cannot create SSL context with the requested protocol " + requestedProtocol, e); //$NON-NLS-1$
            log.info("Using SSL context with the default protocol TLS"); //$NON-NLS-1$

            return SSLContext.getInstance("TLS"); //$NON-NLS-1$
        }
    }

    private String getRequestedProtocol() {
        final String protocol = System.getProperty(SSL_PROTOCOL_PROPERTY_NAME);
        if (StringUtil.isNullOrEmpty(protocol)) {
            return EnvironmentVariables.getString(EnvironmentVariables.SSL_PROTOCOL_NAME, "TLS"); //$NON-NLS-1$
        }
        return protocol;
    }

    private void configureSNI(final Socket socket, final String host) {

        if (System.getProperty("java.version").compareTo("1.8") < 0) { //$NON-NLS-1$ //$NON-NLS-2$
            return;
        }

        /*
         * Classes used to configure Server Name client-hello extension were
         * introduced in Java 8. So, we neither can use nor compile this code
         * using Java 6-7. Thus, let's use reflection.
         */

        try {
            final SSLSocket sslSocket = (SSLSocket) socket;
            final SSLParameters params = sslSocket.getSSLParameters();

            final Class<?> sniHostNameClass = Class.forName("javax.net.ssl.SNIHostName"); //$NON-NLS-1$
            final Constructor<?> sniHostNameClassConstructor = sniHostNameClass.getConstructor(String.class);

            final Object serverName = sniHostNameClassConstructor.newInstance(host);
            final List<Object> serverNames = new ArrayList<Object>(1);
            serverNames.add(serverName);

            final Class<?> paramsClass = params.getClass();
            final Method setServerNames = paramsClass.getMethod("setServerNames", List.class); //$NON-NLS-1$
            setServerNames.invoke(params, serverNames);

            sslSocket.setSSLParameters(params);
        } catch (final Exception e) {
            log.error("Eror configuring SSL socket with SNI cipher extension:", e); //$NON-NLS-1$
        }
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
                final SSLContext context = getSSLContext();

                context.init(
                    // Use the default key managers.
                    getDefaultKeyManagers(),
                    // Use the default x509 trust manager.
                    new TrustManager[] {
                        new DefaultX509TrustManager(null)
                    },
                    null);

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
                final SSLContext context = getSSLContext();

                context.init(
                    // Use the default key managers.
                    getDefaultKeyManagers(),
                    // Use the self-signed x509 trust manager.
                    new TrustManager[] {
                        new SelfSignedX509TrustManager(null)
                    },
                    null);

                selfSignedSocketFactory = context.getSocketFactory();
            }

            return selfSignedSocketFactory;
        }
    }

    private KeyManager[] getDefaultKeyManagers() throws KeyStoreException, NoSuchAlgorithmException {
        final String keyStorePath = System.getProperty("javax.net.ssl.keyStore"); //$NON-NLS-1$

        if (!StringUtil.isNullOrEmpty(keyStorePath)) {
            final String keyStoreType = System.getProperty("javax.net.ssl.keyStoreType", "JKS"); //$NON-NLS-1$ //$NON-NLS-2$
            final String keyStorePassword = System.getProperty("javax.net.ssl.keyStorePassword", StringUtil.EMPTY); //$NON-NLS-1$

            final KeyManagerFactory keyManagerFactory =
                KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            final KeyStore keyStore = KeyStore.getInstance(keyStoreType);

            try {
                final InputStream keyStoreFile = new FileInputStream(keyStorePath);

                keyStore.load(keyStoreFile, keyStorePassword.toCharArray());
                keyManagerFactory.init(keyStore, null);

                KeyManager[] managers = keyManagerFactory.getKeyManagers();

                return managers;
            } catch (final Exception e) {
                // Ignore errors and use default behavior
                log.warn(MessageFormat.format("Error accessingt the client key store {0}", keyStorePath), e); //$NON-NLS-1$
            }
        }

        return null;
    }
}
