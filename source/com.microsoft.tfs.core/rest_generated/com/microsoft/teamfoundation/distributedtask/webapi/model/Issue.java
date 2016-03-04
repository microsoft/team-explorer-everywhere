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

import java.util.HashMap;

/** 
 */
public class Issue {

    private String category;
    private HashMap<String,String> data;
    private String message;
    private IssueType type;

    public String getCategory() {
        return category;
    }

    public void setCategory(final String category) {
        this.category = category;
    }

    public HashMap<String,String> getData() {
        return data;
    }

    public void setData(final HashMap<String,String> data) {
        this.data = data;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(final String message) {
        this.message = message;
    }

    public IssueType getType() {
        return type;
    }

    public void setType(final IssueType type) {
        this.type = type;
    }
}
