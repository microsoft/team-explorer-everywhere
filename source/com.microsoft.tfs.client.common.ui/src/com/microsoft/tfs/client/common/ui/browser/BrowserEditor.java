// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.browser;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Preferences;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.EditorPart;

import com.microsoft.tfs.client.common.ui.TFSCommonUIClientPlugin;
import com.microsoft.tfs.client.common.ui.prefs.UIPreferenceConstants;

public class BrowserEditor extends EditorPart {
    public static final String ID = "com.microsoft.tfs.client.common.ui.browser.browsereditor"; //$NON-NLS-1$
    private static Boolean browserAvailable = null;

    public static boolean isAvailable() {
        /*
         * for a similar implementation, see:
         * org.eclipse.ui.internal.browser.WebBrowserUtil
         * #canUseInternalWebBrowser in the org.eclipse.ui.browser plugin
         * (Eclipse 3.1 and up)
         */

        synchronized (BrowserEditor.class) {
            if (browserAvailable == null) {
                Shell shell = null;
                try {
                    shell = new Shell(PlatformUI.getWorkbench().getDisplay());
                    new Browser(shell, getBrowserStyle());
                    browserAvailable = Boolean.TRUE;
                } catch (final Throwable t) {
                    browserAvailable = Boolean.FALSE;
                } finally {
                    if (shell != null) {
                        shell.dispose();
                    }
                }
            }

            return browserAvailable.booleanValue();
        }
    }

    public static void openEditor(final String url, final String title, final String tooltip, final String browserId) {
        openEditor(new BrowserEditorInput(url, title, tooltip, browserId));
    }

    private static void openEditor(final BrowserEditorInput input) {
        final IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
        try {
            page.openEditor(input, ID);
        } catch (final PartInitException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void doSave(final IProgressMonitor monitor) {
    }

    @Override
    public void doSaveAs() {
    }

    @Override
    public void init(final IEditorSite site, final IEditorInput input) throws PartInitException {
        if (!(input instanceof BrowserEditorInput)) {
            throw new PartInitException("Invalid Input: Must be BrowserEditorInput"); //$NON-NLS-1$
        }
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
    public void createPartControl(final Composite parent) {
        final BrowserEditorInput input = (BrowserEditorInput) getEditorInput();

        setPartName(input.getName());
        setTitleToolTip(input.getToolTipText());

        parent.setLayout(new FillLayout());

        final Browser browser = new Browser(parent, getBrowserStyle());
        browser.setUrl(input.getURL());
    }

    @Override
    public void setFocus() {
    }

    private static int getBrowserStyle() {
        final Preferences preferences = TFSCommonUIClientPlugin.getDefault().getPluginPreferences();
        return preferences.getInt(UIPreferenceConstants.EMBEDDED_WEB_BROWSER_TYPE);
    }
}
