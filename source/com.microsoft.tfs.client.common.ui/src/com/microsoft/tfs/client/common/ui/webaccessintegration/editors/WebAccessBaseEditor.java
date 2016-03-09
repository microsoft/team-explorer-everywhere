// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.webaccessintegration.editors;

import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Preferences;
import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTException;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.ProgressEvent;
import org.eclipse.swt.browser.ProgressListener;
import org.eclipse.swt.events.MenuDetectEvent;
import org.eclipse.swt.events.MenuDetectListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.EditorPart;

import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.TFSCommonUIClientPlugin;
import com.microsoft.tfs.client.common.ui.controls.generic.FullFeaturedBrowser;
import com.microsoft.tfs.client.common.ui.framework.helper.UIHelpers;
import com.microsoft.tfs.client.common.ui.prefs.UIPreferenceConstants;
import com.microsoft.tfs.core.TFSTeamProjectCollection;
import com.microsoft.tfs.core.clients.versioncontrol.WebServiceLevel;
import com.microsoft.tfs.util.Check;

public abstract class WebAccessBaseEditor extends EditorPart {
    /**
     * The logger.
     */
    private static final Log log = LogFactory.getLog(WebAccessBaseEditor.class);

    /**
     * True if the class BrowserFunction is available (since Eclipse 3.5).
     */
    private static Boolean hasBrowserFunctionSupport = null;

    protected FullFeaturedBrowser browser;

    protected boolean javascriptLoaded = false;
    protected boolean serverSideShim = false;
    private String javascript;

    /**
     * Test current java runtime environment and the current TFS connection are
     * capable of supporting a Web Access hosted editor. The SWT BrowserFunction
     * class must be available (since 3.5) and the TFS connection must be one of
     * the final versions of TFS 2012.
     *
     *
     * @param collection
     *        The current TFS collection. Must not be null.
     * @return
     */
    public static boolean isSupported(final TFSTeamProjectCollection collection) {
        Check.notNull(collection, "collection"); //$NON-NLS-1$

        if (hasBrowserFunctionSupport == null) {
            try {
                Class.forName("org.eclipse.swt.browser.BrowserFunction"); //$NON-NLS-1$
                hasBrowserFunctionSupport = true;
            } catch (final Exception e) {
                hasBrowserFunctionSupport = false;
            }
        }

        if (hasBrowserFunctionSupport == false) {
            return false;
        }

        final WebServiceLevel level = collection.getVersionControlClient().getServiceLevel();
        return level.getValue() >= WebServiceLevel.TFS_2012_2.getValue();
    }

    @Override
    public void doSave(final IProgressMonitor monitor) {
    }

    @Override
    public void doSaveAs() {
    }

    @Override
    public void init(final IEditorSite site, final IEditorInput input) throws PartInitException {
        setSite(site);
        setInput(input);
    }

    @Override
    public boolean isDirty() {
        return false;
    }

    @Override
    public boolean isSaveAsAllowed() {
        return false;
    }

    @Override
    public void setFocus() {
        browser.setFocus();
    }

    @Override
    public void createPartControl(final Composite parent) {
        final Preferences preferences = TFSCommonUIClientPlugin.getDefault().getPluginPreferences();
        final int browserStyle = preferences.getInt(UIPreferenceConstants.EMBEDDED_WEB_BROWSER_TYPE);

        browser = new FullFeaturedBrowser(parent, SWT.NONE, browserStyle);

        // Create the callback handlers.
        createBrowserFunctions(browser.getBrowser());

        browser.getBrowser().addMenuDetectListener(new MenuDetectListener() {
            @Override
            public void menuDetected(final MenuDetectEvent e) {
                e.doit = false;
            }
        });

        browser.addProgressListener(new ProgressListener() {

            @Override
            public void changed(final ProgressEvent event) {
            }

            @Override
            public void completed(final ProgressEvent event) {
                final Object ret = browser.evaluate("return typeof(TFS) == 'undefined'"); //$NON-NLS-1$
                final boolean tfsUndefined = ((Boolean) ret).booleanValue();
                if (tfsUndefined) {
                    log.trace("TFS undefined for " + browser.getBrowser().getUrl()); //$NON-NLS-1$
                    return;
                }

                if (pageHasServerSideShim()) {
                    javascriptLoaded = true;
                    serverSideShim = true;
                    return;
                }

                log.trace("Navigated to " + browser.getBrowser().getUrl()); //$NON-NLS-1$
                if (javascript == null) {
                    try {
                        javascript = loadJavascript();
                    } catch (final IOException e) {
                        log.error("Failed to load javascript resource.", e); //$NON-NLS-1$
                    }
                }

                if (javascript != null) {
                    if (browser.execute(javascript)) {
                        javascriptLoaded = true;
                    } else {
                        // Generate some useful information in the log.
                        log.warn("Browser.execute failed to load javascript"); //$NON-NLS-1$
                        log.warn(javascript);
                        try {
                            browser.evaluate(javascript);
                            log.warn("successfully loaded javascript"); //$NON-NLS-1$
                        } catch (final SWTException e) {
                            log.warn(e.getLocalizedMessage());
                        }
                    }
                }

                if (!javascriptLoaded) {
                    // don't let the setting of error text trigger another
                    // completed event.
                    browser.removeProgressListener(this);
                    browser.setText(Messages.getString("WebAccessBaseEditor.JavascriptFailedToLoad")); //$NON-NLS-1$
                }
            }
        });
    }

    protected abstract String loadJavascript() throws IOException;

    protected abstract void createBrowserFunctions(final Browser browser);

    public static void openEditor(final IEditorInput input, final String editorID) {
        final IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
        try {
            page.openEditor(input, editorID);
        } catch (final PartInitException e) {
            throw new RuntimeException(e);
        }
    }

    public static void closeEditor(final EditorPart editor) {
        final IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
        page.closeEditor(editor, false);
    }

    public void onDirtyStateChanged() {
        UIHelpers.runOnUIThread(getSite().getWorkbenchWindow().getWorkbench().getDisplay(), true, new Runnable() {
            @Override
            public void run() {
                firePropertyChange(IEditorPart.PROP_DIRTY);
            }
        });
    }

    private boolean pageHasServerSideShim() {
        try {
            final Object ret2 =
                browser.evaluate("return document.getElementsByTagName('Body')[0].getAttribute('class')"); //$NON-NLS-1$

            final String classNames = (String) ret2;
            return classNames.indexOf("server-shim") != -1; //$NON-NLS-1$
        } catch (final Exception e) {
            return false;
        }
    }
}
