// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.config;

import java.text.MessageFormat;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.microsoft.tfs.core.clients.versioncontrol.PropertyConstants;
import com.microsoft.tfs.core.clients.versioncontrol.Workstation;
import com.microsoft.tfs.core.config.persistence.PersistenceStoreProvider;
import com.microsoft.tfs.jni.PlatformMiscUtils;
import com.microsoft.tfs.util.StringUtil;

/**
 * <p>
 * A collection of all the environment variables that control configuration in
 * classes in the com.microsoft.tfs.core.config package.
 * </p>
 *
 * @since TEE-SDK-10.1
 * @threadsafety immutable
 */
public abstract class EnvironmentVariables {
    /** Log object for this class. */
    private static final Log logger = LogFactory.getLog(EnvironmentVariables.class);

    /**
     * When set to any value untrusted SSL certificates will be accepted.
     * <p>
     * Setting this variable reduces security and should only be used if the
     * user understands the risks in disabling server certificate validation.
     */
    public static final String ACCEPT_UNTRUSTED_CERTIFICATES = "TF_ACCEPT_UNTRUSTED_CERTIFICATES"; //$NON-NLS-1$

    /**
     * When set to any value the old federated authentication dialog will be
     * used.
     * <p>
     * The Visual Studio 2013 client introduces a streamlined authentication
     * dialog when connecting to Team Foundation Service (TFS.VisualStudio.com).
     * Due to changes by the Windows team and our move to the new experience we
     * lost the ability to federate @microsoft.com identities.
     * <p>
     * NOTES:
     * <li>- This is an undocumented workaround for TFS only and is not fully
     * supported.
     * <li>- When OrgID is supported (ETA: post DEV12 RTM \ Winter 2013) you
     * will need to delete this environment variable. If you don’t, you may get
     * errors.
     */
    public static final String USE_LEGACY_MSA = "TF_USE_LEGACY_MSA"; //$NON-NLS-1$

    /**
     * When set to any "false-ish" value the new OAuth authentication dialog
     * will not be used.
     */
    public static final String USE_OAUTH_LIBRARY = "TF_USE_OAUTH_LIBRARY"; //$NON-NLS-1$

    /**
     * When set to an integer positive value defines the maximum size of a file
     * portion during chunked file upload. The value may be specified either -
     * in bytes (e.g 12345678), or - in kilobytes (e.g. 1234K or 1234k), or - in
     * megabytes (e.g. 12M or 12m)
     * <p>
     * Setting this variable to a value less than 2 disables chunked upload.
     * Files are uploaded with a single HTTP request.
     * <p>
     */
    public static final String UPLOAD_CHUNK_SIZE = "TF_UPLOAD_CHUNK_SIZE"; //$NON-NLS-1$

    /**
     * When set to an integer positive value defines the size of a I/O buffer
     * used to read a file during upload. The value may be specified either - in
     * bytes (e.g 12345678), or - in kilobytes (e.g. 1234K or 1234k), or - in
     * megabytes (e.g. 12M or 12m)
     */
    public static final String UPLOAD_BUFFER_SIZE = "TF_UPLOAD_BUFFER_SIZE"; //$NON-NLS-1$

    /**
     * When set to an integer positive value defines the maximum number of
     * attempts to upload a file before an error is reported to the user.
     */
    public static final String MAX_FILE_RETRY_ATTEMPTS = "TF_MAX_FILE_RETRY_ATTEMPTS"; //$NON-NLS-1$

    /**
     * When set to an integer positive value defines the maximum number of
     * attempts to upload a chunk of a file before we attempt to retry the
     * entire file.
     */
    public static final String MAX_CHUNK_RETRY_ATTEMPTS = "TF_MAX_CHUNK_RETRY_ATTEMPTS"; //$NON-NLS-1$

    /**
     * When set to any value, the automatic pend of property
     * {@link PropertyConstants#EXECUTABLE_KEY} for files with the Unix execute
     * bit is skipped when local workspaces are scanned.
     * <p>
     * Has no affect on non-Unix platforms.
     */
    public static final String DISABLE_DETECT_EXECUTABLE_PROP = "TF_DISABLE_DETECT_EXECUTABLE_PROP"; //$NON-NLS-1$

    /**
     * When set to any value, files with the property
     * {@link PropertyConstants#EXECUTABLE_KEY} will not have the Unix execute
     * bit set when they are written to working folders during operations like
     * get, pend, undo, or unshelve.
     * <p>
     * This variable does <em>not</em> prevent the application of the execute
     * bit when specified in a .tpattributes file.
     * <p>
     * Has no affect on non-Unix platforms.
     */
    public static final String DISABLE_APPLY_EXECUTABLE_PROP = "TF_DISABLE_APPLY_EXECUTABLE_PROP"; //$NON-NLS-1$

