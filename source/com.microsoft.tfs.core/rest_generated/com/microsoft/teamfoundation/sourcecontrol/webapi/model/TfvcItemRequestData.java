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
public class TfvcItemRequestData {

    /**
    * If true, include metadata about the file type
    */
    private boolean includeContentMetadata;
    /**
    * Whether to include the _links field on the shallow references
    */
    private boolean includeLinks;
    private TfvcItemDescriptor[] itemDescriptors;

    /**
    * If true, include metadata about the file type
    */
    public boolean getIncludeContentMetadata() {
        return includeContentMetadata;
    }

    /**
    * If true, include metadata about the file type
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

    public TfvcItemDescriptor[] getItemDescriptors() {
        return itemDescriptors;
    }

    public void setItemDescriptors(final TfvcItemDescriptor[] itemDescriptors) {
        this.itemDescriptors = itemDescriptors;
    }
}
