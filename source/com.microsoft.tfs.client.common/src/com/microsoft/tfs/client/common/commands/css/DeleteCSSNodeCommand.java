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

public class DeleteCSSNodeCommand extends CSSNodeCommand {
    private final CSSNode nodeToDelete;
    private final CSSNode reclassifyNode;;
    private final CommonStructureClient css;

    public DeleteCSSNodeCommand(
        final CommonStructureClient css,
        final CSSNode nodeToDelete,
        final CSSNode reclassifyNode) {
        super();

        Check.notNull(css, "css"); //$NON-NLS-1$
        Check.notNull(nodeToDelete, "nodeToDelete"); //$NON-NLS-1$
        Check.notNull(reclassifyNode, "reclassifyNode"); //$NON-NLS-1$

        this.css = css;
        this.nodeToDelete = nodeToDelete;
        this.reclassifyNode = reclassifyNode;

        setConnection(css.getConnection());
    }

    @Override
    public String getName() {
        return Messages.getString("DeleteCssNodeCommand.CommandText"); //$NON-NLS-1$
    }

    @Override
    public String getErrorDescription() {
        return Messages.getString("DeleteCssNodeCommand.ErrorText"); //$NON-NLS-1$
    }

    @Override
    public String getLoggingDescription() {
        return Messages.getString("DeleteCssNodeCommand.CommandText", LocaleUtil.ROOT); //$NON-NLS-1$
    }

    @Override
    protected IStatus doRun(final IProgressMonitor progressMonitor) throws Exception {
        css.deleteBranches(new String[] {
            nodeToDelete.getURI()
        }, reclassifyNode.getURI());

        return Status.OK_STATUS;
    }
}
