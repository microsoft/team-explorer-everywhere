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
public class BuildDefinitionRevision {

    private IdentityRef changedBy;
    private Date changedDate;
    private AuditAction changeType;
    private String comment;
    private String definitionUrl;
    private String name;
    private int revision;

    public IdentityRef getChangedBy() {
        return changedBy;
    }

    public void setChangedBy(final IdentityRef changedBy) {
        this.changedBy = changedBy;
    }

    public Date getChangedDate() {
        return changedDate;
    }

    public void setChangedDate(final Date changedDate) {
        this.changedDate = changedDate;
    }

    public AuditAction getChangeType() {
        return changeType;
    }

    public void setChangeType(final AuditAction changeType) {
        this.changeType = changeType;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(final String comment) {
        this.comment = comment;
    }

    public String getDefinitionUrl() {
        return definitionUrl;
    }

    public void setDefinitionUrl(final String definitionUrl) {
        this.definitionUrl = definitionUrl;
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public int getRevision() {
        return revision;
    }

    public void setRevision(final int revision) {
        this.revision = revision;
    }
}
