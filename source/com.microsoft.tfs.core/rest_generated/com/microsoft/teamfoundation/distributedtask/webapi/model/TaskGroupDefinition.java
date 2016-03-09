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

package com.microsoft.teamfoundation.distributedtask.webapi.model;


/** 
 */
public class TaskGroupDefinition {

    private String displayName;
    private boolean isExpanded;
    private String name;

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(final String displayName) {
        this.displayName = displayName;
    }

    public boolean getIsExpanded() {
        return isExpanded;
    }

    public void setIsExpanded(final boolean isExpanded) {
        this.isExpanded = isExpanded;
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }
}
