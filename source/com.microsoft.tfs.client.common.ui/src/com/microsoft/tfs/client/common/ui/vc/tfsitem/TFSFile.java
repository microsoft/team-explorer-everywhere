// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.vc.tfsitem;

import com.microsoft.tfs.client.common.item.ServerItemPath;
import com.microsoft.tfs.client.common.repository.TFSRepository;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.ExtendedItem;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.RecursionType;
import com.microsoft.tfs.core.clients.versioncontrol.specs.ItemSpec;

public class TFSFile extends TFSItem {
    public TFSFile(final ExtendedItem extendedItem) {
        super(extendedItem);
    }

    public TFSFile(final ServerItemPath itemPath) {
        super(itemPath);
    }

    public TFSFile(final ServerItemPath itemPath, final TFSRepository repository) {
        super(itemPath, repository);
    }

    @Override
    public ItemSpec getItemSpec() {
        return new ItemSpec(getFullPath(), RecursionType.NONE, getDeletionID());
    }
}
