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

import com.microsoft.visualstudio.services.webapi.model.IdentityRef;

/** 
 */
public class RequestReference {

    /**
    * Id of the resource
    */
    private int id;
    /**
    * Name of the requestor
    */
    private IdentityRef requestedFor;
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
    * Name of the requestor
    */
    public IdentityRef getRequestedFor() {
        return requestedFor;
    }

    /**
    * Name of the requestor
    */
    public void setRequestedFor(final IdentityRef requestedFor) {
        this.requestedFor = requestedFor;
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
