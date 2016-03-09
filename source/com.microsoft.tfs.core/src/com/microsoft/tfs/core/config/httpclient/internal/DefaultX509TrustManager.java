// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.config.httpclient.internal;

import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * An {@link X509TrustManager} that includes some additional trusted
 * certificates (for example, the Microsoft Internet Authority CA cert.) These
 * additional certificates may be important for connecting to Team Foundation
 * Servers, particularly hosted (Azure) servers.
 *
 * After configuring the {@link KeyStore} with these additional certificates,
 * this trust manager delegates to the system's default trust manager for the
 * default algorithm.
 *
 * @threadsafety unknown
 */
public class DefaultX509TrustManager implements X509TrustManager {
    private final X509TrustManager standardTrustManager;

    /** Log object for this class. */
    private static final Log log = LogFactory.getLog(SelfSignedX509TrustManager.class);

    /*
     * Resources that contain X509 certificates that should be consider trusted
     * but are not necessarily in the system cacerts file. These will be
     * automatically appended to the certificate chain delivered by the server
     * when they are referenced in said certificate chain. (This emulates the
     * behavior that they were actually delivered by the server.)
     *
     * This should, of course, ONLY be used for well-trusted certificates in
     * extreme situations. For example, delivery of trusted Azure certificates
     * that are also trusted by the internet community "at large", and would be
     * delivered normally in browsers (but we cannot rely on old Java versions
     * having them.)
     */
    private static String[] certificateResources = new String[] {
        "certs/MicrosoftInternetAuthority.x509", //$NON-NLS-1$
    };

    /* The certificate resources (above) are loaded in these certificates. */
    private static final X509Certificate[] certificateAdditions;

    static {
        final List<X509Certificate> certificateList = new ArrayList<X509Certificate>();

        if (certificateResources != null) {
            for (final String resource : certificateResources) {
                final X509Certificate certificate = loadResourceAsX509Certificate(resource);

                if (certificate != null) {
                    certificateList.add(certificate);
                }
            }
        }

        certificateAdditions = certificateList.toArray(new X509Certificate[certificateList.size()]);
    }

    public DefaultX509TrustManager(final KeyStore keyStore)
        throws NoSuchAlgorithmException,
            KeyStoreException,
            CertificateException,
            IOException {
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
    public void checkServerTrusted(X509Certificate[] certificates, final String authType) throws CertificateException {
        if (certificates != null && certificates.length > 0) {
            /*
             * See if the terminal certificate in the chain is known to us (we
             * have its certificate.) If so, we can add our certificate to be
             * the last in the list.
             *
             * This emulates sending the certificate in the server's chain,
             * without making the server team do any actual work.
             */
            final X509Certificate terminalCertificate = certificates[certificates.length - 1];

            /*
             * Make sure the terminal certificate is not self-signed. We cannot
             * possibly have an issuer certificate to add if the terminal
             * certificate is self-signed, and we may cause problems with the
             * certificate validation.
             */
            if (!terminalCertificate.getIssuerX500Principal().equals(terminalCertificate.getSubjectX500Principal())) {
                for (final X509Certificate certificateAddition : certificateAdditions) {
                    /*
                     * If this certificate was issued by our known-good
                     * principal, and its issuer is not its principal (ie, it is
                     * not self-signed), then include it.
                     */
                    if (terminalCertificate.getIssuerX500Principal().equals(
                        certificateAddition.getSubjectX500Principal())) {
                        log.info(MessageFormat.format(
                            "Including certificate for {0}", //$NON-NLS-1$
                            certificateAddition.getSubjectX500Principal()));

                        /*
                         * Rewrite the certificate array to include our trusted
                         * certificate at the end of the chain.
                         */
                        final X509Certificate[] newCertificates = new X509Certificate[certificates.length + 1];

                        for (int i = 0; i < certificates.length; i++) {
                            newCertificates[i] = certificates[i];
                        }

                        newCertificates[newCertificates.length - 1] = certificateAddition;

                        certificates = newCertificates;

                        break;
                    }
                }
            }
        }

        standardTrustManager.checkServerTrusted(certificates, authType);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public X509Certificate[] getAcceptedIssuers() {
        return standardTrustManager.getAcceptedIssuers();
    }

    /**
     * Opens the specified resource and returns the data contained as an
     * {@link X509Certificate}. If the resource could not be loaded or could not
     * be converted to an {@link X509Certificate}, then <code>null</code> is
     * returned.
     *
     * @param resource
     *        The resource path to open
     */
    private static X509Certificate loadResourceAsX509Certificate(final String resource) {
        CertificateFactory certificateFactory;

        try {
            certificateFactory = CertificateFactory.getInstance("X.509"); //$NON-NLS-1$
        } catch (final Exception e) {
            log.warn("Could not load X509 certificate factory", e); //$NON-NLS-1$
            return null;
        }

        final InputStream certificateStream = DefaultX509TrustManager.class.getResourceAsStream(resource);

        if (certificateStream == null) {
            log.warn(MessageFormat.format("Could not load X509 certificate from {0}", resource)); //$NON-NLS-1$
            return null;
        }

        try {
            final Certificate certificate = certificateFactory.generateCertificate(certificateStream);

            if (certificate != null && certificate instanceof X509Certificate) {
                return (X509Certificate) certificate;
            } else {
                log.warn(MessageFormat.format("Could not generate X509 certificate from {0}", resource)); //$NON-NLS-1$
            }
        } catch (final CertificateException e) {
            log.warn(MessageFormat.format("Could not generate X509 certificate from {0}", resource), e); //$NON-NLS-1$
        }

        return null;
    }
}
