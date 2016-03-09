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
public class GitItemRequestData {

    /**
    * Whether to include metadata for all items
    */
    private boolean includeContentMetadata;
    /**
    * Whether to include the _links field on the shallow references
    */
    private boolean includeLinks;
    /**
    * Collection of items to fetch, including path, version, and recursion level
    */
    private GitItemDescriptor[] itemDescriptors;
    /**
    * Whether to include shallow ref to commit that last changed each item
    */
    private boolean latestProcessedChange;

    /**
    * Whether to include metadata for all items
    */
    public boolean getIncludeContentMetadata() {
        return includeContentMetadata;
    }

    /**
    * Whether to include metadata for all items
    */
    public void setIncludeContentMetadata(final boolean includeContentMetadata) {
        this.includeContentMetadata = includeContentMetadata;
    }

    /**
    * Whether to include the _links field on the shallow references
    */
    public boolean getIncludeLinks() {
        return includeLinks;
    }

    /**
    * Whether to include the _links field on the shallow references
    */
    public void setIncludeLinks(final boolean includeLinks) {
        this.includeLinks = includeLinks;
    }

    /**
    * Collection of items to fetch, including path, version, and recursion level
    */
    public GitItemDescriptor[] getItemDescriptors() {
        return itemDescriptors;
    }

    /**
    * Collection of items to fetch, including path, version, and recursion level
    */
    public void setItemDescriptors(final GitItemDescriptor[] itemDescriptors) {
        this.itemDescriptors = itemDescriptors;
    }

    /**
    * Whether to include shallow ref to commit that last changed each item
    */
    public boolean getLatestProcessedChange() {
        return latestProcessedChange;
    }

    /**
    * Whether to include shallow ref to commit that last changed each item
    */
    public void setLatestProcessedChange(final boolean latestProcessedChange) {
        this.latestProcessedChange = latestProcessedChange;
    }
}
