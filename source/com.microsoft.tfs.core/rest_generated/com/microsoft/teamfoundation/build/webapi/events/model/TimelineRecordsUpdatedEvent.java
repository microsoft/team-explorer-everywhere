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

package com.microsoft.teamfoundation.build.webapi.events.model;

import java.util.List;
import com.microsoft.teamfoundation.build.webapi.model.TimelineRecord;

/** 
 */
public class TimelineRecordsUpdatedEvent
    extends RealtimeBuildEvent {

    private List<TimelineRecord> timelineRecords;

    public List<TimelineRecord> getTimelineRecords() {
        return timelineRecords;
    }

    public void setTimelineRecords(final List<TimelineRecord> timelineRecords) {
        this.timelineRecords = timelineRecords;
    }
}
