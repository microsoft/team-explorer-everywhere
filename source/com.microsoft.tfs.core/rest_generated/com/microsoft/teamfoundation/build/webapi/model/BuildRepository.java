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

import java.net.URI;
import java.util.HashMap;

/** 
 */
public class BuildRepository {

    private boolean checkoutSubmodules;
    /**
    * Indicates whether to clean the target folder when getting code from the repository. This is a String so that it can reference variables.
    */
    private String clean;
    /**
    * Gets or sets the name of the default branch.
    */
    private String defaultBranch;
    private String id;
    /**
    * Gets or sets the friendly name of the repository.
    */
    private String name;
    private HashMap<String,String> properties;
    /**
    * Gets or sets the root folder.
    */
    private String rootFolder;
    /**
    * Gets or sets the type of the repository.
    */
    private String type;
    /**
    * Gets or sets the url of the repository.
    */
    private URI url;

    public boolean getCheckoutSubmodules() {
        return checkoutSubmodules;
    }

    public void setCheckoutSubmodules(final boolean checkoutSubmodules) {
        this.checkoutSubmodules = checkoutSubmodules;
    }

    /**
    * Indicates whether to clean the target folder when getting code from the repository. This is a String so that it can reference variables.
    */
    public String getClean() {
        return clean;
    }

    /**
    * Indicates whether to clean the target folder when getting code from the repository. This is a String so that it can reference variables.
    */
    public void setClean(final String clean) {
        this.clean = clean;
    }

    /**
    * Gets or sets the name of the default branch.
    */
    public String getDefaultBranch() {
        return defaultBranch;
    }

    /**
    * Gets or sets the name of the default branch.
    */
    public void setDefaultBranch(final String defaultBranch) {
        this.defaultBranch = defaultBranch;
    }

    public String getId() {
        return id;
    }

    public void setId(final String id) {
        this.id = id;
    }

    /**
    * Gets or sets the friendly name of the repository.
    */
    public String getName() {
        return name;
    }

    /**
    * Gets or sets the friendly name of the repository.
    */
    public void setName(final String name) {
        this.name = name;
    }

    public HashMap<String,String> getProperties() {
        return properties;
    }

    public void setProperties(final HashMap<String,String> properties) {
        this.properties = properties;
    }

    /**
    * Gets or sets the root folder.
    */
    public String getRootFolder() {
        return rootFolder;
    }

    /**
    * Gets or sets the root folder.
    */
    public void setRootFolder(final String rootFolder) {
        this.rootFolder = rootFolder;
    }

    /**
    * Gets or sets the type of the repository.
    */
    public String getType() {
        return type;
    }

    /**
    * Gets or sets the type of the repository.
    */
    public void setType(final String type) {
        this.type = type;
    }

    /**
    * Gets or sets the url of the repository.
    */
    public URI getUrl() {
        return url;
    }

    /**
    * Gets or sets the url of the repository.
    */
    public void setUrl(final URI url) {
        this.url = url;
    }
}
