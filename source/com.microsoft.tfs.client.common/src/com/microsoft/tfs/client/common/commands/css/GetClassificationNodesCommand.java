// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.commands.css;

import java.text.MessageFormat;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import com.microsoft.tfs.client.common.Messages;
import com.microsoft.tfs.client.common.TFSCommonClientPlugin;
import com.microsoft.tfs.client.common.framework.command.ExtendedStatus;
import com.microsoft.tfs.core.clients.commonstructure.CSSNode;
import com.microsoft.tfs.core.clients.commonstructure.CSSStructureType;
import com.microsoft.tfs.core.clients.commonstructure.CommonStructureClient;
import com.microsoft.tfs.core.clients.commonstructure.NodeInfo;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.LocaleUtil;

public class GetClassificationNodesCommand extends CSSNodeCommand {
    private final CommonStructureClient css;
    private final String projectUri;

    private CSSNode areas;
    private CSSNode iterations;

    public GetClassificationNodesCommand(final CommonStructureClient css, final String projectUri) {
        super();

        Check.notNull(css, "css"); //$NON-NLS-1$
        Check.notNullOrEmpty(projectUri, "projectUri"); //$NON-NLS-1$

        this.css = css;
        this.projectUri = projectUri;

        setConnection(css.getConnection());
    }

    @Override
    public String getName() {
        return (Messages.getString("GetClassificationNodesCommand.CommandText")); //$NON-NLS-1$
    }

    @Override
    public String getErrorDescription() {
        return (Messages.getString("GetClassificationNodesCommand.ErrorText")); //$NON-NLS-1$
    }

    @Override
    public String getLoggingDescription() {
        return (Messages.getString("GetClassificationNodesCommand.CommandText", LocaleUtil.ROOT)); //$NON-NLS-1$
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * com.microsoft.tfs.client.common.shared.command.Command#doRun(org.eclipse
     * .core.runtime. IProgressMonitor)
     */
    @Override
    protected IStatus doRun(final IProgressMonitor progressMonitor) throws Exception {
        progressMonitor.beginTask(
            Messages.getString("GetClassificationNodesCommand.ProgressMonitorText"), //$NON-NLS-1$
            IProgressMonitor.UNKNOWN);

        final NodeInfo[] nodeInfoArray = css.listStructures(projectUri);
        if (nodeInfoArray == null || nodeInfoArray.length < 2) {
            final String messageFormat = Messages.getString("GetClassificationNodesCommand.StatusErrorTextFormat"); //$NON-NLS-1$
            final String message = MessageFormat.format(messageFormat, projectUri);

            return new ExtendedStatus(
                new Status(IStatus.ERROR, TFSCommonClientPlugin.PLUGIN_ID, 0, message, null),
                ExtendedStatus.SHOW_MESSAGE_IN_DIALOG | ExtendedStatus.SHOW_MESSAGE_IN_CONSOLE);
        }

        for (int i = 0; i < nodeInfoArray.length; i++) {
            final CSSNode node = css.getCSSNodes(nodeInfoArray[i].getURI(), true);
            if (CSSStructureType.PROJECT_LIFECYCLE.equals(node.getStructureType())) {
                iterations = node;
            } else if (CSSStructureType.PROJECT_MODEL_HIERARCHY.equals(node.getStructureType())) {
                areas = node;
            }
        }
        return Status.OK_STATUS;
    }

    public CSSNode getAreas() {
        return areas;
    }

    public CSSNode getIterations() {
        return iterations;
    }

}
