// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.commands.vc;

import com.microsoft.tfs.client.common.repository.TFSRepository;
import com.microsoft.tfs.util.Check;

public class GetDownloadURLToTempLocationCommand extends AbstractGetToTempLocationCommand {
    private final String downloadUrl;
    private final String description;

    public GetDownloadURLToTempLocationCommand(final TFSRepository repository, final String downloadUrl) {
        this(repository, downloadUrl, downloadUrl);
    }

    public GetDownloadURLToTempLocationCommand(
        final TFSRepository repository,
        final String downloadUrl,
        final String description) {
        super(repository);

        Check.notNull(downloadUrl, "downloadUrl"); //$NON-NLS-1$
        Check.notNull(description, "description"); //$NON-NLS-1$

        this.downloadUrl = downloadUrl;
        this.description = description;
    }

    @Override
    protected String getDownloadURL() {
        return downloadUrl;
    }

    @Override
    public String getFileDescription() {
        return description;
    }
}
