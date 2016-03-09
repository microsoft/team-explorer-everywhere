// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.diagnostics;

import java.io.File;

import com.microsoft.tfs.client.common.repository.TFSRepository;
import com.microsoft.tfs.client.common.ui.TFSCommonUIClientPlugin;
import com.microsoft.tfs.client.common.ui.framework.diagnostics.extend.AvailableCallback;
import com.microsoft.tfs.client.common.ui.framework.diagnostics.extend.DataProvider;
import com.microsoft.tfs.client.common.ui.framework.diagnostics.extend.NonLocalizedDataProvider;
import com.microsoft.tfs.client.common.ui.framework.diagnostics.extend.PopulateCallback;
import com.microsoft.tfs.core.TFSTeamProjectCollection;
import com.microsoft.tfs.core.clients.workitem.WorkItemClient;

public class WITDatabaseFileProvider extends NonLocalizedDataProvider
    implements DataProvider, AvailableCallback, PopulateCallback {
    private File witDatabaseFile;

    @Override
    public boolean isAvailable() {
        return witDatabaseFile != null;
    }

    @Override
    public Object getData() {
        return witDatabaseFile;
    }

    @Override
    public void populate() throws Exception {
        witDatabaseFile = null;

        final TFSRepository defaultRepository =
            TFSCommonUIClientPlugin.getDefault().getProductPlugin().getRepositoryManager().getDefaultRepository();

        if (defaultRepository != null) {
            final TFSTeamProjectCollection c = defaultRepository.getWorkspace().getClient().getConnection();
            final WorkItemClient wiClient = (WorkItemClient) c.getClient(WorkItemClient.class);
            final File witDatabaseDirectory = wiClient.getDatabaseDirectory();
            if (witDatabaseDirectory != null) {
                witDatabaseFile = new File(witDatabaseDirectory, "teamexplorer.script"); //$NON-NLS-1$
                if (!witDatabaseFile.exists()) {
                    witDatabaseFile = null;
                }
            }
        }
    }
}
