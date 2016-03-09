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

package com.microsoft.teamfoundation.build.webapi.model;


/** 
 * Data representation of a build log reference
 * 
 */
public class BuildLogReference {

    /**
    * The id of the log.
    */
    private int id;
    /**
    * The type of the log location.
    */
    private String type;
    /**
    * Full link to the log resource.
    */
    private String url;

    /**
    * The id of the log.
    */
    public int getId() {
        return id;
    }

    /**
    * The id of the log.
    */
    public void setId(final int id) {
        this.id = id;
    }

    /**
    * The type of the log location.
    */
    public String getType() {
        return type;
    }

    /**
    * The type of the log location.
    */
    public void setType(final String type) {
        this.type = type;
    }

    /**
    * Full link to the log resource.
    */
    public String getUrl() {
        return url;
    }

    /**
    * Full link to the log resource.
    */
    public void setUrl(final String url) {
        this.url = url;
    }
}
