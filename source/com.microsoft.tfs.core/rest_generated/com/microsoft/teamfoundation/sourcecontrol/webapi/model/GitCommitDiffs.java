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

import java.util.HashMap;
import java.util.List;

/** 
 */
public class GitCommitDiffs {

    private int aheadCount;
    private boolean allChangesIncluded;
    private int behindCount;
    private HashMap<VersionControlChangeType,Integer> changeCounts;
    private List<GitChange> changes;
    private String commonCommit;

    public int getAheadCount() {
        return aheadCount;
    }

    public void setAheadCount(final int aheadCount) {
        this.aheadCount = aheadCount;
    }

    public boolean getAllChangesIncluded() {
        return allChangesIncluded;
    }

    public void setAllChangesIncluded(final boolean allChangesIncluded) {
        this.allChangesIncluded = allChangesIncluded;
    }

    public int getBehindCount() {
        return behindCount;
    }

    public void setBehindCount(final int behindCount) {
        this.behindCount = behindCount;
    }

    public HashMap<VersionControlChangeType,Integer> getChangeCounts() {
        return changeCounts;
    }

    public void setChangeCounts(final HashMap<VersionControlChangeType,Integer> changeCounts) {
        this.changeCounts = changeCounts;
    }

    public List<GitChange> getChanges() {
        return changes;
    }

    public void setChanges(final List<GitChange> changes) {
        this.changes = changes;
    }

    public String getCommonCommit() {
        return commonCommit;
    }

    public void setCommonCommit(final String commonCommit) {
        this.commonCommit = commonCommit;
    }
}
