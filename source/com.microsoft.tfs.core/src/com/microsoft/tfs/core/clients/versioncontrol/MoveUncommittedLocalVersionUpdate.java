// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol;

import com.microsoft.tfs.util.Closable;

public class MoveUncommittedLocalVersionUpdate implements ILocalVersionUpdate, Closable {
    public MoveUncommittedLocalVersionUpdate(final String newTargetServerItem, final String sourceLocalItem) {
        this.newTargetServerItem = newTargetServerItem;
        this.sourceLocalItem = sourceLocalItem;
    }

    @Override
    public boolean isSendToServer() {
        return false;
    }

    @Override
    public boolean isCommitted() {
        return false;
    }

    @Override
    public String getSourceServerItem() {
        return newTargetServerItem;
    }

    @Override
    public int getItemID() {
        return 0;
    }

    @Override
    public String getTargetLocalItem() {
        return null;
    }

    @Override
    public int getVersionLocal() {
        return 0;
    }

    public String getSourceLocalItem() {
        return sourceLocalItem;
    }

    @Override
    public void close() {
    }

    private final String newTargetServerItem;
    private final String sourceLocalItem;
}
