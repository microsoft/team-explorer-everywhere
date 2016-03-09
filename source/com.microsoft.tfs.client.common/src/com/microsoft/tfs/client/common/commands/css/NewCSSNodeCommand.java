// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.commands.css;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import com.microsoft.tfs.client.common.Messages;
import com.microsoft.tfs.core.clients.commonstructure.CSSNode;
import com.microsoft.tfs.core.clients.commonstructure.CommonStructureClient;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.LocaleUtil;

public class NewCSSNodeCommand extends CSSNodeCommand {
    private final CommonStructureClient css;
    private final CSSNode node;

    private final String newName;

    public NewCSSNodeCommand(final CommonStructureClient css, final CSSNode node, final String newName) {
        super();

        Check.notNull(css, "css"); //$NON-NLS-1$
        Check.notNull(node, "node"); //$NON-NLS-1$
        Check.notNullOrEmpty(newName, "newName"); //$NON-NLS-1$

        this.css = css;
        this.newName = newName;
        this.node = node;

        setConnection(css.getConnection());
    }

    @Override
    public String getName() {
        return Messages.getString("NewCssNodeCommand.CommandText"); //$NON-NLS-1$
    }

    @Override
    public String getErrorDescription() {
        return Messages.getString("NewCssNodeCommand.ErrorText"); //$NON-NLS-1$
    }

    @Override
    public String getLoggingDescription() {
        return Messages.getString("NewCssNodeCommand.CommandText", LocaleUtil.ROOT); //$NON-NLS-1$
    }

    @Override
    protected IStatus doRun(final IProgressMonitor progressMonitor) throws Exception {
        final String uri = css.createNode(newName, node.getParentURI());
        node.setURI(uri);
        node.setName(newName);

        return Status.OK_STATUS;
    }

    public CSSNode getNode() {
        return node;
    }

}
