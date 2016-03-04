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

import java.util.HashMap;
import java.util.List;
import java.util.UUID;

/** 
 * Represents the context of variables and vectors for a job request.
 * 
 */
public class JobEnvironment {

    private List<ServiceEndpoint> endpoints;
    private List<MaskHint> mask;
    private HashMap<UUID,JobOption> options;
    /**
    * Gets or sets the endpoint used for communicating back to the calling service.
    */
    private ServiceEndpoint systemConnection;
    private HashMap<String,String> variables;

    public List<ServiceEndpoint> getEndpoints() {
        return endpoints;
    }

    public void setEndpoints(final List<ServiceEndpoint> endpoints) {
        this.endpoints = endpoints;
    }

    public List<MaskHint> getMask() {
        return mask;
    }

    public void setMask(final List<MaskHint> mask) {
        this.mask = mask;
    }

    public HashMap<UUID,JobOption> getOptions() {
        return options;
    }

    public void setOptions(final HashMap<UUID,JobOption> options) {
        this.options = options;
    }

    /**
    * Gets or sets the endpoint used for communicating back to the calling service.
    */
    public ServiceEndpoint getSystemConnection() {
        return systemConnection;
    }

    /**
    * Gets or sets the endpoint used for communicating back to the calling service.
    */
    public void setSystemConnection(final ServiceEndpoint systemConnection) {
        this.systemConnection = systemConnection;
    }

    public HashMap<String,String> getVariables() {
        return variables;
    }

    public void setVariables(final HashMap<String,String> variables) {
        this.variables = variables;
    }
}
