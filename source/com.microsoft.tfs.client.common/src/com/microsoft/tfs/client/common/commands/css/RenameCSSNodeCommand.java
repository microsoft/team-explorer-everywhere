// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.commands.css;

import java.text.MessageFormat;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import com.microsoft.tfs.client.common.Messages;
import com.microsoft.tfs.core.clients.commonstructure.CSSNode;
import com.microsoft.tfs.core.clients.commonstructure.CommonStructureClient;
import com.microsoft.tfs.util.Check;

public class RenameCSSNodeCommand extends CSSNodeCommand {
    private final CSSNode node;
    private final String newName;
    private final CommonStructureClient css;

    public RenameCSSNodeCommand(final CommonStructureClient css, final CSSNode node, final String newName) {
        super();

        Check.notNull(css, "css"); //$NON-NLS-1$
        Check.notNull(node, "node"); //$NON-NLS-1$
        Check.notNullOrEmpty(newName, "newName"); //$NON-NLS-1$

        this.css = css;
        this.node = node;
        this.newName = newName;

        setConnection(css.getConnection());
    }

    @Override
    public String getName() {
        return Messages.getString("RenameCssNodeCommand.CommandText"); //$NON-NLS-1$
    }

    @Override
    public String getErrorDescription() {
        return Messages.getString("RenameCssNodeCommand.ErrorText"); //$NON-NLS-1$
    }

    @Override
    public String getLoggingDescription() {
        if (!newName.equals(node.getName())) {
            return MessageFormat.format("Renaming node {0} to {1}", node.getName(), newName); //$NON-NLS-1$
        }

        return null;
    }

    @Override
    protected IStatus doRun(final IProgressMonitor progressMonitor) throws Exception {
        if (!newName.equals(node.getName())) {
            css.renameNode(node.getURI(), newName);
            node.setName(newName);
        }

        return Status.OK_STATUS;
    }

    public CSSNode getNode() {
        return node;
    }

}
