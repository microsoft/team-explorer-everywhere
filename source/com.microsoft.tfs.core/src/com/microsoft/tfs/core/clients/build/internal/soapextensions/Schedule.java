// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.build.internal.soapextensions;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import com.microsoft.tfs.core.Messages;
import com.microsoft.tfs.core.clients.build.IBuildDefinition;
import com.microsoft.tfs.core.clients.build.ISchedule;
import com.microsoft.tfs.core.clients.build.flags.ScheduleDays;
import com.microsoft.tfs.core.clients.build.flags.ScheduleType;
import com.microsoft.tfs.core.internal.wrappers.WebServiceObjectWrapper;
import com.microsoft.tfs.util.datetime.CalendarUtils;

import ms.tfs.build.buildservice._04._Schedule;

public class Schedule extends WebServiceObjectWrapper implements ISchedule {
    private IBuildDefinition buildDefinition;

    private final String TIMEZONE_UTC_DOTNET_ID = "UTC"; //$NON-NLS-1$
    private final TimeZone TIMEZONE_UTC = TimeZone.getTimeZone("UTC"); //$NON-NLS-1$
    private final TimeZone TIMEZONE_DEFAULT = TimeZone.getDefault();
    private final Locale LOCALE_DEFAULT = Locale.getDefault();

    public Schedule(final _Schedule schedule) {
        super(schedule);
    }

    public Schedule() {
        this(new _Schedule());

        /*
         * Set the timezone id to UTC. The timezone id is required by 2010
         * servers. This has the side-effect that the scheduled build time will
         * change during daylight savings time, but doing this correctly would
         * require writing a Java to .NET timezone converter.
         */
        getWebServiceObject().setTimeZoneId(TIMEZONE_UTC_DOTNET_ID);
    }

    public Schedule(final IBuildDefinition buildDefinition) {
        this();
        this.buildDefinition = buildDefinition;
    }

    public Schedule(final BuildDefinition buildDefinition, final Schedule2010 schedule2010) {
        this(buildDefinition);

        final _Schedule _o = getWebServiceObject();
        _o.setTimeZoneId(schedule2010.getWebServiceObject().getTimeZoneId());
        _o.setUtcDaysToBuild(TFS2010Helper.convert(schedule2010.getUtcDaysToBuild()).getWebServiceObject());
        _o.setUtcStartTime(schedule2010.getUtcStartTime());
    }

    public _Schedule getWebServiceObject() {
        return (_Schedule) this.webServiceObject;
    }

    @Override
    public IBuildDefinition getBuildDefinition() {
        return buildDefinition;
    }

    public void setBuildDefinition(final IBuildDefinition buildDefinition) {
        this.buildDefinition = buildDefinition;
    }

    @Override
    public ScheduleDays getDaysToBuild() {
        return getLocalDaysToBuildFromSchedule();
    }

    @Override
    public ScheduleType getType() {
        return ScheduleType.WEEKLY;
    }

    @Override
    public int getStartTime() {
        return getLocalTimeFromSchedule();
    }

    @Override
    public void setDaysToBuild(final ScheduleDays days) {
        setScheduleFromLocationTime(getStartTime(), days);
    }

    @Override
    public void setStartTime(final int secondsPastMidnight) {
        setScheduleFromLocationTime(secondsPastMidnight, getDaysToBuild());
    }

    protected int getLocalTimeFromSchedule() {
        /*
         * Note that this differs from the TFS 2008 RTM client to fix a reported
         * bug in the client. For more information see
         * http://blogs.msdn.com/jpricket
         * /archive/2007/10/26/scheduling-a-build-in
         * -vs2008-and-daylight-savings-time.aspx
         */

        final Calendar startTime = new GregorianCalendar(TIMEZONE_UTC, LOCALE_DEFAULT);
        Schedule.removeTime(startTime);

        startTime.add(Calendar.SECOND, getWebServiceObject().getUtcStartTime());

        startTime.setTimeZone(TIMEZONE_DEFAULT);

        return getSecondsSinceMidnight(startTime);
    }

    protected ScheduleDays getLocalDaysToBuildFromSchedule() {
        final Calendar firstDayOfWeek = getFirstDayOfWeek(new GregorianCalendar(TIMEZONE_UTC, LOCALE_DEFAULT));

        final List<Calendar> daysMatched = new ArrayList<Calendar>();
        final int utcSecondsSinceMidnight = getWebServiceObject().getUtcStartTime();
        final ScheduleDays utcDays = new ScheduleDays(getWebServiceObject().getUtcDaysToBuild());
        if (utcDays.contains(ScheduleDays.SUNDAY)) {
            daysMatched.add(getLocalDate(firstDayOfWeek, 0, utcSecondsSinceMidnight));
        }
        if (utcDays.contains(ScheduleDays.MONDAY)) {
            daysMatched.add(getLocalDate(firstDayOfWeek, 1, utcSecondsSinceMidnight));
        }
        if (utcDays.contains(ScheduleDays.TUESDAY)) {
            daysMatched.add(getLocalDate(firstDayOfWeek, 2, utcSecondsSinceMidnight));
        }
        if (utcDays.contains(ScheduleDays.WEDNESDAY)) {
            daysMatched.add(getLocalDate(firstDayOfWeek, 3, utcSecondsSinceMidnight));
        }
        if (utcDays.contains(ScheduleDays.THURSDAY)) {
            daysMatched.add(getLocalDate(firstDayOfWeek, 4, utcSecondsSinceMidnight));
        }
        if (utcDays.contains(ScheduleDays.FRIDAY)) {
            daysMatched.add(getLocalDate(firstDayOfWeek, 5, utcSecondsSinceMidnight));
        }
        if (utcDays.contains(ScheduleDays.SATURDAY)) {
            daysMatched.add(getLocalDate(firstDayOfWeek, 6, utcSecondsSinceMidnight));
        }

        final ScheduleDays days = new ScheduleDays();
        for (final Calendar day : daysMatched) {
            final ScheduleDays.Day other = Schedule.convert(day.get(Calendar.DAY_OF_WEEK));
            days.add(other);
        }

        return days;
    }

