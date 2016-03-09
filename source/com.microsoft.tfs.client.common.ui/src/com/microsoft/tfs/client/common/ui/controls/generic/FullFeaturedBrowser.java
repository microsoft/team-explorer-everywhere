// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.controls.generic;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.swt.SWTException;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.CloseWindowListener;
import org.eclipse.swt.browser.LocationListener;
import org.eclipse.swt.browser.OpenWindowListener;
import org.eclipse.swt.browser.ProgressListener;
import org.eclipse.swt.browser.StatusTextListener;
import org.eclipse.swt.browser.TitleListener;
import org.eclipse.swt.browser.VisibilityWindowListener;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;

/**
 * <p>
 * Provides a {@link Browser}-like class which compiles on SWT/Eclipse 3.2 but
 * provides access to the newer methods in {@link Browser} (using reflection).
 * These new methods will throw {@link RuntimeException} if the underlying
 * {@link Browser} does not support them, but no newer types are leaked through
 * this class (so it compiles against older target platforms).
 * </p>
 * <p>
 * See the "since" Javadoc notes on each method for which SWT/Eclipse version is
 * required for the method to succeed (if no note, 3.2 is required).
 * </p>
 * <p>
 * See {@link CompatibleBrowser}'s Javadoc for the differences between
 * {@link FullFeaturedBrowser} and {@link CompatibleBrowser}.
 * </p>
 *
 * @threadsafety unknown
 * @see CompatibleBrowser
 * @see Browser
 */
public class FullFeaturedBrowser extends Composite {
    private static final Log log = LogFactory.getLog(FullFeaturedBrowser.class);

    private final Browser browser;

    /**
     * Browser style bits can be set separately because they can differ wildly
     * from other SWT style bits in how they work (border looks different when
     * IE paints them vs. SWT, browser style can be set, etc.).
     *
     * @see Browser#Browser(Composite, int)
     */
    public FullFeaturedBrowser(final Composite parent, final int controlStyle, final int browserStyle) {
        /*
         * Don't pass styles up to the parent; let the browser handle the style
         * bits its own way.
         */
        super(parent, controlStyle);

        setLayout(new FillLayout());
        browser = new Browser(this, browserStyle);
    }

    /**
     * @return the {@link Browser} this class wraps (never <code>null</code>)
     */
    public Browser getBrowser() {
        return browser;
    }

    /*
     * Generated delegate methods.
     */

    public void addCloseWindowListener(final CloseWindowListener listener) {
        browser.addCloseWindowListener(listener);
    }

    public void addLocationListener(final LocationListener listener) {
        browser.addLocationListener(listener);
    }

    public void addOpenWindowListener(final OpenWindowListener listener) {
        browser.addOpenWindowListener(listener);
    }

    public void addProgressListener(final ProgressListener listener) {
        browser.addProgressListener(listener);
    }

    public void addStatusTextListener(final StatusTextListener listener) {
        browser.addStatusTextListener(listener);
    }

    public void addTitleListener(final TitleListener listener) {
        browser.addTitleListener(listener);
    }

    public void addVisibilityWindowListener(final VisibilityWindowListener listener) {
        browser.addVisibilityWindowListener(listener);
    }

    public boolean back() {
        return browser.back();
    }

    public boolean execute(final String script) {
        log.trace(script);
        return browser.execute(script);
    }

    public boolean forward() {
        return browser.forward();
    }

    @Override
    public int getStyle() {
        return browser.getStyle();
    }

    public String getURL() {
        return browser.getUrl();
    }

    public boolean isBackEnabled() {
        return browser.isBackEnabled();
    }

    @Override
    public boolean isFocusControl() {
        return browser.isFocusControl();
    }

    public boolean isForwardEnabled() {
        return browser.isForwardEnabled();
    }

    public void refresh() {
        browser.refresh();
    }

    public void removeCloseWindowListener(final CloseWindowListener listener) {
        browser.removeCloseWindowListener(listener);
    }

    public void removeLocationListener(final LocationListener listener) {
        browser.removeLocationListener(listener);
    }

    public void removeOpenWindowListener(final OpenWindowListener listener) {
        browser.removeOpenWindowListener(listener);
    }

    public void removeProgressListener(final ProgressListener listener) {
        browser.removeProgressListener(listener);
    }

    public void removeStatusTextListener(final StatusTextListener listener) {
        browser.removeStatusTextListener(listener);
    }

    public void removeTitleListener(final TitleListener listener) {
        browser.removeTitleListener(listener);
    }

    public void removeVisibilityWindowListener(final VisibilityWindowListener listener) {
        browser.removeVisibilityWindowListener(listener);
    }

    public boolean setText(final String html) {
        return browser.setText(html);
    }

    public boolean setUrl(final String url) {
        return browser.setUrl(url);
    }

    public void stop() {
        browser.stop();
    }

    /*
     * Reflection methods.
     */

