// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.controls.generic;

import java.awt.BorderLayout;
import java.awt.Frame;
import java.awt.Panel;
import java.io.IOException;
import java.lang.reflect.Method;
import java.text.MessageFormat;

import javax.swing.JEditorPane;
import javax.swing.JScrollPane;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.awt.SWT_AWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.LocationListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;

import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.controls.generic.compatibility.link.CompatibilityLinkControl;
import com.microsoft.tfs.client.common.ui.controls.generic.compatibility.link.CompatibilityLinkFactory;
import com.microsoft.tfs.client.common.ui.framework.WindowSystem;
import com.microsoft.tfs.jni.PlatformMiscUtils;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.Platform;

/**
 * <p>
 * A composite that acts like an SWT {@link Browser}, but only supports a few
 * methods. If SWT's {@link Browser} is not supported on the running platform, a
 * {@link JEditorPane} is used instead.
 * </p>
 * <p>
 * The major differences between {@link CompatibleBrowser} and
 * {@link FullFeaturedBrowser}:
 * </p>
 * <ul>
 * <li>{@link CompatibleBrowser} will fall back to the extremely limited
 * {@link JEditorPane} if {@link Browser} can't load;
 * {@link FullFeaturedBrowser} will just error if {@link Browser} can't load
 * </li>
 * <li>{@link CompatibleBrowser} offers very few features;
 * {@link FullFeaturedBrowser} offers lots of features</li>
 * <li>{@link CompatibleBrowser} might use AWT/Swing (which works better on some
 * platforms than others); {@link FullFeaturedBrowser} never uses AWT/Swing</li>
 * </ul>
 */
public class CompatibleBrowser extends Composite {
    private final static Log log = LogFactory.getLog(CompatibleBrowser.class);

    private final static String AWT_TOOLKIT_ENV_VAR_NAME = "AWT_TOOLKIT"; //$NON-NLS-1$
    private final static String XTOOLKIT_ENV_VAR_VALUE = "XToolkit"; //$NON-NLS-1$

    private final static String AWT_TOOLKIT_SYS_PROP_NAME = "awt.toolkit"; //$NON-NLS-1$
    private final static String XTOOLKIT_SYS_PROP_VALUE = "sun.awt.X11.XToolkit"; //$NON-NLS-1$

    /*
     * Only one of these will be initialized to non-null values depending on
     * whether SWT's Browser control works.
     */
    private final JEditorPane editorPane;
    private final Browser browser;
    private static final Object nativeTestLock = new Object();
    private static boolean testedForNativeBrowser = false;
    private static boolean nativeBrowserAvailable = false;

    public CompatibleBrowser(final Composite parent, final int style) {
        super(parent, style);

        Check.notNull(parent, "parent"); //$NON-NLS-1$

        final GridLayout gridLayout = new GridLayout(1, true);
        gridLayout.marginHeight = 0;
        gridLayout.marginWidth = 0;

        setLayout(gridLayout);

        if (isNativeBrowserAvailable()) {
            log.info(MessageFormat.format("{0} using SWT Browser", CompatibleBrowser.class.getName())); //$NON-NLS-1$

            final GridData gd = new GridData();
            gd.grabExcessHorizontalSpace = true;
            gd.horizontalAlignment = SWT.FILL;
            gd.grabExcessVerticalSpace = true;
            gd.verticalAlignment = SWT.FILL;

            browser = new Browser(this, style);
            browser.setLayoutData(gd);

            editorPane = null;
        } else {
            log.info(MessageFormat.format("{0} using JEditorPane", CompatibleBrowser.class.getName())); //$NON-NLS-1$

            browser = null;

            JEditorPane tempEditorPane = null;
            CompatibilityLinkControl tempErrorLabel = null;

            /*
             * Embedded AWT widgets must be in a Composite with SWT.EMBEDDED
             * set, so create one.
             */
            final Composite embeddableComposite = new Composite(this, SWT.EMBEDDED);

            final GridData compositeGridData = new GridData();
            compositeGridData.grabExcessHorizontalSpace = true;
            compositeGridData.horizontalAlignment = SWT.FILL;
            compositeGridData.grabExcessVerticalSpace = true;
            compositeGridData.verticalAlignment = SWT.FILL;

            embeddableComposite.setLayoutData(compositeGridData);

            /*
             * We have to skip trying AWT entirely some places.
             */
            boolean loadedAWTBrowser = false;
            if (CompatibleBrowser.isAWTDangerousHere() == false) {
                try {
                    /*
                     * Create a Frame in the SWT Composite as the top-level
                     * element.
                     */
                    final Frame browserFrame = SWT_AWT.new_Frame(embeddableComposite);

                    /*
                     * Create a panel with a simple BorderLayout to hold
                     * contents.
                     */
                    final Panel panel = new Panel(new BorderLayout());
                    browserFrame.add(panel);

                    /*
                     * Create an JEditorPane with an HTML document.
                     */
                    final String pageContents = "<html><body></body></html>"; //$NON-NLS-1$
                    tempEditorPane = new JEditorPane("text/html", pageContents); //$NON-NLS-1$
                    tempEditorPane.setEditable(false);

                    /*
                     * Put the HTML viewer in a scroll pane and parent the
                     * scroll pane in the panel.
                     */
                    final JScrollPane scrollPane = new JScrollPane(tempEditorPane);
                    panel.add(scrollPane);

                    loadedAWTBrowser = true;
                } catch (final Throwable t) {
                    log.warn("Error embedding AWT frame for JEditorPane", t); //$NON-NLS-1$
                }
            }

            if (loadedAWTBrowser == false) {
                /*
                 * We don't need the embeddable composite because AWT embedding
                 * failed, and we can't put normal (error label) things in it,
                 * so hide it.
                 */
                compositeGridData.widthHint = 0;
                compositeGridData.heightHint = 0;

                tempErrorLabel = CompatibilityLinkFactory.createLink(this, SWT.NONE);

                tempErrorLabel.setText(
                    MessageFormat.format(
                        Messages.getString("CompatibleBrowser.CouldNotLoadSWTBrowserFormat"), //$NON-NLS-1$
                        AWT_TOOLKIT_ENV_VAR_NAME,
                        XTOOLKIT_ENV_VAR_VALUE));

                final GridData labelGridData = new GridData();
                labelGridData.grabExcessHorizontalSpace = true;
                labelGridData.horizontalAlignment = SWT.FILL;
                labelGridData.grabExcessVerticalSpace = true;
                labelGridData.verticalAlignment = SWT.FILL;

                tempErrorLabel.getControl().setLayoutData(labelGridData);
            }

            editorPane = tempEditorPane;
        }
    }

