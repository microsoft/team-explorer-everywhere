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

import java.util.Date;
import java.util.List;
import java.util.UUID;

/** 
 */
public class Timeline
    extends TimelineReference {

    private UUID lastChangedBy;
    private Date lastChangedOn;
    private List<TimelineRecord> records;

    public UUID getLastChangedBy() {
        return lastChangedBy;
    }

    public void setLastChangedBy(final UUID lastChangedBy) {
        this.lastChangedBy = lastChangedBy;
    }

    public Date getLastChangedOn() {
        return lastChangedOn;
    }

    public void setLastChangedOn(final Date lastChangedOn) {
        this.lastChangedOn = lastChangedOn;
    }

    public List<TimelineRecord> getRecords() {
        return records;
    }

    public void setRecords(final List<TimelineRecord> records) {
        this.records = records;
    }
}
