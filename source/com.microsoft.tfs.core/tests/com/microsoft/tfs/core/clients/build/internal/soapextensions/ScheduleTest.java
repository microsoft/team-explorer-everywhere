// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.build.internal.soapextensions;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import com.microsoft.tfs.core.clients.build.flags.ScheduleDays;

import junit.framework.TestCase;
import ms.tfs.build.buildservice._04._Schedule;

public class ScheduleTest extends TestCase {

    private final TimeZone defaultTz = TimeZone.getDefault();

    @Override
    protected void setUp() throws Exception {
    }

    @Override
    protected void tearDown() throws Exception {
        TimeZone.setDefault(defaultTz);
    }

    public void testGetFirstDayOfWeek() {
        final Schedule schedule = new Schedule();
        assertEquals(Calendar.SUNDAY, schedule.getFirstDayOfWeek(Calendar.getInstance()).get(Calendar.DAY_OF_WEEK));
        assertEquals(0, schedule.getFirstDayOfWeek(Calendar.getInstance()).get(Calendar.HOUR_OF_DAY));
        assertEquals(0, schedule.getFirstDayOfWeek(Calendar.getInstance()).get(Calendar.MINUTE));
        assertEquals(0, schedule.getFirstDayOfWeek(Calendar.getInstance()).get(Calendar.SECOND));
    }

    public void testConvert() {
        assertEquals(ScheduleDays.SUNDAY, Schedule.convert(Calendar.SUNDAY));
        assertEquals(ScheduleDays.MONDAY, Schedule.convert(Calendar.MONDAY));
        assertEquals(ScheduleDays.TUESDAY, Schedule.convert(Calendar.TUESDAY));
        assertEquals(ScheduleDays.WEDNESDAY, Schedule.convert(Calendar.WEDNESDAY));
        assertEquals(ScheduleDays.THURSDAY, Schedule.convert(Calendar.THURSDAY));
        assertEquals(ScheduleDays.FRIDAY, Schedule.convert(Calendar.FRIDAY));
        assertEquals(ScheduleDays.SATURDAY, Schedule.convert(Calendar.SATURDAY));
    }

    public void testGetLocalDaysToBuildFromScheduleAll() {
        final ScheduleDays expected = new ScheduleDays(ScheduleDays.ALL);
        final _Schedule soapSchedule = new _Schedule();
        soapSchedule.setUtcDaysToBuild(expected.getWebServiceObject());
        soapSchedule.setUtcStartTime(3600); // 1am UTC

        final Schedule schedule = new Schedule(soapSchedule);

        assertEquals(expected, schedule.getLocalDaysToBuildFromSchedule());
    }

    public void testGetLocalDaysToBuildFromScheduleAllInPST() {
        TimeZone.setDefault(TimeZone.getTimeZone("PST")); //$NON-NLS-1$
        final ScheduleDays expected = new ScheduleDays(ScheduleDays.ALL);
        final _Schedule soapSchedule = new _Schedule();
        soapSchedule.setUtcDaysToBuild(expected.getWebServiceObject());
        soapSchedule.setUtcStartTime(3600); // 1am UTC

        final Schedule schedule = new Schedule(soapSchedule);

        assertEquals(expected, schedule.getLocalDaysToBuildFromSchedule());
    }

    public void testGetLocalDaysToBuildFromScheduleSome() {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC")); //$NON-NLS-1$

        final ScheduleDays monWedFri = new ScheduleDays();
        monWedFri.add(ScheduleDays.MONDAY);
        monWedFri.add(ScheduleDays.WEDNESDAY);
        monWedFri.add(ScheduleDays.FRIDAY);

        final _Schedule soapSchedule = new _Schedule();
        soapSchedule.setUtcDaysToBuild(monWedFri.getWebServiceObject());
        soapSchedule.setUtcStartTime(3600); // 1am UTC

        final Schedule schedule = new Schedule(soapSchedule);

        assertEquals(monWedFri, schedule.getLocalDaysToBuildFromSchedule());
    }

    public void testGetLocalDaysToBuildFromScheduleSomeInPST() {
        TimeZone.setDefault(TimeZone.getTimeZone("PST")); //$NON-NLS-1$

        final ScheduleDays utcDays = new ScheduleDays();
        utcDays.add(ScheduleDays.MONDAY);
        utcDays.add(ScheduleDays.WEDNESDAY);
        utcDays.add(ScheduleDays.FRIDAY);

        new ScheduleDays();
        utcDays.add(ScheduleDays.SUNDAY);
        utcDays.add(ScheduleDays.TUESDAY);
        utcDays.add(ScheduleDays.THURSDAY);

        final _Schedule soapSchedule = new _Schedule();
        soapSchedule.setUtcDaysToBuild(utcDays.getWebServiceObject());
        soapSchedule.setUtcStartTime(3600); // 1am UTC
    }

    public void testRemoveTime() {
        final Calendar cal = new GregorianCalendar();
        Schedule.removeTime(cal);
        assertEquals(0, cal.get(Calendar.HOUR_OF_DAY));
        assertEquals(0, cal.get(Calendar.MINUTE));
        assertEquals(0, cal.get(Calendar.SECOND));
        assertEquals(Calendar.AM, cal.get(Calendar.AM_PM));
    }

