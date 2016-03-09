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

package com.microsoft.teamfoundation.distributedtask.webapi.model;

import java.net.URI;
import java.util.Date;

/** 
 */
public class TaskLog
    extends TaskLogReference {

    private Date createdOn;
    private URI indexLocation;
    private Date lastChangedOn;
    private long lineCount;
    private String path;

    public Date getCreatedOn() {
        return createdOn;
    }

    public void setCreatedOn(final Date createdOn) {
        this.createdOn = createdOn;
    }

    public URI getIndexLocation() {
        return indexLocation;
    }

    public void setIndexLocation(final URI indexLocation) {
        this.indexLocation = indexLocation;
    }

    public Date getLastChangedOn() {
        return lastChangedOn;
    }

    public void setLastChangedOn(final Date lastChangedOn) {
        this.lastChangedOn = lastChangedOn;
    }

    public long getLineCount() {
        return lineCount;
    }

    public void setLineCount(final long lineCount) {
        this.lineCount = lineCount;
    }

    public String getPath() {
        return path;
    }

    public void setPath(final String path) {
        this.path = path;
    }
}
