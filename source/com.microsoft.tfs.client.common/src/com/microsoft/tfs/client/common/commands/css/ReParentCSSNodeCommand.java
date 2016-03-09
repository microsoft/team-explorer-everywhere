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

public class ReParentCSSNodeCommand extends CSSNodeCommand {
    private final CommonStructureClient css;
    private final CSSNode node;
    private final CSSNode newParent;

    public ReParentCSSNodeCommand(final CommonStructureClient css, final CSSNode node, final CSSNode newParent) {
        super();

        Check.notNull(css, "css"); //$NON-NLS-1$
        Check.notNull(node, "node"); //$NON-NLS-1$
        Check.notNull(newParent, "newParent"); //$NON-NLS-1$

        this.css = css;
        this.node = node;
        this.newParent = newParent;

        setConnection(css.getConnection());
    }

    @Override
    public String getName() {
        return Messages.getString("ReParentCssNodeCommand.CommandText"); //$NON-NLS-1$
    }

    @Override
    public String getErrorDescription() {
        return Messages.getString("ReParentCssNodeCommand.ErrorText"); //$NON-NLS-1$
    }

    @Override
    public String getLoggingDescription() {
        return Messages.getString("ReParentCssNodeCommand.CommandText", LocaleUtil.ROOT); //$NON-NLS-1$
    }

    @Override
    protected IStatus doRun(final IProgressMonitor progressMonitor) throws Exception {
        css.moveBranch(node.getURI(), newParent.getURI());
        return Status.OK_STATUS;
    }

    public CSSNode getNode() {
        return node;
    }

}
