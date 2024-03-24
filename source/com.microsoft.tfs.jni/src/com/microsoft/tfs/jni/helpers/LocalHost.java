// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.jni.helpers;

import java.net.InetAddress;
import java.text.MessageFormat;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.microsoft.tfs.jni.PlatformMiscUtils;
import com.microsoft.tfs.util.Platform;

/**
 * <p>
 * Contains static helper methods for discovering machine settings (host name,
 * etc.).
 * </p>
 *
 * @since TEE-SDK-10.1
 */
public abstract class LocalHost {
    /**
     * <b>Constant Duplication Warning</b>
     * <p>
     * This constant is also defined in
     * com.microsoft.tfs.core.clients.versioncontrol.VersionControlConstants
     * which this project does not depend on, so it is redefined here to avoid
     * moving all the constants around.
     * </p>
     */
    public final static int MAX_COMPUTER_NAME_SIZE = 31;

    private static final Log log = LogFactory.getLog(LocalHost.class);

    /**
     * The system property that, if set to a non-empty string, overrides all
     * other methods for determining the computer's short host name.
     */
    public static final String SHORT_NAME_OVERRIDE_PROPERTY = "computerName"; //$NON-NLS-1$

    /**
     * Where the cached computer name string goes.
     */
    private static String computerName;
    // JetBrains TFS plugin compatible name
    private static String computerNameJB;

    /**
     * <p>
     * Gets the short name of the current computer. This method uses multiple
     * techniques to determine the host name, allowing for a system property
     * override. The short name is cached forever: the name returned the first
     * time is returned for the life of the {@link LocalHost}'s classloader
     * regardless of actual host name changes.
     * <p>
     * The returned hostname is at most {@value #MAX_COMPUTER_NAME_SIZE}
     * characters (longer hostnames are truncated).
     * </p>
     * <p>
     * The {@link #SHORT_NAME_OVERRIDE_PROPERTY} may be used to define the
     * computer name exactly.
     * </p>
     *
     * @return a short identifier (name of this computer). Never
     *         <code>null</code> or empty.
     */
    public synchronized static String getShortName() {
        if (computerName == null) {
            String name = null;

            /*
             * The system property overrides all other settings.
             */
            name = getSystemPropertyShortName();

            /*
             * Native code (or the appropriate fallback from that layer) is
             * quite accurate. This will almost always work for all platforms.
             */
            if (name == null) {
                name = getNativeShortName();
            }

            /*
             * Some platforms expose an environment variable.
             */
            if (name == null) {
                name = getEnvironmentShortName();
            }

            /*
             * The last real technique is pure Java, which fails in certain
             * network configurations because it uses DNS.
             */
            if (name == null) {
                name = getPureJavaShortName();
            }

            /*
             * If we still don't have a host name, make up a cheesy one.
             */
            if (name == null || name.length() == 0) {
                name = getMadeUpShortName();
            }

            // Cache the name forever. Truncate if too long.
            computerName =
                name.substring(0, (name.length() > MAX_COMPUTER_NAME_SIZE) ? MAX_COMPUTER_NAME_SIZE : name.length());
        }

        log.info("Short name resolved to: " + computerNameJB); //$NON-NLS-1$
        return computerName;
    }

    /**
     * <p>
     * Gets the short name of the current computer compatible with JetBrains TFS
     * plugin.
     * <p>
     * The only difference with the traditional TEE method
     * {@link #getShortName()} is in the order of particular name detection
     * sub-method calls. This method prefers {@link #getPureJavaShortName()}.
     * </p>
     * <p>
     * The {@link #SHORT_NAME_OVERRIDE_PROPERTY} may be used to define the
     * computer name exactly.
     * </p>
     * 
     * <p>
     * We un-set the computerName variable, because we might need to recalculate
     * it since the system property {@link #SHORT_NAME_OVERRIDE_PROPERTY} might
     * change after call {@link #getShortNameJB()} in
     * {@link com.microsoft.tfs.client.clc.commands.Command#determineCachedWorkspace()}.
     * </p>
     *
     * @return a short identifier (name of this computer). Never
     *         <code>null</code> or empty.
     */
    public synchronized static String getShortNameJB() {

        computerName = null;

        if (computerNameJB == null) {
            String name = getPureJavaShortName();

            if (name != null) {
                // Cache the name forever. Truncate if too long.
                computerNameJB = name.substring(
                    0,
                    (name.length() > MAX_COMPUTER_NAME_SIZE) ? MAX_COMPUTER_NAME_SIZE : name.length());
                log.info("Short name resolved to: " + computerNameJB); //$NON-NLS-1$
            }
        }

        return computerNameJB;
    }

