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
public class AssociatedWorkItem {

    private String assignedTo;
    private int id;
    private String state;
    private String title;
    /**
    * REST url
    */
    private String url;
    private String webUrl;
    private String workItemType;

    public String getAssignedTo() {
        return assignedTo;
    }

    public void setAssignedTo(final String assignedTo) {
        this.assignedTo = assignedTo;
    }

    public int getId() {
        return id;
    }

    public void setId(final int id) {
        this.id = id;
    }

    public String getState() {
        return state;
    }

    public void setState(final String state) {
        this.state = state;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(final String title) {
        this.title = title;
    }

    /**
    * REST url
    */
    public String getUrl() {
        return url;
    }

    /**
    * REST url
    */
    public void setUrl(final String url) {
        this.url = url;
    }

    public String getWebUrl() {
        return webUrl;
    }

    public void setWebUrl(final String webUrl) {
        this.webUrl = webUrl;
    }

    public String getWorkItemType() {
        return workItemType;
    }

    public void setWorkItemType(final String workItemType) {
        this.workItemType = workItemType;
    }
}
