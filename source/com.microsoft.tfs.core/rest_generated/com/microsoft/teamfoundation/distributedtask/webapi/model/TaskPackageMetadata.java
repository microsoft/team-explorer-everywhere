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
public class TaskPackageMetadata {

    /**
    * Gets the name of the package.
    */
    private String type;
    /**
    * Gets the url of the package.
    */
    private String url;
    /**
    * Gets the version of the package.
    */
    private String version;

    /**
    * Gets the name of the package.
    */
    public String getType() {
        return type;
    }

    /**
    * Gets the name of the package.
    */
    public void setType(final String type) {
        this.type = type;
    }

    /**
    * Gets the url of the package.
    */
    public String getUrl() {
        return url;
    }

    /**
    * Gets the url of the package.
    */
    public void setUrl(final String url) {
        this.url = url;
    }

    /**
    * Gets the version of the package.
    */
    public String getVersion() {
        return version;
    }

    /**
    * Gets the version of the package.
    */
    public void setVersion(final String version) {
        this.version = version;
    }
}
