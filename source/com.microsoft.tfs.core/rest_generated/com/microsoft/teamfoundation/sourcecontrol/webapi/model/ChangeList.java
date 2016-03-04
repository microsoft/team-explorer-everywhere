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
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

/** 
 */
public class ChangeList<T> {

    private boolean allChangesIncluded;
    private HashMap<VersionControlChangeType,Integer> changeCounts;
    private List<Change<T>> changes;
    private String comment;
    private boolean commentTruncated;
    private Date creationDate;
    private CheckinNote[] notes;
    private String owner;
    private String ownerDisplayName;
    private UUID ownerId;
    private Date sortDate;
    private String version;

    public boolean getAllChangesIncluded() {
        return allChangesIncluded;
    }

    public void setAllChangesIncluded(final boolean allChangesIncluded) {
        this.allChangesIncluded = allChangesIncluded;
    }

    public HashMap<VersionControlChangeType,Integer> getChangeCounts() {
        return changeCounts;
    }

    public void setChangeCounts(final HashMap<VersionControlChangeType,Integer> changeCounts) {
        this.changeCounts = changeCounts;
    }

    public List<Change<T>> getChanges() {
        return changes;
    }

    public void setChanges(final List<Change<T>> changes) {
        this.changes = changes;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(final String comment) {
        this.comment = comment;
    }

    public boolean getCommentTruncated() {
        return commentTruncated;
    }

    public void setCommentTruncated(final boolean commentTruncated) {
        this.commentTruncated = commentTruncated;
    }

    public Date getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(final Date creationDate) {
        this.creationDate = creationDate;
    }

    public CheckinNote[] getNotes() {
        return notes;
    }

    public void setNotes(final CheckinNote[] notes) {
        this.notes = notes;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(final String owner) {
        this.owner = owner;
    }

    public String getOwnerDisplayName() {
        return ownerDisplayName;
    }

    public void setOwnerDisplayName(final String ownerDisplayName) {
        this.ownerDisplayName = ownerDisplayName;
    }

    public UUID getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(final UUID ownerId) {
        this.ownerId = ownerId;
    }

    public Date getSortDate() {
        return sortDate;
    }

    public void setSortDate(final Date sortDate) {
        this.sortDate = sortDate;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(final String version) {
        this.version = version;
    }
}
