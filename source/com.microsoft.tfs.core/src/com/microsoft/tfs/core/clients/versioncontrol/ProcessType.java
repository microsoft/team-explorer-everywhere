// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol;

import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.GetOperation;
import com.microsoft.tfs.util.TypesafeEnum;

/**
 * An enumeration of kinds of processing that can happen for
 * {@link GetOperation}s.
 *
 * @threadsafety thread-safe
 * @since TEE-SDK-10.1
 */
public class ProcessType extends TypesafeEnum {
    private ProcessType(final int value) {
        super(value);
    }

    public final static ProcessType NONE = new ProcessType(0);
    public final static ProcessType UNDO = new ProcessType(1);
    public final static ProcessType PEND = new ProcessType(2);
    public final static ProcessType GET = new ProcessType(3);
    public final static ProcessType MERGE = new ProcessType(4);
    public final static ProcessType UNSHELVE = new ProcessType(5);

    /**
     * @since TFS 2010
     */
    public final static ProcessType ROLLBACK = new ProcessType(6);
}
