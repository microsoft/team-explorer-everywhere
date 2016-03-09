// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.webaccessintegration.editors;

import java.io.IOException;

import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.widgets.Composite;

import com.microsoft.tfs.client.common.server.TFSServer;
import com.microsoft.tfs.client.common.ui.webaccessintegration.javascript.JavascriptResourceLoader;

public class WebAccessBuildReportEditor extends WebAccessBaseEditor {
    private static final String ID =
        "com.microsoft.tfs.client.common.ui.webaccessintegration.editors.WebAccessBuildReportEditor"; //$NON-NLS-1$

    public static void openEditor(final TFSServer server, final String url, final String buildNumber) {
        openEditor(new WebAccessBuildReportEditorInput(server, url, buildNumber), ID);
    }

    @Override
    public void createPartControl(final Composite parent) {
        super.createPartControl(parent);

        final WebAccessBuildReportEditorInput input = (WebAccessBuildReportEditorInput) getEditorInput();
        setPartName(input.getBuildNumber());
        browser.setUrl(input.getURL());
    }

    @Override
    protected String loadJavascript() throws IOException {
        return JavascriptResourceLoader.loadJavascriptFile("BuildReportEditor.js"); //$NON-NLS-1$
    }

    @Override
    protected void createBrowserFunctions(final Browser browser) {
        new com.microsoft.tfs.client.common.ui.webaccessintegration.browserfunctions.BuildDeletedBrowserFunction(
            browser,
            "Host_DeleteBuild", //$NON-NLS-1$
            this);

        new com.microsoft.tfs.client.common.ui.webaccessintegration.browserfunctions.BuildStoppedBrowserFunction(
            browser,
            "Host_BuildStopped"); //$NON-NLS-1$

        new com.microsoft.tfs.client.common.ui.webaccessintegration.browserfunctions.BuildPropertyChangedBrowserFunction(
            browser,
            "Host_BuildPropertyChanged"); //$NON-NLS-1$

        new com.microsoft.tfs.client.common.ui.webaccessintegration.browserfunctions.OpenArtifactLinkBrowserFunction(
            browser,
            "Host_OpenArtifactLink"); //$NON-NLS-1$

        new com.microsoft.tfs.client.common.ui.webaccessintegration.browserfunctions.OpenWorkItemBrowserFunction(
            browser,
            "Host_OpenWorkItemLink", //$NON-NLS-1$
            ((WebAccessBuildReportEditorInput) getEditorInput()).getServer());

        new com.microsoft.tfs.client.common.ui.webaccessintegration.browserfunctions.OpenURLBrowserFunction(
            browser,
            "Host_OpenURL", //$NON-NLS-1$
            ((WebAccessBuildReportEditorInput) getEditorInput()).getServer());
    }
}
