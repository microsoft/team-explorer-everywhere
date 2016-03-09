// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.webaccessintegration.browserfunctions;

import java.text.MessageFormat;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.BrowserFunction;

import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.webaccessintegration.editors.WebAccessWorkItemEditor;
import com.microsoft.tfs.core.artifact.ArtifactID;
import com.microsoft.tfs.core.artifact.MalformedURIException;
import com.microsoft.tfs.util.Check;

public class WorkItemSavedBrowserFunction extends BrowserFunction {
    private static final Log log = LogFactory.getLog(WorkItemSavedBrowserFunction.class);

    private final WebAccessWorkItemEditor editor;

    public WorkItemSavedBrowserFunction(
        final Browser browser,
        final String name,
        final WebAccessWorkItemEditor editor) {
        super(browser, name);
        this.editor = editor;
    }

    @Override
    public Object function(final Object[] arguments) {
        Check.notNull(arguments, "arguments"); //$NON-NLS-1$
        Check.isTrue(arguments.length == 1, "arguments.length == 1"); //$NON-NLS-1$
        Check.isTrue(arguments[0] instanceof String, "arguments[0] instanceof String"); //$NON-NLS-1$

        final String artifactURI = (String) arguments[0];
        int workItemID;

        try {
            final ArtifactID artifactID = new ArtifactID(artifactURI);
            workItemID = Integer.parseInt(artifactID.getToolSpecificID());
        } catch (final MalformedURIException e) {
            final String format = Messages.getString("WorkItemSavedBrowserFunction.MalformedURIFormat"); //$NON-NLS-1$
            final String message = MessageFormat.format(format, artifactURI);

            log.error(message, e);
            return null;
        }

        editor.onWorkItemSaved(workItemID);
        return null;
    }
}
