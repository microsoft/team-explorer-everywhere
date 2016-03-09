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
public class WebApiTeamRef {

    /**
    * Team (Identity) Guid. A Team Foundation ID.
    */
    private UUID id;
    /**
    * Team name
    */
    private String name;
    /**
    * Team REST API Url
    */
    private String url;

    /**
    * Team (Identity) Guid. A Team Foundation ID.
    */
    public UUID getId() {
        return id;
    }

    /**
    * Team (Identity) Guid. A Team Foundation ID.
    */
    public void setId(final UUID id) {
        this.id = id;
    }

    /**
    * Team name
    */
    public String getName() {
        return name;
    }

    /**
    * Team name
    */
    public void setName(final String name) {
        this.name = name;
    }

    /**
    * Team REST API Url
    */
    public String getUrl() {
        return url;
    }

    /**
    * Team REST API Url
    */
    public void setUrl(final String url) {
        this.url = url;
    }
}
