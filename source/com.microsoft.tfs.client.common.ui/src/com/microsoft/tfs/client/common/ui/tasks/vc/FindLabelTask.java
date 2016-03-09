// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.tasks.vc;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.swt.widgets.Shell;

import com.microsoft.tfs.client.common.repository.TFSRepository;
import com.microsoft.tfs.client.common.ui.controls.vc.FindLabelDialog;
import com.microsoft.tfs.client.common.ui.tasks.BaseTask;
import com.microsoft.tfs.util.Check;

public class FindLabelTask extends BaseTask {
    private final TFSRepository repository;
    private final String teamProjectServerPath;

    public FindLabelTask(final Shell shell, final TFSRepository repository, final String teamProjectServerPath) {
        super(shell);

        Check.notNull(repository, "repository"); //$NON-NLS-1$
        Check.notNullOrEmpty(teamProjectServerPath, "teamProjectServerPath"); //$NON-NLS-1$

        this.repository = repository;
        this.teamProjectServerPath = teamProjectServerPath;
    }

    @Override
    public IStatus run() {
        final FindLabelDialog dialog = new FindLabelDialog(getShell(), repository, teamProjectServerPath);

        dialog.open();

        return Status.OK_STATUS;
    }
}
