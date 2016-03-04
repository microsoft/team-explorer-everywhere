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

/** 
 */
public class TfvcItem
    extends ItemModel {

    private Date changeDate;
    private int deletionId;
    private boolean isBranch;
    private boolean isPendingChange;
    private int version;

    public Date getChangeDate() {
        return changeDate;
    }

    public void setChangeDate(final Date changeDate) {
        this.changeDate = changeDate;
    }

    public int getDeletionId() {
        return deletionId;
    }

    public void setDeletionId(final int deletionId) {
        this.deletionId = deletionId;
    }

    public boolean getIsBranch() {
        return isBranch;
    }

    public void setIsBranch(final boolean isBranch) {
        this.isBranch = isBranch;
    }

    public boolean getIsPendingChange() {
        return isPendingChange;
    }

    public void setIsPendingChange(final boolean isPendingChange) {
        this.isPendingChange = isPendingChange;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(final int version) {
        this.version = version;
    }
}
