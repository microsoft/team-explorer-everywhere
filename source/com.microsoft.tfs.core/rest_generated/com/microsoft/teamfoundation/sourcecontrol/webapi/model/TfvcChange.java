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
public class TfvcChange
    extends Change<TfvcItem> {

    /**
    * List of merge sources in case of rename or branch creation.
    */
    private List<TfvcMergeSource> mergeSources;
    /**
    * Version at which a (shelved) change was pended against
    */
    private int pendingVersion;

    /**
    * List of merge sources in case of rename or branch creation.
    */
    public List<TfvcMergeSource> getMergeSources() {
        return mergeSources;
    }

    /**
    * List of merge sources in case of rename or branch creation.
    */
    public void setMergeSources(final List<TfvcMergeSource> mergeSources) {
        this.mergeSources = mergeSources;
    }

    /**
    * Version at which a (shelved) change was pended against
    */
    public int getPendingVersion() {
        return pendingVersion;
    }

    /**
    * Version at which a (shelved) change was pended against
    */
    public void setPendingVersion(final int pendingVersion) {
        this.pendingVersion = pendingVersion;
    }
}
