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
public class TaskAgentReference {

    /**
    * Gets the identifier of the agent.
    */
    private int id;
    /**
    * Gets the name of the agent.
    */
    private String name;
    /**
    * Gets the version of the agent.
    */
    private String version;

    /**
    * Gets the identifier of the agent.
    */
    public int getId() {
        return id;
    }

    /**
    * Gets the identifier of the agent.
    */
    public void setId(final int id) {
        this.id = id;
    }

    /**
    * Gets the name of the agent.
    */
    public String getName() {
        return name;
    }

    /**
    * Gets the name of the agent.
    */
    public void setName(final String name) {
        this.name = name;
    }

    /**
    * Gets the version of the agent.
    */
    public String getVersion() {
        return version;
    }

    /**
    * Gets the version of the agent.
    */
    public void setVersion(final String version) {
        this.version = version;
    }
}
