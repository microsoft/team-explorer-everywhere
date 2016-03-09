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

/** 
 * Represents a build log.
 * 
 */
public class BuildLog
    extends BuildLogReference {

    /**
    * The date the log was created.
    */
    private Date createdOn;
    /**
    * The date the log was last changed.
    */
    private Date lastChangedOn;
    /**
    * The number of lines in the log.
    */
    private long lineCount;

    /**
    * The date the log was created.
    */
    public Date getCreatedOn() {
        return createdOn;
    }

    /**
    * The date the log was created.
    */
    public void setCreatedOn(final Date createdOn) {
        this.createdOn = createdOn;
    }

    /**
    * The date the log was last changed.
    */
    public Date getLastChangedOn() {
        return lastChangedOn;
    }

    /**
    * The date the log was last changed.
    */
    public void setLastChangedOn(final Date lastChangedOn) {
        this.lastChangedOn = lastChangedOn;
    }

    /**
    * The number of lines in the log.
    */
    public long getLineCount() {
        return lineCount;
    }

    /**
    * The number of lines in the log.
    */
    public void setLineCount(final long lineCount) {
        this.lineCount = lineCount;
    }
}