    protected void setScheduleFromLocationTime(final int secondsSinceMidnight, final ScheduleDays daysToBuild) {
        final Calendar firstDayOfWeek = getFirstDayOfWeek(Calendar.getInstance());

        final List<Calendar> daysMatched = new ArrayList<Calendar>();

        if (daysToBuild.contains(ScheduleDays.SUNDAY)) {
            daysMatched.add(getUTCDate(firstDayOfWeek, 0, secondsSinceMidnight));
        }
        if (daysToBuild.contains(ScheduleDays.MONDAY)) {
            daysMatched.add(getUTCDate(firstDayOfWeek, 1, secondsSinceMidnight));
        }
        if (daysToBuild.contains(ScheduleDays.TUESDAY)) {
            daysMatched.add(getUTCDate(firstDayOfWeek, 2, secondsSinceMidnight));
        }
        if (daysToBuild.contains(ScheduleDays.WEDNESDAY)) {
            daysMatched.add(getUTCDate(firstDayOfWeek, 3, secondsSinceMidnight));
        }
        if (daysToBuild.contains(ScheduleDays.THURSDAY)) {
            daysMatched.add(getUTCDate(firstDayOfWeek, 4, secondsSinceMidnight));
        }
        if (daysToBuild.contains(ScheduleDays.FRIDAY)) {
            daysMatched.add(getUTCDate(firstDayOfWeek, 5, secondsSinceMidnight));
        }
        if (daysToBuild.contains(ScheduleDays.SATURDAY)) {
            daysMatched.add(getUTCDate(firstDayOfWeek, 6, secondsSinceMidnight));
        }

        final ScheduleDays days = new ScheduleDays();
        for (final Calendar day : daysMatched) {
            final ScheduleDays.Day other = Schedule.convert(day.get(Calendar.DAY_OF_WEEK));
            days.add(other);
        }

        getWebServiceObject().setUtcDaysToBuild(days.getWebServiceObject());

        // Note that this differs from the TFS 2008 RTM client to fix a reported
        // bug in the client. For more information see
        // http://blogs.msdn.com/jpricket/archive/2007/10/26/scheduling-a-build-in-vs2008-and-daylight-savings-time.aspx

        getWebServiceObject().setUtcStartTime(
            getSecondsSinceMidnight(getUTCDate(removeTime(new GregorianCalendar()), 0, secondsSinceMidnight)));
    }

    protected Calendar getLocalDate(final Calendar firstDayOfWeek, final int daysToAdd, final int secondsToAdd) {
        final Calendar day = (Calendar) firstDayOfWeek.clone();
        day.add(Calendar.DAY_OF_YEAR, daysToAdd);
        day.add(Calendar.SECOND, secondsToAdd);
        day.setTimeZone(TIMEZONE_DEFAULT);
        return day;
    }

    protected Calendar getUTCDate(final Calendar firstDayOfWeek, final int daysToAdd, final int secondsToAdd) {
        final Calendar day = (Calendar) firstDayOfWeek.clone();
        day.add(Calendar.DAY_OF_YEAR, daysToAdd);
        day.add(Calendar.SECOND, secondsToAdd);
        day.setTimeZone(TIMEZONE_UTC);
        return day;
    }

    /**
     * Return the closest Sunday
     */
    protected Calendar getFirstDayOfWeek(final Calendar referenceDate) {
        final Calendar cal = (Calendar) referenceDate.clone();
        // remove the time part.
        Schedule.removeTime(cal);

        while (cal.get(Calendar.DAY_OF_WEEK) != Calendar.SUNDAY) {
            cal.add(Calendar.DAY_OF_YEAR, -1);
        }

        return cal;
    }

    protected static Calendar removeTime(final Calendar date) {
        return CalendarUtils.removeTime(date);
    }

    protected static int getSecondsSinceMidnight(final Calendar time) {
        return CalendarUtils.getSecondsSinceMidnight(time);
    }

    protected static ScheduleDays.Day convert(final int dayOfWeek) {
        switch (dayOfWeek) {
            case Calendar.SUNDAY:
                return ScheduleDays.SUNDAY;

            case Calendar.MONDAY:
                return ScheduleDays.MONDAY;

            case Calendar.TUESDAY:
                return ScheduleDays.TUESDAY;

            case Calendar.WEDNESDAY:
                return ScheduleDays.WEDNESDAY;

            case Calendar.THURSDAY:
                return ScheduleDays.THURSDAY;

            case Calendar.FRIDAY:
                return ScheduleDays.FRIDAY;

            case Calendar.SATURDAY:
                return ScheduleDays.SATURDAY;

            default:
                break;
        }

        throw new IllegalArgumentException(
            MessageFormat.format(Messages.getString("Schedule.DayOfWeekInvalidFormat"), dayOfWeek)); //$NON-NLS-1$
    }
}