    /**
     * @since 3.3
     */
    public Object getWebBrowser() {
        try {
            final Method method = Browser.class.getMethod("getWebBrowser", (Class<?>[]) null); //$NON-NLS-1$

            return method.invoke(browser);
        } catch (final Exception e) {
            throw handleReflectionException(e);
        }
    }

    /**
     * @since 3.4
     */
    public String getText() {
        try {
            final Method method = Browser.class.getMethod("getText", (Class<?>[]) null); //$NON-NLS-1$

            return (String) method.invoke(browser);
        } catch (final Exception e) {
            throw handleReflectionException(e);
        }
    }

    /**
     * @since 3.5
     */
    public boolean getJavascriptEnabled() {
        try {
            final Method method = Browser.class.getMethod("getJavascriptEnabled", (Class<?>[]) null); //$NON-NLS-1$

            return ((Boolean) method.invoke(browser)).booleanValue();
        } catch (final Exception e) {
            throw handleReflectionException(e);
        }
    }

    /**
     * @since 3.5
     */
    public void setJavascriptEnabled(final boolean enabled) {
        try {
            final Method method = Browser.class.getMethod("setJavascriptEnabled", boolean.class); //$NON-NLS-1$

            method.invoke(browser, enabled);
        } catch (final Exception e) {
            throw handleReflectionException(e);
        }
    }

    /**
     * @param listener
     *        the org.eclipse.swt.browser.AuthenticationListener to add
     *
     * @since 3.5
     */
    public void addAuthenticationListener(final Object listener) {
        try {
            final Class<?> authenticationListenerClass =
                Class.forName("org.eclipse.swt.browser.AuthenticationListener"); //$NON-NLS-1$
            final Method method = Browser.class.getMethod("addAuthenticationListener", authenticationListenerClass); //$NON-NLS-1$

            method.invoke(browser, listener);
        } catch (final Exception e) {
            throw handleReflectionException(e);
        }
    }

    /**
     * @param listener
     *        the org.eclipse.swt.browser.AuthenticationListener to remove
     *
     * @since 3.5
     */
    public void removeAuthenticationListener(final Object listener) {
        try {
            final Class<?> authenticationListenerClass =
                Class.forName("org.eclipse.swt.browser.AuthenticationListener"); //$NON-NLS-1$
            final Method method = Browser.class.getMethod("removeAuthenticationListener", authenticationListenerClass); //$NON-NLS-1$

            method.invoke(browser, listener);
        } catch (final Exception e) {
            throw handleReflectionException(e);
        }
    }

    /**
     * @since 3.5
     */
    public Object evaluate(final String script) throws SWTException {
        try {
            log.trace(script);

            final Method method = Browser.class.getMethod("evaluate", String.class); //$NON-NLS-1$

            return method.invoke(browser, script);
        } catch (final Exception e) {
            throw handleReflectionException(e);
        }
    }

    /**
     * @since 3.5
     */
    public String getBrowserType() {
        try {
            final Method method = Browser.class.getMethod("getBrowserType", (Class<?>[]) null); //$NON-NLS-1$

            return (String) method.invoke(browser, (Object[]) null);
        } catch (final Exception e) {
            throw handleReflectionException(e);
        }
    }

    /**
     * @since 3.6
     */
    public boolean close() {
        try {
            final Method method = Browser.class.getMethod("close", (Class<?>[]) null); //$NON-NLS-1$

            return ((Boolean) method.invoke(browser, (Object[]) null)).booleanValue();
        } catch (final Exception e) {
            throw handleReflectionException(e);
        }
    }

    /**
     * Note that {@link #setText(String)} is available since 3.0. This method
     * simply adds the trusted flag.
     *
     * @since 3.6
     */
    public boolean setText(final String html, final boolean trusted) {
        try {
            final Method method = Browser.class.getMethod("setText", String.class, boolean.class); //$NON-NLS-1$

            return ((Boolean) method.invoke(browser, html, trusted)).booleanValue();
        } catch (final Exception e) {
            throw handleReflectionException(e);
        }
    }

    /**
     * Note that {@link #setUrl(String)} is available since 3.0. This method
     * simply adds POST and header support.
     *
     * @since 3.6
     */
    public boolean setUrl(final String url, final String postData, final String[] headers) {
        try {
            final Method method = Browser.class.getMethod("setUrl", String.class, String.class, String[].class); //$NON-NLS-1$

            return ((Boolean) method.invoke(browser, url, postData, headers)).booleanValue();
        } catch (final Exception e) {
            throw handleReflectionException(e);
        }
    }

    private RuntimeException handleReflectionException(final Exception e) {
        log.warn("Error using reflection", e); //$NON-NLS-1$

        if (e instanceof RuntimeException) {
            return (RuntimeException) e;
        } else if (e instanceof InvocationTargetException) {
            if (e.getCause() instanceof RuntimeException) {
                return (RuntimeException) e.getCause();
            } else {
                return new RuntimeException(e.getCause());
            }
        }

        return new RuntimeException(e);
    }
}
