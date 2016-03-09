// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.engines.internal.workers;

import java.util.concurrent.Callable;

import com.microsoft.tfs.core.clients.versioncontrol.engines.internal.CheckinEngine;
import com.microsoft.tfs.core.clients.versioncontrol.engines.internal.GetEngine;

/**
 * An {@link Worker} is a {@link Callable} that is used by {@link GetEngine} or
 * {@link CheckinEngine} to perform some of its work.
 */
public interface Worker extends Callable<WorkerStatus> {
    /**
     * Maximum size in bytes of the uncompressed file that can be compressed
     * with gzip.
     */
    static final long MAX_GZIP_INPUT_SIZE = 0xFFFFFFFFL;
}
