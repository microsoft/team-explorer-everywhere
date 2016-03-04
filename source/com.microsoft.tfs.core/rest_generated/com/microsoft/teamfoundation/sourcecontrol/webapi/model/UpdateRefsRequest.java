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
public class UpdateRefsRequest {

    private List<GitRefUpdate> refUpdateRequests;
    private GitRefUpdateMode updateMode;

    public List<GitRefUpdate> getRefUpdateRequests() {
        return refUpdateRequests;
    }

    public void setRefUpdateRequests(final List<GitRefUpdate> refUpdateRequests) {
        this.refUpdateRequests = refUpdateRequests;
    }

    public GitRefUpdateMode getUpdateMode() {
        return updateMode;
    }

    public void setUpdateMode(final GitRefUpdateMode updateMode) {
        this.updateMode = updateMode;
    }
}