    /**
     * Call this method to test whether calling
     * {@link SWT_AWT#new_Frame(Composite)} would crash the JVM on this system.
     *
     * Background:
     *
     * Calling {@link SWT_AWT#new_Frame(Composite)} on some Unix platforms when
     * the AWT toolkit is MToolkit can cause a JVM crash (AIX 5.2 with Eclipse
     * 3.3-3.5 and IBM Java 1.5.0). I dug into this and simply loading
     * java.awt.Toolkit that causes the crash, so we can't even interrogate the
     * default Toolkit instance to see what kind it is! Instead we check
     * environment variables and system properties.
     *
     * See http://java.sun.com/j2se/1.5.0/docs/guide/awt/1.5/xawt.html.
     *
     * @return true if {@link SWT_AWT#new_Frame(Composite)} would crash the JVM
     *         (as it does on some Unixes when MToolkit is the active AWT
     *         toolkit)
     */
    private static boolean isAWTDangerousHere() {
        /*
         * Unfortunately we can't test any system property or environment
         * variable or static class information directly to see if MToolkit will
         * be the one chosen if the Toolkit class is used, because it's just
         * hard coded behavior in that class. There is an environment variable
         * and system property the user can set to enable XToolkit, so we test
         * for the existence of those as proof it's safe to continue.
         *
         * This isn't optimal, because there may be other non-dangerous toolkits
         * (headless mode?) that we should have gone ahead with. Probably not an
         * issue for a web browser control.
         */
        if (Platform.isCurrentPlatform(Platform.AIX)) {
            final String envVal = PlatformMiscUtils.getInstance().getEnvironmentVariable(AWT_TOOLKIT_ENV_VAR_NAME);
            final String sysPropVal = System.getProperty(AWT_TOOLKIT_SYS_PROP_NAME);

            boolean xToolkitActive = false;

            /*
             * Environment variable overrides system property according to Sun's
             * docs.
             */
            if (envVal != null) {
                xToolkitActive = envVal.equals(XTOOLKIT_ENV_VAR_VALUE);
            } else if (sysPropVal != null) {
                xToolkitActive = sysPropVal.equals(XTOOLKIT_SYS_PROP_VALUE);
            }

            return xToolkitActive == false;
        }

        /*
         * Safe to try and use AWT.
         */
        return false;
    }

    /**
     * @see Browser#setText(String)
     * @see JEditorPane#setText(String)
     */
    public void setText(final String html) {
        if (browser != null) {
            browser.setText(html);
        } else if (editorPane != null) {
            editorPane.setText(html);
        } else {
            // Do nothing for the error label case.
        }
    }

