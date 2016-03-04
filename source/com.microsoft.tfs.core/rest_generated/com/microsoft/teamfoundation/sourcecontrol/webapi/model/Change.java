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
public class Change<T> {

    private VersionControlChangeType changeType;
    private T item;
    private ItemContent newContent;
    private String sourceServerItem;
    private String url;

    public VersionControlChangeType getChangeType() {
        return changeType;
    }

    public void setChangeType(final VersionControlChangeType changeType) {
        this.changeType = changeType;
    }

    public T getItem() {
        return item;
    }

    public void setItem(final T item) {
        this.item = item;
    }

    public ItemContent getNewContent() {
        return newContent;
    }

    public void setNewContent(final ItemContent newContent) {
        this.newContent = newContent;
    }

    public String getSourceServerItem() {
        return sourceServerItem;
    }

    public void setSourceServerItem(final String sourceServerItem) {
        this.sourceServerItem = sourceServerItem;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(final String url) {
        this.url = url;
    }
}
