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

/** 
 */
public class GitCommitChanges {

    private ChangeCountDictionary changeCounts;
    private List<GitChange> changes;

    public ChangeCountDictionary getChangeCounts() {
        return changeCounts;
    }

    public void setChangeCounts(final ChangeCountDictionary changeCounts) {
        this.changeCounts = changeCounts;
    }

    public List<GitChange> getChanges() {
        return changes;
    }

    public void setChanges(final List<GitChange> changes) {
        this.changes = changes;
    }
}
