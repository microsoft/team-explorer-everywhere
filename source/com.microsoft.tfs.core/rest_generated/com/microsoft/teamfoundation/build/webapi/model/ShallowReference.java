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
 * An abstracted reference to some other resource. This class is used to provide the build data contracts with a uniform way to reference other resources in a way that provides easy traversal through links.
 * 
 */
public class ShallowReference {

    /**
    * Id of the resource
    */
    private int id;
    /**
    * Name of the linked resource (definition name, controller name, etc.)
    */
    private String name;
    /**
    * Full http link to the resource
    */
    private String url;

    /**
    * Id of the resource
    */
    public int getId() {
        return id;
    }

    /**
    * Id of the resource
    */
    public void setId(final int id) {
        this.id = id;
    }

    /**
    * Name of the linked resource (definition name, controller name, etc.)
    */
    public String getName() {
        return name;
    }

    /**
    * Name of the linked resource (definition name, controller name, etc.)
    */
    public void setName(final String name) {
        this.name = name;
    }

    /**
    * Full http link to the resource
    */
    public String getUrl() {
        return url;
    }

    /**
    * Full http link to the resource
    */
    public void setUrl(final String url) {
        this.url = url;
    }
}
