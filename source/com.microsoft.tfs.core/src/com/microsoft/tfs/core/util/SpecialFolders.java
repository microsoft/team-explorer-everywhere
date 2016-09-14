// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.util;

import java.io.File;

import com.microsoft.tfs.jni.PlatformMiscUtils;
import com.microsoft.tfs.util.Platform;

/**
 * Gets the path to some of the Windows Shell special folders. A path is
 * returned only if the folder exists on disk (the return value is
 * <code>null</code> if the folder does not exist on disk). This is consistent
 * with the .NET Environment.GetFolderPath(SpecialFolder) method behavior.
 *
 * @threadsafety immutable
 */
public class SpecialFolders {
    /*
     * See http://msdn.microsoft.com/en-us/library/dd378457%28v=vs.85%29.aspx
     * for a table of equivalence between CSIDL_*, FOLDERID_*, and environment
     * variables.
     */

    /**
     * Gets the application data folder if it exists on disk (<code>null</code>
     * if it does not).
     * <p>
     * Represents the file system directory that serves as a common repository
     * for application-specific data for the current, roaming user. A roaming
     * user works on more than one computer on a network. A roaming user's
     * profile is kept on a server on the network and is loaded onto a system
     * when the user logs on.
     * <p>
     * CSIDL_APPDATA, FOLDERID_RoamingAppData
     *
     * @return the full path to the roaming application data folder,
     *         <code>null</code> if it does not exist on disk and
     *         <code>null</code> on non-Windows platforms
     */
    public static String getApplicationDataPath() {
        if (Platform.isCurrentPlatform(Platform.WINDOWS)) {
            final String path = PlatformMiscUtils.getInstance().getEnvironmentVariable("APPDATA"); //$NON-NLS-1$

            if (path != null && path.length() > 0 && new File(path).exists()) {
                return path;
            }
        }
        return null;
    }

    /**
     * Gets the common application data folder if it exists on disk (
     * <code>null</code> if it does not).
     * <p>
     * Represents the file system directory that serves as a common repository
     * for application-specific data that is used by all users.
     *
     * @return the full path to the common application data folder,
     *         <code>null</code> if it does not exist on disk and
     *         <code>null</code> on non-Windows platforms
     */
    public static String getCommonApplicationDataPath() {
        if (Platform.isCurrentPlatform(Platform.WINDOWS)) {
            final String path = PlatformMiscUtils.getInstance().getEnvironmentVariable("ALLUSERSPROFILE"); //$NON-NLS-1$

            if (path != null && path.length() > 0 && new File(path).exists()) {
                return path;
            }
        }
        return null;
    }

    /**
     * Gets the local application data folder if it exists on disk (
     * <code>null</code> if it does not).
     * <p>
     * Represents the file system directory that serves as a common repository
     * for application specific data that is used by the current, non-roaming
     * user.
     *
     * @return the full path to the local application data folder,
     *         <code>null</code> if it does not exist on disk and
     *         <code>null</code> on non-Windows platforms
     */
    public static String getLocalApplicationDataPath() {
        if (Platform.isCurrentPlatform(Platform.WINDOWS)) {
            final String path = PlatformMiscUtils.getInstance().getEnvironmentVariable("LOCALAPPDATA"); //$NON-NLS-1$

            if (path != null && path.length() > 0 && new File(path).exists()) {
                return path;
            }
        }
        return null;
    }

    /**
     * Gets the ueser profile folder if it exists on disk ( <code>null</code> if
     * it does not).
     * <p>
     * Represents the file system directory that serves as a root for by the
     * current user data.
     *
     * @return the full path to the user profile folder, <code>null</code> if it
     *         does not exist on disk and <code>null</code> on non-Windows
     *         platforms
     */
    public static String getUserProfilePath() {
        if (Platform.isCurrentPlatform(Platform.WINDOWS)) {
            final String path = PlatformMiscUtils.getInstance().getEnvironmentVariable("USERPROFILE"); //$NON-NLS-1$

            if (path != null && path.length() > 0 && new File(path).exists()) {
                return path;
            }
        }
        return null;
    }
}
