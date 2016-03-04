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


/** 
 */
public class WebApiTeam
    extends WebApiTeamRef {

    /**
    * Team description
    */
    private String description;
    /**
    * Identity REST API Url to this team
    */
    private String identityUrl;

    /**
    * Team description
    */
    public String getDescription() {
        return description;
    }

    /**
    * Team description
    */
    public void setDescription(final String description) {
        this.description = description;
    }

    /**
    * Identity REST API Url to this team
    */
    public String getIdentityUrl() {
        return identityUrl;
    }

    /**
    * Identity REST API Url to this team
    */
    public void setIdentityUrl(final String identityUrl) {
        this.identityUrl = identityUrl;
    }
}
