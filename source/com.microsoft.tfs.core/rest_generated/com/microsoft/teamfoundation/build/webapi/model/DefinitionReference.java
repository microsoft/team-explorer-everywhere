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
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.microsoft.teamfoundation.core.webapi.model.TeamProjectReference;

/** 
 * A reference to a definition.
 * 
 */
@JsonDeserialize(using = DefinitionReferenceDeserializer.class)
@JsonSerialize(using = DefinitionReferenceSerializer.class)
public class DefinitionReference
    extends ShallowReference {

    /**
    * The project.
    */
    private TeamProjectReference project;
    /**
    * If builds can be queued from this definition
    */
    private DefinitionQueueStatus queueStatus;
    /**
    * The definition revision number.
    */
    private int revision;
    /**
    * The type of the definition.
    */
    private DefinitionType type;
    /**
    * The Uri of the definition
    */
    private URI uri;

    /**
    * The project.
    */
    public TeamProjectReference getProject() {
        return project;
    }

    /**
    * The project.
    */
    public void setProject(final TeamProjectReference project) {
        this.project = project;
    }

    /**
    * If builds can be queued from this definition
    */
    public DefinitionQueueStatus getQueueStatus() {
        return queueStatus;
    }

    /**
    * If builds can be queued from this definition
    */
    public void setQueueStatus(final DefinitionQueueStatus queueStatus) {
        this.queueStatus = queueStatus;
    }

    /**
    * The definition revision number.
    */
    public int getRevision() {
        return revision;
    }

    /**
    * The definition revision number.
    */
    public void setRevision(final int revision) {
        this.revision = revision;
    }

    /**
    * The type of the definition.
    */
    public DefinitionType getType() {
        return type;
    }

    /**
    * The type of the definition.
    */
    public void setType(final DefinitionType type) {
        this.type = type;
    }

    /**
    * The Uri of the definition
    */
    public URI getUri() {
        return uri;
    }

    /**
    * The Uri of the definition
    */
    public void setUri(final URI uri) {
        this.uri = uri;
    }
}
