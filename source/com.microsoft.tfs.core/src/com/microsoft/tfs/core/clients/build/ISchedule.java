// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.build;

import com.microsoft.tfs.core.clients.build.flags.ScheduleDays;
import com.microsoft.tfs.core.clients.build.flags.ScheduleType;

public interface ISchedule {
    /**
     * The build definition that owns the schedule.
     *
     *
     * @return
     */
    public IBuildDefinition getBuildDefinition();

    /**
     * The type of schedule (Weekly is the only supported type currently)
     *
     *
     * @return
     */
    public ScheduleType getType();

    /**
     * The time as seconds past midnight.
     *
     *
     * @return
     */
    public int getStartTime();

    public void setStartTime(int value);

    /**
     * Specifies on which days of the week the schedule will trigger a build.
     *
     *
     * @return
     */
    public ScheduleDays getDaysToBuild();

    public void setDaysToBuild(ScheduleDays value);

    // / TODO: NYI
    /**
     * Specifies the time zone for which daylight savings time rules should
     * apply.
     */
    // /public TimeZoneInfo TimeZone { get; set; }
}
