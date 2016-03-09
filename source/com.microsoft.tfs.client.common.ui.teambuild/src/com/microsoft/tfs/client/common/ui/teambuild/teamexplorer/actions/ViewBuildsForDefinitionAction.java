// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.teambuild.teamexplorer.actions;

import java.text.MessageFormat;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.jface.action.IAction;

import com.microsoft.tfs.client.common.ui.teambuild.teamexplorer.helpers.BuildHelpers;

public class ViewBuildsForDefinitionAction extends TeamExplorerSingleBuildDefinitionAction {
    private static final Log log = LogFactory.getLog(ViewBuildsForDefinitionAction.class);

    @Override
    public void doRun(final IAction action) {
        log.info(MessageFormat.format(
            "Opening build definitions for {0} for project {1}", //$NON-NLS-1$
            selectedDefinition.getName(),
            selectedDefinition.getTeamProject()));

        BuildHelpers.viewTodaysBuildsForDefinition(selectedDefinition);
    }
}
