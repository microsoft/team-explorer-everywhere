// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.git.utils;

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.runtime.Platform;
import org.osgi.framework.Bundle;

import com.microsoft.tfs.util.Check;

public abstract class GitHelpers {
    private static final Log log = LogFactory.getLog(GitHelpers.class);

    private static final String EGIT_CORE_BUNDLE_ID = "org.eclipse.egit.core"; //$NON-NLS-1$
    private static final String EGIT_UI_BUNDLE_ID = "org.eclipse.egit.ui"; //$NON-NLS-1$
    private static final String JGIT_BUNDLE_ID = "org.eclipse.jgit"; //$NON-NLS-1$

    private static final Map<String, Bundle> bundleMap = new HashMap<String, Bundle>();

    public static boolean isEGitInstalled(final boolean activateEGit) {
        final Bundle egit_core = Platform.getBundle(EGIT_CORE_BUNDLE_ID);

        if (egit_core == null || egit_core.getVersion().getMajor() < 2) {
            /*
             * EGit plug-in is not installed or its version is too old.
             */
            return false;
        } else if (activateEGit) {
            /*
             * EGit plug-in of required version is installed. We're going to use
             * its functionality right now, so load it first.
             */
            return activateBundle(JGIT_BUNDLE_ID) && activateBundle(egit_core) && activateEGitUI();
        } else {
            /*
             * EGit plug-in of required version is installed. We do not need its
             * functionality right now. E.g. we check to display/hide our
             * functionality that depends on the plug-in.
             */
            return true;
        }
    }

    public static boolean activateEGitUI() {
        final Bundle egit_ui = Platform.getBundle(EGIT_UI_BUNDLE_ID);
        return activateBundle(egit_ui);
    }

    public static boolean activateBundle(final String bundleID) {
        final Bundle bundle = Platform.getBundle(bundleID);
        return activateBundle(bundle);
    }

    public static boolean activateBundle(final Bundle bundle) {
        if (bundle == null) {
            return false;
        }

        if ((bundle.getState() & Bundle.ACTIVE) == 0) {
            try {
                bundle.start(Bundle.START_TRANSIENT);
                return true;
            } catch (final Exception e) {
                log.error("Error activating " + bundle.getSymbolicName(), e); //$NON-NLS-1$
            }
        }

        return (bundle.getState() & Bundle.ACTIVE) != 0;
    }

    public static Object getInstance(
        final String pluginName,
        final String className,
        final Class<?>[] constructorParameterTypes,
        final Object[] constructorParameterValues) {
        Check.notNullOrEmpty(pluginName, "pluginName"); //$NON-NLS-1$
        Check.notNullOrEmpty(className, "className"); //$NON-NLS-1$
        Check.notNull(constructorParameterTypes, "constructorParameterTypes"); //$NON-NLS-1$
        Check.notNull(constructorParameterTypes, "constructorParameterTypes"); //$NON-NLS-1$

        final Class<?> operationClass = getClass(pluginName, className);
        if (operationClass == null) {
            return null;
        }

        final Constructor<?> constructor;
        try {
            constructor = operationClass.getDeclaredConstructor(constructorParameterTypes);
            constructor.setAccessible(true);
        } catch (final Exception e) {
            log.error("Searching for the " + operationClass.getName() + " constructor", e); //$NON-NLS-1$ //$NON-NLS-2$
            return null;
        }

        final Object operationInstance;
        try {
            operationInstance = constructor.newInstance(constructorParameterValues);
        } catch (final Exception e) {
            log.error("Creating the " + operationClass.getName() + " object", e); //$NON-NLS-1$ //$NON-NLS-2$
            return null;
        }

        return operationInstance;
    }

    public static Class<?> getClass(final String pluginName, final String className) {
        Check.notNullOrEmpty(pluginName, "pluginName"); //$NON-NLS-1$
        Check.notNullOrEmpty(className, "className"); //$NON-NLS-1$

        final Bundle plugin = getPlugin(pluginName);

        if (plugin == null) {
            return null;
        }

        final Class<?> operationClass;
        try {
            operationClass = plugin.loadClass(className);
        } catch (final ClassNotFoundException e) {
            log.error("Loading the " + className + " class", e); //$NON-NLS-1$ //$NON-NLS-2$
            return null;
        }

        return operationClass;
    }

    public static Bundle getPlugin(final String pluginName) {
        if (bundleMap.containsKey(pluginName)) {
            return bundleMap.get(pluginName);
        }

        Bundle bundle = Platform.getBundle(pluginName);

        if (bundle == null) {
            log.error(pluginName + " is not installed"); //$NON-NLS-1$
        } else if ((bundle.getState() & Bundle.ACTIVE) == 0) {
            try {
                bundle.start(Bundle.START_TRANSIENT);
            } catch (final Exception e) {
                log.error("Activating " + pluginName, e); //$NON-NLS-1$
            }

            if ((bundle.getState() & Bundle.ACTIVE) == 0) {
                bundle = null;
            }
        }

        bundleMap.put(pluginName, bundle);
        return bundle;
    }
}
