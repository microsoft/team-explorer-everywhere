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

package com.microsoft.teamfoundation.sourcecontrol.webapi.model;

import java.util.Date;
import java.util.UUID;

/** 
 */
public class GitPushSearchCriteria {

    private Date fromDate;
    /**
    * Whether to include the _links field on the shallow references
    */
    private boolean includeLinks;
    private boolean includeRefUpdates;
    private UUID pusherId;
    private String refName;
    private Date toDate;

    public Date getFromDate() {
        return fromDate;
    }

    public void setFromDate(final Date fromDate) {
        this.fromDate = fromDate;
    }

    /**
    * Whether to include the _links field on the shallow references
    */
    public boolean getIncludeLinks() {
        return includeLinks;
    }

    /**
    * Whether to include the _links field on the shallow references
    */
    public void setIncludeLinks(final boolean includeLinks) {
        this.includeLinks = includeLinks;
    }

    public boolean getIncludeRefUpdates() {
        return includeRefUpdates;
    }

    public void setIncludeRefUpdates(final boolean includeRefUpdates) {
        this.includeRefUpdates = includeRefUpdates;
    }

    public UUID getPusherId() {
        return pusherId;
    }

    public void setPusherId(final UUID pusherId) {
        this.pusherId = pusherId;
    }

    public String getRefName() {
        return refName;
    }

    public void setRefName(final String refName) {
        this.refName = refName;
    }

    public Date getToDate() {
        return toDate;
    }

    public void setToDate(final Date toDate) {
        this.toDate = toDate;
    }
}