    /**
     * Gets the host name using environment variables. Many major versions of
     * Java do not support reading environment variables, and on those platforms
     * this method returns null, so it is not often useful. Also, Windows
     * usually sets COMPUTERNAME to the right value, but no variables are read
     * on Unix because there isn't a convention for exposing the host name with
     * an environment variable.
     *
     * @return the host name read from the environment variables, or null if
     *         none was found.
     */
    private static String getEnvironmentShortName() {
        /*
         * On Windows, the COMPUTERNAME environment variable is usually set to
         * the correct thing. Unix doesn't have a common environment variable
         * containing the host name.
         */
        if (Platform.isCurrentPlatform(Platform.WINDOWS)) {
            final String computerName = PlatformMiscUtils.getInstance().getEnvironmentVariable("COMPUTERNAME"); //$NON-NLS-1$

            if (computerName != null && computerName.length() > 0) {
                return computerName;
            }
        }

        return null;
    }

    /**
     * Gets the host name using native code (or appropriate fallback if native
     * code does not load for this platform).
     *
     * @return the host name determined in native code, or null if the host name
     *         could not be determined.
     */
    private static String getNativeShortName() {
        final String name = PlatformMiscUtils.getInstance().getComputerName();

        if (name != null && name.length() > 0) {
            return name;
        }

        return null;
    }

    /**
     * Gets the host name by checking the {@link #SHORT_NAME_OVERRIDE_PROPERTY}
     * system property's value.
     *
     * @return the host name read from the system property, or null if the
     *         property was not set or was empty.
     */
    private static String getSystemPropertyShortName() {
        final String name = System.getProperty(SHORT_NAME_OVERRIDE_PROPERTY);

        if (name != null && name.length() > 0) {
            return name;
        }

        return null;
    }

    /**
     * Only called when all techniques to get the host name fail. Simply makes
     * up a string based on the username.
     *
     * @return a made-up hostname including the username running Java and some
     *         other text. Never null.
     */
    private static String getMadeUpShortName() {
        String username = System.getProperty("user.name"); //$NON-NLS-1$

        /*
         * Scrub out non-alphanumeric characters.
         */
        if (username != null) {
            final StringBuffer newUsername = new StringBuffer();
            for (int i = 0; i < username.length(); i++) {
                final char c = username.charAt(i);

                if (Character.isLetterOrDigit(c)) {
                    newUsername.append(c);
                }
            }

            username = newUsername.toString();
        }

        if (username != null && username.length() != 0) {
            // Something like "JohnComputer".
            return username + "Computer"; //$NON-NLS-1$
        }

        log.warn(
            MessageFormat.format(
                "Could not make a hostname from the username '{0}' because it had no usable characters", //$NON-NLS-1$
                username));

        // Everything failed.
        return "TEEComputer"; //$NON-NLS-1$
    }

    /**
     * Gets the host name using only standard Java features. This method is
     * unreliable in certain network configurations because it relies on Java's
     * DNS features to resolve the local interface, and DNS may not always work.
     * Other methods should be preferred to this one.
     *
     * @return the short host name (without domain names), or null if none could
     *         be determined.
     */
    private static String getPureJavaShortName() {
        /*
         * I have a feeling this may fail for some computers with unusual
         * network configurations (lots of interfaces; no DNS; etc.). It may
         * need to be scrapped and re-written.
         */
        String name = null;

        try {
            final InetAddress localMachine = InetAddress.getLocalHost();
            name = localMachine.getHostName();
        } catch (final java.net.UnknownHostException e) {
            log.warn("Pure Java host name lookup failed", e); //$NON-NLS-1$

            /*
             * This happens in some network configurations on Unix, and possibly
             * Windows.
             */
            return null;
        }

        /*
         * GCJ (and possibly others) returns a full hostname
         * ("fun.whatever.domain") instead of just "fun", so we use only the
         * first part.
         */
        final int dotIndex = name.indexOf('.');
        if (dotIndex != -1) {
            name = name.substring(0, dotIndex);
        }

        return name;
    }
}
