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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.microsoft.visualstudio.services.webapi.model.ReferenceLinks;

/** 
 */
public class AgentPoolQueue
    extends ShallowReference {

    private ReferenceLinks _links;
    /**
    * The pool used by this queue.
    */
    private TaskAgentPoolReference pool;

    @JsonProperty("_links")
    public ReferenceLinks getLinks() {
        return _links;
    }

    @JsonProperty("_links")
    public void setLinks(final ReferenceLinks _links) {
        this._links = _links;
    }

    /**
    * The pool used by this queue.
    */
    public TaskAgentPoolReference getPool() {
        return pool;
    }

    /**
    * The pool used by this queue.
    */
    public void setPool(final TaskAgentPoolReference pool) {
        this.pool = pool;
    }
}
