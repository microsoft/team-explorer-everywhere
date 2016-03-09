// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.util.datetime;

import java.text.SimpleDateFormat;
import java.util.Calendar;

import junit.framework.TestCase;

/**
 * Some random tests to help me figure out the drop down calendar.
 */
public class CalTest extends TestCase {
    public void testCalFields() throws Exception {
        final Calendar cal = Calendar.getInstance();

        System.out.println(cal.getTime());

        // cal.set(Calendar.AM_PM, 0);
        cal.add(Calendar.AM_PM, 1);

        System.out.println(cal.getTime());

        System.out.println(SimpleDateFormat.getDateInstance().format(DotNETDate.MIN_CALENDAR_LOCAL.getTime()));
    }
}
