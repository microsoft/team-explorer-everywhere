// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.build.flags;

import com.microsoft.tfs.core.internal.wrappers.EnumerationWrapper;

import ms.tfs.build.buildservice._04._QueuedBuildRetryOption;

public class QueuedBuildRetryOption extends EnumerationWrapper {
    public static final QueuedBuildRetryOption NONE = new QueuedBuildRetryOption(_QueuedBuildRetryOption.None);
    public static final QueuedBuildRetryOption IN_PROGRESS_BUILD =
        new QueuedBuildRetryOption(_QueuedBuildRetryOption.InProgressBuild);
    public static final QueuedBuildRetryOption COMPLETED_BUILD =
        new QueuedBuildRetryOption(_QueuedBuildRetryOption.CompletedBuild);

    private QueuedBuildRetryOption(final _QueuedBuildRetryOption type) {
        super(type);
    }

    public static QueuedBuildRetryOption fromWebServiceObject(final _QueuedBuildRetryOption webServiceObject) {
        if (webServiceObject == null) {
            return null;
        }
        return (QueuedBuildRetryOption) EnumerationWrapper.fromWebServiceObject(webServiceObject);
    }

    public _QueuedBuildRetryOption getWebServiceObject() {
        return (_QueuedBuildRetryOption) webServiceObject;
    }

    @Override
    public boolean equals(final Object o) {
        return super.equals(o);
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }
}
