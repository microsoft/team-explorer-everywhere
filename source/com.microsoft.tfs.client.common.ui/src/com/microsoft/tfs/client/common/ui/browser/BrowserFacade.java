// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.browser;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PlatformUI;

import com.microsoft.tfs.client.common.ui.framework.launcher.Launcher;
import com.microsoft.tfs.util.Check;

/**
 * The BrowserFacade class is used to reliably launch a browser (either external
 * or internal) across all versions of Eclipse that Team Explorer Everywhere
 * supports.
 *
 * Under Eclipse 3.1 and higher, BrowserFacade uses IWorkbenchBrowserSupport and
 * related classes (reflectively, so as to still compile for Eclipse 3.0). This
 * is the best solution, as it allows the user to specify browser launching
 * preferences and has the widest platform support.
 *
 * This mode should also work for the Explorer client, since it is built with
 * the latest stable version of Eclipse. Ensure that Explorer includes the
 * org.eclipse.ui.browser plugin, or else internal browser support will be
 * disabled.
 *
 * Under Eclipse 3.0, BrowserFacade falls back to a legacy mode. The legacy mode
 * will either host the SWT Browser control inside an editor, or will use the
 * com.microsoft.tfs.client.common.ui.shared.helper.Launcher class to launch an
 * external browser.
 *
 * Either way, the launch mode (internal vs external) can be specified, or can
 * be left to the user's preference (recommended).
 */
public class BrowserFacade {
    /**
     * Specifies the launch mode for a BrowserFacade call.
     */
    public static final class LaunchMode {
        /**
         * If possible, an internal editor-based browser will be launched. If
         * this is not possible, an external browser will be launched instead.
         * The user preference, if any, is ignored.
         */
        public static final LaunchMode INTERNAL = new LaunchMode();

        /**
         * An external browser will be launched. The user preference, if any,
         * will be ignored.
         */
        public static final LaunchMode EXTERNAL = new LaunchMode();

        /**
         * The user preference, if it exists, will be used to select internal vs
         * external browser launching. This is the recommended and default
         * launch mode.
         */
        public static final LaunchMode USER_PREFERENCE = new LaunchMode();

        private LaunchMode() {
        }
    }

    private static final Log log = LogFactory.getLog(BrowserFacade.class);
    private static final boolean useWorkbenchBrowserSupport =
        !Boolean.getBoolean("com.microsoft.tfs.ui.browser.legacy"); //$NON-NLS-1$

    /**
     * Launches a URL using the default launch mode.
     *
     * @param uri
     *        the URL to launch (must not be <code>null</code>)
     * @param title
     *        the title and tooltip to use for an internal browser
     */
    public static void launchURL(final URI uri, final String title) {
        launchURL(uri, title, title, title, null);
    }

    /**
     * Launches a URL. All configurable options can be specified.
     *
     * @param uri
     *        the URL to launch (must not be <code>null</code>)
     * @param title
     *        the title to use for an internal browser
     * @param tooltip
     *        the tooltip to use for an internal browser
     * @param browserId
     *        the ID to use for an internal browser
     * @param launchMode
     *        the launch mode
     */
    public static void launchURL(
        final URI uri,
        final String title,
        String tooltip,
        String browserId,
        LaunchMode launchMode) {
        Check.notNull(uri, "url"); //$NON-NLS-1$

        if (!URISchemeHelper.isOnTrustedUriWhiteList(uri)) {
            URISchemeHelper.showUnsafeSchemeError(uri);
            return;
        }

        if (launchMode == null) {
            launchMode = LaunchMode.USER_PREFERENCE;
        }

        if (browserId == null) {
            browserId = ""; //$NON-NLS-1$
        }

        // Tooltip can not be null in Eclipse 3.0
        if (tooltip == null) {
            tooltip = ""; //$NON-NLS-1$
        }

        final boolean workbenchBrowserMode;

        synchronized (BrowserFacade.class) {
            workbenchBrowserMode = useWorkbenchBrowserSupport;
        }

        if (workbenchBrowserMode) {
            try {
                launchWithWorkbenchBrowserSupport(uri, title, tooltip, browserId, launchMode);
                return;
            } catch (final Exception e) {
                log.warn("Problem launching web browser with workbench support (fallback will be tried)", e); //$NON-NLS-1$
            }
        }

        launchInFallbackMode(uri.toString(), title, tooltip, browserId, launchMode);
    }

    private static void launchInFallbackMode(
        final String url,
        final String title,
        final String tooltip,
        final String browserId,
        final LaunchMode launchMode) {
        /*
         * TODO: currently in fallback mode (Eclipse 3.0, or later versions with
         * browser configuration problems), we treat LaunchMode.USER_PREFERENCE
         * the same as LaunchMode.INTERNAL. It would be nice to create a
         * preference page that would only show on Eclipse 3.0 and allow the
         * user to actually choose external vs internal.
         */

        if (launchMode == LaunchMode.INTERNAL || launchMode == LaunchMode.USER_PREFERENCE) {
            if (BrowserEditor.isAvailable()) {
                BrowserEditor.openEditor(url, title, tooltip, browserId);
                return;
            }
        }

        Launcher.launch(url);
    }

    private static void launchWithWorkbenchBrowserSupport(
        final URI uri,
        final String title,
        final String tooltip,
        final String browserId,
        final LaunchMode launchMode)
            throws IllegalArgumentException,
                SecurityException,
                IllegalAccessException,
                InvocationTargetException,
                NoSuchMethodException {
        URL url;
        try {
            url = uri.toURL();
        } catch (final MalformedURLException e) {
            throw new RuntimeException(e);
        }

        /*
         * compute style int for IWorkbenchBrowserSupport.createBrowser
         */
        int style = 0;
        if (launchMode == LaunchMode.EXTERNAL) {
            style |= (1 << 7); // IWorkbenchBrowserSupport.AS_EXTERNAL
        } else if (launchMode == LaunchMode.INTERNAL) {
            style |= (1 << 5); // IWorkbenchBrowserSupport.AS_EDITOR
        }

        /*
         * Method: IWorkbench#getBrowserSupport
         */
        final Method getBrowserSupportMethod = IWorkbench.class.getDeclaredMethod("getBrowserSupport", new Class[] {}); //$NON-NLS-1$

        /*
         * Interface: IWorkbenchBrowserSupport
         */
        final Class iWorkbenchBrowserSupportInterface = getBrowserSupportMethod.getReturnType();

        /*
         * Object: workbench browser support object
         */
        final Object workbenchBrowserSupport =
            getBrowserSupportMethod.invoke(PlatformUI.getWorkbench(), (Object[]) null);

        /*
         * Method: IWorkbenchBrowserSupport#createBrowser(int, String, String,
         * String)
         */
        final Method createBrowserMethod = iWorkbenchBrowserSupportInterface.getDeclaredMethod(
            "createBrowser", //$NON-NLS-1$
            new Class[] {
                Integer.TYPE,
                String.class,
                String.class,
                String.class
        });

        /*
         * Interface: IWebBrowser
         */
        final Class iWebBrowserInterface = createBrowserMethod.getReturnType();

        /*
         * Object: web browser object
         */
        final Object webBrowser = createBrowserMethod.invoke(workbenchBrowserSupport, new Object[] {
            new Integer(style),
            browserId,
            title,
            tooltip
        });

        /*
         * Launch the browser: IWebBrowser#openURL
         */
        iWebBrowserInterface.getDeclaredMethod("openURL", new Class[] //$NON-NLS-1$
        {
            URL.class
        }).invoke(webBrowser, new Object[] {
            url
        });
    }
}
