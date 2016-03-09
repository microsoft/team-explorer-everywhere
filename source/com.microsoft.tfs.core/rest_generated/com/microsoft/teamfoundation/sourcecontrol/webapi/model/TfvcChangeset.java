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

import java.util.List;
import java.util.UUID;

/** 
 */
public class TfvcChangeset
    extends TfvcChangesetRef {

    private UUID accountId;
    private List<TfvcChange> changes;
    private CheckinNote[] checkinNotes;
    private UUID collectionId;
    private boolean hasMoreChanges;
    private TfvcPolicyOverrideInfo policyOverride;
    private List<AssociatedWorkItem> workItems;

    public UUID getAccountId() {
        return accountId;
    }

    public void setAccountId(final UUID accountId) {
        this.accountId = accountId;
    }

    public List<TfvcChange> getChanges() {
        return changes;
    }

    public void setChanges(final List<TfvcChange> changes) {
        this.changes = changes;
    }

    public CheckinNote[] getCheckinNotes() {
        return checkinNotes;
    }

    public void setCheckinNotes(final CheckinNote[] checkinNotes) {
        this.checkinNotes = checkinNotes;
    }

    public UUID getCollectionId() {
        return collectionId;
    }

    public void setCollectionId(final UUID collectionId) {
        this.collectionId = collectionId;
    }

    public boolean getHasMoreChanges() {
        return hasMoreChanges;
    }

    public void setHasMoreChanges(final boolean hasMoreChanges) {
        this.hasMoreChanges = hasMoreChanges;
    }

    public TfvcPolicyOverrideInfo getPolicyOverride() {
        return policyOverride;
    }

    public void setPolicyOverride(final TfvcPolicyOverrideInfo policyOverride) {
        this.policyOverride = policyOverride;
    }

    public List<AssociatedWorkItem> getWorkItems() {
        return workItems;
    }

    public void setWorkItems(final List<AssociatedWorkItem> workItems) {
        this.workItems = workItems;
    }
}
