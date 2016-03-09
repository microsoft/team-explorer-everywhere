// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.webaccessintegration.editors;

import java.io.IOException;
import java.net.URI;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.swt.SWTException;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IEditorPart;

import com.microsoft.tfs.client.common.ui.helpers.WorkItemEditorHelper;
import com.microsoft.tfs.client.common.ui.webaccessintegration.javascript.JavascriptResourceLoader;
import com.microsoft.tfs.client.common.ui.wit.form.WorkItemEditorInput;

public class WebAccessWorkItemEditor extends WebAccessBaseEditor implements IEditorPart {
    private static final Log log = LogFactory.getLog(WebAccessWorkItemEditor.class);

    @Override
    public boolean isDirty() {
        if (!javascriptLoaded) {
            return false;
        }

        try {
            final Object ret = browser.evaluate(getJavascriptForIsModified());
            return ((Boolean) ret).booleanValue();
        } catch (final SWTException e) {
            log.error("Call to IsModified failed", e); //$NON-NLS-1$
            return false;
        }
    }

    @Override
    public void doSave(final IProgressMonitor monitor) {
        try {
            browser.evaluate(getJavascriptForSave());

            final WorkItemEditorInput input = (WorkItemEditorInput) getEditorInput();
            setPartName(input.getName());
        } catch (final SWTException e) {
            log.error("Call to DoSave failed", e); //$NON-NLS-1$
        }
    }

    @Override
    public void onDirtyStateChanged() {
        final WebAccessWorkItemEditorInput input = (WebAccessWorkItemEditorInput) getEditorInput();

        // Check the scenario where a new work item is saved by clicking the
        // SAVE button inside the web access work item. There is no hook for the
        // embedded save button and we won't get a callback that allows us to
        // update the input to the saved work item ID and update the document
        // title. We do get an event from web access showing the dirty state
        // change to false. So, when we know the dirty state changed from true
        // to false, we can check to see if we initially had a new work item (id
        // == 0) and probe the web access document for it's current work item
        // ID. If it's non-zero, we can assume this was a save and update the
        // EditorInput and document title.

        if (input.getWorkItemID() == 0 && isDirty() == false) {
            // Get the work item ID from the web access page.
            final Object ret = browser.evaluate(getJavascriptForGetWorkItemID());
            final int workItemID = ((Double) ret).intValue();

            if (workItemID != 0) {
                onWorkItemSaved(workItemID);
            }
        }

        super.onDirtyStateChanged();
    }

    public void onWorkItemSaved(final int workItemID) {
        final WebAccessWorkItemEditorInput input = (WebAccessWorkItemEditorInput) getEditorInput();
        input.updateWorkItemID(workItemID);
        setPartName(input.getName());
    }

    @Override
    public void createPartControl(final Composite parent) {
        super.createPartControl(parent);

        final WorkItemEditorInput input = (WorkItemEditorInput) getEditorInput();
        setPartName(input.getName());

        final URI url = WorkItemEditorHelper.workItemToWebAccessURI(
            input.getServer(),
            input.getWorkItem(),
            input.getDocumentNumber(),
            true);

        browser.setUrl(url.toString());
    }

    @Override
    protected String loadJavascript() throws IOException {
        return JavascriptResourceLoader.loadJavascriptFile("WorkItemEditor.js"); //$NON-NLS-1$
    }

    @Override
    protected void createBrowserFunctions(final Browser browser) {
        new com.microsoft.tfs.client.common.ui.webaccessintegration.browserfunctions.WorkItemDirtyChangedBrowserFunction(
            browser,
            "Host_WorkItemDirtyChanged", //$NON-NLS-1$
            this);

        new com.microsoft.tfs.client.common.ui.webaccessintegration.browserfunctions.OpenArtifactLinkBrowserFunction(
            browser,
            "Host_OpenArtifactLink"); //$NON-NLS-1$

        new com.microsoft.tfs.client.common.ui.webaccessintegration.browserfunctions.OpenWorkItemBrowserFunction(
            browser,
            "Host_OpenWorkItemLink", //$NON-NLS-1$
            ((WorkItemEditorInput) getEditorInput()).getServer());

        new com.microsoft.tfs.client.common.ui.webaccessintegration.browserfunctions.OpenURLBrowserFunction(
            browser,
            "Host_OpenURL", //$NON-NLS-1$
            ((WorkItemEditorInput) getEditorInput()).getServer());

        new com.microsoft.tfs.client.common.ui.webaccessintegration.browserfunctions.WorkItemSavedBrowserFunction(
            browser,
            "Host_WorkItemSaved", //$NON-NLS-1$
            this);

        new com.microsoft.tfs.client.common.ui.webaccessintegration.browserfunctions.WorkItemSaveFailedBrowserFunction(
            browser,
            "Host_WorkItemSaveFailed"); //$NON-NLS-1$

        new com.microsoft.tfs.client.common.ui.webaccessintegration.browserfunctions.DiscardNewWorkItemBrowserFunction(
            browser,
            "Host_DiscardNewWorkItem", //$NON-NLS-1$
            this);
    }

    private String getJavascriptForSave() {
        return serverSideShim ? "TFS.UI.Unified.Shim.WorkItem.save();" : "Host_DoSave()"; //$NON-NLS-1$ //$NON-NLS-2$
    }

    private String getJavascriptForIsModified() {
        return serverSideShim ? "return TFS.UI.Unified.Shim.WorkItem.isModified();" : "return Host_IsModified();"; //$NON-NLS-1$ //$NON-NLS-2$
    }

    private String getJavascriptForGetWorkItemID() {
        return serverSideShim ? "return TFS.UI.Unified.Shim.WorkItem.getWorkItemID()" : "return Host_GetWorkItemID()"; //$NON-NLS-1$ //$NON-NLS-2$
    }
}
