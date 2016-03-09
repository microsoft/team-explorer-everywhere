// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.internal.db;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.sql.Connection;
import java.sql.Driver;
import java.text.MessageFormat;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.microsoft.tfs.core.clients.workitem.internal.metadata.Metadata;
import com.microsoft.tfs.core.persistence.FilesystemPersistenceStore;
import com.microsoft.tfs.core.persistence.PersistenceStore;
import com.microsoft.tfs.core.persistence.VersionedVendorFilesystemPersistenceStore;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.locking.AdvisoryFileLock;

/**
 * A class responsible for configuring a database and creating connections to
 * it. A ConnectionConfiguration takes care of loading the database
 * configuration file, loading the proper JDBC driver, and creating Connection
 * objects.
 */
public class ConnectionConfiguration {
    private static final Log log = LogFactory.getLog(ConnectionConfiguration.class);

    private static final String DB_PROPS_FILE_NAME = "db.properties"; //$NON-NLS-1$

    private static final String DRIVER_CONFIG_KEY = "jdbc.driver.classname"; //$NON-NLS-1$
    private static final String URL_CONFIG_KEY = "jdbc.connection.url"; //$NON-NLS-1$
    private static final String USERNAME_CONFIG_KEY = "jdbc.username"; //$NON-NLS-1$
    private static final String PASSWORD_CONFIG_KEY = "jdbc.password"; //$NON-NLS-1$
    private static final String CLASSPATH_CONFIG_KEY = "jdbc.classpath"; //$NON-NLS-1$

    private static final String DRIVER_DEFAULT = "org.hsqldb.jdbcDriver"; //$NON-NLS-1$
    private static final String URL_DEFAULT = "jdbc:hsqldb:LOCAL_SETTINGS_DISKFILE"; //$NON-NLS-1$
    private static final String USERNAME_DEFAULT = "sa"; //$NON-NLS-1$
    private static final String PASSWORD_DEFAULT = ""; //$NON-NLS-1$
    private static final String URL_FALLBACK = "jdbc:hsqldb:mem:DB"; //$NON-NLS-1$

    private final String driverClass;
    private String url;
    private final String username;
    private final String password;
    private File databaseDiskDirectory;
    private AdvisoryFileLock lock;
    private final String pathIdentifer;
    private final Configuration configuration;

    private Driver driver;

    private final boolean verbose;

    public ConnectionConfiguration(final PersistenceStore baseStore, final String pathId) {
        this(baseStore, pathId, false);
    }

    public ConnectionConfiguration(final PersistenceStore cacheStore, final String pathId, final boolean verboseInput) {
        Check.notNull(cacheStore, "cacheStore"); //$NON-NLS-1$

        pathIdentifer = Metadata.SCHEMA_VERSION + "-" + pathId; //$NON-NLS-1$
        verbose = verboseInput;

        configuration = new Configuration(ConnectionConfiguration.class, "/" + DB_PROPS_FILE_NAME); //$NON-NLS-1$

        driverClass = configuration.getConfiguration(DRIVER_CONFIG_KEY, DRIVER_DEFAULT);
        url = configuration.getConfiguration(URL_CONFIG_KEY, URL_DEFAULT);
        username = configuration.getConfiguration(USERNAME_CONFIG_KEY, USERNAME_DEFAULT);
        password = configuration.getConfiguration(PASSWORD_CONFIG_KEY, PASSWORD_DEFAULT);

        final String extraClasspath = configuration.getConfiguration(CLASSPATH_CONFIG_KEY, null);

        if (url.equals(URL_DEFAULT)) {
            if (cacheStore instanceof VersionedVendorFilesystemPersistenceStore) {
                /*
                 * This may still return null if no candidate areas were
                 * lockable.
                 */
                databaseDiskDirectory =
                    getDirectoryForDiskDatabase((VersionedVendorFilesystemPersistenceStore) cacheStore, pathIdentifer);
            } else {
                log.warn(MessageFormat.format(
                    "The {0} used to create {1} is not a {2}, which is required to retarget HSQLDB storage (it can only use files).  The fallback URL of {3} will be used.", //$NON-NLS-1$
                    PersistenceStore.class.getName(),
                    ConnectionConfiguration.class.getName(),
                    VersionedVendorFilesystemPersistenceStore.class.getName(),
                    URL_FALLBACK));
            }

            if (databaseDiskDirectory == null) {
                url = URL_FALLBACK;
            } else {
                final File db = new File(databaseDiskDirectory, "teamexplorer"); //$NON-NLS-1$
                url = "jdbc:hsqldb:file:" + db.getAbsolutePath(); //$NON-NLS-1$
            }
        }

        ClassLoader jdbcLoader = getClass().getClassLoader();

        if (extraClasspath != null) {
            final File extraJar = new File(extraClasspath);
            try {
                jdbcLoader = new URLClassLoader(new URL[] {
                    extraJar.toURL()
                }, jdbcLoader);
            } catch (final MalformedURLException ex) {
                throw new RuntimeException(ex);
            }
        }

        try {
            driver = (Driver) jdbcLoader.loadClass(driverClass).newInstance();
        } catch (final Throwable t) {
            throw new RuntimeException(MessageFormat.format(
                "unable to load specified jdbc driver class [{0}]", //$NON-NLS-1$
                driverClass), t);
        }

        if (verbose) {
            System.out.println(MessageFormat.format("DB connection URL:     [{0}]", url)); //$NON-NLS-1$
            System.out.println(MessageFormat.format(
                "DB driver class:       [{0}] (version {1}.{2})", //$NON-NLS-1$
                driver.getClass().getName(),
                Integer.toString(driver.getMajorVersion()),
                Integer.toString(driver.getMinorVersion())));

            System.out.println(MessageFormat.format(
                "DB driver loaded from: [{0}]", //$NON-NLS-1$
                getDriverClassURL().toExternalForm()));
        }
    }

