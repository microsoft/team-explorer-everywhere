// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.tasks;

import java.net.URI;
import java.text.MessageFormat;

import org.eclipse.swt.widgets.Shell;

import com.microsoft.alm.teamfoundation.build.webapi.BuildDefinitionTemplate;
import com.microsoft.alm.teamfoundation.build.webapi.DefinitionReference;
import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.browser.BrowserFacade;
import com.microsoft.tfs.client.common.ui.browser.BrowserFacade.LaunchMode;
import com.microsoft.tfs.core.TFSTeamProjectCollection;
import com.microsoft.tfs.core.util.TSWAHyperlinkBuilder;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.StringUtil;

public abstract class BuildDefinitionVNextTask extends BaseTask {
    protected final static String VIEW_BUILDS_ACTION = "viewbuilds"; //$NON-NLS-1$
    protected final static String QUEUE_BUILD_ACTION = "queuebuild"; //$NON-NLS-1$
    protected final static String NEW_DEFINITION_ACTION = "new"; //$NON-NLS-1$
    protected final static String OPEN_DEFINITION_ACTION = "open"; //$NON-NLS-1$

    final DefinitionReference definition;
    final String projectName;
    final TFSTeamProjectCollection connection;

    public BuildDefinitionVNextTask(
        final Shell shell,
        final TFSTeamProjectCollection connection,
        final String projectName) {
        super(shell);

        Check.notNull(connection, "connection"); //$NON-NLS-1$

        this.connection = connection;
        this.definition = null;
        this.projectName = projectName;
    }

    public BuildDefinitionVNextTask(
        final Shell shell,
        final TFSTeamProjectCollection connection,
        final DefinitionReference definition) {
        super(shell);

        Check.notNull(connection, "connection"); //$NON-NLS-1$
        Check.notNull(definition, "definition"); //$NON-NLS-1$

        this.connection = connection;
        this.definition = definition;
        this.projectName = definition.getProject().getName();
    }

    protected URI getActionUri(final String action) {
        final TSWAHyperlinkBuilder builder = new TSWAHyperlinkBuilder(connection, false);
        if (definition != null) {
            return builder.getBuildDefinitionVNextURI(projectName, action, definition.getId());
        } else {
            return builder.getBuildDefinitionVNextURI(projectName, action);
        }
    }

    protected URI getActionUri(final String action, final BuildDefinitionTemplate template) {
        final TSWAHyperlinkBuilder builder = new TSWAHyperlinkBuilder(connection, false);
        if (template != null) {
            return builder.getBuildDefinitionVNextURI(projectName, action, template.getId());
        } else {
            return builder.getBuildDefinitionVNextURI(projectName, action);
        }
    }

    protected void openBrowser(final URI actionUri) {
        final String windowTitle =
            definition == null ? Messages.getString("BuildDefinitionVNextTask.NewBuildDefinitionTitle") //$NON-NLS-1$
                : MessageFormat.format(
                    Messages.getString("BuildDefinitionVNextTask.BuildDefinitionTitleFormat"), //$NON-NLS-1$
                    definition.getName());

        BrowserFacade.launchURL(actionUri, windowTitle, StringUtil.EMPTY, actionUri.toString(), LaunchMode.EXTERNAL);
    }
}
