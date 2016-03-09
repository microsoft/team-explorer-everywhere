// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.vc.serveritem;

import com.microsoft.tfs.client.common.git.json.TfsGitRepositoryJson;

public class TypedServerGitRepository extends TypedServerItem {
    private final TfsGitRepositoryJson gitRepositoryJson;

    public TypedServerGitRepository(final String serverPath, final TfsGitRepositoryJson gitRepositoryJson) {
        super(serverPath, ServerItemType.GIT_REPOSITORY);
        this.gitRepositoryJson = gitRepositoryJson;
    }

    @Override
    public String getName() {
        return gitRepositoryJson.getName();
    }

    public TfsGitRepositoryJson getJson() {
        return gitRepositoryJson;
    }
}