    public URL getDriverClassURL() {
        final String resourceName = driverClass.replaceAll("\\.", "/") + ".class"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        return driver.getClass().getClassLoader().getResource(resourceName);
    }

    public int getDriverMajorVersion() {
        return driver.getMajorVersion();
    }

    public int getDriverMinorVersion() {
        return driver.getMinorVersion();
    }

    public String getPathIdentifier() {
        return pathIdentifer;
    }

    public File getDatabaseDiskDirectory() {
        return databaseDiskDirectory;
    }

    public String getDriverClass() {
        return driverClass;
    }

    public String getURL() {
        return url;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public DBConnection createNewConnection() {
        try {
            final Properties info = new Properties();
            info.setProperty("user", username); //$NON-NLS-1$
            info.setProperty("password", password); //$NON-NLS-1$

            final Connection connection = driver.connect(url, info);

            if (connection == null) {
                throw new RuntimeException(MessageFormat.format(
                    "the driver [{0}] did not accept [{1}]", //$NON-NLS-1$
                    driver.getClass().getName(),
                    url));
            }

            return new DBConnection(connection, driverClass);
        } catch (final Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    private File getDirectoryForDiskDatabase(
        final VersionedVendorFilesystemPersistenceStore cacheStore,
        final String pathIdentifier) {
        final PersistenceStore scopedDatabaseStore =
            cacheStore.getChildStore("TEE-WorkItemTracking").getChildStore(pathIdentifier); //$NON-NLS-1$

        for (int i = 0; i < 1000; i++) {
            final String name = "data" + (i != 0 ? String.valueOf(i) : ""); //$NON-NLS-1$ //$NON-NLS-2$

            final PersistenceStore candidateStore = scopedDatabaseStore.getChildStore(name);

            lock = tryLock(candidateStore);

            if (lock != null) {
                return ((FilesystemPersistenceStore) candidateStore).getStoreFile();
            }
        }

        return null;
    }

    private AdvisoryFileLock tryLock(final PersistenceStore store) {
        try {
            store.initialize();

            final AdvisoryFileLock lock = store.getStoreLock(false);

            if (lock == null) {
                log.info(MessageFormat.format("unable to lock [{0}]", store.toString())); //$NON-NLS-1$
            } else {
                log.info(MessageFormat.format("locked [{0}]", store.toString())); //$NON-NLS-1$
            }
            return lock;
        } catch (final IOException e) {
            log.warn(MessageFormat.format("error getting lock on [{0}]", store.toString()), e); //$NON-NLS-1$
            return null;
        } catch (final InterruptedException e) {
            log.warn(MessageFormat.format("error getting lock on [{0}]", store.toString()), e); //$NON-NLS-1$
            return null;
        }
    }

    public void releaseLock() {
        if (lock != null) {
            try {
                lock.release();
            } catch (final IOException e) {
                log.warn("error releasing db directory lock", e); //$NON-NLS-1$
            }
        }
    }
}
