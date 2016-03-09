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
import com.microsoft.visualstudio.services.webapi.model.IdentityRef;

/** 
 */
public class BuildSummary {

    private ShallowReference build;
    private Date finishTime;
    private boolean keepForever;
    private String quality;
    private BuildReason reason;
    private IdentityRef requestedFor;
    private Date startTime;
    private BuildStatus status;

    public ShallowReference getBuild() {
        return build;
    }

    public void setBuild(final ShallowReference build) {
        this.build = build;
    }

    public Date getFinishTime() {
        return finishTime;
    }

    public void setFinishTime(final Date finishTime) {
        this.finishTime = finishTime;
    }

    public boolean getKeepForever() {
        return keepForever;
    }

    public void setKeepForever(final boolean keepForever) {
        this.keepForever = keepForever;
    }

    public String getQuality() {
        return quality;
    }

    public void setQuality(final String quality) {
        this.quality = quality;
    }

    public BuildReason getReason() {
        return reason;
    }

    public void setReason(final BuildReason reason) {
        this.reason = reason;
    }

    public IdentityRef getRequestedFor() {
        return requestedFor;
    }

    public void setRequestedFor(final IdentityRef requestedFor) {
        this.requestedFor = requestedFor;
    }

    public Date getStartTime() {
        return startTime;
    }

    public void setStartTime(final Date startTime) {
        this.startTime = startTime;
    }

    public BuildStatus getStatus() {
        return status;
    }

    public void setStatus(final BuildStatus status) {
        this.status = status;
    }
}
