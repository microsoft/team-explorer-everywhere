// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.config;

import java.text.MessageFormat;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.microsoft.tfs.core.Messages;
import com.microsoft.tfs.jni.RegistryException;
import com.microsoft.tfs.jni.RegistryKey;
import com.microsoft.tfs.jni.RootKey;
import com.microsoft.tfs.util.Platform;

/**
 * <p>
 * A collection methods for using the Windows registry to load and store core
 * configuration settings.
 * </p>
 *
 * @threadsafety thread-safe
 */
public abstract class RegistryUtils {
    private static final Log log = LogFactory.getLog(RegistryUtils.class);

    /**
     * The path of the root registry key for settings for this version of Visual
     * Studio.
     */
    public static final String VISUAL_STUDIO_KEY_ROOT_PATH = "Software\\Microsoft\\VisualStudio\\12.0"; //$NON-NLS-1$

    /**
     * Gets the current user's registry key for settings for this version of
     * Visual Studio (in {@link RootKey#HKEY_CURRENT_USER}).
     * <p>
     * Throws {@link RuntimeException} if called from a non-Windows platform.
     *
     * @return the {@link RegistryKey} or <code>null</code> if it did not exist
     *         and could not be created
     */
    public static RegistryKey openOrCreateRootUserRegistryKey() {
        if (!Platform.isCurrentPlatform(Platform.WINDOWS)) {
            throw new RuntimeException(Messages.getString("RegistryUtils.RegistryFeaturesNotAvailable")); //$NON-NLS-1$
        }

        RegistryKey key = new RegistryKey(RootKey.HKEY_CURRENT_USER, VISUAL_STUDIO_KEY_ROOT_PATH);

        if (!key.exists()) {
            try {
                key.create();
            } catch (final RegistryException e) {
                log.error(
                    MessageFormat.format(Messages.getString("RegistryUtils.CouldNotCreateRegistryKeyFormat"), key), //$NON-NLS-1$
                    e);

                key = null;
            }
        }

        return key;
    }
}
