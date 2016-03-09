// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.tasks.vc;

import org.eclipse.swt.widgets.Shell;

import com.microsoft.tfs.client.common.repository.TFSRepository;
import com.microsoft.tfs.core.clients.versioncontrol.GetOptions;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.GetRequest;
import com.microsoft.tfs.util.Check;

/**
 * Gets the specified versions of the file(s).
 */
public class GetTask extends AbstractGetTask {
    private final GetRequest[] getRequests;
    private final GetOptions getOptions;

    public GetTask(
        final Shell shell,
        final TFSRepository repository,
        final GetRequest[] getRequests,
        final GetOptions getOptions) {
        super(shell, repository);

        Check.notNull(getRequests, "getRequests"); //$NON-NLS-1$
        Check.notNull(getOptions, "getOptions"); //$NON-NLS-1$

        this.getRequests = getRequests;
        this.getOptions = getOptions;
    }

    @Override
    protected boolean init() {
        return true;
    }

    @Override
    protected GetRequest[] getGetRequests() {
        return getRequests;
    }

    @Override
    protected GetOptions getGetOptions() {
        return getOptions;
    }
}
