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
public class TfvcShelvesetRequestData {

    /**
    * Whether to include policyOverride and notes
    */
    private boolean includeDetails;
    /**
    * Whether to include the _links field on the shallow references
    */
    private boolean includeLinks;
    /**
    * Whether to include workItems
    */
    private boolean includeWorkItems;
    /**
    * Max number of changes to include
    */
    private int maxChangeCount;
    /**
    * Max length of comment
    */
    private int maxCommentLength;
    /**
    * Shelveset's name
    */
    private String name;
    /**
    * Owner's ID. Could be a name or a guid.
    */
    private String owner;

    /**
    * Whether to include policyOverride and notes
    */
    public boolean getIncludeDetails() {
        return includeDetails;
    }

    /**
    * Whether to include policyOverride and notes
    */
    public void setIncludeDetails(final boolean includeDetails) {
        this.includeDetails = includeDetails;
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
    * Whether to include workItems
    */
    public boolean getIncludeWorkItems() {
        return includeWorkItems;
    }

    /**
    * Whether to include workItems
    */
    public void setIncludeWorkItems(final boolean includeWorkItems) {
        this.includeWorkItems = includeWorkItems;
    }

    /**
    * Max number of changes to include
    */
    public int getMaxChangeCount() {
        return maxChangeCount;
    }

    /**
    * Max number of changes to include
    */
    public void setMaxChangeCount(final int maxChangeCount) {
        this.maxChangeCount = maxChangeCount;
    }

    /**
    * Max length of comment
    */
    public int getMaxCommentLength() {
        return maxCommentLength;
    }

    /**
    * Max length of comment
    */
    public void setMaxCommentLength(final int maxCommentLength) {
        this.maxCommentLength = maxCommentLength;
    }

    /**
    * Shelveset's name
    */
    public String getName() {
        return name;
    }

    /**
    * Shelveset's name
    */
    public void setName(final String name) {
        this.name = name;
    }

    /**
    * Owner's ID. Could be a name or a guid.
    */
    public String getOwner() {
        return owner;
    }

    /**
    * Owner's ID. Could be a name or a guid.
    */
    public void setOwner(final String owner) {
        this.owner = owner;
    }
}
