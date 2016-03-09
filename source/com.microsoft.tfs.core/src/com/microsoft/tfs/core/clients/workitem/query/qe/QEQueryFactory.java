// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.query.qe;

import com.microsoft.tfs.core.clients.workitem.WorkItemClient;
import com.microsoft.tfs.core.clients.workitem.internal.query.qe.QEQueryImpl;
import com.microsoft.tfs.core.clients.workitem.queryhierarchy.LinkQueryMode;

/**
 * @since TEE-SDK-10.1
 */
public class QEQueryFactory {
    public static QEQuery createQueryFromWIQL(
        final String wiql,
        final WorkItemClient client,
        final LinkQueryMode mode) {
        return new QEQueryImpl(client, wiql, mode);
    }
}
