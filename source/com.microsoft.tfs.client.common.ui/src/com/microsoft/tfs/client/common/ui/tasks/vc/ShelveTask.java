// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.tasks.vc;

import org.eclipse.swt.widgets.Shell;

import com.microsoft.tfs.client.common.repository.TFSRepository;
import com.microsoft.tfs.core.pendingcheckin.PendingCheckin;

public class ShelveTask extends AbstractShelveTask {
    private final PendingCheckin pendingCheckin;
    private final String shelvesetName;
    private final boolean preserveFlag;

    public ShelveTask(
        final Shell shell,
        final TFSRepository repository,
        final PendingCheckin pendingCheckin,
        final String shelvesetName,
        final boolean preserveFlag) {
        super(shell, repository);

        this.pendingCheckin = pendingCheckin;
        this.shelvesetName = shelvesetName;
        this.preserveFlag = preserveFlag;
    }

    @Override
    protected PendingCheckin getPendingCheckin() {
        return pendingCheckin;
    }

    @Override
    protected String getShelvesetName() {
        return shelvesetName;
    }

    @Override
    protected boolean getPreserveChangesFlag() {
        return preserveFlag;
    }

    @Override
    protected boolean userCanceledShelve() {
        return false;
    }

}