    /**
     * @throws IOException
     * @see Browser#setUrl(String)
     * @see JEditorPane#setURL(String)
     */
    public void setURL(final String url) throws IOException {
        if (browser != null && !browser.isDisposed()) {
            browser.setUrl(url);
        } else if (editorPane != null) {
            editorPane.setPage(url);
        }
    }

    /**
     * @see Browser#setJavascriptEnabled(boolean)
     */
    public void setJavascriptEnabled(final boolean enabled) {
        if (browser != null && !browser.isDisposed()) {
            try {
                // Method setJavascriptEnabled is Eclipse 3.5.
                final Method m = browser.getClass().getMethod("setJavascriptEnabled", new Class[] //$NON-NLS-1$
                {
                    boolean.class
                });
                m.invoke(browser, new Object[] {
                    Boolean.valueOf(enabled)
                });
            } catch (final Exception e) {
                // fallback is to do nothing.
                log.warn("Unable to modify javascript enablement", e); //$NON-NLS-1$
            }
        }
    }

    /**
     * @see Browser#stop(String)
     */
    public void stop() {
        if (browser != null) {
            browser.stop();
        }
    }

    /**
     * @see Browser#addLocationListener(LocationListener)
     */
    public void addLocationListener(final LocationListener listener) {
        if (browser != null) {
            browser.addLocationListener(listener);
        }
    }

    /**
     * @see Browser#removeLocationListener(LocationListener)
     */
    public void removeLocationListener(final LocationListener listener) {
        if (browser != null) {
            browser.removeLocationListener(listener);
        }
    }

    /**
     * Checks if the SWT Browser control is available.
     *
     * @return true if the WorkItemHistoryControl is available on the current
     *         platform. Due to its dependency on the SWT Browser control, the
     *         WorkItemHistoryControl is not always available.
     */
    public static boolean isNativeBrowserAvailable() {
        synchronized (nativeTestLock) {
            if (testedForNativeBrowser == false) {
                final Shell shell = new Shell();

                try {
                    /*
                     * OS X's SWT native browser for Carbon is full of bugs
                     * (https://bugs.eclipse.org/bugs/show_bug.cgi?id=230035) so
                     * we fall back to the AWT browser which works better
                     * (though with very few features) whenever possible.
                     *
                     * However, if we started in AWT headless mode (which we
                     * shouldn't, but some dumb IDE -- FlexBuilder? -- does)
                     * then we should fall back to the SWT browser because the
                     * AWT browser obviously can't be initialized in headless.
                     * See bug 2465.
                     */
                    if (WindowSystem.isCurrentWindowSystem(WindowSystem.CARBON)) {
                        try {
                            final String headless = System.getProperty("java.awt.headless"); //$NON-NLS-1$

                            if ("true".equalsIgnoreCase(headless)) //$NON-NLS-1$
                            {
                                /* Headless, AWT browser is not available */
                                log.info("Not using AWT browser (java.awt.headless=true), falling back to SWT browser"); //$NON-NLS-1$
                                nativeBrowserAvailable = true;
                            } else {
                                /*
                                 * Try to create an AWT frame to ensure that we
                                 * can actually do so. Eclipse 3.3 on Mac OS has
                                 * some problems doing this for unknown reasons.
                                 */
                                final Composite dummyComposite = new Composite(shell, SWT.EMBEDDED);
                                final Frame dummyAwtFrame = SWT_AWT.new_Frame(dummyComposite);

                                if (dummyAwtFrame == null) {
                                    /* AWT browser not available, use native */
                                    log.info("Could not instantiate AWT browser, falling back to SWT browser"); //$NON-NLS-1$
                                    nativeBrowserAvailable = true;
                                } else {
                                    /* AWT browser is available, prefer it */
                                    log.info(
                                        "AWT browser is available, preferring it.  Set java.awt.headless=true to use SWT browser."); //$NON-NLS-1$
                                    nativeBrowserAvailable = false;
                                }
                            }
                        } catch (final Throwable t) {
                            log.info("Could not instantiate AWT browser, falling back to SWT browser", t); //$NON-NLS-1$
                            nativeBrowserAvailable = true;
                        }
                    } else {
                        try {
                            final Browser browser = new Browser(shell, SWT.NONE);
                            browser.dispose();

                            log.info("SWT Browser found during isNativeBrowserAvailable() check"); //$NON-NLS-1$
                            nativeBrowserAvailable = true;
                        } catch (final Throwable t) {
                            log.info("SWT Browser not usable during isNativeBrowserAvailable() check", t); //$NON-NLS-1$
                            nativeBrowserAvailable = false;
                        }
                    }
                } finally {
                    shell.dispose();
                    testedForNativeBrowser = true;
                }
            }

            return nativeBrowserAvailable;
        }
    }
}