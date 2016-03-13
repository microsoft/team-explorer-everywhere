// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.persistence;

import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.microsoft.tfs.core.clients.versioncontrol.path.LocalPath;
import com.microsoft.tfs.core.config.EnvironmentVariables;
import com.microsoft.tfs.core.util.SpecialFolders;
import com.microsoft.tfs.jni.PlatformMiscUtils;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.Platform;
import com.microsoft.tfs.util.StringUtil;

/**
 * <p>
 * Extends {@link FilesystemPersistenceStore} to automatically find the right
 * base directory by looking in some system locations, depending on platform.
 * </p>
 * <p>
 * A Visual Studio-compatible location is used on Windows.
 * </p>
 *
 * @since TEE-SDK-10.1
 * @threadsafety thread-safe
 */
public class VersionedVendorFilesystemPersistenceStore extends FilesystemPersistenceStore {
    private final static Log log = LogFactory.getLog(VersionedVendorFilesystemPersistenceStore.class);

    /**
     * Creates a {@link FilesystemPersistenceStore} which points at a directory
     * inside the automatically detected "home directory" location (depends on
     * platform) with the specified vendor name, application name, and settings
     * version mixed into the end of the path. Mixing in these parts gives
     * better control over isolation.
     *
     * See {@link #getChildStore(String)} to create children inside this
     * persistence store.
     *
     * @param vendorName
     *        the vendor name to mix into the path (must not be
     *        <code>null</code> or empty)
     * @param applicationName
     *        the application name to add after the vendor name (must not be
     *        <code>null</code> or empty)
     * @param version
     *        the current version to add after the application name (must not be
     *        <code>null</code> or empty)
     */
    public VersionedVendorFilesystemPersistenceStore(
        final String vendorName,
        final String applicationName,
        final String version) {
        this(makeDirectoryForVendorApplicationVersion(vendorName, applicationName, version));
    }

    /**
     * Creates a {@link VersionedVendorFilesystemPersistenceStore} for a
     * directory.
     *
     * @param directory
     *        the directory which does not have to exist (must not be
     *        <code>null</code>)
     */
    protected VersionedVendorFilesystemPersistenceStore(final File directory) {
        super(directory);
    }

    /**
     * Gets the local directory to store things in for the given vendor,
     * application, and version.
     *
     * @param vendorName
     *        the vendor name (must not be <code>null</code> or empty)
     * @param applicationName
     *        the application name (must not be <code>null</code> or empty)
     * @param version
     *        the version string (must not be <code>null</code> or empty)
     * @return the {@link File} for the constructed path, like
     *         "~/.vendorName/applicationName/version"
     */
    private static File makeDirectoryForVendorApplicationVersion(
        final String vendorName,
        final String applicationName,
        final String version) {
        Check.notNullOrEmpty(vendorName, "vendorName"); //$NON-NLS-1$
        Check.notNullOrEmpty(applicationName, "applicationName"); //$NON-NLS-1$
        Check.notNullOrEmpty(version, "version"); //$NON-NLS-1$

        String path =
            PlatformMiscUtils.getInstance().getEnvironmentVariable(EnvironmentVariables.TEE_PROFILE_DIRECTORY);

        if (path != null) {
            path = path.trim();
            try {
                if (StringUtil.isNullOrEmpty(path)) {
                    log.warn("User specified profile location path is empty, TEE will use default profile location."); //$NON-NLS-1$
                } else if (!LocalPath.isPathRooted(path)) {
                    log.warn("User specified location " //$NON-NLS-1$
                        + path
                        + "is not an absolute path. TEE will use default profile location."); //$NON-NLS-1$
                } else {
                    final File file = new File(path).getCanonicalFile();
                    if (!file.exists()) {
                        file.mkdirs();
                    }

                    if (file.exists() && file.isDirectory() && file.canRead() && file.canWrite()) {
                        return file;
                    } else {
                        log.warn("User specified location " //$NON-NLS-1$
                            + path
                            + "can not be accessed. TEE will use default profile location."); //$NON-NLS-1$
                    }
                }
            } catch (final IOException e) {
                log.error("Exception testing profile location specified by the user: " //$NON-NLS-1$
                    + path
                    + ".\n" //$NON-NLS-1$
                    + "TEE will use the default profile location.", e); //$NON-NLS-1$
            }
        }

        if (Platform.isCurrentPlatform(Platform.WINDOWS)) {
            /*
             * Check to see if we can find the user's local application data
             * directory.
             */
            path = SpecialFolders.getLocalApplicationDataPath();
            if (path == null || path.length() == 0) {
                /*
                 * If the user has never logged onto this box they will not have
                 * a local application data directory. Check to see if they have
                 * a roaming network directory that moves with them.
                 */
                path = SpecialFolders.getApplicationDataPath();
                if (path == null || path.length() == 0) {
                    /*
                     * The user does not have a roaming network directory
                     * either. Just place the cache in the common area.
                     */
                    path = SpecialFolders.getCommonApplicationDataPath();
                }
            }

            // "C:\\Users\\[username]\\AppData\\Local\\Microsoft
            path = path + File.separator + vendorName;
        } else if (Platform.isCurrentPlatform(Platform.MAC_OS_X)) {
            // Use the user's Library directory, creating a vendor name
            // directory there.

            // "~/Library/Application Support/Microsoft"
            path = System.getProperty("user.home") //$NON-NLS-1$
                + File.separator
                + "Library" //$NON-NLS-1$
                + File.separator
                + "Application Support" //$NON-NLS-1$
                + File.separator
                + vendorName;
        } else {
            // Consider all other operating systems a normal variant of
            // Unix. We lowercase the path components because that's closer to
            // convention.

            // "~/.microsoft"
            path = System.getProperty("user.home") + File.separator + "." + vendorName.toLowerCase(); //$NON-NLS-1$ //$NON-NLS-2$
        }

        path = path + File.separator + applicationName + File.separator + version;

        log.debug(MessageFormat.format(
            "Using path {0} for vendorName {1}, application {2}, and version {3}", //$NON-NLS-1$
            path,
            vendorName,
            applicationName,
            version));

        return new File(path);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PersistenceStore getChildStore(final String childName) {
        /*
         * This is overridden so the children are the same type as this class
         * (required by the PersistenceStore).
         */
        return new VersionedVendorFilesystemPersistenceStore(new File(getStoreFile(), childName));
    }
}
