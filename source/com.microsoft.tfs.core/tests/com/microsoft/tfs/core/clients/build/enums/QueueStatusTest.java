// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.build.enums;

import com.microsoft.tfs.core.clients.build.flags.QueueStatus;

import junit.framework.TestCase;
import ms.tfs.build.buildservice._04._QueueStatus;
import ms.tfs.build.buildservice._04._QueueStatus._QueueStatus_Flag;

public class QueueStatusTest extends TestCase {
    public void testSpecialNone() {
        // Start empty
        QueueStatus status = QueueStatus.NONE;
        _QueueStatus wso = status.getWebServiceObject();
        assertEquals(1, wso.getFlags().length);
        assertEquals(_QueueStatus_Flag.None, wso.getFlags()[0]);

        // Add CANCELED
        status = status.combine(QueueStatus.CANCELED);
        wso = status.getWebServiceObject();
        assertEquals(1, wso.getFlags().length);
        assertEquals(_QueueStatus_Flag.Canceled, wso.getFlags()[0]);

        // Remove CANCELED

        status = status.remove(QueueStatus.CANCELED);
        wso = status.getWebServiceObject();
        assertEquals(1, wso.getFlags().length);
        assertEquals(_QueueStatus_Flag.None, wso.getFlags()[0]);
    }

    public void testSpecialAll() {
        // Start empty
        QueueStatus status = QueueStatus.NONE;
        _QueueStatus wso = status.getWebServiceObject();
        assertEquals(1, wso.getFlags().length);
        assertEquals(_QueueStatus_Flag.None, wso.getFlags()[0]);

        // Add all
        status = QueueStatus.combine(new QueueStatus[] {
            QueueStatus.IN_PROGRESS,
            QueueStatus.RETRY,
            QueueStatus.QUEUED,
            QueueStatus.POSTPONED,
            QueueStatus.COMPLETED,
            QueueStatus.CANCELED
        });

        wso = status.getWebServiceObject();
        assertEquals(1, wso.getFlags().length);
        assertEquals(_QueueStatus_Flag.All, wso.getFlags()[0]);
    }
}
