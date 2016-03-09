// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.framework.command;

public interface CommandCancellableListener {
    void cancellableChanged(boolean isCancellable);
}
