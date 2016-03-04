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

import java.net.URI;
import java.util.HashMap;
import java.util.UUID;

/** 
 * Represents an endpoint which may be used by an orchestration job.
 * 
 */
public class ServiceEndpoint {

    /**
    * Gets or sets the authorization data for talking to the endpoint.
    */
    private EndpointAuthorization authorization;
    private HashMap<String,String> data;
    /**
    * Gets or sets the identifier of this endpoint.
    */
    private UUID id;
    /**
    * Gets or sets the friendly name of the endpoint.
    */
    private String name;
    /**
    * Gets or sets the type of the endpoint.
    */
    private String type;
    /**
    * Gets or sets the url of the endpoint.
    */
    private URI url;

    /**
    * Gets or sets the authorization data for talking to the endpoint.
    */
    public EndpointAuthorization getAuthorization() {
        return authorization;
    }

    /**
    * Gets or sets the authorization data for talking to the endpoint.
    */
    public void setAuthorization(final EndpointAuthorization authorization) {
        this.authorization = authorization;
    }

    public HashMap<String,String> getData() {
        return data;
    }

    public void setData(final HashMap<String,String> data) {
        this.data = data;
    }

    /**
    * Gets or sets the identifier of this endpoint.
    */
    public UUID getId() {
        return id;
    }

    /**
    * Gets or sets the identifier of this endpoint.
    */
    public void setId(final UUID id) {
        this.id = id;
    }

    /**
    * Gets or sets the friendly name of the endpoint.
    */
    public String getName() {
        return name;
    }

    /**
    * Gets or sets the friendly name of the endpoint.
    */
    public void setName(final String name) {
        this.name = name;
    }

    /**
    * Gets or sets the type of the endpoint.
    */
    public String getType() {
        return type;
    }

    /**
    * Gets or sets the type of the endpoint.
    */
    public void setType(final String type) {
        this.type = type;
    }

    /**
    * Gets or sets the url of the endpoint.
    */
    public URI getUrl() {
        return url;
    }

    /**
    * Gets or sets the url of the endpoint.
    */
    public void setUrl(final URI url) {
        this.url = url;
    }
}
