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
 * Optional details to include when returning an item model
 * 
 */
public class ItemDetailsOptions {

    /**
    * If true, include metadata about the file type
    */
    private boolean includeContentMetadata;
    /**
    * Specifies whether to include children (OneLevel), all descendants (Full) or None for folder items
    */
    private VersionControlRecursionType recursionLevel;

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
    * Specifies whether to include children (OneLevel), all descendants (Full) or None for folder items
    */
    public VersionControlRecursionType getRecursionLevel() {
        return recursionLevel;
    }

    /**
    * Specifies whether to include children (OneLevel), all descendants (Full) or None for folder items
    */
    public void setRecursionLevel(final VersionControlRecursionType recursionLevel) {
        this.recursionLevel = recursionLevel;
    }
}
