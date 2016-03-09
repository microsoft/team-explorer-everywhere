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

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.microsoft.visualstudio.services.webapi.model.IdentityRef;

/** 
 */
@JsonDeserialize(using = JsonDeserializer.None.class)
@JsonSerialize(using = JsonSerializer.None.class)
public class BuildDefinitionReference
    extends DefinitionReference {

    /**
    * The author of the definition.
    */
    private IdentityRef authoredBy;
    /**
    * If this is a draft definition, it might have a parent
    */
    private DefinitionReference draftOf;
    /**
    * The quality of the definition document (draft, etc.)
    */
    private DefinitionQuality quality;
    /**
    * The default queue which should be used for requests.
    */
    private AgentPoolQueue queue;

    /**
    * The author of the definition.
    */
    public IdentityRef getAuthoredBy() {
        return authoredBy;
    }

    /**
    * The author of the definition.
    */
    public void setAuthoredBy(final IdentityRef authoredBy) {
        this.authoredBy = authoredBy;
    }

    /**
    * If this is a draft definition, it might have a parent
    */
    public DefinitionReference getDraftOf() {
        return draftOf;
    }

    /**
    * If this is a draft definition, it might have a parent
    */
    public void setDraftOf(final DefinitionReference draftOf) {
        this.draftOf = draftOf;
    }

    /**
    * The quality of the definition document (draft, etc.)
    */
    public DefinitionQuality getQuality() {
        return quality;
    }

    /**
    * The quality of the definition document (draft, etc.)
    */
    public void setQuality(final DefinitionQuality quality) {
        this.quality = quality;
    }

    /**
    * The default queue which should be used for requests.
    */
    public AgentPoolQueue getQueue() {
        return queue;
    }

    /**
    * The default queue which should be used for requests.
    */
    public void setQueue(final AgentPoolQueue queue) {
        this.queue = queue;
    }
}
