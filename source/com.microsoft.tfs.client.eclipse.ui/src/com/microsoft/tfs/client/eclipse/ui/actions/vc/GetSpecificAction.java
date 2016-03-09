// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.eclipse.ui.actions.vc;

import org.eclipse.swt.widgets.Shell;

import com.microsoft.tfs.client.common.repository.TFSRepository;
import com.microsoft.tfs.client.common.ui.tasks.vc.AbstractGetTask;
import com.microsoft.tfs.client.common.ui.tasks.vc.GetSpecificTask;
import com.microsoft.tfs.client.common.ui.vc.serveritem.TypedServerItem;

public class GetSpecificAction extends GetAction {
    @Override
    protected AbstractGetTask getGetTask(
        final Shell shell,
        final TFSRepository repository,
        final TypedServerItem[] serverItems) {
        return new GetSpecificTask(shell, repository, serverItems);
    }
}