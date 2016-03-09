// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.commands.vc;

import java.text.MessageFormat;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import com.microsoft.tfs.client.common.Messages;
import com.microsoft.tfs.client.common.commands.TFSCommand;
import com.microsoft.tfs.client.common.repository.TFSRepository;
import com.microsoft.tfs.core.clients.versioncontrol.VersionControlConstants;
import com.microsoft.tfs.core.clients.versioncontrol.path.ServerPath;
import com.microsoft.tfs.core.clients.versioncontrol.specs.ItemSpec;
import com.microsoft.tfs.util.Check;

public class QueryExclusiveCheckoutCommand extends TFSCommand {
    private final TFSRepository repository;
    private final ItemSpec[] itemSpecs;

    private boolean exclusiveCheckout = false;

    private static final Log log = LogFactory.getLog(QueryExclusiveCheckoutCommand.class);

    public QueryExclusiveCheckoutCommand(final TFSRepository repository, final ItemSpec[] itemSpecs) {
        Check.notNull(repository, "repository"); //$NON-NLS-1$
        Check.notNull(itemSpecs, "itemSpecs"); //$NON-NLS-1$

        this.repository = repository;
        this.itemSpecs = itemSpecs;

        setCancellable(true);
    }

    @Override
    public String getName() {
        if (itemSpecs.length == 1) {
            final String messageFormat = Messages.getString("QueryExclusiveCheckoutCommand.SingleItemTextFormat"); //$NON-NLS-1$
            return MessageFormat.format(messageFormat, itemSpecs[0].getItem());
        } else {
            return (Messages.getString("QueryExclusiveCheckoutCommand.MultiItemText")); //$NON-NLS-1$
        }
    }

    @Override
    public String getErrorDescription() {
        return (Messages.getString("QueryExclusiveCheckoutCommand.ErrorText")); //$NON-NLS-1$
    }

    @Override
    public String getLoggingDescription() {
        /*
         * May hit the cache, don't log - the annotation cache will handle
         * logging of server calls
         */
        return null;
    }

    @Override
    protected IStatus doRun(final IProgressMonitor progressMonitor) throws Exception {
        final Set<String> teamProjectSet = new HashSet<String>();

        /* Determine server paths of all itemspecs */
        for (int i = 0; i < itemSpecs.length; i++) {
            final String itemPath = itemSpecs[i].getItem();
            String serverPath;

            if (ServerPath.isServerPath(itemPath)) {
                serverPath = itemPath;
            } else {
                serverPath = repository.getWorkspace().getMappedServerPath(itemPath);
            }

            teamProjectSet.add(ServerPath.getTeamProject(serverPath));
        }

        for (final Iterator<String> i = teamProjectSet.iterator(); i.hasNext();) {
            final String teamProject = i.next();

            final String exclusiveCheckoutAnnotation = repository.getAnnotationCache().getAnnotationValue(
                VersionControlConstants.EXCLUSIVE_CHECKOUT_ANNOTATION,
                teamProject,
                0);

            if ("true".equalsIgnoreCase(exclusiveCheckoutAnnotation)) //$NON-NLS-1$
            {
                log.info(MessageFormat.format("Exclusive checkout detected for team project {0}", teamProject)); //$NON-NLS-1$

                exclusiveCheckout = true;
                break;
            }
        }

        return Status.OK_STATUS;
    }

    public boolean isExclusiveCheckout() {
        return exclusiveCheckout;
    }
}
