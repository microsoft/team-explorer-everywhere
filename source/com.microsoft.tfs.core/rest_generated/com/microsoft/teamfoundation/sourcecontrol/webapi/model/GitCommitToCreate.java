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


/** 
 */
public class GitCommitToCreate {

    private GitRef baseRef;
    private String comment;
    private GitPathAction[] pathActions;

    public GitRef getBaseRef() {
        return baseRef;
    }

    public void setBaseRef(final GitRef baseRef) {
        this.baseRef = baseRef;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(final String comment) {
        this.comment = comment;
    }

    public GitPathAction[] getPathActions() {
        return pathActions;
    }

    public void setPathActions(final GitPathAction[] pathActions) {
        this.pathActions = pathActions;
    }
}
