// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

 /*
* ---------------------------------------------------------
* Generated file, DO NOT EDIT
* ---------------------------------------------------------
*
* See following wiki page for instructions on how to regenerate:
*   https://vsowiki.com/index.php?title=Rest_Client_Generation
*/

package com.microsoft.teamfoundation.build.webapi.model;

import java.util.List;
import java.util.UUID;

/** 
 */
public class Schedule {

    private List<String> branchFilters;
    /**
    * Days for a build (flags enum for days of the week)
    */
    private ScheduleDays daysToBuild;
    /**
    * The Job Id of the Scheduled job that will queue the scheduled build. Since a single trigger can have multiple schedules and we want a single job to process a single schedule (since each schedule has a list of branches to build), the schedule itself needs to define the Job Id. This value will be filled in when a definition is added or updated.  The UI does not provide it or use it.
    */
    private UUID scheduleJobId;
    /**
    * Local timezone hour to start
    */
    private int startHours;
    /**
    * Local timezone minute to start
    */
    private int startMinutes;
    /**
    * Time zone of the build schedule (string representation of the time zone id)
    */
    private String timeZoneId;

    public List<String> getBranchFilters() {
        return branchFilters;
    }

    public void setBranchFilters(final List<String> branchFilters) {
        this.branchFilters = branchFilters;
    }

    /**
    * Days for a build (flags enum for days of the week)
    */
    public ScheduleDays getDaysToBuild() {
        return daysToBuild;
    }

    /**
    * Days for a build (flags enum for days of the week)
    */
    public void setDaysToBuild(final ScheduleDays daysToBuild) {
        this.daysToBuild = daysToBuild;
    }

    /**
    * The Job Id of the Scheduled job that will queue the scheduled build. Since a single trigger can have multiple schedules and we want a single job to process a single schedule (since each schedule has a list of branches to build), the schedule itself needs to define the Job Id. This value will be filled in when a definition is added or updated.  The UI does not provide it or use it.
    */
    public UUID getScheduleJobId() {
        return scheduleJobId;
    }

    /**
    * The Job Id of the Scheduled job that will queue the scheduled build. Since a single trigger can have multiple schedules and we want a single job to process a single schedule (since each schedule has a list of branches to build), the schedule itself needs to define the Job Id. This value will be filled in when a definition is added or updated.  The UI does not provide it or use it.
    */
    public void setScheduleJobId(final UUID scheduleJobId) {
        this.scheduleJobId = scheduleJobId;
    }

    /**
    * Local timezone hour to start
    */
    public int getStartHours() {
        return startHours;
    }

    /**
    * Local timezone hour to start
    */
    public void setStartHours(final int startHours) {
        this.startHours = startHours;
    }

    /**
    * Local timezone minute to start
    */
    public int getStartMinutes() {
        return startMinutes;
    }

    /**
    * Local timezone minute to start
    */
    public void setStartMinutes(final int startMinutes) {
        this.startMinutes = startMinutes;
    }

    /**
    * Time zone of the build schedule (string representation of the time zone id)
    */
    public String getTimeZoneId() {
        return timeZoneId;
    }

    /**
    * Time zone of the build schedule (string representation of the time zone id)
    */
    public void setTimeZoneId(final String timeZoneId) {
        this.timeZoneId = timeZoneId;
    }
}
