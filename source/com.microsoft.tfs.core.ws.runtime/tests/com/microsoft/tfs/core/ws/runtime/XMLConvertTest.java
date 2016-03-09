// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.ws.runtime;

import java.util.Calendar;
import java.util.TimeZone;

import com.microsoft.tfs.core.ws.runtime.xml.XMLConvert;

import junit.framework.TestCase;

public class XMLConvertTest extends TestCase {
    public void testParseDateUTC() {
        Calendar c;

        // 'Z' parses as UTC and comes back with UTC zone set
        c = XMLConvert.parseDate("2011-09-26Z"); //$NON-NLS-1$
        assertEquals(TimeZone.getTimeZone("GMT"), c.getTimeZone()); //$NON-NLS-1$
        assertEquals(2011, c.get(Calendar.YEAR));
        assertEquals(8, c.get(Calendar.MONTH));
        assertEquals(26, c.get(Calendar.DAY_OF_MONTH));
        assertEquals(0, c.get(Calendar.HOUR));
        assertEquals(0, c.get(Calendar.MINUTE));
        assertEquals(0, c.get(Calendar.SECOND));
        assertEquals(0, c.get(Calendar.MILLISECOND));

        // No time zone means parse as UTC, but comes back with default time
        // zone
        c = XMLConvert.parseDate("2011-09-26"); //$NON-NLS-1$
        assertEquals(TimeZone.getDefault(), c.getTimeZone());
        // Adjust back for easy field comparison
        c.setTimeZone(TimeZone.getTimeZone("GMT")); //$NON-NLS-1$
        assertEquals(2011, c.get(Calendar.YEAR));
        assertEquals(8, c.get(Calendar.MONTH));
        assertEquals(26, c.get(Calendar.DAY_OF_MONTH));
        assertEquals(0, c.get(Calendar.HOUR));
        assertEquals(0, c.get(Calendar.MINUTE));
        assertEquals(0, c.get(Calendar.SECOND));
        assertEquals(0, c.get(Calendar.MILLISECOND));
    }

    public void testParseDateLocal() {
        Calendar c;

        // Calendar's time zone is default but time was adjusted for the
        // specified zone
        c = XMLConvert.parseDate("2011-09-26-00:00"); //$NON-NLS-1$
        assertEquals(TimeZone.getDefault(), c.getTimeZone());
        // Adjust back for easy field comparison
        c.setTimeZone(TimeZone.getTimeZone("GMT")); //$NON-NLS-1$
        assertEquals(2011, c.get(Calendar.YEAR));
        assertEquals(8, c.get(Calendar.MONTH));
        assertEquals(26, c.get(Calendar.DAY_OF_MONTH));
        assertEquals(0, c.get(Calendar.HOUR));
        assertEquals(0, c.get(Calendar.MINUTE));
        assertEquals(0, c.get(Calendar.SECOND));
        assertEquals(0, c.get(Calendar.MILLISECOND));

        // Calendar's time zone is default but time was adjusted for the
        // specified zone
        c = XMLConvert.parseDate("2011-09-26-04:00"); //$NON-NLS-1$
        assertEquals(TimeZone.getDefault(), c.getTimeZone());
        // Adjust back for easy field comparison
        c.setTimeZone(TimeZone.getTimeZone("GMT-04:00")); //$NON-NLS-1$
        assertEquals(2011, c.get(Calendar.YEAR));
        assertEquals(8, c.get(Calendar.MONTH));
        assertEquals(26, c.get(Calendar.DAY_OF_MONTH));
        assertEquals(0, c.get(Calendar.HOUR));
        assertEquals(0, c.get(Calendar.MINUTE));
        assertEquals(0, c.get(Calendar.SECOND));
        assertEquals(0, c.get(Calendar.MILLISECOND));

        // Calendar's time zone is default but time was adjusted for the
        // specified zone
        c = XMLConvert.parseDate("2011-09-26+05:00"); //$NON-NLS-1$
        assertEquals(TimeZone.getDefault(), c.getTimeZone());
        // Adjust back for easy field comparison
        c.setTimeZone(TimeZone.getTimeZone("GMT+05:00")); //$NON-NLS-1$
        assertEquals(2011, c.get(Calendar.YEAR));
        assertEquals(8, c.get(Calendar.MONTH));
        assertEquals(26, c.get(Calendar.DAY_OF_MONTH));
        assertEquals(0, c.get(Calendar.HOUR));
        assertEquals(0, c.get(Calendar.MINUTE));
        assertEquals(0, c.get(Calendar.SECOND));
        assertEquals(0, c.get(Calendar.MILLISECOND));
    }
}