    /**
     * When set to a file path, this will be used as the global tpattributes
     * file.
     */
    public static final String GLOBAL_TPATTRIBUTES = "TF_GLOBAL_TPATTRIBUTES"; //$NON-NLS-1$

    /**
     * When set to any value, the automatic pend of property
     * {@link PropertyConstants#SYMBOLIC_KEY} for symbolic links is skipped when
     * local workspaces are scanned.
     * <p>
     * Has no affect on non-Unix platforms.
     */
    public static final String DISABLE_SYMBOLIC_LINK_PROP = "TF_DISABLE_SYMBOLIC_LINK_PROP"; //$NON-NLS-1$

    /**
     * When set to any value, the value of this variable is used as the URL to
     * the Team Foundation Proxy (not the general HTTP proxy).
     * <p>
     * Visual Studio uses the same environment variable for this purpose.
     */
    public static final String TF_PROXY = "TFSPROXY"; //$NON-NLS-1$

    /**
     * Overrides the standard cache file storage directory the
     * {@link Workstation} class uses. Does not affect where non-
     * {@link Workstation} cache files are placed (see
     * {@link PersistenceStoreProvider}).
     * <p>
     * The {@link Workstation} cache directory contains the version control
     * workspace cache file.
     * <p>
     * Visual Studio uses the same environment variable for this purpose.
     */
    public static final String WORKSTATION_CACHE_DIRECTORY = "TFSVC_CACHE_DIR"; //$NON-NLS-1$

    /**
     * Overrides the standard cache file storage directory the
     * {@link Workstation} class uses. Does not affect where non-
     * {@link Workstation} cache files are placed (see
     * {@link PersistenceStoreProvider}).
     * <p>
     * The {@link Workstation} configuration directory contains the version
     * control exclusion patterns file.
     * <p>
     * Visual Studio uses the same environment variable for this purpose.
     */
    public static final String WORKSTATION_CONFIGURATION_DIRECTORY = "TFSVC_CONFIG_DIR"; //$NON-NLS-1$

    /**
     * Overrides the standard calculation used to compute the local workspace
     * metadata root directory (
     * {@link Workstation#getOfflineMetadataFileRoot()}).
     * <p>
     * Visual Studio uses the same environment variable for this purpose.
     */
    public static final String OFFLINE_METADATA_ROOT_DIRECTORY = "TFS_OFFLINE_METADATA_ROOT"; //$NON-NLS-1$

    /**
     * Overrides the standard persistent store directory to store user profiles
     * including configuration, momento, etc.
     */
    public static final String TEE_PROFILE_DIRECTORY = "TEE_PROFILE_DIRECTORY"; //$NON-NLS-1$

    /**
     * Controls ThrowOnProjectRenamed feature. If is set to any value server
     * does not throw ReconcileBlockedByProjectRenameException.
     */
    public static final String DD_SUITES_PROJECT_RENAME_UNPATCHED_CLIENT =
        "TF_DD_SUITES_PROJECT_RENAME_UNPATCHED_CLIENT"; //$NON-NLS-1$

    /*
     * Home environment variable used in git to hold the path of the
     * repositories drectory
     */
    public static final String HOME = "HOME"; //$NON-NLS-1$

    public static final String USER_PROFILE = "USERPROFILE"; //$NON-NLS-1$

    public static int getInt(final String variableName, final int defaultValue) {
        final String value = getString(variableName);
        if (StringUtil.isNullOrEmpty(value)) {
            return defaultValue;
        } else {
            try {
                final int number = StringUtil.toInt(value);
                return number;
            } catch (final NumberFormatException e) {
                final String message =
                    MessageFormat.format("Incorrect value of the environment variable {0} = {1}", variableName, value); //$NON-NLS-1$
                logger.error(message, e);

                return defaultValue;
            }
        }
    }

    public static boolean getBoolean(final String variableName, final boolean defaultValue) {
        final String value = getString(variableName);

        if (StringUtil.isNullOrEmpty(value)) {
            return defaultValue;
        } else {
            return !value.equalsIgnoreCase("FALSE") //$NON-NLS-1$
                && !value.equalsIgnoreCase("NO") //$NON-NLS-1$
                && !value.equalsIgnoreCase("N"); //$NON-NLS-1$
        }
    }

    public static String getString(final String variableName) {
        return PlatformMiscUtils.getInstance().getEnvironmentVariable(variableName);
    }

    public static boolean isDefined(final String variableName) {
        return !StringUtil.isNullOrEmpty(getString(variableName));
    }
}
