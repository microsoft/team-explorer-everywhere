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

package com.microsoft.teamfoundation.core.webapi.model;

import java.util.UUID;

/** 
 */
public class WebApiProjectCollectionRef {

    /**
    * Collection Tfs Url (Host Url)
    */
    private String collectionUrl;
    /**
    * Collection Guid
    */
    private UUID id;
    /**
    * Collection Name
    */
    private String name;
    /**
    * Collection REST Url
    */
    private String url;

    /**
    * Collection Tfs Url (Host Url)
    */
    public String getCollectionUrl() {
        return collectionUrl;
    }

    /**
    * Collection Tfs Url (Host Url)
    */
    public void setCollectionUrl(final String collectionUrl) {
        this.collectionUrl = collectionUrl;
    }

    /**
    * Collection Guid
    */
    public UUID getId() {
        return id;
    }

    /**
    * Collection Guid
    */
    public void setId(final UUID id) {
        this.id = id;
    }

    /**
    * Collection Name
    */
    public String getName() {
        return name;
    }

    /**
    * Collection Name
    */
    public void setName(final String name) {
        this.name = name;
    }

    /**
    * Collection REST Url
    */
    public String getUrl() {
        return url;
    }

    /**
    * Collection REST Url
    */
    public void setUrl(final String url) {
        this.url = url;
    }
}
