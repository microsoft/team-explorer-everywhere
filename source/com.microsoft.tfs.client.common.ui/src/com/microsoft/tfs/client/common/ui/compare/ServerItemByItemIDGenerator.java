// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.compare;

import java.lang.reflect.InvocationTargetException;
import java.text.MessageFormat;

import org.eclipse.core.runtime.IProgressMonitor;

import com.microsoft.tfs.client.common.ui.framework.compare.DifferencerInputGenerator;
import com.microsoft.tfs.core.clients.versioncontrol.VersionControlClient;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Item;
import com.microsoft.tfs.util.Check;

/**
 * Note: please consider using item ids carefully due to changes in 2010 for
 * slot mode vs item mode.
 */
public class ServerItemByItemIDGenerator implements DifferencerInputGenerator {
    private final VersionControlClient versionControlClient;
    private final int itemId;
    private final int changeset;

    public ServerItemByItemIDGenerator(
        final VersionControlClient versionControlClient,
        final int itemId,
        final int changeset) {
        Check.notNull(versionControlClient, "versionControlClient"); //$NON-NLS-1$

        this.versionControlClient = versionControlClient;
        this.itemId = itemId;
        this.changeset = changeset;
    }

    @Override
    public String getLoggingDescription() {
        return MessageFormat.format(
            "Item ID {0} at changeset {1}", //$NON-NLS-1$
            Integer.toString(itemId),
            Integer.toString(changeset));
    }

    @Override
    public Object getInput(final IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
        final Item item = versionControlClient.getItem(itemId, changeset, true);

        return new TFSItemNode(item, versionControlClient);
    }
}
