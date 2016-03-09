// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.config.httpclient.internal;

import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.text.MessageFormat;

import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * The default {@link X509TrustManager} for Team Foundation Server clients,
 * capable of accepting self-signed certificates.
 */
public class SelfSignedX509TrustManager implements X509TrustManager {
    private X509TrustManager standardTrustManager = null;

    /** Log object for this class. */
    private static final Log log = LogFactory.getLog(SelfSignedX509TrustManager.class);

    /**
     * Creates a trust manager capable of accepting self-signed certificates.
     *
     * @param keyStore
     *        The {@link KeyStore} to use for user-specified keys (or
     *        <code>null</code>)
     * @throws NoSuchAlgorithmException
     * @throws KeyStoreException
     */
    public SelfSignedX509TrustManager(final KeyStore keyStore) throws NoSuchAlgorithmException, KeyStoreException {
        final TrustManagerFactory factory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        factory.init(keyStore);
        final TrustManager[] trustManagers = factory.getTrustManagers();

        if (trustManagers.length == 0) {
            throw new NoSuchAlgorithmException("No trust manager found"); //$NON-NLS-1$
        }

        if (!(trustManagers[0] instanceof X509TrustManager)) {
            throw new NoSuchAlgorithmException("No X509 trust manager found"); //$NON-NLS-1$
        }

        standardTrustManager = (X509TrustManager) trustManagers[0];
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void checkClientTrusted(final X509Certificate[] certificates, final String authType)
        throws CertificateException {
        standardTrustManager.checkClientTrusted(certificates, authType);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void checkServerTrusted(final X509Certificate[] certificates, final String authType)
        throws CertificateException {
        if (certificates != null && certificates.length >= 1) {
            if (log.isDebugEnabled()) {
                log.debug("Accepting self-signed certificate with certificate chain:"); //$NON-NLS-1$

                for (int i = 0; i < certificates.length; i++) {
                    log.debug(MessageFormat.format("X509Certificate[{0}] = {1}", Integer.toString(i), certificates[i])); //$NON-NLS-1$
                }
            }

            certificates[0].checkValidity();
        } else {
            standardTrustManager.checkServerTrusted(certificates, authType);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public X509Certificate[] getAcceptedIssuers() {
        return standardTrustManager.getAcceptedIssuers();
    }
}
