// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.license;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.microsoft.tfs.core.config.persistence.DefaultPersistenceStoreProvider;
import com.microsoft.tfs.core.memento.Memento;
import com.microsoft.tfs.core.memento.XMLMemento;
import com.microsoft.tfs.core.util.MementoRepository;
import com.microsoft.tfs.util.listeners.SingleListenerFacade;

public final class LicenseManager {
    private final static Log log = LogFactory.getLog(LicenseManager.class);

    private final MementoRepository mementoRepository =
        new MementoRepository(DefaultPersistenceStoreProvider.INSTANCE.getConfigurationPersistenceStore());

    private static final String EULA_ACCEPTED_PROPERTY = "com.microsoft.tfs.client.eulaAccepted"; //$NON-NLS-1$

    private static final String MEMENTO_NAME = "com.microsoft.tfs.client.productid"; //$NON-NLS-1$

    private static final String ROOT_MEMENTO_NAME = "ProductIdData"; //$NON-NLS-1$

    private static final String EULA_MEMENTO_NAME = "eula-14.0"; //$NON-NLS-1$
    private static final String EULA_ACCEPTED_KEY_NAME = "value"; //$NON-NLS-1$

    private final static Object instanceLock = new Object();
    private static LicenseManager instance;

    private final Object dataLock = new Object();

    private boolean eulaAccepted = false;
    private final ProductID productId = ProductID.DEFAULT_PRODUCT_ID;

    private final SingleListenerFacade listeners = new SingleListenerFacade(LicenseListener.class);

    private LicenseManager() {
    }

    /**
     * Gets the instance of the running LicenseManager.
     *
     * @return A LicenseManager which can hold Licenses
     */
    public static LicenseManager getInstance() {
        synchronized (instanceLock) {
            if (instance == null) {
                instance = new LicenseManager();

                try {
                    instance.read();
                } catch (final Throwable t) {
                    log.error("Could not read product id data", t); //$NON-NLS-1$
                }
            }

            return instance;
        }
    }

    public boolean isEULAAccepted() {
        synchronized (dataLock) {
            return eulaAccepted;
        }
    }

    public void setEULAAccepted(final boolean accepted) {
        synchronized (dataLock) {
            eulaAccepted = accepted;
        }

        ((LicenseListener) listeners.getListener()).eulaAcceptanceChanged(accepted);
    }

    public ProductID getProductID() {
        synchronized (dataLock) {
            return productId;
        }
    }

    public void read() {
        synchronized (dataLock) {
            eulaAccepted = false;

            readLicenseMemento();
            readLicenseProperties();
        }
    }

    private void readLicenseMemento() {
        final Memento rootMemento = mementoRepository.load(MEMENTO_NAME);

        if (rootMemento == null) {
            return;
        }

        final Memento eulaAcceptedMemento = rootMemento.getChild(EULA_MEMENTO_NAME);

        if (eulaAcceptedMemento != null) {
            final Boolean eulaAcceptedValue = eulaAcceptedMemento.getBoolean(EULA_ACCEPTED_KEY_NAME);
            eulaAccepted = (eulaAcceptedValue == null) ? false : eulaAcceptedValue.booleanValue();
        }
    }

    private void readLicenseProperties() {
        if (eulaAccepted) {
            return;
        }

        /*
         * Possible workaround for an unreproduced user-reported bug where we
         * continually prompt for licensing in the UI, even though the license
         * appears to be correctly configured. Allow the user to override EULA
         * acceptance with a system property.
         */

        if ("true".equalsIgnoreCase(System.getProperty(EULA_ACCEPTED_PROPERTY))) //$NON-NLS-1$
        {
            eulaAccepted = true;
        }
    }

    public void write() {
        synchronized (dataLock) {
            Memento rootMemento = mementoRepository.load(MEMENTO_NAME);

            if (rootMemento == null) {
                rootMemento = new XMLMemento(ROOT_MEMENTO_NAME);
            } else {
                /*
                 * Note: we remove only the *active* children for this release
                 * (and rewrite them with the accurate data.) Our
                 * EULA_MEMENTO_NAME is versioned, and we only wish to remove
                 * the current version. Do not remove previous (or future)
                 * versioned names, or this breaks downgrade/upgrade eula
                 * acceptance.
                 */
                rootMemento.removeChildren(EULA_MEMENTO_NAME);
            }

            final Memento eulaAcceptedMemento = rootMemento.createChild(EULA_MEMENTO_NAME);
            eulaAcceptedMemento.putBoolean(EULA_ACCEPTED_KEY_NAME, eulaAccepted);

            mementoRepository.save(MEMENTO_NAME, rootMemento);
        }
    }

    public void addListener(final LicenseListener licenseListener) {
        listeners.addListener(licenseListener);
    }

    public void removeListener(final LicenseListener licenseListener) {
        listeners.removeListener(licenseListener);
    }
}
