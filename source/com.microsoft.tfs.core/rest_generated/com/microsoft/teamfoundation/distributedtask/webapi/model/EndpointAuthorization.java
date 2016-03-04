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

/** 
 */
public class EndpointAuthorization {

    private HashMap<String,String> parameters;
    private String scheme;

    public HashMap<String,String> getParameters() {
        return parameters;
    }

    public void setParameters(final HashMap<String,String> parameters) {
        this.parameters = parameters;
    }

    public String getScheme() {
        return scheme;
    }

    public void setScheme(final String scheme) {
        this.scheme = scheme;
    }
}
