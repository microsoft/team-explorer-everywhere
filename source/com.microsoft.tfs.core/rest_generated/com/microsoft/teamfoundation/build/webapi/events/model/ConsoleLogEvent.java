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
import java.util.UUID;

/** 
 */
public class ConsoleLogEvent
    extends RealtimeBuildEvent {

    private List<String> lines;
    private UUID timelineId;
    private UUID timelineRecordId;

    public List<String> getLines() {
        return lines;
    }

    public void setLines(final List<String> lines) {
        this.lines = lines;
    }

    public UUID getTimelineId() {
        return timelineId;
    }

    public void setTimelineId(final UUID timelineId) {
        this.timelineId = timelineId;
    }

    public UUID getTimelineRecordId() {
        return timelineRecordId;
    }

    public void setTimelineRecordId(final UUID timelineRecordId) {
        this.timelineRecordId = timelineRecordId;
    }
}
