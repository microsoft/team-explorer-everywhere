// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.build.enums;

import com.microsoft.tfs.core.clients.build.flags.ScheduleDays;

import junit.framework.TestCase;
import ms.tfs.build.buildservice._04._ScheduleDays;
import ms.tfs.build.buildservice._04._ScheduleDays._ScheduleDays_Flag;

public class ScheduleDaysTest extends TestCase {
    public void testSpecialNone() {
        // Start empty
        final ScheduleDays days = new ScheduleDays();
        _ScheduleDays wso = days.getWebServiceObject();
        assertEquals(1, wso.getFlags().length);
        assertEquals(_ScheduleDays_Flag.None, wso.getFlags()[0]);

        // Add monday
        days.add(ScheduleDays.MONDAY);
        wso = days.getWebServiceObject();
        assertEquals(1, wso.getFlags().length);
        assertEquals(_ScheduleDays_Flag.Monday, wso.getFlags()[0]);

        // Remove monday

        days.remove(ScheduleDays.MONDAY);
        wso = days.getWebServiceObject();
        assertEquals(1, wso.getFlags().length);
        assertEquals(_ScheduleDays_Flag.None, wso.getFlags()[0]);
    }

    public void testSpecialAll() {
        // Start empty
        final ScheduleDays days = new ScheduleDays();
        _ScheduleDays wso = days.getWebServiceObject();
        assertEquals(1, wso.getFlags().length);
        assertEquals(_ScheduleDays_Flag.None, wso.getFlags()[0]);

        // Add all
        days.add(ScheduleDays.MONDAY);
        days.add(ScheduleDays.TUESDAY);
        days.add(ScheduleDays.WEDNESDAY);
        days.add(ScheduleDays.THURSDAY);
        days.add(ScheduleDays.FRIDAY);
        days.add(ScheduleDays.SATURDAY);
        days.add(ScheduleDays.SUNDAY);
        wso = days.getWebServiceObject();
        assertEquals(1, wso.getFlags().length);
        assertEquals(_ScheduleDays_Flag.All, wso.getFlags()[0]);
    }
}
