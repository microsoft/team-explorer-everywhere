// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem;

import com.microsoft.tfs.util.TypesafeEnum;

/**
 * Defineds work item service versions.
 *
 * @since TEE-SDK-10.1
 */
public class WorkItemServerVersion extends TypesafeEnum {
    public final static WorkItemServerVersion V1 = new WorkItemServerVersion(1);
    public static final WorkItemServerVersion V2 = new WorkItemServerVersion(2);
    public final static WorkItemServerVersion V3 = new WorkItemServerVersion(3);
    public final static WorkItemServerVersion V5 = new WorkItemServerVersion(5);
    public final static WorkItemServerVersion V6 = new WorkItemServerVersion(6);
    public final static WorkItemServerVersion V7 = new WorkItemServerVersion(7);
    public final static WorkItemServerVersion V8 = new WorkItemServerVersion(8);

    private WorkItemServerVersion(final int version) {
        super(version);
    }
}