    public void testGetLocalTimeFromScheduleMidnightUTC() {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC")); //$NON-NLS-1$

        final _Schedule soapSchedule = new _Schedule();
        soapSchedule.setUtcStartTime(0);

        final Schedule schedule = new Schedule(soapSchedule);

        final int actual = schedule.getLocalTimeFromSchedule();
        assertEquals(0, actual);
    }

    public void testGetLocalTimeFromSchedule3amUTC() {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC")); //$NON-NLS-1$

        final _Schedule soapSchedule = new _Schedule();
        soapSchedule.setUtcStartTime(10800);

        final Schedule schedule = new Schedule(soapSchedule);

        final int actual = schedule.getLocalTimeFromSchedule();
        assertEquals(10800, actual);
    }

    public void testGetLocalTimeFromScheduleIST() {
        TimeZone.setDefault(TimeZone.getTimeZone("IST")); //$NON-NLS-1$

        final _Schedule soapSchedule = new _Schedule();
        soapSchedule.setUtcStartTime(68400);

        final Schedule schedule = new Schedule(soapSchedule);

        final int actual = schedule.getLocalTimeFromSchedule();
        assertEquals("19:00 UTC = 00:30 IST", 1800, actual); //$NON-NLS-1$
    }

    public void testAScheduleUTC() {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC")); //$NON-NLS-1$

        final Schedule schedule = new Schedule();

        schedule.setDaysToBuild(new ScheduleDays(ScheduleDays.ALL));
        schedule.setStartTime(10800);

        assertEquals(new ScheduleDays(ScheduleDays.ALL), schedule.getDaysToBuild());
        assertEquals(10800, schedule.getStartTime());

        _Schedule soapSchedule = schedule.getWebServiceObject();
        assertEquals(10800, soapSchedule.getUtcStartTime());

        final ScheduleDays monWedFri = new ScheduleDays();
        monWedFri.add(ScheduleDays.MONDAY);
        monWedFri.add(ScheduleDays.WEDNESDAY);
        monWedFri.add(ScheduleDays.FRIDAY);

        schedule.setDaysToBuild(monWedFri);

        assertEquals(monWedFri, schedule.getDaysToBuild());
        assertEquals(10800, schedule.getStartTime());

        soapSchedule = schedule.getWebServiceObject();
        assertEquals(monWedFri.getWebServiceObject(), soapSchedule.getUtcDaysToBuild());
        assertEquals(10800, soapSchedule.getUtcStartTime());

        schedule.setStartTime(0);
        assertEquals(monWedFri, schedule.getDaysToBuild());
        assertEquals(0, schedule.getStartTime());

        soapSchedule = schedule.getWebServiceObject();
        assertEquals(monWedFri.getWebServiceObject(), soapSchedule.getUtcDaysToBuild());
        assertEquals(0, soapSchedule.getUtcStartTime());

    }

    // Think there is a daylight savings issue with this code.
    //
    // public void testASchedulePST()
    // {
    // TimeZone.setDefault(TimeZone.getTimeZone("PST")); //$NON-NLS-1$
    //
    // Schedule schedule = new Schedule();
    //
    // schedule.setDaysToBuild(new ScheduleDays(ScheduleDays.ALL));
    // schedule.setStartTime(10800);
    //
    // assertEquals(new ScheduleDays(ScheduleDays.ALL),
    // schedule.getDaysToBuild());
    // assertEquals(10800, schedule.getStartTime());
    //
    // _Schedule soapSchedule = schedule.getWebServiceObject();
    //
    // ScheduleDays monWedFri = new ScheduleDays();
    // monWedFri.add(ScheduleDays.MONDAY);
    // monWedFri.add(ScheduleDays.WEDNESDAY);
    // monWedFri.add(ScheduleDays.FRIDAY);
    //
    // schedule.setDaysToBuild(monWedFri);
    //
    // assertEquals(monWedFri, schedule.getDaysToBuild());
    // assertEquals(10800, schedule.getStartTime());
    //
    // soapSchedule = schedule.getWebServiceObject();
    // assertEquals(monWedFri.getWebServiceObject(),
    // soapSchedule.getUtcDaysToBuild());
    //
    // schedule.setStartTime(68400);
    // assertEquals(monWedFri, schedule.getDaysToBuild());
    // assertEquals(68400, schedule.getStartTime());
    //
    // ScheduleDays tueThuSat = new ScheduleDays();
    // monWedFri.add(ScheduleDays.TUESDAY);
    // monWedFri.add(ScheduleDays.THURSDAY);
    // monWedFri.add(ScheduleDays.SATURDAY);
    //
    // }

    private void assertEquals(final ScheduleDays expected, final ScheduleDays actual) {
        assertEquals("Unexepected ScheduleDays item count", expected.size(), actual.size()); //$NON-NLS-1$
        assertTrue("Unexpected ScheduleDays content", actual.containsAll(expected)); //$NON-NLS-1$
    }
}
